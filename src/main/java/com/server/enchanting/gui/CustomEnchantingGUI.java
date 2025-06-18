package com.server.enchanting.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.enchanting.CustomEnchantment;
import com.server.enchanting.CustomEnchantmentRegistry;
import com.server.enchanting.EnchantingLevelCalculator;
import com.server.enchanting.EnchantingLevelCalculator.EnchantingLevel;
import com.server.enchanting.EnchantmentMaterial;
import com.server.enchanting.EnchantmentRandomizer;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Custom enchanting GUI that replaces vanilla enchanting
 */
public class CustomEnchantingGUI {
    
    // GUI title following your pattern
    public static final String GUI_TITLE = ChatColor.DARK_GRAY + "✦ " + ChatColor.DARK_PURPLE + "Enchanting Table" + ChatColor.DARK_GRAY + " ✦";
    
    // Slot layout for the enchanting GUI (5 rows - 45 slots)
    private static final int ITEM_TO_ENCHANT_SLOT = 21;        // Center slot for item
    private static final int[] ENHANCEMENT_SLOTS = {19, 20, 23, 24}; // 4 slots around item
    private static final int CONFIRM_SLOT = 25;                // Right of item
    
    // Display slots for enchantment information
    private static final int XP_COST_SLOT = 11;
    private static final int ESSENCE_COST_SLOT = 12;
    private static final int ENCHANTING_LEVEL_SLOT = 13;
    private static final int SUCCESS_RATE_SLOT = 14;
    private static final int ENCHANTMENT_PREVIEW_SLOT = 15;
    
    
    // Store active enchanting GUIs and their data
    private static final Map<Player, Inventory> activeEnchantingGUIs = new ConcurrentHashMap<>();
    private static final Map<Player, Location> playerEnchantingTables = new ConcurrentHashMap<>();
    private static final Map<Player, EnchantingLevel> playerEnchantingLevels = new ConcurrentHashMap<>();
    
    /**
     * Open enchanting GUI for a player at an enchanting table
     */
    public static void openEnchantingGUI(Player player, Location enchantingTableLocation) {
        // Calculate available enchanting level
        EnchantingLevel enchantingLevel = EnchantingLevelCalculator.calculateEnchantingLevel(enchantingTableLocation);
        
        // Create the GUI
        Inventory gui = Bukkit.createInventory(null, 45, GUI_TITLE);
        
        // Store data
        activeEnchantingGUIs.put(player, gui);
        playerEnchantingTables.put(player, enchantingTableLocation);
        playerEnchantingLevels.put(player, enchantingLevel);
        
        // Create the layout
        createEnchantingLayout(gui, player, enchantingLevel);
        
        // Open the GUI
        player.openInventory(gui);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Enchanting GUI] Opened for " + player.getName() + " at level " + enchantingLevel.getTotalLevel());
        }
    }
    
    /**
     * Create the enchanting table layout
     */
    private static void createEnchantingLayout(Inventory gui, Player player, EnchantingLevel enchantingLevel) {
        // Fill with decorative border
        fillEnchantingBorder(gui);
        
        // Clear functional slots
        clearFunctionalSlots(gui);
        
        // Add visual indicators
        addSlotIndicators(gui);
        
        // Add decorative elements
        addDecorativeElements(gui, enchantingLevel);
        
        // Update display information
        updateEnchantingDisplays(gui, player, enchantingLevel);
    }

    /**
     * Update enchantment preview display with multiple enchantments and probabilities
     */
    private static void updateEnchantmentPreview(Inventory gui, Player player, ItemStack itemToEnchant, 
                                            ItemStack[] enhancementMaterials, EnchantingLevel enchantingLevel) {
        ItemStack previewDisplay = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = previewDisplay.getItemMeta();
        
        if (itemToEnchant == null || itemToEnchant.getType() == Material.AIR) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Enchantment Preview");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Place an item to see available enchantments"
            ));
        } else {
            // Generate enchantment selections
            List<EnchantmentRandomizer.EnchantmentSelection> selections = 
                EnchantmentRandomizer.generateEnchantmentSelections(
                    itemToEnchant, enchantingLevel, enhancementMaterials, player);
            
            if (!selections.isEmpty()) {
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "✦ Enchantment Preview ✦");
                
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GOLD + "Possible Enchantments:");
                lore.add("");
                
                // Show each possible enchantment with probability
                for (int i = 0; i < selections.size(); i++) {
                    EnchantmentRandomizer.EnchantmentSelection selection = selections.get(i);
                    
                    // Color code by probability
                    ChatColor probabilityColor;
                    if (selection.probability >= 0.8) {
                        probabilityColor = ChatColor.GREEN;
                    } else if (selection.probability >= 0.5) {
                        probabilityColor = ChatColor.YELLOW;
                    } else if (selection.probability >= 0.3) {
                        probabilityColor = ChatColor.GOLD;
                    } else {
                        probabilityColor = ChatColor.RED;
                    }
                    
                    String priorityLabel = "";
                    if (selection.isGuaranteed) {
                        priorityLabel = ChatColor.AQUA + " [GUARANTEED]";
                    } else if (i == 1) {
                        priorityLabel = ChatColor.YELLOW + " [SECONDARY]";
                    } else if (i > 1) {
                        priorityLabel = ChatColor.GRAY + " [BONUS]";
                    }
                    
                    lore.add(ChatColor.GRAY + "• " + selection.enchantment.getFormattedName(selection.level) + priorityLabel);
                    lore.add(ChatColor.GRAY + "  Chance: " + probabilityColor + 
                            String.format("%.1f%%", selection.probability * 100));
                    lore.add(ChatColor.GRAY + "  " + selection.enchantment.getDescription());
                    
                    if (i < selections.size() - 1) {
                        lore.add("");
                    }
                }
                
                lore.add("");
                lore.add(ChatColor.DARK_GRAY + "Note: Multiple enchantments may be applied!");
                lore.add(ChatColor.DARK_GRAY + "Higher enchanting levels increase bonus chances.");
                
                meta.setLore(lore);
            } else {
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Enchantment Preview");
                
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.RED + "No compatible enchantments");
                lore.add("");
                lore.add(ChatColor.GRAY + "This item cannot be enchanted, or");
                lore.add(ChatColor.GRAY + "your enchanting level is too low");
                
                meta.setLore(lore);
            }
        }
        
        previewDisplay.setItemMeta(meta);
        gui.setItem(ENCHANTMENT_PREVIEW_SLOT, previewDisplay);
    }

    /**
     * Update success rate display with multiple enchantment information
     */
    private static void updateSuccessRateDisplay(Inventory gui, Player player, ItemStack itemToEnchant, 
                                            ItemStack[] enhancementMaterials, EnchantingLevel enchantingLevel) {
        ItemStack successDisplay = new ItemStack(Material.CLOCK);
        ItemMeta meta = successDisplay.getItemMeta();
        
        if (itemToEnchant == null || itemToEnchant.getType() == Material.AIR) {
            meta.setDisplayName(ChatColor.YELLOW + "Success Rate: " + ChatColor.GRAY + "No item");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Place an item to see success rates"
            ));
        } else {
            List<EnchantmentRandomizer.EnchantmentSelection> selections = 
                EnchantmentRandomizer.generateEnchantmentSelections(
                    itemToEnchant, enchantingLevel, enhancementMaterials, player);
            
            if (!selections.isEmpty()) {
                // Calculate overall success metrics
                double guaranteedCount = selections.stream().mapToDouble(s -> s.isGuaranteed ? 1.0 : 0.0).sum();
                double expectedEnchantments = guaranteedCount + 
                    selections.stream().filter(s -> !s.isGuaranteed).mapToDouble(s -> s.probability).sum();
                
                ChatColor rateColor = expectedEnchantments >= 3.0 ? ChatColor.GREEN : 
                                    expectedEnchantments >= 2.0 ? ChatColor.YELLOW : 
                                    expectedEnchantments >= 1.0 ? ChatColor.GOLD : ChatColor.RED;
                
                meta.setDisplayName(ChatColor.YELLOW + "Expected Enchantments: " + rateColor + 
                                String.format("%.1f", expectedEnchantments));
                
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Guaranteed: " + ChatColor.GREEN + String.format("%.0f", guaranteedCount));
                lore.add(ChatColor.GRAY + "Expected Total: " + rateColor + String.format("%.1f", expectedEnchantments));
                lore.add("");
                
                // Show breakdown by enchantment
                lore.add(ChatColor.AQUA + "Individual Success Rates:");
                for (EnchantmentRandomizer.EnchantmentSelection selection : selections) {
                    ChatColor chanceColor = selection.probability >= 0.8 ? ChatColor.GREEN : 
                                        selection.probability >= 0.5 ? ChatColor.YELLOW : ChatColor.RED;
                    
                    String guaranteedText = selection.isGuaranteed ? " ✓" : "";
                    lore.add(ChatColor.GRAY + "• " + selection.enchantment.getDisplayName() + ": " + 
                            chanceColor + String.format("%.1f%%", selection.probability * 100) + guaranteedText);
                }
                
                lore.add("");
                if (expectedEnchantments >= 3.0) {
                    lore.add(ChatColor.GREEN + "✓ Excellent enchanting potential!");
                } else if (expectedEnchantments >= 2.0) {
                    lore.add(ChatColor.YELLOW + "⚠ Good enchanting potential");
                } else if (expectedEnchantments >= 1.0) {
                    lore.add(ChatColor.GOLD + "⚠ Moderate enchanting potential");
                } else {
                    lore.add(ChatColor.RED + "✗ Low enchanting potential");
                }
                
                meta.setLore(lore);
            } else {
                meta.setDisplayName(ChatColor.YELLOW + "Success Rate: " + ChatColor.GRAY + "No enchantments");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "No applicable enchantments for this item"
                ));
            }
        }
        
        successDisplay.setItemMeta(meta);
        gui.setItem(SUCCESS_RATE_SLOT, successDisplay);
    }

    /**
     * Get enchantment selections for current GUI state (for listener use)
     */
    public static List<EnchantmentRandomizer.EnchantmentSelection> getCurrentEnchantmentSelections(Player player) {
        Inventory gui = getPlayerEnchantingGUI(player);
        EnchantingLevel enchantingLevel = getPlayerEnchantingLevel(player);
        
        if (gui == null || enchantingLevel == null) {
            return new ArrayList<>();
        }
        
        ItemStack itemToEnchant = gui.getItem(ITEM_TO_ENCHANT_SLOT);
        ItemStack[] enhancementMaterials = getEnhancementMaterials(gui);
        
        if (itemToEnchant == null || itemToEnchant.getType() == Material.AIR) {
            return new ArrayList<>();
        }
        
        return EnchantmentRandomizer.generateEnchantmentSelections(itemToEnchant, enchantingLevel, enhancementMaterials, player);
    }
    
    /**
     * Create decorative border with mystical theme
     */
    private static void fillEnchantingBorder(Inventory gui) {
        // Create different glass panes for mystical appearance
        ItemStack purpleBorder = createGlassPane(Material.PURPLE_STAINED_GLASS_PANE, " ");
        ItemStack magentaBorder = createGlassPane(Material.MAGENTA_STAINED_GLASS_PANE, " ");
        ItemStack blackFiller = createGlassPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        
        // Fill all slots with black glass initially
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, blackFiller);
        }
        
        // Create mystical border pattern
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, i % 2 == 0 ? purpleBorder : magentaBorder);
            gui.setItem(36 + i, i % 2 == 0 ? magentaBorder : purpleBorder);
        }
        
        // Side borders
        for (int i = 1; i < 4; i++) {
            gui.setItem(i * 9, purpleBorder);
            gui.setItem(i * 9 + 8, purpleBorder);
        }
        
        // Corners with special emphasis
        gui.setItem(0, magentaBorder);
        gui.setItem(8, magentaBorder);
        gui.setItem(36, magentaBorder);
        gui.setItem(44, magentaBorder);
    }
    
    /**
     * Clear functional slots for item placement
     */
    private static void clearFunctionalSlots(Inventory gui) {
        // Clear item slot
        gui.setItem(ITEM_TO_ENCHANT_SLOT, null);
        
        // Clear enhancement material slots
        for (int slot : ENHANCEMENT_SLOTS) {
            gui.setItem(slot, null);
        }
        
        // Clear confirm slot (will be filled with barrier initially)
        gui.setItem(CONFIRM_SLOT, createConfirmBarrier());
    }
    
    /**
     * Add visual slot indicators
     */
    private static void addSlotIndicators(Inventory gui) {
        // Item slot indicator (above item slot)
        gui.setItem(ITEM_TO_ENCHANT_SLOT - 9, createSlotIndicator(
            Material.DIAMOND_SWORD, 
            ChatColor.AQUA + "⬇ Item to Enchant", 
            Arrays.asList(
                ChatColor.GRAY + "Place the item you want to",
                ChatColor.GRAY + "enchant in this slot"
            )
        ));
        
        // Enhancement slots indicators
        gui.setItem(ENHANCEMENT_SLOTS[0] - 9, createSlotIndicator(
            Material.LAPIS_LAZULI,
            ChatColor.GOLD + "⬇ Enhancement Materials",
            Arrays.asList(
                ChatColor.GRAY + "Place materials to improve",
                ChatColor.GRAY + "enchantment success rates"
            )
        ));
        
        // Confirm slot indicator
        gui.setItem(CONFIRM_SLOT - 9, createSlotIndicator(
            Material.EMERALD,
            ChatColor.GREEN + "⬇ Confirm Enchant",
            Arrays.asList(
                ChatColor.GRAY + "Click when ready to",
                ChatColor.GRAY + "apply the enchantment"
            )
        ));
    }
    
    /**
     * Add decorative elements specific to enchanting
     */
    private static void addDecorativeElements(Inventory gui, EnchantingLevel enchantingLevel) {
        // Enchanting book icon
        ItemStack enchantingIcon = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta iconMeta = enchantingIcon.getItemMeta();
        iconMeta.setDisplayName(ChatColor.DARK_PURPLE + "✦ Enchanting Table ✦");
        iconMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Imbue your items with magical power!",
            "",
            ChatColor.YELLOW + "• Place item and enhancement materials",
            ChatColor.YELLOW + "• Higher levels unlock better enchantments",
            ChatColor.YELLOW + "• Use rune books for advanced enchanting",
            "",
            ChatColor.AQUA + "Current Level: " + ChatColor.GOLD + enchantingLevel.getFormattedLevel()
        ));
        enchantingIcon.setItemMeta(iconMeta);
        gui.setItem(4, enchantingIcon); // Top center
        
        // Mystical crystals for decoration
        ItemStack crystal = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta crystalMeta = crystal.getItemMeta();
        crystalMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Mystical Crystal");
        crystalMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Ancient crystals that resonate with",
            ChatColor.GRAY + "the magical energies of enchantment"
        ));
        crystal.setItemMeta(crystalMeta);
        
        // Place crystals in corners if space allows
        if (!isFunctionalSlot(37)) gui.setItem(37, crystal);
        if (!isFunctionalSlot(43)) gui.setItem(43, crystal);
    }
    
    /**
     * Update all enchanting displays based on current state
     */
    public static void updateEnchantingDisplays(Inventory gui, Player player, EnchantingLevel enchantingLevel) {
        ItemStack itemToEnchant = gui.getItem(ITEM_TO_ENCHANT_SLOT);
        ItemStack[] enhancementMaterials = getEnhancementMaterials(gui);
        
        // Update XP cost display
        updateXpCostDisplay(gui, player, itemToEnchant, enhancementMaterials, enchantingLevel);
        
        // Update essence cost display
        updateEssenceCostDisplay(gui, player, itemToEnchant, enhancementMaterials, enchantingLevel);
        
        // Update enchanting level display
        updateEnchantingLevelDisplay(gui, enchantingLevel);
        
        // Update success rate display
        updateSuccessRateDisplay(gui, player, itemToEnchant, enhancementMaterials, enchantingLevel);
        
        // Update enchantment preview
        updateEnchantmentPreview(gui, player, itemToEnchant, enhancementMaterials, enchantingLevel);
        
        // Update confirm button
        updateConfirmButton(gui, player, itemToEnchant, enhancementMaterials, enchantingLevel);
    }
    
    /**
     * Update XP cost display
     */
    private static void updateXpCostDisplay(Inventory gui, Player player, ItemStack itemToEnchant, 
                                          ItemStack[] enhancementMaterials, EnchantingLevel enchantingLevel) {
        ItemStack xpDisplay = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = xpDisplay.getItemMeta();
        
        if (itemToEnchant == null || itemToEnchant.getType() == Material.AIR) {
            meta.setDisplayName(ChatColor.GREEN + "XP Cost: " + ChatColor.GRAY + "No item");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Place an item to see XP cost"
            ));
        } else {
            CustomEnchantment enchantment = getAvailableEnchantment(player, itemToEnchant, enchantingLevel);
            if (enchantment != null) {
                int level = calculateEnchantmentLevel(enchantment, enhancementMaterials, enchantingLevel);
                int xpCost = enchantment.getXpCost(level);
                
                ChatColor costColor = player.getLevel() >= xpCost ? ChatColor.GREEN : ChatColor.RED;
                meta.setDisplayName(ChatColor.GREEN + "XP Cost: " + costColor + xpCost + " levels");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Your XP: " + ChatColor.YELLOW + player.getLevel() + " levels",
                    "",
                    player.getLevel() >= xpCost ? 
                        ChatColor.GREEN + "✓ You have enough XP" :
                        ChatColor.RED + "✗ You need " + (xpCost - player.getLevel()) + " more levels"
                ));
            } else {
                meta.setDisplayName(ChatColor.GREEN + "XP Cost: " + ChatColor.GRAY + "No enchantment");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "No applicable enchantments for this item"
                ));
            }
        }
        
        xpDisplay.setItemMeta(meta);
        gui.setItem(XP_COST_SLOT, xpDisplay);
    }
    
    /**
     * Update essence cost display
     */
    private static void updateEssenceCostDisplay(Inventory gui, Player player, ItemStack itemToEnchant, 
                                                ItemStack[] enhancementMaterials, EnchantingLevel enchantingLevel) {
        ItemStack essenceDisplay = new ItemStack(Material.GHAST_TEAR);
        ItemMeta meta = essenceDisplay.getItemMeta();
        
        if (itemToEnchant == null || itemToEnchant.getType() == Material.AIR) {
            meta.setDisplayName(ChatColor.AQUA + "Essence Cost: " + ChatColor.GRAY + "No item");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Place an item to see essence cost"
            ));
        } else {
            CustomEnchantment enchantment = getAvailableEnchantment(player, itemToEnchant, enchantingLevel);
            if (enchantment != null) {
                int level = calculateEnchantmentLevel(enchantment, enhancementMaterials, enchantingLevel);
                int essenceCost = enchantment.getEssenceCost(level);
                
                // Get player's essence
                PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[
                    ProfileManager.getInstance().getActiveProfile(player.getUniqueId())
                ];
                int playerEssence = profile != null ? profile.getEssence() : 0;
                
                ChatColor costColor = playerEssence >= essenceCost ? ChatColor.GREEN : ChatColor.RED;
                meta.setDisplayName(ChatColor.AQUA + "Essence Cost: " + costColor + essenceCost + " essence");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Your Essence: " + ChatColor.AQUA + playerEssence,
                    "",
                    playerEssence >= essenceCost ? 
                        ChatColor.GREEN + "✓ You have enough essence" :
                        ChatColor.RED + "✗ You need " + (essenceCost - playerEssence) + " more essence"
                ));
            } else {
                meta.setDisplayName(ChatColor.AQUA + "Essence Cost: " + ChatColor.GRAY + "No enchantment");
                meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "No applicable enchantments for this item"
                ));
            }
        }
        
        essenceDisplay.setItemMeta(meta);
        gui.setItem(ESSENCE_COST_SLOT, essenceDisplay);
    }
    
    /**
     * Update enchanting level display
     */
    private static void updateEnchantingLevelDisplay(Inventory gui, EnchantingLevel enchantingLevel) {
        ItemStack levelDisplay = new ItemStack(Material.BOOKSHELF);
        ItemMeta meta = levelDisplay.getItemMeta();
        
        meta.setDisplayName(ChatColor.GOLD + "✦ Enchanting Level ✦");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.AQUA + "Total Level: " + ChatColor.YELLOW + enchantingLevel.getTotalLevel());
        lore.add(ChatColor.GRAY + "Vanilla Level: " + ChatColor.WHITE + enchantingLevel.getVanillaLevel() + "/30");
        
        if (enchantingLevel.getAdvancedLevel() > 0) {
            lore.add(ChatColor.LIGHT_PURPLE + "Advanced Level: " + ChatColor.GOLD + enchantingLevel.getAdvancedLevel());
            lore.add(ChatColor.GRAY + "From: " + ChatColor.YELLOW + enchantingLevel.getChiseledBookshelves().size() + " Rune Books");
        }
        
        lore.add("");
        lore.add(ChatColor.GREEN + "Vanilla Bookshelves: " + ChatColor.WHITE + enchantingLevel.getVanillaBookshelves().size());
        
        if (!enchantingLevel.getChiseledBookshelves().isEmpty()) {
            lore.add(ChatColor.LIGHT_PURPLE + "Chiseled Bookshelves: " + ChatColor.WHITE + 
                    enchantingLevel.getChiseledBookshelves().size());
        }
        
        if (enchantingLevel.getTotalLevel() >= 1000) {
            lore.add("");
            lore.add(ChatColor.GOLD + "✦ MAXIMUM LEVEL REACHED ✦");
        }
        
        meta.setLore(lore);
        levelDisplay.setItemMeta(meta);
        gui.setItem(ENCHANTING_LEVEL_SLOT, levelDisplay);
    }
    
    /**
     * Update confirm button based on current state
     */
    private static void updateConfirmButton(Inventory gui, Player player, ItemStack itemToEnchant, 
                                          ItemStack[] enhancementMaterials, EnchantingLevel enchantingLevel) {
        if (itemToEnchant == null || itemToEnchant.getType() == Material.AIR) {
            gui.setItem(CONFIRM_SLOT, createConfirmBarrier());
            return;
        }
        
        CustomEnchantment enchantment = getAvailableEnchantment(player, itemToEnchant, enchantingLevel);
        if (enchantment == null) {
            gui.setItem(CONFIRM_SLOT, createConfirmBarrier());
            return;
        }
        
        // Check if player has required resources
        int level = calculateEnchantmentLevel(enchantment, enhancementMaterials, enchantingLevel);
        int xpCost = enchantment.getXpCost(level);
        int essenceCost = enchantment.getEssenceCost(level);
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[
            ProfileManager.getInstance().getActiveProfile(player.getUniqueId())
        ];
        int playerEssence = profile != null ? profile.getEssence() : 0;
        
        boolean canAfford = player.getLevel() >= xpCost && playerEssence >= essenceCost;
        
        ItemStack confirmButton = new ItemStack(canAfford ? Material.EMERALD : Material.REDSTONE);
        ItemMeta meta = confirmButton.getItemMeta();
        
        if (canAfford) {
            meta.setDisplayName(ChatColor.GREEN + "✓ Confirm Enchantment");
            
            double successRate = Math.min(
                calculateBaseSuccessRate(enchantment, enchantingLevel) + 
                calculateMaterialBonus(enchantment, enhancementMaterials), 1.0
            );
            
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Click to apply: " + enchantment.getFormattedName(level),
                "",
                ChatColor.GRAY + "Costs:",
                ChatColor.GREEN + "• " + xpCost + " XP levels",
                ChatColor.AQUA + "• " + essenceCost + " essence",
                "",
                ChatColor.YELLOW + "Success Rate: " + String.format("%.1f%%", successRate * 100),
                "",
                ChatColor.GREEN + "Click to enchant!"
            ));
        } else {
            meta.setDisplayName(ChatColor.RED + "✗ Cannot Enchant");
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Missing requirements:");
            
            if (player.getLevel() < xpCost) {
                lore.add(ChatColor.RED + "• Need " + (xpCost - player.getLevel()) + " more XP levels");
            }
            if (playerEssence < essenceCost) {
                lore.add(ChatColor.RED + "• Need " + (essenceCost - playerEssence) + " more essence");
            }
            
            meta.setLore(lore);
        }
        
        confirmButton.setItemMeta(meta);
        gui.setItem(CONFIRM_SLOT, confirmButton);
    }
    
    /**
     * Get enhancement materials from the GUI
     */
    private static ItemStack[] getEnhancementMaterials(Inventory gui) {
        ItemStack[] materials = new ItemStack[ENHANCEMENT_SLOTS.length];
        for (int i = 0; i < ENHANCEMENT_SLOTS.length; i++) {
            materials[i] = gui.getItem(ENHANCEMENT_SLOTS[i]);
        }
        return materials;
    }
    
    /**
     * Get the best available enchantment for an item
     */
    private static CustomEnchantment getAvailableEnchantment(Player player, ItemStack item, EnchantingLevel enchantingLevel) {
        if (item == null || item.getType() == Material.AIR) return null;
        
        List<CustomEnchantment> applicable = CustomEnchantmentRegistry.getInstance().getApplicableEnchantments(item);
        
        // Filter by enchanting level and find the best one
        CustomEnchantment best = null;
        for (CustomEnchantment enchantment : applicable) {
            // Check if player's enchanting level is sufficient
            int requiredLevel = getRequiredEnchantingLevel(enchantment);
            if (enchantingLevel.getTotalLevel() >= requiredLevel) {
                if (best == null || enchantment.getRarity().ordinal() > best.getRarity().ordinal()) {
                    best = enchantment;
                }
            }
        }
        
        return best;
    }
    
    /**
     * Calculate the level of enchantment that will be applied
     */
    private static int calculateEnchantmentLevel(CustomEnchantment enchantment, ItemStack[] enhancementMaterials, 
                                               EnchantingLevel enchantingLevel) {
        int baseLevel = 1;
        
        // Add level bonus from enhancement materials
        for (ItemStack material : enhancementMaterials) {
            if (material != null && material.getType() != Material.AIR) {
                EnchantmentMaterial enhancementMaterial = EnchantmentMaterial.Registry.fromItem(material);
                if (enhancementMaterial != null) {
                    baseLevel += enhancementMaterial.getLevelBonus();
                }
            }
        }
        
        // Cap at enchantment's max level
        return Math.min(baseLevel, enchantment.getMaxLevel());
    }
    
    /**
     * Calculate base success rate for an enchantment
     */
    private static double calculateBaseSuccessRate(CustomEnchantment enchantment, EnchantingLevel enchantingLevel) {
        // Base rate depends on rarity and required level
        double baseRate = 0.8 - (enchantment.getRarity().ordinal() * 0.1); // 80% for common, down to 30% for mythic
        
        // Bonus from high enchanting level
        int requiredLevel = getRequiredEnchantingLevel(enchantment);
        int playerLevel = enchantingLevel.getTotalLevel();
        
        if (playerLevel > requiredLevel) {
            double levelBonus = Math.min((playerLevel - requiredLevel) * 0.01, 0.2); // Up to 20% bonus
            baseRate += levelBonus;
        }
        
        return Math.max(0.1, Math.min(baseRate, 0.95)); // Between 10% and 95%
    }
    
    /**
     * Calculate success rate bonus from enhancement materials
     */
    private static double calculateMaterialBonus(CustomEnchantment enchantment, ItemStack[] enhancementMaterials) {
        double totalBonus = 0.0;
        
        for (ItemStack material : enhancementMaterials) {
            if (material != null && material.getType() != Material.AIR) {
                EnchantmentMaterial enhancementMaterial = EnchantmentMaterial.Registry.fromItem(material);
                if (enhancementMaterial != null) {
                    totalBonus += enhancementMaterial.getEnhancementBonus(enchantment);
                }
            }
        }
        
        return Math.min(totalBonus, 0.4); // Cap bonus at 40%
    }
    
    /**
     * Get required enchanting level for an enchantment
     */
    private static int getRequiredEnchantingLevel(CustomEnchantment enchantment) {
        // Base requirement by rarity
        switch (enchantment.getRarity()) {
            case COMMON: return 1;
            case UNCOMMON: return 5;
            case RARE: return 15;
            case EPIC: return 25;
            case LEGENDARY: return 40;
            case MYTHIC: return 60;
            default: return 1;
        }
    }
    
    /**
     * Helper methods for creating GUI elements
     */
    private static ItemStack createGlassPane(Material material, String name) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(name);
        pane.setItemMeta(meta);
        return pane;
    }
    
    private static ItemStack createSlotIndicator(Material material, String name, List<String> lore) {
        ItemStack indicator = new ItemStack(material);
        ItemMeta meta = indicator.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        indicator.setItemMeta(meta);
        return indicator;
    }
    
    private static ItemStack createConfirmBarrier() {
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta meta = barrier.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "No Item to Enchant");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Place an item in the enchanting slot",
            ChatColor.GRAY + "to begin the enchanting process"
        ));
        barrier.setItemMeta(meta);
        return barrier;
    }
    
    private static boolean isFunctionalSlot(int slot) {
        if (slot == ITEM_TO_ENCHANT_SLOT || slot == CONFIRM_SLOT) return true;
        
        for (int enhancementSlot : ENHANCEMENT_SLOTS) {
            if (slot == enhancementSlot) return true;
        }
        
        return false;
    }
    
    // Public access methods
    public static Inventory getPlayerEnchantingGUI(Player player) {
        return activeEnchantingGUIs.get(player);
    }
    
    public static Location getPlayerEnchantingTable(Player player) {
        return playerEnchantingTables.get(player);
    }
    
    public static EnchantingLevel getPlayerEnchantingLevel(Player player) {
        return playerEnchantingLevels.get(player);
    }
    
    public static void removePlayerData(Player player) {
        activeEnchantingGUIs.remove(player);
        playerEnchantingTables.remove(player);
        playerEnchantingLevels.remove(player);
    }
    
    // Slot checking methods for listener
    public static boolean isItemSlot(int slot) {
        return slot == ITEM_TO_ENCHANT_SLOT;
    }
    
    public static boolean isEnhancementSlot(int slot) {
        for (int enhancementSlot : ENHANCEMENT_SLOTS) {
            if (slot == enhancementSlot) return true;
        }
        return false;
    }
    
    public static boolean isConfirmSlot(int slot) {
        return slot == CONFIRM_SLOT;
    }
}