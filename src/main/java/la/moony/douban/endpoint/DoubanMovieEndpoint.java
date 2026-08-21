package la.moony.douban.endpoint;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import la.moony.douban.DoubanMovieQuery;
import la.moony.douban.DoubanPosterProxy;
import la.moony.douban.SettingConfig;
import la.moony.douban.service.DoubanService;
import la.moony.douban.sqlite.SqliteDoubanMovieStore;
import la.moony.douban.sqlite.SqlitePage;
import la.moony.douban.sqlite.entity.DoubanMovieData;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

/**
 * Console 豆瓣条目 CRUD，替代原 Extension API（doubanCoreApiClient.doubanMovie）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DoubanMovieEndpoint implements CustomEndpoint {

    private final String tag = "console.api.douban.moony.la/v1alpha1/DoubanMovie";

    private final SqliteDoubanMovieStore movieStore;
    private final DoubanService doubanService;
    private final SettingConfig settingConfig;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var idPathParam = parameterBuilder().name("id")
            .in(ParameterIn.PATH)
            .required(true)
            .implementation(String.class);

        return SpringdocRouteBuilder.route()
            .GET("doubanmovies", this::listDoubanMovies, builder -> {
                builder.operationId("ListDoubanMovies")
                    .description("分页查询豆瓣条目")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(DoubanMovieListResponse.class)
                    );
                DoubanMovieQuery.buildParameters(builder);
            })
            .POST("doubanmovies", this::createDoubanMovie, builder ->
                builder.operationId("CreateDoubanMovie")
                    .tag(tag)
                    .description("创建豆瓣条目")
                    .requestBody(requestBodyBuilder().implementation(DoubanMovieData.class))
                    .response(responseBuilder().implementation(DoubanMovieData.class))
            )
            .GET("doubanmovies/{id}", this::getDoubanMovie, builder ->
                builder.operationId("GetDoubanMovie")
                    .tag(tag)
                    .description("获取豆瓣条目")
                    .parameter(idPathParam)
                    .response(responseBuilder().implementation(DoubanMovieData.class))
            )
            .PUT("doubanmovies/{id}", this::updateDoubanMovie, builder ->
                builder.operationId("UpdateDoubanMovie")
                    .tag(tag)
                    .description("更新豆瓣条目")
                    .parameter(idPathParam)
                    .requestBody(requestBodyBuilder().implementation(DoubanMovieData.class))
                    .response(responseBuilder().implementation(DoubanMovieData.class))
            )
            .DELETE("doubanmovies/{id}", this::deleteDoubanMovie, builder ->
                builder.operationId("DeleteDoubanMovie")
                    .tag(tag)
                    .description("删除豆瓣条目")
                    .parameter(idPathParam)
                    .response(responseBuilder().implementation(Void.class))
            )
            .POST("doubanmovies/-/synchronizationDouban", this::synchronizationDouban, builder ->
                builder.operationId("SynchronizationDouban")
                    .tag(tag)
                    .description("同步豆瓣数据")
                    .response(responseBuilder().implementation(Void.class))
            )
            .DELETE("doubanmovies/-/clear", this::clearDoubanMovies, builder ->
                builder.operationId("ClearDoubanMovies")
                    .tag(tag)
                    .description("清空全部豆瓣条目")
                    .response(responseBuilder().implementation(Void.class))
            )
            .build();
    }

    Mono<ServerResponse> listDoubanMovies(ServerRequest request) {
        DoubanMovieQuery query = new DoubanMovieQuery(request);
        return settingConfig.getBaseConfig()
            .flatMap(baseConfig -> Mono.fromCallable(() -> {
                    int page = Math.max(query.getPage(), 1);
                    int size = clampPageSize(query.getSize());
                    SqlitePage<DoubanMovieData> slice = movieStore.listPage(
                        query.toMovieQuery(), page, size, query.toSort());
                    long total = movieStore.count(query.toMovieQuery());
                    int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / (double) size);
                    List<DoubanMovieData> items = slice.items().stream()
                        .map(data -> {
                            DoubanPosterProxy.applyProxy(data, baseConfig);
                            return data;
                        })
                        .toList();
                    return buildListResponse(page, size, total, totalPages, items,
                        slice.hasNext());
                })
                .subscribeOn(Schedulers.boundedElastic()))
            .flatMap(response -> ServerResponse.ok().bodyValue(response));
    }

    Mono<ServerResponse> createDoubanMovie(ServerRequest request) {
        return request.bodyToMono(DoubanMovieData.class)
            .flatMap(body -> Mono.fromCallable(() -> {
                    if (StringUtils.isBlank(body.getId())) {
                        body.setId(UUID.randomUUID().toString());
                    }
                    if (body.getCreationTimestamp() == null) {
                        body.setCreationTimestamp(Instant.now());
                    }
                    if (movieStore.findById(body.getId()).isPresent()) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "DoubanMovie already exists: " + body.getId());
                    }
                    return movieStore.create(body);
                }).subscribeOn(Schedulers.boundedElastic()))
            .flatMap(created -> ServerResponse.ok().bodyValue(created));
    }

    Mono<ServerResponse> getDoubanMovie(ServerRequest request) {
        String id = request.pathVariable("id");
        return settingConfig.getBaseConfig()
            .flatMap(baseConfig -> Mono.fromCallable(() -> {
                    DoubanMovieData data = movieStore.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "DoubanMovie not found: " + id));
                    DoubanPosterProxy.applyProxy(data, baseConfig);
                    return data;
                }).subscribeOn(Schedulers.boundedElastic()))
            .flatMap(data -> ServerResponse.ok().bodyValue(data));
    }

    Mono<ServerResponse> updateDoubanMovie(ServerRequest request) {
        String id = request.pathVariable("id");
        return request.bodyToMono(DoubanMovieData.class)
            .flatMap(body -> settingConfig.getBaseConfig()
                .flatMap(baseConfig -> Mono.fromCallable(() -> {
                        DoubanMovieData existing = movieStore.findById(id)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "DoubanMovie not found: " + id));
                        body.setId(id);
                        if (body.getCreationTimestamp() == null) {
                            body.setCreationTimestamp(existing.getCreationTimestamp());
                        }
                        // 列表返回的是反代后的封面，写回时保留库里的原始地址
                        DoubanPosterProxy.preserveOriginalPosterIfProxied(body, existing, baseConfig);
                        return movieStore.update(body);
                    }).subscribeOn(Schedulers.boundedElastic())))
            .flatMap(updated -> ServerResponse.ok().bodyValue(updated));
    }

    Mono<ServerResponse> deleteDoubanMovie(ServerRequest request) {
        String id = request.pathVariable("id");
        return Mono.fromCallable(() -> {
                if (!movieStore.deleteById(id)) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "DoubanMovie not found: " + id);
                }
                return null;
            })
            .subscribeOn(Schedulers.boundedElastic())
            .then(ServerResponse.ok().build());
    }

    Mono<ServerResponse> synchronizationDouban(ServerRequest request) {
        return Mono.fromRunnable(doubanService::synchronizationDouban)
            .subscribeOn(Schedulers.boundedElastic())
            .then(ServerResponse.ok().build());
    }

    Mono<ServerResponse> clearDoubanMovies(ServerRequest request) {
        return Mono.fromRunnable(movieStore::deleteAll)
            .subscribeOn(Schedulers.boundedElastic())
            .then(ServerResponse.ok().build());
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.douban.moony.la/v1alpha1");
    }

    private static int clampPageSize(int size) {
        return size < 1 ? 20 : Math.min(size, 100);
    }

    private DoubanMovieListResponse buildListResponse(int page, int size, long total,
        int totalPages, List<DoubanMovieData> items, boolean hasNext) {
        DoubanMovieListResponse response = new DoubanMovieListResponse();
        response.setPage(page);
        response.setSize(size);
        response.setTotal(total);
        response.setTotalPages(totalPages);
        response.setFirst(page == 1);
        response.setLast(!hasNext);
        response.setHasNext(hasNext);
        response.setHasPrevious(page > 1);
        response.setItems(items);
        return response;
    }

    @Data
    public static class DoubanMovieListResponse {
        private int page;
        private int size;
        private long total;
        private int totalPages;
        private boolean first;
        private boolean last;
        private boolean hasNext;
        private boolean hasPrevious;
        private List<DoubanMovieData> items;
    }
}
