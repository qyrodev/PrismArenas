package me.qyro.prismarenas.listener;

import me.qyro.prismarenas.manager.ConfigManager;
import me.qyro.prismarenas.manager.MessageManager;
import me.qyro.prismarenas.manager.SelectionManager;
import me.qyro.prismarenas.util.PrismWand;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class WandListener implements Listener {

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final SelectionManager selectionManager;

    public WandListener(Plugin plugin, ConfigManager configManager, MessageManager messageManager,
                        SelectionManager selectionManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.selectionManager = selectionManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!PrismWand.isWand(plugin, item)) {
            return;
        }

        if (!player.hasPermission("prismarenas.wand")) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        event.setCancelled(true);

        SelectionManager.Selection selection = selectionManager.getSelection(player.getUniqueId());

        if (action == Action.LEFT_CLICK_BLOCK) {
            selection.setPos1(block.getLocation());
            messageManager.send(player, "wand.position-1");
        } else {
            selection.setPos2(block.getLocation());
            messageManager.send(player, "wand.position-2");
        }

        if (configManager.isSoundsEnabled()) {
            player.playSound(player.getLocation(), configManager.getSoundSelection(), 1.0f, 1.2f);
        }
    }
}
