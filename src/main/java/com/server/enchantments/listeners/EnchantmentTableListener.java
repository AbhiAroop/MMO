package com.server.enchantments.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.server.enchantments.gui.EnchantmentTableGUI;
import com.server.enchantments.structure.EnchantmentTableStructure;

/**
 * Listens for player interactions with enchantment altar armor stands
 * and opens the custom GUI for valid altars.
 */
public class EnchantmentTableListener implements Listener {
    
    private final Plugin plugin;
    private final EnchantmentTableStructure structureManager;
    private final EnchantmentGUIListener guiListener;
    
    public EnchantmentTableListener(Plugin plugin, EnchantmentTableStructure structureManager, EnchantmentGUIListener guiListener) {
        this.plugin = plugin;
        this.structureManager = structureManager;
        this.guiListener = guiListener;
    }
    
    @EventHandler
    public void onAltarClick(PlayerInteractEntityEvent event) {
        // Check if right-clicking an entity
        if (event.getRightClicked().getType() != EntityType.ARMOR_STAND) {
            return;
        }
        
        ArmorStand armorStand = (ArmorStand) event.getRightClicked();
        Player player = event.getPlayer();
        
        // Check if this is a valid enchantment altar (has enchanting table helmet)
        ItemStack helmet = armorStand.getEquipment().getHelmet();
        if (helmet == null || helmet.getType() != Material.ENCHANTING_TABLE) {
            return; // Not an altar, ignore silently
        }
        
        plugin.getLogger().info("[Altar] " + player.getName() + " clicked valid altar");
        
        // Check if altar is registered
        if (!structureManager.isRegistered(armorStand)) {
            player.sendMessage(ChatColor.RED + "This altar is not registered!");
            player.sendMessage(ChatColor.GRAY + "An admin must register it with /enchant spawn");
            return;
        }
        
        // Check permissions
        if (!player.hasPermission("mmo.enchant.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use enchantment altars!");
            return;
        }
        
        // Cancel default interaction
        event.setCancelled(true);
        
        // Check if player already has GUI open (prevent double-opening)
        if (guiListener.hasActiveGUI(player)) {
            plugin.getLogger().info("[Altar] Player already has GUI open, ignoring");
            return;
        }
        
        // Open custom GUI
        plugin.getLogger().info("[Altar] Opening GUI for " + player.getName());
        EnchantmentTableGUI gui = new EnchantmentTableGUI(player);
        guiListener.registerGUI(player, gui);
        gui.open();
        
        player.sendMessage(ChatColor.GREEN + "✓ Opened enchantment altar!");
    }
    
    /**
     * Handle right-click on blocks to detect nearby marker armor stands
     * (Marker armor stands can't be clicked directly)
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check if player already has GUI open (prevent double-opening)
        if (guiListener.hasActiveGUI(player)) {
            return;
        }
        
        // Find nearby altar within click radius
        ArmorStand altar = structureManager.findNearbyAltar(player.getLocation());
        if (altar == null) return;
        
        plugin.getLogger().info("Found nearby altar for " + player.getName());
        
        // Check if altar is registered
        if (!structureManager.isRegistered(altar)) {
            player.sendMessage(ChatColor.RED + "This altar is not registered!");
            return;
        }
        
        // Check permissions
        if (!player.hasPermission("mmo.enchant.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use enchantment altars!");
            return;
        }
        
        // Cancel event
        event.setCancelled(true);
        
        // Open custom GUI
        plugin.getLogger().info("Opening GUI for " + player.getName());
        EnchantmentTableGUI gui = new EnchantmentTableGUI(player);
        guiListener.registerGUI(player, gui);
        gui.open();
        
        player.sendMessage(ChatColor.GREEN + "Opened enchantment altar!");
    }
}
