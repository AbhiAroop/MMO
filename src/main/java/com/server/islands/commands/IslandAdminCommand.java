package com.server.islands.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
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
 * Admin command handler for the island system.
 * Provides tools for managing islands, tokens, levels, and testing.
 * Permission: island.admin
 */
public class IslandAdminCommand implements CommandExecutor, TabCompleter {
    
    private final IslandManager islandManager;
    private com.server.islands.managers.ChallengeManager challengeManager;
    
    public IslandAdminCommand(IslandManager islandManager) {
        this.islandManager = islandManager;
    }
    
    public void setChallengeManager(com.server.islands.managers.ChallengeManager challengeManager) {
        this.challengeManager = challengeManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("island.admin")) {
            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("You don't have permission to use this command!", NamedTextColor.RED)));
            return true;
        }
        
        // No arguments - open admin GUI
        if (args.length == 0) {
            player.sendMessage(Component.text("✗ Admin GUI coming soon! Use commands for now.", NamedTextColor.YELLOW));
            sendUsage(player);
            return true;
        }
        
        // Handle subcommands
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "settokens":
                handleSetTokens(player, args);
                break;
            case "addtokens":
                handleAddTokens(player, args);
                break;
            case "setlevel":
                handleSetLevel(player, args);
                break;
            case "setvalue":
                handleSetValue(player, args);
                break;
            case "info":
                handleInfo(player, args);
                break;
            case "completechallenge":
            case "complete":
                handleCompleteChallenge(player, args);
                break;
            default:
                sendUsage(player);
                break;
        }
        
        return true;
    }
    
    private void handleSetTokens(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /islandadmin settokens <player> <amount>", NamedTextColor.RED));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("✗ Player not found!", NamedTextColor.RED));
            return;
        }
        
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("✗ Invalid number!", NamedTextColor.RED));
            return;
        }
        
        islandManager.getPlayerIslandId(target.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ Player doesn't have an island!", NamedTextColor.RED));
                return;
            }
            
            islandManager.loadIsland(islandId).thenAccept(island -> {
                if (island == null) return;
                
                island.setIslandTokens(amount);
                islandManager.getDataManager().saveIsland(island).thenRun(() -> {
                    player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(Component.text("Set ", NamedTextColor.GREEN))
                        .append(Component.text(target.getName(), NamedTextColor.AQUA))
                        .append(Component.text("'s island tokens to ", NamedTextColor.GREEN))
                        .append(Component.text(amount + " ⭐", NamedTextColor.GOLD, TextDecoration.BOLD)));
                });
            });
        });
    }
    
    private void handleAddTokens(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /islandadmin addtokens <player> <amount>", NamedTextColor.RED));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("✗ Player not found!", NamedTextColor.RED));
            return;
        }
        
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("✗ Invalid number!", NamedTextColor.RED));
            return;
        }
        
        islandManager.getPlayerIslandId(target.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ Player doesn't have an island!", NamedTextColor.RED));
                return;
            }
            
            islandManager.loadIsland(islandId).thenAccept(island -> {
                if (island == null) return;
                
                island.addIslandTokens(amount);
                islandManager.getDataManager().saveIsland(island).thenRun(() -> {
                    player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(Component.text("Added ", NamedTextColor.GREEN))
                        .append(Component.text(amount + " ⭐", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .append(Component.text(" to ", NamedTextColor.GREEN))
                        .append(Component.text(target.getName(), NamedTextColor.AQUA))
                        .append(Component.text("'s island! (Total: " + island.getIslandTokens() + " ⭐)", NamedTextColor.GREEN)));
                });
            });
        });
    }
    
    private void handleSetLevel(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /islandadmin setlevel <player> <level>", NamedTextColor.RED));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("✗ Player not found!", NamedTextColor.RED));
            return;
        }
        
        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("✗ Invalid number!", NamedTextColor.RED));
            return;
        }
        
        islandManager.getPlayerIslandId(target.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ Player doesn't have an island!", NamedTextColor.RED));
                return;
            }
            
            islandManager.loadIsland(islandId).thenAccept(island -> {
                if (island == null) return;
                
                island.setIslandLevel(level);
                islandManager.getDataManager().saveIsland(island).thenRun(() -> {
                    player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(Component.text("Set ", NamedTextColor.GREEN))
                        .append(Component.text(target.getName(), NamedTextColor.AQUA))
                        .append(Component.text("'s island level to ", NamedTextColor.GREEN))
                        .append(Component.text(level, NamedTextColor.GOLD, TextDecoration.BOLD)));
                });
            });
        });
    }
    
    private void handleSetValue(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /islandadmin setvalue <player> <value>", NamedTextColor.RED));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("✗ Player not found!", NamedTextColor.RED));
            return;
        }
        
        long value;
        try {
            value = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("✗ Invalid number!", NamedTextColor.RED));
            return;
        }
        
        islandManager.getPlayerIslandId(target.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ Player doesn't have an island!", NamedTextColor.RED));
                return;
            }
            
            islandManager.loadIsland(islandId).thenAccept(island -> {
                if (island == null) return;
                
                island.setIslandValue(value);
                islandManager.getDataManager().saveIsland(island).thenRun(() -> {
                    player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(Component.text("Set ", NamedTextColor.GREEN))
                        .append(Component.text(target.getName(), NamedTextColor.AQUA))
                        .append(Component.text("'s island value to ", NamedTextColor.GREEN))
                        .append(Component.text(value, NamedTextColor.GOLD, TextDecoration.BOLD)));
                });
            });
        });
    }
    
    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /islandadmin info <player>", NamedTextColor.RED));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("✗ Player not found!", NamedTextColor.RED));
            return;
        }
        
        islandManager.getPlayerIslandId(target.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ Player doesn't have an island!", NamedTextColor.RED));
                return;
            }
            
            islandManager.loadIsland(islandId).thenAccept(island -> {
                if (island == null) return;
                
                player.sendMessage(Component.empty());
                player.sendMessage(Component.text("━━━━━ ", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH)
                    .append(Component.text(" Island Info ", NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text(" ━━━━━", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH))
                    .decoration(TextDecoration.STRIKETHROUGH, false));
                player.sendMessage(Component.text("  Owner: ", NamedTextColor.GRAY)
                    .append(Component.text(target.getName(), NamedTextColor.AQUA)));
                player.sendMessage(Component.text("  Island ID: ", NamedTextColor.GRAY)
                    .append(Component.text(islandId.toString(), NamedTextColor.WHITE)));
                player.sendMessage(Component.text("  Level: ", NamedTextColor.GRAY)
                    .append(Component.text(island.getIslandLevel(), NamedTextColor.GOLD)));
                player.sendMessage(Component.text("  Value: ", NamedTextColor.GRAY)
                    .append(Component.text(island.getIslandValue(), NamedTextColor.YELLOW)));
                player.sendMessage(Component.text("  Tokens: ", NamedTextColor.GRAY)
                    .append(Component.text(island.getIslandTokens() + " ⭐", NamedTextColor.GOLD, TextDecoration.BOLD)));
                player.sendMessage(Component.text("  Size: ", NamedTextColor.GRAY)
                    .append(Component.text(island.getCurrentSize() + "x" + island.getCurrentSize(), NamedTextColor.GREEN))
                    .append(Component.text(" (Level " + island.getSizeLevel() + ")", NamedTextColor.DARK_GREEN)));
                player.sendMessage(Component.text("  Members: ", NamedTextColor.GRAY)
                    .append(Component.text(island.getCurrentPlayerLimit() + " max", NamedTextColor.GREEN))
                    .append(Component.text(" (Level " + island.getPlayerLimitLevel() + ")", NamedTextColor.DARK_GREEN)));
                player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH));
                player.sendMessage(Component.empty());
            });
        });
    }
    
    private void handleCompleteChallenge(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /islandadmin completechallenge <player> <challengeId>", NamedTextColor.RED));
            player.sendMessage(Component.text("Example: /islandadmin complete AbhiAroop farming_wheat_10", NamedTextColor.GRAY));
            return;
        }
        
        if (challengeManager == null) {
            player.sendMessage(Component.text("✗ Challenge system not initialized!", NamedTextColor.RED));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("✗ Player not found!", NamedTextColor.RED));
            return;
        }
        
        String challengeId = args[2];
        
        // Check if challenge exists
        if (challengeManager.getChallenge(challengeId) == null) {
            player.sendMessage(Component.text("✗ Challenge not found: " + challengeId, NamedTextColor.RED));
            player.sendMessage(Component.text("Use tab completion or check challenge IDs in StarterChallenges.java", NamedTextColor.GRAY));
            return;
        }
        
        islandManager.getPlayerIslandId(target.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ Player doesn't have an island!", NamedTextColor.RED));
                return;
            }
            
            // Force complete the challenge
            challengeManager.forceCompleteChallenge(islandId, challengeId).thenAccept(success -> {
                if (success) {
                    player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(Component.text("Completed challenge ", NamedTextColor.GREEN))
                        .append(Component.text(challengeId, NamedTextColor.AQUA))
                        .append(Component.text(" for ", NamedTextColor.GREEN))
                        .append(Component.text(target.getName(), NamedTextColor.AQUA)));
                } else {
                    player.sendMessage(Component.text("✗ Failed to complete challenge! It may already be completed.", NamedTextColor.RED));
                }
            });
        });
    }
    
    private void sendUsage(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("━━━━━ ", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH)
            .append(Component.text(" Island Admin Commands ", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text(" ━━━━━", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH))
            .decoration(TextDecoration.STRIKETHROUGH, false));
        player.sendMessage(Component.text("  /islandadmin", NamedTextColor.AQUA)
            .append(Component.text(" - Open admin GUI", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /islandadmin settokens <player> <amount>", NamedTextColor.AQUA)
            .append(Component.text(" - Set island tokens", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /islandadmin addtokens <player> <amount>", NamedTextColor.AQUA)
            .append(Component.text(" - Add island tokens", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /islandadmin setlevel <player> <level>", NamedTextColor.AQUA)
            .append(Component.text(" - Set island level", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /islandadmin setvalue <player> <value>", NamedTextColor.AQUA)
            .append(Component.text(" - Set island value", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /islandadmin info <player>", NamedTextColor.AQUA)
            .append(Component.text(" - View island info", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /islandadmin completechallenge <player> <challengeId>", NamedTextColor.AQUA)
            .append(Component.text(" - Force complete a challenge", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH));
        player.sendMessage(Component.empty());
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("settokens");
            completions.add("addtokens");
            completions.add("setlevel");
            completions.add("setvalue");
            completions.add("info");
            completions.add("completechallenge");
            completions.add("gui");
        } else if (args.length == 2) {
            // Suggest online player names
            return null; // Bukkit will handle player name completion
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("completechallenge") || args[0].equalsIgnoreCase("complete"))) {
            // Suggest challenge IDs
            if (challengeManager != null) {
                return challengeManager.getAllChallenges().stream()
                    .map(challenge -> challenge.getId())
                    .collect(java.util.stream.Collectors.toList());
            }
        }
        
        return completions;
    }
}
