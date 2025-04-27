package com.server.profiles.skills.events;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import com.server.Main;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillProgressionManager;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.core.SkillType;
import com.server.profiles.skills.skills.mining.MiningSkill;
import com.server.profiles.skills.skills.mining.subskills.GemCarvingSubskill;
import com.server.profiles.skills.skills.mining.subskills.OreExtractionSubskill;

/**
 * Listens for Bukkit events that should award skill XP
 */
public class SkillEventListener implements Listener {
    private final Main plugin;
    
    public SkillEventListener(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle block breaking for Mining and Excavating skills
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material material = block.getType();
        
        // Determine which skill should get XP
        SkillType skillType = null;
        double xpAmount = 0;
        
        if (isMiningBlock(material)) {
            skillType = SkillType.MINING;
            xpAmount = getMiningXp(material);
            
            // Get main mining skill
            Skill miningSkill = SkillRegistry.getInstance().getSkill(skillType);
            if (miningSkill != null) {
                // CHANGE: Don't award XP to main skill directly anymore
                // SkillProgressionManager.getInstance().addExperience(player, miningSkill, xpAmount);
                
                // Process subskills - they will be the ones getting XP directly now
                processSubskills(player, miningSkill, material, xpAmount);
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info(player.getName() + " mining " + material.name() + 
                                        " - XP directed to subskills only");
                }
            }
        } else if (isExcavatingBlock(material)) {
            skillType = SkillType.EXCAVATING;
            xpAmount = getExcavatingXp(material);
            
            // Award XP to main excavating skill for now (can be changed later)
            Skill excavatingSkill = SkillRegistry.getInstance().getSkill(skillType);
            if (excavatingSkill != null) {
                SkillProgressionManager.getInstance().addExperience(player, excavatingSkill, xpAmount);
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info(player.getName() + " gained " + xpAmount + 
                                        " XP in " + skillType.getDisplayName() + 
                                        " for breaking " + material.name());
                }
            }
        }
    }

    /**
     * Process subskills for mining
     */
    private void processSubskills(Player player, Skill mainSkill, Material material, double baseXpAmount) {
        if (!(mainSkill instanceof MiningSkill)) return;
        
        // Only process OreExtractionSubskill - GemCarvingSubskill will be implemented separately later
        for (Skill subskill : mainSkill.getSubskills()) {
            if (subskill instanceof OreExtractionSubskill) {
                OreExtractionSubskill oreSkill = (OreExtractionSubskill) subskill;
                
                // Check if this material is affected by the subskill
                if (oreSkill.affectsMaterial(material)) {
                    // Use a more balanced XP scaling for ores
                    double subskillXp = 0;
                    
                    // Scale XP based on ore value
                    if (material.name().contains("DIAMOND")) {
                        subskillXp = 20.0; // Fixed amount for diamond ore
                    } else if (material.name().contains("EMERALD")) {
                        subskillXp = 22.0; // Fixed amount for emerald ore
                    } else if (material.name().contains("GOLD")) {
                        subskillXp = 15.0; // Fixed amount for gold ore
                    } else if (material.name().contains("IRON")) {
                        subskillXp = 10.0; // Fixed amount for iron ore
                    } else if (material.name().contains("REDSTONE")) {
                        subskillXp = 8.0; // Fixed amount for redstone ore
                    } else if (material.name().contains("LAPIS")) {
                        subskillXp = 12.0; // Fixed amount for lapis ore
                    } else if (material.name().contains("COAL")) {
                        subskillXp = 5.0; // Fixed amount for coal ore
                    } else if (material.name().contains("COPPER")) {
                        subskillXp = 7.0; // Fixed amount for copper ore
                    } else if (material.name().contains("ANCIENT_DEBRIS")) {
                        subskillXp = 35.0; // Fixed amount for ancient debris
                    } else if (material.name().contains("NETHER_QUARTZ")) {
                        subskillXp = 8.0; // Fixed amount for nether quartz
                    } else {
                        subskillXp = baseXpAmount * 0.75; // Default for other materials
                    }
                    
                    // Award the subskill XP
                    SkillProgressionManager.getInstance().addExperience(player, subskill, subskillXp);
                    
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info(player.getName() + " gained " + subskillXp + 
                                            " XP in " + subskill.getDisplayName() + 
                                            " for breaking " + material.name());
                    }
                }
            }
            // GemCarvingSubskill is NOT processed here - it will be implemented separately later
        }
    }
    
    /**
     * Try to find a gem when breaking a block
     */
    private void tryFindGem(Player player, GemCarvingSubskill gemSkill, Material material) {
        // Get player's current level in the skill
        int level = gemSkill.getSkillLevel(player).getLevel();
        
        // Check if we should find a gem based on chance
        double findChance = gemSkill.getGemFindChance(level);
        if (Math.random() < findChance) {
            // TODO: Implement actual gem-finding mechanic, maybe open a mini-game GUI
            // For now, just inform in debug mode
            if (plugin.isDebugMode()) {
                plugin.getLogger().info(player.getName() + " would have found a gem! (Level " + level + 
                                     ", Find Chance: " + String.format("%.1f%%", findChance * 100) + ")");
            }
        }
    }
    
    // Other methods remain the same
    
    /**
     * Check if a material should grant Mining XP
     */
    private boolean isMiningBlock(Material material) {
        return material.name().contains("ORE") ||
               material.name().contains("STONE") ||
               material.name().contains("DEEPSLATE") ||
               material.name().contains("GRANITE") ||
               material.name().contains("DIORITE") ||
               material.name().contains("ANDESITE") ||
               material.name().contains("TUFF") ||
               material.name().contains("BLACKSTONE") ||
               material.name().contains("BASALT") ||
               material.name().contains("OBSIDIAN") ||
               material.name().contains("NETHERRACK") ||
               material.name().contains("END_STONE") ||
               material.name().contains("AMETHYST") ||
               material.name().contains("COPPER") ||
               material.name().contains("ANCIENT_DEBRIS");
    }
    
    /**
     * Check if a material should grant Excavating XP
     */
    private boolean isExcavatingBlock(Material material) {
        return material == Material.DIRT ||
               material == Material.COARSE_DIRT || 
               material == Material.GRASS_BLOCK ||
               material == Material.PODZOL ||
               material == Material.MYCELIUM ||
               material == Material.SOUL_SAND ||
               material == Material.SOUL_SOIL ||
               material == Material.SAND ||
               material == Material.RED_SAND ||
               material == Material.GRAVEL ||
               material == Material.CLAY;
    }
    
    /**
     * Get the amount of Mining XP for a material
     */
    private double getMiningXp(Material material) {
        if (material.name().contains("DIAMOND")) return 30.0;
        if (material.name().contains("EMERALD")) return 35.0;
        if (material.name().contains("GOLD")) return 25.0;
        if (material.name().contains("IRON")) return 15.0;
        if (material.name().contains("REDSTONE")) return 15.0;
        if (material.name().contains("LAPIS")) return 20.0;
        if (material.name().contains("COAL")) return 10.0;
        if (material.name().contains("COPPER")) return 12.0;
        if (material.name().contains("ANCIENT_DEBRIS")) return 50.0;
        if (material.name().contains("NETHER_QUARTZ")) return 15.0;
        if (material.name().contains("AMETHYST")) return 25.0;
        if (material.name().contains("OBSIDIAN")) return 35.0;
        
        // Base stone blocks give less XP
        if (material.name().contains("STONE") || 
            material.name().contains("DEEPSLATE") ||
            material.name().contains("GRANITE") ||
            material.name().contains("DIORITE") ||
            material.name().contains("ANDESITE") ||
            material.name().contains("TUFF") ||
            material.name().contains("BLACKSTONE") ||
            material.name().contains("BASALT") ||
            material.name().contains("NETHERRACK") ||
            material.name().contains("END_STONE")) {
            return 5.0;
        }
        
        return 0.0;
    }
    
    /**
     * Get the amount of Excavating XP for a material
     */
    private double getExcavatingXp(Material material) {
        if (material == Material.CLAY) return 15.0;
        if (material == Material.SOUL_SAND || material == Material.SOUL_SOIL) return 10.0;
        if (material == Material.GRAVEL) return 8.0;
        if (material == Material.SAND || material == Material.RED_SAND) return 5.0;
        if (material == Material.DIRT || 
            material == Material.COARSE_DIRT || 
            material == Material.GRASS_BLOCK ||
            material == Material.PODZOL ||
            material == Material.MYCELIUM) {
            return 3.0;
        }
        
        return 0.0;
    }
    
    /**
     * Get the amount of Farming XP for a crop
     */
    private double getFarmingXp(Material material) {
        if (material == Material.WHEAT) return 10.0;
        if (material == Material.POTATOES) return 12.0;
        if (material == Material.CARROTS) return 12.0;
        if (material == Material.BEETROOTS) return 15.0;
        if (material == Material.NETHER_WART) return 20.0;
        if (material == Material.PUMPKIN) return 20.0;
        if (material == Material.MELON) return 15.0;
        if (material == Material.SWEET_BERRY_BUSH) return 8.0;
        
        return 0.0;
    }
    
    /**
     * Get the amount of Combat XP for killing an entity
     */
    private double getCombatXp(Entity entity) {
        switch (entity.getType()) {
            case ENDER_DRAGON: return 500.0;
            case WITHER: return 300.0;
            case ELDER_GUARDIAN: return 150.0;
            case WARDEN: return 250.0;
            case RAVAGER: return 100.0;
            
            case CREEPER: return 20.0;
            case ZOMBIE: return 15.0;
            case SKELETON: return 15.0;
            case SPIDER: return 15.0;
            case CAVE_SPIDER: return 20.0;
            case WITCH: return 25.0;
            case ENDERMAN: return 25.0;
            case BLAZE: return 25.0;
            case GHAST: return 30.0;
            case GUARDIAN: return 35.0;
            case SHULKER: return 30.0;
            case SLIME: return 10.0;
            case MAGMA_CUBE: return 12.0;
            case PHANTOM: return 20.0;
            
            case ZOMBIE_VILLAGER: return 18.0;
            case DROWNED: return 18.0;
            case HUSK: return 18.0;
            case STRAY: return 18.0;
            
            case PIG:
            case COW:
            case SHEEP:
            case CHICKEN:
            case RABBIT:
                return 5.0;
                
            case WOLF:
            case FOX:
            case POLAR_BEAR:
                return 10.0;
                
            default:
                return 8.0;
        }
    }
}