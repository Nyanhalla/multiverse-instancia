package com.nyanhalla.multiverseinstances.core;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import com.nyanhalla.multiverseinstances.models.Team;
import com.nyanhalla.multiverseinstances.storage.InstanceDataStore;
import com.nyanhalla.multiverseinstances.templates.TemplateManager;
import com.nyanhalla.multiverseinstances.utils.MessageUtils;
import com.nyanhalla.multiverseinstances.winconditions.CTMWinCondition;
import com.nyanhalla.multiverseinstances.winconditions.DungeonWinCondition;
import com.nyanhalla.multiverseinstances.winconditions.CheckpointWinCondition;
import com.nyanhalla.multiverseinstances.winconditions.WinCondition;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InstanceManager {

    private final MultiverseInstances plugin;
    private final InstanceDataStore dataStore;
    private final WorldCloner worldCloner;
    private final TemplateManager templateManager;
    private final Map<String, Instance> activeInstances;
    private final Map<UUID, String> playerInstances;
    private final Map<UUID, Team> playerTeams;
    private final Map<UUID, UUID> pendingInvites;
    private final Map<String, WinCondition> winConditions;

    public InstanceManager(MultiverseInstances plugin, InstanceDataStore dataStore,
                           WorldCloner worldCloner, TemplateManager templateManager) {
        this.plugin = plugin;
        this.dataStore = dataStore;
        this.worldCloner = worldCloner;
        this.templateManager = templateManager;
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
        activeInstances.clear();
        playerInstances.clear();

        List<Instance> instances = dataStore.getAll();
        for (Instance instance : instances) {
            if (instance.getStatus() == Instance.InstanceStatus.ACTIVE) {
                if (instance.getWorld() != null) {
                    activeInstances.put(instance.getId(), instance);
                    for (String player : instance.getPlayers()) {
                        UUID uuid = Bukkit.getOfflinePlayer(player).getUniqueId();
                        playerInstances.put(uuid, instance.getId());
                    }
                } else {
                    instance.setStatus(Instance.InstanceStatus.EXPIRED);
                    dataStore.save(instance);
                }
            }
        }
    }

    public Instance createInstance(String baseWorld, String teamName, CommandSender creator) {
        String normalizedTeamName = teamName == null ? "" : teamName.trim();
        if (normalizedTeamName.isEmpty()) {
            creator.sendMessage(MessageUtils.colorize("&cDebes indicar un nombre de equipo valido."));
            return null;
        }

        String prefix = plugin.getConfig().getString("instance.prefix", "inst_");
        int maxInstances = plugin.getConfig().getInt("instance.max-instances", 10);
        long defaultTtl = plugin.getConfig().getLong("instance.default-ttl", 3600);

        if (activeInstances.size() >= maxInstances) {
            creator.sendMessage(MessageUtils.colorize("&cSe ha alcanzado el maximo de instancias activas."));
            return null;
        }

        if (!worldCloner.worldExists(baseWorld) || Bukkit.getWorld(baseWorld) == null) {
            creator.sendMessage(MessageUtils.colorize("&cEl mundo base &e" + baseWorld + "&c no existe, no esta cargado o no pertenece a Multiverse."));
            return null;
        }

        String winConditionType = templateManager.getWinConditionType(baseWorld);
        if (winConditionType == null) {
            creator.sendMessage(MessageUtils.colorize("&cEl mundo base &e" + baseWorld + "&c no tiene plantilla configurada. Usa &6/mi template&c primero."));
            return null;
        }

        String instanceId = generateInstanceId(baseWorld, normalizedTeamName);
        String worldName = generateWorldName(prefix, baseWorld, normalizedTeamName);
        long expiresAt = defaultTtl > 0 ? System.currentTimeMillis() + (defaultTtl * 1000) : 0;

        Instance instance = new Instance(
                instanceId,
                baseWorld,
                normalizedTeamName + " - " + baseWorld,
                normalizedTeamName,
                worldName,
                System.currentTimeMillis(),
                expiresAt,
                Instance.InstanceStatus.ACTIVE,
                winConditionType
        );

        if (!worldCloner.cloneWorld(baseWorld, worldName)) {
            creator.sendMessage(MessageUtils.colorize("&cNo se pudo crear la instancia para &e" + baseWorld + "&c."));
            return null;
        }

        if (!templateManager.applyTemplate(instance)) {
            worldCloner.deleteWorld(worldName);
            creator.sendMessage(MessageUtils.colorize("&cNo se pudo aplicar la plantilla del mapa base &e" + baseWorld + "&c."));
            return null;
        }

        templateManager.createInstanceRegions(instance);

        dataStore.save(instance);
        activeInstances.put(instanceId, instance);

        WinCondition wc = winConditions.get(winConditionType);
        if (wc != null) {
            wc.initializeInstance(instance);
        }

        dataStore.save(instance);
        creator.sendMessage(MessageUtils.colorize("&aInstancia &e" + instanceId + "&a creada para el equipo &b" + normalizedTeamName + "&a."));
        return instance;
    }

    private String generateInstanceId(String baseWorld, String teamName) {
        String normalizedBaseWorld = normalizeSegment(baseWorld);
        String normalizedTeam = normalizeSegment(teamName);
        String baseId = normalizedBaseWorld + "_" + normalizedTeam;
        String candidate = baseId;
        int suffix = 1;

        while (activeInstances.containsKey(candidate) || dataStore.exists(candidate)) {
            suffix++;
            candidate = baseId + "_" + suffix;
        }

        return candidate;
    }

    private String generateWorldName(String prefix, String baseWorld, String teamName) {
        String normalizedBaseWorld = normalizeSegment(baseWorld);
        String normalizedTeam = normalizeSegment(teamName);
        String baseName = prefix + normalizedBaseWorld + "_" + normalizedTeam;
        String candidate = baseName;
        int suffix = 1;

        while (worldCloner.worldExists(candidate)) {
            suffix++;
            candidate = baseName + "_" + suffix;
        }

        return candidate;
    }

    private String normalizeSegment(String input) {
        String normalized = input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        return normalized.isEmpty() ? "instance" : normalized;
    }

    public boolean deleteInstance(String instanceId, CommandSender sender) {
        Instance instance = activeInstances.get(instanceId);
        if (instance == null) {
            sender.sendMessage(MessageUtils.colorize("&cInstancia &e" + instanceId + "&c no encontrada."));
            return false;
        }

        removeAllPlayersFromInstance(instance, true, true);
        templateManager.deleteInstanceRegions(instance);

        if (plugin.getConfig().getBoolean("instance.reset-on-cleanup", true)) {
            if (!worldCloner.deleteWorld(instance.getWorldName())) {
                sender.sendMessage(MessageUtils.colorize("&cNo se pudo eliminar el mundo de la instancia &e" + instanceId + "&c."));
                return false;
            }
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

        removeAllPlayersFromInstance(instance, true, true);
        templateManager.deleteInstanceRegions(instance);

        if (!worldCloner.deleteWorld(instance.getWorldName())) {
            sender.sendMessage(MessageUtils.colorize("&cNo se pudo eliminar el mundo actual de la instancia."));
            return false;
        }

        if (!worldCloner.cloneWorld(instance.getBaseWorld(), instance.getWorldName())) {
            sender.sendMessage(MessageUtils.colorize("&cNo se pudo recrear la instancia desde el mundo base."));
            return false;
        }

        if (!templateManager.applyTemplate(instance)) {
            sender.sendMessage(MessageUtils.colorize("&cNo se pudo reaplicar la plantilla sobre la instancia."));
            return false;
        }

        templateManager.createInstanceRegions(instance);

        WinCondition wc = winConditions.get(instance.getWinConditionType());
        if (wc != null) {
            wc.resetInstance(instance);
        }

        dataStore.save(instance);
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

        Instance currentInstance = getPlayerInstance(player.getUniqueId());
        if (currentInstance != null && !currentInstance.getId().equals(instanceId)) {
            leaveInstance(player);
        }

        instance.addPlayer(player.getName());
        playerInstances.put(player.getUniqueId(), instanceId);

        World world = instance.getWorld();
        if (world != null) {
            player.teleport(world.getSpawnLocation());
        } else {
            player.sendMessage(MessageUtils.colorize("&cEl mundo de la instancia no esta cargado."));
            instance.removePlayer(player.getName());
            playerInstances.remove(player.getUniqueId());
            return false;
        }

        dataStore.save(instance);
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

            World baseWorld = Bukkit.getWorld(instance.getBaseWorld());
            if (baseWorld != null) {
                player.teleport(baseWorld.getSpawnLocation());
            }

            dataStore.save(instance);
        }

        playerInstances.remove(uuid);
        player.sendMessage(MessageUtils.colorize("&aHas salido de la instancia."));
        return true;
    }

    public void handlePlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        Instance instance = getPlayerInstance(uuid);
        if (instance == null) {
            return;
        }

        instance.savePlayerLocation(player.getName(), player.getLocation());
        instance.removePlayer(player.getName());
        playerInstances.remove(uuid);

        if (instance.isEmpty()) {
            int autoCloseEmpty = plugin.getConfig().getInt("instance.auto-close-empty", 300);
            if (autoCloseEmpty > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Instance current = activeInstances.get(instance.getId());
                    if (current != null && current.isEmpty()) {
                        plugin.getLogger().info("Cerrando instancia vacia: " + current.getId());
                        deleteInstance(current.getId(), Bukkit.getConsoleSender());
                    }
                }, autoCloseEmpty * 20L);
            }
        }

        dataStore.save(instance);
    }

    public void onVictory(Instance instance) {
        instance.setStatus(Instance.InstanceStatus.COMPLETED);
        dataStore.save(instance);

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
        String normalizedTeamName = teamName == null ? "" : teamName.trim();
        if (normalizedTeamName.isEmpty()) {
            leader.sendMessage(MessageUtils.colorize("&cDebes indicar un nombre de equipo valido."));
            return false;
        }

        if (playerTeams.containsKey(leader.getUniqueId())) {
            leader.sendMessage(MessageUtils.colorize("&cYa tienes un equipo."));
            return false;
        }

        Team team = new Team(normalizedTeamName);
        team.addMember(leader.getUniqueId());
        playerTeams.put(leader.getUniqueId(), team);

        leader.sendMessage(MessageUtils.colorize("&aEquipo &e" + normalizedTeamName + "&a creado."));
        return true;
    }

    public void inviteToTeam(Player inviter, Player target) {
        Team team = playerTeams.get(inviter.getUniqueId());
        if (team == null) {
            inviter.sendMessage(MessageUtils.colorize("&cNo tienes un equipo."));
            return;
        }

        if (inviter.getUniqueId().equals(target.getUniqueId())) {
            inviter.sendMessage(MessageUtils.colorize("&cNo puedes invitarte a ti mismo."));
            return;
        }

        if (playerTeams.containsKey(target.getUniqueId())) {
            inviter.sendMessage(MessageUtils.colorize("&cEse jugador ya pertenece a un equipo."));
            return;
        }

        pendingInvites.put(target.getUniqueId(), inviter.getUniqueId());
        inviter.sendMessage(MessageUtils.colorize("&aInvitacion enviada a &e" + target.getName() + "&a."));
        target.sendMessage(MessageUtils.colorize("&e" + inviter.getName() + "&a te ha invitado a unirte a &b" + team.getName() + "&a. Usa &c/mi team accept &apara aceptar."));
    }

    public boolean acceptInvite(Player player) {
        if (playerTeams.containsKey(player.getUniqueId())) {
            player.sendMessage(MessageUtils.colorize("&cYa perteneces a un equipo."));
            pendingInvites.remove(player.getUniqueId());
            return false;
        }

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

    public Instance getInstanceByWorldName(String worldName) {
        if (worldName == null) {
            return null;
        }

        for (Instance instance : activeInstances.values()) {
            if (worldName.equals(instance.getWorldName())) {
                return instance;
            }
        }
        return null;
    }

    public void listInstances(CommandSender sender) {
        List<Instance> instances = getActiveInstances().stream()
                .sorted(Comparator.comparing(Instance::getId))
                .toList();

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
                removeAllPlayersFromInstance(instance, false, true);
                instance.setStatus(Instance.InstanceStatus.EXPIRED);
                dataStore.delete(instance.getId());
                activeInstances.remove(instance.getId());
                purged++;
            }
        }
        sender.sendMessage(MessageUtils.colorize("&aSe han eliminado &e" + purged + "&a instancias huerfanas."));
    }

    private void removeAllPlayersFromInstance(Instance instance, boolean teleportOnlinePlayers, boolean clearMappings) {
        World fallbackWorld = Bukkit.getWorld(instance.getBaseWorld());
        for (String playerName : new HashSet<>(instance.getPlayers())) {
            Player onlinePlayer = Bukkit.getPlayerExact(playerName);
            if (teleportOnlinePlayers && onlinePlayer != null && fallbackWorld != null) {
                onlinePlayer.teleport(fallbackWorld.getSpawnLocation());
                onlinePlayer.sendMessage(MessageUtils.colorize("&eLa instancia &6" + instance.getId() + " &eha sido cerrada."));
            }

            instance.removePlayer(playerName);
            if (clearMappings) {
                playerInstances.remove(Bukkit.getOfflinePlayer(playerName).getUniqueId());
            }
        }
    }
}
