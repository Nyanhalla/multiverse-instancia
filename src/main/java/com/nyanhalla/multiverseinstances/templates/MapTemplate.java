package com.nyanhalla.multiverseinstances.templates;

import com.nyanhalla.multiverseinstances.models.Checkpoint;
import com.nyanhalla.multiverseinstances.models.Monument;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class MapTemplate {

    private final String worldName;
    private String winConditionType;
    private final List<Checkpoint> checkpoints;
    private final List<Monument> monuments;

    public MapTemplate(String worldName, String winConditionType) {
        this.worldName = worldName;
        this.winConditionType = winConditionType;
        this.checkpoints = new ArrayList<>();
        this.monuments = new ArrayList<>();
    }

    public String getWorldName() {
        return worldName;
    }

    public String getWinConditionType() {
        return winConditionType;
    }

    public void setWinConditionType(String winConditionType) {
        this.winConditionType = winConditionType;
    }

    public List<Checkpoint> getCheckpoints() {
        return new ArrayList<>(checkpoints);
    }

    public List<Monument> getMonuments() {
        return new ArrayList<>(monuments);
    }

    public Optional<Checkpoint> getCheckpoint(int id) {
        return checkpoints.stream().filter(checkpoint -> checkpoint.getId() == id).findFirst();
    }

    public Optional<Monument> getMonument(String id) {
        return monuments.stream().filter(monument -> monument.getId().equalsIgnoreCase(id)).findFirst();
    }

    public void upsertCheckpoint(Checkpoint checkpoint) {
        checkpoints.removeIf(existing -> existing.getId() == checkpoint.getId());
        checkpoints.add(checkpoint);
        checkpoints.sort(Comparator.comparingInt(Checkpoint::getId));
    }

    public boolean removeCheckpoint(int id) {
        return checkpoints.removeIf(checkpoint -> checkpoint.getId() == id);
    }

    public void upsertMonument(Monument monument) {
        monuments.removeIf(existing -> existing.getId().equalsIgnoreCase(monument.getId()));
        monuments.add(monument);
        monuments.sort(Comparator.comparing(Monument::getId));
    }

    public boolean removeMonument(String id) {
        return monuments.removeIf(monument -> monument.getId().equalsIgnoreCase(id));
    }
}
