package com.nyanhalla.multiverseinstances.models;

import org.bukkit.Location;
import org.bukkit.Material;

public class Monument {

    private final String id;
    private final Material material;
    private final Location location;
    private boolean completed;

    public Monument(String id, Material material, Location location) {
        this(id, material, location, false);
    }

    public Monument(String id, Material material, Location location, boolean completed) {
        this.id = id;
        this.material = material;
        this.location = location;
        this.completed = completed;
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
