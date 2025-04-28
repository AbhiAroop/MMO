package com.server.profiles.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
            player.sendMessage("§cYou need to select a profile first!");
            return;
        }

        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;

        // Get current stats from the profile - these already include both base stats and equipment bonuses
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
        
        // Current values already include all equipment bonuses thanks to StatScanManager
        int currentPDamage = stats.getPhysicalDamage();
        int currentMDamage = stats.getMagicDamage();
        int currentTotalMana = stats.getTotalMana();
        int currentHealth = stats.getHealth();
        int currentArmor = stats.getArmor();
        int currentMagicResist = stats.getMagicResist();
        double currentAttackRange = stats.getAttackRange();
        double currentSize = stats.getSize();
        
        // Calculate total bonuses (equipment + permanent)
        int totalPDamageBonus = currentPDamage - basePDamage;
        int totalMDamageBonus = currentMDamage - baseMDamage;
        int totalManaBonus = currentTotalMana - baseMana;
        int totalHealthBonus = currentHealth - baseHealth;
        int totalArmorBonus = currentArmor - baseArmor;
        int totalMagicResistBonus = currentMagicResist - baseMagicResist;
        double totalAttackRangeBonus = currentAttackRange - baseAttackRange;
        double totalSizeBonus = currentSize - baseSize;

        // Extract information about equipped items for display purposes only
        // (not for stat calculation since StatScanManager already handles that)
        
        // Display info for armor pieces
        String helmetName = "None";
        String chestplateName = "None";
        String leggingsName = "None";
        String bootsName = "None";
        
        List<String> helmetStats = new ArrayList<>();
        List<String> chestplateStats = new ArrayList<>();
        List<String> leggingsStats = new ArrayList<>();
        List<String> bootsStats = new ArrayList<>();
        
        // Get helmet info
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet != null && helmet.hasItemMeta()) {
            helmetName = getItemDisplayName(helmet);
            if (helmet.getItemMeta().hasLore()) {
                extractItemStatsForDisplay(helmet, helmetStats);
            }
        }
        
        // Get chestplate info
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate != null && chestplate.hasItemMeta()) {
            chestplateName = getItemDisplayName(chestplate);
            if (chestplate.getItemMeta().hasLore()) {
                extractItemStatsForDisplay(chestplate, chestplateStats);
            }
        }
        
        // Get leggings info
        ItemStack leggings = player.getInventory().getLeggings();
        if (leggings != null && leggings.hasItemMeta()) {
            leggingsName = getItemDisplayName(leggings);
            if (leggings.getItemMeta().hasLore()) {
                extractItemStatsForDisplay(leggings, leggingsStats);
            }
        }
        
        // Get boots info
        ItemStack boots = player.getInventory().getBoots();
        if (boots != null && boots.hasItemMeta()) {
            bootsName = getItemDisplayName(boots);
            if (boots.getItemMeta().hasLore()) {
                extractItemStatsForDisplay(boots, bootsStats);
            }
        }
        
        // Get main hand item info for display
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        String mainHandName = "None";
        List<String> mainHandStats = new ArrayList<>();
        
        if (mainHand != null && mainHand.hasItemMeta()) {
            mainHandName = getItemDisplayName(mainHand);
            if (mainHand.getItemMeta().hasLore()) {
                extractItemStatsForDisplay(mainHand, mainHandStats);
            }
        }

        // Create the main GUI
        Inventory gui = Bukkit.createInventory(null, 36, "Profile Stats");

        // Combat Stats (Diamond Sword)
        ItemStack combatItem = createGuiItem(Material.DIAMOND_SWORD, "§c§lCombat Stats",
            "§7Physical Damage: §f" + currentPDamage + 
                (totalPDamageBonus > 0 ? " §a(+" + totalPDamageBonus + ")" : ""),
            "§7Magic Damage: §f" + currentMDamage + 
                (totalMDamageBonus > 0 ? " §a(+" + totalMDamageBonus + ")" : ""),
            "§7Attack Range: §f" + currentAttackRange + " blocks" +
                (totalAttackRangeBonus > 0 ? " §a(+" + totalAttackRangeBonus + ")" : ""),
            "§7Ranged Damage: §f" + stats.getRangedDamage(),
            "§7Critical Chance: §f" + String.format("%.1f", stats.getCriticalChance() * 100) + "%",
            "§7Critical Damage: §f" + stats.getCriticalDamage() + "x",
            "§7Burst Chance: §f" + String.format("%.1f", stats.getBurstChance() * 100) + "%",
            "§7Burst Damage: §f" + stats.getBurstDamage() + "x",
            "§7Attack Speed: §f" + stats.getAttackSpeed()
        );

        // Defense Stats (Diamond Chestplate)
        ItemStack defenseItem = createGuiItem(Material.DIAMOND_CHESTPLATE, "§b§lDefense Stats",
            "§7Health: §f" + currentHealth + 
                (totalHealthBonus > 0 ? " §a(+" + totalHealthBonus + ")" : ""),
            "§7Armor: §f" + currentArmor + 
                (totalArmorBonus > 0 ? " §a(+" + totalArmorBonus + ")" : "") +
                " §7[" + String.format("%.1f", stats.getPhysicalDamageReduction()) + "% reduction]",                
            "§7Magic Resist: §f" + currentMagicResist + 
                (totalMagicResistBonus > 0 ? " §a(+" + totalMagicResistBonus + ")" : "") +
                " §7[" + String.format("%.1f", stats.getMagicDamageReduction()) + "% reduction]",
            "§7Life Steal: §f" + stats.getLifeSteal() + "%",
            "§7Omnivamp: §f" + stats.getOmnivamp() + "%",
            "§7Size: §f" + currentSize + 
            (totalSizeBonus > 0 ? " §a(+" + String.format("%.2f", totalSizeBonus) + ")" : ""),
            "§7Health Regeneration: §f" + stats.getHealthRegen() + " §7per second"
        );

        // Resource Stats (Experience Bottle)
        ItemStack resourceItem = createGuiItem(Material.EXPERIENCE_BOTTLE, "§a§lResource Stats",
            "§7Mana: §f" + stats.getMana() + "/" + currentTotalMana,
            "§7Base Mana: §f" + baseMana + 
                (totalManaBonus > 0 ? " §a(+" + totalManaBonus + ")" : ""),
            "§7Mana Regen: §f" + stats.getManaRegen() + "/s",
            "§7Cooldown Reduction: §f" + stats.getCooldownReduction() + "%",
            "§7Movement Speed: §f" + stats.getSpeed() + "x"
        );

        // Fortune Stats (Gold Ingot)
        ItemStack fortuneItem = createGuiItem(Material.GOLD_INGOT, "§e§lFortune Stats",
            "§7Mining Fortune: §f" + String.format("%.2f", stats.getMiningFortune()) + "x",
            "§8 • Each 100 points guarantees +1 ore drops",
            "§8 • Remaining points give chance for another drop",
            "§8 • Current: §f" + getFortuneDescription(stats.getMiningFortune(), "ore"),
            "",
            "§7Farming Fortune: §f" + String.format("%.2f", stats.getFarmingFortune()) + "x",
            "§7Looting Fortune: §f" + String.format("%.2f", stats.getLootingFortune()) + "x", 
            "§7Fishing Fortune: §f" + String.format("%.2f", stats.getFishingFortune()) + "x",
            "§7Luck: §f" + stats.getLuck()
        );

        // Equipment summary (for display purposes only)
        List<String> armorLore = new ArrayList<>();
        
        // Helmet
        armorLore.add("§6§lHelmet:");
        armorLore.add("§7" + helmetName);
        helmetStats.forEach(stat -> armorLore.add("  §7" + stat));
        armorLore.add("");
        
        // Chestplate
        armorLore.add("§6§lChestplate:");
        armorLore.add("§7" + chestplateName);
        chestplateStats.forEach(stat -> armorLore.add("  §7" + stat));
        armorLore.add("");
        
        // Leggings
        armorLore.add("§6§lLeggings:");
        armorLore.add("§7" + leggingsName);
        leggingsStats.forEach(stat -> armorLore.add("  §7" + stat));
        armorLore.add("");
        
        // Boots
        armorLore.add("§6§lBoots:");
        armorLore.add("§7" + bootsName);
        bootsStats.forEach(stat -> armorLore.add("  §7" + stat));
        
        // Main hand (if it has stats)
        if (mainHandStats.size() > 0) {
            armorLore.add("");
            armorLore.add("§6§lMain Hand:");
            armorLore.add("§7" + mainHandName);
            mainHandStats.forEach(stat -> armorLore.add("  §7" + stat));
        }
        
        // Equipment display item
        ItemStack equippedItem = createGuiItem(Material.GOLDEN_HELMET, "§d§lEquipped Items", 
                                            armorLore.toArray(new String[0]));

        // Place items in GUI
        gui.setItem(10, combatItem);    // Left side
        gui.setItem(12, defenseItem);   // Middle-left
        gui.setItem(14, resourceItem);  // Middle-right
        gui.setItem(16, fortuneItem);   // Right side
        gui.setItem(22, equippedItem);  // Bottom center

        // Fill empty slots with glass panes
        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
    }

    /**
     * Extract item stats for display purposes only (not for calculation)
     */
    private static void extractItemStatsForDisplay(ItemStack item, List<String> statsList) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return;
        
        for (String loreLine : item.getItemMeta().getLore()) {
            String cleanLine = loreLine.replaceAll("§[0-9a-fk-or]", "");
            
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
                cleanLine.contains("Attack Speed:")) {
                
                statsList.add(cleanLine);
            }
        }
    }

    private static void extractArmorStats(ItemStack armor, String pieceType, int health, int armorVal, int magicResist, int physDamage, int magicDamage) {
        if (armor == null || !armor.hasItemMeta() || !armor.getItemMeta().hasLore()) {
            return;
        }
        
        for (String loreLine : armor.getItemMeta().getLore()) {
            String cleanLine = loreLine.replaceAll("§[0-9a-fk-or]", "");
            
            if (cleanLine.contains("Health:")) {
                try {
                    String[] parts = cleanLine.split("\\+");
                    if (parts.length > 1) {
                        health = Integer.parseInt(parts[1].trim());
                    }
                } catch (Exception e) {
                    // Ignore parsing errors
                }
            } else if (cleanLine.contains("Armor:")) {
                try {
                    String[] parts = cleanLine.split("\\+");
                    if (parts.length > 1) {
                        armorVal = Integer.parseInt(parts[1].trim());
                    }
                } catch (Exception e) {
                    // Ignore parsing errors
                }
            } else if (cleanLine.contains("Magic Resist:")) {
                try {
                    String[] parts = cleanLine.split("\\+");
                    if (parts.length > 1) {
                        magicResist = Integer.parseInt(parts[1].trim());
                    }
                } catch (Exception e) {
                    // Ignore parsing errors
                }
            } else if (cleanLine.contains("Physical Damage:")) {
                try {
                    String[] parts = cleanLine.split("\\+");
                    if (parts.length > 1) {
                        physDamage = Integer.parseInt(parts[1].trim());
                    }
                } catch (Exception e) {
                    // Ignore parsing errors
                }
            } else if (cleanLine.contains("Magic Damage:")) {
                try {
                    String[] parts = cleanLine.split("\\+");
                    if (parts.length > 1) {
                        magicDamage = Integer.parseInt(parts[1].trim());
                    }
                } catch (Exception e) {
                    // Ignore parsing errors
                }
            }
        }
    }

    private static String getItemDisplayName(ItemStack item) {
        if (item == null) return "None";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return formatMaterialName(item.getType().name());
    }

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

    private static ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        
        List<String> loreList = new ArrayList<>();
        loreList.addAll(Arrays.asList(lore));
        
        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Get a readable description of fortune effects
     */
    private static String getFortuneDescription(double fortune, String type) {
        int guaranteedMultiplier = (int)(fortune / 100);
        double chanceForNext = (fortune % 100);
        
        if (guaranteedMultiplier == 0) {
            return String.format("%.1f%% chance for 2x %s drops", chanceForNext, type);
        } else if (guaranteedMultiplier == 1) {
            return String.format("2x %s drops + %.1f%% chance for 3x", type, chanceForNext);
        } else {
            return String.format("%dx %s drops + %.1f%% chance for %dx", 
                guaranteedMultiplier + 1, type, chanceForNext, guaranteedMultiplier + 2);
        }
    }
}