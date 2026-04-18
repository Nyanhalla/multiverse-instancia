package com.nyanhalla.multiverseinstancia.commands;

import com.nyanhalla.multiverseinstancia.MultiverseInstancia;
import com.nyanhalla.multiverseinstancia.core.InstanceManager;
import com.nyanhalla.multiverseinstancia.models.Team;
import com.nyanhalla.multiverseinstancia.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TeamCommand implements CommandExecutor, TabCompleter {

    private final MultiverseInstancia plugin;

    public TeamCommand(MultiverseInstancia plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.colorize("&cEste comando solo puede ser usado por jugadores."));
            return true;
        }

        if (!player.hasPermission("mi.player.team")) {
            player.sendMessage(MessageUtils.colorize("&cNo tienes permiso para usar comandos de equipo."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        InstanceManager manager = plugin.getInstanceManager();

        switch (subCommand) {
            case "create" -> {
                if (args.length < 2) {
                    player.sendMessage(MessageUtils.colorize("&cUso: /mi team create <nombre>"));
                    return true;
                }
                manager.createTeam(args[1], player);
            }
            case "invite" -> {
                if (args.length < 2) {
                    player.sendMessage(MessageUtils.colorize("&cUso: /mi team invite <jugador>"));
                    return true;
                }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(MessageUtils.colorize("&cJugador no encontrado."));
                    return true;
                }
                manager.inviteToTeam(player, target);
            }
            case "accept" -> manager.acceptInvite(player);
            case "leave" -> manager.leaveTeam(player);
            case "info" -> {
                Team team = manager.getPlayerTeam(player);
                if (team == null) {
                    player.sendMessage(MessageUtils.colorize("&cNo tienes un equipo."));
                    return true;
                }
                player.sendMessage(MessageUtils.colorize("&6&lEquipo: " + team.getName()));
                player.sendMessage(MessageUtils.colorize("&eMiembros: &f" + team.getMembers().size()));
                player.sendMessage(MessageUtils.colorize("&eTag: " + team.getTag()));
            }
            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageUtils.colorize("&6&lComandos de Equipo"));
        player.sendMessage(MessageUtils.colorize("&7" + "=".repeat(40)));
        player.sendMessage(MessageUtils.colorize("&e/mi team create <nombre> &7- Crear equipo"));
        player.sendMessage(MessageUtils.colorize("&e/mi team invite <jugador> &7- Invitar a equipo"));
        player.sendMessage(MessageUtils.colorize("&e/mi team accept &7- Aceptar invitacion"));
        player.sendMessage(MessageUtils.colorize("&e/mi team leave &7- Dejar equipo"));
        player.sendMessage(MessageUtils.colorize("&e/mi team info &7- Ver info del equipo"));
        player.sendMessage(MessageUtils.colorize("&7" + "=".repeat(40)));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("create", "invite", "accept", "leave", "info");
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
