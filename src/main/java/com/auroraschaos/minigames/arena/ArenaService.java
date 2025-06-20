// ArenaService.java
package com.auroraschaos.minigames.arena;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.auroraschaos.minigames.config.ArenaConfig;
import com.auroraschaos.minigames.config.ArenaDefinition;

import java.util.Map;

/**
 * High-level service orchestrating arena lifecycle:
 * definition lookup, slot allocation, schematic loading,
 * registry, and resets.
 */
public class ArenaService {
    private final MinigamesPlugin plugin;
    private final ArenaDefinitionRepository definitionRepo;
    @SuppressWarnings("unused")
    private final SlotAllocator slotAllocator;
    @SuppressWarnings("unused")
    private final SchematicLoader schematicLoader;
    private final ArenaFactory arenaFactory;
    private final ArenaRegistry registry;
    private final ArenaResetService resetService;

    public ArenaService(MinigamesPlugin plugin,
                        ArenaConfig config,
                        SlotAllocator slotAllocator,
                        SchematicLoader schematicLoader,
                        ArenaResetService resetService) {
        this.plugin        = plugin;
        this.definitionRepo= new ArenaDefinitionRepository(config);
        this.slotAllocator = slotAllocator;
        this.schematicLoader = schematicLoader;
        this.arenaFactory  = new ArenaFactory(slotAllocator, schematicLoader);
        this.registry      = new ArenaRegistry();
        this.resetService  = resetService;
    }

    /**
     * Instantiate all arenas defined in config and register them.
     */
    public void initializeAll() {
        plugin.logVerbose("[ArenaService] Starting arena initialization...");
        plugin.logVerbose(String.format(
            "[ArenaService] %d arena definitions found.",
            definitionRepo.getAll().size()
        ));

        for (ArenaDefinition def : definitionRepo.getAll()) {
            plugin.logVerbose(String.format(
                "[ArenaService] Preparing arena '%s' (world=%s, schematic=%s)",
                def.getKey(), def.getWorldName(), def.getSchematic()
            ));

            try {
                Arena arena = arenaFactory.create(plugin, def);
                registry.register(arena);
                resetService.scheduleReset(arena, def.getResetIntervalTicks());
                plugin.logVerbose(String.format(
                    "[ArenaService] Registered arena '%s' at %s",
                    arena.getName(), arena.getOrigin()
                ));
            } catch (ArenaCreationException ex) {
                plugin.getLogger().log(
                    java.util.logging.Level.WARNING,
                    String.format(
                        "[ArenaService] Failed to create arena '%s'",
                        def.getKey()
                    ),
                    ex
                );
            }
        }

        plugin.logVerbose(String.format(
            "[ArenaService] Initialized %d arenas.",
            registry.count()
        ));
        plugin.logVerbose("[ArenaService] Arena initialization complete.");
    }

    /**  
     * Find a free arena instance matching the given key (type) and mark it in use.  
     * @param key the arena key (case-insensitive)  
     * @return an Arena or null if none free/matching  
     */  
    public Arena createArenaInstance(String key) {  
        for (Arena arena : registry.getAll().values()) {  
            if (arena.getName().equalsIgnoreCase(key) && !arena.isInUse()) {  
                arena.setInUse(true);  
                return arena;  
            }  
        }  
        return null;  
    }

    /**  
     * Immediately reset a single arena (e.g. after a game ends).  
     */  
    public void resetArena(Arena arena) {  
        arena.reset();  
        // Optionally re-schedule auto-reset if desired:  
        resetService.scheduleReset(arena, arena.getResetIntervalTicks());  
    }

    public Arena getArena(String name) {
        return registry.get(name).orElse(null);
    }

    public Map<String, Arena> getAllArenas() {
        return registry.getAll();
    }

    /**  
     * Clean up all arenas and clear registry (on plugin shutdown).  
     */  
    public void shutdownAll() {  
        registry.getAll().values().forEach(Arena::cleanup);  
        registry.unregisterAll();  
        plugin.getLogger().info("[ArenaService] All arenas cleaned up and registry cleared.");  
    }
}
