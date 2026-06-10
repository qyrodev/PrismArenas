package me.qyro.prismarenas.bootstrap;

import me.qyro.prismarenas.PrismArenas;
import me.qyro.prismarenas.manager.ArenaManager;
import me.qyro.prismarenas.manager.ConfigManager;
import me.qyro.prismarenas.manager.MessageManager;
import me.qyro.prismarenas.manager.RegenerationManager;
import me.qyro.prismarenas.manager.SelectionManager;
import me.qyro.prismarenas.manager.SnapshotManager;
import me.qyro.prismarenas.snapshot.SnapshotEngine;
import me.qyro.prismarenas.storage.StorageManager;
import me.qyro.prismarenas.storage.provider.StorageException;

public final class PluginServices {

    private final PrismArenas plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final StorageManager storageManager;
    private final SelectionManager selectionManager;
    private final SnapshotEngine snapshotEngine;
    private final SnapshotManager snapshotManager;
    private final ArenaManager arenaManager;
    private final RegenerationManager regenerationManager;

    public PluginServices(PrismArenas plugin) throws StorageException {
        this.plugin = plugin;

        this.configManager = new ConfigManager(plugin);
        this.configManager.load();

        this.messageManager = new MessageManager(plugin);
        this.messageManager.load();

        this.storageManager = new StorageManager(plugin, configManager);
        this.storageManager.initialize();

        this.selectionManager = new SelectionManager();
        this.snapshotEngine = new SnapshotEngine(plugin, configManager);
        this.snapshotManager = new SnapshotManager(plugin, configManager, storageManager);
        this.arenaManager = new ArenaManager(plugin, configManager, messageManager, storageManager, snapshotManager, snapshotEngine);
        this.regenerationManager = new RegenerationManager(plugin, arenaManager, snapshotManager, configManager,
                messageManager, snapshotEngine);
        this.arenaManager.setRegenerationManager(regenerationManager);
    }

    public void loadArenas() {
        if (configManager.isLoadArenasOnStartup()) {
            arenaManager.loadAll();
        }
    }

    public void startSchedulers() {
        snapshotManager.startCacheEviction();
        regenerationManager.startAutoRegenScheduler();
    }

    public void shutdown() {
        regenerationManager.stopAutoRegenScheduler();
        regenerationManager.cancelAllTasks();
        snapshotManager.stopCacheEviction();

        if (configManager.isSaveOnDisable()) {
            arenaManager.saveAllSync();
        }

        storageManager.shutdown();
        messageManager.logInfo("console.disabled");
    }

    public boolean reload() {
        try {
            configManager.load();
            messageManager.reload();
            storageManager.reload();
            regenerationManager.restartAutoRegenScheduler();
            snapshotManager.stopCacheEviction();
            snapshotManager.startCacheEviction();
            return true;
        } catch (StorageException ex) {
            plugin.getLogger().severe("Failed to reload storage: " + ex.getMessage());
            return false;
        }
    }

    public void printStartupBanner() {
        if (!configManager.isShowBanner()) {
            return;
        }

        messageManager.logInfo("console.startup-header");
        messageManager.logInfo("console.startup-title", "version", plugin.getDescription().getVersion());
        messageManager.logInfo("console.startup-subtitle");
        messageManager.logInfo("console.startup-author");
        messageManager.logInfo("console.startup-platform");
        messageManager.logInfo("console.startup-status");
        messageManager.logInfo("");

        if (configManager.isShowLoadedArenas()) {
            messageManager.logInfo("console.startup-arenas", "arenas", String.valueOf(arenaManager.getArenaCount()));
        }
        if (configManager.isShowActiveTasks()) {
            messageManager.logInfo("console.startup-tasks", "tasks", String.valueOf(regenerationManager.getActiveTaskCount()));
        }

        messageManager.logInfo("console.startup-engine-header");
        messageManager.logInfo("console.startup-engine-snapshot");
        messageManager.logInfo("console.startup-engine-changed");
        messageManager.logInfo("console.startup-engine-async");
        messageManager.logInfo("console.startup-engine-batched");
        messageManager.logInfo("console.startup-ready");
        messageManager.logInfo("console.startup-footer");
    }

    public ConfigManager config() {
        return configManager;
    }

    public MessageManager messages() {
        return messageManager;
    }

    public StorageManager storage() {
        return storageManager;
    }

    public SelectionManager selections() {
        return selectionManager;
    }

    public SnapshotEngine snapshotEngine() {
        return snapshotEngine;
    }

    public SnapshotManager snapshots() {
        return snapshotManager;
    }

    public ArenaManager arenas() {
        return arenaManager;
    }

    public RegenerationManager regeneration() {
        return regenerationManager;
    }
}
