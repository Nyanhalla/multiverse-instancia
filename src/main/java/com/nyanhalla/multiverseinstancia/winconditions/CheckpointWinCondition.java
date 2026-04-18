package com.nyanhalla.multiverseinstancia.winconditions;

import com.nyanhalla.multiverseinstancia.MultiverseInstancia;
import com.nyanhalla.multiverseinstancia.core.Instance;
import com.nyanhalla.multiverseinstancia.core.InstanceManager;
import com.nyanhalla.multiverseinstancia.core.WinConditionEngine;
import com.nyanhalla.multiverseinstancia.models.Checkpoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class CheckpointWinCondition extends WinCondition {

    private final WinConditionEngine engine;

    public CheckpointWinCondition(MultiverseInstancia plugin, InstanceManager instanceManager) {
        super(plugin, instanceManager);
        this.engine = new WinConditionEngine(plugin, instanceManager);
    }

    @Override
    public void initializeInstance(Instance instance) {
        // Configurar checkpoints desde archivo de mundo o generarlos automaticamente
        int maxCheckpoints = plugin.getConfig().getInt("checkpoint.max-checkpoints", 10);
        boolean autoRegister = plugin.getConfig().getBoolean("checkpoint.auto-register", true);

        if (autoRegister) {
            // Auto-detectar checkpoints usando WorldGuard regions
            detectCheckpoints(instance, maxCheckpoints);
        }

        plugin.getLogger().info("Checkpoint: " + instance.getCheckpoints().size() + " checkpoints inicializados para " + instance.getId());
    }

    private void detectCheckpoints(Instance instance, int maxCheckpoints) {
        // Placeholder - en un caso real se integraria con WorldGuard
        // para detectar regiones con prefijo como "cp1", "cp2", etc.
        plugin.getLogger().info("Checkpoint: Auto-detect no implementado, usa configuracion manual");
    }

    public void registerCheckpoint(Instance instance, int id, String regionId, Location location) {
        instance.addCheckpoint(id, regionId, location);
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

    public void verifyCheckpoint(Instance instance, Player player, String regionId) {
        for (Checkpoint checkpoint : instance.getCheckpoints()) {
            if (checkpoint.getRegionId().equalsIgnoreCase(regionId) && !checkpoint.isReached()) {
                checkpoint.setReached(true);

                // Notificar al jugador
                player.sendMessage(com.nyanhalla.multiverseinstancia.utils.MessageUtils.colorize(
                        "§a§lCheckpoint " + checkpoint.getId() + " alcanzado! §7(" +
                                countReached(instance) + "/" + instance.getCheckpoints().size() + ")"));

                // Guardar progreso si esta configurado
                if (plugin.getConfig().getBoolean("checkpoint.save-on-advance", true)) {
                    // Guardar estado
                    plugin.getInstanceManager().saveAllInstances();
                }

                // Verificar victoria
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
                player.sendMessage(com.nyanhalla.multiverseinstancia.utils.MessageUtils.colorize(
                        "§aTeletransportado al checkpoint " + checkpointId));
                return;
            }
        }
        player.sendMessage(com.nyanhalla.multiverseinstancia.utils.MessageUtils.colorize(
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
