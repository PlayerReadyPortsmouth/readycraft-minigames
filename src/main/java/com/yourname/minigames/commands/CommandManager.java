package com.yourname.minigames.commands;

import com.yourname.minigames.MinigamesPlugin;
import org.bukkit.command.PluginCommand;

/**
 * CommandManager registers all command executors and tab completers.
 * It simply hooks each top‐level command to its dedicated handler class.
 */
public class CommandManager {

    private final MinigamesPlugin plugin;

    public CommandManager(MinigamesPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Call this during onEnable() to register commands.
     */
    public void registerAll() {
        // 1) /minigames command
        PluginCommand minigamesCmd = plugin.getCommand("minigames");
        if (minigamesCmd != null) {
            MinigamesCommand executor = new MinigamesCommand(plugin);
            minigamesCmd.setExecutor(executor);
            minigamesCmd.setTabCompleter(executor);
        }

        // 2) /mgui command
        PluginCommand mguiCmd = plugin.getCommand("mgui");
        if (mguiCmd != null) {
            MGUICommand guiExecutor = new MGUICommand(plugin);
            mguiCmd.setExecutor(guiExecutor);
            // No tab completion needed for /mgui
        }
    }
}