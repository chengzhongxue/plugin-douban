package la.moony.douban.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import la.moony.douban.DoubanRequest;
import reactor.core.publisher.Mono;

public interface DoubanService {

    void synchronizationDouban();

    Mono<ArrayNode> listDouban(DoubanRequest request);
}
