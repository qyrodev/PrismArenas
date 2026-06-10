package me.qyro.prismarenas.manager;

import me.qyro.prismarenas.PrismArenas;
import me.qyro.prismarenas.arena.Arena;
import me.qyro.prismarenas.snapshot.Snapshot;
import me.qyro.prismarenas.snapshot.SnapshotEngine;
import me.qyro.prismarenas.task.RegenerationTask;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RegenerationManager {

    private final PrismArenas plugin;
    private final ArenaManager arenaManager;
    private final SnapshotManager snapshotManager;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final SnapshotEngine snapshotEngine;

    private final Map<String, RegenerationTask> activeTasks = new ConcurrentHashMap<>();
    private BukkitTask autoRegenTask;

    public RegenerationManager(PrismArenas plugin, ArenaManager arenaManager, SnapshotManager snapshotManager,
                               ConfigManager configManager, MessageManager messageManager,
                               SnapshotEngine snapshotEngine) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.snapshotManager = snapshotManager;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.snapshotEngine = snapshotEngine;
    }

    public void startAutoRegenScheduler() {
        restartAutoRegenScheduler();
    }

    public void restartAutoRegenScheduler() {
        stopAutoRegenScheduler();
        long interval = configManager.getAutoRegenCheckInterval();
        autoRegenTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Arena arena : arenaManager.getArenas()) {
                if (arena.isDueForRegen(now) && !isRegenerating(arena.getName())) {
                    regenerate(arena.getName(), true);
                }
            }
        }, interval, interval);
    }

    public void stopAutoRegenScheduler() {
        if (autoRegenTask != null) {
            autoRegenTask.cancel();
            autoRegenTask = null;
        }
    }

    public void cancelAllTasks() {
        for (RegenerationTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
    }

    public void cancelTask(String arenaName) {
        RegenerationTask task = activeTasks.remove(arenaName.toLowerCase());
        if (task != null) {
            task.cancel();
        }
    }

    public int getActiveTaskCount() {
        return activeTasks.size();
    }

    public boolean isRegenerating(String arenaName) {
        return activeTasks.containsKey(arenaName.toLowerCase());
    }

    public boolean regenerate(String arenaName, boolean automatic) {
        Arena arena = arenaManager.getArena(arenaName);
        if (arena == null) {
            return false;
        }

        String key = arena.getName().toLowerCase();
        if (activeTasks.containsKey(key)) {
            return false;
        }

        Snapshot snapshot = snapshotManager.getSnapshot(arena.getName());
        if (snapshot == null) {
            messageManager.logInfo("console.regen-no-snapshot", "arena", arena.getName());
            return false;
        }

        World world = plugin.getServer().getWorld(arena.getWorldName());
        if (world == null) {
            messageManager.logInfo("console.regen-no-world", "arena", arena.getName());
            return false;
        }

        RegenerationTask task = new RegenerationTask(
                plugin,
                arena,
                snapshot,
                snapshotEngine,
                configManager,
                stats -> onRegenComplete(stats, automatic),
                () -> activeTasks.remove(key)
        );

        activeTasks.put(key, task);
        task.runTaskTimer(plugin, 1L, 1L);
        configManager.debug("Started regeneration for arena '" + arena.getName()
                + "' (automatic=" + automatic + ")");
        return true;
    }

    private void onRegenComplete(RegenerationTask.Statistics stats, boolean automatic) {
        activeTasks.remove(stats.getArenaName().toLowerCase());
        Arena arena = arenaManager.getArena(stats.getArenaName());
        if (arena != null) {
            arenaManager.saveArenaAsync(arena);
        }

        if (!configManager.isLogRestorationStats()) {
            return;
        }

        messageManager.logInfo("console.regen-complete-header");
        messageManager.logInfo("console.regen-complete-title");
        messageManager.logInfo("console.regen-complete-arena", "arena", stats.getArenaName());
        messageManager.logInfo("console.regen-complete-changed", "changed", String.valueOf(stats.getBlocksChanged()));
        messageManager.logInfo("console.regen-complete-restored", "restored", String.valueOf(stats.getBlocksRestored()));
        messageManager.logInfo("console.regen-complete-duration", "duration", String.valueOf(stats.getDurationMillis()));
        messageManager.logInfo("console.regen-complete-automatic", "automatic", String.valueOf(automatic));
        messageManager.logInfo("console.regen-complete-footer");
    }
}
