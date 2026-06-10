package me.qyro.prismarenas.listener;

import me.qyro.prismarenas.arena.Arena;
import me.qyro.prismarenas.gui.EditGui;
import me.qyro.prismarenas.manager.ArenaManager;
import me.qyro.prismarenas.manager.ConfigManager;
import me.qyro.prismarenas.manager.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public final class GuiListener implements Listener {

    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final ArenaManager arenaManager;

    public GuiListener(ConfigManager configManager, MessageManager messageManager, ArenaManager arenaManager) {
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.arenaManager = arenaManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof EditGui editGui)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= EditGui.SIZE) {
            return;
        }

        Arena arena = arenaManager.getArena(editGui.getArena().getName());
        if (arena == null) {
            player.closeInventory();
            messageManager.send(player, "gui.arena-not-found");
            return;
        }

        if (slot == EditGui.CLOCK_SLOT) {
            handleClockClick(player, arena, editGui, event.getClick());
        } else if (slot == EditGui.PAPER_SLOT) {
            handleRegenToggle(player, arena, editGui);
        }
    }

    private void handleClockClick(Player player, Arena arena, EditGui editGui, ClickType clickType) {
        int current = arena.getIntervalSeconds();
        int step = configManager.getIntervalStep();
        int updated = current;

        if (clickType.isLeftClick()) {
            updated = Math.min(configManager.getMaximumResetInterval(), current + step);
            playSound(player, configManager.getSoundIntervalIncrease());
        } else if (clickType.isRightClick()) {
            updated = Math.max(configManager.getMinimumResetInterval(), current - step);
            playSound(player, configManager.getSoundIntervalDecrease());
        } else {
            return;
        }

        if (updated != current) {
            arena.setIntervalSeconds(updated);
            arenaManager.saveArenaAsync(arena);
            editGui.refresh();
            messageManager.send(player, "gui.interval-set", "interval", String.valueOf(updated));
        }
    }

    private void handleRegenToggle(Player player, Arena arena, EditGui editGui) {
        boolean newValue = !arena.isAutoRegen();
        arena.setAutoRegen(newValue);
        arenaManager.saveArenaAsync(arena);
        editGui.refresh();

        if (newValue) {
            playSound(player, configManager.getSoundToggleEnable());
            messageManager.send(player, "gui.auto-regen-enabled", "arena", arena.getName());
        } else {
            playSound(player, configManager.getSoundToggleDisable());
            messageManager.send(player, "gui.auto-regen-disabled", "arena", arena.getName());
        }
    }

    private void playSound(Player player, org.bukkit.Sound sound) {
        if (configManager.isSoundsEnabled()) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }
}
