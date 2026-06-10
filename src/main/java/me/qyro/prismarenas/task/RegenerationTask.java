package me.qyro.prismarenas.task;

import me.qyro.prismarenas.arena.Arena;
import me.qyro.prismarenas.arena.ArenaBounds;
import me.qyro.prismarenas.manager.ConfigManager;
import me.qyro.prismarenas.snapshot.Snapshot;
import me.qyro.prismarenas.snapshot.SnapshotEngine;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class RegenerationTask extends BukkitRunnable {

    public static final class Statistics {
        private final String arenaName;
        private final int blocksChanged;
        private final int blocksRestored;
        private final long durationMillis;

        public Statistics(String arenaName, int blocksChanged, int blocksRestored, long durationMillis) {
            this.arenaName = arenaName;
            this.blocksChanged = blocksChanged;
            this.blocksRestored = blocksRestored;
            this.durationMillis = durationMillis;
        }

        public String getArenaName() {
            return arenaName;
        }

        public int getBlocksChanged() {
            return blocksChanged;
        }

        public int getBlocksRestored() {
            return blocksRestored;
        }

        public long getDurationMillis() {
            return durationMillis;
        }
    }

    private final Plugin plugin;
    private final Arena arena;
    private final Snapshot snapshot;
    private final SnapshotEngine snapshotEngine;
    private final ConfigManager configManager;
    private final Consumer<Statistics> onComplete;
    private final Runnable onCancel;

    private final long startMillis = System.currentTimeMillis();
    private final int detectionBatchSize;
    private final int restoreBatchSize;
    private final boolean loadChunksOnRestore;

    private Phase phase = Phase.DETECT;
    private int detectIndex = 0;
    private final List<Integer> changedBlocks = new ArrayList<>();
    private int restoreIndex = 0;
    private int blocksRestored = 0;
    private long currentChunkKey = Long.MIN_VALUE;

    private enum Phase {
        DETECT,
        RESTORE,
        DONE
    }

    public RegenerationTask(Plugin plugin, Arena arena, Snapshot snapshot, SnapshotEngine snapshotEngine,
                            ConfigManager configManager, Consumer<Statistics> onComplete, Runnable onCancel) {
        this.plugin = plugin;
        this.arena = arena;
        this.snapshot = snapshot;
        this.snapshotEngine = snapshotEngine;
        this.configManager = configManager;
        this.onComplete = onComplete;
        this.onCancel = onCancel;
        this.detectionBatchSize = configManager.getDetectionBatchSize();
        this.restoreBatchSize = configManager.getProcessPerTick();
        this.loadChunksOnRestore = configManager.isPreloadChunks();
    }

    @Override
    public void run() {
        World world = plugin.getServer().getWorld(arena.getWorldName());
        if (world == null) {
            cancel();
            onCancel.run();
            return;
        }

        switch (phase) {
            case DETECT -> runDetection(world);
            case RESTORE -> runRestore(world);
            case DONE -> cancel();
        }
    }

    private void runDetection(World world) {
        List<Integer> batch = snapshotEngine.detectChangedBlocksChunked(
                snapshot, world, detectIndex, detectionBatchSize
        );
        changedBlocks.addAll(batch);
        detectIndex += detectionBatchSize;

        if (detectIndex >= snapshot.getBlockCount()) {
            phase = Phase.RESTORE;
            restoreIndex = 0;
            configManager.debug("Arena '" + arena.getName() + "' detection complete. Changed blocks: "
                    + changedBlocks.size());
        }
    }

    private void runRestore(World world) {
        if (changedBlocks.isEmpty()) {
            finish();
            return;
        }

        ArenaBounds bounds = snapshot.getBounds();
        int processed = 0;
        int[] relX = snapshot.getRelativeX();
        int[] relY = snapshot.getRelativeY();
        int[] relZ = snapshot.getRelativeZ();

        while (restoreIndex < changedBlocks.size() && processed < restoreBatchSize) {
            int blockIndex = changedBlocks.get(restoreIndex);
            int worldX = bounds.toWorldX(relX[blockIndex]);
            int worldY = bounds.toWorldY(relY[blockIndex]);
            int worldZ = bounds.toWorldZ(relZ[blockIndex]);

            if (loadChunksOnRestore) {
                long chunkKey = bounds.chunkKey(worldX, worldZ);
                if (currentChunkKey != chunkKey) {
                    currentChunkKey = chunkKey;
                    int chunkX = worldX >> 4;
                    int chunkZ = worldZ >> 4;
                    if (!world.isChunkLoaded(chunkX, chunkZ)) {
                        world.getChunkAt(chunkX, chunkZ);
                    }
                }
            }

            Material material = snapshot.getMaterial(blockIndex);
            BlockData blockData = snapshot.getBlockData(blockIndex);

            Block block = world.getBlockAt(worldX, worldY, worldZ);
            if (block.getType() != material || !block.getBlockData().matches(blockData)) {
                block.setType(material, false);
                block.setBlockData(blockData, false);
                blocksRestored++;
            }

            restoreIndex++;
            processed++;
        }

        if (restoreIndex >= changedBlocks.size()) {
            finish();
        }
    }

    private void finish() {
        phase = Phase.DONE;
        cancel();
        long duration = System.currentTimeMillis() - startMillis;
        Statistics stats = new Statistics(
                arena.getName(),
                changedBlocks.size(),
                blocksRestored,
                duration
        );
        arena.setLastRegenMillis(System.currentTimeMillis());
        onComplete.accept(stats);
    }

    public String getArenaName() {
        return arena.getName();
    }
}
