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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.abilities.AbilityRegistry;
import com.server.profiles.skills.abilities.passive.mining.VeinMinerAbility;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.core.SubskillType;
import com.server.profiles.skills.skills.mining.subskills.OreExtractionSubskill;
import com.server.profiles.skills.trees.PlayerSkillTreeData;

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
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Check if this is an ore that might be locked
        Material blockType = block.getType();
        if (isOre(blockType)) {
            // Get the OreExtraction subskill
            Skill oreExtractionSkill = SkillRegistry.getInstance().getSubskill(SubskillType.ORE_EXTRACTION);
            if (oreExtractionSkill instanceof OreExtractionSubskill) {
                OreExtractionSubskill oreExtraction = (OreExtractionSubskill) oreExtractionSkill;
                
                // Check if player can mine this ore
                if (!oreExtraction.canMineOre(player, blockType)) {
                    // Cancel event and send message
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You need to unlock the ability to mine this ore first.");
                    return;
                }
            }
        }
        
        // Store the original block type before continuing
        Material originalBlockType = block.getType();
        
        // Skip if not an ore block for fortune calculation
        if (!isOre(originalBlockType)) return;
        
        // Check if this is a vein miner block
        boolean isVeinMinerBlock = block.hasMetadata("veinminer_processed");
        int fortuneMultiplier;
        
        if (isVeinMinerBlock) {
            // Get the mining fortune multiplier passed from the vein miner ability
            fortuneMultiplier = 1;
            if (block.hasMetadata("veinminer_fortune")) {
                fortuneMultiplier = block.getMetadata("veinminer_fortune").get(0).asInt();
                block.removeMetadata("veinminer_fortune", plugin);
            }
            
            // Remove the processed metadata
            block.removeMetadata("veinminer_processed", plugin);
            
            // Debug logging
            if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                plugin.debugLog(DebugSystem.SKILLS,player.getName() + " broke vein miner block " + block.getType() + 
                    " with fortune multiplier: " + fortuneMultiplier);
            }
        } else {
            // Regular block - Process vein miner ability if enabled
            PlayerSkillTreeData treeData = profile.getSkillTreeData();
            int veinMinerLevel = treeData.getNodeLevel(SubskillType.ORE_EXTRACTION.getId(), "vein_miner");
            
            if (veinMinerLevel > 0) {
                // The player has unlocked the vein miner ability through skill tree
                AbilityRegistry abilityRegistry = AbilityRegistry.getInstance();
                VeinMinerAbility veinMinerAbility = (VeinMinerAbility) abilityRegistry.getAbility("vein_miner");
                
                if (veinMinerAbility != null && veinMinerAbility.isEnabled(player)) {
                    // Handle the ability - this will process connected blocks
                    veinMinerAbility.handleBlockBreak(player, block);
                }
            }
            
            // Get mining fortune value directly from player stats
            double miningFortune = profile.getStats().getMiningFortune();
            
            // Calculate fortune multiplier for regular blocks
            fortuneMultiplier = calculateFortuneMultiplier(miningFortune);
        }
        // At this point, we have the correct fortune multiplier for both regular and vein miner blocks
        // Process the block with the determined fortune multiplier
        processBlockWithFortune(event, player, block, fortuneMultiplier);
        SkillEventListener skillEventListener = new SkillEventListener(plugin);
        skillEventListener.processBlockBreakDirectly(player, block, originalBlockType);
        
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
        } else {
            // Fallback for any ore types that might not drop correctly
            Material dropType = getDefaultDropForOre(block.getType());
            if (dropType != null) {
                normalDrops.add(new ItemStack(dropType));
            }
        }
        
        // If we still have no drops, let the vanilla event happen
        if (normalDrops.isEmpty()) {
            return;
        }
        
        // Debug output
        if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
            plugin.debugLog(DebugSystem.SKILLS,player.getName() + " broke " + block.getType() + 
                " with Mining Fortune multiplier: " + fortuneMultiplier);
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
        
        // Award XP if it's an ore
        int blockXP = getBlockXP(block.getType());
        if (blockXP > 0) {
            player.giveExp(blockXP);
        }
    }

    /**
     * Calculate fortune multiplier - MUST MATCH the calculation in VeinMinerAbility
     */
    private int calculateFortuneMultiplier(double miningFortune) {
        // Mining fortune is a percentage-based system:
        // 100 mining fortune = 2x drops (guaranteed)
        // 150 mining fortune = 2x drops + 50% chance for 3x drops
        
        // Calculate guaranteed multiplier (divide by 100 and add 1)
        int guaranteedMultiplier = (int) Math.floor(miningFortune / 100) + 1;
        
        // Calculate chance for an extra drop (remainder percentage)
        double chanceForExtraDrop = (miningFortune % 100);
        
        // Default multiplier is the guaranteed portion
        int fortuneMultiplier = guaranteedMultiplier;
        
        // Check for chance-based extra drop
        if (random.nextDouble() * 100 < chanceForExtraDrop) {
            fortuneMultiplier++;
        }
        
        return fortuneMultiplier;
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
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
                return Material.RAW_COPPER;
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                return Material.DIAMOND;
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                return Material.LAPIS_LAZULI;
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
                return Material.REDSTONE;
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                return Material.EMERALD;
            case NETHER_QUARTZ_ORE:
                return Material.QUARTZ;
            case NETHER_GOLD_ORE:
                return Material.GOLD_NUGGET;
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
        switch (material) {
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
            case NETHER_GOLD_ORE:
            case NETHER_QUARTZ_ORE:
            case ANCIENT_DEBRIS:
                return true;
            default:
                return false;
        }
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