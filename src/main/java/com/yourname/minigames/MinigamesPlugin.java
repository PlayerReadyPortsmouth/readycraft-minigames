package com.yourname.minigames;

import org.bukkit.plugin.java.JavaPlugin;

public class MinigamesPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("Minigames Plugin Enabled!");
        // TODO: Initialize managers, commands, and listeners
    }

    @Override
    public void onDisable() {
        getLogger().info("Minigames Plugin Disabled!");
        // TODO: Cleanup
    }
}
