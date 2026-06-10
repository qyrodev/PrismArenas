package me.qyro.prismarenas.storage.yaml;

import me.qyro.prismarenas.PrismArenas;
import me.qyro.prismarenas.arena.Arena;
import me.qyro.prismarenas.arena.ArenaBounds;
import me.qyro.prismarenas.manager.ConfigManager;
import me.qyro.prismarenas.storage.provider.ArenaStorage;
import me.qyro.prismarenas.storage.provider.StorageException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class YamlArenaStorage implements ArenaStorage {

    private final PrismArenas plugin;
    private final ConfigManager configManager;
    private final File arenasFile;
    private FileConfiguration arenasConfig;

    public YamlArenaStorage(PrismArenas plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
    }

    @Override
    public void initialize() throws StorageException {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new StorageException("Failed to create plugin data folder.");
        }
        if (!arenasFile.exists()) {
            plugin.saveResource("arenas.yml", false);
        }
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
    }

    @Override
    public void shutdown() {
        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save arenas.yml on shutdown", e);
        }
    }

    @Override
    public Map<String, Arena> loadAll() throws StorageException {
        Map<String, Arena> arenas = new HashMap<>();
        ConfigurationSection section = arenasConfig.getConfigurationSection("arenas");
        if (section == null) {
            return arenas;
        }

        int defaultInterval = configManager.getDefaultResetInterval();

        for (String name : section.getKeys(false)) {
            ConfigurationSection arenaSection = section.getConfigurationSection(name);
            if (arenaSection == null) {
                continue;
            }

            String world = arenaSection.getString("world");
            if (world == null) {
                plugin.getLogger().warning("Skipping arena '" + name + "': missing world.");
                continue;
            }

            int minX = arenaSection.getInt("pos1.x");
            int minY = arenaSection.getInt("pos1.y");
            int minZ = arenaSection.getInt("pos1.z");
            int maxX = arenaSection.getInt("pos2.x");
            int maxY = arenaSection.getInt("pos2.y");
            int maxZ = arenaSection.getInt("pos2.z");
            int interval = arenaSection.getInt("interval", defaultInterval);
            boolean autoRegen = arenaSection.getBoolean("auto-regen", true);
            long lastRegen = arenaSection.getLong("last-regen", System.currentTimeMillis());

            ArenaBounds bounds = new ArenaBounds(world, minX, minY, minZ, maxX, maxY, maxZ);
            Arena arena = new Arena(name, bounds, interval, autoRegen);
            arena.setLastRegenMillis(lastRegen);
            arenas.put(name.toLowerCase(), arena);
        }
        return arenas;
    }

    @Override
    public void save(Arena arena) throws StorageException {
        ConfigurationSection root = arenasConfig.getConfigurationSection("arenas");
        if (root == null) {
            root = arenasConfig.createSection("arenas");
        }

        ConfigurationSection section = root.getConfigurationSection(arena.getName());
        if (section == null) {
            section = root.createSection(arena.getName());
        }

        ArenaBounds bounds = arena.getBounds();
        section.set("world", bounds.getWorldName());
        section.set("pos1.x", bounds.getMinX());
        section.set("pos1.y", bounds.getMinY());
        section.set("pos1.z", bounds.getMinZ());
        section.set("pos2.x", bounds.getMaxX());
        section.set("pos2.y", bounds.getMaxY());
        section.set("pos2.z", bounds.getMaxZ());
        section.set("interval", arena.getIntervalSeconds());
        section.set("auto-regen", arena.isAutoRegen());
        section.set("last-regen", arena.getLastRegenMillis());
    }

    @Override
    public void saveAll(Collection<Arena> arenas) throws StorageException {
        arenasConfig.set("arenas", null);
        ConfigurationSection root = arenasConfig.createSection("arenas");
        for (Arena arena : arenas) {
            ConfigurationSection section = root.createSection(arena.getName());
            ArenaBounds bounds = arena.getBounds();
            section.set("world", bounds.getWorldName());
            section.set("pos1.x", bounds.getMinX());
            section.set("pos1.y", bounds.getMinY());
            section.set("pos1.z", bounds.getMinZ());
            section.set("pos2.x", bounds.getMaxX());
            section.set("pos2.y", bounds.getMaxY());
            section.set("pos2.z", bounds.getMaxZ());
            section.set("interval", arena.getIntervalSeconds());
            section.set("auto-regen", arena.isAutoRegen());
            section.set("last-regen", arena.getLastRegenMillis());
        }
        flushSync();
    }

    @Override
    public void delete(String arenaName) throws StorageException {
        ConfigurationSection root = arenasConfig.getConfigurationSection("arenas");
        if (root != null) {
            root.set(arenaName, null);
        }
        flushSync();
    }

    @Override
    public CompletableFuture<Void> saveAsync(Arena arena) {
        return CompletableFuture.runAsync(() -> {
            try {
                save(arena);
                flushSync();
            } catch (StorageException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save arena asynchronously: " + arena.getName(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveAllAsync(Collection<Arena> arenas) {
        return CompletableFuture.runAsync(() -> {
            try {
                saveAll(arenas);
            } catch (StorageException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save all arenas asynchronously", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> flushAsync() {
        return CompletableFuture.runAsync(this::flushSync);
    }

    private void flushSync() {
        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save arenas.yml", e);
        }
    }
}
