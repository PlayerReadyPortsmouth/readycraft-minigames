package com.auroraschaos.minigames.integrations;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.auroraschaos.minigames.game.GameManager;
import com.auroraschaos.minigames.game.GameMode;
import com.auroraschaos.minigames.stats.StatsManager;

import java.util.UUID;

/**
 * PlaceholderAPI expansion for:
 *   • minigame stats (wins/losses/plays) [already implemented]
 *   • number of active games
 *   • number of queued players per gameType (and per gameType+mode)
 *
 * Usage examples:
 *   %minigames_active_games%                      → total running games
 *   %minigames_queue_tnt_run%                     → total queued in any TNT_RUN mode
 *   %minigames_queue_tnt_run_classic%             → queued in TNT_RUN CLASSIC mode
 *   %minigames_wins_tnt_run%                      → wins in TNT_RUN (existing)
 *   %minigames_losses_tnt_run%                    → losses in TNT_RUN (existing)
 *   %minigames_plays_tnt_run%                     → total plays in TNT_RUN (existing)
 */
public class MinigamesPlaceholderExpansion extends PlaceholderExpansion {

    private final MinigamesPlugin plugin;
    private final StatsManager statsManager;
    private final GameManager gameManager;

    public MinigamesPlaceholderExpansion() {
        this.plugin = MinigamesPlugin.getInstance();
        this.statsManager = plugin.getStatsManager();
        this.gameManager = plugin.getGameManager();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "minigames";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().isEmpty()
                ? "Unknown" : plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * This method is called when a placeholder with identifier "minigames_<identifier>" is used.
     *
     * @param offlinePlayer The offline or online player for whom the placeholder is requested.
     * @param identifier    The part after "minigames_"
     *                      Supported patterns:
     *                        - "active_games"
     *                        - "queue_<gameType>"
     *                        - "queue_<gameType>_<mode>"
     *                        - "wins_<gameType>"
     *                        - "losses_<gameType>"
     *                        - "plays_<gameType>"
     *
     * @return A String to replace the placeholder (never null; empty if invalid).
     */
    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return "";
        }

        // 1) Check for "active_games"
        if (identifier.equalsIgnoreCase("active_games")) {
            int count = gameManager.getActiveGameCount();
            return String.valueOf(count);
        }

        // 2) Check for "queue_" patterns
        if (identifier.startsWith("queue_")) {
            // Remove "queue_" prefix, split remainder by "_"
            String remainder = identifier.substring("queue_".length());
            String[] parts = remainder.split("_");

            if (parts.length == 1) {
                // Pattern: queue_<gameType>
                String gameType = parts[0].toUpperCase();
                int totalQueued = gameManager.getTotalQueuedForType(gameType);
                return String.valueOf(totalQueued);

            } else if (parts.length == 2) {
                // Pattern: queue_<gameType>_<mode>
                String gameType = parts[0].toUpperCase();
                String modeStr  = parts[1].toUpperCase();
                try {
                    GameMode mode = GameMode.valueOf(modeStr);
                    int queued = gameManager.getQueuedCount(gameType, mode);
                    return String.valueOf(queued);
                } catch (IllegalArgumentException e) {
                    // invalid GameMode
                    return "";
                }
            } else {
                // Too many underscores (invalid)
                return "";
            }
        }

        // 3) Check for stats-related placeholders: wins_, losses_, plays_
        if (identifier.startsWith("wins_") || identifier.startsWith("losses_") || identifier.startsWith("plays_")) {
            String[] parts = identifier.split("_", 2);
            if (parts.length != 2) {
                return "";
            }
            String statType = parts[0].toLowerCase();   // "wins", "losses", or "plays"
            String gameType = parts[1].toUpperCase();   // e.g. "TNT_RUN"
            UUID uuid = offlinePlayer.getUniqueId();

            switch (statType) {
                case "wins":
                    return String.valueOf(statsManager.getWins(uuid, gameType));
                case "losses":
                    return String.valueOf(statsManager.getLosses(uuid, gameType));
                case "plays":
                    return String.valueOf(statsManager.getTotalPlays(uuid, gameType));
                default:
                    return "";
            }
        }

        // 4) No matching pattern
        return "";
    }

    @Override
    public boolean persist() {
        // Keep this expansion registered across reloads
        return true;
    }

    @Override
    public boolean canRegister() {
        // Only register if PlaceholderAPI is present
        return plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    }
}