package com.server.enchantments.elements;

import java.util.EnumMap;
import java.util.Map;

/**
 * Holds affinity values split by category (Offense, Defense, Utility).
 * Each player has separate affinity tracking for each element across all three categories.
 * 
 * Example:
 * - Fire Offense: 45 (high damage with fire enchantments)
 * - Fire Defense: 10 (low resistance to fire attacks)
 * - Fire Utility: 0 (no fire-based utility benefits)
 */
public class CategorizedAffinity {
    
    private final Map<ElementType, Integer> offensiveAffinity;
    private final Map<ElementType, Integer> defensiveAffinity;
    private final Map<ElementType, Integer> utilityAffinity;
    
    /**
     * Create a new CategorizedAffinity with all values at 0.
     */
    public CategorizedAffinity() {
        this.offensiveAffinity = new EnumMap<>(ElementType.class);
        this.defensiveAffinity = new EnumMap<>(ElementType.class);
        this.utilityAffinity = new EnumMap<>(ElementType.class);
        
        // Initialize all elements to 0
        for (ElementType element : ElementType.values()) {
            offensiveAffinity.put(element, 0);
            defensiveAffinity.put(element, 0);
            utilityAffinity.put(element, 0);
        }
    }
    
    /**
     * Get offensive affinity for an element.
     * @param element The element type
     * @return Offensive affinity value
     */
    public int getOffensive(ElementType element) {
        return offensiveAffinity.getOrDefault(element, 0);
    }
    
    /**
     * Get defensive affinity for an element.
     * @param element The element type
     * @return Defensive affinity value
     */
    public int getDefensive(ElementType element) {
        return defensiveAffinity.getOrDefault(element, 0);
    }
    
    /**
     * Get utility affinity for an element.
     * @param element The element type
     * @return Utility affinity value
     */
    public int getUtility(ElementType element) {
        return utilityAffinity.getOrDefault(element, 0);
    }
    
    /**
     * Get affinity for a specific element and category.
     * @param element The element type
     * @param category The affinity category
     * @return Affinity value
     */
    public int get(ElementType element, AffinityCategory category) {
        switch (category) {
            case OFFENSE:
                return getOffensive(element);
            case DEFENSE:
                return getDefensive(element);
            case UTILITY:
                return getUtility(element);
            default:
                return 0;
        }
    }
    
    /**
     * Set offensive affinity for an element.
     * @param element The element type
     * @param value The affinity value
     */
    public void setOffensive(ElementType element, int value) {
        offensiveAffinity.put(element, value);
    }
    
    /**
     * Set defensive affinity for an element.
     * @param element The element type
     * @param value The affinity value
     */
    public void setDefensive(ElementType element, int value) {
        defensiveAffinity.put(element, value);
    }
    
    /**
     * Set utility affinity for an element.
     * @param element The element type
     * @param value The affinity value
     */
    public void setUtility(ElementType element, int value) {
        utilityAffinity.put(element, value);
    }
    
    /**
     * Set affinity for a specific element and category.
     * @param element The element type
     * @param category The affinity category
     * @param value The affinity value
     */
    public void set(ElementType element, AffinityCategory category, int value) {
        switch (category) {
            case OFFENSE:
                setOffensive(element, value);
                break;
            case DEFENSE:
                setDefensive(element, value);
                break;
            case UTILITY:
                setUtility(element, value);
                break;
        }
    }
    
    /**
     * Add to offensive affinity for an element.
     * @param element The element type
     * @param amount Amount to add
     */
    public void addOffensive(ElementType element, int amount) {
        offensiveAffinity.put(element, getOffensive(element) + amount);
    }
    
    /**
     * Add to defensive affinity for an element.
     * @param element The element type
     * @param amount Amount to add
     */
    public void addDefensive(ElementType element, int amount) {
        defensiveAffinity.put(element, getDefensive(element) + amount);
    }
    
    /**
     * Add to utility affinity for an element.
     * @param element The element type
     * @param amount Amount to add
     */
    public void addUtility(ElementType element, int amount) {
        utilityAffinity.put(element, getUtility(element) + amount);
    }
    
    /**
     * Add to affinity for a specific element and category.
     * @param element The element type
     * @param category The affinity category
     * @param amount Amount to add
     */
    public void add(ElementType element, AffinityCategory category, int amount) {
        switch (category) {
            case OFFENSE:
                addOffensive(element, amount);
                break;
            case DEFENSE:
                addDefensive(element, amount);
                break;
            case UTILITY:
                addUtility(element, amount);
                break;
        }
    }
    
    /**
     * Reset all affinities to 0.
     */
    public void reset() {
        for (ElementType element : ElementType.values()) {
            offensiveAffinity.put(element, 0);
            defensiveAffinity.put(element, 0);
            utilityAffinity.put(element, 0);
        }
    }
    
    /**
     * Get all offensive affinities.
     * @return Map of element to offensive affinity
     */
    public Map<ElementType, Integer> getAllOffensive() {
        return new EnumMap<>(offensiveAffinity);
    }
    
    /**
     * Get all defensive affinities.
     * @return Map of element to defensive affinity
     */
    public Map<ElementType, Integer> getAllDefensive() {
        return new EnumMap<>(defensiveAffinity);
    }
    
    /**
     * Get all utility affinities.
     * @return Map of element to utility affinity
     */
    public Map<ElementType, Integer> getAllUtility() {
        return new EnumMap<>(utilityAffinity);
    }
    
    /**
     * Get the highest offensive affinity element.
     * @return Element with highest offensive affinity, or null if all are 0
     */
    public ElementType getHighestOffensive() {
        return offensiveAffinity.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    /**
     * Get the highest defensive affinity element.
     * @return Element with highest defensive affinity, or null if all are 0
     */
    public ElementType getHighestDefensive() {
        return defensiveAffinity.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
}
