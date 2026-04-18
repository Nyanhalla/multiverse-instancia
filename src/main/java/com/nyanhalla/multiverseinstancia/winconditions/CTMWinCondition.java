package com.nyanhalla.multiverseinstancia.winconditions;

import com.nyanhalla.multiverseinstancia.MultiverseInstancia;
import com.nyanhalla.multiverseinstancia.core.Instance;
import com.nyanhalla.multiverseinstancia.core.InstanceManager;
import com.nyanhalla.multiverseinstancia.core.WinConditionEngine;
import com.nyanhalla.multiverseinstancia.models.Monument;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;

public class CTMWinCondition extends WinCondition {

    private final WinConditionEngine engine;

    public CTMWinCondition(MultiverseInstancia plugin, InstanceManager instanceManager) {
        super(plugin, instanceManager);
        this.engine = new WinConditionEngine(plugin, instanceManager);
    }

    @Override
    public void initializeInstance(Instance instance) {
        // Cargar monumentos desde la configuracion
        List<String> monumentBlocks = plugin.getConfig().getStringList("ctm.monument-blocks");

        // Escanear el mundo en busca de bloques de monumentos
        // Esto es una implementacion basica - en un caso real se usaria una lista predefinida
        WorldScanner scanner = new WorldScanner(plugin, instance);
        scanner.scanForMonuments(monumentBlocks);

        List<Monument> monuments = scanner.getFoundMonuments();
        for (Monument monument : monuments) {
            instance.addMonument(monument);
        }

        plugin.getLogger().info("CTM: " + monuments.size() + " monumentos inicializados para " + instance.getId());
    }

    @Override
    public void resetInstance(Instance instance) {
        // Resetear todos los monumentos
        for (Monument monument : instance.getMonuments()) {
            monument.setBroken(false);
            // Restaurar el bloque
            if (monument.getLocation().getBlock() != null) {
                monument.getLocation().getBlock().setType(monument.getMaterial());
            }
        }
    }

    @Override
    public boolean checkVictory(Instance instance) {
        boolean requireAll = plugin.getConfig().getBoolean("ctm.require-all", true);

        if (requireAll) {
            // Todos los monumentos deben estar rotos
            for (Monument monument : instance.getMonuments()) {
                if (!monument.isBroken()) {
                    return false;
                }
            }
            return true;
        } else {
            // Al menos uno roto (para mapas con monumentos opcionales)
            for (Monument monument : instance.getMonuments()) {
                if (monument.isBroken()) {
                    return true;
                }
            }
            return false;
        }
    }

    public void onBlockBreak(Instance instance, Block block) {
        Location blockLoc = block.getLocation();

        // Buscar si este bloque es un monumento
        for (Monument monument : instance.getMonuments()) {
            Location monumentLoc = monument.getLocation();
            if (monumentLoc.getBlockX() == blockLoc.getBlockX() &&
                monumentLoc.getBlockY() == blockLoc.getBlockY() &&
                monumentLoc.getBlockZ() == blockLoc.getBlockZ()) {

                monument.setBroken(true);
                plugin.getLogger().info("Monumento roto en " + instance.getId() + ": " + blockLoc);

                // Notificar a los jugadores
                for (String playerName : instance.getPlayers()) {
                    Player player = Bukkit.getPlayer(playerName);
                    if (player != null) {
                        player.sendMessage(com.nyanhalla.multiverseinstancia.utils.MessageUtils.colorize(
                                "§6§l¡Monumento destruido! §7(" + (instance.getMonuments().indexOf(monument) + 1) + "/" + instance.getMonuments().size() + ")"));
                    }
                }

                // Verificar victoria
                if (checkVictory(instance)) {
                    engine.triggerVictory(instance);
                }
                break;
            }
        }
    }

    // Inner class para escanear el mundo
    private static class WorldScanner {
        private final MultiverseInstancia plugin;
        private final Instance instance;
        private final java.util.List<Monument> foundMonuments = new java.util.ArrayList<>();

        WorldScanner(MultiverseInstancia plugin, Instance instance) {
            this.plugin = plugin;
            this.instance = instance;
        }

        void scanForMonuments(List<String> monumentBlocks) {
            // Implementacion basica - escanea una region alrededor del spawn
            org.bukkit.World world = instance.getWorld();
            if (world == null) return;

            List<Material> materials = monumentBlocks.stream()
                    .map(Material::getMaterial)
                    .toList();

            // Escanear region de 200 bloques desde spawn
            Location spawn = world.getSpawnLocation();
            int radius = 200;

            for (int x = spawn.getBlockX() - radius; x <= spawn.getBlockX() + radius; x += 16) {
                for (int y = spawn.getBlockY() - 64; y <= spawn.getBlockY() + 64; y += 16) {
                    for (int z = spawn.getBlockZ() - radius; z <= spawn.getBlockZ() + radius; z += 16) {
                        Block block = world.getBlockAt(x, y, z);
                        if (materials.contains(block.getType())) {
                            foundMonuments.add(new Monument(block.getType(), block.getLocation()));
                        }
                    }
                }
            }
        }

        List<Monument> getFoundMonuments() {
            return foundMonuments;
        }
    }
}
