package com.nyanhalla.multiverseinstances.core;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import com.nyanhalla.multiverseinstances.winconditions.WinCondition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class WinConditionEngine {

    private final MultiverseInstances plugin;
    private final InstanceManager instanceManager;

    public WinConditionEngine(MultiverseInstances plugin, InstanceManager instanceManager) {
        this.plugin = plugin;
        this.instanceManager = instanceManager;
    }

    public void triggerVictory(Instance instance) {
        String trigger = plugin.getConfig().getString(instance.getWinConditionType() + ".victory-trigger", "broadcast");

        switch (trigger) {
            case "command" -> executeVictoryCommands(instance);
            case "broadcast" -> broadcastVictory(instance);
            case "reward" -> {
                broadcastVictory(instance);
                giveRewards(instance);
            }
            default -> broadcastVictory(instance);
        }

        instanceManager.onVictory(instance);
    }

    private void executeVictoryCommands(Instance instance) {
        String path = instance.getWinConditionType() + ".victory-commands";
        List<String> commands = plugin.getConfig().getStringList(path);

        for (String command : commands) {
            if (command.contains("%player%")) {
                for (String playerName : instance.getPlayers()) {
                    dispatchVictoryCommand(instance, command, playerName);
                }
                continue;
            }

            dispatchVictoryCommand(instance, command, null);
        }
    }

    private void dispatchVictoryCommand(Instance instance, String command, String playerName) {
        String processed = command
                .replace("%team%", instance.getTeam())
                .replace("%instance%", instance.getId())
                .replace("%player%", playerName == null ? "" : playerName);

        if (processed.startsWith("bc ") || processed.startsWith("broadcast ")) {
            String message = processed.substring(processed.indexOf(' ') + 1);
            plugin.broadcast(message);
            return;
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
    }

    private void broadcastVictory(Instance instance) {
        String path = instance.getWinConditionType() + ".broadcast-title";
        if (plugin.getConfig().contains(path)) {
            String title = plugin.getConfig().getString(path + ".title", "§6§l¡VICTORIA!");
            String subtitle = plugin.getConfig().getString(path + ".subtitle", "§eMapa completado");
            int fadeIn = plugin.getConfig().getInt(path + ".fade-in", 10);
            int stay = plugin.getConfig().getInt(path + ".stay", 60);
            int fadeOut = plugin.getConfig().getInt(path + ".fade-out", 20);

            for (String playerName : instance.getPlayers()) {
                Player player = Bukkit.getPlayer(playerName);
                if (player != null) {
                    player.sendTitle(
                            com.nyanhalla.multiverseinstances.utils.MessageUtils.colorize(title),
                            com.nyanhalla.multiverseinstances.utils.MessageUtils.colorize(subtitle),
                            fadeIn,
                            stay,
                            fadeOut
                    );
                }
            }
        }

        plugin.broadcast("§6§l¡Equipo " + instance.getTeam() + " ha completado el mapa!");
    }

    private void giveRewards(Instance instance) {
        List<String> commands = plugin.getConfig().getStringList(instance.getWinConditionType() + ".victory-commands");

        for (String playerName : instance.getPlayers()) {
            if (Bukkit.getPlayer(playerName) == null) {
                continue;
            }

            for (String command : commands) {
                if (!command.contains("%player%")) {
                    continue;
                }
                dispatchVictoryCommand(instance, command, playerName);
            }
        }
    }
}
