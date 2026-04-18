package com.nyanhalla.multiverseinstancia.listeners;

import com.nyanhalla.multiverseinstancia.MultiverseInstancia;
import com.nyanhalla.multiverseinstancia.core.Instance;
import com.nyanhalla.multiverseinstancia.core.InstanceManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final MultiverseInstancia plugin;

    public PlayerQuitListener(MultiverseInstancia plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        InstanceManager manager = plugin.getInstanceManager();
        Instance instance = manager.getPlayerInstance(player.getUniqueId());

        if (instance == null) {
            return;
        }

        // Guardar ubicacion del jugador
        instance.savePlayerLocation(player.getName(), player.getLocation());

        // Remover de la instancia
        instance.removePlayer(player.getName());

        // Verificar si la instancia quedo vacia
        if (instance.isEmpty()) {
            int autoCloseEmpty = plugin.getConfig().getInt("instance.auto-close-empty", 300);
            if (autoCloseEmpty > 0) {
                // Programar cierre automatico
                scheduleAutoClose(instance, autoCloseEmpty);
            }
        }

        // Guardar estado de la instancia
        manager.saveAllInstances();
    }

    private void scheduleAutoClose(Instance instance, int delaySeconds) {
        org.bukkit.scheduler.BukkitRunnable task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                Instance current = plugin.getInstanceManager().getInstance(instance.getId());
                if (current != null && current.isEmpty()) {
                    plugin.getLogger().info("Cerrando instancia vacia: " + instance.getId());
                    plugin.getInstanceManager().deleteInstance(instance.getId(), org.bukkit.Bukkit.getConsoleSender());
                }
            }
        };
        task.runTaskLater(plugin, delaySeconds * 20L);
    }
}
