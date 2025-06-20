package com.auroraschaos.minigames.commands;

import org.bukkit.command.PluginCommand;

import com.auroraschaos.minigames.MinigamesPlugin;

/**
 * CommandManager registers all command executors and tab completers.
 * It simply hooks each top‐level command to its dedicated handler class.
 */
public class CommandManager {

    private final MinigamesPlugin plugin;
    
    /**
     * Constructs a new CommandManager.
     *
     * @param plugin The main plugin instance.
     */
    public CommandManager(MinigamesPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers all command executors and tab completers for the plugin's commands.
     * This method should be called during the plugin's {@code onEnable()} phase.
     * It hooks each top-level command defined in the plugin.yml to its
     * corresponding command handler class.
     */
    public void registerAll() {
        // 1) /minigames command
        PluginCommand minigamesCmd = plugin.getCommand("minigames");
        if (minigamesCmd != null) {
            MinigamesCommand executor = new MinigamesCommand(plugin);
            minigamesCmd.setExecutor(executor);
            minigamesCmd.setTabCompleter(executor);
        } else {
            plugin.getLogger().warning("Command 'minigames' not found in plugin.yml");
        }

        // 2) /mgui command
        PluginCommand mguiCmd = plugin.getCommand("mgui");
        if (mguiCmd != null) {
            MGUICommand guiExecutor = new MGUICommand(plugin);
            mguiCmd.setExecutor(guiExecutor);
            // No tab completion needed for /mgui
        } else {
            plugin.getLogger().warning("Command 'mgui' not found in plugin.yml");
        }

        PluginCommand partyCmd = plugin.getCommand("party");
        if (partyCmd != null) {
            PartyCommand partyExecutor = new PartyCommand(plugin);
            partyCmd.setExecutor(partyExecutor);
        } else {
            plugin.getLogger().warning("Command 'party' not found in plugin.yml");
        }

        // 4) /minigamesadmin command
        PluginCommand adminCmd = plugin.getCommand("minigamesadmin");
        if (adminCmd != null) {
            AdminCommand adminExecutor = new AdminCommand(plugin);
            adminCmd.setExecutor(adminExecutor);
            adminCmd.setTabCompleter(adminExecutor);
        } else {
            plugin.getLogger().warning("Command 'minigamesadmin' not found in plugin.yml");
        }
    }
}