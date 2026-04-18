package com.nyanhalla.multiverseinstances.integrations;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Optional;

public class WorldEditHook {

    public Optional<CuboidSelection> getSelection(Player player, World world) {
        var wePlayer = BukkitAdapter.adapt(player);
        var weWorld = BukkitAdapter.adapt(world);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);

        if (!session.isSelectionDefined(weWorld)) {
            return Optional.empty();
        }

        try {
            Region region = session.getSelection(weWorld);
            return Optional.of(new CuboidSelection(region.getMinimumPoint(), region.getMaximumPoint()));
        } catch (IncompleteRegionException ignored) {
            return Optional.empty();
        }
    }

    public Optional<Location> getPrimaryPosition(Player player, World world) {
        var wePlayer = BukkitAdapter.adapt(player);
        var weWorld = BukkitAdapter.adapt(world);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);

        try {
            BlockVector3 primary = session.getRegionSelector(weWorld).getPrimaryPosition();
            return Optional.of(BukkitAdapter.adapt(world, primary));
        } catch (IncompleteRegionException ignored) {
            return Optional.empty();
        }
    }

    public record CuboidSelection(BlockVector3 minimum, BlockVector3 maximum) {
    }
}
