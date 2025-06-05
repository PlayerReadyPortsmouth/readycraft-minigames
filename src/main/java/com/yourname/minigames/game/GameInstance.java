package com.yourname.minigames.game;

import com.yourname.minigames.MinigamesPlugin;
import com.yourname.minigames.arena.Arena;
import com.yourname.minigames.stats.StatsManager;
import com.yourname.minigames.game.GameMode;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.sk89q.worldedit.math.BlockVector3;

import java.util.*;

/**
 * Abstract base class for all minigame instances.
 * Each running game (e.g. a single round of TNT Run) will be represented
 * by a subclass of GameInstance.
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
     *  - Teleport participants into the arena
     *  - Initialize any game-specific state
     *  - Schedule per-tick updates if needed
     */
    public void start() {
        // 1) Teleport all participants to arena spawn/origin
        broadcastMessage("&aStarting " + type + " [" + gameMode + "]...");

        // Compute a Bukkit Location from the arena's BlockVector3 origin:
        BlockVector3 originVec = arena.getOrigin();
        World bukkitWorld = arena.getWorld();
        Location originLocation = new Location(
                bukkitWorld,
                originVec.getX(),
                originVec.getY(),
                originVec.getZ()
        );

        for (Player p : participants) {
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
     *  - Teleport participants back to lobby or spawn
     *  - Cancel any scheduled tasks
     *  - Record stats via StatsManager
     */
    public void stop() {
        // 1) Cancel tick task if running
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }

        // 2) Game-specific cleanup
        onGameEnd();

        // 3) Teleport participants out (plugin.getServer().getWorld("lobby") etc.)
        for (Player p : participants) {
            if (p.isOnline()) {
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
}
