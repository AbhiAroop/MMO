package com.server.islands.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.islands.data.ChallengeCategoryTree;
import com.server.islands.data.IslandChallenge;
import com.server.islands.data.TreeGridPosition;
import com.server.islands.managers.ChallengeManager;
import com.server.islands.managers.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * GUI for displaying challenge trees in a grid-based layout
 * Similar to SkillTreeGUI but for island challenges
 */
public class ChallengeCategoryTreeGUI {
    
    // Size of the grid and translation to inventory slots
    // Vertical progression: bottom to top (like Heart of the Mountain)
    private static final int GRID_WIDTH = 5;  // 5 columns wide (reduced for simpler layout)
    private static final int GRID_HEIGHT = 5; // 5 rows tall (fits in inventory rows 1-5)
    private static final int VIEW_RADIUS_X = 2; // 2 tiles left/right from center
    private static final int VIEW_RADIUS_Y = 2; // 2 tiles up/down from center
    
    // Starting position for all trees (bottom center)
    private static final int START_X = 0; // Center column
    private static final int START_Y = -2; // Bottom row (will be at row 4 when centered at 0,0)
    
    // Title prefix for the GUI
    public static final String GUI_TITLE_PREFIX = "§6§lChallenges: ";
    
    // Map to track current view positions for players
    private static final Map<Player, TreeGridPosition> playerViewPositions = new HashMap<>();
    
    /**
     * Open the challenge tree GUI for a category
     */
    public static void openChallengeTreeGUI(Player player, IslandChallenge.ChallengeCategory category,
                                           ChallengeManager challengeManager, IslandManager islandManager) {
        // Start with center at Y=-1, which will show Y from -3 to +1
        // This puts Y=-2 at row 4 (slot 40 for X=0) - the bottom center position we want
        // Row 1 (top) = Y+1, Row 2 = Y0, Row 3 = Y-1, Row 4 = Y-2 (starter at slot 40), Row 5 = Y-3
        openChallengeTreeAtPosition(player, category, 0, -1, challengeManager, islandManager);
    }
    
    /**
     * Open the challenge tree GUI at a specific position
     */
    public static void openChallengeTreeAtPosition(Player player, IslandChallenge.ChallengeCategory category,
                                                   int centerX, int centerY, ChallengeManager challengeManager,
                                                   IslandManager islandManager) {
        // Get the challenge tree for this category
        ChallengeCategoryTree tree = challengeManager.getChallengeTree(category);
        if (tree == null) {
            player.sendMessage(Component.text("✗ No challenges found for this category!", NamedTextColor.RED));
            return;
        }
        
        // Get player's island and completed challenges
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("✗ You don't have an island!", NamedTextColor.RED));
                return;
            }
            
            // Get completed challenges for this island
            challengeManager.getCompletedChallenges(islandId).thenAccept(completedChallenges -> {
                Bukkit.getScheduler().runTask(challengeManager.getPlugin(), () -> {
                    // Create inventory
                    String title = GUI_TITLE_PREFIX + category.getIcon() + " " + category.getDisplayName();
                    Inventory gui = Bukkit.createInventory(null, 54, title);
                    
                    // Store current view position
                    playerViewPositions.put(player, new TreeGridPosition(centerX, centerY));
                    
                    // Add colorful borders around the GUI FIRST
                    addBorders(gui);
                    
                    // Fill remaining empty slots with black glass decoration
                    fillEmptySlots(gui);
                    
                    // Add navigation buttons (will overwrite slots at specific positions)
                    addNavigationButtons(gui, category);
                    
                    // Fill with challenge tree nodes in the inner area
                    fillChallengeTreeNodes(gui, tree, completedChallenges, centerX, centerY, challengeManager, player, islandId);
                    
                    // Open the inventory immediately (items will populate as they load)
                    player.openInventory(gui);
                });
            });
        });
    }
    
    /**
     * Fill the GUI with challenge nodes
     */
    private static void fillChallengeTreeNodes(Inventory gui, ChallengeCategoryTree tree, Set<String> completedChallenges,
                                              int centerX, int centerY, ChallengeManager challengeManager,
                                              Player player, UUID islandId) {
        // Determine visible grid area (5 wide, 5 tall)
        int minX = centerX - VIEW_RADIUS_X;
        int maxX = centerX + VIEW_RADIUS_X;
        int minY = centerY - VIEW_RADIUS_Y;
        int maxY = centerY + VIEW_RADIUS_Y;
        
        // First, add all visible nodes SYNCHRONOUSLY (no async loading)
        for (int gridY = minY; gridY <= maxY; gridY++) {
            for (int gridX = minX; gridX <= maxX; gridX++) {
                // Translate grid position to inventory slot
                // Invert Y so higher Y values appear at top of screen
                int slot = translateGridToSlot(gridX - minX, maxY - gridY);
                
                // Skip invalid slots
                if (slot < 0 || slot >= 54) {
                    continue;
                }
                
                // Get challenge at this position
                IslandChallenge challenge = tree.getChallengeAtPosition(gridX, gridY);
                
                if (challenge != null) {
                    // DEBUG: Log challenge placement
                    System.out.println("[DEBUG] Placing challenge '" + challenge.getId() + "' at grid(" + gridX + "," + gridY + ") -> slot " + slot);
                    
                    // Create item for this challenge SYNCHRONOUSLY with 0 progress
                    boolean completed = completedChallenges.contains(challenge.getId());
                    boolean available = tree.arePrerequisitesMet(challenge.getId(), completedChallenges);
                    
                    // Create item without progress (we'll load progress later if needed)
                    ItemStack nodeItem = createChallengeItem(challenge, completed, available, 0, tree, completedChallenges);
                    gui.setItem(slot, nodeItem);
                    System.out.println("[DEBUG] Set item for '" + challenge.getId() + "' at slot " + slot + " to " + nodeItem.getType());
                }
            }
        }
        
        // TODO: Add connection lines between visible nodes
        // Currently disabled because they're drawn before async challenge nodes load
        /*
        for (int gridY = minY; gridY <= maxY; gridY++) {
            for (int gridX = minX; gridX <= maxX; gridX++) {
                IslandChallenge challenge = tree.getChallengeAtPosition(gridX, gridY);
                
                if (challenge != null) {
                    addConnectionLines(gui, tree, challenge, completedChallenges, gridX, gridY, minX, maxY);
                }
            }
        }
        */
    }
    
    /**
     * Create an item stack for a challenge node
     */
    private static ItemStack createChallengeItem(IslandChallenge challenge, boolean completed, boolean available,
                                                 int currentProgress, ChallengeCategoryTree tree, Set<String> completedChallenges) {
        // Determine material and color based on state
        Material material;
        NamedTextColor nameColor;
        String statePrefix;
        
        if (completed) {
            material = Material.LIME_STAINED_GLASS;
            nameColor = NamedTextColor.GREEN;
            statePrefix = "§a§l✔ ";
        } else if (available) {
            material = Material.YELLOW_STAINED_GLASS;
            nameColor = NamedTextColor.YELLOW;
            statePrefix = "§e§l➤ ";
        } else {
            material = Material.GRAY_STAINED_GLASS;
            nameColor = NamedTextColor.GRAY;
            statePrefix = "§7§l🔒 ";
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name with state prefix
        meta.displayName(Component.text(statePrefix + challenge.getName(), nameColor, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        // Build lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        
        // Description
        lore.add(Component.text(challenge.getDescription(), NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        
        // Progress bar if not completed and available
        if (!completed && available && currentProgress > 0) {
            int targetAmount = challenge.getTargetAmount();
            double percentage = Math.min(1.0, (double) currentProgress / targetAmount);
            lore.add(createProgressBar(percentage, currentProgress, targetAmount));
            lore.add(Component.empty());
        }
        
        // Difficulty and rewards
        lore.add(Component.text("Difficulty: ", NamedTextColor.GRAY)
            .append(Component.text(challenge.getDifficulty().getDisplayName() + " " + challenge.getDifficulty().getIcon(), NamedTextColor.GOLD))
            .decoration(TextDecoration.ITALIC, false));
        
        lore.add(Component.text("Reward: ", NamedTextColor.GRAY)
            .append(Component.text("+" + challenge.getTokenReward() + " ⭐ Tokens", NamedTextColor.AQUA))
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        
        // Prerequisites
        if (challenge.hasPrerequisites()) {
            lore.add(Component.text("Prerequisites:", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
            for (String prereqId : challenge.getPrerequisites()) {
                IslandChallenge prereq = tree.getChallenge(prereqId);
                if (prereq != null) {
                    boolean prereqCompleted = completedChallenges.contains(prereqId);
                    NamedTextColor prereqColor = prereqCompleted ? NamedTextColor.GREEN : NamedTextColor.RED;
                    String prereqPrefix = prereqCompleted ? "§a✔ " : "§c✗ ";
                    lore.add(Component.text("  " + prereqPrefix + prereq.getName(), prereqColor)
                        .decoration(TextDecoration.ITALIC, false));
                }
            }
            lore.add(Component.empty());
        }
        
        // Status-specific footer
        if (completed) {
            lore.add(Component.text("§a§l✔ COMPLETED", NamedTextColor.GREEN, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        } else if (available) {
            lore.add(Component.text("§e§lClick to view details!", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("§7§l🔒 Locked - Complete prerequisites first", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        }
        
        // Hidden ID for click handling
        lore.add(Component.text("§8ID: " + challenge.getId(), NamedTextColor.DARK_GRAY)
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create a progress bar component
     */
    private static Component createProgressBar(double percentage, int current, int target) {
        int totalBars = 20;
        int filledBars = (int) (percentage * totalBars);
        
        StringBuilder bar = new StringBuilder("§7[");
        for (int i = 0; i < totalBars; i++) {
            if (i < filledBars) {
                bar.append("§a▰");
            } else {
                bar.append("§7▱");
            }
        }
        bar.append("§7] §e").append(current).append("§7/§e").append(target);
        
        return Component.text(bar.toString())
            .decoration(TextDecoration.ITALIC, false);
    }
    
    /**
     * Add connection lines between challenges
     */
    private static void addConnectionLines(Inventory gui, ChallengeCategoryTree tree, IslandChallenge sourceChallenge,
                                          Set<String> completedChallenges, int sourceX, int sourceY, int minX, int maxY) {
        boolean sourceCompleted = completedChallenges.contains(sourceChallenge.getId());
        Set<String> connectedIds = tree.getConnectedChallenges(sourceChallenge.getId());
        
        for (String targetId : connectedIds) {
            IslandChallenge targetChallenge = tree.getChallenge(targetId);
            if (targetChallenge == null) continue;
            
            TreeGridPosition targetPos = targetChallenge.getGridPosition();
            boolean targetCompleted = completedChallenges.contains(targetId);
            
            // Draw line from source to target
            drawConnectionLine(gui, sourceX, sourceY, targetPos.getX(), targetPos.getY(),
                             minX, maxY, sourceCompleted, targetCompleted);
        }
    }
    
    /**
     * Draw a connection line between two points
     */
    private static void drawConnectionLine(Inventory gui, int x1, int y1, int x2, int y2,
                                          int minX, int maxY, boolean sourceCompleted, boolean targetCompleted) {
        // Simple implementation: draw horizontal then vertical lines
        // Horizontal first
        int startX = Math.min(x1, x2);
        int endX = Math.max(x1, x2);
        for (int x = startX + 1; x < endX; x++) {
            addConnectionPiece(gui, x, y1, minX, maxY, sourceCompleted, targetCompleted);
        }
        
        // Then vertical
        int startY = Math.min(y1, y2);
        int endY = Math.max(y1, y2);
        for (int y = startY + 1; y < endY; y++) {
            addConnectionPiece(gui, x2, y, minX, maxY, sourceCompleted, targetCompleted);
        }
    }
    
    /**
     * Add a single connection line piece
     */
    private static void addConnectionPiece(Inventory gui, int x, int y, int minX, int maxY,
                                          boolean sourceCompleted, boolean targetCompleted) {
        int slot = translateGridToSlot(x - minX, maxY - y);
        
        // Skip if out of bounds or slot has a challenge node
        if (slot < 0 || slot >= 54) return;
        ItemStack existing = gui.getItem(slot);
        if (existing != null && existing.getType() != Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }
        
        // Determine line color
        Material lineMaterial;
        if (sourceCompleted && targetCompleted) {
            lineMaterial = Material.LIME_STAINED_GLASS_PANE;
        } else if (sourceCompleted || targetCompleted) {
            lineMaterial = Material.YELLOW_STAINED_GLASS_PANE;
        } else {
            lineMaterial = Material.GRAY_STAINED_GLASS_PANE;
        }
        
        ItemStack lineItem = new ItemStack(lineMaterial);
        ItemMeta meta = lineItem.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        lineItem.setItemMeta(meta);
        
        gui.setItem(slot, lineItem);
    }
    
    /**
     * Translate grid coordinates to inventory slot
     * Maps a 5x5 grid to the center area of the inventory
     * The view shows Y values from (centerY - 2) to (centerY + 2)
     * 
     * For the default view (centerY = 0):
     * - Row 1 (slots 9-17) shows Y = +2 (top of tree)
     * - Row 2 (slots 18-26) shows Y = +1
     * - Row 3 (slots 27-35) shows Y = 0
     * - Row 4 (slots 36-44) shows Y = -1
     * - Row 5 (slots 45-53) is reserved for borders and navigation
     */
    private static int translateGridToSlot(int gridX, int gridY) {
        // Validate grid coordinates (gridX/gridY are relative offsets 0-4)
        if (gridX < 0 || gridX >= GRID_WIDTH || gridY < 0 || gridY >= GRID_HEIGHT) {
            return -1;
        }
        
        // Map to inventory: 
        // gridY 0 (highest Y in view) -> row 1 (slots 9-17)
        // gridY 3 (lowest Y in view) -> row 4 (slots 36-44)
        // gridY 4 is not rendered (would be row 5 which is reserved for borders/navigation)
        // gridX 0 (leftmost) -> column 2 (slots x*9+2)
        // gridX 4 (rightmost) -> column 6 (slots x*9+6)
        // This centers the 5-column tree in the 9-column inventory
        
        int row = gridY + 1; // +1 to skip top UI row (row 0)
        int col = gridX + 2; // +2 to center (columns 2-6 out of 0-8)
        
        // Row must be 1-4 only (not 5, which is reserved for borders)
        // Col must be 2-6
        if (row < 1 || row > 4 || col < 2 || col > 6) {
            return -1;
        }
        
        return row * 9 + col;
    }
    
    /**
     * Add navigation buttons (only up/down for vertical progression)
     */
    private static void addNavigationButtons(Inventory gui, IslandChallenge.ChallengeCategory category) {
        // Up button (slot 26) - scroll down to see higher challenges (middle-right)
        ItemStack upButton = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta upMeta = upButton.getItemMeta();
        upMeta.displayName(Component.text("▲ View Higher Tiers", NamedTextColor.GREEN, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        List<Component> upLore = new ArrayList<>();
        upLore.add(Component.text("Scroll up to see advanced challenges", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        upMeta.lore(upLore);
        upButton.setItemMeta(upMeta);
        gui.setItem(26, upButton);
        
        // Down button (slot 35) - scroll up to see lower challenges (right side, row 3)
        ItemStack downButton = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta downMeta = downButton.getItemMeta();
        downMeta.displayName(Component.text("▼ View Lower Tiers", NamedTextColor.GREEN, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        List<Component> downLore = new ArrayList<>();
        downLore.add(Component.text("Scroll down to see starter challenges", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        downMeta.lore(downLore);
        downButton.setItemMeta(downMeta);
        gui.setItem(35, downButton);
        
        // Category info (slot 4) - centered at top
        ItemStack infoButton = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoButton.getItemMeta();
        infoMeta.displayName(Component.text("§6§l" + category.getIcon() + " " + category.getDisplayName(), NamedTextColor.GOLD, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.text(category.getDescription(), NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        infoLore.add(Component.empty());
        infoLore.add(Component.text("Progress from bottom to top!", NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false));
        infoMeta.lore(infoLore);
        infoButton.setItemMeta(infoMeta);
        gui.setItem(4, infoButton);
        
        // Back button (slot 45) - bottom left area
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.displayName(Component.text("§f§lBack", NamedTextColor.WHITE, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        List<Component> backLore = new ArrayList<>();
        backLore.add(Component.text("Return to challenge categories", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        backMeta.lore(backLore);
        backButton.setItemMeta(backMeta);
        gui.setItem(45, backButton);
    }
    
    /**
     * Add cyan border decoration around the GUI
     */
    private static void addBorders(Inventory gui) {
        ItemStack border = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        border.setItemMeta(meta);
        
        // Top and bottom rows (0-8, 45-53)
        for (int i = 0; i < 9; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, border.clone());
            }
            if (gui.getItem(45 + i) == null) {
                gui.setItem(45 + i, border.clone());
            }
        }
        
        // Left and right columns (slots 9, 18, 27, 36 and 17, 26, 35, 44)
        for (int row = 1; row < 5; row++) {
            int leftSlot = row * 9;
            int rightSlot = row * 9 + 8;
            if (gui.getItem(leftSlot) == null) {
                gui.setItem(leftSlot, border.clone());
            }
            if (gui.getItem(rightSlot) == null) {
                gui.setItem(rightSlot, border.clone());
            }
        }
    }
    
    /**
     * Fill empty slots with decoration
     */
    private static void fillEmptySlots(Inventory gui) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        filler.setItemMeta(meta);
        
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }
    
    /**
     * Get the current view position for a player
     */
    public static TreeGridPosition getPlayerViewPosition(Player player) {
        return playerViewPositions.getOrDefault(player, new TreeGridPosition(0, 0));
    }
    
    /**
     * Clear the view position for a player
     */
    public static void clearPlayerViewPosition(Player player) {
        playerViewPositions.remove(player);
    }
}
