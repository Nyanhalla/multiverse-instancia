package com.nyanhalla.multiverseinstances.scoreboard;

import com.nyanhalla.multiverseinstances.MultiverseInstances;
import com.nyanhalla.multiverseinstances.core.Instance;
import com.nyanhalla.multiverseinstances.models.Checkpoint;
import com.nyanhalla.multiverseinstances.models.Monument;
import com.nyanhalla.multiverseinstances.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona scoreboards de sidebar para jugadores dentro de instancias.
 * Muestra: info del mapa, equipo con vida, y progreso de win condition.
 */
public class InstanceScoreboard {

    private final MultiverseInstances plugin;

    // Jugadores con scoreboard activo
    private final Set<UUID> activePlayers;

    // Jugadores que han ocultado el scoreboard
    private final Set<UUID> hiddenPlayers;

    private static final String OBJECTIVE_NAME = "mi_instance";

    public InstanceScoreboard(MultiverseInstances plugin) {
        this.plugin = plugin;
        this.activePlayers = ConcurrentHashMap.newKeySet();
        this.hiddenPlayers = ConcurrentHashMap.newKeySet();
    }

    /**
     * Crea y muestra el scoreboard para un jugador al entrar a una instancia.
     */
    public void createForPlayer(Player player, Instance instance) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;

        UUID uuid = player.getUniqueId();
        activePlayers.add(uuid);

        if (hiddenPlayers.contains(uuid)) return;

        applyScoreboard(player, instance);
    }

    /**
     * Elimina el scoreboard de un jugador al salir de una instancia.
     */
    public void removeForPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        activePlayers.remove(uuid);

        clearScoreboard(player);
    }

    /**
     * Toggle del scoreboard para un jugador.
     */
    public boolean toggleScoreboard(Player player) {
        UUID uuid = player.getUniqueId();

        if (!activePlayers.contains(uuid)) {
            player.sendMessage(MessageUtils.colorize("&cNo estas en una instancia."));
            return false;
        }

        if (hiddenPlayers.contains(uuid)) {
            hiddenPlayers.remove(uuid);
            Instance instance = plugin.getInstanceManager().getPlayerInstance(uuid);
            if (instance != null) {
                applyScoreboard(player, instance);
            }
            player.sendMessage(MessageUtils.colorize("&aScoreboard &evisible&a."));
        } else {
            hiddenPlayers.add(uuid);
            clearScoreboard(player);
            player.sendMessage(MessageUtils.colorize("&aScoreboard &eoculto&a. Usa &6/mi sb &apara mostrar."));
        }
        return true;
    }

    /**
     * Actualiza todos los scoreboards activos (llamado periódicamente).
     */
    public void updateAll() {
        for (UUID uuid : activePlayers) {
            if (hiddenPlayers.contains(uuid)) continue;

            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                activePlayers.remove(uuid);
                continue;
            }

            Instance instance = plugin.getInstanceManager().getPlayerInstance(uuid);
            if (instance == null) {
                activePlayers.remove(uuid);
                clearScoreboard(player);
                continue;
            }

            applyScoreboard(player, instance);
        }
    }

    /**
     * Aplica el scoreboard con datos actualizados.
     */
    private void applyScoreboard(Player player, Instance instance) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard scoreboard = manager.getNewScoreboard();

        String title = plugin.getConfig().getString("scoreboard.title", "&6&lMULTIVERSE-INSTANCES");
        Objective objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY,
                MessageUtils.colorizeToComponent(title));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = buildLines(instance, player);

        // Los scores van de mayor a menor (la línea 0 está abajo)
        int score = lines.size();
        for (String line : lines) {
            // Bukkit scoreboard entries deben ser únicos, añadimos espacios invisibles
            String uniqueLine = makeUnique(line, score);
            Score s = objective.getScore(uniqueLine);
            s.setScore(score);
            score--;
        }

        player.setScoreboard(scoreboard);
    }

    /**
     * Construye las líneas del scoreboard.
     */
    private List<String> buildLines(Instance instance, Player viewer) {
        List<String> lines = new ArrayList<>();

        lines.add("§7━━━━━━━━━━━━━━━━");

        // Info del mapa
        lines.add("§e⚔ Mapa: §f" + instance.getBaseWorld());

        String typeLabel = switch (instance.getWinConditionType().toLowerCase()) {
            case "ctm" -> "CTM";
            case "checkpoint" -> "Checkpoint";
            case "dungeon" -> "Dungeon";
            default -> instance.getWinConditionType();
        };
        lines.add("§e🏆 Tipo: §f" + typeLabel);

        lines.add("");

        // Equipo
        lines.add("§b§l Equipo");

        Set<String> players = instance.getPlayers();
        if (players.isEmpty()) {
            lines.add("§7  (vacio)");
        } else {
            for (String playerName : players) {
                Player teammate = Bukkit.getPlayerExact(playerName);
                if (teammate != null && teammate.isOnline()) {
                    String healthBar = buildHealthBar(teammate);
                    String marker = teammate.getUniqueId().equals(viewer.getUniqueId()) ? "§f§l" : "§f";
                    lines.add(healthBar + " " + marker + playerName);
                } else {
                    lines.add("§8✖ §7§m" + playerName);
                }
            }
        }

        lines.add("");

        // Progreso según tipo
        lines.add("§d§l Progreso");
        addProgressLines(lines, instance);

        lines.add("§7━━━━━━━━━━━━━━━━");
        lines.add("§8/mi sb §7ocultar");

        return lines;
    }

    /**
     * Construye la barra de vida con corazones de color.
     */
    private String buildHealthBar(Player player) {
        double health = player.getHealth();
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        int hearts = 10; // 10 corazones visuales
        int filledHearts = (int) Math.ceil((health / maxHealth) * hearts);

        // Color según porcentaje de vida
        String color;
        double percentage = health / maxHealth;
        if (percentage > 0.6) {
            color = "§a"; // verde
        } else if (percentage > 0.3) {
            color = "§e"; // amarillo
        } else {
            color = "§c"; // rojo
        }

        StringBuilder bar = new StringBuilder(color);
        for (int i = 0; i < hearts; i++) {
            if (i < filledHearts) {
                bar.append("♥");
            } else {
                bar.append("§7♡");
                if (i < hearts - 1) {
                    bar.append(color); // no necesario ya que es gris hasta el final
                }
            }
        }

        return bar.toString();
    }

    /**
     * Añade líneas de progreso según el tipo de win condition.
     */
    private void addProgressLines(List<String> lines, Instance instance) {
        switch (instance.getWinConditionType().toLowerCase()) {
            case "ctm" -> {
                List<Monument> monuments = instance.getMonuments();
                long completed = monuments.stream().filter(Monument::isCompleted).count();
                long total = monuments.size();
                String progressBar = buildProgressBar(completed, total);
                lines.add("§eMonumentos: §f" + completed + "§7/§f" + total);
                lines.add(progressBar);
            }
            case "checkpoint" -> {
                List<Checkpoint> checkpoints = instance.getCheckpoints();
                long reached = checkpoints.stream().filter(Checkpoint::isReached).count();
                long total = checkpoints.size();
                String progressBar = buildProgressBar(reached, total);
                lines.add("§eCheckpoints: §f" + reached + "§7/§f" + total);
                lines.add(progressBar);
            }
            case "dungeon" -> {
                lines.add("§eBoss: §fActivo");
                lines.add("§e" + instance.getStatus().name());
            }
            default -> {
                lines.add("§7Sin datos");
            }
        }
    }

    /**
     * Construye una barra de progreso visual.
     */
    private String buildProgressBar(long current, long total) {
        if (total == 0) return "§7[§8..........§7]";

        int bars = 10;
        int filled = (int) ((double) current / total * bars);
        StringBuilder bar = new StringBuilder("§7[");

        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                bar.append("§a■");
            } else {
                bar.append("§8■");
            }
        }

        bar.append("§7]");
        return bar.toString();
    }

    /**
     * Limpia el scoreboard de un jugador (restaura al principal del servidor).
     */
    private void clearScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getMainScoreboard());
        }
    }

    /**
     * Hace una línea única añadiendo espacios invisibles según la posición.
     * Bukkit requiere que cada entry del scoreboard sea un String único.
     */
    private String makeUnique(String line, int index) {
        // Añadir suficientes §r (reset) al final para hacer la línea única
        StringBuilder sb = new StringBuilder(line);
        for (int i = 0; i < index; i++) {
            sb.append("§r");
        }

        // Limitar a 40 caracteres (límite del scoreboard)
        String result = sb.toString();
        if (result.length() > 40) {
            result = result.substring(0, 40);
        }
        return result;
    }

    /**
     * Limpieza al desactivar el plugin.
     */
    public void shutdown() {
        for (UUID uuid : activePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                clearScoreboard(player);
            }
        }
        activePlayers.clear();
        hiddenPlayers.clear();
    }
}
