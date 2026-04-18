package com.nyanhalla.multiverseinstancia.models;

import org.bukkit.Location;
import org.bukkit.Material;

public class Monument {

    private final Material material;
    private final Location location;
    private boolean broken;

    public Monument(Material material, Location location) {
        this.material = material;
        this.location = location;
        this.broken = false;
    }

    public Monument(Material material, Location location, boolean broken) {
        this.material = material;
        this.location = location;
        this.broken = broken;
    }

    public Material getMaterial() {
        return material;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isBroken() {
        return broken;
    }

    public void setBroken(boolean broken) {
        this.broken = broken;
    }
}
