package com.auroraschaos.minigames.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Parses and holds settings under the "party" section of config.yml, for example:
 *
 * party:
 *   maxSize: 4
 *   inviteTimeoutSeconds: 30
 *   allowCrossWorld: false
 */
public class PartyConfig {
    private final int maxSize;
    private final long inviteTimeoutSeconds;
    private final boolean allowCrossWorld;

    private PartyConfig(int maxSize, long inviteTimeoutSeconds, boolean allowCrossWorld) {
        this.maxSize             = maxSize;
        this.inviteTimeoutSeconds = inviteTimeoutSeconds;
        this.allowCrossWorld     = allowCrossWorld;
    }

    public static PartyConfig from(ConfigurationSection section) throws ConfigurationException {
        if (section == null) {
            throw new ConfigurationException("'party' section is missing");
        }

        int maxSize = section.getInt("maxSize", 4);
        if (maxSize < 1) {
            throw new ConfigurationException("'party.maxSize' must be at least 1 (found " + maxSize + ")");
        }

        long timeout = section.getLong("inviteTimeoutSeconds", 30L);
        if (timeout < 1) {
            throw new ConfigurationException("'party.inviteTimeoutSeconds' must be at least 1 (found " + timeout + ")");
        }

        boolean crossWorld = section.getBoolean("allowCrossWorld", true);

        return new PartyConfig(maxSize, timeout, crossWorld);
    }

    /** Maximum number of players in a party. */
    public int getMaxSize() {
        return maxSize;
    }

    /** Seconds before an unanswered party invite expires. */
    public long getInviteTimeoutSeconds() {
        return inviteTimeoutSeconds;
    }

    /** Whether party members may join from different worlds. */
    public boolean isAllowCrossWorld() {
        return allowCrossWorld;
    }
}