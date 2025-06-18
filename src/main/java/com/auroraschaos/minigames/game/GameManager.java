package com.auroraschaos.minigames.game;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.auroraschaos.minigames.arena.Arena;
import com.auroraschaos.minigames.arena.ArenaService;
import com.auroraschaos.minigames.game.race.RaceGame;
import com.auroraschaos.minigames.gui.GUIManager;
import com.auroraschaos.minigames.party.PartyManager;
import com.auroraschaos.minigames.stats.StatsManager;
import com.auroraschaos.minigames.scoreboard.QueueScoreboardManager;
import com.auroraschaos.minigames.scoreboard.ScoreboardManager;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.ChatColor;

import java.io.IOException;
import java.util.*;

/**
 * GameManager is responsible for:
 *  1. Managing player queues for each minigame (with optional GameMode).
 *  2. Spinning up dynamic Arena instances on demand via ArenaManager.
 *  3. Tracking ongoing GameInstance objects (one per running game).
 *  4. Handling game start/end lifecycle events.
 *
 *  It now also:
 *  - Starts a 60-second countdown (action-bar) once minPlayers is reached.
 *  - Cancels the countdown if queue size falls below minPlayers.
 *  - Shows how many players have joined in the action-bar during countdown.
 *  - Always updates the action-bar with "Queue: X/Y" whenever someone joins or leaves.
 *
 *  You should specify min/max players in config.yml under:
 *     minigames:
 *       <TYPE>:
 *         minPlayers: <number>
 *         maxPlayers: <number>
 */
public class GameManager {

    private final MinigamesPlugin plugin;
    private final ArenaService arenaService;
    private final PartyManager partyManager;
    private final StatsManager statsManager;
    private final GUIManager guiManager;
    private final QueueScoreboardManager queueSB;

    /** Keeps a queue of players (or parties) waiting for a particular minigame+mode. */
    private final Map<String, Queue<QueueEntry>> queueMap = new HashMap<>();

    /** Tracks all active GameInstance objects by their unique ID. */
    private final Map<String, GameInstance> activeGames = new HashMap<>();

    /**
     * Tracks any running 60-second countdown task for a specific queue key.
     * Key corresponds to “<TYPE>_<MODE>”.
     */
    private final Map<String, BukkitTask> countdownTasks = new HashMap<>();

    /** Duration of the countdown (in seconds) */
    private static final int COUNTDOWN_SECONDS = 60;

    public GameManager(MinigamesPlugin plugin,
                       ArenaService arenaService,
                       PartyManager partyManager,
                       StatsManager statsManager,
                       GUIManager guiManager) {
        this.plugin       = plugin;
        this.arenaService = arenaService;
        this.partyManager = partyManager;
        this.statsManager = statsManager;
        this.guiManager   = guiManager;
        this.queueSB      = new QueueScoreboardManager(plugin, this);

        // Optional: periodic heartbeat to update action-bar even if no join/leave event
        startQueueHeartbeat();
    }

    // ------------------------------------------------------------
    // 1) QUEUE MANAGEMENT
    // ------------------------------------------------------------

    /**
     * Enqueue a single player (or a party) for a specific minigame+mode.
     * Once the queue reaches minPlayers, it starts a 60-second countdown
     * (sending updates to the action bar). If the queue size falls below
     * minPlayers during countdown, it cancels.
     *
     * @param type     the minigame type (e.g. "TNT_RUN")
     * @param mode     the game mode (e.g. GameMode.CLASSIC)
     * @param entrants one or more Player objects (solo or a party)
     */
    public void enqueue(String type, GameMode mode, List<Player> entrants) {
        String key = buildQueueKey(type, mode);
        queueMap.putIfAbsent(key, new LinkedList<>());
        Queue<QueueEntry> queue = queueMap.get(key);

        // Add this entry to the queue
        QueueEntry entry = new QueueEntry(entrants);
        queue.add(entry);
        plugin.getQueueScoreboardManager().updateQueueScoreboard(type, mode);
        plugin.getLogger().info("Enqueued " + entrants.size()
                + " player(s) for " + type + " [" + mode + "]");

        // Notify the entrants via chat
        for (Player p : entrants) {
            p.sendMessage(ChatColor.GREEN + "You joined the queue for "
                    + type + " [" + mode + "]. Currently joined: "
                    + queue.size() + "/" + getMaxPlayers(type));
        }

        // Update the action-bar for everyone in queue
        updateQueueActionBar(type, queue);

        // Attempt to start (or begin countdown)
        attemptToStartGame(type, mode);
    }

    /**
     * Builds a stable key for the queueMap based on minigame type + mode.
     */
    private String buildQueueKey(String type, GameMode mode) {
        return type.toUpperCase() + "_" + mode.name();
    }

    /**
     * Attempt to form a game if enough players (or parties) are in the queue.
     * If currentEntries < minPlayers: notify players and cancel any running countdown.
     * If currentEntries >= minPlayers and no countdown is running: start countdown.
     */
    private void attemptToStartGame(String type, GameMode mode) {
        String key = buildQueueKey(type, mode);
        Queue<QueueEntry> queue = queueMap.get(key);
        if (queue == null || queue.isEmpty()) return;

        int currentEntries = queue.size();
        int minPlayers     = getMinPlayers(type);

        // If not enough entries, inform how many more are needed and cancel countdown
        if (currentEntries < minPlayers) {
            int needed = minPlayers - currentEntries;
            for (QueueEntry e : queue) {
                for (Player p : e.getPlayers()) {
                    p.sendMessage(ChatColor.YELLOW + "Waiting for "
                            + ChatColor.WHITE + needed + ChatColor.YELLOW
                            + " more player" + (needed == 1 ? "" : "s")
                            + " to start " + type + ".");
                }
            }
            cancelCountdownForQueue(key);
            return;
        }

        // At this point, currentEntries >= minPlayers
        // If a countdown is already running, do nothing (let it continue)
        if (countdownTasks.containsKey(key)) {
            return;
        }

        // Otherwise, start a fresh 60-second countdown for this queue
        startCountdownForQueue(key, type, mode, queue);
    }

    /**
     * Dequeue a single player from a specific minigame+mode.
     * If the queue size drops below minPlayers during an active countdown,
     * the countdown is canceled and participants are notified.
     *
     * Call this from your /minigames leave command.
     *
     * @param type     the minigame type
     * @param mode     the game mode
     * @param toRemove the Player object to remove from the queue
     */
    public void dequeue(String type, GameMode mode, Player toRemove) {
        String key = buildQueueKey(type, mode);
        Queue<QueueEntry> queue = queueMap.get(key);
        if (queue == null || queue.isEmpty()) {
            toRemove.sendMessage(ChatColor.RED + "You are not in that queue.");
            return;
        }

        // Find and remove the entry containing this player
        Iterator<QueueEntry> it = queue.iterator();
        boolean found = false;
        while (it.hasNext()) {
            QueueEntry e = it.next();
            if (e.getPlayers().contains(toRemove)) {
                it.remove();
                found = true;
                break;
            }
        }
        if (!found) {
            toRemove.sendMessage(ChatColor.RED + "You are not in that queue.");
            return;
        }

        toRemove.sendMessage(ChatColor.RED + "You left the queue for "
                + type + " [" + mode + "].");

        // Update the action-bar for everyone still in queue
        updateQueueActionBar(type, queue);
        plugin.getQueueScoreboardManager().updateQueueScoreboard(type, mode);

        // If queue size dropped below minPlayers, cancel countdown
        int remaining = queue.size();
        int minPlayers = getMinPlayers(type);
        if (remaining < minPlayers) {
            cancelCountdownForQueue(key);
            for (QueueEntry e : queue) {
                for (Player p : e.getPlayers()) {
                    p.sendMessage(ChatColor.YELLOW + "Countdown aborted. Now waiting for "
                            + ChatColor.WHITE + (minPlayers - remaining) + ChatColor.YELLOW
                            + " more player"
                            + ((minPlayers - remaining) == 1 ? "" : "s") + ".");
                }
            }
        } else {
            // If still ≥ minPlayers, inform via chat how many are joined
            for (QueueEntry e : queue) {
                for (Player p : e.getPlayers()) {
                    p.sendMessage(ChatColor.GREEN + "Queue updated: "
                            + remaining + "/" + getMaxPlayers(type) + " joined.");
                }
            }
        }
    }

    // ------------------------------------------------------------
    // 1a) COUNTDOWN LOGIC
    // ------------------------------------------------------------

    /**
     * Starts a 60-second countdown for the given queue key. Broadcasts the remaining time
     * and number of joined players to everyone in the queue via action-bar. If at any tick
     * the queue size falls below minPlayers, the countdown is canceled.
     *
     * @param key      the queue key ("<TYPE>_<MODE>")
     * @param type     the minigame type
     * @param mode     the GameMode (used when actually starting the game)
     * @param queue    the Queue of QueueEntry (each holding one or more players)
     */
    private void startCountdownForQueue(String key,
                                        String type,
                                        GameMode mode,
                                        Queue<QueueEntry> queue) {
        // Inform queue that countdown has begun
        broadcastToQueue(queue, ChatColor.GREEN + "Minimum players reached! "
                + "Game starts in " + COUNTDOWN_SECONDS + " seconds...");

        // Create a repeating task that runs once per second (20 ticks)
        BukkitTask task = new BukkitRunnable() {
            int secondsLeft = COUNTDOWN_SECONDS;

            @Override
            public void run() {
                // If queue no longer meets minPlayers, cancel immediately
                int currentSize = queue.size();
                if (currentSize < getMinPlayers(type)) {
                    broadcastToQueue(queue, ChatColor.RED + "Countdown aborted: not enough players.");
                    cancel();
                    countdownTasks.remove(key);
                    return;
                }

                // Send action-bar message: e.g. "Starting in 45s | Joined: 3/10"
                String actionMessage = ChatColor.AQUA + "Starting in "
                        + ChatColor.WHITE + secondsLeft
                        + ChatColor.AQUA + "s  |  Joined: "
                        + ChatColor.WHITE + currentSize
                        + ChatColor.AQUA + "/" + getMaxPlayers(type);
                sendActionBarToQueue(queue, actionMessage);

                // When timer reaches 0, start the game
                if (secondsLeft <= 0) {
                    cancel();
                    countdownTasks.remove(key);

                    // Copy the current queue entries into a new list of players
                    List<Player> participants = new ArrayList<>();
                    while (!queue.isEmpty()) {
                        QueueEntry e = queue.poll();
                        participants.addAll(e.getPlayers());
                    }
                    queueMap.remove(key);

                    startNewGame(type, mode, participants);
                    return;
                }

                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        // Store the scheduled task so we can cancel it if needed
        countdownTasks.put(key, task);
    }

    /**
     * Cancels a running countdown task (if any) for the given queue key.
     */
    private void cancelCountdownForQueue(String key) {
        BukkitTask task = countdownTasks.remove(key);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    /**
     * Sends a chat message to every player in the queue (via normal chat).
     */
    private void broadcastToQueue(Queue<QueueEntry> queue, String message) {
        for (QueueEntry e : queue) {
            for (Player p : e.getPlayers()) {
                p.sendMessage(message);
            }
        }
    }

    /**
     * Sends an action-bar (above inventory/hotbar) message to every player in the queue.
     */
    private void sendActionBarToQueue(Queue<QueueEntry> queue, String message) {
        TextComponent text = new TextComponent(message);
        for (QueueEntry e : queue) {
            for (Player p : e.getPlayers()) {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, text);
            }
        }
    }

    /**
     * Updates the action-bar for the given queue to show "Queue: size/max".
     */
    private void updateQueueActionBar(String type, Queue<QueueEntry> queue) {
        int currentSize = queue.size();
        int maxPlayers  = getMaxPlayers(type);
        String actionMsg = ChatColor.AQUA + "Queue: " 
                         + ChatColor.WHITE + currentSize 
                         + ChatColor.AQUA + "/" 
                         + maxPlayers;
        sendActionBarToQueue(queue, actionMsg);
    }

    /**
     * (Optional) Periodically re-broadcasts the "Queue: X/Y" action-bar messages
     * to all non-empty queues every 5 seconds, in case something changes behind the scenes.
     */
    private void startQueueHeartbeat() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<String, Queue<QueueEntry>> entry : queueMap.entrySet()) {
                    Queue<QueueEntry> queue = entry.getValue();
                    if (queue.isEmpty()) continue;

                    // Extract type from key (format "<TYPE>_<MODE>")
                    String key = entry.getKey();
                    String type = key.split("_")[0];
                    updateQueueActionBar(type, queue);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * 5); // every 5 seconds
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
        Arena arena = arenaService.createArenaInstance(type);
        if (arena == null) {
            plugin.getLogger().severe("Failed to create arena for minigame: " + type);
            for (Player p : players) {
                p.sendMessage(ChatColor.RED + "No arena available for " + type + ". Try later.");
            }
            return;
        }
        arena.setInUse(true);

        plugin.getQueueScoreboardManager().clearQueueScoreboard(type, mode);

        // 2) Instantiate a GameInstance based on type + mode
        GameInstance instance;
        switch (type.toUpperCase()) {
            case "TNT_RUN":
                instance = new TNTRunGame(type, mode, plugin, arena, players);
                break;
            case "RACE":
                try {
                    instance = new RaceGame(type, mode, plugin, arena, players, null, null, type);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return;
                }
                break;
            // TODO: Add other minigame constructors here
            default:
                plugin.getLogger().warning("No GameInstance class for type: " + type);
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
     * @param instanceId the unique ID of the GameInstance
     */
    public void endGame(String instanceId) {
        GameInstance instance = activeGames.remove(instanceId);
        if (instance == null) return;

        // 1) Stop the game logic
        instance.stop();

        // 2) Reset the schematic in the arena (wipes any player modifications)
        Arena arena = instance.getArena();
        arenaService.resetArena(arena);
        arena.setInUse(false);

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

    /**
     * Reads the minimum number of players for a given type from config.yml.
     * Path: minigames.<TYPE>.minPlayers  (default = 1 if not specified)
     */
    private int getMinPlayers(String type) {
        return plugin.getConfig().getInt("minigames." + type.toUpperCase() + ".minPlayers", 1);
    }

    /**
     * Reads the maximum number of players for a given type from config.yml.
     * Path: minigames.<TYPE>.maxPlayers  (default = same as minPlayers if not specified)
     */
    public int getMaxPlayers(String type) {
        int min = getMinPlayers(type);
        return plugin.getConfig().getInt("minigames." + type.toUpperCase() + ".maxPlayers", min);
    }

    // ------------------------------------------------------------
    // 4) INNER CLASSES & STRUCTURES
    // ------------------------------------------------------------

    /**
     * Represents a queued entry for a game: either a solo player or a party of players.
     */
    public static class QueueEntry {
        private final List<Player> players;

        /**
         * Constructs a new QueueEntry with the given list of players.
         */
        public QueueEntry(List<Player> players) {
            this.players = new ArrayList<>(players);
        }

        /**
         * @return An unmodifiable list of players in this queue entry.
         */
        public List<Player> getPlayers() {
            return Collections.unmodifiableList(players);
        }
    }
    /**
 * Returns true if the player is currently waiting in any queue (regardless of gameType/mode).
 */
public boolean isPlayerInQueue(Player player) {
    // Look through every queue to see if this player is in any entry
    for (Queue<QueueEntry> queue : queueMap.values()) {
        for (QueueEntry entry : queue) {
            if (entry.getPlayers().contains(player)) {
                return true;
            }
        }
    }
    return false;
}

/**
 * Removes the player from whatever queue they are in (if any).
 * Returns true if they were found and removed; false otherwise.
 */
public boolean removeFromQueue(Player player) {
    for (Map.Entry<String, Queue<QueueEntry>> kv : queueMap.entrySet()) {
        String key = kv.getKey();
        Queue<QueueEntry> queue = kv.getValue();
        Iterator<QueueEntry> it = queue.iterator();
        while (it.hasNext()) {
            QueueEntry entry = it.next();
            if (entry.getPlayers().contains(player)) {
                it.remove();
                // After removal, cancel any countdown if needed:
                if (queue.size() < getMinPlayers(key.split("_")[0])) {
                    cancelCountdownForQueue(key);
                    // Notify remaining players (optional):
                    int minP = getMinPlayers(key.split("_")[0]);
                    int rem  = queue.size();
                    for (QueueEntry e : queue) {
                        for (Player p : e.getPlayers()) {
                            p.sendMessage(ChatColor.YELLOW + "Countdown aborted — waiting for "
                                    + ChatColor.WHITE + (minP - rem) + ChatColor.YELLOW + " more player"
                                    + ((minP - rem) == 1 ? "" : "s") + ".");
                        }
                    }
                }
                return true;
            }
        }
    }
    return false;
}

/**
 * Returns true if the player is currently inside any active GameInstance.
 */
public boolean isPlayerInActiveGame(Player player) {
    for (GameInstance inst : activeGames.values()) {
        if (inst.getParticipants().contains(player)) {
            return true;
        }
    }
    return false;
}

/**
 * Removes the player from whatever active game they are in (if any).
 * You must implement GameInstance.removePlayer(...) so that it handles
 * eliminating a single player. Returns true if removed, false otherwise.
 */
public boolean removeFromActiveGame(Player player) {
    for (Map.Entry<String, GameInstance> kv : activeGames.entrySet()) {
        GameInstance inst = kv.getValue();
        if (inst.getParticipants().contains(player)) {
            inst.removePlayer(player);
            // If that was the last player in this instance, you may want to end the game:
            if (inst.getParticipants().isEmpty()) {
                endGame(inst.getId());
            }
            return true;
        }
    }
    return false;
}

// ----------------------------------------------------------------
// 5) NEW ACCESSORS FOR PLACEHOLDERAPI
// ----------------------------------------------------------------

/**
 * @return The total number of running GameInstance objects.
 */
public int getActiveGameCount() {
    return activeGames.size();
}

/**
 * @return The total number of queued entries (QueueEntry objects) across all modes for a given gameType.
 *   Example: if 2 people are queued in TNT_RUN_CLASSIC and 3 in TNT_RUN_HARDCORE, this returns 5.
 */
public int getTotalQueuedForType(String gameType) {
    int total = 0;
    // Iterate over all queueMap keys like "TNT_RUN_CLASSIC" or "BED_WARS_TEAMS"
    for (Map.Entry<String, Queue<QueueEntry>> entry : queueMap.entrySet()) {
        String key = entry.getKey(); // e.g. "TNT_RUN_CLASSIC"
        if (key.startsWith(gameType.toUpperCase() + "_")) {
            total += entry.getValue().size();
        }
    }
    return total;
}

/**
 * @return The number of queued entries (QueueEntry objects) for a specific gameType + mode.
 *   Example: getQueuedCount("TNT_RUN", GameMode.CLASSIC) returns size of queueMap.get("TNT_RUN_CLASSIC").
 */
public int getQueuedCount(String gameType, GameMode mode) {
    String key = buildQueueKey(gameType, mode);
    Queue<QueueEntry> queue = queueMap.get(key);
    return (queue == null) ? 0 : queue.size();
}

/**
 * Returns the internal Queue<QueueEntry> for a given key,
 * or null if none exists.
 */
public Queue<QueueEntry> getQueue(String key) {
    return queueMap.get(key);
}

}
