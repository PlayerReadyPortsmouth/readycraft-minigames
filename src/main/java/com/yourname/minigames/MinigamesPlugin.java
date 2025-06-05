package com.yourname.minigames;

import com.yourname.minigames.game.GameManager;
import com.yourname.minigames.arena.ArenaManager;
import com.yourname.minigames.party.PartyManager;
import com.yourname.minigames.stats.StatsManager;
import com.yourname.minigames.gui.GUIManager;
import com.yourname.minigames.integrations.MinigamesPlaceholderExpansion;
import com.yourname.minigames.commands.CommandManager;
import com.yourname.minigames.game.GameMode;
import com.yourname.minigames.scoreboard.ScoreboardManager;
import com.yourname.minigames.util.CountdownTimer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * The main class for the MinigamesPlugin. This plugin provides a framework
 * for creating and managing various minigames on a Spigot server. It handles
 * game lifecycle, arena management, queuing, stats, GUIs, and commands.
 */

public class MinigamesPlugin extends JavaPlugin {

    private static MinigamesPlugin instance;

    private GameManager gameManager;
    private ArenaManager arenaManager;
    private PartyManager partyManager;
    private StatsManager statsManager;
    private GUIManager guiManager;
    private CommandManager cmdManager;
    private ScoreboardManager scoreboardManager;
    private CountdownTimer countdownTimer;

    /**
     * Called when the plugin is enabled.
     * Initializes managers, registers commands and events, and hooks into external APIs.
     */
    @Override
    public void onEnable() {
        instance = this;

        // Save default configs
        saveDefaultConfig();
        loadModesConfig();
        getLogger().info("Config loaded.");

        // Initialize managers
        initializeManagers();

        getLogger().info("Managers initialized.");

        // Register commands
        cmdManager.registerAll();

        // Register events
        registerEvents();

        //Hook into APIs
        hookIntoAPIs();

        getLogger().info("MinigamesPlugin has been enabled!");
    }

    /**
     * Called when the plugin is disabled.
     * Handles cleanup tasks such as saving data and cancelling running tasks.
     */
    @Override
    public void onDisable() {
        // TODO: Save data, clean up tasks
        getLogger().info("MinigamesPlugin has been disabled!");
    }

    private void initializeManagers(){
        /**
         * Initializes all the core managers used by the plugin.
         */
        arenaManager = new ArenaManager(this);
        partyManager = new PartyManager(this);
        statsManager = new StatsManager(this);
        guiManager = new GUIManager(this);
        cmdManager = new CommandManager(this);
        scoreboardManager = new ScoreboardManager();
        countdownTimer = new CountdownTimer(this, scoreboardManager);
        gameManager = new GameManager(
            this,
            arenaManager,
            partyManager,
            statsManager,
            guiManager
        );
    }

    private Map<String, List<GameMode>> modesPerGame = new HashMap<>();

    /**
     * Loads the configured game modes for each minigame from the plugin's config.yml.
     * Populates the {@link #modesPerGame} map.
     */
    private void loadModesConfig() {
        FileConfiguration cfg = getConfig();
        if (!cfg.isConfigurationSection("minigames")) return;

        for (String key : cfg.getConfigurationSection("minigames").getKeys(false)) {
            List<String> modeStrings = cfg.getStringList("minigames." + key + ".enabled_modes");
            List<GameMode> modes = new ArrayList<>();
            for (String m : modeStrings) {
                try {
                    modes.add(GameMode.valueOf(m.toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    getLogger().warning("Unknown GameMode '" + m + "' for " + key);
                }
            }
            modesPerGame.put(key.toUpperCase(), modes);
        }
    }

    /**
     * Registers all necessary event listeners for the plugin.
     */
    private void registerEvents() {
        // TODO: Register event listeners
    }

    /**
     * Hooks into external Bukkit APIs like PlaceholderAPI and Citizens if they are present.
     */
    private void hookIntoAPIs(){
    // Register PlaceholderAPI expansion if available
    if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
        new MinigamesPlaceholderExpansion().register();
        getLogger().info("Registered Minigames PlaceholderAPI expansion.");
    } else {
        getLogger().warning("PlaceholderAPI not found: placeholders will not be available.");
    }
        
        // Hook into Citizens
        if (getServer().getPluginManager().getPlugin("Citizens") != null) {
            getLogger().info("Hooked into Citizens.");
            // TODO: Register NPCs
        }
    }

    /**
     * Gets the singleton instance of the MinigamesPlugin.
     *
     * @return The MinigamesPlugin instance.
     */
    public static MinigamesPlugin getInstance() {
        return instance;
    }

    // Getters for managers
    /**
     * Gets the GameManager responsible for game lifecycle and queuing.
     * @return The GameManager instance.
     */
    public GameManager getGameManager() {
        return gameManager;
    }

    /**
     * Gets the ArenaManager responsible for managing arena schematics and instances.
     * @return The ArenaManager instance.
     */
    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    /**
     * Gets the PartyManager responsible for handling player parties.
     * @return The PartyManager instance.
     */
    public PartyManager getPartyManager() {
        return partyManager;
    }

    /**
     * Gets the StatsManager responsible for tracking and storing player statistics.
     * @return The StatsManager instance.
     */
    public StatsManager getStatsManager() {
        return statsManager;
    }

    /**
     * Gets the GUIManager responsible for handling plugin GUIs.
     * @return The GUIManager instance.
     */
    public GUIManager getGUIManager() {
        return guiManager;
    }

    /**
     * Gets the ScoreboardManager responsible for managing per-arena scoreboards.
     * @return The ScoreboardManager instance.
     */
    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    /**
     * Gets the CountdownTimer utility for managing game countdowns.
     * @return The CountdownTimer instance.
     */
    public CountdownTimer getCountdownTimer() {
        return countdownTimer;
    }
}
