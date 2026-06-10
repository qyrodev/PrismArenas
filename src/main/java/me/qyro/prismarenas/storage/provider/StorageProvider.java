package me.qyro.prismarenas.storage.provider;

import org.bukkit.plugin.Plugin;

public interface StorageProvider {

    String getId();

    void initialize(Plugin plugin) throws StorageException;

    void shutdown();

    ArenaStorage arenas();

    SnapshotStorage snapshots();
}