package me.qyro.prismarenas.storage;

import me.qyro.prismarenas.PrismArenas;
import me.qyro.prismarenas.manager.ConfigManager;
import me.qyro.prismarenas.storage.provider.ArenaStorage;
import me.qyro.prismarenas.storage.provider.SnapshotStorage;
import me.qyro.prismarenas.storage.provider.StorageException;
import me.qyro.prismarenas.storage.provider.StorageProvider;
import me.qyro.prismarenas.storage.yaml.YamlStorageProvider;
import org.bukkit.scheduler.BukkitTask;

public final class StorageManager {

    private final PrismArenas plugin;
    private final ConfigManager configManager;
    private StorageProvider provider;
    private BukkitTask autoSaveTask;

    public StorageManager(PrismArenas plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() throws StorageException {
        shutdown();
        provider = createProvider();
        provider.initialize(plugin);
        configManager.verboseStorage("Initialized storage provider: " + provider.getId());
        startAutoSaveTask();
    }

    public void shutdown() {
        stopAutoSaveTask();
        if (provider != null) {
            provider.shutdown();
            provider = null;
        }
    }

    public void reload() throws StorageException {
        initialize();
    }

    public ArenaStorage arenas() {
        return provider.arenas();
    }

    public SnapshotStorage snapshots() {
        return provider.snapshots();
    }

    public String getProviderId() {
        return provider != null ? provider.getId() : "none";
    }

    private StorageProvider createProvider() throws StorageException {
        return switch (configManager.getDatabaseType()) {
            case "SQLITE" -> new YamlStorageProvider(plugin, configManager);
            case "MYSQL", "MARIADB" -> throw new StorageException(
                    "Database type '" + configManager.getDatabaseType()
                            + "' is not yet implemented. Use SQLITE for file-based storage.");
            default -> throw new StorageException("Unknown database type: " + configManager.getDatabaseType());
        };
    }

    private void startAutoSaveTask() {
        stopAutoSaveTask();
        long ticks = configManager.getAutoSaveTicks();
        if (ticks <= 0) {
            return;
        }
        autoSaveTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (provider != null) {
                provider.arenas().flushAsync();
            }
        }, ticks, ticks);
    }

    private void stopAutoSaveTask() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
    }
}
