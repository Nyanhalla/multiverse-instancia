package com.nyanhalla.multiverseinstances.storage;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import com.nyanhalla.multiverseinstances.core.Instance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class YAMLInstanceStore implements InstanceDataStore {

    private final MultiverseInstances plugin;
    private final File instancesFolder;
    private final File indexFile;

    public YAMLInstanceStore(MultiverseInstances plugin) {
        this.plugin = plugin;
        this.instancesFolder = new File(plugin.getDataFolder(), "instances");
        this.indexFile = new File(plugin.getDataFolder(), "instances/index.yml");

        if (!instancesFolder.exists()) {
            instancesFolder.mkdirs();
        }
    }

    @Override
    public void save(Instance instance) {
        File file = new File(instancesFolder, instance.getId() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("id", instance.getId());
        config.set("base-world", instance.getBaseWorld());
        config.set("display-name", instance.getDisplayName());
        config.set("team", instance.getTeam());
        config.set("world", instance.getWorldName());
        config.set("created-at", instance.getCreatedAt());
        config.set("expires-at", instance.getExpiresAt());
        config.set("status", instance.getStatus().name());
        config.set("win-condition.type", instance.getWinConditionType());
        config.set("win-condition.monuments", instance.getMonumentsData());
        config.set("win-condition.checkpoints", instance.getCheckpointsData());
        config.set("players", instance.getPlayers());

        try {
            config.save(file);
            updateIndex(instance.getId(), true);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar instancia " + instance.getId() + ": " + e.getMessage());
        }
    }

    @Override
    public Optional<Instance> load(String instanceId) {
        File file = new File(instancesFolder, instanceId + ".yml");
        if (!file.exists()) {
            return Optional.empty();
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        Instance instance = new Instance(
                config.getString("id"),
                config.getString("base-world"),
                config.getString("display-name"),
                config.getString("team"),
                config.getString("world"),
                config.getLong("created-at"),
                config.getLong("expires-at"),
                Instance.InstanceStatus.valueOf(config.getString("status", "ACTIVE")),
                config.getString("win-condition.type", "ctm")
        );

        // Cargar monumentos
        List<?> monumentsList = config.getList("win-condition.monuments", new ArrayList<>());
        if (monumentsList != null) {
            for (Object item : monumentsList) {
                if (item instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> monumentData = (java.util.Map<String, Object>) item;
                    instance.loadMonumentData(monumentData);
                }
            }
        }

        List<?> checkpointsList = config.getList("win-condition.checkpoints", new ArrayList<>());
        if (checkpointsList != null) {
            for (Object item : checkpointsList) {
                if (item instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> checkpointData = (java.util.Map<String, Object>) item;
                    instance.loadCheckpointData(checkpointData);
                }
            }
        }

        List<String> players = config.getStringList("players");
        for (String player : players) {
            instance.addPlayer(player);
        }

        return Optional.of(instance);
    }

    @Override
    public void delete(String instanceId) {
        File file = new File(instancesFolder, instanceId + ".yml");
        if (file.exists()) {
            file.delete();
        }
        updateIndex(instanceId, false);
    }

    @Override
    public List<Instance> getAll() {
        List<Instance> instances = new ArrayList<>();

        File[] files = instancesFolder.listFiles((dir, name) -> name.endsWith(".yml") && !name.equals("index.yml"));
        if (files != null) {
            for (File file : files) {
                String instanceId = file.getName().replace(".yml", "");
                load(instanceId).ifPresent(instances::add);
            }
        }

        return instances;
    }

    @Override
    public boolean exists(String instanceId) {
        return new File(instancesFolder, instanceId + ".yml").exists();
    }

    @Override
    public void clear() {
        File[] files = instancesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        if (indexFile.exists()) {
            indexFile.delete();
        }
    }

    private void updateIndex(String instanceId, boolean add) {
        FileConfiguration indexConfig;

        if (indexFile.exists()) {
            indexConfig = YamlConfiguration.loadConfiguration(indexFile);
        } else {
            indexConfig = new YamlConfiguration();
        }

        List<String> instanceIds;
        if (indexConfig.contains("instances")) {
            instanceIds = indexConfig.getStringList("instances");
        } else {
            instanceIds = new ArrayList<>();
        }

        if (add) {
            if (!instanceIds.contains(instanceId)) {
                instanceIds.add(instanceId);
            }
        } else {
            instanceIds.remove(instanceId);
        }

        indexConfig.set("instances", instanceIds);

        try {
            indexConfig.save(indexFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al actualizar index: " + e.getMessage());
        }
    }
}
