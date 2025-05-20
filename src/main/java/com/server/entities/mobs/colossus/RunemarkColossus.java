package com.server.entities.mobs.colossus;

import java.lang.reflect.Method;
import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
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

public class RunemarkColossus extends CustomMob {
    
    private static final String MODEL_ID = "golem_prismarine_gm_rain";
    private static final String CUSTOM_NAME = "§6Runemark Colossus";
    private static final String METADATA_KEY = "runemark_colossus";
    private static final double ATTACK_RANGE = 5.0;
    
    public RunemarkColossus(Main plugin, CustomEntityManager entityManager) {
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
                
                // Specific modifications for Iron Golem
                if (entity instanceof IronGolem) {
                    // Mark the golem as player-created to prevent hostile behavior
                    ((IronGolem) entity).setPlayerCreated(true);
                }
                
                // Remove attack AI completely
                removeAttackGoals(entity);
                
                // Register listeners to prevent unwanted behavior
                registerDamagePreventionListener(entity);
                registerDamageCancellationListener(entity);
                
                // Customize movement speed
                if (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                    entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.22);
                }
                
                // Store the stats in the entity manager
                entityManager.registerMobStats(entity.getUniqueId(), stats);
                
                // Update the display name
                entityManager.updateEntityNameplate(entity);
                
                // Initialize behavior
                initializeBehavior(entity, stats);
                
                // Play idle animation after spawning
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    playIdleAnimation(entity);
                }, 5L);
            } catch (Exception e) {
                plugin.debugLog(DebugSystem.ENTITY,"Error setting Runemark Colossus attributes: " + e.getMessage());
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
                
                // Check if we are on attack cooldown
                boolean onCooldown = entity.hasMetadata("attack_cooldown") ? 
                                    entity.getMetadata("attack_cooldown").get(0).asBoolean() : false;
                
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
                        
                        // Decide between normal attack and special ability (20% chance for special)
                        boolean useSpecialAbility = Math.random() < 0.2; // 20% chance
                        
                        if (useSpecialAbility) {
                            // Play debast animation (special area attack)
                            entityManager.playAnimation(entity, "debast");
                            
                            if (plugin.isDebugEnabled(DebugSystem.ENTITY)) {
                                plugin.debugLog(DebugSystem.ENTITY,getCustomName() + " using debast special ability");
                            }
                            
                            // Play special ability sound
                            entity.getWorld().playSound(entity.getLocation(), 
                                Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 0.5f);
                            
                            // Perform area attack after 1 second delay (to match animation)
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                if (entity.isValid() && !entity.isDead()) {
                                    // Ground slam visual effect
                                    entity.getWorld().spawnParticle(
                                        Particle.EXPLOSION_EMITTER, 
                                        entity.getLocation(), 
                                        1, 0.5, 0.2, 0.5, 0
                                    );
                                    
                                    // Dust particles expanding outward
                                    entity.getWorld().spawnParticle(
                                        Particle.BLOCK, 
                                        entity.getLocation().add(0, 0.1, 0), 
                                        50, 3, 0.1, 3, 
                                        0.1, 
                                        org.bukkit.Material.STONE.createBlockData()
                                    );
                                    
                                    // Huge slam sound
                                    entity.getWorld().playSound(
                                        entity.getLocation(), 
                                        Sound.ENTITY_GENERIC_EXPLODE, 
                                        1.0f, 0.5f
                                    );
                                    
                                    // Effect all players in 10 block radius
                                    for (Entity nearby : entity.getNearbyEntities(10, 10, 10)) {
                                        if (nearby instanceof Player) {
                                            Player nearbyPlayer = (Player) nearby;
                                            
                                            // Skip creative/spectator players
                                            if (nearbyPlayer.getGameMode() == GameMode.CREATIVE || 
                                                nearbyPlayer.getGameMode() == GameMode.SPECTATOR) {
                                                continue;
                                            }
                                            
                                            // Launch player in air
                                            Vector launchVector = new Vector(
                                                // Small random horizontal component for natural effect
                                                (Math.random() - 0.5) * 0.3,
                                                // Strong upward force
                                                1.5, 
                                                (Math.random() - 0.5) * 0.3
                                            );
                                            nearbyPlayer.setVelocity(launchVector);
                                            
                                            // Deal 30 physical damage
                                            // Use a custom damage event to bypass armor calculation
                                            EntityDamageEvent customDamageEvent = new EntityDamageEvent(
                                                nearbyPlayer, 
                                                EntityDamageEvent.DamageCause.CUSTOM,
                                                30.0
                                            );
                                            plugin.getServer().getPluginManager().callEvent(customDamageEvent);
                                            
                                            if (!customDamageEvent.isCancelled()) {
                                                // Apply the damage directly
                                                double finalDamage = customDamageEvent.getFinalDamage();
                                                nearbyPlayer.damage(finalDamage);
                                                
                                                // Ensure damage attribution - set the entity as the last damager
                                                try {
                                                    // Use reflection to get access to CraftLivingEntity's setLastDamager method
                                                    Method setLastDamager = nearbyPlayer.getClass().getDeclaredMethod(
                                                        "setLastDamager", 
                                                        org.bukkit.entity.Entity.class
                                                    );
                                                    setLastDamager.setAccessible(true);
                                                    setLastDamager.invoke(nearbyPlayer, entity);
                                                } catch (Exception e) {
                                                    // If reflection fails, do a tiny amount of direct damage for attribution
                                                    nearbyPlayer.damage(0.0, entity);
                                                }
                                            }
                                            
                                            // Play hurt sound for player
                                            nearbyPlayer.getWorld().playSound(
                                                nearbyPlayer.getLocation(), 
                                                Sound.ENTITY_PLAYER_HURT, 
                                                1.0f, 0.8f
                                            );
                                        }
                                    }
                                }
                            }, 20L); // 1 second delay (20 ticks)
                            
                            // Set a longer cooldown for special ability
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                entity.setMetadata("attack_cooldown", new FixedMetadataValue(plugin, false));
                            }, 80L); // 4 seconds cooldown for special
                        }
                        else {
                            // Regular attack
                            entityManager.playAnimation(entity, "attack1");
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
                                        
                                        // We need to apply damage directly to ensure the right amount
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
                                            
                                            // Add visual effects
                                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
                                            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, 
                                                    player.getLocation().add(0, 1, 0), 1, 0.1, 0.1, 0.1, 0.0);
                                            
                                            if (plugin.isDebugEnabled(DebugSystem.ENTITY)) {
                                                plugin.debugLog(DebugSystem.ENTITY,getCustomName() + " hit " + player.getName() + 
                                                        " for " + finalDamage + " damage (stats say: " + damage + ")");
                                            }
                                        }
                                    }
                                }
                            }, 30L); // 1.5 seconds delay to sync with animation
                            
                            // Reset cooldown after attack animation completes
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                entity.setMetadata("attack_cooldown", new FixedMetadataValue(plugin, false));
                            }, 40L); // 2 seconds cooldown for regular attack
                        }
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
    
    @Override
    public void cleanup(LivingEntity entity) {
        super.cleanup(entity);
        entity.removeMetadata("attack_cooldown", plugin);
    }
    
    @Override
    public EntityType getEntityType() {
        return EntityType.IRON_GOLEM;
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
        stats.setHealth(100);
        stats.setMaxHealth(100);
        stats.setPhysicalDamage(10);
        stats.setArmor(50);
        stats.setLevel(5);
        stats.setMobType(MobType.ELITE);
        stats.setName("Runemark Colossus");
        stats.setHasCustomAbilities(true);
        stats.setAttackSpeed(1.0);
        stats.setExpReward(25);
        stats.setMinGoldDrop(15);
        stats.setMaxGoldDrop(25);
        return stats;
    }
    
    /**
     * Play a special ability for the Runemark Colossus
     * 
     * @param entity The entity
     * @param abilityIndex The ability index (1 or 2)
     */
    public void playSpecialAbility(LivingEntity entity, int abilityIndex) {
        if (abilityIndex == 1) {
            // Play debast animation (area slam attack)
            entityManager.playAnimation(entity, "debast");
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 0.5f);
            
            // Execute the ability effect after 1 second (matches animation timing)
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (entity.isValid() && !entity.isDead()) {
                    // Ground slam visual effect
                    entity.getWorld().spawnParticle(
                        Particle.EXPLOSION, 
                        entity.getLocation(), 
                        1, 0.5, 0.2, 0.5, 0
                    );
                    
                    // Dust particles
                    entity.getWorld().spawnParticle(
                        Particle.BLOCK, 
                        entity.getLocation().add(0, 0.1, 0), 
                        50, 3, 0.1, 3, 
                        0.1, 
                        org.bukkit.Material.STONE.createBlockData()
                    );
                    
                    // Slam sound
                    entity.getWorld().playSound(
                        entity.getLocation(), 
                        Sound.ENTITY_GENERIC_EXPLODE, 
                        1.0f, 0.5f
                    );
                    
                    // Effect all players in 10 block radius
                    for (Entity nearby : entity.getNearbyEntities(10, 10, 10)) {
                        if (nearby instanceof Player) {
                            Player nearbyPlayer = (Player) nearby;
                            
                            // Skip creative/spectator players
                            if (nearbyPlayer.getGameMode() == GameMode.CREATIVE || 
                                nearbyPlayer.getGameMode() == GameMode.SPECTATOR) {
                                continue;
                            }
                            
                            // Launch player in air
                            Vector launchVector = new Vector(
                                (Math.random() - 0.5) * 0.3, // Random X
                                1.5,                         // Strong upward
                                (Math.random() - 0.5) * 0.3  // Random Z
                            );
                            nearbyPlayer.setVelocity(launchVector);
                            
                            // Deal 30 physical damage
                            nearbyPlayer.damage(30.0, entity);
                            
                            // Play hurt sound for player
                            nearbyPlayer.getWorld().playSound(
                                nearbyPlayer.getLocation(), 
                                Sound.ENTITY_PLAYER_HURT, 
                                1.0f, 0.8f
                            );
                        }
                    }
                }
            }, 20L); // 1 second delay
        } 
        else if (abilityIndex == 2) {
            // Keep the original special2 ability
            entityManager.playAnimation(entity, "special2");
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 0.8f);
        }
        else {
            // Default attack
            entityManager.playAnimation(entity, "attack1");
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 1.0f);
        }
    }
}