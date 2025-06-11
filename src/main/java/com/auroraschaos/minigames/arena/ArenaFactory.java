// ArenaFactory.java
package com.auroraschaos.minigames.arena;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.auroraschaos.minigames.config.ArenaDefinition;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * Factory for creating Arena instances from definitions.
 */
public class ArenaFactory {
    private final SlotAllocator slotAllocator;
    private final SchematicLoader schematicLoader;

    public ArenaFactory(SlotAllocator slotAllocator, SchematicLoader schematicLoader) {
        this.slotAllocator = slotAllocator;
        this.schematicLoader = schematicLoader;
    }

    public Arena create(MinigamesPlugin plugin, ArenaDefinition def) throws ArenaCreationException {
        World world = plugin.getServer().getWorld(def.getWorldName());
        if (world == null) {
            throw new ArenaCreationException(
                "World '" + def.getWorldName() + "' not found for arena '" + def.getKey() + "'."
            );
        }
        Vector originVec = slotAllocator.nextSlot(world);
        BlockVector3 origin = BlockVector3.at(
            originVec.getBlockX(), originVec.getBlockY(), originVec.getBlockZ()
        );

        try {
            schematicLoader.loadSchematic(def.getSchematic(), world, originVec);
        } catch (Exception e) {
            throw new ArenaCreationException(
                "Failed to load schematic '" + def.getSchematic() + "'.", e
            );
        }

        return new Arena(
            def.getKey(),
            world,
            origin,
            def.getSchematic(),
            def.getFlags(),
            def.getResetIntervalTicks()
        );
    }
}
