package com.nyanhalla.multiverseinstances.winconditions;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import com.nyanhalla.multiverseinstances.core.Instance;
import com.nyanhalla.multiverseinstances.core.InstanceManager;
import com.nyanhalla.multiverseinstances.core.WinConditionEngine;
import com.nyanhalla.multiverseinstances.models.Checkpoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class CheckpointWinCondition extends WinCondition {

    private final WinConditionEngine engine;

    public CheckpointWinCondition(MultiverseInstances plugin, InstanceManager instanceManager) {
        super(plugin, instanceManager);
        this.engine = new WinConditionEngine(plugin, instanceManager);
    }

    @Override
    public void initializeInstance(Instance instance) {
        plugin.getLogger().info("Checkpoint: " + instance.getCheckpoints().size() + " checkpoints inicializados para " + instance.getId());
    }

    public void registerCheckpoint(Instance instance, int id, String regionId, Location location) {
        instance.addCheckpoint(id, regionId, location, location, location);
        plugin.getLogger().info("Checkpoint registrado: " + id + " en " + regionId);
    }

    @Override
    public void resetInstance(Instance instance) {
        for (Checkpoint checkpoint : instance.getCheckpoints()) {
            checkpoint.setReached(false);
        }
    }

    @Override
    public boolean checkVictory(Instance instance) {
        boolean partialVictory = plugin.getConfig().getBoolean("checkpoint.partial-victory", false);

        if (partialVictory) {
            // Victoria al alcanzar el ultimo checkpoint
            Checkpoint lastCheckpoint = null;
            for (Checkpoint cp : instance.getCheckpoints()) {
                if (cp.getId() > (lastCheckpoint != null ? lastCheckpoint.getId() : 0)) {
                    lastCheckpoint = cp;
                }
            }
            return lastCheckpoint != null && lastCheckpoint.isReached();
        } else {
            // Victoria al alcanzar TODOS los checkpoints
            for (Checkpoint cp : instance.getCheckpoints()) {
                if (!cp.isReached()) {
                    return false;
                }
            }
            return !instance.getCheckpoints().isEmpty();
        }
    }

    public void verifyCheckpoint(Instance instance, Player player, Location location) {
        for (Checkpoint checkpoint : instance.getCheckpoints()) {
            if (!checkpoint.isReached() && checkpoint.contains(location)) {
                checkpoint.setReached(true);

                player.sendMessage(com.nyanhalla.multiverseinstances.utils.MessageUtils.colorize(
                        "§a§lCheckpoint " + checkpoint.getId() + " alcanzado! §7(" +
                                countReached(instance) + "/" + instance.getCheckpoints().size() + ")"));

                if (plugin.getConfig().getBoolean("checkpoint.save-on-advance", true)) {
                    plugin.getInstanceManager().saveAllInstances();
                }

                if (checkVictory(instance)) {
                    engine.triggerVictory(instance);
                }

                break;
            }
        }
    }

    private int countReached(Instance instance) {
        int count = 0;
        for (Checkpoint cp : instance.getCheckpoints()) {
            if (cp.isReached()) {
                count++;
            }
        }
        return count;
    }

    public void teleportToCheckpoint(Player player, Instance instance, int checkpointId) {
        for (Checkpoint checkpoint : instance.getCheckpoints()) {
            if (checkpoint.getId() == checkpointId && checkpoint.isReached()) {
                player.teleport(checkpoint.getLocation());
                player.sendMessage(com.nyanhalla.multiverseinstances.utils.MessageUtils.colorize(
                        "§aTeletransportado al checkpoint " + checkpointId));
                return;
            }
        }
        player.sendMessage(com.nyanhalla.multiverseinstances.utils.MessageUtils.colorize(
                "§cCheckpoint " + checkpointId + " no alcanzado o no existe."));
    }

    public String getStatus(Instance instance) {
        int total = instance.getCheckpoints().size();
        int reached = 0;
        for (Checkpoint cp : instance.getCheckpoints()) {
            if (cp.isReached()) reached++;
        }
        return reached + "/" + total + " checkpoints";
    }
}
