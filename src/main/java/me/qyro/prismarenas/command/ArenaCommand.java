package me.qyro.prismarenas.command;

import me.qyro.prismarenas.bootstrap.PluginServices;
import me.qyro.prismarenas.arena.Arena;
import me.qyro.prismarenas.gui.EditGui;
import me.qyro.prismarenas.manager.ArenaManager;
import me.qyro.prismarenas.manager.ConfigManager;
import me.qyro.prismarenas.manager.MessageManager;
import me.qyro.prismarenas.manager.RegenerationManager;
import me.qyro.prismarenas.manager.SelectionManager;
import me.qyro.prismarenas.util.PrismWand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class ArenaCommand implements CommandExecutor {

    private final Plugin plugin;
    private final PluginServices services;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final ArenaManager arenaManager;
    private final RegenerationManager regenerationManager;
    private final SelectionManager selectionManager;

    public ArenaCommand(Plugin plugin, PluginServices services) {
        this.plugin = plugin;
        this.services = services;
        this.configManager = services.config();
        this.messageManager = services.messages();
        this.arenaManager = services.arenas();
        this.regenerationManager = services.regeneration();
        this.selectionManager = services.selections();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "wand" -> handleWand(sender);
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "edit" -> handleEdit(sender, args);
            case "regen" -> handleRegen(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleWand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageManager.send(sender, "general.player-only");
            return;
        }
        if (!player.hasPermission("prismarenas.wand")) {
            messageManager.send(sender, "general.no-permission");
            return;
        }

        Map<Integer, ItemStack> overflow = player.getInventory()
                .addItem(PrismWand.create(plugin, messageManager));
        if (!overflow.isEmpty()) {
            for (ItemStack item : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
        messageManager.send(player, "wand.received");
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.send(sender, "general.player-only");
            return;
        }
        if (!player.hasPermission("prismarenas.create")) {
            messageManager.send(sender, "general.no-permission");
            return;
        }
        if (args.length < 2) {
            messageManager.send(sender, "arena.create.usage");
            return;
        }

        String arenaName = args[1];
        if (arenaName.isBlank()) {
            messageManager.send(sender, "arena.create.empty-name");
            return;
        }
        if (!arenaName.matches("[a-zA-Z0-9_-]+")) {
            messageManager.send(sender, "arena.create.invalid-name");
            return;
        }
        if (arenaManager.arenaExists(arenaName)) {
            messageManager.send(sender, "arena.create.exists");
            return;
        }

        SelectionManager.Selection selection = selectionManager.getSelection(player.getUniqueId());
        if (!selection.isComplete()) {
            messageManager.send(sender, "arena.create.no-selection");
            return;
        }

        messageManager.send(player, "arena.create.scanning", "arena", arenaName);
        arenaManager.createArena(arenaName, selection.getPos1(), selection.getPos2(), success -> {
            if (success) {
                messageManager.send(player, "arena.create.success", "arena", arenaName);
                selectionManager.clearSelection(player.getUniqueId());
            } else {
                messageManager.send(player, "arena.create.failed");
            }
        });
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("prismarenas.delete")) {
            messageManager.send(sender, "general.no-permission");
            return;
        }
        if (args.length < 2) {
            messageManager.send(sender, "arena.delete.usage");
            return;
        }

        String arenaName = args[1];
        Arena arena = arenaManager.getArena(arenaName);
        if (arena == null) {
            messageManager.send(sender, "arena.delete.not-found", "arena", arenaName);
            return;
        }

        arenaManager.deleteArena(arena.getName());
        messageManager.send(sender, "arena.delete.success", "arena", arena.getName());
    }

    private void handleEdit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.send(sender, "general.player-only");
            return;
        }
        if (!player.hasPermission("prismarenas.edit")) {
            messageManager.send(sender, "general.no-permission");
            return;
        }
        if (args.length < 2) {
            messageManager.send(sender, "arena.edit.usage");
            return;
        }

        Arena arena = arenaManager.getArena(args[1]);
        if (arena == null) {
            messageManager.send(sender, "arena.edit.not-found", "arena", args[1]);
            return;
        }

        if (configManager.isSoundsEnabled()) {
            player.playSound(player.getLocation(), configManager.getSoundGuiOpen(), 1.0f, 1.0f);
        }
        new EditGui(arena).open(player);
    }

    private void handleRegen(CommandSender sender, String[] args) {
        if (!sender.hasPermission("prismarenas.regen")) {
            messageManager.send(sender, "general.no-permission");
            return;
        }
        if (args.length < 2) {
            messageManager.send(sender, "arena.regen.usage");
            return;
        }

        String arenaName = args[1];
        Arena arena = arenaManager.getArena(arenaName);
        if (arena == null) {
            messageManager.send(sender, "arena.regen.not-found", "arena", arenaName);
            return;
        }
        if (arenaManager.isScanning(arena.getName())) {
            messageManager.send(sender, "arena.regen.scanning", "arena", arena.getName());
            return;
        }
        if (regenerationManager.isRegenerating(arena.getName())) {
            messageManager.send(sender, "arena.regen.already-running", "arena", arena.getName());
            return;
        }

        boolean started = regenerationManager.regenerate(arena.getName(), false);
        if (started) {
            messageManager.send(sender, "arena.regen.started", "arena", arena.getName());
            if (sender instanceof Player player && configManager.isSoundsEnabled()) {
                player.playSound(player.getLocation(), configManager.getSoundArenaRegenerated(), 1.0f, 1.0f);
            }
        } else {
            messageManager.send(sender, "arena.regen.failed", "arena", arena.getName());
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("prismarenas.reload")) {
            messageManager.send(sender, "general.no-permission");
            return;
        }

        if (services.reload()) {
            messageManager.send(sender, "general.reload-success");
        } else {
            messageManager.send(sender, "general.reload-failed");
        }
    }

    private void sendUsage(CommandSender sender) {
        messageManager.send(sender, "command.usage-header");
        messageManager.send(sender, "command.usage-wand");
        messageManager.send(sender, "command.usage-create");
        messageManager.send(sender, "command.usage-delete");
        messageManager.send(sender, "command.usage-edit");
        messageManager.send(sender, "command.usage-regen");
        messageManager.send(sender, "command.usage-reload");
    }
}
