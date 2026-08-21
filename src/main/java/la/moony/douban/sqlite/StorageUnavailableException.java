package la.moony.douban.sqlite;

/**
 * SQLite 存储不可用；需重启插件后恢复。
 */
public class StorageUnavailableException extends RuntimeException {

    public StorageUnavailableException(String message) {
        super(message);
    }

    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
