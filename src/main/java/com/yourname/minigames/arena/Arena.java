package com.yourname.minigames.arena;

import org.bukkit.World;
import com.sk89q.worldedit.math.BlockVector3;

/**
 * Represents a minigame arena.
 * An arena is a defined space within a world where a minigame can take place.
 * It includes information about its name, type, world, origin point for pasting,
 * and the schematic file associated with it.
 */
public class Arena {
    private final String name;
    private final String type;
    private final World world;
    /**
     * The origin point (minimum coordinates) in the world where the schematic should be pasted.
     * This serves as the dynamic paste location for the arena schematic.
     */
 private final BlockVector3 origin;
    /**
     * The name of the schematic file associated with this arena.
     */
    private final String schematic;
    /**
     * Flag indicating whether the arena is currently in use by a game instance.
     */
    private boolean inUse;

    public Arena(String name, String type, World world, BlockVector3 origin, String schematic) {
        this.name = name;
        this.type = type;
        this.world = world;
        this.origin = origin;
        this.schematic = schematic;
        this.inUse = false;
    }

    /**
     * Gets the name of the arena.
     *
     * @return The arena name.
     */
    public String getName() { return name; }

    /**
     * Gets the type of minigame this arena is designed for.
     *
     * @return The arena type (e.g., "TNTRun", "Bedwars").
     */
    public String getType() { return type; }

    /**
     * Gets the world where the arena is located.
     *
     * @return The Bukkit World object.
     */
    public World getWorld() { return world; }

    /**
     * Gets the origin point (minimum coordinates) for pasting the arena schematic.
     *
     * @return The BlockVector3 representing the origin.
     */
    public BlockVector3 getOrigin() { return origin; }

    /**
     * Gets the name of the schematic file used for this arena.
     *
     * @return The schematic file name.
     */
    public String getSchematic() { return schematic; }

    /**
     * Checks if the arena is currently in use by a game.
     *
     * @return True if the arena is in use, false otherwise.
     */
    public boolean isInUse() { return inUse; }
    public void setInUse(boolean inUse) { this.inUse = inUse; }
}
