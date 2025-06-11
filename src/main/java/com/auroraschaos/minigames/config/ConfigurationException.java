package com.auroraschaos.minigames.config;

/**
 * Exception thrown when a configuration parsing or validation error occurs.
 */
public class ConfigurationException extends Exception {
    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}