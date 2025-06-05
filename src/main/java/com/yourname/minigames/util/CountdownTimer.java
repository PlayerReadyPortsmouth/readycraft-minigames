package com.yourname.minigames.util;

import com.yourname.minigames.scoreboard.ScoreboardManager;
import com.yourname.minigames.MinigamesPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages countdown timers per arena. Each second, updates the scoreboard
 * line “timer_<arenaId>” to show remaining time.
 */
public class CountdownTimer {

    private final MinigamesPlugin plugin;
    private final ScoreboardManager scoreboardManager;
    private final Map<String, Integer> timeLeftMap = new HashMap<>();
    private final Map<String, BukkitRunnable> tasks = new HashMap<>();

    public CountdownTimer(MinigamesPlugin plugin, ScoreboardManager sbm) {
        this.plugin = plugin;
        this.scoreboardManager = sbm;
    }

    /**
     * Start a countdown for a given arena ID and duration (seconds).
     * Will schedule a repeating task that decrements once per second.
     *
     * @param arenaId       unique ID of the game/arena
     * @param durationSecs  initial duration in seconds
     */
    public void startCountdown(String arenaId, int durationSecs) {
        cancelCountdown(arenaId);
        timeLeftMap.put(arenaId, durationSecs);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                int timeLeft = timeLeftMap.getOrDefault(arenaId, 0);
                if (timeLeft <= 0) {
                    // Time’s up: stop timer and cancel
                    cancelCountdown(arenaId);
                    return;
                }
                // Update scoreboard line, e.g. “Time Left: 00:59”
                String minutes = String.format("%02d", timeLeft / 60);
                String seconds = String.format("%02d", timeLeft % 60);
                scoreboardManager.setScoreLine(arenaId,
                        "timer_" + arenaId,
                        "Time Left: " + minutes + ":" + seconds,
                        2  // score position
                );
                timeLeftMap.put(arenaId, timeLeft - 1);
            }
        };

        task.runTaskTimer(plugin, 0L, 20L);
        tasks.put(arenaId, task);
    }

    /** Cancel and remove the countdown for a given arena ID */
    public void cancelCountdown(String arenaId) {
        BukkitRunnable task = tasks.remove(arenaId);
        if (task != null) {
            task.cancel();
        }
        timeLeftMap.remove(arenaId);
        // Optionally clear the scoreboard line
        scoreboardManager.setScoreLine(arenaId, "timer_" + arenaId, "", 2);
    }
}
