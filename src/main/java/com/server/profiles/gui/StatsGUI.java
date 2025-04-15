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
        
        // Calculate current values (already includes permanent bonuses)
        int currentPDamage = stats.getPhysicalDamage();
        int currentMDamage = stats.getMagicDamage(); 
        int currentTotalMana = stats.getTotalMana();
        
        // Calculate permanent bonuses
        int permPDamageBonus = currentPDamage - basePDamage;
        int permMDamageBonus = currentMDamage - baseMDamage;
        int permManaBonus = currentTotalMana - baseMana;

        // Get base attack range
        double baseAttackRange = stats.getDefaultAttackRange();
        double currentAttackRange = stats.getAttackRange();
        double attackRangeBonus = currentAttackRange - baseAttackRange;


        // Extract additional temporary bonuses from held item
        int heldPhysicalDamage = 0;
        int heldMagicDamage = 0;
        int heldManaBonus = 0;
        double heldAttackRange = 0;
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem != null && heldItem.hasItemMeta() && heldItem.getItemMeta().hasLore()) {
            for (String loreLine : heldItem.getItemMeta().getLore()) {
                if (loreLine.contains("Physical Damage:")) {
                    try {
                        String damageStr = loreLine.split("\\+")[1].trim();
                        heldPhysicalDamage = (int) Double.parseDouble(damageStr);
                    } catch (Exception e) {
                        // Ignore parsing errors
                    }
                } else if (loreLine.contains("Magic Damage:")) {
                    try {
                        String damageStr = loreLine.split("\\+")[1].trim();
                        heldMagicDamage = (int) Double.parseDouble(damageStr);
                    } catch (Exception e) {
                        // Ignore parsing errors
                    }
                } else if (loreLine.contains("Mana:")) {
                    try {
                        String manaStr = loreLine.split("\\+")[1].trim();
                        heldManaBonus = (int) Double.parseDouble(manaStr);
                    } catch (Exception e) {
                        // Ignore parsing errors
                    }
                }
                else if (loreLine.contains("Attack Range:")) {
                    try {
                        String rangeStr = loreLine.split("\\+")[1].trim();
                        heldAttackRange = Double.parseDouble(rangeStr);
                    } catch (Exception e) {
                        // Ignore parsing errors
                    }
                }    
            }
        }

        Inventory gui = Bukkit.createInventory(null, 27, "Profile Stats");

        // Combat Stats (Diamond Sword) - properly shows base, permanent bonuses, and held item bonuses
        ItemStack combatItem = createGuiItem(Material.DIAMOND_SWORD, "§c§lCombat Stats",
            "§7Physical Damage: §f" + basePDamage + 
                (permPDamageBonus > 0 ? " §a(+" + permPDamageBonus + ")" : "") + 
                (heldPhysicalDamage > 0 ? " §e[+" + heldPhysicalDamage + " weapon]" : ""),
            "§7Magic Damage: §f" + baseMDamage + 
                (permMDamageBonus > 0 ? " §a(+" + permMDamageBonus + ")" : "") +
                (heldMagicDamage > 0 ? " §e[+" + heldMagicDamage + " weapon]" : ""),
            "§7Total Physical Damage: §f" + (currentPDamage + heldPhysicalDamage),
            "§7Total Magic Damage: §f" + (currentMDamage + heldMagicDamage),
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
            "§7Health: §f" + stats.getHealth(),
            "§7Armor: §f" + stats.getArmor(),
            "§7Magic Resist: §f" + stats.getMagicResist(),
            "§7Life Steal: §f" + stats.getLifeSteal(),
            "§7Omnivamp: §f" + stats.getOmnivamp()
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

        // Place items in GUI
        gui.setItem(10, combatItem);    // Left side
        gui.setItem(12, defenseItem);   // Middle-left
        gui.setItem(14, resourceItem);  // Middle-right
        gui.setItem(16, fortuneItem);   // Right side

        // Fill empty slots with glass panes
        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
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