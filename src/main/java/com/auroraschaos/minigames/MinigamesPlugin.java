package com.auroraschaos.minigames;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.auroraschaos.minigames.arena.ArenaRegistry;
import com.auroraschaos.minigames.arena.ArenaResetService;
import com.auroraschaos.minigames.arena.ArenaService;
import com.auroraschaos.minigames.arena.GridSlotAllocator;
import com.auroraschaos.minigames.arena.SchematicLoader;
import com.auroraschaos.minigames.commands.CommandManager;
import com.auroraschaos.minigames.config.ConfigManager;
import com.auroraschaos.minigames.config.ConfigurationException;
import com.auroraschaos.minigames.game.GameManager;
import com.auroraschaos.minigames.game.GameMode;
import com.auroraschaos.minigames.gui.GUIManager;
import com.auroraschaos.minigames.integrations.MinigamesPlaceholderExpansion;
import com.auroraschaos.minigames.party.PartyManager;
import com.auroraschaos.minigames.scoreboard.QueueScoreboardManager;
import com.auroraschaos.minigames.scoreboard.ScoreboardManager;
import com.auroraschaos.minigames.stats.StatsManager;
import com.auroraschaos.minigames.util.CountdownTimer;
import com.auroraschaos.minigames.arena.SlotAllocator;
import com.auroraschaos.minigames.arena.WorldEditSchematicLoader;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * The main class for the MinigamesPlugin. This plugin provides a framework
 * for creating and managing various minigames on a Spigot server. It handles
 * game lifecycle, arena management, queuing, stats, GUIs, and commands.
 */

public class MinigamesPlugin extends JavaPlugin {

    private static MinigamesPlugin instance;

    private GameManager gameManager;
    private ArenaService arenaService;
    private PartyManager partyManager;
    private StatsManager statsManager;
    private GUIManager guiManager;
    private CommandManager cmdManager;
    private ScoreboardManager scoreboardManager;
    private CountdownTimer countdownTimer;
    private QueueScoreboardManager queueSB;

    private ConfigManager configManager;

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
        getLogger().info("Commands Registered.");

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
        if (arenaService != null) {
            arenaService.shutdownAll();
        }
        if (statsManager != null) {
            statsManager.save();
        }
        getLogger().info("MinigamesPlugin has been disabled!");
    }

    private void initializeManagers() {
        // 1) Load & validate all configs
        configManager = new ConfigManager(this);
        try {
            configManager.loadAll();
            getLogger().info("[Config] All configurations loaded.");
        } catch (ConfigurationException ex) {
            getLogger().severe("[Config] Failed to load config: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
    
        // 2) Prepare the arena dependencies
        ArenaRegistry arenaRegistry = new ArenaRegistry();
        ArenaResetService arenaResetService = new ArenaResetService(this, arenaRegistry);
        SlotAllocator slotAllocator = new GridSlotAllocator(new Vector(0, 64, 0), /*columns*/5, /*spacingX*/300, /*spacingZ*/300);
        SchematicLoader schematicLoader = new WorldEditSchematicLoader(this);
    
        // 3) Construct the ArenaService with everything it needs
        arenaService = new ArenaService(
            this,
            configManager.getArenaConfig(),
            slotAllocator,
            schematicLoader,
            arenaResetService
        );
        getLogger().info("[Init] ArenaService constructed.");
    
        // 4) Other managers
        partyManager      = new PartyManager(this, configManager.getPartyConfig());
        statsManager      = new StatsManager(this, configManager.getStatsConfig());
        guiManager        = new GUIManager(this);
        cmdManager        = new CommandManager(this);
        scoreboardManager = new ScoreboardManager();
        countdownTimer    = new CountdownTimer(this, scoreboardManager);
    
        // 5) Game & queue
        gameManager = new GameManager(
            this,
            arenaService,
            partyManager,
            statsManager,
            guiManager
        );
        queueSB = new QueueScoreboardManager(this, gameManager);

        // Load and register arenas from configuration
        arenaService.initializeAll();
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
     * Gets the {@link ArenaService} responsible for managing arena schematics and instances.
     *
     * @return The arena service instance.
     */
    public ArenaService getArenaService() {
        return arenaService;
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

    public QueueScoreboardManager getQueueScoreboardManager(){
        return queueSB;
    }
}
