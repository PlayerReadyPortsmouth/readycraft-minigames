package com.auroraschaos.minigames.arena;

import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * Service interface for loading and pasting schematics into a world.
 */
public interface SchematicLoader {
    /**
     * Load and paste the given schematic at the specified origin in the target world.
     * @param schematicName filename (or key) of the schematic to load
     * @param world         the Bukkit World instance
     * @param origin        the origin Vector where the schematic is pasted
     * @throws ArenaCreationException if loading or pasting fails
     */
    void loadSchematic(String schematicName, World world, Vector origin) throws ArenaCreationException;
}