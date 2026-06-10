package me.qyro.prismarenas;

import me.qyro.prismarenas.bootstrap.PluginServices;
import me.qyro.prismarenas.command.ArenaCommand;
import me.qyro.prismarenas.command.ArenaTabCompleter;
import me.qyro.prismarenas.listener.GuiListener;
import me.qyro.prismarenas.listener.WandListener;
import me.qyro.prismarenas.storage.provider.StorageException;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class PrismArenas extends JavaPlugin {

    private PluginServices services;

    @Override
    public void onEnable() {
        try {
            services = new PluginServices(this);
        } catch (StorageException e) {
            getLogger().severe("Failed to initialize storage: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        services.loadArenas();
        services.startSchedulers();
        registerCommands();
        registerListeners();
        services.printStartupBanner();
    }

    @Override
    public void onDisable() {
        if (services != null) {
            services.shutdown();
        }
    }

    private void registerCommands() {
        PluginCommand command = getCommand("parenas");
        if (command == null) {
            getLogger().severe("Failed to register /parenas command. Check plugin.yml.");
            return;
        }
        command.setExecutor(new ArenaCommand(this, services));
        command.setTabCompleter(new ArenaTabCompleter(services.arenas()));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new WandListener(this, services.config(), services.messages(), services.selections()), this);
        getServer().getPluginManager().registerEvents(
                new GuiListener(services.config(), services.messages(), services.arenas()), this);
    }

    public PluginServices getServices() {
        return services;
    }
}
