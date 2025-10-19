package com.server.enchantments.abilities.utility;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
import com.server.profiles.stats.PlayerStats;

/**
 * Arc Nexus - Lightning Utility Enchantment
 * 
 * Builds up attack speed with each consecutive hit
 * Grants temporary attack speed player stat bonus based on stacks
 * Stacks decay after brief period without hitting, resetting attack speed
 * Lightning affinity increases max stacks
 * 
 * Equipment: All armor pieces (representing gauntlets/rings/amulets)
 * Rarity: UNCOMMON
 */
public class ArcNexus extends CustomEnchantment {
    
    private static final String ARC_NEXUS_MODIFIER = "mmo.arc_nexus.attack_speed";
    
    // Stack mechanics
    private static class StackData {
        int currentStacks;
        long lastHitTime;
        BukkitRunnable decayTask;
        double originalAttackSpeed; // Store the original attack speed
        
        StackData(int stacks, long time, double originalSpeed) {
            this.currentStacks = stacks;
            this.lastHitTime = time;
            this.originalAttackSpeed = originalSpeed;
        }
    }
    
    private static final Map<UUID, StackData> playerStacks = new HashMap<>();
    private static final int STACK_DECAY_DELAY_TICKS = 60; // 3 seconds without hit
    
    public ArcNexus() {
        super(
            "arc_nexus",
            "Arc Nexus",
            "Each hit builds up attack speed stat, granting stacking bonuses",
            EnchantmentRarity.UNCOMMON,
            ElementType.LIGHTNING
        );
    }
    
    @Override
    public int getMaxLevel() {
        return 5; // Attack speed stacking - balanced
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Can be applied to all armor pieces
        return type == Material.DIAMOND_HELMET || type == Material.DIAMOND_CHESTPLATE ||
               type == Material.DIAMOND_LEGGINGS || type == Material.DIAMOND_BOOTS ||
               type == Material.NETHERITE_HELMET || type == Material.NETHERITE_CHESTPLATE ||
               type == Material.NETHERITE_LEGGINGS || type == Material.NETHERITE_BOOTS ||
               type == Material.IRON_HELMET || type == Material.IRON_CHESTPLATE ||
               type == Material.IRON_LEGGINGS || type == Material.IRON_BOOTS ||
               type == Material.GOLDEN_HELMET || type == Material.GOLDEN_CHESTPLATE ||
               type == Material.GOLDEN_LEGGINGS || type == Material.GOLDEN_BOOTS;
    }
    
    @Override
    public double[] getBaseStats() {
        // [max_stacks, attack_speed_per_stack]
        return new double[]{5.0, 0.15};
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent)) return;
        EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
        
        if (!(damageEvent.getEntity() instanceof LivingEntity)) return;
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Get player profile and stats
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(playerId);
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(playerId)[activeSlot];
        if (profile == null) return;
        
        final PlayerStats playerStats = profile.getStats();
        
        // Get scaled stats (quality only)
        double[] stats = getScaledStats(quality);
        int baseMaxStacks = (int) Math.round(stats[0]);
        double attackSpeedPerStack = stats[1];
        
        // Apply affinity modifier for max stacks
        int maxStacks = Math.min(baseMaxStacks + getAffinityStackBonus(player), 8);
        
        // Get or create stack data
        StackData stackData = playerStacks.get(playerId);
        
        if (stackData == null) {
            // First hit - create new stack and store original attack speed
            double originalSpeed = playerStats.getAttackSpeed();
            stackData = new StackData(1, currentTime, originalSpeed);
            playerStacks.put(playerId, stackData);
        } else {
            // Check if we should decay stacks
            long timeSinceLastHit = currentTime - stackData.lastHitTime;
            if (timeSinceLastHit > STACK_DECAY_DELAY_TICKS * 50L) { // Convert to ms
                // Decay all stacks - reset to original attack speed
                playerStats.setAttackSpeed(stackData.originalAttackSpeed);
                playerStats.applyToPlayer(player);
                
                // Remove Arc Nexus attack speed bonus
                applyAttackSpeedAttribute(player, 0);
                
                // Restart with 1 stack
                stackData.currentStacks = 1;
                stackData.originalAttackSpeed = playerStats.getAttackSpeed();
            } else {
                // Add stack if under max
                if (stackData.currentStacks < maxStacks) {
                    stackData.currentStacks++;
                }
            }
            stackData.lastHitTime = currentTime;
            
            // Cancel existing decay task
            if (stackData.decayTask != null) {
                stackData.decayTask.cancel();
            }
        }
        
        // Calculate and apply attack speed bonus based on stacks
        double totalAttackSpeedBonus = stackData.currentStacks * attackSpeedPerStack;
        double newAttackSpeed = stackData.originalAttackSpeed + totalAttackSpeedBonus;
        playerStats.setAttackSpeed(newAttackSpeed);
        playerStats.applyToPlayer(player);
        
        // Apply attack speed attribute directly
        applyAttackSpeedAttribute(player, totalAttackSpeedBonus);
        
        // Visual and audio feedback
        createStackParticles(player, stackData.currentStacks);
        if (player.getLocation() != null) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 
                                       0.5f, 1.0f + (stackData.currentStacks * 0.1f));
        }
        
        // Show stack count with attack speed value
        String speedDisplay = String.format("%.2f", newAttackSpeed);
        if (stackData.currentStacks == maxStacks) {
            player.sendMessage("§e⚡ Arc Nexus §7[§e" + stackData.currentStacks + "§7/§e" + maxStacks + "§7] §6MAX! §7(§e" + speedDisplay + " AS§7)");
        } else {
            player.sendMessage("§e⚡ Arc Nexus §7[§e" + stackData.currentStacks + "§7/§e" + maxStacks + "§7] §7(§e" + speedDisplay + " AS§7)");
        }
        
        // Store final reference to original speed for decay task
        final double originalSpeed = stackData.originalAttackSpeed;
        
        // Schedule decay task
        stackData.decayTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Reset attack speed to original value
                playerStats.setAttackSpeed(originalSpeed);
                playerStats.applyToPlayer(player);
                
                // Remove Arc Nexus attack speed bonus
                applyAttackSpeedAttribute(player, 0);
                
                // Decay all stacks
                playerStacks.remove(playerId);
                player.sendMessage("§e⚡ Arc Nexus §7stacks have decayed");
                if (player.getLocation() != null) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 
                                              0.3f, 0.5f);
                }
            }
        };
        stackData.decayTask.runTaskLater(Main.getInstance(), STACK_DECAY_DELAY_TICKS);
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        // Delegate to the quality-only version
        trigger(player, quality, event);
    }
    
    /**
     * Directly apply attack speed attribute modifier
     */
    private void applyAttackSpeedAttribute(Player player, double attackSpeedBonus) {
        try {
            AttributeInstance attackSpeed = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (attackSpeed == null) return;
            
            // Remove existing Arc Nexus modifier
            for (AttributeModifier mod : attackSpeed.getModifiers()) {
                if (mod.getName().equals(ARC_NEXUS_MODIFIER)) {
                    attackSpeed.removeModifier(mod);
                }
            }
            
            // Add new modifier if bonus is not zero
            if (attackSpeedBonus != 0) {
                AttributeModifier modifier = new AttributeModifier(
                    UUID.randomUUID(),
                    ARC_NEXUS_MODIFIER,
                    attackSpeedBonus,
                    AttributeModifier.Operation.ADD_NUMBER
                );
                attackSpeed.addModifier(modifier);
            }
        } catch (Exception e) {
            // Silent fail
        }
    }
    
    /**
     * Create particle effects based on stack count
     */
    private void createStackParticles(Player player, int stacks) {
        // Create spiral of particles around player
        double radius = 0.5 + (stacks * 0.1);
        int particleCount = stacks * 3;
        
        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double x = player.getLocation().getX() + radius * Math.cos(angle);
            double z = player.getLocation().getZ() + radius * Math.sin(angle);
            double y = player.getLocation().getY() + 1.0 + (Math.sin(angle) * 0.3);
            
            player.getWorld().spawnParticle(
                Particle.ELECTRIC_SPARK,
                x, y, z,
                1, 0, 0, 0, 0.02
            );
        }
        
        // Additional particles at max stacks
        if (stacks >= 5) {
            player.getWorld().spawnParticle(
                Particle.END_ROD,
                player.getLocation().add(0, 1.5, 0),
                10, 0.3, 0.3, 0.3, 0.1
            );
        }
    }
    
    /**
     * Calculate bonus stacks from player's lightning affinity
     * Returns number of additional stacks (0-3)
     */
    private int getAffinityStackBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return 0;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return 0;
        
        double lightningAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.LIGHTNING);
        
        // +1 stack every 20 affinity, max +3 stacks
        return Math.min((int) (lightningAffinity / 20.0), 3);
    }
    
    @Override
    public CustomEnchantment.TriggerType getTriggerType() {
        return CustomEnchantment.TriggerType.ON_HIT;
    }
    
    @Override
    public int[] getAntiSynergyGroups() {
        return new int[]{6}; // Attack Speed Modifiers
    }
    
    @Override
    public String[] getConflictingEnchantments() {
        return new String[]{}; // Sole member of group 6
    }
}
