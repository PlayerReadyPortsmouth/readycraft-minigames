package com.yourname.minigames;

import com.yourname.minigames.game.GameManager;
import com.yourname.minigames.arena.ArenaManager;
import com.yourname.minigames.party.PartyManager;
import com.yourname.minigames.stats.StatsManager;
import com.yourname.minigames.gui.GUIManager;
import com.yourname.minigames.commands.CommandManager;

import org.bukkit.plugin.java.JavaPlugin;

public class MinigamesPlugin extends JavaPlugin {

    private static MinigamesPlugin instance;

    private GameManager gameManager;
    private ArenaManager arenaManager;
    private PartyManager partyManager;
    private StatsManager statsManager;
    private GUIManager guiManager;
    private cmdManager CommandManager;
    private scoreboardManager ScoreboardManager;
    private CountdownTimer countdownTimer;

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

    @Override
    public void onDisable() {
        // TODO: Save data, clean up tasks
        getLogger().info("MinigamesPlugin has been disabled!");
    }

    private void initializeManagers(){
        arenaManager = new ArenaManager(this);
        gameManager = new GameManager(this, arenaManager);
        partyManager = new PartyManager(this);
        statsManager = new StatsManager(this);
        guiManager = new GUIManager(this);
        cmdManager = new CommandManager(this);
        scoreboardManager = new ScoreboardManager();
        countdownTimer = new CountdownTimer(this, scoreboardManager);
    }

    private Map<String, List<GameMode>> modesPerGame = new HashMap<>();

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

    private void registerEvents() {
        // TODO: Register event listeners
    }

    private void hookIntoAPIs(){
        // Hook into PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("Hooked into PlaceholderAPI.");
            // TODO: Register placeholders
        }
        
        // Hook into Citizens
        if (getServer().getPluginManager().getPlugin("Citizens") != null) {
            getLogger().info("Hooked into Citizens.");
            // TODO: Register NPCs
        }
    }

    public static MinigamesPlugin getInstance() {
        return instance;
    }

    // Getters for managers
    public GameManager getGameManager() {
        return gameManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public CountdownTimer getCountdownTimer() {
        return countdownTimer;
    }
}
