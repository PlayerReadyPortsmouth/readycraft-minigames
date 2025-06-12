package com.auroraschaos.minigames.game.race;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TrackConfig {
    private final YamlConfiguration config;

    public TrackConfig(File trackFile, File defaultsFile) throws IOException {
        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(defaultsFile);
        this.config = YamlConfiguration.loadConfiguration(trackFile);
        this.config.addDefaults(defaults);
        this.config.options().copyDefaults(true);
        this.config.save(trackFile);
    }

    public int getInt(String path) {
        return config.getInt(path);
    }
    public double getDouble(String path) {
        return config.getDouble(path);
    }
    public String getString(String path) {
        return config.getString(path);
    }
    public <T> List<T> getList(String path) {
        return (List<T>) config.getList(path);
    }
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }
    public List<Material> getMaterialList(String path) {
        List<Material> list = new ArrayList<>();
        ConfigurationSection sec = config.getConfigurationSection(path);
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                list.add(Material.valueOf(key));
            }
        }
        return list;
    }
    public List<Location> getLocationList(String path) {
        List<Location> locs = new ArrayList<>();
        List<?> raw = config.getList(path);
        if (raw != null) {
            for (Object obj : raw) {
                if (obj instanceof Location) locs.add((Location) obj);
            }
        }
        return locs;
    }
    public boolean contains(String path) {
        return config.contains(path);
    }

    public KartConfig getDefaultKartConfig() {
        throw new UnsupportedOperationException("Unimplemented method 'getDefaultKartConfig'");
    }
}
