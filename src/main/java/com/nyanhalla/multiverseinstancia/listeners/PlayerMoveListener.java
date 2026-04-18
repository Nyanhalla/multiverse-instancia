package com.nyanhalla.multiverseinstancia.listeners;

import com.nyanhalla.multiverseinstancia.MultiverseInstancia;
import com.nyanhalla.multiverseinstancia.core.Instance;
import com.nyanhalla.multiverseinstancia.core.InstanceManager;
import com.nyanhalla.multiverseinstancia.winconditions.CheckpointWinCondition;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMoveListener implements Listener {

    private final MultiverseInstancia plugin;
    private final Map<UUID, Location> lastLocations = new HashMap<>();

    public PlayerMoveListener(MultiverseInstancia plugin) {
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

        // Verificar checkpoint win condition
        if ("checkpoint".equalsIgnoreCase(instance.getWinConditionType())) {
            CheckpointWinCondition checkpointWin = (CheckpointWinCondition) plugin.getInstanceManager()
                    .getWinCondition("checkpoint");
            if (checkpointWin != null) {
                // Verificar region - esto es un placeholder
                // En un caso real se integraria con WorldGuard para verificar
                // si el jugador entro en una region de checkpoint
                // String regionId = checkWorldGuardRegion(player, instance);
                // checkpointWin.verifyCheckpoint(instance, player, regionId);
            }
        }
    }
}
