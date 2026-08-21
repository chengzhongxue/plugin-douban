package la.moony.douban.sqlite;

public interface StorageMaintenance {

    boolean isAvailable();

    void compactIfNeeded();
}
