package com.auroraschaos.minigames.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Map;

/**
 * Central manager for loading and validating all plugin configuration files.
 */
public class ConfigManager {
    private final JavaPlugin plugin;
    private final FileConfiguration config;

    private ArenaConfig arenaConfig;
    //private GameModeConfig gameModeConfig;
    //private PartyConfig partyConfig;
    //private StatsConfig statsConfig;
    //private GuiConfig guiConfig;
    //private ScoreboardConfig scoreboardConfig;
    //private CountdownConfig countdownConfig;
    //private QueueConfig queueConfig;
    // ... other sub-configs as needed

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    /**
     * Load and validate all sub-configurations. Throws on any error.
     */
    public void loadAll() throws ConfigurationException {
        // Validate root config presence
        if (config == null) {
            throw new ConfigurationException("Missing root configuration file (config.yml)");
        }
        // Parse each section
        arenaConfig       = parseArenaConfig();
        //gameModeConfig    = parseGameModeConfig();
        //partyConfig       = parsePartyConfig();
        //statsConfig       = parseStatsConfig();
        //guiConfig         = parseGuiConfig();
        //scoreboardConfig  = parseScoreboardConfig();
        //countdownConfig   = parseCountdownConfig();
        //queueConfig       = parseQueueConfig();
        // Add more as plugin evolves
    }

    private ArenaConfig parseArenaConfig() throws ConfigurationException {
        final String path = "arenas";
        if (!config.isConfigurationSection(path)) {
            throw new ConfigurationException("Missing '" + path + "' section in config.yml");
        }
        return ArenaConfig.from(config.getConfigurationSection(path));
    }
    /** 
    private GameModeConfig parseGameModeConfig() throws ConfigurationException {
        final String path = "gamemodes";
        if (!config.isConfigurationSection(path)) {
            throw new ConfigurationException("Missing '" + path + "' section in config.yml");
        }
        return GameModeConfig.from(config.getConfigurationSection(path));
    }

    private PartyConfig parsePartyConfig() throws ConfigurationException {
        final String path = "party";
        if (!config.isConfigurationSection(path)) {
            throw new ConfigurationException("Missing '" + path + "' section in config.yml");
        }
        return PartyConfig.from(config.getConfigurationSection(path));
    }

    private StatsConfig parseStatsConfig() throws ConfigurationException {
        final String path = "stats";
        if (!config.isConfigurationSection(path)) {
            throw new ConfigurationException("Missing '" + path + "' section in config.yml");
        }
        return StatsConfig.from(config.getConfigurationSection(path));
    }

    private GuiConfig parseGuiConfig() throws ConfigurationException {
        final String path = "gui";
        if (!config.isConfigurationSection(path)) {
            throw new ConfigurationException("Missing '" + path + "' section in config.yml");
        }
        return GuiConfig.from(config.getConfigurationSection(path));
    }

    private ScoreboardConfig parseScoreboardConfig() throws ConfigurationException {
        final String path = "scoreboard";
        if (!config.isConfigurationSection(path)) {
            throw new ConfigurationException("Missing '" + path + "' section in config.yml");
        }
        return ScoreboardConfig.from(config.getConfigurationSection(path));
    }

    private CountdownConfig parseCountdownConfig() throws ConfigurationException {
        final String path = "countdown";
        if (!config.isConfigurationSection(path)) {
            throw new ConfigurationException("Missing '" + path + "' section in config.yml");
        }
        return CountdownConfig.from(config.getConfigurationSection(path));
    }

    private QueueConfig parseQueueConfig() throws ConfigurationException {
        final String path = "queue";
        if (!config.isConfigurationSection(path)) {
            throw new ConfigurationException("Missing '" + path + "' section in config.yml");
        }
        return QueueConfig.from(config.getConfigurationSection(path));
    }
    */

    // Getters for sub-configs
    public ArenaConfig getArenaConfig() { return arenaConfig; }
    //public GameModeConfig getGameModeConfig() { return gameModeConfig; }
    //public PartyConfig getPartyConfig() { return partyConfig; }
    //public StatsConfig getStatsConfig() { return statsConfig; }
    //public GuiConfig getGuiConfig() { return guiConfig; }
    //public ScoreboardConfig getScoreboardConfig() { return scoreboardConfig; }
    //public CountdownConfig getCountdownConfig() { return countdownConfig; }
    //public QueueConfig getQueueConfig() { return queueConfig; }
}
