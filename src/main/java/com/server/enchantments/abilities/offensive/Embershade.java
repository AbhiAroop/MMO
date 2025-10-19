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
 * Embershade - Fire/Shadow Hybrid Offensive Enchantment
 * 
 * Burns enemies with dark flames that obscure vision and weaken defenses
 * Applies blindness and increases damage taken
 * Fire primary (60%), Shadow secondary (40%)
 * 
 * Equipment: Daggers (swords), Scythes (hoes), Bows
 * Rarity: EPIC
 */
public class Embershade extends CustomEnchantment {
    
    private static final Map<UUID, Long> embershadeCooldowns = new HashMap<>();
    private static final long EMBERSHADE_COOLDOWN = 7000; // 7 seconds
    
    public Embershade() {
        super(
            "embershade",
            "Embershade",
            "Engulf foes in dark flames that blind and weaken their defenses",
            EnchantmentRarity.EPIC,
            HybridElement.ASH
        );
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Daggers/scythes (swords/hoes), bows
        return type == Material.WOODEN_SWORD || type == Material.STONE_SWORD ||
               type == Material.IRON_SWORD || type == Material.GOLDEN_SWORD ||
               type == Material.DIAMOND_SWORD || type == Material.NETHERITE_SWORD ||
               type == Material.WOODEN_HOE || type == Material.STONE_HOE ||
               type == Material.IRON_HOE || type == Material.GOLDEN_HOE ||
               type == Material.DIAMOND_HOE || type == Material.NETHERITE_HOE ||
               type == Material.BOW;
    }
    
    @Override
    public double[] getBaseStats() {
        // [burn_duration_ticks, blind_duration_ticks, damage_vulnerability_percent]
        return new double[]{100.0, 60.0, 15.0};
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent)) return;
        EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
        
        if (!(damageEvent.getEntity() instanceof LivingEntity)) return;
        LivingEntity target = (LivingEntity) damageEvent.getEntity();
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check cooldown
        if (embershadeCooldowns.containsKey(playerId)) {
            long lastEmbershade = embershadeCooldowns.get(playerId);
            if (currentTime - lastEmbershade < EMBERSHADE_COOLDOWN) {
                return;
            }
        }
        
        // Update cooldown
        embershadeCooldowns.put(playerId, currentTime);
        
        // Get scaled stats
        double[] stats = getScaledStats(quality);
        int baseBurnDuration = (int) stats[0];
        int baseBlindDuration = (int) stats[1];
        double baseVulnerability = stats[2];
        
        // Apply hybrid affinity bonuses (60% fire, 40% shadow)
        double[] affinityBonuses = getHybridAffinityBonus(player);
        double fireBonus = affinityBonuses[0]; // Primary (60%)
        double shadowBonus = affinityBonuses[1]; // Secondary (40%)
        
        // Fire affects burn duration and vulnerability, Shadow affects blind duration
        int finalBurnDuration = (int) (baseBurnDuration * (1.0 + fireBonus * 0.6)); // +60% max from fire
        int finalBlindDuration = (int) (baseBlindDuration * (1.0 + shadowBonus * 0.5)); // +50% max from shadow
        double finalVulnerability = baseVulnerability * (1.0 + fireBonus * 0.4); // +40% max vulnerability from fire
        
        // Apply dark fire
        target.setFireTicks(finalBurnDuration);
        
        // Apply blindness
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, finalBlindDuration, 0, false, true, true));
        
        // Apply damage vulnerability (unluck increases damage taken)
        int vulnerabilityAmplifier = (int) (finalVulnerability / 10.0);
        target.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, finalBurnDuration, 
                                                vulnerabilityAmplifier, false, true, true));
        
        // Create dark flame aura
        createEmbershadeAura(target, finalBurnDuration);
        
        // Visual and sound effects
        Location targetLoc = target.getLocation();
        if (targetLoc != null && targetLoc.getWorld() != null) {
            targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.6f);
            targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_PHANTOM_AMBIENT, 0.7f, 0.8f);
        }
        
        // Feedback
        player.sendMessage("§8💀 §c🔥 Embershade §7consumed §f" + target.getName() + " §7in dark flames!");
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        trigger(player, quality, event);
    }
    
    /**
     * Create dark flame aura around target
     */
    private void createEmbershadeAura(LivingEntity target, int duration) {
        new BukkitRunnable() {
            private int ticks = 0;
            
            @Override
            public void run() {
                if (!target.isValid() || target.isDead() || ticks >= duration) {
                    this.cancel();
                    return;
                }
                
                Location loc = target.getLocation();
                if (loc == null || loc.getWorld() == null) {
                    this.cancel();
                    return;
                }
                
                // Dark flames circling the target
                double radius = 0.8;
                int points = 8;
                double angleOffset = (ticks * 0.2) % (2 * Math.PI);
                
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i / points) + angleOffset;
                    double x = loc.getX() + Math.cos(angle) * radius;
                    double z = loc.getZ() + Math.sin(angle) * radius;
                    double y = loc.getY() + 1.0 + (Math.sin(angle + ticks * 0.1) * 0.3);
                    
                    Location flameLoc = new Location(loc.getWorld(), x, y, z);
                    
                    // Dark red/black flames
                    loc.getWorld().spawnParticle(Particle.FLAME, flameLoc, 1, 0.05, 0.05, 0.05, 0.01);
                    loc.getWorld().spawnParticle(Particle.SMOKE, flameLoc, 1, 0.05, 0.05, 0.05, 0.01);
                    
                    // Shadow particles
                    if (Math.random() < 0.3) {
                        loc.getWorld().spawnParticle(Particle.SQUID_INK, flameLoc, 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
                
                // Rising ash
                loc.getWorld().spawnParticle(Particle.ASH, loc.clone().add(0, 1.5, 0), 2, 0.3, 0.5, 0.3, 0.01);
                
                ticks += 3;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 3L);
    }
    
    /**
     * Get hybrid affinity bonuses [fire, shadow]
     * Fire is primary (60%), Shadow is secondary (40%)
     */
    private double[] getHybridAffinityBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return new double[]{0.0, 0.0};
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return new double[]{0.0, 0.0};
        
        double fireAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.FIRE);
        double shadowAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.SHADOW);
        
        // 60% weight for primary (fire), 40% weight for secondary (shadow)
        double fireBonus = Math.min(0.60, fireAffinity / 100.0 * 0.6);
        double shadowBonus = Math.min(0.40, shadowAffinity / 100.0 * 0.4);
        
        return new double[]{fireBonus, shadowBonus};
    }
    
    @Override
    public CustomEnchantment.TriggerType getTriggerType() {
        return CustomEnchantment.TriggerType.ON_HIT;
    }
    
    @Override
    public int[] getAntiSynergyGroups() {
        return new int[]{1}; // Fire Damage
    }
    
    @Override
    public String[] getConflictingEnchantments() {
        return new String[]{"Cinderwake", "Stormfire"};
    }
}
