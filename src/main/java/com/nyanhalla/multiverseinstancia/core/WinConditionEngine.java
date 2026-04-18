package com.nyanhalla.multiverseinstancia.core;

import com.nyanhalla.multiverseinstancia.MultiverseInstancia;
import com.nyanhalla.multiverseinstancia.winconditions.WinCondition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class WinConditionEngine {

    private final MultiverseInstancia plugin;
    private final InstanceManager instanceManager;

    public WinConditionEngine(MultiverseInstancia plugin, InstanceManager instanceManager) {
        this.plugin = plugin;
        this.instanceManager = instanceManager;
    }

    public void triggerVictory(Instance instance) {
        String trigger = plugin.getConfig().getString(instance.getWinConditionType() + ".victory-trigger", "broadcast");

        switch (trigger) {
            case "command" -> executeVictoryCommands(instance);
            case "broadcast" -> broadcastVictory(instance);
            case "reward" -> {
                executeVictoryCommands(instance);
                giveRewards(instance);
            }
            default -> broadcastVictory(instance);
        }

        // Notificar al InstanceManager para cleanup si esta configurado
        instanceManager.onVictory(instance);
    }

    private void executeVictoryCommands(Instance instance) {
        String path = instance.getWinConditionType() + ".victory-commands";
        List<String> commands = plugin.getConfig().getStringList(path);

        for (String command : commands) {
            String processed = command
                    .replace("%team%", instance.getTeam())
                    .replace("%instance%", instance.getId());

            if (processed.startsWith("bc ") || processed.startsWith("broadcast ")) {
                String message = processed.substring(processed.indexOf(' ') + 1);
                plugin.broadcast(message);
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
            }
        }
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
                            com.nyanhalla.multiverseinstancia.utils.MessageUtils.colorize(title),
                            com.nyanhalla.multiverseinstancia.utils.MessageUtils.colorize(subtitle),
                            fadeIn * 50,
                            stay * 50,
                            fadeOut * 50
                    );
                }
            }
        }

        plugin.broadcast("§6§l¡Equipo " + instance.getTeam() + " ha completado el mapa!");
    }

    private void giveRewards(Instance instance) {
        List<String> commands = plugin.getConfig().getStringList(instance.getWinConditionType() + ".victory-commands");

        for (String playerName : instance.getPlayers()) {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                for (String command : commands) {
                    if (command.startsWith("give ") || command.startsWith("give %player%")) {
                        String processed = command.replace("%player%", playerName);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
                    }
                }
            }
        }
    }
}
