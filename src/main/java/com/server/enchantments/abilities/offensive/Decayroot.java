package com.server.enchantments.abilities.offensive;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentLevel;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;
import com.server.enchantments.elements.HybridElement;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Decayroot - Earth/Shadow Hybrid Offensive Enchantment
 * 
 * Roots enemies in place with shadow-corrupted earth
 * Applies stacking weakness and wither damage over time
 * Earth primary (60%), Shadow secondary (40%)
 * 
 * Equipment: Maces (axes), Hammers (axes), Staves (sticks)
 * Rarity: EPIC
 */
public class Decayroot extends CustomEnchantment {
    
    private static final Map<UUID, Long> rootCooldowns = new HashMap<>();
    private static final Map<UUID, Integer> rootStacks = new HashMap<>();
    private static final long ROOT_COOLDOWN = 5000; // 5 seconds
    private static final int MAX_STACKS = 3;
    
    public Decayroot() {
        super(
            "decayroot",
            "Decayroot",
            "Corrupt the earth beneath your foes, rooting them with shadow-infused decay",
            EnchantmentRarity.EPIC,
            HybridElement.DECAY
        );
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Maces/hammers (axes), staves (sticks in offhand)
        return type == Material.WOODEN_AXE || type == Material.STONE_AXE ||
               type == Material.IRON_AXE || type == Material.GOLDEN_AXE ||
               type == Material.DIAMOND_AXE || type == Material.NETHERITE_AXE ||
               type == Material.STICK;
    }
    
    @Override
    public double[] getBaseStats() {
        // [root_duration_ticks, wither_duration_ticks, weakness_amplifier]
        return new double[]{60.0, 80.0, 1.0};
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent)) return;
        EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
        
        if (!(damageEvent.getEntity() instanceof LivingEntity)) return;
        LivingEntity target = (LivingEntity) damageEvent.getEntity();
        
        UUID playerId = player.getUniqueId();
        UUID targetId = target.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check cooldown
        if (rootCooldowns.containsKey(playerId)) {
            long lastRoot = rootCooldowns.get(playerId);
            if (currentTime - lastRoot < ROOT_COOLDOWN) {
                return;
            }
        }
        
        // Update cooldown
        rootCooldowns.put(playerId, currentTime);
        
        // Get or increment stacks
        int currentStacks = rootStacks.getOrDefault(targetId, 0);
        currentStacks = Math.min(currentStacks + 1, MAX_STACKS);
        rootStacks.put(targetId, currentStacks);
        
        // Get scaled stats
        double[] stats = getScaledStats(quality);
        int baseRootDuration = (int) stats[0];
        int baseWitherDuration = (int) stats[1];
        int baseWeaknessAmplifier = (int) stats[2];
        
        // Apply hybrid affinity bonuses (60% earth, 40% shadow)
        double[] affinityBonuses = getHybridAffinityBonus(player);
        double earthBonus = affinityBonuses[0]; // Primary (60%)
        double shadowBonus = affinityBonuses[1]; // Secondary (40%)
        
        // Earth affects root duration and weakness, Shadow affects wither damage
        int finalRootDuration = (int) (baseRootDuration * (1.0 + earthBonus * 0.5)); // +50% max from earth
        int finalWitherDuration = (int) (baseWitherDuration * (1.0 + shadowBonus * 0.6)); // +60% max from shadow
        int finalWeaknessAmplifier = baseWeaknessAmplifier + (int) (earthBonus * 2); // +2 max amplifier from earth
        
        // Apply root (slowness 10 = nearly immobile)
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, finalRootDuration, 10, false, true, true));
        
        // Apply weakness (stacks with repeated hits)
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, finalRootDuration, 
                                                finalWeaknessAmplifier + currentStacks - 1, false, true, true));
        
        // Apply wither damage
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, finalWitherDuration, 
                                                currentStacks - 1, false, true, true));
        
        // Create decay visual
        createDecayEffect(target.getLocation(), currentStacks);
        
        // Visual and sound effects
        Location targetLoc = target.getLocation();
        if (targetLoc != null && targetLoc.getWorld() != null) {
            targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_ROOTS_BREAK, 1.0f, 0.6f);
            targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.8f);
        }
        
        // Feedback
        player.sendMessage("§8☠ Decayroot §7rooted §f" + target.getName() + 
                          " §7[§8" + currentStacks + "§7/§8" + MAX_STACKS + "§7]");
        
        // Clear stacks after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                rootStacks.remove(targetId);
            }
        }.runTaskLater(Main.getInstance(), finalRootDuration + 20L);
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        trigger(player, quality, event);
    }
    
    /**
     * Create visual decay effect with roots
     */
    private void createDecayEffect(Location center, int stacks) {
        if (center == null || center.getWorld() == null) return;
        
        // Ground roots pattern
        for (int i = 0; i < 12 * stacks; i++) {
            double angle = (2 * Math.PI * i) / (12 * stacks);
            double radius = 1.0 + (stacks * 0.3);
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            double y = center.getY() + 0.1;
            
            Location particleLoc = new Location(center.getWorld(), x, y, z);
            
            // Root/earth particles
            center.getWorld().spawnParticle(Particle.BLOCK, particleLoc, 1, 0, 0, 0, 0, 
                                           Material.ROOTED_DIRT.createBlockData());
            
            // Shadow corruption
            if (Math.random() < 0.4) {
                center.getWorld().spawnParticle(Particle.SQUID_INK, particleLoc.add(0, 0.5, 0), 
                                               1, 0.1, 0.2, 0.1, 0.01);
            }
        }
        
        // Rising decay particles
        for (int i = 0; i < 5; i++) {
            double yOffset = i * 0.4;
            Location pillarLoc = center.clone().add(0, yOffset, 0);
            center.getWorld().spawnParticle(Particle.SMOKE, pillarLoc, 2, 0.2, 0.1, 0.2, 0.01);
            center.getWorld().spawnParticle(Particle.ASH, pillarLoc, 3, 0.3, 0.1, 0.3, 0.01);
        }
    }
    
    /**
     * Get hybrid affinity bonuses [earth, shadow]
     * Earth is primary (60%), Shadow is secondary (40%)
     */
    private double[] getHybridAffinityBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return new double[]{0.0, 0.0};
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return new double[]{0.0, 0.0};
        
        double earthAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.EARTH);
        double shadowAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.SHADOW);
        
        // 60% weight for primary (earth), 40% weight for secondary (shadow)
        double earthBonus = Math.min(0.60, earthAffinity / 100.0 * 0.6);
        double shadowBonus = Math.min(0.40, shadowAffinity / 100.0 * 0.4);
        
        return new double[]{earthBonus, shadowBonus};
    }
    
    @Override
    public CustomEnchantment.TriggerType getTriggerType() {
        return CustomEnchantment.TriggerType.ON_HIT;
    }
}
