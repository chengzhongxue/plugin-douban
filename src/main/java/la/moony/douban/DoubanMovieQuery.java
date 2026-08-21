package la.moony.douban;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import la.moony.douban.sqlite.SqliteDoubanMovieStore;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.server.ServerRequest;
import run.halo.app.core.extension.endpoint.SortResolver;
import run.halo.app.extension.router.IListRequest;
import run.halo.app.extension.router.SortableRequest;
import java.util.List;
import java.util.Optional;

import static org.springdoc.core.fn.builders.arrayschema.Builder.arraySchemaBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static run.halo.app.extension.router.QueryParamBuildUtil.sortParameter;

public class DoubanMovieQuery extends SortableRequest {

    public DoubanMovieQuery(ServerRequest request) {
        super(request.exchange());
    }

    @Nullable
    public String getKeyword() {
        return StringUtils.defaultIfBlank(queryParams.getFirst("keyword"), null);
    }

    @Nullable
    public String getStatus() {
        return queryParams.getFirst("status");
    }

    @Nullable
    public String getType() {
        return queryParams.getFirst("type");
    }

    @Nullable
    public String getDataType() {
        return queryParams.getFirst("dataType");
    }

    @Nullable
    public Optional<List<String>> getGenres() {
        return Optional.ofNullable(queryParams.get("genre"))
            .filter(genres -> !genres.isEmpty());
    }

    public SqliteDoubanMovieStore.MovieQuery toMovieQuery() {
        SqliteDoubanMovieStore.MovieQuery query = new SqliteDoubanMovieStore.MovieQuery();
        query.setKeyword(getKeyword());
        query.setStatus(getStatus());
        query.setType(getType());
        query.setDataType(getDataType());
        getGenres().ifPresent(query::setGenres);
        return query;
    }

    public Sort toSort() {
        var sort = SortResolver.defaultInstance.resolve(exchange);
        if (sort == null || sort.isUnsorted()) {
            return Sort.by(Sort.Order.desc("favesCreateTime"));
        }
        return sort.and(Sort.by(Sort.Order.desc("favesCreateTime")));
    }

    public static void buildParameters(Builder builder) {
        IListRequest.buildParameters(builder);
        builder.parameter(sortParameter())
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("keyword")
                .description("DoubanMovies filtered by keyword.")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("status")
                .description("DoubanMovies filtered by status.")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("type")
                .description("DoubanMovies filtered by type.")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("dataType")
                .description("DoubanMovies filtered by dataType.")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("genre")
                .description("DoubanMovies filtered by genre.")
                .required(false)
                .array(
                    arraySchemaBuilder()
                        .uniqueItems(true)
                        .schema(schemaBuilder()
                            .implementation(String.class))
                )
                .implementationArray(String.class));
    }
}
