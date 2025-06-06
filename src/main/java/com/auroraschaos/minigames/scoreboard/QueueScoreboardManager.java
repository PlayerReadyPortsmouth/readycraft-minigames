package com.auroraschaos.minigames.scoreboard;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.auroraschaos.minigames.game.GameManager;
import com.auroraschaos.minigames.game.GameMode;
import com.auroraschaos.minigames.game.GameManager.QueueEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

/**
 * Manages a per‐queue sidebar scoreboard that shows:
 *  - “Queued: <gameType> [<mode>]”
 *  - “Waiting: <currentSize> / <maxPlayers>”
 *  - (Optionally) next few entries: either player-names or party-colors
 *
 * Usage:
 *  - When GameManager.enqueue(...) is called, also call queueSB.updateQueueScoreboard(...)
 *  - When GameManager.dequeue(...) is called, rebuild and re–send scoreboard for that queue
 *  - When queue dissolves or game starts, call queueSB.clearQueueScoreboard(...)
 *
 * We identify each queue uniquely by “<gameType>_<mode>”, same as GameManager’s buildQueueKey().
 */
public class QueueScoreboardManager {

    private final MinigamesPlugin plugin;
    private final GameManager gameManager;

    public QueueScoreboardManager(MinigamesPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    /**
     * Updates (or creates) the sidebar scoreboard for everyone currently in the queue “key”.
     *
     * @param gameType  e.g. "TNT_RUN"
     * @param mode      e.g. GameMode.CLASSIC
     */
    public void updateQueueScoreboard(String gameType, GameMode mode) {
        String key = gameType.toUpperCase() + "_" + mode.name();
        Queue<QueueEntry> queue = gameManager.getQueue(key);
        if (queue == null || queue.isEmpty()) return;

        // Create a new sidebar objective
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = scoreboard.registerNewObjective("queueInfo", "dummy", ChatColor.GREEN + "Queued: " +
                ChatColor.WHITE + gameType + " [" + mode.name() + "]");

        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int currentSize = queue.size();
        int maxPlayers = gameManager.getMaxPlayers(gameType);

        // Line 1: Number waiting
        Score waitingScore = obj.getScore(ChatColor.YELLOW + "Waiting: " + ChatColor.WHITE + currentSize +
                ChatColor.GRAY + "/" + ChatColor.WHITE + maxPlayers);
        waitingScore.setScore(5);

        // Next lines: list up to 3 entries (by name or party prefix). If the entry is a party, show party color + leader name.
        List<String> lines = new ArrayList<>();
        Iterator<QueueEntry> it = queue.iterator();
        int count = 0;
        while (it.hasNext() && count < 3) {
            QueueEntry entry = it.next();
            List<Player> players = entry.getPlayers();
            if (players.size() == 1) {
                // Single player
                lines.add(ChatColor.GRAY + "- " + ChatColor.WHITE + players.get(0).getName());
            } else {
                // A party (multiple players). Show “<colored>Party of N” or leader name?
                Player leader = players.get(0);
                String line = ChatColor.GRAY + "- " +
                        ChatColor.valueOf(plugin.getPartyManager().getParty(leader.getUniqueId())
                                .getColor().name()) + leader.getName() +
                        ChatColor.WHITE + "'s party (" + players.size() + ")";
                lines.add(line);
            }
            count++;
        }

        int scoreValue = 4;
        for (String l : lines) {
            obj.getScore(l).setScore(scoreValue--);
        }

        // Now send this scoreboard to every player in that queue
        for (QueueEntry e : queue) {
            for (Player p : e.getPlayers()) {
                p.setScoreboard(scoreboard);
            }
        }
    }

    /**
     * Clears the queue scoreboard for everyone in that queue.
     * Call this right before you start the game (so players lose the sidebar).
     *
     * @param gameType the game type
     * @param mode     the mode
     */
    public void clearQueueScoreboard(String gameType, GameMode mode) {
        String key = gameType.toUpperCase() + "_" + mode.name();
        Queue<QueueEntry> queue = gameManager.getQueue(key);
        if (queue == null || queue.isEmpty()) return;

        // Reset scoreboard to player’s default (null or main scoreboard)
        for (QueueEntry e : queue) {
            for (Player p : e.getPlayers()) {
                p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
        }
    }
}