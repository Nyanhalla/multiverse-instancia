package com.nyanhalla.multiverseinstances.tasks;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import com.nyanhalla.multiverseinstances.core.Instance;
import com.nyanhalla.multiverseinstances.models.Checkpoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CheckpointParticleTask extends BukkitRunnable {

    private final MultiverseInstances plugin;

    public CheckpointParticleTask(MultiverseInstances plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("checkpoint.particles.enabled", true)) {
            return;
        }

        Particle particle;
        try {
            particle = Particle.valueOf(plugin.getConfig().getString("checkpoint.particles.type", "END_ROD"));
        } catch (IllegalArgumentException ex) {
            particle = Particle.END_ROD;
        }

        int count = plugin.getConfig().getInt("checkpoint.particles.count", 10);
        double offsetX = plugin.getConfig().getDouble("checkpoint.particles.offset-x", 0.35D);
        double offsetY = plugin.getConfig().getDouble("checkpoint.particles.offset-y", 0.9D);
        double offsetZ = plugin.getConfig().getDouble("checkpoint.particles.offset-z", 0.35D);
        double extra = plugin.getConfig().getDouble("checkpoint.particles.extra", 0.01D);
        double viewDistance = plugin.getConfig().getDouble("checkpoint.particles.view-distance", 32.0D);
        boolean showReached = plugin.getConfig().getBoolean("checkpoint.particles.show-reached", false);
        double viewDistanceSquared = viewDistance * viewDistance;

        for (Instance instance : plugin.getInstanceManager().getActiveInstances()) {
            if (!"checkpoint".equalsIgnoreCase(instance.getWinConditionType())) {
                continue;
            }

            for (String playerName : instance.getPlayers()) {
                Player player = Bukkit.getPlayerExact(playerName);
                if (player == null || player.getWorld() == null || !player.getWorld().getName().equals(instance.getWorldName())) {
                    continue;
                }

                for (Checkpoint checkpoint : instance.getCheckpoints()) {
                    if (checkpoint.isReached() && !showReached) {
                        continue;
                    }

                    Location marker = checkpoint.getLocation();
                    if (marker == null || marker.getWorld() == null || !marker.getWorld().getName().equals(player.getWorld().getName())) {
                        continue;
                    }

                    if (player.getLocation().distanceSquared(marker) > viewDistanceSquared) {
                        continue;
                    }

                    Location display = marker.clone().add(0.5D, 1.0D, 0.5D);
                    player.spawnParticle(particle, display, count, offsetX, offsetY, offsetZ, extra);
                }
            }
        }
    }
}
