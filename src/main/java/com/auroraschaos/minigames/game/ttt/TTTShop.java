package com.auroraschaos.minigames.game.ttt;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.auroraschaos.minigames.config.TTTConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the shop system for the Trouble in Terrorist Town minigame.
 * Players earn points to buy configured items via a simple GUI.
 */
public class TTTShop implements Listener {

    /** Title for the shop inventory. */
    private static final String SHOP_TITLE = ChatColor.DARK_GREEN + "TTT Shop";

    /** Material that opens the shop when right-clicked. */
    private static final Material OPEN_MATERIAL = Material.EMERALD;

    private final Map<Integer, ShopEntry> shopItems = new HashMap<>();
    private final Map<Player, Integer> points = new HashMap<>();
    private final MinigamesPlugin plugin;

    public TTTShop(MinigamesPlugin plugin, List<TTTConfig.ShopItem> items) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadItems(items);
    }

    private void loadItems(List<TTTConfig.ShopItem> items) {
        shopItems.clear();
        for (TTTConfig.ShopItem cfg : items) {
            ItemStack item = new ItemStack(cfg.getMaterial());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', cfg.getName()));
            List<String> lore = new ArrayList<>();
            for (String line : cfg.getLore()) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
            shopItems.put(cfg.getSlot(), new ShopEntry(item, cfg.getCost()));
        }
    }

    /** Create the inventory opener item. */
    public ItemStack createOpenerItem() {
        ItemStack item = new ItemStack(OPEN_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Open Shop");
        item.setItemMeta(meta);
        return item;
    }

    /** Give a player a point to spend in the shop. */
    public void addPoint(Player player) {
        int newPts = points.getOrDefault(player, 0) + 1;
        points.put(player, newPts);
        player.sendMessage(ChatColor.GREEN + "Shop points: " + newPts);
    }

    /** Open the shop GUI for a player. */
    public void openShop(Player player) {
        Inventory inv = Bukkit.createInventory(player, 9, SHOP_TITLE);
        for (Map.Entry<Integer, ShopEntry> e : shopItems.entrySet()) {
            inv.setItem(e.getKey(), e.getValue().item);
        }
        player.openInventory(inv);
    }

    /** Handle buying an item when clicking inside the shop inventory. */
    @EventHandler
    public void onShopClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(SHOP_TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        ShopEntry entry = shopItems.get(slot);
        if (entry == null) return;
        int pts = points.getOrDefault(p, 0);
        if (pts < entry.cost) {
            p.sendMessage(ChatColor.RED + "Not enough points!");
            return;
        }
        points.put(p, pts - entry.cost);
        p.getInventory().addItem(entry.item.clone());
        String name = ChatColor.stripColor(entry.item.getItemMeta().getDisplayName());
        p.sendMessage(ChatColor.GREEN + "Purchased " + name + "!");
        p.closeInventory();
    }

    /** Detect right-click with the opener item to show the shop. */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null && item.getType() == OPEN_MATERIAL) {
            event.setCancelled(true);
            openShop(p);
        }
    }

    /** Stop listening for events when the game ends. */
    public void unregister() {
        HandlerList.unregisterAll(this);
        points.clear();
    }

    private static class ShopEntry {
        final ItemStack item;
        final int cost;
        ShopEntry(ItemStack item, int cost) {
            this.item = item;
            this.cost = cost;
        }
    }
}

