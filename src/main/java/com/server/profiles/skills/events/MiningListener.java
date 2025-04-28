package com.server.profiles.skills.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Handles mining-related events including mining fortune calculations
 */
public class MiningListener implements Listener {
    
    private final Main plugin;
    private final Random random = new Random();
    
    public MiningListener(Main plugin) {
        this.plugin = plugin;
    }
    
   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Skip if in creative mode
        if (player.getGameMode() == GameMode.CREATIVE) return;
        
        // Skip if not an ore block
        if (!isOre(block.getType())) return;
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Get mining fortune value
        double miningFortune = profile.getStats().getMiningFortune();
        
        // Save the drops before we cancel the event
        Collection<ItemStack> normalDrops = new ArrayList<>();
        
        // Capture the normal drops that would have been generated
        Collection<ItemStack> vanillaDrops = block.getDrops(player.getInventory().getItemInMainHand());
        if (!vanillaDrops.isEmpty()) {
            normalDrops.addAll(vanillaDrops);
        } else {
            // Fallback for any ore types that might not drop correctly
            Material dropType = getDefaultDropForOre(block.getType());
            if (dropType != null) {
                normalDrops.add(new ItemStack(dropType));
            }
        }
        
        // If we still have no drops, let the vanilla event happen
        if (normalDrops.isEmpty()) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("Could not determine drops for " + block.getType() + " - using vanilla behavior");
            }
            return; // Let vanilla handle it
        }
        
        // CRITICAL FIX: Store the block's material and location before cancelling
        Material blockMaterial = block.getType();
        Location blockLocation = block.getLocation().clone();
        
        // Now we can safely cancel the event since we have drops to give
        event.setCancelled(true);
        
        // Break the block without dropping items
        block.setType(Material.AIR);
        
        // Calculate drop multiplier based on mining fortune
        // For 0-99 mining fortune: 1x guaranteed + (0-99)% chance for 2x
        // For 100-199 mining fortune: 2x guaranteed + (0-99)% chance for 3x
        // And so on...
        int guaranteedMultiplier = (int)(miningFortune / 100) + 1; // Always at least 1x drop
        double chanceForExtraTier = (miningFortune % 100) / 100.0;
        
        // Final multiplier is guaranteed + potential extra tier based on chance
        int finalMultiplier = guaranteedMultiplier;
        
        // Check for extra tier based on remaining chance
        boolean gotExtraTier = false;
        if (random.nextDouble() < chanceForExtraTier) {
            finalMultiplier++;
            gotExtraTier = true;
        }
        
        // Debug output
        if (plugin.isDebugMode()) {
            plugin.getLogger().info(player.getName() + " broke " + blockMaterial + 
                " with Mining Fortune: " + miningFortune + 
                " (Guaranteed: " + guaranteedMultiplier + 
                "x, Extra Chance: " + String.format("%.1f%%", chanceForExtraTier * 100) + 
                ") = " + finalMultiplier + "x drops");
        }
        
        // Create multiplied drops
        List<ItemStack> multipliedDrops = new ArrayList<>();
        for (ItemStack normalDrop : normalDrops) {
            ItemStack multipliedDrop = normalDrop.clone();
            multipliedDrop.setAmount(normalDrop.getAmount() * finalMultiplier);
            multipliedDrops.add(multipliedDrop);
        }
        
        // Drop the items at the block location
        Location dropLocation = blockLocation.add(0.5, 0.5, 0.5);
        for (ItemStack drop : multipliedDrops) {
            block.getWorld().dropItemNaturally(dropLocation, drop);
        }
        
        // Notify player about bonus drops
        if (gotExtraTier) {
            String message;
            if (finalMultiplier > 2) {
                // Lucky bonus on top of guaranteed bonus
                message = "§e⚒ §6Mining Fortune §eprovided a §abonus drop§e! (§a+" + String.format("%.1f", chanceForExtraTier * 100) + "% §eluck)";
            } else {
                // Just lucky bonus (fortune < 100)
                message = "§e⚒ §6Mining Fortune §egenerated a §abonus drop§e! (§a+" + String.format("%.1f", chanceForExtraTier * 100) + "% §eluck)";
            }
            player.sendMessage(message);
        }
        
        // Ensure XP is still dropped
        int xpAmount = getBlockXP(blockMaterial);
        if (xpAmount > 0) {
            block.getWorld().spawn(dropLocation, org.bukkit.entity.ExperienceOrb.class).setExperience(xpAmount);
        }
        
        // CRITICAL FIX: Instead of creating a new event, directly call the SkillEventListener
        // This ensures that the correct subskill XP is awarded
        SkillEventListener skillListener = new SkillEventListener(plugin);
        skillListener.processOreExtractionXP(player, blockMaterial);
    }

    /**
     * Get the default drop for an ore type, used as a fallback
     */
    private Material getDefaultDropForOre(Material oreType) {
        switch (oreType) {
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
                return Material.COAL;
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
                return Material.RAW_IRON;
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
                return Material.RAW_GOLD;
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                return Material.DIAMOND;
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                return Material.EMERALD;
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                return Material.LAPIS_LAZULI;
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
                return Material.REDSTONE;
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
                return Material.RAW_COPPER;
            case NETHER_GOLD_ORE:
                return Material.GOLD_NUGGET;
            case NETHER_QUARTZ_ORE:
                return Material.QUARTZ;
            case ANCIENT_DEBRIS:
                return Material.ANCIENT_DEBRIS;
            default:
                return null;
        }
    }
    
    /**
     * Check if a material is an ore
     */
    private boolean isOre(Material material) {
        return material.name().contains("_ORE") || 
               material == Material.ANCIENT_DEBRIS ||
               material == Material.NETHER_GOLD_ORE ||
               material == Material.NETHER_QUARTZ_ORE;
    }
    
    /**
     * Get the XP that should drop from a block type
     */
    private int getBlockXP(Material material) {
        switch (material) {
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
                return 1;
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
            case NETHER_QUARTZ_ORE:
                return 2;
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                return 3;
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                return 5;
            case NETHER_GOLD_ORE:
                return 1;
            case ANCIENT_DEBRIS:
                return 3;
            default:
                return 0;
        }
    }
}