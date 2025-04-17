package la.moony.douban;

import la.moony.douban.finders.DoubanFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.theme.TemplateNameResolver;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DoubanRouter {

    private final TemplateNameResolver templateNameResolver;

    private final ReactiveSettingFetcher settingFetcher;

    private final DoubanFinder doubanFinder;

    @Bean
    RouterFunction<ServerResponse> friendTemplateRoute() {

        return RouterFunctions.route().GET("/douban",this::handlerFunction)
            .build();
    }

    private Mono<ServerResponse> handlerFunction(ServerRequest request) {
        String type = request.queryParam("type")
            .filter(StringUtils::isNotBlank)
            .orElse(null);
        String status = request.queryParam("status")
            .filter(StringUtils::isNotBlank)
            .orElse("done");
        return  templateNameResolver.resolveTemplateNameOrDefault(request.exchange(), "douban")
            .flatMap( templateName -> ServerResponse.ok().render(templateName,
                java.util.Map.of("title",getDoubanTitle(),
                    "douban",doubanFinder.list(type,status),
                    "genres",doubanFinder.listAllGenres(type),
                    "types",doubanFinder.listAllType())));
    }

    Mono<String> getDoubanTitle() {
        return this.settingFetcher.get("base").map(
            setting -> setting.get("title").asText("豆瓣记录")).defaultIfEmpty(
            "豆瓣记录");
    }

}
