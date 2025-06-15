package com.server.enchanting;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.ChiseledBookshelf;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.enchanting.CustomEnchantment.EnchantmentCategory;

/**
 * Calculates enchanting levels based on nearby bookshelves and rune books
 */
public class EnchantingLevelCalculator {
    
    // Constants for enchanting setup
    private static final int MAX_VANILLA_LEVEL = 30;
    private static final int MAX_ENCHANTING_LEVEL = 1000;
    private static final int MAX_BOOKSHELF_DISTANCE = 2; // 2 block radius around enchanting table
    private static final int MAX_CHISELED_BOOKSHELVES = 32; // Maximum chiseled bookshelves that count
    
    /**
     * Calculate the total enchanting level available at a location
     */
    public static EnchantingLevel calculateEnchantingLevel(Location enchantingTableLocation) {
        if (enchantingTableLocation == null) {
            return new EnchantingLevel(0, 0, new ArrayList<>(), new ArrayList<>());
        }
        
        List<Location> vanillaBookshelves = new ArrayList<>();
        List<ChiseledBookshelfData> chiseledBookshelves = new ArrayList<>();
        
        // Scan area around enchanting table
        scanBookshelvesInRadius(enchantingTableLocation, vanillaBookshelves, chiseledBookshelves);
        
        // Calculate vanilla level (1-30)
        int vanillaLevel = Math.min(vanillaBookshelves.size(), 15); // 15 bookshelves = level 30
        
        // Calculate advanced level from rune books
        int advancedLevel = calculateAdvancedLevel(chiseledBookshelves);
        
        // Total enchanting level
        int totalLevel = Math.min(vanillaLevel * 2 + advancedLevel, MAX_ENCHANTING_LEVEL);
        
        if (Main.getInstance().isDebugEnabled(DebugSystem.GUI)) {
            Main.getInstance().debugLog(DebugSystem.GUI,
                "[Enchanting Level] Location: " + enchantingTableLocation + 
                ", Vanilla: " + vanillaLevel + ", Advanced: " + advancedLevel + 
                ", Total: " + totalLevel);
        }
        
        return new EnchantingLevel(totalLevel, vanillaLevel, vanillaBookshelves, chiseledBookshelves);
    }
    
    /**
     * Scan for bookshelves in radius around enchanting table
     */
    private static void scanBookshelvesInRadius(Location center, List<Location> vanillaBookshelves, 
                                              List<ChiseledBookshelfData> chiseledBookshelves) {
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        
        // Check all blocks in radius
        for (int x = centerX - MAX_BOOKSHELF_DISTANCE; x <= centerX + MAX_BOOKSHELF_DISTANCE; x++) {
            for (int y = centerY - 1; y <= centerY + 1; y++) { // Only check same level and ±1
                for (int z = centerZ - MAX_BOOKSHELF_DISTANCE; z <= centerZ + MAX_BOOKSHELF_DISTANCE; z++) {
                    Location loc = new Location(center.getWorld(), x, y, z);
                    Block block = loc.getBlock();
                    
                    // Skip the enchanting table itself
                    if (loc.equals(center)) {
                        continue;
                    }
                    
                    // Check for vanilla bookshelf
                    if (block.getType() == Material.BOOKSHELF) {
                        if (hasLineOfSight(center, loc)) {
                            vanillaBookshelves.add(loc);
                        }
                    }
                    
                    // Check for chiseled bookshelf
                    else if (block.getType() == Material.CHISELED_BOOKSHELF) {
                        if (hasLineOfSight(center, loc) && chiseledBookshelves.size() < MAX_CHISELED_BOOKSHELVES) {
                            ChiseledBookshelfData data = analyzeChiseledBookshelf(loc);
                            if (data.hasRuneBooks()) {
                                chiseledBookshelves.add(data);
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Check if there's a clear line of sight between enchanting table and bookshelf
     */
    private static boolean hasLineOfSight(Location enchantingTable, Location bookshelf) {
        // For simplicity, we'll check if there are no solid blocks between them
        // This is a basic implementation - could be enhanced for more complex scenarios
        
        Location start = enchantingTable.clone().add(0.5, 0.5, 0.5);
        Location end = bookshelf.clone().add(0.5, 0.5, 0.5);
        
        // Simple air block check - more sophisticated ray tracing could be implemented
        return true; // For now, assume line of sight is always clear
    }
    
    /**
     * Analyze a chiseled bookshelf to get rune book data
     */
    private static ChiseledBookshelfData analyzeChiseledBookshelf(Location location) {
        Block block = location.getBlock();
        BlockState state = block.getState();
        
        if (!(state instanceof ChiseledBookshelf)) {
            return new ChiseledBookshelfData(location, new ArrayList<>());
        }
        
        ChiseledBookshelf bookshelf = (ChiseledBookshelf) state;
        List<RuneBook> runeBooks = new ArrayList<>();
        
        // Check all 6 slots in the chiseled bookshelf
        for (int slot = 0; slot < 6; slot++) {
            ItemStack item = bookshelf.getInventory().getItem(slot);
            if (item != null && RuneBook.isRuneBook(item)) {
                RuneBook runeBook = RuneBook.fromItem(item);
                if (runeBook != null) {
                    runeBooks.add(runeBook);
                }
            }
        }
        
        return new ChiseledBookshelfData(location, runeBooks);
    }
    
    /**
     * Calculate advanced enchanting level from rune books
     */
    private static int calculateAdvancedLevel(List<ChiseledBookshelfData> chiseledBookshelves) {
        int totalPower = 0;
        
        for (ChiseledBookshelfData bookshelf : chiseledBookshelves) {
            for (RuneBook runeBook : bookshelf.getRuneBooks()) {
                totalPower += runeBook.getEnchantingPower();
            }
        }
        
        // Convert power to levels (every 10 power = 1 level beyond vanilla cap)
        return totalPower / 10;
    }
    
    /**
     * Calculate category-specific bonuses for enchanting
     */
    public static double getCategoryBonus(EnchantingLevel enchantingLevel, EnchantmentCategory category) {
        double bonus = 0.0;
        
        for (ChiseledBookshelfData bookshelf : enchantingLevel.getChiseledBookshelves()) {
            for (RuneBook runeBook : bookshelf.getRuneBooks()) {
                if (runeBook.getSpecialization() == category) {
                    bonus += runeBook.getCategoryBonus();
                }
            }
        }
        
        return bonus;
    }
    
    /**
     * Data structure to hold enchanting level information
     */
    public static class EnchantingLevel {
        private final int totalLevel;
        private final int vanillaLevel;
        private final List<Location> vanillaBookshelves;
        private final List<ChiseledBookshelfData> chiseledBookshelves;
        
        public EnchantingLevel(int totalLevel, int vanillaLevel, List<Location> vanillaBookshelves,
                              List<ChiseledBookshelfData> chiseledBookshelves) {
            this.totalLevel = totalLevel;
            this.vanillaLevel = vanillaLevel;
            this.vanillaBookshelves = new ArrayList<>(vanillaBookshelves);
            this.chiseledBookshelves = new ArrayList<>(chiseledBookshelves);
        }
        
        public int getTotalLevel() { return totalLevel; }
        public int getVanillaLevel() { return vanillaLevel; }
        public int getAdvancedLevel() { return totalLevel - vanillaLevel; }
        public List<Location> getVanillaBookshelves() { return new ArrayList<>(vanillaBookshelves); }
        public List<ChiseledBookshelfData> getChiseledBookshelves() { return new ArrayList<>(chiseledBookshelves); }
        
        public boolean canEnchantAtLevel(int requiredLevel) {
            return totalLevel >= requiredLevel;
        }
        
        public String getFormattedLevel() {
            if (totalLevel <= MAX_VANILLA_LEVEL) {
                return "Level " + totalLevel;
            } else {
                return "Level " + totalLevel + " (Advanced)";
            }
        }
    }
    
    /**
     * Data structure to hold chiseled bookshelf information
     */
    public static class ChiseledBookshelfData {
        private final Location location;
        private final List<RuneBook> runeBooks;
        
        public ChiseledBookshelfData(Location location, List<RuneBook> runeBooks) {
            this.location = location;
            this.runeBooks = new ArrayList<>(runeBooks);
        }
        
        public Location getLocation() { return location; }
        public List<RuneBook> getRuneBooks() { return new ArrayList<>(runeBooks); }
        public boolean hasRuneBooks() { return !runeBooks.isEmpty(); }
        public int getTotalPower() {
            return runeBooks.stream().mapToInt(RuneBook::getEnchantingPower).sum();
        }
    }
}