package com.auroraschaos.minigames.party;

import org.bukkit.ChatColor;

import java.util.*;

/**
 * A simple POJO representing one Party:
 *  - each party has a unique ID (UUID)
 *  - a leader (UUID)
 *  - a set of members (UUIDs)
 *  - a displayColor (for the scoreboard team and chat coloring)
 */
public class Party {
    private final UUID id;
    private UUID leader;
    private final Set<UUID> members = new HashSet<>();
    private final ChatColor color;

    public Party(UUID leader, ChatColor color) {
        this.id = UUID.randomUUID();
        this.leader = leader;
        this.color = color;
        this.members.add(leader);
    }

    public UUID getId() {
        return id;
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID newLeader) {
        if (members.contains(newLeader)) {
            this.leader = newLeader;
        }
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public ChatColor getColor() {
        return color;
    }

    public boolean addMember(UUID playerUUID) {
        return members.add(playerUUID);
    }

    public boolean removeMember(UUID playerUUID) {
        // if removing the leader, either disband or pick a new leader outside
        if (!members.contains(playerUUID)) return false;
        members.remove(playerUUID);
        if (playerUUID.equals(leader)) {
            // if party is now empty, leader is gone; leave leader null
            if (members.isEmpty()) {
                leader = null;
            } else {
                // pick a random remaining member as leader
                leader = members.iterator().next();
            }
        }
        return true;
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }
}