package me.qyro.prismarenas.util;

import me.qyro.prismarenas.manager.MessageManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class PrismWand {

    public static final String WAND_KEY = "prism_wand";
    private static final Material WAND_MATERIAL = Material.WOODEN_AXE;

    private PrismWand() {
    }

    public static ItemStack create(Plugin plugin, MessageManager messageManager) {
        ItemStack item = new ItemStack(WAND_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messageManager.parse("<aqua><bold>PrismWand"));
        meta.lore(List.of(
                messageManager.parse("<gray>Left-Click Position 1"),
                messageManager.parse("<gray>Right-Click Position 2")
        ));
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, WAND_KEY),
                PersistentDataType.BYTE,
                (byte) 1
        );
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isWand(Plugin plugin, ItemStack item) {
        if (item == null || item.getType() != WAND_MATERIAL || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(
                new NamespacedKey(plugin, WAND_KEY),
                PersistentDataType.BYTE
        );
    }
}
