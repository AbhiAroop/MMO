package com.server.enchanting.listeners;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.enchanting.EnchantingLevelCalculator.EnchantingLevel;
import com.server.enchanting.EnchantmentApplicator;
import com.server.enchanting.EnchantmentMaterial;
import com.server.enchanting.EnchantmentRandomizer;
import com.server.enchanting.gui.CustomEnchantingGUI;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Handles interactions with the custom enchanting GUI
 */
public class CustomEnchantingGUIListener implements Listener {
    
    private final Main plugin;
    
    public CustomEnchantingGUIListener(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle inventory clicks in the enchanting GUI
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        // Check if this is our custom enchanting GUI
        if (!CustomEnchantingGUI.GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        
        int slot = event.getRawSlot();
        
        // Handle clicks outside the GUI (in player inventory)
        if (slot >= inventory.getSize()) {
            // Check for shift-clicks from player inventory
            if (event.getClick().isShiftClick() && event.getCurrentItem() != null) {
                handleShiftClickFromPlayerInventory(event, player, event.getCurrentItem());
            }
            return; // Allow normal player inventory interactions
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Enchanting GUI] Player " + player.getName() + " clicked slot " + slot);
        }
        
        // Handle functional slot interactions
        if (CustomEnchantingGUI.isItemSlot(slot)) {
            handleItemSlotClick(event, player);
        } else if (CustomEnchantingGUI.isEnhancementSlot(slot)) {
            handleEnhancementSlotClick(event, player, slot);
        } else if (CustomEnchantingGUI.isConfirmSlot(slot)) {
            handleConfirmSlotClick(event, player);
        } else {
            // Cancel clicks on decorative elements
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle shift-clicks from player inventory
     */
    private void handleShiftClickFromPlayerInventory(InventoryClickEvent event, Player player, ItemStack item) {
        event.setCancelled(true);
        
        Inventory gui = event.getInventory();
        
        // Try to place in item slot first
        if (gui.getItem(21) == null || gui.getItem(21).getType() == Material.AIR) {
            // Check if item can be enchanted
            if (canItemBeEnchanted(item)) {
                gui.setItem(21, item.clone());
                item.setAmount(item.getAmount() - 1);
                
                // Update GUI displays
                updateEnchantingGUI(player);
                
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
                
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI,
                        "[Enchanting GUI] Placed " + item.getType().name() + " in item slot via shift-click");
                }
                return;
            }
        }
        
        // Try to place in enhancement slots if it's enhancement material
        if (EnchantmentMaterial.Registry.isEnhancementMaterial(item)) {
            for (int slot : new int[]{19, 20, 23, 24}) { // Enhancement slots
                if (gui.getItem(slot) == null || gui.getItem(slot).getType() == Material.AIR) {
                    gui.setItem(slot, item.clone());
                    item.setAmount(item.getAmount() - 1);
                    
                    // Update GUI displays
                    updateEnchantingGUI(player);
                    
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
                    
                    if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                        Main.getInstance().debugLog(DebugSystem.GUI,
                            "[Enchanting GUI] Placed enhancement material in slot " + slot + " via shift-click");
                    }
                    return;
                }
            }
        }
        
        // If we couldn't place the item anywhere
        player.sendMessage(ChatColor.RED + "Cannot place this item in the enchanting table!");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
    }
    
    /**
     * Handle clicks in the item slot
     */
    private void handleItemSlotClick(InventoryClickEvent event, Player player) {
        // Allow normal item placement/removal
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            updateEnchantingGUI(player);
        });
    }
    
    /**
     * Handle clicks in enhancement material slots
     */
    private void handleEnhancementSlotClick(InventoryClickEvent event, Player player, int slot) {
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        
        // Check if trying to place an invalid enhancement material
        if (cursorItem != null && cursorItem.getType() != Material.AIR) {
            if (!EnchantmentMaterial.Registry.isEnhancementMaterial(cursorItem)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "This item cannot be used for enchantment enhancement!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
                return;
            }
        }
        
        // Allow normal item placement/removal for valid materials
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            updateEnchantingGUI(player);
        });
    }
    
    /**
     * Handle clicks on the confirm button
     */
    private void handleConfirmSlotClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        Inventory gui = event.getInventory();
        ItemStack confirmButton = gui.getItem(25);
        
        if (confirmButton == null || confirmButton.getType() != Material.EMERALD) {
            player.sendMessage(ChatColor.RED + "Cannot enchant at this time!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
            return;
        }
        
        // Perform the enchantment
        boolean success = performEnchantment(player, gui);
        
        if (success) {
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
            player.sendMessage(ChatColor.GREEN + "✦ Enchantment applied successfully! ✦");
            
            // Update GUI after enchantment
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                updateEnchantingGUI(player);
            }, 1L);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.5f);
        }
    }
    
    /**
     * Perform the actual multi-enchantment process
     */
    private boolean performEnchantment(Player player, Inventory gui) {
        ItemStack itemToEnchant = gui.getItem(21);
        if (itemToEnchant == null || itemToEnchant.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "No item to enchant!");
            return false;
        }
        
        // Get enchanting level and data
        EnchantingLevel enchantingLevel = CustomEnchantingGUI.getPlayerEnchantingLevel(player);
        if (enchantingLevel == null) {
            player.sendMessage(ChatColor.RED + "Enchanting table data not found!");
            return false;
        }
        
        // Get enhancement materials
        ItemStack[] enhancementMaterials = getEnhancementMaterials(gui);
        
        // Generate enchantment selections
        List<EnchantmentRandomizer.EnchantmentSelection> selections = 
            EnchantmentRandomizer.generateEnchantmentSelections(itemToEnchant, enchantingLevel, enhancementMaterials);
        
        if (selections.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No compatible enchantments available!");
            return false;
        }
        
        // Calculate total costs (based on primary enchantment)
        EnchantmentRandomizer.EnchantmentSelection primarySelection = selections.get(0);
        int xpCost = primarySelection.enchantment.getXpCost(primarySelection.level);
        int essenceCost = primarySelection.enchantment.getEssenceCost(primarySelection.level);
        
        // Add costs for additional enchantments (reduced cost)
        for (int i = 1; i < selections.size(); i++) {
            EnchantmentRandomizer.EnchantmentSelection selection = selections.get(i);
            xpCost += (selection.enchantment.getXpCost(selection.level) / (i + 1)); // Reduced cost for additional enchantments
            essenceCost += (selection.enchantment.getEssenceCost(selection.level) / (i + 1));
        }
        
        // Check if player can afford
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[
            ProfileManager.getInstance().getActiveProfile(player.getUniqueId())
        ];
        if (profile == null) {
            player.sendMessage(ChatColor.RED + "Profile data not found!");
            return false;
        }
        
        if (player.getLevel() < xpCost) {
            player.sendMessage(ChatColor.RED + "Insufficient XP! Need " + xpCost + " levels.");
            return false;
        }
        
        if (profile.getEssence() < essenceCost) {
            player.sendMessage(ChatColor.RED + "Insufficient essence! Need " + essenceCost + " essence.");
            return false;
        }
        
        // Execute enchantment selections
        List<EnchantmentRandomizer.AppliedEnchantment> appliedEnchantments = 
            EnchantmentRandomizer.executeEnchantmentSelections(selections);
        
        if (appliedEnchantments.isEmpty()) {
            // Failed enchantment - still consume resources but at reduced cost
            int failedXpCost = xpCost / 2;
            int failedEssenceCost = essenceCost / 2;
            
            player.setLevel(player.getLevel() - failedXpCost);
            profile.removeEssence(failedEssenceCost);
            
            // Consume enhancement materials on failure too
            consumeEnhancementMaterials(gui, enhancementMaterials);
            
            player.sendMessage(ChatColor.RED + "✦ All enchantments failed! ✦");
            player.sendMessage(ChatColor.GRAY + "Lost " + failedXpCost + " XP and " + failedEssenceCost + " essence");
            
            return false;
        }
        
        // Success! Apply multiple enchantments
        ItemStack enchantedItem = EnchantmentApplicator.applyMultipleEnchantments(itemToEnchant, appliedEnchantments);
        
        if (enchantedItem == null) {
            player.sendMessage(ChatColor.RED + "Failed to apply enchantments!");
            return false;
        }
        
        // Consume resources
        player.setLevel(player.getLevel() - xpCost);
        profile.removeEssence(essenceCost);
        
        // Consume enhancement materials
        consumeEnhancementMaterials(gui, enhancementMaterials);
        
        // Replace item in GUI
        gui.setItem(21, enchantedItem);
        
        // Send success message with details
        StringBuilder successMessage = new StringBuilder();
        successMessage.append(ChatColor.GREEN).append("✦ Successfully applied ").append(appliedEnchantments.size()).append(" enchantment");
        if (appliedEnchantments.size() > 1) {
            successMessage.append("s");
        }
        successMessage.append("! ✦");
        
        player.sendMessage(successMessage.toString());
        
        // List applied enchantments
        for (EnchantmentRandomizer.AppliedEnchantment applied : appliedEnchantments) {
            player.sendMessage(ChatColor.GRAY + "• " + applied.enchantment.getFormattedName(applied.level));
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Enchanting] Applied " + appliedEnchantments.size() + " enchantments to " + 
                itemToEnchant.getType().name() + " (Cost: " + xpCost + " XP, " + essenceCost + " essence)");
        }
        
        return true;
    }
    
    /**
     * Consume enhancement materials from the GUI
     */
    private void consumeEnhancementMaterials(Inventory gui, ItemStack[] enhancementMaterials) {
        int[] enhancementSlots = {19, 20, 23, 24};
        
        for (int i = 0; i < enhancementSlots.length; i++) {
            ItemStack material = gui.getItem(enhancementSlots[i]);
            if (material != null && material.getType() != Material.AIR) {
                material.setAmount(material.getAmount() - 1);
                if (material.getAmount() <= 0) {
                    gui.setItem(enhancementSlots[i], null);
                }
            }
        }
    }
    
    /**
     * Handle inventory drag events
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        if (!CustomEnchantingGUI.GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        
        // Check if any dragged slots are outside the allowed functional areas
        for (int slot : event.getRawSlots()) {
            if (slot < event.getInventory().getSize()) {
                if (!CustomEnchantingGUI.isItemSlot(slot) && !CustomEnchantingGUI.isEnhancementSlot(slot)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        
        // Update GUI after drag
        Player player = (Player) event.getWhoClicked();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateEnchantingGUI(player);
        }, 1L);
    }
    
    /**
     * Handle inventory close events
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        
        if (!CustomEnchantingGUI.GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        
        Inventory gui = event.getInventory();
        
        // Return items to player
        returnItemsToPlayer(player, gui);
        
        // Clean up player data
        CustomEnchantingGUI.removePlayerData(player);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Enchanting GUI] " + player.getName() + " closed enchanting GUI");
        }
    }
    
    /**
     * Return all items from functional slots to player
     */
    private void returnItemsToPlayer(Player player, Inventory gui) {
        // Return item from item slot
        ItemStack itemToReturn = gui.getItem(21);
        if (itemToReturn != null && itemToReturn.getType() != Material.AIR) {
            player.getInventory().addItem(itemToReturn);
        }
        
        // Return enhancement materials
        int[] enhancementSlots = {19, 20, 23, 24};
        for (int slot : enhancementSlots) {
            ItemStack material = gui.getItem(slot);
            if (material != null && material.getType() != Material.AIR) {
                player.getInventory().addItem(material);
            }
        }
    }
    
    /**
     * Update the enchanting GUI displays
     */
    private void updateEnchantingGUI(Player player) {
        Inventory gui = CustomEnchantingGUI.getPlayerEnchantingGUI(player);
        com.server.enchanting.EnchantingLevelCalculator.EnchantingLevel enchantingLevel = CustomEnchantingGUI.getPlayerEnchantingLevel(player);
        
        if (gui != null && enchantingLevel != null) {
            CustomEnchantingGUI.updateEnchantingDisplays(gui, player, enchantingLevel);
        }
    }
    
    /**
     * Helper methods from the main GUI class
     */
    private boolean canItemBeEnchanted(ItemStack item) {
        return com.server.enchanting.CustomEnchantmentRegistry.getInstance()
            .getApplicableEnchantments(item).size() > 0;
    }
    
    private com.server.enchanting.CustomEnchantment getAvailableEnchantment(Player player, ItemStack item, 
                                                                            com.server.enchanting.EnchantingLevelCalculator.EnchantingLevel enchantingLevel) {
        // Implementation matches the private method in CustomEnchantingGUI
        if (item == null || item.getType() == Material.AIR) return null;
        
        java.util.List<com.server.enchanting.CustomEnchantment> applicable = 
            com.server.enchanting.CustomEnchantmentRegistry.getInstance().getApplicableEnchantments(item);
        
        com.server.enchanting.CustomEnchantment best = null;
        for (com.server.enchanting.CustomEnchantment enchantment : applicable) {
            int requiredLevel = getRequiredEnchantingLevel(enchantment);
            if (enchantingLevel.getTotalLevel() >= requiredLevel) {
                if (best == null || enchantment.getRarity().ordinal() > best.getRarity().ordinal()) {
                    best = enchantment;
                }
            }
        }
        
        return best;
    }
    
    private ItemStack[] getEnhancementMaterials(Inventory gui) {
        int[] enhancementSlots = {19, 20, 23, 24};
        ItemStack[] materials = new ItemStack[enhancementSlots.length];
        for (int i = 0; i < enhancementSlots.length; i++) {
            materials[i] = gui.getItem(enhancementSlots[i]);
        }
        return materials;
    }
    
    private int calculateEnchantmentLevel(com.server.enchanting.CustomEnchantment enchantment, 
                                        ItemStack[] enhancementMaterials, 
                                        com.server.enchanting.EnchantingLevelCalculator.EnchantingLevel enchantingLevel) {
        int baseLevel = 1;
        
        for (ItemStack material : enhancementMaterials) {
            if (material != null && material.getType() != Material.AIR) {
                EnchantmentMaterial enhancementMaterial = EnchantmentMaterial.Registry.fromItem(material);
                if (enhancementMaterial != null) {
                    baseLevel += enhancementMaterial.getLevelBonus();
                }
            }
        }
        
        return Math.min(baseLevel, enchantment.getMaxLevel());
    }
    
    private double calculateBaseSuccessRate(com.server.enchanting.CustomEnchantment enchantment, 
                                          com.server.enchanting.EnchantingLevelCalculator.EnchantingLevel enchantingLevel) {
        double baseRate = 0.8 - (enchantment.getRarity().ordinal() * 0.1);
        
        int requiredLevel = getRequiredEnchantingLevel(enchantment);
        int playerLevel = enchantingLevel.getTotalLevel();
        
        if (playerLevel > requiredLevel) {
            double levelBonus = Math.min((playerLevel - requiredLevel) * 0.01, 0.2);
            baseRate += levelBonus;
        }
        
        return Math.max(0.1, Math.min(baseRate, 0.95));
    }
    
    private double calculateMaterialBonus(com.server.enchanting.CustomEnchantment enchantment, ItemStack[] enhancementMaterials) {
        double totalBonus = 0.0;
        
        for (ItemStack material : enhancementMaterials) {
            if (material != null && material.getType() != Material.AIR) {
                EnchantmentMaterial enhancementMaterial = EnchantmentMaterial.Registry.fromItem(material);
                if (enhancementMaterial != null) {
                    totalBonus += enhancementMaterial.getEnhancementBonus(enchantment);
                }
            }
        }
        
        return Math.min(totalBonus, 0.4);
    }
    
    private int getRequiredEnchantingLevel(com.server.enchanting.CustomEnchantment enchantment) {
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
}