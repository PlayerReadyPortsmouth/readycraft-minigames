package com.auroraschaos.minigames.party;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.auroraschaos.minigames.config.PartyConfig;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

/**
 * Keeps track of all parties on the server.
 *
 * Responsibilities:
 *  - create / disband parties
 *  - add / remove members
 *  - assign each party a unique ChatColor and a scoreboard Team so that
 *    everyone in a party sees each other’s names colored the same.
 *  - lookup whether a player is in a party, get that party, etc.
 */
public class PartyManager {

    private final MinigamesPlugin plugin;
    private final Scoreboard mainScoreboard;
    private final PartyConfig partyConfig;

    /** Map from party ID → Party object */
    private final Map<UUID, Party> parties = new HashMap<>();

    /** Map from player UUID → party ID (so we can quickly find a player’s party) */
    private final Map<UUID, UUID> playerToParty = new HashMap<>();

    /** Pre‐defined ChatColor pool for parties (cycled through) */
    private final List<ChatColor> availableColors = new ArrayList<>(Arrays.asList(
        ChatColor.AQUA, ChatColor.GREEN, ChatColor.LIGHT_PURPLE,
        ChatColor.YELLOW, ChatColor.GOLD, ChatColor.RED, ChatColor.BLUE
    ));
    private int nextColorIndex = 0;

    public PartyManager(MinigamesPlugin plugin, PartyConfig partyConfig) {
        this.plugin = plugin;
        this.partyConfig = partyConfig;
        // Use the server’s main scoreboard (so name‐coloring updates everywhere)
        this.mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    // ----------------------------------------------------------------
    // 1) PARTY CREATION / DELETION
    // ----------------------------------------------------------------

    /**
     * Create a new party, with "leader" as the initial member.
     * Assigns the next available ChatColor.
     *
     * @param leader Player who will be the party’s leader.
     * @return The newly created Party, or null if the player was already in a party.
     */
    public Party createParty(Player leader) {
        UUID leaderUUID = leader.getUniqueId();
        if (playerToParty.containsKey(leaderUUID)) {
            // Already in a party
            return null;
        }

        // Pick next color (cycle if necessary)
        ChatColor color = availableColors.get(nextColorIndex);
        nextColorIndex = (nextColorIndex + 1) % availableColors.size();

        Party party = new Party(leaderUUID, color);
        parties.put(party.getId(), party);
        playerToParty.put(leaderUUID, party.getId());

        // Create a scoreboard Team for this party
        registerPartyTeam(party);

        return party;
    }

    /**
     * Disband the party whose ID is "partyId".
     * Removes all members and unregisters the scoreboard Team.
     *
     * @param partyId the UUID of the party to disband.
     */
    public void disbandParty(UUID partyId) {
        Party party = parties.remove(partyId);
        if (party == null) return;

        // Remove every member's mapping
        for (UUID memberUUID : party.getMembers()) {
            playerToParty.remove(memberUUID);
            Player p = Bukkit.getPlayer(memberUUID);
            if (p != null && p.isOnline()) {
                // Reset name color to default (by removing them from the team)
                ScoreboardTeamRemover(p);
            }
        }

        // Unregister the scoreboard Team
        Team team = mainScoreboard.getTeam(getTeamName(party));
        if (team != null) {
            team.unregister();
        }
    }

    // ----------------------------------------------------------------
    // 2) PARTY JOIN / LEAVE
    // ----------------------------------------------------------------

    /**
     * Adds "invitee" to the party whose ID is "partyId".
     *
     * @param partyId Player to invite’s party ID.
     * @param invitee the Player being added.
     * @return true if successful, false if already in a party or invalid party.
     */
    public boolean addToParty(UUID partyId, Player invitee) {
        UUID inviteeUUID = invitee.getUniqueId();
        if (playerToParty.containsKey(inviteeUUID)) {
            // Already in a party
            return false;
        }
        Party party = parties.get(partyId);
        if (party == null) return false;

        party.addMember(inviteeUUID);
        playerToParty.put(inviteeUUID, partyId);

        // Add invitee to the scoreboard team
        Team team = mainScoreboard.getTeam(getTeamName(party));
        if (team != null) {
            team.addEntry(invitee.getName());
        }
        return true;
    }

    /**
     * Remove "member" from whichever party they’re in (if any).
     * If they were the last member, disbands the party.
     *
     * @param member The Player to remove.
     * @return true if they were in a party and got removed.
     */
    public boolean removeFromParty(Player member) {
        UUID memberUUID = member.getUniqueId();
        UUID partyId = playerToParty.get(memberUUID);
        if (partyId == null) return false;

        Party party = parties.get(partyId);
        if (party == null) {
            playerToParty.remove(memberUUID);
            return false;
        }

        party.removeMember(memberUUID);
        playerToParty.remove(memberUUID);

        // Remove from scoreboard team
        Team team = mainScoreboard.getTeam(getTeamName(party));
        if (team != null) {
            team.removeEntry(member.getName());
        }

        // If the party is now empty, disband it
        if (party.isEmpty()) {
            disbandParty(partyId);
        }
        return true;
    }

    /**
     * Returns the Party object that this player is currently in, or null if not in one.
     */
    public Party getParty(UUID playerUUID) {
        UUID partyId = playerToParty.get(playerUUID);
        if (partyId == null) return null;
        return parties.get(partyId);
    }

    /**
     * @return true if this player is in any party.
     */
    public boolean isInParty(UUID playerUUID) {
        return playerToParty.containsKey(playerUUID);
    }

    // ----------------------------------------------------------------
    // 3) SCOREBOARD TEAM CREATION / NAME COLORING
    // ----------------------------------------------------------------

    /**
     * When a new Party is created, register a new Scoreboard Team named “party-<partyId>”
     * with the party’s color. That way, as soon as players are added to that Team,
     * their name (tablist + chat) shows in the party’s color.
     */
    private void registerPartyTeam(Party party) {
        String teamName = getTeamName(party);
        // Each scoreboard team name must be <= 16 chars, so we prefix and then substring if needed:
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        Team existing = mainScoreboard.getTeam(teamName);
        if (existing != null) {
            existing.unregister();
        }

        Team team = mainScoreboard.registerNewTeam(teamName);
        team.setColor(party.getColor());
        team.setAllowFriendlyFire(false);
        team.setCanSeeFriendlyInvisibles(true);

        // Add each existing member to the team
        for (UUID memberUUID : party.getMembers()) {
            Player p = Bukkit.getPlayer(memberUUID);
            if (p != null && p.isOnline()) {
                team.addEntry(p.getName());
            }
        }
    }

    /**
     * If you need to forcibly remove a player’s name from any scoreboard team (e.g. when they leave),
     * call this method.
     */
    private void ScoreboardTeamRemover(Player p) {
        for (Team team : mainScoreboard.getTeams()) {
            if (team.hasEntry(p.getName())) {
                team.removeEntry(p.getName());
            }
        }
    }

    /**
     * Returns a valid team name for a given Party. (prefix + first 12 chars of UUID)
     */
    private String getTeamName(Party party) {
        String shortId = party.getId().toString().replace("-", "").substring(0, 12);
        return "party-" + shortId;
    }
}
