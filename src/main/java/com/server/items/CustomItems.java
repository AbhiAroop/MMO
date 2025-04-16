package com.server.items;

import java.util.Arrays;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.server.Main;
import static com.server.items.ItemRarity.BASIC;

public class CustomItems {
    
    public static ItemStack createWitchHat() {
        ItemStack item = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = item.getItemMeta();
        
        // Set custom model data (1XXXXX for cosmetics, 20000 base for helmets)
        meta.setCustomModelData(120001); // 1 for cosmetic, 2 for helmet, 0001 for first variant
        
        // Set name and lore
        meta.setDisplayName("§5§lWitch's Hat");
        meta.setLore(Arrays.asList(
            "§7Rarity: " + ItemRarity.RARE.getFormattedName(),
            "§7\"A mysterious hat imbued with ancient magic\"",
            "",
            "§7Cosmetic Item",
            "§8Equip in cosmetic menu with /cosmetics"
        ));
        
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createApprenticeEdge() {
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        
        // Set custom model data (2XXXXX for weapons, 10000 base for swords)
        meta.setCustomModelData(210001); // 2 for weapon, 1 for sword type, 0001 for first variant
        
        // Set name and lore
        meta.setDisplayName("§7§lApprentice's Edge");
        meta.setLore(Arrays.asList(
            "§7Rarity: " + ItemRarity.BASIC.getFormattedName(),
            "§7\"A reliable blade forged for aspiring warriors.\"",
            "",
            "§7Stats:",
            "§cPhysical Damage: §c+5",
            "",
            "§6Passive: §ePrecision Strike",
            "§7Every 5th attack deals §c+3 §7bonus physical damage.",
            "",
            "§8This simple yet effective weapon serves as",
            "§8a faithful companion for those beginning",
            "§8their journey into combat mastery."
        ));
        
        // Track hit counter with persistent data
        NamespacedKey key = new NamespacedKey(Main.getInstance(), "hit_counter");
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 0);
        
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createEmberwoodStaff() {
        ItemStack item = new ItemStack(Material.WOODEN_HOE);
        ItemMeta meta = item.getItemMeta();
        
        // Set custom model data (2XXXXX for functional items, 40000 base for staves)
        meta.setCustomModelData(240001); // 2 for weapon, 4 for staff, 0001 for first variant
        
        // Set name and lore
        meta.setDisplayName("§c§lEmberwood Staff");
        meta.setLore(Arrays.asList("§7Rarity: " + BASIC.getFormattedName(),
            "§7\"Carved from ancient ember trees that once grew",
            "§7near the heart of a dormant volcano.\"",
            "",
            "§7Stats:",
            "§bMana: §b+20",
            "",
            "§6Ability: §eFire Beam §7(Right-Click)",
            "§7Shoots a beam of fire that extends up to",
            "§710 blocks and sets enemies ablaze, dealing",
            "§7§b3 §7magic damage per second.",
            "§7Mana Cost: §b30",
            "§7Cooldown: §e20s",
            "",
            "§8The wood remains eternally warm to the touch,",
            "§8resonating with the ancient fire magic within."
        ));
        
        // Set base attack speed attribute (wooden hoes have -3 attack speed by default)
        // We need to override this to make it usable
        AttributeModifier attackSpeedMod;
            attackSpeedMod = new AttributeModifier(
                    UUID.randomUUID(),
                    "generic.attackSpeed",
                    -2.0, // Set to player's default attack speed
                    AttributeModifier.Operation.ADD_NUMBER
            );
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, attackSpeedMod);
        
        // Add identifier for ability system
        NamespacedKey keyAbilityId = new NamespacedKey(Main.getInstance(), "ability_id");
        meta.getPersistentDataContainer().set(keyAbilityId, PersistentDataType.STRING, "fire_beam");
        
        // Add cooldown tracker
        NamespacedKey keyCooldown = new NamespacedKey(Main.getInstance(), "cooldown");
        meta.getPersistentDataContainer().set(keyCooldown, PersistentDataType.LONG, 0L);
        
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createArcloom() {
        ItemStack item = new ItemStack(Material.WOODEN_HOE);
        ItemMeta meta = item.getItemMeta();
        
        // Set custom model data (2XXXXX for functional items, 50000 base for special weapons)
        meta.setCustomModelData(250001); // 2 for weapon, 5 for special weapon, 0001 for first variant
        
        // Set name and lore
        meta.setDisplayName("§b§lArcloom");
        meta.setLore(Arrays.asList(
            "§7Rarity: " + ItemRarity.ARCANE.getFormattedName(),
            "§7\"A legendary weapon infused with the power of",
            "§7storms and the reach of the horizon.\"",
            "",
            "§7Stats:",
            "§cPhysical Damage: §c+35",
            "§eAttack Range: §e+1",
            "",
            "§6Ability: §eLightning Throw §7(Right-Click)",
            "§7Hurl the trident forward, striking all enemies",
            "§7in its path for §c10 §7+ §c10% §7of your physical",
            "§7damage. Returns to your hand after 2 seconds.",
            "§7Mana Cost: §b20",
            "§7Cooldown: §e10s",
            "",
            "§8Forged in the heart of a tempest, this weapon",
            "§8commands the fury of storms and extends the",
            "§8reach of its wielder beyond normal limits."
        ));
        
        // Set attack speed modifier to override wooden hoe's slow attack speed
        AttributeModifier attackSpeedMod = new AttributeModifier(
            UUID.randomUUID(),
            "generic.attackSpeed",
            -2.0, // Set to player's default attack speed
            AttributeModifier.Operation.ADD_NUMBER
        );
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, attackSpeedMod);
        
        // Add identifier for ability system
        NamespacedKey keyAbilityId = new NamespacedKey(Main.getInstance(), "ability_id");
        meta.getPersistentDataContainer().set(keyAbilityId, PersistentDataType.STRING, "lightning_throw");
        
        // Add cooldown tracker
        NamespacedKey keyCooldown = new NamespacedKey(Main.getInstance(), "cooldown");
        meta.getPersistentDataContainer().set(keyCooldown, PersistentDataType.LONG, 0L);
        
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCrownOfMagnus() {
        ItemStack item = new ItemStack(Material.GOLDEN_HELMET);
        ItemMeta meta = item.getItemMeta();
        
        // Set custom model data (2XXXXX for functional items, 30000 base for helmets)
        meta.setCustomModelData(230001); // 2 for functional item, 3 for helmet, 0001 for first variant
        
        // Set name and lore
        meta.setDisplayName("§a§lCrown of Magnus");
        meta.setLore(Arrays.asList(
            "§7Rarity: " + ItemRarity.ENHANCED.getFormattedName(),
            "§7\"A towering crown once worn by the giant king",
            "§7Magnus, granting its wearer extraordinary",
            "§7vitality and stature.\"",
            "",
            "§7Stats:",
            "§cHealth: §c+300",
            "§aArmor: §a+30",
            "§bMagic Resist: §b+20",
            "§eSize: §e+0.5",
            "",
            "§8The crown adjusts to fit its wearer, but the",
            "§8magic within still carries the essence of its",
            "§8former giant owner."
        ));
        
        // Add armor attribute modifier
        AttributeModifier armorModifier = new AttributeModifier(
            UUID.randomUUID(),
            "generic.armor",
            0.0, // Same as golden helmet base armor
            AttributeModifier.Operation.ADD_NUMBER
        );
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, armorModifier);
        
        item.setItemMeta(meta);
        return item;
    }
    
}