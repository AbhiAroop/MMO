package com.server.enchantments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.server.enchantments.abilities.EmberVeil;
import com.server.enchantments.abilities.Frostflow;
import com.server.enchantments.abilities.InfernoStrike;
import com.server.enchantments.abilities.defensive.Mistveil;
import com.server.enchantments.abilities.defensive.Terraheart;
import com.server.enchantments.abilities.defensive.Whispers;
import com.server.enchantments.abilities.offensive.BurdenedStone;
import com.server.enchantments.abilities.offensive.Cinderwake;
import com.server.enchantments.abilities.offensive.Dawnstrike;
import com.server.enchantments.abilities.offensive.Deepcurrent;
import com.server.enchantments.abilities.offensive.HollowEdge;
import com.server.enchantments.abilities.offensive.Voltbrand;
import com.server.enchantments.abilities.utility.ArcNexus;
import com.server.enchantments.abilities.utility.AshenVeil;
import com.server.enchantments.abilities.utility.GaleStep;
import com.server.enchantments.abilities.utility.RadiantGrace;
import com.server.enchantments.abilities.utility.Veilborn;
import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;
import com.server.enchantments.elements.HybridElement;

/**
 * Registry for all custom enchantments
 * Singleton pattern for global access
 */
public class EnchantmentRegistry {
    
    private static EnchantmentRegistry instance;
    
    private final Map<String, CustomEnchantment> enchantments = new HashMap<>();
    private final Map<ElementType, List<CustomEnchantment>> byElement = new HashMap<>();
    private final Map<HybridElement, List<CustomEnchantment>> byHybrid = new HashMap<>();
    private final Map<EnchantmentRarity, List<CustomEnchantment>> byRarity = new HashMap<>();
    
    private EnchantmentRegistry() {
        registerEnchantments();
    }
    
    public static EnchantmentRegistry getInstance() {
        if (instance == null) {
            instance = new EnchantmentRegistry();
        }
        return instance;
    }
    
    /**
     * Register all enchantments
     */
    private void registerEnchantments() {
        // Fire enchantments
        register(new EmberVeil());
        register(new InfernoStrike());
        register(new Cinderwake());
        register(new AshenVeil());
        
        // Water enchantments
        register(new Frostflow());
        register(new Deepcurrent());
        register(new Mistveil());
        
        // Earth enchantments
        register(new BurdenedStone());
        register(new Terraheart());
        
        // Air enchantments
        register(new GaleStep());
        register(new Whispers());
        
        // Lightning enchantments
        register(new Voltbrand());
        register(new ArcNexus());
        
        // Shadow enchantments
        register(new HollowEdge());
        register(new Veilborn());
        
        // Light enchantments
        register(new Dawnstrike());
        register(new RadiantGrace());
        
        // TODO: Add more enchantments as they're implemented
        // register(new ThunderClap());  // Lightning
        // register(new VineGrasp());    // Nature
        // register(new Frostbite());    // Ice (Hybrid)
    }
    
    /**
     * Register a single enchantment
     */
    private void register(CustomEnchantment enchant) {
        enchantments.put(enchant.getId(), enchant);
        
        // Index by element
        if (!enchant.isHybrid()) {
            byElement.computeIfAbsent(enchant.getElement(), k -> new ArrayList<>()).add(enchant);
        } else {
            byHybrid.computeIfAbsent(enchant.getHybridElement(), k -> new ArrayList<>()).add(enchant);
        }
        
        // Index by rarity
        byRarity.computeIfAbsent(enchant.getRarity(), k -> new ArrayList<>()).add(enchant);
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
        return Collections.unmodifiableCollection(enchantments.values());
    }
    
    /**
     * Get enchantments by element
     */
    public List<CustomEnchantment> getEnchantmentsByElement(ElementType element) {
        return byElement.getOrDefault(element, Collections.emptyList());
    }
    
    /**
     * Get enchantments by hybrid element
     */
    public List<CustomEnchantment> getEnchantmentsByHybrid(HybridElement hybrid) {
        return byHybrid.getOrDefault(hybrid, Collections.emptyList());
    }
    
    /**
     * Get enchantments by rarity
     */
    public List<CustomEnchantment> getEnchantmentsByRarity(EnchantmentRarity rarity) {
        return byRarity.getOrDefault(rarity, Collections.emptyList());
    }
    
    /**
     * Get enchantments by element and maximum rarity
     */
    public List<CustomEnchantment> getEnchantmentsByElementAndMaxRarity(
            ElementType element, EnchantmentRarity maxRarity) {
        return getEnchantmentsByElement(element).stream()
                .filter(e -> e.getRarity().getTierLevel() <= maxRarity.getTierLevel())
                .collect(Collectors.toList());
    }
    
    /**
     * Select a random enchantment for an element with weighted rarity
     * @param element The element type
     * @param rarityWeights Array of weights [common, uncommon, rare, epic, legendary]
     * @return Random enchantment or null if none available
     */
    public CustomEnchantment getRandomEnchantment(ElementType element, int[] rarityWeights) {
        List<CustomEnchantment> available = getEnchantmentsByElement(element);
        if (available.isEmpty()) return null;
        
        // Roll for rarity first
        EnchantmentRarity targetRarity = rollRarity(rarityWeights);
        
        // Filter by rarity
        List<CustomEnchantment> filtered = available.stream()
                .filter(e -> e.getRarity() == targetRarity)
                .collect(Collectors.toList());
        
        // If none at that rarity, fallback to any rarity
        if (filtered.isEmpty()) {
            filtered = available;
        }
        
        // Random selection
        return filtered.get(new Random().nextInt(filtered.size()));
    }
    
    /**
     * Select a random enchantment for a hybrid element with weighted rarity
     */
    public CustomEnchantment getRandomEnchantment(HybridElement hybrid, int[] rarityWeights) {
        List<CustomEnchantment> available = getEnchantmentsByHybrid(hybrid);
        if (available.isEmpty()) return null;
        
        // Roll for rarity first
        EnchantmentRarity targetRarity = rollRarity(rarityWeights);
        
        // Filter by rarity
        List<CustomEnchantment> filtered = available.stream()
                .filter(e -> e.getRarity() == targetRarity)
                .collect(Collectors.toList());
        
        // If none at that rarity, fallback to any rarity
        if (filtered.isEmpty()) {
            filtered = available;
        }
        
        // Random selection
        return filtered.get(new Random().nextInt(filtered.size()));
    }
    
    /**
     * Roll for rarity based on weighted chances
     */
    private EnchantmentRarity rollRarity(int[] weights) {
        if (weights == null || weights.length != EnchantmentRarity.values().length) {
            return EnchantmentRarity.COMMON;
        }
        
        int totalWeight = 0;
        for (int weight : weights) {
            totalWeight += weight;
        }
        
        int roll = new Random().nextInt(totalWeight);
        int currentWeight = 0;
        
        for (int i = 0; i < weights.length; i++) {
            currentWeight += weights[i];
            if (roll < currentWeight) {
                return EnchantmentRarity.values()[i];
            }
        }
        
        return EnchantmentRarity.COMMON;
    }
    
    /**
     * Check if an enchantment ID exists
     */
    public boolean hasEnchantment(String id) {
        return enchantments.containsKey(id);
    }
    
    /**
     * Get total registered enchantment count
     */
    public int getEnchantmentCount() {
        return enchantments.size();
    }
    
    /**
     * Get count by element
     */
    public int getCountByElement(ElementType element) {
        return getEnchantmentsByElement(element).size();
    }
    
    /**
     * Get count by rarity
     */
    public int getCountByRarity(EnchantmentRarity rarity) {
        return getEnchantmentsByRarity(rarity).size();
    }
}
