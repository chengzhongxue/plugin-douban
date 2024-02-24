package la.moony.douban;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import la.moony.douban.extension.DoubanMovie;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import run.halo.app.core.extension.endpoint.SortResolver;
import run.halo.app.extension.Extension;
import run.halo.app.extension.router.IListRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Comparator.comparing;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToPredicate;

public class DoubanMovieQuery extends IListRequest.QueryListRequest {

    private final ServerWebExchange exchange;

    public DoubanMovieQuery(ServerWebExchange exchange) {
        super(exchange.getRequest().getQueryParams());
        this.exchange = exchange;
    }

    public String getKeyword() {
        return queryParams.getFirst("keyword");
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

    public String getGenre() {
        return StringUtils.defaultIfBlank(queryParams.getFirst("genre"), null);
    }

    @ArraySchema(uniqueItems = true,
        arraySchema = @Schema(name = "sort",
            description = "Sort property and direction of the list result. Supported fields: "
                + "creationTimestamp, priority"),
        schema = @Schema(description = "doubanMovie field,asc or field,desc",
            implementation = String.class,
            example = "creationTimestamp,desc"))
    public Sort getSort() {
        return SortResolver.defaultInstance.resolve(exchange);
    }

    public Predicate<DoubanMovie> toPredicate() {
        Predicate<DoubanMovie> predicate = doubanMovie -> {return true;};
        Predicate<DoubanMovie> keywordPredicate = doubanMovie -> {
            var keyword = getKeyword();
            if (StringUtils.isBlank(keyword)) {
                return true;
            }
            String keywordToSearch = keyword.trim().toLowerCase();
            return StringUtils.containsAnyIgnoreCase(doubanMovie.getSpec().getName(),
                keywordToSearch)
                || StringUtils.containsAnyIgnoreCase(doubanMovie.getSpec().getCardSubtitle(),
                keywordToSearch);
        };

        if (StringUtils.isNotEmpty(queryParams.getFirst("status"))){
            String status = getStatus();
            predicate = predicate.and(doubanMovie -> {
                if (doubanMovie.getFaves()!=null){
                    if (StringUtils.isNotEmpty(doubanMovie.getFaves().getStatus())){
                        return doubanMovie.getFaves().getStatus().equals(status);
                    }
                    return false;
                }else {
                    return false;
                }
            });
        }

        if (StringUtils.isNotEmpty(queryParams.getFirst("type"))){
            String type = getType();
            predicate = predicate.and(doubanMovie -> doubanMovie.getSpec().getType().equals(type));
        }

        if (StringUtils.isNotEmpty(queryParams.getFirst("dataType"))){
            String dataType = getDataType();
            predicate = predicate.and(doubanMovie -> doubanMovie.getSpec().getDataType().equals(dataType));
        }

        if (StringUtils.isNotEmpty(queryParams.getFirst("genre"))) {
            String genre = getGenre();
            predicate = predicate.and(doubanMovie -> {
                Set<String> genres = doubanMovie.getSpec().getGenres();
                if (CollectionUtils.isEmpty(genres)) {
                    return false;
                }
                return doubanMovie.getSpec().getGenres().contains(genre);
            });
        }

        Predicate<Extension> labelAndFieldSelectorToPredicate =
            labelAndFieldSelectorToPredicate(getLabelSelector(), getFieldSelector());
        return predicate.and(keywordPredicate).and(labelAndFieldSelectorToPredicate);
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


    public static <E extends Extension> Comparator<E> compareName(boolean asc) {
        var comparator = Comparator.<E, String>comparing(e -> e.getMetadata().getName());
        return asc ? comparator : comparator.reversed();
    }


}
