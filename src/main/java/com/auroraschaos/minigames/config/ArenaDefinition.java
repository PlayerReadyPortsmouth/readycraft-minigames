// ArenaDefinition.java
package com.auroraschaos.minigames.config;

import org.bukkit.configuration.ConfigurationSection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Definition for a single arena type, parsed from configuration.
 */
public class ArenaDefinition {
    private final String key;
    private final String schematic;
    private final String worldName;
    private final Map<String, String> flags;
    private final long resetIntervalTicks;

    public ArenaDefinition(String key,
                           String schematic,
                           String worldName,
                           Map<String, String> flags,
                           long resetIntervalTicks) {
        this.key = key;
        this.schematic = schematic;
        this.worldName = worldName;
        this.flags = Collections.unmodifiableMap(new HashMap<>(flags));
        this.resetIntervalTicks = resetIntervalTicks;
    }

    public static ArenaDefinition from(ConfigurationSection sec)
            throws ConfigurationException {
        String key = sec.getName().toLowerCase();
        String schematic = sec.getString("schematic");
        if (schematic == null || schematic.isEmpty()) {
            throw new ConfigurationException(
                "Missing 'schematic' for arena: " + key
            );
        }

        String world = sec.getString("world", "minigames_world");
        Map<String, String> flagsMap = new HashMap<>();
        ConfigurationSection flagsSec = sec.getConfigurationSection("flags");
        if (flagsSec != null) {
            for (String flagName : flagsSec.getKeys(false)) {
                String value = flagsSec.getString(flagName);
                if (value == null) {
                    throw new ConfigurationException(
                        "Flag '" + flagName + "' for arena '" + key + "' is null"
                    );
                }
                flagsMap.put(flagName.toUpperCase(), value.toUpperCase());
            }
        }

        long intervalSec = sec.getLong("resetIntervalSeconds", 60L);
        long intervalTicks = intervalSec * 20L;

        com.auroraschaos.minigames.MinigamesPlugin.getInstance().getLogger().info(
            String.format(
                "[ArenaConfig] Parsed arena '%s' world=%s schematic=%s",
                key,
                world,
                schematic
            )
        );

        return new ArenaDefinition(key, schematic, world, flagsMap, intervalTicks);
    }

    public String getKey() { return key; }
    public String getSchematic() { return schematic; }
    public String getWorldName() { return worldName; }
    public Map<String, String> getFlags() { return flags; }
    public long getResetIntervalTicks() { return resetIntervalTicks; }
}
