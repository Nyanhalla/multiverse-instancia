package com.nyanhalla.multiverseinstances.core;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import org.bukkit.Bukkit;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.options.CloneWorldOptions;
import org.mvplugins.multiverse.core.world.options.DeleteWorldOptions;

public class WorldCloner {

    private final MultiverseInstances plugin;
    private final WorldManager worldManager;

    public WorldCloner(MultiverseInstances plugin, MultiverseCoreApi coreApi) {
        this.plugin = plugin;
        this.worldManager = coreApi.getWorldManager();
    }

    public boolean cloneWorld(String sourceWorldName, String newWorldName) {
        if (worldExists(newWorldName)) {
            plugin.getLogger().warning("El mundo de instancia " + newWorldName + " ya existe.");
            return false;
        }

        var sourceWorld = worldManager.getLoadedWorld(sourceWorldName);
        if (sourceWorld.isEmpty()) {
            plugin.getLogger().warning("El mundo base " + sourceWorldName + " no esta cargado o no pertenece a Multiverse.");
            return false;
        }

        CloneWorldOptions options = CloneWorldOptions.fromTo(sourceWorld.get(), newWorldName)
                .keepGameRule(true)
                .keepWorldConfig(true)
                .keepWorldBorder(true)
                .saveBukkitWorld(true);

        Attempt<LoadedMultiverseWorld, ?> attempt = worldManager.cloneWorld(options);
        if (attempt.isFailure()) {
            plugin.getLogger().warning("No se pudo clonar " + sourceWorldName + " como " + newWorldName
                    + ": " + attempt.getFailureReason());
            return false;
        }

        plugin.getLogger().info("Mundo " + sourceWorldName + " clonado como " + newWorldName + " usando la API de Multiverse.");
        return true;
    }

    public boolean deleteWorld(String worldName) {
        var managedWorld = worldManager.getWorld(worldName);
        if (managedWorld.isEmpty()) {
            managedWorld = worldManager.getUnloadedWorld(worldName);
        }

        if (managedWorld.isEmpty()) {
            return Bukkit.getWorld(worldName) == null;
        }

        Attempt<String, ?> attempt = worldManager.deleteWorld(DeleteWorldOptions.world(managedWorld.get()));
        if (attempt.isFailure()) {
            plugin.getLogger().warning("No se pudo eliminar el mundo " + worldName + ": " + attempt.getFailureReason());
            return false;
        }

        return true;
    }

    public boolean worldExists(String worldName) {
        return worldManager.isWorld(worldName) || worldManager.isUnloadedWorld(worldName) || Bukkit.getWorld(worldName) != null;
    }

    public MultiverseWorld getManagedWorld(String worldName) {
        var managedWorld = worldManager.getWorld(worldName);
        if (managedWorld.isDefined()) {
            return managedWorld.get();
        }

        managedWorld = worldManager.getUnloadedWorld(worldName);
        return managedWorld.getOrElse((MultiverseWorld) null);
    }
}
