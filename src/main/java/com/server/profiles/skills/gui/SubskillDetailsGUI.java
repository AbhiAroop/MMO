package com.server.profiles.skills.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.data.SkillLevel;
import com.server.profiles.skills.skills.mining.subskills.GemCarvingSubskill;
import com.server.profiles.skills.skills.mining.subskills.OreExtractionSubskill;
import com.server.profiles.skills.trees.PlayerSkillTreeData;

/**
 * GUI for displaying specialized information about specific subskills
 */
public class SubskillDetailsGUI {
    
    private static final String GUI_TITLE_PREFIX = ChatColor.GOLD + "✦ " + ChatColor.AQUA;
    private static final String GUI_TITLE_SUFFIX = ChatColor.GOLD + " ✦";
    
    // Maps to store material icons and XP values for different ore types
    private static final Map<String, Material> ORE_MATERIALS = new HashMap<>();
    private static final Map<String, Double> ORE_XP_VALUES = new HashMap<>();
    
    // Maps to store material icons and details for different gem types
    private static final Map<String, Material> GEM_MATERIALS = new HashMap<>();
    private static final Map<String, Integer> GEM_QUALITIES = new HashMap<>();
    private static final Map<String, Double> GEM_XP_VALUES = new HashMap<>();
    
    static {
        // Initialize ore materials and XP values
        ORE_MATERIALS.put("coal", Material.COAL_ORE);
        ORE_MATERIALS.put("deepslate_coal", Material.DEEPSLATE_COAL_ORE);
        ORE_MATERIALS.put("iron", Material.IRON_ORE);
        ORE_MATERIALS.put("deepslate_iron", Material.DEEPSLATE_IRON_ORE);
        ORE_MATERIALS.put("copper", Material.COPPER_ORE);
        ORE_MATERIALS.put("deepslate_copper", Material.DEEPSLATE_COPPER_ORE);
        ORE_MATERIALS.put("gold", Material.GOLD_ORE);
        ORE_MATERIALS.put("deepslate_gold", Material.DEEPSLATE_GOLD_ORE);
        ORE_MATERIALS.put("redstone", Material.REDSTONE_ORE);
        ORE_MATERIALS.put("deepslate_redstone", Material.DEEPSLATE_REDSTONE_ORE);
        ORE_MATERIALS.put("lapis", Material.LAPIS_ORE);
        ORE_MATERIALS.put("deepslate_lapis", Material.DEEPSLATE_LAPIS_ORE);
        ORE_MATERIALS.put("diamond", Material.DIAMOND_ORE);
        ORE_MATERIALS.put("deepslate_diamond", Material.DEEPSLATE_DIAMOND_ORE);
        ORE_MATERIALS.put("emerald", Material.EMERALD_ORE);
        ORE_MATERIALS.put("deepslate_emerald", Material.DEEPSLATE_EMERALD_ORE);
        ORE_MATERIALS.put("nether_quartz", Material.NETHER_QUARTZ_ORE);
        ORE_MATERIALS.put("nether_gold", Material.NETHER_GOLD_ORE);
        ORE_MATERIALS.put("ancient_debris", Material.ANCIENT_DEBRIS);
        
        ORE_XP_VALUES.put("coal", 2.0);
        ORE_XP_VALUES.put("deepslate_coal", 3.0);
        ORE_XP_VALUES.put("iron", 3.5);
        ORE_XP_VALUES.put("deepslate_iron", 5.0);
        ORE_XP_VALUES.put("copper", 2.5);
        ORE_XP_VALUES.put("deepslate_copper", 3.5);
        ORE_XP_VALUES.put("gold", 5.0);
        ORE_XP_VALUES.put("deepslate_gold", 7.0);
        ORE_XP_VALUES.put("redstone", 4.0);
        ORE_XP_VALUES.put("deepslate_redstone", 5.5);
        ORE_XP_VALUES.put("lapis", 4.5);
        ORE_XP_VALUES.put("deepslate_lapis", 6.0);
        ORE_XP_VALUES.put("diamond", 8.0);
        ORE_XP_VALUES.put("deepslate_diamond", 10.0);
        ORE_XP_VALUES.put("emerald", 10.0);
        ORE_XP_VALUES.put("deepslate_emerald", 12.5);
        ORE_XP_VALUES.put("nether_quartz", 3.0);
        ORE_XP_VALUES.put("nether_gold", 4.0);
        ORE_XP_VALUES.put("ancient_debris", 15.0);
        
        // Initialize gem materials and properties based on the GemCarvingMinigame
        GEM_MATERIALS.put("mooncrystal", Material.QUARTZ);
        GEM_MATERIALS.put("azuralite", Material.LAPIS_LAZULI);
        GEM_MATERIALS.put("pyrethine", Material.COPPER_INGOT);
        GEM_MATERIALS.put("solvanecystal", Material.GOLD_INGOT);
        GEM_MATERIALS.put("nyxstone", Material.AMETHYST_SHARD);
        GEM_MATERIALS.put("lucenthar", Material.EMERALD);
        GEM_MATERIALS.put("veyrithcrystal", Material.DIAMOND);
        GEM_MATERIALS.put("drakthyst", Material.NETHERITE_INGOT);
        
        // Crystal tier qualities (1-8)
        GEM_QUALITIES.put("mooncrystal", 1);
        GEM_QUALITIES.put("azuralite", 2);
        GEM_QUALITIES.put("pyrethine", 3);
        GEM_QUALITIES.put("solvanecystal", 4);
        GEM_QUALITIES.put("nyxstone", 5);
        GEM_QUALITIES.put("lucenthar", 6);
        GEM_QUALITIES.put("veyrithcrystal", 7);
        GEM_QUALITIES.put("drakthyst", 8);
        
        // XP values based on minigame base XP values
        GEM_XP_VALUES.put("mooncrystal", 100.0);
        GEM_XP_VALUES.put("azuralite", 125.0);
        GEM_XP_VALUES.put("pyrethine", 150.0);
        GEM_XP_VALUES.put("solvanecystal", 175.0);
        GEM_XP_VALUES.put("nyxstone", 200.0);
        GEM_XP_VALUES.put("lucenthar", 250.0);
        GEM_XP_VALUES.put("veyrithcrystal", 350.0);
        GEM_XP_VALUES.put("drakthyst", 500.0);
    }

    /**
     * Open the subskill details GUI for a player
     */
    public static void openSubskillDetailsGUI(Player player, Skill subskill) {
        // Create inventory with a size of 54 slots (6 rows) for better layout
        String title = GUI_TITLE_PREFIX + subskill.getDisplayName() + " Details" + GUI_TITLE_SUFFIX;
        Inventory gui = Bukkit.createInventory(null, 54, title);
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) {
            player.sendMessage(ChatColor.RED + "You need to select a profile first!");
            return;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Get skill level
        SkillLevel level = profile.getSkillData().getSkillLevel(subskill);
        
        // Create border
        createBorder(gui);
        
        // ===== HEADER =====
        // Add subskill info at the top center only
        ItemStack infoItem = createSubskillInfoItem(subskill, level);
        gui.setItem(4, infoItem);
        
        // ===== MAIN CONTENT =====
        // Add specific content based on subskill type
        if (subskill instanceof OreExtractionSubskill) {
            populateOreExtractionDetails(gui, (OreExtractionSubskill)subskill, level, player, profile);
        } 
        else if (subskill instanceof GemCarvingSubskill) {
            populateGemCarvingDetails(gui, (GemCarvingSubskill)subskill, level, player, profile);
        }
        else {
            // Generic subskill (for future expansion)
            populateGenericSubskillDetails(gui, subskill, level);
        }
        
        // ===== FOOTER =====
        // Add back button in bottom left
        ItemStack backButton = createBackButton(subskill.getParentSkill());
        gui.setItem(45, backButton);
        
        // Add help button in bottom right
        ItemStack helpButton = createHelpButton(subskill);
        gui.setItem(53, helpButton);
        
        // Fill empty slots
        fillEmptySlots(gui);
        
        // Open inventory
        player.openInventory(gui);
    }


    /**
     * Create a summary stats item for OreExtraction
     */
    private static ItemStack createStatSummary(OreExtractionSubskill subskill, SkillLevel level, Player player) {
        // Get mining speed multiplier from skill tree if available
        double speedMultiplier = subskill.getMiningSpeedMultiplier(level.getLevel());
        double fortuneBonus = subskill.getMiningFortuneBonus(level.getLevel());
        double miningFortuneFromTree = 0;
        double speedBoostFromTree = 0;
        
        if (player != null) {
            // Get skill tree benefits if available
            Map<String, Double> benefits = subskill.getSkillTreeBenefits(player);
            miningFortuneFromTree = benefits.getOrDefault("mining_fortune", 0.0);
            speedBoostFromTree = benefits.getOrDefault("mining_speed", 0.0);
        }
        
        double totalFortune = fortuneBonus + miningFortuneFromTree;
        double totalSpeedMultiplier = speedMultiplier + speedBoostFromTree;
        
        return createStatDisplay(
            Material.GOLDEN_PICKAXE,
            "Mining Efficiency",
            new String[] {
                "Your current mining efficiency stats",
                "",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Speed Multiplier: " + 
                ChatColor.GREEN + String.format("%.2fx", totalSpeedMultiplier),
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Mining Fortune: " + 
                ChatColor.GREEN + "+" + String.format("%.1f", totalFortune),
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Bonus Drop Chance: " + 
                ChatColor.GREEN + String.format("%.1f%%", subskill.getBonusDropChance(level.getLevel()) * 100),
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Cave-in Risk: " + 
                (subskill.getCaveInChance(level.getLevel()) <= 0.05 ? ChatColor.GREEN : ChatColor.RED) +
                String.format("%.1f%%", subskill.getCaveInChance(level.getLevel()) * 100),
                "",
                ChatColor.GRAY + "(Includes both level-based stats and skill tree bonuses)"
            });
    }

    /**
     * Create a summary stats item for GemCarving
     */
    private static ItemStack createStatSummary(GemCarvingSubskill subskill, SkillLevel level, Player player) {
        // Get basic stats
        double gemFindChance = subskill.getGemFindChance(level.getLevel()) * 100;
        double extractionSuccess = subskill.getExtractionSuccessChance(level.getLevel()) * 100;
        double qualityMultiplier = subskill.getGemQualityMultiplier(level.getLevel());
        int bonusXp = 0;
        
        // Check skill tree for XP boost
        if (player != null) {
            PlayerProfile profile = null;
            Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
            if (activeSlot != null) {
                profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
                if (profile != null) {
                    PlayerSkillTreeData skillTreeData = profile.getSkillTreeData();
                    Map<String, Integer> nodeLevels = skillTreeData.getNodeLevels(subskill.getId());
                    
                    // Check for XP boost
                    if (nodeLevels.containsKey("gemcarving_xp_boost")) {
                        bonusXp = nodeLevels.get("gemcarving_xp_boost");
                    }
                }
            }
        }
        
        return createStatDisplay(
            Material.IRON_SWORD,
            "Carving Precision",
            new String[] {
                "Your current gem carving precision stats",
                "",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Gem Find Rate: " + 
                ChatColor.GREEN + String.format("+%.1f%%", gemFindChance),
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Extraction Success: " + 
                ChatColor.GREEN + String.format("%.1f%%", extractionSuccess),
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Gem Quality Bonus: " + 
                ChatColor.GREEN + "+" + String.format("%.2fx", qualityMultiplier),
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Bonus XP Per Carve: " + 
                (bonusXp > 0 ? ChatColor.GREEN + "+" + bonusXp : ChatColor.GRAY + "+0"),
                "",
                ChatColor.GRAY + "(Higher level improves extraction chance and quality)"
            });
    }

    /**
     * Create a generic stat summary for other subskills
     */
    private static ItemStack createGenericStatSummary(Skill subskill, SkillLevel level) {
        return createStatDisplay(
            Material.BOOK,
            "Skill Progress",
            new String[] {
                "Your current progress in this subskill",
                "",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Current Level: " + 
                ChatColor.GREEN + level.getLevel() + "/" + subskill.getMaxLevel(),
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Total XP Earned: " + 
                ChatColor.GREEN + String.format("%,.1f", level.getTotalXp()),
                "",
                level.getLevel() < subskill.getMaxLevel() ? 
                    (ChatColor.GRAY + "Keep training to unlock more benefits!") : 
                    (ChatColor.GOLD + "Maximum level reached!")
            });
    }

    /**
     * Create a skill tree button
     */
    private static ItemStack createSkillTreeButton(Skill subskill, PlayerProfile profile) {
        // Get skill tree data
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        int tokenCount = treeData.getTokenCount(subskill.getId());
        
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "✦ " + subskill.getDisplayName() + " Skill Tree ✦");
        
        // Add enchant glow if tokens available
        if (tokenCount > 0) {
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + "Access the skill tree to unlock");
        lore.add(ChatColor.GRAY + "new abilities and improvements.");
        lore.add("");
        
        // Add token information
        if (tokenCount > 0) {
            lore.add(ChatColor.GOLD + "✦ " + ChatColor.GREEN + tokenCount + ChatColor.YELLOW + " Skill Points Available! ✦");
        } else {
            lore.add(ChatColor.GRAY + "No skill points currently available.");
        }
        
        lore.add("");
        lore.add(ChatColor.GREEN + "Click to open skill tree");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create a decorative item for visual appeal
     */
    private static ItemStack createDecorativeItem(Material material, String name, String[] lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (name != null) {
            meta.setDisplayName(name);
        } else {
            meta.setDisplayName(" ");
        }
        
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        if (lore != null && lore.length > 0) {
            List<String> lorelist = new ArrayList<>();
            for (String line : lore) {
                lorelist.add(ChatColor.GRAY + line);
            }
            meta.setLore(lorelist);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Populate OreExtraction-specific details - simplified for cleaner layout
     */
    private static void populateOreExtractionDetails(Inventory gui, OreExtractionSubskill subskill, 
                                            SkillLevel level, Player player, PlayerProfile profile) {
        // Calculate unlocked ore types from skill tree
        boolean hasBasicOres = true; // Always unlocked
        boolean hasCopperOres = true; // Always unlocked
        boolean hasIronOres = level.getLevel() >= 5; // Level requirement
        boolean hasGoldOres = false;
        boolean hasRedstoneOres = false;
        boolean hasLapisOres = false;
        boolean hasDiamondOres = false;
        boolean hasEmeraldOres = false;
        boolean hasNetherOres = false;
        boolean hasAncientDebris = false;
        
        if (profile != null) {
            // Check skill tree nodes
            PlayerSkillTreeData skillTreeData = profile.getSkillTreeData();
            Map<String, Integer> nodeLevels = skillTreeData.getNodeLevels(subskill.getId());
            
            // Check if specific nodes are unlocked
            hasGoldOres = nodeLevels.containsKey("gold_mining") && nodeLevels.get("gold_mining") > 0;
            hasRedstoneOres = nodeLevels.containsKey("redstone_mining") && nodeLevels.get("redstone_mining") > 0;
            hasLapisOres = nodeLevels.containsKey("lapis_mining") && nodeLevels.get("lapis_mining") > 0;
            hasDiamondOres = nodeLevels.containsKey("diamond_mining") && nodeLevels.get("diamond_mining") > 0;
            hasEmeraldOres = nodeLevels.containsKey("emerald_mining") && nodeLevels.get("emerald_mining") > 0;
            hasNetherOres = nodeLevels.containsKey("nether_mining") && nodeLevels.get("nether_mining") > 0;
            hasAncientDebris = nodeLevels.containsKey("ancient_debris_mining") && nodeLevels.get("ancient_debris_mining") > 0;
        } else {
            // Fallback to level-based unlocking if profile unavailable
            hasGoldOres = level.getLevel() >= 10;
            hasRedstoneOres = level.getLevel() >= 15;
            hasLapisOres = level.getLevel() >= 20;
            hasDiamondOres = level.getLevel() >= 25;
            hasEmeraldOres = level.getLevel() >= 30;
            hasNetherOres = level.getLevel() >= 20;
            hasAncientDebris = level.getLevel() >= 40;
        }
        
        // Get total mining fortune for calculations
        double fortuneBonus = subskill.getMiningFortuneBonus(level.getLevel());
        double miningFortuneFromTree = 0;
        
        if (player != null) {
            Map<String, Double> benefits = subskill.getSkillTreeBenefits(player);
            miningFortuneFromTree = benefits.getOrDefault("mining_fortune", 0.0);
        }
        
        double totalFortune = fortuneBonus + miningFortuneFromTree;
        
        // Organize ores in a grid layout for better visualization
        
        // Regular Ores - Left Side
        // Row 1: Common ores
        gui.setItem(10, createOreInfoItem("coal", true, 1, totalFortune));
        gui.setItem(11, createOreInfoItem("copper", true, 1, totalFortune));
        gui.setItem(12, createOreInfoItem("iron", hasIronOres, 5, totalFortune));
        
        // Row 2: Uncommon ores
        gui.setItem(19, createOreInfoItem("gold", hasGoldOres, 10, totalFortune));
        gui.setItem(20, createOreInfoItem("redstone", hasRedstoneOres, 15, totalFortune));
        gui.setItem(21, createOreInfoItem("lapis", hasLapisOres, 20, totalFortune));
        
        // Row 3: Rare ores
        gui.setItem(28, createOreInfoItem("diamond", hasDiamondOres, 25, totalFortune));
        gui.setItem(29, createOreInfoItem("emerald", hasEmeraldOres, 30, totalFortune));
        
        // Deepslate Variants - Right Side
        // Row 1: Common deepslate
        gui.setItem(14, createOreInfoItem("deepslate_coal", true, 1, totalFortune));
        gui.setItem(15, createOreInfoItem("deepslate_copper", true, 1, totalFortune));
        gui.setItem(16, createOreInfoItem("deepslate_iron", hasIronOres, 5, totalFortune));
        
        // Row 2: Uncommon deepslate
        gui.setItem(23, createOreInfoItem("deepslate_gold", hasGoldOres, 10, totalFortune));
        gui.setItem(24, createOreInfoItem("deepslate_redstone", hasRedstoneOres, 15, totalFortune));
        gui.setItem(25, createOreInfoItem("deepslate_lapis", hasLapisOres, 20, totalFortune));
        
        // Row 3: Rare deepslate
        gui.setItem(32, createOreInfoItem("deepslate_diamond", hasDiamondOres, 25, totalFortune));
        gui.setItem(33, createOreInfoItem("deepslate_emerald", hasEmeraldOres, 30, totalFortune));
        
        // Nether Ores - Bottom Row
        gui.setItem(37, createOreInfoItem("nether_quartz", hasNetherOres, 15, totalFortune));
        gui.setItem(38, createOreInfoItem("nether_gold", hasNetherOres, 20, totalFortune));
        gui.setItem(40, createOreInfoItem("ancient_debris", hasAncientDebris, 40, totalFortune));
    }

    /**
     * Populate GemCarving-specific details - simplified for cleaner layout
     */
    private static void populateGemCarvingDetails(Inventory gui, GemCarvingSubskill subskill, 
                                            SkillLevel level, Player player, PlayerProfile profile) {
        // Get skill tree benefits
        boolean hasBasicCrystals = true; // Always unlocked (mooncrystal, azuralite)
        boolean hasIntermediateCrystals = level.getLevel() >= 15; // pyrethine, solvanecystal
        boolean hasAdvancedCrystals = false; // nyxstone, lucenthar
        boolean hasMasterCrystals = false; // veyrithcrystal, drakthyst
        
        // Check skill tree for unlocks
        if (profile != null) {
            PlayerSkillTreeData skillTreeData = profile.getSkillTreeData();
            Map<String, Integer> nodeLevels = skillTreeData.getNodeLevels(subskill.getId());
            
            // Check unlocked crystal tiers
            hasAdvancedCrystals = nodeLevels.containsKey("advanced_crystals") && nodeLevels.get("advanced_crystals") > 0;
            hasMasterCrystals = nodeLevels.containsKey("master_crystals") && nodeLevels.get("master_crystals") > 0;
        } else {
            // Fallback to level-based unlocking
            hasAdvancedCrystals = level.getLevel() >= 30;
            hasMasterCrystals = level.getLevel() >= 50;
        }
        
        // Get basic stats for calculations
        double extractionSuccess = subskill.getExtractionSuccessChance(level.getLevel()) * 100;
        double qualityMultiplier = subskill.getGemQualityMultiplier(level.getLevel());
        
        // Organize crystals in a grid layout by tier
        
        // Tier 1-2: Basic (Common) Crystals - Top Row
        gui.setItem(11, createGemInfoItem("mooncrystal", true, 1, extractionSuccess, qualityMultiplier, CrystalTier.COMMON));
        gui.setItem(15, createGemInfoItem("azuralite", true, 5, extractionSuccess, qualityMultiplier, CrystalTier.COMMON));
        
        // Tier 3-4: Intermediate (Uncommon) Crystals - Second Row
        gui.setItem(20, createGemInfoItem("pyrethine", hasIntermediateCrystals, 15, extractionSuccess, qualityMultiplier, CrystalTier.UNCOMMON));
        gui.setItem(24, createGemInfoItem("solvanecystal", hasIntermediateCrystals, 20, extractionSuccess, qualityMultiplier, CrystalTier.UNCOMMON));
        
        // Tier 5-6: Advanced (Rare/Epic) Crystals - Third Row
        gui.setItem(29, createGemInfoItem("nyxstone", hasAdvancedCrystals, 30, extractionSuccess - 0.1, qualityMultiplier, CrystalTier.RARE));
        gui.setItem(33, createGemInfoItem("lucenthar", hasAdvancedCrystals, 40, extractionSuccess - 0.15, qualityMultiplier, CrystalTier.EPIC));
        
        // Tier 7-8: Master (Legendary) Crystals - Bottom Row
        gui.setItem(38, createGemInfoItem("veyrithcrystal", hasMasterCrystals, 50, extractionSuccess - 0.2, qualityMultiplier, CrystalTier.LEGENDARY));
        gui.setItem(42, createGemInfoItem("drakthyst", hasMasterCrystals, 60, extractionSuccess - 0.25, qualityMultiplier, CrystalTier.LEGENDARY));
    }

    /**
     * Create a tier label for organizing crystal sections
     */
    private static ItemStack createTierLabel(String title, String description) {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + title);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + description);
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create decorative border for GUI - Modified to be more subtle with black glass in middle
     */
    private static void createBorder(Inventory gui) {
        ItemStack blue = createGlassPane(Material.BLUE_STAINED_GLASS_PANE);
        ItemStack lightBlue = createGlassPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemStack cyan = createGlassPane(Material.CYAN_STAINED_GLASS_PANE);
        ItemStack black = createGlassPane(Material.BLACK_STAINED_GLASS_PANE);
        
        // Set corners with special glass color
        gui.setItem(0, cyan);
        gui.setItem(8, cyan);
        gui.setItem(gui.getSize() - 9, cyan);
        gui.setItem(gui.getSize() - 1, cyan);
        
        // Top and bottom rows
        for (int i = 1; i < 8; i++) {
            gui.setItem(i, i % 2 == 0 ? blue : lightBlue);
            gui.setItem(gui.getSize() - 9 + i, i % 2 == 0 ? blue : lightBlue);
        }
        
        // Side borders only - leave center area free
        for (int i = 1; i < gui.getSize() / 9 - 1; i++) {
            gui.setItem(i * 9, i % 2 == 0 ? blue : lightBlue);
            gui.setItem(i * 9 + 8, i % 2 == 0 ? blue : lightBlue);
        }
        
        // Fill middle of border (slots 9, 17, 18, 26, 27, 35, 36, 44) with black glass
        // This creates a visual separation between info elements and main content
        gui.setItem(9, black);
        gui.setItem(17, black);
        gui.setItem(18, black);
        gui.setItem(26, black);
        gui.setItem(27, black);
        gui.setItem(35, black);
        gui.setItem(36, black);
        gui.setItem(44, black);
    }
    
    /**
     * Populate generic subskill details (placeholder for future expansion)
     */
    private static void populateGenericSubskillDetails(Inventory gui, Skill subskill, SkillLevel level) {
        // Add a basic description item since this is a generic subskill
        ItemStack descriptionItem = createStatDisplay(
            Material.BOOK,
            "Subskill Information",
            new String[] {
                "Details about this subskill",
                "",
                ChatColor.GRAY + subskill.getDescription(),
                "",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Current Level: " + 
                ChatColor.WHITE + level.getLevel() + "/" + subskill.getMaxLevel(),
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Total XP Earned: " + 
                ChatColor.WHITE + String.format("%,.1f", level.getTotalXp()),
                "",
                ChatColor.YELLOW + "This subskill doesn't have specialized",
                ChatColor.YELLOW + "information available yet."
            });
        gui.setItem(22, descriptionItem);
    }
    
    /**
     * Create an item with subskill information
     */
    private static ItemStack createSubskillInfoItem(Skill subskill, SkillLevel level) {
        Material icon;
        
        // Choose appropriate icon based on subskill type
        if (subskill instanceof OreExtractionSubskill) {
            icon = Material.IRON_ORE;
        } 
        else if (subskill instanceof GemCarvingSubskill) {
            icon = Material.DIAMOND;
        }
        else {
            icon = Material.NETHER_STAR;
        }
        
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name with enhanced formatting
        meta.setDisplayName(ChatColor.GOLD + "✦ " + subskill.getDisplayName() + " " + 
                ChatColor.YELLOW + "[Level " + level.getLevel() + "/" + subskill.getMaxLevel() + "]");
        
        // Add enchant glow if max level
        if (level.getLevel() >= subskill.getMaxLevel()) {
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        // Hide attributes to keep the tooltip clean
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        // Create lore with dividers for better readability
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        // Format description with line breaks for better readability
        for (String line : subskill.getDescription().split("\\.")) {
            if (!line.trim().isEmpty()) {
                lore.add(ChatColor.GRAY + line.trim() + ".");
            }
        }
        
        lore.add("");
        lore.add(ChatColor.YELLOW + "Total XP Earned: " + ChatColor.WHITE + 
                String.format("%,.1f", level.getTotalXp()));
        
        // Add parent skill
        lore.add("");
        lore.add(ChatColor.AQUA + "» Parent Skill: " + ChatColor.YELLOW + subskill.getParentSkill().getDisplayName());
        
        // Add progress information
        if (level.getLevel() < subskill.getMaxLevel()) {
            double xpForNextLevel = subskill.getXpForLevel(level.getLevel() + 1);
            double progress = level.getProgressPercentage(xpForNextLevel);
            
            lore.add("");
            lore.add(ChatColor.YELLOW + "Progress to Level " + (level.getLevel() + 1) + ":");
            lore.add(createProgressBar(progress));
            lore.add(ChatColor.WHITE + "XP: " + ChatColor.AQUA + String.format("%,.1f", level.getCurrentXp()) + 
                    ChatColor.GRAY + "/" + ChatColor.AQUA + String.format("%,.1f", xpForNextLevel) + 
                    ChatColor.GRAY + " (" + ChatColor.GREEN + String.format("%.1f", progress * 100) + "%" + 
                    ChatColor.GRAY + ")");
        } else {
            lore.add("");
            lore.add(ChatColor.GREEN + "✦ MAXIMUM LEVEL REACHED! ✦");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create a gem information item with crystal tier information
     */
    private static ItemStack createGemInfoItem(String gemType, boolean unlocked, int requiredLevel, 
                                            double extractionSuccess, double qualityMultiplier, CrystalTier tier) {
        Material material = GEM_MATERIALS.getOrDefault(gemType, Material.DIAMOND);
        double baseXp = GEM_XP_VALUES.getOrDefault(gemType, 5.0);
        int quality = GEM_QUALITIES.getOrDefault(gemType, 1);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Format gem name nicely
        String gemName = formatMaterialName(gemType);
        
        // Set color based on whether it's unlocked and tier
        ChatColor nameColor;
        switch (tier) {
            case LEGENDARY:
                nameColor = ChatColor.LIGHT_PURPLE;
                break;
            case EPIC:
                nameColor = ChatColor.DARK_PURPLE;
                break;
            case RARE:
                nameColor = ChatColor.BLUE;
                break;
            case UNCOMMON:
                nameColor = ChatColor.GREEN;
                break;
            default:
                nameColor = ChatColor.WHITE;
        }
        
        if (unlocked) {
            meta.setDisplayName(nameColor + gemName);
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.setDisplayName(ChatColor.RED + gemName + ChatColor.GRAY + " (Locked)");
        }
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        if (unlocked) {
            // Add tier
            lore.add(ChatColor.YELLOW + "Tier: " + getTierDisplay(tier));
            
            // Quality rating
            lore.add(ChatColor.YELLOW + "Quality: " + getQualityStars(quality));
            
            // Success chance - reduced for higher tier gems
            double tierSuccessModifier = 1.0;
            switch (tier) {
                case LEGENDARY:
                    tierSuccessModifier = 0.7; // 30% reduction for legendary
                    break;
                case EPIC:
                    tierSuccessModifier = 0.8; // 20% reduction for epic
                    break;
                case RARE:
                    tierSuccessModifier = 0.9; // 10% reduction for rare
                    break;
                default:
                    tierSuccessModifier = 1.0; // No reduction for common/uncommon
            }
            
            double adjustedSuccess = extractionSuccess * tierSuccessModifier;
            lore.add(ChatColor.YELLOW + "Extraction Success: " + 
                    ChatColor.WHITE + String.format("%.1f%%", adjustedSuccess));
            
            // Show XP gained
            lore.add(ChatColor.YELLOW + "Base XP: " + ChatColor.WHITE + baseXp);
            
            double bonusXp = baseXp * (qualityMultiplier - 1.0);
            if (bonusXp > 0) {
                lore.add(ChatColor.YELLOW + "Quality Bonus: " + ChatColor.GREEN + "+" + String.format("%.1f", bonusXp));
            }
            
            lore.add(ChatColor.YELLOW + "Total XP per Extraction: " + ChatColor.AQUA + String.format("%.1f", baseXp + bonusXp));
            
            // Add extraction difficulty based on tier
            lore.add("");
            lore.add(ChatColor.AQUA + "» " + ChatColor.YELLOW + "Extraction Difficulty:");
            
            switch (tier) {
                case LEGENDARY:
                    lore.add(ChatColor.LIGHT_PURPLE + "Extremely Difficult");
                    lore.add(ChatColor.GRAY + "8-9 carving points");
                    lore.add(ChatColor.GRAY + "Short time limit");
                    break;
                case EPIC:
                    lore.add(ChatColor.DARK_PURPLE + "Very Difficult");
                    lore.add(ChatColor.GRAY + "6-7 carving points");
                    lore.add(ChatColor.GRAY + "Limited time");
                    break;
                case RARE:
                    lore.add(ChatColor.BLUE + "Challenging");
                    lore.add(ChatColor.GRAY + "5-6 carving points");
                    lore.add(ChatColor.GRAY + "Moderate time limit");
                    break;
                case UNCOMMON:
                    lore.add(ChatColor.GREEN + "Moderate");
                    lore.add(ChatColor.GRAY + "4-5 carving points");
                    lore.add(ChatColor.GRAY + "Comfortable time limit");
                    break;
                default:
                    lore.add(ChatColor.WHITE + "Basic");
                    lore.add(ChatColor.GRAY + "3-5 carving points");
                    lore.add(ChatColor.GRAY + "Generous time limit");
            }
        } else {
            // Show that it's locked
            lore.add(ChatColor.RED + "This crystal type is currently locked.");
            
            if (requiredLevel > 0) {
                lore.add(ChatColor.YELLOW + "Required Level: " + ChatColor.WHITE + requiredLevel);
            }
            
            // Add skill tree hint
            if (requiredLevel >= 30) {
                lore.add("");
                lore.add(ChatColor.YELLOW + "Unlock via Skill Tree:");
                
                if (requiredLevel >= 50) {
                    lore.add(ChatColor.GRAY + "• Master Crystals node");
                } else {
                    lore.add(ChatColor.GRAY + "• Advanced Crystals node");
                }
            }
        }
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create an ore information item with more detailed info
     */
    private static ItemStack createOreInfoItem(String oreType, boolean unlocked, int requiredLevel, double fortuneBonus) {
        Material material = ORE_MATERIALS.getOrDefault(oreType, Material.STONE);
        double baseXp = ORE_XP_VALUES.getOrDefault(oreType, 1.0);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Format ore name nicely
        String oreName = formatMaterialName(oreType);
        
        // Set color based on whether it's unlocked
        if (unlocked) {
            meta.setDisplayName(ChatColor.GREEN + oreName);
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.setDisplayName(ChatColor.RED + oreName + ChatColor.GRAY + " (Locked)");
        }
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        if (unlocked) {
            // Show XP gained and modifiers
            lore.add(ChatColor.YELLOW + "Base XP: " + ChatColor.WHITE + baseXp);
            
            // Calculate fortune bonus for this specific ore
            double bonusXp = baseXp * (fortuneBonus / 10.0);
            if (bonusXp > 0) {
                lore.add(ChatColor.YELLOW + "Fortune Bonus: " + ChatColor.GREEN + "+" + String.format("%.1f", bonusXp));
            }
            
            lore.add(ChatColor.YELLOW + "Total XP per Mine: " + ChatColor.AQUA + String.format("%.1f", baseXp + bonusXp));
            
            // Add mining information
            lore.add("");
            lore.add(ChatColor.AQUA + "» " + ChatColor.YELLOW + "Possible Drops:");
            
            // Add common drops
            if (oreType.contains("deepslate_")) {
                String baseOreName = oreName.replace("Deepslate ", "");
                lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + baseOreName);
                
                // Higher drop rate and potential bonus drops for deepslate variants
                lore.add(ChatColor.GRAY + "• " + ChatColor.GREEN + "+25% " + ChatColor.GRAY + "drop quantity");
            } else {
                lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + oreName.replace(" Ore", ""));
            }
            
            // Add rare drops if applicable
            if (oreType.equals("diamond") || oreType.equals("deepslate_diamond") || 
                oreType.equals("emerald") || oreType.equals("deepslate_emerald")) {
                lore.add(ChatColor.GRAY + "• " + ChatColor.LIGHT_PURPLE + "Rare Gems " + ChatColor.GRAY + "(Low Chance)");
            }
            
            if (oreType.equals("ancient_debris")) {
                lore.add(ChatColor.GRAY + "• " + ChatColor.LIGHT_PURPLE + "Netherite Scraps");
                lore.add(ChatColor.GRAY + "• " + ChatColor.GOLD + "Ancient Artifacts " + ChatColor.GRAY + "(Very Low Chance)");
            }
            
            // Add mining time info for deepslate variants
            if (oreType.contains("deepslate_")) {
                lore.add("");
                lore.add(ChatColor.RED + "» " + ChatColor.YELLOW + "Mining Notes:");
                lore.add(ChatColor.GRAY + "Deepslate variants take longer");
                lore.add(ChatColor.GRAY + "to mine but give more XP and drops");
            }
            
            // Add nether information for nether ores
            if (oreType.contains("nether_") || oreType.equals("ancient_debris")) {
                lore.add("");
                lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Nether Ore:");
                lore.add(ChatColor.GRAY + "Found only in the Nether");
            }
        } else {
            // Show that it's locked
            lore.add(ChatColor.RED + "This ore is currently locked.");
            
            // Add skill tree node hint
            lore.add("");
            lore.add(ChatColor.YELLOW + "Unlock via Skill Tree:");
            
            if (oreType.startsWith("gold") || oreType.startsWith("deepslate_gold")) {
                lore.add(ChatColor.GRAY + "• Gold Mining node");
            } else if (oreType.startsWith("redstone") || oreType.startsWith("deepslate_redstone")) {
                lore.add(ChatColor.GRAY + "• Redstone Mining node");
            } else if (oreType.startsWith("lapis") || oreType.startsWith("deepslate_lapis")) {
                lore.add(ChatColor.GRAY + "• Lapis Mining node");
            } else if (oreType.startsWith("diamond") || oreType.startsWith("deepslate_diamond")) {
                lore.add(ChatColor.GRAY + "• Diamond Mining node");
            } else if (oreType.startsWith("emerald") || oreType.startsWith("deepslate_emerald")) {
                lore.add(ChatColor.GRAY + "• Emerald Mining node");
            } else if (oreType.contains("nether")) {
                lore.add(ChatColor.GRAY + "• Nether Mining node");
            } else if (oreType.equals("ancient_debris")) {
                lore.add(ChatColor.GRAY + "• Ancient Debris Mining node");
            } else if (oreType.startsWith("iron") || oreType.startsWith("deepslate_iron")) {
                lore.add(ChatColor.GRAY + "• Reach level 5 to unlock");
            }
        }
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Get text representation of unlock status
     */
    private static String getUnlockStatusText(boolean unlocked) {
        return unlocked ? ChatColor.GREEN + "Unlocked" : ChatColor.RED + "Locked";
    }

    /**
     * Create an info display item
     */
    private static ItemStack createInfoItem(Material material, String title, String[] description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);
        
        // Add enchant glow for visual appeal
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        for (String line : description) {
            lore.add(ChatColor.GRAY + line);
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create a section title item
     */
    private static ItemStack createSectionTitle(String title, String description) {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "✦ " + title + " ✦");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + description);
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create a stat display item
     */
    private static ItemStack createStatDisplay(Material material, String title, String[] description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "✦ " + title + " ✦");
        
        // Add enchant glow for visual appeal
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        for (String line : description) {
            lore.add(ChatColor.GRAY + line);
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create back button with parent skill reference
     */
    private static ItemStack createBackButton(Skill parentSkill) {
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta meta = backButton.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "« Back to " + parentSkill.getDisplayName() + " Details");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Return to the skill details screen");
        lore.add("");
        // Add hidden data to identify which skill to return to
        lore.add(ChatColor.BLACK + "SKILL_ID:" + parentSkill.getId());
        meta.setLore(lore);
        
        backButton.setItemMeta(meta);
        return backButton;
    }
    
    /**
     * Create help button with information about the GUI
     */
    private static ItemStack createHelpButton(Skill subskill) {
        ItemStack helpButton = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = helpButton.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Information");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "This screen shows specialized information");
        lore.add(ChatColor.GRAY + "about the " + ChatColor.YELLOW + subskill.getDisplayName() + ChatColor.GRAY + " subskill.");
        lore.add("");
        
        if (subskill instanceof OreExtractionSubskill) {
            lore.add(ChatColor.AQUA + "» " + ChatColor.YELLOW + "Ore Information:");
            lore.add(ChatColor.GRAY + "• Green items are unlocked ores");
            lore.add(ChatColor.GRAY + "• Red items are locked (need higher level)");
            lore.add(ChatColor.GRAY + "• XP values shown are per block mined");
        }
        else if (subskill instanceof GemCarvingSubskill) {
            lore.add(ChatColor.AQUA + "» " + ChatColor.YELLOW + "Gem Information:");
            lore.add(ChatColor.GRAY + "• Green items are unlocked gems");
            lore.add(ChatColor.GRAY + "• Red items are locked (need higher level)");
            lore.add(ChatColor.GRAY + "• Stars indicate gem quality & value");
        }
        
        meta.setLore(lore);
        helpButton.setItemMeta(meta);
        return helpButton;
    }
    
    /**
     * Generate quality stars display
     */
    private static String getQualityStars(int quality) {
        StringBuilder stars = new StringBuilder();
        
        for (int i = 0; i < quality; i++) {
            stars.append(ChatColor.GOLD + "★");
        }
        for (int i = quality; i < 7; i++) {
            stars.append(ChatColor.GRAY + "★");
        }
        
        return stars.toString();
    }
    
    /**
     * Format material name for display
     */
    private static String formatMaterialName(String materialName) {
        String[] words = materialName.replaceAll("_", " ").split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase())
                    .append(" ");
            }
        }
        
        return result.toString().trim();
    }
    
    /**
     * Create a progress bar visualization
     */
    private static String createProgressBar(double progress) {
        StringBuilder bar = new StringBuilder();
        int barLength = 24;
        int filledBars = (int) Math.round(progress * barLength);
        
        // Start with bracket
        bar.append(ChatColor.GRAY + "[");
        
        // Create gradient of colors based on fill
        for (int i = 0; i < barLength; i++) {
            if (i < filledBars) {
                if (progress < 0.25) {
                    bar.append(ChatColor.RED);
                } else if (progress < 0.5) {
                    bar.append(ChatColor.GOLD);
                } else if (progress < 0.75) {
                    bar.append(ChatColor.YELLOW);
                } else {
                    bar.append(ChatColor.GREEN);
                }
                bar.append("■");
            } else {
                bar.append(ChatColor.DARK_GRAY).append("■");
            }
        }
        
        // Close bracket
        bar.append(ChatColor.GRAY + "]");
        
        return bar.toString();
    }
    
    /**
     * Check if an ore is unlocked at the given level
     * Implements level requirements for different ore types
     */
    private static boolean isOreUnlocked(String oreType, int level) {
        return level >= getRequiredLevel(oreType);
    }
    
    /**
     * Get required level for an ore type
     */
    private static int getRequiredLevel(String oreType) {
        switch (oreType) {
            case "coal": return 1;
            case "iron": return 5;
            case "gold": return 10;
            case "redstone": return 15;
            case "lapis": return 20;
            case "diamond": return 25;
            case "emerald": return 30;
            case "nether_quartz": return 15;
            case "nether_gold": return 20;
            case "ancient_debris": return 40;
            default: return 1;
        }
    }
    
    /**
     * Check if a gem is unlocked at the given level
     */
    private static boolean isGemUnlocked(String gemType, int level) {
        return level >= getGemRequiredLevel(gemType);
    }
    
    /**
     * Get required level for a gem type
     */
    private static int getGemRequiredLevel(String gemType) {
        switch (gemType) {
            case "amber": return 1;
            case "amethyst": return 5;
            case "sapphire": return 15;
            case "ruby": return 25;
            case "emerald_crystal": return 35;
            case "diamond_crystal": return 45;
            case "obsidian_crystal": return 50;
            default: return 1;
        }
    }
    
    /**
     * Create glass pane with empty name for decoration
     */
    private static ItemStack createGlassPane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }
    
    /**
     * Fill empty slots with glass panes
     */
    private static void fillEmptySlots(Inventory gui) {
        ItemStack filler = createGlassPane(Material.BLACK_STAINED_GLASS_PANE);
        
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }

    /**
     * Get display text for crystal tiers
     */
    private static String getTierDisplay(CrystalTier tier) {
        switch (tier) {
            case LEGENDARY:
                return ChatColor.LIGHT_PURPLE + "Legendary";
            case EPIC:
                return ChatColor.DARK_PURPLE + "Epic";
            case RARE:
                return ChatColor.BLUE + "Rare";
            case UNCOMMON:
                return ChatColor.GREEN + "Uncommon";
            default:
                return ChatColor.WHITE + "Common";
        }
    }

    /**
     * Enum for crystal tiers from the minigame
     */
    private enum CrystalTier {
        COMMON, UNCOMMON, RARE, EPIC, LEGENDARY
    }
}