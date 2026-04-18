package com.nyanhalla.multiverseinstances.listeners;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import com.nyanhalla.multiverseinstances.core.Instance;
import com.nyanhalla.multiverseinstances.winconditions.CTMWinCondition;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {

    private final MultiverseInstances plugin;

    public BlockPlaceListener(MultiverseInstances plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Instance instance = plugin.getInstanceManager().getPlayerInstance(player.getUniqueId());
        if (instance == null || !"ctm".equalsIgnoreCase(instance.getWinConditionType())) {
            return;
        }

        CTMWinCondition ctmWin = (CTMWinCondition) plugin.getInstanceManager().getWinCondition("ctm");
        if (ctmWin != null) {
            ctmWin.onBlockPlace(instance, event.getBlockPlaced());
        }
    }
}
