package com.server.profiles.skills.gui;

import java.util.ArrayList;
import java.util.List;

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
import com.server.profiles.skills.abilities.AbilityRegistry;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.data.SkillLevel;
import com.server.profiles.skills.data.SkillReward;
import com.server.profiles.skills.skills.mining.subskills.GemCarvingSubskill;
import com.server.profiles.skills.skills.mining.subskills.OreExtractionSubskill;
import com.server.profiles.skills.tokens.SkillToken;
import com.server.profiles.skills.trees.PlayerSkillTreeData;

/**
 * GUI for displaying detailed information about a skill
 */
public class SkillDetailsGUI {

    // Keep the GUI title prefix the same to maintain compatibility with listeners
    private static final String GUI_TITLE_PREFIX = "Skill Details: ";
    
    /**
     * Open the skill details menu for a player
     */
    public static void openSkillDetailsMenu(Player player, Skill skill) {
        // Create inventory with a larger size for better layout (36 slots instead of 27)
        Inventory gui = Bukkit.createInventory(null, 36, GUI_TITLE_PREFIX + skill.getDisplayName());
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) {
            player.sendMessage(ChatColor.RED + "You need to select a profile first!");
            return;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Get skill level
        SkillLevel level = profile.getSkillData().getSkillLevel(skill);
        
        // Create decorative border
        createBorder(gui);
        
        // Add skill info in top center
        ItemStack infoItem = createSkillInfoItem(skill, level);
        gui.setItem(4, infoItem);
        
        // Add progress bar in center
        ItemStack progressItem = createProgressItem(skill, level);
        gui.setItem(13, progressItem);
        
        // Add rewards button to the left
        ItemStack rewardsItem = createRewardsItem(skill, level);
        gui.setItem(11, rewardsItem);
        
        // Add skill tree button to the right
        ItemStack skillTreeItem = createSkillTreeItem(skill, profile);
        gui.setItem(15, skillTreeItem);
        
        // Add abilities button below center
        ItemStack abilitiesItem = createAbilitiesItem(skill, player);
        gui.setItem(22, abilitiesItem);
        
        // If this is a main skill with subskills, add subskills button
        if (skill.isMainSkill() && !skill.getSubskills().isEmpty()) {
            // Add general subskills button
            ItemStack subskillsItem = createSubskillsButton(skill);
            gui.setItem(20, subskillsItem); // Left of abilities
        } 
        // If this is a subskill, add detailed info button
        else if (skill.getParentSkill() != null) {
            // This is a subskill, add detailed info button
            ItemStack detailedInfoButton = createSubskillDetailedInfoButton(skill);
            gui.setItem(24, detailedInfoButton); // Right of abilities
        }
        
        // Add back button in bottom left
        ItemStack backButton = createBackButton();
        gui.setItem(27, backButton);
        
        // Add a help button in bottom right to explain GUI
        ItemStack helpButton = createHelpButton();
        gui.setItem(35, helpButton);
        
        // Fill remaining slots with glass panes
        fillEmptySlots(gui);
        
        // Open inventory
        player.openInventory(gui);
    }

    /**
     * Create a detailed info button for subskill details
     */
    private static ItemStack createSubskillDetailedInfoButton(Skill subskill) {
        Material icon;
        
        // Choose appropriate icon based on subskill type
        if (subskill instanceof OreExtractionSubskill) {
            icon = Material.IRON_PICKAXE;
        } 
        else if (subskill instanceof GemCarvingSubskill) {
            icon = Material.EMERALD;
        }
        else {
            // Default icons for other subskills based on parent skill
            switch (subskill.getParentSkill().getId()) {
                case "mining":
                    icon = Material.DIAMOND_PICKAXE;
                    break;
                case "excavating":
                    icon = Material.DIAMOND_SHOVEL;
                    break;
                case "fishing":
                    icon = Material.FISHING_ROD;
                    break;
                case "farming":
                    icon = Material.DIAMOND_HOE;
                    break;
                case "combat":
                    icon = Material.COMPASS;
                    break;
                default:
                    icon = Material.KNOWLEDGE_BOOK;
            }
        }
        
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "✦ " + ChatColor.AQUA + "Detailed " + subskill.getDisplayName() + " Info");
        
        // Add enchant glow for visual appeal
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + "View specialized information");
        lore.add(ChatColor.GRAY + "about the " + ChatColor.YELLOW + subskill.getDisplayName() + ChatColor.GRAY + " subskill.");
        lore.add("");
        
        // Add subskill-specific description
        if (subskill instanceof OreExtractionSubskill) {
            lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Details include:");
            lore.add(ChatColor.GRAY + "• Unlockable ore types");
            lore.add(ChatColor.GRAY + "• XP gained per ore");
            lore.add(ChatColor.GRAY + "• Mining efficiency stats");
            lore.add(ChatColor.GRAY + "• Fortune bonus information");
        }
        else if (subskill instanceof GemCarvingSubskill) {
            lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Details include:");
            lore.add(ChatColor.GRAY + "• Crystal rarities & qualities");
            lore.add(ChatColor.GRAY + "• Extraction success rates");
            lore.add(ChatColor.GRAY + "• XP values per gem type");
            lore.add(ChatColor.GRAY + "• Unlocking requirements");
        }
        else {
            lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Details include:");
            lore.add(ChatColor.GRAY + "• Special bonuses and stats");
            lore.add(ChatColor.GRAY + "• XP sources and values");
            lore.add(ChatColor.GRAY + "• Level-specific information");
        }
        
        lore.add("");
        lore.add(ChatColor.GREEN + "Click to view detailed information");
        
        // Add hidden subskill ID for event handler
        lore.add(ChatColor.BLACK + "VIEW_DETAILS:" + subskill.getId());
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create a help button with GUI explanation
     */
    private static ItemStack createHelpButton() {
        ItemStack helpButton = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = helpButton.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Help");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "This screen shows detailed");
        lore.add(ChatColor.GRAY + "information about this skill.");
        lore.add("");
        lore.add(ChatColor.AQUA + "» " + ChatColor.YELLOW + "Top: " + ChatColor.WHITE + "Skill overview");
        lore.add(ChatColor.AQUA + "» " + ChatColor.YELLOW + "Middle: " + ChatColor.WHITE + "Progress and bonuses");
        lore.add(ChatColor.AQUA + "» " + ChatColor.YELLOW + "Bottom: " + ChatColor.WHITE + "Related features");
        
        meta.setLore(lore);
        helpButton.setItemMeta(meta);
        return helpButton;
    }

    /**
     * Create subskills button with enhanced design
     */
    private static ItemStack createSubskillsButton(Skill skill) {
        ItemStack subskillsItem = new ItemStack(Material.BOOKSHELF);
        ItemMeta meta = subskillsItem.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "✦ View Subskills");
        
        // Add enchant glow
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Browse specialized skills within");
        lore.add(ChatColor.GRAY + "the " + ChatColor.YELLOW + skill.getDisplayName() + ChatColor.GRAY + " category.");
        lore.add("");
        
        // Add subskill list with color coding
        List<Skill> subskills = skill.getSubskills();
        lore.add(ChatColor.AQUA + "Available Subskills " + ChatColor.GRAY + "(" + subskills.size() + "):");
        for (Skill subskill : subskills) {
            lore.add(ChatColor.YELLOW + "• " + ChatColor.WHITE + subskill.getDisplayName());
        }
        
        lore.add("");
        lore.add(ChatColor.GREEN + "Click to view and manage subskills");
        
        meta.setLore(lore);
        subskillsItem.setItemMeta(meta);
        return subskillsItem;
    }
    
    /**
     * Create back button
     */
    private static ItemStack createBackButton() {
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta meta = backButton.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "« Back to Skills");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Return to the skills overview");
        meta.setLore(lore);
        
        backButton.setItemMeta(meta);
        return backButton;
    }
    
    /**
     * Create an item with skill information
     */
    private static ItemStack createSkillInfoItem(Skill skill, SkillLevel level) {
        Material icon = getSkillIcon(skill);
        
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name with enhanced formatting
        meta.setDisplayName(ChatColor.GOLD + "✦ " + skill.getDisplayName() + " " + 
                ChatColor.YELLOW + "[Level " + level.getLevel() + "/" + skill.getMaxLevel() + "]");
        
        // Add enchant glow if max level
        if (level.getLevel() >= skill.getMaxLevel()) {
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        // Create lore with dividers for better readability
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        // Format description with line breaks for better readability
        for (String line : skill.getDescription().split("\\.")) {
            if (!line.trim().isEmpty()) {
                lore.add(ChatColor.GRAY + line.trim() + ".");
            }
        }
        
        lore.add("");
        lore.add(ChatColor.YELLOW + "Total XP Earned: " + ChatColor.WHITE + 
                String.format("%,.1f", level.getTotalXp()));
        
        if (skill.isMainSkill()) {
            lore.add("");
            lore.add(ChatColor.AQUA + "» Subskill Categories:");
            
            // Add subskills with better formatting
            if (!skill.getSubskills().isEmpty()) {
                for (Skill subskill : skill.getSubskills()) {
                    lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + subskill.getDisplayName());
                }
            }
        } else {
            lore.add("");
            lore.add(ChatColor.AQUA + "» Parent Skill: " + ChatColor.YELLOW + skill.getParentSkill().getDisplayName());
        }
        
        // Add milestones with better formatting
        lore.add("");
        lore.add(ChatColor.AQUA + "» Milestone Levels:");
        List<Integer> milestones = skill.getMilestones();
        if (milestones.isEmpty()) {
            lore.add(ChatColor.GRAY + "None");
        } else {
            StringBuilder sb = new StringBuilder(ChatColor.GRAY.toString());
            for (int i = 0; i < milestones.size(); i++) {
                Integer milestone = milestones.get(i);
                // Color milestone based on player progress
                if (level.getLevel() >= milestone) {
                    sb.append(ChatColor.GREEN).append(milestone);
                } else {
                    sb.append(ChatColor.RED).append(milestone);
                }
                
                if (i < milestones.size() - 1) {
                    sb.append(ChatColor.GRAY).append(", ");
                }
            }
            lore.add(sb.toString());
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Get appropriate icon for a skill
     */
    private static Material getSkillIcon(Skill skill) {
        switch (skill.getId()) {
            case "mining":
                return Material.DIAMOND_PICKAXE;
            case "excavating":
                return Material.DIAMOND_SHOVEL;
            case "fishing":
                return Material.FISHING_ROD;
            case "farming":
                return Material.DIAMOND_HOE;
            case "combat":
                return Material.DIAMOND_SWORD;
            case "ore_extraction":
                return Material.IRON_ORE;
            case "gem_carving":
                return Material.DIAMOND;
            default:
                return Material.NETHER_STAR;
        }
    }
    
    /**
     * Create an item showing skill progress
     */
    private static ItemStack createProgressItem(Skill skill, SkillLevel level) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "✦ Skill Progress");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        // Make sure we're working with accurate data
        double currentXp = level.getCurrentXp();
        double totalXp = level.getTotalXp();
        int currentLevel = level.getLevel();
        
        if (currentLevel < skill.getMaxLevel()) {
            double xpForNextLevel = skill.getXpForLevel(currentLevel + 1);
            double progress = level.getProgressPercentage(xpForNextLevel);
            
            // Add level and XP information with better formatting
            lore.add(ChatColor.YELLOW + "Current Level: " + ChatColor.WHITE + currentLevel + 
                    ChatColor.GRAY + "/" + ChatColor.WHITE + skill.getMaxLevel());
            lore.add("");
            
            lore.add(ChatColor.YELLOW + "Progress to Level " + (currentLevel + 1) + ":");
            lore.add(createFancyProgressBar(progress));
            lore.add(ChatColor.WHITE + "XP: " + ChatColor.AQUA + String.format("%,.1f", currentXp) + 
                    ChatColor.GRAY + "/" + ChatColor.AQUA + String.format("%,.1f", xpForNextLevel) + 
                    ChatColor.GRAY + " (" + ChatColor.GREEN + String.format("%.1f", progress * 100) + "%" + 
                    ChatColor.GRAY + ")");
            
            lore.add("");
            lore.add(ChatColor.YELLOW + "XP Required to Max Level:");
            
            // Calculate total XP needed to reach max level
            double totalXpToMax = 0;
            for (int i = currentLevel + 1; i <= skill.getMaxLevel(); i++) {
                totalXpToMax += skill.getXpForLevel(i);
            }
            
            // Subtract already earned progress toward next level
            totalXpToMax -= currentXp;
            
            lore.add(ChatColor.AQUA + String.format("%,.1f", totalXpToMax) + ChatColor.GRAY + " XP remaining");
            
            // Add ETA estimate
            lore.add(ChatColor.GRAY + "Estimated time: " + ChatColor.YELLOW + "~" + 
                    calculateTimeEstimate(totalXpToMax));
        } else {
            // Max level reached
            lore.add(ChatColor.GOLD + "✦ " + ChatColor.GREEN + "MAXIMUM LEVEL REACHED!" + ChatColor.GOLD + " ✦");
            lore.add(ChatColor.YELLOW + "Current Level: " + ChatColor.GREEN + currentLevel + 
                    ChatColor.GRAY + "/" + ChatColor.GREEN + skill.getMaxLevel());
            lore.add("");
            lore.add(ChatColor.GRAY + "Total XP Earned: " + ChatColor.AQUA + String.format("%,.1f", totalXp));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Congratulations on mastering this skill!");
            lore.add(ChatColor.GRAY + "You've unlocked all skill benefits.");
        }
        
        // Add skill-specific bonuses
        addSpecialSkillBonuses(skill, level, lore);
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Add special bonuses for specific skill types
     */
    private static void addSpecialSkillBonuses(Skill skill, SkillLevel level, List<String> lore) {
        lore.add("");
        lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Current Skill Bonuses:");
        
        if (skill instanceof OreExtractionSubskill) {
            OreExtractionSubskill oreSkill = (OreExtractionSubskill) skill;
            addOreExtractionBonuses(oreSkill, level, lore);
        }
        else if (skill instanceof GemCarvingSubskill) {
            GemCarvingSubskill gemSkill = (GemCarvingSubskill) skill;
            addGemCarvingBonuses(gemSkill, level, lore);
        }
        else {
            // Generic bonuses for other skills
            lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "XP Gain: " + 
                    ChatColor.GREEN + "+" + (5 + (level.getLevel() / 10)) + "%");
            
            if (level.getLevel() >= 10) {
                lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Efficiency: " + 
                        ChatColor.GREEN + "+" + (level.getLevel() / 5) + "%");
            }
        }
    }
    
    /**
     * Add Ore Extraction specific bonuses
     */
    private static void addOreExtractionBonuses(OreExtractionSubskill skill, SkillLevel level, List<String> lore) {
        lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Mining Speed: " + 
                ChatColor.GREEN + String.format("%.2fx", skill.getMiningSpeedMultiplier(level.getLevel())));
        lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Mining Fortune: " + 
                ChatColor.GREEN + String.format("+%.1f", skill.getMiningFortuneBonus(level.getLevel())));
        lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Bonus Drops: " + 
                ChatColor.GREEN + String.format("+%.1f%%", skill.getBonusDropChance(level.getLevel()) * 100));
        lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Cave-in Risk: " + 
                getBetterOrWorse(skill.getCaveInChance(level.getLevel()), true) + 
                String.format("%.1f%%", skill.getCaveInChance(level.getLevel()) * 100));
    }
    
    /**
     * Add Gem Carving specific bonuses
     */
    private static void addGemCarvingBonuses(GemCarvingSubskill skill, SkillLevel level, List<String> lore) {
        lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Gem Find Rate: " + 
                ChatColor.GREEN + String.format("+%.1f%%", skill.getGemFindChance(level.getLevel()) * 100));
        lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Extraction Success: " + 
                ChatColor.GREEN + String.format("%.1f%%", skill.getExtractionSuccessChance(level.getLevel()) * 100));
        lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Gem Quality: " + 
                ChatColor.GREEN + String.format("%.2fx", skill.getGemQualityMultiplier(level.getLevel())));
    }
    
    /**
     * Get color code for better/worse stats
     */
    private static String getBetterOrWorse(double value, boolean lowerIsBetter) {
        if (lowerIsBetter) {
            return value <= 0.05 ? ChatColor.GREEN.toString() : ChatColor.RED.toString();
        } else {
            return value >= 1.5 ? ChatColor.GREEN.toString() : ChatColor.YELLOW.toString();
        }
    }
    
    /**
     * Calculate a rough time estimate for reaching max level
     * This is simplified and could be made more accurate with player XP gain rate tracking
     */
    private static String calculateTimeEstimate(double xpRemaining) {
        // Rough estimate assuming 100 XP per hour gameplay (adjust based on your actual rates)
        double hoursNeeded = xpRemaining / 100.0;
        
        if (hoursNeeded < 1) {
            return "Less than 1 hour";
        } else if (hoursNeeded < 24) {
            return String.format("%.1f hours", hoursNeeded);
        } else {
            return String.format("%.1f days", hoursNeeded / 24.0);
        }
    }
    
    /**
     * Create an item showing skill rewards
     */
    private static ItemStack createRewardsItem(Skill skill, SkillLevel level) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "✦ Skill Rewards");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        // Count rewards
        int levelsWithRewards = 0;
        int rewardsEarned = 0;
        
        for (int i = 1; i <= skill.getMaxLevel(); i++) {
            List<SkillReward> rewards = skill.getRewardsForLevel(i);
            if (!rewards.isEmpty()) {
                levelsWithRewards++;
                if (i <= level.getLevel()) {
                    rewardsEarned++;
                }
            }
        }
        
        // Progress indication
        double progress = levelsWithRewards > 0 ? (double)rewardsEarned / levelsWithRewards : 1.0;
        lore.add(ChatColor.YELLOW + "Rewards Unlocked:");
        lore.add(createFancyProgressBar(progress));
        lore.add("" + ChatColor.WHITE + rewardsEarned + ChatColor.GRAY + "/" + 
                ChatColor.WHITE + levelsWithRewards + 
                ChatColor.GRAY + " (" + ChatColor.GREEN + String.format("%.1f", progress * 100) + "%" + 
                ChatColor.GRAY + ")");
        
        lore.add("");
        
        // Recent rewards
        lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Recent Unlocks:");
        boolean hasEarnedRewards = false;
        
        for (int i = Math.max(1, level.getLevel() - 2); i <= level.getLevel() && i <= skill.getMaxLevel(); i++) {
            List<SkillReward> rewards = skill.getRewardsForLevel(i);
            if (!rewards.isEmpty()) {
                hasEarnedRewards = true;
                lore.add(ChatColor.AQUA + "Level " + i + ":");
                for (SkillReward reward : rewards) {
                    lore.add(ChatColor.GRAY + "• " + ChatColor.GREEN + reward.getDescription());
                }
            }
        }
        
        if (!hasEarnedRewards) {
            lore.add(ChatColor.GRAY + "No recent rewards");
        }
        
        lore.add("");
        
        // Coming rewards
        lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Upcoming Rewards:");
        boolean hasNextRewards = false;
        
        for (int i = level.getLevel() + 1; i <= Math.min(skill.getMaxLevel(), level.getLevel() + 3); i++) {
            List<SkillReward> rewards = skill.getRewardsForLevel(i);
            if (!rewards.isEmpty()) {
                hasNextRewards = true;
                lore.add(ChatColor.AQUA + "Level " + i + ":");
                for (SkillReward reward : rewards) {
                    lore.add(ChatColor.GRAY + "• " + ChatColor.RED + reward.getDescription());
                }
            }
        }
        
        if (!hasNextRewards) {
            if (level.getLevel() >= skill.getMaxLevel()) {
                lore.add(ChatColor.GREEN + "All rewards earned!");
            } else {
                lore.add(ChatColor.GRAY + "None in the next few levels");
            }
        }
        
        lore.add("");
        lore.add(ChatColor.GREEN + "Click to view all skill rewards");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create a button for accessing the skill tree
     */
    private static ItemStack createSkillTreeItem(Skill skill, PlayerProfile profile) {
        // Get skill tree data
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        int tokenCount = treeData.getTokenCount(skill.getId());
        
        // Get token display information
        SkillToken.TokenInfo tokenInfo = SkillToken.getTokenInfo(skill);
        
        // Create item
        ItemStack item = new ItemStack(tokenInfo.material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "✦ Skill Tree");
        
        // Add enchant glow if tokens available
        if (tokenCount > 0) {
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + "Unlock special abilities and");
        lore.add(ChatColor.GRAY + "bonuses for " + ChatColor.YELLOW + skill.getDisplayName());
        lore.add("");
        
        // Token display with animation if available
        if (tokenCount > 0) {
            lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Available Tokens:");
            lore.add(tokenInfo.color + "✦ " + tokenCount + " " + 
                tokenInfo.displayName + " Token" + (tokenCount != 1 ? "s" : "") + " ✦");
        } else {
            lore.add(ChatColor.YELLOW + "Available Tokens:");
            lore.add(ChatColor.GRAY + "None available");
        }
        
        lore.add("");
        lore.add(ChatColor.GREEN + "Click to access Skill Tree");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create a button for accessing abilities
     */
    private static ItemStack createAbilitiesItem(Skill skill, Player player) {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "✦ Skill Abilities");
        
        // Add ability counts
        AbilityRegistry registry = AbilityRegistry.getInstance();
        int unlockedPassive = registry.getUnlockedPassiveAbilities(player, skill.getId()).size();
        int totalPassive = registry.getPassiveAbilities(skill.getId()).size();
        int unlockedActive = registry.getUnlockedActiveAbilities(player, skill.getId()).size();
        int totalActive = registry.getActiveAbilities(skill.getId()).size();
        
        // Add enchant glow if any abilities unlocked
        if (unlockedPassive > 0 || unlockedActive > 0) {
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + "View and manage special abilities");
        lore.add(ChatColor.GRAY + "for your " + ChatColor.YELLOW + skill.getDisplayName() + ChatColor.GRAY + " skill.");
        lore.add("");
        
        lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Passive Abilities:");
        lore.add(createMiniProgressBar(unlockedPassive, totalPassive) + " " + 
                ChatColor.GREEN + unlockedPassive + "/" + totalPassive);
        
        lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Active Abilities:");
        lore.add(createMiniProgressBar(unlockedActive, totalActive) + " " + 
                ChatColor.GREEN + unlockedActive + "/" + totalActive);
        
        lore.add("");
        lore.add(ChatColor.GREEN + "Click to view and manage abilities");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Convert a list of integers to a list of strings
     */
    private static List<String> convertListToStrings(List<Integer> list) {
        List<String> strings = new ArrayList<>();
        for (Integer item : list) {
            strings.add(item.toString());
        }
        return strings;
    }
    
    /**
     * Create a fancy visual progress bar with gradients
     */
    private static String createFancyProgressBar(double progress) {
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
     * Create a detailed info button for subskills
     */
    private static ItemStack createSubskillDetailButton(Skill subskill) {
        Material icon;
        
        // Choose appropriate icon based on subskill type
        if (subskill instanceof OreExtractionSubskill) {
            icon = Material.IRON_PICKAXE;
        } 
        else if (subskill instanceof GemCarvingSubskill) {
            icon = Material.EMERALD;
        }
        else {
            icon = Material.COMPASS;
        }
        
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "✦ " + subskill.getDisplayName() + " Information");
        
        // Add enchant glow for visual appeal
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + "View specialized information about");
        lore.add(ChatColor.GRAY + "the " + ChatColor.YELLOW + subskill.getDisplayName() + ChatColor.GRAY + " subskill.");
        lore.add("");
        
        if (subskill instanceof OreExtractionSubskill) {
            lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Details include:");
            lore.add(ChatColor.GRAY + "• Unlocked ore types");
            lore.add(ChatColor.GRAY + "• XP values per ore");
            lore.add(ChatColor.GRAY + "• Fortune bonuses");
            lore.add(ChatColor.GRAY + "• Mining efficiency stats");
        }
        else if (subskill instanceof GemCarvingSubskill) {
            lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Details include:");
            lore.add(ChatColor.GRAY + "• Unlocked gem crystals");
            lore.add(ChatColor.GRAY + "• Extraction success rates");
            lore.add(ChatColor.GRAY + "• Crystal qualities & values");
            lore.add(ChatColor.GRAY + "• Carving precision stats");
        }
        
        lore.add("");
        lore.add(ChatColor.GREEN + "Click to view detailed information");
        
        // Add hidden subskill ID for event handler
        lore.add(ChatColor.BLACK + "SUBSKILL_ID:" + subskill.getId());
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create a simple progress bar for compact display
     */
    private static String createMiniProgressBar(int value, int max) {
        StringBuilder bar = new StringBuilder();
        int barLength = 10;
        int filledBars = max > 0 ? (int) Math.round((double) value / max * barLength) : 0;
        
        bar.append(ChatColor.GREEN);
        for (int i = 0; i < filledBars; i++) {
            bar.append("■");
        }
        
        bar.append(ChatColor.GRAY);
        for (int i = filledBars; i < barLength; i++) {
            bar.append("■");
        }
        
        return bar.toString();
    }
    
    /**
     * Fill empty slots with glass panes
     */
    private static void fillEmptySlots(Inventory gui) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }
    
    /**
     * Create decorative border for GUI
     */
    private static void createBorder(Inventory gui) {
        ItemStack blue = createGlassPane(Material.BLUE_STAINED_GLASS_PANE);
        ItemStack cyan = createGlassPane(Material.CYAN_STAINED_GLASS_PANE);
        ItemStack corner = createGlassPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        
        // Set corners
        gui.setItem(0, corner);
        gui.setItem(8, corner);
        gui.setItem(gui.getSize() - 9, corner);
        gui.setItem(gui.getSize() - 1, corner);
        
        // Top and bottom rows
        for (int i = 1; i < 8; i++) {
            gui.setItem(i, i % 2 == 0 ? blue : cyan);
            gui.setItem(gui.getSize() - 9 + i, i % 2 == 0 ? blue : cyan);
        }
        
        // Side borders
        for (int i = 1; i <= 2; i++) {
            gui.setItem(i * 9, i % 2 == 0 ? blue : cyan);
            gui.setItem(i * 9 + 8, i % 2 == 0 ? blue : cyan);
        }
    }
    
    /**
     * Create glass pane with empty name
     */
    private static ItemStack createGlassPane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }
}