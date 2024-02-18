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
public class TypeDoubanIndexer implements Reconciler<Reconciler.Request> {
    private static final String FINALIZER_NAME = "type-douban-protection";

    private final ExtensionClient client;

    private final TypeIndexer typeIndexer = new TypeIndexer();


    @NonNull
    public Set<String> listPublicByTypeName(String typeName) {
        return typeIndexer.getByIndex(typeName)
            .stream()
            .collect(Collectors.toSet());
    }

    @NonNull
    public Set<String> listAllType() {
        return typeIndexer.keySet();
    }
    @Override
    public Result reconcile(Request request) {
        client.fetch(DoubanMovie.class, request.name()).ifPresent(doubanMovie -> {
            if (doubanMovie.getMetadata().getDeletionTimestamp() != null) {
                typeIndexer.delete(doubanMovie);
                removeFinalizer(request.name());
                return;
            }
            addFinalizer(doubanMovie);
            typeIndexer.update(doubanMovie);
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

    static class TypeIndexer {
        final TypeDoubanIndexer.TypeIndexer.IndexFunc<DoubanMovie> indexFunc = doubanMovie -> {
            Set<String> types = new HashSet<>();
            types.add(doubanMovie.getSpec().getType());
            return types != null ? Set.copyOf(types) : Set.of();
        };
        final SetMultimap<String, String> typesDoubanCache = HashMultimap.create();
        final Set<String> publicDoubanMovies = new HashSet<>();

        public synchronized void add(DoubanMovie doubanMovie) {
            Set<String> indexKeys = indexFunc.apply(doubanMovie);
            for (String indexKey : indexKeys) {
                typesDoubanCache.put(indexKey, getObjectKey(doubanMovie));
                publicDoubanMovies.add(getObjectKey(doubanMovie));
            }
        }

        public synchronized Set<String> getByIndex(String indexKey) {
            Set<String> doubanNames = typesDoubanCache.get(indexKey);
            return Set.copyOf(doubanNames);
        }

        public synchronized void delete(DoubanMovie doubanMovie) {
            Set<String> indexKeys = indexFunc.apply(doubanMovie);
            for (String indexKey : indexKeys) {
                typesDoubanCache.remove(indexKey, getObjectKey(doubanMovie));
                publicDoubanMovies.remove(getObjectKey(doubanMovie));
            }
        }

        public synchronized void update(DoubanMovie doubanMovie) {
            String objectKey = getObjectKey(doubanMovie);
            // find old index
            Set<String> oldIndexKeys = new HashSet<>();
            for (String indexKey : typesDoubanCache.keySet()) {
                if (typesDoubanCache.get(indexKey).contains(objectKey)) {
                    oldIndexKeys.add(indexKey);
                }
            }
            // remove old index
            for (String indexKey : oldIndexKeys) {
                typesDoubanCache.remove(indexKey, objectKey);
                publicDoubanMovies.remove(objectKey);
            }
            // add new index
            this.add(doubanMovie);
        }

        public synchronized Set<String> keySet() {
            return Set.copyOf(typesDoubanCache.keySet());
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
