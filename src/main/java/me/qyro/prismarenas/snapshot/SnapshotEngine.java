package me.qyro.prismarenas.snapshot;

import me.qyro.prismarenas.arena.ArenaBounds;
import me.qyro.prismarenas.manager.ConfigManager;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class SnapshotEngine {

    private final Plugin plugin;
    private final ConfigManager configManager;

    public SnapshotEngine(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void scanAsync(ArenaBounds bounds, World world, Consumer<Snapshot> onComplete, Runnable onFailure) {
        new BukkitRunnable() {
            int rx = 0;
            int ry = 0;
            int rz = 0;
            final Map<String, Integer> paletteMap = new HashMap<>();
            final List<String> paletteList = new ArrayList<>();
            final List<Integer> relXList = new ArrayList<>();
            final List<Integer> relYList = new ArrayList<>();
            final List<Integer> relZList = new ArrayList<>();
            final List<Integer> paletteIndexList = new ArrayList<>();
            int processedThisTick = 0;
            final int scanBatchSize = configManager.getScanBatchSize();
            final boolean loadChunks = configManager.isPreloadChunks();

            @Override
            public void run() {
                if (!world.getName().equals(bounds.getWorldName())) {
                    cancel();
                    onFailure.run();
                    return;
                }

                processedThisTick = 0;

                while (processedThisTick < scanBatchSize) {
                    if (rx >= bounds.getSizeX()) {
                        finish();
                        return;
                    }

                    int worldX = bounds.toWorldX(rx);
                    int worldY = bounds.toWorldY(ry);
                    int worldZ = bounds.toWorldZ(rz);

                    if (loadChunks) {
                        int chunkX = worldX >> 4;
                        int chunkZ = worldZ >> 4;
                        if (!world.isChunkLoaded(chunkX, chunkZ)) {
                            world.getChunkAt(chunkX, chunkZ);
                        }
                    }

                    Block block = world.getBlockAt(worldX, worldY, worldZ);
                    Material material = block.getType();
                    BlockData blockData = block.getBlockData();
                    String entry = Snapshot.encodePaletteEntry(material, blockData);

                    int paletteIndex = paletteMap.computeIfAbsent(entry, key -> {
                        int index = paletteList.size();
                        paletteList.add(key);
                        return index;
                    });

                    relXList.add(rx);
                    relYList.add(ry);
                    relZList.add(rz);
                    paletteIndexList.add(paletteIndex);

                    processedThisTick++;
                    advanceCursor();
                }
            }

            private void advanceCursor() {
                rz++;
                if (rz >= bounds.getSizeZ()) {
                    rz = 0;
                    ry++;
                    if (ry >= bounds.getSizeY()) {
                        ry = 0;
                        rx++;
                    }
                }
            }

            private void finish() {
                cancel();
                Snapshot snapshot = Snapshot.create(
                        bounds,
                        paletteList.toArray(new String[0]),
                        toIntArray(relXList),
                        toIntArray(relYList),
                        toIntArray(relZList),
                        toIntArray(paletteIndexList)
                );
                onComplete.accept(snapshot);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public List<Integer> detectChangedBlocksChunked(Snapshot snapshot, World world, int startIndex, int batchSize) {
        ArenaBounds bounds = snapshot.getBounds();
        int count = snapshot.getBlockCount();
        List<Integer> changed = new ArrayList<>(Math.min(batchSize, Math.max(0, count - startIndex)));

        int end = Math.min(count, startIndex + batchSize);
        int[] relX = snapshot.getRelativeX();
        int[] relY = snapshot.getRelativeY();
        int[] relZ = snapshot.getRelativeZ();

        for (int i = startIndex; i < end; i++) {
            int worldX = bounds.toWorldX(relX[i]);
            int worldY = bounds.toWorldY(relY[i]);
            int worldZ = bounds.toWorldZ(relZ[i]);

            if (!world.isChunkLoaded(worldX >> 4, worldZ >> 4)) {
                changed.add(i);
                continue;
            }

            Block block = world.getBlockAt(worldX, worldY, worldZ);
            Material expectedMaterial = snapshot.getMaterial(i);
            BlockData expectedData = snapshot.getBlockData(i);

            if (block.getType() != expectedMaterial || !block.getBlockData().matches(expectedData)) {
                changed.add(i);
            }
        }

        return changed;
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
