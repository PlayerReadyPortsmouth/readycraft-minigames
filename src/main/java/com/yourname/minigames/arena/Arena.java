package com.yourname.minigames.arena;

import org.bukkit.World;
import com.sk89q.worldedit.math.BlockVector3;

public class Arena {
    private final String name;
    private final String type;
    private final World world;
    private final BlockVector3 origin;  // Dynamic paste location
    private final String schematic;
    private boolean inUse;

    public Arena(String name, String type, World world, BlockVector3 origin, String schematic) {
        this.name = name;
        this.type = type;
        this.world = world;
        this.origin = origin;
        this.schematic = schematic;
        this.inUse = false;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public World getWorld() { return world; }
    public BlockVector3 getOrigin() { return origin; }
    public String getSchematic() { return schematic; }
    public boolean isInUse() { return inUse; }
    public void setInUse(boolean inUse) { this.inUse = inUse; }
}
