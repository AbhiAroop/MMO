package com.server.crafting.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.crafting.furnace.FurnaceData;
import com.server.crafting.furnace.FurnaceType;
import com.server.crafting.gui.CustomFurnaceGUI;
import com.server.crafting.manager.CustomFurnaceManager;
import com.server.debug.DebugManager.DebugSystem;

/**
 * Handles furnace placement, interaction, and destruction
 * Step 2: Furnace interaction system
 */
public class CustomFurnaceListener implements Listener {
    
    private final Main plugin;
    
    public CustomFurnaceListener(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle furnace placement
     * Step 2: Placement detection
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        
        // Check if a furnace was placed
        if (block.getType() == Material.FURNACE) {
            
            // For now, default to Stone Furnace
            // Later this will be expanded to check for special furnace items
            FurnaceType furnaceType = determineFurnaceType(player, event.getItemInHand());
            
            if (furnaceType != null) {
                // Create custom furnace data
                boolean success = CustomFurnaceManager.getInstance()
                    .createCustomFurnace(block.getLocation(), furnaceType, player);
                
                if (success) {
                    if (plugin.isDebugEnabled(DebugSystem.GUI)) {
                        plugin.debugLog(DebugSystem.GUI,
                            "[Custom Furnace] Player " + player.getName() + 
                            " placed " + furnaceType.getDisplayName() + 
                            " at " + block.getLocation());
                    }
                } else {
                    // Failed to create - cancel placement
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Failed to create custom furnace!");
                }
            }
        }
    }
    
    /**
     * Handle furnace interaction (right-click)
     * Step 2: Furnace access
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.FURNACE) {
            return;
        }
        
        Player player = event.getPlayer();
        CustomFurnaceManager manager = CustomFurnaceManager.getInstance();
        
        // Check if this is a custom furnace
        FurnaceData furnaceData = manager.getFurnaceData(block.getLocation());
        if (furnaceData == null) {
            // Not a custom furnace - let vanilla handle it
            return;
        }
        
        // Cancel vanilla furnace opening
        event.setCancelled(true);
        
        // Grant access and open custom GUI (GUI will be implemented in Step 3)
        manager.grantFurnaceAccess(player, block.getLocation());
        
        // For now, just show furnace info
        showFurnaceInfo(player, furnaceData);
        
        if (plugin.isDebugEnabled(DebugSystem.GUI)) {
            plugin.debugLog(DebugSystem.GUI,
                "[Custom Furnace] Player " + player.getName() + 
                " accessed " + furnaceData.getFurnaceType().getDisplayName() + 
                " at " + block.getLocation());
        }
    }
    
    /**
     * Handle furnace destruction
     * Step 2: Destruction handling
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        if (block.getType() == Material.FURNACE) {
            Player player = event.getPlayer();
            CustomFurnaceManager manager = CustomFurnaceManager.getInstance();
            
            // Check if this is a custom furnace
            FurnaceData furnaceData = manager.getFurnaceData(block.getLocation());
            if (furnaceData != null) {
                
                // Drop furnace contents before removal
                dropFurnaceContents(block.getLocation(), furnaceData);
                
                // Remove the custom furnace data
                manager.removeCustomFurnace(block.getLocation(), player);
                
                if (plugin.isDebugEnabled(DebugSystem.GUI)) {
                    plugin.debugLog(DebugSystem.GUI,
                        "[Custom Furnace] Player " + player.getName() + 
                        " destroyed " + furnaceData.getFurnaceType().getDisplayName() + 
                        " at " + block.getLocation());
                }
            }
        }
    }
    
    /**
     * Determine furnace type from placed item
     * Step 2: Furnace type detection
     */
    private FurnaceType determineFurnaceType(Player player, ItemStack placedItem) {
        // For now, default to STONE_FURNACE
        // Later this will check for special furnace items with NBT data
        
        if (placedItem != null && placedItem.hasItemMeta() && placedItem.getItemMeta().hasDisplayName()) {
            String displayName = placedItem.getItemMeta().getDisplayName();
            
            // Check for custom furnace items by display name
            for (FurnaceType type : FurnaceType.values()) {
                if (displayName.contains(type.getDisplayName())) {
                    return type;
                }
            }
        }
        
        // Default to stone furnace for regular furnaces
        return FurnaceType.STONE_FURNACE;
    }
    
    /**
     * Show furnace information to player - UPDATED: Now opens GUI
     * Step 3: GUI integration
     */
    private void showFurnaceInfo(Player player, FurnaceData furnaceData) {
        // Open the dynamic GUI instead of text info
        CustomFurnaceGUI.openFurnaceGUI(player, furnaceData);
    }
    
    /**
     * Drop all furnace contents when destroyed
     * Step 2: Content preservation
     */
    private void dropFurnaceContents(org.bukkit.Location location, FurnaceData furnaceData) {
        // Drop input items
        for (int i = 0; i < furnaceData.getFurnaceType().getInputSlots(); i++) {
            ItemStack item = furnaceData.getInputSlot(i);
            if (item != null && item.getAmount() > 0) {
                location.getWorld().dropItemNaturally(location, item);
            }
        }
        
        // Drop fuel items
        for (int i = 0; i < furnaceData.getFurnaceType().getFuelSlots(); i++) {
            ItemStack item = furnaceData.getFuelSlot(i);
            if (item != null && item.getAmount() > 0) {
                location.getWorld().dropItemNaturally(location, item);
            }
        }
        
        // Drop output items
        for (int i = 0; i < furnaceData.getFurnaceType().getOutputSlots(); i++) {
            ItemStack item = furnaceData.getOutputSlot(i);
            if (item != null && item.getAmount() > 0) {
                location.getWorld().dropItemNaturally(location, item);
            }
        }
    }
}