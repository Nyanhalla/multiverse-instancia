package com.nyanhalla.multiverseinstances.queue;

import java.util.UUID;

/**
 * Representa una entrada en la cola de instancias.
 */
public record QueueEntry(UUID playerId, String mapName, String teamName, long joinedAt) {

    public QueueEntry(UUID playerId, String mapName, String teamName) {
        this(playerId, mapName, teamName, System.currentTimeMillis());
    }

    public long getWaitTimeMillis() {
        return System.currentTimeMillis() - joinedAt;
    }

    public String getWaitTimeFormatted() {
        long seconds = getWaitTimeMillis() / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return minutes + "m " + remainingSeconds + "s";
    }
}
