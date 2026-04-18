package com.nyanhalla.multiverseinstances.commands;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import com.nyanhalla.multiverseinstances.core.Instance;
import com.nyanhalla.multiverseinstances.models.Checkpoint;
import com.nyanhalla.multiverseinstances.models.Monument;
import com.nyanhalla.multiverseinstances.models.Team;
import com.nyanhalla.multiverseinstances.templates.MapTemplate;
import com.nyanhalla.multiverseinstances.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.World;
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
import java.util.Optional;
import java.util.stream.Collectors;

public class InstanceCommand implements CommandExecutor, TabCompleter {

    private final MultiverseInstances plugin;

    public InstanceCommand(MultiverseInstances plugin) {
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
            case "join" -> handleJoin(sender, args);
            case "leave" -> handleLeave(sender);
            case "status" -> handleStatus(sender);
            case "team" -> handleTeam(sender, args);
            case "template" -> handleTemplate(sender, args);
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

    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.colorize("&cEste comando solo puede ser usado por jugadores."));
            return;
        }

        if (!player.hasPermission("mi.player.join")) {
            player.sendMessage(MessageUtils.colorize("&cNo tienes permiso para unirte a instancias."));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(MessageUtils.colorize("&cUso: /mi join <instancia>"));
            return;
        }

        plugin.getInstanceManager().joinInstance(args[1], player);
    }

    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.colorize("&cEste comando solo puede ser usado por jugadores."));
            return;
        }

        plugin.getInstanceManager().leaveInstance(player);
    }

    private void handleStatus(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.colorize("&cEste comando solo puede ser usado por jugadores."));
            return;
        }

        Instance instance = plugin.getInstanceManager().getPlayerInstance(player.getUniqueId());
        if (instance == null) {
            player.sendMessage(MessageUtils.colorize("&cNo estas en ninguna instancia."));
            return;
        }

        player.sendMessage(MessageUtils.colorize("&6&lEstado de Instancia"));
        player.sendMessage(MessageUtils.colorize("&7" + "=".repeat(40)));
        player.sendMessage(MessageUtils.colorize("&eInstancia: &f" + instance.getId()));
        player.sendMessage(MessageUtils.colorize("&eEquipo: &f" + instance.getTeam()));
        player.sendMessage(MessageUtils.colorize("&eTipo: &f" + instance.getWinConditionType()));
        player.sendMessage(MessageUtils.colorize("&eJugadores: &f" + instance.getPlayerCount()));
        player.sendMessage(MessageUtils.colorize("&eEstado: &f" + instance.getStatus().name()));
        if ("ctm".equalsIgnoreCase(instance.getWinConditionType())) {
            long completed = instance.getMonuments().stream().filter(Monument::isCompleted).count();
            player.sendMessage(MessageUtils.colorize("&eMonumentos: &f" + completed + "/" + instance.getMonuments().size()));
        }
        if ("checkpoint".equalsIgnoreCase(instance.getWinConditionType())) {
            long reached = instance.getCheckpoints().stream().filter(Checkpoint::isReached).count();
            player.sendMessage(MessageUtils.colorize("&eCheckpoints: &f" + reached + "/" + instance.getCheckpoints().size()));
        }
        player.sendMessage(MessageUtils.colorize("&7" + "=".repeat(40)));
    }

    private void handleTeam(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.colorize("&cEste comando solo puede ser usado por jugadores."));
            return;
        }

        if (!player.hasPermission("mi.player.team")) {
            player.sendMessage(MessageUtils.colorize("&cNo tienes permiso para usar comandos de equipo."));
            return;
        }

        if (args.length < 2) {
            sendTeamHelp(player);
            return;
        }

        String teamSubCommand = args[1].toLowerCase();
        switch (teamSubCommand) {
            case "create" -> {
                if (args.length < 3) {
                    player.sendMessage(MessageUtils.colorize("&cUso: /mi team create <nombre>"));
                    return;
                }
                plugin.getInstanceManager().createTeam(args[2], player);
            }
            case "invite" -> {
                if (args.length < 3) {
                    player.sendMessage(MessageUtils.colorize("&cUso: /mi team invite <jugador>"));
                    return;
                }
                Player target = plugin.getServer().getPlayer(args[2]);
                if (target == null) {
                    player.sendMessage(MessageUtils.colorize("&cJugador no encontrado."));
                    return;
                }
                plugin.getInstanceManager().inviteToTeam(player, target);
            }
            case "accept" -> plugin.getInstanceManager().acceptInvite(player);
            case "leave" -> plugin.getInstanceManager().leaveTeam(player);
            case "info" -> {
                Team team = plugin.getInstanceManager().getPlayerTeam(player);
                if (team == null) {
                    player.sendMessage(MessageUtils.colorize("&cNo tienes un equipo."));
                    return;
                }

                player.sendMessage(MessageUtils.colorize("&6&lEquipo: " + team.getName()));
                player.sendMessage(MessageUtils.colorize("&eMiembros: &f" + team.getMembers().size()));
                player.sendMessage(MessageUtils.colorize("&eTag: &f" + team.getTag()));
            }
            default -> sendTeamHelp(player);
        }
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

    private void handleTemplate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mi.admin.template")) {
            sender.sendMessage(MessageUtils.colorize("&cNo tienes permiso para gestionar plantillas."));
            return;
        }

        if (args.length < 2) {
            sendTemplateHelp(sender);
            return;
        }

        String templateSubCommand = args[1].toLowerCase();
        switch (templateSubCommand) {
            case "set" -> handleTemplateSet(sender, args);
            case "info" -> handleTemplateInfo(sender, args);
            case "list" -> handleTemplateList(sender);
            case "checkpoint" -> handleTemplateCheckpoint(sender, args);
            case "monument" -> handleTemplateMonument(sender, args);
            default -> sendTemplateHelp(sender);
        }
    }

    private void handleTemplateSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.colorize("&cUso: /mi template set <ctm|checkpoint|dungeon> [world]"));
            return;
        }

        String type = args[2].toLowerCase();
        if (!Arrays.asList("ctm", "checkpoint", "dungeon").contains(type)) {
            sender.sendMessage(MessageUtils.colorize("&cTipo invalido. Usa ctm, checkpoint o dungeon."));
            return;
        }

        World world = resolveWorld(sender, args, 3);
        if (world == null) {
            sender.sendMessage(MessageUtils.colorize("&cDebes indicar un mundo valido o ejecutar el comando dentro del mundo plantilla."));
            return;
        }

        plugin.getTemplateManager().setTemplate(world, type);
        sender.sendMessage(MessageUtils.colorize("&aPlantilla configurada para &e" + world.getName() + "&a con win condition &b" + type + "&a."));
    }

    private void handleTemplateInfo(CommandSender sender, String[] args) {
        World world = resolveWorld(sender, args, 2);
        if (world == null) {
            sender.sendMessage(MessageUtils.colorize("&cDebes indicar un mundo valido o ejecutar el comando dentro del mundo plantilla."));
            return;
        }

        MapTemplate template = plugin.getTemplateManager().getTemplate(world.getName()).orElse(null);
        if (template == null) {
            sender.sendMessage(MessageUtils.colorize("&cNo existe plantilla para el mundo &e" + world.getName() + "&c."));
            return;
        }

        sender.sendMessage(MessageUtils.colorize("&6&lPlantilla: &f" + template.getWorldName()));
        sender.sendMessage(MessageUtils.colorize("&eTipo: &f" + template.getWinConditionType()));
        sender.sendMessage(MessageUtils.colorize("&eWorldEdit: &f" + (plugin.getTemplateManager().isWorldEditAvailable() ? "si" : "no")));
        sender.sendMessage(MessageUtils.colorize("&eWorldGuard: &f" + (plugin.getTemplateManager().isWorldGuardAvailable() ? "si" : "no")));
        sender.sendMessage(MessageUtils.colorize("&eCheckpoints: &f" + template.getCheckpoints().size()));
        for (Checkpoint checkpoint : template.getCheckpoints()) {
            sender.sendMessage(MessageUtils.colorize("&7- CP " + checkpoint.getId() + " &fregion=&e" + checkpoint.getRegionId()));
        }
        sender.sendMessage(MessageUtils.colorize("&eMonumentos: &f" + template.getMonuments().size()));
        for (Monument monument : template.getMonuments()) {
            sender.sendMessage(MessageUtils.colorize("&7- " + monument.getId() + " &fmaterial=&e" + monument.getMaterial().name()));
        }
    }

    private void handleTemplateList(CommandSender sender) {
        List<MapTemplate> templates = plugin.getTemplateManager().getTemplates();
        if (templates.isEmpty()) {
            sender.sendMessage(MessageUtils.colorize("&7No hay plantillas registradas."));
            return;
        }

        sender.sendMessage(MessageUtils.colorize("&6&lPlantillas Registradas"));
        for (MapTemplate template : templates) {
            sender.sendMessage(MessageUtils.colorize("&e" + template.getWorldName() + " &7- &b" + template.getWinConditionType()
                    + " &7- &fCP: " + template.getCheckpoints().size()
                    + " &7- &fMon: " + template.getMonuments().size()));
        }
    }

    private void handleTemplateCheckpoint(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.colorize("&cEste comando requiere un jugador en el mundo plantilla."));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(MessageUtils.colorize("&cUso: /mi template checkpoint <add|remove> <id> [region-id]"));
            return;
        }

        String action = args[2].toLowerCase();
        int checkpointId;
        try {
            checkpointId = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(MessageUtils.colorize("&cEl id del checkpoint debe ser un numero."));
            return;
        }

        if ("add".equals(action)) {
            String regionId = args.length >= 5 ? args[4] : null;
            Optional<Checkpoint> checkpoint = plugin.getTemplateManager().addCheckpoint(player, checkpointId, regionId);
            if (checkpoint.isEmpty()) {
                sender.sendMessage(MessageUtils.colorize("&cNo se pudo registrar el checkpoint."));
                return;
            }

            sender.sendMessage(MessageUtils.colorize("&aCheckpoint &e" + checkpointId + "&a registrado en &b" + player.getWorld().getName()
                    + "&a con region &e" + checkpoint.get().getRegionId() + "&a."));
            return;
        }

        if ("remove".equals(action)) {
            if (!plugin.getTemplateManager().removeCheckpoint(player.getWorld(), checkpointId)) {
                sender.sendMessage(MessageUtils.colorize("&cNo existe ese checkpoint en la plantilla actual."));
                return;
            }

            sender.sendMessage(MessageUtils.colorize("&aCheckpoint &e" + checkpointId + "&a eliminado de la plantilla."));
            return;
        }

        sender.sendMessage(MessageUtils.colorize("&cAccion invalida. Usa add o remove."));
    }

    private void handleTemplateMonument(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.colorize("&cEste comando requiere un jugador en el mundo plantilla."));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(MessageUtils.colorize("&cUso: /mi template monument <add|remove> <id> [material]"));
            return;
        }

        String action = args[2].toLowerCase();
        String monumentId = args[3];

        if ("add".equals(action)) {
            if (args.length < 5) {
                sender.sendMessage(MessageUtils.colorize("&cUso: /mi template monument add <id> <material>"));
                return;
            }

            Material material = Material.matchMaterial(args[4]);
            if (material == null || !material.isBlock()) {
                sender.sendMessage(MessageUtils.colorize("&cDebes indicar un bloque valido."));
                return;
            }

            Optional<Monument> monument = plugin.getTemplateManager().addMonument(player, monumentId, material);
            if (monument.isEmpty()) {
                sender.sendMessage(MessageUtils.colorize("&cNo se pudo registrar el monumento. Usa seleccion primaria de WorldEdit o apunta a un bloque."));
                return;
            }

            sender.sendMessage(MessageUtils.colorize("&aMonumento &e" + monumentId + "&a registrado en &b" + player.getWorld().getName()
                    + "&a con bloque &e" + material.name() + "&a."));
            return;
        }

        if ("remove".equals(action)) {
            if (!plugin.getTemplateManager().removeMonument(player.getWorld(), monumentId)) {
                sender.sendMessage(MessageUtils.colorize("&cNo existe ese monumento en la plantilla actual."));
                return;
            }

            sender.sendMessage(MessageUtils.colorize("&aMonumento &e" + monumentId + "&a eliminado de la plantilla."));
            return;
        }

        sender.sendMessage(MessageUtils.colorize("&cAccion invalida. Usa add o remove."));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtils.colorize("&6&lMultiverse-Instances &7v" + plugin.getDescription().getVersion()));
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
        sender.sendMessage(MessageUtils.colorize("&e/mi template ... &7- Gestionar plantilla del mapa"));
        sender.sendMessage(MessageUtils.colorize("&7" + "=".repeat(40)));
    }

    private void sendTeamHelp(Player player) {
        player.sendMessage(MessageUtils.colorize("&6&lComandos de Equipo"));
        player.sendMessage(MessageUtils.colorize("&7" + "=".repeat(40)));
        player.sendMessage(MessageUtils.colorize("&e/mi team create <nombre> &7- Crear equipo"));
        player.sendMessage(MessageUtils.colorize("&e/mi team invite <jugador> &7- Invitar a equipo"));
        player.sendMessage(MessageUtils.colorize("&e/mi team accept &7- Aceptar invitacion"));
        player.sendMessage(MessageUtils.colorize("&e/mi team leave &7- Dejar equipo"));
        player.sendMessage(MessageUtils.colorize("&e/mi team info &7- Ver info del equipo"));
        player.sendMessage(MessageUtils.colorize("&7" + "=".repeat(40)));
    }

    private void sendTemplateHelp(CommandSender sender) {
        sender.sendMessage(MessageUtils.colorize("&6&lPlantillas de Mapa"));
        sender.sendMessage(MessageUtils.colorize("&7" + "=".repeat(40)));
        sender.sendMessage(MessageUtils.colorize("&e/mi template set <tipo> [world] &7- Define la plantilla base"));
        sender.sendMessage(MessageUtils.colorize("&e/mi template info [world] &7- Ver plantilla actual"));
        sender.sendMessage(MessageUtils.colorize("&e/mi template list &7- Listar plantillas"));
        sender.sendMessage(MessageUtils.colorize("&e/mi template checkpoint add <id> [region] &7- Usa seleccion WE o tu bloque actual"));
        sender.sendMessage(MessageUtils.colorize("&e/mi template checkpoint remove <id> &7- Elimina un checkpoint"));
        sender.sendMessage(MessageUtils.colorize("&e/mi template monument add <id> <material> &7- Usa pos1 de WE o bloque apuntado"));
        sender.sendMessage(MessageUtils.colorize("&e/mi template monument remove <id> &7- Elimina un monumento"));
        sender.sendMessage(MessageUtils.colorize("&7" + "=".repeat(40)));
    }

    private World resolveWorld(CommandSender sender, String[] args, int worldArgIndex) {
        if (args.length > worldArgIndex) {
            return plugin.getServer().getWorld(args[worldArgIndex]);
        }

        if (sender instanceof Player player) {
            return player.getWorld();
        }

        return null;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("create", "list", "info", "delete", "force-reset", "purge", "reload", "join", "leave", "status", "team", "template");
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("info") || subCommand.equals("delete") || subCommand.equals("force-reset") || subCommand.equals("join")) {
                return plugin.getInstanceManager().getActiveInstances().stream()
                        .map(Instance::getId)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (subCommand.equals("team")) {
                return Arrays.asList("create", "invite", "accept", "leave", "info").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (subCommand.equals("template")) {
                return Arrays.asList("set", "info", "list", "checkpoint", "monument").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("team") && args[1].equalsIgnoreCase("invite")) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("template") && args[1].equalsIgnoreCase("set")) {
            return Arrays.asList("ctm", "checkpoint", "dungeon").stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("template")
                && (args[1].equalsIgnoreCase("checkpoint") || args[1].equalsIgnoreCase("monument"))) {
            return Arrays.asList("add", "remove").stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 5 && args[0].equalsIgnoreCase("template")
                && args[1].equalsIgnoreCase("monument")
                && args[2].equalsIgnoreCase("add")) {
            return Arrays.stream(Material.values())
                    .filter(Material::isBlock)
                    .map(Material::name)
                    .filter(s -> s.toLowerCase().startsWith(args[4].toLowerCase()))
                    .limit(50)
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
