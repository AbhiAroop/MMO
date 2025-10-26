package com.server.profiles.skills.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillProgressionManager;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.core.SubskillType;
import com.server.profiles.skills.skills.farming.subskills.CultivatingSubskill;
import com.server.profiles.skills.skills.farming.subskills.HarvestingSubskill;

/**
 * Handles farming-related events including crop harvesting, planting, and growth
 */
public class FarmingListener implements Listener {
    
    private final Main plugin;
    private final Random random = new Random();
    
    public FarmingListener(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle crop harvesting (Harvesting subskill)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();
        
        // Skip if in creative mode
        if (player.getGameMode() == GameMode.CREATIVE) return;
        
        // Check if this is a crop
        if (!isCrop(blockType)) return;
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Get the Harvesting subskill
        Skill harvestingSkill = SkillRegistry.getInstance().getSubskill(SubskillType.HARVESTING);
        if (!(harvestingSkill instanceof HarvestingSubskill)) return;
        
        HarvestingSubskill harvesting = (HarvestingSubskill) harvestingSkill;
        
        // Check if player can harvest this crop
        if (!harvesting.canHarvestCrop(player, blockType)) {
            // Cancel event and send message
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You need to unlock the ability to harvest " + 
                getCropDisplayName(blockType) + ChatColor.RED + " first!");
            player.sendMessage(ChatColor.YELLOW + "Check your Harvesting skill tree to unlock this crop.");
            return;
        }
        
        // Check if crop is fully grown before awarding XP
        if (!isCropFullyGrown(block)) {
            // Don't give XP for breaking immature crops
            return;
        }
        
        // Apply farming fortune bonus
        double farmingFortune = profile.getStats().getFarmingFortune();
        int fortuneMultiplier = calculateFortuneMultiplier(farmingFortune);
        
        // Debug logging
        if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
            plugin.debugLog(DebugSystem.SKILLS, 
                player.getName() + " broke " + blockType + " with Farming Fortune multiplier: " + fortuneMultiplier);
        }
        
        // If fortune multiplier > 1, modify the drops
        if (fortuneMultiplier > 1) {
            processBlockWithFortune(event, player, block, fortuneMultiplier);
        }
        
        // Award XP for harvesting (only if player can gain XP from this crop)
        if (harvesting.canGainXpFromCrop(player, blockType)) {
            double xpAmount = getCropHarvestXp(blockType);
            
            // Apply XP boost from skill tree
            double xpBoost = harvesting.getSkillTreeBenefits(player).get("xp_boost");
            xpAmount *= (1.0 + xpBoost);
            
            SkillProgressionManager.getInstance().addExperience(player, harvestingSkill, xpAmount);
            
            if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                plugin.debugLog(DebugSystem.SKILLS, 
                    player.getName() + " gained " + xpAmount + " XP in Harvesting for harvesting " + blockType.name());
            }
        }
    }
    
    /**
     * Handle crop planting (Cultivating subskill)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropPlant(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        Material plantableType = item.getType();
        
        // Skip if in creative mode
        if (player.getGameMode() == GameMode.CREATIVE) return;
        
        // Check if this is a plantable crop
        if (!isPlantable(plantableType)) return;
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Get the Cultivating subskill
        Skill cultivatingSkill = SkillRegistry.getInstance().getSubskill(SubskillType.CULTIVATING);
        if (!(cultivatingSkill instanceof CultivatingSubskill)) return;
        
        CultivatingSubskill cultivating = (CultivatingSubskill) cultivatingSkill;
        
        // Check if Cultivating is unlocked (requires Harvesting level 10)
        if (!cultivating.isCultivatingUnlocked(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You need to reach Harvesting level 10 to unlock Cultivating!");
            player.sendMessage(ChatColor.YELLOW + "Keep harvesting wheat to level up your Harvesting skill.");
            return;
        }
        
        // Check if player can plant this crop
        if (!cultivating.canPlantCrop(player, plantableType)) {
            // Cancel event and send message
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You need to unlock the ability to plant " + 
                getCropDisplayName(plantableType) + ChatColor.RED + " first!");
            player.sendMessage(ChatColor.YELLOW + "Check your Cultivating skill tree to unlock this crop.");
            return;
        }
        
        // Award XP for planting (only if player can gain XP from this crop)
        if (cultivating.canGainXpFromPlanting(player, plantableType)) {
            double xpAmount = getCropPlantXp(plantableType);
            
            // Apply XP boost from skill tree
            double xpBoost = cultivating.getSkillTreeBenefits(player).get("xp_boost");
            xpAmount *= (1.0 + xpBoost);
            
            SkillProgressionManager.getInstance().addExperience(player, cultivatingSkill, xpAmount);
            
            if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                plugin.debugLog(DebugSystem.SKILLS, 
                    player.getName() + " gained " + xpAmount + " XP in Cultivating for planting " + plantableType.name());
            }
        }
    }
    
    /**
     * Handle crop growth (Cultivating subskill)
     * This grants small XP bonuses and applies growth speed multipliers
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropGrow(BlockGrowEvent event) {
        Block block = event.getBlock();
        Material cropType = block.getType();
        
        // Check if this is a crop we care about
        if (!isCrop(cropType)) return;
        
        // We don't have a player reference in BlockGrowEvent, so we can't grant XP
        // Instead, we'll apply growth speed multipliers to nearby players
        // For now, just let it grow normally - growth speed can be implemented later
        // via scheduled tasks that check player proximity and cultivating level
        
        if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
            plugin.debugLog(DebugSystem.SKILLS, 
                "Crop grew: " + cropType.name() + " at " + block.getLocation());
        }
    }
    
    /**
     * Process a block with fortune multiplier
     */
    private void processBlockWithFortune(BlockBreakEvent event, Player player, Block block, int fortuneMultiplier) {
        // Save the drops before we cancel the event
        Collection<ItemStack> normalDrops = new ArrayList<>();
        
        // Capture the normal drops that would have been generated
        Collection<ItemStack> vanillaDrops = block.getDrops(player.getInventory().getItemInMainHand());
        if (!vanillaDrops.isEmpty()) {
            normalDrops.addAll(vanillaDrops);
        }
        
        // If we still have no drops, let the vanilla event happen
        if (normalDrops.isEmpty()) {
            return;
        }
        
        // Cancel the default drop behavior
        event.setCancelled(true);
        
        // Create multiplied drops
        List<ItemStack> multipliedDrops = new ArrayList<>();
        for (ItemStack normalDrop : normalDrops) {
            // Create a copy of each drop with the appropriate amount
            ItemStack multipliedDrop = normalDrop.clone();
            multipliedDrop.setAmount(normalDrop.getAmount() * fortuneMultiplier);
            multipliedDrops.add(multipliedDrop);
        }
        
        // Break the block - replace with air so it looks broken
        block.setType(Material.AIR);
        
        // Drop the items in the world
        Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack drop : multipliedDrops) {
            block.getWorld().dropItemNaturally(dropLocation, drop);
        }
    }
    
    /**
     * Calculate fortune multiplier - same formula as mining fortune
     */
    private int calculateFortuneMultiplier(double farmingFortune) {
        // Farming fortune is a percentage-based system:
        // 100 farming fortune = 2x drops (guaranteed)
        // 150 farming fortune = 2x drops + 50% chance for 3x drops
        
        // Calculate guaranteed multiplier (divide by 100 and add 1)
        int guaranteedMultiplier = (int) Math.floor(farmingFortune / 100) + 1;
        
        // Calculate chance for an extra drop (remainder percentage)
        double chanceForExtraDrop = (farmingFortune % 100);
        
        // Default multiplier is the guaranteed portion
        int fortuneMultiplier = guaranteedMultiplier;
        
        // Check for chance-based extra drop
        if (random.nextDouble() * 100 < chanceForExtraDrop) {
            fortuneMultiplier++;
        }
        
        return fortuneMultiplier;
    }
    
    /**
     * Check if a crop is fully grown
     */
    private boolean isCropFullyGrown(Block block) {
        BlockData blockData = block.getBlockData();
        
        // Check if it's an ageable crop (wheat, carrots, potatoes, beetroots, nether wart)
        if (blockData instanceof Ageable) {
            Ageable ageable = (Ageable) blockData;
            return ageable.getAge() == ageable.getMaximumAge();
        }
        
        // For non-ageable crops like melons, pumpkins, cocoa, sweet berries - they're always "grown"
        return true;
    }
    
    /**
     * Check if a material is a crop
     */
    private boolean isCrop(Material material) {
        switch (material) {
            case WHEAT:
            case CARROTS:
            case POTATOES:
            case BEETROOTS:
            case SWEET_BERRY_BUSH:
            case COCOA:
            case NETHER_WART:
            case MELON:
            case PUMPKIN:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Check if a material is plantable
     */
    private boolean isPlantable(Material material) {
        switch (material) {
            case WHEAT_SEEDS:
            case CARROT:
            case POTATO:
            case BEETROOT_SEEDS:
            case SWEET_BERRIES:
            case COCOA_BEANS:
            case NETHER_WART:
            case MELON_SEEDS:
            case PUMPKIN_SEEDS:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Get the XP amount for harvesting a crop
     */
    private double getCropHarvestXp(Material material) {
        switch (material) {
            case WHEAT:
                return 5.0;
            case CARROTS:
            case POTATOES:
                return 6.0;
            case BEETROOTS:
                return 7.0;
            case SWEET_BERRY_BUSH:
                return 4.0;
            case COCOA:
                return 8.0;
            case NETHER_WART:
                return 10.0;
            case MELON:
                return 3.0;
            case PUMPKIN:
                return 8.0;
            default:
                return 0.0;
        }
    }
    
    /**
     * Get the XP amount for planting a crop
     */
    private double getCropPlantXp(Material material) {
        // Planting gives less XP than harvesting (about 1/3)
        switch (material) {
            case WHEAT_SEEDS:
                return 2.0;
            case CARROT:
            case POTATO:
                return 2.5;
            case BEETROOT_SEEDS:
                return 3.0;
            case SWEET_BERRIES:
                return 2.0;
            case COCOA_BEANS:
                return 3.0;
            case NETHER_WART:
                return 4.0;
            case MELON_SEEDS:
                return 1.5;
            case PUMPKIN_SEEDS:
                return 3.0;
            default:
                return 0.0;
        }
    }
    
    /**
     * Get display name for a crop/plantable material
     */
    private String getCropDisplayName(Material material) {
        switch (material) {
            case WHEAT:
            case WHEAT_SEEDS:
                return "Wheat";
            case CARROTS:
            case CARROT:
                return "Carrots";
            case POTATOES:
            case POTATO:
                return "Potatoes";
            case BEETROOTS:
            case BEETROOT_SEEDS:
                return "Beetroots";
            case SWEET_BERRY_BUSH:
            case SWEET_BERRIES:
                return "Sweet Berries";
            case COCOA:
            case COCOA_BEANS:
                return "Cocoa";
            case NETHER_WART:
                return "Nether Wart";
            case MELON:
            case MELON_SEEDS:
                return "Melons";
            case PUMPKIN:
            case PUMPKIN_SEEDS:
                return "Pumpkins";
            default:
                return material.name();
        }
    }
}
