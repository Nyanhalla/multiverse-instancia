package com.nyanhalla.multiverseinstancia.winconditions;

import com.nyanhalla.multiverseinstancia.MultiverseInstancia;
import com.nyanhalla.multiverseinstancia.core.Instance;
import com.nyanhalla.multiverseinstancia.core.InstanceManager;
import com.nyanhalla.multiverseinstancia.core.WinConditionEngine;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DungeonWinCondition extends WinCondition {

    private final WinConditionEngine engine;
    private final java.util.Map<String, BossBar> activeBossBars = new java.util.concurrent.ConcurrentHashMap<>();

    public DungeonWinCondition(MultiverseInstancia plugin, InstanceManager instanceManager) {
        super(plugin, instanceManager);
        this.engine = new WinConditionEngine(plugin, instanceManager);
    }

    @Override
    public void initializeInstance(Instance instance) {
        // Verificar si hay un boss configurado
        if (!plugin.getConfig().getBoolean("dungeon.enabled", true)) {
            return;
        }

        String displayName = plugin.getConfig().getString("dungeon.boss-bar.display-name", "§c§lDungeon Boss");

        // La creacion de la boss bar se hara cuando aparezca el boss en el mundo
        plugin.getLogger().info("Dungeon: Boss bar configurado para " + instance.getId());
    }

    @Override
    public void resetInstance(Instance instance) {
        // Remover boss bars activas
        activeBossBars.remove(instance.getId());
    }

    @Override
    public boolean checkVictory(Instance instance) {
        // Victoria cuando el boss muere (verificado en EntityDeathListener)
        return true; // Se llama desde el listener
    }

    public void onBossDeath(Instance instance) {
        boolean victoryOnDeath = plugin.getConfig().getBoolean("dungeon.victory-on-death", true);

        if (victoryOnDeath) {
            // Notificar a los jugadores
            for (String playerName : instance.getPlayers()) {
                Player player = Bukkit.getPlayer(playerName);
                if (player != null) {
                    player.sendMessage(com.nyanhalla.multiverseinstancia.utils.MessageUtils.colorize("§6§l¡BOSS DERROTADO!"));
                }
            }

            engine.triggerVictory(instance);
        }

        // Remover boss bar
        activeBossBars.remove(instance.getId());
    }

    public void onBossDamaged(Instance instance, Entity boss, double damage) {
        if (!plugin.getConfig().getBoolean("dungeon.boss-bar.health-bar-sync", true)) {
            return;
        }

        if (!(boss instanceof LivingEntity livingBoss)) {
            return;
        }

        BossBar bossBar = activeBossBars.get(instance.getId());
        if (bossBar == null) {
            String displayName = plugin.getConfig().getString("dungeon.boss-bar.display-name", "§c§lDungeon Boss");
            bossBar = Bukkit.createBossBar(displayName, BarColor.RED, BarStyle.SEGMENTED_10);
            activeBossBars.put(instance.getId(), bossBar);

            // Agregar jugadores
            for (String playerName : instance.getPlayers()) {
                Player player = Bukkit.getPlayer(playerName);
                if (player != null) {
                    bossBar.addPlayer(player);
                }
            }
        }

        double maxHealth = livingBoss.getMaxHealth();
        double currentHealth = livingBoss.getHealth();
        double progress = Math.max(0, currentHealth / maxHealth);

        bossBar.setProgress(progress);
    }

    public void registerPlayer(Player player, Instance instance) {
        BossBar bossBar = activeBossBars.get(instance.getId());
        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
    }

    public void unregisterPlayer(Player player, Instance instance) {
        BossBar bossBar = activeBossBars.get(instance.getId());
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }
}
