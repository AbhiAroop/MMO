package com.server.enchantments.data;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;

import com.server.enchantments.elements.ElementType;
import com.server.enchantments.elements.HybridElement;

import de.tr7zw.changeme.nbtapi.NBTItem;

/**
 * Stores enchantment data in item NBT
 * Format: CustomEnchant_<index>_ID, _Quality, _Level, _Stats, _Affinity
 */
public class EnchantmentData {
    
    private static final String NBT_PREFIX = "MMO_Enchant_";
    private static final String NBT_COUNT = "MMO_EnchantCount";
    
    private final String enchantmentId;
    private final EnchantmentQuality quality;
    private final EnchantmentLevel level;
    private final double[] scaledStats;
    private final int affinityValue;
    private final ElementType element;
    private final HybridElement hybridElement;
    private final boolean isHybrid;
    
    public EnchantmentData(String enchantmentId, EnchantmentQuality quality, EnchantmentLevel level,
                          double[] scaledStats, int affinityValue,
                          ElementType element, HybridElement hybridElement, boolean isHybrid) {
        this.enchantmentId = enchantmentId;
        this.quality = quality;
        this.level = level;
        this.scaledStats = scaledStats;
        this.affinityValue = affinityValue;
        this.element = element;
        this.hybridElement = hybridElement;
        this.isHybrid = isHybrid;
    }
    
    public String getEnchantmentId() {
        return enchantmentId;
    }
    
    public EnchantmentQuality getQuality() {
        return quality;
    }
    
    public EnchantmentLevel getLevel() {
        return level;
    }
    
    public double[] getScaledStats() {
        return scaledStats;
    }
    
    public int getAffinityValue() {
        return affinityValue;
    }
    
    public ElementType getElement() {
        return element;
    }
    
    public HybridElement getHybridElement() {
        return hybridElement;
    }
    
    public boolean isHybrid() {
        return isHybrid;
    }
    
    /**
     * Save enchantment data to item NBT (with default Level I)
     */
    public static void addEnchantmentToItem(ItemStack item, CustomEnchantment enchant, 
                                           EnchantmentQuality quality) {
        addEnchantmentToItem(item, enchant, quality, EnchantmentLevel.I);
    }
    
    /**
     * Save enchantment data to item NBT (with specified level)
     */
    public static void addEnchantmentToItem(ItemStack item, CustomEnchantment enchant, 
                                           EnchantmentQuality quality, EnchantmentLevel level) {
        if (item == null) return;
        
        // PRESERVE ORIGINAL ITEM META BEFORE NBT OPERATIONS
        org.bukkit.inventory.meta.ItemMeta originalMeta = item.hasItemMeta() ? item.getItemMeta().clone() : null;
        String originalName = null;
        List<String> originalLore = null;
        Integer originalCustomModelData = null;
        
        if (originalMeta != null) {
            if (originalMeta.hasDisplayName()) {
                originalName = originalMeta.getDisplayName();
            }
            if (originalMeta.hasLore()) {
                originalLore = new ArrayList<>(originalMeta.getLore());
            }
            if (originalMeta.hasCustomModelData()) {
                originalCustomModelData = originalMeta.getCustomModelData();
            }
        }
        
        NBTItem nbtItem = new NBTItem(item);
        
        // Get current enchantment count
        int count = nbtItem.hasKey(NBT_COUNT) ? nbtItem.getInteger(NBT_COUNT) : 0;
        
        // Check if this enchantment already exists on the item
        int existingIndex = -1;
        for (int i = 0; i < count; i++) {
            String checkPrefix = NBT_PREFIX + i + "_";
            if (nbtItem.hasKey(checkPrefix + "ID")) {
                String existingId = nbtItem.getString(checkPrefix + "ID");
                if (existingId.equals(enchant.getId())) {
                    existingIndex = i;
                    break;
                }
            }
        }
        
        // If enchantment exists, update it; otherwise add new
        String prefix;
        if (existingIndex >= 0) {
            // Update existing enchantment
            prefix = NBT_PREFIX + existingIndex + "_";
        } else {
            // Add new enchantment
            prefix = NBT_PREFIX + count + "_";
            // Only increment count if we're adding a new enchantment
            nbtItem.setInteger(NBT_COUNT, count + 1);
        }
        
        // Store/Update enchantment data
        nbtItem.setString(prefix + "ID", enchant.getId());
        nbtItem.setString(prefix + "Quality", quality.name());
        nbtItem.setInteger(prefix + "Level", level.getNumericLevel());
        
        // Store scaled stats with BOTH quality and level multipliers
        double[] baseStats = enchant.getBaseStats();
        double qualityMultiplier = quality.getEffectivenessMultiplier();
        double levelMultiplier = level.getPowerMultiplier();
        double combinedMultiplier = qualityMultiplier * levelMultiplier;
        
        double[] scaledStats = new double[baseStats.length];
        for (int i = 0; i < baseStats.length; i++) {
            scaledStats[i] = baseStats[i] * combinedMultiplier;
        }
        
        for (int i = 0; i < scaledStats.length; i++) {
            nbtItem.setDouble(prefix + "Stat_" + i, scaledStats[i]);
        }
        nbtItem.setInteger(prefix + "StatCount", scaledStats.length);
        
        // Store affinity value
        nbtItem.setInteger(prefix + "Affinity", quality.getAffinityValue());
        
        // Store element info
        if (enchant.isHybrid()) {
            nbtItem.setString(prefix + "Type", "HYBRID");
            nbtItem.setString(prefix + "Hybrid", enchant.getHybridElement().name());
        } else {
            nbtItem.setString(prefix + "Type", "ELEMENT");
            nbtItem.setString(prefix + "Element", enchant.getElement().name());
        }
        
        // Apply NBT back to item
        ItemStack nbtAppliedItem = nbtItem.getItem();
        item.setItemMeta(nbtAppliedItem.getItemMeta());
        
        // RESTORE ORIGINAL META (name, lore, custom model data)
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (originalName != null) {
                meta.setDisplayName(originalName);
            }
            if (originalLore != null) {
                meta.setLore(originalLore);
            }
            if (originalCustomModelData != null) {
                meta.setCustomModelData(originalCustomModelData);
            }
            item.setItemMeta(meta);
        }
        
        // NOW UPDATE THE LORE TO ADD ENCHANTMENT (preserves original lore)
        updateItemLore(item);
    }
    
    /**
     * Updates the item's lore to display all enchantments.
     * PRESERVES existing lore that isn't enchantment-related.
     */
    public static void updateItemLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        
        // Find and remove ONLY the enchantment section
        // Look for the specific enchantment header format: "§m          §r §6⚔ Enchantments §r§m          §r"
        int enchantSectionStart = -1;
        int enchantSectionEnd = -1;
        
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            String stripped = org.bukkit.ChatColor.stripColor(line);
            
            // Find enchantment header - look for "Enchantments" with decorative formatting
            if (stripped != null && stripped.contains("Enchantments") && line.contains("§m")) {
                // Check if there's an empty line before this header
                if (i > 0 && lore.get(i - 1).trim().isEmpty()) {
                    enchantSectionStart = i - 1; // Include the empty line before header
                } else {
                    enchantSectionStart = i; // Start from the header itself
                }
            }
            // If we found the start, look for where enchantment section ends
            else if (enchantSectionStart >= 0 && enchantSectionEnd == -1) {
                // Enchantment section ends when:
                // 1. We hit another section header (contains §m but not "Enchantments")
                // 2. We hit an empty line followed by non-enchantment content
                // 3. We reach end of lore
                
                String currentStripped = org.bukkit.ChatColor.stripColor(line);
                if (currentStripped != null && line.contains("§m") && !currentStripped.contains("Enchantments")) {
                    // Another section header found - don't include the empty line before it
                    enchantSectionEnd = i;
                    if (i > 0 && lore.get(i - 1).trim().isEmpty()) {
                        enchantSectionEnd = i - 1;
                    }
                    break;
                }
            }
        }
        
        // If we found the start but not the end, enchantments go to end of lore
        if (enchantSectionStart >= 0 && enchantSectionEnd == -1) {
            enchantSectionEnd = lore.size();
        }
        
        // Remove old enchantment section if found
        if (enchantSectionStart >= 0 && enchantSectionEnd > enchantSectionStart) {
            for (int i = enchantSectionEnd - 1; i >= enchantSectionStart; i--) {
                lore.remove(i);
            }
        }
        
        // Get all enchantments from NBT
        List<EnchantmentData> enchantments = getEnchantmentsFromItem(item);
        
        if (!enchantments.isEmpty()) {
            // Add enchantment section at the END of existing lore
            lore.add("");
            lore.add(org.bukkit.ChatColor.GRAY + "§m          §r " + 
                    org.bukkit.ChatColor.GOLD + "⚔ Enchantments §r" + 
                    org.bukkit.ChatColor.GRAY + "§m          §r");
            
            // Add each enchantment
            for (EnchantmentData data : enchantments) {
                // Get the enchantment from registry
                com.server.enchantments.EnchantmentRegistry registry = 
                    com.server.enchantments.EnchantmentRegistry.getInstance();
                CustomEnchantment enchant = registry.getEnchantment(data.getEnchantmentId());
                
                if (enchant != null) {
                    EnchantmentQuality quality = data.getQuality();
                    String elementIcon = "";
                    
                    if (data.isHybrid() && data.getHybridElement() != null) {
                        elementIcon = data.getHybridElement().getIcon() + " ";
                    } else if (data.getElement() != null) {
                        elementIcon = data.getElement().getIcon() + " ";
                    }
                    
                    EnchantmentLevel level = data.getLevel();
                    
                    // Enchantment name line with level and quality
                    // Format: "• 🔥 Ember Veil III [Legendary]"
                    lore.add(org.bukkit.ChatColor.GRAY + "• " + elementIcon + 
                            enchant.getDisplayName() + " " +
                            level.getDisplayName() + " " +
                            quality.getColor() + "[" + quality.getDisplayName() + "]");
                    
                    // Description
                    lore.add(org.bukkit.ChatColor.DARK_GRAY + "  " + enchant.getDescription());
                }
            }
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
    
    /**
     * Get all enchantments from an item
     */
    public static List<EnchantmentData> getEnchantmentsFromItem(ItemStack item) {
        List<EnchantmentData> enchantments = new ArrayList<>();
        if (item == null) return enchantments;
        
        NBTItem nbtItem = new NBTItem(item);
        
        if (!nbtItem.hasKey(NBT_COUNT)) return enchantments;
        
        int count = nbtItem.getInteger(NBT_COUNT);
        
        for (int i = 0; i < count; i++) {
            String prefix = NBT_PREFIX + i + "_";
            
            if (!nbtItem.hasKey(prefix + "ID")) continue;
            
            String id = nbtItem.getString(prefix + "ID");
            EnchantmentQuality quality = EnchantmentQuality.valueOf(
                nbtItem.getString(prefix + "Quality"));
            
            // Load level (default to I if not present for backwards compatibility)
            EnchantmentLevel level = EnchantmentLevel.I;
            if (nbtItem.hasKey(prefix + "Level")) {
                level = EnchantmentLevel.fromNumeric(nbtItem.getInteger(prefix + "Level"));
            }
            
            // Load scaled stats
            int statCount = nbtItem.getInteger(prefix + "StatCount");
            double[] stats = new double[statCount];
            for (int j = 0; j < statCount; j++) {
                stats[j] = nbtItem.getDouble(prefix + "Stat_" + j);
            }
            
            int affinity = nbtItem.getInteger(prefix + "Affinity");
            
            // Load element info
            String type = nbtItem.getString(prefix + "Type");
            ElementType element = null;
            HybridElement hybrid = null;
            boolean isHybrid = false;
            
            if ("HYBRID".equals(type)) {
                hybrid = HybridElement.valueOf(nbtItem.getString(prefix + "Hybrid"));
                isHybrid = true;
            } else {
                element = ElementType.valueOf(nbtItem.getString(prefix + "Element"));
            }
            
            enchantments.add(new EnchantmentData(id, quality, level, stats, affinity, 
                                                element, hybrid, isHybrid));
        }
        
        return enchantments;
    }
    
    /**
     * Check if item has any custom enchantments
     */
    public static boolean hasEnchantments(ItemStack item) {
        if (item == null) return false;
        NBTItem nbtItem = new NBTItem(item);
        return nbtItem.hasKey(NBT_COUNT) && nbtItem.getInteger(NBT_COUNT) > 0;
    }
    
    /**
     * Get enchantment count on item
     */
    public static int getEnchantmentCount(ItemStack item) {
        if (item == null) return 0;
        NBTItem nbtItem = new NBTItem(item);
        return nbtItem.hasKey(NBT_COUNT) ? nbtItem.getInteger(NBT_COUNT) : 0;
    }
    
    /**
     * Remove all enchantments from item
     */
    public static void clearEnchantments(ItemStack item) {
        if (item == null) return;
        
        NBTItem nbtItem = new NBTItem(item);
        
        if (!nbtItem.hasKey(NBT_COUNT)) return;
        
        int count = nbtItem.getInteger(NBT_COUNT);
        
        // Remove all enchantment keys
        for (int i = 0; i < count; i++) {
            String prefix = NBT_PREFIX + i + "_";
            nbtItem.removeKey(prefix + "ID");
            nbtItem.removeKey(prefix + "Quality");
            nbtItem.removeKey(prefix + "Affinity");
            nbtItem.removeKey(prefix + "Type");
            nbtItem.removeKey(prefix + "Element");
            nbtItem.removeKey(prefix + "Hybrid");
            nbtItem.removeKey(prefix + "StatCount");
            
            // Remove stats
            for (int j = 0; j < 10; j++) { // Assume max 10 stats
                nbtItem.removeKey(prefix + "Stat_" + j);
            }
        }
        
        nbtItem.removeKey(NBT_COUNT);
        nbtItem.applyNBT(item);
    }
}
