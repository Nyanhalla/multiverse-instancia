package com.nyanhalla.multiverseinstances.models;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Team {

    private final String name;
    private final Set<UUID> members;
    private String tag;
    private long createdAt;

    public Team(String name) {
        this.name = name;
        this.members = new HashSet<>();
        this.tag = "§7[" + name + "§7]";
        this.createdAt = System.currentTimeMillis();
    }

    public Team(String name, String tag, long createdAt) {
        this.name = name;
        this.members = new HashSet<>();
        this.tag = tag;
        this.createdAt = createdAt;
    }

    public String getName() {
        return name;
    }

    public Set<UUID> getMembers() {
        return new HashSet<>(members);
    }

    public void addMember(UUID playerId) {
        members.add(playerId);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public boolean hasMember(UUID playerId) {
        return members.contains(playerId);
    }

    public int getSize() {
        return members.size();
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
