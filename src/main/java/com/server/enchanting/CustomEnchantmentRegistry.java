package com.server.enchanting;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.enchanting.CustomEnchantment.EnchantmentCategory;
import com.server.enchanting.CustomEnchantment.EnchantmentRarity;
import com.server.enchanting.CustomEnchantment.ItemCategory;

/**
 * Registry for all custom enchantments
 */
public class CustomEnchantmentRegistry {
    
    private static CustomEnchantmentRegistry instance;
    private final Map<String, CustomEnchantment> enchantments;
    
    private CustomEnchantmentRegistry() {
        this.enchantments = new HashMap<>();
        initializeEnchantments();
    }
    
    public static CustomEnchantmentRegistry getInstance() {
        if (instance == null) {
            instance = new CustomEnchantmentRegistry();
        }
        return instance;
    }
    
    /**
     * Initialize all custom enchantments
     */
    private void initializeEnchantments() {
        registerCombatEnchantments();
        registerToolEnchantments();
        registerProtectionEnchantments();
        registerUtilityEnchantments();
        registerMysticalEnchantments();
        registerCursedEnchantments();
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Enchantment Registry] Initialized " + enchantments.size() + " custom enchantments");
        }
    }
    
    /**
     * Register combat enchantments (renamed for originality)
     */
    private void registerCombatEnchantments() {
        // Savagery - Raw damage increase (replaces Brutality)
        registerEnchantment(new CustomEnchantment(
            "savagery", "Savagery", "Increases physical damage by 3-15 per level",
            EnchantmentCategory.COMBAT, 5, EnchantmentRarity.COMMON,
            Arrays.asList(ItemCategory.SWORD, ItemCategory.AXE),
            Arrays.asList(), 5, 25, 1.3
        ));
        
        // Bloodthirst - Life steal (replaces Vampiric)
        registerEnchantment(new CustomEnchantment(
            "bloodthirst", "Bloodthirst", "Heals 1-5 health per level when dealing damage",
            EnchantmentCategory.COMBAT, 5, EnchantmentRarity.UNCOMMON,
            Arrays.asList(ItemCategory.SWORD, ItemCategory.AXE),
            Arrays.asList("soul_harvest"), 8, 40, 1.5
        ));
        
        // Executioner - Increased crit chance and damage
        registerEnchantment(new CustomEnchantment(
            "executioner", "Executioner", "Increases critical chance by 5-25% and critical damage by 0.1-0.5x per level",
            EnchantmentCategory.COMBAT, 5, EnchantmentRarity.RARE,
            Arrays.asList(ItemCategory.SWORD, ItemCategory.BOW),
            Arrays.asList(), 12, 60, 1.8
        ));
        
        // Rampage - Damage increases as health decreases
        registerEnchantment(new CustomEnchantment(
            "rampage", "Rampage", "Gain 2-10% damage per level for every 10% health missing",
            EnchantmentCategory.COMBAT, 5, EnchantmentRarity.EPIC,
            Arrays.asList(ItemCategory.SWORD, ItemCategory.AXE),
            Arrays.asList("guardian_blessing"), 20, 100, 2.0
        ));
        
        // Soul Harvest - Gain essence on kills
        registerEnchantment(new CustomEnchantment(
            "soul_harvest", "Soul Harvest", "Gain 1-5 essence per level when killing enemies",
            EnchantmentCategory.COMBAT, 5, EnchantmentRarity.LEGENDARY,
            Arrays.asList(ItemCategory.SWORD, ItemCategory.AXE),
            Arrays.asList("bloodthirst"), 30, 150, 2.5
        ));
        
        // Voidstrike - Chance to deal % max health damage
        registerEnchantment(new CustomEnchantment(
            "voidstrike", "Voidstrike", "1-5% chance per level to deal 10% of target's max health as bonus damage",
            EnchantmentCategory.COMBAT, 5, EnchantmentRarity.MYTHIC,
            Arrays.asList(ItemCategory.SWORD),
            Arrays.asList(), 50, 250, 3.0
        ));
    }
    
    /**
     * Register tool enchantments (with PlayerStats integration)
     */
    private void registerToolEnchantments() {
        // Prospector - Mining fortune boost
        registerEnchantment(new CustomEnchantment(
            "prospector", "Prospector", "Increases mining fortune by 0.5-2.5x per level",
            EnchantmentCategory.TOOL, 5, EnchantmentRarity.COMMON,
            Arrays.asList(ItemCategory.PICKAXE),
            Arrays.asList(), 8, 30, 1.4
        ));
        
        // Swiftbreak - Mining speed increase
        registerEnchantment(new CustomEnchantment(
            "swiftbreak", "Swiftbreak", "Increases mining speed by 0.2-1.0 per level",
            EnchantmentCategory.TOOL, 5, EnchantmentRarity.UNCOMMON,
            Arrays.asList(ItemCategory.PICKAXE, ItemCategory.SHOVEL, ItemCategory.AXE),
            Arrays.asList(), 10, 40, 1.5
        ));
        
        // Veinbreaker - Breaks connected ore blocks
        registerEnchantment(new CustomEnchantment(
            "veinbreaker", "Veinbreaker", "Breaks 1-5 connected ore blocks per level",
            EnchantmentCategory.TOOL, 5, EnchantmentRarity.RARE,
            Arrays.asList(ItemCategory.PICKAXE),
            Arrays.asList(), 15, 75, 2.0
        ));
        
        // Cultivator - Farming fortune and speed
        registerEnchantment(new CustomEnchantment(
            "cultivator", "Cultivator", "Increases farming fortune by 0.3-1.5x and farming speed by 10-50% per level",
            EnchantmentCategory.TOOL, 5, EnchantmentRarity.UNCOMMON,
            Arrays.asList(ItemCategory.HOE),
            Arrays.asList(), 12, 50, 1.6
        ));
        
        // Lumberjack - Tree cutting efficiency
        registerEnchantment(new CustomEnchantment(
            "lumberjack", "Lumberjack", "Breaks entire trees and increases wood drops by 10-50% per level",
            EnchantmentCategory.TOOL, 5, EnchantmentRarity.RARE,
            Arrays.asList(ItemCategory.AXE),
            Arrays.asList(), 18, 80, 1.8
        ));
        
        // Architect - Build range increase
        registerEnchantment(new CustomEnchantment(
            "architect", "Architect", "Increases build range by 1-5 blocks per level",
            EnchantmentCategory.TOOL, 5, EnchantmentRarity.EPIC,
            Arrays.asList(ItemCategory.PICKAXE, ItemCategory.SHOVEL),
            Arrays.asList(), 25, 120, 2.2
        ));
    }
    
    /**
     * Register protection enchantments
     */
    private void registerProtectionEnchantments() {
        // Aegis - General damage reduction
        registerEnchantment(new CustomEnchantment(
            "aegis", "Aegis", "Reduces all damage taken by 2-10% per level",
            EnchantmentCategory.PROTECTION, 5, EnchantmentRarity.COMMON,
            Arrays.asList(ItemCategory.HELMET, ItemCategory.CHESTPLATE, ItemCategory.LEGGINGS, ItemCategory.BOOTS),
            Arrays.asList(), 10, 35, 1.3
        ));
        
        // Warding - Magic resistance
        registerEnchantment(new CustomEnchantment(
            "warding", "Warding", "Increases magic resistance by 5-25 per level",
            EnchantmentCategory.PROTECTION, 5, EnchantmentRarity.UNCOMMON,
            Arrays.asList(ItemCategory.HELMET, ItemCategory.CHESTPLATE, ItemCategory.LEGGINGS, ItemCategory.BOOTS),
            Arrays.asList(), 12, 45, 1.5
        ));
        
        // Fortification - Armor increase
        registerEnchantment(new CustomEnchantment(
            "fortification", "Fortification", "Increases armor by 3-15 per level",
            EnchantmentCategory.PROTECTION, 5, EnchantmentRarity.COMMON,
            Arrays.asList(ItemCategory.HELMET, ItemCategory.CHESTPLATE, ItemCategory.LEGGINGS, ItemCategory.BOOTS),
            Arrays.asList(), 8, 30, 1.4
        ));
        
        // Regeneration - Health regeneration
        registerEnchantment(new CustomEnchantment(
            "regeneration", "Regeneration", "Regenerates 0.5-2.5 health per level every 3 seconds",
            EnchantmentCategory.PROTECTION, 5, EnchantmentRarity.RARE,
            Arrays.asList(ItemCategory.HELMET, ItemCategory.CHESTPLATE),
            Arrays.asList(), 15, 70, 1.8
        ));
        
        // Guardian Blessing - Damage reduction when low health
        registerEnchantment(new CustomEnchantment(
            "guardian_blessing", "Guardian Blessing", "Take 5-25% less damage per level when below 30% health",
            EnchantmentCategory.PROTECTION, 5, EnchantmentRarity.EPIC,
            Arrays.asList(ItemCategory.CHESTPLATE),
            Arrays.asList("rampage"), 25, 125, 2.5
        ));
        
        // Immortal - Prevents death once per day
        registerEnchantment(new CustomEnchantment(
            "immortal", "Immortal", "Prevents death and restores 20-100% health per level (24h cooldown)",
            EnchantmentCategory.PROTECTION, 5, EnchantmentRarity.MYTHIC,
            Arrays.asList(ItemCategory.CHESTPLATE),
            Arrays.asList(), 100, 500, 4.0
        ));
    }
    
    /**
     * Register utility enchantments
     */
    private void registerUtilityEnchantments() {
        // Swift - Movement speed increase
        registerEnchantment(new CustomEnchantment(
            "swift", "Swift", "Increases movement speed by 5-25% per level",
            EnchantmentCategory.UTILITY, 5, EnchantmentRarity.COMMON,
            Arrays.asList(ItemCategory.BOOTS),
            Arrays.asList(), 8, 25, 1.2
        ));
        
        // Leaping - Jump boost
        registerEnchantment(new CustomEnchantment(
            "leaping", "Leaping", "Increases jump height by 10-50% per level",
            EnchantmentCategory.UTILITY, 5, EnchantmentRarity.UNCOMMON,
            Arrays.asList(ItemCategory.BOOTS),
            Arrays.asList(), 10, 35, 1.4
        ));
        
        // Treasure Hunter - Looting fortune increase
        registerEnchantment(new CustomEnchantment(
            "treasure_hunter", "Treasure Hunter", "Increases looting fortune by 0.2-1.0x per level",
            EnchantmentCategory.UTILITY, 5, EnchantmentRarity.RARE,
            Arrays.asList(ItemCategory.SWORD, ItemCategory.AXE),
            Arrays.asList(), 15, 60, 1.8
        ));
        
        // Angler - Fishing fortune and speed
        registerEnchantment(new CustomEnchantment(
            "angler", "Angler", "Increases fishing fortune by 0.3-1.5x and fishing speed by 20-100% per level",
            EnchantmentCategory.UTILITY, 5, EnchantmentRarity.UNCOMMON,
            Arrays.asList(ItemCategory.TOOL), // For fishing rods (custom item)
            Arrays.asList(), 12, 45, 1.6
        ));
        
        // Lucky - General luck increase
        registerEnchantment(new CustomEnchantment(
            "lucky", "Lucky", "Increases luck by 1-5 points per level",
            EnchantmentCategory.UTILITY, 5, EnchantmentRarity.EPIC,
            Arrays.asList(ItemCategory.HELMET, ItemCategory.CHESTPLATE, ItemCategory.LEGGINGS, ItemCategory.BOOTS),
            Arrays.asList(), 20, 100, 2.0
        ));
        
        // Magnetism - Item attraction
        registerEnchantment(new CustomEnchantment(
            "magnetism", "Magnetism", "Attracts items from 2-10 blocks away per level",
            EnchantmentCategory.UTILITY, 5, EnchantmentRarity.RARE,
            Arrays.asList(ItemCategory.CHESTPLATE),
            Arrays.asList(), 18, 75, 1.9
        ));
    }
    
    /**
     * Register mystical enchantments
     */
    private void registerMysticalEnchantments() {
        // Arcane Power - Mana increase
        registerEnchantment(new CustomEnchantment(
            "arcane_power", "Arcane Power", "Increases maximum mana by 10-50 per level",
            EnchantmentCategory.MYSTICAL, 5, EnchantmentRarity.UNCOMMON,
            Arrays.asList(ItemCategory.HELMET, ItemCategory.STAFF, ItemCategory.WAND),
            Arrays.asList(), 12, 50, 1.5
        ));
        
        // Spell Focus - Cooldown reduction
        registerEnchantment(new CustomEnchantment(
            "spell_focus", "Spell Focus", "Reduces ability cooldowns by 5-25% per level",
            EnchantmentCategory.MYSTICAL, 5, EnchantmentRarity.RARE,
            Arrays.asList(ItemCategory.STAFF, ItemCategory.WAND, ItemCategory.HELMET),
            Arrays.asList(), 15, 75, 1.8
        ));
        
        // Mana Efficiency - Reduced mana costs
        registerEnchantment(new CustomEnchantment(
            "mana_efficiency", "Mana Efficiency", "Reduces mana costs by 3-15% per level",
            EnchantmentCategory.MYSTICAL, 5, EnchantmentRarity.UNCOMMON,
            Arrays.asList(ItemCategory.STAFF, ItemCategory.WAND, ItemCategory.RELIC),
            Arrays.asList(), 10, 40, 1.4
        ));
        
        // Spell Power - Magic damage increase
        registerEnchantment(new CustomEnchantment(
            "spell_power", "Spell Power", "Increases magic damage by 2-10 per level",
            EnchantmentCategory.MYSTICAL, 5, EnchantmentRarity.RARE,
            Arrays.asList(ItemCategory.STAFF, ItemCategory.WAND),
            Arrays.asList(), 18, 80, 2.0
        ));
        
        // Arcane Mastery - Multiple mystical bonuses
        registerEnchantment(new CustomEnchantment(
            "arcane_mastery", "Arcane Mastery", "Grants +5-25 mana, +2-10% spell power, and +1-5% cooldown reduction per level",
            EnchantmentCategory.MYSTICAL, 5, EnchantmentRarity.LEGENDARY,
            Arrays.asList(ItemCategory.STAFF, ItemCategory.RELIC),
            Arrays.asList(), 35, 175, 2.8
        ));
        
        // Reality Rend - Ignores all defenses
        registerEnchantment(new CustomEnchantment(
            "reality_rend", "Reality Rend", "Spells have 2-10% chance per level to ignore all resistances and deal true damage",
            EnchantmentCategory.MYSTICAL, 5, EnchantmentRarity.MYTHIC,
            Arrays.asList(ItemCategory.STAFF, ItemCategory.WAND),
            Arrays.asList(), 60, 300, 3.5
        ));
    }
    
    /**
     * Register cursed enchantments (high risk, high reward)
     */
    private void registerCursedEnchantments() {
        // Glass Cannon - High damage, low defense
        registerEnchantment(new CustomEnchantment(
            "glass_cannon", "Glass Cannon", "Increases damage by 10-50% per level but reduces max health by 5-25%",
            EnchantmentCategory.CURSED, 5, EnchantmentRarity.EPIC,
            Arrays.asList(ItemCategory.SWORD, ItemCategory.AXE, ItemCategory.BOW),
            Arrays.asList(), 25, 100, 2.0
        ));
        
        // Berserker's Curse - Damage increases with missing health but can't heal
        registerEnchantment(new CustomEnchantment(
            "berserkers_curse", "Berserker's Curse", "Gain 3-15% damage per level for every 10% health missing, but natural healing is reduced by 50%",
            EnchantmentCategory.CURSED, 5, EnchantmentRarity.LEGENDARY,
            Arrays.asList(ItemCategory.SWORD, ItemCategory.AXE),
            Arrays.asList(), 40, 200, 2.5
        ));
        
        // Soul Bind - Massive bonuses but item is lost on death
        registerEnchantment(new CustomEnchantment(
            "soul_bind", "Soul Bind", "Grants massive stat bonuses (+50% all stats per level) but item is destroyed on death",
            EnchantmentCategory.CURSED, 3, EnchantmentRarity.MYTHIC,
            Arrays.asList(ItemCategory.SWORD, ItemCategory.STAFF, ItemCategory.CHESTPLATE),
            Arrays.asList(), 100, 500, 3.0
        ));
        
        // Chaos Strike - Random damage multiplier
        registerEnchantment(new CustomEnchantment(
            "chaos_strike", "Chaos Strike", "Damage varies randomly from 50% to 150-300% per level of normal damage",
            EnchantmentCategory.CURSED, 5, EnchantmentRarity.RARE,
            Arrays.asList(ItemCategory.SWORD, ItemCategory.AXE, ItemCategory.BOW),
            Arrays.asList(), 20, 80, 1.8
        ));
        
        // Mana Burn - Convert health to mana
        registerEnchantment(new CustomEnchantment(
            "mana_burn", "Mana Burn", "Converts 2-10 health per level to 5-25 mana when casting spells",
            EnchantmentCategory.CURSED, 5, EnchantmentRarity.EPIC,
            Arrays.asList(ItemCategory.STAFF, ItemCategory.WAND),
            Arrays.asList(), 30, 150, 2.3
        ));
        
        // Void Touched - Powerful but damages wearer
        registerEnchantment(new CustomEnchantment(
            "void_touched", "Void Touched", "Grants +10-50% all damage per level but deals 1-5 true damage to wearer every 10 seconds",
            EnchantmentCategory.CURSED, 5, EnchantmentRarity.MYTHIC,
            Arrays.asList(ItemCategory.HELMET, ItemCategory.CHESTPLATE, ItemCategory.RELIC),
            Arrays.asList(), 80, 400, 3.2
        ));
    }
    
    /**
     * Register an enchantment
     */
    public void registerEnchantment(CustomEnchantment enchantment) {
        enchantments.put(enchantment.getId(), enchantment);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Enchantment Registry] Registered " + enchantment.getDisplayName() + 
                " (" + enchantment.getRarity().getDisplayName() + ")");
        }
    }
    
    /**
     * Get enchantment by ID
     */
    public CustomEnchantment getEnchantment(String id) {
        return enchantments.get(id);
    }
    
    /**
     * Get all enchantments
     */
    public Collection<CustomEnchantment> getAllEnchantments() {
        return enchantments.values();
    }
    
    /**
     * Get enchantments applicable to an item
     */
    public List<CustomEnchantment> getApplicableEnchantments(ItemStack item) {
        return enchantments.values().stream()
                .filter(enchantment -> enchantment.canApplyTo(item))
                .collect(Collectors.toList());
    }
    
    /**
     * Get enchantments by category
     */
    public List<CustomEnchantment> getEnchantmentsByCategory(EnchantmentCategory category) {
        return enchantments.values().stream()
                .filter(enchantment -> enchantment.getCategory() == category)
                .collect(Collectors.toList());
    }
    
    /**
     * Get enchantments by rarity
     */
    public List<CustomEnchantment> getEnchantmentsByRarity(EnchantmentRarity rarity) {
        return enchantments.values().stream()
                .filter(enchantment -> enchantment.getRarity() == rarity)
                .collect(Collectors.toList());
    }
    
    /**
     * Check if an enchantment exists
     */
    public boolean hasEnchantment(String id) {
        return enchantments.containsKey(id);
    }
    
    /**
     * Remove an enchantment (for admin commands)
     */
    public boolean removeEnchantment(String id) {
        return enchantments.remove(id) != null;
    }
}