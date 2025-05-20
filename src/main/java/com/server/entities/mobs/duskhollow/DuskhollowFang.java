package com.server.entities.mobs.duskhollow;

import java.lang.reflect.Method;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.entities.CustomEntityManager;
import com.server.entities.CustomMobStats;
import com.server.entities.MobType;
import com.server.entities.mobs.CustomMob;

/**
 * Duskhollow Fang - a spectral dark wolf with high damage and lifesteal abilities
 */
public class DuskhollowFang extends CustomMob {
    
    private static final String MODEL_ID = "darkwolf";
    private static final String CUSTOM_NAME = "§5Duskhollow Fang";
    private static final String METADATA_KEY = "duskhollow_fang";
    private static final double ATTACK_RANGE = 5.0;
    private static final double BASE_LIFESTEAL_PERCENT = 25.0; // Increased base lifesteal to compensate for removal of empowered state
    
    public DuskhollowFang(Main plugin, CustomEntityManager entityManager) {
        super(plugin, entityManager);
    }
    
    @Override
    public LivingEntity spawn(Location location) {
        LivingEntity entity = entityManager.spawnCustomMob(getEntityType(), location, getModelId(), getCustomName());
        
        if (entity != null) {
            // Create and apply custom stats
            CustomMobStats stats = createStats();
            
            // Apply base entity settings
            try {
                applyBaseEntityModifications(entity, stats);
                
                // Specific modifications for Wolf entity
                if (entity instanceof Wolf) {
                    Wolf wolf = (Wolf) entity;
                    wolf.setAngry(true); // Make it look angry
                    wolf.setAdult(); // Ensure it's an adult
                }
                
                // Remove attack AI completely
                removeAttackGoals(entity);
                
                // Register listeners to prevent unwanted behavior
                registerDamagePreventionListener(entity);
                registerDamageCancellationListener(entity);
                
                // Customize movement speed
                if (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                    entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.35); // Slightly faster by default
                }
                
                // Store the stats in the entity manager
                entityManager.registerMobStats(entity.getUniqueId(), stats);
                
                // Set custom metadata for nameplate height adjustment
                entity.setMetadata("nameplate_height_offset", new FixedMetadataValue(plugin, 4.0));
                
                // Update the display name
                entityManager.updateEntityNameplate(entity);
                
                // Initialize behavior
                initializeBehavior(entity, stats);
                
                // Play idle animation after spawning
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    playIdleAnimation(entity);
                }, 5L);
            } catch (Exception e) {
                plugin.debugLog(DebugSystem.ENTITY,"Error setting Duskhollow Fang attributes: " + e.getMessage());
                if (plugin.isDebugEnabled(DebugSystem.ENTITY)) {
                    e.printStackTrace();
                }
            }
        }
        
        return entity;
    }
    
    @Override
    public void initializeBehavior(LivingEntity entity, CustomMobStats stats) {
        // Track whether an attack animation is currently playing
        entity.setMetadata("attack_cooldown", new FixedMetadataValue(plugin, false));
        
        // Start a task to handle custom follow behavior
        behaviorTask = new BukkitRunnable() {
            @Override
            public void run() {
                // If entity is dead or no longer valid, cancel task
                if (!entity.isValid() || entity.isDead()) {
                    this.cancel();
                    return;
                }
                
                // Check if attack is on cooldown
                boolean onCooldown = entity.hasMetadata("attack_cooldown") && 
                                     entity.getMetadata("attack_cooldown").get(0).asBoolean();
                
                // Find closest player within 15 blocks
                Player targetPlayer = null;
                double closestDistance = 15.0;
                
                for (Entity nearby : entity.getNearbyEntities(15, 15, 15)) {
                    if (nearby instanceof Player) {
                        Player player = (Player) nearby;
                        
                        // Skip players in creative/spectator mode
                        if (player.getGameMode() == GameMode.CREATIVE || 
                            player.getGameMode() == GameMode.SPECTATOR) {
                            continue;
                        }
                        
                        double distance = entity.getLocation().distance(player.getLocation());
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            targetPlayer = player;
                        }
                    }
                }
                
                // If we found a player to follow
                if (targetPlayer != null) {
                    Player player = targetPlayer;
                    
                    // If within attack range and not on cooldown, attack
                    if (closestDistance <= ATTACK_RANGE && !onCooldown) {
                        // Set cooldown flag
                        entity.setMetadata("attack_cooldown", new FixedMetadataValue(plugin, true));
                        
                        // Look at the player
                        Location lookLoc = entity.getLocation().clone();
                        lookLoc.setDirection(player.getLocation().subtract(entity.getLocation()).toVector());
                        entity.teleport(lookLoc);
                        
                        // Perform regular attack with lifesteal
                        performAttack(entity, player, stats);
                    }
                    // If not in attack range, follow the player
                    else if (closestDistance > 2.0) {
                        // Use Bukkit's pathfinding to follow the player
                        if (entity instanceof Mob) {
                            Mob mob = (Mob) entity;
                            mob.getPathfinder().moveTo(player.getLocation(), 1.0);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }
    
    /**
     * Perform an attack against the target
     */
    private void performAttack(LivingEntity entity, Player player, CustomMobStats stats) {
        // Play attack animation and sound
        entityManager.playAnimation(entity, "attack1");
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WOLF_GROWL, 1.2f, 0.7f);
        
        if (plugin.isDebugEnabled(DebugSystem.ENTITY)) {
            plugin.debugLog(DebugSystem.ENTITY,getCustomName() + " playing attack1 animation at player: " + player.getName());
        }
        
        // Deal damage after a delay to match animation
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (entity.isValid() && !entity.isDead() && player.isOnline()) {
                // Check if still in range
                if (entity.getLocation().distance(player.getLocation()) <= 7.0) {
                    // Get the exact physical damage from stats
                    double damage = stats.getPhysicalDamage();
                    
                    // Use the CUSTOM cause to avoid our own damage cancellation
                    EntityDamageEvent customDamageEvent = new EntityDamageEvent(
                        player, 
                        EntityDamageEvent.DamageCause.CUSTOM,
                        damage
                    );
                    plugin.getServer().getPluginManager().callEvent(customDamageEvent);
                    
                    if (!customDamageEvent.isCancelled()) {
                        // Apply the damage directly
                        double finalDamage = customDamageEvent.getFinalDamage();
                        player.damage(finalDamage);
                        
                        // Ensure damage attribution - set the entity as the last damager
                        try {
                            // Use reflection to get access to CraftLivingEntity's setLastDamager method
                            Method setLastDamager = player.getClass().getDeclaredMethod(
                                "setLastDamager", 
                                org.bukkit.entity.Entity.class
                            );
                            setLastDamager.setAccessible(true);
                            setLastDamager.invoke(player, entity);
                        } catch (Exception e) {
                            // If reflection fails, do a tiny amount of direct damage for attribution
                            player.damage(0.0, entity);
                        }
                        
                        // Apply lifesteal
                        double healAmount = finalDamage * (BASE_LIFESTEAL_PERCENT / 100.0);
                        double newHealth = Math.min(entity.getHealth() + healAmount, stats.getMaxHealth());
                        entity.setHealth(newHealth);
                        
                        // Visual effects for the attack
                        applyAttackVisualEffects(entity, player);
                        
                        // Update nameplate after lifesteal
                        entityManager.updateEntityNameplate(entity);
                        
                        if (plugin.isDebugEnabled(DebugSystem.ENTITY)) {
                            plugin.debugLog(DebugSystem.ENTITY,getCustomName() + " hit " + player.getName() + 
                                    " for " + finalDamage + " damage and healed for " + String.format("%.1f", healAmount));
                        }
                    }
                }
            }
        }, 40L); // 1.25 seconds delay to sync with animation
        
        // Reset cooldown after attack animation completes
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            entity.setMetadata("attack_cooldown", new FixedMetadataValue(plugin, false));
        }, 45L); // 1.75 seconds cooldown
    }
    
    /**
     * Apply visual effects for a successful attack
     */
    private void applyAttackVisualEffects(LivingEntity entity, Player player) {
        // Apply visual lifesteal effect
        for (int i = 0; i < 8; i++) {
            Vector particleOffset = new Vector(
                (Math.random() - 0.5) * 0.5,
                Math.random() * 1.0,
                (Math.random() - 0.5) * 0.5
            );
            player.getWorld().spawnParticle(
                Particle.DAMAGE_INDICATOR, 
                player.getLocation().add(0, 1, 0).add(particleOffset), 
                1, 0, 0, 0, 0
            );
        }
        
        // Crimson particles flowing to the wolf
        for (int i = 0; i < 5; i++) {
            final int particleIndex = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Location startLoc = player.getLocation().add(0, 1, 0);
                Location endLoc = entity.getLocation().add(0, 1, 0);
                Vector direction = endLoc.clone().subtract(startLoc).toVector().normalize().multiply(0.5);
                
                Location particleLoc = startLoc.clone().add(direction.clone().multiply(particleIndex));
                player.getWorld().spawnParticle(
                    Particle.CRIMSON_SPORE, 
                    particleLoc,
                    5, 0.1, 0.1, 0.1, 0
                );
            }, i + 1);
        }
        
        // Bite sound
        player.getWorld().playSound(
            player.getLocation(), 
            Sound.ENTITY_WOLF_GROWL, 
            1.0f, 0.8f
        );
    }
    
    @Override
    public void cleanup(LivingEntity entity) {
        super.cleanup(entity);
        entity.removeMetadata("attack_cooldown", plugin);
    }
    
    @Override
    public EntityType getEntityType() {
        return EntityType.WOLF;
    }
    
    @Override
    public String getModelId() {
        return MODEL_ID;
    }
    
    @Override
    public String getCustomName() {
        return CUSTOM_NAME;
    }
    
    @Override
    public String getMetadataKey() {
        return METADATA_KEY;
    }
    
    @Override
    public CustomMobStats createStats() {
        CustomMobStats stats = new CustomMobStats();
        stats.setHealth(300);
        stats.setMaxHealth(300);
        stats.setPhysicalDamage(30);
        stats.setArmor(30);  // Medium armor
        stats.setLevel(7);
        stats.setMobType(MobType.ELITE);
        stats.setName("Duskhollow Fang");
        stats.setHasCustomAbilities(false); // Set to false since we removed special abilities
        stats.setAttackSpeed(1.3);  // Faster attack speed
        stats.setExpReward(35);
        stats.setMinGoldDrop(25);
        stats.setMaxGoldDrop(40);
        return stats;
    }
}