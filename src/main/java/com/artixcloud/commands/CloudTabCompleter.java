package com.artixcloud.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab-completer para o comando /cloud.
 */
public class CloudTabCompleter implements TabCompleter {

    private static final List<String> SUB_COMMANDS = List.of("criar", "status", "help", "reload");

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return List.of();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return SUB_COMMANDS.stream()
                    .filter(s -> s.startsWith(partial))
                    .filter(s -> !s.equals("reload") || player.hasPermission("artixcloud.admin"))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
