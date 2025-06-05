package com.yourname.minigames.game;

import com.yourname.minigames.MinigamesPlugin;
import com.yourname.minigames.arena.Arena;
import com.yourname.minigames.arena.ArenaManager;
import com.yourname.minigames.party.PartyManager;
import com.yourname.minigames.stats.StatsManager;
import com.yourname.minigames.gui.GUIManager;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * GameManager is responsible for:
 *  1. Managing player queues for each minigame (with optional GameMode).
 *  2. Spinning up dynamic Arena instances on demand via ArenaManager.
 *  3. Tracking ongoing GameInstance objects (one per running game).
 *  4. Handling game start/end lifecycle events.
 */
public class GameManager {

    private final MinigamesPlugin plugin;
    private final ArenaManager arenaManager;
    private final PartyManager partyManager;
    private final StatsManager statsManager;
    private final GUIManager guiManager;

    /** Keeps a queue of players (or parties) waiting for a particular minigame + mode. */
    private final Map<String, Queue<QueueEntry>> queueMap = new HashMap<>();

    /** Tracks all active GameInstance objects by their unique ID. */
    private final Map<String, GameInstance> activeGames = new HashMap<>();

    public GameManager(MinigamesPlugin plugin, ArenaManager arenaManager,
                       PartyManager partyManager, StatsManager statsManager, GUIManager guiManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.partyManager = partyManager;
        this.statsManager = statsManager;
        this.guiManager = guiManager;
    }

    // ------------------------------------------------------------
    // 1) QUEUE MANAGEMENT
    // ------------------------------------------------------------

    /**
     * Enqueue a single player (or party) for a specific minigame+mode.
     * When enough entries are queued (e.g. 1 player for solo, or N players for team),
     * this will attempt to start a new GameInstance.
     *
     * @param type  the minigame type (e.g. "TNT_RUN")
     * @param mode  the game mode (e.g. GameMode.CLASSIC)
     * @param entrants  one or more Player objects (for solo, pass a single-player list;
     *                  for a party, pass all party members)
     */
    public void enqueue(String type, GameMode mode, List<Player> entrants) {
        String key = buildQueueKey(type, mode);
        queueMap.putIfAbsent(key, new LinkedList<>());
        Queue<QueueEntry> queue = queueMap.get(key);

        // Create a new QueueEntry (could represent a solo player or a party)
        QueueEntry entry = new QueueEntry(entrants);
        queue.add(entry);
        plugin.getLogger().info("Enqueued " + entrants.size() + " for " + type + " [" + mode + "]");

        attemptToStartGame(type, mode);
    }

    /**
     * Builds a stable key for the queueMap based on minigame type + mode.
     */
    private String buildQueueKey(String type, GameMode mode) {
        return type.toUpperCase() + "_" + mode.name();
    }

    /**
     * Attempt to form a game if enough players (or parties) exist in the queue.
     * This example assumes a solo minigame (1 player needed). Adjust logic for team games.
     */
    private void attemptToStartGame(String type, GameMode mode) {
        String key = buildQueueKey(type, mode);
        Queue<QueueEntry> queue = queueMap.get(key);
        if (queue == null || queue.isEmpty()) return;

        // Example: if this is a solo game (1 player per instance)
        if (mode == GameMode.CLASSIC || mode == GameMode.HARDCORE || mode == GameMode.TIMED) {
            // Pop one entry and start a solo game
            QueueEntry entry = queue.poll();
            if (entry == null) return;
            List<Player> players = entry.getPlayers();
            startNewGame(type, mode, players);
        }
        // Add additional logic for team-based modes (TEAM size, etc.) here
    }

    // ------------------------------------------------------------
    // 2) GAME LIFECYCLE MANAGEMENT
    // ------------------------------------------------------------

    /**
     * Creates & starts a new GameInstance for the given players.
     *
     * @param type     minigame type (e.g. "TNT_RUN")
     * @param mode     selected GameMode
     * @param players  list of Player objects (solo or party)
     */
    private void startNewGame(String type, GameMode mode, List<Player> players) {
        // 1) Spin up a dynamic Arena instance
        Arena arena = arenaManager.createArenaInstance(type);
        if (arena == null) {
            plugin.getLogger().severe("Failed to create arena for minigame: " + type);
            return;
        }
        arena.setInUse(true);

        // 2) Instantiate a GameInstance based on type + mode
        GameInstance instance;
        switch (type.toUpperCase()) {
            case "TNT_RUN":
                instance = new TNTRunGame(type, mode, plugin, arena, players);
                break;
            case "BED_WARS":
                instance = new BedWarsGame(type, mode, plugin, arena, players);
                break;
            // TODO: Add other minigame constructors here
            default:
                plugin.getLogger().warning("No GameInstance found for type: " + type);
                arena.setInUse(false);
                return;
        }

        // 3) Register & start the instance
        activeGames.put(instance.getId(), instance);
        instance.start();

        plugin.getLogger().info("Started " + type + " [" + mode + "] with ID: " + instance.getId());
    }

    /**
     * Cleanly ends a running game, frees the arena, and updates stats.
     *
     * @param instanceId  the unique ID of the GameInstance
     */
    public void endGame(String instanceId) {
        GameInstance instance = activeGames.remove(instanceId);
        if (instance == null) return;

        // 1) Stop the game logic
        instance.stop();

        // 2) Reset the schematic in the arena (wipes any player modifications)
        Arena arena = instance.getArena();
        arenaManager.resetArena(arena);
        arena.setInUse(false);

        // 3) Update stats via StatsManager
        statsManager.recordGameResult(instance);

        plugin.getLogger().info("Ended game " + instanceId + " (" + instance.getType() + ")");
    }

    // ------------------------------------------------------------
    // 3) ACCESSORS & HELPERS
    // ------------------------------------------------------------

    /**
     * Returns an unmodifiable view of all active GameInstances.
     */
    public Collection<GameInstance> getActiveGames() {
        return Collections.unmodifiableCollection(activeGames.values());
    }

    /**
     * Fetch a specific GameInstance by its ID.
     */
    public GameInstance getGameInstance(String instanceId) {
        return activeGames.get(instanceId);
    }

    /**
     * Check if a player is already in a queue or active game.
     */
    public boolean isPlayerInGameOrQueue(Player player) {
        // Check active games
        for (GameInstance instance : activeGames.values()) {
            if (instance.getParticipants().contains(player)) return true;
        }

        // Check queues
        for (Queue<QueueEntry> queue : queueMap.values()) {
            for (QueueEntry entry : queue) {
                if (entry.getPlayers().contains(player)) return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------
    // 4) INNER CLASSES & STRUCTURES
    // ------------------------------------------------------------

    /**
     * Represents a queued entry for a game: either a solo player or a party of players.
     */
    public static class QueueEntry {
        private final List<Player> players;

        public QueueEntry(List<Player> players) {
            this.players = new ArrayList<>(players);
        }

        public List<Player> getPlayers() {
            return players;
        }
    }
}
