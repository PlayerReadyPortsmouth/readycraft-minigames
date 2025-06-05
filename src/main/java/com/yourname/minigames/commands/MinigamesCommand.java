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

    /**
     * Constructs a new MinigamesCommand.
     *
     * @param plugin The main plugin instance.
     */
    public MinigamesCommand(MinigamesPlugin plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    /**
     * Executes the given command, returning its success.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return true if the command was successfully executed, otherwise false
     */
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

    /**
     * Handles the "/minigames join <gameType> <mode>" subcommand.
     * Enqueues the player for the specified game type and mode.
     *
     * @param player The player executing the command.
     * @param args   The command arguments. Expecting gameType and mode.
     */
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

    /**
     * Handles the "/minigames leave" subcommand:
     *   • If the player is in a queue, remove them from that queue.
     *   • Otherwise, if the player is in an active game, remove them from that game.
     *   • If neither, inform them they’re not in a queue or a game.
     *
     * @param player The player executing the command.
     */
    private void handleLeave(Player player) {
        String red = ChatColor.RED.toString();
        String yellow = ChatColor.YELLOW.toString();
        String green = ChatColor.GREEN.toString();

        // 1) If in a queue, remove from queue
        if (gameManager.isPlayerInQueue(player)) {
            boolean removed = gameManager.removeFromQueue(player);
            if (removed) {
                player.sendMessage(green + "You have left the queue.");
            } else {
                player.sendMessage(red + "Could not remove you from the queue.");
            }
            return;
        }

        // 2) Else, if in an active game, remove from the game
        if (gameManager.isPlayerInActiveGame(player)) {
            boolean removed = gameManager.removeFromActiveGame(player);
            if (removed) {
                player.sendMessage(green + "You have left the current game.");
            } else {
                player.sendMessage(red + "Could not remove you from the game.");
            }
            return;
        }

        // 3) Otherwise, not in queue or game
        player.sendMessage(red + "You are not currently in a queue or a game.");
    }

    /**
     * Handles the "/minigames stats [player]" subcommand.
     * Currently sends a message indicating that stats are not yet implemented.
     *
     * @param player The player executing the command.
     * @param args   The command arguments. Optionally includes a target player name.
     */
    private void handleStats(Player player, String[] args) {
        if (args.length == 1) {
            player.sendMessage(ChatColor.YELLOW + "Your stats: (not implemented)");
        } else {
            String targetName = args[1];
            player.sendMessage(ChatColor.YELLOW + "Stats for " + targetName + ": (not implemented)");
        }
    }

    /**
     * Sends the help text for the /minigames command to the specified player.
     *
     * @param player The player to send the help message to.
     */
    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.AQUA + "------ Minigames Help ------");
        player.sendMessage(ChatColor.YELLOW + "/minigames join <gameType> <mode> " + ChatColor.WHITE +
                "- Join a minigame queue");
        player.sendMessage(ChatColor.YELLOW + "/minigames leave " + ChatColor.WHITE +
                "- Leave your current queue or game");
        player.sendMessage(ChatColor.YELLOW + "/minigames stats [player] " + ChatColor.WHITE +
                "- View stats");
        player.sendMessage(ChatColor.AQUA + "----------------------------");
    }

    /**
     * Requests a list of possible completions for a command argument.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param alias   Alias of the command which was used
     * @param args    The arguments passed to the command, including the current argument being completed.
     * @return A list of possible completions.
     */
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

    /**
     * Helper method to find partial matches for tab completion.
     * Returns a list of strings from candidates that start with 'arg' (case-insensitive).
     *
     * @param arg        The current argument being completed.
     * @param candidates The possible completion candidates.
     * @return A list of candidates that partially match the argument.
     */
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
