package me.qyro.prismarenas.manager;

import me.qyro.prismarenas.PrismArenas;
import me.qyro.prismarenas.arena.Arena;
import me.qyro.prismarenas.snapshot.Snapshot;
import me.qyro.prismarenas.storage.StorageManager;
import me.qyro.prismarenas.storage.provider.StorageException;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SnapshotManager {

    private final PrismArenas plugin;
    private final ConfigManager configManager;
    private final StorageManager storageManager;
    private final Map<String, Snapshot> cache = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAccess = new ConcurrentHashMap<>();
    private BukkitTask expireTask;

    public SnapshotManager(PrismArenas plugin, ConfigManager configManager, StorageManager storageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.storageManager = storageManager;
    }

    public void startCacheEviction() {
        stopCacheEviction();
        if (!configManager.isCacheSnapshots() || !configManager.isUnloadUnusedSnapshots()) {
            return;
        }
        expireTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::evictExpired, 1200L, 1200L);
    }

    public void stopCacheEviction() {
        if (expireTask != null) {
            expireTask.cancel();
            expireTask = null;
        }
    }

    public void loadAll(Iterable<Arena> arenas) {
        cache.clear();
        lastAccess.clear();
        if (!configManager.isCacheSnapshots()) {
            return;
        }
        for (Arena arena : arenas) {
            loadIntoCache(arena.getName());
        }
    }

    public Snapshot getSnapshot(String arenaName) {
        if (arenaName == null) {
            return null;
        }
        String key = arenaName.toLowerCase();

        if (configManager.isCacheSnapshots()) {
            Snapshot cached = cache.get(key);
            if (cached != null) {
                lastAccess.put(key, System.currentTimeMillis());
                return cached;
            }
            loadIntoCache(arenaName);
            Snapshot loaded = cache.get(key);
            if (loaded != null) {
                lastAccess.put(key, System.currentTimeMillis());
            }
            return loaded;
        }

        return loadDirect(arenaName);
    }

    public void cacheSnapshot(String arenaName, Snapshot snapshot) {
        if (!configManager.isCacheSnapshots()) {
            return;
        }
        String key = arenaName.toLowerCase();
        cache.put(key, snapshot);
        lastAccess.put(key, System.currentTimeMillis());
    }

    public void removeSnapshot(String arenaName) {
        String key = arenaName.toLowerCase();
        cache.remove(key);
        lastAccess.remove(key);
    }

    public void loadIntoCache(String arenaName) {
        if (!configManager.isCacheSnapshots()) {
            return;
        }
        Snapshot snapshot = loadDirect(arenaName);
        if (snapshot != null) {
            String key = arenaName.toLowerCase();
            cache.put(key, snapshot);
            lastAccess.put(key, System.currentTimeMillis());
            configManager.debug("Cached snapshot for '" + arenaName + "' ("
                    + snapshot.getBlockCount() + " blocks, ~"
                    + snapshot.estimateMemoryBytes() / 1024 + " KB)");
        }
    }

    private Snapshot loadDirect(String arenaName) {
        try {
            return storageManager.snapshots().load(arenaName);
        } catch (StorageException e) {
            configManager.debug("Failed to load snapshot for '" + arenaName + "': " + e.getMessage());
            return null;
        }
    }

    public void saveSnapshotAsync(String arenaName, Snapshot snapshot, Runnable onComplete) {
        if (configManager.isCacheSnapshots()) {
            cacheSnapshot(arenaName, snapshot);
        }

        Runnable complete = () -> {
            if (onComplete != null) {
                plugin.getServer().getScheduler().runTask(plugin, onComplete);
            }
        };

        storageManager.snapshots().saveAsync(arenaName, snapshot).thenRun(complete);
    }

    public void deleteSnapshot(String arenaName) {
        removeSnapshot(arenaName);
        try {
            storageManager.snapshots().delete(arenaName);
        } catch (StorageException e) {
            configManager.debug("Failed to delete snapshot for '" + arenaName + "': " + e.getMessage());
        }
    }

    public void clearCache() {
        cache.clear();
        lastAccess.clear();
    }

    public int getCachedCount() {
        return cache.size();
    }

    private void evictExpired() {
        long expireBefore = System.currentTimeMillis() - configManager.getCacheExpireMillis();
        for (Map.Entry<String, Long> entry : lastAccess.entrySet()) {
            if (entry.getValue() < expireBefore) {
                cache.remove(entry.getKey());
                lastAccess.remove(entry.getKey());
                configManager.debug("Evicted inactive snapshot from cache: " + entry.getKey());
            }
        }
    }
}
