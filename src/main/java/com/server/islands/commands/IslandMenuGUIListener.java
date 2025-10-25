package com.server.islands.commands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.server.islands.managers.ChallengeManager;
import com.server.islands.managers.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Handles clicks in the Island Menu GUI
 */
public class IslandMenuGUIListener implements Listener {
    
    private final IslandManager islandManager;
    private ChallengeManager challengeManager; // Optional, set later
    
    public IslandMenuGUIListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }
    
    public void setChallengeManager(ChallengeManager challengeManager) {
        this.challengeManager = challengeManager;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        
        // Cancel all island GUI interactions to prevent item removal
        if (title.contains("Island Menu") || 
            title.contains("Island Members") ||
            title.contains("Invite Player") ||
            title.contains("Visit Island") ||
            title.contains("Leave Island") ||
            title.contains("Delete Island") ||
            title.contains("Manage Roles") ||
            title.contains("Transfer Ownership")) {
            event.setCancelled(true);
        }
        
        if (!title.contains("Island Menu")) {
            return;
        }
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        Component displayName = clicked.getItemMeta().displayName();
        if (displayName == null) {
            return;
        }
        
        String itemName = PlainTextComponentSerializer.plainText().serialize(displayName);
        
        player.closeInventory();
        
        // Handle different menu options
        if (itemName.contains("Island Home")) {
            handleIslandHome(player);
        } else if (itemName.contains("Island Information")) {
            handleIslandInfo(player);
        } else if (itemName.contains("Island Upgrades")) {
            handleIslandUpgrades(player);
        } else if (itemName.contains("Island Members")) {
            handleIslandMembers(player);
        } else if (itemName.contains("Invite Player")) {
            if (!itemName.contains("✗")) {
                handleInvitePlayer(player);
            } else {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have permission to invite players!", NamedTextColor.RED)));
            }
        } else if (itemName.contains("Island Settings")) {
            if (!itemName.contains("✗")) {
                handleIslandSettings(player);
            } else {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have permission to change settings!", NamedTextColor.RED)));
            }
        } else if (itemName.contains("Island Shop")) {
            handleIslandShop(player);
        } else if (itemName.contains("Visit Island")) {
            handleVisitIsland(player);
        } else if (itemName.contains("Leave Island")) {
            handleLeaveIsland(player);
        } else if (itemName.contains("Manage Roles")) {
            if (!itemName.contains("✗")) {
                handleManageRoles(player);
            } else {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have permission to manage roles!", NamedTextColor.RED)));
            }
        } else if (itemName.contains("Transfer Ownership")) {
            handleTransferOwnership(player);
        } else if (itemName.contains("Delete Island")) {
            handleDeleteIsland(player);
        } else if (itemName.contains("Island Challenges")) {
            handleIslandChallenges(player);
        } else if (itemName.contains("Island Help")) {
            handleIslandHelp(player);
        }
    }
    
    private void handleIslandHome(Player player) {
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have an island!", NamedTextColor.RED)));
                return;
            }
            
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
    
    private void handleIslandInfo(Player player) {
        islandManager.getIsland(player.getUniqueId()).thenAccept(island -> {
            if (island == null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have an island!", NamedTextColor.RED)));
                return;
            }
            
            Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                IslandInfoGUI.openForOwnIsland(player, island, islandManager);
            });
        });
    }
    
    private void handleIslandUpgrades(Player player) {
        islandManager.getIsland(player.getUniqueId()).thenAccept(island -> {
            if (island == null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You don't have an island!", NamedTextColor.RED)));
                return;
            }
            
            Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                IslandUpgradeGUI.open(player, island, islandManager);
            });
        });
    }
    
    private void handleIslandMembers(Player player) {
        Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
            IslandMembersGUI.open(player, islandManager);
        });
    }
    
    private void handleInvitePlayer(Player player) {
        Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
            IslandInviteGUI.open(player, islandManager);
        });
    }
    
    private void handleIslandSettings(Player player) {
        player.sendMessage(Component.text("⚙ ", NamedTextColor.GOLD, TextDecoration.BOLD)
            .append(Component.text("Island settings GUI coming soon!", NamedTextColor.YELLOW)));
    }
    
    private void handleVisitIsland(Player player) {
        Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
            IslandVisitGUI.open(player, islandManager);
        });
    }
    
    private void handleLeaveIsland(Player player) {
        Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
            IslandLeaveConfirmGUI.open(player, islandManager);
        });
    }
    
    private void handleManageRoles(Player player) {
        Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
            IslandManageRolesGUI.open(player, islandManager);
        });
    }
    
    private void handleTransferOwnership(Player player) {
        Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
            IslandTransferGUI.open(player, islandManager);
        });
    }
    
    private void handleDeleteIsland(Player player) {
        Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
            IslandDeleteConfirmGUI.open(player, islandManager);
        });
    }
    
    private void handleIslandHelp(Player player) {
        // Send help message
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("       🏝 ISLAND COMMANDS 🏝", NamedTextColor.AQUA, TextDecoration.BOLD));
        player.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  • ", NamedTextColor.GRAY)
            .append(Component.text("/island", NamedTextColor.YELLOW))
            .append(Component.text(" - Open island menu", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("  • ", NamedTextColor.GRAY)
            .append(Component.text("/island create", NamedTextColor.YELLOW))
            .append(Component.text(" - Create an island", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("  • ", NamedTextColor.GRAY)
            .append(Component.text("/island home", NamedTextColor.YELLOW))
            .append(Component.text(" - Teleport to your island", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("  • ", NamedTextColor.GRAY)
            .append(Component.text("/island visit <player>", NamedTextColor.YELLOW))
            .append(Component.text(" - Visit another island", NamedTextColor.WHITE)));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD));
    }
    
    private void handleIslandShop(Player player) {
        Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
            com.server.islands.gui.IslandShopGUI.open(player, islandManager);
        });
    }
    
    private void handleIslandChallenges(Player player) {
        if (challengeManager == null) {
            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("Challenge system is not initialized yet!", NamedTextColor.RED)));
            return;
        }
        
        Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
            IslandChallengesGUI.open(player, islandManager, challengeManager);
        });
    }
}
