package com.nyanhalla.multiverseinstances.models;

import org.bukkit.Location;

public class Checkpoint {

    private final int id;
    private final String regionId;
    private final Location location;
    private final Location minimumPoint;
    private final Location maximumPoint;
    private boolean reached;

    public Checkpoint(int id, String regionId, Location location, Location minimumPoint, Location maximumPoint) {
        this(id, regionId, location, minimumPoint, maximumPoint, false);
    }

    public Checkpoint(int id, String regionId, Location location, Location minimumPoint, Location maximumPoint, boolean reached) {
        this.id = id;
        this.regionId = regionId;
        this.location = location;
        this.minimumPoint = minimumPoint;
        this.maximumPoint = maximumPoint;
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

    public Location getMinimumPoint() {
        return minimumPoint;
    }

    public Location getMaximumPoint() {
        return maximumPoint;
    }

    public boolean isReached() {
        return reached;
    }

    public void setReached(boolean reached) {
        this.reached = reached;
    }

    public boolean contains(Location candidate) {
        if (candidate == null || minimumPoint == null || maximumPoint == null) {
            return false;
        }

        if (candidate.getWorld() == null || minimumPoint.getWorld() == null || maximumPoint.getWorld() == null) {
            return false;
        }

        if (!candidate.getWorld().getName().equals(minimumPoint.getWorld().getName())) {
            return false;
        }

        int minX = Math.min(minimumPoint.getBlockX(), maximumPoint.getBlockX());
        int minY = Math.min(minimumPoint.getBlockY(), maximumPoint.getBlockY());
        int minZ = Math.min(minimumPoint.getBlockZ(), maximumPoint.getBlockZ());
        int maxX = Math.max(minimumPoint.getBlockX(), maximumPoint.getBlockX());
        int maxY = Math.max(minimumPoint.getBlockY(), maximumPoint.getBlockY());
        int maxZ = Math.max(minimumPoint.getBlockZ(), maximumPoint.getBlockZ());

        return candidate.getBlockX() >= minX && candidate.getBlockX() <= maxX
                && candidate.getBlockY() >= minY && candidate.getBlockY() <= maxY
                && candidate.getBlockZ() >= minZ && candidate.getBlockZ() <= maxZ;
    }
}
