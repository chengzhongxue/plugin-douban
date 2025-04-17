package la.moony.douban;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import la.moony.douban.extension.DoubanMovie;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.server.ServerRequest;
import run.halo.app.core.extension.endpoint.SortResolver;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.router.IListRequest;
import run.halo.app.extension.router.SortableRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparing;
import static org.springdoc.core.fn.builders.arrayschema.Builder.arraySchemaBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static run.halo.app.extension.index.query.QueryFactory.contains;
import static run.halo.app.extension.index.query.QueryFactory.equal;
import static run.halo.app.extension.index.query.QueryFactory.in;
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

    public ListOptions toListOptions() {
        var builder = ListOptions.builder(super.toListOptions());

        Optional.ofNullable(getKeyword())
            .filter(StringUtils::isNotBlank)
            .ifPresent(keyword -> builder.andQuery(
                contains("spec.name", getKeyword()))
            );

        Optional.ofNullable(getStatus())
            .filter(StringUtils::isNotBlank)
            .ifPresent(status -> builder.andQuery(equal("faves.status", status)));

        Optional.ofNullable(getType())
            .filter(StringUtils::isNotBlank)
            .ifPresent(type -> builder.andQuery(equal("spec.type", type)));

        Optional.ofNullable(getDataType())
            .filter(StringUtils::isNotBlank)
            .ifPresent(dataType -> builder.andQuery(equal("spec.dataType", dataType)));


        getGenres().ifPresent(genres -> builder.andQuery(in("spec.genres", genres)));


        return builder.build();
    }

    public Comparator<DoubanMovie> toComparator() {
        List<Comparator<DoubanMovie>> comparators = new ArrayList<>();
        var sort = getSort();
        var ctOrder = sort.getOrderFor("createTime");
        if (ctOrder != null) {
            Comparator<DoubanMovie> comparator =
                comparing(doubanMovie -> doubanMovie.getFaves().getCreateTime());
            if (ctOrder.isDescending()) {
                comparator = comparator.reversed();
            }
            comparators.add(comparator);
        }
        Comparator<DoubanMovie> comparator =
            comparing(doubanMovie -> doubanMovie.getFaves().getCreateTime());
        comparators.add(comparator.reversed());
        return comparators.stream()
            .reduce(Comparator::thenComparing)
            .orElse(null);
    }


    public Sort getSort() {
        var sort = SortResolver.defaultInstance.resolve(exchange);
        return sort.and(Sort.by("faves.createTime").descending());
    }

    public PageRequest toPageRequest() {
        return PageRequestImpl.of(getPage(), getSize(), getSort());
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
