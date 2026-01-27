package la.moony.douban;

import lombok.Data;
import reactor.core.publisher.Mono;

public interface SettingConfig {

    Mono<BaseConfig> getBaseConfig();

    @Data
    class BaseConfig {
        public static final String GROUP = "base";
        private String title;
        private String pageSize;
        private String doubanId;
        private String apiKey;
        private Boolean isProxy;
        private String proxyHost;
    }
}
