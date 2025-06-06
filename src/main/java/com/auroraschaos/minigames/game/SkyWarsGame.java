package com.auroraschaos.minigames.game;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.auroraschaos.minigames.arena.Arena;
import com.auroraschaos.minigames.game.GameMode;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.bukkit.BukkitWorld;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

/**
 * SkyWarsGame: a full implementation of a SkyWars minigame.
 *
 * - Loads global and per-arena YAML configs (schematic, spawns, loot_table, events, timings).
 * - Pastes a schematic via WorldEdit, detects all chest locations.
 * - Fills each chest with weighted-random loot.
 * - Assigns players to spawn locations and teleports them.
 * - Schedules random events (double damage, no healing, etc.) according to weights.
 * - Computes an initial shrink radius, then shrinks the arena ring by ring.
 * - Handles player deaths and out-of-bounds eliminations, moving eliminated players to spectator mode.
 * - Announces the winner when only one player remains (or none).
 * - Cleans up tasks, listeners, and returns spectators to lobby on game end.
 */
public class SkyWarsGame extends GameInstance implements Listener {

    // ---------- CONFIGURATION FIELDS ----------

    private final MinigamesPlugin plugin;
    private final Arena arena;
    private final GameMode gameMode;

    // Per-arena settings (loaded from SkyWars/ArenaX.yml or Defaults.yml)
    private String schematicFile;
    private Location shrinkCenter;
    private List<Location> spawnLocations;
    private List<Location> chestLocations;
    private List<Map.Entry<ItemStack, Integer>> lootTable;

    // Global SkyWars settings (loaded from SkyWars.yml)
    private int eventInterval;
    private int eventWarningTime;
    private int shrinkStart;
    private int shrinkInterval;
    private int shrinkSpeed;
    private Map<String, EventDef> globalEvents;

    // Computed at runtime
    private int shrinkRadius = -1;

    // State flags for events
    private boolean doubleDamageActive = false;
    private boolean noHealingActive = false;

    // ---------- RUNTIME STATE ----------

    private final List<Player> alivePlayers = new ArrayList<>();
    private final List<Player> spectators = new ArrayList<>();

    private BukkitTask eventTask;
    private BukkitTask shrinkTask;

    private final Random lootRng = new Random();

    // ---------- CONSTRUCTOR ----------

    public SkyWarsGame(MinigamesPlugin plugin, Arena arena, String type, GameMode gameMode, List<Player> participants) {
        super(plugin, arena, type, gameMode, participants);
        this.plugin = plugin;
        this.arena = arena;
        this.gameMode = gameMode;
    }

    // ---------- GAME LIFECYCLE METHODS ----------

    @Override
    protected void onGameStart() {
        // 1) Register event listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // 2) Load configuration (global + per-arena)
        loadArenaConfig();

        // 3) Paste schematic & detect chest locations
        pasteSchematicAndDetectChests();

        // 4) Compute initial shrink radius based on pasted blocks
        computeInitialShrinkRadius();

        // 5) Fill all detected chests with loot
        fillAllChests();

        // 6) Assign and teleport players to islands
        assignSpawnsAndTeleportPlayers();

        // 7) Announce game start
        broadcastMessage("§aSkyWars has begun on arena: " + arena.getName() + "!");

        // 8) Start event scheduling and arena shrinking
        startEventSchedule();
        startShrinkSchedule();
    }

    @Override
    protected void onGameEnd() {
        // Unregister all event listeners
        HandlerList.unregisterAll(this);

        // Cancel scheduled tasks
        if (eventTask != null) eventTask.cancel();
        if (shrinkTask != null) shrinkTask.cancel();

        // Return spectators to lobby spawn in first world
        for (Player spec : spectators) {
            spec.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
            spec.setGameMode(org.bukkit.GameMode.SURVIVAL);
        }
    }

    @Override
    protected boolean requiresTicks() {
        return false; // We use our own BukkitTasks instead of tick()
    }

    @Override
    protected void tick() {
        // Not used
    }

    // ---------- CONFIG LOADING ----------

    /**
     * Load global SkyWars.yml and per-arena YAML (falling back to Defaults.yml).
     */
    private void loadArenaConfig() {
        // 1) Load global SkyWars.yml
        File globalFile = new File(plugin.getDataFolder(), "SkyWars.yml");
        FileConfiguration globalCfg = YamlConfiguration.loadConfiguration(globalFile);

        // 2) Load arena-specific YAML; fallback to Defaults.yml if not found
        String arenaFileName = arena.getName() + ".yml"; // e.g. "Arena1.yml"
        File arenaFile = new File(plugin.getDataFolder(), "SkyWars/" + arenaFileName);
        FileConfiguration arenaCfg;
        if (!arenaFile.exists()) {
            File defaults = new File(plugin.getDataFolder(), "SkyWars/Defaults.yml");
            arenaCfg = YamlConfiguration.loadConfiguration(defaults);
        } else {
            arenaCfg = YamlConfiguration.loadConfiguration(arenaFile);
        }

        // 3) Read schematic file name
        String schematicName = arenaCfg.getString("schematic", "");
        if (schematicName.isEmpty()) {
            plugin.getLogger().warning("No schematic defined for arena " + arena.getName());
        }
        this.schematicFile = schematicName;

        // 4) Read shrink center location
        double cx = arenaCfg.getDouble("center.x", 0.0);
        double cy = arenaCfg.getDouble("center.y", 64.0);
        double cz = arenaCfg.getDouble("center.z", 0.0);
        this.shrinkCenter = new Location(arena.getWorld(), cx, cy, cz);

        // 5) Read spawn locations
        this.spawnLocations = new ArrayList<>();
        if (arenaCfg.isList("spawns")) {
            for (Object obj : arenaCfg.getList("spawns")) {
                if (obj instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> m = (java.util.Map<String, Object>) obj;
                    double x = ((Number) m.get("x")).doubleValue();
                    double y = ((Number) m.get("y")).doubleValue();
                    double z = ((Number) m.get("z")).doubleValue();
                    this.spawnLocations.add(new Location(arena.getWorld(), x, y, z));
                }
            }
        } else {
            plugin.getLogger().warning("No spawns defined in " + arena.getName() + ".yml");
        }

        // 6) Build loot table from per-arena config
        this.lootTable = new ArrayList<>();
        if (arenaCfg.isList("loot_table")) {
            for (Object raw : arenaCfg.getList("loot_table")) {
                if (!(raw instanceof java.util.Map)) continue;
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> entry = (java.util.Map<String, Object>) raw;
                String typeName = (String) entry.get("type");
                int weight = ((Number) entry.getOrDefault("weight", 1)).intValue();
                int amount = ((Number) entry.getOrDefault("amount", 1)).intValue();

                Material mat = Material.getMaterial(typeName.toUpperCase());
                if (mat == null) {
                    plugin.getLogger().warning("Invalid material '" + typeName + "' in " + arena.getName());
                    continue;
                }
                ItemStack stack = new ItemStack(mat, amount);
                this.lootTable.add(new SimpleEntry<>(stack, weight));
            }
        }

        // 7) Read global event settings
        this.eventInterval = globalCfg.getInt("event_interval", 90);
        this.eventWarningTime = globalCfg.getInt("event_warning_time", 15);
        this.shrinkStart = globalCfg.getInt("shrink_start", 180);
        this.shrinkInterval = globalCfg.getInt("shrink_interval", 10);
        this.shrinkSpeed = globalCfg.getInt("shrink_speed", 1);

        this.globalEvents = new HashMap<>();
        if (globalCfg.isConfigurationSection("events")) {
            for (String key : globalCfg.getConfigurationSection("events").getKeys(false)) {
                String path = "events." + key + ".";
                int weight = globalCfg.getInt(path + "weight", 1);
                int duration = globalCfg.getInt(path + "duration", 10);
                String announce = globalCfg.getString(path + "announce", "");
                globalEvents.put(key, new EventDef(key, weight, duration, announce));
            }
        }
    }

    /**
     * Simple holder for event definition (name, weight, duration, announce text).
     */
    private static class EventDef {
        public final String name;
        public final int weight;
        public final int duration;
        public final String announceText;

        public EventDef(String name, int weight, int duration, String announceText) {
            this.name = name;
            this.weight = weight;
            this.duration = duration;
            this.announceText = announceText;
        }
    }

    // ---------- SCHEMATIC PASTING & CHEST DETECTION ----------

    /**
     * Paste the schematic at shrinkCenter using WorldEdit 7+ API, then scan the pasted region
     * for all Chest blocks and record their locations.
     */
    private void pasteSchematicAndDetectChests() {
        try {
            File schematic = new File(plugin.getDataFolder(), "SkyWars/schematics/" + schematicFile);
            if (!schematic.exists()) {
                plugin.getLogger().warning("Schematic " + schematicFile + " not found for arena " + arena.getName());
                return;
            }

            // Load schematic into a WorldEdit Clipboard
            Clipboard clipboard;
            try (FileInputStream fis = new FileInputStream(schematic)) {
                ClipboardReader reader = com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats.findByFile(schematic).getReader(fis);
                clipboard = reader.read();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to read schematic “" + schematicFile + "”: " + e.getMessage());
                return;
            }

            // Paste the clipboard at shrinkCenter using the correct WorldEdit 7+ pattern
            com.sk89q.worldedit.world.World weWorld = new BukkitWorld(arena.getWorld());
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                Operation operation = new com.sk89q.worldedit.session.ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(
                        shrinkCenter.getBlockX(),
                        shrinkCenter.getBlockY(),
                        shrinkCenter.getBlockZ()
                    ))
                    .ignoreAirBlocks(false)
                    .build();
                Operations.complete(operation);
            }

            // Determine pasted region bounds
            int w = clipboard.getDimensions().getX();
            int h = clipboard.getDimensions().getY();
            int d = clipboard.getDimensions().getZ();

            int minX = shrinkCenter.getBlockX();
            int minY = shrinkCenter.getBlockY();
            int minZ = shrinkCenter.getBlockZ();
            int maxX = minX + w;
            int maxY = minY + h;
            int maxZ = minZ + d;

            // Scan for chest blocks
            this.chestLocations = new ArrayList<>();
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Location loc = new Location(arena.getWorld(), x, y, z);
                        Block b = loc.getBlock();
                        if (b.getState() instanceof Chest) {
                            chestLocations.add(loc);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().severe("Unexpected error while pasting schematic: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ---------- SHRINK RADIUS COMPUTATION ----------

    /**
     * Compute the initial shrinkRadius by scanning non-air blocks around shrinkCenter.
     * Assumes arena is contained within a finite bounding cube.
     */
    private void computeInitialShrinkRadius() {
        int cx = shrinkCenter.getBlockX();
        int cy = shrinkCenter.getBlockY();
        int cz = shrinkCenter.getBlockZ();

        int maxDist = 0;
        int bound = 100; // Adjust to encompass your arena footprint
        int minY = cy - 5;
        int maxY = cy + 50; // Adjust vertical range as needed

        for (int x = cx - bound; x <= cx + bound; x++) {
            for (int z = cz - bound; z <= cz + bound; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Location loc = new Location(arena.getWorld(), x, y, z);
                    if (loc.getBlock().getType() != Material.AIR) {
                        int dx = Math.abs(x - cx);
                        int dz = Math.abs(z - cz);
                        int dist = Math.max(dx, dz);
                        maxDist = Math.max(maxDist, dist);
                    }
                }
            }
        }

        this.shrinkRadius = maxDist;
    }

    // ---------- LOOT SELECTION & CHEST FILLING ----------

    /**
     * Select one random ItemStack from lootTable based on weights.
     * Returns a list containing exactly one item; adjust if you want multiple items per chest.
     */
    private List<ItemStack> selectLootForChest() {
        List<ItemStack> chosen = new ArrayList<>();
        if (lootTable.isEmpty()) return chosen;

        int totalWeight = 0;
        for (Map.Entry<ItemStack, Integer> entry : lootTable) {
            totalWeight += entry.getValue();
        }

        int r = lootRng.nextInt(totalWeight);
        int cumulative = 0;
        for (Map.Entry<ItemStack, Integer> entry : lootTable) {
            cumulative += entry.getValue();
            if (r < cumulative) {
                chosen.add(entry.getKey().clone());
                break;
            }
        }

        return chosen;
    }

    /**
     * Iterate through all detected chestLocations, clear each chest inventory,
     * and fill it with selected loot.
     */
    private void fillAllChests() {
        for (Location loc : chestLocations) {
            Block b = loc.getBlock();
            if (b.getState() instanceof Chest) {
                Inventory inv = ((Chest) b.getState()).getBlockInventory();
                inv.clear();
                List<ItemStack> loot = selectLootForChest();
                for (ItemStack item : loot) {
                    inv.addItem(item);
                }
            }
        }
    }

    // ---------- PLAYER SPAWNS & TELEPORT ----------

    /**
     * Assign each participant to a spawn location (cycled if participants > spawns),
     * add to alivePlayers, and teleport them.
     */
    private void assignSpawnsAndTeleportPlayers() {
        for (int i = 0; i < participants.size(); i++) {
            Player p = participants.get(i);
            alivePlayers.add(p);

            Location spawn = spawnLocations.get(i % spawnLocations.size());
            p.teleport(spawn);
            p.getInventory().clear();
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1.0f, 1.0f);
        }
    }

    // ---------- RANDOM EVENT SCHEDULING ----------

    /**
     * Kick off a repeating task that, every eventInterval seconds, picks a weighted random event.
     */
    private void startEventSchedule() {
        if (globalEvents.isEmpty()) return;

        eventTask = new BukkitRunnable() {
            @Override
            public void run() {
                scheduleNextEvent();
            }
        }.runTaskTimer(plugin, 20L * eventInterval, 20L * eventInterval);
    }

    /**
     * Pick a random event from globalEvents by weight, announce warning, then activate after eventWarningTime.
     */
    private void scheduleNextEvent() {
        if (globalEvents.isEmpty()) return;

        int totalWeight = 0;
        for (EventDef def : globalEvents.values()) {
            totalWeight += def.weight;
        }

        int r = lootRng.nextInt(totalWeight);
        int cumulative = 0;
        EventDef chosenLocal = null;
        for (EventDef def : globalEvents.values()) {
            cumulative += def.weight;
            if (r < cumulative) {
                chosenLocal = def;
                break;
            }
        }
        if (chosenLocal == null) return;

        // Store in a final local for use in the inner class
        final EventDef eventToActivate = chosenLocal;

        // Announce warning
        broadcastMessage(eventToActivate.announceText);

        // Activate event after warning time
        new BukkitRunnable() {
            @Override
            public void run() {
                activateEvent(eventToActivate);
            }
        }.runTaskLater(plugin, 20L * eventWarningTime);
    }

    /**
     * Route to the appropriate activation logic based on event name.
     */
    private void activateEvent(EventDef def) {
        switch (def.name.toLowerCase()) {
            case "double_damage":
                activateDoubleDamage(def.duration);
                break;
            case "no_healing":
                activateNoHealing(def.duration);
                break;
            default:
                plugin.getLogger().warning("Unknown event: " + def.name);
        }
    }

    private void activateDoubleDamage(int durationSeconds) {
        doubleDamageActive = true;
        broadcastMessage("§cDouble Damage ACTIVE for " + durationSeconds + " seconds!");
        new BukkitRunnable() {
            @Override
            public void run() {
                doubleDamageActive = false;
                broadcastMessage("§cDouble Damage has ended!");
            }
        }.runTaskLater(plugin, 20L * durationSeconds);
    }

    private void activateNoHealing(int durationSeconds) {
        noHealingActive = true;
        broadcastMessage("§4No Healing ACTIVE for " + durationSeconds + " seconds!");
        new BukkitRunnable() {
            @Override
            public void run() {
                noHealingActive = false;
                broadcastMessage("§4No Healing has ended!");
            }
        }.runTaskLater(plugin, 20L * durationSeconds);
    }

    // ---------- ARENA SHRINKING ----------

    /**
     * Schedule the shrinkArena() method to run after shrinkStart seconds, then every shrinkInterval seconds.
     */
    private void startShrinkSchedule() {
        shrinkTask = new BukkitRunnable() {
            @Override
            public void run() {
                shrinkArena();
            }
        }.runTaskTimer(plugin, 20L * shrinkStart, 20L * shrinkInterval);
    }

    /**
     * Remove a 1-block-thick square ring at current shrinkRadius, then decrement shrinkRadius.
     */
    private void shrinkArena() {
        if (shrinkRadius <= 0) {
            if (shrinkTask != null) {
                shrinkTask.cancel();
            }
            return;
        }

        int cx = shrinkCenter.getBlockX();
        int cy = shrinkCenter.getBlockY();
        int cz = shrinkCenter.getBlockZ();
        int r = shrinkRadius;

        int minY = cy - 5;
        int maxY = cy + 50;

        // Remove blocks on the ring at distance r
        // Top and bottom edges of the square
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z : new int[]{cz - r, cz + r}) {
                for (int y = minY; y <= maxY; y++) {
                    Location loc = new Location(arena.getWorld(), x, y, z);
                    if (loc.getBlock().getType() != Material.AIR) {
                        loc.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
        // Left and right edges (excluding corners already handled)
        for (int z = cz - r + 1; z <= cz + r - 1; z++) {
            for (int x : new int[]{cx - r, cx + r}) {
                for (int y = minY; y <= maxY; y++) {
                    Location loc = new Location(arena.getWorld(), x, y, z);
                    if (loc.getBlock().getType() != Material.AIR) {
                        loc.getBlock().setType(Material.AIR);
                    }
                }
            }
        }

        broadcastMessage("§cArena is shrinking! New radius: " + (r - 1));

        shrinkRadius -= shrinkSpeed;
        if (shrinkRadius <= 0 && shrinkTask != null) {
            shrinkTask.cancel();
        }
    }

    // ---------- EVENT HANDLERS ----------

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deceased = event.getEntity();
        if (!alivePlayers.contains(deceased)) return;

        event.getDrops().clear();
        event.setDroppedExp(0);

        alivePlayers.remove(deceased);
        spectators.add(deceased);

        Location specLoc = new Location(arena.getWorld(), shrinkCenter.getX(), shrinkCenter.getY() + 30, shrinkCenter.getZ());
        deceased.teleport(specLoc);
        deceased.setGameMode(org.bukkit.GameMode.SPECTATOR);

        Player killer = deceased.getKiller();
        if (killer != null) {
            broadcastMessage("§c" + deceased.getName() + " was slain by " + killer.getName() + "!");
        } else {
            broadcastMessage("§c" + deceased.getName() + " died!");
        }

        checkWinCondition();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();

        // Spectators stay in spectator mode
        if (spectators.contains(p)) {
            if (p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                p.setGameMode(org.bukkit.GameMode.SPECTATOR);
            }
            return;
        }

        // Alive players: check out-of-bounds (outside shrinkRadius)
        if (alivePlayers.contains(p) && shrinkRadius > 0) {
            double dx = p.getLocation().getX() - shrinkCenter.getX();
            double dz = p.getLocation().getZ() - shrinkCenter.getZ();
            int dist = (int) Math.max(Math.abs(dx), Math.abs(dz));
            if (dist > shrinkRadius) {
                alivePlayers.remove(p);
                spectators.add(p);
                p.teleport(new Location(arena.getWorld(), shrinkCenter.getX(), shrinkCenter.getY() + 30, shrinkCenter.getZ()));
                p.setGameMode(org.bukkit.GameMode.SPECTATOR);
                broadcastMessage("§c" + p.getName() + " was outside the boundary and eliminated!");

                checkWinCondition();
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;
        Player damager = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        if (!alivePlayers.contains(damager) || !alivePlayers.contains(victim)) return;

        if (doubleDamageActive) {
            event.setDamage(event.getDamage() * 2);
        }
    }

    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        if (!alivePlayers.contains(p)) return;
        if (noHealingActive) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerFall(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        if (!alivePlayers.contains(p)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            alivePlayers.remove(p);
            spectators.add(p);
            p.teleport(new Location(arena.getWorld(), shrinkCenter.getX(), shrinkCenter.getY() + 30, shrinkCenter.getZ()));
            p.setGameMode(org.bukkit.GameMode.SPECTATOR);
            broadcastMessage("§c" + p.getName() + " fell into the void and was eliminated!");
            event.setCancelled(true);

            checkWinCondition();
        }
    }

    // ---------- WIN CONDITION ----------

    /**
     * Checks if only one (or zero) players remain, announces winner or no-winner, and ends game.
     */
    private void checkWinCondition() {
        if (alivePlayers.size() == 1) {
            Player winner = alivePlayers.get(0);
            broadcastMessage("§a" + winner.getName() + " is the last player standing! Congratulations!");
            winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            onGameEnd();
        } else if (alivePlayers.isEmpty()) {
            broadcastMessage("§eAll players have been eliminated. No winners this round.");
            onGameEnd();
        }
    }
}
