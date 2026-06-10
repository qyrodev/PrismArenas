package me.qyro.prismarenas.arena;

import org.bukkit.Location;
import org.bukkit.World;

public final class Arena {

    private final String name;
    private final ArenaBounds bounds;
    private int intervalSeconds;
    private boolean autoRegen;
    private volatile long lastRegenMillis;

    public Arena(String name, ArenaBounds bounds, int intervalSeconds, boolean autoRegen) {
        this.name = name;
        this.bounds = bounds;
        this.intervalSeconds = intervalSeconds;
        this.autoRegen = autoRegen;
        this.lastRegenMillis = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public ArenaBounds getBounds() {
        return bounds;
    }

    public String getWorldName() {
        return bounds.getWorldName();
    }

    public Location getPos1(World world) {
        return new Location(world, bounds.getMinX(), bounds.getMinY(), bounds.getMinZ());
    }

    public Location getPos2(World world) {
        return new Location(world, bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public boolean isAutoRegen() {
        return autoRegen;
    }

    public void setAutoRegen(boolean autoRegen) {
        this.autoRegen = autoRegen;
    }

    public long getLastRegenMillis() {
        return lastRegenMillis;
    }

    public void setLastRegenMillis(long lastRegenMillis) {
        this.lastRegenMillis = lastRegenMillis;
    }

    public boolean isDueForRegen(long nowMillis) {
        return autoRegen && (nowMillis - lastRegenMillis) >= (intervalSeconds * 1000L);
    }
}
