package com.server.islands.commands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.server.islands.data.IslandMember;
import com.server.islands.managers.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Handles all island GUI actions (invite, visit, leave, delete, manage, transfer)
 */
public class IslandGUIActionsListener implements Listener {
    
    private final IslandManager islandManager;
    
    public IslandGUIActionsListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        Component displayName = clicked.getItemMeta().displayName();
        if (displayName == null) {
            return;
        }
        
        String itemName = PlainTextComponentSerializer.plainText().serialize(displayName);
        
        // Handle different GUI types
        if (title.contains("Invite Player")) {
            event.setCancelled(true);
            handleInviteGUI(player, clicked, itemName);
        } else if (title.contains("Visit Island")) {
            event.setCancelled(true);
            handleVisitGUI(player, clicked, itemName);
        } else if (title.contains("Leave Island")) {
            event.setCancelled(true);
            handleLeaveConfirmGUI(player, itemName);
        } else if (title.contains("Delete Island")) {
            event.setCancelled(true);
            handleDeleteConfirmGUI(player, itemName);
        } else if (title.contains("Manage Roles")) {
            event.setCancelled(true);
            handleManageRolesGUI(player, clicked, event.getClick());
        } else if (title.contains("Transfer Ownership")) {
            event.setCancelled(true);
            handleTransferGUI(player, clicked, itemName);
        } else if (title.contains("Island Members")) {
            event.setCancelled(true);
            handleMembersGUI(player, itemName);
        }
    }
    
    /**
     * Handle invite player GUI clicks
     */
    private void handleInviteGUI(Player player, ItemStack clicked, String itemName) {
        if (itemName.contains("Back")) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(islandManager.getPlugin(), () -> {
                IslandMenuGUI.open(player, islandManager);
            }, 1L);
            return;
        }
        
        // Get target player from skull
        if (clicked.getItemMeta() instanceof SkullMeta) {
            SkullMeta skull = (SkullMeta) clicked.getItemMeta();
            if (skull.getOwningPlayer() != null) {
                Player targetPlayer = Bukkit.getPlayer(skull.getOwningPlayer().getUniqueId());
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    player.closeInventory();
                    
                    // Send invitation
                    islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
                        if (islandId == null) {
                            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                                .append(Component.text("You don't have an island!", NamedTextColor.RED)));
                            return;
                        }
                        
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
                }
            }
        }
    }
    
    /**
     * Handle visit island GUI clicks
     */
    private void handleVisitGUI(Player player, ItemStack clicked, String itemName) {
        if (itemName.contains("Back")) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(islandManager.getPlugin(), () -> {
                IslandMenuGUI.open(player, islandManager);
            }, 1L);
            return;
        }
        
        // Get target player from skull
        if (clicked.getItemMeta() instanceof SkullMeta) {
            SkullMeta skull = (SkullMeta) clicked.getItemMeta();
            if (skull.getOwningPlayer() != null) {
                Player targetPlayer = Bukkit.getPlayer(skull.getOwningPlayer().getUniqueId());
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    player.closeInventory();
                    
                    // Use visitIsland to enforce visitor permissions
                    islandManager.visitIsland(player, targetPlayer.getUniqueId()).thenAccept(success -> {
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
            }
        }
    }
    
    /**
     * Handle leave confirmation GUI clicks
     */
    private void handleLeaveConfirmGUI(Player player, String itemName) {
        player.closeInventory();
        
        if (itemName.contains("CONFIRM LEAVE")) {
            // Leave the island
            islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
                if (islandId == null) {
                    player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("You are not part of any island!", NamedTextColor.RED)));
                    return;
                }
                
                islandManager.getMemberRole(islandId, player.getUniqueId()).thenAccept(role -> {
                    if (role == IslandMember.IslandRole.OWNER) {
                        player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                            .append(Component.text("Island owners cannot leave! Transfer ownership first.", NamedTextColor.RED)));
                        return;
                    }
                    
                    islandManager.removeMember(islandId, player.getUniqueId()).thenAccept(v -> {
                        player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                            .append(Component.text("You have left the island.", NamedTextColor.GREEN)));
                        
                        // Notify owner
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
        } else if (itemName.contains("CANCEL")) {
            // Reopen main menu
            Bukkit.getScheduler().runTaskLater(islandManager.getPlugin(), () -> {
                IslandMenuGUI.open(player, islandManager);
            }, 1L);
        }
    }
    
    /**
     * Handle delete confirmation GUI clicks
     */
    private void handleDeleteConfirmGUI(Player player, String itemName) {
        player.closeInventory();
        
        if (itemName.contains("CONFIRM DELETE")) {
            // Delete the island
            islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
                if (islandId == null) {
                    player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("You don't have an island to delete.", NamedTextColor.RED)));
                    return;
                }
                
                islandManager.getMembers(islandId).thenAccept(members -> {
                    islandManager.deleteIsland(player.getUniqueId()).thenAccept(success -> {
                        if (success) {
                            player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .append(Component.text("Your island has been deleted.", NamedTextColor.GREEN)));
                            
                            // Notify all members
                            for (IslandMember member : members) {
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
        } else if (itemName.contains("CANCEL")) {
            // Reopen main menu
            Bukkit.getScheduler().runTaskLater(islandManager.getPlugin(), () -> {
                IslandMenuGUI.open(player, islandManager);
            }, 1L);
        }
    }
    
    /**
     * Handle manage roles GUI clicks
     */
    private void handleManageRolesGUI(Player player, ItemStack clicked, ClickType clickType) {
        if (!(clicked.getItemMeta() instanceof SkullMeta)) {
            // Back button or decoration
            Component displayName = clicked.getItemMeta().displayName();
            if (displayName != null) {
                String itemName = PlainTextComponentSerializer.plainText().serialize(displayName);
                if (itemName.contains("Back")) {
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(islandManager.getPlugin(), () -> {
                        IslandMenuGUI.open(player, islandManager);
                    }, 1L);
                }
            }
            return;
        }
        
        SkullMeta skull = (SkullMeta) clicked.getItemMeta();
        if (skull.getOwningPlayer() == null) return;
        
        Player targetPlayer = Bukkit.getPlayer(skull.getOwningPlayer().getUniqueId());
        if (targetPlayer == null) {
            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("That player is not online!", NamedTextColor.RED)));
            return;
        }
        
        player.closeInventory();
        
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) return;
            
            islandManager.getMemberRole(islandId, targetPlayer.getUniqueId()).thenAccept(targetRole -> {
                if (targetRole == null) return;
                
                if (clickType == ClickType.LEFT) {
                    // Promote
                    promotePlayer(player, targetPlayer, islandId, targetRole);
                } else if (clickType == ClickType.RIGHT) {
                    // Demote
                    demotePlayer(player, targetPlayer, islandId, targetRole);
                } else if (clickType == ClickType.SHIFT_RIGHT) {
                    // Kick
                    kickPlayer(player, targetPlayer, islandId, targetRole);
                }
            });
        });
    }
    
    private void promotePlayer(Player player, Player target, java.util.UUID islandId, IslandMember.IslandRole currentRole) {
        // Prevent self-promotion
        if (player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("You cannot promote yourself!", NamedTextColor.RED)));
            return;
        }
        
        // Check if player has permission
        islandManager.getMemberRole(islandId, player.getUniqueId()).thenAccept(playerRole -> {
            if (playerRole == null || !playerRole.canManageRole(currentRole)) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have permission to promote this player!", NamedTextColor.RED)));
                return;
            }
            
            // Determine next role
            IslandMember.IslandRole newRole;
            if (currentRole == IslandMember.IslandRole.MEMBER) {
                newRole = IslandMember.IslandRole.MOD;
            } else if (currentRole == IslandMember.IslandRole.MOD) {
                newRole = IslandMember.IslandRole.ADMIN;
            } else if (currentRole == IslandMember.IslandRole.ADMIN) {
                newRole = IslandMember.IslandRole.CO_OWNER;
            } else {
                newRole = currentRole;
            }
            
            if (newRole == currentRole) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("Cannot promote " + target.getName() + " further!", NamedTextColor.RED)));
                return;
            }
            
            // Check if player can manage the new role too
            if (!playerRole.canManageRole(newRole)) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have permission to promote to ", NamedTextColor.RED))
                    .append(Component.text(newRole.getDisplayName(), NamedTextColor.GOLD))
                    .append(Component.text("!", NamedTextColor.RED)));
                return;
            }
            
            islandManager.setMemberRole(islandId, target.getUniqueId(), newRole).thenAccept(success -> {
                if (success) {
                    player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(Component.text(target.getName(), NamedTextColor.AQUA))
                        .append(Component.text(" promoted to ", NamedTextColor.GREEN))
                        .append(Component.text(newRole.getDisplayName(), NamedTextColor.GOLD))
                        .append(Component.text("!", NamedTextColor.GREEN)));
                    
                    if (target.isOnline()) {
                        target.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                            .append(Component.text("You have been promoted to ", NamedTextColor.GREEN))
                            .append(Component.text(newRole.getDisplayName(), NamedTextColor.GOLD))
                            .append(Component.text("!", NamedTextColor.GREEN)));
                    }
                }
            });
        });
    }
    
    private void demotePlayer(Player player, Player target, java.util.UUID islandId, IslandMember.IslandRole currentRole) {
        // Prevent self-demotion
        if (player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("You cannot demote yourself!", NamedTextColor.RED)));
            return;
        }
        
        // Check if player has permission
        islandManager.getMemberRole(islandId, player.getUniqueId()).thenAccept(playerRole -> {
            if (playerRole == null || !playerRole.canManageRole(currentRole)) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have permission to demote this player!", NamedTextColor.RED)));
                return;
            }
            
            // Determine lower role
            IslandMember.IslandRole newRole;
            if (currentRole == IslandMember.IslandRole.CO_OWNER) {
                newRole = IslandMember.IslandRole.ADMIN;
            } else if (currentRole == IslandMember.IslandRole.ADMIN) {
                newRole = IslandMember.IslandRole.MOD;
            } else if (currentRole == IslandMember.IslandRole.MOD) {
                newRole = IslandMember.IslandRole.MEMBER;
            } else {
                newRole = currentRole;
            }
            
            if (newRole == currentRole) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("Cannot demote " + target.getName() + " further!", NamedTextColor.RED)));
                return;
            }
            
            islandManager.setMemberRole(islandId, target.getUniqueId(), newRole).thenAccept(success -> {
                if (success) {
                    player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(Component.text(target.getName(), NamedTextColor.AQUA))
                        .append(Component.text(" demoted to ", NamedTextColor.GREEN))
                        .append(Component.text(newRole.getDisplayName(), NamedTextColor.GOLD))
                        .append(Component.text("!", NamedTextColor.GREEN)));
                    
                    if (target.isOnline()) {
                        target.sendMessage(Component.text("⚠ ", NamedTextColor.YELLOW, TextDecoration.BOLD)
                            .append(Component.text("You have been demoted to ", NamedTextColor.YELLOW))
                            .append(Component.text(newRole.getDisplayName(), NamedTextColor.GOLD))
                            .append(Component.text(".", NamedTextColor.YELLOW)));
                    }
                }
            });
        });
    }
    
    private void kickPlayer(Player player, Player target, java.util.UUID islandId, IslandMember.IslandRole targetRole) {
        islandManager.getMemberRole(islandId, player.getUniqueId()).thenAccept(role -> {
            if (role == null || !role.canManageRole(targetRole)) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have permission to kick this player!", NamedTextColor.RED)));
                return;
            }
            
            islandManager.removeMember(islandId, target.getUniqueId()).thenAccept(v -> {
                player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .append(Component.text(target.getName(), NamedTextColor.AQUA))
                    .append(Component.text(" has been removed from your island.", NamedTextColor.GREEN)));
                
                if (target.isOnline()) {
                    target.sendMessage(Component.text("⚠ ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("You have been removed from the island.", NamedTextColor.RED)));
                }
            });
        });
    }
    
    /**
     * Handle transfer ownership GUI clicks
     */
    private void handleTransferGUI(Player player, ItemStack clicked, String itemName) {
        if (itemName.contains("Back")) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(islandManager.getPlugin(), () -> {
                IslandMenuGUI.open(player, islandManager);
            }, 1L);
            return;
        }
        
        if (!(clicked.getItemMeta() instanceof SkullMeta)) return;
        
        SkullMeta skull = (SkullMeta) clicked.getItemMeta();
        if (skull.getOwningPlayer() == null) return;
        
        Player targetPlayer = Bukkit.getPlayer(skull.getOwningPlayer().getUniqueId());
        if (targetPlayer == null) {
            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("That player is not online!", NamedTextColor.RED)));
            return;
        }
        
        player.closeInventory();
        
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) return;
            
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
    }
    
    /**
     * Handle members GUI clicks (back button)
     */
    private void handleMembersGUI(Player player, String itemName) {
        if (itemName.contains("Back")) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(islandManager.getPlugin(), () -> {
                IslandMenuGUI.open(player, islandManager);
            }, 1L);
        }
    }
}
