package com.auroraschaos.minigames.game;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.auroraschaos.minigames.arena.Arena;
import com.auroraschaos.minigames.config.TTTConfig;
import com.auroraschaos.minigames.scoreboard.ScoreboardManager;
import com.auroraschaos.minigames.util.CountdownTimer;
import com.auroraschaos.minigames.util.SpectatorUtil;
import com.auroraschaos.minigames.game.ttt.TTTShop;
import com.sk89q.worldedit.math.BlockVector3;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Minimal Trouble in Terrorist Town implementation. Players receive hidden
 * roles and must eliminate the opposing team. Traitors win when they equal or
 * outnumber innocents. Innocents win when all traitors are eliminated or time
 * expires.
 */
public class TTTGame extends GameInstance implements Listener {

    private enum Role { INNOCENT, TRAITOR, DETECTIVE }

    private final int spawnYOffset;
    private final int gameDuration;
    private final double traitorRatio;
    private final double detectiveRatio;

    private final ScoreboardManager scoreboardManager;
    private final CountdownTimer countdownTimer;

    private final Map<Player, Role> roles = new HashMap<>();
    private final List<Player> alivePlayers = new ArrayList<>();
    private final List<Player> spectators = new ArrayList<>();
    private final TTTShop shop;

    private BukkitTask loopTask;
    private int timeLeft;

    public TTTGame(String type,
                   GameMode gameMode,
                   MinigamesPlugin plugin,
                   Arena arena,
                   List<Player> participants) {
        super(plugin, arena, type, gameMode, participants);
        this.scoreboardManager = plugin.getScoreboardManager();
        this.countdownTimer = plugin.getCountdownTimer();

        TTTConfig cfg = plugin.getConfigManager().getTTTConfig();
        this.spawnYOffset = cfg.getSpawnYOffset();
        this.gameDuration = cfg.getGameDuration();
        this.traitorRatio = cfg.getTraitorRatio();
        this.detectiveRatio = cfg.getDetectiveRatio();
        this.shop = new TTTShop(plugin, cfg.getShopItems());
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

        scoreboardManager.getOrCreateScoreboard(getId());
        scoreboardManager.setScoreLine(getId(), "title_" + getId(),
                "§5Trouble In Terrorist Town", 4);
        scoreboardManager.setScoreLine(getId(), "players_" + getId(),
                "Alive: " + participants.size(), 3);

        for (Player p : participants) {
            alivePlayers.add(p);
            p.teleport(spawn);
            p.getInventory().clear();
            scoreboardManager.showToPlayer(getId(), p);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }

        assignRoles();

        for (Player p : participants) {
            p.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
            Role role = roles.get(p);
            if (role == Role.TRAITOR || role == Role.DETECTIVE) {
                p.getInventory().setItem(8, shop.createOpenerItem());
            }
        }

        countdownTimer.startCountdown(getId(), gameDuration);
        timeLeft = gameDuration;
        loopTask = new BukkitRunnable() {
            @Override
            public void run() {
                scoreboardManager.setScoreLine(getId(), "players_" + getId(),
                        "Alive: " + alivePlayers.size(), 3);
                timeLeft--;
                if (timeLeft <= 0) {
                    broadcastMessage("§aTime's up! Innocents win.");
                    plugin.getGameManager().endGame(getId());
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    @Override
    protected void onGameEnd() {
        HandlerList.unregisterAll(this);
        if (loopTask != null) loopTask.cancel();
        countdownTimer.cancelCountdown(getId());

        for (Player p : participants) {
            scoreboardManager.removeFromPlayer(p);
        }
        for (Player p : spectators) {
            scoreboardManager.removeFromPlayer(p);
        }
        scoreboardManager.clearArenaScoreboard(getId());
        shop.unregister();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        if (!alivePlayers.contains(p)) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDeathMessage(null);
        Player killer = p.getKiller();
        if (killer != null && alivePlayers.contains(killer)) {
            Role killerRole = roles.get(killer);
            Role victimRole = roles.get(p);
            if (killerRole == Role.TRAITOR) {
                shop.addPoint(killer);
            } else if (killerRole == Role.DETECTIVE && victimRole == Role.TRAITOR) {
                shop.addPoint(killer);
            }
        }
        eliminatePlayer(p);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (spectators.contains(p)) {
            p.setGameMode(org.bukkit.GameMode.SPECTATOR);
        }
    }

    private void assignRoles() {
        List<Player> shuffled = new ArrayList<>(participants);
        Collections.shuffle(shuffled);
        int traitorCount = Math.max(1, (int) Math.round(shuffled.size() * traitorRatio));
        int detectiveCount = Math.min(shuffled.size() - traitorCount,
                (int) Math.round(shuffled.size() * detectiveRatio));
        int index = 0;
        for (int i = 0; i < traitorCount; i++) {
            Player p = shuffled.get(index++);
            roles.put(p, Role.TRAITOR);
            p.sendMessage("§cYou are a TRAITOR! Eliminate everyone else.");
        }
        for (int i = 0; i < detectiveCount; i++) {
            Player p = shuffled.get(index++);
            roles.put(p, Role.DETECTIVE);
            p.sendMessage("§9You are the DETECTIVE! Find the traitors.");
        }
        while (index < shuffled.size()) {
            Player p = shuffled.get(index++);
            roles.put(p, Role.INNOCENT);
            p.sendMessage("§aYou are INNOCENT. Survive and uncover the traitors.");
        }
    }

    private void eliminatePlayer(Player p) {
        if (!alivePlayers.remove(p)) return;
        SpectatorUtil.makeSpectator(p, arena);
        spectators.add(p);
        scoreboardManager.showToPlayer(getId(), p);
        p.sendMessage("§cYou were eliminated!");
        checkWinCondition();
    }

    private void checkWinCondition() {
        int traitors = 0;
        for (Player p : alivePlayers) {
            if (roles.get(p) == Role.TRAITOR) traitors++;
        }
        int others = alivePlayers.size() - traitors;
        if (traitors == 0) {
            broadcastMessage("§aInnocents win!");
            plugin.getGameManager().endGame(getId());
        } else if (traitors >= others) {
            broadcastMessage("§cTraitors win!");
            plugin.getGameManager().endGame(getId());
        }
    }


    @Override
    protected boolean requiresTicks() {
        return false;
    }

    @Override
    protected void tick() {
        // unused
    }
}
