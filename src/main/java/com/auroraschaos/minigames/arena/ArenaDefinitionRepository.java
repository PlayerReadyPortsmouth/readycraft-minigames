// ArenaDefinitionRepository.java
package com.auroraschaos.minigames.arena;

import com.auroraschaos.minigames.config.ArenaConfig;
import com.auroraschaos.minigames.config.ArenaDefinition;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository for retrieving arena definitions parsed from configuration.
 */
public class ArenaDefinitionRepository {
    private final Map<String, ArenaDefinition> definitions;

    public ArenaDefinitionRepository(ArenaConfig config) {
        this.definitions = config.getArenas().entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                e -> e.getKey().toLowerCase(),
                Map.Entry::getValue
            ));
    }

    public Optional<ArenaDefinition> get(String key) {
        if (key == null) return Optional.empty();
        return Optional.ofNullable(definitions.get(key.toLowerCase()));
    }

    public Collection<ArenaDefinition> getAll() {
        return definitions.values();
    }

    public boolean exists(String key) {
        return key != null && definitions.containsKey(key.toLowerCase());
    }
}
