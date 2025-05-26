package com.server.profiles.skills.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.tokens.SkillToken;
import com.server.profiles.skills.trees.PlayerSkillTreeData;
import com.server.profiles.skills.trees.SkillTree;
import com.server.profiles.skills.trees.SkillTreeNode;
import com.server.profiles.skills.trees.SkillTreeRegistry;
import com.server.profiles.skills.trees.TreeGridPosition;

/**
 * GUI for displaying and interacting with skill trees
 */
public class SkillTreeGUI {
    // Size of the grid and translation to inventory slots
    private static final int GRID_SIZE = 7; // 5x5 visible grid
    private static final int VIEW_RADIUS = GRID_SIZE / 2; // How many tiles in each direction from center
    
    // Title prefix for the GUI
    public static final String GUI_TITLE_PREFIX = "Skill Tree: ";
    
    // Map to track current view positions for players
    private static final Map<Player, TreeGridPosition> playerViewPositions = new HashMap<>();
    
    /**
     * Open the skill tree GUI for a skill
     */
    public static void openSkillTreeGUI(Player player, Skill skill) {
        // Clear any previous view position after tree resets
        if (skill != null && player.hasMetadata("skill_tree_reset") && 
            player.getMetadata("skill_tree_reset").size() > 0 && 
            player.getMetadata("skill_tree_reset").get(0).asString().startsWith(skill.getId())) {
            
            clearPlayerViewPosition(player);
            player.removeMetadata("skill_tree_reset", Main.getInstance());
        }
        
        // Default to showing the root node
        openSkillTreeAtPosition(player, skill, 0, 0);
    }
    
    /**
     * Open the skill tree GUI at a specific position
     */
    public static void openSkillTreeAtPosition(Player player, Skill skill, int centerX, int centerY) {
        // Create inventory
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE_PREFIX + skill.getDisplayName());
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) {
            player.sendMessage(ChatColor.RED + "You need to select a profile first!");
            return;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Store current view position
        playerViewPositions.put(player, new TreeGridPosition(centerX, centerY));
        
        // Get tokens and tree data
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        int tokenCount = treeData.getTokenCount(skill.getId());
        
        // Get unlocked nodes and their levels
        Set<String> unlockedNodes = treeData.getUnlockedNodes(skill.getId());
        Map<String, Integer> nodeLevels = treeData.getNodeLevels(skill.getId());
        
        // Get the skill tree for this skill
        SkillTree tree = SkillTreeRegistry.getInstance().getSkillTree(skill);
        
        // REMOVED: No longer auto-unlock the root node
        // We want the player to click on it to unlock it first

        // Fill empty slots
        fillEmptySlots(gui);
        
        // Fill with skill tree nodes
        fillSkillTreeNodes(gui, tree, unlockedNodes, nodeLevels, centerX, centerY, treeData);
        
        // Add navigation buttons
        addNavigationButtons(gui, skill, treeData);
        
        // Add info button
        addInfoButton(gui, skill, tokenCount);
        
        // Open the inventory
        player.openInventory(gui);
    }

    /**
     * Add a connection line between nodes
     */
    private static void addConnectionLine(Inventory gui, int startX, int startY, int dx, int dy, 
                                        int minX, int minY, boolean sourceUnlocked, boolean targetUnlocked) {
        // Calculate absolute coordinates
        int x = startX + dx;
        int y = startY + dy;
        
        // Check if the coordinates are within the visible grid
        if (x < minX || x > minX + (GRID_SIZE - 1) || y < minY || y > minY + (GRID_SIZE - 1)) {
            return; // Outside the visible area
        }
        
        // Calculate inventory slot - this handles the special edge slots
        int slot = translateGridToSlot(x - minX, y - minY);
        
        // Strict bounds checking
        if (slot < 0 || slot >= 54) {
            return; // Skip if the slot is invalid
        }
        
        try {
            // Try to get the current item at the slot to verify it's accessible
            ItemStack currentItem = gui.getItem(slot);
            
            // Skip if there's already a node at this position (don't overwrite nodes with lines)
            if (currentItem != null && currentItem.hasItemMeta() && 
                currentItem.getItemMeta().hasLore() && 
                currentItem.getItemMeta().getLore().stream().anyMatch(line -> line.startsWith(ChatColor.BLACK + "ID:"))) {
                return;
            }
            
            // Choose the right material for the connection line based on state
            Material lineMaterial;
            ChatColor glassColor;
            
            if (sourceUnlocked && targetUnlocked) {
                // Both nodes are unlocked - fully unlocked path
                lineMaterial = Material.LIME_STAINED_GLASS_PANE;
                glassColor = ChatColor.GREEN;
            } else if (sourceUnlocked || targetUnlocked) {
                // Only one node is unlocked - path leading to an available node
                lineMaterial = Material.YELLOW_STAINED_GLASS_PANE;
                glassColor = ChatColor.YELLOW;
            } else {
                // Neither node is unlocked - locked path
                lineMaterial = Material.GRAY_STAINED_GLASS_PANE;
                glassColor = ChatColor.GRAY;
            }
            
            // Create the item
            ItemStack lineItem = new ItemStack(lineMaterial);
            ItemMeta meta = lineItem.getItemMeta();
            meta.setDisplayName(glassColor + "Path");
            
            // Set lore to identify this as a connection line
            List<String> lore = new ArrayList<>();
            if (sourceUnlocked && targetUnlocked) {
                lore.add(ChatColor.GREEN + "Unlocked path");
            } else if (sourceUnlocked || targetUnlocked) {
                lore.add(ChatColor.YELLOW + "Path to next available node");
            } else {
                lore.add(ChatColor.GRAY + "Locked path");
            }
            lore.add(ChatColor.BLACK + "CONNECTION_LINE"); // Hidden lore for identification
            meta.setLore(lore);
            
            lineItem.setItemMeta(meta);
            
            // Set the item in the inventory
            gui.setItem(slot, lineItem);
        } catch (Exception e) {
            // Log the error but continue
            Main.getInstance().getLogger().warning("Error adding connection line at slot " + slot + ": " + e.getMessage());
        }
    }
    
    
    /**
     * Add navigation buttons to the GUI with improved token display - FIXED ORDER
     */
    private static void addNavigationButtons(Inventory gui, Skill skill, PlayerSkillTreeData treeData) {
        // FIRST: Fill empty slots with glass panes (this won't override our buttons)
        fillEmptySlots(gui);
        
        // THEN: Add all our buttons (this ensures they override any glass panes)
        
        // Add directional movement buttons
        gui.setItem(47, createNavigationButton(Material.SPECTRAL_ARROW, "Move Up", "north"));
        gui.setItem(48, createNavigationButton(Material.SPECTRAL_ARROW, "Move Left", "west"));
        gui.setItem(50, createNavigationButton(Material.SPECTRAL_ARROW, "Move Right", "east"));
        gui.setItem(51, createNavigationButton(Material.SPECTRAL_ARROW, "Move Down", "south"));
        
        // Add enhanced token display in the center bottom with tier breakdown
        SkillToken.TokenInfo tokenInfo = SkillToken.getTokenInfo(skill);
        Map<SkillToken.TokenTier, Integer> tokenCounts = treeData.getAllTokenCounts(skill.getId());
        int totalTokens = treeData.getTokenCount(skill.getId());
        
        // Choose material based on highest tier available
        Material displayMaterial = tokenInfo.material;
        if (tokenCounts.getOrDefault(SkillToken.TokenTier.MASTER, 0) > 0) {
            displayMaterial = getTierMaterial(tokenInfo.material, SkillToken.TokenTier.MASTER);
        } else if (tokenCounts.getOrDefault(SkillToken.TokenTier.ADVANCED, 0) > 0) {
            displayMaterial = getTierMaterial(tokenInfo.material, SkillToken.TokenTier.ADVANCED);
        } else {
            displayMaterial = getTierMaterial(tokenInfo.material, SkillToken.TokenTier.BASIC);
        }
        
        ItemStack tokenDisplay = new ItemStack(displayMaterial);
        ItemMeta tokenMeta = tokenDisplay.getItemMeta();
        
        // Enhanced title with tier indicators
        StringBuilder titleBuilder = new StringBuilder();
        titleBuilder.append(tokenInfo.color).append(tokenInfo.displayName).append(" Tokens");
        
        // Add tier symbols to title if player has them
        if (tokenCounts.getOrDefault(SkillToken.TokenTier.MASTER, 0) > 0) {
            titleBuilder.append(" ").append(SkillToken.TokenTier.MASTER.getColor())
                    .append(SkillToken.TokenTier.MASTER.getSymbol());
        }
        if (tokenCounts.getOrDefault(SkillToken.TokenTier.ADVANCED, 0) > 0) {
            titleBuilder.append(" ").append(SkillToken.TokenTier.ADVANCED.getColor())
                    .append(SkillToken.TokenTier.ADVANCED.getSymbol());
        }
        if (tokenCounts.getOrDefault(SkillToken.TokenTier.BASIC, 0) > 0) {
            titleBuilder.append(" ").append(SkillToken.TokenTier.BASIC.getColor())
                    .append(SkillToken.TokenTier.BASIC.getSymbol());
        }
        
        titleBuilder.append(ChatColor.WHITE).append(" (").append(totalTokens).append(" total)");
        
        tokenMeta.setDisplayName(titleBuilder.toString());
        
        List<String> tokenLore = new ArrayList<>();
        tokenLore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        tokenLore.add(ChatColor.YELLOW + "Your Token Inventory:");
        tokenLore.add("");
        
        // Show each tier with count and description
        for (SkillToken.TokenTier tier : SkillToken.TokenTier.values()) {
            int count = tokenCounts.getOrDefault(tier, 0);
            ChatColor countColor = count > 0 ? ChatColor.WHITE : ChatColor.GRAY;
            
            tokenLore.add(tier.getColor() + tier.getSymbol() + " " + tier.getDisplayName() + 
                        " Tokens: " + countColor + count);
            
            // Add tier description
            switch (tier) {
                case BASIC:
                    tokenLore.add(ChatColor.GRAY + "  » Unlocks foundation abilities");
                    break;
                case ADVANCED:
                    tokenLore.add(ChatColor.GRAY + "  » Unlocks specialized techniques");
                    break;
                case MASTER:
                    tokenLore.add(ChatColor.GRAY + "  » Unlocks elite abilities");
                    break;
            }
            
            if (tier != SkillToken.TokenTier.MASTER) {
                tokenLore.add(""); // Add spacing between tiers
            }
        }
        
        tokenLore.add("");
        tokenLore.add(ChatColor.AQUA + "Token Usage:");
        tokenLore.add(ChatColor.GRAY + "• Each node requires specific tier tokens");
        tokenLore.add(ChatColor.GRAY + "• Higher tier tokens can unlock lower tier nodes");
        tokenLore.add(ChatColor.GRAY + "• Plan your progression strategically!");
        tokenLore.add("");
        tokenLore.add(ChatColor.YELLOW + "Earn tokens by reaching skill milestones");
        tokenLore.add(ChatColor.GRAY + "(Every 5 levels in " + tokenInfo.color + skill.getDisplayName() + ChatColor.GRAY + ")");
        
        tokenMeta.setLore(tokenLore);
        tokenDisplay.setItemMeta(tokenMeta);
        gui.setItem(49, tokenDisplay);
        
        // Add back button to go to skill details instead of skills menu
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "« Back to " + skill.getDisplayName() + " Details");
        
        List<String> backLore = new ArrayList<>();
        backLore.add(ChatColor.GRAY + "Return to " + skill.getDisplayName() + " skill details");
        backLore.add("");
        backLore.add(ChatColor.YELLOW + "View skill information, subskills,");
        backLore.add(ChatColor.YELLOW + "rewards, and other features");
        
        // Add skill ID for the event handler to identify which skill details to open
        backLore.add(ChatColor.BLACK + "SKILL_ID:" + skill.getId());
        
        backMeta.setLore(backLore);
        backButton.setItemMeta(backMeta);
        
        // Place back button at slot 45 (bottom left)
        gui.setItem(45, backButton);
        
        // Add reset button with tiered token preview - ENSURE IT'S PLACED LAST AT SLOT 53
        ItemStack resetButton = new ItemStack(Material.BARRIER);
        ItemMeta resetMeta = resetButton.getItemMeta();
        resetMeta.setDisplayName(ChatColor.RED + "Reset Skill Tree");
        
        List<String> resetLore = new ArrayList<>();
        resetLore.add(ChatColor.GRAY + "Reset all unlocked nodes and");
        resetLore.add(ChatColor.GRAY + "refund all spent tokens by tier.");
        resetLore.add("");
        resetLore.add(ChatColor.YELLOW + "Special nodes will be preserved");
        resetLore.add(ChatColor.YELLOW + "but locked until prerequisites");
        resetLore.add(ChatColor.YELLOW + "are met again.");
        resetLore.add("");
        
        // Show what would be refunded
        if (totalTokens > 0) {
            resetLore.add(ChatColor.AQUA + "Tokens that would be refunded:");
            Map<SkillToken.TokenTier, Integer> potentialRefunds = calculatePotentialRefunds(skill, treeData);
            for (Map.Entry<SkillToken.TokenTier, Integer> entry : potentialRefunds.entrySet()) {
                if (entry.getValue() > 0) {
                    SkillToken.TokenTier tier = entry.getKey();
                    resetLore.add("  " + tier.getColor() + tier.getSymbol() + " " + 
                                entry.getValue() + " " + tier.getDisplayName() + " Tokens");
                }
            }
            resetLore.add("");
        }
        
        resetLore.add(ChatColor.RED + "This action cannot be undone!");
        resetLore.add("");
        resetLore.add(ChatColor.GREEN + "Click to reset");
        
        // Add identifier for the reset button
        resetLore.add(ChatColor.BLACK + "RESET_BUTTON");
        
        resetMeta.setLore(resetLore);
        resetButton.setItemMeta(resetMeta);
        
        // FORCE set the reset button at slot 53 - this must be LAST
        gui.setItem(53, resetButton);
        
        // Debug output to verify the button is placed
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("[SkillTreeGUI] Reset button placed at slot 53: " + 
                (gui.getItem(53) != null ? gui.getItem(53).getType() : "NULL"));
        }
    }

    /**
     * Get tier-appropriate material for a token (helper method)
     */
    private static Material getTierMaterial(Material baseMaterial, SkillToken.TokenTier tier) {
        // Upgrade materials based on tier
        switch (baseMaterial) {
            case IRON_INGOT:
                switch (tier) {
                    case BASIC: return Material.IRON_INGOT;
                    case ADVANCED: return Material.GOLD_INGOT;
                    case MASTER: return Material.NETHERITE_INGOT;
                }
                break;
            case IRON_SWORD:
                switch (tier) {
                    case BASIC: return Material.IRON_SWORD;
                    case ADVANCED: return Material.DIAMOND_SWORD;
                    case MASTER: return Material.NETHERITE_SWORD;
                }
                break;
            case IRON_PICKAXE:
                switch (tier) {
                    case BASIC: return Material.IRON_PICKAXE;
                    case ADVANCED: return Material.DIAMOND_PICKAXE;
                    case MASTER: return Material.NETHERITE_PICKAXE;
                }
                break;
            case IRON_SHOVEL:
                switch (tier) {
                    case BASIC: return Material.IRON_SHOVEL;
                    case ADVANCED: return Material.DIAMOND_SHOVEL;
                    case MASTER: return Material.NETHERITE_SHOVEL;
                }
                break;
            case IRON_AXE:
                switch (tier) {
                    case BASIC: return Material.IRON_AXE;
                    case ADVANCED: return Material.DIAMOND_AXE;
                    case MASTER: return Material.NETHERITE_AXE;
                }
                break;
            case WHEAT:
                switch (tier) {
                    case BASIC: return Material.WHEAT;
                    case ADVANCED: return Material.GOLDEN_CARROT;
                    case MASTER: return Material.ENCHANTED_GOLDEN_APPLE;
                }
                break;
            case FISHING_ROD:
                switch (tier) {
                    case BASIC: return Material.FISHING_ROD;
                    case ADVANCED: return Material.COD;
                    case MASTER: return Material.SALMON;
                }
                break;
            default:
                // For materials without clear upgrades
                switch (tier) {
                    case BASIC: return baseMaterial;
                    case ADVANCED: return Material.GOLD_NUGGET;
                    case MASTER: return Material.NETHER_STAR;
                }
                break;
        }
        return baseMaterial;
    }

    /**
     * Calculate potential token refunds for reset preview
     */
    private static Map<SkillToken.TokenTier, Integer> calculatePotentialRefunds(Skill skill, PlayerSkillTreeData treeData) {
        Map<SkillToken.TokenTier, Integer> refunds = new HashMap<>();
        
        // Initialize refund counters
        for (SkillToken.TokenTier tier : SkillToken.TokenTier.values()) {
            refunds.put(tier, 0);
        }
        
        // Get skill tree
        SkillTree tree = SkillTreeRegistry.getInstance().getSkillTree(skill);
        if (tree == null) return refunds;
        
        // Get current progress
        Set<String> unlockedNodes = treeData.getUnlockedNodes(skill.getId());
        Map<String, Integer> nodeLevels = treeData.getNodeLevels(skill.getId());
        
        // Calculate what would be refunded by tier
        return calculateTokensToRefund(tree, unlockedNodes, nodeLevels);
    }

    /**
     * Fill the GUI with a decorative border
     */
    private static void fillEmptySlots(Inventory gui) {
        // Create border pane item with a more visually appealing color - BLUE instead of GRAY
        ItemStack borderPane = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderPane.getItemMeta();
        borderMeta.setDisplayName(" ");
        borderPane.setItemMeta(borderMeta);
        
        // Create corner pane items with a distinctive color - LIGHT_BLUE for corners
        ItemStack cornerPane = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta cornerMeta = cornerPane.getItemMeta();
        cornerMeta.setDisplayName(" ");
        cornerPane.setItemMeta(cornerMeta);
        
        // Fill background filler item (for empty slots)
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        
        // Set corners
        gui.setItem(0, cornerPane);
        gui.setItem(8, cornerPane);
        gui.setItem(45, gui.getItem(45) != null ? gui.getItem(45) : cornerPane); // Preserve back button if it exists
        gui.setItem(53, cornerPane);
        
        // Create the border pattern, excluding all potential skill tree slots
        for (int i = 0; i < 9; i++) {
            // Top row (excluding slots 11-15 and corners)
            if ((i > 0 && i < 8) && !(i >= 2 && i <= 6) && gui.getItem(i) == null) {
                gui.setItem(i, borderPane);
            }
            
            // Bottom row (skipping corners and back button)
            if (i > 0 && i < 8 && i != 4 && gui.getItem(45 + i) == null) {
                gui.setItem(45 + i, borderPane);
            }
            
            // Left and right columns (skip slots 10, 19, 28, 37 on left and 16, 25, 34, 43 on right)
            for (int row = 1; row < 5; row++) {
                // Skip these specific slots to make them available for the skill tree grid
                int leftSlot = row * 9;
                int rightSlot = row * 9 + 8;
                
                // Don't fill these edge slots with border - we'll use them for the skill tree
                boolean isSpecialLeftSlot = (leftSlot == 10 || leftSlot == 19 || leftSlot == 28 || leftSlot == 37);
                boolean isSpecialRightSlot = (rightSlot == 16 || rightSlot == 25 || rightSlot == 34 || rightSlot == 43);
                
                if (!isSpecialLeftSlot) {
                    // Left column (not a grid slot)
                    if (gui.getItem(leftSlot) == null) {
                        gui.setItem(leftSlot, borderPane);
                    }
                }
                
                if (!isSpecialRightSlot) {
                    // Right column (not a grid slot)
                    if (gui.getItem(rightSlot) == null) {
                        gui.setItem(rightSlot, borderPane);
                    }
                }
            }
        }
        
        // Fill the rest with black panes
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }
    
    /**
     * Add info button to explain skill trees
     */
    private static void addInfoButton(Inventory gui, Skill skill, int tokenCount) {
        ItemStack infoButton = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoButton.getItemMeta();
        infoMeta.setDisplayName(ChatColor.GOLD + "Skill Tree Guide");
        
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.GRAY + "This is your " + ChatColor.YELLOW + skill.getDisplayName() + 
                   ChatColor.GRAY + " skill tree.");
        infoLore.add("");
        infoLore.add(ChatColor.WHITE + "• " + ChatColor.GRAY + "Navigate with the arrow buttons");
        infoLore.add(ChatColor.WHITE + "• " + ChatColor.GRAY + "Unlock nodes with your tokens");
        infoLore.add(ChatColor.WHITE + "• " + ChatColor.GRAY + "Only unlocked or adjacent nodes");
        infoLore.add(ChatColor.GRAY + "  can be unlocked");
        infoLore.add("");
        infoLore.add(ChatColor.YELLOW + "You have " + tokenCount + " tokens to spend");
        
        infoMeta.setLore(infoLore);
        infoButton.setItemMeta(infoMeta);
        gui.setItem(4, infoButton);
    }
    
    /**
     * Create an item for a node in the skill tree
     */
    private static ItemStack createNodeItem(SkillTreeNode node, boolean unlocked, 
                                        SkillTree tree, Set<String> unlockedNodes, 
                                        Map<String, Integer> nodeLevels, PlayerSkillTreeData treeData) {
        Material icon = node.getIcon();
        
        // Current node level
        int currentLevel = nodeLevels.getOrDefault(node.getId(), 0);
        boolean fullyUpgraded = currentLevel >= node.getMaxLevel();
        
        // Special handling for root node if not unlocked
        boolean isRootNode = node.getId().equals("root");
        
        // Special handling for special nodes with preserved levels
        boolean isSpecialReunlock = false;
        int savedLevel = 0;
        
        if (!unlocked && node.isSpecialNode()) {
            savedLevel = nodeLevels.getOrDefault(node.getId(), 0);
            if (savedLevel > 0) {
                isSpecialReunlock = true;
            }
        }
        
        // Change appearance based on node state
        if (unlocked) {
            // Unlocked node - use actual icon
        } else if (isRootNode || tree.isNodeAvailable(node.getId(), unlockedNodes, nodeLevels)) {
            // Available but not unlocked - use glowing effect or different color
            // For certain icons, use a different material to make them more visible
            if (icon == Material.DIAMOND_PICKAXE) icon = Material.IRON_PICKAXE;
            else if (icon == Material.GOLDEN_PICKAXE) icon = Material.STONE_PICKAXE;
            // Add more conversions as needed
        } else {
            // Not available - use locked appearance
            icon = Material.BARRIER;
        }
        
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name
        String displayName;
        if (unlocked) {
            if (node.isUpgradable()) {
                if (fullyUpgraded) {
                    displayName = node.getColor() + node.getName() + ChatColor.GOLD + " ⊹ MAX ⊹";
                    
                    // Add enchant glow effect for max level nodes
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.AQUA_AFFINITY, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                } else {
                    displayName = node.getColor() + node.getName() + ChatColor.YELLOW + " [Lv " + currentLevel + "]";
                }
            } else {
                // Non-upgradable node that's fully unlocked - also add glow
                displayName = node.getColor() + node.getName() + ChatColor.GREEN + " ✓";
                
                // Add enchant glow effect for non-upgradable but unlocked nodes
                meta.addEnchant(org.bukkit.enchantments.Enchantment.AQUA_AFFINITY, 1, true);
                // Hide the enchantments from the lore
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
        } else if (isRootNode || tree.isNodeAvailable(node.getId(), unlockedNodes, nodeLevels)) {
            displayName = node.getColor() + node.getName() + ChatColor.YELLOW + " (Available)";
        } else {
            displayName = ChatColor.GRAY + node.getName() + ChatColor.RED + " (Locked)";
        }
        meta.setDisplayName(displayName);
        
        // Create lore
        List<String> lore = new ArrayList<>();
        
        // Add description - use level-specific description if available
        String description = unlocked ? node.getDescription(currentLevel) : node.getDescription();
        for (String line : description.split("\n")) {
            lore.add(ChatColor.GRAY + line);
        }
        
        lore.add("");
        
        // Add level/upgrade information if this is an upgradable node
        if (node.isUpgradable()) {
            lore.add(ChatColor.GRAY + "Level: " + ChatColor.YELLOW + currentLevel + "/" + node.getMaxLevel());
            
            if (unlocked) {
                if (!fullyUpgraded) {
                    lore.add(ChatColor.GREEN + "Next level: " + node.getDescription(currentLevel + 1));
                }
            } else {
                lore.add(ChatColor.GRAY + "Level 1: " + node.getDescription(1));
            }
            
            // If this is a special node that maintains progress through resets, indicate this clearly
            if (node.isSpecialNode()) {
                lore.add("");
                lore.add(ChatColor.GOLD + "Special Node: Maintains progress through resets");
                if (isSpecialReunlock) {
                    lore.add(ChatColor.YELLOW + "Saved Progress: Level " + savedLevel);
                    lore.add(ChatColor.GRAY + "Unlock to restore saved progress");
                }
                lore.add(ChatColor.RED + "Note: Resetting this node will lock it but preserve");
                lore.add(ChatColor.RED + "the skill tree and does not refund tokens.");
            }
        }
        
        // Add token tier requirement information - NEW SECTION
        SkillToken.TokenTier requiredTier = node.getRequiredTokenTier();
        int tokenCost = node.getTokenCost(currentLevel + 1);
        
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "▬▬ Token Requirements ▬▬");
        lore.add(requiredTier.getColor() + requiredTier.getSymbol() + " Requires " + 
                requiredTier.getDisplayName() + " Tier Tokens");
        
        if (!unlocked || !fullyUpgraded) {
            lore.add(ChatColor.YELLOW + "Cost: " + tokenCost + " " + requiredTier.getColor() + 
                    requiredTier.getDisplayName() + ChatColor.YELLOW + " Token" + 
                    (tokenCost > 1 ? "s" : ""));
            
            // Show player's token counts for this tier and higher
            String skillId = tree.getSkill().getId();
            Map<SkillToken.TokenTier, Integer> tokenCounts = treeData.getAllTokenCounts(skillId);
            
            lore.add("");
            lore.add(ChatColor.GRAY + "Your tokens:");
            for (SkillToken.TokenTier tier : SkillToken.TokenTier.values()) {
                if (tier.getLevel() >= requiredTier.getLevel()) {
                    int count = tokenCounts.getOrDefault(tier, 0);
                    ChatColor countColor = count >= tokenCost ? ChatColor.GREEN : ChatColor.RED;
                    lore.add("  " + tier.getColor() + tier.getSymbol() + " " + tier.getDisplayName() + 
                            ": " + countColor + count);
                }
            }
            
            // Check if player can afford this node
            boolean canAfford = treeData.canAffordNode(node, currentLevel);
            lore.add("");
            if (canAfford) {
                lore.add(ChatColor.GREEN + "✓ You can afford this upgrade!");
            } else {
                lore.add(ChatColor.RED + "✗ Not enough tokens of required tier");
            }
        }
        
        // Add prerequisite information
        if (!unlocked && !isRootNode) {
            List<String> prerequisites = getPrerequisiteNodes(tree, node.getId(), unlockedNodes, nodeLevels);
            if (!prerequisites.isEmpty()) {
                lore.add("");
                lore.add(ChatColor.RED + "Prerequisites:");
                for (String prereq : prerequisites) {
                    SkillTreeNode prereqNode = tree.getNode(prereq);
                    if (prereqNode != null) {
                        lore.add(ChatColor.RED + "• " + prereqNode.getName());
                    }
                }
            }
        }
        
        // Add click instructions
        lore.add("");
        if (unlocked && !fullyUpgraded) {
            lore.add(ChatColor.GREEN + "Click to upgrade!");
        } else if (!unlocked && (isRootNode || tree.isNodeAvailable(node.getId(), unlockedNodes, nodeLevels))) {
            lore.add(ChatColor.GREEN + "Click to unlock!");
        } else if (!unlocked) {
            lore.add(ChatColor.RED + "Must unlock prerequisites first");
        }
        
        // Add node ID for identification
        lore.add(ChatColor.BLACK + "ID:" + node.getId());
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Add info button with enhanced token tier explanation
     */
    private static void addInfoButton(Inventory gui, Skill skill, PlayerSkillTreeData treeData) {
        ItemStack infoItem = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = infoItem.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "✦ Skill Tree Guide ✦");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GOLD + "Three-Tier Token System:");
        lore.add("");
        
        // Explain the three tiers with enhanced descriptions
        lore.add(SkillToken.TokenTier.BASIC.getColor() + SkillToken.TokenTier.BASIC.getSymbol() + 
                " " + SkillToken.TokenTier.BASIC.getDisplayName() + " Tokens");
        lore.add(ChatColor.GRAY + "• Foundation skills and core abilities");
        lore.add(ChatColor.GRAY + "• Earned from levels 5-25 milestones");
        lore.add(ChatColor.GRAY + "• Most common and accessible");
        lore.add("");
        
        lore.add(SkillToken.TokenTier.ADVANCED.getColor() + SkillToken.TokenTier.ADVANCED.getSymbol() + 
                " " + SkillToken.TokenTier.ADVANCED.getDisplayName() + " Tokens");
        lore.add(ChatColor.GRAY + "• Specialized techniques and bonuses");
        lore.add(ChatColor.GRAY + "• Earned from levels 25-70 milestones");
        lore.add(ChatColor.GRAY + "• Can unlock Basic tier nodes too");
        lore.add("");
        
        lore.add(SkillToken.TokenTier.MASTER.getColor() + SkillToken.TokenTier.MASTER.getSymbol() + 
                " " + SkillToken.TokenTier.MASTER.getDisplayName() + " Tokens");
        lore.add(ChatColor.GRAY + "• Elite abilities and game-changers");
        lore.add(ChatColor.GRAY + "• Earned from levels 70+ milestones");
        lore.add(ChatColor.GRAY + "• Can unlock any tier nodes");
        lore.add("");
        
        lore.add(ChatColor.YELLOW + "How Token Usage Works:");
        lore.add(ChatColor.WHITE + "• Each node specifies required token tier");
        lore.add(ChatColor.WHITE + "• Higher tier tokens work on lower tier nodes");
        lore.add(ChatColor.WHITE + "• System uses optimal tokens automatically");
        lore.add(ChatColor.WHITE + "• Strategic planning maximizes efficiency");
        lore.add("");
        
        lore.add(ChatColor.AQUA + "Navigation & Controls:");
        lore.add(ChatColor.WHITE + "• Arrow buttons move view around tree");
        lore.add(ChatColor.WHITE + "• Click nodes to unlock/upgrade them");
        lore.add(ChatColor.WHITE + "• Gray connections show prerequisites");
        lore.add(ChatColor.WHITE + "• Glowing nodes are fully unlocked");
        
        // Show current token summary
        lore.add("");
        lore.add(ChatColor.GOLD + "Your Current Tokens:");
        Map<SkillToken.TokenTier, Integer> tokenCounts = treeData.getAllTokenCounts(skill.getId());
        int totalTokens = 0;
        
        for (SkillToken.TokenTier tier : SkillToken.TokenTier.values()) {
            int count = tokenCounts.getOrDefault(tier, 0);
            totalTokens += count;
            
            if (count > 0) {
                lore.add("  " + tier.getColor() + tier.getSymbol() + " " + count + " " + 
                        tier.getDisplayName() + ChatColor.WHITE + " (" + 
                        String.format("%.1f", (count * 100.0 / Math.max(1, totalTokens))) + "%)");
            } else {
                lore.add("  " + ChatColor.DARK_GRAY + tier.getSymbol() + " 0 " + 
                        tier.getDisplayName() + " (0.0%)");
            }
        }
        
        if (totalTokens == 0) {
            lore.add("");
            lore.add(ChatColor.YELLOW + "Earn tokens by leveling " + skill.getDisplayName() + "!");
            lore.add(ChatColor.GRAY + "Milestone levels (every 5) award tokens");
        }
        
        meta.setLore(lore);
        infoItem.setItemMeta(meta);
        gui.setItem(45, infoItem);
    }

    /**
     * Handle a node click in the skill tree
     */
    public static void handleNodeClick(Player player, ItemStack clickedItem) {
        if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasLore()) return;
        
        String nodeId = null;
        for (String lore : clickedItem.getItemMeta().getLore()) {
            if (lore.startsWith(ChatColor.BLACK + "ID:")) {
                nodeId = lore.substring((ChatColor.BLACK + "ID:").length());
                break;
            }
        }
        
        if (nodeId == null) return;
        
        // Get skill from GUI title
        String title = player.getOpenInventory().getTitle();
        if (!title.startsWith(GUI_TITLE_PREFIX)) return;
        
        String skillName = title.substring(GUI_TITLE_PREFIX.length());
        Skill skill = findSkillByDisplayName(skillName);
        if (skill == null) return;
        
        // Get player profile
        PlayerProfile profile = getPlayerProfile(player);
        if (profile == null) return;
        
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        SkillTree tree = SkillTreeRegistry.getInstance().getSkillTree(skill);
        if (tree == null) return;
        
        SkillTreeNode node = tree.getNode(nodeId);
        if (node == null) return;
        
        // Check if node is available
        Set<String> unlockedNodes = treeData.getUnlockedNodes(skill.getId());
        Map<String, Integer> nodeLevels = treeData.getNodeLevels(skill.getId());
        
        if (!tree.isNodeAvailable(nodeId, unlockedNodes, nodeLevels)) {
            player.sendMessage(ChatColor.RED + "You must unlock prerequisite nodes first!");
            return;
        }
        
        // Get current level and check if we can upgrade
        int currentLevel = nodeLevels.getOrDefault(nodeId, 0);
        int nextLevel = currentLevel + 1;
        
        if (nextLevel > node.getMaxLevel()) {
            player.sendMessage(ChatColor.YELLOW + "This node is already at maximum level!");
            return;
        }
        
        // Get token cost for the next level
        int tokenCost = node.getTokenCost(nextLevel);
        SkillToken.TokenTier requiredTier = node.getRequiredTokenTier();
        
        // Check if player has enough tokens
        if (!treeData.useTokens(skill.getId(), requiredTier, tokenCost)) {
            player.sendMessage(ChatColor.RED + "You need " + tokenCost + " " + 
                            requiredTier.getColor() + requiredTier.getDisplayName() + 
                            ChatColor.RED + " tokens to upgrade this node!");
            return;
        }
        
        // FIXED: Use setNodeLevel directly instead of upgradeNode to avoid double processing
        treeData.setNodeLevel(skill.getId(), nodeId, nextLevel);
        
        // Apply node benefits directly through the main skill
        if (skill instanceof com.server.profiles.skills.skills.mining.MiningSkill) {
            com.server.profiles.skills.skills.mining.MiningSkill miningSkill = 
                (com.server.profiles.skills.skills.mining.MiningSkill) skill;
            miningSkill.applyNodeUpgrade(player, nodeId, currentLevel, nextLevel);
        }
        // Add other main skills here as they are implemented
        
        // Play success sound
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);
        
        // Refresh the GUI
        openSkillTreeGUI(player, skill);
        
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("[SkillTreeGUI] Node " + nodeId + 
                                            " upgraded to level " + nextLevel + " for " + player.getName());
        }
    }

    /**
     * Smart token usage that tries to use tokens efficiently
     */
    private static boolean useTokensSmartly(PlayerSkillTreeData treeData, String skillId, 
                                        SkillToken.TokenTier minTier, int cost) {
        // Try to use tokens starting from the minimum required tier
        SkillToken.TokenTier[] tiersToTry = {
            SkillToken.TokenTier.BASIC,
            SkillToken.TokenTier.ADVANCED,
            SkillToken.TokenTier.MASTER
        };
        
        int remaining = cost;
        Map<SkillToken.TokenTier, Integer> toDeduct = new HashMap<>();
        
        // Plan the deduction, starting from the required tier
        for (SkillToken.TokenTier tier : tiersToTry) {
            if (tier.getLevel() < minTier.getLevel()) continue; // Skip lower tiers
            
            int available = treeData.getTokenCount(skillId, tier);
            int toUse = Math.min(remaining, available);
            
            if (toUse > 0) {
                toDeduct.put(tier, toUse);
                remaining -= toUse;
            }
            
            if (remaining <= 0) break;
        }
        
        // Check if we have enough tokens total
        if (remaining > 0) {
            return false;
        }
        
        // Actually deduct the tokens
        for (Map.Entry<SkillToken.TokenTier, Integer> entry : toDeduct.entrySet()) {
            if (!treeData.useTokens(skillId, entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Calculate how many tokens to refund for resetting a skill tree with tier distribution
     */
    private static Map<SkillToken.TokenTier, Integer> calculateTokensToRefund(SkillTree tree, 
                                                                            Set<String> unlockedNodes, 
                                                                            Map<String, Integer> nodeLevels) {
        Map<SkillToken.TokenTier, Integer> tokensToRefund = new HashMap<>();
        
        // Initialize counters
        for (SkillToken.TokenTier tier : SkillToken.TokenTier.values()) {
            tokensToRefund.put(tier, 0);
        }
        
        for (String nodeId : unlockedNodes) {
            SkillTreeNode node = tree.getNode(nodeId);
            if (node == null) continue;
            
            // Skip the root node and special nodes
            if (nodeId.equals("root") || node.isSpecialNode()) continue;
            
            int level = nodeLevels.getOrDefault(nodeId, 0);
            SkillToken.TokenTier requiredTier = node.getRequiredTokenTier();
            
            if (node.isUpgradable()) {
                // For upgradable nodes, add the cost of each level
                for (int i = 1; i <= level; i++) {
                    int currentRefund = tokensToRefund.getOrDefault(requiredTier, 0);
                    tokensToRefund.put(requiredTier, currentRefund + node.getTokenCost(i));
                }
            } else {
                // For non-upgradable nodes, just add the token cost
                int currentRefund = tokensToRefund.getOrDefault(requiredTier, 0);
                tokensToRefund.put(requiredTier, currentRefund + node.getTokenCost());
            }
        }
        
        return tokensToRefund;
    }

    /**
     * Handle the confirmation of a skill tree reset with tiered tokens
     */
    public static boolean handleResetConfirmation(Player player) {
        UUID playerId = player.getUniqueId();
        if (!resetConfirmations.containsKey(playerId)) {
            return false;
        }
        
        PlayerProfile profile = getPlayerProfile(player);
        if (profile == null) return false;
        
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        if (treeData == null) return false;
        
        // Get the reset data from the confirmation system
        String skillId = null;
        ResetData resetData = null;
        
        Map<String, ResetData> playerResets = resetConfirmations.get(playerId);
        for (Map.Entry<String, ResetData> entry : playerResets.entrySet()) {
            skillId = entry.getKey();
            resetData = entry.getValue();
            break; // Get the first (and should be only) entry
        }
        
        if (skillId == null || resetData == null) return false;
        
        Skill skill = SkillRegistry.getInstance().getSkill(skillId);
        if (skill == null) return false;
        
        // Perform the reset with tiered token refunds
        Map<SkillToken.TokenTier, Integer> refundedTokens = resetSkillTreeWithTierRefund(treeData, skillId);
        
        // Clear the reset confirmation
        resetConfirmations.remove(playerId);
        
        // Notify the player with detailed refund information
        player.sendMessage(ChatColor.GREEN + "Your " + ChatColor.GOLD + skill.getDisplayName() + 
                        ChatColor.GREEN + " skill tree has been reset.");
        
        int totalRefunded = 0;
        for (Map.Entry<SkillToken.TokenTier, Integer> entry : refundedTokens.entrySet()) {
            SkillToken.TokenTier tier = entry.getKey();
            int count = entry.getValue();
            
            if (count > 0) {
                totalRefunded += count;
                player.sendMessage(ChatColor.GREEN + "Refunded " + ChatColor.YELLOW + count + " " + 
                                tier.getColor() + tier.getDisplayName() + ChatColor.GREEN + " tokens");
            }
        }
        
        if (totalRefunded == 0) {
            player.sendMessage(ChatColor.YELLOW + "No tokens were refunded (no unlocked nodes found).");
        }
        
        // Open the skill tree again
        openSkillTreeGUI(player, skill);
        
        return true;
    }

    /**
     * Reset skill tree and return tiered token refunds
     */
    private static Map<SkillToken.TokenTier, Integer> resetSkillTreeWithTierRefund(PlayerSkillTreeData treeData, String skillId) {
        Map<SkillToken.TokenTier, Integer> refunds = new HashMap<>();
        
        // Initialize refund counters
        for (SkillToken.TokenTier tier : SkillToken.TokenTier.values()) {
            refunds.put(tier, 0);
        }
        
        // Get skill tree
        SkillTree tree = SkillTreeRegistry.getInstance().getSkillTree(skillId);
        if (tree == null) return refunds;
        
        // Get current progress
        Set<String> unlockedNodes = treeData.getUnlockedNodes(skillId);
        Map<String, Integer> nodeLevels = treeData.getNodeLevels(skillId);
        
        // Calculate what should be refunded by tier BEFORE resetting
        Map<SkillToken.TokenTier, Integer> calculatedRefunds = calculateTokensToRefund(tree, unlockedNodes, nodeLevels);
        
        // Perform the actual reset (this clears the nodes but doesn't handle tiered tokens properly)
        int totalTokensRefunded = treeData.resetSkillTree(skillId);
        
        // The resetSkillTree method adds tokens back using the old system (Basic tier only)
        // We need to remove those and add the correct tiered tokens instead
        
        // Remove the incorrectly added Basic tokens
        int basicTokensToRemove = treeData.getTokenCount(skillId, SkillToken.TokenTier.BASIC);
        if (basicTokensToRemove >= totalTokensRefunded) {
            treeData.setTokenCount(skillId, SkillToken.TokenTier.BASIC, basicTokensToRemove - totalTokensRefunded);
        }
        
        // Add the calculated tiered refunds to the player's tokens
        for (Map.Entry<SkillToken.TokenTier, Integer> entry : calculatedRefunds.entrySet()) {
            SkillToken.TokenTier tier = entry.getKey();
            int count = entry.getValue();
            
            if (count > 0) {
                treeData.addTokens(skillId, tier, count);
                refunds.put(tier, count);
            }
        }
        
        return refunds;
    }

        /**
         * Get prerequisite nodes that need to be unlocked first
         */
        private static List<String> getPrerequisiteNodes(SkillTree tree, String nodeId, 
                                                    Set<String> unlockedNodes, Map<String, Integer> nodeLevels) {
            List<String> prerequisites = new ArrayList<>();
            
            // Find all nodes that connect to this node
            for (Map.Entry<String, Set<String>> entry : tree.getAllConnections().entrySet()) {
                String fromNodeId = entry.getKey();
                Set<String> targetNodes = entry.getValue();
                
                if (targetNodes.contains(nodeId)) {
                    // This is a connecting node
                    SkillTreeNode prereqNode = tree.getNode(fromNodeId);
                    if (prereqNode != null) {
                        boolean isUnlocked = unlockedNodes.contains(fromNodeId);
                        int currentLevel = nodeLevels.getOrDefault(fromNodeId, 0);
                        int requiredLevel = tree.getMinLevelRequirement(fromNodeId, nodeId);
                        
                        if (!isUnlocked) {
                            // Node is not unlocked at all
                            prerequisites.add(prereqNode.getName() + " (Not Unlocked)");
                        } else if (currentLevel < requiredLevel) {
                            // Node is not at required level
                            prerequisites.add(prereqNode.getName() + " (Level " + currentLevel + "/" + requiredLevel + ")");
                        }
                    }
                }
            }
            
            return prerequisites;
        }

    
    /**
 * Create a navigation button
    */
    private static ItemStack createNavigationButton(Material material, String name, String direction) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + name);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Move the view " + direction);
        lore.add(ChatColor.BLACK + "NAVIGATION:" + direction);
        
        meta.setLore(lore);
        button.setItemMeta(meta);
        return button;
    }
    
    /**
     * Get the player's profile
     */
    private static PlayerProfile getPlayerProfile(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return null;
        
        return ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
    }

    /**
     * Handle a navigation button click
     */
    public static void handleNavigationClick(Player player, ItemStack clickedItem) {
        if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasLore()) return;
        
        String direction = null;
        for (String lore : clickedItem.getItemMeta().getLore()) {
            if (lore.startsWith(ChatColor.BLACK + "NAVIGATION:")) {
                direction = lore.substring((ChatColor.BLACK + "NAVIGATION:").length());
                break;
            }
        }
        
        if (direction == null) return;
        
        // Get current position
        TreeGridPosition currentPos = playerViewPositions.get(player);
        if (currentPos == null) {
            currentPos = new TreeGridPosition(0, 0);
        }
        
        // Calculate new position
        int newX = currentPos.getX();
        int newY = currentPos.getY();
        
        switch (direction.toLowerCase()) {
            case "north":
                newY--;
                break;
            case "south":
                newY++;
                break;
            case "west":
                newX--;
                break;
            case "east":
                newX++;
                break;
        }
        
        // Get skill from GUI title
        String title = player.getOpenInventory().getTitle();
        if (!title.startsWith(GUI_TITLE_PREFIX)) return;
        
        String skillName = title.substring(GUI_TITLE_PREFIX.length());
        Skill skill = findSkillByDisplayName(skillName);
        if (skill == null) return;
        
        // Open at new position
        openSkillTreeAtPosition(player, skill, newX, newY);
    }

    /**
     * Clear a player's view position
     */
    public static void clearPlayerViewPosition(Player player) {
        playerViewPositions.remove(player);
    }
    
   /**
     * Translate a grid position to an inventory slot
     */
    private static int translateGridToSlot(int gridX, int gridY) {
        // Ensure gridX and gridY are within valid ranges
        if (gridX < 0 || gridX >= GRID_SIZE || gridY < 0 || gridY >= GRID_SIZE) {
            return -1; // Invalid grid position
        }
        
        // Handle special edge slots
        if (gridX == 0) {
            // Left edge - slots 10, 19, 28, 37
            if (gridY >= 0 && gridY <= 3) {
                return 10 + (gridY * 9);
            }
        } else if (gridX == 6) {
            // Right edge - slots 16, 25, 34, 43
            if (gridY >= 0 && gridY <= 3) {
                return 16 + (gridY * 9);
            }
        }
        
        // Top row slots handling (11-15)
        if (gridY == 0 && gridX >= 1 && gridX <= 5) {
            return 11 + (gridX - 1); // Maps to slots 11, 12, 13, 14, 15
        }
        
        // Calculate normal grid slot
        int row = gridY + 1; // Add 1 to account for the top row
        int col = gridX + 1; // Add 1 to account for the left column
        
        // Translate to inventory slot number
        int slot = (row * 9) + col;
        
        // Special case: Always exclude slot 46 (bottom-left corner, coordinates 1,4)
        if (slot == 46) {
            return -1; // Slot 46 is reserved for border
        }
        
        return slot;
    }
    /**
     * Fill the GUI with skill tree nodes based on current view position
     */
    private static void fillSkillTreeNodes(Inventory gui, SkillTree tree, Set<String> unlockedNodes, 
                                        Map<String, Integer> nodeLevels, int centerX, int centerY, 
                                        PlayerSkillTreeData treeData) {
        // Convert grid positions to inventory slots for visible area
        Map<String, SkillTreeNode> allNodes = tree.getAllNodes();
        
        // Determine visible grid area
        int minX = centerX - VIEW_RADIUS;
        int maxX = centerX + VIEW_RADIUS;
        int minY = centerY - VIEW_RADIUS;
        int maxY = centerY + VIEW_RADIUS;
        
        // First, add all visible nodes
        for (int gridY = minY; gridY <= maxY; gridY++) {
            for (int gridX = minX; gridX <= maxX; gridX++) {
                // Translate grid position to inventory slot
                int slot = translateGridToSlot(gridX - minX, gridY - minY);
                
                // Skip invalid slots
                if (slot < 0 || slot >= 54) {
                    continue;
                }
                
                // Get node at this position
                SkillTreeNode node = tree.getNodeAtPosition(gridX, gridY);
                
                if (node != null) {
                    // Create item for this node
                    boolean unlocked = unlockedNodes.contains(node.getId());
                    ItemStack nodeItem = createNodeItem(node, unlocked, tree, unlockedNodes, nodeLevels, treeData);
                    gui.setItem(slot, nodeItem);
                }
            }
        }
        
        // Now add connection lines between visible nodes
        for (int gridY = minY; gridY <= maxY; gridY++) {
            for (int gridX = minX; gridX <= maxX; gridX++) {
                SkillTreeNode node = tree.getNodeAtPosition(gridX, gridY);
                
                if (node != null) {
                    // Add connection lines to adjacent nodes if they exist and are connected
                        addConnectionLines(gui, tree, node, unlockedNodes, gridX, gridY, minX, minY);
                }
            }
        }
        
        // Finally, check edge slots for connections to off-screen nodes
        checkEdgeConnections(gui, tree, unlockedNodes, nodeLevels, minX, minY, maxX, maxY);
    }

    /**
     * Check edge slots for connections to off-screen nodes
     */
    private static void checkEdgeConnections(Inventory gui, SkillTree tree, Set<String> unlockedNodes,
                                        Map<String, Integer> nodeLevels, int minX, int minY, int maxX, int maxY) {
        // Define all edge slot coordinates that need checking
        int[][] edgeSlots = {
            // Left edge: 10, 19, 28, 37
            {0, 0}, {0, 1}, {0, 2}, {0, 3},
            // Right edge: 16, 25, 34, 43
            {6, 0}, {6, 1}, {6, 2}, {6, 3},
            // Top edge: 11, 12, 13, 14, 15 (excluding corners already checked)
            {1, 0}, {2, 0}, {3, 0}, {4, 0}, {5, 0}
        };
        
        // Check each edge slot for potential connections to off-screen nodes
        for (int[] edgeCoord : edgeSlots) {
            int gridX = minX + edgeCoord[0];
            int gridY = minY + edgeCoord[1];
            
            // Find all connected nodes and determine the connection status
            ConnectionState connectionState = checkForOffScreenConnections(tree, unlockedNodes, nodeLevels, gridX, gridY);
            
            // If there's a potential connection but no node at this position, add a path indicator
            SkillTreeNode node = tree.getNodeAtPosition(gridX, gridY);
            if (connectionState != ConnectionState.NONE && node == null) {
                int slot = translateGridToSlot(edgeCoord[0], edgeCoord[1]);
                if (slot >= 0 && slot < 54 && (gui.getItem(slot) == null || 
                    (gui.getItem(slot).getType() == Material.BLACK_STAINED_GLASS_PANE))) {
                    
                    // Create a path indicator based on connection state
                    Material pathMaterial;
                    ChatColor pathColor;
                    String pathText;
                    
                    if (connectionState == ConnectionState.FULL) {
                        // Both connected nodes are unlocked
                        pathMaterial = Material.LIME_STAINED_GLASS_PANE;
                        pathColor = ChatColor.GREEN;
                        pathText = "Path to unlocked node";
                    } else {
                        // At least one connected node is available but not unlocked
                        pathMaterial = Material.YELLOW_STAINED_GLASS_PANE;
                        pathColor = ChatColor.YELLOW;
                        pathText = "Path to available node";
                    }
                    
                    ItemStack pathItem = new ItemStack(pathMaterial);
                    ItemMeta meta = pathItem.getItemMeta();
                    meta.setDisplayName(pathColor + "Path to Node");
                    List<String> lore = new ArrayList<>();
                    lore.add(pathColor + pathText);
                    lore.add(ChatColor.GRAY + "Continue in this direction");
                    lore.add(ChatColor.BLACK + "CONNECTION_LINE");
                    meta.setLore(lore);
                    pathItem.setItemMeta(meta);
                    
                    gui.setItem(slot, pathItem);
                }
            }
        }
    }

    /**
     * Check if a position has connections to unlocked nodes that might be off-screen
     */
    private static boolean checkForOffScreenConnections(SkillTree tree, Set<String> unlockedNodes, 
                                                    int gridX, int gridY) {
        // First, check all connections from nodes to this position
        for (Map.Entry<String, Set<String>> entry : tree.getAllConnections().entrySet()) {
            String sourceId = entry.getKey();
            SkillTreeNode sourceNode = tree.getNode(sourceId);
            
            if (sourceNode != null && unlockedNodes.contains(sourceId)) {
                TreeGridPosition sourcePos = sourceNode.getGridPosition();
                
                // For any target ID in this node's connections
                for (String targetId : entry.getValue()) {
                    SkillTreeNode targetNode = tree.getNode(targetId);
                    
                    if (targetNode != null) {
                        TreeGridPosition targetPos = targetNode.getGridPosition();
                        
                        // Check if this connection passes through our edge position
                        if (isOnPath(sourcePos.getX(), sourcePos.getY(), 
                                targetPos.getX(), targetPos.getY(), 
                                gridX, gridY)) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }

    /**
     * Check if a point is on the path between two other points (for straight horizontal/vertical lines only)
     */
    private static boolean isOnPath(int x1, int y1, int x2, int y2, int checkX, int checkY) {
        // Only handle straight lines (horizontal or vertical)
        if (x1 == x2) { // Vertical line
            return (checkX == x1) && (Math.min(y1, y2) <= checkY) && (checkY <= Math.max(y1, y2));
        } else if (y1 == y2) { // Horizontal line
            return (checkY == y1) && (Math.min(x1, x2) <= checkX) && (checkX <= Math.max(x1, x2));
        }
        
        return false; // Not on a straight line path
    }

    /**
     * Add connection lines between nodes
     */
    private static void addConnectionLines(Inventory gui, SkillTree tree, SkillTreeNode node, 
                                        Set<String> unlockedNodes, int gridX, int gridY, 
                                        int minX, int minY) {
        // Get connections from this node
        Set<String> connections = tree.getConnections(node.getId());
        boolean sourceUnlocked = unlockedNodes.contains(node.getId());
        
        for (String targetId : connections) {
            SkillTreeNode targetNode = tree.getNode(targetId);
            if (targetNode == null) continue;
            
            TreeGridPosition targetPos = targetNode.getGridPosition();
            int targetX = targetPos.getX();
            int targetY = targetPos.getY();
            
            // Check if target is in visible area
            if (targetX >= minX && targetX <= minX + 2 * VIEW_RADIUS && 
                targetY >= minY && targetY <= minY + 2 * VIEW_RADIUS) {
                
                // Calculate direction
                int dx = targetX - gridX;
                int dy = targetY - gridY;
                
                // Only create horizontal or vertical connections (no diagonals)
                if (dx == 0 || dy == 0) {
                    boolean targetUnlocked = unlockedNodes.contains(targetId);
                    
                    // For horizontal lines
                    if (dy == 0 && dx != 0) {
                        int steps = Math.abs(dx);
                        int stepDirection = Integer.signum(dx);
                        
                        // Add each path segment
                        for (int i = 1; i < steps; i++) {
                            addConnectionLine(gui, gridX, gridY, i * stepDirection, 0, 
                                            minX, minY, sourceUnlocked, targetUnlocked);
                        }
                    }
                    // For vertical lines
                    else if (dx == 0 && dy != 0) {
                        int steps = Math.abs(dy);
                        int stepDirection = Integer.signum(dy);
                        
                        // Add each path segment
                        for (int i = 1; i < steps; i++) {
                            addConnectionLine(gui, gridX, gridY, 0, i * stepDirection, 
                                            minX, minY, sourceUnlocked, targetUnlocked);
                        }
                    }
                }
            } else {
                // For off-screen target nodes, add a path indicator at the edge
                handleOffScreenConnection(gui, tree, node, targetNode, unlockedNodes, 
                                        gridX, gridY, targetX, targetY, minX, minY);
            }
        }
    }

    /**
     * Handle connections to nodes that are off-screen
     */
    private static void handleOffScreenConnection(Inventory gui, SkillTree tree, 
                                            SkillTreeNode sourceNode, SkillTreeNode targetNode,
                                            Set<String> unlockedNodes, 
                                            int gridX, int gridY, int targetX, int targetY,
                                            int minX, int minY) {
        boolean sourceUnlocked = unlockedNodes.contains(sourceNode.getId());
        boolean targetUnlocked = unlockedNodes.contains(targetNode.getId());
        
        // Only handle straight lines (horizontal or vertical)
        if (gridX == targetX || gridY == targetY) {
            int dx = targetX - gridX;
            int dy = targetY - gridY;
            
            // Calculate how far to draw the path within the visible area
            int visibleMaxX = minX + GRID_SIZE - 1;
            int visibleMaxY = minY + GRID_SIZE - 1;
            
            // Direction vectors
            int stepX = Integer.signum(dx);
            int stepY = Integer.signum(dy);
            
            // Maximum steps in each direction
            int stepsX = 0;
            int stepsY = 0;
            
            // Calculate how many steps we can take before hitting the edge
            if (stepX > 0) {
                stepsX = Math.min(visibleMaxX - gridX, Math.abs(dx));
            } else if (stepX < 0) {
                stepsX = Math.min(gridX - minX, Math.abs(dx));
            }
            
            if (stepY > 0) {
                stepsY = Math.min(visibleMaxY - gridY, Math.abs(dy));
            } else if (stepY < 0) {
                stepsY = Math.min(gridY - minY, Math.abs(dy));
            }
            
            // Draw the path to the edge
            if (dx != 0) { // Horizontal path
                for (int i = 1; i <= stepsX; i++) {
                    addConnectionLine(gui, gridX, gridY, i * stepX, 0, 
                                minX, minY, sourceUnlocked, targetUnlocked);
                }
            } else if (dy != 0) { // Vertical path
                for (int i = 1; i <= stepsY; i++) {
                    addConnectionLine(gui, gridX, gridY, 0, i * stepY, 
                                minX, minY, sourceUnlocked, targetUnlocked);
                }
            }
        }
    }
    
    /**
     * Get the current view position for a player
     */
    private static TreeGridPosition getCurrentViewPosition(Player player) {
        return playerViewPositions.getOrDefault(player, new TreeGridPosition(0, 0));
    }

    /**
     * Enum to represent the state of a connection
     */
    private enum ConnectionState {
        NONE,      // No connection
        PARTIAL,   // Connection exists but not all nodes are unlocked
        FULL       // Connection exists and all nodes are unlocked
    }

    /**
     * Check if a position has connections to unlocked nodes that might be off-screen
     */
    private static ConnectionState checkForOffScreenConnections(SkillTree tree, Set<String> unlockedNodes, 
                                                        Map<String, Integer> nodeLevels, int gridX, int gridY) {
        boolean hasConnection = false;
        boolean allUnlocked = true;
        
        // First check connections FROM THIS position TO off-screen nodes
        // This handles the case where parent nodes are on-screen and child nodes are off-screen
        SkillTreeNode sourceNode = tree.getNodeAtPosition(gridX, gridY);
        
        if (sourceNode != null) {
            boolean sourceUnlocked = unlockedNodes.contains(sourceNode.getId());
            
            // If the source node itself isn't unlocked, then all connections must be partial at best
            if (!sourceUnlocked) {
                Set<String> connections = tree.getConnections(sourceNode.getId());
                if (!connections.isEmpty()) {
                    for (String targetId : connections) {
                        SkillTreeNode targetNode = tree.getNode(targetId);
                        if (targetNode != null) {
                            TreeGridPosition targetPos = targetNode.getGridPosition();
                            // Check if this connection is to an off-screen node
                            if (isOffScreen(targetPos.getX(), targetPos.getY(), gridX, gridY)) {
                                hasConnection = true;
                                return ConnectionState.PARTIAL; // Source not unlocked = partial at best
                            }
                        }
                    }
                }
            }
            
            // Source node is unlocked, check connections to off-screen nodes
            if (sourceUnlocked) {
                Set<String> connections = tree.getConnections(sourceNode.getId());
                if (!connections.isEmpty()) {
                    for (String targetId : connections) {
                        SkillTreeNode targetNode = tree.getNode(targetId);
                        if (targetNode != null) {
                            TreeGridPosition targetPos = targetNode.getGridPosition();
                            // Check if this connection is to an off-screen node
                            if (isOffScreen(targetPos.getX(), targetPos.getY(), gridX, gridY)) {
                                hasConnection = true;
                                boolean targetUnlocked = unlockedNodes.contains(targetId);
                                if (!targetUnlocked) {
                                    allUnlocked = false;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Second, check connections TO this position FROM off-screen nodes
        // This handles the case where child nodes are on-screen and parent nodes are off-screen
        for (Map.Entry<String, Set<String>> entry : tree.getAllConnections().entrySet()) {
            String fromNodeId = entry.getKey();
            Set<String> toNodeIds = entry.getValue();
            
            SkillTreeNode fromNode = tree.getNode(fromNodeId);
            if (fromNode != null) {
                TreeGridPosition fromPos = fromNode.getGridPosition();
                
                // For any node that connects to this position
                for (String toNodeId : toNodeIds) {
                    SkillTreeNode toNode = tree.getNode(toNodeId);
                    if (toNode != null) {
                        TreeGridPosition toPos = toNode.getGridPosition();
                        
                        // Check if this is a connection that passes through our edge position
                        if (isOnPath(fromPos.getX(), fromPos.getY(), toPos.getX(), toPos.getY(), gridX, gridY)) {
                            hasConnection = true;
                            
                            boolean fromUnlocked = unlockedNodes.contains(fromNodeId);
                            boolean toUnlocked = unlockedNodes.contains(toNodeId);
                            
                            // Check connection level requirements
                            String connectionKey = fromNodeId + ":" + toNodeId;
                            int requiredLevel = tree.getMinLevelRequirement(fromNodeId, toNodeId);
                            int sourceLevel = nodeLevels.getOrDefault(fromNodeId, 0);
                            
                            boolean meetsLevelReq = fromUnlocked && (sourceLevel >= requiredLevel);
                            
                            // If either node is not unlocked or the connection doesn't meet level requirements,
                            // we can return early since it's a partial connection at best
                            if (!fromUnlocked || !toUnlocked || !meetsLevelReq) {
                                allUnlocked = false;
                            }
                        }
                    }
                }
            }
        }
        
        if (hasConnection) {
            return allUnlocked ? ConnectionState.FULL : ConnectionState.PARTIAL;
        } else {
            return ConnectionState.NONE;
        }
    }

    /**
     * Helper method to check if a position is off-screen relative to a center position
     */
    private static boolean isOffScreen(int x, int y, int centerX, int centerY) {
        // Define the screen bounds based on VIEW_RADIUS
        int minX = centerX - VIEW_RADIUS;
        int maxX = centerX + VIEW_RADIUS;
        int minY = centerY - VIEW_RADIUS;
        int maxY = centerY + VIEW_RADIUS;
        
        // Check if the position is outside these bounds
        return x < minX || x > maxX || y < minY || y > maxY;
    }

    /**
     * Handle reset button click with tiered token support
     */
    public static void handleResetClick(Player player, String skillName) {
        // Find the skill
        Skill skill = findSkillByDisplayName(skillName);
        if (skill == null) {
            player.sendMessage(ChatColor.RED + "Error: Skill not found.");
            return;
        }
        
        // Get player profile
        PlayerProfile profile = getPlayerProfile(player);
        if (profile == null) return;
        
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        if (treeData == null) return;
        
        // Get skill tree
        SkillTree tree = SkillTreeRegistry.getInstance().getSkillTree(skill);
        if (tree == null) {
            player.sendMessage(ChatColor.RED + "Error: Skill tree not found.");
            return;
        }
        
        // Calculate what tokens would be refunded
        Set<String> unlockedNodes = treeData.getUnlockedNodes(skill.getId());
        Map<String, Integer> nodeLevels = treeData.getNodeLevels(skill.getId());
        Map<SkillToken.TokenTier, Integer> tokensToRefund = calculateTokensToRefund(tree, unlockedNodes, nodeLevels);
        
        // Calculate total tokens to refund
        int totalTokensToRefund = 0;
        for (int count : tokensToRefund.values()) {
            totalTokensToRefund += count;
        }
        
        if (totalTokensToRefund == 0) {
            player.sendMessage(ChatColor.YELLOW + "No tokens to refund - no unlocked nodes found.");
            return;
        }
        
        // Open confirmation GUI
        ConfirmationGUI.openResetConfirmationGUI(player, skill);
}

    /**
     * Find a skill by its display name
     */
    private static Skill findSkillByDisplayName(String displayName) {
        for (Skill skill : SkillRegistry.getInstance().getAllSkills()) {
            if (skill.getDisplayName().equals(displayName)) {
                return skill;
            }
        }
        return null;
    }

    // Store data for skill tree reset confirmations
    private static final Map<UUID, Map<String, ResetData>> resetConfirmations = new HashMap<>();

    /**
     * Class to store data for skill tree reset
     */
    private static class ResetData {
        private final String skillId;
        private final int tokensToRefund;
        
        public ResetData(String skillId, int tokensToRefund) {
            this.skillId = skillId;
            this.tokensToRefund = tokensToRefund;
        }
        
        public String getSkillId() {
            return skillId;
        }
        
        public int getTokensToRefund() {
            return tokensToRefund;
        }
    }

    /**
     * Handle the cancellation of a skill tree reset
     */
    public static boolean handleResetCancellation(Player player) {
        if (!resetConfirmations.containsKey(player.getUniqueId()) || 
            !resetConfirmations.get(player.getUniqueId()).containsKey("skill_tree_reset")) {
            player.sendMessage(ChatColor.RED + "No pending skill tree reset confirmation.");
            return false;
        }
        
        resetConfirmations.get(player.getUniqueId()).remove("skill_tree_reset");
        if (resetConfirmations.get(player.getUniqueId()).isEmpty()) {
            resetConfirmations.remove(player.getUniqueId());
        }
        
        player.sendMessage(ChatColor.GREEN + "Skill tree reset cancelled.");
        
        return true;
    }

}