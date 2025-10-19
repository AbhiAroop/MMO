package com.server.enchantments.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.enchantments.EnchantmentRegistry;
import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentData;
import com.server.enchantments.elements.ElementType;
import com.server.enchantments.elements.HybridElement;
import com.server.enchantments.items.ElementalFragment;

/**
 * Custom GUI for the enchantment table interaction.
 * Provides a 54-slot (6 row) interface for enchanting items with fragments.
 * 
 * Organized Layout with Clear Separation:
 * Row 1 (0-8): Decorative border
 * Row 2 (9-17): Info slots at 10-16, borders at 9 and 17
 * Row 3 (18-26): Item slot (22 - CENTER), borders around
 * Row 4 (27-35): Fragment slots (29, 31, 33) with spacing, output indicator
 * Row 5 (36-44): Output slot (40), Enchant button (38), Cancel (42)
 * Row 6 (45-53): Decorative border
 */
public class EnchantmentTableGUI {
    
    private static final String GUI_TITLE = ChatColor.DARK_PURPLE + "⚔ " + ChatColor.LIGHT_PURPLE + "Enchantment Altar " + ChatColor.DARK_PURPLE + "⚔";
    private static final int GUI_SIZE = 54; // 6 rows
    
    // Slot positions - CLEARLY SEPARATED for no confusion
    public static final int ITEM_SLOT = 22; // Row 3, CENTER (visually distinct)
    public static final int[] FRAGMENT_SLOTS = {29, 31, 33}; // Row 4, spaced out (left, center, right)
    public static final int OUTPUT_SLOT = 40; // Row 5, center
    public static final int ENCHANT_BUTTON_SLOT = 38; // Row 5, left of output
    public static final int CANCEL_BUTTON_SLOT = 42; // Row 5, right of output
    public static final int[] INFO_SLOTS = {10, 11, 12, 13, 14, 15, 16}; // Row 2, slots 1-7
    
    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, ItemStack> placedFragments;
    private ItemStack itemToEnchant;
    
    public EnchantmentTableGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        this.placedFragments = new HashMap<>();
        this.itemToEnchant = null;
        
        initializeGUI();
    }
    
    /**
     * Initializes the GUI with decorative elements and buttons.
     */
    private void initializeGUI() {
        // Fill ALL slots with black glass panes first (filler for non-interactive slots)
        ItemStack fillerPane = createFillerPane();
        for (int i = 0; i < GUI_SIZE; i++) {
            inventory.setItem(i, fillerPane);
        }
        
        // Fill borders with decorative stained glass panes
        ItemStack borderPane = createBorderPane();
        
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, borderPane); // Row 1
            inventory.setItem(45 + i, borderPane); // Row 6
        }
        
        // Left and right columns
        for (int row = 1; row < 5; row++) {
            inventory.setItem(row * 9, borderPane); // Left edge
            inventory.setItem(row * 9 + 8, borderPane); // Right edge
        }
        
        // Add visual separators around item slot (row 3)
        ItemStack itemSeparator = createSeparatorPane(ChatColor.GOLD + "Item Slot Below");
        inventory.setItem(19, itemSeparator); // Left of item area
        inventory.setItem(21, itemSeparator); // Above item
        inventory.setItem(23, itemSeparator); // Above item  
        inventory.setItem(25, itemSeparator); // Right of item area
        
        // Leave ITEM SLOT empty (visually distinct in center)
        inventory.setItem(ITEM_SLOT, null);
        
        // Add visual separators around fragment slots (row 4)
        ItemStack fragmentSeparator = createSeparatorPane(ChatColor.AQUA + "Fragment Slots");
        inventory.setItem(28, fragmentSeparator); // Left of fragments
        inventory.setItem(30, fragmentSeparator); // Between fragments
        inventory.setItem(32, fragmentSeparator); // Between fragments
        inventory.setItem(34, fragmentSeparator); // Right of fragments
        
        // Leave FRAGMENT SLOTS empty
        for (int slot : FRAGMENT_SLOTS) {
            inventory.setItem(slot, null);
        }
        
        // Add labels for visual clarity
        ItemStack itemLabel = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta itemLabelMeta = itemLabel.getItemMeta();
        itemLabelMeta.setDisplayName(ChatColor.YELLOW + "⬇ Place Item Here ⬇");
        itemLabelMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Place the weapon or armor",
            ChatColor.GRAY + "you want to enchant"
        ));
        itemLabel.setItemMeta(itemLabelMeta);
        inventory.setItem(13, itemLabel); // Center of info row, above item slot
        
        // Add down arrow pointing to output
        ItemStack downArrow = new ItemStack(Material.ARROW);
        ItemMeta downArrowMeta = downArrow.getItemMeta();
        downArrowMeta.setDisplayName(ChatColor.YELLOW + "⬇ Result Below ⬇");
        downArrowMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Enchanted item will",
            ChatColor.GRAY + "appear here"
        ));
        downArrow.setItemMeta(downArrowMeta);
        inventory.setItem(37, downArrow); // Above output in row 5
        
        // Output slot placeholder
        inventory.setItem(OUTPUT_SLOT, createPlaceholder(Material.LIME_STAINED_GLASS_PANE,
            ChatColor.GREEN + "⬛ Output",
            ChatColor.GRAY + "Take enchanted item here"));
        
        // Buttons
        updateEnchantButton();
        inventory.setItem(CANCEL_BUTTON_SLOT, createCancelButton());
        updateInfoDisplay();
    }
    
    /**
     * Opens the GUI for the player.
     */
    public void open() {
        player.openInventory(inventory);
    }
    
    /**
     * Gets the GUI inventory.
     */
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Gets the player viewing this GUI.
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Checks if a slot is a fragment slot.
     */
    public boolean isFragmentSlot(int slot) {
        for (int fragmentSlot : FRAGMENT_SLOTS) {
            if (fragmentSlot == slot) return true;
        }
        return false;
    }
    
    /**
     * Checks if a slot is the item slot.
     */
    public boolean isItemSlot(int slot) {
        return slot == ITEM_SLOT;
    }
    
    /**
     * Checks if a slot is a button or interactive element.
     */
    public boolean isButtonSlot(int slot) {
        return slot == ENCHANT_BUTTON_SLOT || slot == CANCEL_BUTTON_SLOT;
    }
    
    /**
     * Checks if a slot is an output slot.
     */
    public boolean isOutputSlot(int slot) {
        return slot == OUTPUT_SLOT;
    }
    
    /**
     * Checks if a slot is an info display slot.
     */
    public boolean isInfoSlot(int slot) {
        for (int infoSlot : INFO_SLOTS) {
            if (infoSlot == slot) return true;
        }
        return false;
    }
    
    /**
     * Checks if an item can be enchanted.
     * Validates custom items and vanilla items.
     */
    public boolean canEnchant(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        // Check if it's a custom item with model data
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            int modelData = item.getItemMeta().getCustomModelData();
            String modelStr = String.valueOf(modelData);
            
            // Custom items with 2XXXXX model data (functional items)
            if (modelStr.startsWith("2") && modelStr.length() >= 6) {
                String typeCode = modelStr.substring(1, 3);
                // Allow weapons and armor
                // 10 = Sword, 11 = Axe, 13 = Pickaxe, 20 = Scythe, 22 = Hoe, etc.
                // 30 = Helmet, 31 = Chestplate, 32 = Leggings, 33 = Boots
                // 40 = Staff, 41 = Wand, 50 = Special weapons
                return true; // Allow all functional custom items
            }
        }
        
        // Fallback to vanilla item types
        Material type = item.getType();
        String name = type.name();
        
        return name.endsWith("_SWORD") || 
               name.endsWith("_AXE") || 
               name.endsWith("_PICKAXE") || 
               name.endsWith("_SHOVEL") || 
               name.endsWith("_HOE") ||
               name.endsWith("_HELMET") || 
               name.endsWith("_CHESTPLATE") || 
               name.endsWith("_LEGGINGS") || 
               name.endsWith("_BOOTS") ||
               type == Material.BOW ||
               type == Material.CROSSBOW ||
               type == Material.TRIDENT ||
               type == Material.SHIELD;
    }
    
    /**
     * Places an item in the enchanting slot.
     */
    public boolean setItemToEnchant(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            itemToEnchant = null;
            inventory.setItem(ITEM_SLOT, null); // Leave empty
            updateEnchantButton();
            updateInfoDisplay();
            return false;
        }
        
        // Validate item can be enchanted
        if (!canEnchant(item)) {
            return false;
        }
        
        itemToEnchant = item.clone();
        inventory.setItem(ITEM_SLOT, item);
        updateEnchantButton();
        updateInfoDisplay();
        return true;
    }
    
    /**
     * Gets the item to be enchanted.
     */
    public ItemStack getItemToEnchant() {
        return itemToEnchant;
    }
    
    /**
     * Places a fragment in a slot.
     */
    public boolean setFragment(int slot, ItemStack fragment) {
        if (!isFragmentSlot(slot)) {
            return false;
        }
        
        if (fragment == null || fragment.getType() == Material.AIR) {
            placedFragments.remove(slot);
            inventory.setItem(slot, null); // Leave empty
        } else if (ElementalFragment.isFragment(fragment)) {
            placedFragments.put(slot, fragment.clone());
            inventory.setItem(slot, fragment);
        } else {
            return false; // Not a valid fragment
        }
        
        updateEnchantButton();
        updateInfoDisplay();
        return true;
    }
    
    /**
     * Syncs the GUI's internal state with the actual inventory contents.
     * Called after items are placed/removed to ensure tracking is accurate.
     */
    public void syncWithInventory() {
        // Sync item slot
        ItemStack itemInSlot = inventory.getItem(ITEM_SLOT);
        if (itemInSlot == null || itemInSlot.getType() == Material.AIR) {
            itemToEnchant = null;
        } else {
            itemToEnchant = itemInSlot.clone();
        }
        
        // Sync fragment slots
        placedFragments.clear();
        for (int slot : FRAGMENT_SLOTS) {
            ItemStack fragmentInSlot = inventory.getItem(slot);
            if (fragmentInSlot != null && fragmentInSlot.getType() != Material.AIR) {
                if (ElementalFragment.isFragment(fragmentInSlot)) {
                    placedFragments.put(slot, fragmentInSlot.clone());
                }
            }
        }
        
        // Update GUI display
        updateEnchantButton();
        updateInfoDisplay();
    }
    
    /**
     * Gets all placed fragments.
     */
    public List<ItemStack> getFragments() {
        return new ArrayList<>(placedFragments.values());
    }
    
    /**
     * Gets the number of fragments placed.
     */
    public int getFragmentCount() {
        return placedFragments.size();
    }
    
    /**
     * Checks if the GUI is ready to enchant (has item and valid fragments).
     */
    public boolean isReadyToEnchant() {
        int fragmentCount = placedFragments.size();
        return itemToEnchant != null && fragmentCount >= 1 && fragmentCount <= 3;
    }
    
    /**
     * Updates the enchant button based on current state.
     */
    private void updateEnchantButton() {
        ItemStack button;
        
        if (isReadyToEnchant()) {
            button = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
            ItemMeta meta = button.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + "✔ Enchant Item");
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to enchant your item");
            lore.add(ChatColor.GRAY + "with the selected fragments");
            lore.add("");
            
            // Calculate estimated XP cost
            int fragmentCount = placedFragments.size();
            int baseCost = 10 * fragmentCount;
            lore.add(ChatColor.GOLD + "Estimated Cost: " + ChatColor.YELLOW + baseCost + " XP Levels");
            
            meta.setLore(lore);
            button.setItemMeta(meta);
        } else {
            button = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta meta = button.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "✖ Cannot Enchant");
            
            List<String> lore = new ArrayList<>();
            if (itemToEnchant == null) {
                lore.add(ChatColor.GRAY + "Place an item to enchant");
            }
            if (placedFragments.size() < 1) {
                lore.add(ChatColor.GRAY + "Need at least 1 fragment");
            }
            if (placedFragments.size() > 3) {
                lore.add(ChatColor.GRAY + "Maximum 3 fragments allowed");
            }
            
            meta.setLore(lore);
            button.setItemMeta(meta);
        }
        
        inventory.setItem(ENCHANT_BUTTON_SLOT, button);
    }
    
    /**
     * Creates the cancel button.
     */
    private ItemStack createCancelButton() {
        ItemStack button = new ItemStack(Material.BARRIER);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Cancel");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Close this interface");
        lore.add(ChatColor.GRAY + "Items will be returned");
        
        meta.setLore(lore);
        button.setItemMeta(meta);
        return button;
    }
    
    /**
     * Updates the info display with current fragment information across multiple slots.
     */
    private void updateInfoDisplay() {
        // Clear all info slots first
        for (int slot : INFO_SLOTS) {
            inventory.setItem(slot, createFillerPane());
        }
        
        if (placedFragments.isEmpty()) {
            // Show helpful info items when no fragments placed
            
            // Slot 0: How to use
            ItemStack howTo = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta howToMeta = howTo.getItemMeta();
            howToMeta.setDisplayName(ChatColor.GOLD + "⚔ How to Enchant");
            List<String> howToLore = new ArrayList<>();
            howToLore.add(ChatColor.GRAY + "1. Place an item in the slot");
            howToLore.add(ChatColor.GRAY + "2. Add 1-3 elemental fragments");
            howToLore.add(ChatColor.GRAY + "3. Click the enchant button");
            howToMeta.setLore(howToLore);
            howTo.setItemMeta(howToMeta);
            inventory.setItem(INFO_SLOTS[0], howTo);
            
            // Slot 1: Fragment info
            ItemStack fragmentInfo = new ItemStack(Material.PRISMARINE_SHARD);
            ItemMeta fragmentMeta = fragmentInfo.getItemMeta();
            fragmentMeta.setDisplayName(ChatColor.AQUA + "✦ Fragments");
            List<String> fragmentLore = new ArrayList<>();
            fragmentLore.add(ChatColor.GRAY + "Required: " + ChatColor.WHITE + "1-3 fragments");
            fragmentLore.add(ChatColor.GRAY + "More fragments = Better quality");
            fragmentLore.add(ChatColor.GRAY + "Higher tiers = Better results");
            fragmentMeta.setLore(fragmentLore);
            fragmentInfo.setItemMeta(fragmentMeta);
            inventory.setItem(INFO_SLOTS[1], fragmentInfo);
            
            // Slot 2: Element types
            ItemStack elementInfo = new ItemStack(Material.BLAZE_POWDER);
            ItemMeta elementMeta = elementInfo.getItemMeta();
            elementMeta.setDisplayName(ChatColor.RED + "🔥 Elements");
            List<String> elementLore = new ArrayList<>();
            elementLore.add(ChatColor.RED + "Fire" + ChatColor.GRAY + " | " + ChatColor.BLUE + "Water");
            elementLore.add(ChatColor.GREEN + "Earth" + ChatColor.GRAY + " | " + ChatColor.WHITE + "Air");
            elementLore.add(ChatColor.DARK_PURPLE + "Void" + ChatColor.GRAY + " | " + ChatColor.YELLOW + "Light");
            elementMeta.setLore(elementLore);
            elementInfo.setItemMeta(elementMeta);
            inventory.setItem(INFO_SLOTS[2], elementInfo);
            
            // Slot 3: Hybrid enchantments
            ItemStack hybridInfo = new ItemStack(Material.NETHER_STAR);
            ItemMeta hybridMeta = hybridInfo.getItemMeta();
            hybridMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "⚡ Hybrid Power");
            List<String> hybridLore = new ArrayList<>();
            hybridLore.add(ChatColor.GRAY + "Mix 2+ element types for");
            hybridLore.add(ChatColor.GRAY + "powerful hybrid enchantments!");
            hybridLore.add(ChatColor.LIGHT_PURPLE + "Example: Fire + Water = Steam");
            hybridMeta.setLore(hybridLore);
            hybridInfo.setItemMeta(hybridMeta);
            inventory.setItem(INFO_SLOTS[3], hybridInfo);
            
            // Slot 4: Quality tiers
            ItemStack qualityInfo = new ItemStack(Material.DIAMOND);
            ItemMeta qualityMeta = qualityInfo.getItemMeta();
            qualityMeta.setDisplayName(ChatColor.AQUA + "⭐ Quality Tiers");
            List<String> qualityLore = new ArrayList<>();
            qualityLore.add(ChatColor.GRAY + "Poor → Common → Uncommon");
            qualityLore.add(ChatColor.BLUE + "Rare" + ChatColor.GRAY + " → " + ChatColor.DARK_PURPLE + "Epic" + ChatColor.GRAY + " → " + ChatColor.GOLD + "Legendary");
            qualityLore.add(ChatColor.LIGHT_PURPLE + "Mythic" + ChatColor.GRAY + " → " + ChatColor.RED + "" + ChatColor.BOLD + "Godly");
            qualityMeta.setLore(qualityLore);
            qualityInfo.setItemMeta(qualityMeta);
            inventory.setItem(INFO_SLOTS[4], qualityInfo);
            
            // Slot 5: XP Cost
            ItemStack xpInfo = new ItemStack(Material.EXPERIENCE_BOTTLE);
            ItemMeta xpMeta = xpInfo.getItemMeta();
            xpMeta.setDisplayName(ChatColor.GREEN + "⚡ XP Cost");
            List<String> xpLore = new ArrayList<>();
            xpLore.add(ChatColor.GRAY + "Enchanting requires XP levels");
            xpLore.add(ChatColor.GRAY + "Cost varies by rarity");
            xpLore.add(ChatColor.YELLOW + "Make sure you have enough!");
            xpMeta.setLore(xpLore);
            xpInfo.setItemMeta(xpMeta);
            inventory.setItem(INFO_SLOTS[5], xpInfo);
            
            // Slot 6: Shift-click tip
            ItemStack shiftInfo = new ItemStack(Material.ARROW);
            ItemMeta shiftMeta = shiftInfo.getItemMeta();
            shiftMeta.setDisplayName(ChatColor.YELLOW + "💡 Quick Tip");
            List<String> shiftLore = new ArrayList<>();
            shiftLore.add(ChatColor.GRAY + "Shift-click items and fragments");
            shiftLore.add(ChatColor.GRAY + "from your inventory for faster");
            shiftLore.add(ChatColor.GRAY + "placement!");
            shiftMeta.setLore(shiftLore);
            shiftInfo.setItemMeta(shiftMeta);
            inventory.setItem(INFO_SLOTS[6], shiftInfo);
            
        } else {
            // Count elements
            Map<ElementType, Integer> elementCounts = new HashMap<>();
            for (ItemStack fragment : placedFragments.values()) {
                ElementType element = ElementalFragment.getElement(fragment);
                if (element != null) {
                    elementCounts.put(element, elementCounts.getOrDefault(element, 0) + 1);
                }
            }
            
            // Display each element in separate slots
            int slotIndex = 0;
            for (Map.Entry<ElementType, Integer> entry : elementCounts.entrySet()) {
                if (slotIndex >= INFO_SLOTS.length) break;
                
                ElementType element = entry.getKey();
                int count = entry.getValue();
                
                ItemStack elementInfo = new ItemStack(Material.PAPER);
                ItemMeta meta = elementInfo.getItemMeta();
                meta.setDisplayName(element.getColor() + element.getIcon() + " " + element.getDisplayName());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Count: " + ChatColor.WHITE + count);
                meta.setLore(lore);
                elementInfo.setItemMeta(meta);
                
                inventory.setItem(INFO_SLOTS[slotIndex], elementInfo);
                slotIndex++;
            }
            
            // Check for potential hybrid
            if (elementCounts.size() >= 2) {
                ItemStack hybridInfo = new ItemStack(Material.NETHER_STAR);
                ItemMeta meta = hybridInfo.getItemMeta();
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "⚡ Hybrid Possible!");
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Multiple elements detected");
                lore.add(ChatColor.GRAY + "May form hybrid enchantment");
                meta.setLore(lore);
                hybridInfo.setItemMeta(meta);
                inventory.setItem(INFO_SLOTS[INFO_SLOTS.length - 1], hybridInfo); // Last slot
            }
            
            // Check for anti-synergy conflicts and warn player
            if (itemToEnchant != null && slotIndex < INFO_SLOTS.length) {
                List<String> potentialConflicts = checkPotentialConflicts();
                if (!potentialConflicts.isEmpty()) {
                    ItemStack warningItem = new ItemStack(Material.BARRIER);
                    ItemMeta warningMeta = warningItem.getItemMeta();
                    warningMeta.setDisplayName(ChatColor.RED + "⚠ WARNING: Conflicts Detected!");
                    List<String> warningLore = new ArrayList<>();
                    warningLore.add(ChatColor.GRAY + "Enchanting may remove:");
                    for (String conflict : potentialConflicts) {
                        warningLore.add(ChatColor.RED + "  • " + conflict);
                    }
                    warningLore.add("");
                    warningLore.add(ChatColor.YELLOW + "These enchantments will be");
                    warningLore.add(ChatColor.YELLOW + "replaced by the new one!");
                    warningMeta.setLore(warningLore);
                    warningItem.setItemMeta(warningMeta);
                    
                    // Place warning in first available slot or override last slot if needed
                    if (slotIndex < INFO_SLOTS.length) {
                        inventory.setItem(INFO_SLOTS[slotIndex], warningItem);
                    } else {
                        inventory.setItem(INFO_SLOTS[INFO_SLOTS.length - 1], warningItem);
                    }
                }
            }
        }
    }
    
    /**
     * Checks for potential anti-synergy conflicts between fragments and existing enchantments.
     * Returns a list of enchantment names that might be removed.
     */
    private List<String> checkPotentialConflicts() {
        List<String> conflicts = new ArrayList<>();
        
        if (itemToEnchant == null || placedFragments.isEmpty()) {
            return conflicts;
        }
        
        // Get existing enchantments on the item
        List<EnchantmentData> existingEnchants = 
            EnchantmentData.getEnchantmentsFromItem(itemToEnchant);
        
        if (existingEnchants.isEmpty()) {
            return conflicts; // No existing enchantments to conflict with
        }
        
        // Get all possible enchantments from fragments
        Map<ElementType, Integer> elementCounts = new HashMap<>();
        for (ItemStack fragment : placedFragments.values()) {
            ElementType element = ElementalFragment.getElement(fragment);
            if (element != null) {
                elementCounts.put(element, elementCounts.getOrDefault(element, 0) + 1);
            }
        }
        
        // Get applicable enchantments for these elements
        EnchantmentRegistry registry = EnchantmentRegistry.getInstance();
        List<CustomEnchantment> possibleEnchants = new ArrayList<>();
        
        // Check single element enchantments
        for (ElementType element : elementCounts.keySet()) {
            possibleEnchants.addAll(registry.getEnchantmentsByElement(element));
        }
        
        // Check hybrid enchantments if multiple elements
        if (elementCounts.size() >= 2) {
            for (HybridElement hybridType : HybridElement.values()) {
                ElementType primary = hybridType.getPrimary();
                ElementType secondary = hybridType.getSecondary();
                
                if (elementCounts.containsKey(primary) && elementCounts.containsKey(secondary)) {
                    possibleEnchants.addAll(registry.getEnchantmentsByHybrid(hybridType));
                }
            }
        }
        
        // Check each possible enchantment against existing ones
        for (CustomEnchantment possibleEnchant : possibleEnchants) {
            int[] possibleGroups = possibleEnchant.getAntiSynergyGroups();
            
            if (possibleGroups.length > 0) {
                for (EnchantmentData existingData : existingEnchants) {
                    CustomEnchantment existingEnchant = registry.getEnchantment(existingData.getEnchantmentId());
                    
                    if (existingEnchant != null) {
                        int[] existingGroups = existingEnchant.getAntiSynergyGroups();
                        
                        // Check for group overlap
                        boolean hasConflict = false;
                        for (int possibleGroup : possibleGroups) {
                            for (int existingGroup : existingGroups) {
                                if (possibleGroup == existingGroup) {
                                    hasConflict = true;
                                    break;
                                }
                            }
                            if (hasConflict) break;
                        }
                        
                        if (hasConflict && !conflicts.contains(existingEnchant.getDisplayName())) {
                            conflicts.add(existingEnchant.getDisplayName());
                        }
                    }
                }
            }
        }
        
        return conflicts;
    }
    
    /**
     * Creates a border pane item.
     */
    private ItemStack createBorderPane() {
        ItemStack pane = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }
    
    /**
     * Creates a filler pane item for non-interactive slots.
     */
    private ItemStack createFillerPane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }
    
    /**
     * Creates a separator pane with tooltip.
     */
    private ItemStack createSeparatorPane(String tooltip) {
        ItemStack pane = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(tooltip);
        pane.setItemMeta(meta);
        return pane;
    }
    
    /**
     * Creates a placeholder item.
     */
    private ItemStack createPlaceholder(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (loreLines.length > 0) {
            meta.setLore(Arrays.asList(loreLines));
        }
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Sets the enchanted item in the output slot.
     */
    public void setEnchantedOutput(ItemStack enchantedItem) {
        inventory.setItem(OUTPUT_SLOT, enchantedItem);
    }
    
    /**
     * Gets the item from the output slot.
     */
    public ItemStack getOutputItem() {
        return inventory.getItem(OUTPUT_SLOT);
    }
    
    /**
     * Clears the input slots after successful enchant.
     */
    public void clearInputs() {
        // Clear item slot - leave empty
        itemToEnchant = null;
        inventory.setItem(ITEM_SLOT, null);
        
        // Clear fragment slots - leave empty
        for (int slot : FRAGMENT_SLOTS) {
            inventory.setItem(slot, null);
        }
        
        placedFragments.clear();
        updateEnchantButton();
        updateInfoDisplay();
    }
    
    /**
     * Clears all items and returns them to the player.
     */
    public void returnItems() {
        // Return output item if present
        ItemStack output = inventory.getItem(OUTPUT_SLOT);
        if (output != null && output.getType() != Material.AIR && 
            output.getType() != Material.LIME_STAINED_GLASS_PANE) {
            player.getInventory().addItem(output);
        }
        
        // Return item to enchant
        if (itemToEnchant != null) {
            player.getInventory().addItem(itemToEnchant);
        }
        
        // Return fragments
        for (ItemStack fragment : placedFragments.values()) {
            player.getInventory().addItem(fragment);
        }
        
        placedFragments.clear();
        itemToEnchant = null;
    }
}
