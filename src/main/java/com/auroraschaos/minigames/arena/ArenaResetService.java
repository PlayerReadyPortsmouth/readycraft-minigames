// ArenaResetService.java
package com.auroraschaos.minigames.arena;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Service for scheduling and executing arena resets.
 */
public class ArenaResetService {
    private final JavaPlugin plugin;
    private final ArenaRegistry registry;

    public ArenaResetService(JavaPlugin plugin, ArenaRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void scheduleReset(Arena arena, long delayTicks) {
        new BukkitRunnable() {
            @Override
            public void run() {
                arena.reset();
            }
        }.runTaskLater(plugin, delayTicks);
    }

    public void resetAll() {
        registry.getAll().values().forEach(Arena::reset);
    }
}
