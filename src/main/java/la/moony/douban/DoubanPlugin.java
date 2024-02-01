package la.moony.douban;

import la.moony.douban.extension.CronDouban;
import la.moony.douban.extension.DoubanMovie;
import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.BasePlugin;

/**
 * @author moony
 * @url https://moony.la
 * @date 2024/2/1
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
