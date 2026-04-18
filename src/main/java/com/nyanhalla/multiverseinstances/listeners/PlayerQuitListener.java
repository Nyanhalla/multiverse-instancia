package com.nyanhalla.multiverseinstances.listeners;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final MultiverseInstances plugin;

    public PlayerQuitListener(MultiverseInstances plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getInstanceManager().handlePlayerQuit(event.getPlayer());
    }
}
