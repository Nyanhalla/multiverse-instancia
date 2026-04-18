package com.nyanhalla.multiverseinstancia.commands;

import com.nyanhalla.multiverseinstancia.MultiverseInstancia;
import com.nyanhalla.multiverseinstancia.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JoinCommand implements CommandExecutor, TabCompleter {

    private final MultiverseInstancia plugin;

    public JoinCommand(MultiverseInstancia plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                           @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.colorize("&cEste comando solo puede ser usado por jugadores."));
            return true;
        }

        String commandName = command.getName().toLowerCase();

        if (commandName.equals("mijoin") || commandName.equals("join")) {
            return handleJoin(player, args);
        } else if (commandName.equals("mileave") || commandName.equals("leave")) {
            return handleLeave(player);
        }

        return true;
    }

    private boolean handleJoin(Player player, String[] args) {
        if (!player.hasPermission("mi.player.join")) {
            player.sendMessage(MessageUtils.colorize("&cNo tienes permiso para unirte a instancias."));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(MessageUtils.colorize("&cUso: /mi join <instancia>"));
            return true;
        }

        String instanceId = args[0];
        return plugin.getInstanceManager().joinInstance(instanceId, player);
    }

    private boolean handleLeave(Player player) {
        return plugin.getInstanceManager().leaveInstance(player);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && (command.getName().equalsIgnoreCase("mijoin") ||
                                  command.getName().equalsIgnoreCase("join"))) {
            return plugin.getInstanceManager().getActiveInstances().stream()
                    .map(instance -> instance.getId())
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
