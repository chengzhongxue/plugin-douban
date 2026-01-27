package la.moony.douban;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

@Component
@RequiredArgsConstructor
public class SettingConfigImpl implements SettingConfig {

    private final ReactiveSettingFetcher settingFetcher;

    @Override
    public Mono<BaseConfig> getBaseConfig() {
        return settingFetcher.fetch(BaseConfig.GROUP, BaseConfig.class)
            .defaultIfEmpty(new BaseConfig());
    }
}
