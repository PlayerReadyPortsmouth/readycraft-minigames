package com.yourname.minigames.arena;

import com.yourname.minigames.MinigamesPlugin;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World as WEWorld;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class ArenaManager {
    private final MinigamesPlugin plugin;
    private final Map<String, Arena> arenas = new HashMap<>();
    private final Map<String, Map<String, String>> minigameFlags = new HashMap<>();
    private final YamlConfiguration config;

    public ArenaManager(MinigamesPlugin plugin) {
        this.plugin = plugin;
        File arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        if (!arenasFile.exists()) {
            plugin.saveResource("arenas.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(arenasFile);
        loadFlags();
    }

    private void loadFlags() {
        plugin.getLogger().info("Loading arena flags...");
        for (String key : config.getConfigurationSection("arenas").getKeys(false)) {
            Map<String, String> flags = new HashMap<>();
            for (String flag : config.getConfigurationSection("arenas." + key + ".flags").getKeys(false)) {
                String value = config.getString("arenas." + key + ".flags." + flag);
                flags.put(flag.toUpperCase(), value.toUpperCase());
            }
            minigameFlags.put(key.toUpperCase(), flags);
            plugin.getLogger().info("Loaded flags for " + key + ": " + flags);
        }
    }

    public Arena createArenaInstance(String type) {
        String key = type.toLowerCase();
        String schematic = config.getString("arenas." + key + ".schematic");
        if (schematic == null) {
            plugin.getLogger().severe("No schematic defined for minigame type: " + type);
            return null;
        }

        World world = Bukkit.getWorld("minigames_world");
        if (world == null) {
            plugin.getLogger().severe("World 'minigames_world' not loaded!");
            return null;
        }

        // Allocate a dynamic slot
        BlockVector3 origin = findAvailableSlot(world);

        Arena arena = new Arena(UUID.randomUUID().toString(), type, world, origin, schematic);
        pasteArenaSchematic(arena);
        createWorldGuardRegion(arena);

        arenas.put(arena.getName(), arena);
        plugin.getLogger().info("Arena instance created: " + arena.getName());
        return arena;
    }

    private BlockVector3 findAvailableSlot(World world) {
        int spacing = 300; // adjust as needed
        int instanceCount = arenas.size();
        int gridX = instanceCount % 5;
        int gridZ = instanceCount / 5;
        int originX = gridX * spacing;
        int originZ = gridZ * spacing;
        int y = 64;
        return BlockVector3.at(originX, y, originZ);
    }

    private void pasteArenaSchematic(Arena arena) {
        try {
            File schemFile = new File(plugin.getDataFolder(), "schematics/" + arena.getSchematic());
            ClipboardFormat format = ClipboardFormat.findByFile(schemFile);
            if (format == null) {
                plugin.getLogger().warning("Unknown schematic format: " + schemFile.getName());
                return;
            }

            try (ClipboardReader reader = format.getReader(new FileInputStream(schemFile))) {
                Clipboard clipboard = reader.read();
                EditSession editSession = com.fastasyncworldedit.core.FaweAPI.getWorldEdit().newEditSession(BukkitAdapter.adapt(arena.getWorld()));
                Operations.complete(new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(arena.getOrigin())
                        .ignoreAirBlocks(false)
                        .build());
                editSession.flushSession();
                plugin.getLogger().info("Schematic pasted for arena: " + arena.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to paste schematic for arena: " + arena.getName());
            e.printStackTrace();
        }
    }

    private void createWorldGuardRegion(Arena arena) {
        WorldGuardPlugin wg = (WorldGuardPlugin) plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        if (wg == null) {
            plugin.getLogger().warning("WorldGuard not found. Skipping region creation.");
            return;
        }

        RegionManager regionManager = wg.getRegionContainer().get(BukkitAdapter.adapt(arena.getWorld()));
        if (regionManager == null) {
            plugin.getLogger().warning("Could not get RegionManager for world: " + arena.getWorld().getName());
            return;
        }

        // For dynamic region, assume a fixed schematic size or calculate from clipboard if needed
        int regionSize = 100; // adjust this depending on schematic size
        BlockVector3 min = arena.getOrigin();
        BlockVector3 max = min.add(regionSize, 50, regionSize);

        String regionId = "arena_" + arena.getName().toLowerCase();
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionId, min, max);

        // Apply flags from arenas.yml
        Map<String, String> flags = minigameFlags.get(arena.getType().toUpperCase());
        if (flags != null) {
            for (Map.Entry<String, String> entry : flags.entrySet()) {
                try {
                    Flag<?> wgFlag = Flags.fuzzyMatchFlag(regionManager.getFlagRegistry(), entry.getKey());
                    if (wgFlag != null && wgFlag instanceof com.sk89q.worldguard.protection.flags.StateFlag) {
                        com.sk89q.worldguard.protection.flags.StateFlag stateFlag = (com.sk89q.worldguard.protection.flags.StateFlag) wgFlag;
                        com.sk89q.worldguard.protection.flags.StateFlag.State state = com.sk89q.worldguard.protection.flags.StateFlag.State.valueOf(entry.getValue());
                        region.setFlag(stateFlag, state);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid flag: " + entry.getKey() + " or value: " + entry.getValue());
                }
            }
        }

        regionManager.addRegion(region);
        plugin.getLogger().info("WorldGuard region '" + regionId + "' created for arena: " + arena.getName());
    }

    public void resetArena(Arena arena) {
        pasteArenaSchematic(arena);
    }

    public Collection<Arena> getAllArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }
}
