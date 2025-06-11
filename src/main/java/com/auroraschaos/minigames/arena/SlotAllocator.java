// SlotAllocator.java
package com.auroraschaos.minigames.arena;

import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * Strategy interface for allocating schematic placement slots in a world.
 */
public interface SlotAllocator {
    Vector nextSlot(World world);
}
