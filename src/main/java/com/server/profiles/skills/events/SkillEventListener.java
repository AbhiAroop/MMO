package com.server.profiles.skills.events;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillProgressionManager;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.core.SkillType;
import com.server.profiles.skills.core.SubskillType;
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
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material material = block.getType();
        
        // Debug output to track block breaks
        if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
            plugin.debugLog(DebugSystem.SKILLS,"[SkillEventListener] Processing block break: " + material.name() + " by " + player.getName());
        }
        
        // Determine which skill should get XP
        SkillType skillType = null;
        double xpAmount = 0;
        
        if (isMiningBlock(material)) {
            skillType = SkillType.MINING;
            xpAmount = getMiningXp(material);
            
            // IMPORTANT: Only non-ore mining blocks should give regular Mining XP
            if (!isOreBlock(material)) {
                // Get main mining skill
                Skill miningSkill = SkillRegistry.getInstance().getSkill(skillType);
                if (miningSkill != null) {
                    // Award XP to main mining skill for non-ore blocks only
                    SkillProgressionManager.getInstance().addExperience(player, miningSkill, xpAmount);
                    
                    if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                        plugin.debugLog(DebugSystem.SKILLS,player.getName() + " gained " + xpAmount + 
                                            " XP in " + skillType.getDisplayName() + 
                                            " for breaking " + material.name());
                    }
                }
            }
            
            // Process subskills separately, even for ore blocks that don't give main skill XP
            Skill miningSkill = SkillRegistry.getInstance().getSkill(SkillType.MINING);
            if (miningSkill != null) {
                processSubskills(player, miningSkill, material, xpAmount);
            }
            
        } else if (isExcavatingBlock(material)) {
            skillType = SkillType.EXCAVATING;
            xpAmount = getExcavatingXp(material);
            
            // Award XP to main excavating skill for now (can be changed later)
            Skill excavatingSkill = SkillRegistry.getInstance().getSkill(skillType);
            if (excavatingSkill != null) {
                SkillProgressionManager.getInstance().addExperience(player, excavatingSkill, xpAmount);
                
                if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                    plugin.debugLog(DebugSystem.SKILLS,player.getName() + " gained " + xpAmount + 
                                        " XP in " + skillType.getDisplayName() + 
                                        " for breaking " + material.name());
                }
            }
        }
    }

    /**
     * Process block breaks directly when called from MiningListener
     * This bypasses event cancellation issues
     */
    public void processBlockBreakDirectly(Player player, Block block, Material originalMaterial) {
        // Debug output to track direct processing
        if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
            plugin.debugLog(DebugSystem.SKILLS,"[SkillEventListener] Direct processing block: " + originalMaterial.name() + " by " + player.getName());
        }
        
        // Determine which skill should get XP
        double xpAmount = 0;
        
        if (isMiningBlock(originalMaterial)) {
            xpAmount = getMiningXp(originalMaterial);
            
            // IMPORTANT: Only non-ore mining blocks should give regular Mining XP
            if (!isOreBlock(originalMaterial)) {
                // Get main mining skill
                Skill miningSkill = SkillRegistry.getInstance().getSkill(SkillType.MINING);
                if (miningSkill != null) {
                    // Award XP to main mining skill for non-ore blocks only
                    SkillProgressionManager.getInstance().addExperience(player, miningSkill, xpAmount);
                    
                    if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                        plugin.debugLog(DebugSystem.SKILLS,player.getName() + " gained " + xpAmount + 
                                            " XP in " + miningSkill.getDisplayName() + 
                                            " for breaking " + originalMaterial.name());
                    }
                }
            }
            
            // Process subskills separately, even for ore blocks that don't give main skill XP
            Skill miningSkill = SkillRegistry.getInstance().getSkill(SkillType.MINING);
            if (miningSkill != null) {
                processSubskills(player, miningSkill, originalMaterial, xpAmount);
            }
        } else if (isExcavatingBlock(originalMaterial)) {
            // Handle excavating blocks...
        }
    }

    /**
     * Process subskills for mining
     */
    private void processSubskills(Player player, Skill mainSkill, Material material, double baseXpAmount) {
        // Debug output
        if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
            plugin.debugLog(DebugSystem.SKILLS,"[SkillEventListener] Processing subskills for " + player.getName() + " - Material: " + material.name());
        }
        
        SkillRegistry registry = SkillRegistry.getInstance();
        
        // For Mining, we want to award XP to Ore Extraction or Gem Carving based on the block
        if (mainSkill.getId().equals(SkillType.MINING.getId())) {
            // Check for ore extraction - handles ore blocks
            if (isOreBlock(material)) {
                // Apply ore extraction specific XP logic
                Skill oreExtractionSkill = registry.getSubskill(SubskillType.ORE_EXTRACTION);
                if (oreExtractionSkill instanceof OreExtractionSubskill) {
                    OreExtractionSubskill oreSkill = (OreExtractionSubskill) oreExtractionSkill;
                    
                    // Check if player can mine this ore type
                    if (oreSkill.canMineOre(player, material)) {
                        // Calculate XP amount based on material
                        double xpAmount = calculateOreXp(material);
                        
                        // Apply any XP bonuses from skill tree
                        Map<String, Double> benefits = oreSkill.getSkillTreeBenefits(player);
                        double xpBoost = benefits.getOrDefault("xp_boost", 0.0); // This is a decimal multiplier (0.01 = 1%)
                        double miningXpSplit = benefits.getOrDefault("mining_xp_split", 0.0); // XP split percentage (0.0-0.5)

                        // Calculate the modified XP amount with the boost
                        double modifiedXpAmount = xpAmount * (1.0 + xpBoost);

                        // Calculate XP split if Ore Conduit is active
                        double subskillXpAmount = modifiedXpAmount;
                        double mainSkillXpAmount = 0.0;

                        if (miningXpSplit > 0.0) {
                            mainSkillXpAmount = modifiedXpAmount * miningXpSplit;
                            subskillXpAmount = modifiedXpAmount * (1.0 - miningXpSplit);
                            
                            // Create and fire the skill XP gain event for the main skill first
                            SkillExpGainEvent mainSkillEvent = new SkillExpGainEvent(player, mainSkill, mainSkillXpAmount);
                            plugin.getServer().getPluginManager().callEvent(mainSkillEvent);
                            
                            // Award XP to the main Mining skill only if the event wasn't cancelled
                            if (!mainSkillEvent.isCancelled()) {
                                SkillProgressionManager.getInstance().addExperience(player, mainSkill, mainSkillEvent.getAmount());
                            }
                            
                            if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                                plugin.debugLog(DebugSystem.SKILLS,player.getName() + " gained " + mainSkillXpAmount + 
                                    " Mining XP from Ore Conduit (" + (miningXpSplit * 100) + "% split)");
                            }
                        }
                        
                        // Create and fire the skill XP gain event for the ore extraction subskill
                        // Add metadata to the event to indicate it's part of a split
                        SkillExpGainEvent oreEvent = new SkillExpGainEvent(player, oreSkill, subskillXpAmount);
                        if (miningXpSplit > 0.0) {
                            oreEvent.setMetadata("split_percentage", miningXpSplit);
                            oreEvent.setMetadata("main_skill_amount", mainSkillXpAmount);
                        }
                        plugin.getServer().getPluginManager().callEvent(oreEvent);
                        
                        // Award XP to the ore extraction subskill only if the event wasn't cancelled
                        if (!oreEvent.isCancelled()) {
                            SkillProgressionManager.getInstance().addExperience(player, oreSkill, oreEvent.getAmount());
                        }
                        
                        if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                            plugin.debugLog(DebugSystem.SKILLS,player.getName() + " gained " + subskillXpAmount + 
                                    " OreExtraction XP for mining " + material.name() +
                                    (miningXpSplit > 0.0 ? " (with " + (miningXpSplit * 100) + "% split to Mining)" : ""));
                        }
                    } else {
                        if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                            plugin.debugLog(DebugSystem.SKILLS,player.getName() + " cannot mine " + material.name() + " yet. No XP awarded.");
                        }
                    }
                }
            }
            
            // Check for gem carving - handles blocks that might contain gems
        }
    }

    /**
     * Calculate XP for ore blocks
     */
    private double calculateOreXp(Material material) {
        // Base XP values
        double baseXp = 0;
        
        // First, determine the base ore type by removing DEEPSLATE_ prefix if present
        String materialName = material.name();
        boolean isDeepslate = materialName.contains("DEEPSLATE_");
        
        // Extract the core ore type by removing DEEPSLATE_ prefix
        String coreType = isDeepslate ? materialName.replace("DEEPSLATE_", "") : materialName;
        
        // Determine base XP for the core ore type
        if (coreType.contains("COAL_ORE")) {
            baseXp = 5.0;
        } else if (coreType.contains("COPPER_ORE")) {
            baseXp = 7.0;
        } else if (coreType.contains("IRON_ORE")) {
            baseXp = 10.0;
        } else if (coreType.contains("GOLD_ORE") || coreType.equals("NETHER_GOLD_ORE")) {
            baseXp = 15.0;
        } else if (coreType.contains("REDSTONE_ORE")) {
            baseXp = 8.0;
        } else if (coreType.contains("LAPIS_ORE")) {
            baseXp = 12.0;
        } else if (coreType.contains("DIAMOND_ORE")) {
            baseXp = 20.0;
        } else if (coreType.contains("EMERALD_ORE")) {
            baseXp = 22.0;
        } else if (coreType.equals("NETHER_QUARTZ_ORE")) {
            baseXp = 8.0;
        } else if (coreType.equals("ANCIENT_DEBRIS")) {
            baseXp = 35.0;
        }
        
        // Apply 2x multiplier for deepslate variants
        if (isDeepslate) {
            baseXp *= 2.0;
        }
        
        return baseXp;
    }

    /**
     * Check if a material is an ore block
     */
    private boolean isOreBlock(Material material) {
        return material == Material.COAL_ORE || material == Material.DEEPSLATE_COAL_ORE ||
            material == Material.IRON_ORE || material == Material.DEEPSLATE_IRON_ORE ||
            material == Material.COPPER_ORE || material == Material.DEEPSLATE_COPPER_ORE ||
            material == Material.GOLD_ORE || material == Material.DEEPSLATE_GOLD_ORE ||
            material == Material.REDSTONE_ORE || material == Material.DEEPSLATE_REDSTONE_ORE ||
            material == Material.LAPIS_ORE || material == Material.DEEPSLATE_LAPIS_ORE ||
            material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE ||
            material == Material.EMERALD_ORE || material == Material.DEEPSLATE_EMERALD_ORE ||
            material == Material.NETHER_GOLD_ORE || material == Material.NETHER_QUARTZ_ORE ||
            material == Material.ANCIENT_DEBRIS;
    }

    /**
     * Check if a material potentially contains gems
     */
    private boolean containsGems(Material material) {
        return material == Material.STONE || 
            material == Material.GRANITE || 
            material == Material.DIORITE || 
            material == Material.ANDESITE ||
            material == Material.DEEPSLATE || 
            material == Material.TUFF || 
            material == Material.BASALT || 
            material == Material.BLACKSTONE ||
            material == Material.AMETHYST_CLUSTER || 
            material == Material.EMERALD_ORE || 
            material == Material.DEEPSLATE_EMERALD_ORE ||
            material == Material.DIAMOND_ORE || 
            material == Material.DEEPSLATE_DIAMOND_ORE;
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
            if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                plugin.debugLog(DebugSystem.SKILLS,player.getName() + " would have found a gem! (Level " + level + 
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
    
    /**
     * Process OreExtraction XP directly (called from MiningListener)
     */
    public void processOreExtractionXP(Player player, Material material) {
        // Get mining skill
        Skill miningSkill = SkillRegistry.getInstance().getSkill(SkillType.MINING);
        if (miningSkill == null) return;
        
        // Find OreExtraction subskill
        for (Skill subskill : miningSkill.getSubskills()) {
            if (subskill instanceof OreExtractionSubskill) {
                OreExtractionSubskill oreSkill = (OreExtractionSubskill) subskill;
                
                // Check if this material is affected by the subskill
                if (oreSkill.affectsMaterial(material)) {
                    // Calculate XP amount based on material
                    double xpAmount = 0;
                    
                    // Scale XP based on ore value
                    if (material.name().contains("DIAMOND")) {
                        xpAmount = 20.0; // Fixed amount for diamond ore
                    } else if (material.name().contains("EMERALD")) {
                        xpAmount = 22.0; // Fixed amount for emerald ore
                    } else if (material.name().contains("GOLD")) {
                        xpAmount = 15.0; // Fixed amount for gold ore
                    } else if (material.name().contains("IRON")) {
                        xpAmount = 10.0; // Fixed amount for iron ore
                    } else if (material.name().contains("REDSTONE")) {
                        xpAmount = 8.0; // Fixed amount for redstone ore
                    } else if (material.name().contains("LAPIS")) {
                        xpAmount = 12.0; // Fixed amount for lapis ore
                    } else if (material.name().contains("COAL")) {
                        xpAmount = 5.0; // Fixed amount for coal ore
                    } else if (material.name().contains("COPPER")) {
                        xpAmount = 7.0; // Fixed amount for copper ore
                    } else if (material.name().contains("ANCIENT_DEBRIS")) {
                        xpAmount = 35.0; // Fixed amount for ancient debris
                    } else if (material.name().contains("NETHER_QUARTZ")) {
                        xpAmount = 8.0; // Fixed amount for nether quartz
                    } else {
                        xpAmount = getMiningXp(material) * 0.75; // Default for other materials
                    }
                    
                    // Award the subskill XP
                    SkillProgressionManager.getInstance().addExperience(player, subskill, xpAmount);
                    
                    if (plugin.isDebugEnabled(DebugSystem.SKILLS)) {
                        plugin.debugLog(DebugSystem.SKILLS,player.getName() + " gained " + xpAmount + 
                                            " XP in " + subskill.getDisplayName() + 
                                            " for breaking " + material.name());
                    }
                    break; // Only award XP once
                }
            }
        }
    }
}