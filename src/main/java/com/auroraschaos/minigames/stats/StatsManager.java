package com.auroraschaos.minigames.stats;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.auroraschaos.minigames.config.StatsConfig;
import com.auroraschaos.minigames.game.GameInstance;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages persistent storage of minigame statistics (wins, losses, plays) per player.
 * Supports both flatfile (YAML) and MySQL backends as configured via StatsConfig.
 */
public class StatsManager {

    private final MinigamesPlugin plugin;
    private final StatsConfig statsConfig;

    // Flatfile storage
    private File statsFile;
    private FileConfiguration statsStorage;

    // MySQL storage (stubbed—replace with your preferred DataSource)
    // private DataSource mysqlDataSource;

    public StatsManager(MinigamesPlugin plugin, StatsConfig statsConfig) {
        this.plugin = plugin;
        this.statsConfig = statsConfig;

        // Ensure plugin data folder exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        switch (statsConfig.getStorageType()) {
            case FLATFILE:
                initFlatfileStorage();
                scheduleAutoSave();
                break;
            case MYSQL:
                initMySqlStorage();
                break;
        }
    }

    // -------------------
    // Initialization
    // -------------------

    private void initFlatfileStorage() {
        // Use the configured subfolder for stats
        File dir = new File(plugin.getDataFolder(), statsConfig.getFlatfileFolder());
        if (!dir.exists()) dir.mkdirs();

        statsFile = new File(dir, "stats.yml");
        try {
            if (!statsFile.exists()) statsFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE,
                "Could not create stats file: " + statsFile.getAbsolutePath(), e);
        }

        statsStorage = YamlConfiguration.loadConfiguration(statsFile);
        plugin.getLogger().info("[StatsManager] Flatfile storage initialized at " + statsFile);
    }

    private void initMySqlStorage() {
        // TODO: initialize your JDBC DataSource here, e.g. HikariCP with statsConfig.getMysqlConfig()
        // StatsConfig.MySQLConfig cfg = statsConfig.getMysqlConfig();
        // String url = "jdbc:mysql://" + cfg.getHost() + ":" + cfg.getPort() + "/" + cfg.getDatabase();
        // ... set up DataSource ...
        plugin.getLogger().info("[StatsManager] MySQL storage selected, initialization pending.");
    }

    private void scheduleAutoSave() {
        long intervalTicks = statsConfig.getAutoSaveIntervalSeconds() * 20L;
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(
            plugin,
            this::saveFlatfile,
            intervalTicks,
            intervalTicks
        );
        plugin.getLogger().info("[StatsManager] Scheduled auto-save every "
            + statsConfig.getAutoSaveIntervalSeconds() + " seconds.");
    }

    // -------------------
    // Public API
    // -------------------

    public void recordGameResult(GameInstance instance) {
        if (instance.getParticipants().isEmpty()) {
            return;
        }

        Player winner = null;
        // Default logic: last survivor is winner
        if (instance.getParticipants().size() == 1) {
            winner = instance.getParticipants().get(0);
            recordWin(winner.getUniqueId(), instance.getType());
        }

        // Everyone except the winner is a loser
        for (Player p : instance.getParticipants()) {
            if (winner == null || !p.equals(winner)) {
                recordLoss(p.getUniqueId(), instance.getType());
            }
        }
    }

    public void recordWin(UUID playerUUID, String gameType) {
        if (statsConfig.getStorageType() == StatsConfig.StorageType.FLATFILE) {
            String path = "stats." + playerUUID + "." + gameType + ".wins";
            int current = statsStorage.getInt(path, 0);
            statsStorage.set(path, current + 1);
            saveFlatfile();
        } else {
            // TODO: implement SQL increment for wins
            plugin.getLogger().info("[StatsManager] (MySQL) recordWin for " + playerUUID + " in " + gameType);
        }
    }

    public void recordLoss(UUID playerUUID, String gameType) {
        if (statsConfig.getStorageType() == StatsConfig.StorageType.FLATFILE) {
            String path = "stats." + playerUUID + "." + gameType + ".losses";
            int current = statsStorage.getInt(path, 0);
            statsStorage.set(path, current + 1);
            saveFlatfile();
        } else {
            // TODO: implement SQL increment for losses
            plugin.getLogger().info("[StatsManager] (MySQL) recordLoss for " + playerUUID + " in " + gameType);
        }
    }

    public int getWins(UUID playerUUID, String gameType) {
        if (statsConfig.getStorageType() == StatsConfig.StorageType.FLATFILE) {
            String path = "stats." + playerUUID + "." + gameType + ".wins";
            return statsStorage.getInt(path, 0);
        } else {
            // TODO: query SQL
            return 0;
        }
    }

    public int getLosses(UUID playerUUID, String gameType) {
        if (statsConfig.getStorageType() == StatsConfig.StorageType.FLATFILE) {
            String path = "stats." + playerUUID + "." + gameType + ".losses";
            return statsStorage.getInt(path, 0);
        } else {
            // TODO: query SQL
            return 0;
        }
    }

    public int getTotalPlays(UUID playerUUID, String gameType) {
        return getWins(playerUUID, gameType) + getLosses(playerUUID, gameType);
    }

    /**
     * Persist any pending statistic changes to disk or database.
     * This should be invoked when the plugin shuts down to avoid
     * losing data.
     */
    public void save() {
        if (statsConfig.getStorageType() == StatsConfig.StorageType.FLATFILE) {
            saveFlatfile();
        } else {
            // TODO: flush to SQL backend when implemented
        }
    }

    // -------------------
    // Helpers
    // -------------------

    private void saveFlatfile() {
        try {
            statsStorage.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE,
                "[StatsManager] Error saving stats file: " + statsFile.getAbsolutePath(), e);
        }
    }
}
