package me.qyro.prismarenas.manager;

import me.qyro.prismarenas.PrismArenas;
import me.qyro.prismarenas.util.SoundUtil;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigManager {

    private static final int INTERVAL_STEP = 60;
    private static final int DETECTION_BATCH_SIZE = 10000;
    private static final int SCAN_BATCH_SIZE = 8000;
    private static final int AUTO_REGEN_CHECK_INTERVAL = 20;

    private final PrismArenas plugin;

    private boolean debug;
    private boolean saveOnDisable;
    private boolean loadArenasOnStartup;
    private int autoSaveMinutes;
    private int defaultResetInterval;
    private int minimumResetInterval;
    private int maximumResetInterval;

    private boolean changedBlocksOnly;
    private boolean preloadChunks;
    private boolean batchProcessing;
    private int processPerTick;
    private int maxProcessPerTick;
    private boolean asyncComparison;
    private boolean logRestorationStats;

    private boolean cacheArenas;
    private boolean cacheSnapshots;
    private boolean unloadUnusedSnapshots;
    private int cacheExpireMinutes;

    private String databaseType;
    private String sqliteFile;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;
    private boolean mysqlUseSsl;
    private int mysqlPoolSize;

    private String snapshotsDirectory;
    private boolean snapshotCompression;
    private int snapshotCompressionLevel;
    private boolean snapshotAsyncSaving;

    private boolean soundsEnabled;
    private Sound soundSelection;
    private Sound soundGuiOpen;
    private Sound soundIntervalIncrease;
    private Sound soundIntervalDecrease;
    private Sound soundToggleEnable;
    private Sound soundToggleDisable;
    private Sound soundArenaRegenerated;

    private boolean showBanner;
    private boolean showLoadedArenas;
    private boolean showActiveTasks;

    private boolean developerTimings;
    private boolean verboseStorage;
    private boolean verboseRestoration;

    public ConfigManager(PrismArenas plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        debug = config.getBoolean("settings.debug", false);
        autoSaveMinutes = Math.max(0, config.getInt("settings.auto-save-minutes", 5));
        defaultResetInterval = config.getInt("settings.default-reset-interval", 300);
        minimumResetInterval = config.getInt("settings.minimum-reset-interval", 60);
        maximumResetInterval = config.getInt("settings.maximum-reset-interval", 86400);
        saveOnDisable = config.getBoolean("settings.save-on-disable", true);
        loadArenasOnStartup = config.getBoolean("settings.load-arenas-on-startup", true);

        changedBlocksOnly = config.getBoolean("restoration.changed-blocks-only", true);
        preloadChunks = config.getBoolean("restoration.preload-chunks", true);
        batchProcessing = config.getBoolean("restoration.batch-processing", true);
        maxProcessPerTick = Math.max(500, config.getInt("restoration.max-process-per-tick", 5000));
        processPerTick = clamp(config.getInt("restoration.process-per-tick", 1000), 500, maxProcessPerTick);
        asyncComparison = config.getBoolean("restoration.async-comparison", true);
        logRestorationStats = config.getBoolean("restoration.log-restoration-stats", true);

        cacheArenas = config.getBoolean("performance.cache-arenas", true);
        cacheSnapshots = config.getBoolean("performance.cache-snapshots", true);
        unloadUnusedSnapshots = config.getBoolean("performance.unload-unused-snapshots", true);
        cacheExpireMinutes = Math.max(1, config.getInt("performance.cache-expire-minutes", 15));

        databaseType = config.getString("database.type", "SQLITE").trim().toUpperCase();
        sqliteFile = config.getString("database.sqlite.file", "database.db");
        mysqlHost = config.getString("database.mysql.host", "localhost");
        mysqlPort = config.getInt("database.mysql.port", 3306);
        mysqlDatabase = config.getString("database.mysql.database", "prismarenas");
        mysqlUsername = config.getString("database.mysql.username", "root");
        mysqlPassword = config.getString("database.mysql.password", "password");
        mysqlUseSsl = config.getBoolean("database.mysql.use-ssl", false);
        mysqlPoolSize = config.getInt("database.mysql.connection-pool-size", 10);

        snapshotsDirectory = config.getString("snapshots.directory", "snapshots");
        snapshotCompression = config.getBoolean("snapshots.compression", true);
        snapshotCompressionLevel = clamp(config.getInt("snapshots.compression-level", 6), 1, 9);
        snapshotAsyncSaving = config.getBoolean("snapshots.async-saving", true);

        soundsEnabled = config.getBoolean("sounds.enabled", true);
        soundSelection = parseSound(config, "sounds.selection", Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        soundGuiOpen = parseSound(config, "sounds.gui-open", Sound.BLOCK_ENDER_CHEST_OPEN);
        soundIntervalIncrease = parseSound(config, "sounds.interval-increase", Sound.ENTITY_PLAYER_LEVELUP);
        soundIntervalDecrease = parseSound(config, "sounds.interval-decrease", Sound.BLOCK_NOTE_BLOCK_BASS);
        soundToggleEnable = parseSound(config, "sounds.toggle-enable", Sound.ENTITY_VILLAGER_YES);
        soundToggleDisable = parseSound(config, "sounds.toggle-disable", Sound.ENTITY_VILLAGER_NO);
        soundArenaRegenerated = parseSound(config, "sounds.arena-regenerated", Sound.ENTITY_PLAYER_LEVELUP);

        showBanner = config.getBoolean("startup.show-banner", true);
        showLoadedArenas = config.getBoolean("startup.show-loaded-arenas", true);
        showActiveTasks = config.getBoolean("startup.show-active-tasks", true);

        developerTimings = config.getBoolean("developer.timings", false);
        verboseStorage = config.getBoolean("developer.verbose-storage", false);
        verboseRestoration = config.getBoolean("developer.verbose-restoration", false);
    }

    public void debug(String message) {
        if (debug) {
            plugin.getLogger().info("[Debug] " + message);
        }
    }

    public void verboseStorage(String message) {
        if (verboseStorage || debug) {
            plugin.getLogger().info("[Storage] " + message);
        }
    }

    public void verboseRestoration(String message) {
        if (verboseRestoration || debug) {
            plugin.getLogger().info("[Restoration] " + message);
        }
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isSaveOnDisable() {
        return saveOnDisable;
    }

    public boolean isLoadArenasOnStartup() {
        return loadArenasOnStartup;
    }

    public int getAutoSaveMinutes() {
        return autoSaveMinutes;
    }

    public long getAutoSaveTicks() {
        return autoSaveMinutes * 60L * 20L;
    }

    public int getDefaultResetInterval() {
        return defaultResetInterval;
    }

    public int getMinimumResetInterval() {
        return minimumResetInterval;
    }

    public int getMaximumResetInterval() {
        return maximumResetInterval;
    }

    public int getIntervalStep() {
        return INTERVAL_STEP;
    }

    public boolean isChangedBlocksOnly() {
        return changedBlocksOnly;
    }

    public boolean isPreloadChunks() {
        return preloadChunks;
    }

    public boolean isBatchProcessing() {
        return batchProcessing;
    }

    public int getProcessPerTick() {
        return batchProcessing ? processPerTick : Integer.MAX_VALUE;
    }

    public int getMaxProcessPerTick() {
        return maxProcessPerTick;
    }

    public boolean isAsyncComparison() {
        return asyncComparison;
    }

    public boolean isLogRestorationStats() {
        return logRestorationStats;
    }

    public int getDetectionBatchSize() {
        return DETECTION_BATCH_SIZE;
    }

    public int getScanBatchSize() {
        return SCAN_BATCH_SIZE;
    }

    public int getAutoRegenCheckInterval() {
        return AUTO_REGEN_CHECK_INTERVAL;
    }

    public boolean isCacheArenas() {
        return cacheArenas;
    }

    public boolean isCacheSnapshots() {
        return cacheSnapshots;
    }

    public boolean isUnloadUnusedSnapshots() {
        return unloadUnusedSnapshots;
    }

    public int getCacheExpireMinutes() {
        return cacheExpireMinutes;
    }

    public long getCacheExpireMillis() {
        return cacheExpireMinutes * 60_000L;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public String getSqliteFile() {
        return sqliteFile;
    }

    public String getMysqlHost() {
        return mysqlHost;
    }

    public int getMysqlPort() {
        return mysqlPort;
    }

    public String getMysqlDatabase() {
        return mysqlDatabase;
    }

    public String getMysqlUsername() {
        return mysqlUsername;
    }

    public String getMysqlPassword() {
        return mysqlPassword;
    }

    public boolean isMysqlUseSsl() {
        return mysqlUseSsl;
    }

    public int getMysqlPoolSize() {
        return mysqlPoolSize;
    }

    public String getSnapshotsDirectory() {
        return snapshotsDirectory;
    }

    public boolean isSnapshotCompression() {
        return snapshotCompression;
    }

    public int getSnapshotCompressionLevel() {
        return snapshotCompressionLevel;
    }

    public boolean isSnapshotAsyncSaving() {
        return snapshotAsyncSaving;
    }

    public boolean isSoundsEnabled() {
        return soundsEnabled;
    }

    public Sound getSoundSelection() {
        return soundSelection;
    }

    public Sound getSoundGuiOpen() {
        return soundGuiOpen;
    }

    public Sound getSoundIntervalIncrease() {
        return soundIntervalIncrease;
    }

    public Sound getSoundIntervalDecrease() {
        return soundIntervalDecrease;
    }

    public Sound getSoundToggleEnable() {
        return soundToggleEnable;
    }

    public Sound getSoundToggleDisable() {
        return soundToggleDisable;
    }

    public Sound getSoundArenaRegenerated() {
        return soundArenaRegenerated;
    }

    public boolean isShowBanner() {
        return showBanner;
    }

    public boolean isShowLoadedArenas() {
        return showLoadedArenas;
    }

    public boolean isShowActiveTasks() {
        return showActiveTasks;
    }

    public boolean isDeveloperTimings() {
        return developerTimings;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private Sound parseSound(FileConfiguration config, String path, Sound fallback) {
        return SoundUtil.parse(config.getString(path), fallback);
    }
}
