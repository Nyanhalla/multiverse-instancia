package com.nyanhalla.multiverseinstances.templates;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import com.nyanhalla.multiverseinstances.core.Instance;
import com.nyanhalla.multiverseinstances.integrations.WorldEditHook;
import com.nyanhalla.multiverseinstances.integrations.WorldGuardHook;
import com.nyanhalla.multiverseinstances.models.Checkpoint;
import com.nyanhalla.multiverseinstances.models.Monument;
import com.nyanhalla.multiverseinstances.storage.YAMLTemplateStore;
import com.nyanhalla.multiverseinstances.utils.InstanceUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TemplateManager {

    private final MultiverseInstances plugin;
    private final YAMLTemplateStore templateStore;
    private final WorldEditHook worldEditHook;
    private final WorldGuardHook worldGuardHook;

    public TemplateManager(MultiverseInstances plugin, YAMLTemplateStore templateStore,
                           WorldEditHook worldEditHook, WorldGuardHook worldGuardHook) {
        this.plugin = plugin;
        this.templateStore = templateStore;
        this.worldEditHook = worldEditHook;
        this.worldGuardHook = worldGuardHook;
    }

    public Optional<MapTemplate> getTemplate(String worldName) {
        return templateStore.load(worldName);
    }

    public List<MapTemplate> getTemplates() {
        return templateStore.getAll();
    }

    public MapTemplate setTemplate(World world, String winConditionType) {
        MapTemplate template = templateStore.load(world.getName())
                .orElseGet(() -> new MapTemplate(world.getName(), normalizeWinCondition(winConditionType)));

        template.setWinConditionType(normalizeWinCondition(winConditionType));
        templateStore.save(template);
        return template;
    }

    public boolean hasTemplate(String worldName) {
        return templateStore.load(worldName).isPresent();
    }

    public String getWinConditionType(String worldName) {
        return templateStore.load(worldName)
                .map(MapTemplate::getWinConditionType)
                .orElse(null);
    }

    public Optional<Checkpoint> addCheckpoint(Player player, int checkpointId, String regionId) {
        World world = player.getWorld();
        MapTemplate template = templateStore.load(world.getName())
                .orElse(new MapTemplate(world.getName(), "checkpoint"));

        String normalizedRegionId = normalizeRegionId(regionId, checkpointId);
        SelectionBounds bounds = resolveCheckpointBounds(player);
        Location checkpointLocation = player.getLocation().clone();

        Checkpoint checkpoint = new Checkpoint(
                checkpointId,
                normalizedRegionId,
                checkpointLocation,
                bounds.minimum(),
                bounds.maximum()
        );

        template.upsertCheckpoint(checkpoint);
        templateStore.save(template);

        if (worldGuardHook != null) {
            worldGuardHook.upsertCuboidRegion(world, normalizedRegionId, bounds.minimum(), bounds.maximum());
        }

        return Optional.of(checkpoint);
    }

    public boolean removeCheckpoint(World world, int checkpointId) {
        Optional<MapTemplate> maybeTemplate = templateStore.load(world.getName());
        if (maybeTemplate.isEmpty()) {
            return false;
        }

        MapTemplate template = maybeTemplate.get();
        Optional<Checkpoint> maybeCheckpoint = template.getCheckpoint(checkpointId);
        if (maybeCheckpoint.isEmpty()) {
            return false;
        }

        boolean removed = template.removeCheckpoint(checkpointId);
        if (!removed) {
            return false;
        }

        templateStore.save(template);
        if (worldGuardHook != null) {
            worldGuardHook.removeRegion(world, maybeCheckpoint.get().getRegionId());
        }
        return true;
    }

    public Optional<Monument> addMonument(Player player, String monumentId, Material material) {
        World world = player.getWorld();
        MapTemplate template = templateStore.load(world.getName())
                .orElse(new MapTemplate(world.getName(), "ctm"));

        Location location = resolveMonumentLocation(player);
        if (location == null) {
            return Optional.empty();
        }

        Monument monument = new Monument(monumentId, material, location);
        template.upsertMonument(monument);
        templateStore.save(template);
        return Optional.of(monument);
    }

    public boolean removeMonument(World world, String monumentId) {
        Optional<MapTemplate> maybeTemplate = templateStore.load(world.getName());
        if (maybeTemplate.isEmpty()) {
            return false;
        }

        MapTemplate template = maybeTemplate.get();
        boolean removed = template.removeMonument(monumentId);
        if (removed) {
            templateStore.save(template);
        }
        return removed;
    }

    public boolean applyTemplate(Instance instance) {
        Optional<MapTemplate> maybeTemplate = templateStore.load(instance.getBaseWorld());
        if (maybeTemplate.isEmpty()) {
            return false;
        }

        World instanceWorld = instance.getWorld();
        if (instanceWorld == null) {
            return false;
        }

        MapTemplate template = maybeTemplate.get();
        instance.clearCheckpoints();
        instance.clearMonuments();

        for (Checkpoint checkpoint : template.getCheckpoints()) {
            instance.addCheckpoint(new Checkpoint(
                    checkpoint.getId(),
                    checkpoint.getRegionId(),
                    InstanceUtils.cloneToWorld(checkpoint.getLocation(), instanceWorld),
                    InstanceUtils.cloneToWorld(checkpoint.getMinimumPoint(), instanceWorld),
                    InstanceUtils.cloneToWorld(checkpoint.getMaximumPoint(), instanceWorld),
                    false
            ));
        }

        for (Monument monument : template.getMonuments()) {
            Location monumentLocation = InstanceUtils.cloneToWorld(monument.getLocation(), instanceWorld);
            boolean completed = monumentLocation != null && monumentLocation.getBlock().getType() == monument.getMaterial();
            instance.addMonument(new Monument(monument.getId(), monument.getMaterial(), monumentLocation, completed));
        }

        return true;
    }

    public void createInstanceRegions(Instance instance) {
        if (worldGuardHook == null || instance.getWorld() == null) {
            return;
        }

        for (Checkpoint checkpoint : instance.getCheckpoints()) {
            worldGuardHook.upsertCuboidRegion(
                    instance.getWorld(),
                    checkpoint.getRegionId(),
                    checkpoint.getMinimumPoint(),
                    checkpoint.getMaximumPoint()
            );
        }
    }

    public void deleteInstanceRegions(Instance instance) {
        if (worldGuardHook == null || instance.getWorld() == null) {
            return;
        }

        for (Checkpoint checkpoint : instance.getCheckpoints()) {
            worldGuardHook.removeRegion(instance.getWorld(), checkpoint.getRegionId());
        }
    }

    public boolean isWorldEditAvailable() {
        return worldEditHook != null;
    }

    public boolean isWorldGuardAvailable() {
        return worldGuardHook != null;
    }

    private SelectionBounds resolveCheckpointBounds(Player player) {
        if (worldEditHook != null) {
            Optional<WorldEditHook.CuboidSelection> selection = worldEditHook.getSelection(player, player.getWorld());
            if (selection.isPresent()) {
                Location minimum = new Location(
                        player.getWorld(),
                        selection.get().minimum().getBlockX(),
                        selection.get().minimum().getBlockY(),
                        selection.get().minimum().getBlockZ()
                );
                Location maximum = new Location(
                        player.getWorld(),
                        selection.get().maximum().getBlockX(),
                        selection.get().maximum().getBlockY(),
                        selection.get().maximum().getBlockZ()
                );
                return new SelectionBounds(minimum, maximum);
            }
        }

        Location blockLocation = player.getLocation().getBlock().getLocation();
        return new SelectionBounds(blockLocation, blockLocation);
    }

    private Location resolveMonumentLocation(Player player) {
        if (worldEditHook != null) {
            Optional<Location> primary = worldEditHook.getPrimaryPosition(player, player.getWorld());
            if (primary.isPresent()) {
                return primary.get().getBlock().getLocation();
            }
        }

        Block targetBlock = player.getTargetBlockExact(10);
        if (targetBlock != null) {
            return targetBlock.getLocation();
        }

        return player.getLocation().getBlock().getLocation();
    }

    private String normalizeWinCondition(String type) {
        return type.toLowerCase(Locale.ROOT);
    }

    private String normalizeRegionId(String regionId, int checkpointId) {
        if (regionId == null || regionId.isBlank()) {
            return "mi_cp_" + checkpointId;
        }

        return regionId.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_\\-]+", "_")
                .replaceAll("_{2,}", "_");
    }

    private record SelectionBounds(Location minimum, Location maximum) {
    }
}
