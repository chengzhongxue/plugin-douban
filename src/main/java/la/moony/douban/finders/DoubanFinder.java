package la.moony.douban.finders;

import java.util.Map;
import la.moony.douban.sqlite.entity.DoubanMovieData;
import la.moony.douban.vo.DoubanGenresVo;
import la.moony.douban.vo.DoubanTypeVo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

public interface DoubanFinder {

    Flux<DoubanGenresVo> listAllGenres(String type);

    Flux<DoubanTypeVo> listAllType();

    Mono<ListResult<DoubanMovieData>> list(Map<String, Object> params);

    Mono<ListResult<DoubanMovieData>> list(Integer page, Integer size);

    Mono<ListResult<DoubanMovieData>> list(Integer page, Integer size, String type, String status);

    Mono<ListResult<DoubanMovieData>> listByType(Integer page, Integer size, String type);

    Flux<DoubanMovieData> list(String type, String status);

    Flux<DoubanMovieData> listByGenre(String genre);

    Flux<DoubanMovieData> listByType(String type);

    Mono<DoubanMovieData> get(String doubanName);
}
