package com.server.enchantments.abilities.utility;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentLevel;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Radiant Grace - Light Utility Enchantment
 * 
 * When damaged, releases a healing aura that heals nearby allies
 * Healing amount scales with light affinity
 * Creates radiant particle effects around healed allies
 * 
 * Equipment: All armor pieces (representing robes/amulets)
 * Rarity: RARE
 */
public class RadiantGrace extends CustomEnchantment {
    
    private static final Map<UUID, Long> healCooldowns = new HashMap<>();
    private static final long HEAL_COOLDOWN = 8000; // 8 seconds
    private static final double HEAL_RADIUS = 8.0; // 8 blocks
    
    public RadiantGrace() {
        super(
            "radiant_grace",
            "Radiant Grace",
            "When harmed, emit a healing aura that restores health to nearby allies",
            EnchantmentRarity.RARE,
            ElementType.LIGHT
        );
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // All armor pieces
        return type == Material.LEATHER_HELMET || type == Material.LEATHER_CHESTPLATE ||
               type == Material.LEATHER_LEGGINGS || type == Material.LEATHER_BOOTS ||
               type == Material.CHAINMAIL_HELMET || type == Material.CHAINMAIL_CHESTPLATE ||
               type == Material.CHAINMAIL_LEGGINGS || type == Material.CHAINMAIL_BOOTS ||
               type == Material.IRON_HELMET || type == Material.IRON_CHESTPLATE ||
               type == Material.IRON_LEGGINGS || type == Material.IRON_BOOTS ||
               type == Material.GOLDEN_HELMET || type == Material.GOLDEN_CHESTPLATE ||
               type == Material.GOLDEN_LEGGINGS || type == Material.GOLDEN_BOOTS ||
               type == Material.DIAMOND_HELMET || type == Material.DIAMOND_CHESTPLATE ||
               type == Material.DIAMOND_LEGGINGS || type == Material.DIAMOND_BOOTS ||
               type == Material.NETHERITE_HELMET || type == Material.NETHERITE_CHESTPLATE ||
               type == Material.NETHERITE_LEGGINGS || type == Material.NETHERITE_BOOTS;
    }
    
    @Override
    public double[] getBaseStats() {
        // [heal_amount, heal_radius_bonus]
        return new double[]{3.0, 0.0};
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        if (!(event instanceof EntityDamageEvent)) return;
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check cooldown
        if (healCooldowns.containsKey(playerId)) {
            long lastHeal = healCooldowns.get(playerId);
            if (currentTime - lastHeal < HEAL_COOLDOWN) {
                return; // Still on cooldown
            }
        }
        
        // Get scaled stats
        double[] stats = getScaledStats(quality);
        double baseHealAmount = stats[0];
        
        // Apply light affinity bonus to healing
        double affinityBonus = getAffinityHealingBonus(player);
        double finalHealAmount = baseHealAmount * affinityBonus;
        
        // Calculate heal radius (scales slightly with affinity)
        double finalRadius = HEAL_RADIUS + (getAffinityRadiusBonus(player));
        
        // Find and heal nearby allies
        int healedCount = 0;
        Location playerLoc = player.getLocation();
        
        for (Entity entity : player.getNearbyEntities(finalRadius, finalRadius, finalRadius)) {
            if (!(entity instanceof Player)) continue;
            
            Player ally = (Player) entity;
            
            // Don't heal self (this enchantment is for allies)
            if (ally.getUniqueId().equals(playerId)) continue;
            
            // Check if player is actually an ally (not in combat with them, etc.)
            // For now, heal all nearby players
            
            // Apply healing
            double currentHealth = ally.getHealth();
            double maxHealth = ally.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            double newHealth = Math.min(currentHealth + finalHealAmount, maxHealth);
            ally.setHealth(newHealth);
            
            // Visual effects on healed ally
            createHealingEffect(ally.getLocation());
            
            // Sound for healed ally
            Location allyLoc = ally.getLocation();
            if (allyLoc != null && allyLoc.getWorld() != null) {
                allyLoc.getWorld().playSound(allyLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.5f);
            }
            
            // Feedback to healed ally
            ally.sendMessage("§e☀ Radiant Grace §7healed you for §c+" + String.format("%.1f", finalHealAmount) + "❤");
            
            healedCount++;
        }
        
        // Visual and sound effects at caster location
        if (healedCount > 0) {
            createRadiantAura(playerLoc);
            if (playerLoc != null && playerLoc.getWorld() != null) {
                playerLoc.getWorld().playSound(playerLoc, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.2f);
                playerLoc.getWorld().playSound(playerLoc, Sound.ENTITY_PLAYER_LEVELUP, 0.4f, 1.5f);
            }
            
            // Set cooldown
            healCooldowns.put(playerId, currentTime);
            
            // Feedback to caster
            player.sendMessage("§e☀ Radiant Grace §7healed §e" + healedCount + " §7" + 
                             (healedCount == 1 ? "ally" : "allies") + "!");
        }
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        // Delegate to the quality-only version
        trigger(player, quality, event);
    }
    
    /**
     * Create healing effect on ally
     */
    private void createHealingEffect(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        
        // Rising golden particles
        for (int i = 0; i < 5; i++) {
            double yOffset = i * 0.4;
            Location particleLoc = loc.clone().add(0, yOffset, 0);
            loc.getWorld().spawnParticle(Particle.GLOW, particleLoc, 3, 0.3, 0.1, 0.3, 0.02);
            loc.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 2, 0.2, 0.1, 0.2, 0.01);
        }
        
        // Heart particles
        loc.getWorld().spawnParticle(Particle.HEART, loc.add(0, 1.5, 0), 3, 0.5, 0.3, 0.5, 0);
    }
    
    /**
     * Create radiant aura effect at caster location
     */
    private void createRadiantAura(Location center) {
        if (center == null || center.getWorld() == null) return;
        
        // Expanding ring of light
        for (int ring = 1; ring <= 3; ring++) {
            double radius = ring * 1.5;
            int points = ring * 12;
            
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI * i) / points;
                double x = center.getX() + Math.cos(angle) * radius;
                double z = center.getZ() + Math.sin(angle) * radius;
                double y = center.getY() + 0.2;
                
                Location particleLoc = new Location(center.getWorld(), x, y, z);
                center.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
                center.getWorld().spawnParticle(Particle.GLOW, particleLoc, 1, 0.1, 0.1, 0.1, 0);
            }
        }
        
        // Central pillar of light
        for (int i = 0; i < 10; i++) {
            Location pillarLoc = center.clone().add(0, i * 0.3, 0);
            center.getWorld().spawnParticle(Particle.FIREWORK, pillarLoc, 2, 0.1, 0.1, 0.1, 0.02);
        }
    }
    
    /**
     * Calculate healing bonus from player's light affinity
     * Returns multiplier (1.0 baseline, up to 1.6x with max affinity)
     */
    private double getAffinityHealingBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return 1.0;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return 1.0;
        
        double lightAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.LIGHT);
        
        // +0.6% healing per light affinity, max +60% at 100 affinity
        double bonus = Math.min(0.60, lightAffinity / 166.67);
        return 1.0 + bonus;
    }
    
    /**
     * Calculate radius bonus from player's light affinity
     * Returns additional radius (0 to 2 blocks at 100 affinity)
     */
    private double getAffinityRadiusBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return 0.0;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return 0.0;
        
        double lightAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.LIGHT);
        
        // +0.02 blocks per light affinity, max +2 blocks at 100 affinity
        return Math.min(2.0, lightAffinity / 50.0);
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
        return new String[]{"Mistveil", "Whispers"};
    }
}
