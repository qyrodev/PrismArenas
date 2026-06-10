package me.qyro.prismarenas.gui;

import me.qyro.prismarenas.arena.Arena;
import me.qyro.prismarenas.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class EditGui implements InventoryHolder {

    public static final int SIZE = 27;
    public static final int CLOCK_SLOT = 11;
    public static final int PAPER_SLOT = 15;

    private static final Material FILLER_MATERIAL = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
    private static final Material CLOCK_MATERIAL = Material.CLOCK;
    private static final Material PAPER_MATERIAL = Material.PAPER;

    private final Arena arena;
    private final Inventory inventory;

    public EditGui(Arena arena) {
        this.arena = arena;
        this.inventory = Bukkit.createInventory(this, SIZE, title(arena.getName()));
        populate();
    }

    private static Component title(String arenaName) {
        return TextUtil.parse("&fEditing: &b" + arenaName, TextUtil.Format.LEGACY);
    }

    public Arena getArena() {
        return arena;
    }

    public int getSize() {
        return SIZE;
    }

    public void refresh() {
        populate();
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    private void populate() {
        ItemStack filler = createFiller();
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(CLOCK_SLOT, createClockItem());
        inventory.setItem(PAPER_SLOT, createRegenItem());
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(FILLER_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.parse(" ", TextUtil.Format.LEGACY));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createClockItem() {
        ItemStack item = new ItemStack(CLOCK_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.parse("&e&lSet Interval", TextUtil.Format.LEGACY));
        meta.lore(List.of(
                TextUtil.parse("&fCurrent: &a" + arena.getIntervalSeconds() + "s", TextUtil.Format.LEGACY),
                Component.empty(),
                TextUtil.parse("&fLeft-Click: &a+60s", TextUtil.Format.LEGACY),
                TextUtil.parse("&fRight-Click: &c-60s", TextUtil.Format.LEGACY)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRegenItem() {
        ItemStack item = new ItemStack(PAPER_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.parse("&f&lRegeneration", TextUtil.Format.LEGACY));
        if (arena.isAutoRegen()) {
            meta.lore(List.of(
                    TextUtil.parse("&fAutoRegen: &aEnabled", TextUtil.Format.LEGACY),
                    Component.empty(),
                    TextUtil.parse("&fClick to toggle", TextUtil.Format.LEGACY)
            ));
        } else {
            meta.lore(List.of(
                    TextUtil.parse("&fAutoRegen: &cDisabled", TextUtil.Format.LEGACY),
                    Component.empty(),
                    TextUtil.parse("&fClick to toggle", TextUtil.Format.LEGACY)
            ));
        }
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
