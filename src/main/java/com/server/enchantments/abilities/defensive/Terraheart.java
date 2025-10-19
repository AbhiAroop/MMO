package com.server.enchantments.abilities.defensive;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentLevel;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Terraheart - Earth Defensive Enchantment
 * 
 * Roots the player to the ground while stationary, granting damage reduction.
 * The longer you stand still, the more damage reduction you gain (up to a cap).
 * Bonus increases with earth affinity.
 * 
 * Equipment: Chestplates, Shields
 * Rarity: RARE
 * Element: EARTH
 */
public class Terraheart extends CustomEnchantment {
    
    private static final Map<UUID, Location> lastLocation = new HashMap<>();
    private static final Map<UUID, Long> stationaryTime = new HashMap<>();
    private static final Map<UUID, Integer> rootedTicks = new HashMap<>();
    private static final double MOVEMENT_THRESHOLD = 0.5; // Blocks moved to count as movement
    private static final int MAX_ROOTED_TICKS = 100; // 5 seconds to max bonus
    
    public Terraheart() {
        super(
            "terraheart",
            "Terraheart",
            "Roots you to the earth while stationary, reducing damage taken",
            EnchantmentRarity.RARE,
            ElementType.EARTH
        );
        
        // Start monitoring player movement
        startMovementMonitor();
    }
    
    @Override
    public int getMaxLevel() {
        return 6; // Strong defensive mechanic
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Can be applied to chestplates and shields
        return type == Material.LEATHER_CHESTPLATE || type == Material.CHAINMAIL_CHESTPLATE ||
               type == Material.IRON_CHESTPLATE || type == Material.GOLDEN_CHESTPLATE ||
               type == Material.DIAMOND_CHESTPLATE || type == Material.NETHERITE_CHESTPLATE ||
               type == Material.SHIELD;
    }
    
    @Override
    public double[] getBaseStats() {
        // [max_damage_reduction_percentage]
        return new double[]{30.0}; // 30% max damage reduction
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        if (!(event instanceof EntityDamageEvent)) return;
        EntityDamageEvent damageEvent = (EntityDamageEvent) event;
        
        UUID playerId = player.getUniqueId();
        int ticks = rootedTicks.getOrDefault(playerId, 0);
        
        // Only apply if player has been stationary
        if (ticks < 10) return; // Must be still for at least 0.5 seconds
        
        // Get scaled stats (quality only)
        double[] stats = getScaledStats(quality);
        double maxReduction = stats[0];
        
        // Apply affinity modifier to damage reduction cap
        double affinityBonus = getAffinityReductionBonus(player);
        double finalMaxReduction = Math.min(50.0, maxReduction + affinityBonus); // Hard cap at 50%
        
        // Calculate reduction based on how long player has been stationary
        double reductionProgress = Math.min(1.0, (double) ticks / MAX_ROOTED_TICKS);
        double damageReduction = finalMaxReduction * reductionProgress;
        
        // Apply damage reduction
        double originalDamage = damageEvent.getDamage();
        double reducedDamage = originalDamage * (1.0 - (damageReduction / 100.0));
        damageEvent.setDamage(reducedDamage);
        
        double damageBlocked = originalDamage - reducedDamage;
        
        // Visual feedback
        Location loc = player.getLocation();
        if (loc != null && loc.getWorld() != null) {
            // Stone barrier particles
            loc.getWorld().spawnParticle(
                Particle.BLOCK,
                loc.add(0, 1, 0),
                (int) (15 * reductionProgress),
                0.5, 0.8, 0.5,
                0.05,
                Material.STONE.createBlockData()
            );
            
            // Crack particles on heavy hits
            if (damageBlocked > 5.0) {
                loc.getWorld().spawnParticle(
                    Particle.BLOCK,
                    loc,
                    20,
                    0.6, 1.0, 0.6,
                    0.1,
                    Material.COBBLESTONE.createBlockData()
                );
                loc.getWorld().playSound(loc, Sound.BLOCK_STONE_HIT, 1.0f, 0.8f);
            }
        }
        
        // Feedback message
        String rootBar = "▮".repeat((int) (reductionProgress * 10)) + "▯".repeat(10 - (int) (reductionProgress * 10));
        player.sendMessage("§6⚔ §7Terraheart §8[" + rootBar + "§8] §7-" + String.format("%.1f", damageBlocked) + " dmg");
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        // Delegate to the quality-only version
        trigger(player, quality, event);
    }
    
    /**
     * Start monitoring player movement to track stationary time
     */
    private void startMovementMonitor() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Main.getInstance().getServer().getOnlinePlayers()) {
                    // Check if player has this enchantment equipped
                    ItemStack chest = player.getInventory().getChestplate();
                    ItemStack offhand = player.getInventory().getItemInOffHand();
                    
                    boolean hasEnchant = false;
                    if (chest != null && canApplyTo(chest)) {
                        hasEnchant = true;
                    } else if (offhand != null && canApplyTo(offhand)) {
                        hasEnchant = true;
                    }
                    
                    if (!hasEnchant) {
                        // Cleanup if they don't have the enchant
                        lastLocation.remove(player.getUniqueId());
                        stationaryTime.remove(player.getUniqueId());
                        rootedTicks.remove(player.getUniqueId());
                        continue;
                    }
                    
                    UUID playerId = player.getUniqueId();
                    Location currentLoc = player.getLocation();
                    Location lastLoc = lastLocation.get(playerId);
                    
                    if (lastLoc == null) {
                        lastLocation.put(playerId, currentLoc);
                        stationaryTime.put(playerId, System.currentTimeMillis());
                        rootedTicks.put(playerId, 0);
                        continue;
                    }
                    
                    // Check if player moved
                    double distance = currentLoc.distance(lastLoc);
                    
                    if (distance < MOVEMENT_THRESHOLD) {
                        // Still stationary
                        int ticks = rootedTicks.getOrDefault(playerId, 0);
                        if (ticks < MAX_ROOTED_TICKS) {
                            rootedTicks.put(playerId, ticks + 1);
                            
                            // Show rooting particles periodically
                            if (ticks % 20 == 0 && ticks > 0) {
                                Location loc = player.getLocation();
                                if (loc.getWorld() != null) {
                                    loc.getWorld().spawnParticle(
                                        Particle.BLOCK,
                                        loc,
                                        5,
                                        0.3, 0.1, 0.3,
                                        0.0,
                                        Material.ROOTED_DIRT.createBlockData()
                                    );
                                }
                            }
                            
                            // Full root notification
                            if (ticks == MAX_ROOTED_TICKS) {
                                player.sendMessage("§6⚔ §7Terraheart §afully rooted! §7(Max protection)");
                                Location loc = player.getLocation();
                                if (loc.getWorld() != null) {
                                    loc.getWorld().playSound(loc, Sound.BLOCK_ROOTS_PLACE, 1.0f, 0.8f);
                                    loc.getWorld().spawnParticle(
                                        Particle.BLOCK,
                                        loc.add(0, 0.5, 0),
                                        30,
                                        0.5, 0.5, 0.5,
                                        0.05,
                                        Material.ROOTED_DIRT.createBlockData()
                                    );
                                }
                            }
                        }
                    } else {
                        // Player moved - reset
                        int previousTicks = rootedTicks.getOrDefault(playerId, 0);
                        lastLocation.put(playerId, currentLoc);
                        stationaryTime.put(playerId, System.currentTimeMillis());
                        rootedTicks.put(playerId, 0);
                        
                        // Notify if they lost significant rooting
                        if (previousTicks > 20) {
                            player.sendMessage("§6⚔ §7Terraheart §cuprooted!");
                            Location loc = player.getLocation();
                            if (loc.getWorld() != null) {
                                loc.getWorld().playSound(loc, Sound.BLOCK_ROOTS_BREAK, 0.8f, 1.0f);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L); // Check every tick
    }
    
    /**
     * Calculate damage reduction bonus from player's earth affinity
     * Returns a flat bonus percentage (0 to +15%)
     */
    private double getAffinityReductionBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return 0.0;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return 0.0;
        
        double earthAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.EARTH);
        
        // Flat bonus: up to +15% at 60 affinity
        return Math.min(15.0, earthAffinity * 0.25);
    }
    
    @Override
    public CustomEnchantment.TriggerType getTriggerType() {
        return CustomEnchantment.TriggerType.ON_DAMAGED;
    }
    
    @Override
    public int[] getAntiSynergyGroups() {
        return new int[]{8}; // Sustain/Barriers
    }
    
    @Override
    public String[] getConflictingEnchantments() {
        return new String[]{"PureReflection"};
    }
}
