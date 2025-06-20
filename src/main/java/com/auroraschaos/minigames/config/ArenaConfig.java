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

    public static ArenaConfig from(ConfigurationSection section)
            throws ConfigurationException {
        if (section == null) {
            throw new ConfigurationException("'arenas' section is missing");
        }

        com.auroraschaos.minigames.MinigamesPlugin.getInstance()
            .logVerbose("[ArenaConfig] Loading arena definitions...");

        Map<String, ArenaDefinition> map = new HashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection sec = section.getConfigurationSection(key);
            if (sec == null) {
                com.auroraschaos.minigames.MinigamesPlugin.getInstance()
                    .getLogger()
                    .warning("[ArenaConfig] Section for '" + key + "' is missing");
                continue;
            }
            map.put(key.toLowerCase(), ArenaDefinition.from(sec));
        }

        com.auroraschaos.minigames.MinigamesPlugin.getInstance().logVerbose(
            String.format("[ArenaConfig] Loaded %d arena definitions", map.size())
        );

        return new ArenaConfig(map);
    }

    public Map<String, ArenaDefinition> getArenas() {
        return arenas;
    }
}
