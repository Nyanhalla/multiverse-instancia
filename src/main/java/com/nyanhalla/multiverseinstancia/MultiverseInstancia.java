package com.nyanhalla.multiverseinstancia;

import com.nyanhalla.multiverseinstancia.commands.InstanceCommand;
import com.nyanhalla.multiverseinstancia.commands.JoinCommand;
import com.nyanhalla.multiverseinstancia.commands.StatusCommand;
import com.nyanhalla.multiverseinstancia.commands.TeamCommand;
import com.nyanhalla.multiverseinstancia.core.InstanceManager;
import com.nyanhalla.multiverseinstancia.core.WinConditionEngine;
import com.nyanhalla.multiverseinstancia.listeners.BlockBreakListener;
import com.nyanhalla.multiverseinstancia.listeners.EntityDeathListener;
import com.nyanhalla.multiverseinstancia.listeners.PlayerMoveListener;
import com.nyanhalla.multiverseinstancia.listeners.PlayerQuitListener;
import com.nyanhalla.multiverseinstancia.listeners.WorldListener;
import com.nyanhalla.multiverseinstancia.storage.YAMLInstanceStore;
import com.nyanhalla.multiverseinstancia.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class MultiverseInstancia extends JavaPlugin {

    private static MultiverseInstancia instance;
    private InstanceManager instanceManager;
    private WinConditionEngine winConditionEngine;
    private YAMLInstanceStore instanceStore;

    @Override
    public void onEnable() {
        instance = this;

        // Verificar Multiverse-Core
        if (!isMultiverseEnabled()) {
            getLogger().severe("Multiverse-Core no encontrado. El plugin se deshabilitara.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Guardar configuracion por defecto
        saveDefaultConfig();

        // Inicializar componentes
        this.instanceStore = new YAMLInstanceStore(this);
        this.instanceManager = new InstanceManager(this, instanceStore);
        this.winConditionEngine = new WinConditionEngine(this, instanceManager);

        // Registrar comandos
        registerCommands();

        // Registrar listeners
        registerListeners();

        getLogger().info("Multiverse-Instancia ha sido habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        if (instanceManager != null) {
            instanceManager.saveAllInstances();
            instanceManager.shutdown();
        }
        instance = null;
    }

    private boolean isMultiverseEnabled() {
        return getServer().getPluginManager().getPlugin("Multiverse-Core") != null;
    }

    private void registerCommands() {
        getCommand("mi").setExecutor(new InstanceCommand(this));
        getCommand("mijoin").setExecutor(new JoinCommand(this));
        getCommand("mileave").setExecutor(new JoinCommand(this));
        getCommand("mistatus").setExecutor(new StatusCommand(this));
        getCommand("miteam").setExecutor(new TeamCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
    }

    public void reload() {
        reloadConfig();
        instanceManager.reload();
    }

    public static MultiverseInstancia getInstance() {
        return instance;
    }

    public InstanceManager getInstanceManager() {
        return instanceManager;
    }

    public WinConditionEngine getWinConditionEngine() {
        return winConditionEngine;
    }

    public YAMLInstanceStore getInstanceStore() {
        return instanceStore;
    }

    public void broadcast(String message) {
        Bukkit.broadcastMessage(MessageUtils.colorize(message));
    }
}
