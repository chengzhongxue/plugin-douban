package la.moony.douban;

import la.moony.douban.extension.DoubanMovie;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import run.halo.app.core.extension.endpoint.SortResolver;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.router.SortableRequest;
import run.halo.app.extension.router.selector.FieldSelector;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import static java.util.Comparator.comparing;
import static run.halo.app.extension.index.query.QueryFactory.all;
import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.contains;
import static run.halo.app.extension.index.query.QueryFactory.equal;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToListOptions;

public class DoubanMovieQuery extends SortableRequest {

    private final MultiValueMap<String, String> queryParams;


    public DoubanMovieQuery(ServerWebExchange exchange) {
        super(exchange);
        this.queryParams = exchange.getRequest().getQueryParams();
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

    public String getGenre() {
        return StringUtils.defaultIfBlank(queryParams.getFirst("genre"), null);
    }

    public ListOptions toListOptions() {
        var listOptions =
            labelAndFieldSelectorToListOptions(getLabelSelector(), getFieldSelector());
        var query = all();
        if (StringUtils.isNotBlank(getStatus())) {
            query = and(query, equal("faves.status", getStatus()));
        }
        if (StringUtils.isNotBlank(getType())) {
            query = and(query, equal("spec.type", getType()));
        }
        if (StringUtils.isNotBlank(getDataType())) {
            query = and(query, equal("spec.dataType", getDataType()));
        }

        if (StringUtils.isNotBlank(getGenre())) {
            query = and(query, equal("spec.genres", getGenre()));
        }

        if (listOptions.getFieldSelector() != null
            && listOptions.getFieldSelector().query() != null) {
            query = and(query, listOptions.getFieldSelector().query());
        }
        if (StringUtils.isNotBlank(getKeyword())) {
            query = and(query, contains("spec.name", getKeyword()));
        }
        listOptions.setFieldSelector(FieldSelector.of(query));
        return listOptions;
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


}
