package com.auroraschaos.minigames.game.race;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.auroraschaos.minigames.arena.Arena;
import com.auroraschaos.minigames.game.GameInstance;
import com.auroraschaos.minigames.game.GameMode;
import com.auroraschaos.minigames.scoreboard.ScoreboardManager;
import com.auroraschaos.minigames.util.CountdownTimer;
import com.auroraschaos.minigames.util.SpectatorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * RaceGame handles a boat-based kart race, selecting a track and managing the race lifecycle.
 */
public class RaceGame extends GameInstance {
    private final TrackConfig trackConfig;
    private final KartConfig defaultKartConfig;
    private final List<KartConfig> allKartConfigs;
    private final ScoreboardManager scoreboardManager;
    private final CountdownTimer countdownTimer;

    private final Map<Player, KartVehicle> karts = new HashMap<>();
    private BukkitTask startTask;
    private BukkitTask raceLoop;

    public RaceGame(String type,
                    GameMode gameMode,
                    MinigamesPlugin plugin,
                    Arena arena,
                    List<Player> participants,
                    List<File> availableTracks,
                    Map<String, KartConfig> kartConfigs,
                    String specifiedTrackName) throws IOException {
        super(plugin, arena, type, gameMode, participants);
        this.scoreboardManager = plugin.getScoreboardManager();
        this.countdownTimer   = plugin.getCountdownTimer();

        // Load kart configs
        this.allKartConfigs    = List.copyOf(kartConfigs.values());
        this.defaultKartConfig = allKartConfigs.get(0);

        // Choose track
        plugin.saveResource("races/defaults.yml", false);
        File trackDefaults = new File(plugin.getDataFolder(), "races/defaults.yml");
        File trackFile     = chooseTrack(availableTracks, specifiedTrackName);
        this.trackConfig   = new TrackConfig(trackFile, trackDefaults);
    }

    private File chooseTrack(List<File> tracks, String name) {
        if (name != null) {
            for (File f : tracks) {
                if (f.getName().equalsIgnoreCase(name + ".yml")) {
                    return f;
                }
            }
        }
        return tracks.get(new Random().nextInt(tracks.size()));
    }

    @Override
    protected void onGameStart() {
        // Initialize and display scoreboard
        scoreboardManager.getOrCreateScoreboard(getId());
        for (Player p : participants) {
            scoreboardManager.showToPlayer(getId(), p);
        }
        // Title line
        scoreboardManager.setScoreLine(getId(), "title_" + getId(),
            trackConfig.getString("displayName"), 4);

        // Spawn karts
        List<Location> starts      = trackConfig.getLocationList("startLine");
        List<Location> checkpoints = trackConfig.getLocationList("checkpoints");
        for (int i = 0; i < participants.size(); i++) {
            Player p = participants.get(i);
            Location loc = starts.get(i % starts.size());
            var boat = p.getWorld().spawn(loc, org.bukkit.entity.Boat.class);
            boat.setGravity(false);
            boat.addPassenger(p);
            karts.put(p, new KartVehicle(p, boat, defaultKartConfig, checkpoints));
        }

        // Countdown then start race
        int secs = trackConfig.getInt("countdownSeconds");
        countdownTimer.startCountdown(getId(), secs);
        startTask = new BukkitRunnable() {
            @Override public void run() {
                launchRaceLoop();
            }
        }.runTaskLater(plugin, secs * 20L);
        plugin.logVerbose(String.format(
                "[RaceGame] Started with %d players on %s",
                participants.size(), arena.getName()));
    }

    private void launchRaceLoop() {
        // Remove countdown line
        countdownTimer.cancelCountdown(getId());
        // Broadcast GO
        String go = trackConfig.getString("messages.go");
        if (go != null) Bukkit.getServer().broadcastMessage(go);

        // Schedule per-tick race updates
        int interval = trackConfig.getInt("scoreboard.updateInterval");
        raceLoop = new BukkitRunnable() {
            @Override public void run() {
                for (KartVehicle kv : karts.values()) {
                    kv.updateMovement();
                    kv.handleOffTrack();
                }
                // Update scoreboard lines
                int line = 3;
                for (Map.Entry<Player, KartVehicle> e : karts.entrySet()) {
                    Player p = e.getKey();
                    KartVehicle kv = e.getValue();
                    String text = p.getName() + " - Lap " + kv.getLap() + "/" + trackConfig.getInt("totalLaps");
                    scoreboardManager.setScoreLine(getId(), "lap_" + p.getName(), text, line--);
                }
                // Check finish
                if (karts.values().stream().allMatch(KartVehicle::hasFinishedLap)) {
                    onGameEnd();
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    @Override
    protected void onGameEnd() {
        if (startTask != null) startTask.cancel();
        if (raceLoop   != null) raceLoop.cancel();

        // Clean up boats
        karts.values().forEach(kv -> kv.getBoat().remove());

        // Return players
        Location spawn = plugin.getServer().getWorlds().get(0).getSpawnLocation();
        for (Player p : participants) {
            SpectatorUtil.returnToLobby(p, spawn);
            scoreboardManager.removeFromPlayer(p);
        }
        scoreboardManager.clearArenaScoreboard(getId());

        // End in GameManager
        plugin.getStatsManager().recordGameResult(this);
        plugin.getGameManager().endGame(getId());
        plugin.logVerbose("[RaceGame] Ended on arena " + arena.getName());
    }

    @Override
    protected boolean requiresTicks() {
        return false;
    }
}
