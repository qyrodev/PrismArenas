package me.qyro.prismarenas.command;

import me.qyro.prismarenas.manager.ArenaManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ArenaTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("wand", "create", "delete", "edit", "regen", "reload");

    private final ArenaManager arenaManager;

    public ArenaTabCompleter(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("delete") || sub.equals("edit") || sub.equals("regen")) {
                return filter(arenaManager.getArenaNames(), args[1]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
