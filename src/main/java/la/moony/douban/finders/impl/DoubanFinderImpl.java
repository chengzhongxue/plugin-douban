package la.moony.douban.finders.impl;

import java.util.List;
import java.util.Map;
import la.moony.douban.DoubanPosterProxy;
import la.moony.douban.SettingConfig;
import la.moony.douban.finders.DoubanFinder;
import la.moony.douban.sqlite.SqliteDoubanMovieStore;
import la.moony.douban.sqlite.SqlitePage;
import la.moony.douban.sqlite.entity.DoubanMovieData;
import la.moony.douban.vo.DoubanGenresVo;
import la.moony.douban.vo.DoubanTypeVo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.halo.app.extension.ListResult;
import run.halo.app.theme.finders.Finder;

@Finder("doubanFinder")
@RequiredArgsConstructor
public class DoubanFinderImpl implements DoubanFinder {

    private final SqliteDoubanMovieStore movieStore;
    private final SettingConfig settingConfig;

    @Override
    public Mono<ListResult<DoubanMovieData>> list(Integer page, Integer size) {
        return pageDoubanMovie(new SqliteDoubanMovieStore.MovieQuery(), pageNullSafe(page),
            sizeNullSafe(size));
    }

    @Override
    public Flux<DoubanMovieData> listByGenre(String genre) {
        SqliteDoubanMovieStore.MovieQuery query = new SqliteDoubanMovieStore.MovieQuery();
        query.setGenre(genre);
        query.setRequireStatus(true);
        return listAll(query);
    }

    @Override
    public Mono<DoubanMovieData> get(String doubanName) {
        return settingConfig.getBaseConfig()
            .flatMap(baseConfig -> Mono.fromCallable(() -> movieStore.findById(doubanName))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> optional
                    .map(data -> {
                        DoubanPosterProxy.applyProxy(data, baseConfig);
                        return Mono.just(data);
                    })
                    .orElseGet(Mono::empty)));
    }

    static Sort defaultSort() {
        return Sort.by(Sort.Order.desc("favesCreateTime"));
    }

    @Override
    public Flux<DoubanGenresVo> listAllGenres(String type) {
        return Mono.fromCallable(() -> movieStore.listGenreCounts(type, true))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(Flux::fromIterable)
            .map(count -> DoubanGenresVo.builder()
                .name(count.name())
                .doubanCount(count.count())
                .build());
    }

    @Override
    public Flux<DoubanTypeVo> listAllType() {
        return Mono.fromCallable(() -> movieStore.listTypeCounts(true))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(Flux::fromIterable)
            .map(count -> DoubanTypeVo.builder()
                .name(getTypeName(count.key()))
                .key(count.key())
                .doubanCount(count.count())
                .build());
    }

    public String getTypeName(String type) {
        return switch (type) {
            case "movie" -> "电影";
            case "book" -> "图书";
            case "music" -> "音乐";
            case "game" -> "游戏";
            case "drama" -> "舞台剧";
            default -> "";
        };
    }

    @Override
    public Mono<ListResult<DoubanMovieData>> listByType(Integer page, Integer size,
        String typeName) {
        SqliteDoubanMovieStore.MovieQuery query = new SqliteDoubanMovieStore.MovieQuery();
        if (StringUtils.isNotBlank(typeName)) {
            query.setType(typeName);
        }
        query.setRequireStatus(true);
        return pageDoubanMovie(query, pageNullSafe(page), sizeNullSafe(size));
    }

    @Override
    public Mono<ListResult<DoubanMovieData>> list(Integer page, Integer size, String typeName,
        String statusName) {
        SqliteDoubanMovieStore.MovieQuery query = new SqliteDoubanMovieStore.MovieQuery();
        if (StringUtils.isNotBlank(typeName)) {
            query.setType(typeName);
        }
        if (StringUtils.isNotBlank(statusName)) {
            query.setStatus(statusName);
        } else {
            query.setRequireStatus(true);
        }
        return pageDoubanMovie(query, pageNullSafe(page), sizeNullSafe(size));
    }

    @Override
    public Flux<DoubanMovieData> listByType(String type) {
        SqliteDoubanMovieStore.MovieQuery query = new SqliteDoubanMovieStore.MovieQuery();
        query.setType(type);
        query.setStatus("done");
        return listAll(query);
    }

    @Override
    public Flux<DoubanMovieData> list(String type, String status) {
        SqliteDoubanMovieStore.MovieQuery query = new SqliteDoubanMovieStore.MovieQuery();
        if (StringUtils.isNotEmpty(type)) {
            query.setType(type);
        }
        if (StringUtils.isNotEmpty(status)) {
            query.setStatus(status);
        }
        return listAll(query);
    }

    @Override
    public Mono<ListResult<DoubanMovieData>> list(Map<String, Object> params) {
        SqliteDoubanMovieStore.MovieQuery query = new SqliteDoubanMovieStore.MovieQuery();
        query.setRequireStatus(true);
        Integer page = 1;
        Integer size = 10;
        if (params != null) {
            Object type = params.get("type");
            Object dataType = params.get("dataType");
            Object status = params.get("status");
            Object genre = params.get("genre");
            Object pageObj = params.get("page");
            Object sizeObj = params.get("size");
            if (type instanceof String typeValue && StringUtils.isNotBlank(typeValue)) {
                query.setType(typeValue);
            }
            if (dataType instanceof String dataTypeValue && StringUtils.isNotBlank(dataTypeValue)) {
                query.setDataType(dataTypeValue);
            }
            if (status instanceof String statusValue && StringUtils.isNotBlank(statusValue)) {
                query.setStatus(statusValue);
                query.setRequireStatus(false);
            }
            if (genre instanceof String genreValue && StringUtils.isNotBlank(genreValue)) {
                query.setGenre(genreValue);
            }
            if (pageObj instanceof Number number) {
                page = number.intValue();
            }
            if (sizeObj instanceof Number number) {
                size = number.intValue();
            }
        }
        return pageDoubanMovie(query, pageNullSafe(page), sizeNullSafe(size));
    }

    private Flux<DoubanMovieData> listAll(SqliteDoubanMovieStore.MovieQuery query) {
        return settingConfig.getBaseConfig()
            .flatMapMany(baseConfig -> Mono.fromCallable(
                    () -> movieStore.listAll(query, defaultSort()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .map(data -> {
                    DoubanPosterProxy.applyProxy(data, baseConfig);
                    return data;
                }));
    }

    private Mono<ListResult<DoubanMovieData>> pageDoubanMovie(
        SqliteDoubanMovieStore.MovieQuery query, int page, int size) {
        if (StringUtils.isBlank(query.getStatus())) {
            query.setRequireStatus(true);
        }
        return settingConfig.getBaseConfig()
            .flatMap(base -> Mono.fromCallable(() -> {
                    SqlitePage<DoubanMovieData> slice =
                        movieStore.listPage(query, page, size, defaultSort());
                    long total = movieStore.count(query);
                    List<DoubanMovieData> items = slice.items().stream()
                        .map(data -> {
                            DoubanPosterProxy.applyProxy(data, base);
                            return data;
                        })
                        .toList();
                    return new ListResult<>(page, size, total, items);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .defaultIfEmpty(new ListResult<>(page, size, 0L, List.of())));
    }

    static int pageNullSafe(Integer page) {
        return ObjectUtils.defaultIfNull(page, 1);
    }

    static int sizeNullSafe(Integer size) {
        return ObjectUtils.defaultIfNull(size, 10);
    }
}
