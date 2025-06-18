// Arena.java
package com.auroraschaos.minigames.arena;

import org.bukkit.World;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.Map;

/**
 * Represents a minigame arena.
 * An arena is a defined space within a world where a minigame can take place.
 */
public class Arena {
    private final String name;
    private final World world;
    private final BlockVector3 origin;
    private final String schematic;
    private final Map<String, String> flags;
    private final long resetIntervalTicks;
    private boolean inUse;

    /**
     * Constructs a new Arena.
     *
     * @param name               unique identifier
     * @param world              world where the arena is placed
     * @param origin             origin point for pasting schematic
     * @param schematic          schematic filename
     * @param flags              custom flags
     * @param resetIntervalTicks ticks before auto-reset
     */
    public Arena(String name,
                 World world,
                 BlockVector3 origin,
                 String schematic,
                 Map<String, String> flags,
                 long resetIntervalTicks) {
        this.name               = name;
        this.world              = world;
        this.origin             = origin;
        this.schematic          = schematic;
        this.flags              = Map.copyOf(flags);
        this.resetIntervalTicks = resetIntervalTicks;
        this.inUse              = false;
    }

    public String getName()                 { return name; }
    public World getWorld()                 { return world; }
    public BlockVector3 getOrigin()         { return origin; }
    public String getSchematic()            { return schematic; }
    public Map<String, String> getFlags()   { return flags; }
    public long getResetIntervalTicks()     { return resetIntervalTicks; }
    public boolean isInUse()                { return inUse; }
    public void setInUse(boolean inUse)     { this.inUse = inUse; }

    /**
     * Reset the arena by re-pasting the schematic and resetting any state.
     */
    public void reset() {
    }

    /**
     * Clean up any resources (scheduled tasks, regions, etc.) when shutting down.
     */
    public void cleanup() {
    }
}
