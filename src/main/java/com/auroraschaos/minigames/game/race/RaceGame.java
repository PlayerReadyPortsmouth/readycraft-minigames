package com.auroraschaos.minigames.game.race;

import com.auroraschaos.minigames.game.GameInstance;
import com.auroraschaos.minigames.game.GameManager;
import com.auroraschaos.minigames.util.SpectatorUtil;
import com.auroraschaos.minigames.util.CountdownTimer;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RaceGame extends GameInstance {
    private final TrackConfig cfg;
    private final Map<org.bukkit.entity.Player, KartVehicle> karts = new HashMap<>();
    private final CountdownTimer preTimer;
    private BukkitRunnable raceLoop;

    public RaceGame(GameManager plugin, String arenaName, TrackConfig cfg) {
        super(plugin, arenaName);
        this.cfg = cfg;
        this.preTimer = new CountdownTimer(plugin, cfg.getInt("countdown"), arenaName);
    }

    @Override
    protected void onGameStart() {
        List<org.bukkit.Location> starts = cfg.getLocationList("startLine");
        int idx = 0;
        for (org.bukkit.entity.Player p : participants) {
            org.bukkit.Location loc = starts.get(idx++ % starts.size());
            org.bukkit.entity.Boat boat = (org.bukkit.entity.Boat) getArenaWorld().spawn(loc, org.bukkit.entity.Boat.class);
            boat.setGravity(false);
            boat.addPassenger(p);
            karts.put(p, new KartVehicle(p, boat, cfg));
        }

        preTimer.start(() -> {
            raceLoop = new BukkitRunnable() {
                @Override public void run() {
                    karts.values().forEach(kv -> {
                        kv.updateMovement(); kv.handleOffTrack(); kv.spawnParticles();
                        // kv.updateProgress(trackPath);
                    });
                    plugin.getScoreboardManager().updateRaceBoard(arenaName, participants, karts, cfg);
                    if (checkWinCondition()) {
                        endGame(); cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, cfg.getInt("scoreboard.updateInterval"));
            Bukkit.broadcastMessage(cfg.getString("messages.go"));
        });
    }

    protected boolean checkWinCondition() {
        long done = karts.values().stream().filter(KartVehicle::hasFinishedLap).count();
        return done == participants.size();
    }



    @Override
    protected void onGameEnd() {
        raceLoop.cancel();
        karts.values().forEach(kv -> kv.getBoat().remove());
        karts.clear();
        participants.forEach(p -> SpectatorUtil.returnToLobby(p, arenaName));
        plugin.getStatsManager().recordRaceResults(arenaName, karts);
        plugin.getGameManager().freeArena(arenaName);
    }
}
