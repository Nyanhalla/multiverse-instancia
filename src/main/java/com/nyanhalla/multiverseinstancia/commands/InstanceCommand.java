package com.nyanhalla.multiverseinstancia.commands;

import com.nyanhalla.multiverseinstancia.MultiverseInstancia;
import com.nyanhalla.multiverseinstancia.core.Instance;
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

public class InstanceCommand implements CommandExecutor, TabCompleter {

    private final MultiverseInstancia plugin;

    public InstanceCommand(MultiverseInstancia plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create" -> handleCreate(sender, args);
            case "list" -> plugin.getInstanceManager().listInstances(sender);
            case "info" -> handleInfo(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "force-reset", "reset" -> handleForceReset(sender, args);
            case "purge" -> plugin.getInstanceManager().purgeOrphans(sender);
            case "reload" -> {
                if (!sender.hasPermission("mi.admin.reload")) {
                    sender.sendMessage(MessageUtils.colorize("&cNo tienes permiso."));
                    return true;
                }
                plugin.reload();
                sender.sendMessage(MessageUtils.colorize("&aConfiguracion recargada."));
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mi.admin.create") && !sender.hasPermission("mi.player.create")) {
            sender.sendMessage(MessageUtils.colorize("&cNo tienes permiso para crear instancias."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtils.colorize("&cUso: /mi create <nombre_mapa> [equipo]"));
            return;
        }

        String mapName = args[1];
        String teamName = args.length >= 3 ? args[2] : (sender instanceof Player ? sender.getName() : "Solo");

        plugin.getInstanceManager().createInstance(mapName, teamName, sender);
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mi.admin.list")) {
            sender.sendMessage(MessageUtils.colorize("&cNo tienes permiso."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtils.colorize("&cUso: /mi info <instancia>"));
            return;
        }

        plugin.getInstanceManager().sendInstanceInfo(sender, args[1]);
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mi.admin.delete")) {
            sender.sendMessage(MessageUtils.colorize("&cNo tienes permiso."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtils.colorize("&cUso: /mi delete <instancia>"));
            return;
        }

        plugin.getInstanceManager().deleteInstance(args[1], sender);
    }

    private void handleForceReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mi.admin.delete")) {
            sender.sendMessage(MessageUtils.colorize("&cNo tienes permiso."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtils.colorize("&cUso: /mi force-reset <instancia>"));
            return;
        }

        plugin.getInstanceManager().forceReset(args[1], sender);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtils.colorize("&6&lMultiverse-Instancia &7v1.0.0"));
        sender.sendMessage(MessageUtils.colorize("&7" + "=".repeat(40)));
        sender.sendMessage(MessageUtils.colorize("&e/mi create <mapa> [equipo] &7- Crear instancia"));
        sender.sendMessage(MessageUtils.colorize("&e/mi list &7- Listar instancias"));
        sender.sendMessage(MessageUtils.colorize("&e/mi info <instancia> &7- Ver detalles"));
        sender.sendMessage(MessageUtils.colorize("&e/mi delete <instancia> &7- Eliminar instancia"));
        sender.sendMessage(MessageUtils.colorize("&e/mi force-reset <instancia> &7- Resetear instancia"));
        sender.sendMessage(MessageUtils.colorize("&e/mi join <instancia> &7- Unirse a instancia"));
        sender.sendMessage(MessageUtils.colorize("&e/mi leave &7- Salir de instancia"));
        sender.sendMessage(MessageUtils.colorize("&e/mi status &7- Ver progreso"));
        sender.sendMessage(MessageUtils.colorize("&e/mi team create <nombre> &7- Crear equipo"));
        sender.sendMessage(MessageUtils.colorize("&7" + "=".repeat(40)));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("create", "list", "info", "delete", "force-reset", "purge", "reload");
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("info") || subCommand.equals("delete") || subCommand.equals("force-reset")) {
                return plugin.getInstanceManager().getActiveInstances().stream()
                        .map(Instance::getId)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}
