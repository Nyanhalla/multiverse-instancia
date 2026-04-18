package com.nyanhalla.multiverseinstances.listeners;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import com.nyanhalla.multiverseinstances.core.Instance;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldListener implements Listener {

    private final MultiverseInstances plugin;

    public WorldListener(MultiverseInstances plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldUnload(WorldUnloadEvent event) {
        World world = event.getWorld();

        Instance instance = plugin.getInstanceManager().getInstanceByWorldName(world.getName());

        if (instance != null) {
            if (instance.getStatus() == Instance.InstanceStatus.ACTIVE && !instance.isEmpty()) {
                event.setCancelled(true);
                plugin.getLogger().warning("Se intento descargar el mundo de una instancia activa: " + world.getName());
            }
        }
    }
}
