package com.server.profiles.stats;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

public class StatsUpdateManager {
    private static final long UPDATE_INTERVAL = 5L; // Update every 5 ticks (1/4 second)
    private final Main plugin;
    private final Map<UUID, BukkitTask> playerTrackingTasks = new HashMap<>();
    
    // Patterns for extracting stat values from lore - updated to handle color codes (§)
    private static final Pattern PHYSICAL_DAMAGE_PATTERN = Pattern.compile("Physical Damage: .*?\\+(\\d+)");
    private static final Pattern MAGIC_DAMAGE_PATTERN = Pattern.compile("Magic Damage: .*?\\+(\\d+)");
    private static final Pattern MANA_PATTERN = Pattern.compile("Mana: .*?\\+(\\d+)");
    private static final Pattern ARMOR_PATTERN = Pattern.compile("Armor: .*?\\+(\\d+)");
    private static final Pattern MAGIC_RESIST_PATTERN = Pattern.compile("Magic Resist: .*?\\+(\\d+)");
    private static final Pattern COOLDOWN_REDUCTION_PATTERN = Pattern.compile("Cooldown Reduction: .*?\\+(\\d+)%");
    private static final Pattern ATTACK_SPEED_PATTERN = Pattern.compile("Attack Speed: .*?\\+(\\d+\\.?\\d*)");
    private static final Pattern ATTACK_RANGE_PATTERN = Pattern.compile("Attack Range: .*?\\+(\\d+\\.?\\d*)");

    public StatsUpdateManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Start tracking and updating a player's stats
     * @param player The player to track
     */
    public void startTracking(Player player) {
        stopTracking(player); // Stop any existing tracking first
        
        // Initial stats update
        updatePlayerStats(player);
        
        // Schedule regular updates
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    playerTrackingTasks.remove(player.getUniqueId());
                    return;
                }
                
                // Check if player still has an active profile
                Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
                if (activeSlot == null) return;
                
                PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
                if (profile == null) return;
                
                updatePlayerStats(player);
            }
        }.runTaskTimer(plugin, UPDATE_INTERVAL, UPDATE_INTERVAL);
        
        // Store the task reference
        playerTrackingTasks.put(player.getUniqueId(), task);
    }
    
    /**
     * Stop tracking a player's stats
     * @param player The player to stop tracking
     */
    public void stopTracking(Player player) {
        BukkitTask task = playerTrackingTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public void updatePlayerStats(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;

        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;

        // Only update stats from armor, don't reapply for every equippable item
        // This will correct the bug where stats get permanent bonuses
        updateArmorStats(player, profile.getStats());
    }

    private void updateArmorStats(Player player, PlayerStats stats) {
        double currentHealth = player.getHealth();
        
        // Store current mana values before resetting
        int currentMana = stats.getMana();
        double manaPercentage = (double) currentMana / stats.getTotalMana();

        // Reset to base values
        stats.resetToDefaults();

        // Track total bonuses from ARMOR ONLY (not held item)
        int totalArmor = 0;
        int totalMagicResist = 0;
        int totalPhysicalDamage = 0;
        int totalMagicDamage = 0;
        int totalMana = 0;
        int totalCooldownReduction = 0;
        double totalAttackSpeed = 0;
        double totalAttackRange = 0;

        // Check armor slots for bonuses
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor == null || !armor.hasItemMeta() || !armor.getItemMeta().hasLore()) continue;
            
            for (String loreLine : armor.getItemMeta().getLore()) {
                // Strip color codes for regex matching
                String cleanLine = stripColorCodes(loreLine);
                
                // Parse stats from lore
                totalArmor += extractStat(cleanLine, ARMOR_PATTERN);
                totalMagicResist += extractStat(cleanLine, MAGIC_RESIST_PATTERN);
                totalMagicDamage += extractStat(cleanLine, MAGIC_DAMAGE_PATTERN);
                totalMana += extractStat(cleanLine, MANA_PATTERN);
                totalCooldownReduction += extractStat(cleanLine, COOLDOWN_REDUCTION_PATTERN);
                totalAttackSpeed += extractDoubleStat(cleanLine, ATTACK_SPEED_PATTERN);
                totalAttackRange += extractDoubleStat(cleanLine, ATTACK_RANGE_PATTERN);
            }
        }

        // Apply gathered stats from armor
        stats.setArmor(stats.getArmor() + totalArmor);
        stats.setMagicResist(stats.getMagicResist() + totalMagicResist);
        stats.setPhysicalDamage(stats.getDefaultPhysicalDamage() + totalPhysicalDamage);
        stats.setMagicDamage(stats.getDefaultMagicDamage() + totalMagicDamage);
        stats.setTotalMana(stats.getDefaultMana() + totalMana);
        stats.setCooldownReduction(stats.getCooldownReduction() + totalCooldownReduction);
        stats.setAttackSpeed(stats.getDefaultAttackSpeed() + totalAttackSpeed);
        stats.setAttackRange(stats.getDefaultAttackRange() + totalAttackRange);

        // Apply attack range attribute if available in Minecraft 1.20.5
        applyAttackRangeAttribute(player, stats.getAttackRange());

        // Maintain mana percentage after applying bonuses
        int newMana = Math.min((int)(manaPercentage * stats.getTotalMana()), stats.getTotalMana());
        stats.setMana(newMana);

        // Apply attack speed directly to override item-specific attack speed values
        applyAttackSpeedOverride(player, stats.getAttackSpeed());
        
        // Update player with other stats
        stats.applyToPlayer(player);
        
        // Restore health
        player.setHealth(Math.min(currentHealth, stats.getHealth()));
    }
    
    // Add method to apply the attack range attribute (for Minecraft 1.20.5)
    private void applyAttackRangeAttribute(Player player, double attackRange) {
        try {
            // Check if the attribute exists (only in 1.20.5+)
            if (player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE) != null) {
                // Clear existing modifiers
                player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE).getModifiers().forEach(modifier -> {
                    if (modifier.getName().equals("mmo.attack_range_override")) {
                        player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE).removeModifier(modifier);
                    }
                });
                
                // Calculate bonus over default (3.0)
                double rangeBonus = attackRange - 3.0;
                
                // Only apply modifier if we're increasing the range
                if (rangeBonus > 0) {
                    AttributeModifier rangeModifier = new AttributeModifier(
                        UUID.randomUUID(),
                        "mmo.attack_range_override",
                        rangeBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE).addModifier(rangeModifier);
                }
            }
        } catch (Exception e) {
            // This will catch errors if the attribute doesn't exist in older versions
            plugin.getLogger().info("Entity interaction range attribute not supported in this version");
        }
    }
        
    // New method to strip color codes for better regex matching
    private String stripColorCodes(String input) {
        return input.replaceAll("§[0-9a-fk-or]", "");
    }
    
    // The rest of the methods remain unchanged
    private void applyAttackSpeedOverride(Player player, double attackSpeed) {
        try {
            if (player.getAttribute(Attribute.GENERIC_ATTACK_SPEED) != null) {
                player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).getModifiers().forEach(modifier -> {
                    player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).removeModifier(modifier);
                });
                
                player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(4.0);
                
                AttributeModifier speedModifier = new AttributeModifier(
                    UUID.randomUUID(),
                    "mmo.attackspeed.override", 
                    attackSpeed - 4.0,
                    AttributeModifier.Operation.ADD_NUMBER
                );
                player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).addModifier(speedModifier);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply attack speed override: " + e.getMessage());
        }
    }
    
    private int extractStat(String loreLine, Pattern pattern) {
        try {
            Matcher matcher = pattern.matcher(loreLine);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return 0;
    }
    
    private double extractDoubleStat(String loreLine, Pattern pattern) {
        try {
            Matcher matcher = pattern.matcher(loreLine);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return 0;
    }
}