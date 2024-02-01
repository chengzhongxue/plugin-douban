package la.moony.douban;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import la.moony.douban.extension.DoubanMovie;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GenresDoubanIndexer implements Reconciler<Reconciler.Request> {

    private static final String FINALIZER_NAME = "genres-douban-protection";

    private final ExtensionClient client;

    private final GenresIndexer genreIndexer = new GenresIndexer();


    @NonNull
    public Set<String> listPublicByGenreName(String genreName) {
        return genreIndexer.getByIndex(genreName)
            .stream()
            .filter(genreIndexer::isPublicMoment)
            .collect(Collectors.toSet());
    }
    
    @NonNull
    public Set<String> listAllGenres() {
        return genreIndexer.keySet();
    }
    @Override
    public Result reconcile(Request request) {
        client.fetch(DoubanMovie.class, request.name()).ifPresent(doubanMovie -> {
            if (doubanMovie.getMetadata().getDeletionTimestamp() != null) {
                genreIndexer.delete(doubanMovie);
                removeFinalizer(request.name());
                return;
            }
            addFinalizer(doubanMovie);
            genreIndexer.update(doubanMovie);
        });
        return Result.doNotRetry();
    }

    void addFinalizer(DoubanMovie oldDoubanMovie) {
        Set<String> oldFinalizers = oldDoubanMovie.getMetadata().getFinalizers();
        if (oldFinalizers != null && oldFinalizers.contains(FINALIZER_NAME)) {
            return;
        }
        client.fetch(DoubanMovie.class, oldDoubanMovie.getMetadata().getName()).ifPresent(doubanMovie -> {
            Set<String> finalizers = doubanMovie.getMetadata().getFinalizers();
            if (finalizers == null) {
                finalizers = new HashSet<>();
            }
            finalizers.add(FINALIZER_NAME);
            doubanMovie.getMetadata().setFinalizers(finalizers);
            client.update(doubanMovie);
        });
    }


    void removeFinalizer(String name) {
        client.fetch(DoubanMovie.class, name).ifPresent(doubanMovie -> {
            Set<String> finalizers = doubanMovie.getMetadata().getFinalizers();
            if (finalizers == null || !finalizers.contains(FINALIZER_NAME)) {
                return;
            }
            finalizers.remove(FINALIZER_NAME);
            client.update(doubanMovie);
        });
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return builder
            .extension(new DoubanMovie())
            .build();
    }

    static class GenresIndexer {
        final IndexFunc<DoubanMovie> indexFunc = doubanMovie -> {
            Set<String> genres = doubanMovie.getSpec().getGenres();
            return genres != null ? Set.copyOf(genres) : Set.of();
        };
        final SetMultimap<String, String> genresDoubanCache = HashMultimap.create();
        final Set<String> publicDoubanMovies = new HashSet<>();

        public synchronized void add(DoubanMovie doubanMovie) {
            Set<String> indexKeys = indexFunc.apply(doubanMovie);
            for (String indexKey : indexKeys) {
                genresDoubanCache.put(indexKey, getObjectKey(doubanMovie));
                publicDoubanMovies.add(getObjectKey(doubanMovie));
            }
        }

        public synchronized Set<String> getByIndex(String indexKey) {
            Set<String> doubanNames = genresDoubanCache.get(indexKey);
            return Set.copyOf(doubanNames);
        }

        public synchronized void delete(DoubanMovie doubanMovie) {
            Set<String> indexKeys = indexFunc.apply(doubanMovie);
            for (String indexKey : indexKeys) {
                genresDoubanCache.remove(indexKey, getObjectKey(doubanMovie));
                publicDoubanMovies.remove(getObjectKey(doubanMovie));
            }
        }

        public synchronized void update(DoubanMovie doubanMovie) {
            String objectKey = getObjectKey(doubanMovie);
            // find old index
            Set<String> oldIndexKeys = new HashSet<>();
            for (String indexKey : genresDoubanCache.keySet()) {
                if (genresDoubanCache.get(indexKey).contains(objectKey)) {
                    oldIndexKeys.add(indexKey);
                }
            }
            // remove old index
            for (String indexKey : oldIndexKeys) {
                genresDoubanCache.remove(indexKey, objectKey);
                publicDoubanMovies.remove(objectKey);
            }
            // add new index
            this.add(doubanMovie);
        }

        public synchronized Set<String> keySet() {
            return Set.copyOf(genresDoubanCache.keySet());
        }

        public synchronized boolean isPublicMoment(String doubanName) {
            return publicDoubanMovies.contains(doubanName);
        }


        @FunctionalInterface
        interface IndexFunc<T> {
            Set<String> apply(T obj);
        }

        private String getObjectKey(DoubanMovie obj) {
            Assert.notNull(obj, "DoubanMovie must not be null");
            Assert.notNull(obj.getMetadata(), "Object metadata must not be null");
            Assert.notNull(obj.getMetadata().getName(), "Object name must not be null");
            return obj.getMetadata().getName();
        }
    }
}
