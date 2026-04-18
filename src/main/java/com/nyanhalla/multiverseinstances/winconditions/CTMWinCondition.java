package com.nyanhalla.multiverseinstances.winconditions;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import com.nyanhalla.multiverseinstances.core.Instance;
import com.nyanhalla.multiverseinstances.core.InstanceManager;
import com.nyanhalla.multiverseinstances.core.WinConditionEngine;
import com.nyanhalla.multiverseinstances.models.Monument;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class CTMWinCondition extends WinCondition {

    private final WinConditionEngine engine;

    public CTMWinCondition(MultiverseInstances plugin, InstanceManager instanceManager) {
        super(plugin, instanceManager);
        this.engine = new WinConditionEngine(plugin, instanceManager);
    }

    @Override
    public void initializeInstance(Instance instance) {
        refreshMonumentStates(instance);
        plugin.getLogger().info("CTM: " + instance.getMonuments().size() + " monumentos cargados para " + instance.getId());
    }

    @Override
    public void resetInstance(Instance instance) {
        for (Monument monument : instance.getMonuments()) {
            monument.setCompleted(false);
        }
    }

    @Override
    public boolean checkVictory(Instance instance) {
        boolean requireAll = plugin.getConfig().getBoolean("ctm.require-all", true);

        if (requireAll) {
            for (Monument monument : instance.getMonuments()) {
                if (!monument.isCompleted()) {
                    return false;
                }
            }
            return true;
        } else {
            for (Monument monument : instance.getMonuments()) {
                if (monument.isCompleted()) {
                    return true;
                }
            }
            return false;
        }
    }

    public void onBlockPlace(Instance instance, Block block) {
        refreshMonument(instance, block.getLocation(), true);
    }

    public void onBlockBreak(Instance instance, Block block) {
        refreshMonument(instance, block.getLocation(), false);
    }

    private void refreshMonument(Instance instance, Location blockLocation, boolean placedState) {
        for (Monument monument : instance.getMonuments()) {
            Location monumentLocation = monument.getLocation();
            if (monumentLocation == null || !sameBlock(monumentLocation, blockLocation)) {
                continue;
            }

            boolean wasCompleted = monument.isCompleted();
            boolean isCompleted = placedState
                    && blockLocation.getWorld() != null
                    && blockLocation.getBlock().getType() == monument.getMaterial();

            monument.setCompleted(isCompleted);

            if (!wasCompleted && isCompleted) {
                notifyPlayers(instance, "§6§lMonumento completado: §e" + monument.getId()
                        + " §7(" + completedCount(instance) + "/" + instance.getMonuments().size() + ")");
            } else if (wasCompleted && !isCompleted) {
                notifyPlayers(instance, "§c§lMonumento invalidado: §e" + monument.getId());
            }

            if (checkVictory(instance)) {
                engine.triggerVictory(instance);
            }
            break;
        }
    }

    public void refreshMonumentStates(Instance instance) {
        for (Monument monument : instance.getMonuments()) {
            Location monumentLocation = monument.getLocation();
            boolean completed = monumentLocation != null
                    && monumentLocation.getWorld() != null
                    && monumentLocation.getBlock().getType() == monument.getMaterial();
            monument.setCompleted(completed);
        }
    }

    private int completedCount(Instance instance) {
        int completed = 0;
        for (Monument monument : instance.getMonuments()) {
            if (monument.isCompleted()) {
                completed++;
            }
        }
        return completed;
    }

    private void notifyPlayers(Instance instance, String rawMessage) {
        for (String playerName : instance.getPlayers()) {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                player.sendMessage(com.nyanhalla.multiverseinstances.utils.MessageUtils.colorize(rawMessage));
            }
        }
    }

    private boolean sameBlock(Location a, Location b) {
        return a.getWorld() != null
                && b.getWorld() != null
                && a.getWorld().getName().equals(b.getWorld().getName())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}
