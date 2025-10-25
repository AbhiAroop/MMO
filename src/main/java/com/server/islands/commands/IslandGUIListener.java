package com.server.islands.commands;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.server.islands.data.IslandChallenge.ChallengeCategory;
import com.server.islands.data.IslandType;
import com.server.islands.data.TreeGridPosition;
import com.server.islands.gui.ChallengeCategoryTreeGUI;
import com.server.islands.managers.ChallengeManager;
import com.server.islands.managers.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Handles all island GUI click events.
 */
public class IslandGUIListener implements Listener {
    
    private final IslandManager islandManager;
    private final ChallengeManager challengeManager;
    
    public IslandGUIListener(IslandManager islandManager, ChallengeManager challengeManager) {
        this.islandManager = islandManager;
        this.challengeManager = challengeManager;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        
        // Check if it's an island GUI
        if (title.contains("Create Island")) {
            event.setCancelled(true);
            handleCreateGUIClick(player, event.getSlot(), event.getCurrentItem());
        } else if (title.contains("Island Upgrades")) {
            event.setCancelled(true);
            handleUpgradeGUIClick(player, event.getSlot());
        } else if (title.contains("Island Info")) {
            event.setCancelled(true);
            if (event.getSlot() == 49) { // Close button
                player.closeInventory();
            }
        } else if (title.contains("Island Challenges")) {
            event.setCancelled(true);
            handleChallengesGUIClick(player, event.getSlot());
        } else if (title.contains("Challenges:")) {
            event.setCancelled(true);
            handleTreeGUIClick(player, event.getSlot(), title);
        }
    }
    
    /**
     * Handles clicks in the Create Island GUI
     */
    private void handleCreateGUIClick(Player player, int slot, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        Component displayName = item.getItemMeta().displayName();
        if (displayName == null) {
            return;
        }
        
        String itemName = PlainTextComponentSerializer.plainText().serialize(displayName);
        
        // Determine which island type was clicked
        IslandType type = null;
        if (itemName.contains("Sky Island")) {
            type = IslandType.SKY;
        } else if (itemName.contains("Ocean Island")) {
            type = IslandType.OCEAN;
        } else if (itemName.contains("Forest Island")) {
            type = IslandType.FOREST;
        }
        
        if (type != null) {
            IslandCreateGUI.handleClick(player, type, islandManager);
        }
    }
    
    /**
     * Handles clicks in the Upgrade GUI
     */
    private void handleUpgradeGUIClick(Player player, int slot) {
        // Get the player's island ID (works for both owners and members)
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                return;
            }
            
            // Handle back button first (doesn't need island data)
            if (slot == 49) {
                // Back button - return to island menu
                org.bukkit.Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                    player.closeInventory();
                    IslandMenuGUI.open(player, islandManager);
                });
                return;
            }
            
            // For upgrade clicks, load the island
            if (slot == 11 || slot == 13 || slot == 15 || slot == 29 || slot == 31) {
                islandManager.loadIsland(islandId).thenAccept(island -> {
                    if (island == null) {
                        return;
                    }
                    IslandUpgradeGUI.handleUpgradeClick(player, island, slot, islandManager);
                });
            }
        });
    }
    
    /**
     * Handles clicks in the Island Challenges menu
     */
    private void handleChallengesGUIClick(Player player, int slot) {
        ChallengeCategory category = null;
        
        // Map slots to categories
        switch (slot) {
            case 10: category = ChallengeCategory.FARMING; break;
            case 12: category = ChallengeCategory.MINING; break;
            case 14: category = ChallengeCategory.COMBAT; break;
            case 16: category = ChallengeCategory.BUILDING; break;
            case 19: category = ChallengeCategory.CRAFTING; break;
            case 21: category = ChallengeCategory.EXPLORATION; break;
            case 23: category = ChallengeCategory.ECONOMY; break;
            case 25: category = ChallengeCategory.SOCIAL; break;
            case 28: category = ChallengeCategory.PROGRESSION; break;
            case 34: category = ChallengeCategory.SPECIAL; break;
            case 49: // Back button
                org.bukkit.Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                    player.closeInventory();
                    IslandMenuGUI.open(player, islandManager);
                });
                return;
            default:
                return;
        }
        
        if (category != null) {
            // Open the tree GUI for this category
            ChallengeCategoryTreeGUI.openChallengeTreeGUI(player, category, challengeManager, islandManager);
        }
    }
    
    /**
     * Handles clicks in the Challenge Tree GUI
     */
    private void handleTreeGUIClick(Player player, int slot, String title) {
        // Extract category from title (format: "Challenges: 🌾 Farming")
        // Remove color codes and split
        String cleanTitle = title.replaceAll("§.", "").trim();
        String[] parts = cleanTitle.split(": ");
        if (parts.length < 2) return;
        
        // Extract category name (last word after icon)
        String categoryPart = parts[1].trim();
        String[] categoryWords = categoryPart.split(" ");
        String categoryName = categoryWords[categoryWords.length - 1].toUpperCase();
        
        ChallengeCategory category;
        try {
            category = ChallengeCategory.valueOf(categoryName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("§cError: Invalid category - " + categoryName));
            return;
        }
        
        // Get current position from the GUI title or default to (0,0)
        TreeGridPosition currentPos = ChallengeCategoryTreeGUI.getPlayerViewPosition(player);
        if (currentPos == null) {
            currentPos = new TreeGridPosition(0, 0);
        }
        
        // Navigation buttons (vertical only)
        if (slot == 26) { // Up arrow - scroll the view UP (increase centerY to show lower Y values at bottom)
            ChallengeCategoryTreeGUI.openChallengeTreeAtPosition(player, category, 
                currentPos.getX(), currentPos.getY() + 1, challengeManager, islandManager);
        } else if (slot == 35) { // Down arrow - scroll the view DOWN (decrease centerY to show higher Y values at top)
            ChallengeCategoryTreeGUI.openChallengeTreeAtPosition(player, category, 
                currentPos.getX(), currentPos.getY() - 1, challengeManager, islandManager);
        } else if (slot == 45) { // Back button
            IslandChallengesGUI.open(player, islandManager, challengeManager);
        }
        // Slots 9-44 can be challenge nodes - handle challenge clicks
        else if (slot >= 9 && slot <= 44) {
            // TODO: Implement challenge detail view or start progress
            player.sendMessage(Component.text("§7Challenge clicked at slot " + slot));
        }
    }
}
