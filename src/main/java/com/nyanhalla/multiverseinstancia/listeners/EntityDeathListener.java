package com.nyanhalla.multiverseinstancia.listeners;

import com.nyanhalla.multiverseinstancia.MultiverseInstancia;
import com.nyanhalla.multiverseinstancia.core.Instance;
import com.nyanhalla.multiverseinstancia.core.InstanceManager;
import com.nyanhalla.multiverseinstancia.winconditions.DungeonWinCondition;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeathListener implements Listener {

    private final MultiverseInstancia plugin;

    public EntityDeathListener(MultiverseInstancia plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        // Verificar si es un boss de dungeon
        String bossType = plugin.getConfig().getString("dungeon.boss-bar.entity-type", "WITHER_SKELETON");
        EntityType expectedType;

        try {
            expectedType = EntityType.valueOf(bossType);
        } catch (IllegalArgumentException e) {
            return;
        }

        if (event.getEntityType() != expectedType) {
            return;
        }

        // Verificar si esta en una instancia
        Instance instance = findInstanceByWorld(event.getEntity().getWorld());
        if (instance == null) {
            return;
        }

        // Verificar win condition tipo dungeon
        if (!"dungeon".equalsIgnoreCase(instance.getWinConditionType())) {
            return;
        }

        DungeonWinCondition dungeonWin = (DungeonWinCondition) plugin.getInstanceManager()
                .getWinCondition("dungeon");
        if (dungeonWin != null) {
            dungeonWin.onBossDeath(instance);
        }
    }

    private Instance findInstanceByWorld(org.bukkit.World world) {
        for (Instance instance : plugin.getInstanceManager().getActiveInstances()) {
            if (instance.getWorldName().equals(world.getName())) {
                return instance;
            }
        }
        return null;
    }
}
