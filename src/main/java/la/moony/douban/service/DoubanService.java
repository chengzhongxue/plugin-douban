package la.moony.douban.service;

import la.moony.douban.DoubanMovieQuery;
import la.moony.douban.sqlite.entity.DoubanMovieData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

public interface DoubanService {

    void synchronizationDouban();

    Mono<DoubanMovieData> getDoubanDetail(String url);

    Flux<String> listAllGenres(String type);

    Mono<ListResult<DoubanMovieData>> listDoubanMovie(DoubanMovieQuery doubanMovieQuery);
}
