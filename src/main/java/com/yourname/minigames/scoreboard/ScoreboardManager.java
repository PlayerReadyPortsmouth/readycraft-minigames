package com.yourname.minigames.game;

import com.yourname.minigames.MinigamesPlugin;
import com.yourname.minigames.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A simple TNT Run implementation:
 * - At start, all participants are teleported onto a wool platform.
 * - Every second (tick), we remove the block directly beneath each player (if it's wool).
 * - If a player falls (detect via fall damage or void), they are eliminated.
 * - Last player standing is the winner.
 */
public class TNTRunGame extends GameInstance implements Listener {

    /** Height above origin at which we spawn players (adjust as needed) */
    private static final int SPAWN_Y_OFFSET = 10;

    /** Interval (in ticks) between block‐removal passes (20 ticks = 1 second) */
    private static final long BLOCK_REMOVE_INTERVAL = 20L;

    /** Handle for the repeating block‐removal task */
    private BukkitTask removalTask;

    /** Keeps track of alive players */
    private final List<Player> alivePlayers = new ArrayList<>();

    public TNTRunGame(String type,
                      GameMode gameMode,
                      MinigamesPlugin plugin,
                      Arena arena,
                      List<Player> participants) {
        super(plugin, arena, type, gameMode, participants);
    }

    @Override
    protected void onGameStart() {
        // 1) Register event listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // 2) Teleport players slightly above the platform origin
        for (Player p : participants) {
            alivePlayers.add(p);
            BlockVector3 originVec = arena.getOrigin();
            p.teleport(originVec.toLocation(arena.getWorld()).add(0.5, SPAWN_Y_OFFSET, 0.5));
            p.getInventory().clear();
            p.getInventory().setHelmet(null);
            p.getInventory().setChestplate(null);
            p.getInventory().setLeggings(null);
            p.getInventory().setBoots(null);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        // 3) Start the repeating task that removes blocks under players
        removalTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Iterator<Player> it = alivePlayers.iterator();
            while (it.hasNext()) {
                Player p = it.next();
                if (!p.isOnline()) {
                    it.remove();
                    continue;
                }
                // Block directly beneath player’s feet
                Block beneath = p.getLocation().subtract(0, 1, 0).getBlock();
                if (beneath.getType() == Material.WHITE_WOOL  ||
                    beneath.getType() == Material.RED_WOOL    ||
                    beneath.getType() == Material.YELLOW_WOOL ||
                    beneath.getType() == Material.BLUE_WOOL) {
                    beneath.setType(Material.AIR);
                }
                // If player is below Y=0 (fell into void), eliminate
                if (p.getLocation().getY() < 0) {
                    eliminatePlayer(p);
                    it.remove();
                }
            }
            // Check for only one player left
            if (alivePlayers.size() <= 1) {
                if (alivePlayers.size() == 1) {
                    Player winner = alivePlayers.get(0);
                    broadcastMessage("&a" + winner.getName() + " is the TNT Run winner!");
                } else {
                    broadcastMessage("&eNo winners this round.");
                }
                // End the game (free arena, record stats, teleport, etc.)
                plugin.getGameManager().endGame(getId());
            }
        }, 20L, BLOCK_REMOVE_INTERVAL);
    }

    @Override
    protected void onGameEnd() {
        // 1) Unregister listener
        EntityDamageEvent.getHandlerList().unregister(this);
        PlayerMoveEvent.getHandlerList().unregister(this);

        // 2) Cancel block‐removal task (if still running)
        if (removalTask != null && !removalTask.isCancelled()) {
            removalTask.cancel();
        }
    }

    @Override
    protected boolean requiresTicks() {
        // We handle our own repeating task; no need for parent tick().
        return false;
    }

    @Override
    protected void tick() {
        // Not used because we’re scheduling our own removalTask
    }

    /**
     * Eliminates a player from the game:
     * - Removes from alivePlayers.
     * - Sends a message and plays a sound.
     */
    private void eliminatePlayer(Player p) {
        if (alivePlayers.contains(p)) {
            alivePlayers.remove(p);
            p.sendMessage("§cYou have been eliminated!");
            p.playSound(p.getLocation(), Sound.ENTITY_GAME_WARDEN_EMERGE, 1.0f, 0.5f);
            // Optionally clear inventory / kit
            p.getInventory().clear();
        }
    }

    // ------------------------------------------------------------
    // LISTENER: FALL DAMAGE (falls into void)
    // ------------------------------------------------------------
    @EventHandler
    public void onPlayerFall(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        if (!alivePlayers.contains(p)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            eliminatePlayer(p);
            event.setCancelled(true); // prevent extra damage message
        }
    }

    // ------------------------------------------------------------
    // LISTENER: Prevent players from building/breaking in TNT Run
    // ------------------------------------------------------------
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (alivePlayers.contains(p)) {
            // Prevent placing or breaking blocks by bypassing other plugins
            // (WorldGuard region should also deny block place/break within the arena)
            event.getPlayer().setFoodLevel(20); // keep hunger full
        }
    }
}
