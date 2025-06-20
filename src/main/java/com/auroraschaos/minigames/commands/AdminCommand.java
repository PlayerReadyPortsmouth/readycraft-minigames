package com.auroraschaos.minigames.commands;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.auroraschaos.minigames.config.ConfigManager;
import com.auroraschaos.minigames.game.GameManager;
import com.auroraschaos.minigames.game.GameMode;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Handles admin level commands under /minigamesadmin.
 */
public class AdminCommand implements CommandExecutor, TabCompleter {

    private final MinigamesPlugin plugin;
    private final GameManager gameManager;
    private final ConfigManager configManager;

    public AdminCommand(MinigamesPlugin plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("minigames.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "forcestart":
                handleForceStart(sender, args);
                break;
            case "verbose":
                handleVerbose(sender, args);
                break;
            case "dumpconfig":
                handleDumpConfig(sender);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " for help.");
                break;
        }
        return true;
    }

    private void handleForceStart(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /minigamesadmin forcestart <type> <mode>");
            return;
        }

        String type = args[1].toUpperCase();
        GameMode mode;
        try {
            mode = GameMode.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid mode: " + args[2]);
            return;
        }

        boolean success = gameManager.forceStart(type, mode);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Forced start of " + type + " [" + mode + "]");
        } else {
            sender.sendMessage(ChatColor.RED + "Queue for " + type + " [" + mode + "] is empty.");
        }
    }

    private void handleVerbose(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Verbose logging is currently "
                    + (configManager.isVerboseLogging() ? "ON" : "OFF") + ".");
            sender.sendMessage(ChatColor.YELLOW + "Use /minigamesadmin verbose <on|off>");
            return;
        }

        boolean enable = args[1].equalsIgnoreCase("on");
        configManager.setVerboseLogging(enable);
        sender.sendMessage(ChatColor.GREEN + "Verbose logging " + (enable ? "enabled" : "disabled") + ".");
    }

    private void handleDumpConfig(CommandSender sender) {
        plugin.getLogger().info("--- Config Dump ---\n" + plugin.getConfig().saveToString());
        sender.sendMessage(ChatColor.GREEN + "Configuration dumped to console.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "------ Minigames Admin Help ------");
        sender.sendMessage(ChatColor.YELLOW + "/minigamesadmin forcestart <type> <mode>" + ChatColor.WHITE + " - Force start a queued game");
        sender.sendMessage(ChatColor.YELLOW + "/minigamesadmin verbose <on|off>" + ChatColor.WHITE + " - Toggle verbose logging");
        sender.sendMessage(ChatColor.YELLOW + "/minigamesadmin dumpconfig" + ChatColor.WHITE + " - Dump config to console");
        sender.sendMessage(ChatColor.AQUA + "---------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("minigames.admin")) return Collections.emptyList();

        if (args.length == 1) {
            return partialMatches(args[0], "forcestart", "verbose", "dumpconfig");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("forcestart")) {
            return partialMatches(args[1], plugin.getConfig().getConfigurationSection("minigames").getKeys(false).toArray(new String[0]));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("forcestart")) {
            String[] modes = Arrays.stream(GameMode.values()).map(Enum::name).toArray(String[]::new);
            return partialMatches(args[2], modes);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("verbose")) {
            return partialMatches(args[1], "on", "off");
        }
        return Collections.emptyList();
    }

    private List<String> partialMatches(String arg, String... candidates) {
        List<String> result = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase().startsWith(arg.toLowerCase())) {
                result.add(candidate);
            }
        }
        return result;
    }
}
