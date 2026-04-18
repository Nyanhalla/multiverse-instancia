package com.nyanhalla.multiverseinstances.listeners;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import com.nyanhalla.multiverseinstances.core.Instance;
import com.nyanhalla.multiverseinstances.core.InstanceManager;
import com.nyanhalla.multiverseinstances.winconditions.CTMWinCondition;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {

    private final MultiverseInstances plugin;

    public BlockBreakListener(MultiverseInstances plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Instance instance = plugin.getInstanceManager().getPlayerInstance(player.getUniqueId());

        if (instance == null) {
            return; // No esta en una instancia
        }

        // Verificar tipo de win condition
        String winConditionType = instance.getWinConditionType();
        if ("ctm".equalsIgnoreCase(winConditionType)) {
            CTMWinCondition ctmWin = (CTMWinCondition) plugin.getInstanceManager()
                    .getWinCondition(winConditionType);
            if (ctmWin != null) {
                ctmWin.onBlockBreak(instance, event.getBlock());
            }
        }
    }
}
