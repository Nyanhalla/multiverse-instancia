package com.nyanhalla.multiverseinstances.storage;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import com.nyanhalla.multiverseinstances.models.Checkpoint;
import com.nyanhalla.multiverseinstances.models.Monument;
import com.nyanhalla.multiverseinstances.templates.MapTemplate;
import com.nyanhalla.multiverseinstances.utils.InstanceUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class YAMLTemplateStore {

    private final MultiverseInstances plugin;
    private final File templatesFolder;

    public YAMLTemplateStore(MultiverseInstances plugin) {
        this.plugin = plugin;
        this.templatesFolder = new File(plugin.getDataFolder(), "templates");
        if (!templatesFolder.exists()) {
            templatesFolder.mkdirs();
        }
    }

    public void save(MapTemplate template) {
        File file = new File(templatesFolder, template.getWorldName() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("world", template.getWorldName());
        config.set("win-condition.type", template.getWinConditionType());
        config.set("checkpoints", serializeCheckpoints(template.getCheckpoints()));
        config.set("monuments", serializeMonuments(template.getMonuments()));

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar plantilla " + template.getWorldName() + ": " + e.getMessage());
        }
    }

    public Optional<MapTemplate> load(String worldName) {
        File file = new File(templatesFolder, worldName + ".yml");
        if (!file.exists()) {
            return Optional.empty();
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        MapTemplate template = new MapTemplate(
                config.getString("world", worldName),
                config.getString("win-condition.type", "ctm")
        );

        for (Object item : config.getList("checkpoints", new ArrayList<>())) {
            if (!(item instanceof java.util.Map<?, ?> checkpointData)) {
                continue;
            }

            int id = (Integer) checkpointData.get("id");
            String regionId = (String) checkpointData.get("region");
            Location location = deserializeLocation((String) checkpointData.get("location"));
            Location minimum = deserializeLocation((String) checkpointData.get("minimum"));
            Location maximum = deserializeLocation((String) checkpointData.get("maximum"));

            template.upsertCheckpoint(new Checkpoint(id, regionId, location, minimum, maximum));
        }

        for (Object item : config.getList("monuments", new ArrayList<>())) {
            if (!(item instanceof java.util.Map<?, ?> monumentData)) {
                continue;
            }

            String id = (String) monumentData.get("id");
            Material material = Material.matchMaterial((String) monumentData.get("material"));
            Location location = deserializeLocation((String) monumentData.get("location"));
            if (id == null || material == null || location == null) {
                continue;
            }

            template.upsertMonument(new Monument(id, material, location));
        }

        return Optional.of(template);
    }

    public List<MapTemplate> getAll() {
        List<MapTemplate> templates = new ArrayList<>();
        File[] files = templatesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return templates;
        }

        for (File file : files) {
            String worldName = file.getName().replace(".yml", "");
            load(worldName).ifPresent(templates::add);
        }

        return templates;
    }

    private List<java.util.Map<String, Object>> serializeCheckpoints(List<Checkpoint> checkpoints) {
        List<java.util.Map<String, Object>> data = new ArrayList<>();
        for (Checkpoint checkpoint : checkpoints) {
            java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("id", checkpoint.getId());
            entry.put("region", checkpoint.getRegionId());
            entry.put("location", InstanceUtils.locationToString(checkpoint.getLocation()));
            entry.put("minimum", InstanceUtils.locationToString(checkpoint.getMinimumPoint()));
            entry.put("maximum", InstanceUtils.locationToString(checkpoint.getMaximumPoint()));
            data.add(entry);
        }
        return data;
    }

    private List<java.util.Map<String, Object>> serializeMonuments(List<Monument> monuments) {
        List<java.util.Map<String, Object>> data = new ArrayList<>();
        for (Monument monument : monuments) {
            java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("id", monument.getId());
            entry.put("material", monument.getMaterial().name());
            entry.put("location", InstanceUtils.locationToString(monument.getLocation()));
            data.add(entry);
        }
        return data;
    }

    private Location deserializeLocation(String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            return null;
        }

        String worldName = InstanceUtils.extractWorldFromLocationString(serialized);
        String coords = InstanceUtils.extractCoordsFromLocationString(serialized);
        return InstanceUtils.stringToLocation(worldName, coords);
    }
}
