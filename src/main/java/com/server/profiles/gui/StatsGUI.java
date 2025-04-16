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

        // Get current stats WITHOUT modifying them with StatsUpdateManager
        PlayerStats stats = profile.getStats();

        // Calculate base values
        int basePDamage = stats.getDefaultPhysicalDamage();
        int baseMDamage = stats.getDefaultMagicDamage();
        int baseMana = stats.getDefaultMana();
        int baseHealth = stats.getDefaultHealth();
        int baseArmor = stats.getDefaultArmor();
        int baseMagicResist = stats.getDefaultMagicResist();
        
        // Calculate current values (already includes permanent bonuses)
        int currentPDamage = stats.getPhysicalDamage();
        int currentMDamage = stats.getMagicDamage();
        int currentTotalMana = stats.getTotalMana();
        int currentHealth = stats.getHealth();
        int currentArmor = stats.getArmor();
        int currentMagicResist = stats.getMagicResist();
        
        // Calculate permanent bonuses
        int permPDamageBonus = currentPDamage - basePDamage;
        int permMDamageBonus = currentMDamage - baseMDamage;
        int permManaBonus = currentTotalMana - baseMana;
        int permHealthBonus = currentHealth - baseHealth;
        int permArmorBonus = currentArmor - baseArmor;
        int permMagicResistBonus = currentMagicResist - baseMagicResist;

        // Get base attack range
        double baseAttackRange = stats.getDefaultAttackRange();
        double currentAttackRange = stats.getAttackRange();
        double attackRangeBonus = currentAttackRange - baseAttackRange;

        // Get base size
        double baseSize = stats.getDefaultSize();
        double currentSize = stats.getSize();
        double sizeBonus = currentSize - baseSize;

        // Extract additional temporary bonuses from held item
        int heldPhysicalDamage = 0;
        int heldMagicDamage = 0;
        int heldManaBonus = 0;
        double heldAttackRange = 0;
        double heldSize = 0;

        // Extract equipped armor stats
        int helmetHealth = 0, helmetArmor = 0, helmetMagicResist = 0, helmetPDamage = 0, helmetMDamage = 0;
        int chestplateHealth = 0, chestplateArmor = 0, chestplateMagicResist = 0, chestplatePDamage = 0, chestplateMDamage = 0;
        int leggingsHealth = 0, leggingsArmor = 0, leggingsMagicResist = 0, leggingsPDamage = 0, leggingsMDamage = 0;
        int bootsHealth = 0, bootsArmor = 0, bootsMagicResist = 0, bootsPDamage = 0, bootsMDamage = 0;
        
        // Check held item stats (only if not armor)
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem != null && heldItem.hasItemMeta() && heldItem.getItemMeta().hasLore()) {
            // First check if it's an armor item - if so, don't show its stats when held
            boolean isArmor = false;
            if (heldItem.getType().name().endsWith("_HELMET") || 
                heldItem.getType().name().endsWith("_CHESTPLATE") || 
                heldItem.getType().name().endsWith("_LEGGINGS") || 
                heldItem.getType().name().endsWith("_BOOTS") ||
                heldItem.getType().name().equals("CARVED_PUMPKIN") || 
                heldItem.getType().name().equals("PLAYER_HEAD") ||
                heldItem.getType().name().equals("SKULL_ITEM")) {
                isArmor = true;
            }
            
            // Only process stats if it's not an armor item
            if (!isArmor) {
                for (String loreLine : heldItem.getItemMeta().getLore()) {
                    if (loreLine.contains("Physical Damage:")) {
                        try {
                            String damageStr = loreLine.split("\\+")[1].trim();
                            damageStr = damageStr.replaceAll("§[0-9a-fk-or]", "");
                            heldPhysicalDamage = (int) Double.parseDouble(damageStr);
                        } catch (Exception e) {
                            // Ignore parsing errors
                        }
                    } else if (loreLine.contains("Magic Damage:")) {
                        try {
                            String damageStr = loreLine.split("\\+")[1].trim();
                            damageStr = damageStr.replaceAll("§[0-9a-fk-or]", "");
                            heldMagicDamage = (int) Double.parseDouble(damageStr);
                        } catch (Exception e) {
                            // Ignore parsing errors
                        }
                    } else if (loreLine.contains("Mana:")) {
                        try {
                            String manaStr = loreLine.split("\\+")[1].trim();
                            manaStr = manaStr.replaceAll("§[0-9a-fk-or]", "");
                            heldManaBonus = (int) Double.parseDouble(manaStr);
                        } catch (Exception e) {
                            // Ignore parsing errors
                        }
                    }
                    else if (loreLine.contains("Attack Range:")) {
                        try {
                            String rangeStr = loreLine.split("\\+")[1].trim();
                            rangeStr = rangeStr.replaceAll("§[0-9a-fk-or]", "");
                            heldAttackRange = Double.parseDouble(rangeStr);
                        } catch (Exception e) {
                            // Ignore parsing errors
                        }
                    }
                    else if (loreLine.contains("Size:")) {
                        try {
                            String sizeStr = loreLine.split("\\+")[1].trim();
                            sizeStr = sizeStr.replaceAll("§[0-9a-fk-or]", "");
                            heldSize = Double.parseDouble(sizeStr);
                        } catch (Exception e) {
                            // Ignore parsing errors
                        }
                    }
                }
            }
        }

        // Check equipped armor stats
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet != null && helmet.hasItemMeta() && helmet.getItemMeta().hasLore()) {
            extractArmorStats(helmet, "Helmet", helmetHealth, helmetArmor, helmetMagicResist, helmetPDamage, helmetMDamage);
        }

        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate != null && chestplate.hasItemMeta() && chestplate.getItemMeta().hasLore()) {
            extractArmorStats(chestplate, "Chestplate", chestplateHealth, chestplateArmor, chestplateMagicResist, chestplatePDamage, chestplateMDamage);
        }

        ItemStack leggings = player.getInventory().getLeggings();
        if (leggings != null && leggings.hasItemMeta() && leggings.getItemMeta().hasLore()) {
            extractArmorStats(leggings, "Leggings", leggingsHealth, leggingsArmor, leggingsMagicResist, leggingsPDamage, leggingsMDamage);
        }

        ItemStack boots = player.getInventory().getBoots();
        if (boots != null && boots.hasItemMeta() && boots.getItemMeta().hasLore()) {
            extractArmorStats(boots, "Boots", bootsHealth, bootsArmor, bootsMagicResist, bootsPDamage, bootsMDamage);
        }

        // Create the main GUI
        Inventory gui = Bukkit.createInventory(null, 36, "Profile Stats");

        // Combat Stats (Diamond Sword) - properly shows base, permanent bonuses, and held item bonuses
        ItemStack combatItem = createGuiItem(Material.DIAMOND_SWORD, "§c§lCombat Stats",
            "§7Physical Damage: §f" + (currentPDamage + heldPhysicalDamage) + 
                (permPDamageBonus > 0 ? " §a(+" + permPDamageBonus + ")" : "") + 
                (heldPhysicalDamage > 0 ? " §e[+" + heldPhysicalDamage + " weapon]" : ""),
            "§7Magic Damage: §f" + (currentMDamage + heldMagicDamage) + 
                (permMDamageBonus > 0 ? " §a(+" + permMDamageBonus + ")" : "") +
                (heldMagicDamage > 0 ? " §e[+" + heldMagicDamage + " weapon]" : ""),
            "§7Attack Range: §f" + baseAttackRange + 
                (attackRangeBonus > 0 ? " §a(+" + attackRangeBonus + ")" : "") +
                (heldAttackRange > 0 ? " §e[+" + heldAttackRange + " weapon]" : "") +
                " blocks",
            "§7Effective Range: §f" + (currentAttackRange + heldAttackRange) + " blocks",
            "§7Ranged Damage: §f" + stats.getRangedDamage(),
            "§7Critical Chance: §f" + (stats.getCriticalChance() * 100) + "%",
            "§7Critical Damage: §f" + stats.getCriticalDamage() + "x",
            "§7Burst Chance: §f" + (stats.getBurstChance() * 100) + "%",
            "§7Burst Damage: §f" + stats.getBurstDamage() + "x",
            "§7Attack Speed: §f" + stats.getAttackSpeed()
        );

        // Defense Stats (Diamond Chestplate)
        ItemStack defenseItem = createGuiItem(Material.DIAMOND_CHESTPLATE, "§b§lDefense Stats",
            "§7Health: §f" + stats.getHealth() + 
                (permHealthBonus > 0 ? " §a(+" + permHealthBonus + ")" : ""),
            "§7Armor: §f" + stats.getArmor() + 
                (permArmorBonus > 0 ? " §a(+" + permArmorBonus + ")" : "") +
                " §7[" + String.format("%.1f", stats.getPhysicalDamageReduction()) + "% reduction]",                
            "§7Magic Resist: §f" + stats.getMagicResist() + 
                (permMagicResistBonus > 0 ? " §a(+" + permMagicResistBonus + ")" : "") +
                " §7[" + String.format("%.1f", stats.getMagicDamageReduction()) + "% reduction]",
            "§7Life Steal: §f" + stats.getLifeSteal(),
            "§7Omnivamp: §f" + stats.getOmnivamp(),
            "§7Size: §f" + baseSize + 
            (sizeBonus > 0 ? " §a(+" + String.format("%.2f", sizeBonus) + ")" : "") +
            (heldSize > 0 ? " §e[+" + String.format("%.2f", heldSize) + " weapon]" : "")
        );

        // Resource Stats (Experience Bottle) - properly shows mana with temporary bonuses
        ItemStack resourceItem = createGuiItem(Material.EXPERIENCE_BOTTLE, "§a§lResource Stats",
            "§7Mana: §f" + stats.getMana() + "/" + (currentTotalMana + heldManaBonus),
            "§7Base Mana: §f" + baseMana + 
                (permManaBonus > 0 ? " §a(+" + permManaBonus + ")" : "") +
                (heldManaBonus > 0 ? " §e[+" + heldManaBonus + " weapon]" : ""),
            "§7Mana Regen: §f" + stats.getManaRegen() + "/s",
            "§7Cooldown Reduction: §f" + stats.getCooldownReduction() + "%",
            "§7Movement Speed: §f" + stats.getSpeed() + "x"
        );

        // Fortune Stats (Gold Ingot)
        ItemStack fortuneItem = createGuiItem(Material.GOLD_INGOT, "§e§lFortune Stats",
            "§7Mining Fortune: §f" + stats.getMiningFortune() + "x",
            "§7Farming Fortune: §f" + stats.getFarmingFortune() + "x",
            "§7Looting Fortune: §f" + stats.getLootingFortune() + "x",
            "§7Fishing Fortune: §f" + stats.getFishingFortune() + "x",
            "§7Luck: §f" + stats.getLuck()
        );

        // New: Equipped Armor Stats
        ItemStack equippedItem = createGuiItem(Material.GOLDEN_HELMET, "§d§lEquipped Armor",
            "§6§lHelmet:",
            (helmet != null ? "§7" + getItemDisplayName(helmet) : "§7None"),
            helmetHealth > 0 ? "  §7Health: §a+" + helmetHealth : "",
            helmetArmor > 0 ? "  §7Armor: §a+" + helmetArmor : "",
            helmetMagicResist > 0 ? "  §7Magic Resist: §a+" + helmetMagicResist : "",
            "",
            "§6§lChestplate:",
            (chestplate != null ? "§7" + getItemDisplayName(chestplate) : "§7None"),
            chestplateHealth > 0 ? "  §7Health: §a+" + chestplateHealth : "",
            chestplateArmor > 0 ? "  §7Armor: §a+" + chestplateArmor : "",
            chestplateMagicResist > 0 ? "  §7Magic Resist: §a+" + chestplateMagicResist : "",
            "",
            "§6§lLeggings:",
            (leggings != null ? "§7" + getItemDisplayName(leggings) : "§7None"),
            leggingsHealth > 0 ? "  §7Health: §a+" + leggingsHealth : "",
            leggingsArmor > 0 ? "  §7Armor: §a+" + leggingsArmor : "",
            leggingsMagicResist > 0 ? "  §7Magic Resist: §a+" + leggingsMagicResist : "",
            "",
            "§6§lBoots:",
            (boots != null ? "§7" + getItemDisplayName(boots) : "§7None"),
            bootsHealth > 0 ? "  §7Health: §a+" + bootsHealth : "",
            bootsArmor > 0 ? "  §7Armor: §a+" + bootsArmor : "",
            bootsMagicResist > 0 ? "  §7Magic Resist: §a+" + bootsMagicResist : ""
        );

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
}