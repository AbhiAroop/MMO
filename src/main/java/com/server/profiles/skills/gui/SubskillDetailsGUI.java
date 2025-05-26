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
        if (subskill.isMainSkill()) {
            player.sendMessage(ChatColor.RED + "This is not a subskill!");
            return;
        }
        
        // Create inventory
        String title = GUI_TITLE_PREFIX + subskill.getDisplayName() + " Details" + GUI_TITLE_SUFFIX;
        Inventory gui = Bukkit.createInventory(null, 54, title);
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        SkillLevel level = profile.getSkillData().getSkillLevel(subskill);
        
        // Create decorative border
        createBorder(gui);
        
        // === HEADER: Subskill title in slot 4 (top row) ===
        ItemStack headerItem = createSubskillHeaderItem(subskill, level);
        gui.setItem(4, headerItem);
        
        // === CONTENT: Ores/gems in 28 slots starting from slot 10 ===
        if (subskill instanceof OreExtractionSubskill) {
            populateOreExtractionContent(gui, (OreExtractionSubskill) subskill, level, player, profile);
        } else if (subskill instanceof GemCarvingSubskill) {
            populateGemCarvingContent(gui, (GemCarvingSubskill) subskill, level, player, profile);
        } else {
            // Generic subskill - show a simple message
            ItemStack infoItem = new ItemStack(Material.PAPER);
            ItemMeta meta = infoItem.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + "Subskill Information");
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "This subskill is still being developed.");
            lore.add(ChatColor.GRAY + "More features coming soon!");
            
            meta.setLore(lore);
            infoItem.setItemMeta(meta);
            gui.setItem(22, infoItem); // Center of available area
        }
        
        // === BOTTOM NAVIGATION: Back button and help button ===
        ItemStack backButton = createBackButton(subskill);
        gui.setItem(45, backButton); // Bottom left corner
        
        ItemStack helpButton = createHelpButton(subskill);
        gui.setItem(53, helpButton); // Bottom right corner
        
        // Open inventory
        player.openInventory(gui);
    }

    /**
     * Populate ore extraction content - COMPACT LAYOUT using 28 slots
     */
    private static void populateOreExtractionContent(Inventory gui, OreExtractionSubskill subskill, 
                                            SkillLevel level, Player player, PlayerProfile profile) {
        
        // Define ore types
        String[] oreTypes = {
            "coal", "copper", "iron", "gold", "redstone", 
            "lapis", "diamond", "emerald", "nether_quartz", 
            "nether_gold", "ancient_debris"
        };
        
        // Deepslate variants
        String[] deepslateOres = {
            "deepslate_coal", "deepslate_copper", "deepslate_iron", 
            "deepslate_gold", "deepslate_redstone", "deepslate_lapis", 
            "deepslate_diamond", "deepslate_emerald"
        };
        
        // Use 28 slots in 4 rows of 7 (avoiding border)
        // Row 2: slots 10-16 (7 slots)
        // Row 3: slots 19-25 (7 slots) 
        // Row 4: slots 28-34 (7 slots)
        // Row 5: slots 37-43 (7 slots)
        int[] positions = {
            10, 11, 12, 13, 14, 15, 16,  // Row 2
            19, 20, 21, 22, 23, 24, 25,  // Row 3
            28, 29, 30, 31, 32, 33, 34,  // Row 4
            37, 38, 39, 40, 41, 42, 43   // Row 5
        };
        
        int index = 0;
        
        // Place regular ores first
        for (String oreType : oreTypes) {
            if (index >= positions.length) break;
            
            boolean unlocked = isOreUnlocked(oreType, level.getLevel(), profile);
            ItemStack oreItem = createOreItem(oreType, unlocked, level.getLevel());
            gui.setItem(positions[index], oreItem);
            index++;
        }
        
        // Place deepslate ores in remaining slots
        for (String deepslateOre : deepslateOres) {
            if (index >= positions.length) break;
            
            boolean unlocked = isOreUnlocked(deepslateOre, level.getLevel(), profile);
            ItemStack oreItem = createOreItem(deepslateOre, unlocked, level.getLevel());
            gui.setItem(positions[index], oreItem);
            index++;
        }
    }

    /**
     * Populate gem carving content - COMPACT LAYOUT using 28 slots
     */
    private static void populateGemCarvingContent(Inventory gui, GemCarvingSubskill subskill, 
                                            SkillLevel level, Player player, PlayerProfile profile) {
        
        // Define gem types - FIXED: Only include gems that are actually in the GemCarvingSubskill
        String[] gemTypes = {
            "mooncrystal", "azuralite", "pyrethine", "solvanecystal", 
            "nyxstone", "lucenthar", "veyrithcrystal", "drakthyst"
            // Removed all the extra gems that aren't actually implemented
        };
        
        // Use only the first 8 slots in the first row since we only have 8 gems
        int[] positions = {
            10, 11, 12, 13, 14, 15, 16, 19  // First row + one in second row
        };
        
        for (int i = 0; i < gemTypes.length && i < positions.length; i++) {
            String gemType = gemTypes[i];
            boolean unlocked = isGemUnlocked(gemType, level.getLevel(), profile);
            ItemStack gemItem = createGemItem(gemType, unlocked, level.getLevel());
            gui.setItem(positions[i], gemItem);
        }
        
        // Add a note about additional gems coming soon in the remaining visible slots
        if (gemTypes.length < 12) { // If we have space for more info
            ItemStack comingSoonItem = new ItemStack(Material.PAPER);
            ItemMeta meta = comingSoonItem.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "More Gems Coming Soon!");
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Additional gem types will be");
            lore.add(ChatColor.GRAY + "added in future updates.");
            lore.add("");
            lore.add(ChatColor.AQUA + "Current gems available: " + gemTypes.length);
            
            meta.setLore(lore);
            comingSoonItem.setItemMeta(meta);
            gui.setItem(20, comingSoonItem); // Place in a visible spot
        }
    }

    /**
     * Create a detailed header item for the subskill (slot 4)
     */
    private static ItemStack createSubskillHeaderItem(Skill subskill, SkillLevel level) {
        Material icon = getSubskillIcon(subskill);
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        // Add glow effect if max level
        if (level.getLevel() >= subskill.getMaxLevel()) {
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        meta.setDisplayName(ChatColor.GOLD + "✦ " + ChatColor.AQUA + subskill.getDisplayName() + ChatColor.GOLD + " ✦");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("");
        
        // Description
        for (String line : subskill.getDescription().split("\n")) {
            lore.add(ChatColor.GRAY + line);
        }
        
        lore.add("");
        lore.add(ChatColor.YELLOW + "Current Level: " + ChatColor.WHITE + level.getLevel() + "/" + subskill.getMaxLevel());
        
        // Progress information
        if (level.getLevel() < subskill.getMaxLevel()) {
            double currentXp = level.getCurrentXp();
            double neededXp = subskill.getXpForLevel(level.getLevel() + 1);
            double progress = currentXp / neededXp;
            
            lore.add(ChatColor.YELLOW + "Progress: " + createCompactProgressBar(progress));
            lore.add(ChatColor.YELLOW + "XP to Next: " + ChatColor.WHITE + String.format("%.0f", neededXp - currentXp));
        } else {
            lore.add(ChatColor.GOLD + "★ MASTERED ★");
        }
        
        // Next milestone
        String nextMilestone = getNextMilestone(subskill, level.getLevel());
        if (nextMilestone != null) {
            lore.add("");
            lore.add(ChatColor.AQUA + "Next Milestone: " + ChatColor.YELLOW + nextMilestone);
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create a back button for subskill details GUI
     */
    private static ItemStack createBackButton(Skill subskill) {
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta meta = backButton.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "« Back to " + subskill.getDisplayName() + " Details");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Return to the skill details menu");
        
        // FIXED: Store the subskill ID, not the parent skill ID
        lore.add(ChatColor.BLACK + "SUBSKILL_ID:" + subskill.getId());
        
        meta.setLore(lore);
        backButton.setItemMeta(meta);
        return backButton;
    }
    

    /**
     * Create a help button for subskill details GUI
     */
    private static ItemStack createHelpButton(Skill subskill) {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "✦ Help & Information");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + "Learn about " + subskill.getDisplayName() + ":");
        lore.add("");
        
        if (subskill instanceof OreExtractionSubskill) {
            lore.add(ChatColor.AQUA + "Ore Extraction:");
            lore.add(ChatColor.WHITE + "• Mine different ore types for XP");
            lore.add(ChatColor.WHITE + "• Higher levels = faster mining");
            lore.add(ChatColor.WHITE + "• Deepslate ores give +25% XP");
            lore.add(ChatColor.WHITE + "• Unlock ores via skill tree");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Tips:");
            lore.add(ChatColor.GRAY + "• Green checkmark = unlocked");
            lore.add(ChatColor.GRAY + "• Red X = locked");
            lore.add(ChatColor.GRAY + "• XP values shown for each ore");
        } else if (subskill instanceof GemCarvingSubskill) {
            lore.add(ChatColor.AQUA + "Gem Carving:");
            lore.add(ChatColor.WHITE + "• Find gems while mining stone");
            lore.add(ChatColor.WHITE + "• Careful extraction required");
            lore.add(ChatColor.WHITE + "• Higher levels = better rates");
            lore.add(ChatColor.WHITE + "• Different gems unlock by level");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Rarity Colors:");
            lore.add(ChatColor.WHITE + "• " + ChatColor.WHITE + "Common");
            lore.add(ChatColor.WHITE + "• " + ChatColor.GREEN + "Uncommon");
            lore.add(ChatColor.WHITE + "• " + ChatColor.BLUE + "Rare");
            lore.add(ChatColor.WHITE + "• " + ChatColor.DARK_PURPLE + "Epic");
            lore.add(ChatColor.WHITE + "• " + ChatColor.GOLD + "Legendary");
        } else {
            lore.add(ChatColor.YELLOW + "• Level up by practicing this skill");
            lore.add(ChatColor.YELLOW + "• Higher levels provide better bonuses");
            lore.add(ChatColor.YELLOW + "• Milestone levels award tokens");
        }
        
        lore.add("");
        lore.add(ChatColor.AQUA + "Token System:");
        lore.add(ChatColor.WHITE + "Tokens from this subskill go to the");
        lore.add(ChatColor.WHITE + "" + ChatColor.YELLOW + subskill.getParentSkill().getDisplayName() + 
                ChatColor.WHITE + " skill tree for upgrades.");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create a simple ore item showing XP value and unlock status
     */
    private static ItemStack createOreItem(String oreType, boolean unlocked, int playerLevel) {
        Material oreMaterial = ORE_MATERIALS.getOrDefault(oreType, Material.STONE);
        ItemStack item = new ItemStack(oreMaterial);
        ItemMeta meta = item.getItemMeta();
        
        // Get ore display name
        String displayName = formatOreName(oreType);
        double xpValue = ORE_XP_VALUES.getOrDefault(oreType, 0.0);
        
        if (unlocked) {
            meta.setDisplayName(ChatColor.GREEN + "✓ " + ChatColor.WHITE + displayName);
            
            // Add glow for unlocked ores
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.setDisplayName(ChatColor.RED + "✗ " + ChatColor.GRAY + displayName + " (Locked)");
        }
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        if (unlocked) {
            lore.add(ChatColor.YELLOW + "XP Value: " + ChatColor.GREEN + xpValue);
            
            if (oreType.contains("deepslate")) {
                lore.add(ChatColor.AQUA + "Deepslate Bonus: " + ChatColor.GREEN + "+25% XP");
            }
            
            lore.add("");
            lore.add(ChatColor.GREEN + "✓ Ready to mine!");
            lore.add(ChatColor.GRAY + "Break this ore type to gain XP");
        } else {
            lore.add(ChatColor.RED + "Locked");
            lore.add("");
            lore.add(ChatColor.GRAY + "Unlock this ore type by purchasing");
            lore.add(ChatColor.GRAY + "the required node in the skill tree.");
            
            // Show potential XP value
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "XP Value when unlocked: " + xpValue);
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create a simple gem item showing XP value and unlock status
     */
    private static ItemStack createGemItem(String gemType, boolean unlocked, int playerLevel) {
        Material gemMaterial = GEM_MATERIALS.getOrDefault(gemType, Material.QUARTZ);
        ItemStack item = new ItemStack(gemMaterial);
        ItemMeta meta = item.getItemMeta();
        
        // Get gem display name and properties
        String displayName = formatGemName(gemType);
        double xpValue = GEM_XP_VALUES.getOrDefault(gemType, 100.0);
        String rarity = getGemRarity(gemType);
        ChatColor rarityColor = getRarityColor(rarity);
        
        if (unlocked) {
            meta.setDisplayName(ChatColor.GREEN + "✓ " + rarityColor + displayName);
            
            // Add glow for unlocked gems
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.setDisplayName(ChatColor.RED + "✗ " + ChatColor.GRAY + displayName + " (Locked)");
        }
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + "Rarity: " + rarityColor + rarity);
        
        if (unlocked) {
            lore.add(ChatColor.YELLOW + "XP Value: " + ChatColor.GREEN + String.format("%.0f", xpValue));
            lore.add("");
            lore.add(ChatColor.GREEN + "✓ Available for extraction!");
            lore.add(ChatColor.GRAY + "Found while mining stone blocks");
            lore.add(ChatColor.GRAY + "Success rate depends on level");
        } else {
            lore.add("");
            lore.add(ChatColor.RED + "Locked");
            lore.add("");
            lore.add(ChatColor.GRAY + "Unlock by reaching level " + getGemUnlockLevel(gemType));
            lore.add(ChatColor.GRAY + "in Gem Carving subskill.");
            
            // Show potential XP value
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "XP Value when unlocked: " + String.format("%.0f", xpValue));
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Enhanced method to get expanded gem types with unlock levels
     */
    private static int getGemUnlockLevel(String gemType) {
        Map<String, Integer> gemUnlockLevels = new HashMap<>();
        
        // Basic gems (early levels)
        gemUnlockLevels.put("mooncrystal", 1);
        gemUnlockLevels.put("azuralite", 5);
        gemUnlockLevels.put("pyrethine", 10);
        gemUnlockLevels.put("solvanecystal", 15);
        
        // Intermediate gems
        gemUnlockLevels.put("nyxstone", 20);
        gemUnlockLevels.put("lucenthar", 25);
        gemUnlockLevels.put("veyrithcrystal", 30);
        gemUnlockLevels.put("drakthyst", 35);
                
        return gemUnlockLevels.getOrDefault(gemType, 1);
    }

    /**
     * Enhanced gem name formatting - FIXED to only include actual gems
     */
    private static String formatGemName(String gemType) {
        Map<String, String> gemNames = new HashMap<>();
        
        // Only the gems that are actually implemented
        gemNames.put("mooncrystal", "Mooncrystal");
        gemNames.put("azuralite", "Azuralite");
        gemNames.put("pyrethine", "Pyrethine");
        gemNames.put("solvanecystal", "Solvane Crystal");
        gemNames.put("nyxstone", "Nyxstone");
        gemNames.put("lucenthar", "Lucenthar");
        gemNames.put("veyrithcrystal", "Veyrith Crystal");
        gemNames.put("drakthyst", "Drakthyst");
        
        return gemNames.getOrDefault(gemType, gemType);
    }

    /**
     * Enhanced gem rarity system - FIXED to only include actual gems
     */
    private static String getGemRarity(String gemType) {
        Map<String, String> gemRarities = new HashMap<>();
        
        // Progression-based rarity for the 8 actual gems
        gemRarities.put("mooncrystal", "Common");
        gemRarities.put("azuralite", "Common");
        gemRarities.put("pyrethine", "Uncommon");
        gemRarities.put("solvanecystal", "Uncommon");
        gemRarities.put("nyxstone", "Rare");
        gemRarities.put("lucenthar", "Rare");
        gemRarities.put("veyrithcrystal", "Epic");
        gemRarities.put("drakthyst", "Legendary");
        
        return gemRarities.getOrDefault(gemType, "Common");
    }

    /**
     * Get the icon material for a specific subskill
     */
    private static Material getSubskillIcon(Skill subskill) {
        switch (subskill.getId()) {
            case "ore_extraction":
                return Material.IRON_ORE;
            case "gem_carving":
                return Material.DIAMOND;
            default:
                return Material.PAPER;
        }
    }

    /**
     * Create decorative border with accent corners
     */
    private static void createBorder(Inventory gui) {
        ItemStack borderItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderItem.getItemMeta();
        borderMeta.setDisplayName(" ");
        borderItem.setItemMeta(borderMeta);
        
        // Top and bottom borders
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, borderItem);
            gui.setItem(45 + i, borderItem);
        }
        
        // Side borders
        for (int i = 1; i < 5; i++) {
            gui.setItem(i * 9, borderItem);
            gui.setItem(i * 9 + 8, borderItem);
        }
        
        // Accent corners with colored glass
        ItemStack accentItem = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta accentMeta = accentItem.getItemMeta();
        accentMeta.setDisplayName(" ");
        accentItem.setItemMeta(accentMeta);
        
        gui.setItem(0, accentItem);  // Top left
        gui.setItem(8, accentItem);  // Top right
        gui.setItem(45, accentItem); // Will be replaced by back button
        gui.setItem(53, accentItem); // Will be replaced by help button
    }

    /**
     * Check if an ore type is unlocked for the player
     */
    private static boolean isOreUnlocked(String oreType, int playerLevel, PlayerProfile profile) {
        // Basic ores are always unlocked
        if (oreType.equals("coal") || oreType.equals("deepslate_coal") || 
            oreType.equals("copper") || oreType.equals("deepslate_copper")) {
            return true;
        }
        
        // Check skill tree unlocks
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        
        // Map ore types to their skill tree nodes
        Map<String, String> oreToNode = new HashMap<>();
        oreToNode.put("iron", "unlock_iron_ore");
        oreToNode.put("deepslate_iron", "unlock_iron_ore");
        oreToNode.put("gold", "unlock_gold_ore");
        oreToNode.put("deepslate_gold", "unlock_gold_ore");
        oreToNode.put("redstone", "unlock_redstone_ore");
        oreToNode.put("deepslate_redstone", "unlock_redstone_ore");
        oreToNode.put("lapis", "unlock_lapis_ore");
        oreToNode.put("deepslate_lapis", "unlock_lapis_ore");
        oreToNode.put("diamond", "unlock_diamond_ore");
        oreToNode.put("deepslate_diamond", "unlock_diamond_ore");
        oreToNode.put("emerald", "unlock_emerald_ore");
        oreToNode.put("deepslate_emerald", "unlock_emerald_ore");
        oreToNode.put("nether_quartz", "unlock_nether_mining");
        oreToNode.put("nether_gold", "unlock_nether_mining");
        oreToNode.put("ancient_debris", "unlock_ancient_debris");
        
        String requiredNode = oreToNode.get(oreType);
        if (requiredNode != null) {
            return treeData.isNodeUnlocked("mining", requiredNode);
        }
        
        return true; // Default to unlocked if no specific requirement
    }

    /**
     * Check if a gem type is unlocked for the player
     */
    private static boolean isGemUnlocked(String gemType, int playerLevel, PlayerProfile profile) {
        // Map gem types to unlock levels
        Map<String, Integer> gemUnlockLevels = new HashMap<>();
        gemUnlockLevels.put("mooncrystal", 1);
        gemUnlockLevels.put("azuralite", 5);
        gemUnlockLevels.put("pyrethine", 15);
        gemUnlockLevels.put("solvanecystal", 25);
        gemUnlockLevels.put("nyxstone", 35);
        gemUnlockLevels.put("lucenthar", 50);
        gemUnlockLevels.put("veyrithcrystal", 70);
        gemUnlockLevels.put("drakthyst", 90);
        
        int requiredLevel = gemUnlockLevels.getOrDefault(gemType, 1);
        return playerLevel >= requiredLevel;
    }

    /**
     * Get the unlock level for an ore type
     */
    private static int getOreUnlockLevel(String oreType) {
        // Most ores are unlocked via skill tree, not level
        return 0;
    }

    /**
     * Format ore name for display
     */
    private static String formatOreName(String oreType) {
        String formatted = oreType.replace("_", " ")
                    .replace("deepslate ", "Deepslate ")
                    .replace("nether ", "Nether ")
                    .replace("ancient debris", "Ancient Debris");
        
        StringBuilder result = new StringBuilder();
        for (String word : formatted.split(" ")) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
            }
        }
        return result.toString().trim();
    }

    /**
     * Get color for gem rarity
     */
    private static ChatColor getRarityColor(String rarity) {
        switch (rarity.toLowerCase()) {
            case "common": return ChatColor.WHITE;
            case "uncommon": return ChatColor.GREEN;
            case "rare": return ChatColor.BLUE;
            case "epic": return ChatColor.DARK_PURPLE;
            case "legendary": return ChatColor.GOLD;
            default: return ChatColor.GRAY;
        }
    }

    /**
     * Create a compact progress bar
     */
    private static String createCompactProgressBar(double progress) {
        StringBuilder bar = new StringBuilder();
        int barLength = 10;
        int filledBars = (int) Math.round(progress * barLength);
        
        bar.append(ChatColor.GRAY + "[");
        
        for (int i = 0; i < barLength; i++) {
            if (i < filledBars) {
                if (progress < 0.33) {
                    bar.append(ChatColor.RED);
                } else if (progress < 0.66) {
                    bar.append(ChatColor.YELLOW);
                } else {
                    bar.append(ChatColor.GREEN);
                }
                bar.append("█");
            } else {
                bar.append(ChatColor.DARK_GRAY).append("█");
            }
        }
        
        bar.append(ChatColor.GRAY + "] " + ChatColor.WHITE + String.format("%.1f%%", progress * 100));
        return bar.toString();
    }

    /**
     * Get the next milestone level for a skill
     */
    private static String getNextMilestone(Skill skill, int currentLevel) {
        for (int milestoneLevel : skill.getMilestones()) {
            if (milestoneLevel > currentLevel) {
                return "Level " + milestoneLevel;
            }
        }
        return null; // No more milestones
    }
    
    /**
     * Enum for crystal tiers from the minigame
     */
    private enum CrystalTier {
        COMMON, UNCOMMON, RARE, EPIC, LEGENDARY
    }
}