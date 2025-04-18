package com.server.events;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

public class RangedCombatManager implements Listener {
    private final Main plugin;
    private static final String ATTACK_RANGE_BASE_MODIFIER_NAME = "mmo.attack_range.base";
    private static final String ATTACK_RANGE_ITEM_MODIFIER_NAME = "mmo.attack_range.item";
    private static final double DEFAULT_ATTACK_RANGE = 3.0; // Minecraft default attack range
    
    public RangedCombatManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Updates both attack range and size for a player
     * @param player The player to update
     */
    public void updatePlayerAttributes(Player player) {
        updateAttackRange(player);
        updatePlayerSize(player);
    }

    private double getAttackRangeFromArmor(Player player) {
        double totalRangeBonus = 0.0;
        PlayerInventory inventory = player.getInventory();
        
        // Check helmet
        ItemStack helmet = inventory.getHelmet();
        if (isValidArmorPiece(helmet)) {
            double bonus = extractAttackRangeBonus(helmet);
            if (bonus > 0) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Found attack range bonus in helmet: +" + bonus + " from " + 
                                        (helmet.hasItemMeta() && helmet.getItemMeta().hasDisplayName() ? 
                                        helmet.getItemMeta().getDisplayName() : helmet.getType().name()));
                }
                totalRangeBonus += bonus;
            }
        }
        
        // Check chestplate
        ItemStack chestplate = inventory.getChestplate();
        if (isValidArmorPiece(chestplate)) {
            double bonus = extractAttackRangeBonus(chestplate);
            if (bonus > 0) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Found attack range bonus in chestplate: +" + bonus + " from " + 
                                        (chestplate.hasItemMeta() && chestplate.getItemMeta().hasDisplayName() ? 
                                        chestplate.getItemMeta().getDisplayName() : chestplate.getType().name()));
                }
                totalRangeBonus += bonus;
            }
        }
        
        // Check leggings
        ItemStack leggings = inventory.getLeggings();
        if (isValidArmorPiece(leggings)) {
            double bonus = extractAttackRangeBonus(leggings);
            if (bonus > 0) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Found attack range bonus in leggings: +" + bonus + " from " + 
                                        (leggings.hasItemMeta() && leggings.getItemMeta().hasDisplayName() ? 
                                        leggings.getItemMeta().getDisplayName() : leggings.getType().name()));
                }
                totalRangeBonus += bonus;
            }
        }
        
        // Check boots
        ItemStack boots = inventory.getBoots();
        if (isValidArmorPiece(boots)) {
            double bonus = extractAttackRangeBonus(boots);
            if (bonus > 0) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Found attack range bonus in boots: +" + bonus + " from " + 
                                        (boots.hasItemMeta() && boots.getItemMeta().hasDisplayName() ? 
                                        boots.getItemMeta().getDisplayName() : boots.getType().name()));
                }
                totalRangeBonus += bonus;
            }
        }
        
        return totalRangeBonus;
    }

    // Update updateAttackRange method to include armor bonuses
    public void updateAttackRange(Player player) {
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;

        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Get the player's base attack range stat value
        double baseAttackRange = profile.getStats().getAttackRange();
        double baseBonus = baseAttackRange - DEFAULT_ATTACK_RANGE;
        
        // Get bonuses from held item and armor
        double heldItemBonus = getAttackRangeFromHeldItem(player);
        double armorBonus = getAttackRangeFromArmor(player);
        double totalItemBonus = heldItemBonus + armorBonus;
        
        // Debug output to verify values - only in debug mode
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Player: " + player.getName() + 
                                ", Base Attack Range: " + baseAttackRange + 
                                ", Base Bonus: " + baseBonus +
                                ", Held Item Bonus: " + heldItemBonus +
                                ", Armor Bonus: " + armorBonus +
                                ", Total Item Bonus: " + totalItemBonus);
        }
        
        // Apply attack range attribute if available in Minecraft 1.20.5
        try {
            AttributeInstance attackRangeAttribute = player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
            if (attackRangeAttribute != null) {
                // IMPORTANT: Remove ALL modifiers, not just our custom ones
                // This ensures there are no unexpected modifiers from other sources
                Set<AttributeModifier> allModifiers = new HashSet<>(attackRangeAttribute.getModifiers());
                for (AttributeModifier modifier : allModifiers) {
                    // Log what we're removing for debugging - only in debug mode
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Removing modifier: " + modifier.getName() + 
                                            " (amount: " + modifier.getAmount() + 
                                            ", operation: " + modifier.getOperation() + ")");
                    }
                    attackRangeAttribute.removeModifier(modifier);
                }
                
                // Force base value to default
                attackRangeAttribute.setBaseValue(DEFAULT_ATTACK_RANGE);
                
                // Add base stat bonus if positive
                if (baseBonus > 0) {
                    AttributeModifier baseModifier = new AttributeModifier(
                        UUID.randomUUID(),
                        ATTACK_RANGE_BASE_MODIFIER_NAME,
                        baseBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    attackRangeAttribute.addModifier(baseModifier);
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Added base attack range modifier: +" + baseBonus);
                    }
                }
                
                // Add held item bonus if any
                if (heldItemBonus > 0) {
                    AttributeModifier itemModifier = new AttributeModifier(
                        UUID.randomUUID(),
                        "mmo.attack_range.held_item",
                        heldItemBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    attackRangeAttribute.addModifier(itemModifier);
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Added held item attack range modifier: +" + heldItemBonus);
                    }
                }
                
                // Add armor bonus if any
                if (armorBonus > 0) {
                    AttributeModifier armorModifier = new AttributeModifier(
                        UUID.randomUUID(),
                        "mmo.attack_range.armor",
                        armorBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    attackRangeAttribute.addModifier(armorModifier);
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Added armor attack range modifier: +" + armorBonus);
                    }
                }
                
                // Calculate total effective range
                double effectiveRange = DEFAULT_ATTACK_RANGE;
                if (baseBonus > 0) effectiveRange += baseBonus;
                if (totalItemBonus > 0) effectiveRange += totalItemBonus;
                
                // Verify the final attribute value - only in debug mode
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Attribute value check for " + player.getName() + ": " + 
                                        "Base value: " + attackRangeAttribute.getBaseValue() + 
                                        ", Default value: " + attackRangeAttribute.getDefaultValue() + 
                                        ", Final value: " + attackRangeAttribute.getValue());
                    
                    // Log all modifiers after our changes
                    plugin.getLogger().info("Current modifiers for " + player.getName() + ":");
                    for (AttributeModifier mod : attackRangeAttribute.getModifiers()) {
                        plugin.getLogger().info("  - " + mod.getName() + ": " + mod.getAmount() + 
                                            " (" + mod.getOperation() + ")");
                    }
                    
                    // Log the final value
                    plugin.getLogger().info("Final attack range for " + player.getName() + ": " + effectiveRange + 
                                        " (default: " + DEFAULT_ATTACK_RANGE +
                                        ", base bonus: " + baseBonus +
                                        ", item bonus: " + totalItemBonus + ")");
                }
                
                // Remove player feedback message completely
            } else {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().warning("PLAYER_ENTITY_INTERACTION_RANGE attribute not found for " + player.getName());
                    // Try to see what attributes are available
                    for (Attribute attr : Attribute.values()) {
                        if (player.getAttribute(attr) != null) {
                            plugin.getLogger().info("Available attribute: " + attr.name());
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error applying attack range: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Extract attack range bonus from a player's currently held item
     * @param player The player
     * @return The attack range bonus value
     */
    private double getAttackRangeFromHeldItem(Player player) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || !heldItem.hasItemMeta() || !heldItem.getItemMeta().hasLore()) {
            return 0;
        }
        
        // Check if the held item is an armor piece - if so, don't apply its stats when held
        if (isArmorItem(heldItem)) {
            return 0;
        }
        
        for (String loreLine : heldItem.getItemMeta().getLore()) {
            if (loreLine.contains("Attack Range:")) {
                try {
                    // Extract the numeric value after the +
                    String[] parts = loreLine.split("\\+");
                    if (parts.length > 1) {
                        String valueStr = parts[1].replaceAll("§[0-9a-fk-or]", "").trim();
                        double value = Double.parseDouble(valueStr);
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("Found attack range bonus in item lore: +" + value);
                        }
                        return value;
                    }
                } catch (Exception e) {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().warning("Error parsing attack range from lore: " + e.getMessage());
                    }
                }
            }
        }
        
        return 0;
    }

    private double getSizeFromHeldItem(Player player) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || !heldItem.hasItemMeta() || !heldItem.getItemMeta().hasLore()) {
            return 0;
        }
        
        // Check if the held item is an armor piece - if so, don't apply its stats when held
        if (isArmorItem(heldItem)) {
            return 0;
        }
        
        for (String loreLine : heldItem.getItemMeta().getLore()) {
            if (loreLine.contains("Size:")) {
                try {
                    // Extract the numeric value after the +
                    String[] parts = loreLine.split("\\+");
                    if (parts.length > 1) {
                        String valueStr = parts[1].replaceAll("§[0-9a-fk-or]", "").trim();
                        double value = Double.parseDouble(valueStr);
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("Found size bonus in item lore: +" + value);
                        }
                        return value;
                    }
                } catch (Exception e) {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().warning("Error parsing size from lore: " + e.getMessage());
                    }
                }
            }
        }
        
        return 0;
    }

    private double getSizeFromArmor(Player player) {
        double totalSizeBonus = 0.0;
        PlayerInventory inventory = player.getInventory();
        
        // Check helmet
        ItemStack helmet = inventory.getHelmet();
        if (isValidArmorPiece(helmet)) {
            double bonus = extractSizeBonus(helmet);
            if (bonus > 0) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Found size bonus in helmet: +" + bonus + " from " + 
                                        (helmet.hasItemMeta() && helmet.getItemMeta().hasDisplayName() ? 
                                        helmet.getItemMeta().getDisplayName() : helmet.getType().name()));
                }
                totalSizeBonus += bonus;
            }
        }
        
        // Check chestplate
        ItemStack chestplate = inventory.getChestplate();
        if (isValidArmorPiece(chestplate)) {
            double bonus = extractSizeBonus(chestplate);
            if (bonus > 0) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Found size bonus in chestplate: +" + bonus + " from " + 
                                        (chestplate.hasItemMeta() && chestplate.getItemMeta().hasDisplayName() ? 
                                        chestplate.getItemMeta().getDisplayName() : chestplate.getType().name()));
                }
                totalSizeBonus += bonus;
            }
        }
        
        // Check leggings
        ItemStack leggings = inventory.getLeggings();
        if (isValidArmorPiece(leggings)) {
            double bonus = extractSizeBonus(leggings);
            if (bonus > 0) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Found size bonus in leggings: +" + bonus + " from " + 
                                        (leggings.hasItemMeta() && leggings.getItemMeta().hasDisplayName() ? 
                                        leggings.getItemMeta().getDisplayName() : leggings.getType().name()));
                }
                totalSizeBonus += bonus;
            }
        }
        
        // Check boots
        ItemStack boots = inventory.getBoots();
        if (isValidArmorPiece(boots)) {
            double bonus = extractSizeBonus(boots);
            if (bonus > 0) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Found size bonus in boots: +" + bonus + " from " + 
                                        (boots.hasItemMeta() && boots.getItemMeta().hasDisplayName() ? 
                                        boots.getItemMeta().getDisplayName() : boots.getType().name()));
                }
                totalSizeBonus += bonus;
            }
        }
        
        return totalSizeBonus;
    }

    /**
     * Helper method to check if an armor piece is valid and has metadata
     */
    private boolean isValidArmorPiece(ItemStack item) {
        return item != null && item.getType() != Material.AIR && 
               item.hasItemMeta() && item.getItemMeta().hasLore();
    }

    /**
     * Extract the size bonus from an item's lore
     */
    private double extractSizeBonus(ItemStack item) {
        if (!isValidArmorPiece(item)) return 0;
        
        for (String loreLine : item.getItemMeta().getLore()) {
            if (loreLine.contains("Size:")) {
                try {
                    // Extract the numeric value after the +
                    String[] parts = loreLine.split("\\+");
                    if (parts.length > 1) {
                        String valueStr = parts[1].replaceAll("§[0-9a-fk-or]", "").trim();
                        return Double.parseDouble(valueStr);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error parsing size from lore: " + e.getMessage());
                }
            }
        }
        
        return 0;
    }

    /**
     * Extract the attack range bonus from an item's lore
     */
    private double extractAttackRangeBonus(ItemStack item) {
        if (!isValidArmorPiece(item)) return 0;
        
        for (String loreLine : item.getItemMeta().getLore()) {
            if (loreLine.contains("Attack Range:")) {
                try {
                    // Extract the numeric value after the +
                    String[] parts = loreLine.split("\\+");
                    if (parts.length > 1) {
                        String valueStr = parts[1].replaceAll("§[0-9a-fk-or]", "").trim();
                        return Double.parseDouble(valueStr);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error parsing attack range from lore: " + e.getMessage());
                }
            }
        }
        
        return 0;
    }

    // Update updatePlayerSize method to include armor bonuses
    public void updatePlayerSize(Player player) {
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;

        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Get the player's base size value
        double baseSize = profile.getStats().getSize();
        
        // Get size bonuses from held item and armor
        double heldItemBonus = getSizeFromHeldItem(player);
        double armorBonus = getSizeFromArmor(player);
        double totalItemBonus = heldItemBonus + armorBonus;
        
        // Calculate final size
        double finalSize = baseSize + totalItemBonus;
        
        // Debug output in DEBUG mode only
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Player: " + player.getName() + 
                                ", Base Size: " + baseSize + 
                                ", Held Item Size Bonus: " + heldItemBonus +
                                ", Armor Size Bonus: " + armorBonus +
                                ", Total Bonus: " + totalItemBonus);
        }
        
        // Apply scale attribute if available in Minecraft 1.20.5
        try {
            AttributeInstance scaleAttribute = player.getAttribute(Attribute.GENERIC_SCALE);
            if (scaleAttribute != null) {
                // Remove existing modifiers
                Set<AttributeModifier> allModifiers = new HashSet<>(scaleAttribute.getModifiers());
                for (AttributeModifier modifier : allModifiers) {
                    // Only log in debug mode
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Removing size modifier: " + modifier.getName() + 
                                            " (amount: " + modifier.getAmount() + 
                                            ", operation: " + modifier.getOperation() + ")");
                    }
                    scaleAttribute.removeModifier(modifier);
                }
                
                // Set base value to default size
                scaleAttribute.setBaseValue(profile.getStats().getDefaultSize());
                
                // Add base bonus
                double baseBonus = baseSize - profile.getStats().getDefaultSize();
                if (baseBonus != 0) {
                    AttributeModifier baseModifier = new AttributeModifier(
                        UUID.randomUUID(),
                        "mmo.size.base",
                        baseBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    scaleAttribute.addModifier(baseModifier);
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Added base size modifier: +" + baseBonus);
                    }
                }
                
                // Add held item bonus if any
                if (heldItemBonus > 0) {
                    AttributeModifier itemModifier = new AttributeModifier(
                        UUID.randomUUID(),
                        "mmo.size.held_item",
                        heldItemBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    scaleAttribute.addModifier(itemModifier);
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Added held item size modifier: +" + heldItemBonus);
                    }
                }
                
                // Add armor bonus if any
                if (armorBonus > 0) {
                    AttributeModifier armorModifier = new AttributeModifier(
                        UUID.randomUUID(),
                        "mmo.size.armor",
                        armorBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    scaleAttribute.addModifier(armorModifier);
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Added armor size modifier: +" + armorBonus);
                    }
                }
                
                // Verify final value in debug mode only
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Scale attribute value check for " + player.getName() + ": " + 
                                        "Base value: " + scaleAttribute.getBaseValue() + 
                                        ", Final value: " + scaleAttribute.getValue());
                    
                    // Log all modifiers after our changes
                    plugin.getLogger().info("Current size modifiers for " + player.getName() + ":");
                    for (AttributeModifier mod : scaleAttribute.getModifiers()) {
                        plugin.getLogger().info("  - " + mod.getName() + ": " + mod.getAmount() + 
                                            " (" + mod.getOperation() + ")");
                    }
                }
                
                // Remove player feedback message completely
            } else {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().warning("GENERIC_SCALE attribute not found for " + player.getName());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error applying player size: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        
        // First, reset all attributes to vanilla defaults
        resetPlayerAttributes(player);
        
        // Now schedule multiple updates with delays to ensure the profile is loaded and properly applied
        plugin.getLogger().info("Player joined: " + player.getName() + ", applying attributes...");
        
        // Delay for several ticks to ensure player is fully loaded and profile is ready
        // Only try a few times with increasing delays to reduce console spam
        for (int delay : new int[] {1, 5, 10, 20}) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        return; // Player logged out during the delay
                    }
                    plugin.getLogger().info("Updating attributes for " + player.getName() + " (attempt at " + delay + " ticks)");
                    updatePlayerAttributes(player);
                }
            }.runTaskLater(plugin, delay);
        }
    }

    /**
     * Reset a player's attributes to default Minecraft values except health
     */
    private void resetPlayerAttributes(Player player) {
        try {
            // Reset attack range attribute
            AttributeInstance attackRangeAttribute = player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
            if (attackRangeAttribute != null) {
                Set<AttributeModifier> allModifiers = new HashSet<>(attackRangeAttribute.getModifiers());
                for (AttributeModifier modifier : allModifiers) {
                    attackRangeAttribute.removeModifier(modifier);
                }
                attackRangeAttribute.setBaseValue(3.0); // Vanilla default
            }
            
            // Reset scale attribute
            AttributeInstance scaleAttribute = player.getAttribute(Attribute.GENERIC_SCALE);
            if (scaleAttribute != null) {
                Set<AttributeModifier> allModifiers = new HashSet<>(scaleAttribute.getModifiers());
                for (AttributeModifier modifier : allModifiers) {
                    scaleAttribute.removeModifier(modifier);
                }
                scaleAttribute.setBaseValue(1.0); // Vanilla default
            }
            
            // Reset attack speed attribute
            AttributeInstance attackSpeedAttribute = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (attackSpeedAttribute != null) {
                Set<AttributeModifier> speedModifiers = new HashSet<>(attackSpeedAttribute.getModifiers());
                for (AttributeModifier modifier : speedModifiers) {
                    attackSpeedAttribute.removeModifier(modifier);
                }
                attackSpeedAttribute.setBaseValue(4.0); // Vanilla default
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error resetting attributes on join: " + e.getMessage());
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up when player leaves
        Player player = event.getPlayer();
        try {
            // Reset attack range
            AttributeInstance attackRangeAttribute = player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
            if (attackRangeAttribute != null) {
                Set<AttributeModifier> allModifiers = new HashSet<>(attackRangeAttribute.getModifiers());
                for (AttributeModifier modifier : allModifiers) {
                    attackRangeAttribute.removeModifier(modifier);
                }
                attackRangeAttribute.setBaseValue(DEFAULT_ATTACK_RANGE);
            }
            
            // Reset scale
            AttributeInstance scaleAttribute = player.getAttribute(Attribute.GENERIC_SCALE);
            if (scaleAttribute != null) {
                Set<AttributeModifier> allModifiers = new HashSet<>(scaleAttribute.getModifiers());
                for (AttributeModifier modifier : allModifiers) {
                    scaleAttribute.removeModifier(modifier);
                }
                scaleAttribute.setBaseValue(1.0);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error cleaning up attributes for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        // Update attributes when player changes held item
        final Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLogger().info("Player changed held item: " + player.getName());
                updatePlayerAttributes(player);
            }
        }.runTaskLater(plugin, 1L);
    }
    
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        // Update attributes when player swaps hands
        final Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLogger().info("Player swapped hands: " + player.getName());
                updatePlayerAttributes(player);
            }
        }.runTaskLater(plugin, 1L);
    }
    
    /**
     * More focused handler for armor-related inventory clicks
     * This ensures we specifically detect when armor is equipped or unequipped
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorChange(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        // Check if this is an armor slot interaction
        boolean isArmorSlotClick = event.getSlotType() == InventoryType.SlotType.ARMOR;
        
        // Check if this is a shift-click that might equip armor
        boolean isPotentialArmorEquip = event.isShiftClick() && 
                                        isArmorItem(event.getCurrentItem());
        
        // Check if this is a direct armor slot click from cursor
        boolean isArmorPlacement = event.getSlotType() == InventoryType.SlotType.ARMOR && 
                                isArmorItem(event.getCursor());
        
        // Store old items and slots for event firing
        final ItemStack[] oldArmorContents = player.getInventory().getArmorContents().clone();
        final ItemStack oldHelmet = oldArmorContents[3] != null ? oldArmorContents[3].clone() : null;
        final ItemStack oldChestplate = oldArmorContents[2] != null ? oldArmorContents[2].clone() : null;
        final ItemStack oldLeggings = oldArmorContents[1] != null ? oldArmorContents[1].clone() : null;
        final ItemStack oldBoots = oldArmorContents[0] != null ? oldArmorContents[0].clone() : null;
        
        // If any of these conditions are met, we need to update attributes
        if (isArmorSlotClick || isPotentialArmorEquip || isArmorPlacement) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        // Fire events for each armor slot if there's a change
                        PlayerInventory inv = player.getInventory();
                        
                        // Check helmet
                        ItemStack newHelmet = inv.getHelmet();
                        if (!itemStacksEqual(oldHelmet, newHelmet)) {
                            fireArmorChangeEvent(player, EquipmentSlot.HEAD, oldHelmet, newHelmet);
                        }
                        
                        // Check chestplate
                        ItemStack newChestplate = inv.getChestplate();
                        if (!itemStacksEqual(oldChestplate, newChestplate)) {
                            fireArmorChangeEvent(player, EquipmentSlot.CHEST, oldChestplate, newChestplate);
                        }
                        
                        // Check leggings
                        ItemStack newLeggings = inv.getLeggings();
                        if (!itemStacksEqual(oldLeggings, newLeggings)) {
                            fireArmorChangeEvent(player, EquipmentSlot.LEGS, oldLeggings, newLeggings);
                        }
                        
                        // Check boots
                        ItemStack newBoots = inv.getBoots();
                        if (!itemStacksEqual(oldBoots, newBoots)) {
                            fireArmorChangeEvent(player, EquipmentSlot.FEET, oldBoots, newBoots);
                        }
                        
                        plugin.getLogger().info("Armor equipment changed for: " + player.getName());
                        updatePlayerAttributes(player);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    // Helper method to compare ItemStacks for equality
    private boolean itemStacksEqual(ItemStack item1, ItemStack item2) {
        // Both null = equal
        if (item1 == null && item2 == null) return true;
        // One null, one not null = not equal
        if (item1 == null || item2 == null) return false;
        // Compare type, metadata, and amount
        return item1.getType() == item2.getType() && 
            item1.hasItemMeta() == item2.hasItemMeta() &&
            (!item1.hasItemMeta() || item1.getItemMeta().equals(item2.getItemMeta())) &&
            item1.getAmount() == item2.getAmount();
    }
    
    /**
     * Helper method to check if an item is an armor piece
     */
    private boolean isArmorItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        String typeName = item.getType().name();
        return typeName.endsWith("_HELMET") || 
               typeName.endsWith("_CHESTPLATE") || 
               typeName.endsWith("_LEGGINGS") || 
               typeName.endsWith("_BOOTS") ||
               typeName.equals("CARVED_PUMPKIN") || // For custom helmets like Witch's Hat
               typeName.equals("PLAYER_HEAD") ||    // For custom helmets
               typeName.equals("SKULL_ITEM");       // For older versions
    }

    private void fireArmorChangeEvent(Player player, EquipmentSlot slot, ItemStack oldItem, ItemStack newItem) {
        PlayerArmorChangeEvent event = new PlayerArmorChangeEvent(player, slot, oldItem, newItem);
        plugin.getServer().getPluginManager().callEvent(event);
    }       
    
    /**
     * Try to detect when armor is drag-dropped onto a player model
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        // Check if any armor slots were affected
        Set<Integer> armorSlots = new HashSet<>(Arrays.asList(5, 6, 7, 8)); // Armor slot numbers
        
        // Store old armor
        final ItemStack oldHelmet = player.getInventory().getHelmet() != null ? 
                                player.getInventory().getHelmet().clone() : null;
        final ItemStack oldChestplate = player.getInventory().getChestplate() != null ? 
                                    player.getInventory().getChestplate().clone() : null;
        final ItemStack oldLeggings = player.getInventory().getLeggings() != null ? 
                                    player.getInventory().getLeggings().clone() : null;
        final ItemStack oldBoots = player.getInventory().getBoots() != null ? 
                                player.getInventory().getBoots().clone() : null;
        
        // If any armor slots were involved, schedule an update
        if (event.getInventorySlots().stream().anyMatch(armorSlots::contains)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        // Check for changes in each armor slot
                        PlayerInventory inv = player.getInventory();
                        
                        // Check helmet (slot 5 or HEAD)
                        ItemStack newHelmet = inv.getHelmet();
                        if (!itemStacksEqual(oldHelmet, newHelmet)) {
                            fireArmorChangeEvent(player, EquipmentSlot.HEAD, oldHelmet, newHelmet);
                        }
                        
                        // Check chestplate (slot 6 or CHEST)
                        ItemStack newChestplate = inv.getChestplate();
                        if (!itemStacksEqual(oldChestplate, newChestplate)) {
                            fireArmorChangeEvent(player, EquipmentSlot.CHEST, oldChestplate, newChestplate);
                        }
                        
                        // Check leggings (slot 7 or LEGS)
                        ItemStack newLeggings = inv.getLeggings();
                        if (!itemStacksEqual(oldLeggings, newLeggings)) {
                            fireArmorChangeEvent(player, EquipmentSlot.LEGS, oldLeggings, newLeggings);
                        }
                        
                        // Check boots (slot 8 or FEET)
                        ItemStack newBoots = inv.getBoots();
                        if (!itemStacksEqual(oldBoots, newBoots)) {
                            fireArmorChangeEvent(player, EquipmentSlot.FEET, oldBoots, newBoots);
                        }
                        
                        plugin.getLogger().info("Armor dragged for: " + player.getName());
                        updatePlayerAttributes(player);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        // Check if this is a player
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        
        // Check if the player's main hand is currently empty
        PlayerInventory inv = player.getInventory();
        boolean mainHandWillReceive = inv.getItemInMainHand().getType().isAir();
        
        // If the main hand is empty, the item might go there directly
        if (mainHandWillReceive) {
            // Schedule an update immediately after the pickup completes
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Player picked up item into empty hand: " + player.getName());
                    }
                    updatePlayerAttributes(player);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        // Only care about player inventory changes
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        // We need to update after the next tick to let inventory changes settle
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Inventory click might have affected attributes: " + player.getName());
                    }
                    updatePlayerAttributes(player);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        // Only care about player inventory changes
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        
        // Update after inventory is closed in case items were moved
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Inventory closed, checking attributes: " + player.getName());
                    }
                    updatePlayerAttributes(player);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArmorBreak(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        
        // Check if damage might break armor
        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE || 
            event.getCause() == EntityDamageEvent.DamageCause.LAVA ||
            event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
            event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK ||
            event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
            
            // Schedule an update to check for broken armor
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        updatePlayerAttributes(player);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerArmorChange(PlayerItemBreakEvent event) {
        // Update when armor breaks
        Player player = event.getPlayer();
        ItemStack brokenItem = event.getBrokenItem();
        
        if (brokenItem.getType().name().contains("_HELMET") || 
            brokenItem.getType().name().contains("_CHESTPLATE") ||
            brokenItem.getType().name().contains("_LEGGINGS") ||
            brokenItem.getType().name().contains("_BOOTS")) {
            
            // Determine which slot the broken item was in
            EquipmentSlot slot = null;
            if (brokenItem.getType().name().contains("_HELMET")) {
                slot = EquipmentSlot.HEAD;
            } else if (brokenItem.getType().name().contains("_CHESTPLATE")) {
                slot = EquipmentSlot.CHEST;
            } else if (brokenItem.getType().name().contains("_LEGGINGS")) {
                slot = EquipmentSlot.LEGS;
            } else if (brokenItem.getType().name().contains("_BOOTS")) {
                slot = EquipmentSlot.FEET;
            }
            
            if (slot != null) {
                // Fire the event (old = broken item, new = null/air)
                final ItemStack brokenItemCopy = brokenItem.clone();
                final EquipmentSlot finalSlot = slot;
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        fireArmorChangeEvent(player, finalSlot, brokenItemCopy, null);
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("Armor broke, updating attributes for: " + player.getName());
                        }
                        updatePlayerAttributes(player);
                    }
                }.runTaskLater(plugin, 1L);
            }
        }
    }   

}