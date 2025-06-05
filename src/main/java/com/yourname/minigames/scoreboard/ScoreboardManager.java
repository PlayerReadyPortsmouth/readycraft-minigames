package com.yourname.minigames.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages per‐arena scoreboards. Each arena has its own scoreboard
 * so you can display timers, remaining players, etc., visible only to
 * participants (and optionally spectators).
 */
public class ScoreboardManager {

    /** Holds the scoreboard and objective for each arena ID */
    private final Map<String, ArenaScoreboard> arenaBoards = new HashMap<>();

    /**
     * Creates (or retrieves) a scoreboard for a given arena ID.
     * If none exists, it registers a new one with a unique objective name.
     *
     * @param arenaId  unique ID of the arena/game instance
     * @return the ArenaScoreboard wrapper
     */
    public ArenaScoreboard getOrCreateScoreboard(String arenaId) {
        if (arenaBoards.containsKey(arenaId)) {
            return arenaBoards.get(arenaId);
        }

        // Create a new scoreboard
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        String objName = "arena_" + arenaId.substring(0, Math.min(10, arenaId.length()));
        Objective obj = board.registerNewObjective(objName, "dummy", "§aArena Status");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        ArenaScoreboard arenaBoard = new ArenaScoreboard(board, obj);
        arenaBoards.put(arenaId, arenaBoard);
        return arenaBoard;
    }

    /**
     * Show the arena’s scoreboard to a player (participant or spectator).
     */
    public void showToPlayer(String arenaId, Player player) {
        ArenaScoreboard asb = arenaBoards.get(arenaId);
        if (asb == null) return;
        player.setScoreboard(asb.board);
    }

    /**
     * Removes the scoreboard from a player (resets to default).
     */
    public void removeFromPlayer(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    /**
     * Update or add a line on the scoreboard. Lower scores appear lower in the list.
     *
     * @param arenaId  ID of arena
     * @param lineKey  a unique key for this line (e.g., "timer", "players")
     * @param text     the text to display (colored)
     * @param score    integer score (ordering)
     */
    public void setScoreLine(String arenaId, String lineKey, String text, int score) {
        ArenaScoreboard asb = getOrCreateScoreboard(arenaId);

        // Remove old entry if present
        if (asb.entries.containsKey(lineKey)) {
            Score old = asb.entries.get(lineKey);
            asb.objective.getScoreboard().resetScores(old.getEntry());
        }

        // Create new entry
        Score sc = asb.objective.getScore(text);
        sc.setScore(score);
        asb.entries.put(lineKey, sc);
    }

    /**
     * Clears all lines and unregisters the objective for the arena.
     * Call when the game ends.
     */
    public void clearArenaScoreboard(String arenaId) {
        ArenaScoreboard asb = arenaBoards.remove(arenaId);
        if (asb == null) return;
        asb.objective.unregister();
    }

    /**
     * Internal wrapper to hold the scoreboard and its entries.
     */
    private static class ArenaScoreboard {
        final Scoreboard board;
        final Objective objective;
        final Map<String, Score> entries = new HashMap<>();

        ArenaScoreboard(Scoreboard board, Objective objective) {
            this.board = board;
            this.objective = objective;
        }
    }
}
