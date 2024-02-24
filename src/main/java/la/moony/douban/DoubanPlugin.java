package la.moony.douban;

import la.moony.douban.extension.CronDouban;
import la.moony.douban.extension.DoubanMovie;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpec;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

import java.util.Optional;
import java.util.Set;

import static run.halo.app.extension.index.IndexAttributeFactory.multiValueAttribute;
import static run.halo.app.extension.index.IndexAttributeFactory.simpleAttribute;

/**
 * @author moony
 * @url https://moony.la
 * @date 2024/2/1
 */
@Component
public class DoubanPlugin extends BasePlugin {

    private final SchemeManager schemeManager;

    public DoubanPlugin(PluginContext pluginContext, SchemeManager schemeManager) {
        super(pluginContext);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        schemeManager.register(DoubanMovie.class, indexSpecs -> {
            indexSpecs.add(new IndexSpec()
                .setName("spec.genres")
                .setIndexFunc(multiValueAttribute(DoubanMovie.class, doubanMovie -> {
                    var genres = doubanMovie.getSpec().getGenres();
                    return genres == null ? Set.of() : genres;
                }))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.type")
                .setIndexFunc(
                    simpleAttribute(DoubanMovie.class, doubanMovie -> doubanMovie.getSpec().getType())));
            indexSpecs.add(new IndexSpec()
                .setName("spec.dataType")
                .setIndexFunc(
                    simpleAttribute(DoubanMovie.class, doubanMovie -> doubanMovie.getSpec().getDataType())));
            indexSpecs.add(new IndexSpec()
                .setName("spec.name")
                .setIndexFunc(
                    simpleAttribute(DoubanMovie.class, doubanMovie -> doubanMovie.getSpec().getName())));
            indexSpecs.add(new IndexSpec()
                .setName("spec.id")
                .setIndexFunc(
                    simpleAttribute(DoubanMovie.class, doubanMovie -> doubanMovie.getSpec().getId())));
            indexSpecs.add(new IndexSpec()
                .setName("faves.status")
                .setIndexFunc(simpleAttribute(DoubanMovie.class, doubanMovie -> {
                    var status = doubanMovie.getFaves().getStatus();
                    return status == null ? null : status.toString();
                }))
            );

            indexSpecs.add(new IndexSpec()
                .setName("faves.createTime")
                .setIndexFunc(simpleAttribute(DoubanMovie.class, moment -> {
                    var createTime = moment.getFaves().getCreateTime();
                    return createTime == null ? null : createTime.toString();
                }))
            );

            indexSpecs.add(new IndexSpec()
                .setName(DoubanMovie.REQUIRE_SYNC_ON_STARTUP_INDEX_NAME)
                .setIndexFunc(simpleAttribute(DoubanMovie.class, doubanMovie -> {
                    var observedVersion = Optional.ofNullable(doubanMovie.getStatus())
                        .map(DoubanMovie.Status::getObservedVersion)
                        .orElse(-1L);
                    if (observedVersion < doubanMovie.getMetadata().getVersion()) {
                        return BooleanUtils.TRUE;
                    }
                    // don't care about the false case
                    return null;
                })));
        });
        schemeManager.register(CronDouban.class);
    }

    @Override
    public void stop() {
        schemeManager.unregister(schemeManager.get(DoubanMovie.class));
        schemeManager.unregister(schemeManager.get(CronDouban.class));
    }
}
