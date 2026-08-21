package la.moony.douban;

import la.moony.douban.finders.DoubanFinder;
import la.moony.douban.sqlite.entity.DoubanMovieData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.context.LazyContextVariable;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.theme.TemplateNameResolver;
import run.halo.app.theme.router.ModelConst;
import run.halo.app.theme.router.PageUrlUtils;
import run.halo.app.theme.router.UrlContextListResult;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static run.halo.app.theme.router.PageUrlUtils.totalPage;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DoubanRouter {

    private static final Duration BLOCKING_TIMEOUT = Duration.ofSeconds(10);

    private final TemplateNameResolver templateNameResolver;

    private final ReactiveSettingFetcher settingFetcher;

    private final DoubanFinder doubanFinder;

    @Bean
    RouterFunction<ServerResponse> friendTemplateRoute() {
        return route(GET("/douban").or(GET("/douban/page/{page:\\d+}")),
                handlerFunction()
            );
    }

    HandlerFunction<ServerResponse> handlerFunction() {
        return request -> {
            String type = request.queryParam("type")
                .filter(StringUtils::isNotBlank)
                .orElse(null);
            var doubanMovies = new LazyContextVariable<UrlContextListResult<DoubanMovieData>>() {
                @Override
                protected UrlContextListResult<DoubanMovieData> loadValue() {
                    return doubanMovieList(request).block(BLOCKING_TIMEOUT);
                }
            };
            return templateNameResolver.resolveTemplateNameOrDefault(request.exchange(), "douban")
                .flatMap( templateName -> ServerResponse.ok().render(templateName,
                    java.util.Map.of("title",getDoubanTitle(),
                        "douban", doubanMovies,
                        "genres",doubanFinder.listAllGenres(type),
                        "types",doubanFinder.listAllType(),
                        ModelConst.TEMPLATE_ID, "douban")));
        };
    }


    private Mono<UrlContextListResult<DoubanMovieData>> doubanMovieList(ServerRequest request) {
        String path = request.path();
        int pageNum = pageNumInPathVariable(request);
        String type = request.queryParam("type")
            .filter(StringUtils::isNotBlank)
            .orElse(null);
        String status = request.queryParam("status")
            .filter(StringUtils::isNotBlank)
            .orElse("done");
        String dataType = request.queryParam("dataType")
            .filter(StringUtils::isNotBlank)
            .orElse(null);
        String genre = request.queryParam("genre")
            .filter(StringUtils::isNotBlank)
            .orElse(null);


        return this.settingFetcher.get("base")
            .map(item -> item.get("pageSize").asInt(10))
            .defaultIfEmpty(10)
            .flatMap(pageSize -> {
                Map<String, Object> params = new java.util.HashMap<>();
                params.put("page", pageNum);
                params.put("size", pageSize);
                params.put("type", type);
                params.put("status", status);
                params.put("dataType", dataType);
                params.put("genre", genre);
                return doubanFinder.list(params)
                    .map(list -> new UrlContextListResult.Builder<DoubanMovieData>()
                        .listResult(list)
                        .nextUrl(addQueryParams(
                            PageUrlUtils.nextPageUrl(path, totalPage(list)), type, status, dataType, genre)
                        )
                        .prevUrl(addQueryParams(PageUrlUtils.prevPageUrl(path), type, status, dataType, genre))
                        .build()
                    );
                }
            );
    }

    private static String addQueryParams(String path, String type, String status, String dataType, String genre) {
        return UriComponentsBuilder.fromPath(path)
            .queryParamIfPresent("type", Optional.ofNullable(type))
            .queryParamIfPresent("status", Optional.ofNullable(status))
            .queryParamIfPresent("dataType", Optional.ofNullable(dataType))
            .queryParamIfPresent("genre", Optional.ofNullable(genre))
            .build()
            .toString();
    }

    private int pageNumInPathVariable(ServerRequest request) {
        String page = request.pathVariables().get("page");
        return NumberUtils.toInt(page, 1);
    }

    Mono<String> getDoubanTitle() {
        return this.settingFetcher.get("base").map(
            setting -> setting.get("title").asText("豆瓣记录")).defaultIfEmpty(
            "豆瓣记录");
    }

}
