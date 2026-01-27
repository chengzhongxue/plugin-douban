package la.moony.douban.finders;

import la.moony.douban.vo.DoubanGenresVo;
import la.moony.douban.vo.DoubanMovieVo;
import la.moony.douban.vo.DoubanTypeVo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import java.util.Map;

public interface DoubanFinder {

    Flux<DoubanGenresVo> listAllGenres(String type);

    Flux<DoubanTypeVo> listAllType();

    Mono<ListResult<DoubanMovieVo>> list(Map<String, Object> params);

    Mono<ListResult<DoubanMovieVo>> list(Integer page, Integer size);

    Mono<ListResult<DoubanMovieVo>> list(Integer page, Integer size, String type, String status);

    Mono<ListResult<DoubanMovieVo>> listByType(Integer page, Integer size, String type);

    Flux<DoubanMovieVo> list(String type,String status);

    Flux<DoubanMovieVo> listByGenre(String genre);

    Flux<DoubanMovieVo> listByType(String type);

    Mono<DoubanMovieVo> get(String doubanName);

}
