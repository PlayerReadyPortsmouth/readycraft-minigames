package com.auroraschaos.minigames.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.auroraschaos.minigames.game.GameMode;

import java.util.*;

/**
 * GUIManager builds and manages:
 *  1) Main Minigame Selection Menu (one item per configured minigame)
 *  2) Mode Selection Menu (one item per enabled GameMode of the clicked minigame)
 *
 * Clicking on a GameMode item runs "/minigames join <gameType> <mode>" for that player.
 */
public class GUIManager implements Listener {

    private final MinigamesPlugin plugin;

    /** Inventory titles must be unique per menu */
    private static final String MAIN_MENU_TITLE = ChatColor.DARK_GREEN + "Select Minigame";
    private static final String MODE_MENU_TITLE_PREFIX = ChatColor.DARK_BLUE + "Select Mode: ";

    /** Cache the built main menu so we don't rebuild each time */
    private Inventory mainMenuInventory;

    /** Temporarily store which player opened which mode menu for which gameType */
    private final Map<UUID, String> playerToGameType = new HashMap<>();

    public GUIManager(MinigamesPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Opens the main minigame selection menu for the given player.
     */
    public void openMainMenu(Player player) {
        if (mainMenuInventory == null) {
            mainMenuInventory = buildMainMenu();
        }
        player.openInventory(mainMenuInventory);
    }

    /**
     * Builds the main menu Inventory based on config.yml entries under "minigames".
     */
    private Inventory buildMainMenu() {
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection mgSection = cfg.getConfigurationSection("minigames");
        if (mgSection == null) {
            plugin.getLogger().warning("No 'minigames' section found in config.yml");
            // Create an empty 9-slot inventory
            return Bukkit.createInventory(null, 9, MAIN_MENU_TITLE);
        }

        Set<String> gameKeys = mgSection.getKeys(false);
        int size = ((gameKeys.size() - 1) / 9 + 1) * 9; // round up to multiple of 9
        Inventory inv = Bukkit.createInventory(null, size, MAIN_MENU_TITLE);

        int slot = 0;
        for (String gameKey : gameKeys) {
            ConfigurationSection gameCfg = mgSection.getConfigurationSection(gameKey);
            if (gameCfg == null) continue;

            String displayName = ChatColor.translateAlternateColorCodes('&', gameCfg.getString("display_name", gameKey));
            String iconName = gameCfg.getString("icon", "STONE");
            Material iconMat;
            try { iconMat = Material.valueOf(iconName.toUpperCase()); }
            catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid icon '" + iconName + "' for game " + gameKey + ". Using STONE.");
                iconMat = Material.STONE;
            }

            List<String> loreRaw = gameCfg.getStringList("lore");
            List<String> lore = new ArrayList<>();
            for (String line : loreRaw) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }

            ItemStack item = new ItemStack(iconMat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            item.setItemMeta(meta);

            // Store the gameKey in the item's persistent data or use slot mapping
            inv.setItem(slot, item);
            slot++;
        }

        return inv;
    }

    /**
     * Builds a mode selection inventory for a specific gameType, based on config.yml.
     */
    private Inventory buildModeMenu(String gameType) {
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection mgSection = cfg.getConfigurationSection("minigames." + gameType);
        if (mgSection == null) {
            plugin.getLogger().warning("No config found for gameType: " + gameType);
            return Bukkit.createInventory(null, 9, MODE_MENU_TITLE_PREFIX + gameType);
        }

        List<String> modeListCfg = mgSection.getStringList("enabled_modes");
        List<GameMode> modes = new ArrayList<>();
        for (String modeStr : modeListCfg) {
            try {
                modes.add(GameMode.valueOf(modeStr.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid GameMode '" + modeStr + "' for " + gameType);
            }
        }

        int size = ((modes.size() - 1) / 9 + 1) * 9;
        Inventory inv = Bukkit.createInventory(null, size, MODE_MENU_TITLE_PREFIX + gameType);

        int slot = 0;
        for (GameMode mode : modes) {
            // Use a corresponding Material for each mode, or fallback to PAPER
            Material icon;
            switch (mode) {
                case CLASSIC:  icon = Material.PAPER; break;
                case HARDCORE: icon = Material.IRON_SWORD; break;
                case TEAMS:    icon = Material.WHITE_BANNER; break;
                case SOLO:     icon = Material.FEATHER; break;
                case TIMED:    icon = Material.CLOCK; break;
                case INSANE:   icon = Material.TNT; break;
                default:       icon = Material.PAPER; break;
            }

            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + mode.name().replace("_", " "));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Click to join");
            meta.setLore(lore);
            item.setItemMeta(meta);

            inv.setItem(slot, item);
            slot++;
        }


        // Back button in the final slot
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Go Back");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Return to previous menu");
        meta.setLore(lore);
        item.setItemMeta(meta);

        inv.setItem(8, item);


        return inv;
    }

    /**
     * Handle clicks in both main menu and mode menu.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        
        String title = event.getView().getTitle();
        Inventory inv  = event.getInventory();

        // Prevent taking items
        event.setCancelled(true);

        // 1) Handle Main Menu clicks
        if (title.equals(MAIN_MENU_TITLE)) {
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= inv.getSize()) return;

            ItemStack clicked = inv.getItem(slot);
            if (clicked == null || !clicked.hasItemMeta()) return;

            String displayName = clicked.getItemMeta().getDisplayName();
            // Reverse‐lookup the gameType from displayName or slot index:
            FileConfiguration cfg = plugin.getConfig();
            for (String gameKey : cfg.getConfigurationSection("minigames").getKeys(false)) {
                String configuredName = ChatColor.translateAlternateColorCodes('&',
                        cfg.getString("minigames." + gameKey + ".display_name", gameKey));
                if (configuredName.equals(displayName)) {
                    // Found the gameKey
                    openModeMenu(p, gameKey);
                    return;
                }
            }
        }

        // 2) Handle Mode Menu clicks
        if (title.startsWith(MODE_MENU_TITLE_PREFIX)) {
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= inv.getSize()) return;

            ItemStack clicked = inv.getItem(slot);
            if (clicked == null || !clicked.hasItemMeta()) return;

            // If the user wishes to go back to the previous menu
            if (clicked.getItemMeta().getDisplayName().equals(ChatColor.RED + "Go Back")) {
                openMainMenu(p);
                return;
            }

            String modeName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).replace(" ", "_");
            String gameType = playerToGameType.get(p.getUniqueId());
            if (gameType == null) return;

            // Execute join command
            p.closeInventory();
            Bukkit.dispatchCommand(p, "minigames join " + gameType + " " + modeName);
        }
    }

    /**
     * Opens the mode selection menu for the given gameType and stores the mapping.
     */
    private void openModeMenu(Player player, String gameType) {
        playerToGameType.put(player.getUniqueId(), gameType);
        Inventory modeMenu = buildModeMenu(gameType);
        player.openInventory(modeMenu);
    }
}
