package com.server.events;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
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
     * Apply the attack range attribute modifiers to a player based on their stats and held item
     * @param player The player to update
     */
    public void updateAttackRange(Player player) {
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;

        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Get the player's base attack range stat value
        double baseAttackRange = profile.getStats().getAttackRange();
        double baseBonus = baseAttackRange - DEFAULT_ATTACK_RANGE;
        
        // Get item bonus (if any)
        double itemBonus = getAttackRangeFromHeldItem(player);
        
        // Debug output to verify values
        plugin.getLogger().info("Player: " + player.getName() + 
                               ", Base Attack Range: " + baseAttackRange + 
                               ", Base Bonus: " + baseBonus +
                               ", Item Bonus: " + itemBonus);
        
        // Apply attack range attribute if available in Minecraft 1.20.5
        try {
            AttributeInstance attackRangeAttribute = player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
            if (attackRangeAttribute != null) {
                // IMPORTANT: Remove ALL modifiers, not just our custom ones
                // This ensures there are no unexpected modifiers from other sources
                Set<AttributeModifier> allModifiers = new HashSet<>(attackRangeAttribute.getModifiers());
                for (AttributeModifier modifier : allModifiers) {
                    // Log what we're removing for debugging
                    plugin.getLogger().info("Removing modifier: " + modifier.getName() + 
                                           " (amount: " + modifier.getAmount() + 
                                           ", operation: " + modifier.getOperation() + ")");
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
                    plugin.getLogger().info("Added base attack range modifier: +" + baseBonus);
                }
                
                // Add item bonus if any
                if (itemBonus > 0) {
                    AttributeModifier itemModifier = new AttributeModifier(
                        UUID.randomUUID(),
                        ATTACK_RANGE_ITEM_MODIFIER_NAME,
                        itemBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    attackRangeAttribute.addModifier(itemModifier);
                    plugin.getLogger().info("Added item attack range modifier: +" + itemBonus);
                }
                
                // Calculate total effective range
                double effectiveRange = DEFAULT_ATTACK_RANGE;
                if (baseBonus > 0) effectiveRange += baseBonus;
                if (itemBonus > 0) effectiveRange += itemBonus;
                
                // Verify the final attribute value
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
                                      ", item bonus: " + itemBonus + ")");
                
                // Only send message if there's a meaningful change
                if (baseBonus > 0 || itemBonus > 0) {
                    String itemBonusText = itemBonus > 0 ? String.format(" §e(+%.1f from item)", itemBonus) : "";
                    player.sendMessage(String.format("§7Attack range: §f%.1f blocks%s", effectiveRange, itemBonusText));
                }
            } else {
                plugin.getLogger().warning("PLAYER_ENTITY_INTERACTION_RANGE attribute not found for " + player.getName());
                // Try to see what attributes are available
                for (Attribute attr : Attribute.values()) {
                    if (player.getAttribute(attr) != null) {
                        plugin.getLogger().info("Available attribute: " + attr.name());
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error applying attack range: " + e.getMessage());
            e.printStackTrace();
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
        
        for (String loreLine : heldItem.getItemMeta().getLore()) {
            if (loreLine.contains("Attack Range:")) {
                try {
                    // Extract the numeric value after the +
                    String[] parts = loreLine.split("\\+");
                    if (parts.length > 1) {
                        String valueStr = parts[1].replaceAll("§[0-9a-fk-or]", "").trim();
                        double value = Double.parseDouble(valueStr);
                        plugin.getLogger().info("Found attack range bonus in item lore: +" + value);
                        return value;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error parsing attack range from lore: " + e.getMessage());
                }
            }
        }
        
        return 0;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        
        // Apply immediately to ensure the attribute is cleared right away
        try {
            AttributeInstance attackRangeAttribute = player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
            if (attackRangeAttribute != null) {
                // Reset the attribute completely
                Set<AttributeModifier> allModifiers = new HashSet<>(attackRangeAttribute.getModifiers());
                plugin.getLogger().info("[JOIN] Removing " + allModifiers.size() + " modifiers from " + player.getName());
                for (AttributeModifier modifier : allModifiers) {
                    attackRangeAttribute.removeModifier(modifier);
                }
                attackRangeAttribute.setBaseValue(DEFAULT_ATTACK_RANGE);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error resetting attack range on join: " + e.getMessage());
        }
        
        // Now schedule multiple updates with delays to ensure the profile is loaded
        plugin.getLogger().info("Player joined: " + player.getName() + ", applying attack range...");
        
        // Delay for several ticks to ensure player is fully loaded and profile is ready
        for (int delay = 1; delay <= 40; delay += 5) {
            final int currentDelay = delay;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        return; // Player logged out during the delay
                    }
                    plugin.getLogger().info("Updating attack range for " + player.getName() + " (attempt at " + currentDelay + " ticks)");
                    updateAttackRange(player);
                }
            }.runTaskLater(plugin, delay);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up when player leaves
        Player player = event.getPlayer();
        try {
            AttributeInstance attackRangeAttribute = player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
            if (attackRangeAttribute != null) {
                // Remove all modifiers for clean exit
                Set<AttributeModifier> allModifiers = new HashSet<>(attackRangeAttribute.getModifiers());
                for (AttributeModifier modifier : allModifiers) {
                    attackRangeAttribute.removeModifier(modifier);
                }
                // Reset to default
                attackRangeAttribute.setBaseValue(DEFAULT_ATTACK_RANGE);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error cleaning up attack range for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        // Update attack range when player changes held item
        final Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLogger().info("Player changed held item: " + player.getName());
                updateAttackRange(player);
            }
        }.runTaskLater(plugin, 1L);
    }
    
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        // Update attack range when player swaps hands
        final Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLogger().info("Player swapped hands: " + player.getName());
                updateAttackRange(player);
            }
        }.runTaskLater(plugin, 1L);
    }
    
    // NEW HANDLERS FOR DIRECT INVENTORY CHANGES
    
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
                    plugin.getLogger().info("Player picked up item into empty hand: " + player.getName());
                    updateAttackRange(player);
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
                    plugin.getLogger().info("Inventory click might have affected main hand: " + player.getName());
                    updateAttackRange(player);
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
                    plugin.getLogger().info("Inventory closed, checking main hand: " + player.getName());
                    updateAttackRange(player);
                }
            }
        }.runTaskLater(plugin, 1L);
    }
}