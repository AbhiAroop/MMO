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
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentLevel;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

/**
 * Hollow Edge - Shadow Offensive Enchantment
 * 
 * Restores health and mana when killing enemies
 * Amount restored scales with shadow affinity
 * Creates dark absorption particles on kill
 * 
 * Equipment: Daggers (swords), Scythes (hoes), Katanas (swords)
 * Rarity: UNCOMMON
 */
public class HollowEdge extends CustomEnchantment {
    
    private static final Map<UUID, Long> killCooldowns = new HashMap<>();
    private static final long KILL_COOLDOWN = 500; // 0.5 second between procs
    
    public HollowEdge() {
        super(
            "hollow_edge",
            "Hollow Edge",
            "Drains life essence from slain enemies, restoring health and mana",
            EnchantmentRarity.UNCOMMON,
            ElementType.SHADOW
        );
    }
    
    @Override
    public int getMaxLevel() {
        return 4; // Simple sustain mechanic
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // Daggers (swords), scythes (hoes), katanas (swords)
        return type == Material.WOODEN_SWORD || type == Material.STONE_SWORD ||
               type == Material.IRON_SWORD || type == Material.GOLDEN_SWORD ||
               type == Material.DIAMOND_SWORD || type == Material.NETHERITE_SWORD ||
               type == Material.WOODEN_HOE || type == Material.STONE_HOE ||
               type == Material.IRON_HOE || type == Material.GOLDEN_HOE ||
               type == Material.DIAMOND_HOE || type == Material.NETHERITE_HOE;
    }
    
    @Override
    public double[] getBaseStats() {
        // [health_restore, mana_restore]
        return new double[]{3.0, 5.0};
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        if (!(event instanceof EntityDeathEvent)) return;
        EntityDeathEvent deathEvent = (EntityDeathEvent) event;
        
        LivingEntity killedEntity = deathEvent.getEntity();
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check cooldown
        if (killCooldowns.containsKey(playerId)) {
            long lastKill = killCooldowns.get(playerId);
            if (currentTime - lastKill < KILL_COOLDOWN) {
                return; // Still on cooldown
            }
        }
        
        // Update cooldown
        killCooldowns.put(playerId, currentTime);
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(playerId);
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(playerId)[activeSlot];
        if (profile == null) return;
        
        // Get scaled stats (quality only)
        double[] stats = getScaledStats(quality);
        double baseHealthRestore = stats[0];
        double baseManaRestore = stats[1];
        
        // Apply shadow affinity bonus
        double affinityBonus = getAffinityRestorationBonus(player);
        double healthRestore = baseHealthRestore * affinityBonus;
        double manaRestore = baseManaRestore * affinityBonus;
        
        // Restore health
        double currentHealth = player.getHealth();
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        double newHealth = Math.min(currentHealth + healthRestore, maxHealth);
        player.setHealth(newHealth);
        
        // Restore mana
        int currentMana = profile.getStats().getTotalMana();
        int maxMana = profile.getStats().getMana();
        int newMana = Math.min(currentMana + (int)manaRestore, maxMana);
        profile.getStats().setTotalMana(newMana);
        
        // Visual and sound effects
        createDrainEffect(killedEntity.getLocation(), player.getLocation());
        if (player.getLocation() != null) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 
                                       0.6f, 0.8f);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SOUL_SAND_BREAK,
                                       0.8f, 0.5f);
        }
        
        // Feedback
        player.sendMessage("§5☠ Hollow Edge §7drained §c+" + String.format("%.1f", healthRestore) + "❤ §7and §b+" + (int)manaRestore + "✦");
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, EnchantmentLevel level, Event event) {
        // Delegate to the quality-only version
        trigger(player, quality, event);
    }
    
    /**
     * Create visual drain effect from killed entity to player
     */
    private void createDrainEffect(Location from, Location to) {
        if (from == null || to == null || from.getWorld() == null) return;
        
        // Create particle trail from killed entity to player
        double distance = from.distance(to);
        int points = (int) (distance * 4); // 4 particles per block
        
        for (int i = 0; i <= points; i++) {
            double ratio = (double) i / points;
            
            double x = from.getX() + (to.getX() - from.getX()) * ratio;
            double y = from.getY() + 1.0 + (to.getY() + 1.0 - from.getY() - 1.0) * ratio;
            double z = from.getZ() + (to.getZ() - from.getZ()) * ratio;
            
            Location particleLoc = new Location(from.getWorld(), x, y, z);
            
            // Shadow particles
            from.getWorld().spawnParticle(Particle.SMOKE, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
            from.getWorld().spawnParticle(Particle.SQUID_INK, particleLoc, 2, 0.1, 0.1, 0.1, 0.01);
        }
        
        // Absorption effect at player
        to.getWorld().spawnParticle(Particle.SOUL, to.add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.05);
        to.getWorld().spawnParticle(Particle.SMOKE, to, 10, 0.4, 0.4, 0.4, 0.02);
    }
    
    /**
     * Calculate restoration bonus from player's shadow affinity
     * Returns multiplier (1.0 baseline, up to 1.5x with max affinity)
     */
    private double getAffinityRestorationBonus(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return 1.0;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return 1.0;
        
        double shadowAffinity = profile.getStats().getElementalAffinity().getAffinity(ElementType.SHADOW);
        
        // +1% restoration per 2 shadow affinity, max +50% at 100 affinity
        double bonus = Math.min(0.50, shadowAffinity / 200.0);
        return 1.0 + bonus;
    }
    
    @Override
    public CustomEnchantment.TriggerType getTriggerType() {
        return CustomEnchantment.TriggerType.ON_KILL;
    }
    
    @Override
    public int[] getAntiSynergyGroups() {
        return new int[]{9}; // On-Kill Effects
    }
    
    @Override
    public String[] getConflictingEnchantments() {
        return new String[]{"AshenVeil"};
    }
}
