package com.server.entities.npc.behaviors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import com.server.Main;
import com.server.entities.npc.NPCStats;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;

/**
 * Behavior for NPCs that can equip and use items
 */
public class EquipmentBehavior implements NPCBehavior {

    private NPC npc;
    private NPCStats stats;
    private Equipment equipment;
    private Map<Equipment.EquipmentSlot, ItemStack> equippedItems = new HashMap<>();
    
    @Override
    public void initialize(NPC npc) {
        this.npc = npc;
        
        // Load stats from metadata or create default stats
        this.stats = loadStatsFromMetadata();
        
        // Get the equipment trait
        this.equipment = npc.getOrAddTrait(Equipment.class);
        
        // Load currently equipped items
        loadEquippedItems();
    }

    @Override
    public void update() {
        // Nothing to do on regular updates
    }

    @Override
    public boolean onDamage(Entity source, double amount) {
        // Let other behaviors handle damage
        return false;
    }

    @Override
    public void onInteract(Player player, boolean isRightClick) {
        // Nothing to do on interact
    }

    @Override
    public void cleanup() {
        // Nothing to clean up
    }

    @Override
    public int getPriority() {
        // Low priority as this is a passive behavior
        return 2;
    }
    
    /**
     * Equip an item in a specific slot
     * 
     * @param slot The equipment slot
     * @param item The item to equip
     * @return True if the item was equipped successfully
     */
    public boolean equipItem(Equipment.EquipmentSlot slot, ItemStack item) {
        if (!npc.isSpawned() || equipment == null) return false;
        
        // Store the item
        equippedItems.put(slot, item);
        
        // Equip on the NPC
        equipment.set(slot, item);
        
        // Update NPC stats based on item
        applyItemStatsToNPC(stats, item, slot);
        
        // Store updated stats in metadata
        saveStatsToMetadata();
        
        return true;
    }
    
    /**
     * Get all equipment currently equipped by the NPC
     * 
     * @return A map of equipment slots to items
     */
    public Map<Equipment.EquipmentSlot, ItemStack> getEquippedItems() {
        Map<Equipment.EquipmentSlot, ItemStack> equipment = new HashMap<>();
        
        if (npc == null || !npc.isSpawned()) {
            return equipment;
        }
        
        // Get the equipment trait
        Equipment equipmentTrait = npc.getOrAddTrait(Equipment.class);
        if (equipmentTrait == null) {
            return equipment;
        }
        
        // Log debug info
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("Getting equipped items for NPC " + npc.getName());
        }
        
        // Get items from all slots
        for (Equipment.EquipmentSlot slot : Equipment.EquipmentSlot.values()) {
            ItemStack item = equipmentTrait.get(slot);
            if (item != null && item.getType() != Material.AIR) {
                equipment.put(slot, item);
                
                // Log the item found
                if (Main.getInstance().isDebugMode()) {
                    String itemInfo = item.getType().name();
                    if (item.hasItemMeta()) {
                        if (item.getItemMeta().hasDisplayName()) {
                            itemInfo = item.getItemMeta().getDisplayName();
                        }
                        if (item.getItemMeta().hasCustomModelData()) {
                            itemInfo += " (Model:" + item.getItemMeta().getCustomModelData() + ")";
                        }
                    }
                    Main.getInstance().getLogger().info("  Found item in slot " + slot + ": " + itemInfo);
                }
            }
        }
        
        // Log the total number of items found
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("Found " + equipment.size() + " equipped items for NPC " + npc.getName());
        }
        
        return equipment;
    }
    
    /**
     * Get item in a specific slot
     * 
     * @param slot The equipment slot
     * @return The equipped item, or null if none
     */
    public ItemStack getEquippedItem(Equipment.EquipmentSlot slot) {
        return equippedItems.get(slot);
    }
    
    /**
     * Apply item stats to the NPC's stats
     * 
     * @param stats The stats object to update
     * @param item The item with stats
     * @param slot The equipment slot
     */
    public void applyItemStatsToNPC(NPCStats stats, ItemStack item, Equipment.EquipmentSlot slot) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        // Special handling for specific items
        if (item.getItemMeta().hasCustomModelData()) {
            int modelData = item.getItemMeta().getCustomModelData();
            
            if (modelData == 250001) { // Arcloom
                int physDamageBonus = 35;
                double attackRangeBonus = 1.0;
                
                stats.setPhysicalDamage(stats.getPhysicalDamage() + physDamageBonus);
                stats.setAttackRange(stats.getAttackRange() + attackRangeBonus);
                
                if (Main.getInstance().isDebugMode()) {
                    Main.getInstance().getLogger().info("Applied Arcloom stats to NPC: Physical Damage +" + 
                                                    physDamageBonus + ", Attack Range +" + attackRangeBonus);
                }
                return;
            }
        }
        
        // If we don't have lore, we're done
        if (!item.getItemMeta().hasLore()) {
            return;
        }
        
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) return;
        
        // Log the item being processed
        if (Main.getInstance().isDebugMode()) {
            String itemName = item.getItemMeta().hasDisplayName() ? 
                item.getItemMeta().getDisplayName() : item.getType().name();
            Main.getInstance().getLogger().info("Processing item stats for NPC: " + itemName);
        }
        
        for (String line : lore) {
            // Remove color codes for parsing
            String cleanLine = line.replaceAll("§[0-9a-fA-Fk-oK-OrR]", "").trim();
            
            // Debug the lore line we're processing
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().info("Parsing lore line: '" + cleanLine + "'");
            }
            
            try {
                // Physical damage (weapons)
                if (cleanLine.startsWith("Physical Damage: +")) {
                    int amount = Integer.parseInt(cleanLine.substring("Physical Damage: +".length()).trim());
                    stats.setPhysicalDamage(stats.getPhysicalDamage() + amount);
                    
                    if (Main.getInstance().isDebugMode()) {
                        Main.getInstance().getLogger().info("  Added Physical Damage: +" + amount);
                    }
                }
                
                // Health (any equipment)
                else if (cleanLine.startsWith("Health: +")) {
                    int amount = Integer.parseInt(cleanLine.substring("Health: +".length()).trim());
                    stats.setMaxHealth(stats.getMaxHealth() + amount);
                    
                    if (Main.getInstance().isDebugMode()) {
                        Main.getInstance().getLogger().info("  Added Health: +" + amount);
                    }
                }
                
                // Armor (armor pieces)
                else if (cleanLine.startsWith("Armor: +")) {
                    int amount = Integer.parseInt(cleanLine.substring("Armor: +".length()).trim());
                    stats.setArmor(stats.getArmor() + amount);
                    
                    if (Main.getInstance().isDebugMode()) {
                        Main.getInstance().getLogger().info("  Added Armor: +" + amount);
                    }
                }
                
                // Magic damage
                else if (cleanLine.startsWith("Magic Damage: +")) {
                    int amount = Integer.parseInt(cleanLine.substring("Magic Damage: +".length()).trim());
                    stats.setMagicDamage(stats.getMagicDamage() + amount);
                    
                    if (Main.getInstance().isDebugMode()) {
                        Main.getInstance().getLogger().info("  Added Magic Damage: +" + amount);
                    }
                }
                
                // Magic resist
                else if (cleanLine.startsWith("Magic Resist: +")) {
                    int amount = Integer.parseInt(cleanLine.substring("Magic Resist: +".length()).trim());
                    stats.setMagicResist(stats.getMagicResist() + amount);
                    
                    if (Main.getInstance().isDebugMode()) {
                        Main.getInstance().getLogger().info("  Added Magic Resist: +" + amount);
                    }
                }
                
                // Attack Speed
                else if (cleanLine.startsWith("Attack Speed: +")) {
                    String speedStr = cleanLine.substring("Attack Speed: +".length()).trim();
                    double amount = Double.parseDouble(speedStr);
                    stats.setAttackSpeed(stats.getAttackSpeed() + amount);
                    
                    if (Main.getInstance().isDebugMode()) {
                        Main.getInstance().getLogger().info("  Added Attack Speed: +" + amount);
                    }
                }
                
                // Attack Range
                else if (cleanLine.startsWith("Attack Range: +")) {
                    String rangeStr = cleanLine.substring("Attack Range: +".length()).trim();
                    double amount = Double.parseDouble(rangeStr);
                    stats.setAttackRange(stats.getAttackRange() + amount);
                    
                    if (Main.getInstance().isDebugMode()) {
                        Main.getInstance().getLogger().info("  Added Attack Range: +" + amount);
                    }
                }
            } catch (Exception e) {
                if (Main.getInstance().isDebugMode()) {
                    Main.getInstance().getLogger().warning("Error parsing lore line: " + cleanLine + " - " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Save the current stats to the NPC's metadata
     */
    private void saveStatsToMetadata() {
        if (!npc.isSpawned()) return;
        
        npc.getEntity().setMetadata("max_health", new FixedMetadataValue(Main.getInstance(), stats.getMaxHealth()));
        npc.getEntity().setMetadata("physical_damage", new FixedMetadataValue(Main.getInstance(), stats.getPhysicalDamage()));
        npc.getEntity().setMetadata("magic_damage", new FixedMetadataValue(Main.getInstance(), stats.getMagicDamage()));
        npc.getEntity().setMetadata("armor", new FixedMetadataValue(Main.getInstance(), stats.getArmor()));
        npc.getEntity().setMetadata("magic_resist", new FixedMetadataValue(Main.getInstance(), stats.getMagicResist()));
    }
    
    /**
     * Load the NPC's stats from metadata
     */
    private NPCStats loadStatsFromMetadata() {
        NPCStats stats = new NPCStats();
        
        if (npc.isSpawned()) {
            // Get level
            if (npc.getEntity().hasMetadata("level")) {
                stats.setLevel(npc.getEntity().getMetadata("level").get(0).asInt());
            }
            
            // Get max health
            if (npc.getEntity().hasMetadata("max_health")) {
                stats.setMaxHealth(npc.getEntity().getMetadata("max_health").get(0).asDouble());
            }
            
            // Get physical damage
            if (npc.getEntity().hasMetadata("physical_damage")) {
                stats.setPhysicalDamage(npc.getEntity().getMetadata("physical_damage").get(0).asInt());
            }
            
            // Get magic damage
            if (npc.getEntity().hasMetadata("magic_damage")) {
                stats.setMagicDamage(npc.getEntity().getMetadata("magic_damage").get(0).asInt());
            }
            
            // Get armor
            if (npc.getEntity().hasMetadata("armor")) {
                stats.setArmor(npc.getEntity().getMetadata("armor").get(0).asInt());
            }
            
            // Get magic resist
            if (npc.getEntity().hasMetadata("magic_resist")) {
                stats.setMagicResist(npc.getEntity().getMetadata("magic_resist").get(0).asInt());
            }
        }
        
        return stats;
    }
    
    /**
     * Load the NPC's equipped items
     */
    private void loadEquippedItems() {
        if (equipment == null) return;
        
        // Load each equipment slot
        for (Equipment.EquipmentSlot slot : Equipment.EquipmentSlot.values()) {
            ItemStack item = equipment.get(slot);
            if (item != null) {
                equippedItems.put(slot, item);
            }
        }
    }
    
    /**
     * Get the NPC's current stats
     */
    public NPCStats getStats() {
        return stats;
    }
}