package la.moony.douban;

import la.moony.douban.extension.CronDouban;
import la.moony.douban.extension.DoubanMovie;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

/**
 * 仍注册 Scheme，供迁移 Reconciler 扫描旧 Extension；业务数据读写 SQLite。
 *
 * @author moony
 * @url https://kunkunyu.com
 * @date 2024/2/1
 */
@Component
@EnableScheduling
public class DoubanPlugin extends BasePlugin {

    private final SchemeManager schemeManager;

    public DoubanPlugin(PluginContext pluginContext, SchemeManager schemeManager) {
        super(pluginContext);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        // 仅用于迁移旧 Extension，新数据走 SQLite
        schemeManager.register(DoubanMovie.class);
        schemeManager.register(CronDouban.class);
    }

    @Override
    public void stop() {
        schemeManager.unregister(Scheme.buildFromType(DoubanMovie.class));
        schemeManager.unregister(Scheme.buildFromType(CronDouban.class));
    }
}
