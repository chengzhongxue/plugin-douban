package la.moony.douban.finders;

import la.moony.douban.vo.DoubanGenresVo;
import la.moony.douban.vo.DoubanMovieVo;
import la.moony.douban.vo.DoubanTypeVo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

public interface DoubanFinder {

    Flux<DoubanGenresVo> listAllGenres();

    Flux<DoubanTypeVo> listAllType();

    Mono<ListResult<DoubanMovieVo>> list(Integer page, Integer size);

    Flux<DoubanMovieVo> listByGenre(String genre);

    Mono<DoubanMovieVo> get(String doubanName);

    Mono<ListResult<DoubanMovieVo>> listByType(Integer page, Integer size, String type);

    Mono<ListResult<DoubanMovieVo>> list(Integer page, Integer size, String type, String status);

    Flux<DoubanMovieVo> listByType(String type);

    Flux<DoubanMovieVo> list(String type,String status);
}
