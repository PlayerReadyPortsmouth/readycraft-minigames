package com.auroraschaos.minigames.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Holds configuration for the Trouble in Terrorist Town minigame.
 */
public class TTTConfig {
    private final int spawnYOffset;
    private final int gameDuration;
    private final double traitorRatio;
    private final double detectiveRatio;
    private final java.util.List<ShopItem> shopItems;

    private TTTConfig(int spawnYOffset, int gameDuration,
                      double traitorRatio, double detectiveRatio,
                      java.util.List<ShopItem> shopItems) {
        this.spawnYOffset = spawnYOffset;
        this.gameDuration = gameDuration;
        this.traitorRatio = traitorRatio;
        this.detectiveRatio = detectiveRatio;
        this.shopItems = java.util.List.copyOf(shopItems);
    }

    /**
     * Parse a YAML configuration into a {@code TTTConfig} instance.
     */
    public static TTTConfig from(YamlConfiguration cfg) throws ConfigurationException {
        if (cfg == null) {
            throw new ConfigurationException("TTT configuration is missing");
        }
        int offset = cfg.getInt("spawn_y_offset", 10);
        int duration = cfg.getInt("game_duration", 300);
        double tRatio = cfg.getDouble("traitor_ratio", 0.25);
        double dRatio = cfg.getDouble("detective_ratio", 0.15);
        if (tRatio <= 0 || tRatio >= 1) {
            throw new ConfigurationException("traitor_ratio must be between 0 and 1");
        }
        if (dRatio < 0 || dRatio >= 1) {
            throw new ConfigurationException("detective_ratio must be between 0 and 1");
        }
        java.util.List<ShopItem> items = new java.util.ArrayList<>();
        ConfigurationSection shopSection = cfg.getConfigurationSection("shop.items");
        if (shopSection != null) {
            for (String key : shopSection.getKeys(false)) {
                ConfigurationSection sec = shopSection.getConfigurationSection(key);
                if (sec == null) continue;
                int slot = sec.getInt("slot", 0);
                String matName = sec.getString("material", "STONE");
                Material mat = Material.matchMaterial(matName);
                if (mat == null) {
                    throw new ConfigurationException("Unknown material '" + matName + "' in shop item '" + key + "'");
                }
                String name = sec.getString("name", matName);
                java.util.List<String> lore = sec.getStringList("lore");
                int cost = sec.getInt("cost", 1);
                items.add(new ShopItem(slot, mat, name, lore, cost));
            }
        }
        return new TTTConfig(offset, duration, tRatio, dRatio, items);
    }

    public int getSpawnYOffset() { return spawnYOffset; }
    public int getGameDuration() { return gameDuration; }
    public double getTraitorRatio() { return traitorRatio; }
    public double getDetectiveRatio() { return detectiveRatio; }
    public java.util.List<ShopItem> getShopItems() { return shopItems; }

    /** Item definition for the TTT shop. */
    public static class ShopItem {
        private final int slot;
        private final Material material;
        private final String name;
        private final java.util.List<String> lore;
        private final int cost;

        public ShopItem(int slot, Material material, String name,
                        java.util.List<String> lore, int cost) {
            this.slot = slot;
            this.material = material;
            this.name = name;
            this.lore = java.util.List.copyOf(lore);
            this.cost = cost;
        }

        public int getSlot() { return slot; }
        public Material getMaterial() { return material; }
        public String getName() { return name; }
        public java.util.List<String> getLore() { return lore; }
        public int getCost() { return cost; }
    }
}
