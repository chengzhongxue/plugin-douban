package la.moony.douban;

import static run.halo.app.extension.index.query.QueryFactory.equal;

import java.util.Set;
import la.moony.douban.extension.DoubanMovie;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import run.halo.app.core.extension.notification.Subscription;
import run.halo.app.extension.DefaultExtensionMatcher;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import run.halo.app.extension.router.selector.FieldSelector;
import run.halo.app.notification.NotificationCenter;


@Component
@RequiredArgsConstructor
public class DoubanMovieReconciler implements Reconciler<Reconciler.Request> {

    private static final String FINALIZER = "douban-protection";
    private final ExtensionClient client;

    @Override
    public Result reconcile(Request request) {
        client.fetch(DoubanMovie.class, request.name()).ifPresent(doubanMovie -> {
            var status = doubanMovie.getStatus();
            if (status == null) {
                status = new DoubanMovie.Status();
                doubanMovie.setStatus(status);
            }
            status.setObservedVersion(doubanMovie.getMetadata().getVersion() + 1);
            client.update(doubanMovie);
        });
        return Result.doNotRetry();
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        final var doubanMovie = new DoubanMovie();
        return builder
            .extension(doubanMovie)
            .workerCount(5)
            .onAddMatcher(DefaultExtensionMatcher.builder(client, doubanMovie.groupVersionKind())
                .fieldSelector(
                    FieldSelector.of(equal(DoubanMovie.REQUIRE_SYNC_ON_STARTUP_INDEX_NAME, "true"))
                )
                .build()
            )
            .build();
    }
}
