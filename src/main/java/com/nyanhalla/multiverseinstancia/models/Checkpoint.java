package com.nyanhalla.multiverseinstancia.models;

import org.bukkit.Location;

public class Checkpoint {

    private final int id;
    private final String regionId;
    private final Location location;
    private boolean reached;

    public Checkpoint(int id, String regionId, Location location) {
        this.id = id;
        this.regionId = regionId;
        this.location = location;
        this.reached = false;
    }

    public Checkpoint(int id, String regionId, Location location, boolean reached) {
        this.id = id;
        this.regionId = regionId;
        this.location = location;
        this.reached = reached;
    }

    public int getId() {
        return id;
    }

    public String getRegionId() {
        return regionId;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isReached() {
        return reached;
    }

    public void setReached(boolean reached) {
        this.reached = reached;
    }
}
