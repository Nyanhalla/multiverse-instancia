package com.nyanhalla.multiverseinstances.utils;

import org.bukkit.Location;
import org.bukkit.World;

public final class InstanceUtils {

    private InstanceUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String locationToString(Location location) {
        if (location == null) {
            return "";
        }
        return String.format("%s:%.1f,%.1f,%.1f",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ());
    }

    public static Location stringToLocation(String worldName, String coords) {
        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null || coords == null) {
            return null;
        }
        String[] parts = coords.split(",");
        if (parts.length < 3) {
            return null;
        }
        try {
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Location cloneToWorld(Location location, World targetWorld) {
        if (location == null || targetWorld == null) {
            return null;
        }

        return new Location(
                targetWorld,
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public static String extractWorldFromLocationString(String locationString) {
        if (locationString == null || locationString.isEmpty()) {
            return null;
        }
        int colonIndex = locationString.indexOf(':');
        if (colonIndex > 0) {
            return locationString.substring(0, colonIndex);
        }
        return null;
    }

    public static String extractCoordsFromLocationString(String locationString) {
        if (locationString == null || locationString.isEmpty()) {
            return null;
        }
        int colonIndex = locationString.indexOf(':');
        if (colonIndex > 0 && colonIndex < locationString.length() - 1) {
            return locationString.substring(colonIndex + 1);
        }
        return null;
    }
}
