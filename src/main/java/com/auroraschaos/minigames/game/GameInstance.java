package com.auroraschaos.minigames.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.auroraschaos.minigames.arena.Arena;
import com.auroraschaos.minigames.game.GameMode;
import com.auroraschaos.minigames.stats.StatsManager;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.*;

/**
 * Abstract base class for all minigame instances.
 * Each running game (e.g. a single round of TNT Run) will be represented
 * by a subclass of GameInstance.
 *
 * Added feature:
 *   • When a player leaves mid-game, they are returned to their original world/location.
 *   • At end of game, all remaining players are returned to their original world/locations.
 */
public abstract class GameInstance {

    /** Unique identifier for this game instance */
    private final String id;

    /** BukkitTask handle if the game requires periodic tick updates */
    private BukkitTask task;

    /** Reference to the main plugin */
    protected final MinigamesPlugin plugin;

    /** The arena (with its dynamic origin) where this game is running */
    protected final Arena arena;

    /** The type of minigame (e.g. "TNT_RUN", "BED_WARS") */
    protected final String type;

    /** Chosen game mode (e.g. CLASSIC, HARDCORE, TEAMS, etc.) */
    protected final GameMode gameMode;

    /** List of participants (players or party members) */
    protected final List<Player> participants = new ArrayList<>();

    /** StatsManager reference for recording results */
    protected final StatsManager statsManager;

    /**
     * Store each player's original Location (including world) before they are teleported
     * into the arena. We will use this to return them when they leave or when the game ends.
     */
    protected final Map<Player, Location> originalLocation = new HashMap<>();

    /**
     * Constructor: initialize a new GameInstance.
     *
     * @param plugin       reference to main plugin
     * @param arena        the Arena (dynamic origin) assigned to this game
     * @param type         the minigame type string
     * @param gameMode     chosen GameMode
     * @param participants list of players in this instance
     */
    public GameInstance(MinigamesPlugin plugin,
                        Arena arena,
                        String type,
                        GameMode gameMode,
                        List<Player> participants) {
        this.plugin = plugin;
        this.arena = arena;
        this.type = type;
        this.gameMode = gameMode;
        this.participants.addAll(participants);
        this.id = UUID.randomUUID().toString();
        this.statsManager = plugin.getStatsManager();
    }

    /** @return unique ID of this game instance */
    public String getId() {
        return id;
    }

    /** @return minigame type (e.g., "TNT_RUN") */
    public String getType() {
        return type;
    }

    /** @return chosen game mode */
    public GameMode getGameMode() {
        return gameMode;
    }

    /** @return list of players in this game */
    public List<Player> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    /** @return the Arena assigned to this game */
    public Arena getArena() {
        return arena;
    }

    /**
     * Called once to start the game.
     * Concrete subclasses should:
     *  - Save each player's original location
     *  - Teleport participants into the arena
     *  - Initialize any game-specific state
     *  - Schedule per-tick updates if needed
     */
    public void start() {
        // 1) Save and teleport all participants to arena spawn/origin
        broadcastMessage("&aStarting " + type + " [" + gameMode + "]...");

        BlockVector3 originVec = arena.getOrigin();
        World bukkitWorld = arena.getWorld();
        Location originLocation = new Location(
                bukkitWorld,
                originVec.getX(),
                originVec.getY(),
                originVec.getZ()
        );

        for (Player p : participants) {
            // Save original location
            originalLocation.put(p, p.getLocation());

            // Teleport into arena
            p.teleport(originLocation);
            p.getInventory().clear(); // clear inventory (minigame should kit them later)
        }

        // 2) Initialize game-specific logic
        onGameStart();

        // 3) If subclass requires per-tick updates, schedule a repeating task
        if (requiresTicks()) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        }
    }

    /**
     * Called to cleanly end the game.
     * Concrete subclasses should:
     *  - Determine winners/losers
     *  - Cancel any scheduled tasks
     *  - Teleport participants back to their original locations
     *  - Record stats via StatsManager
     */
    public void stop() {
        // 1) Cancel tick task if running
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }

        // 2) Game-specific cleanup
        onGameEnd();

        // 3) Teleport participants back to their original location (or main spawn if missing)
        for (Player p : participants) {
            if (!p.isOnline()) continue;
            Location returnLoc = originalLocation.get(p);
            if (returnLoc != null) {
                p.teleport(returnLoc);
            } else {
                // Fallback: teleport to main world spawn
                World mainWorld = plugin.getServer().getWorlds().get(0);
                p.teleport(mainWorld.getSpawnLocation());
            }
        }

        // 4) Record stats
        statsManager.recordGameResult(this);
    }

    /**
     * Per-tick update (called every second by default, if requiresTicks() == true).
     * Concrete subclasses override this to implement timers, score checks, etc.
     */
    protected void tick() {
        // Default: no-op. Subclasses can override.
    }

    /**
     * @return true if this game needs per-tick updates; override in subclass if needed.
     */
    protected boolean requiresTicks() {
        return false;
    }

    /**
     * Called once when the game actually starts (right after teleporting players).
     * Subclasses must override to set up game-specific mechanics (e.g., give kits).
     */
    protected abstract void onGameStart();

    /**
     * Called once when the game is ending (before teleporting players away).
     * Subclasses must override to handle scoring, end-of-game effects, etc.
     */
    protected abstract void onGameEnd();

    /**
     * Broadcast a colored message to all participants.
     */
    protected void broadcastMessage(String message) {
        String colored = message.replace("&", "§");
        for (Player p : participants) {
            p.sendMessage(colored);
        }
    }

    /**
     * Called when a single player leaves mid-game (voluntarily or is eliminated).
     * This will:
     *  1) Remove them from participants list
     *  2) Cancel their scheduled tasks if any
     *  3) Teleport them back to original location (or fallback spawn)
     *  4) Notify remaining players
     *  5) If only 0 or 1 remain, end the game early
     *
     * Subclasses can override to add extra cleanup, but should call super.removePlayer(p) first.
     *
     * @param player The player to remove from this match.
     */
    public void removePlayer(Player player) {
        // 1) If not actually participating, nothing to do
        if (!participants.contains(player)) {
            return;
        }

        // 2) Remove from the participants list
        participants.remove(player);

        // 3) Cancel any scheduled task we had for this player (if subclasses use such tasks)
        if (task != null) {
            // In this base class, tick() is global; subclasses with per-player tasks should manage them separately
        }

        // 4) Teleport back to their original location (if we saved it)
        Location returnLoc = originalLocation.get(player);
        if (returnLoc != null) {
            player.teleport(returnLoc);
        } else {
            // Fallback: teleport to main world spawn
            World mainWorld = plugin.getServer().getWorlds().get(0);
            player.teleport(mainWorld.getSpawnLocation());
        }
        player.sendMessage("§eYou have left " + type + " [" + gameMode + "].");

        // Record a loss for the departing player immediately
        statsManager.recordLoss(player.getUniqueId(), type);

        // 5) Notify remaining participants
        for (Player p : participants) {
            p.sendMessage("§c" + player.getName() + " has left the game!");
        }

        // 6) If only one (or zero) participants remain, end the game early
        if (participants.size() <= 1) {
            // If one player remains, simply announce the winner.
            if (participants.size() == 1) {
                Player winner = participants.get(0);
                winner.sendMessage("§6You are the winner of " + type + "!");
            }
            // End the match
            stop();
            // Reset and free the arena
            plugin.getArenaService().resetArena(arena);
            arena.setInUse(false);
            plugin.logVerbose("Game " + id + " (" + type + ") ended early due to too few players.");
        }
    }
}
