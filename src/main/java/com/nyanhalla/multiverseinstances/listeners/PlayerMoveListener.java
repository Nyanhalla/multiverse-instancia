package com.nyanhalla.multiverseinstances.listeners;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import com.nyanhalla.multiverseinstances.core.Instance;
import com.nyanhalla.multiverseinstances.winconditions.CheckpointWinCondition;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveListener implements Listener {

    private final MultiverseInstances plugin;

    public PlayerMoveListener(MultiverseInstances plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Solo procesar si hay cambio de posicion
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Ignorar solo rotacion
        if (from.getBlockX() == to.getBlockX() &&
            from.getBlockY() == to.getBlockY() &&
            from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Instance instance = plugin.getInstanceManager().getPlayerInstance(player.getUniqueId());

        if (instance == null) {
            return;
        }

        if (!player.getWorld().getName().equals(instance.getWorldName())) {
            return;
        }

        if ("checkpoint".equalsIgnoreCase(instance.getWinConditionType())) {
            CheckpointWinCondition checkpointWin = (CheckpointWinCondition) plugin.getInstanceManager()
                    .getWinCondition("checkpoint");
            if (checkpointWin != null) {
                checkpointWin.verifyCheckpoint(instance, player, to);
            }
        }
    }
}
