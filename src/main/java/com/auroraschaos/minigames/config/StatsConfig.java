package com.auroraschaos.minigames.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Parses and holds settings under the "stats" section of config.yml, for example:
 *
 * stats:
 *   storage: "flatfile"                  # or "mysql"
 *   flatfileFolder: "player-stats"       # only used for flatfile
 *   mysql:
 *     host: "localhost"
 *     port: 3306
 *     database: "minigames"
 *     user: "root"
 *     password: ""
 *   leaderboard:
 *     enabled: true
 *     size: 10
 *   autoSaveIntervalSeconds: 60
 */
public class StatsConfig {
    public enum StorageType { FLATFILE, MYSQL }

    private final StorageType storageType;
    private final String flatfileFolder;

    private final MySQLConfig mysqlConfig;

    private final boolean leaderboardEnabled;
    private final int leaderboardSize;

    private final long autoSaveIntervalSeconds;

    private StatsConfig(
            StorageType storageType,
            String flatfileFolder,
            MySQLConfig mysqlConfig,
            boolean leaderboardEnabled,
            int leaderboardSize,
            long autoSaveIntervalSeconds
    ) {
        this.storageType = storageType;
        this.flatfileFolder = flatfileFolder;
        this.mysqlConfig = mysqlConfig;
        this.leaderboardEnabled = leaderboardEnabled;
        this.leaderboardSize = leaderboardSize;
        this.autoSaveIntervalSeconds = autoSaveIntervalSeconds;
    }

    public static StatsConfig from(ConfigurationSection section) throws ConfigurationException {
        if (section == null) {
            throw new ConfigurationException("'stats' section is missing");
        }

        String rawStorage = section.getString("storage", "flatfile").toUpperCase();
        StorageType storageType;
        try {
            storageType = StorageType.valueOf(rawStorage);
        } catch (IllegalArgumentException ex) {
            throw new ConfigurationException(
                "'stats.storage' must be one of [flatfile, mysql] (found '" + rawStorage + "')"
            );
        }

        // Flatfile-specific setting
        String flatfileFolder = section.getString("flatfileFolder", "player-stats");
        if (storageType == StorageType.FLATFILE && (flatfileFolder.isBlank())) {
            throw new ConfigurationException("'stats.flatfileFolder' must not be empty when using flatfile storage");
        }

        // MySQL-specific settings
        MySQLConfig mysqlConfig = null;
        if (storageType == StorageType.MYSQL) {
            ConfigurationSection mysqlSec = section.getConfigurationSection("mysql");
            if (mysqlSec == null) {
                throw new ConfigurationException("'stats.mysql' section is required when storage=mysql");
            }
            String host = mysqlSec.getString("host", "").trim();
            int port = mysqlSec.getInt("port", 3306);
            String database = mysqlSec.getString("database", "").trim();
            String user = mysqlSec.getString("user", "").trim();
            String password = mysqlSec.getString("password", "");

            if (host.isEmpty()) {
                throw new ConfigurationException("'stats.mysql.host' must be provided");
            }
            if (database.isEmpty()) {
                throw new ConfigurationException("'stats.mysql.database' must be provided");
            }
            // user/password may be empty for local setups, so we allow those

            mysqlConfig = new MySQLConfig(host, port, database, user, password);
        }

        // Leaderboard settings
        ConfigurationSection lb = section.getConfigurationSection("leaderboard");
        boolean lbEnabled = false;
        int lbSize = 10;
        if (lb != null) {
            lbEnabled = lb.getBoolean("enabled", false);
            lbSize = lb.getInt("size", 10);
            if (lbSize < 1) {
                throw new ConfigurationException("'stats.leaderboard.size' must be ≥ 1 (found " + lbSize + ")");
            }
        }

        // Auto-save interval
        long interval = section.getLong("autoSaveIntervalSeconds", 60L);
        if (interval < 1) {
            throw new ConfigurationException(
                "'stats.autoSaveIntervalSeconds' must be ≥ 1 (found " + interval + ")"
            );
        }

        return new StatsConfig(
            storageType,
            flatfileFolder,
            mysqlConfig,
            lbEnabled,
            lbSize,
            interval
        );
    }

    /** Which storage backend to use. */
    public StorageType getStorageType() {
        return storageType;
    }

    /** Folder under plugin data for flatfile stats. */
    public String getFlatfileFolder() {
        return flatfileFolder;
    }

    /** MySQL connection settings (only non-null if storageType=MYSQL). */
    public MySQLConfig getMysqlConfig() {
        return mysqlConfig;
    }

    /** Whether to enable a top‐N leaderboard. */
    public boolean isLeaderboardEnabled() {
        return leaderboardEnabled;
    }

    /** How many entries to show on the leaderboard. */
    public int getLeaderboardSize() {
        return leaderboardSize;
    }

    /** How often to auto‐save stats (in seconds). */
    public long getAutoSaveIntervalSeconds() {
        return autoSaveIntervalSeconds;
    }

    /** Nested class for MySQL connection info. */
    public static class MySQLConfig {
        private final String host;
        private final int port;
        private final String database;
        private final String user;
        private final String password;

        public MySQLConfig(String host, int port, String database, String user, String password) {
            this.host = host;
            this.port = port;
            this.database = database;
            this.user = user;
            this.password = password;
        }

        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getDatabase() { return database; }
        public String getUser() { return user; }
        public String getPassword() { return password; }
    }
}