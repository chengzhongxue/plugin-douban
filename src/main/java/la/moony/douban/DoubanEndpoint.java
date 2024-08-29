package la.moony.douban;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import la.moony.douban.extension.DoubanMovie;
import la.moony.douban.service.DoubanService;
import la.moony.douban.vo.DoubanMovieVo;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

@Component
public class DoubanEndpoint implements CustomEndpoint {

    private final String doubanMovieTag = "api.douban.moony.la/v1alpha1/DoubanMovie";

    private final DoubanService doubanService;


    public DoubanEndpoint(DoubanService doubanService) {
        this.doubanService = doubanService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .GET("doubanmovies", this::listDoubanmovie, builder -> {
                builder.operationId("listDoubanMovie")
                    .description("List doubanMovie.")
                    .tag(doubanMovieTag)
                    .response(
                        responseBuilder()
                            .implementation(ListResult.generateGenericClass(DoubanMovie.class))
                    );
                DoubanMovieQuery.buildParameters(builder);
            })
            .GET("doubanmovies/-/genres", this::ListGenres,
                builder -> builder.operationId("ListGenres")
                    .description("List all douban genres.")
                    .tag(doubanMovieTag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.QUERY)
                        .description("Genres name to query")
                        .required(false)
                        .implementation(String.class)
                    )
                    .response(responseBuilder()
                        .implementationArray(String.class)
                    ))
            .GET("doubanmovies/-/getDoubanDetail", this::getDoubanDetail,
                builder -> builder.operationId("getDoubanDetail")
                    .description("getDoubanDetail.")
                    .tag(doubanMovieTag)
                    .parameter(parameterBuilder()
                        .name("url")
                        .in(ParameterIn.QUERY)
                        .description("doubanmovie url to query")
                        .required(false)
                        .implementation(String.class)
                    )
                    .response(responseBuilder()
                        .implementationArray(DoubanMovieVo.class)
                    ))
            .build();
    }

    Mono<ServerResponse> listDoubanmovie(ServerRequest request) {
        DoubanMovieQuery query = new DoubanMovieQuery(request);
        return doubanService.listDoubanMovie(query)
            .flatMap(doubanMovies -> ServerResponse.ok().bodyValue(doubanMovies));
    }

    private Mono<ServerResponse> ListGenres(ServerRequest request) {
        String name = request.queryParam("name").orElse(null);
        return doubanService.listAllGenres()
            .filter(genreName -> StringUtils.isBlank(name) || StringUtils.containsIgnoreCase(genreName,
                name))
            .collectList()
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private Mono<ServerResponse> getDoubanDetail(ServerRequest request) {
        String url = request.queryParam("url").orElse(null);
        return doubanService.getDoubanDetail(url)
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }


    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.douban.moony.la/v1alpha1");
    }
}
