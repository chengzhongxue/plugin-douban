package la.moony.douban.controller;


import la.moony.douban.extension.DoubanMovie;
import la.moony.douban.service.DoubanService;
import la.moony.douban.vo.DoubanMovieVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.ApiVersion;

@ApiVersion("v1alpha1")
@RequestMapping("/douban")
@RestController
@Slf4j
public class DoubanController {

    private final DoubanService doubanService;

    private final ReactiveExtensionClient client;

    public DoubanController(DoubanService doubanService, ReactiveExtensionClient client) {
        this.doubanService = doubanService;
        this.client = client;
    }

    @PostMapping("/synchronizationDouban")
    public void synchronizationDouban() {
        doubanService.synchronizationDouban();
    }

    @GetMapping("/getDoubanDetail")
    public Mono<DoubanMovieVo> getDoubanDetail(@RequestParam("url") String url){
        return doubanService.getDoubanDetail(url);
    }


    @DeleteMapping("/clear")
    public Mono<Void> clearLogs() {
        return client.list(DoubanMovie.class, null, null).flatMap(doubanMovie -> client.delete(doubanMovie))
            .then();
    }

}
