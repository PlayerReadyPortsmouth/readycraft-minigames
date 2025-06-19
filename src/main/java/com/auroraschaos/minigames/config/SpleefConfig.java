package com.auroraschaos.minigames.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration values for the Spleef minigame loaded from Spleef.yml.
 */
public class SpleefConfig {
    private final int spawnYOffset;
    private final int gameDuration;
    private final int snowballCooldown;
    private final List<Material> breakableBlocks;

    private SpleefConfig(int spawnYOffset, int gameDuration, int snowballCooldown,
                         List<Material> breakableBlocks) {
        this.spawnYOffset = spawnYOffset;
        this.gameDuration = gameDuration;
        this.snowballCooldown = snowballCooldown;
        this.breakableBlocks = List.copyOf(breakableBlocks);
    }

    /**
     * Parse the supplied YAML configuration to a {@code SpleefConfig} instance.
     */
    public static SpleefConfig from(YamlConfiguration cfg) throws ConfigurationException {
        if (cfg == null) {
            throw new ConfigurationException("Spleef configuration is missing");
        }
        int offset = cfg.getInt("spawn_y_offset", 10);
        int duration = cfg.getInt("game_duration", 300);
        int cooldown = cfg.getInt("snowball_cooldown", 1);
        List<String> rawBlocks = cfg.getStringList("breakable_blocks");
        if (rawBlocks.isEmpty()) {
            rawBlocks = List.of("SNOW_BLOCK", "SNOW");
        }
        List<Material> blocks = new ArrayList<>();
        for (String s : rawBlocks) {
            Material m = Material.matchMaterial(s);
            if (m == null) {
                throw new ConfigurationException("Unknown material '" + s + "' in breakable_blocks");
            }
            blocks.add(m);
        }
        return new SpleefConfig(offset, duration, cooldown, blocks);
    }

    public int getSpawnYOffset() { return spawnYOffset; }
    public int getGameDuration() { return gameDuration; }
    public int getSnowballCooldown() { return snowballCooldown; }
    public List<Material> getBreakableBlocks() { return breakableBlocks; }
}
