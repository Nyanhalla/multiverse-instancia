package com.nyanhalla.multiverseinstances.core;

import com.nyanhalla.multiverseinstances.models.Checkpoint;
import com.nyanhalla.multiverseinstances.models.Monument;
import com.nyanhalla.multiverseinstances.utils.InstanceUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
public class Instance {

    public enum InstanceStatus {
        ACTIVE, COMPLETED, EXPIRED
    }

    private final String id;
    private final String baseWorld;
    private String displayName;
    private final String team;
    private final String worldName;
    private final long createdAt;
    private long expiresAt;
    private InstanceStatus status;
    private final String winConditionType;

    private final Set<String> players;
    private final List<Monument> monuments;
    private final List<Checkpoint> checkpoints;

    // Runtime data
    private final Map<String, Location> playerLocations;
    private final Map<String, Map<Integer, Object>> playerInventories;

    public Instance(String id, String baseWorld, String displayName, String team,
                    String worldName, long createdAt, long expiresAt,
                    InstanceStatus status, String winConditionType) {
        this.id = id;
        this.baseWorld = baseWorld;
        this.displayName = displayName;
        this.team = team;
        this.worldName = worldName;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.winConditionType = winConditionType;
        this.players = new HashSet<>();
        this.monuments = new ArrayList<>();
        this.checkpoints = new ArrayList<>();
        this.playerLocations = new HashMap<>();
        this.playerInventories = new HashMap<>();
    }

    // Getters
    public String getId() { return id; }
    public String getBaseWorld() { return baseWorld; }
    public String getDisplayName() { return displayName; }
    public String getTeam() { return team; }
    public String getWorldName() { return worldName; }
    public World getWorld() { return Bukkit.getWorld(worldName); }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }
    public InstanceStatus getStatus() { return status; }
    public String getWinConditionType() { return winConditionType; }
    public Set<String> getPlayers() { return new HashSet<>(players); }
    public List<Monument> getMonuments() { return new ArrayList<>(monuments); }
    public List<Checkpoint> getCheckpoints() { return new ArrayList<>(checkpoints); }

    // Setters
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
    public void setStatus(InstanceStatus status) { this.status = status; }

    // Player management
    public void addPlayer(String playerName) {
        players.add(playerName);
    }

    public void removePlayer(String playerName) {
        players.remove(playerName);
    }

    public boolean hasPlayer(String playerName) {
        return players.contains(playerName);
    }

    public void clearMonuments() {
        monuments.clear();
    }

    public void clearCheckpoints() {
        checkpoints.clear();
    }

    // Monument management
    public void addMonument(Monument monument) {
        monuments.add(monument);
    }

    public void addMonument(String id, Material material, Location location) {
        monuments.add(new Monument(id, material, location));
    }

    public List<Map<String, Object>> getMonumentsData() {
        List<Map<String, Object>> data = new ArrayList<>();
        for (Monument monument : monuments) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", monument.getId());
            map.put("location", InstanceUtils.locationToString(monument.getLocation()));
            map.put("material", monument.getMaterial().name());
            map.put("completed", monument.isCompleted());
            data.add(map);
        }
        return data;
    }

    public void loadMonumentData(Map<String, Object> data) {
        String locationStr = (String) data.get("location");
        String worldName = InstanceUtils.extractWorldFromLocationString(locationStr);
        String coords = InstanceUtils.extractCoordsFromLocationString(locationStr);
        Location location = InstanceUtils.stringToLocation(worldName, coords);

        String id = (String) data.get("id");
        Material material = Material.valueOf((String) data.get("material"));
        boolean completed = (Boolean) data.getOrDefault("completed", data.getOrDefault("broken", false));

        monuments.add(new Monument(id, material, location, completed));
    }

    // Checkpoint management
    public void addCheckpoint(Checkpoint checkpoint) {
        checkpoints.add(checkpoint);
    }

    public void addCheckpoint(int id, String regionId, Location location, Location minimumPoint, Location maximumPoint) {
        checkpoints.add(new Checkpoint(id, regionId, location, minimumPoint, maximumPoint));
    }

    public List<Map<String, Object>> getCheckpointsData() {
        List<Map<String, Object>> data = new ArrayList<>();
        for (Checkpoint checkpoint : checkpoints) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", checkpoint.getId());
            map.put("region", checkpoint.getRegionId());
            map.put("location", InstanceUtils.locationToString(checkpoint.getLocation()));
            map.put("minimum", InstanceUtils.locationToString(checkpoint.getMinimumPoint()));
            map.put("maximum", InstanceUtils.locationToString(checkpoint.getMaximumPoint()));
            map.put("reached", checkpoint.isReached());
            data.add(map);
        }
        return data;
    }

    public void loadCheckpointData(Map<String, Object> data) {
        int id = (Integer) data.get("id");
        String region = (String) data.get("region");
        String locationStr = (String) data.get("location");
        String minimumStr = (String) data.get("minimum");
        String maximumStr = (String) data.get("maximum");
        String worldName = InstanceUtils.extractWorldFromLocationString(locationStr);
        String coords = InstanceUtils.extractCoordsFromLocationString(locationStr);
        String minimumWorld = InstanceUtils.extractWorldFromLocationString(minimumStr);
        String minimumCoords = InstanceUtils.extractCoordsFromLocationString(minimumStr);
        String maximumWorld = InstanceUtils.extractWorldFromLocationString(maximumStr);
        String maximumCoords = InstanceUtils.extractCoordsFromLocationString(maximumStr);
        Location location = InstanceUtils.stringToLocation(worldName, coords);
        Location minimumPoint = minimumStr == null ? location : InstanceUtils.stringToLocation(minimumWorld, minimumCoords);
        Location maximumPoint = maximumStr == null ? location : InstanceUtils.stringToLocation(maximumWorld, maximumCoords);
        boolean reached = (Boolean) data.getOrDefault("reached", false);

        checkpoints.add(new Checkpoint(id, region, location, minimumPoint, maximumPoint, reached));
    }

    // Player locations
    public void savePlayerLocation(String playerName, Location location) {
        playerLocations.put(playerName, location);
    }

    public Location getPlayerLocation(String playerName) {
        return playerLocations.get(playerName);
    }

    // Inventory snapshots
    public void savePlayerInventory(String playerName, Map<Integer, Object> inventory) {
        playerInventories.put(playerName, inventory);
    }

    public Map<Integer, Object> getPlayerInventory(String playerName) {
        return playerInventories.get(playerName);
    }

    // Utilities
    public boolean isExpired() {
        if (expiresAt <= 0) return false;
        return System.currentTimeMillis() > expiresAt;
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    public int getPlayerCount() {
        return players.size();
    }
}
