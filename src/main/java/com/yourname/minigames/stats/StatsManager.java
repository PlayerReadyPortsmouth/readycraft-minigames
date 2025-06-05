package com.yourname.minigames.stats;

import com.yourname.minigames.MinigamesPlugin;
import com.yourname.minigames.game.GameInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Manages persistent storage of minigame statistics (wins, losses, plays) per player
 * and integrates basic PlaceholderAPI placeholders.
 */
public class StatsManager /* implements PlaceholderExpansion (if you wish to register placeholders) */ {

    private final MinigamesPlugin plugin;
    private final File statsFile;
    private final FileConfiguration statsConfig;

    /**
     * Constructs a new StatsManager.
     * Loads (or creates) stats.yml in the plugin's data folder.
     *
     * @param plugin The main MinigamesPlugin instance.
     */
    public StatsManager(MinigamesPlugin plugin) {
        this.plugin = plugin;

        // Ensure plugin data folder exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // Initialize stats.yml
        statsFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create stats.yml: " + e.getMessage());
            }
        }

        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
    }

    /**
     * Save the in-memory statsConfig back to stats.yml.
     */
    private void saveStatsFile() {
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save stats.yml: " + e.getMessage());
        }
    }

    /**
     * Records the result of a completed game instance.
     * This default implementation:
     *   • If exactly one participant remains in the instance's participant list, that is the winner.
     *   • All others (either eliminated before or still in the list if size>1) count as losses.
     *
     * If your GameInstance already calls recordWin/recordLoss during the match,
     * you can override or leave this as a no-op.
     *
     * @param instance The {@link GameInstance} that has just finished.
     */
    public void recordGameResult(GameInstance instance) {
        // Attempt to identify winner if exactly one remains
        if (instance.getParticipants().size() == 1) {
            Player winner = instance.getParticipants().get(0);
            recordWin(winner.getUniqueId(), instance.getType());
            plugin.getLogger().info("Recorded WIN for " + winner.getName() + " in " + instance.getType());
        }

        // Every player who participated but is not the sole survivor (or eliminated earlier),
        // count them as a loss. This assumes GameInstance removed eliminated players from participants.
        // If your GameInstance keeps all participants in the list, you may need custom logic.
        for (Player p : instance.getParticipants()) {
            // If more than one left, count them as loss
            if (instance.getParticipants().size() != 1) {
                recordLoss(p.getUniqueId(), instance.getType());
                plugin.getLogger().info("Recorded LOSS for " + p.getName() + " in " + instance.getType());
            }
        }
    }

    /**
     * Increments the win count for a given player and minigame type.
     *
     * Data stored as:
     *   stats:
     *     <uuid>:
     *       <gameType>:
     *         wins: <int>
     *         losses: <int>
     */
    public void recordWin(UUID playerUUID, String gameType) {
        String path = "stats." + playerUUID + "." + gameType + ".wins";
        int current = statsConfig.getInt(path, 0);
        statsConfig.set(path, current + 1);
        saveStatsFile();
    }

    /**
     * Increments the loss count for a given player and minigame type.
     *
     * Data stored as:
     *   stats:
     *     <uuid>:
     *       <gameType>:
     *         wins: <int>
     *         losses: <int>
     */
    public void recordLoss(UUID playerUUID, String gameType) {
        String path = "stats." + playerUUID + "." + gameType + ".losses";
        int current = statsConfig.getInt(path, 0);
        statsConfig.set(path, current + 1);
        saveStatsFile();
    }

    /**
     * Retrieves how many wins a player has for a specific minigame type.
     *
     * @param playerUUID UUID of the player.
     * @param gameType   The minigame type (e.g., "TNT_RUN").
     * @return number of wins (0 if none recorded).
     */
    public int getWins(UUID playerUUID, String gameType) {
        String path = "stats." + playerUUID + "." + gameType + ".wins";
        return statsConfig.getInt(path, 0);
    }

    /**
     * Retrieves how many losses a player has for a specific minigame type.
     *
     * @param playerUUID UUID of the player.
     * @param gameType   The minigame type (e.g., "TNT_RUN").
     * @return number of losses (0 if none recorded).
     */
    public int getLosses(UUID playerUUID, String gameType) {
        String path = "stats." + playerUUID + "." + gameType + ".losses";
        return statsConfig.getInt(path, 0);
    }

    /**
     * Retrieves total plays (wins + losses) for a player in a given minigame type.
     *
     * @param playerUUID UUID of the player.
     * @param gameType   The minigame type.
     * @return total plays (0 if none).
     */
    public int getTotalPlays(UUID playerUUID, String gameType) {
        return getWins(playerUUID, gameType) + getLosses(playerUUID, gameType);
    }
}
