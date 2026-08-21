package la.moony.douban.sqlite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.sqlite.JDBC;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;
import run.halo.app.plugin.PluginsRootGetter;

@Slf4j
@Component
public class DoubanSqliteDatabase implements DisposableBean, StorageMaintenance {

    static final int SCHEMA_VERSION = 1;
    private static final String PLUGIN_DIR = "plugin-douban";
    private static final String DB_NAME = "plugin-douban.sqlite";
    private static final int BUSY_TIMEOUT_MILLIS = 5_000;
    private static final int MAX_BUSY_RETRIES = 2;
    private static final int MAX_BACKUPS = 2;
    private static final double VACUUM_FREE_PAGE_RATIO = 0.25d;
    private static final Duration BACKUP_INTERVAL = Duration.ofDays(1);
    private static final DateTimeFormatter FILE_TIMESTAMP =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").withZone(ZoneOffset.UTC);
    private static final Set<SQLiteErrorCode> UNAVAILABLE_CODES = Set.of(
        SQLiteErrorCode.SQLITE_CORRUPT,
        SQLiteErrorCode.SQLITE_NOTADB,
        SQLiteErrorCode.SQLITE_IOERR,
        SQLiteErrorCode.SQLITE_FULL,
        SQLiteErrorCode.SQLITE_CANTOPEN,
        SQLiteErrorCode.SQLITE_PROTOCOL,
        SQLiteErrorCode.SQLITE_READONLY
    );
    private static int liveDatabaseComponents;

    private final Path dbPath;
    private final Path backupsDir;
    private final Clock clock;
    private Connection connection;
    private volatile boolean available;
    private boolean driverLeaseHeld;

    @Autowired
    public DoubanSqliteDatabase(PluginsRootGetter pluginsRootGetter) {
        this(resolveDbPath(pluginsRootGetter), Clock.systemUTC());
    }

    public DoubanSqliteDatabase(Path dbPath) {
        this(dbPath, Clock.systemUTC());
    }

    DoubanSqliteDatabase(Path dbPath, Clock clock) {
        this.dbPath = dbPath;
        this.backupsDir = dbPath.resolveSibling("backups");
        this.clock = clock;
        try {
            acquireDriverLease();
            driverLeaseHeld = true;
            Files.createDirectories(dbPath.getParent());
            Files.createDirectories(backupsDir);
            cleanupTemporaryFiles();
            restoreOrCreateActiveDatabase();
            this.connection = openActiveDatabase(dbPath);
            this.available = true;
        } catch (Exception e) {
            closeQuietly(connection);
            connection = null;
            available = false;
            releaseDriverLease();
            log.error("[plugin-douban] SQLite storage could not start at {}", dbPath, e);
        }
    }

    public synchronized <T> T execute(SqlCallback<T> callback) {
        requireAvailable();
        int busyRetries = 0;
        while (true) {
            try {
                return callback.apply(connection);
            } catch (SQLException e) {
                if (isBusy(e) && busyRetries++ < MAX_BUSY_RETRIES) {
                    sleepBeforeRetry();
                    continue;
                }
                if (isStorageFailure(e) || isBusy(e)) {
                    available = false;
                    log.error("[plugin-douban] SQLite storage became unavailable at {}", dbPath, e);
                    throw new StorageUnavailableException(
                        "Douban storage is unavailable until the plugin restarts.", e);
                }
                throw new IllegalStateException("Douban database operation failed", e);
            }
        }
    }

    public synchronized <T> T inTransaction(SqlCallback<T> callback) {
        return execute(current -> {
            boolean autoCommit = current.getAutoCommit();
            current.setAutoCommit(false);
            try {
                T result = callback.apply(current);
                current.commit();
                current.setAutoCommit(autoCommit);
                return result;
            } catch (SQLException | RuntimeException e) {
                try {
                    current.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                try {
                    current.setAutoCommit(autoCommit);
                } catch (SQLException autoCommitError) {
                    e.addSuppressed(autoCommitError);
                }
                throw e;
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public synchronized void compactIfNeeded() {
        if (!available) {
            return;
        }
        try {
            execute(current -> {
                long pageCount = pragmaLong(current, "page_count");
                long freePages = pragmaLong(current, "freelist_count");
                if (pageCount > 0 && (double) freePages / pageCount >= VACUUM_FREE_PAGE_RATIO) {
                    try (Statement statement = current.createStatement()) {
                        statement.execute("PRAGMA wal_checkpoint(TRUNCATE)");
                        statement.execute("VACUUM");
                    }
                }
                return null;
            });
        } catch (RuntimeException e) {
            log.warn("[plugin-douban] Failed to compact SQLite database", e);
        }
    }

    @Scheduled(fixedDelay = 60 * 60 * 1000L, initialDelay = 60 * 60 * 1000L)
    public synchronized void backupIfDue() {
        if (!available || !backupIsDue()) {
            return;
        }
        createBackup();
    }

    synchronized void createBackup() {
        if (!available || isClosed() || !Files.exists(dbPath)) {
            return;
        }
        String timestamp = FILE_TIMESTAMP.format(Instant.now(clock));
        Path target = backupsDir.resolve(DB_NAME + ".bak-" + timestamp);
        Path temp = backupsDir.resolve(DB_NAME + ".bak-" + timestamp + ".tmp");
        try {
            Files.createDirectories(backupsDir);
            Files.deleteIfExists(temp);
            SQLiteConnection sqliteConnection = connection.unwrap(SQLiteConnection.class);
            int result = sqliteConnection.getDatabase().backup("main", temp.toString(), null);
            if (result != SQLiteErrorCode.SQLITE_OK.code) {
                throw new SQLException("SQLite backup failed with result code " + result);
            }
            if (!isValidStandaloneDatabase(temp)) {
                throw new SQLException("New SQLite backup failed validation: " + temp);
            }
            deleteQuietly(sidecar(temp, "-wal"));
            deleteQuietly(sidecar(temp, "-shm"));
            moveReplacing(temp, target);
            Files.setLastModifiedTime(target, FileTime.from(Instant.now(clock)));
            rotateBackups();
        } catch (Exception e) {
            log.warn("[plugin-douban] Failed to create SQLite backup at {}", target, e);
            deleteQuietly(temp);
            deleteQuietly(sidecar(temp, "-wal"));
            deleteQuietly(sidecar(temp, "-shm"));
        }
    }

    @Override
    public synchronized void destroy() {
        available = false;
        closeQuietly(connection);
        connection = null;
        releaseDriverLease();
    }

    Path dbPath() {
        return dbPath;
    }

    static Connection openActiveDatabase(Path path) throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setSynchronous(SQLiteConfig.SynchronousMode.FULL);
        config.setBusyTimeout(BUSY_TIMEOUT_MILLIS);
        Connection result = config.createConnection("jdbc:sqlite:" + path);
        try {
            createSchema(result);
            return result;
        } catch (SQLException e) {
            closeQuietly(result);
            throw e;
        }
    }

    static void createSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS douban_movies (
                  id TEXT PRIMARY KEY,
                  creation_timestamp TEXT,
                  name TEXT,
                  poster TEXT,
                  link TEXT,
                  douban_id TEXT,
                  score TEXT,
                  year TEXT,
                  type TEXT,
                  pubdate TEXT,
                  card_subtitle TEXT,
                  data_type TEXT,
                  genres TEXT,
                  faves_remark TEXT,
                  faves_create_time TEXT,
                  faves_score TEXT,
                  faves_status TEXT
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS storage_metadata (
                  key TEXT PRIMARY KEY,
                  value TEXT NOT NULL
                )
                """);
            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_douban_movies_create_time
                ON douban_movies(faves_create_time DESC, id DESC)
                """);
            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_douban_movies_type_status
                ON douban_movies(type, faves_status, faves_create_time DESC, id DESC)
                """);
            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_douban_movies_type_id
                ON douban_movies(type, douban_id, data_type)
                """);
            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_douban_movies_data_type
                ON douban_movies(data_type)
                """);
            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_douban_movies_name
                ON douban_movies(name)
                """);
            statement.execute("PRAGMA user_version = " + SCHEMA_VERSION);
        }
    }

    static boolean isValidDatabase(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        SQLiteConfig config = new SQLiteConfig();
        config.setBusyTimeout(BUSY_TIMEOUT_MILLIS);
        try (Connection candidate = config.createConnection("jdbc:sqlite:" + path);
            Statement statement = candidate.createStatement()) {
            statement.execute("PRAGMA query_only = ON");
            return hasCurrentSchemaAndPassesQuickCheck(statement);
        } catch (SQLException e) {
            return false;
        }
    }

    static boolean isValidStandaloneDatabase(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        try (Connection candidate = openStandaloneReadOnly(path);
            Statement statement = candidate.createStatement()) {
            return hasCurrentSchemaAndPassesQuickCheck(statement);
        } catch (SQLException e) {
            return false;
        }
    }

    static Connection openStandaloneReadOnly(Path path) throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.setBusyTimeout(BUSY_TIMEOUT_MILLIS);
        String fileUri = path.toAbsolutePath().normalize().toUri().toASCIIString();
        return config.createConnection("jdbc:sqlite:" + fileUri + "?mode=ro&immutable=1");
    }

    private void restoreOrCreateActiveDatabase() throws IOException, SQLException {
        if (isValidDatabase(dbPath)) {
            return;
        }
        if (Files.exists(dbPath) || Files.exists(sidecar(dbPath, "-wal"))
            || Files.exists(sidecar(dbPath, "-shm"))) {
            isolateActiveDatabase();
        }
        for (Path backup : backups()) {
            if (!isValidStandaloneDatabase(backup)) {
                continue;
            }
            Path temp = dbPath.resolveSibling(dbPath.getFileName() + ".restore.tmp");
            Files.copy(backup, temp, StandardCopyOption.REPLACE_EXISTING);
            if (!isValidStandaloneDatabase(temp)) {
                deleteQuietly(temp);
                continue;
            }
            moveReplacing(temp, dbPath);
            return;
        }
        try (Connection ignored = openActiveDatabase(dbPath)) {
            // Schema creation initializes an empty database.
        }
    }

    private void isolateActiveDatabase() throws IOException {
        String suffix = ".corrupt-" + FILE_TIMESTAMP.format(Instant.now(clock)) + "-"
            + UUID.randomUUID();
        moveIfExists(dbPath, dbPath.resolveSibling(dbPath.getFileName() + suffix));
        moveIfExists(sidecar(dbPath, "-wal"),
            dbPath.resolveSibling(dbPath.getFileName() + suffix + "-wal"));
        moveIfExists(sidecar(dbPath, "-shm"),
            dbPath.resolveSibling(dbPath.getFileName() + suffix + "-shm"));
    }

    private boolean backupIsDue() {
        List<Path> backups = validBackups();
        if (backups.isEmpty()) {
            return true;
        }
        try {
            Instant latest = Files.getLastModifiedTime(backups.get(0)).toInstant();
            return !Instant.now(clock).isBefore(latest.plus(BACKUP_INTERVAL));
        } catch (IOException e) {
            return true;
        }
    }

    private List<Path> backups() {
        String prefix = DB_NAME + ".bak-";
        try {
            if (!Files.isDirectory(backupsDir)) {
                return List.of();
            }
            try (Stream<Path> stream = Files.list(backupsDir)) {
                return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .filter(path -> !path.getFileName().toString().endsWith(".tmp"))
                    .filter(path -> !path.getFileName().toString().endsWith("-wal"))
                    .filter(path -> !path.getFileName().toString().endsWith("-shm"))
                    .sorted(Comparator.comparing((Path path) -> path.getFileName().toString())
                        .reversed())
                    .toList();
            }
        } catch (IOException e) {
            log.warn("[plugin-douban] Failed to list SQLite backups", e);
            return List.of();
        }
    }

    private void rotateBackups() {
        List<Path> all = backups();
        List<Path> valid = all.stream()
            .filter(DoubanSqliteDatabase::isValidStandaloneDatabase)
            .toList();
        all.stream()
            .filter(backup -> !valid.contains(backup))
            .forEach(DoubanSqliteDatabase::deleteQuietly);
        for (int i = MAX_BACKUPS; i < valid.size(); i++) {
            deleteQuietly(valid.get(i));
        }
    }

    private List<Path> validBackups() {
        return backups().stream()
            .filter(DoubanSqliteDatabase::isValidStandaloneDatabase)
            .toList();
    }

    private void cleanupTemporaryFiles() throws IOException {
        String dbName = dbPath.getFileName().toString();
        try (Stream<Path> stream = Files.list(dbPath.getParent())) {
            stream.filter(Files::isRegularFile)
                .filter(path -> {
                    String name = path.getFileName().toString();
                    return name.startsWith(dbName) && (name.endsWith(".tmp")
                        || name.contains(".tmp-"));
                })
                .forEach(DoubanSqliteDatabase::deleteQuietly);
        }
        if (Files.isDirectory(backupsDir)) {
            try (Stream<Path> stream = Files.list(backupsDir)) {
                stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".tmp"))
                    .forEach(DoubanSqliteDatabase::deleteQuietly);
            }
        }
    }

    private void requireAvailable() {
        if (!available || isClosed()) {
            available = false;
            throw new StorageUnavailableException(
                "Douban storage is unavailable until the plugin restarts.");
        }
    }

    private boolean isClosed() {
        try {
            return connection == null || connection.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }

    private static long pragmaLong(Connection connection, String pragma) throws SQLException {
        try (Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("PRAGMA " + pragma)) {
            return result.next() ? result.getLong(1) : 0;
        }
    }

    private static boolean hasCurrentSchemaAndPassesQuickCheck(Statement statement)
        throws SQLException {
        try (ResultSet version = statement.executeQuery("PRAGMA user_version")) {
            if (!version.next() || version.getInt(1) != SCHEMA_VERSION) {
                return false;
            }
        }
        try (ResultSet check = statement.executeQuery("PRAGMA quick_check")) {
            return check.next() && "ok".equalsIgnoreCase(check.getString(1)) && !check.next();
        }
    }

    private static boolean isBusy(SQLException error) {
        return error instanceof SQLiteException sqliteError
            && (sqliteError.getResultCode() == SQLiteErrorCode.SQLITE_BUSY
            || sqliteError.getResultCode() == SQLiteErrorCode.SQLITE_LOCKED
            || sqliteError.getResultCode() == SQLiteErrorCode.SQLITE_BUSY_TIMEOUT
            || sqliteError.getResultCode() == SQLiteErrorCode.SQLITE_BUSY_RECOVERY
            || sqliteError.getResultCode() == SQLiteErrorCode.SQLITE_BUSY_SNAPSHOT);
    }

    private static boolean isStorageFailure(SQLException error) {
        if (!(error instanceof SQLiteException sqliteError)) {
            return false;
        }
        SQLiteErrorCode code = sqliteError.getResultCode();
        return UNAVAILABLE_CODES.contains(code)
            || code.name().startsWith("SQLITE_IOERR_")
            || code.name().startsWith("SQLITE_CORRUPT_")
            || code.name().startsWith("SQLITE_CANTOPEN_")
            || code.name().startsWith("SQLITE_READONLY_");
    }

    private static void sleepBeforeRetry() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void moveIfExists(Path source, Path target) throws IOException {
        if (Files.exists(source)) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path sidecar(Path path, String suffix) {
        return path.resolveSibling(path.getFileName() + suffix);
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE);
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("[plugin-douban] Failed to delete SQLite file {}", path, e);
        }
    }

    private static void closeQuietly(Connection candidate) {
        if (candidate == null) {
            return;
        }
        try {
            candidate.close();
        } catch (SQLException e) {
            log.warn("[plugin-douban] Failed to close SQLite connection", e);
        }
    }

    private static synchronized void acquireDriverLease() throws SQLException {
        ensureSqliteDriverRegistered();
        liveDatabaseComponents++;
    }

    private void releaseDriverLease() {
        if (!driverLeaseHeld) {
            return;
        }
        driverLeaseHeld = false;
        releaseSqliteDriver();
    }

    private static synchronized void releaseSqliteDriver() {
        if (liveDatabaseComponents == 0) {
            return;
        }
        liveDatabaseComponents--;
        if (liveDatabaseComponents == 0) {
            deregisterPluginSqliteDrivers();
        }
    }

    private static void ensureSqliteDriverRegistered() throws SQLException {
        try {
            DriverManager.getDriver("jdbc:sqlite::memory:");
            return;
        } catch (SQLException ignored) {
            // Register below when service loading or a previous plugin lifecycle left none.
        }
        JDBC candidate = new JDBC();
        try {
            DriverManager.getDriver("jdbc:sqlite::memory:");
        } catch (SQLException ignored) {
            DriverManager.registerDriver(candidate);
        }
    }

    private static void deregisterPluginSqliteDrivers() {
        ClassLoader pluginClassLoader = DoubanSqliteDatabase.class.getClassLoader();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (!JDBC.class.getName().equals(driver.getClass().getName())
                || driver.getClass().getClassLoader() != pluginClassLoader) {
                continue;
            }
            try {
                DriverManager.deregisterDriver(driver);
            } catch (SQLException e) {
                log.warn("[plugin-douban] Failed to deregister SQLite JDBC driver", e);
            }
        }
    }

    private static Path resolveDbPath(PluginsRootGetter pluginsRootGetter) {
        return pluginsRootGetter.get().resolve(PLUGIN_DIR).resolve(DB_NAME);
    }

    @FunctionalInterface
    public interface SqlCallback<T> {
        T apply(Connection connection) throws SQLException;
    }
}
