package com.auroraschaos.minigames.game.race;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads per-kart configuration (with defaults) and exposes stats, metadata, and config values.
 */
public class KartConfig {
    private final YamlConfiguration config;
    private final String name;
    private final Map<String, Double> stats = new HashMap<>();

    public KartConfig(File file, File defaultsFile) throws IOException {
        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(defaultsFile);
        this.config = YamlConfiguration.loadConfiguration(file);
        this.config.addDefaults(defaults);
        this.config.options().copyDefaults(true);
        this.config.save(file);
        this.name = file.getName().replaceFirst("\\.yml$", "");
        loadStats();
    }

    private void loadStats() {
        if (config.isConfigurationSection("stats")) {
            for (String key : config.getConfigurationSection("stats").getKeys(false)) {
                stats.put(key, config.getDouble("stats." + key));
            }
        }
    }

    /** Gets the internal name (filename without extension) */
    public String getName() {
        return name;
    }

    /** Display name for GUIs */
    public String getDisplayName() {
        return config.getString("display_name");
    }

    /** Icon material for GUIs */
    public Material getIcon() {
        return Material.valueOf(config.getString("icon"));
    }

    /** Retrieve a double stat by key under the 'stats' section */
    public double getStat(String key) {
        return stats.getOrDefault(key, 0.0);
    }

    /** Retrieve a generic double value */
    public double getDouble(String path) {
        return config.getDouble(path);
    }

    /** Retrieve an integer value */
    public int getInt(String path) {
        return config.getInt(path);
    }

    /** Retrieve a string value */
    public String getString(String path) {
        return config.getString(path);
    }

    /** Retrieve a list of locations from config (List<Map<String, Object>> to Location) */
    public List<Location> getLocationList(String path) {
        List<Location> list = new ArrayList<>();
        List<?> raw = config.getList(path);
        if (raw != null) {
            for (Object obj : raw) {
                if (obj instanceof Location) {
                    list.add((Location) obj);
                } else if (obj instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) obj;
                    double x = ((Number) m.get("x")).doubleValue();
                    double y = ((Number) m.get("y")).doubleValue();
                    double z = ((Number) m.get("z")).doubleValue();
                    float yaw = m.containsKey("yaw") ? ((Number) m.get("yaw")).floatValue() : 0f;
                    float pitch = m.containsKey("pitch") ? ((Number) m.get("pitch")).floatValue() : 0f;
                    list.add(new Location(null, x, y, z, yaw, pitch));
                }
            }
        }
        return list;
    }

    /** Retrieve a material list (for blocks, etc.) */
    public List<Material> getMaterialList(String path) {
        List<Material> mats = new ArrayList<>();
        List<String> raw = config.getStringList(path);
        for (String s : raw) {
            mats.add(Material.valueOf(s));
        }
        return mats;
    }

    /** Check if a config path exists */
    public boolean contains(String path) {
        return config.contains(path);
    }
}
