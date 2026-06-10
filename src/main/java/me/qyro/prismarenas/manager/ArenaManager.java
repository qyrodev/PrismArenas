package me.qyro.prismarenas.manager;

import me.qyro.prismarenas.PrismArenas;
import me.qyro.prismarenas.arena.Arena;
import me.qyro.prismarenas.arena.ArenaBounds;
import me.qyro.prismarenas.snapshot.Snapshot;
import me.qyro.prismarenas.snapshot.SnapshotEngine;
import me.qyro.prismarenas.storage.StorageManager;
import me.qyro.prismarenas.storage.provider.StorageException;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ArenaManager {

    private final PrismArenas plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final StorageManager storageManager;
    private final SnapshotManager snapshotManager;
    private final SnapshotEngine snapshotEngine;
    private RegenerationManager regenerationManager;

    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();
    private final Map<String, Boolean> scanningArenas = new ConcurrentHashMap<>();

    public ArenaManager(PrismArenas plugin, ConfigManager configManager, MessageManager messageManager,
                        StorageManager storageManager, SnapshotManager snapshotManager,
                        SnapshotEngine snapshotEngine) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.storageManager = storageManager;
        this.snapshotManager = snapshotManager;
        this.snapshotEngine = snapshotEngine;
    }

    public void setRegenerationManager(RegenerationManager regenerationManager) {
        this.regenerationManager = regenerationManager;
    }

    public void loadAll() {
        arenas.clear();
        snapshotManager.clearCache();
        try {
            arenas.putAll(storageManager.arenas().loadAll());
        } catch (StorageException e) {
            plugin.getLogger().severe("Failed to load arenas: " + e.getMessage());
            return;
        }

        snapshotManager.loadAll(arenas.values());

        for (Arena arena : arenas.values()) {
            if (snapshotManager.getSnapshot(arena.getName()) == null) {
                messageManager.logInfo("console.missing-snapshot", "arena", arena.getName());
            }
        }
    }

    public Collection<Arena> getArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    public List<String> getArenaNames() {
        List<String> names = new ArrayList<>(arenas.size());
        for (Arena arena : arenas.values()) {
            names.add(arena.getName());
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public Arena getArena(String name) {
        if (name == null) {
            return null;
        }
        return arenas.get(name.toLowerCase());
    }

    public boolean arenaExists(String name) {
        return name != null && arenas.containsKey(name.toLowerCase());
    }

    public boolean isScanning(String name) {
        return scanningArenas.getOrDefault(name.toLowerCase(), false);
    }

    public void createArena(String name, Location pos1, Location pos2, Consumer<Boolean> callback) {
        String key = name.toLowerCase();
        if (arenas.containsKey(key)) {
            callback.accept(false);
            return;
        }

        World world = pos1.getWorld();
        if (world == null || !world.equals(pos2.getWorld())) {
            callback.accept(false);
            return;
        }

        ArenaBounds bounds = new ArenaBounds(world.getName(), pos1, pos2);
        Arena arena = new Arena(name, bounds, configManager.getDefaultResetInterval(), true);
        scanningArenas.put(key, true);

        snapshotEngine.scanAsync(bounds, world, snapshot -> {
            scanningArenas.remove(key);
            arenas.put(key, arena);
            snapshotManager.cacheSnapshot(name, snapshot);

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    storageManager.arenas().save(arena);
                    storageManager.arenas().flushAsync().join();
                    snapshotManager.saveSnapshotAsync(name, snapshot, () -> plugin.getServer().getScheduler()
                            .runTask(plugin, () -> {
                                configManager.debug("Arena '" + name + "' created with "
                                        + snapshot.getBlockCount() + " blocks.");
                                regenerationManager.regenerate(name, true);
                                callback.accept(true);
                            }));
                } catch (StorageException e) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(false));
                }
            });
        }, () -> {
            scanningArenas.remove(key);
            callback.accept(false);
        });
    }

    public void deleteArena(String name) {
        String key = name.toLowerCase();
        Arena arena = arenas.get(key);
        if (arena == null) {
            return;
        }

        regenerationManager.cancelTask(arena.getName());
        arenas.remove(key);
        snapshotManager.deleteSnapshot(arena.getName());

        try {
            storageManager.arenas().delete(arena.getName());
        } catch (StorageException e) {
            configManager.debug("Failed to delete arena '" + arena.getName() + "': " + e.getMessage());
        }
    }

    public void saveArenaAsync(Arena arena) {
        if (arena == null) {
            return;
        }
        storageManager.arenas().saveAsync(arena);
    }

    public void saveAllSync() {
        try {
            storageManager.arenas().saveAll(arenas.values());
        } catch (StorageException e) {
            plugin.getLogger().severe("Failed to save all arenas: " + e.getMessage());
        }
    }

    public int getArenaCount() {
        return arenas.size();
    }
}
