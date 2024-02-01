package la.moony.douban.finders;

import la.moony.douban.vo.DoubanGenresVo;
import la.moony.douban.vo.DoubanMovieVo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

public interface DoubanFinder {

    Flux<DoubanGenresVo> listAllGenres();

    Mono<ListResult<DoubanMovieVo>> list(Integer page, Integer size);

    Flux<DoubanMovieVo> listBy(String genre);

    Mono<DoubanMovieVo> get(String doubanName);

    Mono<ListResult<DoubanMovieVo>> listByGenre(Integer pageNum, Integer pageSize, String genreName);

    Mono<ListResult<DoubanMovieVo>> listByType(Integer page, Integer size,String type);
}
