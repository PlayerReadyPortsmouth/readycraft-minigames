package com.yourname.minigames.commands;

import com.yourname.minigames.MinigamesPlugin;
import com.yourname.minigames.gui.GUIManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

/**
 * Handles the /mgui command, which simply opens the Minigames GUI.
 */
public class MGUICommand implements CommandExecutor {

    private final MinigamesPlugin plugin;
    private final GUIManager guiManager;

    public MGUICommand(MinigamesPlugin plugin) {
        this.plugin = plugin;
        this.guiManager = plugin.getGUIManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can open the Minigames GUI.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("minigames.play")) {
            player.sendMessage(ChatColor.RED + "You lack permission to open the Minigames GUI.");
            return true;
        }

        guiManager.openMainMenu(player);
        return true;
    }
}
