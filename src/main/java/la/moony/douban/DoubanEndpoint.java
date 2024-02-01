package la.moony.douban;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import la.moony.douban.extension.DoubanMovie;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.router.QueryParamBuildUtil;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

@Component
public class DoubanEndpoint implements CustomEndpoint {

    private final String doubanMovieTag = "api.plugin.halo.run/v1alpha1/DoubanMovie";

    private final ReactiveExtensionClient client;
    
    private final GenresDoubanIndexer genresDoubanIndexer;

    public DoubanEndpoint(ReactiveExtensionClient client, GenresDoubanIndexer genresDoubanIndexer) {
        this.client = client;
        this.genresDoubanIndexer = genresDoubanIndexer;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .GET("plugins/plugin-douban/doubanmovies", this::listDoubanmovie, builder -> {
                builder.operationId("listDoubanMovie")
                    .description("List doubanMovie.")
                    .tag(doubanMovieTag);
                QueryParamBuildUtil.buildParametersFromType(builder, DoubanMovieQuery.class);
            })
            .GET("plugins/plugin-douban/genres", this::ListGenres,
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
            .build();
    }

    Mono<ServerResponse> listDoubanmovie(ServerRequest request) {
        DoubanMovieQuery doubanMovieQuery = new DoubanMovieQuery(request.exchange());
        return listDoubanMovie(doubanMovieQuery)
            .flatMap(doubanMovies -> ServerResponse.ok().bodyValue(doubanMovies));
    }

    private Mono<ListResult<DoubanMovie>> listDoubanMovie(DoubanMovieQuery query) {
        return client.list(DoubanMovie.class, query.toPredicate(),
            query.toComparator(),
            query.getPage(),
            query.getSize()
        );
    }


    private Mono<ServerResponse> ListGenres(ServerRequest request) {
        String name = request.queryParam("name").orElse(null);
        return Flux.fromIterable(genresDoubanIndexer.listAllGenres())
            .filter(genreName -> StringUtils.isBlank(name) || StringUtils.containsIgnoreCase(genreName,
                name))
            .collectList()
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.plugin.halo.run/v1alpha1");
    }
}
