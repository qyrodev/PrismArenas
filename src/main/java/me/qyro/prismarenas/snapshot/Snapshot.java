package me.qyro.prismarenas.snapshot;

import me.qyro.prismarenas.arena.ArenaBounds;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public final class Snapshot {

    private final ArenaBounds bounds;
    private final String[] palette;
    private final int[] relativeX;
    private final int[] relativeY;
    private final int[] relativeZ;
    private final int[] paletteIndex;
    private final Material[] cachedMaterials;
    private final BlockData[] cachedBlockData;

    private Snapshot(ArenaBounds bounds, String[] palette, int[] relativeX, int[] relativeY,
                     int[] relativeZ, int[] paletteIndex, Material[] cachedMaterials,
                     BlockData[] cachedBlockData) {
        this.bounds = bounds;
        this.palette = palette;
        this.relativeX = relativeX;
        this.relativeY = relativeY;
        this.relativeZ = relativeZ;
        this.paletteIndex = paletteIndex;
        this.cachedMaterials = cachedMaterials;
        this.cachedBlockData = cachedBlockData;
    }

    public static Snapshot create(ArenaBounds bounds, String[] palette, int[] relativeX, int[] relativeY,
                                  int[] relativeZ, int[] paletteIndex) {
        Material[] materials = new Material[palette.length];
        BlockData[] blockData = new BlockData[palette.length];
        for (int i = 0; i < palette.length; i++) {
            ParsedPaletteEntry entry = parsePaletteEntry(palette[i]);
            materials[i] = entry.material();
            blockData[i] = entry.blockData();
        }
        return new Snapshot(bounds, palette, relativeX, relativeY, relativeZ, paletteIndex, materials, blockData);
    }

    public ArenaBounds getBounds() {
        return bounds;
    }

    public String[] getPalette() {
        return palette;
    }

    public int[] getRelativeX() {
        return relativeX;
    }

    public int[] getRelativeY() {
        return relativeY;
    }

    public int[] getRelativeZ() {
        return relativeZ;
    }

    public int[] getPaletteIndex() {
        return paletteIndex;
    }

    public int getBlockCount() {
        return paletteIndex.length;
    }

    public Material getMaterial(int blockIndex) {
        return cachedMaterials[paletteIndex[blockIndex]];
    }

    public BlockData getBlockData(int blockIndex) {
        return cachedBlockData[paletteIndex[blockIndex]];
    }

    public static String encodePaletteEntry(Material material, BlockData data) {
        return material.name() + '|' + data.getAsString();
    }

    public long estimateMemoryBytes() {
        long bytes = (long) relativeX.length * 16L;
        bytes += (long) paletteIndex.length * 4L;
        bytes += (long) cachedMaterials.length * 16L;
        bytes += (long) cachedBlockData.length * 32L;
        for (String entry : palette) {
            bytes += entry.length() * 2L;
        }
        return bytes;
    }

    private static ParsedPaletteEntry parsePaletteEntry(String entry) {
        int separator = entry.indexOf('|');
        if (separator < 0) {
            Material material = Material.matchMaterial(entry);
            if (material == null) {
                material = Material.AIR;
            }
            return new ParsedPaletteEntry(material, material.createBlockData());
        }
        String materialName = entry.substring(0, separator);
        String data = entry.substring(separator + 1);
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.AIR;
        }
        return new ParsedPaletteEntry(material, material.createBlockData(data));
    }

    private record ParsedPaletteEntry(Material material, BlockData blockData) {
    }
}
