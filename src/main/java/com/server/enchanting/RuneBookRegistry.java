package com.server.enchanting;

import java.util.HashMap;
import java.util.Map;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.enchanting.CustomEnchantment.EnchantmentCategory;

/**
 * Registry for all rune book types and their creation
 */
public class RuneBookRegistry {
    
    private static RuneBookRegistry instance;
    private final Map<String, RuneBook> runeBooks;
    
    private RuneBookRegistry() {
        this.runeBooks = new HashMap<>();
        initializeRuneBooks();
    }
    
    public static RuneBookRegistry getInstance() {
        if (instance == null) {
            instance = new RuneBookRegistry();
        }
        return instance;
    }
    
    /**
     * Initialize all rune book combinations
     */
    private void initializeRuneBooks() {
        // Create rune books for each tier and category combination
        for (RuneBook.RuneTier tier : RuneBook.RuneTier.values()) {
            for (EnchantmentCategory category : EnchantmentCategory.values()) {
                createRuneBook(tier, category);
            }
        }
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Rune Book Registry] Initialized " + runeBooks.size() + " rune book types");
        }
    }
    
    /**
     * Create a specific rune book type
     */
    private void createRuneBook(RuneBook.RuneTier tier, EnchantmentCategory category) {
        String id = tier.name().toLowerCase() + "_" + category.name().toLowerCase();
        String displayName = tier.getDisplayName() + " " + category.getDisplayName() + " Codex";
        
        // Calculate enchanting power based on tier
        int power = tier.getBasePower();
        
        // Category bonus (specialization benefit)
        double categoryBonus = tier.getMultiplier() * 0.1; // 10% per tier level
        
        String description = "Contains " + tier.getDescription().toLowerCase() + " about " + 
                           category.getDisplayName().toLowerCase() + " magic";
        
        RuneBook runeBook = new RuneBook(id, displayName, tier, category, power, categoryBonus, description);
        runeBooks.put(id, runeBook);
    }
    
    /**
     * Get a rune book by tier and category
     */
    public RuneBook getRuneBook(RuneBook.RuneTier tier, EnchantmentCategory category) {
        String id = tier.name().toLowerCase() + "_" + category.name().toLowerCase();
        return runeBooks.get(id);
    }
    
    /**
     * Get a rune book by ID
     */
    public RuneBook getRuneBook(String id) {
        return runeBooks.get(id);
    }
    
    /**
     * Get all rune books
     */
    public Map<String, RuneBook> getAllRuneBooks() {
        return new HashMap<>(runeBooks);
    }
}