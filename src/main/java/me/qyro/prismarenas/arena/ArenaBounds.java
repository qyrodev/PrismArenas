package me.qyro.prismarenas.arena;

import org.bukkit.Location;
import org.bukkit.World;

public final class ArenaBounds {

    private final String worldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final long blockCount;

    public ArenaBounds(String worldName, Location pos1, Location pos2) {
        this.worldName = worldName;
        this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        this.maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        this.sizeX = maxX - minX + 1;
        this.sizeY = maxY - minY + 1;
        this.sizeZ = maxZ - minZ + 1;
        this.blockCount = (long) sizeX * sizeY * sizeZ;
    }

    public ArenaBounds(String worldName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.worldName = worldName;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.sizeX = maxX - minX + 1;
        this.sizeY = maxY - minY + 1;
        this.sizeZ = maxZ - minZ + 1;
        this.blockCount = (long) sizeX * sizeY * sizeZ;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int getSizeZ() {
        return sizeZ;
    }

    public long getBlockCount() {
        return blockCount;
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public int toRelativeX(int worldX) {
        return worldX - minX;
    }

    public int toRelativeY(int worldY) {
        return worldY - minY;
    }

    public int toRelativeZ(int worldZ) {
        return worldZ - minZ;
    }

    public int toWorldX(int relativeX) {
        return minX + relativeX;
    }

    public int toWorldY(int relativeY) {
        return minY + relativeY;
    }

    public int toWorldZ(int relativeZ) {
        return minZ + relativeZ;
    }

    public long chunkKey(int worldX, int worldZ) {
        return (((long) worldX >> 4) << 32) | ((worldZ >> 4) & 0xFFFFFFFFL);
    }
}
