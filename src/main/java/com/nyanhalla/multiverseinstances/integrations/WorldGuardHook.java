package com.nyanhalla.multiverseinstances.integrations;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;

public class WorldGuardHook {

    public boolean upsertCuboidRegion(World world, String regionId, Location minimum, Location maximum) {
        if (world == null || regionId == null || minimum == null || maximum == null) {
            return false;
        }

        if (!ProtectedRegion.isValidId(regionId)) {
            return false;
        }

        RegionManager regionManager = getRegionManager(world);
        if (regionManager == null) {
            return false;
        }

        ProtectedCuboidRegion region = new ProtectedCuboidRegion(
                regionId,
                BlockVector3.at(minimum.getBlockX(), minimum.getBlockY(), minimum.getBlockZ()),
                BlockVector3.at(maximum.getBlockX(), maximum.getBlockY(), maximum.getBlockZ())
        );

        regionManager.addRegion(region);
        return save(regionManager);
    }

    public boolean removeRegion(World world, String regionId) {
        if (world == null || regionId == null || regionId.isEmpty()) {
            return false;
        }

        RegionManager regionManager = getRegionManager(world);
        if (regionManager == null || !regionManager.hasRegion(regionId)) {
            return false;
        }

        regionManager.removeRegion(regionId);
        return save(regionManager);
    }

    private RegionManager getRegionManager(World world) {
        return WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(world));
    }

    private boolean save(RegionManager regionManager) {
        try {
            regionManager.saveChanges();
            return true;
        } catch (StorageException ignored) {
            return false;
        }
    }
}
