package me.qyro.prismarenas.manager;

import me.qyro.prismarenas.PrismArenas;
import me.qyro.prismarenas.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public final class MessageManager {

    private final PrismArenas plugin;
    private File messagesFile;
    private FileConfiguration messages;
    private TextUtil.Format format = TextUtil.Format.AUTO;
    private String prefix = "";

    public MessageManager(PrismArenas plugin) {
        this.plugin = plugin;
    }

    public void load() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        String formatName = messages.getString("format", "auto").trim().toUpperCase();
        format = switch (formatName) {
            case "LEGACY" -> TextUtil.Format.LEGACY;
            case "MINIMESSAGE" -> TextUtil.Format.MINIMESSAGE;
            default -> TextUtil.Format.AUTO;
        };

        prefix = messages.getString("prefix", "<aqua><bold>PRISMARENAS <dark_gray>- <white>");
    }

    public void reload() {
        load();
    }

    public Component parse(String input) {
        return TextUtil.parse(input, format);
    }

    public Component get(String path) {
        return parse(messages.getString(path, "<red>Missing message: " + path));
    }

    public Component get(String path, String... placeholders) {
        String raw = messages.getString(path, "<red>Missing message: " + path);
        return parse(TextUtil.replacePlaceholders(raw, placeholders));
    }

    public void send(CommandSender sender, String path) {
        sender.sendMessage(get("prefix").append(get(path)));
    }

    public void send(CommandSender sender, String path, String... placeholders) {
        sender.sendMessage(get("prefix").append(get(path, placeholders)));
    }

    public void sendPrefixed(CommandSender sender, Component message) {
        sender.sendMessage(get("prefix").append(message));
    }

    public void sendRaw(CommandSender sender, String path) {
        sender.sendMessage(get(path));
    }

    public void sendRaw(CommandSender sender, String path, String... placeholders) {
        sender.sendMessage(get(path, placeholders));
    }

    public void send(Player player, String path) {
        send((CommandSender) player, path);
    }

    public void send(Player player, String path, String... placeholders) {
        send((CommandSender) player, path, placeholders);
    }

    public void logInfo(String path, String... placeholders) {
        plugin.getLogger().info(strip(get(path, placeholders)));
    }

    public String strip(Component component) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
    }

    public void saveDefaultsIfNeeded() {
        if (messagesFile == null) {
            return;
        }
        try {
            if (!messagesFile.exists()) {
                plugin.saveResource("messages.yml", false);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to ensure messages.yml exists", e);
        }
    }
}
