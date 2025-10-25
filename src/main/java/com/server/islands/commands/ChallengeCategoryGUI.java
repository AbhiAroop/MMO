package com.server.islands.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.islands.data.ChallengeProgress;
import com.server.islands.data.IslandChallenge;
import com.server.islands.data.IslandChallenge.ChallengeCategory;
import com.server.islands.managers.ChallengeManager;
import com.server.islands.managers.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * GUI showing challenges within a specific category with pagination.
 */
public class ChallengeCategoryGUI {
    
    private static final int CHALLENGES_PER_PAGE = 28; // 4 rows of 7 items
    
    /**
     * Opens a category-specific challenge GUI for a player.
     */
    public static void open(Player player, ChallengeCategory category, int page, 
                           IslandManager islandManager, ChallengeManager challengeManager) {
        
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You need an island to view challenges!", NamedTextColor.RED)));
                return;
            }
            
            // Get all challenges in this category
            List<IslandChallenge> allChallenges = challengeManager.getChallengesByCategory(category);
            
            // Get completed challenges
            challengeManager.getCompletedChallenges(islandId, player.getUniqueId()).thenAccept(completedIds -> {
                
                // Calculate pagination
                final int totalPages = Math.max(1, (int) Math.ceil((double) allChallenges.size() / CHALLENGES_PER_PAGE));
                final int currentPage = Math.max(1, Math.min(page, totalPages));
                
                int startIndex = (currentPage - 1) * CHALLENGES_PER_PAGE;
                int endIndex = Math.min(startIndex + CHALLENGES_PER_PAGE, allChallenges.size());
                
                List<IslandChallenge> pageChallenges = allChallenges.subList(startIndex, endIndex);
                
                Bukkit.getScheduler().runTask(challengeManager.getPlugin(), () -> {
                    Inventory inv = Bukkit.createInventory(null, 54,
                        Component.text(category.getIcon() + " " + category.getDisplayName() + " Challenges", 
                            NamedTextColor.GOLD, TextDecoration.BOLD));
                    
                    // Decorative borders
                    Material borderMaterial = getBorderMaterial(category);
                    ItemStack border = new ItemStack(borderMaterial);
                    ItemMeta borderMeta = border.getItemMeta();
                    borderMeta.displayName(Component.text(" "));
                    border.setItemMeta(borderMeta);
                    
                    for (int i = 0; i < 9; i++) inv.setItem(i, border);
                    for (int i = 45; i < 54; i++) inv.setItem(i, border);
                    
                    // Category info
                    ItemStack categoryInfo = new ItemStack(getCategoryMaterial(category));
                    ItemMeta infoMeta = categoryInfo.getItemMeta();
                    infoMeta.displayName(Component.text(category.getDisplayName(), NamedTextColor.GOLD, TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false));
                    
                    List<Component> infoLore = new ArrayList<>();
                    infoLore.add(Component.empty());
                    infoLore.add(Component.text(category.getDescription(), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                    infoLore.add(Component.empty());
                    infoLore.add(Component.text("Total Challenges: " + allChallenges.size(), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
                    infoLore.add(Component.text("Completed: " + completedIds.size(), NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
                    infoLore.add(Component.text("Page " + currentPage + "/" + totalPages, NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
                    infoMeta.lore(infoLore);
                    categoryInfo.setItemMeta(infoMeta);
                    inv.setItem(4, categoryInfo);
                    
                    // Add challenges to inventory
                    final int[] slotCounter = {10}; // Use array to make it effectively final
                    for (int i = 0; i < pageChallenges.size(); i++) {
                        IslandChallenge challenge = pageChallenges.get(i);
                        final int displaySlot = slotCounter[0];
                        
                        boolean isCompleted = completedIds.contains(challenge.getId());
                        
                        // Check prerequisites
                        boolean prerequisitesMet = true;
                        if (challenge.hasPrerequisites()) {
                            for (String prereqId : challenge.getPrerequisites()) {
                                if (!completedIds.contains(prereqId)) {
                                    prerequisitesMet = false;
                                    break;
                                }
                            }
                        }
                        
                        final boolean finalPrereqMet = prerequisitesMet;
                        
                        // Get progress
                        if (challenge.isIslandWide()) {
                            challengeManager.getIslandProgress(islandId, challenge.getId()).thenAccept(progress -> {
                                Bukkit.getScheduler().runTask(challengeManager.getPlugin(), () -> {
                                    ItemStack item = createChallengeItem(challenge, progress, isCompleted, finalPrereqMet);
                                    if (inv.getViewers().contains(player)) {
                                        inv.setItem(getSlot(displaySlot), item);
                                    }
                                });
                            });
                        } else {
                            challengeManager.getPlayerProgress(player.getUniqueId(), islandId, challenge.getId()).thenAccept(progress -> {
                                Bukkit.getScheduler().runTask(challengeManager.getPlugin(), () -> {
                                    ItemStack item = createChallengeItem(challenge, progress, isCompleted, finalPrereqMet);
                                    if (inv.getViewers().contains(player)) {
                                        inv.setItem(getSlot(displaySlot), item);
                                    }
                                });
                            });
                        }
                        
                        slotCounter[0]++;
                        // Skip border slots
                        if (slotCounter[0] % 9 == 0 || slotCounter[0] % 9 == 8) slotCounter[0] += 2;
                        if (slotCounter[0] >= 44) break;
                    }
                    
                    // Navigation buttons
                    if (currentPage > 1) {
                        ItemStack prev = new ItemStack(Material.ARROW);
                        ItemMeta prevMeta = prev.getItemMeta();
                        prevMeta.displayName(Component.text("← Previous Page", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false));
                        List<Component> prevLore = new ArrayList<>();
                        prevLore.add(Component.text("Page " + (currentPage - 1), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                        prevMeta.lore(prevLore);
                        prev.setItemMeta(prevMeta);
                        inv.setItem(48, prev);
                    }
                    
                    if (currentPage < totalPages) {
                        ItemStack next = new ItemStack(Material.ARROW);
                        ItemMeta nextMeta = next.getItemMeta();
                        nextMeta.displayName(Component.text("Next Page →", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false));
                        List<Component> nextLore = new ArrayList<>();
                        nextLore.add(Component.text("Page " + (currentPage + 1), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                        nextMeta.lore(nextLore);
                        next.setItemMeta(nextMeta);
                        inv.setItem(50, next);
                    }
                    
                    // Back button
                    ItemStack back = new ItemStack(Material.BARRIER);
                    ItemMeta backMeta = back.getItemMeta();
                    backMeta.displayName(Component.text("← Back to Categories", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false));
                    back.setItemMeta(backMeta);
                    inv.setItem(49, back);
                    
                    player.openInventory(inv);
                });
            });
        });
    }
    
    /**
     * Converts linear slot to inventory slot (skipping borders).
     */
    private static int getSlot(int linearSlot) {
        int row = linearSlot / 7;
        int col = linearSlot % 7;
        return (row + 1) * 9 + col + 1;
    }
    
    /**
     * Creates an item representing a challenge.
     */
    private static ItemStack createChallengeItem(IslandChallenge challenge, ChallengeProgress progress, 
                                                 boolean isCompleted, boolean prerequisitesMet) {
        Material material;
        NamedTextColor nameColor;
        
        if (isCompleted) {
            material = Material.LIME_STAINED_GLASS_PANE;
            nameColor = NamedTextColor.GREEN;
        } else if (!prerequisitesMet) {
            material = Material.RED_STAINED_GLASS_PANE;
            nameColor = NamedTextColor.DARK_GRAY;
        } else {
            material = Material.PAPER;
            nameColor = NamedTextColor.YELLOW;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text(challenge.getName(), nameColor, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(challenge.getDescription(), NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        
        // Difficulty
        lore.add(Component.text("Difficulty: ", NamedTextColor.GRAY)
            .append(Component.text(challenge.getDifficulty().getDisplayName() + " " + challenge.getDifficulty().getIcon(), 
                NamedTextColor.GOLD))
            .decoration(TextDecoration.ITALIC, false));
        
        // Reward
        lore.add(Component.text("Reward: ", NamedTextColor.GRAY)
            .append(Component.text("+" + challenge.getTokenReward() + " Island Tokens", NamedTextColor.AQUA))
            .decoration(TextDecoration.ITALIC, false));
        
        // Type
        lore.add(Component.text("Type: ", NamedTextColor.GRAY)
            .append(Component.text(challenge.isIslandWide() ? "Island-Wide" : "Individual", NamedTextColor.YELLOW))
            .decoration(TextDecoration.ITALIC, false));
        
        lore.add(Component.empty());
        
        // Progress
        if (isCompleted) {
            lore.add(Component.text("✔ COMPLETED!", NamedTextColor.GREEN, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        } else if (!prerequisitesMet) {
            lore.add(Component.text("🔒 LOCKED", NamedTextColor.RED, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Complete prerequisites first!", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        } else {
            int percentage = progress.getProgressPercentage(challenge.getTargetAmount());
            lore.add(Component.text("Progress: ", NamedTextColor.GRAY)
                .append(Component.text(progress.getCurrentProgress() + "/" + challenge.getTargetAmount() + 
                    " (" + percentage + "%)", NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false));
            
            // Progress bar
            int barLength = 20;
            int filled = (int) ((percentage / 100.0) * barLength);
            StringBuilder bar = new StringBuilder("▰");
            for (int i = 0; i < filled; i++) bar.append("▰");
            for (int i = filled; i < barLength; i++) bar.append("▱");
            
            lore.add(Component.text(bar.toString(), percentage >= 100 ? NamedTextColor.GREEN : NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        }
        
        // Prerequisites
        if (challenge.hasPrerequisites() && !prerequisitesMet) {
            lore.add(Component.empty());
            lore.add(Component.text("Prerequisites:", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
            for (String prereqId : challenge.getPrerequisites()) {
                lore.add(Component.text("  • " + prereqId, NamedTextColor.DARK_RED)
                    .decoration(TextDecoration.ITALIC, false));
            }
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Gets the border material based on category.
     */
    private static Material getBorderMaterial(ChallengeCategory category) {
        switch (category) {
            case FARMING: return Material.LIME_STAINED_GLASS_PANE;
            case MINING: return Material.GRAY_STAINED_GLASS_PANE;
            case COMBAT: return Material.RED_STAINED_GLASS_PANE;
            case BUILDING: return Material.ORANGE_STAINED_GLASS_PANE;
            case CRAFTING: return Material.BROWN_STAINED_GLASS_PANE;
            case EXPLORATION: return Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case ECONOMY: return Material.GREEN_STAINED_GLASS_PANE;
            case SOCIAL: return Material.PINK_STAINED_GLASS_PANE;
            case PROGRESSION: return Material.PURPLE_STAINED_GLASS_PANE;
            case SPECIAL: return Material.YELLOW_STAINED_GLASS_PANE;
            default: return Material.WHITE_STAINED_GLASS_PANE;
        }
    }
    
    /**
     * Gets the display material for a category.
     */
    private static Material getCategoryMaterial(ChallengeCategory category) {
        switch (category) {
            case FARMING: return Material.GOLDEN_HOE;
            case MINING: return Material.DIAMOND_PICKAXE;
            case COMBAT: return Material.DIAMOND_SWORD;
            case BUILDING: return Material.BRICKS;
            case CRAFTING: return Material.CRAFTING_TABLE;
            case EXPLORATION: return Material.COMPASS;
            case ECONOMY: return Material.EMERALD;
            case SOCIAL: return Material.PLAYER_HEAD;
            case PROGRESSION: return Material.EXPERIENCE_BOTTLE;
            case SPECIAL: return Material.DRAGON_EGG;
            default: return Material.PAPER;
        }
    }
}
