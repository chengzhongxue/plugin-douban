package la.moony.douban.finders.impl;

import jakarta.annotation.Nonnull;
import la.moony.douban.extension.DoubanMovie;
import la.moony.douban.finders.DoubanFinder;
import la.moony.douban.vo.DoubanGenresVo;
import la.moony.douban.vo.DoubanMovieVo;
import la.moony.douban.vo.DoubanTypeVo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.router.selector.FieldSelector;
import run.halo.app.theme.finders.Finder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import static run.halo.app.extension.index.query.QueryFactory.all;
import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.equal;
import static run.halo.app.extension.index.query.QueryFactory.isNotNull;


@Finder("doubanFinder")
@RequiredArgsConstructor
public class DoubanFinderImpl implements DoubanFinder {

    private final ReactiveExtensionClient client;

    public static final Predicate<DoubanMovie> FIXED_PREDICATE = doubanMovie -> true;


    @Override
    public Mono<ListResult<DoubanMovieVo>> list(Integer page, Integer size) {
        var pageRequest = PageRequestImpl.of(pageNullSafe(page), sizeNullSafe(size), defaultSort());
        return pageDoubanMovie(null, pageRequest);
    }

    @Override
    public Flux<DoubanMovieVo> listByGenre(String genre) {
        var listOptions = new ListOptions();
        var query = and(isNotNull("faves.status"),equal("spec.genres", genre));
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listAll(DoubanMovie.class, listOptions, defaultSort())
            .flatMap(this::getDoubanMovieVo);
    }

    @Override
    public Mono<DoubanMovieVo> get(String doubanName) {
        return client.get(DoubanMovie.class, doubanName)
            .filter(FIXED_PREDICATE)
            .flatMap(this::getDoubanMovieVo);
    }

    static Sort defaultSort() {
        return Sort.by("faves.createTime").descending();
    }

    @Override
    public Flux<DoubanGenresVo> listAllGenres(String type) {
        var listOptions = new ListOptions();
        var query = and(all("spec.genres"),isNotNull("faves.status"));
        FieldSelector fieldSelector = FieldSelector.of(query);
        if (StringUtils.isNotEmpty(type)) {
            fieldSelector =  fieldSelector.andQuery(equal("spec.type", type));
        }
        listOptions.setFieldSelector(fieldSelector);
        return client.listAll(DoubanMovie.class, listOptions, defaultSort())
            .flatMapIterable(doubanMovie -> {
                var genres = doubanMovie.getSpec().getGenres();
                if (genres == null) {
                    return List.of();
                }
                return genres.stream()
                    .map(genre -> new DoubanMoviePair(genre, doubanMovie.getMetadata().getName()))
                    .toList();
            })
            .groupBy(DoubanMoviePair::genresName)
            .flatMap(groupedFlux -> groupedFlux.count()
                .defaultIfEmpty(0L)
                .map(count -> DoubanGenresVo.builder()
                    .name(groupedFlux.key())
                    .doubanCount(count.intValue())
                    .build()
                )
            );
    }

    record DoubanMoviePair(String genresName, String doubanMovieName) {
    }

    @Override
    public Flux<DoubanTypeVo> listAllType() {
        var listOptions = new ListOptions();
        var query = and(all("spec.type"),isNotNull("faves.status"));
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listAll(DoubanMovie.class, listOptions, defaultSort())
            .flatMapIterable(doubanMovie -> {
                var type = doubanMovie.getSpec().getType();
                Set<String> types = new HashSet<>();
                if (type == null) {
                    return List.of();
                }else {
                    types.add(doubanMovie.getSpec().getType());
                }
                return types.stream()
                    .map(typeName -> new DoubanMoviePair(typeName, doubanMovie.getMetadata().getName()))
                    .toList();
            })
            .groupBy(DoubanMoviePair::genresName)
            .flatMap(groupedFlux -> groupedFlux.count()
                .defaultIfEmpty(0L)
                .map(count -> DoubanTypeVo.builder()
                    .name(getTypeName(groupedFlux.key()))
                    .key(groupedFlux.key())
                    .doubanCount(count.intValue())
                    .build()
                )
            );
    }

    public String getTypeName(String type){
        String name = "";
        switch (type){
            case "movie":
                name = "电影";
                break;
            case "book":
                name = "图书";
                break;
            case "music":
                name = "音乐";
                break;
            case "game":
                name = "游戏";
                break;
            case "drama":
                name = "舞台剧";
                break;
        }
        return name;
    }

    @Override
    public Mono<ListResult<DoubanMovieVo>> listByType(Integer page, Integer size, String typeName) {
        var query = all();
        if (org.apache.commons.lang3.StringUtils.isNoneBlank(typeName)) {
            query = and(query, equal("spec.type", typeName));
        }
        var pageRequest =
            PageRequestImpl.of(pageNullSafe(page), sizeNullSafe(size), defaultSort());
        return pageDoubanMovie(FieldSelector.of(query), pageRequest);
    }

    @Override
    public Mono<ListResult<DoubanMovieVo>> list(Integer page, Integer size, String typeName, String statusName) {
        var query = all();
        if (org.apache.commons.lang3.StringUtils.isNoneBlank(typeName)) {
            query = and(query, equal("spec.type", typeName));
        }
        if (org.apache.commons.lang3.StringUtils.isNoneBlank(statusName)) {
            query = and(query, equal("faves.status", statusName));
        }
        var pageRequest =
            PageRequestImpl.of(pageNullSafe(page), sizeNullSafe(size), defaultSort());

        return pageDoubanMovie(FieldSelector.of(query), pageRequest);
    }


    @Override
    public Flux<DoubanMovieVo> listByType(String type) {

        var listOptions = new ListOptions();
        var query = and(equal("faves.status", "done"),equal("spec.type", type));
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listAll(DoubanMovie.class, listOptions, defaultSort())
            .flatMap(this::getDoubanMovieVo);
    }

    @Override
    public Flux<DoubanMovieVo> list(String type, String status) {
        var listOptions = new ListOptions();
        var query = all();
        if (org.apache.commons.lang3.StringUtils.isNoneBlank(type)) {
            if (org.apache.commons.lang3.StringUtils.isNoneBlank(type)){
                query = and(query,equal("spec.type", type));
            }else {
                query = and(equal("faves.status", "done"),equal("spec.type", type));
            }
        }
        if (org.apache.commons.lang3.StringUtils.isNoneBlank(status)) {
            query = and(query, equal("faves.status", status));
        }

        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listAll(DoubanMovie.class, listOptions, defaultSort())
            .flatMap(this::getDoubanMovieVo);
    }

    private Mono<ListResult<DoubanMovieVo>> pageDoubanMovie(FieldSelector fieldSelector, PageRequest page) {
        var listOptions = new ListOptions();
        var query = isNotNull("faves.status");
        if (fieldSelector != null && fieldSelector.query() != null) {
            query = and(query, fieldSelector.query());
        }
        listOptions.setFieldSelector(FieldSelector.of(query));
        return client.listBy(DoubanMovie.class, listOptions, page)
            .flatMap(list -> Flux.fromStream(list.get())
                .concatMap(this::getDoubanMovieVo)
                .collectList()
                .map(doubanMovieVos -> new ListResult<>(list.getPage(), list.getSize(),
                    list.getTotal(), doubanMovieVos)
                )
            )
            .defaultIfEmpty(
                new ListResult<>(page.getPageNumber(), page.getPageSize(), 0L, List.of()));
    }

    private Mono<DoubanMovieVo> getDoubanMovieVo(@Nonnull DoubanMovie doubanMovie) {
        DoubanMovieVo doubanMovieVo = DoubanMovieVo.from(doubanMovie);
        return Mono.just(doubanMovieVo);
    }

    int pageNullSafe(Integer page) {
        return ObjectUtils.defaultIfNull(page, 1);
    }

    int sizeNullSafe(Integer size) {
        return ObjectUtils.defaultIfNull(size, 10);
    }


}
