package la.moony.douban.reconciler;

import la.moony.douban.extension.DoubanMovie;
import la.moony.douban.sqlite.DoubanConverters;
import la.moony.douban.sqlite.SqliteDoubanMovieStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;

/**
 * 将旧 Extension {@link DoubanMovie} 迁移到 SQLite 后删除。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DoubanMovieMigrationReconciler implements Reconciler<Reconciler.Request> {

    private final ExtensionClient client;
    private final SqliteDoubanMovieStore movieStore;

    @Override
    public Result reconcile(Request request) {
        client.fetch(DoubanMovie.class, request.name()).ifPresent(doubanMovie -> {
            if (!ExtensionUtil.isDeleted(doubanMovie)) {
                var record = DoubanConverters.toMovieData(doubanMovie);
                movieStore.create(record);
                client.delete(doubanMovie);
            }
        });
        return Result.doNotRetry();
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return builder
            .extension(new DoubanMovie())
            .syncAllOnStart(true)
            .build();
    }
}
