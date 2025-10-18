package com.server.profiles.stats;

import java.util.HashMap;
import java.util.Map;

import com.server.enchantments.elements.ElementType;

/**
 * Tracks a player's elemental affinity values based on enchanting history.
 * Affinity(Element) = Σ (Fragment Tier × Fragment Quality × Number of Fragments)
 * 
 * Higher affinity represents deeper investment and mastery in that element.
 */
public class ElementalAffinity {
    
    // Map of element type to affinity value
    private Map<ElementType, Double> affinityMap;
    
    public ElementalAffinity() {
        this.affinityMap = new HashMap<>();
        // Initialize all elements to 0
        for (ElementType element : ElementType.values()) {
            affinityMap.put(element, 0.0);
        }
    }
    
    /**
     * Get affinity value for a specific element
     * @param element The element type
     * @return The affinity value (0+)
     */
    public double getAffinity(ElementType element) {
        return affinityMap.getOrDefault(element, 0.0);
    }
    
    /**
     * Set affinity value for a specific element
     * @param element The element type
     * @param value The affinity value
     */
    public void setAffinity(ElementType element, double value) {
        affinityMap.put(element, Math.max(0, value));
    }
    
    /**
     * Add to affinity value for a specific element
     * Called when fragments are used in enchanting
     * 
     * Formula: Affinity += (fragmentTier × fragmentQuality × fragmentCount)
     * 
     * @param element The element type
     * @param fragmentTierLevel Tier level (1-3 for Basic/Refined/Pristine)
     * @param fragmentQualityValue Quality affinity value (10-100)
     * @param fragmentCount Number of fragments used
     */
    public void addAffinity(ElementType element, int fragmentTierLevel, 
                           int fragmentQualityValue, int fragmentCount) {
        double currentAffinity = getAffinity(element);
        double affinityGain = fragmentTierLevel * fragmentQualityValue * fragmentCount;
        setAffinity(element, currentAffinity + affinityGain);
    }
    
    /**
     * Add to affinity value for a specific element (direct value)
     * Called when scanning equipment enchantments
     * 
     * @param element The element type
     * @param value The affinity value to add
     */
    public void addAffinity(ElementType element, double value) {
        double currentAffinity = getAffinity(element);
        setAffinity(element, currentAffinity + value);
    }
    
    /**
     * Get the element with the highest affinity
     * @return The dominant element type, or null if no affinity
     */
    public ElementType getDominantElement() {
        ElementType dominant = null;
        double maxAffinity = 0;
        
        for (Map.Entry<ElementType, Double> entry : affinityMap.entrySet()) {
            if (entry.getValue() > maxAffinity) {
                maxAffinity = entry.getValue();
                dominant = entry.getKey();
            }
        }
        
        return dominant;
    }
    
    /**
     * Get total affinity across all elements
     * @return Sum of all affinity values
     */
    public double getTotalAffinity() {
        double total = 0;
        for (double affinity : affinityMap.values()) {
            total += affinity;
        }
        return total;
    }
    
    /**
     * Get affinity percentage for an element (relative to total)
     * @param element The element type
     * @return Percentage (0.0 to 1.0)
     */
    public double getAffinityPercentage(ElementType element) {
        double total = getTotalAffinity();
        if (total == 0) return 0.0;
        return getAffinity(element) / total;
    }
    
    /**
     * Get all affinity values as a map
     * @return Map of element to affinity value
     */
    public Map<ElementType, Double> getAllAffinities() {
        return new HashMap<>(affinityMap);
    }
    
    /**
     * Reset all affinities to 0
     */
    public void reset() {
        for (ElementType element : ElementType.values()) {
            affinityMap.put(element, 0.0);
        }
    }
    
    /**
     * Reset all affinities to 0 (alias for reset)
     */
    public void resetAllAffinity() {
        reset();
    }
    
    /**
     * Get affinity tier/rank for an element
     * @param element The element type
     * @return Tier name based on affinity value
     */
    public String getAffinityTier(ElementType element) {
        double affinity = getAffinity(element);
        
        if (affinity == 0) return "§8None";
        if (affinity < 1000) return "§7Novice";
        if (affinity < 5000) return "§fApprentice";
        if (affinity < 15000) return "§aAdept";
        if (affinity < 40000) return "§9Expert";
        if (affinity < 100000) return "§5Master";
        if (affinity < 250000) return "§6Grandmaster";
        return "§cLegend";
    }
}
