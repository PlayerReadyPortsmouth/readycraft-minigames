package com.auroraschaos.minigames.commands;

import com.auroraschaos.minigames.MinigamesPlugin;
import com.auroraschaos.minigames.party.Party;
import com.auroraschaos.minigames.party.PartyManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * /party create      → create a new party
 * /party invite <player>   → invite another to your party (auto‐joins)
 * /party leave       → leave your current party
 * /party disband     → disband if you are the leader
 */
public class PartyCommand implements CommandExecutor {

    private final PartyManager partyManager;

    public PartyCommand(MinigamesPlugin plugin) {
        this.partyManager = plugin.getPartyManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use that.");
            return true;
        }
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "/party create | invite <player> | leave | disband");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create":
                if (partyManager.isInParty(uuid)) {
                    player.sendMessage(ChatColor.RED + "You’re already in a party.");
                    return true;
                }
                Party party = partyManager.createParty(player);
                if (party == null) {
                    player.sendMessage(ChatColor.RED + "Could not create party.");
                } else {
                    player.sendMessage(ChatColor.GREEN + "Party created! You are the leader.");
                }
                break;

            case "invite":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.YELLOW + "Usage: /party invite <player>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                if (!partyManager.isInParty(uuid)) {
                    player.sendMessage(ChatColor.RED + "You’re not in a party. Use /party create first.");
                    return true;
                }
                Party yourParty = partyManager.getParty(uuid);
                if (!yourParty.getLeader().equals(uuid)) {
                    player.sendMessage(ChatColor.RED + "Only the leader can invite.");
                    return true;
                }
                if (partyManager.addToParty(yourParty.getId(), target)) {
                    player.sendMessage(ChatColor.GREEN + "Invited " + target.getName() + " to your party.");
                    target.sendMessage(ChatColor.GREEN + "You joined " + player.getName() +
                                       "'s party. All party members share color: " +
                                       yourParty.getColor() + yourParty.getColor().name());
                } else {
                    player.sendMessage(ChatColor.RED + "Could not add " + target.getName() +
                                       " (maybe already in a party?).");
                }
                break;

            case "leave":
                if (!partyManager.isInParty(uuid)) {
                    player.sendMessage(ChatColor.RED + "You’re not in a party.");
                    return true;
                }
                Party p = partyManager.getParty(uuid);
                boolean removed = partyManager.removeFromParty(player);
                if (removed) {
                    player.sendMessage(ChatColor.YELLOW + "You left the party.");
                    // If you were the leader, a new leader is chosen automatically.
                    // If party is disbanded (no members remain), nothing more to do.
                } else {
                    player.sendMessage(ChatColor.RED + "Could not leave the party.");
                }
                break;

            case "disband":
                if (!partyManager.isInParty(uuid)) {
                    player.sendMessage(ChatColor.RED + "You’re not in a party.");
                    return true;
                }
                Party toDisband = partyManager.getParty(uuid);
                if (!toDisband.getLeader().equals(uuid)) {
                    player.sendMessage(ChatColor.RED + "Only the leader can disband the party.");
                    return true;
                }
                partyManager.disbandParty(toDisband.getId());
                player.sendMessage(ChatColor.YELLOW + "Your party has been disbanded.");
                break;

            default:
                player.sendMessage(ChatColor.YELLOW + "Unknown subcommand. Use /party create|invite|leave|disband");
                break;
        }
        return true;
    }
}