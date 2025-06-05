package com.yourname.minigames.game;

import com.yourname.minigames.MinigamesPlugin;
import com.yourname.minigames.arena.Arena;
import com.yourname.minigames.scoreboard.ScoreboardManager;
import com.yourname.minigames.util.CountdownTimer;
import com.yourname.minigames.util.SpectatorUtil;
import com.yourname.minigames.game.GameMode;

import com.sk89q.worldedit.math.BlockVector3;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A polished TNTRunGame implementation extending GameInstance.
 * - Teleports players to a wool platform.
 * - Removes blocks under each player every second.
 * - Tracks alive players and moves eliminated ones into spectator mode.
 * - Displays a per-arena scoreboard with time remaining and players left.
 * - Ends when one (or zero) players remain or time runs out.
 */
public class TNTRunGame extends GameInstance implements Listener {

    /** How high above the origin to spawn players */
    private static final int SPAWN_Y_OFFSET = 10;

    /** Interval (in ticks) between block removal passes (20 ticks = 1 second) */
    private static final long BLOCK_REMOVE_INTERVAL = 20L;

    /** Total game duration in seconds (e.g., 5 minutes) */
    private static final int GAME_DURATION = 300;

    /** Handles the repeating block-removal and game logic */
    private BukkitTask removalTask;

    /** List of players still “alive” */
    private final List<Player> alivePlayers = new ArrayList<>();

    /** List of players in spectator mode */
    private final List<Player> spectators = new ArrayList<>();

    /** Manages scoreboards for this arena */
    private final ScoreboardManager scoreboardManager;

    /** Manages the countdown timer for this arena */
    private final CountdownTimer countdownTimer;

    public TNTRunGame(String type,
                      GameMode gameMode,
                      MinigamesPlugin plugin,
                      Arena arena,
                      List<Player> participants) {
        super(plugin, arena, type, gameMode, participants);
        this.scoreboardManager = plugin.getScoreboardManager();
        this.countdownTimer  = plugin.getCountdownTimer();
    }

    @Override
    protected void onGameStart() {
        // 1) Register event listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // 2) Teleport participants and initialize alive list
        for (Player p : participants) {
            alivePlayers.add(p);

            // Replace toLocation(...) with manual Location construction
            BlockVector3 originVec = arena.getOrigin();
            Location spawnLocation = new Location(
                    arena.getWorld(),
                    originVec.getX() + 0.5,
                    originVec.getY() + SPAWN_Y_OFFSET,
                    originVec.getZ() + 0.5
            );
            p.teleport(spawnLocation);
            p.getInventory().clear();
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        // 3) Initialize per-arena scoreboard and show to participants
        scoreboardManager.getOrCreateScoreboard(getId());
        for (Player p : participants) {
            scoreboardManager.showToPlayer(getId(), p);
        }
        // 3a) Initialize scoreboard lines
        scoreboardManager.setScoreLine(getId(), "title_" + getId(), "§aTNT Run", 4);
        scoreboardManager.setScoreLine(getId(), "players_" + getId(),
                "Players Left: " + alivePlayers.size(), 3);

        // 4) Start countdown timer (updates scoreboard via CountdownTimer)
        countdownTimer.startCountdown(getId(), GAME_DURATION);

        // 5) Schedule block removal & game logic
        setupRemovalTask();
    }

    @Override
    protected void onGameEnd() {
        // 1) Cancel the repeating task if running
        if (removalTask != null && !removalTask.isCancelled()) {
            removalTask.cancel();
        }
        // 2) Unregister event listeners
        EntityDamageEvent.getHandlerList().unregister(this);
        PlayerMoveEvent.getHandlerList().unregister(this);

        // 3) Clear scoreboard from all participants & spectators
        countdownTimer.cancelCountdown(getId());
        for (Player p : participants) {
            scoreboardManager.removeFromPlayer(p);
        }
        for (Player spec : spectators) {
            scoreboardManager.removeFromPlayer(spec);
        }
        scoreboardManager.clearArenaScoreboard(getId());

        // 4) Return spectators to lobby
        for (Player spec : spectators) {
            SpectatorUtil.returnToLobby(spec,
                plugin.getServer().getWorlds().get(0).getSpawnLocation());
        }
    }

    @Override
    protected boolean requiresTicks() {
        // We handle our own repeating task; parent tick() is unused
        return false;
    }

    @Override
    protected void tick() {
        // Not used; block removal and logic is in removalTask
    }

    /**
     * Eliminates a player from alivePlayers:
     *  - Converts them to a spectator in-arena
     *  - Shows them the arena scoreboard
     *  - Plays elimination sound & message
     */
    private void eliminatePlayer(Player p) {
        if (alivePlayers.remove(p)) {
            // 1) Switch to spectator mode
            SpectatorUtil.makeSpectator(p, arena);
            spectators.add(p);

            // 2) Show scoreboard to spectator
            scoreboardManager.showToPlayer(getId(), p);

            // 3) Notify elimination
            p.sendMessage("§cYou have been eliminated! Now in Spectator mode.");
            p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.5f);
        }
    }

    // ----------------------------------------
    // Event Handlers
    // ----------------------------------------

    @EventHandler
    public void onPlayerFall(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        if (!alivePlayers.contains(p)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            eliminatePlayer(p);
            event.setCancelled(true); // Prevent default death
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Ensure spectators remain in spectator mode and cannot interact
        Player p = event.getPlayer();
        if (spectators.contains(p)) {
            // Optional: keep them at a fixed Y or within region
            // WorldGuard region should already confine them
            p.setGameMode(org.bukkit.GameMode.SPECTATOR);
        }
    }

    private void setupRemovalTask() {
        removalTask = new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Player> iterator = alivePlayers.iterator();
                while (iterator.hasNext()) {
                    Player p = iterator.next();
                    if (!p.isOnline()) {
                        eliminatePlayer(p);
                        iterator.remove();
                        continue;
                    }

                    // Remove any wool block directly beneath the player
                    Block beneath = p.getLocation().subtract(0, 1, 0).getBlock();
                    Material type = beneath.getType();
                    switch (type) {
                        case WHITE_WOOL:
                        case RED_WOOL:
                        case YELLOW_WOOL:
                        case BLUE_WOOL:
                        case GREEN_WOOL:
                        case ORANGE_WOOL:
                        case PINK_WOOL:
                        case LIGHT_BLUE_WOOL:
                            beneath.setType(Material.AIR);
                            break;
                        default:
                            // not wool—do nothing
                    }

                    // If the player fell into the void (Y < 0), eliminate them
                    if (p.getLocation().getY() < 0) {
                        eliminatePlayer(p);
                        iterator.remove();
                    }
                }

                // Update "Players Left" on the scoreboard
                scoreboardManager.setScoreLine(
                    getId(),
                    "players_" + getId(),
                    "Players Left: " + alivePlayers.size(),
                    3
                );

                // Check end condition: one or zero players remain
                if (alivePlayers.size() <= 1) {
                    if (alivePlayers.size() == 1) {
                        Player winner = alivePlayers.get(0);
                        broadcastMessage("§a" + winner.getName() + " wins TNT Run!");
                        winner.playSound(
                            winner.getLocation(),
                            Sound.UI_TOAST_CHALLENGE_COMPLETE,
                            1.0f, 1.0f
                        );
                    } else {
                        broadcastMessage("§eNo winners this round.");
                    }
                    // End the game (reset arena, free slot, record stats, etc.)
                    plugin.getGameManager().endGame(getId());
                }
            }
        }.runTaskTimer(plugin, 20L, BLOCK_REMOVE_INTERVAL);
    }
}
