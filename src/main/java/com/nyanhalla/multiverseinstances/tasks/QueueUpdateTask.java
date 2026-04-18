package com.nyanhalla.multiverseinstances.tasks;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Tarea periódica que actualiza la action bar de jugadores en cola.
 */
public class QueueUpdateTask extends BukkitRunnable {

    private final MultiverseInstances plugin;

    public QueueUpdateTask(MultiverseInstances plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (plugin.getQueueManager() == null) return;
        plugin.getQueueManager().sendQueueUpdates();
    }
}
