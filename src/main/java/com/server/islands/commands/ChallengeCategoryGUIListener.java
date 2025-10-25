package com.server.islands.commands;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.server.islands.data.IslandChallenge.ChallengeCategory;
import com.server.islands.gui.ChallengeCategoryTreeGUI;
import com.server.islands.managers.ChallengeManager;
import com.server.islands.managers.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Handles clicks in challenge-related GUIs.
 */
public class ChallengeCategoryGUIListener implements Listener {
    
    private final IslandManager islandManager;
    private final ChallengeManager challengeManager;
    
    public ChallengeCategoryGUIListener(IslandManager islandManager, ChallengeManager challengeManager) {
        this.islandManager = islandManager;
        this.challengeManager = challengeManager;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Component title = event.getView().title();
        String titleText = PlainTextComponentSerializer.plainText().serialize(title);
        
        // Check if it's a challenge-related GUI
        if (!titleText.contains("Challenges") && !titleText.contains("Challenge")) {
            return;
        }
        
        event.setCancelled(true); // Prevent item taking
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        Component displayName = clicked.getItemMeta().displayName();
        if (displayName == null) return;
        
        String itemName = PlainTextComponentSerializer.plainText().serialize(displayName);
        
        // Main challenges menu
        if (titleText.contains("Island Challenges")) {
            handleMainChallengesMenu(player, itemName);
        }
        // Category-specific challenges
        else if (titleText.contains("Challenges")) {
            handleCategoryMenu(player, titleText, itemName);
        }
    }
    
    /**
     * Handles clicks in the main challenges menu.
     */
    private void handleMainChallengesMenu(Player player, String itemName) {
        // Back button
        if (itemName.contains("Back to Island Menu")) {
            player.closeInventory();
            org.bukkit.Bukkit.getScheduler().runTaskLater(islandManager.getPlugin(), () -> {
                IslandMenuGUI.open(player, islandManager);
            }, 1L);
            return;
        }
        
        // Category selection
        ChallengeCategory category = getCategoryFromName(itemName);
        if (category != null) {
            player.closeInventory();
            org.bukkit.Bukkit.getScheduler().runTaskLater(islandManager.getPlugin(), () -> {
                // Open the new tree-based GUI instead of the old paginated one
                ChallengeCategoryTreeGUI.openChallengeTreeGUI(player, category, challengeManager, islandManager);
            }, 1L);
        }
    }
    
    /**
     * Handles clicks in a category-specific menu.
     */
    private void handleCategoryMenu(Player player, String title, String itemName) {
        // Back to categories
        if (itemName.contains("Back to Categories")) {
            player.closeInventory();
            org.bukkit.Bukkit.getScheduler().runTaskLater(islandManager.getPlugin(), () -> {
                IslandChallengesGUI.open(player, islandManager, challengeManager);
            }, 1L);
            return;
        }
        
        // Get category from title
        ChallengeCategory category = null;
        for (ChallengeCategory cat : ChallengeCategory.values()) {
            if (title.contains(cat.getDisplayName())) {
                category = cat;
                break;
            }
        }
        
        if (category == null) return;
        final ChallengeCategory finalCategory = category;
        
        // Previous page
        if (itemName.contains("Previous Page")) {
            String pageStr = itemName.replaceAll("[^0-9]", "");
            if (!pageStr.isEmpty()) {
                int page = Integer.parseInt(pageStr);
                player.closeInventory();
                org.bukkit.Bukkit.getScheduler().runTaskLater(islandManager.getPlugin(), () -> {
                    ChallengeCategoryGUI.open(player, finalCategory, page, islandManager, challengeManager);
                }, 1L);
            }
            return;
        }
        
        // Next page
        if (itemName.contains("Next Page")) {
            String pageStr = itemName.replaceAll("[^0-9]", "");
            if (!pageStr.isEmpty()) {
                int page = Integer.parseInt(pageStr);
                player.closeInventory();
                org.bukkit.Bukkit.getScheduler().runTaskLater(islandManager.getPlugin(), () -> {
                    ChallengeCategoryGUI.open(player, finalCategory, page, islandManager, challengeManager);
                }, 1L);
            }
            return;
        }
        
        // Challenge click - just show info (no action needed as progress is automatic)
        // Future: Could add claim rewards, manual completion, etc.
    }
    
    /**
     * Gets a category from an item name.
     */
    private ChallengeCategory getCategoryFromName(String itemName) {
        for (ChallengeCategory category : ChallengeCategory.values()) {
            if (itemName.contains(category.getDisplayName())) {
                return category;
            }
        }
        return null;
    }
}
