package la.moony.douban.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import la.moony.douban.DoubanMovieQuery;
import la.moony.douban.DoubanRequest;
import la.moony.douban.extension.DoubanMovie;
import la.moony.douban.vo.DoubanMovieVo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

public interface DoubanService {

    void synchronizationDouban();

    Mono<ArrayNode> listDouban(DoubanRequest request);

    Mono<DoubanMovieVo> getDoubanDetail(String url);

    Flux<String> listAllGenres();

    Mono<ListResult<DoubanMovie>> listDoubanMovie(DoubanMovieQuery doubanMovieQuery);
}
