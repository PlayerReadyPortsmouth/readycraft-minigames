package com.auroraschaos.minigames.config;

import com.auroraschaos.minigames.game.GameMode;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds typed settings for one minigame type:
 *   minPlayers, maxPlayers, and which GameMode enum values are enabled.
 */
public class GameTypeConfig {
    private final String key;
    private final int minPlayers;
    private final int maxPlayers;
    private final List<GameMode> enabledModes;

    private GameTypeConfig(String key, int minPlayers, int maxPlayers, List<GameMode> enabledModes) {
        this.key = key;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.enabledModes = List.copyOf(enabledModes);
    }

    /**
     * Parse and validate a single game’s subsection.
     */
    public static GameTypeConfig from(String key, ConfigurationSection sec) throws ConfigurationException {
        String node = "minigames." + key;
        int min = sec.getInt("minPlayers", 1);
        if (min < 1) {
            throw new ConfigurationException(node + ".minPlayers must be ≥ 1 (found " + min + ")");
        }

        int max = sec.getInt("maxPlayers", min);
        if (max < min) {
            throw new ConfigurationException(
                node + ".maxPlayers (" + max + ") must be ≥ minPlayers (" + min + ")"
            );
        }

        List<String> rawModes = sec.getStringList("enabled_modes");
        if (rawModes.isEmpty()) {
            throw new ConfigurationException(node + ".enabled_modes must list at least one mode");
        }
        List<GameMode> modes = new ArrayList<>();
        for (String m : rawModes) {
            try {
                modes.add(GameMode.valueOf(m.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new ConfigurationException(
                    node + ".enabled_modes contains unknown mode: '" + m + "'"
                );
            }
        }

        return new GameTypeConfig(key.toLowerCase(), min, max, modes);
    }

    public String getKey() {
        return key;
    }
    public int getMinPlayers() {
        return minPlayers;
    }
    public int getMaxPlayers() {
        return maxPlayers;
    }
    public List<GameMode> getEnabledModes() {
        return enabledModes;
    }
}