package com.auroraschaos.minigames.game;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.auroraschaos.minigames.arena.Arena;
import com.auroraschaos.minigames.scoreboard.ScoreboardManager;
import com.auroraschaos.minigames.util.CountdownTimer;
import com.auroraschaos.minigames.util.SpectatorUtil;

import com.auroraschaos.minigames.config.SpleefConfig;

import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basic Spleef implementation following the style of {@link TNTRunGame}.
 * Players spawn on a snow platform and receive shovels to break blocks
 * beneath opponents. Falling into the void eliminates a player. The
 * last remaining player wins or the game ends when time expires.
 */
public class SpleefGame extends GameInstance implements Listener {

    /** Height offset from arena origin for spawn location. */
    private final int spawnYOffset;

    /** Total round duration in seconds. */
    private final int gameDuration;

    /** Cooldown between snowball throws in seconds. */
    private final int snowballCooldown;

    /** Block types snowballs can break. */
    private final List<Material> breakableBlocks = new ArrayList<>();

    /** Last time a player threw a snowball (ms) */
    private final Map<Player, Long> lastThrow = new HashMap<>();

    private final List<Player> alivePlayers = new ArrayList<>();
    private final List<Player> spectators   = new ArrayList<>();

    private final ScoreboardManager scoreboardManager;
    private final CountdownTimer countdownTimer;

    /** Periodic task that checks win condition. */
    private BukkitTask checkTask;

    public SpleefGame(String type,
                      GameMode gameMode,
                      MinigamesPlugin plugin,
                      Arena arena,
                      List<Player> participants) {
        super(plugin, arena, type, gameMode, participants);
        this.scoreboardManager = plugin.getScoreboardManager();
        this.countdownTimer    = plugin.getCountdownTimer();

        // Load configuration via ConfigManager
        SpleefConfig cfg = plugin.getConfigManager().getSpleefConfig();
        this.spawnYOffset = cfg.getSpawnYOffset();
        this.gameDuration = cfg.getGameDuration();
        this.snowballCooldown = cfg.getSnowballCooldown();
        this.breakableBlocks.addAll(cfg.getBreakableBlocks());
    }

    @Override
    protected void onGameStart() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        BlockVector3 originVec = arena.getOrigin();
        Location spawn = new Location(
                arena.getWorld(),
                originVec.getX() + 0.5,
                originVec.getY() + spawnYOffset,
                originVec.getZ() + 0.5
        );

        // Prepare scoreboard
        scoreboardManager.getOrCreateScoreboard(getId());
        scoreboardManager.setScoreLine(getId(), "title_" + getId(), "§dSpleef", 4);
        scoreboardManager.setScoreLine(getId(), "players_" + getId(),
                "Players Left: " + participants.size(), 3);

        for (Player p : participants) {
            alivePlayers.add(p);
            p.teleport(spawn);
            p.getInventory().clear();
            p.getInventory().addItem(new ItemStack(Material.DIAMOND_SHOVEL));
            scoreboardManager.showToPlayer(getId(), p);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }

        countdownTimer.startCountdown(getId(), gameDuration);

        checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Update scoreboard
                scoreboardManager.setScoreLine(getId(), "players_" + getId(),
                        "Players Left: " + alivePlayers.size(), 3);

                if (alivePlayers.size() <= 1) {
                    announceWinner();
                    plugin.getGameManager().endGame(getId());
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    @Override
    protected void onGameEnd() {
        HandlerList.unregisterAll(this);
        if (checkTask != null) checkTask.cancel();
        countdownTimer.cancelCountdown(getId());

        for (Player p : participants) {
            scoreboardManager.removeFromPlayer(p);
        }
        for (Player p : spectators) {
            scoreboardManager.removeFromPlayer(p);
            SpectatorUtil.returnToLobby(p,
                    plugin.getServer().getWorlds().get(0).getSpawnLocation());
        }
        scoreboardManager.clearArenaScoreboard(getId());
        lastThrow.clear();
    }

    @Override
    protected boolean requiresTicks() {
        return false;
    }

    @Override
    protected void tick() {
        // unused
    }

    private void announceWinner() {
        if (alivePlayers.isEmpty()) {
            broadcastMessage("§eNo winners this round.");
        } else {
            Player winner = alivePlayers.get(0);
            broadcastMessage("§a" + winner.getName() + " wins Spleef!");
            winner.playSound(winner.getLocation(),
                    Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
    }

    private void eliminatePlayer(Player p) {
        if (!alivePlayers.remove(p)) return;
        SpectatorUtil.makeSpectator(p, arena);
        spectators.add(p);
        scoreboardManager.showToPlayer(getId(), p);
        p.sendMessage("§cYou were eliminated!");
        p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.5f);
    }

    // ---------------------- Event Handlers ----------------------

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (!alivePlayers.contains(p)) return;
        Block b = event.getBlock();
        Material type = b.getType();
        if (type != Material.SNOW_BLOCK && type != Material.SNOW) {
            event.setCancelled(true);
        } else {
            event.setDropItems(false);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (!alivePlayers.contains(p)) return;
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (p.getInventory().getItemInMainHand().getType() == Material.DIAMOND_SHOVEL) {
                long now = System.currentTimeMillis();
                long last = lastThrow.getOrDefault(p, 0L);
                if (now - last >= snowballCooldown * 1000L) {
                    lastThrow.put(p, now);
                    p.launchProjectile(Snowball.class);
                    p.playSound(p.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 1f);
                }
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        Player shooter = (Player) event.getEntity().getShooter();
        if (!alivePlayers.contains(shooter)) return;

        Block hit = event.getHitBlock();
        if (hit != null && breakableBlocks.contains(hit.getType())) {
            hit.setType(Material.AIR);
        }
    }

    @EventHandler
    public void onVoidDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        if (!alivePlayers.contains(p)) return;
        event.setCancelled(true);
        eliminatePlayer(p);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (!alivePlayers.contains(p)) return;
        if (p.getLocation().getY() < arena.getOrigin().getY() - 1) {
            eliminatePlayer(p);
        }
    }
}

