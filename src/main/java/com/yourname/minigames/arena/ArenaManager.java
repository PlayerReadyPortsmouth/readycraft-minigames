package com.yourname.minigames.arena;

import com.yourname.minigames.MinigamesPlugin;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;       // To convert a Bukkit World → WE world

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;

import org.bukkit.Bukkit;
import org.bukkit.World;                                  // Bukkit’s World
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * ArenaManager dynamically creates and resets “arena slots” for each minigame instance.
 * It pastes schematics via FAWE/WorldEdit and protects each area via WorldGuard, loading flags from config.
 */
public class ArenaManager {

    private final MinigamesPlugin plugin;
    private final Map<String, Arena> arenas = new HashMap<>();
    private final Map<String, Map<String, String>> minigameFlags = new HashMap<>();
    private final YamlConfiguration config;

    public ArenaManager(MinigamesPlugin plugin) {
        this.plugin = plugin;

        // Load arenas.yml (if missing, save resource)
        File arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        if (!arenasFile.exists()) {
            plugin.saveResource("arenas.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(arenasFile);

        // Load flags (per-minigame)
        loadFlags();
    }

    /**
     * Reads “flags” under each minigame key in arenas.yml, e.g.:
     * arenas:
     *   tnt_run:
     *     schematic: "tnt_run.schem"
     *     flags:
     *       BLOCK_BREAK: DENY
     *       BLOCK_PLACE: DENY
     */
    private void loadFlags() {
        plugin.getLogger().info("Loading arena flags from arenas.yml...");
        if (!config.isConfigurationSection("arenas")) return;

        for (String key : config.getConfigurationSection("arenas").getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection("arenas." + key + ".flags");
            if (section == null) continue;

            Map<String, String> flags = new HashMap<>();
            for (String flagName : section.getKeys(false)) {
                String value = config.getString("arenas." + key + ".flags." + flagName, "").toUpperCase();
                flags.put(flagName.toUpperCase(), value);
            }
            minigameFlags.put(key.toUpperCase(), flags);
            plugin.getLogger().info("  Loaded flags for [" + key + "]: " + flags);
        }
    }

    /**
     * Creates a new Arena instance for the given minigame type.
     *  - Finds a free “slot” (grid) in the single world.
     *  - Pastes the schematic.
     *  - Creates a WorldGuard region with flags from config.
     */
    public Arena createArenaInstance(String type) {
        String key = type.toLowerCase();
        String schematic = config.getString("arenas." + key + ".schematic");
        if (schematic == null) {
            plugin.getLogger().severe("No schematic defined for minigame: " + type);
            return null;
        }

        // All arenas live in a single world named “minigames_world”
        World bukkitWorld = Bukkit.getWorld("minigames_world");
        if (bukkitWorld == null) {
            plugin.getLogger().severe("World 'minigames_world' not found!");
            return null;
        }

        // Determine a free “slot” origin (in WE coordinates)
        BlockVector3 origin = findAvailableSlot(bukkitWorld);

        // Create & store the Arena instance
        Arena arena = new Arena(UUID.randomUUID().toString(), type, bukkitWorld, origin, schematic);
        arenas.put(arena.getName(), arena);

        // Paste the schematic into the slot
        pasteArenaSchematic(arena);

        // Create a protected region via WorldGuard
        createWorldGuardRegion(arena);

        plugin.getLogger().info("Arena instance [" + arena.getName() + "] created at " +
                origin.getX() + ", " + origin.getY() + ", " + origin.getZ());
        return arena;
    }

    /**
     * Grid-based slot allocation:
     * Each existing arena occupies one grid cell spaced by 300 blocks.
     * This simply picks the next free grid coordinate.
     */
    private BlockVector3 findAvailableSlot(World world) {
        int spacing = 300;
        int instanceCount = arenas.size();
        int gridX = instanceCount % 5;
        int gridZ = instanceCount / 5;
        int originX = gridX * spacing;
        int originZ = gridZ * spacing;
        int y = 64;
        return BlockVector3.at(originX, y, originZ);
    }

    /**
     * Uses WorldEdit (with FAWE under the hood) to paste the given arena’s schematic at its origin.
     */
    private void pasteArenaSchematic(Arena arena) {
        try {
            File schemFile = new File(plugin.getDataFolder(), "schematics/" + arena.getSchematic());
            if (!schemFile.exists()) {
                plugin.getLogger().warning("Schematic not found: " + schemFile.getAbsolutePath());
                return;
            }

            // 1) Detect the correct schematic format:
            ClipboardFormat format = ClipboardFormats.findByFile(schemFile);
            if (format == null) {
                plugin.getLogger().warning("Unknown schematic format: " + schemFile.getName());
                return;
            }

            // 2) Read the schematic into a Clipboard:
            try (ClipboardReader reader = format.getReader(new FileInputStream(schemFile))) {
                Clipboard clipboard = reader.read();

                // 3) Convert Bukkit’s World → WorldEdit’s World:
                com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(arena.getWorld());

                // 4) Build a WorldEdit EditSession (wired to EventBus) with no limit:
                EditSession editSession = WorldEdit
                    .getInstance()
                    .newEditSessionBuilder()
                    .world(weWorld)
                    .build();

                // 5) Paste the clipboard at the arena’s origin:
                Operations.complete(
                    new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(arena.getOrigin())
                        .ignoreAirBlocks(false)
                        .build()
                );

                // 6) Flush so that changes apply:
                editSession.flushSession();
                plugin.getLogger().info("Pasted schematic for arena: " + arena.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to paste schematic for arena: " + arena.getName());
            e.printStackTrace();
        }
    }

    /**
     * Creates a WorldGuard cuboid region around the arena’s pasted area, applying flags
     * loaded from arenas.yml. We assume a fixed region size large enough to cover
     * the schematic (e.g. 100×50×100 blocks) or you can calculate bounds from the clipboard.
     */
    private void createWorldGuardRegion(Arena arena) {
        RegionContainer wg = WorldGuard.getInstance().getPlatform().getRegionContainer();

        if (wg == null) {
            plugin.getLogger().warning("WorldGuard not found; skipping region creation.");
            return;
        }

        RegionManager regionManager = wg.get(BukkitAdapter.adapt(arena.getWorld()));
        if (regionManager == null) {
            plugin.getLogger().warning("Unable to get RegionManager for world: " + arena.getWorld().getName());
            return;
        }

        // Define region bounds around origin:
        BlockVector3 min = arena.getOrigin();
        BlockVector3 max = arena.getOrigin().add(100, 50, 100);

        String regionId = "arena_" + arena.getName().toLowerCase();
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionId, min, max);

        // Apply flags from config (if any)
        Map<String, String> flags = minigameFlags
                .getOrDefault(arena.getType().toUpperCase(), Collections.emptyMap());
        for (Map.Entry<String, String> entry : flags.entrySet()) {
            String flagName  = entry.getKey().toLowerCase();
            String flagValue = entry.getValue().toUpperCase();

            // Look up the flag in the global registry
            Flag<?> wgFlag = WorldGuard.getInstance()
                                   .getFlagRegistry().get(flagName);

            if (wgFlag instanceof StateFlag) {
                StateFlag sf = (StateFlag) wgFlag;
                try {
                    StateFlag.State state = StateFlag.State.valueOf(flagValue);
                    region.setFlag(sf, state);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Invalid value '" + flagValue
                        + "' for flag " + flagName);
                }
            } else {
                plugin.getLogger().warning("Flag not found or not a StateFlag: " + flagName);
            }
        }

        regionManager.addRegion(region);
        plugin.getLogger().info("WorldGuard region '" + regionId + "' created with flags: " + flags);
    }

    /**
     * Resets an arena by re-pasting its schematic. Any WorldGuard region remains intact.
     */
    public void resetArena(Arena arena) {
        pasteArenaSchematic(arena);
        plugin.getLogger().info("Arena reset: " + arena.getName());
    }

    /**
     * Returns an unmodifiable collection of all active arenas.
     */
    public Collection<Arena> getAllArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }
}
