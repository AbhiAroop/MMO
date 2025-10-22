package com.server.islands.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.server.islands.managers.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Main command handler for the island system.
 * Handles all /island subcommands.
 */
public class IslandCommand implements CommandExecutor, TabCompleter {
    
    private final IslandManager islandManager;
    
    public IslandCommand(IslandManager islandManager) {
        this.islandManager = islandManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        // No arguments - teleport to own island or show help
        if (args.length == 0) {
            handleIslandHome(player);
            return true;
        }
        
        // Handle subcommands
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "create":
                handleIslandCreate(player, args);
                break;
            case "delete":
                handleIslandDelete(player);
                break;
            case "home":
            case "h":
                handleIslandHome(player);
                break;
            case "visit":
            case "v":
                handleIslandVisit(player, args);
                break;
            case "upgrade":
            case "up":
                handleIslandUpgrade(player);
                break;
            case "settings":
            case "config":
                handleIslandSettings(player);
                break;
            case "info":
            case "i":
                handleIslandInfo(player, args);
                break;
            case "members":
            case "m":
                handleIslandMembers(player);
                break;
            case "invite":
                handleIslandInvite(player, args);
                break;
            case "kick":
                handleIslandKick(player, args);
                break;
            case "help":
            case "?":
                sendHelpMessage(player);
                break;
            default:
                player.sendMessage(Component.text("Unknown subcommand. Use ", NamedTextColor.RED)
                    .append(Component.text("/island help", NamedTextColor.YELLOW))
                    .append(Component.text(" for a list of commands.", NamedTextColor.RED)));
                break;
        }
        
        return true;
    }
    
    /**
     * Handles /island create [type]
     */
    private void handleIslandCreate(Player player, String[] args) {
        // Open GUI to select island type
        IslandCreateGUI.open(player, islandManager);
    }
    
    /**
     * Handles /island delete
     */
    private void handleIslandDelete(Player player) {
        islandManager.deleteIsland(player.getUniqueId()).thenAccept(success -> {
            if (success) {
                player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .append(Component.text("Your island has been deleted.", NamedTextColor.GREEN)));
            } else {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have an island to delete.", NamedTextColor.RED)));
            }
        });
    }
    
    /**
     * Handles /island or /island home
     */
    private void handleIslandHome(Player player) {
        islandManager.teleportToIsland(player, player.getUniqueId()).thenAccept(success -> {
            if (success) {
                player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .append(Component.text("Welcome to your island!", NamedTextColor.GREEN)));
            } else {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have an island yet! Use ", NamedTextColor.RED))
                    .append(Component.text("/island create", NamedTextColor.YELLOW))
                    .append(Component.text(" to purchase one.", NamedTextColor.RED)));
            }
        });
    }
    
    /**
     * Handles /island visit <player>
     */
    private void handleIslandVisit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: ", NamedTextColor.RED)
                .append(Component.text("/island visit <player>", NamedTextColor.YELLOW)));
            return;
        }
        
        String targetName = args[1];
        Player targetPlayer = player.getServer().getPlayer(targetName);
        
        if (targetPlayer == null) {
            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("Player not found: " + targetName, NamedTextColor.RED)));
            return;
        }
        
        islandManager.teleportToIsland(player, targetPlayer.getUniqueId()).thenAccept(success -> {
            if (success) {
                player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .append(Component.text("Welcome to ", NamedTextColor.GREEN))
                    .append(Component.text(targetPlayer.getName(), NamedTextColor.AQUA))
                    .append(Component.text("'s island!", NamedTextColor.GREEN)));
            } else {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("Could not visit that island. It may not exist or is full.", NamedTextColor.RED)));
            }
        });
    }
    
    /**
     * Handles /island upgrade - Opens upgrade GUI
     */
    private void handleIslandUpgrade(Player player) {
        // Check if player has an island
        islandManager.getIsland(player.getUniqueId()).thenAccept(island -> {
            if (island == null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have an island!", NamedTextColor.RED)));
                return;
            }
            
            // Open upgrade GUI on main thread
            org.bukkit.Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                IslandUpgradeGUI.open(player, island, islandManager);
            });
        });
    }
    
    /**
     * Handles /island settings - Opens settings GUI
     */
    private void handleIslandSettings(Player player) {
        player.sendMessage(Component.text("⚙ ", NamedTextColor.GOLD, TextDecoration.BOLD)
            .append(Component.text("Island settings GUI coming soon!", NamedTextColor.YELLOW)));
    }
    
    /**
     * Handles /island info [player] - Shows island information
     */
    private void handleIslandInfo(Player player, String[] args) {
        if (args.length < 2) {
            // Show own island info
            IslandInfoGUI.open(player, player.getUniqueId(), islandManager);
        } else {
            // Show target player's island info
            String targetName = args[1];
            Player targetPlayer = player.getServer().getPlayer(targetName);
            
            if (targetPlayer == null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("Player not found: " + targetName, NamedTextColor.RED)));
                return;
            }
            
            IslandInfoGUI.open(player, targetPlayer.getUniqueId(), islandManager);
        }
    }
    
    /**
     * Handles /island members - Shows island members
     */
    private void handleIslandMembers(Player player) {
        player.sendMessage(Component.text("👥 ", NamedTextColor.AQUA, TextDecoration.BOLD)
            .append(Component.text("Island members GUI coming soon!", NamedTextColor.YELLOW)));
    }
    
    /**
     * Handles /island invite <player>
     */
    private void handleIslandInvite(Player player, String[] args) {
        player.sendMessage(Component.text("📨 ", NamedTextColor.GREEN, TextDecoration.BOLD)
            .append(Component.text("Island invite system coming soon!", NamedTextColor.YELLOW)));
    }
    
    /**
     * Handles /island kick <player>
     */
    private void handleIslandKick(Player player, String[] args) {
        player.sendMessage(Component.text("👢 ", NamedTextColor.RED, TextDecoration.BOLD)
            .append(Component.text("Island kick system coming soon!", NamedTextColor.YELLOW)));
    }
    
    /**
     * Sends help message with all available commands
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("       🏝 ISLAND COMMANDS 🏝", NamedTextColor.AQUA, TextDecoration.BOLD));
        player.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.empty());
        
        sendCommandHelp(player, "/island", "Teleport to your island");
        sendCommandHelp(player, "/island create", "Open GUI to purchase an island");
        sendCommandHelp(player, "/island delete", "Delete your island (cannot be undone!)");
        sendCommandHelp(player, "/island home", "Teleport to your island");
        sendCommandHelp(player, "/island visit <player>", "Visit another player's island");
        sendCommandHelp(player, "/island upgrade", "Open upgrade GUI");
        sendCommandHelp(player, "/island info [player]", "View island information");
        sendCommandHelp(player, "/island settings", "Configure island settings");
        sendCommandHelp(player, "/island members", "View island members");
        sendCommandHelp(player, "/island invite <player>", "Invite a player to your island");
        sendCommandHelp(player, "/island kick <player>", "Remove a player from your island");
        
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD));
    }
    
    private void sendCommandHelp(Player player, String command, String description) {
        player.sendMessage(
            Component.text("  • ", NamedTextColor.GRAY)
                .append(Component.text(command, NamedTextColor.YELLOW))
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(Component.text(description, NamedTextColor.WHITE))
        );
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }
        
        List<String> suggestions = new ArrayList<>();
        
        if (args.length == 1) {
            // Suggest subcommands
            suggestions.addAll(Arrays.asList(
                "create", "delete", "home", "visit", "upgrade", 
                "info", "settings", "members", "invite", "kick", "help"
            ));
            
            // Filter by what player has typed
            return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            // Suggest player names for visit, info, invite, kick
            if (subCommand.equals("visit") || subCommand.equals("info") || 
                subCommand.equals("invite") || subCommand.equals("kick")) {
                return sender.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        return suggestions;
    }
}
