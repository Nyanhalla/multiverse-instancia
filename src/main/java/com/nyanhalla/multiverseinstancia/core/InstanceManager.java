package com.nyanhalla.multiverseinstancia.core;

import com.nyanhalla.multiverseinstancia.MultiverseInstancia;
import com.nyanhalla.multiverseinstancia.models.Team;
import com.nyanhalla.multiverseinstancia.storage.InstanceDataStore;
import com.nyanhalla.multiverseinstancia.utils.MessageUtils;
import com.nyanhalla.multiverseinstancia.winconditions.WinCondition;
import com.nyanhalla.multiverseinstancia.winconditions.CTMWinCondition;
import com.nyanhalla.multiverseinstancia.winconditions.DungeonWinCondition;
import com.nyanhalla.multiverseinstancia.winconditions.CheckpointWinCondition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InstanceManager {

    private final MultiverseInstancia plugin;
    private final InstanceDataStore dataStore;
    private final Map<String, Instance> activeInstances;
    private final Map<UUID, String> playerInstances;
    private final Map<UUID, Team> playerTeams;
    private final Map<UUID, UUID> pendingInvites;
    private final Map<String, WinCondition> winConditions;

    public InstanceManager(MultiverseInstancia plugin, InstanceDataStore dataStore) {
        this.plugin = plugin;
        this.dataStore = dataStore;
        this.activeInstances = new ConcurrentHashMap<>();
        this.playerInstances = new ConcurrentHashMap<>();
        this.playerTeams = new ConcurrentHashMap<>();
        this.pendingInvites = new ConcurrentHashMap<>();
        this.winConditions = new ConcurrentHashMap<>();

        initializeWinConditions();
        loadInstances();
    }

    private void initializeWinConditions() {
        winConditions.put("ctm", new CTMWinCondition(plugin, this));
        winConditions.put("dungeon", new DungeonWinCondition(plugin, this));
        winConditions.put("checkpoint", new CheckpointWinCondition(plugin, this));
    }

    private void loadInstances() {
        List<Instance> instances = dataStore.getAll();
        for (Instance instance : instances) {
            if (instance.getStatus() == Instance.InstanceStatus.ACTIVE) {
                // Verificar si el mundo aun existe
                if (instance.getWorld() != null) {
                    activeInstances.put(instance.getId(), instance);
                    for (String player : instance.getPlayers()) {
                        UUID uuid = Bukkit.getOfflinePlayer(player).getUniqueId();
                        playerInstances.put(uuid, instance.getId());
                    }
                } else {
                    // Mundo no existe - marcar como huerfano
                    instance.setStatus(Instance.InstanceStatus.EXPIRED);
                    dataStore.save(instance);
                }
            }
        }
    }

    public Instance createInstance(String baseWorld, String teamName, CommandSender creator) {
        String prefix = plugin.getConfig().getString("instance.prefix", "inst_");
        int maxInstances = plugin.getConfig().getInt("instance.max-instances", 10);
        long defaultTtl = plugin.getConfig().getLong("instance.default-ttl", 3600);

        if (activeInstances.size() >= maxInstances) {
            creator.sendMessage(MessageUtils.colorize("&cSe ha alcanzado el maximo de instancias activas."));
            return null;
        }

        // Verificar que el mundo base existe
        World baseWorldObj = Bukkit.getWorld(baseWorld);
        if (baseWorldObj == null) {
            creator.sendMessage(MessageUtils.colorize("&cEl mundo base &e" + baseWorld + "&c no existe."));
            return null;
        }

        // Generar ID unico
        String instanceId = generateInstanceId(baseWorld, teamName);
        String worldName = prefix + baseWorld + "_" + teamName + "_" + System.currentTimeMillis() % 1000;

        // Obtener tipo de win condition del mundo
        String winConditionType = getWinConditionType(baseWorld);

        long expiresAt = defaultTtl > 0 ? System.currentTimeMillis() + (defaultTtl * 1000) : 0;

        Instance instance = new Instance(
                instanceId,
                baseWorld,
                "§c" + teamName + " §7- " + baseWorld,
                teamName,
                worldName,
                System.currentTimeMillis(),
                expiresAt,
                Instance.InstanceStatus.ACTIVE,
                winConditionType
        );

        // Clonar el mundo usando WorldCloner
        WorldCloner.cloneWorld(plugin, baseWorld, worldName);

        // Guardar y registrar
        dataStore.save(instance);
        activeInstances.put(instanceId, instance);

        // Configurar win condition inicial
        WinCondition wc = winConditions.get(winConditionType);
        if (wc != null) {
            wc.initializeInstance(instance);
        }

        creator.sendMessage(MessageUtils.colorize("&aInstancia &e" + instanceId + "&a creada para el equipo &b" + teamName + "&a."));
        return instance;
    }

    private String generateInstanceId(String baseWorld, String teamName) {
        return baseWorld + "_" + teamName + "_" + (activeInstances.size() + 1);
    }

    private String getWinConditionType(String baseWorld) {
        // Por defecto CTM, luego se puede extender para leer de world-config
        return "ctm";
    }

    public boolean deleteInstance(String instanceId, CommandSender sender) {
        Instance instance = activeInstances.get(instanceId);
        if (instance == null) {
            sender.sendMessage(MessageUtils.colorize("&cInstancia &e" + instanceId + "&c no encontrada."));
            return false;
        }

        // Desregistrar jugadores
        for (String playerName : instance.getPlayers()) {
            UUID uuid = Bukkit.getOfflinePlayer(playerName).getUniqueId();
            playerInstances.remove(uuid);
        }

        // Eliminar mundo si esta configurado
        if (plugin.getConfig().getBoolean("instance.reset-on-cleanup", true)) {
            WorldCloner.deleteWorld(plugin, instance.getWorldName());
        }

        instance.setStatus(Instance.InstanceStatus.EXPIRED);
        dataStore.delete(instanceId);
        activeInstances.remove(instanceId);

        sender.sendMessage(MessageUtils.colorize("&aInstancia &e" + instanceId + "&a eliminada."));
        return true;
    }

    public boolean forceReset(String instanceId, CommandSender sender) {
        Instance instance = activeInstances.get(instanceId);
        if (instance == null) {
            sender.sendMessage(MessageUtils.colorize("&cInstancia &e" + instanceId + "&c no encontrada."));
            return false;
        }

        // Eliminar mundo actual
        WorldCloner.deleteWorld(plugin, instance.getWorldName());

        // Recrear desde el mundo base
        WorldCloner.cloneWorld(plugin, instance.getBaseWorld(), instance.getWorldName());

        // Resetear estado de win condition
        WinCondition wc = winConditions.get(instance.getWinConditionType());
        if (wc != null) {
            wc.resetInstance(instance);
        }

        sender.sendMessage(MessageUtils.colorize("&aInstancia &e" + instanceId + "&a reseteada."));
        return true;
    }

    public boolean joinInstance(String instanceId, Player player) {
        Instance instance = activeInstances.get(instanceId);
        if (instance == null) {
            player.sendMessage(MessageUtils.colorize("&cInstancia no encontrada."));
            return false;
        }

        if (instance.getStatus() != Instance.InstanceStatus.ACTIVE) {
            player.sendMessage(MessageUtils.colorize("&cEsta instancia no esta activa."));
            return false;
        }

        // Guardar inventario actual antes de cambiar
        // TODO: Implementar guardado de inventario

        instance.addPlayer(player.getName());
        playerInstances.put(player.getUniqueId(), instanceId);

        // Teleportear al mundo
        World world = instance.getWorld();
        if (world != null) {
            player.teleport(world.getSpawnLocation());
        }

        player.sendMessage(MessageUtils.colorize("&aTe has unido a la instancia &e" + instanceId + "&a."));
        return true;
    }

    public boolean leaveInstance(Player player) {
        UUID uuid = player.getUniqueId();
        String instanceId = playerInstances.get(uuid);

        if (instanceId == null) {
            player.sendMessage(MessageUtils.colorize("&cNo estas en ninguna instancia."));
            return false;
        }

        Instance instance = activeInstances.get(instanceId);
        if (instance != null) {
            instance.removePlayer(player.getName());

            // Restaurar inventario
            // TODO: Implementar restauracion de inventario

            // Teleportear al mundo base
            World baseWorld = Bukkit.getWorld(instance.getBaseWorld());
            if (baseWorld != null) {
                player.teleport(baseWorld.getSpawnLocation());
            }
        }

        playerInstances.remove(uuid);
        player.sendMessage(MessageUtils.colorize("&aHas salido de la instancia."));
        return true;
    }

    public void onVictory(Instance instance) {
        String team = instance.getTeam();
        instance.setStatus(Instance.InstanceStatus.COMPLETED);

        // Ejecutar comandos de victoria
        if (plugin.getConfig().getBoolean("instance.auto-cleanup-on-victory", true)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                deleteInstance(instance.getId(), Bukkit.getConsoleSender());
            }, 100L);
        }
    }

    public Instance getInstance(String instanceId) {
        return activeInstances.get(instanceId);
    }

    public Instance getPlayerInstance(UUID playerId) {
        String instanceId = playerInstances.get(playerId);
        return instanceId != null ? activeInstances.get(instanceId) : null;
    }

    public List<Instance> getActiveInstances() {
        return new ArrayList<>(activeInstances.values());
    }

    public void saveAllInstances() {
        for (Instance instance : activeInstances.values()) {
            dataStore.save(instance);
        }
    }

    public void shutdown() {
        saveAllInstances();
        activeInstances.clear();
        playerInstances.clear();
    }

    public void reload() {
        loadInstances();
    }

    // Team management
    public boolean createTeam(String teamName, Player leader) {
        if (playerTeams.containsKey(leader.getUniqueId())) {
            leader.sendMessage(MessageUtils.colorize("&cYa tienes un equipo."));
            return false;
        }

        Team team = new Team(teamName);
        team.addMember(leader.getUniqueId());
        playerTeams.put(leader.getUniqueId(), team);

        leader.sendMessage(MessageUtils.colorize("&aEquipo &e" + teamName + "&a creado."));
        return true;
    }

    public void inviteToTeam(Player inviter, Player target) {
        Team team = playerTeams.get(inviter.getUniqueId());
        if (team == null) {
            inviter.sendMessage(MessageUtils.colorize("&cNo tienes un equipo."));
            return;
        }

        pendingInvites.put(target.getUniqueId(), inviter.getUniqueId());
        inviter.sendMessage(MessageUtils.colorize("&aInvitacion enviada a &e" + target.getName() + "&a."));
        target.sendMessage(MessageUtils.colorize("&e" + inviter.getName() + "&a te ha invitado a unirte a &b" + team.getName() + "&a. Usa &c/mi team accept &apara aceptar."));
    }

    public boolean acceptInvite(Player player) {
        UUID inviterId = pendingInvites.remove(player.getUniqueId());
        if (inviterId == null) {
            player.sendMessage(MessageUtils.colorize("&cNo tienes invitaciones pendientes."));
            return false;
        }

        Player inviter = Bukkit.getPlayer(inviterId);
        Team team = playerTeams.get(inviterId);
        if (team == null) {
            player.sendMessage(MessageUtils.colorize("&cEl equipo ya no existe."));
            return false;
        }

        team.addMember(player.getUniqueId());
        playerTeams.put(player.getUniqueId(), team);

        player.sendMessage(MessageUtils.colorize("&aTe has unido al equipo &b" + team.getName() + "&a."));
        return true;
    }

    public boolean leaveTeam(Player player) {
        Team team = playerTeams.get(player.getUniqueId());
        if (team == null) {
            player.sendMessage(MessageUtils.colorize("&cNo tienes un equipo."));
            return false;
        }

        team.removeMember(player.getUniqueId());
        playerTeams.remove(player.getUniqueId());

        if (team.getSize() == 0) {
            // Eliminar equipo si queda vacio
            playerTeams.values().removeIf(t -> t == team);
        }

        player.sendMessage(MessageUtils.colorize("&aHas dejado el equipo."));
        return true;
    }

    public Team getPlayerTeam(Player player) {
        return playerTeams.get(player.getUniqueId());
    }

    public WinCondition getWinCondition(String type) {
        return winConditions.get(type);
    }

    public void listInstances(CommandSender sender) {
        List<Instance> instances = getActiveInstances();

        if (instances.isEmpty()) {
            sender.sendMessage(MessageUtils.colorize("&7No hay instancias activas."));
            return;
        }

        sender.sendMessage(MessageUtils.colorize("&6&lInstancias Activas (&e" + instances.size() + "&6)"));
        sender.sendMessage(MessageUtils.colorize("&7" + "=".repeat(40)));

        for (Instance instance : instances) {
            String status = instance.getStatus() == Instance.InstanceStatus.ACTIVE ? "&aActiva" : "&c" + instance.getStatus();
            sender.sendMessage(MessageUtils.colorize(
                    "&e" + instance.getId() + " &7- &b" + instance.getTeam() +
                    " &7- &f" + instance.getPlayerCount() + " jugadores &7- " + status
            ));
        }
    }

    public void sendInstanceInfo(CommandSender sender, String instanceId) {
        Instance instance = activeInstances.get(instanceId);
        if (instance == null) {
            sender.sendMessage(MessageUtils.colorize("&cInstancia no encontrada."));
            return;
        }

        sender.sendMessage(MessageUtils.colorize("&6&lInformacion de Instancia"));
        sender.sendMessage(MessageUtils.colorize("&7" + "=".repeat(40)));
        sender.sendMessage(MessageUtils.colorize("&eID: &f" + instance.getId()));
        sender.sendMessage(MessageUtils.colorize("&eEquipo: &f" + instance.getTeam()));
        sender.sendMessage(MessageUtils.colorize("&eMundo Base: &f" + instance.getBaseWorld()));
        sender.sendMessage(MessageUtils.colorize("&eMundo Instancia: &f" + instance.getWorldName()));
        sender.sendMessage(MessageUtils.colorize("&eWin Condition: &f" + instance.getWinConditionType()));
        sender.sendMessage(MessageUtils.colorize("&eEstado: &f" + instance.getStatus()));
        sender.sendMessage(MessageUtils.colorize("&eJugadores: &f" + String.join(", ", instance.getPlayers())));
    }

    public void purgeOrphans(CommandSender sender) {
        int purged = 0;
        for (Instance instance : new ArrayList<>(activeInstances.values())) {
            if (instance.getWorld() == null) {
                instance.setStatus(Instance.InstanceStatus.EXPIRED);
                dataStore.delete(instance.getId());
                activeInstances.remove(instance.getId());
                purged++;
            }
        }
        sender.sendMessage(MessageUtils.colorize("&aSe han eliminado &e" + purged + "&a instancias huerfanas."));
    }
}
