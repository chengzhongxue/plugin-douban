package la.moony.douban.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import la.moony.douban.DoubanRequest;
import la.moony.douban.vo.DoubanMovieVo;
import reactor.core.publisher.Mono;

public interface DoubanService {

    void synchronizationDouban();

    Mono<ArrayNode> listDouban(DoubanRequest request);

    Mono<DoubanMovieVo> getDoubanDetail(String url);
}
