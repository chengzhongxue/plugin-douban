package la.moony.douban.finders.impl;

import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Nonnull;
import la.moony.douban.GenresDoubanIndexer;
import la.moony.douban.TypeDoubanIndexer;
import la.moony.douban.extension.DoubanMovie;
import la.moony.douban.finders.DoubanFinder;
import la.moony.douban.vo.DoubanGenresVo;
import la.moony.douban.vo.DoubanMovieVo;
import la.moony.douban.vo.DoubanTypeVo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.lang.Nullable;
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

    private final TypeDoubanIndexer typeDoubanIndexer;

    public static final Predicate<DoubanMovie> FIXED_PREDICATE = doubanMovie -> true;


    @Override
    public Mono<ListResult<DoubanMovieVo>> list(Integer page, Integer size) {
        return pageDoubanMovie(page, size, null, defaultComparator());
    }

    @Override
    public Flux<DoubanMovieVo> listByGenre(String genre) {
        return doubanMovieList(FIXED_PREDICATE.and(doubanMovie -> doubanMovie.getSpec()
            .getGenres().contains(genre)))
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
    public Flux<DoubanTypeVo> listAllType() {
        return Flux.fromIterable(typeDoubanIndexer.listAllType())
            .map(typeName -> DoubanTypeVo.builder()
                .name(getTypeName(typeName))
                .key(typeName)
                .doubanCount(typeDoubanIndexer.listPublicByTypeName(typeName).size())
                .build()
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
        return pageDoubanMovie(page, size,
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

    @Override
    public Mono<ListResult<DoubanMovieVo>> list(Integer page, Integer size, String typeName, String statusName) {
        return pageDoubanMovie(page, size,
            FIXED_PREDICATE.and(doubanMovie -> {
                if (StringUtils.isBlank(typeName)) {
                    return true;
                }
                String type = doubanMovie.getSpec().getType();
                if (type == null) {
                    return false;
                }
                return type.equals(typeName);
            }).and(doubanMovie -> {
                if (StringUtils.isBlank(statusName)) {
                    return true;
                }
                String status = doubanMovie.getFaves().getStatus();
                if (status == null) {
                    return false;
                }
                return status.equals(statusName);
            }),
            defaultComparator()
        );
    }


    @Override
    public Flux<DoubanMovieVo> listByType(String type) {
        return doubanMovieList(FIXED_PREDICATE
            .and(doubanMovie -> doubanMovie.getFaves().getStatus().equals("done"))
            .and(doubanMovie -> {
                if (StringUtils.isBlank(type)){
                    return true;
                }else {
                    if (doubanMovie.getSpec().getType().equals(type)){
                        return true;
                    }
                    return false;
                }
            })).flatMap(this::getDoubanMovieVo);
    }

    @Override
    public Flux<DoubanMovieVo> list(String type, String status) {
        return doubanMovieList(FIXED_PREDICATE
        .and(doubanMovie -> doubanMovie.getFaves().getStatus().equals("done"))
        .and(doubanMovie -> {
            if (StringUtils.isBlank(type)){
                return true;
            }else {
                if (doubanMovie.getSpec().getType().equals(type)){
                    return true;
                }
                return false;
            }
        }).and(doubanMovie -> {
            if (StringUtils.isBlank(status)){
                return true;
            }else {
                if (doubanMovie.getFaves().getStatus().equals(status)){
                    return true;
                }
                return false;
            }
        })).flatMap(this::getDoubanMovieVo);
    }

    Flux<DoubanMovie> doubanMovieList(@Nullable Predicate<DoubanMovie> predicate) {
        return client.list(DoubanMovie.class, predicate, defaultComparator());
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
