package com.nyanhalla.multiverseinstances;

import com.nyanhalla.multiverseinstances.commands.InstanceCommand;
import com.nyanhalla.multiverseinstances.commands.JoinCommand;
import com.nyanhalla.multiverseinstances.commands.StatusCommand;
import com.nyanhalla.multiverseinstances.commands.TeamCommand;
import com.nyanhalla.multiverseinstances.core.InstanceManager;
import com.nyanhalla.multiverseinstances.core.WorldCloner;
import com.nyanhalla.multiverseinstances.core.WinConditionEngine;
import com.nyanhalla.multiverseinstances.listeners.BlockBreakListener;
import com.nyanhalla.multiverseinstances.listeners.BlockPlaceListener;
import com.nyanhalla.multiverseinstances.listeners.EntityDeathListener;
import com.nyanhalla.multiverseinstances.listeners.PlayerMoveListener;
import com.nyanhalla.multiverseinstances.listeners.PlayerQuitListener;
import com.nyanhalla.multiverseinstances.listeners.WorldListener;
import com.nyanhalla.multiverseinstances.queue.QueueManager;
import com.nyanhalla.multiverseinstances.scoreboard.InstanceScoreboard;
import com.nyanhalla.multiverseinstances.storage.YAMLInstanceStore;
import com.nyanhalla.multiverseinstances.storage.YAMLTemplateStore;
import com.nyanhalla.multiverseinstances.tasks.CheckpointParticleTask;
import com.nyanhalla.multiverseinstances.tasks.QueueUpdateTask;
import com.nyanhalla.multiverseinstances.tasks.ScoreboardUpdateTask;
import com.nyanhalla.multiverseinstances.templates.TemplateManager;
import com.nyanhalla.multiverseinstances.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import com.nyanhalla.multiverseinstances.integrations.WorldEditHook;
import com.nyanhalla.multiverseinstances.integrations.WorldGuardHook;

public class MultiverseInstances extends JavaPlugin {

    private static MultiverseInstances instance;
    private InstanceManager instanceManager;
    private WinConditionEngine winConditionEngine;
    private YAMLInstanceStore instanceStore;
    private YAMLTemplateStore templateStore;
    private WorldCloner worldCloner;
    private TemplateManager templateManager;
    private QueueManager queueManager;
    private InstanceScoreboard instanceScoreboard;

    @Override
    public void onEnable() {
        instance = this;

        if (!MultiverseCoreApi.isLoaded()) {
            getLogger().severe("Multiverse-Core 5 no esta disponible. El plugin se deshabilitara.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        MultiverseCoreApi coreApi = MultiverseCoreApi.get();
        this.worldCloner = new WorldCloner(this, coreApi);
        this.instanceStore = new YAMLInstanceStore(this);
        this.templateStore = new YAMLTemplateStore(this);
        WorldEditHook worldEditHook = getServer().getPluginManager().getPlugin("WorldEdit") != null ? new WorldEditHook() : null;
        WorldGuardHook worldGuardHook = getServer().getPluginManager().getPlugin("WorldGuard") != null ? new WorldGuardHook() : null;
        this.templateManager = new TemplateManager(this, templateStore, worldEditHook, worldGuardHook);
        this.instanceManager = new InstanceManager(this, instanceStore, worldCloner, templateManager);
        this.winConditionEngine = new WinConditionEngine(this, instanceManager);
        this.queueManager = new QueueManager(this);
        this.instanceScoreboard = new InstanceScoreboard(this);

        registerCommands();
        registerListeners();
        new CheckpointParticleTask(this).runTaskTimer(this, 20L, 20L);
        new QueueUpdateTask(this).runTaskTimer(this, 20L, 20L);
        new ScoreboardUpdateTask(this).runTaskTimer(this, 20L, 20L);

        getLogger().info("Multiverse-Instances ha sido habilitado correctamente usando la API de Multiverse 5.");
    }

    @Override
    public void onDisable() {
        if (instanceScoreboard != null) {
            instanceScoreboard.shutdown();
        }
        if (queueManager != null) {
            queueManager.shutdown();
        }
        if (instanceManager != null) {
            instanceManager.saveAllInstances();
            instanceManager.shutdown();
        }
        instance = null;
    }

    private void registerCommands() {
        InstanceCommand instanceCommand = new InstanceCommand(this);
        JoinCommand joinCommand = new JoinCommand(this);
        StatusCommand statusCommand = new StatusCommand(this);
        TeamCommand teamCommand = new TeamCommand(this);

        registerCommand("mi", instanceCommand, instanceCommand);
        registerCommand("mijoin", joinCommand, joinCommand);
        registerCommand("mileave", joinCommand, joinCommand);
        registerCommand("mistatus", statusCommand, null);
        registerCommand("miteam", teamCommand, teamCommand);
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor,
                                 org.bukkit.command.TabCompleter completer) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("No se pudo registrar el comando " + name + " porque no existe en plugin.yml.");
            return;
        }

        command.setExecutor(executor);
        if (completer != null) {
            command.setTabCompleter(completer);
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
    }

    public void reload() {
        reloadConfig();
        instanceManager.reload();
    }

    public static MultiverseInstances getInstance() {
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

    public YAMLTemplateStore getTemplateStore() {
        return templateStore;
    }

    public WorldCloner getWorldCloner() {
        return worldCloner;
    }

    public TemplateManager getTemplateManager() {
        return templateManager;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public InstanceScoreboard getInstanceScoreboard() {
        return instanceScoreboard;
    }

    public void broadcast(String message) {
        Bukkit.broadcastMessage(MessageUtils.colorize(message));
    }
}
