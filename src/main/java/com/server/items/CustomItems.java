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
        ItemStack item = new ItemStack(Material.CARROT_ON_A_STICK);
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
            "§8their journey into combat mastery.",
            ""
        ));
        
        // Track hit counter with persistent data
        NamespacedKey key = new NamespacedKey(Main.getInstance(), "hit_counter");
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 0);
        
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createEmberwoodStaff() {
        ItemStack item = new ItemStack(Material.CARROT_ON_A_STICK);
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
            "§8resonating with the ancient fire magic within.",
            ""
        ));
        
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
        ItemStack item = new ItemStack(Material.CARROT_ON_A_STICK);
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
            "§8reach of its wielder beyond normal limits.",
            ""
        ));
        
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
            "§8former giant owner.",
            ""
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

    public static ItemStack createSiphonFang() {
        ItemStack item = new ItemStack(Material.CARROT_ON_A_STICK);
        ItemMeta meta = item.getItemMeta();
        
        // Set custom model data (2XXXXX for weapons, 10000 base for swords)
        meta.setCustomModelData(210002); // 2 for weapon, 1 for sword type, 0002 for second sword variant
        
        // Set name and lore
        meta.setDisplayName("§c§lSiphon Fang");
        meta.setLore(Arrays.asList(
            "§7Rarity: " + ItemRarity.BASIC.getFormattedName(),
            "§7\"A jagged blade that hungers for the essence of its victims.\"",
            "",
            "§7Stats:",
            "§cPhysical Damage: §c+7",
            "§dLifesteal: §d+3",
            "",
            "§8Forged from the fang of an ancient parasitic creature,",
            "§8this weapon transfers the life force of its victims",
            "§8directly to its wielder with each strike.",
            ""
        ));
        
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createFleshrake() {
        ItemStack item = new ItemStack(Material.CARROT_ON_A_STICK);
        ItemMeta meta = item.getItemMeta();
        
        // Set custom model data (2XXXXX for weapons, 20000 base for special scythes)
        meta.setCustomModelData(220001); // 2 for weapon, 2 for scythe type, 0001 for first variant
        
        // Set name and lore
        meta.setDisplayName("§4§lFleshrake");
        meta.setLore(Arrays.asList(
            "§7Rarity: " + ItemRarity.RARE.getFormattedName(),
            "§7\"An unholy scythe that harvests more than just flesh.\"",
            "",
            "§7Stats:",
            "§cPhysical Damage: §c+25",
            "§dOmnivamp: §d+10%",
            "",
            "§6Ability: §eBlood Harvest §7(Right-Click)",
            "§7Unleash a devastating arc that deals §b50 §7magic",
            "§7damage to all enemies in a cone in front of you.",
            "§7If 3 or more targets are hit, damage increases to §b80",
            "§7for all targets.",
            "§7Mana Cost: §b40",
            "§7Cooldown: §e10s",
            "",
            "§8Forged from bones of the fallen and etched with",
            "§8runes that hunger for life essence, this weapon",
            "§8drains vitality from all those unfortunate enough",
            "§8to fall under its wicked edge.",
            ""
        ));
        
        // Add identifier for ability system
        NamespacedKey keyAbilityId = new NamespacedKey(Main.getInstance(), "ability_id");
        meta.getPersistentDataContainer().set(keyAbilityId, PersistentDataType.STRING, "blood_harvest");
        
        // Add cooldown tracker
        NamespacedKey keyCooldown = new NamespacedKey(Main.getInstance(), "cooldown");
        meta.getPersistentDataContainer().set(keyCooldown, PersistentDataType.LONG, 0L);
        
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createShatteredShellPickaxe() {
        ItemStack item = new ItemStack(Material.CARROT_ON_A_STICK);
        ItemMeta meta = item.getItemMeta();
        
        // Set custom model data (2XXXXX for functional items, 13000 base for pickaxes)
        meta.setCustomModelData(213001); // 2 for functional item, 13 for pickaxe, 001 for first variant
        
        // Set name and lore
        meta.setDisplayName("§7§lShattered Shell Pickaxe");
        meta.setLore(Arrays.asList(
            "§7Rarity: " + ItemRarity.BASIC.getFormattedName(),
            "§7\"Forged from the fragments of ancient sea creatures,",
            "§7this pickaxe cuts through stone with surprising ease.\"",
            "",
            "§7Stats:",
            "§cPhysical Damage: §c+3",
            "§9Mining Speed: §9+0.1",
            "",
            "§8Its jagged edges, once part of a creature's",
            "§8protective shell, now slice through rock and ore",
            "§8with preternatural sharpness.",
            ""
        ));
        
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createWanderersWeaveHood() {
        ItemStack item = new ItemStack(Material.LEATHER_HELMET);
        ItemMeta meta = item.getItemMeta();
        
        // Set custom model data (2XXXXX for functional items, 30000 base for helmets)
        meta.setCustomModelData(230002); // 2 for functional item, 3 for helmet, 0002 for second variant
        
        // Set name and lore
        meta.setDisplayName("§6§lWanderer's Weave Hood");
        meta.setLore(Arrays.asList(
            "§7Rarity: " + ItemRarity.BASIC.getFormattedName(),
            "§7\"A lightweight hood woven from enchanted cloth",
            "§7that adapts to any climate.\"",
            "",
            "§7Stats:",
            "§cHealth: §c+3",
            "§aArmor: §a+1",
            "",
            "§8This fabric seems to shimmer slightly when touched,",
            "§8as if it contains fragments of distant memories.",
            ""
        ));
                
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createWanderersWeaveTunic() {
        ItemStack item = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        
        // Set custom model data (2XXXXX for functional items, 31000 base for chestplates)
        meta.setCustomModelData(231001); // 2 for functional item, 31 for chestplate, 001 for first variant
        
        // Set name and lore
        meta.setDisplayName("§6§lWanderer's Weave Tunic");
        meta.setLore(Arrays.asList(
            "§7Rarity: " + ItemRarity.BASIC.getFormattedName(),
            "§7\"A comfortable tunic that provides surprising",
            "§7protection despite its light weight.\"",
            "",
            "§7Stats:",
            "§cHealth: §c+3",
            "§aArmor: §a+1",
            "",
            "§8The intricate weaving pattern seems to shift",
            "§8and adapt to the wearer's movements.",
            ""
        ));
            
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createWanderersWeaveLeggings() {
        ItemStack item = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemMeta meta = item.getItemMeta();
        
        // Set custom model data (2XXXXX for functional items, 32000 base for leggings)
        meta.setCustomModelData(232001); // 2 for functional item, 32 for leggings, 001 for first variant
        
        // Set name and lore
        meta.setDisplayName("§6§lWanderer's Weave Pants");
        meta.setLore(Arrays.asList(
            "§7Rarity: " + ItemRarity.BASIC.getFormattedName(),
            "§7\"Durable pants designed for those who journey",
            "§7far across all terrains.\"",
            "",
            "§7Stats:",
            "§cHealth: §c+3",
            "§aArmor: §a+1",
            "",
            "§8The fabric never seems to tear or soil,",
            "§8perfect for extended travels.",
            ""
        ));
               
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createWanderersWeaveBoots() {
        ItemStack item = new ItemStack(Material.LEATHER_BOOTS);
        ItemMeta meta = item.getItemMeta();
        
        // Set custom model data (2XXXXX for functional items, 33000 base for boots)
        meta.setCustomModelData(233001); // 2 for functional item, 33 for boots, 001 for first variant
        
        // Set name and lore
        meta.setDisplayName("§6§lWanderer's Weave Boots");
        meta.setLore(Arrays.asList(
            "§7Rarity: " + ItemRarity.BASIC.getFormattedName(),
            "§7\"Comfortable boots that make long journeys",
            "§7feel like a casual stroll.\"",
            "",
            "§7Stats:",
            "§cHealth: §c+3",
            "§aArmor: §a+1",
            "",
            "§8These boots seem to always find the easiest path,",
            "§8guiding their wearer through difficult terrain.",
            ""
        ));
              
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createRustyCrumbledPickaxe() {
        ItemStack item = new ItemStack(Material.CARROT_ON_A_STICK);
        ItemMeta meta = item.getItemMeta();
        
        // Set custom model data (2XXXXX for functional items, 13000 base for pickaxes)
        meta.setCustomModelData(213002); // 2 for functional item, 13 for pickaxe, 002 for second variant
        
        // Set name and lore
        meta.setDisplayName("§7§lRusty-Crumbled Pickaxe");
        meta.setLore(Arrays.asList(
            "§7Rarity: " + ItemRarity.BASIC.getFormattedName(),
            "§7\"An old mining tool showing signs of wear,",
            "§7yet still functional for basic excavation work.\"",
            "",
            "§7Stats:",
            "§9Mining Speed: §9+0.1",
            "§6Mining Fortune: §6+1",
            "",
            "§8Despite its weathered appearance, this pickaxe",
            "§8has seen countless hours in the mines and",
            "§8still serves its purpose with reliable durability.",
            ""
        ));
        
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createRootCrackedPickaxe() {
        ItemStack item = new ItemStack(Material.CARROT_ON_A_STICK);
        ItemMeta meta = item.getItemMeta();
        
        // Set custom model data (2XXXXX for functional items, 13000 base for pickaxes)
        meta.setCustomModelData(213003); // 2 for functional item, 13 for pickaxe, 003 for third variant
        
        // Set name and lore
        meta.setDisplayName("§7§lRoot-Cracked Pickaxe");
        meta.setLore(Arrays.asList(
            "§7Rarity: " + ItemRarity.BASIC.getFormattedName(),
            "§7\"A pickaxe with a wooden handle split by",
            "§7ancient tree roots, surprisingly enhancing its grip.\"",
            "",
            "§7Stats:",
            "§9Mining Speed: §9+0.2",
            "§6Mining Fortune: §6+2",
            "",
            "§8The organic cracks in the handle seem to channel",
            "§8natural energy, making each swing more precise",
            "§8and effective than expected.",
            ""
        ));
        
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCopperheadPickaxe() {
        ItemStack item = new ItemStack(Material.CARROT_ON_A_STICK);
        ItemMeta meta = item.getItemMeta();
        
        // Set custom model data (2XXXXX for functional items, 13000 base for pickaxes)
        meta.setCustomModelData(213004); // 2 for functional item, 13 for pickaxe, 004 for fourth variant
        
        // Set name and lore
        meta.setDisplayName("§7§lCopperhead Pickaxe");
        meta.setLore(Arrays.asList(
            "§7Rarity: " + ItemRarity.BASIC.getFormattedName(),
            "§7\"A sturdy pickaxe with a copper-tinted head",
            "§7that gleams with hidden potential.\"",
            "",
            "§7Stats:",
            "§cPhysical Damage: §c+5",
            "§9Mining Speed: §9+0.3",
            "§6Mining Fortune: §6+5",
            "",
            "§8The copper alloy head not only enhances mining",
            "§8efficiency but also makes this tool effective",
            "§8as a weapon when the situation demands it.",
            ""
        ));
        
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createForgedCopperPickaxe() {
        ItemStack item = new ItemStack(Material.CARROT_ON_A_STICK);
        ItemMeta meta = item.getItemMeta();
        
        // Set custom model data (2XXXXX for functional items, 13000 base for pickaxes)
        meta.setCustomModelData(213005); // 2 for functional item, 13 for pickaxe, 005 for fifth variant
        
        // Set name and lore
        meta.setDisplayName("§7§lForged Copper Pickaxe");
        meta.setLore(Arrays.asList(
            "§7Rarity: " + ItemRarity.BASIC.getFormattedName(),
            "§7\"A masterfully crafted pickaxe forged from",
            "§7refined copper, balanced for both combat and mining.\"",
            "",
            "§7Stats:",
            "§cPhysical Damage: §c+7",
            "§9Mining Speed: §9+0.3",
            "§6Mining Fortune: §6+7",
            "",
            "§8Expertly forged in the fires of experienced",
            "§8smiths, this pickaxe represents the pinnacle",
            "§8of copper craftsmanship and utility.",
            ""
        ));
        
        item.setItemMeta(meta);
        return item;
    }

    
}