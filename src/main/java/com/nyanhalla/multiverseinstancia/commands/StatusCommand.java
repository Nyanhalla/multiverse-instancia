package com.nyanhalla.multiverseinstancia.commands;

import com.nyanhalla.multiverseinstancia.MultiverseInstancia;
import com.nyanhalla.multiverseinstancia.core.Instance;
import com.nyanhalla.multiverseinstancia.core.InstanceManager;
import com.nyanhalla.multiverseinstancia.utils.MessageUtils;
import com.nyanhalla.multiverseinstancia.winconditions.CheckpointWinCondition;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StatusCommand implements CommandExecutor {

    private final MultiverseInstancia plugin;

    public StatusCommand(MultiverseInstancia plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.colorize("&cEste comando solo puede ser usado por jugadores."));
            return true;
        }

        InstanceManager manager = plugin.getInstanceManager();
        Instance instance = manager.getPlayerInstance(player.getUniqueId());

        if (instance == null) {
            player.sendMessage(MessageUtils.colorize("&cNo estas en ninguna instancia."));
            return true;
        }

        player.sendMessage(MessageUtils.colorize("&6&lEstado de Instancia"));
        player.sendMessage(MessageUtils.colorize("&7" + "=".repeat(40)));
        player.sendMessage(MessageUtils.colorize("&eInstancia: &f" + instance.getId()));
        player.sendMessage(MessageUtils.colorize("&eEquipo: &f" + instance.getTeam()));
        player.sendMessage(MessageUtils.colorize("&eTipo: &f" + instance.getWinConditionType()));

        // Mostrar progreso segun tipo de win condition
        String winConditionType = instance.getWinConditionType();
        switch (winConditionType.toLowerCase()) {
            case "ctm" -> {
                int total = instance.getMonuments().size();
                long broken = instance.getMonuments().stream().filter(m -> m.isBroken()).count();
                player.sendMessage(MessageUtils.colorize("&eProgreso: &f" + broken + "/" + total + " monumentos"));
            }
            case "checkpoint" -> {
                var checkpointWin = (CheckpointWinCondition) manager.getWinCondition("checkpoint");
                if (checkpointWin != null) {
                    player.sendMessage(MessageUtils.colorize("&eProgreso: &f" + checkpointWin.getStatus(instance)));
                }
            }
            case "dungeon" -> {
                player.sendMessage(MessageUtils.colorize("&eEstado: &aBoss activo"));
            }
            default -> {
                player.sendMessage(MessageUtils.colorize("&eWin Condition: &f" + winConditionType));
            }
        }

        player.sendMessage(MessageUtils.colorize("&7" + "=".repeat(40)));

        // Teleport a checkpoint si esta habilitado y hay argumentos
        if (args.length > 0 && winConditionType.equalsIgnoreCase("checkpoint")) {
            try {
                int checkpointId = Integer.parseInt(args[0]);
                var checkpointWin = (CheckpointWinCondition) manager.getWinCondition("checkpoint");
                if (checkpointWin != null) {
                    checkpointWin.teleportToCheckpoint(player, instance, checkpointId);
                }
            } catch (NumberFormatException ignored) {
                player.sendMessage(MessageUtils.colorize("&cUso: /mi status [checkpoint_id]"));
            }
        }

        return true;
    }
}
