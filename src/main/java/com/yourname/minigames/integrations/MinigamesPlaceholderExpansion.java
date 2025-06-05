package com.yourname.minigames.integrations;

import com.yourname.minigames.MinigamesPlugin;
import com.yourname.minigames.stats.StatsManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

/**
 * Registers PlaceholderAPI placeholders for minigame stats.
 *
 * Available placeholders (example for TNT_RUN):
 *   %minigames_wins_tnt_run%    → number of wins in TNT_RUN
 *   %minigames_losses_tnt_run%  → number of losses in TNT_RUN
 *   %minigames_plays_tnt_run%   → total plays (wins + losses) in TNT_RUN
 *
 * The <gameType> part must match exactly the uppercase key used in config.yml (e.g. TNT_RUN).
 */
public class MinigamesPlaceholderExpansion extends PlaceholderExpansion {

    private final MinigamesPlugin plugin;
    private final StatsManager statsManager;

    public MinigamesPlaceholderExpansion() {
        this.plugin = MinigamesPlugin.getInstance();
        this.statsManager = plugin.getStatsManager();
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
     * Called when a placeholder with identifier "minigames_<stat>_<gameType>" is used.
     *
     * @param offlinePlayer The offline (or online) player for whom the placeholder is requested.
     * @param identifier    The part after "minigames_"
     *                      Expected patterns:
     *                        wins_<gameType>
     *                        losses_<gameType>
     *                        plays_<gameType>
     * @return A String to replace the placeholder, or an empty string if invalid.
     */
    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String identifier) {
        if (offlinePlayer == null) {
            return "";
        }

        String[] parts = identifier.split("_", 2);
        if (parts.length != 2) {
            return "";
        }

        String statType = parts[0].toLowerCase();    // "wins", "losses", or "plays"
        String gameType = parts[1].toUpperCase();    // e.g. "TNT_RUN"

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

    @Override
    public boolean persist() {
        // Ensure this expansion stays registered across reloads
        return true;
    }

    @Override
    public boolean canRegister() {
        // Only register if PlaceholderAPI is present
        return plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    }
}
