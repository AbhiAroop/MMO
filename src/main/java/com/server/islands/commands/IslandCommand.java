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

import com.server.islands.data.IslandInvite;
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
    private com.server.islands.managers.ChallengeManager challengeManager;
    
    public IslandCommand(IslandManager islandManager) {
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
        
        // No arguments - open island menu GUI
        if (args.length == 0) {
            handleIslandMenuGUI(player);
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
            case "challenges":
            case "challenge":
            case "c":
                handleIslandChallenges(player);
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
            case "accept":
                handleIslandAccept(player);
                break;
            case "deny":
                handleIslandDeny(player, args);
                break;
            case "kick":
                handleIslandKick(player, args);
                break;
            case "promote":
                handleIslandPromote(player, args);
                break;
            case "demote":
                handleIslandDemote(player, args);
                break;
            case "transfer":
                handleIslandTransfer(player, args);
                break;
            case "leave":
                handleIslandLeave(player);
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
     * Handles /island - Opens main menu or create GUI
     */
    private void handleIslandMenuGUI(Player player) {
        // Check if player has an island (as owner or member)
        islandManager.hasIslandMembership(player.getUniqueId()).thenAccept(hasMembership -> {
            if (!hasMembership) {
                // Player doesn't have an island - open creation GUI
                org.bukkit.Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                    IslandCreateGUI.open(player, islandManager);
                });
            } else {
                // Player has an island - open main menu
                org.bukkit.Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                    IslandMenuGUI.open(player, islandManager);
                });
            }
        });
    }
    
    /**
     * Handles /island create [type]
     */
    private void handleIslandCreate(Player player, String[] args) {
        // Check if player already has island membership
        islandManager.hasIslandMembership(player.getUniqueId()).thenAccept(hasMembership -> {
            if (hasMembership) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You are already part of an island! Members cannot create new islands.", NamedTextColor.RED)));
                return;
            }
            
            // Open GUI to select island type
            org.bukkit.Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                IslandCreateGUI.open(player, islandManager);
            });
        });
    }
    
    /**
     * Handles /island delete
     */
    private void handleIslandDelete(Player player) {
        // Get player's island first to check permissions
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have an island to delete.", NamedTextColor.RED)));
                return;
            }
            
            // Check if player is the owner
            islandManager.getMemberRole(islandId, player.getUniqueId()).thenAccept(role -> {
                if (role != com.server.islands.data.IslandMember.IslandRole.OWNER) {
                    player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("Only the island owner can delete the island!", NamedTextColor.RED)));
                    return;
                }
                
                // Get all members before deletion
                islandManager.getMembers(islandId).thenAccept(members -> {
                    // Proceed with deletion
                    islandManager.deleteIsland(player.getUniqueId()).thenAccept(success -> {
                        if (success) {
                            player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .append(Component.text("Your island has been deleted.", NamedTextColor.GREEN)));
                            
                            // Notify all members (except the owner who initiated it)
                            for (com.server.islands.data.IslandMember member : members) {
                                if (!member.getPlayerUuid().equals(player.getUniqueId())) {
                                    Player memberPlayer = player.getServer().getPlayer(member.getPlayerUuid());
                                    if (memberPlayer != null && memberPlayer.isOnline()) {
                                        memberPlayer.sendMessage(Component.empty());
                                        memberPlayer.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.RED, TextDecoration.BOLD));
                                        memberPlayer.sendMessage(Component.text("⚠ ISLAND DELETED", NamedTextColor.RED, TextDecoration.BOLD));
                                        memberPlayer.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.RED, TextDecoration.BOLD));
                                        memberPlayer.sendMessage(Component.text("The island you were a member of has been deleted by ", NamedTextColor.YELLOW)
                                            .append(Component.text(player.getName(), NamedTextColor.AQUA))
                                            .append(Component.text(".", NamedTextColor.YELLOW)));
                                        memberPlayer.sendMessage(Component.text("You can now create or join another island.", NamedTextColor.GRAY));
                                        memberPlayer.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.RED, TextDecoration.BOLD));
                                        memberPlayer.sendMessage(Component.empty());
                                    }
                                }
                            }
                        } else {
                            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                                .append(Component.text("Failed to delete island.", NamedTextColor.RED)));
                        }
                    });
                });
            });
        });
    }
    
    /**
     * Handles /island or /island home
     */
    private void handleIslandHome(Player player) {
        // First check if player has an island (as owner or member)
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have an island yet! Use ", NamedTextColor.RED))
                    .append(Component.text("/island create", NamedTextColor.YELLOW))
                    .append(Component.text(" to purchase one.", NamedTextColor.RED)));
                return;
            }
            
            // Load the island and teleport
            islandManager.loadIsland(islandId).thenAccept(island -> {
                if (island == null) {
                    player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("Failed to load island!", NamedTextColor.RED)));
                    return;
                }
                
                islandManager.teleportToIsland(player, island).thenAccept(success -> {
                    if (success) {
                        player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                            .append(Component.text("Welcome to your island!", NamedTextColor.GREEN)));
                    } else {
                        player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                            .append(Component.text("Failed to teleport to island!", NamedTextColor.RED)));
                    }
                });
            });
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
     * Handles /island challenges - Opens challenges GUI
     */
    private void handleIslandChallenges(Player player) {
        // Check if player has an island
        islandManager.getIsland(player.getUniqueId()).thenAccept(island -> {
            if (island == null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have an island!", NamedTextColor.RED)));
                return;
            }
            
            if (challengeManager == null) {
                player.sendMessage(Component.text("✗ Challenge system not initialized!", NamedTextColor.RED));
                return;
            }
            
            // Open challenges GUI on main thread
            org.bukkit.Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                IslandChallengesGUI.open(player, islandManager, challengeManager);
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
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have an island!", NamedTextColor.RED)));
                return;
            }
            
            org.bukkit.Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                IslandMembersGUI.open(player, islandManager);
            });
        });
    }
    
    /**
     * Handles /island invite <player>
     */
    private void handleIslandInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: ", NamedTextColor.RED)
                .append(Component.text("/island invite <player>", NamedTextColor.YELLOW)));
            return;
        }
        
        String targetName = args[1];
        Player targetPlayer = player.getServer().getPlayer(targetName);
        
        if (targetPlayer == null) {
            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("Player not found: " + targetName, NamedTextColor.RED)));
            return;
        }
        
        if (targetPlayer.equals(player)) {
            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("You cannot invite yourself!", NamedTextColor.RED)));
            return;
        }
        
        // Get player's island
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have an island!", NamedTextColor.RED)));
                return;
            }
            
            // Check permissions (MOD or higher can invite)
            islandManager.getMemberRole(islandId, player.getUniqueId()).thenAccept(role -> {
                if (role == null || !role.hasPermission(com.server.islands.data.IslandMember.IslandRole.MOD)) {
                    player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("You don't have permission to invite players!", NamedTextColor.RED)));
                    return;
                }
                
                // Send invitation
                islandManager.invitePlayer(islandId, targetPlayer.getUniqueId(), player.getUniqueId()).thenAccept(success -> {
                    if (success) {
                        player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                            .append(Component.text("Invitation sent to ", NamedTextColor.GREEN))
                            .append(Component.text(targetPlayer.getName(), NamedTextColor.AQUA))
                            .append(Component.text("!", NamedTextColor.GREEN)));
                        
                        targetPlayer.sendMessage(Component.empty());
                        targetPlayer.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD));
                        targetPlayer.sendMessage(Component.text("📨 ISLAND INVITATION", NamedTextColor.GREEN, TextDecoration.BOLD));
                        targetPlayer.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD));
                        targetPlayer.sendMessage(Component.text(player.getName(), NamedTextColor.AQUA)
                            .append(Component.text(" has invited you to join their island!", NamedTextColor.YELLOW)));
                        targetPlayer.sendMessage(Component.text("Use ", NamedTextColor.GRAY)
                            .append(Component.text("/island accept", NamedTextColor.GREEN))
                            .append(Component.text(" to accept or ", NamedTextColor.GRAY))
                            .append(Component.text("/island deny", NamedTextColor.RED))
                            .append(Component.text(" to decline.", NamedTextColor.GRAY)));
                        targetPlayer.sendMessage(Component.text("This invitation expires in 5 minutes.", NamedTextColor.DARK_GRAY));
                        targetPlayer.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD));
                        targetPlayer.sendMessage(Component.empty());
                    } else {
                        player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                            .append(Component.text("Cannot invite this player! They may already have an island.", NamedTextColor.RED)));
                    }
                });
            });
        });
    }
    
    /**
     * Handles /island accept
     */
    private void handleIslandAccept(Player player) {
        islandManager.getPlayerInvites(player.getUniqueId()).thenAccept(invites -> {
            if (invites.isEmpty()) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have any pending island invitations.", NamedTextColor.RED)));
                return;
            }
            
            // Accept the first (most recent) invite
            IslandInvite invite = invites.get(0);
            
            islandManager.acceptInvite(invite.getIslandId(), player.getUniqueId()).thenAccept(success -> {
                if (success) {
                    player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(Component.text("You joined the island! Use ", NamedTextColor.GREEN))
                        .append(Component.text("/island", NamedTextColor.YELLOW))
                        .append(Component.text(" to visit.", NamedTextColor.GREEN)));
                    
                    // Notify inviter if online
                    Player inviter = player.getServer().getPlayer(invite.getInvitedBy());
                    if (inviter != null) {
                        inviter.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                            .append(Component.text(player.getName(), NamedTextColor.AQUA))
                            .append(Component.text(" has joined your island!", NamedTextColor.GREEN)));
                    }
                } else {
                    player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("Failed to accept invitation. You may already have an island.", NamedTextColor.RED)));
                }
            });
        });
    }
    
    /**
     * Handles /island deny
     */
    private void handleIslandDeny(Player player, String[] args) {
        islandManager.getPlayerInvites(player.getUniqueId()).thenAccept(invites -> {
            if (invites.isEmpty()) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have any pending island invitations.", NamedTextColor.RED)));
                return;
            }
            
            // Deny the first (most recent) invite
            IslandInvite invite = invites.get(0);
            
            islandManager.declineInvite(invite.getIslandId(), player.getUniqueId()).thenAccept(success -> {
                player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .append(Component.text("Invitation declined.", NamedTextColor.GREEN)));
            });
        });
    }
    
    /**
     * Handles /island promote <player> <role>
     */
    private void handleIslandPromote(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: ", NamedTextColor.RED)
                .append(Component.text("/island promote <player> <role>", NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("Roles: ", NamedTextColor.GRAY)
                .append(Component.text("OWNER, CO_OWNER, ADMIN, MOD, MEMBER", NamedTextColor.YELLOW)));
            return;
        }
        
        String targetName = args[1];
        String roleName = args[2].toUpperCase();
        
        Player targetPlayer = player.getServer().getPlayer(targetName);
        if (targetPlayer == null) {
            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("Player not found: " + targetName, NamedTextColor.RED)));
            return;
        }
        
        // Parse role
        com.server.islands.data.IslandMember.IslandRole newRole;
        try {
            newRole = com.server.islands.data.IslandMember.IslandRole.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("Invalid role: " + roleName, NamedTextColor.RED)));
            player.sendMessage(Component.text("Valid roles: ", NamedTextColor.GRAY)
                .append(Component.text("OWNER, CO_OWNER, ADMIN, MOD, MEMBER", NamedTextColor.YELLOW)));
            return;
        }
        
        // Get player's island
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have an island!", NamedTextColor.RED)));
                return;
            }
            
            // Check permissions
            islandManager.getMemberRole(islandId, player.getUniqueId()).thenAccept(role -> {
                // Special case: Promoting to OWNER
                if (newRole == com.server.islands.data.IslandMember.IslandRole.OWNER) {
                    if (role != com.server.islands.data.IslandMember.IslandRole.OWNER) {
                        player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                            .append(Component.text("Only the island owner can promote someone to owner!", NamedTextColor.RED)));
                        return;
                    }
                    
                    // Verify target is a member
                    islandManager.getMemberRole(islandId, targetPlayer.getUniqueId()).thenAccept(targetRole -> {
                        if (targetRole == null) {
                            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                                .append(Component.text("That player is not a member of your island!", NamedTextColor.RED)));
                            return;
                        }
                        
                        // Transfer ownership (this will demote current owner to CO_OWNER)
                        islandManager.transferOwnership(islandId, targetPlayer.getUniqueId()).thenAccept(success -> {
                            if (success) {
                                player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                                    .append(Component.text(targetPlayer.getName(), NamedTextColor.AQUA))
                                    .append(Component.text(" is now the owner! You have been demoted to Co-Owner.", NamedTextColor.GREEN)));
                                
                                if (targetPlayer.isOnline()) {
                                    targetPlayer.sendMessage(Component.text("✓ ", NamedTextColor.GOLD, TextDecoration.BOLD)
                                        .append(Component.text("You are now the owner of the island!", NamedTextColor.GOLD)));
                                }
                            } else {
                                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                                    .append(Component.text("Failed to transfer ownership.", NamedTextColor.RED)));
                            }
                        });
                    });
                    return;
                }
                
                // Regular promotion (not to OWNER)
                if (role == null || !role.canManageRole(newRole)) {
                    player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("You don't have permission to promote to this role!", NamedTextColor.RED)));
                    return;
                }
                
                // Promote the player
                islandManager.setMemberRole(islandId, targetPlayer.getUniqueId(), newRole).thenAccept(success -> {
                    if (success) {
                        player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                            .append(Component.text(targetPlayer.getName(), NamedTextColor.AQUA))
                            .append(Component.text(" promoted to ", NamedTextColor.GREEN))
                            .append(Component.text(newRole.getDisplayName(), NamedTextColor.GOLD))
                            .append(Component.text("!", NamedTextColor.GREEN)));
                        
                        if (targetPlayer.isOnline()) {
                            targetPlayer.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .append(Component.text("You have been promoted to ", NamedTextColor.GREEN))
                                .append(Component.text(newRole.getDisplayName(), NamedTextColor.GOLD))
                                .append(Component.text("!", NamedTextColor.GREEN)));
                        }
                    } else {
                        player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                            .append(Component.text("Failed to promote player. They may not be a member.", NamedTextColor.RED)));
                    }
                });
            });
        });
    }
    
    /**
     * Handles /island demote <player>
     */
    private void handleIslandDemote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: ", NamedTextColor.RED)
                .append(Component.text("/island demote <player>", NamedTextColor.YELLOW)));
            return;
        }
        
        String targetName = args[1];
        Player targetPlayer = player.getServer().getPlayer(targetName);
        
        if (targetPlayer == null) {
            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("Player not found: " + targetName, NamedTextColor.RED)));
            return;
        }
        
        // Get player's island
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have an island!", NamedTextColor.RED)));
                return;
            }
            
            // Get target's current role
            islandManager.getMemberRole(islandId, targetPlayer.getUniqueId()).thenAccept(targetRole -> {
                if (targetRole == null) {
                    player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("That player is not a member of your island!", NamedTextColor.RED)));
                    return;
                }
                
                // Check permissions
                islandManager.getMemberRole(islandId, player.getUniqueId()).thenAccept(role -> {
                    if (role == null || !role.canManageRole(targetRole)) {
                        player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                            .append(Component.text("You don't have permission to demote this player!", NamedTextColor.RED)));
                        return;
                    }
                    
                    // Determine new role (one level down)
                    com.server.islands.data.IslandMember.IslandRole newRole = com.server.islands.data.IslandMember.IslandRole.MEMBER;
                    if (targetRole == com.server.islands.data.IslandMember.IslandRole.CO_OWNER) {
                        newRole = com.server.islands.data.IslandMember.IslandRole.ADMIN;
                    } else if (targetRole == com.server.islands.data.IslandMember.IslandRole.ADMIN) {
                        newRole = com.server.islands.data.IslandMember.IslandRole.MOD;
                    } else if (targetRole == com.server.islands.data.IslandMember.IslandRole.MOD) {
                        newRole = com.server.islands.data.IslandMember.IslandRole.MEMBER;
                    }
                    
                    com.server.islands.data.IslandMember.IslandRole finalNewRole = newRole;
                    
                    // Demote the player
                    islandManager.setMemberRole(islandId, targetPlayer.getUniqueId(), finalNewRole).thenAccept(success -> {
                        if (success) {
                            player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .append(Component.text(targetPlayer.getName(), NamedTextColor.AQUA))
                                .append(Component.text(" demoted to ", NamedTextColor.GREEN))
                                .append(Component.text(finalNewRole.getDisplayName(), NamedTextColor.GOLD))
                                .append(Component.text("!", NamedTextColor.GREEN)));
                            
                            if (targetPlayer.isOnline()) {
                                targetPlayer.sendMessage(Component.text("⚠ ", NamedTextColor.YELLOW, TextDecoration.BOLD)
                                    .append(Component.text("You have been demoted to ", NamedTextColor.YELLOW))
                                    .append(Component.text(finalNewRole.getDisplayName(), NamedTextColor.GOLD))
                                    .append(Component.text(".", NamedTextColor.YELLOW)));
                            }
                        } else {
                            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                                .append(Component.text("Failed to demote player.", NamedTextColor.RED)));
                        }
                    });
                });
            });
        });
    }
    
    /**
     * Handles /island transfer <player>
     */
    private void handleIslandTransfer(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: ", NamedTextColor.RED)
                .append(Component.text("/island transfer <player>", NamedTextColor.YELLOW)));
            return;
        }
        
        String targetName = args[1];
        Player targetPlayer = player.getServer().getPlayer(targetName);
        
        if (targetPlayer == null) {
            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("Player not found: " + targetName, NamedTextColor.RED)));
            return;
        }
        
        if (targetPlayer.equals(player)) {
            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("You already own the island!", NamedTextColor.RED)));
            return;
        }
        
        // Get player's island
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have an island!", NamedTextColor.RED)));
                return;
            }
            
            // Check if player is the owner
            islandManager.getMemberRole(islandId, player.getUniqueId()).thenAccept(role -> {
                if (role != com.server.islands.data.IslandMember.IslandRole.OWNER) {
                    player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("Only the island owner can transfer ownership!", NamedTextColor.RED)));
                    return;
                }
                
                // Check if target is a member
                islandManager.getMemberRole(islandId, targetPlayer.getUniqueId()).thenAccept(targetRole -> {
                    if (targetRole == null) {
                        player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                            .append(Component.text("That player is not a member of your island!", NamedTextColor.RED)));
                        return;
                    }
                    
                    // Transfer ownership
                    islandManager.transferOwnership(islandId, targetPlayer.getUniqueId()).thenAccept(success -> {
                        if (success) {
                            player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .append(Component.text("Island ownership transferred to ", NamedTextColor.GREEN))
                                .append(Component.text(targetPlayer.getName(), NamedTextColor.AQUA))
                                .append(Component.text("! You are now a Co-Owner.", NamedTextColor.GREEN)));
                            
                            if (targetPlayer.isOnline()) {
                                targetPlayer.sendMessage(Component.text("✓ ", NamedTextColor.GOLD, TextDecoration.BOLD)
                                    .append(Component.text("You are now the owner of the island!", NamedTextColor.GOLD)));
                            }
                        } else {
                            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                                .append(Component.text("Failed to transfer ownership.", NamedTextColor.RED)));
                        }
                    });
                });
            });
        });
    }
    
    /**
     * Handles /island leave - Leave the island (members to co-owners only)
     */
    private void handleIslandLeave(Player player) {
        // Get player's island
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You are not part of any island!", NamedTextColor.RED)));
                return;
            }
            
            // Check player's role
            islandManager.getMemberRole(islandId, player.getUniqueId()).thenAccept(role -> {
                if (role == null) {
                    player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("Failed to check your island role!", NamedTextColor.RED)));
                    return;
                }
                
                // Owner cannot leave
                if (role == com.server.islands.data.IslandMember.IslandRole.OWNER) {
                    player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("Island owners cannot leave! Use ", NamedTextColor.RED))
                        .append(Component.text("/island delete", NamedTextColor.YELLOW))
                        .append(Component.text(" to delete your island, or ", NamedTextColor.RED))
                        .append(Component.text("/island promote <player> OWNER", NamedTextColor.YELLOW))
                        .append(Component.text(" to transfer ownership first.", NamedTextColor.RED)));
                    return;
                }
                
                // Remove the player from the island
                islandManager.removeMember(islandId, player.getUniqueId()).thenAccept(v -> {
                    player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(Component.text("You have left the island.", NamedTextColor.GREEN)));
                    
                    // Notify the owner
                    islandManager.loadIsland(islandId).thenAccept(island -> {
                        if (island != null) {
                            Player owner = player.getServer().getPlayer(island.getOwnerUuid());
                            if (owner != null && owner.isOnline()) {
                                owner.sendMessage(Component.text("⚠ ", NamedTextColor.YELLOW, TextDecoration.BOLD)
                                    .append(Component.text(player.getName(), NamedTextColor.AQUA))
                                    .append(Component.text(" has left your island.", NamedTextColor.YELLOW)));
                            }
                        }
                    });
                });
            });
        });
    }
    
    /**
     * Handles /island kick <player>
     */
    private void handleIslandKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: ", NamedTextColor.RED)
                .append(Component.text("/island kick <player>", NamedTextColor.YELLOW)));
            return;
        }
        
        String targetName = args[1];
        Player targetPlayer = player.getServer().getPlayer(targetName);
        
        if (targetPlayer == null) {
            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("Player not found: " + targetName, NamedTextColor.RED)));
            return;
        }
        
        // Get player's island
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have an island!", NamedTextColor.RED)));
                return;
            }
            
            // Get target's role
            islandManager.getMemberRole(islandId, targetPlayer.getUniqueId()).thenAccept(targetRole -> {
                if (targetRole == null || targetRole == com.server.islands.data.IslandMember.IslandRole.OWNER) {
                    player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("Cannot kick that player!", NamedTextColor.RED)));
                    return;
                }
                
                // Check permissions (ADMIN or higher can kick)
                islandManager.getMemberRole(islandId, player.getUniqueId()).thenAccept(role -> {
                    if (role == null || !role.canManageRole(targetRole)) {
                        player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                            .append(Component.text("You don't have permission to kick this player!", NamedTextColor.RED)));
                        return;
                    }
                    
                    // Remove the player
                    islandManager.removeMember(islandId, targetPlayer.getUniqueId()).thenAccept(v -> {
                        player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                            .append(Component.text(targetPlayer.getName(), NamedTextColor.AQUA))
                            .append(Component.text(" has been removed from your island.", NamedTextColor.GREEN)));
                        
                        if (targetPlayer.isOnline()) {
                            targetPlayer.sendMessage(Component.text("⚠ ", NamedTextColor.RED, TextDecoration.BOLD)
                                .append(Component.text("You have been removed from the island.", NamedTextColor.RED)));
                        }
                    });
                });
            });
        });
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
        sendCommandHelp(player, "/island delete", "Delete your island (owner only)");
        sendCommandHelp(player, "/island home", "Teleport to your island");
        sendCommandHelp(player, "/island visit <player>", "Visit another player's island");
        sendCommandHelp(player, "/island upgrade", "Open upgrade GUI");
        sendCommandHelp(player, "/island challenges", "View and complete challenges");
        sendCommandHelp(player, "/island info [player]", "View island information");
        sendCommandHelp(player, "/island settings", "Configure island settings");
        sendCommandHelp(player, "/island members", "View island members");
        sendCommandHelp(player, "/island invite <player>", "Invite a player to your island");
        sendCommandHelp(player, "/island accept", "Accept an island invitation");
        sendCommandHelp(player, "/island deny", "Decline an island invitation");
        sendCommandHelp(player, "/island kick <player>", "Remove a player from your island");
        sendCommandHelp(player, "/island leave", "Leave your current island");
        sendCommandHelp(player, "/island promote <player> <role>", "Promote a member");
        sendCommandHelp(player, "/island demote <player>", "Demote a member");
        sendCommandHelp(player, "/island transfer <player>", "Transfer island ownership");
        
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
                "create", "delete", "home", "visit", "upgrade", "challenges",
                "info", "settings", "members", "invite", "accept", "deny",
                "kick", "leave", "promote", "demote", "transfer", "help"
            ));
            
            // Filter by what player has typed
            return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            // Suggest player names for visit, info, invite, kick, promote, demote, transfer
            if (subCommand.equals("visit") || subCommand.equals("info") || 
                subCommand.equals("invite") || subCommand.equals("kick") ||
                subCommand.equals("promote") || subCommand.equals("demote") ||
                subCommand.equals("transfer")) {
                return sender.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            // Suggest roles for promote command
            if (subCommand.equals("promote")) {
                suggestions.addAll(Arrays.asList("OWNER", "CO_OWNER", "ADMIN", "MOD", "MEMBER"));
                return suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        return suggestions;
    }
}
