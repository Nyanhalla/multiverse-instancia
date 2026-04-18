package com.nyanhalla.multiverseinstancia.core;

import com.nyanhalla.multiverseinstancia.MultiverseInstancia;
import com.nyanhalla.multiverseinstancia.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;

public class WorldCloner {

    public static boolean cloneWorld(MultiverseInstancia plugin, String sourceWorld, String newWorldName) {
        try {
            // Verificar que el mundo fuente existe
            World source = Bukkit.getWorld(sourceWorld);
            if (source == null) {
                plugin.getLogger().warning("El mundo fuente " + sourceWorld + " no existe.");
                return false;
            }

            // Verificar que el mundo nuevo no exista
            if (Bukkit.getWorld(newWorldName) != null) {
                plugin.getLogger().warning("El mundo " + newWorldName + " ya existe.");
                return false;
            }

            // Usar comando de Multiverse para clonar
            ConsoleCommandSender console = Bukkit.getConsoleSender();
            boolean result = Bukkit.dispatchCommand(console, "mv clone " + sourceWorld + " " + newWorldName);

            if (result) {
                plugin.getLogger().info("Mundo " + sourceWorld + " clonado como " + newWorldName);
            }

            return result;

        } catch (Exception e) {
            plugin.getLogger().severe("Error al clonar mundo: " + e.getMessage());
            return false;
        }
    }

    public static boolean deleteWorld(MultiverseInstancia plugin, String worldName) {
        try {
            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                return true; // Ya no existe
            }

            // Guardar mundo
            world.save();

            // Des cargar usando comando mv
            ConsoleCommandSender console = Bukkit.getConsoleSender();
            boolean unloaded = Bukkit.dispatchCommand(console, "mv unload " + worldName);

            if (!unloaded) {
                plugin.getLogger().warning("No se pudo descargar el mundo: " + worldName);
            }

            // Eliminar usando comando mv
            return Bukkit.dispatchCommand(console, "mv delete " + worldName);

        } catch (Exception e) {
            plugin.getLogger().severe("Error al eliminar mundo: " + e.getMessage());
            return false;
        }
    }

    public static boolean worldExists(String worldName) {
        return Bukkit.getWorld(worldName) != null;
    }

    public static World getWorld(String worldName) {
        return Bukkit.getWorld(worldName);
    }
}
