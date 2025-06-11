// ArenaRegistry.java
package com.auroraschaos.minigames.arena;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for tracking live Arena instances.
 */
public class ArenaRegistry {
    private final Map<String, Arena> arenas = new HashMap<>();

    public void register(Arena arena) {
        arenas.put(arena.getName().toLowerCase(), arena);
    }

    public Optional<Arena> get(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(arenas.get(name.toLowerCase()));
    }

    public Map<String, Arena> getAll() {
        return Collections.unmodifiableMap(arenas);
    }

    public int count() {
        return arenas.size();
    }

    public void unregisterAll() {
        arenas.clear();
    }
}
