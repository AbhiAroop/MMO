package com.server.profiles.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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
        double totalMiningSpeedBonus = currentMiningSpeed - baseMiningSpeed;

        // Create the main GUI - 54 slots (6 rows)
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "⚔ " + ChatColor.GOLD + player.getName() + "'s Stats " + ChatColor.DARK_GRAY + "⚔");
        
        // Get current attack speed from attribute
        double currentAttackSpeed = stats.getAttackSpeed();
        AttributeInstance attackSpeedAttr = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (attackSpeedAttr != null) {
            currentAttackSpeed = attackSpeedAttr.getValue();
        }
        
        // Get current movement speed from attribute
        double currentMovementSpeed = stats.getSpeed();
        AttributeInstance movementSpeedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (movementSpeedAttr != null) {
            currentMovementSpeed = movementSpeedAttr.getValue();
        }
        
        // === ROW 1: Core Combat Stats ===
        
        // Combat Stats (Diamond Sword)
        ItemStack combatItem = createStatsItem(Material.DIAMOND_SWORD, 
            ChatColor.RED + "⚔ Combat Stats",
            new String[] {
                ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                ChatColor.GRAY + "Physical Damage: " + ChatColor.WHITE + currentPDamage + 
                    (totalPDamageBonus > 0 ? ChatColor.GREEN + " (+" + totalPDamageBonus + ")" : ""),
                ChatColor.GRAY + "Magic Damage: " + ChatColor.WHITE + currentMDamage + 
                    (totalMDamageBonus > 0 ? ChatColor.GREEN + " (+" + totalMDamageBonus + ")" : ""),
                ChatColor.GRAY + "Ranged Damage: " + ChatColor.WHITE + stats.getRangedDamage(),
                ChatColor.GRAY + "Attack Speed: " + ChatColor.WHITE + String.format("%.2f", currentAttackSpeed) + "/s",
                ChatColor.GRAY + "Attack Range: " + ChatColor.WHITE + String.format("%.1f", currentAttackRange) + " blocks",
                "",
                ChatColor.YELLOW + "Critical:",
                ChatColor.GRAY + "  Chance: " + ChatColor.WHITE + String.format("%.1f%%", stats.getCriticalChance() * 100),
                ChatColor.GRAY + "  Damage: " + ChatColor.WHITE + String.format("%.1fx", stats.getCriticalDamage()),
                "",
                ChatColor.YELLOW + "Burst:",
                ChatColor.GRAY + "  Chance: " + ChatColor.WHITE + String.format("%.1f%%", stats.getBurstChance() * 100),
                ChatColor.GRAY + "  Damage: " + ChatColor.WHITE + String.format("%.1fx", stats.getBurstDamage())
            },
            true
        );
        gui.setItem(10, combatItem);

        // Defense Stats (Diamond Chestplate)
        ItemStack defenseItem = createStatsItem(Material.DIAMOND_CHESTPLATE,
            ChatColor.BLUE + "🛡 Defense Stats",
            new String[] {
                ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                ChatColor.GRAY + "Health: " + ChatColor.WHITE + currentHealth + 
                    (totalHealthBonus > 0 ? ChatColor.GREEN + " (+" + totalHealthBonus + ")" : ""),
                ChatColor.GRAY + "Armor: " + ChatColor.WHITE + currentArmor + 
                    (totalArmorBonus > 0 ? ChatColor.GREEN + " (+" + totalArmorBonus + ")" : ""),
                ChatColor.GRAY + "  Reduction: " + ChatColor.WHITE + String.format("%.1f%%", stats.getPhysicalDamageReduction()),
                ChatColor.GRAY + "Magic Resist: " + ChatColor.WHITE + currentMagicResist + 
                    (totalMagicResistBonus > 0 ? ChatColor.GREEN + " (+" + totalMagicResistBonus + ")" : ""),
                ChatColor.GRAY + "  Reduction: " + ChatColor.WHITE + String.format("%.1f%%", stats.getMagicDamageReduction()),
                "",
                ChatColor.YELLOW + "Sustain:",
                ChatColor.GRAY + "  Life Steal: " + ChatColor.WHITE + String.format("%.1f%%", stats.getLifeSteal()),
                ChatColor.GRAY + "  Omnivamp: " + ChatColor.WHITE + String.format("%.1f%%", stats.getOmnivamp()),
                ChatColor.GRAY + "  Health Regen: " + ChatColor.WHITE + String.format("%.1f", stats.getHealthRegen()) + "/s"
            },
            true
        );
        gui.setItem(12, defenseItem);

        // Resource Stats (Experience Bottle)
        ItemStack resourceItem = createStatsItem(Material.EXPERIENCE_BOTTLE,
            ChatColor.AQUA + "✦ Utility Stats",
            new String[] {
                ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                ChatColor.GRAY + "Mana: " + ChatColor.AQUA + stats.getMana() + "/" + currentTotalMana + 
                    (totalManaBonus > 0 ? ChatColor.GREEN + " (+" + totalManaBonus + ")" : ""),
                ChatColor.GRAY + "Mana Regen: " + ChatColor.WHITE + stats.getManaRegen() + "/s",
                ChatColor.GRAY + "Cooldown Reduction: " + ChatColor.WHITE + stats.getCooldownReduction() + "%",
                "",
                ChatColor.GRAY + "Movement Speed: " + ChatColor.WHITE + String.format("%.2f", currentMovementSpeed),
                ChatColor.GRAY + "Size: " + ChatColor.WHITE + String.format("%.2f", currentSize) + "x",
                ChatColor.GRAY + "Build Range: " + ChatColor.WHITE + String.format("%.1f", stats.getBuildRange()) + " blocks",
                ChatColor.GRAY + "Luck: " + ChatColor.YELLOW + stats.getLuck()
            },
            true
        );
        gui.setItem(14, resourceItem);

        // Mining Stats (Diamond Pickaxe)
        ItemStack miningItem = createStatsItem(Material.DIAMOND_PICKAXE,
            ChatColor.GRAY + "⛏ Mining Stats",
            new String[] {
                ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                ChatColor.GRAY + "Mining Speed: " + ChatColor.WHITE + String.format("%.2fx", currentMiningSpeed) +
                    (totalMiningSpeedBonus > 0 ? ChatColor.GREEN + " (+" + String.format("%.2f", totalMiningSpeedBonus) + "x)" : ""),
                ChatColor.GRAY + "Mining Fortune: " + ChatColor.WHITE + String.format("%.2fx", stats.getMiningFortune())
            },
            false
        );
        gui.setItem(16, miningItem);
        
        // === ROW 2: Skill Stats ===
        
        // Farming Stats (Diamond Hoe)
        ItemStack farmingItem = createStatsItem(Material.DIAMOND_HOE,
            ChatColor.GREEN + "🌾 Farming Stats",
            new String[] {
                ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                ChatColor.GRAY + "Farming Fortune: " + ChatColor.WHITE + String.format("%.0f", stats.getFarmingFortune()),
                ChatColor.GRAY + "  Multiplier: " + ChatColor.WHITE + String.format("%.1fx", (stats.getFarmingFortune() / 100.0) + 1.0)
            },
            false
        );
        gui.setItem(19, farmingItem);
        
        // Looting Fortune (Gold Sword)
        ItemStack lootingItem = createStatsItem(Material.GOLDEN_SWORD,
            ChatColor.GOLD + "💰 Looting Stats",
            new String[] {
                ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                ChatColor.GRAY + "Looting Fortune: " + ChatColor.WHITE + String.format("%.2fx", stats.getLootingFortune())
            },
            false
        );
        gui.setItem(21, lootingItem);
        
        // Fishing Stats (Fishing Rod)
        int[] waitTime = stats.getFishingWaitTime();
        String waitTimeStr = String.format("%.1f-%.1fs", waitTime[0] / 20.0, waitTime[1] / 20.0);
        
        ItemStack fishingItem = createStatsItem(Material.FISHING_ROD,
            ChatColor.AQUA + "🎣 Fishing Stats",
            new String[] {
                ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                ChatColor.YELLOW + "Fortune:",
                ChatColor.GRAY + "  Fishing Fortune: " + ChatColor.WHITE + String.format("%.0f", stats.getFishingFortune()),
                ChatColor.GRAY + "  Multiplier: " + ChatColor.WHITE + String.format("%.1fx", (stats.getFishingFortune() / 100.0) + 1.0),
                "",
                ChatColor.YELLOW + "Minigame Stats:",
                ChatColor.GRAY + "  Resilience: " + ChatColor.WHITE + String.format("%.1f%%", stats.getFishingResilience()),
                ChatColor.GRAY + "    " + getResilienceDescription(stats.getFishingResilience()),
                ChatColor.GRAY + "  Focus: " + ChatColor.WHITE + String.format("%.1f", stats.getFishingFocus()),
                ChatColor.GRAY + "    " + getFocusDescription(stats.getFishingFocus()),
                ChatColor.GRAY + "  Precision: " + ChatColor.WHITE + String.format("%.1f%%", stats.getFishingPrecision()),
                ChatColor.GRAY + "    " + getPrecisionDescription(stats.getFishingPrecision()),
                "",
                ChatColor.YELLOW + "Fishing Speed:",
                ChatColor.GRAY + "  Lure Potency: " + ChatColor.WHITE + stats.getLurePotency(),
                ChatColor.GRAY + "  Wait Time: " + ChatColor.WHITE + waitTimeStr,
                "",
                ChatColor.YELLOW + "Spawn Modifiers:",
                ChatColor.GRAY + "  Sea Monster Affinity: " + ChatColor.WHITE + String.format("%.1f%%", stats.getSeaMonsterAffinity()),
                ChatColor.GRAY + "    " + getSeaMonsterAffinityDescription(stats.getSeaMonsterAffinity()),
                ChatColor.GRAY + "  Treasure Sense: " + ChatColor.WHITE + String.format("%.1f%%", stats.getTreasureSense()),
                ChatColor.GRAY + "    " + getTreasureSenseDescription(stats.getTreasureSense())
            },
            false
        );
        gui.setItem(23, fishingItem);
        
        // === ROW 3: Elemental Affinity ===
        
        // Offensive Affinity (Blaze Powder)
        ItemStack offenseAffinityItem = createStatsItem(Material.BLAZE_POWDER,
            ChatColor.RED + "⚔ Offensive Affinity",
            new String[] {
                ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.FIRE, com.server.enchantments.elements.AffinityCategory.OFFENSE),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.WATER, com.server.enchantments.elements.AffinityCategory.OFFENSE),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.EARTH, com.server.enchantments.elements.AffinityCategory.OFFENSE),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.AIR, com.server.enchantments.elements.AffinityCategory.OFFENSE),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.NATURE, com.server.enchantments.elements.AffinityCategory.OFFENSE),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.LIGHTNING, com.server.enchantments.elements.AffinityCategory.OFFENSE),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.SHADOW, com.server.enchantments.elements.AffinityCategory.OFFENSE),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.LIGHT, com.server.enchantments.elements.AffinityCategory.OFFENSE)
            },
            false
        );
        gui.setItem(28, offenseAffinityItem);
        
        // Defensive Affinity (Shield)
        ItemStack defenseAffinityItem = createStatsItem(Material.SHIELD,
            ChatColor.BLUE + "🛡 Defensive Affinity",
            new String[] {
                ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.FIRE, com.server.enchantments.elements.AffinityCategory.DEFENSE),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.WATER, com.server.enchantments.elements.AffinityCategory.DEFENSE),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.EARTH, com.server.enchantments.elements.AffinityCategory.DEFENSE),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.AIR, com.server.enchantments.elements.AffinityCategory.DEFENSE),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.NATURE, com.server.enchantments.elements.AffinityCategory.DEFENSE),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.LIGHTNING, com.server.enchantments.elements.AffinityCategory.DEFENSE),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.SHADOW, com.server.enchantments.elements.AffinityCategory.DEFENSE),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.LIGHT, com.server.enchantments.elements.AffinityCategory.DEFENSE)
            },
            false
        );
        gui.setItem(30, defenseAffinityItem);
        
        // Utility Affinity (Feather)
        ItemStack utilityAffinityItem = createStatsItem(Material.FEATHER,
            ChatColor.GREEN + "✦ Utility Affinity",
            new String[] {
                ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.FIRE, com.server.enchantments.elements.AffinityCategory.UTILITY),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.WATER, com.server.enchantments.elements.AffinityCategory.UTILITY),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.EARTH, com.server.enchantments.elements.AffinityCategory.UTILITY),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.AIR, com.server.enchantments.elements.AffinityCategory.UTILITY),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.NATURE, com.server.enchantments.elements.AffinityCategory.UTILITY),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.LIGHTNING, com.server.enchantments.elements.AffinityCategory.UTILITY),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.SHADOW, com.server.enchantments.elements.AffinityCategory.UTILITY),
                getCategorizedAffinityLine(stats, com.server.enchantments.elements.ElementType.LIGHT, com.server.enchantments.elements.AffinityCategory.UTILITY)
            },
            false
        );
        gui.setItem(32, utilityAffinityItem);
        
        // Create decorative border with varied glass panes
        // Top and bottom rows - Gray glass panes
        for (int i = 0; i < 9; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, createGlassPane(Material.GRAY_STAINED_GLASS_PANE));
            }
            if (gui.getItem(45 + i) == null) {
                gui.setItem(45 + i, createGlassPane(Material.GRAY_STAINED_GLASS_PANE));
            }
        }
        
        // Side borders - Light gray glass panes
        for (int row = 1; row < 5; row++) {
            int leftSlot = row * 9;
            int rightSlot = row * 9 + 8;
            if (gui.getItem(leftSlot) == null) {
                gui.setItem(leftSlot, createGlassPane(Material.LIGHT_GRAY_STAINED_GLASS_PANE));
            }
            if (gui.getItem(rightSlot) == null) {
                gui.setItem(rightSlot, createGlassPane(Material.LIGHT_GRAY_STAINED_GLASS_PANE));
            }
        }
        
        // Fill remaining empty slots with black glass panes
        for (int i = 0; i < 54; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, createGlassPane(Material.BLACK_STAINED_GLASS_PANE));
            }
        }

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
     * Get a readable description of fishing resilience effects
     * Works like fortune: 100% = guaranteed +1 miss, 150% = +1 miss + 50% chance for +2 misses
     */
    private static String getResilienceDescription(double resilience) {
        if (resilience == 0) {
            return "No extra misses";
        }
        
        // Calculate guaranteed extra misses (whole number part after dividing by 100)
        int guaranteedMisses = (int) Math.floor(resilience / 100.0);
        
        // Calculate chance for an additional miss (remainder percentage)
        double chanceForNext = resilience % 100.0;
        
        // Format the description
        if (guaranteedMisses == 0) {
            return String.format("%.1f%% chance for +1 miss", chanceForNext);
        } else if (chanceForNext < 0.1) {
            return String.format("+%d extra miss%s", guaranteedMisses, guaranteedMisses > 1 ? "es" : "");
        } else {
            return String.format("+%d miss%s + %.1f%% chance for +%d", 
                guaranteedMisses, guaranteedMisses > 1 ? "es" : "",
                chanceForNext, guaranteedMisses + 1);
        }
    }
    
    /**
     * Format fishing focus description
     * 10% chance per value to increase catch zone boxes by 1 for the round
     */
    private static String getFocusDescription(double focus) {
        if (focus == 0) {
            return "No bonus";
        }
        
        // Calculate guaranteed bonuses (floor of focus / 10)
        int guaranteedBonuses = (int) Math.floor(focus / 10.0);
        
        // Calculate chance for an additional bonus (remainder percentage)
        double chanceForNext = (focus % 10.0) * 10.0;
        
        // Format the description
        if (guaranteedBonuses == 0) {
            return String.format("%.0f%% chance for +1 box size", chanceForNext);
        } else if (chanceForNext < 0.1) {
            return String.format("+%d box size%s", guaranteedBonuses, guaranteedBonuses > 1 ? "s" : "");
        } else {
            return String.format("+%d box%s + %.0f%% chance for +%d", 
                guaranteedBonuses, guaranteedBonuses > 1 ? "es" : "",
                chanceForNext, guaranteedBonuses + 1);
        }
    }
    
    /**
     * Format fishing precision description
     * Percentage chance to halve spike size (minimum 1)
     */
    private static String getPrecisionDescription(double precision) {
        if (precision == 0) {
            return "No reduction";
        } else if (precision >= 100) {
            return "50% smaller (guaranteed)";
        } else {
            return String.format("%.1f%% chance for 50%% reduction", precision);
        }
    }
    
    private static String getSeaMonsterAffinityDescription(double affinity) {
        if (affinity == 0) {
            return "No bonus";
        } else {
            double multiplier = 1.0 + (affinity / 100.0);
            return String.format("%.1fx mob spawn chance", multiplier);
        }
    }
    
    private static String getTreasureSenseDescription(double sense) {
        if (sense == 0) {
            return "No bonus";
        } else {
            double multiplier = 1.0 + (sense / 100.0);
            return String.format("%.1fx treasure spawn chance", multiplier);
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
     * Get formatted categorized affinity line for an element and category.
     * Only shows elements with non-zero affinity in that category.
     */
    private static String getCategorizedAffinityLine(PlayerStats stats, 
                                                     com.server.enchantments.elements.ElementType element,
                                                     com.server.enchantments.elements.AffinityCategory category) {
        com.server.enchantments.elements.CategorizedAffinity affinity = stats.getCategorizedAffinity();
        int affinityValue = affinity.get(element, category);
        
        // Color based on value
        ChatColor valueColor;
        if (affinityValue == 0) {
            valueColor = ChatColor.DARK_GRAY;
        } else if (affinityValue < 20) {
            valueColor = ChatColor.GRAY;
        } else if (affinityValue < 40) {
            valueColor = ChatColor.WHITE;
        } else if (affinityValue < 60) {
            valueColor = ChatColor.YELLOW;
        } else if (affinityValue < 80) {
            valueColor = ChatColor.GOLD;
        } else {
            valueColor = ChatColor.RED;
        }
        
        // Format: Icon Element: Value
        return ChatColor.DARK_GRAY + " • " + 
               element.getColoredIcon() + " " + 
               element.getColor() + element.name() + ": " + 
               valueColor + affinityValue;
    }
    
    /**
     * Get the player's class (mock method - would pull from the actual class system)
     */
    private static String getPlayerClass(PlayerProfile profile) {
        // Example implementation - in a real plugin this would pull from the player's actual class
        return "Adventurer"; // Placeholder value
    }
}