package com.auroraschaos.minigames.party;

import com.auroraschaos.minigames.MinigamesPlugin;

/**
 * Manages player parties.
 * This includes functionality for creating, inviting players to, and disbanding parties.
 * Parties can be used to queue for minigames together.
 */
public class PartyManager {

    private final MinigamesPlugin plugin;

    /**
     * Constructs a new PartyManager.
     * @param plugin The main plugin instance.
     */
    public PartyManager(MinigamesPlugin plugin) {
        this.plugin = plugin;
        // TODO: Manage party creation, invites, disbanding
    }
}
