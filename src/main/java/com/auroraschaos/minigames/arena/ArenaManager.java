package com.auroraschaos.minigames.arena;

import com.auroraschaos.minigames.MinigamesPlugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.bukkit.BukkitAdapter;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * ArenaManager dynamically creates and resets “arena slots” for each minigame instance.
 * It pastes schematics via WorldEdit and protects each area via WorldGuard flags.
 */
public class ArenaManager {
    private final MinigamesPlugin plugin;
    private final YamlConfiguration config;
    private final Map<String, Map<String, String>> minigameFlags = new HashMap<>();
    private final Map<String, Arena> arenas = new HashMap<>();

    public ArenaManager(MinigamesPlugin plugin) {
        this.plugin = plugin;

        // Load or save default arenas.yml
        File file = new File(plugin.getDataFolder(), "arenas.yml");
        if (!file.exists()) plugin.saveResource("arenas.yml", false);
        this.config = YamlConfiguration.loadConfiguration(file);

        loadFlags();
    }

    private void loadFlags() {
        plugin.getLogger().info("[ArenaManager] Loading arena flags...");
        ConfigurationSection arenasSec = config.getConfigurationSection("arenas");
        if (arenasSec == null) return;

        for (String key : arenasSec.getKeys(false)) {
            ConfigurationSection flagsSec = config.getConfigurationSection("arenas." + key + ".flags");
            Map<String, String> flagsMap = new HashMap<>();
            if (flagsSec != null) {
                for (String flagName : flagsSec.getKeys(false)) {
                    String val = config.getString("arenas." + key + ".flags." + flagName, "")
                                       .toUpperCase();
                    flagsMap.put(flagName.toUpperCase(), val);
                }
            }
            minigameFlags.put(key.toLowerCase(), flagsMap);
            plugin.getLogger().info("  Loaded flags for [" + key + "]: " + flagsMap);
        }
    }

    /**
     * Creates (and pastes) a new arena instance for the given minigame type.
     */
    public Arena createArenaInstance(String type) {
        String key = type.toLowerCase();
        String schematic = config.getString("arenas." + key + ".schematic");
        if (schematic == null) {
            plugin.getLogger().severe("[ArenaManager] No schematic defined for " + type);
            return null;
        }

        World world = Bukkit.getWorld("minigames_world");
        if (world == null) {
            plugin.getLogger().severe("[ArenaManager] World 'minigames_world' not found!");
            return null;
        }

        // Determine placement slot
        BlockVector3 origin = findAvailableSlot(world);

        // Gather flags & reset interval
        Map<String, String> flags = minigameFlags.getOrDefault(key, Collections.emptyMap());
        long intervalSec = config.getLong("arenas." + key + ".resetIntervalSeconds", 60L);
        long intervalTicks = intervalSec * 20L;

        // Name each instance uniquely: "<type>_<index>"
        String name = key + "_" + (arenas.size() + 1);
        Arena arena = new Arena(name, world, origin, schematic, flags, intervalTicks);
        arenas.put(name, arena);

        pasteArenaSchematic(arena);
        createWorldGuardRegion(arena);

        plugin.getLogger().info("[ArenaManager] Created arena " + name +
            " at " + origin.getX() + "," + origin.getY() + "," + origin.getZ());
        return arena;
    }

    private BlockVector3 findAvailableSlot(World world) {
        int spacing = 300;
        int idx = arenas.size();
        int x = (idx % 5) * spacing;
        int z = (idx / 5) * spacing;
        return BlockVector3.at(x, 64, z);
    }

    private void pasteArenaSchematic(Arena arena) {
        try {
            File schemFile = new File(plugin.getDataFolder(), "schematics/" + arena.getSchematic());
            if (!schemFile.exists()) {
                plugin.getLogger().warning("[ArenaManager] Missing schematic: " + schemFile);
                return;
            }

            ClipboardFormat fmt = ClipboardFormats.findByFile(schemFile);
            if (fmt == null) {
                plugin.getLogger().warning("[ArenaManager] Unknown schematic format: " + schemFile);
                return;
            }

            Clipboard clipboard;
            try (ClipboardReader reader = fmt.getReader(new FileInputStream(schemFile))) {
                clipboard = reader.read();
            }

            BlockVector3 pasteOrigin = arena.getOrigin();
            ClipboardHolder holder = new ClipboardHolder(clipboard);

            try (EditSession session = WorldEdit.getInstance()
                    .newEditSessionBuilder()
                    .world(BukkitAdapter.adapt(arena.getWorld()))
                    .maxBlocks(Integer.MAX_VALUE)
                    .build()) {

                Operation op = holder
                    .createPaste(session)
                    .to(pasteOrigin)
                    .ignoreAirBlocks(false)
                    .build();
                Operations.complete(op);
            }

            plugin.getLogger().info("[ArenaManager] Pasted schematic for " + arena.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("[ArenaManager] Error pasting schematic for " + arena.getName());
            e.printStackTrace();
        }
    }

    private void createWorldGuardRegion(Arena arena) {
        var container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        var mgr = container.get(BukkitAdapter.adapt(arena.getWorld()));
        if (mgr == null) {
            plugin.getLogger().warning("[ArenaManager] No WorldGuard manager for world.");
            return;
        }

        BlockVector3 min = arena.getOrigin();
        BlockVector3 max = min.add(100, 50, 100);
        String regionId = "arena_" + arena.getName().toLowerCase();
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionId, min, max);

        // Use the type (the part before the underscore) for flags
        String typeKey = arena.getName().split("_")[0];
        Map<String, String> flags = minigameFlags.getOrDefault(typeKey, Collections.emptyMap());
        for (var ent : flags.entrySet()) {
            Flag<?> wgFlag = WorldGuard.getInstance()
                                      .getFlagRegistry()
                                      .get(ent.getKey().toLowerCase());
            if (wgFlag instanceof StateFlag sf) {
                try {
                    StateFlag.State state = StateFlag.State.valueOf(ent.getValue());
                    region.setFlag(sf, state);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger()
                          .warning("[ArenaManager] Invalid value for flag " 
                                    + ent.getKey() + ": " + ent.getValue());
                }
            } else {
                plugin.getLogger()
                      .warning("[ArenaManager] Flag not found or not a StateFlag: " 
                                + ent.getKey());
            }
        }

        mgr.addRegion(region);
        plugin.getLogger().info("[ArenaManager] Created WG region " + regionId);
    }

    /**
     * Reset an arena by re-pasting its schematic.
     */
    public void resetArena(Arena arena) {
        pasteArenaSchematic(arena);
        plugin.getLogger().info("[ArenaManager] Reset arena: " + arena.getName());
    }

    /**
     * @return all currently instantiated arenas.
     */
    public Collection<Arena> getAllArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }
}
