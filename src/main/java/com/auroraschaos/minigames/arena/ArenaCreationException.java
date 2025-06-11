package com.auroraschaos.minigames.arena;

/**
 * Exception thrown when an arena cannot be created, typically due to schematic loading or world issues.
 */
public class ArenaCreationException extends Exception {
    public ArenaCreationException(String message) {
        super(message);
    }

    public ArenaCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}