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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentLevel;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Whispers - Air Defensive Enchantment
 * 
 * The wind whispers warnings, granting a chance to completely evade attacks.
 * Evasion chance increases with air affinity.
 * 
 * Equipment: Helmets, Cloaks (light armor)
 * Rarity: RARE
 * Element: AIR
 */
public class Whispers extends CustomEnchantment {
    
    private static final Map<UUID, Integer> evasionCounter = new HashMap<>();
    
    public Whispers() {
        super(
            "whispers",
            "Whispers",
            "The wind warns of danger, letting you evade attacks",
            EnchantmentRarity.RARE,
            ElementType.AIR
        );
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Can be applied to helmets and light armor
        return type == Material.LEATHER_HELMET || type == Material.CHAINMAIL_HELMET ||
               type == Material.IRON_HELMET || type == Material.GOLDEN_HELMET ||
               type == Material.DIAMOND_HELMET || type == Material.NETHERITE_HELMET ||
               type == Material.LEATHER_CHESTPLATE || type == Material.CHAINMAIL_CHESTPLATE;
    }
    
    @Override
    public double[] getBaseStats() {
        // [evasion_chance_percentage]
        return new double[]{15.0}; // 15% base evasion chance
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent)) return;
        EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
        
        // Get scaled stats (quality only)
        double[] stats = getScaledStats(quality);
        double baseChance = stats[0];
        
        // Apply affinity modifier to evasion chance
        double affinityBonus = getAffinityEvasionBonus(player);
        double finalChance = Math.min(40.0, baseChance + affinityBonus); // Cap at 40%
        
        // Roll for evasion
        if (Math.random() * 100 > finalChance) {
            return; // Evasion failed
        }
        
        // Evade the attack!
        damageEvent.setCancelled(true);
        
        // Track evasions
        UUID playerId = player.getUniqueId();
        int count = evasionCounter.getOrDefault(playerId, 0) + 1;
        evasionCounter.put(playerId, count);
        
        // Dodge effect - slight push away
        if (damageEvent.getDamager() != null) {
            Location damagerLoc = damageEvent.getDamager().getLocation();
            Location playerLoc = player.getLocation();
            
            if (damagerLoc != null && playerLoc != null) {
                org.bukkit.util.Vector pushDirection = playerLoc.toVector()
                    .subtract(damagerLoc.toVector())
                    .normalize()
                    .multiply(0.5);
                pushDirection.setY(0.2);
                player.setVelocity(pushDirection);
            }
        }
        
        // Visual and sound effects
        Location loc = player.getLocation();
        if (loc != null && loc.getWorld() != null) {
            // Wind swirl particles
            for (int i = 0; i < 360; i += 30) {
                double angle = Math.toRadians(i);
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                
                Location particleLoc = loc.clone().add(x, 1, z);
                loc.getWorld().spawnParticle(
                    Particle.CLOUD,
                    particleLoc,
                    2,
                    0.1, 0.1, 0.1,
                    0.05
                );
            }
            
            // Sweep attack visual
            loc.getWorld().spawnParticle(
                Particle.SWEEP_ATTACK,
                loc.add(0, 1, 0),
                5,
                0.8, 0.5, 0.8,
                0.0
            );
            
            // Wind whisper sound
            loc.getWorld().playSound(loc, Sound.ENTITY_PHANTOM_FLAP, 0.7f, 1.8f);
            loc.getWorld().playSound(loc, Sound.ITEM_ARMOR_EQUIP_ELYTRA, 0.9f, 1.5f);
        }
        
        // Feedback message
        player.sendMessage("§6⚔ §7Whispers §ewarned you! §7Evaded! §8(" + count + " total)");
        
        // Send message to attacker if it's a player
        if (damageEvent.getDamager() instanceof Player) {
            Player attacker = (Player) damageEvent.getDamager();
            attacker.sendMessage("§7" + player.getName() + " §fevaded your attack!");
        }
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        // Delegate to the quality-only version
        trigger(player, quality, event);
    }
    
    /**
     * Calculate evasion bonus from player's air affinity
     * Returns a flat bonus percentage (0 to +20%)
     */
    private double getAffinityEvasionBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return 0.0;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return 0.0;
        
        double airAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.AIR);
        
        // Flat bonus: up to +20% at 60 affinity
        return Math.min(20.0, airAffinity * 0.333); // 60 * 0.333 ≈ 20
    }
    
    @Override
    public CustomEnchantment.TriggerType getTriggerType() {
        return CustomEnchantment.TriggerType.ON_DAMAGED;
    }
    
    @Override
    public int[] getAntiSynergyGroups() {
        return new int[]{5}; // Defensive Response
    }
    
    @Override
    public String[] getConflictingEnchantments() {
        return new String[]{"Mistveil", "RadiantGrace"};
    }
}
