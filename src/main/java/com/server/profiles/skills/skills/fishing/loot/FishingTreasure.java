package com.server.profiles.skills.skills.fishing.loot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.profiles.skills.skills.fishing.types.FishingType;

/**
 * Represents treasure and junk items that can be fished up
 */
public enum FishingTreasure {
    // JUNK ITEMS (Common)
    STICK(Material.STICK, TreasureType.JUNK, 0.15, "§7Old Stick", Arrays.asList("§8A weathered stick", "§8Fishing Level: Any")),
    LEATHER_BOOTS(Material.LEATHER_BOOTS, TreasureType.JUNK, 0.12, "§7Worn Leather Boots", Arrays.asList("§8Soggy and damaged", "§8Fishing Level: Any")),
    LEATHER(Material.LEATHER, TreasureType.JUNK, 0.10, "§7Wet Leather", Arrays.asList("§8Waterlogged leather", "§8Fishing Level: Any")),
    BONE(Material.BONE, TreasureType.JUNK, 0.10, "§7Ancient Bone", Arrays.asList("§8From the depths", "§8Fishing Level: Any")),
    ROTTEN_FLESH(Material.ROTTEN_FLESH, TreasureType.JUNK, 0.10, "§7Rotten Flesh", Arrays.asList("§8Smells awful", "§8Fishing Level: Any")),
    STRING(Material.STRING, TreasureType.JUNK, 0.10, "§7Tangled String", Arrays.asList("§8Old fishing line?", "§8Fishing Level: Any")),
    BOWL(Material.BOWL, TreasureType.JUNK, 0.08, "§7Wooden Bowl", Arrays.asList("§8Someone's lost lunch", "§8Fishing Level: Any")),
    FISHING_ROD(Material.FISHING_ROD, TreasureType.JUNK, 0.06, "§7Broken Fishing Rod", Arrays.asList("§8Barely functional", "§8Fishing Level: Any")),
    
    // COMMON TREASURES
    IRON_INGOT(Material.IRON_INGOT, TreasureType.COMMON, 0.08, "§fIron Ingot", Arrays.asList("§7A useful metal", "§7Fishing Level: Any")),
    COAL(Material.COAL, TreasureType.COMMON, 0.10, "§fCoal", Arrays.asList("§7Fuel for the forge", "§7Fishing Level: Any")),
    GOLD_NUGGET(Material.GOLD_NUGGET, TreasureType.COMMON, 0.08, "§fGold Nugget", Arrays.asList("§7A small golden piece", "§7Fishing Level: Any")),
    EMERALD(Material.EMERALD, TreasureType.COMMON, 0.04, "§aEmerald", Arrays.asList("§7A valuable gem", "§7Fishing Level: 5+")),
    
    // UNCOMMON TREASURES
    NAME_TAG(Material.NAME_TAG, TreasureType.UNCOMMON, 0.03, "§9Name Tag", Arrays.asList("§7Name your pets!", "§7Fishing Level: 10+")),
    SADDLE(Material.SADDLE, TreasureType.UNCOMMON, 0.03, "§9Saddle", Arrays.asList("§7Ride in style", "§7Fishing Level: 10+")),
    BOW(Material.BOW, TreasureType.UNCOMMON, 0.025, "§9Enchanted Bow", Arrays.asList("§7A hunter's tool", "§7Fishing Level: 15+")),
    ENCHANTED_BOOK(Material.ENCHANTED_BOOK, TreasureType.UNCOMMON, 0.025, "§9Enchanted Book", Arrays.asList("§7Knowledge from the depths", "§7Fishing Level: 15+")),
    EXPERIENCE_BOTTLE(Material.EXPERIENCE_BOTTLE, TreasureType.UNCOMMON, 0.04, "§9Bottle o' Enchanting", Arrays.asList("§7Condensed experience", "§7Fishing Level: 10+")),
    
    // RARE TREASURES
    DIAMOND(Material.DIAMOND, TreasureType.RARE, 0.015, "§5Diamond", Arrays.asList("§7A precious gem!", "§7Fishing Level: 20+")),
    GOLDEN_APPLE(Material.GOLDEN_APPLE, TreasureType.RARE, 0.012, "§5Golden Apple", Arrays.asList("§7Heals and protects", "§7Fishing Level: 20+")),
    HEART_OF_THE_SEA(Material.HEART_OF_THE_SEA, TreasureType.RARE, 0.008, "§5Heart of the Sea", Arrays.asList("§7A mysterious treasure", "§7Fishing Level: 25+"), FishingType.NORMAL_WATER),
    NAUTILUS_SHELL(Material.NAUTILUS_SHELL, TreasureType.RARE, 0.010, "§5Nautilus Shell", Arrays.asList("§7A beautiful shell", "§7Fishing Level: 20+"), FishingType.NORMAL_WATER),
    
    // EPIC TREASURES (Environment-specific)
    TRIDENT(Material.TRIDENT, TreasureType.EPIC, 0.005, "§6§lTrident", Arrays.asList("§7Weapon of the seas!", "§7Fishing Level: 30+"), FishingType.NORMAL_WATER),
    NETHERITE_SCRAP(Material.NETHERITE_SCRAP, TreasureType.EPIC, 0.006, "§6§lNetherite Scrap", Arrays.asList("§7From the fiery depths!", "§7Fishing Level: 30+"), FishingType.LAVA),
    ANCIENT_DEBRIS(Material.ANCIENT_DEBRIS, TreasureType.EPIC, 0.003, "§6§lAncient Debris", Arrays.asList("§7Rare Nether material!", "§7Fishing Level: 35+"), FishingType.LAVA),
    ENCHANTED_GOLDEN_APPLE(Material.ENCHANTED_GOLDEN_APPLE, TreasureType.EPIC, 0.004, "§6§lEnchanted Golden Apple", Arrays.asList("§7The legendary notch apple!", "§7Fishing Level: 35+")),
    ELYTRA(Material.ELYTRA, TreasureType.EPIC, 0.002, "§6§lElytra", Arrays.asList("§7Wings from the void!", "§7Fishing Level: 40+"), FishingType.VOID),
    DRAGON_HEAD(Material.DRAGON_HEAD, TreasureType.EPIC, 0.002, "§6§lDragon Head", Arrays.asList("§7A legendary trophy!", "§7Fishing Level: 40+"), FishingType.VOID);
    
    private final Material material;
    private final TreasureType type;
    private final double baseChance;
    private final String displayName;
    private final List<String> lore;
    private final FishingType requiredEnvironment; // null = any environment
    private static final Random random = new Random();
    
    FishingTreasure(Material material, TreasureType type, double baseChance, String displayName, List<String> lore) {
        this(material, type, baseChance, displayName, lore, null);
    }
    
    FishingTreasure(Material material, TreasureType type, double baseChance, String displayName, List<String> lore, FishingType requiredEnvironment) {
        this.material = material;
        this.type = type;
        this.baseChance = baseChance;
        this.displayName = displayName;
        this.lore = lore;
        this.requiredEnvironment = requiredEnvironment;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public TreasureType getType() {
        return type;
    }
    
    public double getBaseChance() {
        return baseChance;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public List<String> getLore() {
        return lore;
    }
    
    public FishingType getRequiredEnvironment() {
        return requiredEnvironment;
    }
    
    /**
     * Get all treasures available in a specific environment
     */
    public static List<FishingTreasure> getTreasuresForType(FishingType fishingType, TreasureType treasureType) {
        List<FishingTreasure> treasures = new ArrayList<>();
        for (FishingTreasure treasure : values()) {
            if (treasure.getType() == treasureType) {
                // Check if treasure is environment-specific
                if (treasure.getRequiredEnvironment() == null || treasure.getRequiredEnvironment() == fishingType) {
                    treasures.add(treasure);
                }
            }
        }
        return treasures;
    }
    
    /**
     * Try to get a treasure item based on fishing type and luck
     * @param fishingType The type of fishing environment
     * @param treasureBonus Treasure bonus stat (0.0 to 1.0+)
     * @return A FishingTreasure if one should be given, null otherwise
     */
    public static FishingTreasure tryGetTreasure(FishingType fishingType, double treasureBonus) {
        // Base chance for treasure: 17% (can be modified by treasure bonus)
        // treasureBonus of 0.0 = 17%, treasureBonus of 1.0 = 34% (doubled)
        double baseTreasureChance = 0.17 + (treasureBonus * 0.17);
        
        // First check if treasure should spawn at all
        if (random.nextDouble() > baseTreasureChance) {
            return null; // No treasure this time
        }
        
        // Treasure spawns! Now determine which treasure type based on treasure bonus
        // Higher treasure bonus = better chance for rare items
        TreasureType rolledType;
        double typeRoll = random.nextDouble();
        
        // Adjust rarity chances based on treasure bonus
        // These are percentages WITHIN the treasure pool
        double epicChance = 0.05 + (treasureBonus * 0.10); // 5-15% of treasures
        double rareChance = 0.15 + (treasureBonus * 0.15); // 15-30% of treasures
        double uncommonChance = 0.25 + (treasureBonus * 0.10); // 25-35% of treasures
        double commonChance = 0.30 - (treasureBonus * 0.10); // 30-20% of treasures
        double junkChance = 0.25 - (treasureBonus * 0.25); // 25-0% of treasures (decreases with luck)
        
        if (typeRoll < epicChance) {
            rolledType = TreasureType.EPIC;
        } else if (typeRoll < epicChance + rareChance) {
            rolledType = TreasureType.RARE;
        } else if (typeRoll < epicChance + rareChance + uncommonChance) {
            rolledType = TreasureType.UNCOMMON;
        } else if (typeRoll < epicChance + rareChance + uncommonChance + commonChance) {
            rolledType = TreasureType.COMMON;
        } else {
            rolledType = TreasureType.JUNK;
        }
        
        // Get all treasures of this type for the environment
        List<FishingTreasure> possibleTreasures = getTreasuresForType(fishingType, rolledType);
        if (possibleTreasures.isEmpty()) {
            return null;
        }
        
        // Calculate total weight
        double totalWeight = 0.0;
        for (FishingTreasure treasure : possibleTreasures) {
            totalWeight += treasure.getBaseChance();
        }
        
        // Select treasure based on weighted chance
        double selectRoll = random.nextDouble() * totalWeight;
        double cumulativeWeight = 0.0;
        
        for (FishingTreasure treasure : possibleTreasures) {
            cumulativeWeight += treasure.getBaseChance();
            if (selectRoll <= cumulativeWeight) {
                return treasure;
            }
        }
        
        // Fallback
        return possibleTreasures.get(0);
    }
    
    /**
     * Create an ItemStack of this treasure
     */
    public ItemStack toItemStack() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(displayName);
            
            List<String> itemLore = new ArrayList<>(lore);
            itemLore.add("");
            itemLore.add("§8Obtained from: §7Fishing");
            itemLore.add("§8Rarity: " + type.getColoredName());
            
            meta.setLore(itemLore);
            
            // Add random enchantments to some items
            if (type == TreasureType.UNCOMMON || type == TreasureType.RARE || type == TreasureType.EPIC) {
                if (material == Material.BOW) {
                    meta.addEnchant(Enchantment.POWER, 1 + random.nextInt(3), true);
                    if (random.nextDouble() < 0.5) {
                        meta.addEnchant(Enchantment.UNBREAKING, 1 + random.nextInt(2), true);
                    }
                } else if (material == Material.FISHING_ROD && type != TreasureType.JUNK) {
                    meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1 + random.nextInt(2), true);
                    if (random.nextDouble() < 0.5) {
                        meta.addEnchant(Enchantment.UNBREAKING, 1 + random.nextInt(2), true);
                    }
                }
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Treasure type categories
     */
    public enum TreasureType {
        JUNK("§7Junk"),
        COMMON("§fCommon"),
        UNCOMMON("§9Uncommon"),
        RARE("§5Rare"),
        EPIC("§6§lEpic");
        
        private final String coloredName;
        
        TreasureType(String coloredName) {
            this.coloredName = coloredName;
        }
        
        public String getColoredName() {
            return coloredName;
        }
    }
}
