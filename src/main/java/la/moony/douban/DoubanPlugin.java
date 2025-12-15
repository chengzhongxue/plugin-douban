package la.moony.douban;

import la.moony.douban.extension.CronDouban;
import la.moony.douban.extension.DoubanMovie;
import org.springframework.stereotype.Component;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpecs;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;



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
            indexSpecs.add(IndexSpecs.<DoubanMovie, String>multi("spec.genres", String.class)
                .indexFunc(
                    doubanMovie -> Optional.ofNullable(doubanMovie.getSpec())
                        .map(DoubanMovie.DoubanMovieSpec::getGenres)
                        .map(Set::copyOf)
                        .orElse(Set.of())
                )
            );
            indexSpecs.add(IndexSpecs.<DoubanMovie, String>single("spec.type", String.class)
                .indexFunc(
                    doubanMovie -> Optional.ofNullable(doubanMovie.getSpec())
                        .map(DoubanMovie.DoubanMovieSpec::getType)
                        .orElse(null)
                )
            );
            indexSpecs.add(IndexSpecs.<DoubanMovie, String>single("spec.dataType", String.class)
                .indexFunc(
                    doubanMovie -> Optional.ofNullable(doubanMovie.getSpec())
                        .map(DoubanMovie.DoubanMovieSpec::getDataType)
                        .orElse(null)
                )
            );
            indexSpecs.add(IndexSpecs.<DoubanMovie, String>single("spec.name", String.class)
                .indexFunc(
                    doubanMovie -> Optional.ofNullable(doubanMovie.getSpec())
                        .map(DoubanMovie.DoubanMovieSpec::getName)
                        .orElse(null)
                )
            );
            indexSpecs.add(IndexSpecs.<DoubanMovie, String>single("spec.id", String.class)
                .indexFunc(
                    doubanMovie -> Optional.ofNullable(doubanMovie.getSpec())
                        .map(DoubanMovie.DoubanMovieSpec::getId)
                        .orElse(null)
                )
            );
            indexSpecs.add(IndexSpecs.<DoubanMovie, String>single("faves.status", String.class)
                .indexFunc(
                    doubanMovie -> Optional.ofNullable(doubanMovie.getFaves())
                        .map(DoubanMovie.DoubanMovieFaves::getStatus)
                        .orElse(null)
                )
            );
            indexSpecs.add(IndexSpecs.<DoubanMovie, Instant>single("faves.createTime", Instant.class)
                .indexFunc(
                    doubanMovie -> Optional.ofNullable(doubanMovie.getFaves())
                        .map(DoubanMovie.DoubanMovieFaves::getCreateTime)
                        .orElse(null)
                )
            );
        });
        schemeManager.register(CronDouban.class);
    }

    @Override
    public void stop() {
        schemeManager.unregister(Scheme.buildFromType(DoubanMovie.class));
        schemeManager.unregister(Scheme.buildFromType(CronDouban.class));
    }
}
