package com.server.profiles.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.stats.PlayerStats;

public class StatsGUI {
    
    // Patterns for extracting stat values from lore - same as StatsUpdateManager
    private static final Pattern PHYSICAL_DAMAGE_PATTERN = Pattern.compile("Physical Damage: \\+(\\d+)");
    private static final Pattern MAGIC_DAMAGE_PATTERN = Pattern.compile("Magic Damage: \\+(\\d+)");
    private static final Pattern HEALTH_PATTERN = Pattern.compile("Health: \\+(\\d+)");
    private static final Pattern ARMOR_PATTERN = Pattern.compile("Armor: \\+(\\d+)");
    private static final Pattern MAGIC_RESIST_PATTERN = Pattern.compile("Magic Resist: \\+(\\d+)");
    
    public static void openStatsMenu(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) {
            player.sendMessage(ChatColor.RED + "You need to select a profile first!");
            return;
        }

        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;

        // Get current stats from the profile
        PlayerStats stats = profile.getStats();

        // Get base/default values for display purposes
        int basePDamage = stats.getDefaultPhysicalDamage();
        int baseMDamage = stats.getDefaultMagicDamage();
        int baseMana = stats.getDefaultMana();
        int baseHealth = stats.getDefaultHealth();
        int baseArmor = stats.getDefaultArmor();
        int baseMagicResist = stats.getDefaultMagicResist();
        double baseAttackRange = stats.getDefaultAttackRange();
        double baseSize = stats.getDefaultSize();
        double baseMiningSpeed = stats.getDefaultMiningSpeed();
        
        // Current values already include all equipment bonuses
        int currentPDamage = stats.getPhysicalDamage();
        int currentMDamage = stats.getMagicDamage();
        int currentTotalMana = stats.getTotalMana();
        int currentHealth = stats.getHealth();
        int currentArmor = stats.getArmor();
        int currentMagicResist = stats.getMagicResist();
        double currentAttackRange = stats.getAttackRange();
        double currentSize = stats.getSize();
        double currentMiningSpeed = stats.getMiningSpeed();
        
        // Calculate total bonuses (equipment + permanent)
        int totalPDamageBonus = currentPDamage - basePDamage;
        int totalMDamageBonus = currentMDamage - baseMDamage;
        int totalManaBonus = currentTotalMana - baseMana;
        int totalHealthBonus = currentHealth - baseHealth;
        int totalArmorBonus = currentArmor - baseArmor;
        int totalMagicResistBonus = currentMagicResist - baseMagicResist;
        double totalAttackRangeBonus = currentAttackRange - baseAttackRange;
        double totalSizeBonus = currentSize - baseSize;
        double totalMiningSpeedBonus = currentMiningSpeed - baseMiningSpeed;

        // Extract equipment info for display
        String helmetName = "None";
        String chestplateName = "None";
        String leggingsName = "None";
        String bootsName = "None";
        
        List<String> helmetStats = new ArrayList<>();
        List<String> chestplateStats = new ArrayList<>();
        List<String> leggingsStats = new ArrayList<>();
        List<String> bootsStats = new ArrayList<>();
        List<String> mainHandStats = new ArrayList<>();
        
        // Extract equipment info
        extractEquipmentStats(player, helmetStats, chestplateStats, leggingsStats, bootsStats, mainHandStats);
        
        // Extract item names
        helmetName = getItemDisplayName(player.getInventory().getHelmet());
        chestplateName = getItemDisplayName(player.getInventory().getChestplate());
        leggingsName = getItemDisplayName(player.getInventory().getLeggings());
        bootsName = getItemDisplayName(player.getInventory().getBoots());
        String mainHandName = getItemDisplayName(player.getInventory().getItemInMainHand());

        // Create the main GUI with fancier design - 54 slots (6 rows)
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.GOLD + "✦ " + ChatColor.GREEN + player.getName() + "'s Stats " + ChatColor.GOLD + "✦");

        // Create player head info item
        ItemStack playerHead = createPlayerHeadItem(player, profile);
        gui.setItem(4, playerHead);
        
        // Combat Stats (Diamond Sword) - Top row
        ItemStack combatItem = createStatsItem(Material.DIAMOND_SWORD, 
            ChatColor.RED + "Combat Stats",
            new String[] {
                ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                ChatColor.GRAY + "Physical Damage: " + ChatColor.WHITE + currentPDamage + 
                    (totalPDamageBonus > 0 ? ChatColor.GREEN + " (+" + totalPDamageBonus + ")" : ""),
                ChatColor.GRAY + "Magic Damage: " + ChatColor.WHITE + currentMDamage + 
                    (totalMDamageBonus > 0 ? ChatColor.GREEN + " (+" + totalMDamageBonus + ")" : ""),
                ChatColor.GRAY + "Attack Range: " + ChatColor.WHITE + String.format("%.1f", currentAttackRange) + " blocks" +
                    (totalAttackRangeBonus > 0 ? ChatColor.GREEN + " (+" + String.format("%.1f", totalAttackRangeBonus) + ")" : ""),
                ChatColor.GRAY + "Ranged Damage: " + ChatColor.WHITE + stats.getRangedDamage(),
                "",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Critical Stats:",
                ChatColor.GRAY + "Critical Chance: " + ChatColor.WHITE + String.format("%.1f%%", stats.getCriticalChance() * 100),
                ChatColor.GRAY + "Critical Damage: " + ChatColor.WHITE + stats.getCriticalDamage() + "x",
                "",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Burst Stats:",
                ChatColor.GRAY + "Burst Chance: " + ChatColor.WHITE + String.format("%.1f%%", stats.getBurstChance() * 100),
                ChatColor.GRAY + "Burst Damage: " + ChatColor.WHITE + stats.getBurstDamage() + "x",
                "",
                ChatColor.GRAY + "Attack Speed: " + ChatColor.WHITE + stats.getAttackSpeed() + " hits/sec"
            },
            true
        );
        gui.setItem(10, combatItem);

        // Defense Stats (Diamond Chestplate)
        ItemStack defenseItem = createStatsItem(Material.DIAMOND_CHESTPLATE,
            ChatColor.BLUE + "Defense Stats",
            new String[] {
                ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                ChatColor.GRAY + "Health: " + ChatColor.WHITE + currentHealth + 
                    (totalHealthBonus > 0 ? ChatColor.GREEN + " (+" + totalHealthBonus + ")" : ""),
                ChatColor.GRAY + "Armor: " + ChatColor.WHITE + currentArmor + 
                    (totalArmorBonus > 0 ? ChatColor.GREEN + " (+" + totalArmorBonus + ")" : "") +
                    ChatColor.GRAY + " [" + String.format("%.1f", stats.getPhysicalDamageReduction()) + "% reduction]",                
                ChatColor.GRAY + "Magic Resist: " + ChatColor.WHITE + currentMagicResist + 
                    (totalMagicResistBonus > 0 ? ChatColor.GREEN + " (+" + totalMagicResistBonus + ")" : "") +
                    ChatColor.GRAY + " [" + String.format("%.1f", stats.getMagicDamageReduction()) + "% reduction]",
                "",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Sustain:",
                ChatColor.GRAY + "Life Steal: " + ChatColor.WHITE + stats.getLifeSteal() + "%",
                ChatColor.GRAY + "Omnivamp: " + ChatColor.WHITE + stats.getOmnivamp() + "%",
                ChatColor.GRAY + "Health Regen: " + ChatColor.WHITE + String.format("%.1f", stats.getHealthRegen()) + "/sec",
                "",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Other:",
                ChatColor.GRAY + "Size: " + ChatColor.WHITE + String.format("%.2f", currentSize) + "x" +
                    (totalSizeBonus > 0 ? ChatColor.GREEN + " (+" + String.format("%.2f", totalSizeBonus) + ")" : "")
            },
            true
        );
        gui.setItem(12, defenseItem);

        // Resource Stats (Experience Bottle)
        ItemStack resourceItem = createStatsItem(Material.EXPERIENCE_BOTTLE,
            ChatColor.GREEN + "Resource Stats",
            new String[] {
                ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                ChatColor.GRAY + "Mana: " + ChatColor.AQUA + stats.getMana() + "/" + currentTotalMana,
                ChatColor.GRAY + "Base Mana: " + ChatColor.WHITE + baseMana + 
                    (totalManaBonus > 0 ? ChatColor.GREEN + " (+" + totalManaBonus + ")" : ""),
                ChatColor.GRAY + "Mana Regen: " + ChatColor.WHITE + stats.getManaRegen() + "/sec",
                "",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Utility:",
                ChatColor.GRAY + "Cooldown Reduction: " + ChatColor.WHITE + stats.getCooldownReduction() + "%",
                ChatColor.GRAY + "Movement Speed: " + ChatColor.WHITE + String.format("%.2f", stats.getSpeed()) + "x",
                "",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Experience:",
                ChatColor.GRAY + "Level: " + ChatColor.YELLOW + player.getLevel(),
                ChatColor.GRAY + "XP Progress: " + ChatColor.YELLOW + String.format("%.1f%%", player.getExp() * 100)
            },
            true
        );
        gui.setItem(14, resourceItem);

        // Fortune Stats (Gold Ingot)
        ItemStack fortuneItem = createStatsItem(Material.GOLD_INGOT,
            ChatColor.GOLD + "Fortune Stats",
            new String[] {
                ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Fortune Multipliers:",
                ChatColor.GRAY + "Mining: " + ChatColor.WHITE + String.format("%.2fx", stats.getMiningFortune()),
                ChatColor.GRAY + "Current Effect: " + ChatColor.WHITE + getFortuneDescription(stats.getMiningFortune(), "ore"),
                "",
                ChatColor.GRAY + "Farming: " + ChatColor.WHITE + String.format("%.2fx", stats.getFarmingFortune()),
                ChatColor.GRAY + "Current Effect: " + ChatColor.WHITE + getFortuneDescription(stats.getFarmingFortune(), "crop"),
                "",
                ChatColor.GRAY + "Looting: " + ChatColor.WHITE + String.format("%.2fx", stats.getLootingFortune()),
                ChatColor.GRAY + "Current Effect: " + ChatColor.WHITE + getFortuneDescription(stats.getLootingFortune(), "mob drop"),
                "",
                ChatColor.GRAY + "Fishing: " + ChatColor.WHITE + String.format("%.2fx", stats.getFishingFortune()),
                ChatColor.GRAY + "Current Effect: " + ChatColor.WHITE + getFortuneDescription(stats.getFishingFortune(), "fish"),
                "",
                ChatColor.GRAY + "Luck: " + ChatColor.YELLOW + "+" + stats.getLuck() + " points"
            },
            true
        );
        gui.setItem(16, fortuneItem);
        
        // Second row of specialized stats
        
        // Mining Stats
        ItemStack miningStatsItem = createStatsItem(Material.DIAMOND_PICKAXE,
            ChatColor.AQUA + "Mining Stats",
            new String[] {
                ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Speed Stats:",
                ChatColor.GRAY + "Mining Speed: " + ChatColor.WHITE + String.format("%.2fx", currentMiningSpeed) +
                    (totalMiningSpeedBonus > 0 ? ChatColor.GREEN + " (+" + String.format("%.2f", totalMiningSpeedBonus) + "x)" : ""),
                "",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Fortune Stats:",
                ChatColor.GRAY + "Mining Fortune: " + ChatColor.WHITE + String.format("%.2fx", stats.getMiningFortune()),
                ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + "Regular Blocks: " + 
                    ChatColor.WHITE + getFortuneDescription(stats.getMiningFortune(), "drop"),
                ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + "Rare Ores: " + 
                    ChatColor.WHITE + getFortuneDescription(stats.getMiningFortune() * 0.75, "drop"),
                "",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Special Bonuses:",
                ChatColor.GRAY + "Gem Find Rate: " + ChatColor.WHITE + calculateGemFindRate(player, stats) + "%",
                ChatColor.GRAY + "Excavation Efficiency: " + ChatColor.WHITE + calculateExcavationRate(player, stats) + "%"
            },
            false
        );
        gui.setItem(28, miningStatsItem);
        
        // Combat Stats - More detailed
        ItemStack detailedCombatItem = createStatsItem(Material.DIAMOND_AXE,
            ChatColor.RED + "Detailed Combat Stats",
            new String[] {
                ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "DPS Calculation:",
                ChatColor.GRAY + "Base DPS: " + ChatColor.WHITE + 
                    calculateBaseDPS(stats),
                ChatColor.GRAY + "Critical DPS: " + ChatColor.WHITE + 
                    calculateCriticalDPS(stats),
                ChatColor.GRAY + "With Burst: " + ChatColor.WHITE + 
                    calculateBurstDPS(stats),
                "",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Effective Stats:",
                ChatColor.GRAY + "Effective Health: " + ChatColor.WHITE + calculateEffectiveHealth(stats),
                ChatColor.GRAY + "vs Physical: " + ChatColor.WHITE + calculateEffectivePhysical(stats),
                ChatColor.GRAY + "vs Magic: " + ChatColor.WHITE + calculateEffectiveMagic(stats),
                "",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Sustain Per Second:",
                ChatColor.GRAY + "From Life Steal: " + ChatColor.WHITE + String.format("%.1f", calculateLifeStealPerSecond(stats)),
                ChatColor.GRAY + "From Regen: " + ChatColor.WHITE + String.format("%.1f", stats.getHealthRegen()),
                "",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Elemental Affinity:",
                getAffinityLine(stats, com.server.enchantments.elements.ElementType.FIRE),
                getAffinityLine(stats, com.server.enchantments.elements.ElementType.WATER),
                getAffinityLine(stats, com.server.enchantments.elements.ElementType.EARTH),
                getAffinityLine(stats, com.server.enchantments.elements.ElementType.AIR),
                getAffinityLine(stats, com.server.enchantments.elements.ElementType.NATURE),
                getAffinityLine(stats, com.server.enchantments.elements.ElementType.LIGHTNING),
                getAffinityLine(stats, com.server.enchantments.elements.ElementType.SHADOW),
                getAffinityLine(stats, com.server.enchantments.elements.ElementType.LIGHT)
            },
            false
        );
        gui.setItem(30, detailedCombatItem);
        
        // Farming Stats
        ItemStack farmingStatsItem = createStatsItem(Material.DIAMOND_HOE,
            ChatColor.GREEN + "Farming Stats",
            new String[] {
                ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Fortune Stats:",
                ChatColor.GRAY + "Farming Fortune: " + ChatColor.WHITE + String.format("%.2fx", stats.getFarmingFortune()),
                ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + "Regular Crops: " + 
                    ChatColor.WHITE + getFortuneDescription(stats.getFarmingFortune(), "drop"),
                ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + "Special Crops: " + 
                    ChatColor.WHITE + getFortuneDescription(stats.getFarmingFortune() * 0.8, "drop"),
                "",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Special Bonuses:",
                ChatColor.GRAY + "Growth Speed: " + ChatColor.WHITE + calculateGrowthSpeed(player, stats) + "%",
                ChatColor.GRAY + "Special Find: " + ChatColor.WHITE + calculateSpecialFind(player, stats) + "%",
                "",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Tool Efficiency:",
                ChatColor.GRAY + "Hoe Efficiency: " + ChatColor.WHITE + calculateHoeEfficiency(player, stats) + "%",
                ChatColor.GRAY + "Soil Quality: " + ChatColor.WHITE + calculateSoilQuality(player, stats) + "%"
            },
            false
        );
        gui.setItem(32, farmingStatsItem);
        
        // Fishing Stats
        ItemStack fishingStatsItem = createStatsItem(Material.FISHING_ROD,
            ChatColor.BLUE + "Fishing Stats",
            new String[] {
                ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Fortune Stats:",
                ChatColor.GRAY + "Fishing Fortune: " + ChatColor.WHITE + String.format("%.2fx", stats.getFishingFortune()),
                ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + "Regular Fish: " + 
                    ChatColor.WHITE + getFortuneDescription(stats.getFishingFortune(), "catch"),
                ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + "Rare Fish: " + 
                    ChatColor.WHITE + getFortuneDescription(stats.getFishingFortune() * 0.7, "catch"),
                "",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Special Bonuses:",
                ChatColor.GRAY + "Treasure Chance: " + ChatColor.WHITE + calculateTreasureChance(player, stats) + "%",
                ChatColor.GRAY + "Double Catch: " + ChatColor.WHITE + calculateDoubleCatch(player, stats) + "%",
                "",
                ChatColor.AQUA + "» " + ChatColor.YELLOW + "Fishing Speed:",
                ChatColor.GRAY + "Bite Speed: " + ChatColor.WHITE + calculateBiteSpeed(player, stats) + "%",
                ChatColor.GRAY + "Lure Power: " + ChatColor.WHITE + calculateLurePower(player, stats) + "%"
            },
            false
        );
        gui.setItem(34, fishingStatsItem);

        // Equipment summary (third row)
        ItemStack equippedItem = createEquipmentItem(
            helmetName, chestplateName, leggingsName, bootsName, mainHandName,
            helmetStats, chestplateStats, leggingsStats, bootsStats, mainHandStats
        );
        gui.setItem(40, equippedItem);

        // Fill empty slots with decorative glass panes
        fillBorder(gui);

        player.openInventory(gui);
    }

    /**
     * Extract item stats from equipped gear for display
     */
    private static void extractEquipmentStats(Player player, 
                                             List<String> helmetStats, List<String> chestplateStats,
                                             List<String> leggingsStats, List<String> bootsStats,
                                             List<String> mainHandStats) {
        // Extract helmet stats
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet != null && helmet.hasItemMeta() && helmet.getItemMeta().hasLore()) {
            extractItemStatsForDisplay(helmet, helmetStats);
        }
        
        // Extract chestplate stats
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate != null && chestplate.hasItemMeta() && chestplate.getItemMeta().hasLore()) {
            extractItemStatsForDisplay(chestplate, chestplateStats);
        }
        
        // Extract leggings stats
        ItemStack leggings = player.getInventory().getLeggings();
        if (leggings != null && leggings.hasItemMeta() && leggings.getItemMeta().hasLore()) {
            extractItemStatsForDisplay(leggings, leggingsStats);
        }
        
        // Extract boots stats
        ItemStack boots = player.getInventory().getBoots();
        if (boots != null && boots.hasItemMeta() && boots.getItemMeta().hasLore()) {
            extractItemStatsForDisplay(boots, bootsStats);
        }
        
        // Extract main hand stats
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && mainHand.getType() != Material.AIR && 
            mainHand.hasItemMeta() && mainHand.getItemMeta().hasLore()) {
            extractItemStatsForDisplay(mainHand, mainHandStats);
        }
    }

    /**
     * Extract item stats for display purposes only
     */
    private static void extractItemStatsForDisplay(ItemStack item, List<String> statsList) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return;
        
        for (String loreLine : item.getItemMeta().getLore()) {
            // Strip color codes for comparison
            String cleanLine = ChatColor.stripColor(loreLine);
            
            // Only add lines that contain stat information
            if (cleanLine.contains("Health:") || 
                cleanLine.contains("Armor:") || 
                cleanLine.contains("Magic Resist:") ||
                cleanLine.contains("Physical Damage:") ||
                cleanLine.contains("Magic Damage:") ||
                cleanLine.contains("Mana:") ||
                cleanLine.contains("Attack Range:") ||
                cleanLine.contains("Size:") ||
                cleanLine.contains("Life Steal:") ||
                cleanLine.contains("Critical Chance:") ||
                cleanLine.contains("Critical Damage:") ||
                cleanLine.contains("Attack Speed:") ||
                cleanLine.contains("Mining Fortune:") ||
                cleanLine.contains("Farming Fortune:") ||
                cleanLine.contains("Mining Speed:") ||
                cleanLine.contains("Luck:")) {
                
                statsList.add(loreLine);
            }
        }
    }

    /**
     * Get the display name of an item
     */
    private static String getItemDisplayName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return "None";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return formatMaterialName(item.getType().name());
    }

    /**
     * Format material name for display
     */
    private static String formatMaterialName(String materialName) {
        // Convert DIAMOND_HELMET to Diamond Helmet
        String[] words = materialName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
            }
        }
        
        return result.toString().trim();
    }

    /**
     * Create a stats display item with enhanced visual design
     */
    private static ItemStack createStatsItem(Material material, String name, String[] lore, boolean enchanted) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        
        List<String> loreList = new ArrayList<>();
        loreList.addAll(Arrays.asList(lore));
        
        meta.setLore(loreList);
        
        // Add enchanted glow if requested
        if (enchanted) {
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        // Hide attributes to keep the tooltip clean
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create a player head item with player info
     */
    private static ItemStack createPlayerHeadItem(Player player, PlayerProfile profile) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(ChatColor.GOLD + "✦ " + ChatColor.AQUA + player.getName() + "'s Profile" + ChatColor.GOLD + " ✦");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + "View detailed stats and");
        lore.add(ChatColor.GRAY + "equipment bonuses below");
        lore.add("");
        lore.add(ChatColor.YELLOW + "» " + ChatColor.WHITE + "Profile: " + ChatColor.GOLD + profile.getName());
        lore.add(ChatColor.YELLOW + "» " + ChatColor.WHITE + "Class: " + ChatColor.GOLD + getPlayerClass(profile));
        lore.add("");
        lore.add(ChatColor.GOLD + "✧ " + ChatColor.YELLOW + "Scroll down to see detailed");
        lore.add(ChatColor.YELLOW + "skill-specific stats!");
        
        meta.setLore(lore);
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        head.setItemMeta(meta);
        return head;
    }

    /**
     * Create an equipment summary item
     */
    private static ItemStack createEquipmentItem(
        String helmetName, String chestplateName, String leggingsName, String bootsName, String mainHandName,
        List<String> helmetStats, List<String> chestplateStats, List<String> leggingsStats, 
        List<String> bootsStats, List<String> mainHandStats) {
        
        ItemStack item = new ItemStack(Material.ARMOR_STAND);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Equipped Items");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        // Helmet
        lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Helmet:");
        lore.add(ChatColor.GRAY + helmetName);
        if (!helmetStats.isEmpty()) {
            for (String stat : helmetStats) {
                lore.add(ChatColor.DARK_GRAY + "• " + stat);
            }
        }
        lore.add("");
        
        // Chestplate
        lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Chestplate:");
        lore.add(ChatColor.GRAY + chestplateName);
        if (!chestplateStats.isEmpty()) {
            for (String stat : chestplateStats) {
                lore.add(ChatColor.DARK_GRAY + "• " + stat);
            }
        }
        lore.add("");
        
        // Leggings
        lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Leggings:");
        lore.add(ChatColor.GRAY + leggingsName);
        if (!leggingsStats.isEmpty()) {
            for (String stat : leggingsStats) {
                lore.add(ChatColor.DARK_GRAY + "• " + stat);
            }
        }
        lore.add("");
        
        // Boots
        lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Boots:");
        lore.add(ChatColor.GRAY + bootsName);
        if (!bootsStats.isEmpty()) {
            for (String stat : bootsStats) {
                lore.add(ChatColor.DARK_GRAY + "• " + stat);
            }
        }
        
        // Main hand (if it has stats)
        if (!mainHandStats.isEmpty()) {
            lore.add("");
            lore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Main Hand:");
            lore.add(ChatColor.GRAY + mainHandName);
            for (String stat : mainHandStats) {
                lore.add(ChatColor.DARK_GRAY + "• " + stat);
            }
        }
        
        meta.setLore(lore);
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        
        return item;
    }

    /**
     * Create a decorative glass pane border around the GUI
     */
    private static void fillBorder(Inventory gui) {
        ItemStack bluePane = createGlassPane(Material.BLUE_STAINED_GLASS_PANE);
        ItemStack purplePane = createGlassPane(Material.PURPLE_STAINED_GLASS_PANE);
        ItemStack cyanPane = createGlassPane(Material.CYAN_STAINED_GLASS_PANE);
        ItemStack blackPane = createGlassPane(Material.BLACK_STAINED_GLASS_PANE);
        
        // Top and bottom borders
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, i % 2 == 0 ? bluePane : purplePane);
            gui.setItem(45 + i, i % 2 == 0 ? purplePane : bluePane);
        }
        
        // Side borders
        for (int i = 1; i < 5; i++) {
            gui.setItem(i * 9, cyanPane);
            gui.setItem(i * 9 + 8, cyanPane);
        }
        
        // Corner enhancements
        gui.setItem(0, cyanPane);
        gui.setItem(8, cyanPane);
        gui.setItem(45, cyanPane);
        gui.setItem(53, cyanPane);
        
        // Fill remaining empty slots
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, blackPane);
            }
        }
    }

    /**
     * Create a glass pane with empty name for decoration
     */
    private static ItemStack createGlassPane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }
    
    /**
     * Get a readable description of fortune effects
     */
    private static String getFortuneDescription(double fortune, String type) {
        // Calculate guaranteed multiplier (whole number part)
        int guaranteedMultiplier = (int) Math.floor(fortune);
        
        // Calculate chance for an extra drop (decimal part)
        double chanceForNext = (fortune - guaranteedMultiplier) * 100.0;
        
        // Format the description based on the guaranteed multiplier
        if (guaranteedMultiplier == 0) {
            return String.format("%.1f%% chance for 2x %s", chanceForNext, type);
        } else if (guaranteedMultiplier == 1) {
            return String.format("2x %s + %.1f%% chance for 3x", type, chanceForNext);
        } else {
            return String.format("%dx %s + %.1f%% chance for %dx", 
                guaranteedMultiplier + 1, type, chanceForNext, guaranteedMultiplier + 2);
        }
    }
    
    /**
     * Calculate DPS stats for display
     */
    private static String calculateBaseDPS(PlayerStats stats) {
        // Basic calculation: damage × attack speed
        double dps = stats.getPhysicalDamage() * stats.getAttackSpeed();
        return String.format("%.1f", dps);
    }
    
    private static String calculateCriticalDPS(PlayerStats stats) {
        // Account for critical hit chance and damage
        double baseDmg = stats.getPhysicalDamage();
        double critChance = stats.getCriticalChance();
        double critDmg = stats.getCriticalDamage();
        double attackSpeed = stats.getAttackSpeed();
        
        double avgDmgPerHit = baseDmg * (1 - critChance) + baseDmg * critDmg * critChance;
        double dps = avgDmgPerHit * attackSpeed;
        
        return String.format("%.1f", dps);
    }
    
    private static String calculateBurstDPS(PlayerStats stats) {
        // Account for critical hits, burst chance and damage
        double baseDmg = stats.getPhysicalDamage();
        double critChance = stats.getCriticalChance();
        double critDmg = stats.getCriticalDamage();
        double burstChance = stats.getBurstChance();
        double burstDmg = stats.getBurstDamage();
        double attackSpeed = stats.getAttackSpeed();
        
        // Calculate average damage considering crits and bursts
        double regularHitDmg = baseDmg * (1 - critChance);
        double critHitDmg = baseDmg * critDmg * critChance;
        
        double avgBaseHitDmg = regularHitDmg + critHitDmg;
        double avgBurstHitDmg = avgBaseHitDmg * burstDmg;
        
        double avgDmgPerHit = avgBaseHitDmg * (1 - burstChance) + avgBurstHitDmg * burstChance;
        double dps = avgDmgPerHit * attackSpeed;
        
        return String.format("%.1f", dps);
    }
    
    /**
     * Calculate effective health against damage types
     */
    private static String calculateEffectiveHealth(PlayerStats stats) {
        return String.format("%d", stats.getHealth());
    }
    
    private static String calculateEffectivePhysical(PlayerStats stats) {
        // Effective health = health / (1 - damage reduction percentage)
        double damageReduction = stats.getPhysicalDamageReduction() / 100.0;
        double effectiveHealth = stats.getHealth() / (1.0 - damageReduction);
        return String.format("%.0f", effectiveHealth);
    }
    
    private static String calculateEffectiveMagic(PlayerStats stats) {
        // Effective health = health / (1 - damage reduction percentage)
        double damageReduction = stats.getMagicDamageReduction() / 100.0;
        double effectiveHealth = stats.getHealth() / (1.0 - damageReduction);
        return String.format("%.0f", effectiveHealth);
    }
    
    /**
     * Calculate life steal healing per second
     */
    private static double calculateLifeStealPerSecond(PlayerStats stats) {
        // Damage per second × life steal percentage
        double dps = stats.getPhysicalDamage() * stats.getAttackSpeed();
        return dps * (stats.getLifeSteal() / 100.0);
    }
    
    /**
     * Mock calculations for skill-specific bonuses
     * In a real implementation, these would pull from the player's actual skill levels
     */
    private static String calculateGemFindRate(Player player, PlayerStats stats) {
        // Mock calculation based on mining fortune and luck
        return String.format("%.1f", 2.0 + stats.getMiningFortune() * 2 + stats.getLuck() * 0.5);
    }
    
    private static String calculateExcavationRate(Player player, PlayerStats stats) {
        // Mock calculation
        return String.format("%.1f", 100.0 + stats.getMiningSpeed() * 10);
    }
    
    private static String calculateGrowthSpeed(Player player, PlayerStats stats) {
        // Mock calculation
        return String.format("%.1f", 100.0 + stats.getFarmingFortune() * 5);
    }
    
    private static String calculateSpecialFind(Player player, PlayerStats stats) {
        // Mock calculation
        return String.format("%.1f", stats.getFarmingFortune() * 3 + stats.getLuck() * 0.7);
    }
    
    private static String calculateHoeEfficiency(Player player, PlayerStats stats) {
        // Mock calculation
        return String.format("%.1f", 100.0 + stats.getMiningSpeed() * 8);
    }
    
    private static String calculateSoilQuality(Player player, PlayerStats stats) {
        // Mock calculation
        return String.format("%.1f", 100.0 + stats.getFarmingFortune() * 7);
    }
    
    private static String calculateTreasureChance(Player player, PlayerStats stats) {
        // Mock calculation
        return String.format("%.1f", 5.0 + stats.getFishingFortune() * 2 + stats.getLuck() * 0.8);
    }
    
    private static String calculateDoubleCatch(Player player, PlayerStats stats) {
        // Mock calculation
        return String.format("%.1f", stats.getFishingFortune() * 4);
    }
    
    private static String calculateBiteSpeed(Player player, PlayerStats stats) {
        // Mock calculation
        return String.format("%.1f", 100.0 + stats.getAttackSpeed() * 30);
    }
    
    private static String calculateLurePower(Player player, PlayerStats stats) {
        // Mock calculation
        return String.format("%.1f", 100.0 + stats.getFishingFortune() * 8);
    }
    
    /**
     * Get formatted affinity line for an element
     */
    private static String getAffinityLine(PlayerStats stats, com.server.enchantments.elements.ElementType element) {
        com.server.profiles.stats.ElementalAffinity affinity = stats.getElementalAffinity();
        double affinityValue = affinity.getAffinity(element);
        String tier = affinity.getAffinityTier(element);
        
        // Format: Icon Element: Value [Tier]
        return ChatColor.DARK_GRAY + "• " + 
               element.getColoredIcon() + " " + 
               element.getColor() + element.name() + ": " + 
               ChatColor.WHITE + String.format("%.0f", affinityValue) + " " +
               tier;
    }
    
    /**
     * Get the player's class (mock method - would pull from the actual class system)
     */
    private static String getPlayerClass(PlayerProfile profile) {
        // Example implementation - in a real plugin this would pull from the player's actual class
        return "Adventurer"; // Placeholder value
    }
}