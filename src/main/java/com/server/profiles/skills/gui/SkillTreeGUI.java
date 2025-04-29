package com.server.profiles.skills.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
        if (skill == null) {
            player.sendMessage(ChatColor.RED + "Invalid skill!");
            return;
        }
        
        // Safety check - make sure the SkillTreeRegistry is initialized
        if (SkillTreeRegistry.getInstance() == null) {
            player.sendMessage(ChatColor.RED + "Skill tree system not available yet. Please try again later.");
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().severe("SkillTreeRegistry is null! This should never happen.");
            }
            return;
        }
        
        // Reset view position for this player to be centered on the root node
        playerViewPositions.put(player, new TreeGridPosition(0, 0));
        
        // Debug message
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("Opening skill tree for " + player.getName() + ": " + skill.getDisplayName());
        }
        
        // Open the GUI at the current view position
        openSkillTreeAtPosition(player, skill, 0, 0);
    }
    
    /**
     * Open the skill tree GUI at a specific position
     */
    public static void openSkillTreeAtPosition(Player player, Skill skill, int centerX, int centerY) {
        // Get the skill tree
        SkillTree tree = SkillTreeRegistry.getInstance().getSkillTree(skill);
        if (tree == null) {
            player.sendMessage(ChatColor.RED + "No skill tree found for " + skill.getDisplayName());
            return;
        }
        
        // Set player's current view position
        playerViewPositions.put(player, new TreeGridPosition(centerX, centerY));
        
        // Get player profile and skill tree data
        PlayerProfile profile = getPlayerProfile(player);
        if (profile == null) return;
        
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        Set<String> unlockedNodes = treeData.getUnlockedNodes(skill.getId());
        int tokenCount = treeData.getTokenCount(skill.getId());
        
        // Create the inventory
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE_PREFIX + skill.getDisplayName());
        
        // Add skill tree nodes
        fillSkillTreeNodes(gui, tree, unlockedNodes, centerX, centerY);
        
        // Add navigation buttons
        addNavigationButtons(gui, skill, tokenCount);
        
        // Add info button
        addInfoButton(gui, skill, tokenCount);
        
        // Fill empty slots
        fillEmptySlots(gui);
        
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
     * Add navigation buttons to the GUI
     */
    private static void addNavigationButtons(Inventory gui, Skill skill, int tokenCount) {
        // Add back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back to Skills");
        backButton.setItemMeta(backMeta);
        gui.setItem(45, backButton);
        
        // Add navigation buttons in a more intuitive layout around the token display
        // Move Up button to the left of Move Left button (position 47 instead of 40)
        gui.setItem(47, createNavigationButton(Material.SPECTRAL_ARROW, "Move Up", "north"));
        
        // Left and right buttons on either side of the token display
        gui.setItem(48, createNavigationButton(Material.SPECTRAL_ARROW, "Move Left", "west"));
        gui.setItem(50, createNavigationButton(Material.SPECTRAL_ARROW, "Move Right", "east"));
        
        // Put the down button at position 51 (to the right of the token)
        gui.setItem(51, createNavigationButton(Material.SPECTRAL_ARROW, "Move Down", "south"));
        
        // Add token display in the center bottom
        SkillToken.TokenInfo tokenInfo = SkillToken.getTokenInfo(skill);
        ItemStack tokenDisplay = new ItemStack(tokenInfo.material);
        ItemMeta tokenMeta = tokenDisplay.getItemMeta();
        tokenMeta.setDisplayName(tokenInfo.color + tokenInfo.displayName + " Tokens: " + 
                            ChatColor.WHITE + tokenCount);
        List<String> tokenLore = new ArrayList<>();
        tokenLore.add(ChatColor.GRAY + "These tokens are used to unlock");
        tokenLore.add(ChatColor.GRAY + "nodes in the " + tokenInfo.color + skill.getDisplayName() + 
                    ChatColor.GRAY + " skill tree.");
        tokenLore.add("");
        tokenLore.add(ChatColor.YELLOW + "You earn tokens by leveling up this skill.");
        tokenMeta.setLore(tokenLore);
        tokenDisplay.setItemMeta(tokenMeta);
        gui.setItem(49, tokenDisplay);
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
                                        SkillTree tree, Set<String> unlockedNodes) {
        Material icon = node.getIcon();
        
        // Change appearance based on node state
        if (unlocked) {
            // Unlocked node - use actual icon
        } else if (tree.isNodeAvailable(node.getId(), unlockedNodes)) {
            // Available but not unlocked - use glowing effect or different color
            if (icon == Material.DIAMOND_PICKAXE) icon = Material.IRON_PICKAXE;
            else if (icon == Material.GOLDEN_PICKAXE) icon = Material.WOODEN_PICKAXE;
            else if (icon == Material.NETHERITE_PICKAXE) icon = Material.STONE_PICKAXE;
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
            displayName = node.getColor() + node.getName() + ChatColor.GREEN + " ✓";
        } else if (tree.isNodeAvailable(node.getId(), unlockedNodes)) {
            displayName = node.getColor() + node.getName() + ChatColor.YELLOW + " (Available)";
        } else {
            displayName = ChatColor.GRAY + node.getName() + ChatColor.RED + " (Locked)";
        }
        meta.setDisplayName(displayName);
        
        // Create lore
        List<String> lore = new ArrayList<>();
        
        // Add description
        for (String line : node.getDescription().split("\n")) {
            lore.add(ChatColor.GRAY + line);
        }
        
        lore.add("");
        
        // Add token cost
        if (node.getTokenCost() > 0) {
            if (unlocked) {
                lore.add(ChatColor.GREEN + "Unlocked!");
            } else if (tree.isNodeAvailable(node.getId(), unlockedNodes)) {
                lore.add(ChatColor.YELLOW + "Cost: " + node.getTokenCost() + " Token" + 
                    (node.getTokenCost() > 1 ? "s" : ""));
                lore.add(ChatColor.YELLOW + "Click to unlock!");
            } else {
                lore.add(ChatColor.RED + "Locked - Unlock connected nodes first");
                
                // Add prerequisite nodes information
                List<String> prerequisites = getPrerequisiteNodes(tree, node.getId(), unlockedNodes);
                if (!prerequisites.isEmpty()) {
                    lore.add("");
                    lore.add(ChatColor.RED + "Required nodes:");
                    for (String prereq : prerequisites) {
                        lore.add(ChatColor.RED + "• " + ChatColor.GRAY + prereq);
                    }
                }
            }
        } else {
            // Root node
            lore.add(ChatColor.GREEN + "Core Node (Free)");
        }
        
        // Add ID for later retrieval
        lore.add(ChatColor.BLACK + "ID:" + node.getId());
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }

    /**
     * Get prerequisite nodes that need to be unlocked first
     */
    private static List<String> getPrerequisiteNodes(SkillTree tree, String nodeId, Set<String> unlockedNodes) {
        List<String> prerequisites = new ArrayList<>();
        
        // Find all nodes that connect to this node
        for (Map.Entry<String, Set<String>> entry : tree.getAllConnections().entrySet()) {
            String fromNodeId = entry.getKey();
            Set<String> targetNodes = entry.getValue();
            
            if (targetNodes.contains(nodeId) && !unlockedNodes.contains(fromNodeId)) {
                // This is a prerequisite node that's not unlocked
                SkillTreeNode prereqNode = tree.getNode(fromNodeId);
                if (prereqNode != null) {
                    prerequisites.add(prereqNode.getName());
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
        
        // Add direction data as hidden lore
        meta.setLore(Arrays.asList(ChatColor.BLACK + "DIRECTION:" + direction));
        
        button.setItemMeta(meta);
        return button;
    }
    
    /**
     * Get the player's profile
     */
    private static PlayerProfile getPlayerProfile(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) {
            player.sendMessage(ChatColor.RED + "You need to select a profile first!");
            return null;
        }
        
        return ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
    }
    
    /**
     * Handle a node click in the skill tree
     */
    public static void handleNodeClick(Player player, ItemStack clickedItem) {
        // Get the node ID from the item lore
        List<String> lore = clickedItem.getItemMeta().getLore();
        String nodeId = null;
        
        for (String line : lore) {
            if (line.startsWith(ChatColor.BLACK + "ID:")) {
                nodeId = line.substring((ChatColor.BLACK + "ID:").length());
                break;
            }
        }
        
        if (nodeId == null) return;
        
        // Get the skill from the GUI title
        String title = player.getOpenInventory().getTitle();
        String skillName = title.substring(GUI_TITLE_PREFIX.length());
        Skill skill = findSkillByDisplayName(skillName);
        
        if (skill == null) {
            player.sendMessage(ChatColor.RED + "Error finding skill: " + skillName);
            return;
        }
        
        // Get the player profile and skill tree data
        PlayerProfile profile = getPlayerProfile(player);
        if (profile == null) return;
        
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        SkillTree tree = SkillTreeRegistry.getInstance().getSkillTree(skill);
        
        // Check if already unlocked
        if (treeData.isNodeUnlocked(skill.getId(), nodeId)) {
            player.sendMessage(ChatColor.YELLOW + "This node is already unlocked.");
            return;
        }
        
        // Check if available to unlock
        Set<String> unlockedNodes = treeData.getUnlockedNodes(skill.getId());
        if (!tree.isNodeAvailable(nodeId, unlockedNodes)) {
            player.sendMessage(ChatColor.RED + "You need to unlock connected nodes first.");
            return;
        }
        
        // Get the node and check token cost
        SkillTreeNode node = tree.getNode(nodeId);
        int tokenCost = node.getTokenCost();
        int tokenCount = treeData.getTokenCount(skill.getId());
        
        if (tokenCount < tokenCost) {
            player.sendMessage(ChatColor.RED + "You don't have enough tokens. Required: " + tokenCost);
            return;
        }
        
        // Unlock the node
        treeData.unlockNode(skill.getId(), nodeId);
        treeData.useTokens(skill.getId(), tokenCost);
        
        // Send message
        SkillToken.TokenInfo tokenInfo = SkillToken.getTokenInfo(skill);
        player.sendMessage(ChatColor.GREEN + "Unlocked " + node.getColor() + node.getName() + 
                        ChatColor.GREEN + " for " + tokenCost + " " + tokenInfo.color + 
                        tokenInfo.displayName + (tokenCost > 1 ? "s" : "") + ChatColor.GREEN + "!");
        
        // Play sound
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        
        // Refresh the GUI
        TreeGridPosition currentView = playerViewPositions.get(player);
        openSkillTreeAtPosition(player, skill, currentView.getX(), currentView.getY());
    }
    
    /**
     * Handle a navigation button click
     */
    public static void handleNavigationClick(Player player, ItemStack clickedItem) {
        // Get the direction from the item lore
        List<String> lore = clickedItem.getItemMeta().getLore();
        String direction = null;
        
        for (String line : lore) {
            if (line.startsWith(ChatColor.BLACK + "DIRECTION:")) {
                direction = line.substring((ChatColor.BLACK + "DIRECTION:").length());
                break;
            }
        }
        
        if (direction == null) return;
        
        // Get the skill from the GUI title
        String title = player.getOpenInventory().getTitle();
        String skillName = title.substring(GUI_TITLE_PREFIX.length());
        Skill skill = findSkillByDisplayName(skillName);
        
        if (skill == null) {
            player.sendMessage(ChatColor.RED + "Error finding skill: " + skillName);
            return;
        }
        
        // Get current view position
        TreeGridPosition currentPos = playerViewPositions.get(player);
        if (currentPos == null) {
            currentPos = new TreeGridPosition(0, 0);
        }
        
        // Calculate new position based on direction
        int newX = currentPos.getX();
        int newY = currentPos.getY();
        
        switch (direction) {
            case "north":
                newY -= 1;
                break;
            case "south":
                newY += 1;
                break;
            case "east":
                newX += 1;
                break;
            case "west":
                newX -= 1;
                break;
            // Could add diagonal directions and zoom in/out later
        }
        
        // Open the GUI at the new position
        openSkillTreeAtPosition(player, skill, newX, newY);
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
        
        // Standard grid slots (middle columns)
        int row = gridY + 1; // Add 1 to account for the top row
        int col = gridX + 1; // Add 1 to account for the left column
        
        // Convert to inventory slot
        int slot = (row * 9) + col;
        
        // Validate slot is in bounds
        if (slot < 0 || slot >= 54) {
            return -1;
        }
        
        return slot;
    }
    /**
     * Fill the GUI with skill tree nodes based on current view position
     */
    private static void fillSkillTreeNodes(Inventory gui, SkillTree tree, Set<String> unlockedNodes, 
                                        int centerX, int centerY) {
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
                    ItemStack nodeItem = createNodeItem(node, unlocked, tree, unlockedNodes);
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
        checkEdgeConnections(gui, tree, unlockedNodes, minX, minY, maxX, maxY);
    }

    /**
     * Check edge slots for connections to off-screen nodes
     */
    private static void checkEdgeConnections(Inventory gui, SkillTree tree, Set<String> unlockedNodes,
                                        int minX, int minY, int maxX, int maxY) {
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
            
            // Find all nodes that connect to or from this position (on-screen or off-screen)
            boolean hasYellowConnection = checkForOffScreenConnections(tree, unlockedNodes, gridX, gridY);
            
            // If there's a potential connection but no node at this position, add a path indicator
            SkillTreeNode node = tree.getNodeAtPosition(gridX, gridY);
            if (hasYellowConnection && node == null) {
                int slot = translateGridToSlot(edgeCoord[0], edgeCoord[1]);
                if (slot >= 0 && slot < 54 && (gui.getItem(slot) == null || 
                    (gui.getItem(slot).getType() == Material.BLACK_STAINED_GLASS_PANE))) {
                    
                    // Create a yellow path indicator for this edge position
                    ItemStack pathItem = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
                    ItemMeta meta = pathItem.getItemMeta();
                    meta.setDisplayName(ChatColor.YELLOW + "Path to Node");
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.YELLOW + "Path to an available node");
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
}