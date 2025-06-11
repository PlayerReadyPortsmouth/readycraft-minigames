package com.auroraschaos.minigames.arena;

import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * A simple grid-based SlotAllocator that places arenas in rows and columns.
 */
public class GridSlotAllocator implements SlotAllocator {
    private final Vector origin;
    private final int columns;
    private final int spacingX;
    private final int spacingZ;
    private int count = 0;

    /**
     * Constructs a grid allocator.
     *
     * @param origin   base coordinates for the first slot
     * @param columns  number of columns per row
     * @param spacingX horizontal spacing between arenas
     * @param spacingZ vertical spacing between arenas (in Z axis)
     */
    public GridSlotAllocator(Vector origin, int columns, int spacingX, int spacingZ) {
        this.origin = origin.clone();
        this.columns = Math.max(1, columns);
        this.spacingX = spacingX;
        this.spacingZ = spacingZ;
    }

    /**
     * Calculates the next slot by computing row/column based on invocation count.
     */
    @Override
    public Vector nextSlot(World world) {
        int row = count / columns;
        int col = count % columns;
        count++;

        int x = origin.getBlockX() + col * spacingX;
        int y = origin.getBlockY();
        int z = origin.getBlockZ() + row * spacingZ;

        return new Vector(x, y, z);
    }
}
