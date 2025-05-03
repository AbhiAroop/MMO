package com.server.profiles.skills.abilities.passive.mining;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.abilities.passive.AbstractPassiveAbility;
import com.server.profiles.skills.core.SubskillType;
import com.server.profiles.skills.trees.PlayerSkillTreeData;
import com.server.profiles.stats.PlayerStats;

/**
 * Vein Miner ability - automatically mines connected ore blocks
 */
public class VeinMinerAbility extends AbstractPassiveAbility {

    private static final int MAX_BLOCKS = 10; // Maximum number of blocks to mine in a vein
    private final Random random = new Random();
    
    public VeinMinerAbility() {
        super(
            "vein_miner",           // ID - must match skill tree node ID
            "Vein Miner",          // Display name
            "Automatically mines connected ore blocks when you mine an ore",
            SubskillType.ORE_EXTRACTION.getId(), // This is "ore_extraction"
            Material.IRON_PICKAXE,
            "Unlock the Vein Miner node in the Ore Extraction skill tree"
        );
    }

    @Override
    public void onEnable(Player player) {
        player.sendMessage(ChatColor.GREEN + "Vein Miner ability enabled.");
    }

    @Override
    public void onDisable(Player player) {
        player.sendMessage(ChatColor.RED + "Vein Miner ability disabled.");
    }
    
    @Override
    public boolean isUnlocked(Player player) {
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return false;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return false;
        
        // Check if the player has unlocked the vein_miner node in the ore_extraction skill tree
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        
        // The player has unlocked the ability if they have at least level 1 in the vein_miner node
        return treeData.getNodeLevel(SubskillType.ORE_EXTRACTION.getId(), "vein_miner") > 0;
    }
    
    /**
     * Get the maximum number of blocks that can be mined based on node level
     */
    public int getMaxVeinSize(Player player) {
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return 0;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return 0;
        
        // Get the node level
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        int nodeLevel = treeData.getNodeLevel(SubskillType.ORE_EXTRACTION.getId(), "vein_miner");
        
        // Return the appropriate vein size based on node level
        // Updated to support 9 levels
        switch (nodeLevel) {
            case 1: return 2;
            case 2: return 3;
            case 3: return 4;
            case 4: return 5;
            case 5: return 6;
            case 6: return 7;
            case 7: return 8;
            case 8: return 9;
            case 9: return 10;
            default: return 0; // Not unlocked
        }
    }
    
    /**
     * Handle vein mining when a player breaks an ore block
     * Should be called from an event listener
     */
    public void handleBlockBreak(Player player, Block block) {
        if (!isEnabled(player) || !isUnlocked(player)) return;
        
        if (!isOre(block.getType())) return;
        
        // Get max vein size based on node level
        int maxVeinSize = getMaxVeinSize(player);
        if (maxVeinSize <= 0) return;
        
        // Save the original block type before it's broken
        Material sourceType = block.getType();
        
        // Create a set to store blocks to mine
        Set<Block> blocksToMine = new HashSet<>();
        
        // Find connected ore blocks
        findConnectedOres(block, sourceType, blocksToMine, maxVeinSize);
        
        // Remove the source block from the set (it's already being broken by the event)
        blocksToMine.remove(block);
        
        // If no additional blocks found, just return (single block doesn't cost mana)
        if (blocksToMine.isEmpty()) return;
        
        // Get player profile for mining fortune & mana check
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // MANA SYSTEM: Calculate how much mana this will cost
        int blocksCount = blocksToMine.size();
        int manaCost = calculateManaCost(blocksCount);
        
        // Get current player mana
        PlayerStats stats = profile.getStats();
        int currentMana = stats.getMana();
        
        // Check if player has enough mana
        if (currentMana < manaCost) {
            // Not enough mana - limit blocks to what the player can afford
            int affordableBlocks = Math.max(0, currentMana / 10);
            
            if (affordableBlocks == 0) {
                player.sendMessage(ChatColor.RED + "Not enough mana to use Vein Miner!");
                return;
            }
            
            // Limit blocks to what player can afford
            if (blocksToMine.size() > affordableBlocks) {
                // We need to reduce the blocks to mine - take only the closest ones
                List<Block> sortedBlocks = sortBlocksByDistance(blocksToMine, block);
                blocksToMine = new HashSet<>(sortedBlocks.subList(0, affordableBlocks));
                
                // Recalculate mana cost
                manaCost = calculateManaCost(affordableBlocks);
            }
        }
        
        // Deduct mana
        stats.setMana(currentMana - manaCost);
        
        // Get mining fortune directly from the player's stats
        double miningFortune = stats.getMiningFortune();
        
        // Calculate the fortune multiplier that will be applied
        int fortuneMultiplier = calculateFortuneMultiplier(miningFortune);
        
        // Mine the additional blocks
        int blocksMinedCount = 0;
        for (Block oreBlock : blocksToMine) {
            // Skip if block was removed or changed since we found it
            if (oreBlock.getType() != sourceType) continue;
            
            // Mark this block as being processed by vein miner 
            oreBlock.setMetadata("veinminer_processed", 
                new FixedMetadataValue(Main.getInstance(), true));
            
            // Apply mining fortune to the block
            oreBlock.setMetadata("veinminer_fortune", 
                new FixedMetadataValue(Main.getInstance(), fortuneMultiplier));
            
            // Debug logging
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().info("Vein miner breaking block with fortune multiplier: " + fortuneMultiplier);
            }
            
            // IMPORTANT: We need to manually handle the drops for these blocks
            // to ensure they receive the same mining fortune as the original block
            
            // Get the drops for this block
            Collection<ItemStack> vanillaDrops = oreBlock.getDrops(player.getInventory().getItemInMainHand());
            List<ItemStack> drops = new ArrayList<>();
            
            if (!vanillaDrops.isEmpty()) {
                drops.addAll(vanillaDrops);
            } else {
                // Fallback for any ore types that might not drop correctly
                Material dropType = getDefaultDropForOre(oreBlock.getType());
                if (dropType != null) {
                    drops.add(new ItemStack(dropType));
                }
            }
            
            // Apply fortune multiplier to the drops
            List<ItemStack> multipliedDrops = new ArrayList<>();
            for (ItemStack drop : drops) {
                ItemStack multipliedDrop = drop.clone();
                multipliedDrop.setAmount(drop.getAmount() * fortuneMultiplier);
                multipliedDrops.add(multipliedDrop);
            }
            
            // Set the block to air
            oreBlock.setType(Material.AIR);
            
            // Drop the items in the world
            Location dropLocation = oreBlock.getLocation().add(0.5, 0.5, 0.5);
            for (ItemStack multipliedDrop : multipliedDrops) {
                oreBlock.getWorld().dropItemNaturally(dropLocation, multipliedDrop);
            }
            
            // Award vanilla XP
            int blockXP = getBlockXP(oreBlock.getType());
            if (blockXP > 0) {
                player.giveExp(blockXP);
            }
            
            blocksMinedCount++;
        }
        
        // Only show messages and play sounds if we actually mined MORE THAN 1 additional block
        if (blocksMinedCount > 1) {
            // Play sound effect to indicate vein miner worked
            player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 0.5f, 1.0f);
            
            // Send message with mana cost information
            player.sendMessage(ChatColor.GREEN + "Vein Miner: " + ChatColor.YELLOW + 
                            "Mined " + blocksMinedCount + " additional blocks! " + 
                            ChatColor.BLUE + "(" + manaCost + " mana)");
        } 
        // Just play a subtle sound for a single additional block, but no message
        else if (blocksMinedCount == 1) {
            player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 0.3f, 1.2f);
        }
    }

    /**
     * Calculate mana cost for vein mining
     * Base cost is 10 mana per block
     */
    private int calculateManaCost(int blockCount) {
        // Single blocks don't cost mana
        if (blockCount <= 0) return 0;
        
        // 10 mana per block
        return blockCount * 10;
    }

    /**
     * Sort blocks by distance from the source block
     */
    private List<Block> sortBlocksByDistance(Set<Block> blocks, Block sourceBlock) {
        List<Block> sortedBlocks = new ArrayList<>(blocks);
        
        // Sort blocks by distance from source
        sortedBlocks.sort((b1, b2) -> {
            double d1 = getBlockDistance(sourceBlock, b1);
            double d2 = getBlockDistance(sourceBlock, b2);
            return Double.compare(d1, d2);
        });
        
        return sortedBlocks;
    }

    /**
     * Calculate distance between blocks
     */
    private double getBlockDistance(Block source, Block target) {
        return source.getLocation().distance(target.getLocation());
    }

    @Override
    protected void addPassiveDetailsToLore(List<String> lore) {
        lore.add("");
        lore.add(ChatColor.GRAY + "When enabled, this ability will");
        lore.add(ChatColor.GRAY + "automatically mine connected ore blocks");
        lore.add(ChatColor.GRAY + "when you mine an ore.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Maximum blocks: " + ChatColor.WHITE + MAX_BLOCKS);
        lore.add(ChatColor.BLUE + "Mana cost: " + ChatColor.WHITE + "10 per block");
        lore.add(ChatColor.YELLOW + "Works with: " + ChatColor.WHITE + "Coal, Iron, Gold,");
        lore.add(ChatColor.WHITE + "Diamond, Redstone, Lapis, Emerald, Copper");
        lore.add("");
        lore.add(ChatColor.GRAY + "Mining Fortune will apply to all blocks mined.");
    }

    /**
     * Calculate the fortune multiplier to apply to vein miner blocks
     * THIS MUST MATCH EXACTLY the same calculation used in MiningListener
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
        int finalMultiplier = guaranteedMultiplier;
        
        // Check for chance-based extra drop
        if (random.nextDouble() * 100 < chanceForExtraDrop) {
            finalMultiplier++;
        }
        
        // Debug log the fortune calculation
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("Vein Miner calculated fortune multiplier: " + 
                finalMultiplier + " from mining fortune: " + miningFortune);
        }
        
        // Return the calculated multiplier
        return finalMultiplier;
    }

    /**
     * Recursively find connected ore blocks of the same type
     * Updated to include diagonal connections
     */
    private void findConnectedOres(Block block, Material type, Set<Block> minedBlocks, int remaining) {
        if (remaining <= 0 || minedBlocks.size() >= remaining) return;
        
        // Add the current block to the set
        minedBlocks.add(block);
        
        // Check all adjacent blocks including diagonals
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    // Skip the center block (the block itself)
                    if (x == 0 && y == 0 && z == 0) continue;
                    
                    Block adjacent = block.getRelative(x, y, z);
                    
                    // Skip blocks already in the set
                    if (minedBlocks.contains(adjacent)) continue;
                    
                    // Check if the adjacent block is of the same type
                    if (adjacent.getType() == type) {
                        // Recursively find more connected blocks
                        findConnectedOres(adjacent, type, minedBlocks, remaining);
                    }
                }
            }
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