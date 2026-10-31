package com.nyanhalla.multiverseinstances.templates;

import com.nyanhalla.multiverseinstances.models.Checkpoint;
import com.nyanhalla.multiverseinstances.models.Monument;
import org.bukkit.Material;

import java.util.*;

public class MapTemplate {

    private final String worldName;
    private String winConditionType;
    private final List<Checkpoint> checkpoints;
    private final List<Monument> monuments;

    // --- Per-world config ---
    private String displayName;
    private String description;
    private Material icon;
    private int difficulty;       // 1-5
    private int minPlayers;       // 0 = usar global
    private int maxPlayers;       // 0 = sin límite
    private int ttl;              // 0 = usar global
    private final Map<String, Object> configOverrides;

    public MapTemplate(String worldName, String winConditionType) {
        this.worldName = worldName;
        this.winConditionType = winConditionType;
        this.checkpoints = new ArrayList<>();
        this.monuments = new ArrayList<>();
        this.displayName = worldName;
        this.description = "";
        this.icon = Material.GRASS_BLOCK;
        this.difficulty = 1;
        this.minPlayers = 0;
        this.maxPlayers = 0;
        this.ttl = 0;
        this.configOverrides = new LinkedHashMap<>();
    }

    // --- Core getters ---
    public String getWorldName() { return worldName; }
    public String getWinConditionType() { return winConditionType; }
    public void setWinConditionType(String winConditionType) { this.winConditionType = winConditionType; }

    // --- Per-world config getters/setters ---
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Material getIcon() { return icon; }
    public void setIcon(Material icon) { this.icon = icon; }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = Math.max(1, Math.min(5, difficulty)); }

    public int getMinPlayers() { return minPlayers; }
    public void setMinPlayers(int minPlayers) { this.minPlayers = minPlayers; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public int getTtl() { return ttl; }
    public void setTtl(int ttl) { this.ttl = ttl; }

    public Map<String, Object> getConfigOverrides() { return new LinkedHashMap<>(configOverrides); }
    public void setConfigOverride(String key, Object value) { configOverrides.put(key, value); }
    public void setConfigOverrides(Map<String, Object> overrides) {
        configOverrides.clear();
        if (overrides != null) configOverrides.putAll(overrides);
    }

    /**
     * Devuelve el valor de un override o null si no existe.
     */
    public Object getConfigOverride(String key) {
        return configOverrides.get(key);
    }

    /**
     * Devuelve el valor override como String, o el defaultValue si no existe.
     */
    public String getOverrideString(String key, String defaultValue) {
        Object val = configOverrides.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    public int getOverrideInt(String key, int defaultValue) {
        Object val = configOverrides.get(key);
        if (val instanceof Number n) return n.intValue();
        return defaultValue;
    }

    public boolean getOverrideBoolean(String key, boolean defaultValue) {
        Object val = configOverrides.get(key);
        if (val instanceof Boolean b) return b;
        return defaultValue;
    }

    /**
     * Genera el texto de dificultad con estrellas.
     */
    public String getDifficultyStars() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            sb.append(i <= difficulty ? "§e★" : "§8☆");
        }
        return sb.toString();
    }

    /**
     * Verifica si el tipo de win condition contiene un tipo específico.
     * Soporta composiciones: "ctm+checkpoint", "ctm|dungeon"
     */
    public boolean hasWinConditionPart(String type) {
        if (winConditionType == null) return false;
        String lower = winConditionType.toLowerCase();
        String typeLower = type.toLowerCase();
        return lower.equals(typeLower)
                || lower.contains("+" + typeLower) || lower.contains(typeLower + "+")
                || lower.contains("|" + typeLower) || lower.contains(typeLower + "|");
    }

    /**
     * Devuelve los tipos individuales de win condition.
     */
    public List<String> getWinConditionParts() {
        if (winConditionType == null || winConditionType.isEmpty()) return Collections.emptyList();
        String[] parts = winConditionType.toLowerCase().split("[+|]");
        return Arrays.asList(parts);
    }

    /**
     * Devuelve true si el tipo es compuesto.
     */
    public boolean isCompositeWinCondition() {
        return winConditionType != null && (winConditionType.contains("+") || winConditionType.contains("|"));
    }

    /**
     * Devuelve la lógica de composición: AND (+) o OR (|).
     */
    public String getCompositeLogic() {
        if (winConditionType == null) return "AND";
        return winConditionType.contains("|") ? "OR" : "AND";
    }

    // --- Checkpoint management ---
    public List<Checkpoint> getCheckpoints() { return new ArrayList<>(checkpoints); }

    public Optional<Checkpoint> getCheckpoint(int id) {
        return checkpoints.stream().filter(checkpoint -> checkpoint.getId() == id).findFirst();
    }

    public void upsertCheckpoint(Checkpoint checkpoint) {
        checkpoints.removeIf(existing -> existing.getId() == checkpoint.getId());
        checkpoints.add(checkpoint);
        checkpoints.sort(Comparator.comparingInt(Checkpoint::getId));
    }

    public boolean removeCheckpoint(int id) {
        return checkpoints.removeIf(checkpoint -> checkpoint.getId() == id);
    }

    // --- Monument management ---
    public List<Monument> getMonuments() { return new ArrayList<>(monuments); }

    public Optional<Monument> getMonument(String id) {
        return monuments.stream().filter(monument -> monument.getId().equalsIgnoreCase(id)).findFirst();
    }

    public void upsertMonument(Monument monument) {
        monuments.removeIf(existing -> existing.getId().equalsIgnoreCase(monument.getId()));
        monuments.add(monument);
        monuments.sort(Comparator.comparing(Monument::getId));
    }

    public boolean removeMonument(String id) {
        return monuments.removeIf(monument -> monument.getId().equalsIgnoreCase(id));
    }
}
