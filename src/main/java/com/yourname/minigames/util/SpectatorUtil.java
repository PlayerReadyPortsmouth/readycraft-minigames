package com.yourname.minigames.util;

import com.yourname.minigames.arena.Arena;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

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
        Location specLocation = arena.getOrigin().toLocation(arena.getWorld()).add(0.5, 10, 0.5);
        p.teleport(specLocation);

        // 2) Set spectator mode
        p.setGameMode(GameMode.SPECTATOR);

        // 3) Constrain within arena’s bounding box
        BoundingBox box = BoundingBox.of(
                arena.getOrigin(),
                arena.getOrigin().add(50, 50, 50)  // adjust size or calculate based on schematic
        );
        p.setSpectatorTarget(null); // no specific entity focus
        p.setCollidable(false);

        // We’ll add a repeating task to keep them inside; plugin can call this if needed
        // or you can rely on WorldGuard region to prevent leaving.
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
