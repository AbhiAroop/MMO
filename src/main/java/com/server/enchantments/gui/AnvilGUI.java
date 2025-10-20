package com.server.enchantments.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.enchantments.items.CustomAnvil;
import com.server.enchantments.items.ElementalFragment;

/**
 * Custom Anvil GUI for combining items, tomes, and fragments.
 * 
 * Layout (27 slots - 3 rows):
 * Row 1 (0-8): Decorative border
 * Row 2 (9-17): Input 1 (11), Plus sign (13), Input 2 (15)
 * Row 3 (18-26): Output slot (22), borders around
 */
public class AnvilGUI {
    
    private static final String GUI_TITLE = ChatColor.DARK_GRAY + "⚒ " + ChatColor.GRAY + "Custom Anvil " + ChatColor.DARK_GRAY + "⚒";
    private static final int GUI_SIZE = 27; // 3 rows
    
    // Slot positions
    public static final int INPUT_SLOT_1 = 11; // First input (left)
    public static final int INPUT_SLOT_2 = 15; // Second input (right)
    public static final int OUTPUT_SLOT = 22; // Output (center bottom)
    public static final int INFO_SLOT = 13; // Plus sign between inputs
    
    private final Player player;
    private final Inventory inventory;
    private ItemStack input1;
    private ItemStack input2;
    private ItemStack previewOutput;
    private int xpCost;
    private int essenceCost;
    
    public AnvilGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        this.input1 = null;
        this.input2 = null;
        this.previewOutput = null;
        this.xpCost = 0;
        this.essenceCost = 0;
        
        initializeGUI();
    }
    
    /**
     * Initializes the GUI with decorative elements.
     */
    private void initializeGUI() {
        // Fill with black glass panes
        ItemStack fillerPane = createFillerPane();
        for (int i = 0; i < GUI_SIZE; i++) {
            inventory.setItem(i, fillerPane);
        }
        
        // Fill borders with decorative glass
        ItemStack borderPane = createBorderPane();
        
        // Top row
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, borderPane);
        }
        
        // Bottom row borders
        for (int i = 18; i < 27; i++) {
            if (i != OUTPUT_SLOT) {
                inventory.setItem(i, borderPane);
            }
        }
        
        // Side borders
        inventory.setItem(9, borderPane);
        inventory.setItem(17, borderPane);
        
        // Clear input slots
        inventory.setItem(INPUT_SLOT_1, null);
        inventory.setItem(INPUT_SLOT_2, null);
        
        // Plus sign indicator
        updateInfoSlot();
        
        // Output slot placeholder
        inventory.setItem(OUTPUT_SLOT, createOutputPlaceholder());
    }
    
    /**
     * Opens the GUI for the player.
     */
    public void open() {
        player.openInventory(inventory);
    }
    
    /**
     * Gets the inventory.
     */
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Gets the player.
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Checks if a slot is an input slot.
     */
    public boolean isInputSlot(int slot) {
        return slot == INPUT_SLOT_1 || slot == INPUT_SLOT_2;
    }
    
    /**
     * Checks if a slot is the output slot.
     */
    public boolean isOutputSlot(int slot) {
        return slot == OUTPUT_SLOT;
    }
    
    /**
     * Gets input 1.
     */
    public ItemStack getInput1() {
        return input1;
    }
    
    /**
     * Gets input 2.
     */
    public ItemStack getInput2() {
        return input2;
    }
    
    /**
     * Gets the preview output.
     */
    public ItemStack getPreviewOutput() {
        return previewOutput;
    }
    
    /**
     * Gets XP cost.
     */
    public int getXpCost() {
        return xpCost;
    }
    
    /**
     * Gets essence cost.
     */
    public int getEssenceCost() {
        return essenceCost;
    }
    
    /**
     * Sets the costs.
     */
    public void setCosts(int xp, int essence) {
        this.xpCost = xp;
        this.essenceCost = essence;
    }
    
    /**
     * Syncs the GUI with inventory contents.
     */
    public void syncWithInventory() {
        // Read input slots
        ItemStack slot1 = inventory.getItem(INPUT_SLOT_1);
        ItemStack slot2 = inventory.getItem(INPUT_SLOT_2);
        
        input1 = (slot1 != null && slot1.getType() != Material.AIR) ? slot1.clone() : null;
        input2 = (slot2 != null && slot2.getType() != Material.AIR) ? slot2.clone() : null;
        
        // Update info slot
        updateInfoSlot();
        
        // Calculate preview (done by listener calling AnvilCombiner)
    }
    
    /**
     * Sets the preview output with uncertainty flag.
     */
    public void setPreviewOutput(ItemStack result, int xp, int essence, boolean hasUncertainty) {
        this.previewOutput = result;
        this.xpCost = xp;
        this.essenceCost = essence;
        
        if (result != null) {
            // Show preview with cost info
            ItemStack preview = result.clone();
            ItemMeta meta = preview.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.GOLD + "Cost:");
                lore.add(ChatColor.YELLOW + "  " + xp + " XP Levels");
                lore.add(ChatColor.YELLOW + "  " + essence + " Essence");
                lore.add("");
                
                if (hasUncertainty) {
                    lore.add(ChatColor.GOLD + "⚠ Warning ⚠");
                    lore.add(ChatColor.GRAY + "Some enchantments may fail");
                    lore.add(ChatColor.GRAY + "based on their apply chance!");
                    lore.add("");
                }
                
                lore.add(ChatColor.GREEN + "Click to combine!");
                meta.setLore(lore);
                preview.setItemMeta(meta);
            }
            inventory.setItem(OUTPUT_SLOT, preview);
            updateInfoSlot(true, hasUncertainty); // Show green or orange
        } else {
            inventory.setItem(OUTPUT_SLOT, createOutputPlaceholder(false));
            updateInfoSlot(false, false); // Show red - invalid
        }
    }
    
    /**
     * Sets the preview output (backwards compatibility - no uncertainty).
     */
    public void setPreviewOutput(ItemStack result, int xp, int essence) {
        setPreviewOutput(result, xp, essence, false);
    }
    
    /**
     * Clears the preview output.
     */
    public void clearPreview() {
        this.previewOutput = null;
        this.xpCost = 0;
        this.essenceCost = 0;
        
        // Check if we have inputs - if yes, show red (invalid), if no, show default
        boolean hasInputs = (input1 != null && input1.getType() != Material.AIR) || 
                           (input2 != null && input2.getType() != Material.AIR);
        
        inventory.setItem(OUTPUT_SLOT, createOutputPlaceholder(false));
        updateInfoSlot(false, false);
    }
    
    /**
     * Clears input slots after successful combination.
     */
    public void clearInputs() {
        input1 = null;
        input2 = null;
        inventory.setItem(INPUT_SLOT_1, null);
        inventory.setItem(INPUT_SLOT_2, null);
        clearPreview();
        updateInfoSlot();
    }
    
    /**
     * Updates the info slot (plus sign) based on whether inputs are valid.
     */
    private void updateInfoSlot() {
        updateInfoSlot(false, false);
    }
    
    /**
     * Updates the info slot with validation state.
     */
    public void updateInfoSlot(boolean hasValidCombination, boolean hasUncertainty) {
        Material glassMaterial;
        ChatColor color;
        String symbol;
        
        if (hasUncertainty) {
            // Orange/yellow warning for uncertain outcomes
            glassMaterial = Material.ORANGE_STAINED_GLASS_PANE;
            color = ChatColor.GOLD;
            symbol = "⚠";
        } else if (hasValidCombination) {
            // Green for valid, certain combinations
            glassMaterial = Material.LIME_STAINED_GLASS_PANE;
            color = ChatColor.GREEN;
            symbol = "+";
        } else {
            // Red for invalid
            glassMaterial = Material.RED_STAINED_GLASS_PANE;
            color = ChatColor.RED;
            symbol = "✗";
        }
        
        ItemStack info = new ItemStack(glassMaterial);
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + symbol);
            
            List<String> lore = new ArrayList<>();
            if (hasUncertainty) {
                lore.add(ChatColor.GRAY + "Uncertain outcome!");
                lore.add(ChatColor.GRAY + "Some enchantments");
                lore.add(ChatColor.GRAY + "may fail to apply");
            } else if (hasValidCombination) {
                lore.add(ChatColor.GREEN + "Valid combination!");
                lore.add(ChatColor.GRAY + "Check output below");
            } else {
                lore.add(ChatColor.RED + "Invalid combination");
                lore.add("");
                lore.add(ChatColor.GRAY + "Place items in slots:");
                lore.add(ChatColor.YELLOW + "Left: Primary item");
                lore.add(ChatColor.YELLOW + "Right: Secondary item");
            }
            
            meta.setLore(lore);
            info.setItemMeta(meta);
        }
        inventory.setItem(INFO_SLOT, info);
    }
    
    /**
     * Creates a filler pane.
     */
    private ItemStack createFillerPane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }
    
    /**
     * Creates a border pane.
     */
    private ItemStack createBorderPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }
    
    /**
     * Creates output placeholder.
     */
    private ItemStack createOutputPlaceholder() {
        return createOutputPlaceholder(false);
    }
    
    /**
     * Creates output placeholder with validation state.
     */
    public ItemStack createOutputPlaceholder(boolean hasValidCombination) {
        Material glassMaterial = hasValidCombination ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        ChatColor color = hasValidCombination ? ChatColor.GREEN : ChatColor.RED;
        String text = hasValidCombination ? "⬛ Output" : "⬛ No valid combination";
        
        ItemStack placeholder = new ItemStack(glassMaterial);
        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + text);
            
            List<String> lore = new ArrayList<>();
            if (hasValidCombination) {
                lore.add(ChatColor.GRAY + "Result appears here");
            } else {
                lore.add(ChatColor.GRAY + "Place valid items to combine");
            }
            
            meta.setLore(lore);
            placeholder.setItemMeta(meta);
        }
        return placeholder;
    }
    
    /**
     * Validates if an item can be placed in input slot 1.
     */
    public boolean canPlaceInSlot1(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return true;
        
        // Cannot place anvil itself
        if (CustomAnvil.isCustomAnvil(item)) return false;
        
        // Cannot place fragments in slot 1
        if (ElementalFragment.isFragment(item)) return false;
        
        // Allow custom items (weapons/armor) and enchanted tomes
        return true;
    }
    
    /**
     * Validates if an item can be placed in input slot 2.
     */
    public boolean canPlaceInSlot2(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return true;
        
        // Cannot place anvil itself
        if (CustomAnvil.isCustomAnvil(item)) return false;
        
        // Allow custom items, enchanted tomes, and fragments
        return true;
    }
}
