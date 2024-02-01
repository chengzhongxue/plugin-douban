package la.moony.douban.finders.impl;

import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Nonnull;
import la.moony.douban.GenresDoubanIndexer;
import la.moony.douban.extension.DoubanMovie;
import la.moony.douban.finders.DoubanFinder;
import la.moony.douban.vo.DoubanGenresVo;
import la.moony.douban.vo.DoubanMovieVo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.util.comparator.Comparators;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.theme.finders.Finder;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;


@Finder("doubanFinder")
@RequiredArgsConstructor
public class DoubanFinderImpl implements DoubanFinder {

    private final ReactiveExtensionClient client;

    private final GenresDoubanIndexer genresDoubanIndexer;

    public static final Predicate<DoubanMovie> FIXED_PREDICATE = doubanMovie -> true;


    @Override
    public Mono<ListResult<DoubanMovieVo>> list(Integer page, Integer size) {
        return pageDoubanMovie(page, size, null, defaultComparator());
    }

    @Override
    public Flux<DoubanMovieVo> listBy(String genre) {
        return this.client.list(DoubanMovie.class, FIXED_PREDICATE.and(doubanMovie -> doubanMovie.getSpec()
                .getGenres().contains(genre)), defaultComparator())
            .flatMap(this::getDoubanMovieVo);
    }

    @Override
    public Mono<DoubanMovieVo> get(String doubanName) {
        return client.get(DoubanMovie.class, doubanName)
            .filter(FIXED_PREDICATE)
            .flatMap(this::getDoubanMovieVo);
    }

    @Override
    public Flux<DoubanGenresVo> listAllGenres() {
        return Flux.fromIterable(genresDoubanIndexer.listAllGenres())
            .map(genreName -> DoubanGenresVo.builder()
                .name(genreName)
                .doubanCount(genresDoubanIndexer.listPublicByGenreName(genreName).size())
                .build()
            );
    }

    @Override
    public Mono<ListResult<DoubanMovieVo>> listByGenre(Integer pageNum, Integer pageSize, String genreName) {
        return pageDoubanMovie(pageNum, pageSize,
            doubanMovie -> {
                if (StringUtils.isBlank(genreName)) {
                    return true;
                }
                Set<String> genres = doubanMovie.getSpec().getGenres();
                if (genres == null) {
                    return false;
                }
                return genres.contains(genreName);
            },
            defaultComparator()
        );
    }

    @Override
    public Mono<ListResult<DoubanMovieVo>> listByType(Integer pageNum, Integer pageSize, String typeName) {
        return pageDoubanMovie(pageNum, pageSize,
            doubanMovie -> {
                if (StringUtils.isBlank(typeName)) {
                    return true;
                }
                String type = doubanMovie.getSpec().getType();
                if (type == null) {
                    return false;
                }
                return type.equals(typeName);
            },
            defaultComparator()
        );
    }

    private Mono<ListResult<DoubanMovieVo>> pageDoubanMovie(Integer page, Integer size,
        Predicate<DoubanMovie> doubanMoviePredicate,
        Comparator<DoubanMovie> comparator) {
        Predicate<DoubanMovie> predicate = FIXED_PREDICATE
            .and(doubanMoviePredicate == null ? doubanMovie -> true : doubanMoviePredicate);
        return client.list(DoubanMovie.class, predicate, comparator,
                pageNullSafe(page), sizeNullSafe(size))
            .flatMap(list -> Flux.fromStream(list.get())
                .concatMap(this::getDoubanMovieVo)
                .collectList()
                .map(doubanMovieVos -> new ListResult<>(list.getPage(), list.getSize(),
                    list.getTotal(), doubanMovieVos)
                )
            )
            .defaultIfEmpty(new ListResult<>(page, size, 0L, List.of()));
    }


    private Comparator<DoubanMovie> defaultComparator() {
        Function<DoubanMovie, Instant> releaseTime =
            doubanMovie -> doubanMovie.getFaves().getCreateTime();
        Function<DoubanMovie, String> name = doubanMovie -> doubanMovie.getMetadata().getName();
        return Comparator.comparing(releaseTime, Comparators.nullsLow())
            .thenComparing(name)
            .reversed();
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
