package com.nyanhalla.multiverseinstances.listeners;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import com.nyanhalla.multiverseinstances.core.Instance;
import com.nyanhalla.multiverseinstances.winconditions.DungeonWinCondition;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeathListener implements Listener {

    private final MultiverseInstances plugin;

    public EntityDeathListener(MultiverseInstances plugin) {
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
        Instance instance = plugin.getInstanceManager().getInstanceByWorldName(event.getEntity().getWorld().getName());
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
}
