package la.moony.douban;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import la.moony.douban.extension.DoubanMovie;
import la.moony.douban.finders.DoubanFinder;
import la.moony.douban.service.DoubanService;
import la.moony.douban.vo.DoubanMovieVo;
import la.moony.douban.vo.DoubanTypeVo;
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

    private final DoubanFinder doubanFinder;


    public DoubanEndpoint(DoubanService doubanService, DoubanFinder doubanFinder) {
        this.doubanService = doubanService;
        this.doubanFinder = doubanFinder;
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
            .GET("doubanmovies/-/types", this::ListTypes,
                builder -> builder.operationId("ListTypes")
                    .description("List all douban types.")
                    .tag(doubanMovieTag)
                    .response(responseBuilder()
                        .implementationArray(DoubanTypeVo.class)
                    ))
            .GET("doubanmovies/-/genres", this::ListGenres,
                builder -> builder.operationId("ListGenres")
                    .description("List all douban genres.")
                    .tag(doubanMovieTag)
                    .parameter(parameterBuilder()
                        .name("type")
                        .in(ParameterIn.QUERY)
                        .description("Genres type to query")
                        .required(false)
                        .implementation(String.class)
                    )
                    .response(responseBuilder()
                        .implementation(String.class)
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
                        .implementation(DoubanMovieVo.class)
                    ))
            .build();
    }

    Mono<ServerResponse> listDoubanmovie(ServerRequest request) {
        DoubanMovieQuery query = new DoubanMovieQuery(request);
        return doubanService.listDoubanMovie(query)
            .flatMap(doubanMovies -> ServerResponse.ok().bodyValue(doubanMovies));
    }

    private Mono<ServerResponse> ListTypes(ServerRequest request) {
        return doubanFinder.listAllType()
            .collectList()
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private Mono<ServerResponse> ListGenres(ServerRequest request) {
        String type = request.queryParam("type").orElse(null);
        return doubanService.listAllGenres(type)
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
