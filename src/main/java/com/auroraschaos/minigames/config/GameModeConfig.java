package com.auroraschaos.minigames.config;

import com.auroraschaos.minigames.game.GameMode;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Parses and holds configuration for each minigame type:
 *   minigames:
 *     RACE:
 *       minPlayers: 2
 *       maxPlayers: 8
 *       enabled_modes: [CLASSIC, TIME_TRIAL]
 */
public class GameModeConfig {
    private final Map<String, GameTypeConfig> games;

    private GameModeConfig(Map<String, GameTypeConfig> games) {
        this.games = Collections.unmodifiableMap(games);
    }

    /**
     * Load and validate the "minigames" section.
     * @param section the ConfigSection at getConfig().getConfigurationSection("minigames")
     */
    public static GameModeConfig from(ConfigurationSection section) throws ConfigurationException {
        if (section == null) {
            throw new ConfigurationException("'minigames' section is missing");
        }
        Map<String, GameTypeConfig> map = new HashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection sec = section.getConfigurationSection(key);
            if (sec == null) continue;
            GameTypeConfig gtc = GameTypeConfig.from(key, sec);
            map.put(key.toLowerCase(), gtc);
        }
        return new GameModeConfig(map);
    }

    /** Lookup a specific game’s config by type name (case‐insensitive). */
    public Optional<GameTypeConfig> get(String gameKey) {
        if (gameKey == null) return Optional.empty();
        return Optional.ofNullable(games.get(gameKey.toLowerCase()));
    }

    /** All game configs. */
    public Map<String, GameTypeConfig> getAll() {
        return games;
    }
}