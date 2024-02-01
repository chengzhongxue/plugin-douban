package la.moony.douban;

import la.moony.douban.extension.CronDouban;
import la.moony.douban.extension.DoubanMovie;
import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.BasePlugin;

/**
 * <p>Plugin main class to manage the lifecycle of the plugin.</p>
 * <p>This class must be public and have a public constructor.</p>
 * <p>Only one main class extending {@link BasePlugin} is allowed per plugin.</p>
 *
 * @author guqing
 * @since 1.0.0
 */
@Component
public class DoubanPlugin extends BasePlugin {

    private final SchemeManager schemeManager;

    public DoubanPlugin(PluginWrapper wrapper, SchemeManager schemeManager) {
        super(wrapper);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        schemeManager.register(DoubanMovie.class);
        schemeManager.register(CronDouban.class);
    }

    @Override
    public void stop() {
        schemeManager.unregister(schemeManager.get(DoubanMovie.class));
        schemeManager.unregister(schemeManager.get(CronDouban.class));
    }
}
