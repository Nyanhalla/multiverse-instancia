package com.nyanhalla.multiverseinstancia.listeners;

import com.nyanhalla.multiverseinstancia.MultiverseInstancia;
import com.nyanhalla.multiverseinstancia.core.Instance;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldListener implements Listener {

    private final MultiverseInstancia plugin;

    public WorldListener(MultiverseInstancia plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldUnload(WorldUnloadEvent event) {
        World world = event.getWorld();

        // Verificar si es una instancia
        Instance instance = plugin.getInstanceManager().getInstance(
                findInstanceIdByWorld(world.getName())
        );

        if (instance != null) {
            // Prevenir descarga de mundos de instancia si estan activos
            if (instance.getStatus() == Instance.InstanceStatus.ACTIVE && !instance.isEmpty()) {
                event.setCancelled(true);
                plugin.getLogger().warning("Se intento descargar el mundo de una instancia activa: " + world.getName());
            }
        }
    }

    private String findInstanceIdByWorld(String worldName) {
        for (Instance instance : plugin.getInstanceManager().getActiveInstances()) {
            if (instance.getWorldName().equals(worldName)) {
                return instance.getId();
            }
        }
        return null;
    }
}
