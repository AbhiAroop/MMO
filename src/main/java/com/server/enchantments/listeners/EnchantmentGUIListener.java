package com.server.enchantments.listeners;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.enchantments.gui.EnchantmentApplicator;
import com.server.enchantments.gui.EnchantmentTableGUI;
import com.server.enchantments.items.ElementalFragment;

/**
 * Handles inventory interactions within the enchantment table GUI.
 */
public class EnchantmentGUIListener implements Listener {
    
    private final Map<UUID, EnchantmentTableGUI> activeGUIs;
    
    public EnchantmentGUIListener() {
        this.activeGUIs = new HashMap<>();
    }
    
    /**
     * Registers a GUI for a player.
     */
    public void registerGUI(Player player, EnchantmentTableGUI gui) {
        activeGUIs.put(player.getUniqueId(), gui);
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Enchant] Registered GUI for " + player.getName() + " (UUID: " + player.getUniqueId() + ")");
        }
    }
    
    /**
     * Checks if a player has an active GUI.
     */
    public boolean hasActiveGUI(Player player) {
        return activeGUIs.containsKey(player.getUniqueId());
    }
    
    /**
     * Gets the active GUI for a player.
     */
    private EnchantmentTableGUI getGUI(Player player) {
        EnchantmentTableGUI gui = activeGUIs.get(player.getUniqueId());
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Enchant] getGUI for " + player.getName() + " (UUID: " + player.getUniqueId() + ") = " + (gui != null));
        }
        return gui;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        EnchantmentTableGUI gui = getGUI(player);
        
        // Debug: Check if GUI is found
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            String title = event.getView().getTitle();
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Enchant] Click detected. Title: " + title + ", GUI found: " + (gui != null));
        }
        
        if (gui == null) {
            return;
        }
        
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }
        
        // Check if clicking in the GUI
        if (clickedInventory.equals(gui.getInventory())) {
            handleGUIClick(event, player, gui);
        } else if (clickedInventory.equals(player.getInventory())) {
            handlePlayerInventoryClick(event, player, gui);
        }
    }
    
    /**
     * Handles clicks within the GUI itself.
     */
    private void handleGUIClick(InventoryClickEvent event, Player player, EnchantmentTableGUI gui) {
        int slot = event.getSlot();
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        
        // ALWAYS cancel by default - only specific slots will be un-cancelled
        event.setCancelled(true);
        
        // Debug logging
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Enchant GUI] Slot " + slot + " clicked. Cancelled: true. Item: " + 
                (current != null ? current.getType() : "null"));
        }
        
        // Handle item slot
        if (gui.isItemSlot(slot)) {
            // Validate what can be placed here
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (ElementalFragment.isFragment(cursor)) {
                    player.sendMessage(ChatColor.RED + "Cannot enchant a fragment!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                    return; // Stay cancelled
                }
                
                if (!gui.canEnchant(cursor)) {
                    player.sendMessage(ChatColor.RED + "This item cannot be enchanted!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                    return; // Stay cancelled
                }
                
                // Valid item being placed - play success sound
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.2f);
            } else if (current != null && current.getType() != Material.AIR) {
                // Item being removed - play pickup sound
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 0.9f);
            }
            // Validation passed - un-cancel to allow natural Minecraft behavior
            event.setCancelled(false);
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, "[Enchant GUI] Item slot - un-cancelled");
            }
            
            // Schedule GUI sync after click completes (1 tick later)
            org.bukkit.Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                gui.syncWithInventory();
            }, 1L);
            return;
        }
        
        // Handle fragment slots
        if (gui.isFragmentSlot(slot)) {
            // Validate what can be placed here
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (!ElementalFragment.isFragment(cursor)) {
                    player.sendMessage(ChatColor.RED + "Only fragments can be placed here!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                    return; // Stay cancelled
                }
                // Valid fragment being placed - play mystic sound
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_PLACE, 0.7f, 1.3f);
            } else if (current != null && current.getType() != Material.AIR) {
                // Fragment being removed - play pickup sound
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.5f, 1.1f);
            }
            // Validation passed - un-cancel to allow natural Minecraft behavior
            event.setCancelled(false);
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, "[Enchant GUI] Fragment slot - un-cancelled");
            }
            
            // Schedule GUI sync after click completes (1 tick later)
            org.bukkit.Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                gui.syncWithInventory();
            }, 1L);
            return;
        }
        
        // Handle enchant button
        if (slot == EnchantmentTableGUI.ENCHANT_BUTTON_SLOT) {
            if (gui.isReadyToEnchant()) {
                // IMPORTANT: Re-validate the item before enchanting
                ItemStack itemToEnchant = gui.getItemToEnchant();
                if (itemToEnchant != null && !gui.canEnchant(itemToEnchant)) {
                    player.sendMessage(ChatColor.RED + "This item cannot be enchanted!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                    return; // Stay cancelled
                }
                
                // Play enchanting initiation sound
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
                handleEnchantment(player, gui);
            } else {
                player.sendMessage(ChatColor.RED + "Cannot enchant! Place an item and 1-3 fragments.");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            }
            return; // Stay cancelled
        }
        
        // Handle cancel button
        if (slot == EnchantmentTableGUI.CANCEL_BUTTON_SLOT) {
            // Return items to player
            if (gui.getItemToEnchant() != null) {
                player.getInventory().addItem(gui.getItemToEnchant());
            }
            for (ItemStack fragment : gui.getFragments()) {
                if (fragment != null) {
                    player.getInventory().addItem(fragment);
                }
            }
            
            // Play cancel/close sound
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_CLOSE, 0.7f, 1.0f);
            player.closeInventory();
            return; // Stay cancelled
        }
        
        // Handle output slot - ONLY allow taking the enchanted result, NOT the placeholder
        if (gui.isOutputSlot(slot)) {
            // Block placing items with cursor
            if (cursor != null && cursor.getType() != Material.AIR) {
                player.sendMessage(ChatColor.RED + "You cannot place items in the output slot!");
                return; // Stay cancelled
            }
            
            // Check if there's an item in the slot
            if (current == null || current.getType() == Material.AIR) {
                return; // Empty slot, stay cancelled
            }
            
            // Check if it's the placeholder
            if (current.getType() == Material.LIME_STAINED_GLASS_PANE) {
                // It's the placeholder - don't allow taking it, stay cancelled
                if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                    Main.getInstance().debugLog(DebugSystem.GUI, "[Enchant GUI] Output slot - blocked placeholder");
                }
                return;
            }
            
            // It's an actual enchanted item - manually give it to player
            player.getInventory().addItem(current);
            gui.getInventory().setItem(slot, createOutputPlaceholder());
            player.sendMessage(ChatColor.GREEN + "✓ Enchanted item received!");
            
            // Play success sound for taking enchanted item
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_NETHERITE, 0.8f, 1.2f);
            
            // Update the enchant button state after taking item
            org.bukkit.Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                gui.syncWithInventory();
            }, 1L);
            
            if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
                Main.getInstance().debugLog(DebugSystem.GUI, "[Enchant GUI] Output slot - gave enchanted item");
            }
            return; // Stay cancelled (already handled manually)
        }
        
        // ALL OTHER SLOTS - stay cancelled (already done at top)
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI, "[Enchant GUI] Decorative slot - stayed cancelled");
        }
    }
    
    /**
     * Handles clicks in player inventory while GUI is open.
     */
    private void handlePlayerInventoryClick(InventoryClickEvent event, Player player, EnchantmentTableGUI gui) {
        // Handle shift-clicking items from player inventory into GUI
        if (event.isShiftClick()) {
            ItemStack clickedItem = event.getCurrentItem();
            
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return; // Nothing to shift-click
            }
            
            // Check if it's a valid item to enchant
            if (gui.canEnchant(clickedItem)) {
                // Try to place in item slot
                if (gui.getItemToEnchant() == null) {
                    event.setCancelled(true);
                    gui.setItemToEnchant(clickedItem);
                    event.setCurrentItem(null);
                    player.sendMessage(ChatColor.GREEN + "Item placed in enchanting slot.");
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Item slot is already occupied.");
                }
                return;
            }
            
            // Check if it's a fragment
            if (ElementalFragment.isFragment(clickedItem)) {
                // Try to find an empty fragment slot
                for (int slot : EnchantmentTableGUI.FRAGMENT_SLOTS) {
                    ItemStack slotItem = gui.getInventory().getItem(slot);
                    if (slotItem == null || slotItem.getType() == Material.AIR) {
                        event.setCancelled(true);
                        gui.setFragment(slot, clickedItem);
                        event.setCurrentItem(null);
                        player.sendMessage(ChatColor.GREEN + "Fragment placed.");
                        return;
                    }
                }
                // No empty slots found
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "All fragment slots are full (maximum 3).");
            }
            
            // Not a valid item or fragment - allow normal behavior
        }
    }
    
    /**
     * Handles the enchantment process.
     */
    private void handleEnchantment(Player player, EnchantmentTableGUI gui) {
        ItemStack item = gui.getItemToEnchant();
        List<ItemStack> fragments = gui.getFragments();
        
        // Attempt enchantment
        EnchantmentApplicator.EnchantmentResult result = 
            EnchantmentApplicator.enchantItem(player, item, fragments);
        
        if (result.isSuccess()) {
            // Success!
            player.sendMessage(result.getMessage());
            
            // Place enchanted item in output slot
            gui.setEnchantedOutput(result.getEnchantedItem());
            
            // Clear input slots (item and fragments consumed)
            gui.clearInputs();
            
            // DO NOT close GUI - let player take output item
            
        } else {
            // Failed
            player.sendMessage(result.getMessage());
        }
    }
    
    /**
     * Creates a placeholder for the output slot.
     */
    private ItemStack createOutputPlaceholder() {
        ItemStack placeholder = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Output Slot");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Enchanted item appears here"));
            placeholder.setItemMeta(meta);
        }
        return placeholder;
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        EnchantmentTableGUI gui = getGUI(player);
        
        if (gui == null) {
            return;
        }
        
        // Prevent dragging items in the GUI
        for (int slot : event.getRawSlots()) {
            if (slot < gui.getInventory().getSize()) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Please place items one at a time.");
                return;
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        EnchantmentTableGUI gui = getGUI(player);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Enchant] onInventoryClose for " + player.getName() + ", GUI found: " + (gui != null));
        }
        
        if (gui == null) {
            return;
        }
        
        // Return items to player
        gui.returnItems();
        
        // Remove from active GUIs
        activeGUIs.remove(player.getUniqueId());
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Enchant] Removed GUI for " + player.getName());
        }
        
        player.sendMessage(ChatColor.GRAY + "Enchantment altar closed. Items returned.");
    }
}
