package com.server.events;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.server.Main;
import com.server.entities.CustomMobStats;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

public class CustomMobListener implements Listener {

    private final Main plugin;
    private final Map<UUID, BukkitTask> attackTasks = new HashMap<>();
    private final Map<UUID, Long> lastAttackTime = new HashMap<>();
    private static final int ATTACK_COOLDOWN = 20; // ticks between attacks (1 second)
    private static final double ATTACK_RANGE = 3.0; // blocks
    
    public CustomMobListener(Main plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        LivingEntity entity = (LivingEntity) event.getEntity();
        
        // Check if it's a custom mob
        if (plugin.getCustomEntityManager().isCustomMob(entity)) {
            // Update the nameplate after damage
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!entity.isDead()) {
                        plugin.getCustomEntityManager().updateEntityNameplate(entity);
                    }
                }
            }.runTaskLater(plugin, 1L);
            
            // For proper visual feedback, play hurt animation when mob takes damage
            if (!entity.isDead()) {
                plugin.getCustomEntityManager().playAnimation(entity, "hurt");
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 0.7f, 1.0f);
            }
        }
    }
    
   @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (!(event.getTarget() instanceof Player)) return;
        
        LivingEntity entity = (LivingEntity) event.getEntity();
        Player player = (Player) event.getTarget();

        // Skip Runemark Colossus entities - they have their own custom behavior
        if (entity.hasMetadata("runemark_colossus")) {
            event.setCancelled(true);
            return;
        }
        
        // Check if it's a custom mob
        if (plugin.getCustomEntityManager().isCustomMob(entity)) {
            // Disable vanilla AI attacks for custom mobs
            event.setCancelled(true);
            
            UUID entityId = entity.getUniqueId();
            
            // Already has an attack task running
            if (attackTasks.containsKey(entityId)) {
                return;
            }
            
            // Get mob stats
            CustomMobStats stats = plugin.getCustomEntityManager().getMobStats(entity);
            if (stats == null) return;
            
            // Calculate attack cooldown based on attack speed
            int attackCooldownTicks = (int)(ATTACK_COOLDOWN / stats.getAttackSpeed());
            
            // Start a new attack cycle
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    // Cancel if entity or player is gone
                    if (!entity.isValid() || entity.isDead() || !player.isOnline()) {
                        attackTasks.remove(entityId);
                        this.cancel();
                        return;
                    }
                    
                    // Check if entity is within attack range of player
                    double distance = entity.getLocation().distance(player.getLocation());
                    if (distance <= ATTACK_RANGE) {
                        // Check cooldown
                        long currentTime = System.currentTimeMillis();
                        if (!lastAttackTime.containsKey(entityId) || 
                            currentTime - lastAttackTime.get(entityId) >= attackCooldownTicks * 50) {
                            
                            // Look at player before attacking
                            entity.teleport(entity.getLocation().setDirection(
                                player.getLocation().subtract(entity.getLocation()).toVector()));
                            
                            // Special handling for Runemark Colossus
                            if (entity.hasMetadata("runemark_colossus")) {
                                plugin.getLogger().info("Runemark Colossus initiating attack1 animation");
                                
                                // Force a specific animation for Runemark Colossus
                                // This animation will be directly passed to the model engine
                                plugin.getCustomEntityManager().playAnimation(entity, "attack1");
                                
                                // Delayed damage application to sync with animation
                                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                    if (entity.isValid() && !entity.isDead() && player.isOnline()) {
                                        // Only apply damage if player is still in range
                                        double currentDistance = entity.getLocation().distance(player.getLocation());
                                        if (currentDistance <= ATTACK_RANGE) {
                                            // Apply damage to player
                                            double damage = stats.getPhysicalDamage();
                                            player.damage(damage, entity);
                                            
                                            // Visual and sound effects
                                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
                                            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, 
                                                    player.getLocation().add(0, 1, 0), 1, 0.1, 0.1, 0.1, 0.0);
                                            player.getWorld().spawnParticle(Particle.CRIT, 
                                                player.getLocation().add(0, 1, 0), 
                                                10, 0.3, 0.3, 0.3, 0.2);
                                            
                                            plugin.getLogger().info("Runemark Colossus successfully damaged player for " + damage);
                                        }
                                    }
                                }, 20L); // 1 second delay for Runemark Colossus's attack animation
                            } else {
                                // Generic attack for other custom mobs
                                String attackAnimation = "attack1";
                                plugin.getCustomEntityManager().playAnimation(entity, attackAnimation);
                                
                                // Standard delay for other mobs
                                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                    if (entity.isValid() && !entity.isDead() && player.isOnline()) {
                                        double currentDistance = entity.getLocation().distance(player.getLocation());
                                        if (currentDistance <= ATTACK_RANGE) {
                                            double damage = stats.getPhysicalDamage();
                                            player.damage(damage, entity);
                                            
                                            // Basic effects
                                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
                                            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, 
                                                    player.getLocation().add(0, 1, 0), 1, 0.1, 0.1, 0.1, 0.0);
                                        }
                                    }
                                }, 10L);
                            }
                            
                            // Update cooldown
                            lastAttackTime.put(entityId, currentTime);
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 5L); // Check every 5 ticks (0.25 seconds)
            
            attackTasks.put(entityId, task);
        }
    }

    /**
     * Handle when player attacks a custom mob
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Only handle player attacking custom mob
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        Player player = (Player) event.getDamager();
        LivingEntity entity = (LivingEntity) event.getEntity();
        
        if (plugin.getCustomEntityManager().isCustomMob(entity)) {
            // Get the player's active profile
            Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
            if (activeSlot == null) return;
            
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile != null) {
                // Play hurt animation with a slight delay to better sync visually
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (!entity.isDead() && entity.isValid()) {
                        plugin.getCustomEntityManager().playAnimation(entity, "hurt");
                        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 0.7f, 1.0f);
                    }
                }, 1L);
                
                // Standard damage calculation already happens in the event
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info(player.getName() + " attacked a custom mob: " + 
                            entity.getCustomName() + " for " + event.getFinalDamage() + " damage");
                }
                
                // Trigger mob to target the player if not already targeting
                // For Runemark Colossus, we need to manually set the target
                // since it's a passive Iron Golem
                if (entity instanceof Mob) {
                    Mob mob = (Mob) entity;
                    if (mob.getTarget() == null || mob.getTarget() != player) {
                        mob.setTarget(player);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDeathEarly(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        
        // Check if it's a custom mob
        if (plugin.getCustomEntityManager().isCustomMob(entity)) {
            // Play death animation and handle death sequence
            plugin.getCustomEntityManager().handleDeath(entity);
            
            // Clean up attack task if exists
            UUID entityId = entity.getUniqueId();
            if (attackTasks.containsKey(entityId)) {
                attackTasks.get(entityId).cancel();
                attackTasks.remove(entityId);
                lastAttackTime.remove(entityId);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        
        // Check if it's a custom mob
        if (plugin.getCustomEntityManager().isCustomMob(entity)) {
            CustomMobStats stats = plugin.getCustomEntityManager().getMobStats(entity);
            if (stats == null) return;
            
            // Handle custom drops
            if (entity.getKiller() != null) {
                Player player = entity.getKiller();
                
                // Get the player's active profile
                Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
                if (activeSlot == null) return;
                
                PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
                
                if (profile != null) {
                    // Award XP
                    int expReward = calculateExpReward(stats);
                    player.sendMessage("§a+" + expReward + " XP from defeating " + stats.getName());
                    
                    // Award gold drops
                    int goldAmount = stats.getMinGoldDrop();
                    if (stats.getMaxGoldDrop() > stats.getMinGoldDrop()) {
                        goldAmount += (int)(Math.random() * (stats.getMaxGoldDrop() - stats.getMinGoldDrop()));
                    }
                    
                    // Add currency to player
                    profile.addUnits(goldAmount);
                    player.sendMessage("§6+" + goldAmount + " Gold");
                    
                    // Clear default drops and set custom ones
                    event.getDrops().clear();
                    
                    // Log the kill for debugging
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info(player.getName() + " killed custom mob: " + 
                                stats.getName() + " (Level " + stats.getLevel() + ")");
                    }
                }
            }
        }
    }
    
    /**
     * Calculate experience rewards based on mob type and level
     * @param stats The mob stats
     * @return The calculated experience reward
     */
    private int calculateExpReward(CustomMobStats stats) {
        int baseExp = stats.getExpReward();
        int levelMultiplier = stats.getLevel();
        
        // Apply multipliers based on mob type
        switch(stats.getMobType()) {
            case BOSS:
                return baseExp * levelMultiplier * 5; // 5x multiplier for bosses
            case MINIBOSS:
                return baseExp * levelMultiplier * 3; // 3x multiplier for minibosses
            case ELITE:
                return baseExp * levelMultiplier * 2; // 2x multiplier for elite mobs
            case NORMAL:
            default:
                return baseExp * levelMultiplier;
        }
    }
}