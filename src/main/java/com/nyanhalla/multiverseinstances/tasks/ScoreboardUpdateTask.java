package com.nyanhalla.multiverseinstances.tasks;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Tarea periódica que actualiza los scoreboards de jugadores en instancias.
 */
public class ScoreboardUpdateTask extends BukkitRunnable {

    private final MultiverseInstances plugin;

    public ScoreboardUpdateTask(MultiverseInstances plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (plugin.getInstanceScoreboard() == null) return;
        plugin.getInstanceScoreboard().updateAll();
    }
}
