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

import static run.halo.app.extension.ExtensionUtil.notDeleting;
import static run.halo.app.extension.index.query.Queries.all;
import static run.halo.app.extension.index.query.Queries.and;
import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.extension.index.query.Queries.isNull;


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
        var query = and(isNull("faves.status").not(),equal("spec.genres", genre));
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
        var query = and(all("spec.genres"),isNull("faves.status").not());
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
        var query = and(all("spec.type"),isNull("faves.status").not());
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
        FieldSelector fieldSelector = FieldSelector.of(notDeleting());
        if (StringUtils.isNoneBlank(typeName)) {
            fieldSelector = fieldSelector.andQuery(equal("spec.type", typeName));
        }
        var pageRequest =
            PageRequestImpl.of(pageNullSafe(page), sizeNullSafe(size), defaultSort());
        return pageDoubanMovie(fieldSelector, pageRequest);
    }

    @Override
    public Mono<ListResult<DoubanMovieVo>> list(Integer page, Integer size, String typeName, String statusName) {
        FieldSelector fieldSelector = FieldSelector.of(notDeleting());
        if (StringUtils.isNoneBlank(typeName)) {
            fieldSelector = fieldSelector.andQuery(equal("spec.type", typeName));
        }
        if (StringUtils.isNoneBlank(statusName)) {
            fieldSelector = fieldSelector.andQuery(equal("faves.status", statusName));
        }
        var pageRequest =
            PageRequestImpl.of(pageNullSafe(page), sizeNullSafe(size), defaultSort());

        return pageDoubanMovie(fieldSelector, pageRequest);
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
        FieldSelector fieldSelector = FieldSelector.of(notDeleting());
        if (StringUtils.isNoneBlank(type)) {
            if (StringUtils.isNoneBlank(type)){
                fieldSelector = fieldSelector.andQuery(equal("spec.type", type));
            }else {
                fieldSelector = fieldSelector.andQuery(equal("faves.status", "done"))
                    .andQuery(equal("spec.type", type));
            }
        }
        if (StringUtils.isNoneBlank(status)) {
            fieldSelector = fieldSelector.andQuery(equal("faves.status", status));
        }

        listOptions.setFieldSelector(fieldSelector);
        return client.listAll(DoubanMovie.class, listOptions, defaultSort())
            .flatMap(this::getDoubanMovieVo);
    }

    private Mono<ListResult<DoubanMovieVo>> pageDoubanMovie(FieldSelector fieldSelector, PageRequest page) {
        var listOptions = new ListOptions();
        FieldSelector selector = FieldSelector.of(isNull("faves.status").not());
        if (fieldSelector != null && fieldSelector.query() != null) {
            selector = selector.andQuery(selector.query());
        }
        listOptions.setFieldSelector(selector);
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
