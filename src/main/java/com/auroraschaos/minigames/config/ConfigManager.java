package com.auroraschaos.minigames.config;

import org.bukkit.configuration.file.FileConfiguration;
import com.auroraschaos.minigames.MinigamesPlugin;
import java.util.Map;

import com.auroraschaos.minigames.config.TTTConfig;

/**
 * Central manager for loading and validating all plugin configuration files.
 */
public class ConfigManager {
    private final MinigamesPlugin plugin;
    private final FileConfiguration config;

    private ArenaConfig arenaConfig;
    private GameModeConfig gameModeConfig;
    private PartyConfig partyConfig;
    private StatsConfig statsConfig;
    private SpleefConfig spleefConfig;
    private TTTConfig tttConfig;
    //private GuiConfig guiConfig;
    //private ScoreboardConfig scoreboardConfig;
    //private CountdownConfig countdownConfig;
    //private QueueConfig queueConfig;
    // ... other sub-configs as needed

    /** Whether verbose logging is enabled via config.yml. */
    private boolean verboseLogging;

    public ConfigManager(MinigamesPlugin plugin) {
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
        plugin.logVerbose("[ConfigManager] Arena config loaded with "
                + arenaConfig.getArenas().size() + " arenas");

        gameModeConfig    = parseGameModeConfig();
        plugin.logVerbose("[ConfigManager] Game mode config loaded");

        partyConfig       = parsePartyConfig();
        plugin.logVerbose("[ConfigManager] Party config loaded");

        statsConfig       = parseStatsConfig();
        plugin.logVerbose("[ConfigManager] Stats config loaded");

        spleefConfig      = parseSpleefConfig();
        plugin.logVerbose("[ConfigManager] Spleef config loaded");

        tttConfig         = parseTTTConfig();
        plugin.logVerbose("[ConfigManager] TTT config loaded");
        //guiConfig         = parseGuiConfig();
        //scoreboardConfig  = parseScoreboardConfig();
        //countdownConfig   = parseCountdownConfig();
        //queueConfig       = parseQueueConfig();
        // Add more as plugin evolves

        // Root-level options
        verboseLogging = config.getBoolean("verboseLogging", false);
        plugin.logVerbose("[ConfigManager] verboseLogging=" + verboseLogging);
    }

    private ArenaConfig parseArenaConfig() throws ConfigurationException {
        final String path = "arenas";
        if (!config.isConfigurationSection(path)) {
            throw new ConfigurationException("Missing '" + path + "' section in config.yml");
        }
        return ArenaConfig.from(config.getConfigurationSection(path));
    }
    
    private GameModeConfig parseGameModeConfig() throws ConfigurationException {
        GameModeConfig gameModeConfig = GameModeConfig.from(config.getConfigurationSection("minigames"));
        return gameModeConfig;
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

    private SpleefConfig parseSpleefConfig() throws ConfigurationException {
        java.io.File file = new java.io.File(plugin.getDataFolder(), "Spleef.yml");
        if (!file.exists()) {
            plugin.saveResource("Spleef.yml", false);
        }
        org.bukkit.configuration.file.YamlConfiguration cfg =
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        return SpleefConfig.from(cfg);
    }

    private TTTConfig parseTTTConfig() throws ConfigurationException {
        java.io.File file = new java.io.File(plugin.getDataFolder(), "TTT.yml");
        if (!file.exists()) {
            plugin.saveResource("TTT.yml", false);
        }
        org.bukkit.configuration.file.YamlConfiguration cfg =
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        return TTTConfig.from(cfg);
    }
/**
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
    public GameModeConfig getGameModeConfig() { return gameModeConfig; }
    public PartyConfig getPartyConfig() { return partyConfig; }
    public StatsConfig getStatsConfig() { return statsConfig; }
    public SpleefConfig getSpleefConfig() { return spleefConfig; }
    public TTTConfig getTTTConfig() { return tttConfig; }
    /** @return true if verbose logging is enabled. */
    public boolean isVerboseLogging() { return verboseLogging; }
    //public GuiConfig getGuiConfig() { return guiConfig; }
    //public ScoreboardConfig getScoreboardConfig() { return scoreboardConfig; }
    //public CountdownConfig getCountdownConfig() { return countdownConfig; }
    //public QueueConfig getQueueConfig() { return queueConfig; }
}
