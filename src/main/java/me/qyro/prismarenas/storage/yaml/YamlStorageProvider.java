package me.qyro.prismarenas.storage.yaml;

import me.qyro.prismarenas.PrismArenas;
import me.qyro.prismarenas.manager.ConfigManager;
import me.qyro.prismarenas.storage.provider.StorageException;
import me.qyro.prismarenas.storage.provider.StorageProvider;
import org.bukkit.plugin.Plugin;

public final class YamlStorageProvider implements StorageProvider {

    public static final String ID = "yaml";

    private final YamlArenaStorage arenaStorage;
    private final YamlSnapshotStorage snapshotStorage;

    public YamlStorageProvider(PrismArenas plugin, ConfigManager configManager) {
        this.arenaStorage = new YamlArenaStorage(plugin, configManager);
        this.snapshotStorage = new YamlSnapshotStorage(plugin, configManager);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void initialize(Plugin plugin) throws StorageException {
        arenaStorage.initialize();
        snapshotStorage.initialize();
    }

    @Override
    public void shutdown() {
        arenaStorage.shutdown();
        snapshotStorage.shutdown();
    }

    @Override
    public YamlArenaStorage arenas() {
        return arenaStorage;
    }

    @Override
    public YamlSnapshotStorage snapshots() {
        return snapshotStorage;
    }
}
