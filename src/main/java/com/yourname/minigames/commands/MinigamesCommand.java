package com.yourname.minigames.commands;

import com.yourname.minigames.MinigamesPlugin;
import com.yourname.minigames.game.GameManager;
import com.yourname.minigames.game.GameMode;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Handles the /minigames command and its subcommands:
 *   /minigames join <gameType> <mode>
 *   /minigames leave
 *   /minigames stats [player]
 */
public class MinigamesCommand implements CommandExecutor, TabCompleter {

    private final MinigamesPlugin plugin;
    private final GameManager gameManager;

    public MinigamesCommand(MinigamesPlugin plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only players can execute these subcommands
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can run this command.");
            return true;
        }

        Player player = (Player) sender;

        // Check permission
        if (!player.hasPermission("minigames.play")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        // No arguments: show help
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "join":
                handleJoin(player, args);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "stats":
                handleStats(player, args);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /minigames for help.");
                break;
        }

        return true;
    }

    // --------------------------------------------------------------------
    // Subcommand: /minigames join <gameType> <mode>
    // --------------------------------------------------------------------
    private void handleJoin(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /minigames join <gameType> <mode>");
            return;
        }

        String gameType = args[1].toUpperCase();
        String modeStr  = args[2].toUpperCase();

        // Validate GameMode
        GameMode mode;
        try {
            mode = GameMode.valueOf(modeStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid mode: " + modeStr);
            player.sendMessage(ChatColor.YELLOW + "Available modes: " + Arrays.toString(GameMode.values()));
            return;
        }

        // Prevent double-queue / double-join
        if (gameManager.isPlayerInGameOrQueue(player)) {
            player.sendMessage(ChatColor.RED + "You are already in a queue or in a game.");
            return;
        }

        // Enqueue the player
        gameManager.enqueue(gameType, mode, Collections.singletonList(player));
        player.sendMessage(ChatColor.GREEN + "You have been queued for " + gameType + " [" + mode + "].");
    }

    // --------------------------------------------------------------------
    // Subcommand: /minigames leave
    // --------------------------------------------------------------------
    private void handleLeave(Player player) {
        // TODO: Implement leaving logic (remove from queue or allow leaving a game)
        player.sendMessage(ChatColor.YELLOW + "Leaving queue/game is not implemented yet.");
    }

    // --------------------------------------------------------------------
    // Subcommand: /minigames stats [player]
    // --------------------------------------------------------------------
    private void handleStats(Player player, String[] args) {
        // TODO: Hook into StatsManager
        if (args.length == 1) {
            player.sendMessage(ChatColor.YELLOW + "Your stats: (not implemented)");
        } else {
            String targetName = args[1];
            player.sendMessage(ChatColor.YELLOW + "Stats for " + targetName + ": (not implemented)");
        }
    }

    // --------------------------------------------------------------------
    // Send help text to player
    // --------------------------------------------------------------------
    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.AQUA + "------ Minigames Help ------");
        player.sendMessage(ChatColor.YELLOW + "/minigames join <gameType> <mode> " + ChatColor.WHITE +
                "- Join a minigame queue");
        player.sendMessage(ChatColor.YELLOW + "/minigames leave " + ChatColor.WHITE +
                "- Leave your current queue/game");
        player.sendMessage(ChatColor.YELLOW + "/minigames stats [player] " + ChatColor.WHITE +
                "- View stats");
        player.sendMessage(ChatColor.AQUA + "----------------------------");
    }

    // --------------------------------------------------------------------
    // Tab Completion for /minigames
    // --------------------------------------------------------------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();

        if (args.length == 1) {
            // Suggest subcommands: join, leave, stats
            return partialMatches(args[0], "join", "leave", "stats");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            // Suggest game types as defined in config (`minigames` section)
            Set<String> keys = plugin.getConfig().getConfigurationSection("minigames").getKeys(false);
            return partialMatches(args[1].toUpperCase(), keys.toArray(new String[0]));
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("join")) {
            // Suggest GameMode values
            String[] modes = Arrays.stream(GameMode.values()).map(Enum::name).toArray(String[]::new);
            return partialMatches(args[2].toUpperCase(), modes);
        }

        return Collections.emptyList();
    }

    // Helper: return strings from candidates that start with 'arg' (case-insensitive)
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
