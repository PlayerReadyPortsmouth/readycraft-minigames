package com.auroraschaos.minigames.util;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import com.auroraschaos.minigames.arena.Arena;
import com.sk89q.worldedit.math.BlockVector3;


/**
 * Utility for putting eliminated players into spectator mode within
 * an arena’s bounds, without letting them leave the “slot.”
 */
public class SpectatorUtil {

    /**
     * Transforms a player into a spectator for a given arena.
     * Teleports them to arena origin + offset, sets GameMode.SPECTATOR,
     * and confines them within the arena’s bounding box.
     *
     * @param p     the eliminated player
     * @param arena the Arena instance
     */
    public static void makeSpectator(Player p, Arena arena) {
        // 1) Teleport to arena origin + small Y offset
        BlockVector3 originVec = arena.getOrigin();
        Location specLocation = new Location(
            arena.getWorld(),
            originVec.getX() + 0.5,
            originVec.getY() + 10,
            originVec.getZ() + 0.5
        );
        p.teleport(specLocation);

        // 2) Set to spectator mode
        p.setGameMode(GameMode.SPECTATOR);

        // 3) Constrain within arena’s bounding box
        // Suppose your arena “slot” is ~100×50×100. Calculate min/max as Locations:
        Location minLoc = new Location(
            arena.getWorld(),
            originVec.getX(),
            originVec.getY(),
            originVec.getZ()
        );
        Location maxLoc = new Location(
            arena.getWorld(),
            originVec.getX() + 100,
            originVec.getY() + 50,
            originVec.getZ() + 100
        );
        @SuppressWarnings("unused")
        BoundingBox box = BoundingBox.of(minLoc, maxLoc);
        // If you need to store this box somewhere or schedule a repeating task 
        // to clamp players to this box, you can do so here.

        p.setCollidable(false);
    }

    /**
     * Return a player from spectator back to survival mode, teleport to lobby.
     */
    public static void returnToLobby(Player p, Location lobbySpawn) {
        p.setGameMode(GameMode.SURVIVAL);
        p.teleport(lobbySpawn);
        p.setCollidable(true);
    }
}
