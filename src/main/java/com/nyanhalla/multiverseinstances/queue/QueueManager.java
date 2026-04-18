package com.nyanhalla.multiverseinstances.queue;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import com.nyanhalla.multiverseinstances.core.Instance;
import com.nyanhalla.multiverseinstances.models.Team;
import com.nyanhalla.multiverseinstances.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Gestiona las colas de espera para instancias por mapa.
 */
public class QueueManager {

    private final MultiverseInstances plugin;

    // Cola por mapa: mapName -> lista ordenada de entries
    private final Map<String, Queue<QueueEntry>> queues;

    // Índice inverso: playerUUID -> entry (para búsqueda rápida)
    private final Map<UUID, QueueEntry> playerQueue;

    // Countdowns activos: mapName -> ticks restantes
    private final Map<String, Integer> activeCountdowns;

    public QueueManager(MultiverseInstances plugin) {
        this.plugin = plugin;
        this.queues = new ConcurrentHashMap<>();
        this.playerQueue = new ConcurrentHashMap<>();
        this.activeCountdowns = new ConcurrentHashMap<>();
    }

    /**
     * Añade un jugador a la cola de un mapa.
     */
    public boolean joinQueue(Player player, String mapName) {
        UUID uuid = player.getUniqueId();

        // Ya está en cola?
        if (playerQueue.containsKey(uuid)) {
            QueueEntry existing = playerQueue.get(uuid);
            player.sendMessage(MessageUtils.colorize(
                    "&cYa estas en la cola de &e" + existing.mapName() + "&c. Usa &6/mi queue leave &cprimero."));
            return false;
        }

        // Ya está en instancia?
        if (plugin.getInstanceManager().getPlayerInstance(uuid) != null) {
            player.sendMessage(MessageUtils.colorize(
                    "&cYa estas dentro de una instancia. Sal primero con &6/mi leave&c."));
            return false;
        }

        // Verificar que el mapa tiene plantilla
        if (!plugin.getTemplateManager().hasTemplate(mapName)) {
            player.sendMessage(MessageUtils.colorize(
                    "&cEl mapa &e" + mapName + " &cno tiene plantilla configurada."));
            return false;
        }

        // Verificar que el mundo base existe
        if (Bukkit.getWorld(mapName) == null) {
            player.sendMessage(MessageUtils.colorize(
                    "&cEl mundo base &e" + mapName + " &cno esta cargado."));
            return false;
        }

        // Obtener equipo del jugador
        Team team = plugin.getInstanceManager().getPlayerTeam(player);
        String teamName = team != null ? team.getName() : player.getName();

        QueueEntry entry = new QueueEntry(uuid, mapName, teamName);
        Queue<QueueEntry> queue = queues.computeIfAbsent(mapName, k -> new ConcurrentLinkedQueue<>());
        queue.add(entry);
        playerQueue.put(uuid, entry);

        int position = getQueuePosition(uuid);
        int total = queue.size();

        player.sendMessage(MessageUtils.colorize(""));
        player.sendMessage(MessageUtils.colorize("&6&l⏳ Cola de Instancia"));
        player.sendMessage(MessageUtils.colorize("&7━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(MessageUtils.colorize("&eMapa: &f" + mapName));
        player.sendMessage(MessageUtils.colorize("&eEquipo: &f" + teamName));
        player.sendMessage(MessageUtils.colorize("&ePosicion: &f#" + position + " &7de &f" + total));
        player.sendMessage(MessageUtils.colorize("&7━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(MessageUtils.colorize("&7Usa &6/mi queue leave &7para salir de la cola."));
        player.sendMessage(MessageUtils.colorize(""));

        // Notificar a otros en la cola
        notifyQueue(mapName, "&a+ &f" + player.getName() + " &7se ha unido a la cola &8(&f" + total + " &7en cola&8)");

        // Check si podemos auto-lanzar
        checkAutoLaunch(mapName);

        return true;
    }

    /**
     * Elimina un jugador de la cola.
     */
    public boolean leaveQueue(Player player) {
        UUID uuid = player.getUniqueId();
        QueueEntry entry = playerQueue.remove(uuid);

        if (entry == null) {
            player.sendMessage(MessageUtils.colorize("&cNo estas en ninguna cola."));
            return false;
        }

        Queue<QueueEntry> queue = queues.get(entry.mapName());
        if (queue != null) {
            queue.removeIf(e -> e.playerId().equals(uuid));
            if (queue.isEmpty()) {
                queues.remove(entry.mapName());
                activeCountdowns.remove(entry.mapName());
            }
        }

        player.sendMessage(MessageUtils.colorize("&aHas salido de la cola de &e" + entry.mapName() + "&a."));

        // Notificar a otros
        int remaining = queue != null ? queue.size() : 0;
        if (remaining > 0) {
            notifyQueue(entry.mapName(), "&c- &f" + player.getName() + " &7ha salido de la cola &8(&f" + remaining + " &7en cola&8)");
        }

        return true;
    }

    /**
     * Elimina un jugador de la cola sin mensaje (para uso interno).
     */
    public void removeFromQueue(UUID playerId) {
        QueueEntry entry = playerQueue.remove(playerId);
        if (entry != null) {
            Queue<QueueEntry> queue = queues.get(entry.mapName());
            if (queue != null) {
                queue.removeIf(e -> e.playerId().equals(playerId));
                if (queue.isEmpty()) {
                    queues.remove(entry.mapName());
                    activeCountdowns.remove(entry.mapName());
                }
            }
        }
    }

    /**
     * Fuerza el lanzamiento de una instancia con los jugadores en cola.
     */
    public boolean forceLaunch(String mapName, org.bukkit.command.CommandSender sender) {
        Queue<QueueEntry> queue = queues.get(mapName);
        if (queue == null || queue.isEmpty()) {
            sender.sendMessage(MessageUtils.colorize("&cNo hay jugadores en la cola de &e" + mapName + "&c."));
            return false;
        }

        return launchInstance(mapName, sender);
    }

    /**
     * Devuelve la posición del jugador en la cola (1-indexed).
     */
    public int getQueuePosition(UUID playerId) {
        QueueEntry entry = playerQueue.get(playerId);
        if (entry == null) return -1;

        Queue<QueueEntry> queue = queues.get(entry.mapName());
        if (queue == null) return -1;

        int position = 1;
        for (QueueEntry e : queue) {
            if (e.playerId().equals(playerId)) {
                return position;
            }
            position++;
        }
        return -1;
    }

    /**
     * Devuelve la entry de cola de un jugador, o null.
     */
    public QueueEntry getPlayerEntry(UUID playerId) {
        return playerQueue.get(playerId);
    }

    /**
     * Devuelve el tamaño de la cola de un mapa.
     */
    public int getQueueSize(String mapName) {
        Queue<QueueEntry> queue = queues.get(mapName);
        return queue != null ? queue.size() : 0;
    }

    /**
     * Devuelve todos los mapas con cola activa.
     */
    public Set<String> getActiveQueueMaps() {
        return new HashSet<>(queues.keySet());
    }

    /**
     * Devuelve las entries de un mapa en orden.
     */
    public List<QueueEntry> getQueueEntries(String mapName) {
        Queue<QueueEntry> queue = queues.get(mapName);
        return queue != null ? new ArrayList<>(queue) : Collections.emptyList();
    }

    /**
     * Verifica si se puede auto-lanzar.
     */
    private void checkAutoLaunch(String mapName) {
        int minPlayers = plugin.getConfig().getInt("queue.min-players", 1);
        int queueSize = getQueueSize(mapName);

        if (queueSize >= minPlayers) {
            int countdown = plugin.getConfig().getInt("queue.countdown", 10);
            if (countdown <= 0) {
                launchInstance(mapName, Bukkit.getConsoleSender());
            } else if (!activeCountdowns.containsKey(mapName)) {
                startCountdown(mapName, countdown);
            }
        }
    }

    /**
     * Inicia un countdown antes de lanzar.
     */
    private void startCountdown(String mapName, int seconds) {
        activeCountdowns.put(mapName, seconds);
        notifyQueue(mapName, "&6&l⚡ &eInstancia se creara en &f" + seconds + "s&e. Usa &6/mi queue leave &epara cancelar.");

        Bukkit.getScheduler().runTaskLater(plugin, () -> tickCountdown(mapName), 20L);
    }

    private void tickCountdown(String mapName) {
        Integer remaining = activeCountdowns.get(mapName);
        if (remaining == null) return;

        Queue<QueueEntry> queue = queues.get(mapName);
        if (queue == null || queue.isEmpty()) {
            activeCountdowns.remove(mapName);
            return;
        }

        remaining--;
        if (remaining <= 0) {
            activeCountdowns.remove(mapName);
            launchInstance(mapName, Bukkit.getConsoleSender());
        } else {
            activeCountdowns.put(mapName, remaining);
            if (remaining <= 5 || remaining % 5 == 0) {
                notifyQueue(mapName, "&6⏳ &eLanzamiento en &f" + remaining + "s&e...");
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> tickCountdown(mapName), 20L);
        }
    }

    /**
     * Lanza una instancia con los jugadores de la cola.
     */
    private boolean launchInstance(String mapName, org.bukkit.command.CommandSender sender) {
        Queue<QueueEntry> queue = queues.get(mapName);
        if (queue == null || queue.isEmpty()) return false;

        // Tomar el primer entry para el nombre de equipo
        QueueEntry leader = queue.peek();
        if (leader == null) return false;

        String teamName = leader.teamName();

        // Crear instancia
        Instance instance = plugin.getInstanceManager().createInstance(mapName, teamName, sender);
        if (instance == null) {
            notifyQueue(mapName, "&c&l✖ &cError al crear la instancia. Reintentando mas tarde...");
            return false;
        }

        // Mover todos los jugadores de la cola a la instancia
        List<QueueEntry> entries = new ArrayList<>(queue);
        for (QueueEntry entry : entries) {
            Player player = Bukkit.getPlayer(entry.playerId());
            if (player != null && player.isOnline()) {
                playerQueue.remove(entry.playerId());
                plugin.getInstanceManager().joinInstance(instance.getId(), player);
            }
        }

        // Limpiar cola
        queues.remove(mapName);
        activeCountdowns.remove(mapName);

        return true;
    }

    /**
     * Notifica a todos los jugadores en cola de un mapa.
     */
    private void notifyQueue(String mapName, String message) {
        Queue<QueueEntry> queue = queues.get(mapName);
        if (queue == null) return;

        for (QueueEntry entry : queue) {
            Player player = Bukkit.getPlayer(entry.playerId());
            if (player != null) {
                player.sendMessage(MessageUtils.colorize(message));
            }
        }
    }

    /**
     * Envía info de posición a todos los jugadores en cola (para la task).
     */
    public void sendQueueUpdates() {
        for (Map.Entry<String, Queue<QueueEntry>> mapEntry : queues.entrySet()) {
            String mapName = mapEntry.getKey();
            Queue<QueueEntry> queue = mapEntry.getValue();
            int total = queue.size();
            int minPlayers = plugin.getConfig().getInt("queue.min-players", 1);

            int position = 1;
            for (QueueEntry entry : queue) {
                Player player = Bukkit.getPlayer(entry.playerId());
                if (player != null) {
                    String actionBar = "§e⏳ Cola: §f#" + position + "§7/§f" + total
                            + " §7| §eMapa: §f" + mapName
                            + " §7| §eEspera: §f" + entry.getWaitTimeFormatted();

                    if (total < minPlayers) {
                        actionBar += " §7| §cFaltan §f" + (minPlayers - total) + " §cjugadores";
                    }

                    player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                            .legacySection().deserialize(actionBar));
                }
                position++;
            }
        }
    }

    /**
     * Limpieza al apagar.
     */
    public void shutdown() {
        for (Map.Entry<String, Queue<QueueEntry>> mapEntry : queues.entrySet()) {
            for (QueueEntry entry : mapEntry.getValue()) {
                Player player = Bukkit.getPlayer(entry.playerId());
                if (player != null) {
                    player.sendMessage(MessageUtils.colorize("&eLa cola se ha reiniciado por recarga del plugin."));
                }
            }
        }
        queues.clear();
        playerQueue.clear();
        activeCountdowns.clear();
    }
}
