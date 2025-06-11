// ArenaConfig.java
package com.auroraschaos.minigames.config;

import org.bukkit.configuration.ConfigurationSection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for all arenas, loaded from the 'arenas' section.
 */
public class ArenaConfig {
    private final Map<String, ArenaDefinition> arenas;

    private ArenaConfig(Map<String, ArenaDefinition> arenas) {
        this.arenas = Collections.unmodifiableMap(arenas);
    }

    public static ArenaConfig from(ConfigurationSection section) throws ConfigurationException {
        if (section == null) {
            throw new ConfigurationException("'arenas' section is missing");
        }
        Map<String, ArenaDefinition> map = new HashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection sec = section.getConfigurationSection(key);
            if (sec == null) continue;
            map.put(key.toLowerCase(), ArenaDefinition.from(sec));
        }
        return new ArenaConfig(map);
    }

    public Map<String, ArenaDefinition> getArenas() {
        return arenas;
    }
}
