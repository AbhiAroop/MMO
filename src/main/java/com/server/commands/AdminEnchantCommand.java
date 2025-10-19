package com.server.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.server.enchantments.EnchantmentRegistry;
import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentData;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.utils.EquipmentTypeValidator;

/**
 * Admin command to directly add enchantments to held items.
 * Usage: /adminenchant <enchantmentId> [quality]
 */
public class AdminEnchantCommand implements CommandExecutor {
    
    private final EnchantmentRegistry registry;
    
    public AdminEnchantCommand(EnchantmentRegistry registry) {
        this.registry = registry;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("mmo.admin.enchant")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }
        
        // Check if holding an item
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "You must be holding an item!");
            return true;
        }
        
        // Parse arguments
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /adminenchant <enchantmentId> [quality]");
            player.sendMessage(ChatColor.GRAY + "Available qualities: POOR, COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC, GODLY");
            player.sendMessage(ChatColor.GRAY + "Example: /adminenchant emberveil LEGENDARY");
            return true;
        }
        
        String enchantId = args[0].toLowerCase();
        EnchantmentQuality quality = EnchantmentQuality.COMMON; // Default
        
        // Parse quality if provided
        if (args.length >= 2) {
            try {
                quality = EnchantmentQuality.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Invalid quality: " + args[1]);
                player.sendMessage(ChatColor.GRAY + "Available: POOR, COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC, GODLY");
                return true;
            }
        }
        
        // Get enchantment from registry
        CustomEnchantment enchantment = registry.getEnchantment(enchantId);
        if (enchantment == null) {
            player.sendMessage(ChatColor.RED + "Unknown enchantment: " + enchantId);
            player.sendMessage(ChatColor.GRAY + "Available enchantments:");
            for (CustomEnchantment e : registry.getAllEnchantments()) {
                player.sendMessage(ChatColor.GRAY + "  - " + e.getId() + " (" + e.getDisplayName() + ")");
            }
            return true;
        }
        
        // Validate equipment compatibility using custom model data
        if (!EquipmentTypeValidator.canEnchantmentApply(item, enchantment)) {
            player.sendMessage(EquipmentTypeValidator.getIncompatibilityMessage(item, enchantment));
            return true;
        }
        
        // Apply enchantment
        EnchantmentData.addEnchantmentToItem(item, enchantment, quality);
        
        player.sendMessage(ChatColor.GREEN + "✓ Added enchantment: " + enchantment.getDisplayName() + 
                          " " + quality.getColor() + "[" + quality.getDisplayName() + "]");
        player.sendMessage(ChatColor.GRAY + "Item has been updated in your hand.");
        
        return true;
    }
}
