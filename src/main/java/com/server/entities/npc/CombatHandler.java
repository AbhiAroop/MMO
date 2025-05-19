package com.server.entities.npc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.server.Main;
import com.server.display.DamageIndicatorManager;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.LookClose;

/**
 * Handles combat interactions for NPCs
 */
public class CombatHandler {
    
    private final Main plugin;
    private final Random rand = new Random();
    
    // Maps to store NPC combat data
    private final Map<UUID, Double> npcHealth = new HashMap<>();
    private final Map<UUID, NPCStats> npcStats = new HashMap<>();
    private final Map<UUID, BukkitTask> combatTasks = new HashMap<>();
    private final Map<UUID, Entity> currentTargets = new HashMap<>();
    private final Map<UUID, Map<String, Long>> abilityCooldownMap = new HashMap<>();
    private final Map<String, Long> respawnTimerMap = new HashMap<>();
    
    // Configuration
    private final double MAX_TARGET_RANGE = 16.0;
    private final int TARGET_CHECK_INTERVAL = 20; // Every second
    private final int RESPAWN_TIME = 60; // Seconds
    
    // Targeting options
    private boolean targetsPlayers = true;
    private boolean targetsNPCs = false;
    private boolean targetsHostileMobs = false;
    
    // NPC identification
    private static final List<UUID> hostileNpcUuids = new ArrayList<>();
    
    /**
     * Create a new combat handler
     */
    public CombatHandler() {
        this.plugin = Main.getInstance();
    }
    
    /**
     * Create a new combat handler with initial stats
     * 
     * @param maxHealth Maximum health for NPCs
     * @param physicalDamage Base physical damage for NPCs
     */
    public CombatHandler(double maxHealth, int physicalDamage) {
        this.plugin = Main.getInstance();
    }
    
    /**
     * Set NPC stats for a specific NPC
     * 
     * @param npcId The UUID of the NPC
     * @param stats The stats to set
     */
    public void setNPCStats(UUID npcId, NPCStats stats) {
        npcStats.put(npcId, stats);
        
        // Initialize health if not already set
        if (!npcHealth.containsKey(npcId)) {
            npcHealth.put(npcId, stats.getMaxHealth());
        }
        
        // Check if the NPC is spawned and has a valid entity
        NPC npc = CitizensAPI.getNPCRegistry().getByUniqueId(npcId);
        if (npc != null && npc.isSpawned()) {
            // Store stats in entity metadata for persistence
            npc.getEntity().setMetadata("max_health", new FixedMetadataValue(plugin, stats.getMaxHealth()));
            npc.getEntity().setMetadata("physical_damage", new FixedMetadataValue(plugin, stats.getPhysicalDamage()));
            npc.getEntity().setMetadata("magic_damage", new FixedMetadataValue(plugin, stats.getMagicDamage()));
            npc.getEntity().setMetadata("armor", new FixedMetadataValue(plugin, stats.getArmor()));
            npc.getEntity().setMetadata("magic_resist", new FixedMetadataValue(plugin, stats.getMagicResist()));
            npc.getEntity().setMetadata("level", new FixedMetadataValue(plugin, stats.getLevel()));
            npc.getEntity().setMetadata("npc_type", new FixedMetadataValue(plugin, stats.getNpcType().name()));
            
            // Update nameplate with new stats
            NPCManager.getInstance().updateNameplate(npc, npcHealth.get(npcId), stats.getMaxHealth());
        }
    }
    
    /**
     * Get stats for an NPC
     * 
     * @param npcId The UUID of the NPC
     * @return The NPC stats or null if not found
     */
    public NPCStats getNPCStats(UUID npcId) {
        if (npcStats.containsKey(npcId)) {
            return npcStats.get(npcId);
        }
        
        // Try to get stats from metadata
        NPC npc = CitizensAPI.getNPCRegistry().getByUniqueId(npcId);
        if (npc != null && npc.isSpawned()) {
            NPCStats stats = new NPCStats();
            
            // Load from metadata if available
            if (npc.getEntity().hasMetadata("max_health")) {
                stats.setMaxHealth(npc.getEntity().getMetadata("max_health").get(0).asDouble());
            }
            if (npc.getEntity().hasMetadata("physical_damage")) {
                stats.setPhysicalDamage(npc.getEntity().getMetadata("physical_damage").get(0).asInt());
            }
            if (npc.getEntity().hasMetadata("magic_damage")) {
                stats.setMagicDamage(npc.getEntity().getMetadata("magic_damage").get(0).asInt());
            }
            if (npc.getEntity().hasMetadata("armor")) {
                stats.setArmor(npc.getEntity().getMetadata("armor").get(0).asInt());
            }
            if (npc.getEntity().hasMetadata("magic_resist")) {
                stats.setMagicResist(npc.getEntity().getMetadata("magic_resist").get(0).asInt());
            }
            if (npc.getEntity().hasMetadata("level")) {
                stats.setLevel(npc.getEntity().getMetadata("level").get(0).asInt());
            }
            if (npc.getEntity().hasMetadata("npc_type")) {
                try {
                    String typeName = npc.getEntity().getMetadata("npc_type").get(0).asString();
                    NPCType type = NPCType.valueOf(typeName);
                    stats.setNpcType(type);
                } catch (Exception e) {
                    // Use default type
                }
            }
            
            // Cache the loaded stats
            npcStats.put(npcId, stats);
            return stats;
        }
        
        // Return default stats if nothing found
        NPCStats defaultStats = new NPCStats();
        npcStats.put(npcId, defaultStats);
        return defaultStats;
    }

    /**
     * Start combat behavior with specific targeting settings specifically for Hostile NPCs
     */
    public void startHostileCombatBehavior(NPC npc, Player initialTarget) {
        if (!npc.isSpawned()) return;
        
        // Get this NPC's stats
        NPCStats stats = getNPCStats(npc.getUniqueId());
        UUID npcId = npc.getUniqueId();
        
        // Set more aggressive stats for hostile NPCs
        stats.setAttackSpeed(1.5); // Even faster attack speed 
        stats.setAttackRange(3.0); // Keep shorter attack range for more aggressive behavior
        npcStats.put(npcId, stats);
        
        // Force targeting players and NPCs
        boolean shouldTargetPlayers = true;
        boolean shouldTargetNPCs = true; // Allow targeting other NPCs too
        
        npc.getEntity().setMetadata("targets_players", new FixedMetadataValue(plugin, shouldTargetPlayers));
        npc.getEntity().setMetadata("targets_npcs", new FixedMetadataValue(plugin, shouldTargetNPCs));
        
        // Keep track of NPC as hostile
        npc.getEntity().setMetadata("hostile", new FixedMetadataValue(plugin, true));
        hostileNpcUuids.add(npcId);
        
        // Start initial target
        if (initialTarget != null) {
            currentTargets.put(npcId, initialTarget);
        }
        
        // Stop any existing task 
        stopCombatBehavior(npcId);
        
        // Start the combat task with increased aggression
        BukkitTask task = new BukkitRunnable() {
            private int tickCounter = 0;
            private Entity target = initialTarget;
            private float attackCharge = 0.5f; // Start with 50% charge for immediate action
            private int attackCooldown = 0;
            private boolean isAttacking = false;
            private long lastPathUpdateTime = 0;
            private long lastAttackTime = 0;
            private Location lastLocation = null;
            private int stuckCounter = 0;
            
            @Override
            public void run() {
                // Check if NPC is still valid
                if (!npc.isSpawned()) {
                    this.cancel();
                    return;
                }
                
                // Get latest stats for dynamic updates
                NPCStats stats = getNPCStats(npcId);
                
                // Update target every few ticks
                if (tickCounter % 10 == 0) { // Every 10 ticks = 0.5 seconds
                    // Try to find a target if we don't have one
                    if (target == null || !target.isValid() || target.isDead()) {
                        // First check the current target map
                        target = currentTargets.get(npcId);
                        
                        // If still no target, find the closest one
                        if (target == null || !target.isValid() || target.isDead()) {
                            target = findBestTarget(npc, shouldTargetPlayers, shouldTargetNPCs);
                            if (target != null) {
                                currentTargets.put(npcId, target);
                            }
                        }
                    }
                }
                
                // Process movement and attacks if we have a target
                if (target != null && target.isValid() && !target.isDead()) {
                    // Calculate distance to target
                    double distance = npc.getEntity().getLocation().distance(target.getLocation());
                    
                    // If target too far away, clear it
                    if (distance > MAX_TARGET_RANGE * 1.5) {
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info(npc.getName() + " losing target " + 
                                                    target.getName() + " - too far away: " + distance);
                        }
                        // Clear target if too far away
                        currentTargets.remove(npcId);
                        target = null;
                    } else {
                        // Look at target constantly
                        npc.faceLocation(target.getLocation());
                        
                        // Update pathfinding - rate-limited to avoid jitter
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastPathUpdateTime > 200) { // 200ms = 4 ticks - faster updates for sprinting
                            lastPathUpdateTime = currentTime;
                            
                            // Use more aggressive navigation
                            updateAggressiveNavigation(npc, target, distance);
                            
                            // Check if stuck
                            Location currentLoc = npc.getEntity().getLocation();
                            if (lastLocation != null) {
                                // Check if we've moved less than a minimal amount
                                if (lastLocation.distanceSquared(currentLoc) < 0.01 && distance > 2.0) {
                                    stuckCounter++;
                                    
                                    // If stuck for too long, try to get unstuck
                                    if (stuckCounter > 20) { // 1 second of being stuck
                                        // Try to jump
                                        if (npc.getEntity() instanceof LivingEntity) {
                                            LivingEntity living = (LivingEntity) npc.getEntity();
                                            living.setVelocity(living.getVelocity().add(new Vector(0, 0.4, 0)));
                                        }
                                        
                                        // Try a new path
                                        Location jumpTarget = currentLoc.clone().add(
                                            (Math.random() - 0.5) * 3,
                                            0,
                                            (Math.random() - 0.5) * 3
                                        );
                                        npc.getNavigator().setTarget(jumpTarget);
                                        
                                        stuckCounter = 0; // Reset counter after trying alternative
                                    }
                                } else {
                                    // We're moving, reset stuck counter
                                    stuckCounter = 0;
                                }
                                
                                lastLocation = currentLoc;
                            }
                            
                            // Handle attacks when within range using attack speed
                            if (attackCooldown > 0) {
                                attackCooldown--;
                            } else if (distance <= 3.0 && !isAttacking) {
                                // CRITICAL FIX: Use attack speed to determine attack rate
                                // For hostile NPCs, we want them to attack slightly faster
                                long attackCurrentTime = System.currentTimeMillis();
                                // Min time between attacks is 1000ms / attackSpeed
                                long minTimeBetweenAttacks = (long)(1000 / stats.getAttackSpeed());
                                
                                boolean canAttackByTime = (attackCurrentTime - lastAttackTime) >= minTimeBetweenAttacks;
                                
                                if (canAttackByTime) {
                                    // FASTER CHARGE BUILDUP based on attack speed
                                    attackCharge += stats.getAttackSpeed() / 12.0f;
                                    
                                    if (attackCharge >= 1.0f) {
                                        // Attack fully charged - set attacking flag to prevent new attacks during animation
                                        isAttacking = true;
                                        
                                        // Update attack timestamp
                                        lastAttackTime = System.currentTimeMillis();
                                        
                                        // Play attack animation and then handle damage
                                        attackTarget(npc, target, 1.0f);
                                        
                                        // Reset charge with shorter cooldown based on attack speed
                                        attackCharge = 0.5f;
                                        attackCooldown = Math.max(1, (int)(20 / stats.getAttackSpeed())); 
                                        
                                        // Reset attacking flag after a shorter delay 
                                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                            isAttacking = false;
                                        }, Math.max(1, (int)(8 / stats.getAttackSpeed())));
                                    }
                                }
                            } else {
                                // If not in range, build up charge faster
                                attackCharge = Math.min(0.9f, attackCharge + 0.03f);
                            }
                        }
                    }
                }
                
                tickCounter++;
            }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick
        
        // Store this task
        combatTasks.put(npcId, task);
        
        // Track this as a hostile NPC
        hostileNpcUuids.add(npcId);
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Started hostile combat behavior for " + npc.getName() + 
                " with attack speed: " + stats.getAttackSpeed());
        }
    }

    /**
     * Find a target within the specified range
     */
    private Entity findTargetWithinRange(NPC npc, boolean targetsPlayers, boolean targetsNPCs, double maxRange) {
        if (!npc.isSpawned()) return null;
        
        Entity bestTarget = null;
        double closestDistance = maxRange;
        
        // Always check if we already have a valid current target that's within an extended range
        // This helps prevent target switching too frequently
        Entity currentTarget = currentTargets.get(npc.getUniqueId());
        if (currentTarget != null && isValidCombatTarget(npc, currentTarget) && 
            npc.getEntity().getLocation().distance(currentTarget.getLocation()) <= maxRange * 1.5) {
            // Keep the current target if still valid and within an extended range
            return currentTarget;
        }
        
        // First, prioritize players as targets if within range
        if (targetsPlayers) {
            for (Player player : npc.getEntity().getWorld().getPlayers()) {
                if (!isValidCombatTarget(npc, player)) {
                    continue;
                }
                
                double distance = npc.getEntity().getLocation().distance(player.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    bestTarget = player;
                }
            }
        }
        
        // If no player targets found or we also target NPCs, look for NPC targets if enabled
        if ((bestTarget == null || targetsNPCs) && targetsNPCs) {
            List<Entity> nearbyEntities = npc.getEntity().getNearbyEntities(maxRange, maxRange, maxRange);
            for (Entity entity : nearbyEntities) {
                if (!isValidCombatTarget(npc, entity) || !(CitizensAPI.getNPCRegistry().isNPC(entity))) {
                    continue;
                }
                
                // Don't target yourself
                if (entity.getUniqueId().equals(npc.getUniqueId())) {
                    continue;
                }
                
                double distance = npc.getEntity().getLocation().distance(entity.getLocation());
                
                // Only choose an NPC target if:
                // 1. We have no target yet, OR
                // 2. This NPC is closer than the current best target AND
                //    either the current best isn't a player OR we don't prioritize players
                if (bestTarget == null || 
                    (distance < closestDistance && (!(bestTarget instanceof Player) || !targetsPlayers))) {
                    closestDistance = distance;
                    bestTarget = entity;
                }
            }
        }
        
        return bestTarget;
    }

    /**
     * More aggressive navigation specifically for hostile NPCs
     */
    private void updateAggressiveNavigation(NPC npc, Entity target, double distance) {
        if (target == null || !npc.isSpawned()) return;
        
        // Calculate the optimal update frequency based on distance
        boolean shouldUpdate = false;
        
        // Always update if not navigating
        if (!npc.getNavigator().isNavigating()) {
            shouldUpdate = true;
        }
        
        // Update more frequently when closer to target
        if (distance < 5) {
            shouldUpdate = npc.getEntity().getTicksLived() % 10 == 0;
        } else {
            shouldUpdate = npc.getEntity().getTicksLived() % 20 == 0;
        }
        
        // Update navigation if needed
        if (shouldUpdate) {
            // Target position directly - no randomness for aggressive NPCs
            Location targetLoc = target.getLocation();
            
            // Adjust speed based on distance - faster when further away
            float speedMod = 1.2f; // Base speed
            
            if (distance > 10) {
                // Far away - move faster
                speedMod = 1.5f;
            } else if (distance < 3) {
                // Close to target - slightly slower for better attack positioning
                speedMod = 1.0f;
            }
            
            // Configure navigation parameters for smooth pathfinding
            npc.getNavigator().getLocalParameters().speedModifier(speedMod);
            npc.getNavigator().getLocalParameters().range(30);
            npc.getNavigator().getLocalParameters().avoidWater(false);
            npc.getNavigator().getLocalParameters().stationaryTicks(2);
            
            // Custom stuck action for smoother movement without teleporting
            npc.getNavigator().getLocalParameters().stuckAction((stuckNpc, navigator) -> {
                // If stuck, try to jump or move sideways slightly
                if (npc.isSpawned() && npc.getEntity() instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity) npc.getEntity();
                    
                    // Apply a slight upward velocity to jump
                    Vector currentVel = living.getVelocity();
                    living.setVelocity(new Vector(currentVel.getX() * 1.1, 0.2, currentVel.getZ() * 1.1));
                    
                    // Try moving to a slightly offset position
                    Location currentPos = living.getLocation();
                    double offsetX = (Math.random() - 0.5) * 2;
                    double offsetZ = (Math.random() - 0.5) * 2;
                    
                    // Set a new path target slightly to the side
                    Location newTarget = currentPos.clone().add(offsetX, 0, offsetZ);
                    navigator.setTarget(newTarget);
                    return false; // Continue with navigation after attempting to unstuck
                }
                return true; // Default behavior
            });
            
            // Navigate directly to the target
            npc.getNavigator().setTarget(targetLoc);
            
            // Make NPC face target for better combat animation
            npc.faceLocation(targetLoc);
        }
    }

    /**
     * Play attack animation and effects
     * 
     * @param npc The NPC performing the attack
     * @param target The target being attacked
     * @param onAnimationComplete Callback to run after animation completes
     */
    private void playAttackAnimation(NPC npc, Entity target, Runnable onAnimationComplete) {
        if (!npc.isSpawned() || target == null) {
            if (onAnimationComplete != null) onAnimationComplete.run();
            return;
        }
        
        World world = npc.getEntity().getWorld();
        
        // Face the target for the attack
        npc.faceLocation(target.getLocation());
        
        // Make the NPC "swing" by temporarily disabling the lookclose trait and then restoring it
        LookClose lookTrait = npc.getTraitNullable(LookClose.class);
        final boolean wasLooking = lookTrait != null ? lookTrait.toggle() : false;
        
        
        // CRITICAL FIX: Perform the arm swing animation
        if (npc.getEntity() instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) npc.getEntity();
            
            // Use the proper swing animation method
            living.swingMainHand();
            
            // For enhanced visual effect, also perform the sweep attack animation
            world.spawnParticle(
                org.bukkit.Particle.SWEEP_ATTACK,
                living.getLocation().add(0, 1.0, 0),
                1, 0.5, 0.1, 0.5, 0.0
            );
        }
        
        // Schedule the actual attack animation after a short delay (5 ticks = 0.25 seconds)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                if (!npc.isSpawned() || npc.getEntity() == null) {
                    if (onAnimationComplete != null) onAnimationComplete.run();
                    return;
                }
                
                // Check if target is still valid
                if (target.isValid() && !target.isDead()) {
                    // Add multiple attack particles for more visual impact
                    Location targetCenter = target.getLocation().add(0, 1, 0);
                    
                    // Sweep attack particles
                    world.spawnParticle(
                        org.bukkit.Particle.SWEEP_ATTACK,
                        targetCenter,
                        2, 0.2, 0.2, 0.2, 0.0
                    );
                    
                    // Crit particles
                    world.spawnParticle(
                        org.bukkit.Particle.CRIT,
                        targetCenter,
                        10, 0.3, 0.3, 0.3, 0.1
                    );
                    
                }
                
                // CRITICAL FIX: Perform another arm swing at impact moment
                if (npc.getEntity() instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity) npc.getEntity();
                    living.swingMainHand();
                }
                
                // Complete the animation and apply damage IMMEDIATELY rather than waiting
                if (onAnimationComplete != null) {
                    onAnimationComplete.run();
                }
                
                // Re-enable look trait AFTER damage is applied
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (lookTrait != null && wasLooking && npc.isSpawned()) {
                        lookTrait.toggle();
                    }
                }, 3L);
            } catch (Exception e) {
                // Catch any errors to prevent crashes
                if (plugin.isDebugMode()) {
                    plugin.getLogger().warning("Error in attack animation: " + e.getMessage());
                    e.printStackTrace();
                }
                
                // Still run the completion callback even if there's an error
                if (onAnimationComplete != null) {
                    try {
                        onAnimationComplete.run();
                    } catch (Exception ex) {
                        // Ignore any errors in the callback
                    }
                }
            }
        }, 5L);
    }
    
    /**
     * Start combat behavior for this NPC
     * 
     * @param npc The NPC
     * @param initialTarget The initial target player
     */
    public void startCombatBehavior(NPC npc, Player initialTarget) {
        if (!npc.isSpawned()) return;
        
        // Get targeting settings from metadata or use defaults
        boolean npcTargetsPlayers = true;
        boolean npcTargetsNPCs = false;
        
        if (npc.getEntity().hasMetadata("targets_players")) {
            npcTargetsPlayers = npc.getEntity().getMetadata("targets_players").get(0).asBoolean();
        }
        if (npc.getEntity().hasMetadata("targets_npcs")) {
            npcTargetsNPCs = npc.getEntity().getMetadata("targets_npcs").get(0).asBoolean();
        }
        
        // IMPORTANT: Never reset health if it already exists
        UUID npcId = npc.getUniqueId();
        NPCStats stats = getNPCStats(npcId);
        
        // Only initialize health if not already set, NEVER reset existing health
        if (!npcHealth.containsKey(npcId)) {
            npcHealth.put(npcId, stats.getMaxHealth());
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Initialized missing health for NPC " + npc.getName() + 
                    " with max health: " + stats.getMaxHealth());
            }
        } else {
            // Log existing health for debugging
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Preserved existing health for NPC " + npc.getName() + 
                    ": " + npcHealth.get(npcId) + "/" + stats.getMaxHealth());
            }
        }
        
        // Cancel any existing combat task to avoid duplicates
        BukkitTask existingTask = combatTasks.get(npcId);
        if (existingTask != null) {
            existingTask.cancel();
            combatTasks.remove(npcId);
        }
        
        // Use the helper method with the correct settings
        startCombatWithSettings(npc, initialTarget, npcTargetsPlayers, npcTargetsNPCs);
        
        // Debug log
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Starting combat behavior for NPC: " + npc.getName());
        }
    }
    
    /**
     * Start combat behavior with specific targeting settings
     */
    private void startCombatWithSettings(NPC npc, Player initialTarget, boolean targetsPlayers, boolean targetsNPCs) {
        if (!npc.isSpawned()) return;
        
        UUID npcId = npc.getUniqueId();
        
        // Stop any existing combat task
        stopCombatBehavior(npcId);
        
        // Start tracking the target
        if (initialTarget != null) {
            currentTargets.put(npcId, initialTarget);
        }
        
        // Set metadata for targeting settings
        npc.getEntity().setMetadata("targets_players", new FixedMetadataValue(plugin, targetsPlayers));
        npc.getEntity().setMetadata("targets_npcs", new FixedMetadataValue(plugin, targetsNPCs));
        
        // Get the NPC's stats
        NPCStats stats = getNPCStats(npcId);
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Starting combat behavior for " + npc.getName() + 
                " with stats: attackSpeed=" + stats.getAttackSpeed() + 
                ", attackRange=" + stats.getAttackRange());
        }
        
        // Start the combat task
        BukkitTask task = new BukkitRunnable() {
            private int tickCounter = 0;
            private Entity target = initialTarget;
            private float attackCharge = 0.0f;
            private int attackCooldown = 0;
            private boolean isAttacking = false;
            private long lastPathUpdateTime = 0;
            private boolean isCharging = true; // Track if NPC is charging an attack
            
            @Override
            public void run() {
                // Check if NPC is still valid
                if (!npc.isSpawned()) {
                    this.cancel();
                    return;
                }
                
                // Only process every few ticks to reduce server load
                if (tickCounter % 2 != 0) {
                    tickCounter++;
                    return;
                }
                
                // Try to find a target if we don't have one
                if (target == null || !target.isValid() || target.isDead()) {
                    target = currentTargets.get(npcId);
                    
                    // If we still don't have a target, try to find one
                    if (target == null || !target.isValid() || target.isDead()) {
                        if (tickCounter % TARGET_CHECK_INTERVAL == 0) {
                            target = findBestTarget(npc, targetsPlayers, targetsNPCs);
                            if (target != null) {
                                currentTargets.put(npcId, target);
                            }
                        }
                    }
                    
                    // Still no target, just update tick counter
                    if (target == null || !target.isValid() || target.isDead()) {
                        tickCounter++;
                        return;
                    }
                }
                
                // Get latest stats - important for dynamic stat changes
                NPCStats stats = getNPCStats(npcId);
                
                // Calculate distance to target
                double distance = npc.getEntity().getLocation().distance(target.getLocation());
                
                // Update navigation if we have a valid target
                updateNavigation(npc, target, distance, stats);
                
                // Get the actual attack range - hostile NPCs force a shorter range for more aggressive behavior
                double attackRange = npc.getEntity().hasMetadata("hostile") && 
                    npc.getEntity().getMetadata("hostile").get(0).asBoolean() ? 
                    3.0 : stats.getAttackRange();
                
                // Handle attacks - always use the appropriate range
                if (attackCooldown > 0) {
                    attackCooldown--;
                } else if (distance <= attackRange) {
                    // In attack range
                    if (isCharging) {
                        // CRITICAL FIX: Calculate charge rate based on attack speed stat
                        // For 1.0 attackSpeed, it should take 20 ticks for full charge (1 second)
                        float chargeRate = (float)(stats.getAttackSpeed() / 20.0);
                        
                        // Add charge based on attack speed
                        attackCharge += chargeRate;
                        
                        if (attackCharge >= 1.0f) {
                            // Attack fully charged - set attacking flag to prevent new attacks during animation
                            isAttacking = true;
                            
                            // Play attack animation and then handle damage
                            attackTarget(npc, target, 1.0f);
                            
                            // Reset charge and set cooldown based on attack speed
                            attackCharge = 0.0f;
                            
                            // CRITICAL FIX: Calculate cooldown in ticks based on attack speed
                            // attackSpeed of 1.0 = 20 tick cooldown (1 second)
                            // attackSpeed of 2.0 = 10 tick cooldown (0.5 seconds)
                            int baseCooldown = Math.max(1, (int)(20 / stats.getAttackSpeed()));
                            
                            // Apply cooldown reduction for hostile NPCs
                            if (npc.getEntity().hasMetadata("hostile") && 
                                npc.getEntity().getMetadata("hostile").get(0).asBoolean()) {
                                baseCooldown = Math.max(1, (int)(baseCooldown * 0.8)); // 20% faster for hostile NPCs
                            }
                            
                            attackCooldown = baseCooldown;
                            
                            // Reset attacking flag after a short delay (proportional to attack speed)
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                isAttacking = false;
                            }, Math.max(1, (int)(8 / stats.getAttackSpeed())));
                        }
                    }
                } else {
                    // If not in range, build up charge faster when closer to attack range
                    double chargeMultiplier = Math.max(0.1, 1.0 - (distance / (attackRange * 2)));
                    attackCharge = Math.min(0.9f, attackCharge + 0.02f * (float)chargeMultiplier);
                }
                
                tickCounter++;
            }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick
        
        // Store this task
        combatTasks.put(npcId, task);
    }
    
    /**
     * Find nearby entities based on targeting settings
     */
    private void findAndTargetNearbyEntities(NPC npc, boolean targetsPlayers, boolean targetsNPCs) {
        if (!npc.isSpawned()) return;
        
        // IMPORTANT: Only the respawned NPC's health should be reset, all other NPCs should preserve theirs
        UUID respawnedNpcId = npc.getUniqueId();
        
        // Store targeting settings on this NPC instance
        npc.getEntity().setMetadata("targets_players", new FixedMetadataValue(plugin, targetsPlayers));
        npc.getEntity().setMetadata("targets_npcs", new FixedMetadataValue(plugin, targetsNPCs));
        
        // Make sure we have the correct stats for this NPC
        NPCStats stats = getNPCStats(respawnedNpcId);
        
        // Debug stats to verify they're correctly preserved
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("NPC STATS ON RESPAWN: " + 
                npc.getName() + " - Health: " + stats.getMaxHealth() + 
                ", PhysicalDmg: " + stats.getPhysicalDamage() +
                ", MagicDmg: " + stats.getMagicDamage() +
                ", Armor: " + stats.getArmor());
        }

        // Restart combat behavior if this is a hostile NPC
        if (npc.getEntity().hasMetadata("hostile_npc") && 
                npc.getEntity().getMetadata("hostile_npc").get(0).asBoolean()) {
            plugin.getLogger().info("Restarted combat behavior for hostile NPC: " + npc.getName() +
                    " with targets_npcs=" + targetsNPCs);
            startCombatWithSettings(npc, null, targetsPlayers, targetsNPCs);
        }
    }
    
    /**
     * Find the best target for an NPC based on distance and type
     */
    private Entity findBestTarget(NPC npc, boolean targetsPlayers, boolean targetsNPCs) {
        if (!npc.isSpawned()) return null;
        
        // Get the location of the NPC
        Location location = npc.getEntity().getLocation();
        double closestDistance = Double.MAX_VALUE;
        Entity bestTarget = null;
        
        // Get all entities within range
        for (Entity entity : location.getWorld().getNearbyEntities(location, MAX_TARGET_RANGE, MAX_TARGET_RANGE, MAX_TARGET_RANGE)) {
            // CRITICAL FIX: Skip self-targeting - prevent NPCs from targeting themselves
            if (entity.equals(npc.getEntity())) {
                continue;
            }
            
            // Skip non-LivingEntities
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            
            // CRITICAL FIX: Skip players in creative mode
            if (entity instanceof Player) {
                Player player = (Player) entity;
                if (player.getGameMode() == GameMode.CREATIVE || 
                    player.getGameMode() == GameMode.SPECTATOR) {
                    continue;
                }
            }
            
            // Skip dead entities
            if (entity instanceof LivingEntity && ((LivingEntity) entity).getHealth() <= 0) {
                continue;
            }
            
            // Check if entity is valid target based on targeting settings
            if (isValidCombatTarget(npc, entity)) {
                double distance = entity.getLocation().distance(location);
                
                // Prioritize closer targets
                if (distance < closestDistance) {
                    closestDistance = distance;
                    bestTarget = entity;
                }
            }
        }
        
        return bestTarget;
    }

    /**
     * Update the NPC's navigation target and improve pathfinding
     */
    private void updateNavigation(NPC npc, Entity target, double distance, NPCStats stats) {
        if (target == null || !npc.isSpawned()) return;
        
        // Check if we need to update the navigation target
        boolean shouldUpdate = false;
        
        // Always update if we're stuck or not moving
        if (!npc.getNavigator().isNavigating()) {
            shouldUpdate = true;
        }
        
        // Update if we're too far from the target
        if (distance > stats.getAttackRange() * 1.5) {
            shouldUpdate = true;
        }
        
        // Update every few ticks to avoid path thrashing
        if (npc.getEntity().getTicksLived() % 20 == 0) {
            shouldUpdate = true;
        }
        
        // Update navigation if needed
        if (shouldUpdate) {
            // Calculate target position with slight adjustment to prevent NPCs from stacking
            Location targetLoc = target.getLocation().clone();
            double offsetX = (Math.random() - 0.5) * 1.5;
            double offsetZ = (Math.random() - 0.5) * 1.5;
            targetLoc.add(offsetX, 0, offsetZ);
            
            // Adjust speed based on distance
            float speedMod = 1.0f;
            if (distance > stats.getAttackRange() * 3) {
                // Far away - move faster
                speedMod = 1.4f;
            } else if (distance > stats.getAttackRange() * 1.5) {
                // Medium distance
                speedMod = 1.2f;
            }
            
            // Configure navigation parameters for better pathfinding
            npc.getNavigator().getLocalParameters().speedModifier(speedMod);
            npc.getNavigator().getLocalParameters().range(30);
            // Using alternative approach for stuck action since StuckAction class isn't available
            npc.getNavigator().getDefaultParameters().stuckAction((stuckNpc, navigator) -> {
                if (npc.isSpawned() && npc.getEntity() instanceof org.bukkit.entity.LivingEntity) {
                    org.bukkit.entity.LivingEntity living = (org.bukkit.entity.LivingEntity) npc.getEntity();
                    living.setVelocity(living.getVelocity().add(new Vector(0, 0.2, 0)));
                }
                return true;
            });
            
            // Move to the target
            npc.getNavigator().setTarget(targetLoc);
            
            if (plugin.isDebugMode() && npc.getEntity().getTicksLived() % 100 == 0) {
                plugin.getLogger().info("Updated navigation for " + npc.getName() + 
                    " to target " + target.getName() + " at distance " + distance + 
                    " with speed mod " + speedMod);
            }
        }
    }
    
    /**
     * Check if an entity is a valid combat target
     */
    private boolean isValidCombatTarget(NPC npc, Entity entity) {
        if (entity == null || entity.isDead() || entity.equals(npc.getEntity())) {
            return false;
        }
        
        // Must be a LivingEntity
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        
        // If it's a player, check if they're in a valid state
        if (entity instanceof Player) {
            Player player = (Player) entity;
            // Skip players in creative/spectator mode or who are vanished
            if (player.getGameMode() == GameMode.CREATIVE || 
                player.getGameMode() == GameMode.SPECTATOR || 
                player.hasMetadata("vanished")) {
                return false;
            }
            
            // Only target players if this NPC targets players
            return npc.getEntity().hasMetadata("targets_players") && 
                npc.getEntity().getMetadata("targets_players").get(0).asBoolean();
        }
        
        // Check if entity is an NPC
        if (CitizensAPI.getNPCRegistry().isNPC(entity)) {
            // Don't target ourselves
            if (npc.getId() == CitizensAPI.getNPCRegistry().getNPC(entity).getId()) {
                return false;
            }
            
            // Don't target friendly/passive NPCs
            if (entity.hasMetadata("passive_npc")) {
                return false;
            }
            
            // Don't target inventory NPCs or other special NPCs
            if (entity.hasMetadata("inventory_npc") || 
                entity.hasMetadata("trade_npc") || 
                entity.hasMetadata("quest_npc")) {
                return false;
            }
            
            // MODIFIED: Force NPC-to-NPC targeting for combat and hostile NPCs
            if (npc.getEntity().hasMetadata("combat_npc") || npc.getEntity().hasMetadata("hostile_npc")) {
                if (entity.hasMetadata("combat_npc") || entity.hasMetadata("hostile_npc")) {
                    // Always allow NPCs to target each other in combat
                    return true;
                }
            }
            
            // If not one of the special cases above, then check the normal targeting rules
            if (!npc.getEntity().hasMetadata("targets_npcs") || 
                !npc.getEntity().getMetadata("targets_npcs").get(0).asBoolean()) {
                return false;
            }
            
            // Combat NPCs target hostile NPCs
            if (npc.getEntity().hasMetadata("combat_npc")) {
                return entity.hasMetadata("hostile_npc");
            }
            
            // Hostile NPCs target any combat or hostile NPCs
            if (npc.getEntity().hasMetadata("hostile_npc")) {
                return entity.hasMetadata("combat_npc") || entity.hasMetadata("hostile_npc");
            }
        }
        
        return false;
    }

    /**
     * Helper method to determine if an entity is a hostile mob
     */
    private boolean isHostileMob(Entity entity) {
        // List of hostile mob types
        switch (entity.getType()) {
            case ZOMBIE:
            case SKELETON:
            case CREEPER:
            case SPIDER:
            case ENDERMAN:
            case WITCH:
            case BLAZE:
            case GHAST:
            case SLIME:
            case MAGMA_CUBE:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Attack a target entity
     */
    private void attackTarget(NPC npc, Entity target, float chargePercent) {
        if (!npc.isSpawned() || target == null || target.isDead()) return;
        
        // CRITICAL FIX: Skip self-targeting - prevent NPCs from attacking themselves
        if (target.equals(npc.getEntity())) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("NPC " + npc.getName() + " attempted to attack itself. Skipping attack.");
            }
            return;
        }
        
        // CRITICAL FIX: Skip players in creative mode
        if (target instanceof Player) {
            Player player = (Player) target;
            if (player.getGameMode() == GameMode.CREATIVE || 
                player.getGameMode() == GameMode.SPECTATOR) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Skipping attack on player in creative/spectator mode: " + player.getName());
                }
                return;
            }
        }
        
        UUID npcId = npc.getUniqueId();
        NPCStats stats = getNPCStats(npcId);
        
        // Calculate damage based on stats and charge
        int physicalDamage = stats.getPhysicalDamage();
        
        // Debug logs
        plugin.getLogger().info("🔸 ATTACK START: NPC " + npc.getName() + 
            " (ID:" + npc.getId() + ") attacking " + target.getName() + 
            " with base damage: " + physicalDamage + ", charge: " + chargePercent);
        
        // Calculate final damage with charge multiplier
        double baseDamage = physicalDamage * chargePercent;
        
        // Determine critical hit chance based on NPC type
        double critChance = 0.1; // Default 10% chance
        
        // Elite and higher NPCs have higher crit chance
        if (stats.getNpcType() == NPCType.ELITE) critChance = 0.15;
        else if (stats.getNpcType() == NPCType.MINIBOSS) critChance = 0.2;
        else if (stats.getNpcType() == NPCType.BOSS) critChance = 0.25;
        
        boolean isCritical = rand.nextDouble() < critChance;
        
        double finalDamage = isCritical ? baseDamage * 1.5 : baseDamage;
        
        // Debug logs
        plugin.getLogger().info("🔸 ATTACKER STATS CHECK: " + npc.getName() + 
            " - PhysicalDmg: " + physicalDamage + ", Source damage: " + finalDamage +
            ", Critical: " + isCritical);
        
        // Play attack animation first, then apply damage when animation completes
        playAttackAnimation(npc, target, () -> {
            try {
                // Check if everything is still valid
                if (target == null || target.isDead() || !npc.isSpawned()) {
                    plugin.getLogger().info("🔸 ATTACK CANCELLED: Target or NPC no longer valid");
                    return;
                }
                
                // CRITICAL FIX: First check if target is an NPC, before checking if it's a Player
                // This is important because NPCs in Citizens can also be instances of Player
                if (CitizensAPI.getNPCRegistry().isNPC(target)) {
                    // CRITICAL FIX: Skip self damage
                    if (target.equals(npc.getEntity())) {
                        plugin.getLogger().warning("NPC " + npc.getName() + " attempted to damage itself. Skipping damage application.");
                        return;
                    }
                    
                    // Apply damage to the NPC
                    NPC targetNPC = CitizensAPI.getNPCRegistry().getNPC(target);
                    if (targetNPC != null && targetNPC.isSpawned()) {
                        // Apply damage to the NPC
                        plugin.getLogger().info("🔸 DAMAGE NPC: " + npc.getName() + 
                            " -> NPC " + targetNPC.getName() + ", Damage: " + finalDamage);
                        
                        // CRITICAL: Apply direct damage to target NPC
                        applyDamageToNPC(npc, targetNPC, finalDamage, isCritical);
                    }
                }
                else if (target instanceof Player) {
                    Player playerTarget = (Player) target;
                    
                    // CRITICAL FIX: Skip players in creative mode
                    if (playerTarget.getGameMode() == GameMode.CREATIVE || 
                        playerTarget.getGameMode() == GameMode.SPECTATOR) {
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("Skipping damage on player in creative/spectator mode: " + playerTarget.getName());
                        }
                        return;
                    }
                    
                    // Apply damage to player
                    plugin.getLogger().info("🔸 DAMAGE PLAYER: " + npc.getName() + 
                        " -> Player " + target.getName() + ", Damage: " + finalDamage);
                    applyDamageToPlayer(npc, (Player) target, finalDamage, isCritical);
                } else if (target instanceof LivingEntity) {
                    // Apply damage to other entities
                    LivingEntity livingTarget = (LivingEntity) target;
                    livingTarget.damage(finalDamage, npc.getEntity());
                }
                
                // Debug info
                plugin.getLogger().info("🔸 ATTACK COMPLETE: " + npc.getName() + 
                    " -> " + target.getName() + ", Damage: " + finalDamage + 
                    ", Critical: " + isCritical);
            } catch (Exception e) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().warning("Error in attack completion: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Make an NPC counterattack even while being attacked
     */
    public void triggerCounterAttack(NPC npc, Entity attacker) {
        if (npc == null || !npc.isSpawned() || attacker == null) return;
        
        // Get the attack charge data
        BukkitTask combatTask = combatTasks.get(npc.getUniqueId());
        
        // Add the attacker as a target
        currentTargets.put(npc.getUniqueId(), attacker);
        
        // Get stats for this NPC
        NPCStats stats = getNPCStats(npc.getUniqueId());
        
        // Check if the NPC is close enough to counterattack
        double distance = npc.getEntity().getLocation().distance(attacker.getLocation());
        if (distance <= stats.getAttackRange()) {
            // Prepare a counter-attack
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Execute a counter-attack if still in range
                if (npc.isSpawned() && attacker.isValid() && 
                    npc.getEntity().getLocation().distance(attacker.getLocation()) <= stats.getAttackRange()) {
                    
                    // Face the target for better counter-attack visual
                    npc.faceLocation(attacker.getLocation());
                    
                    // Counter-attack with slightly reduced damage
                    attackTarget(npc, attacker, 0.8f);
                    
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("NPC " + npc.getName() + " performed a counter-attack against " + attacker.getName());
                    }
                }
            }, 5L); // Counter-attack after a short delay
        }
    }

    /**
     * Find an NPC ID by its UUID
     * 
     * @param uuid The UUID of the NPC
     * @return The NPC ID, or null if not found
     */
    private String findNpcIdByUuid(java.util.UUID uuid) {
        NPCManager manager = NPCManager.getInstance();
        for (String id : manager.getIds()) {
            NPC npc = manager.getNPC(id);
            if (npc != null && npc.getUniqueId().equals(uuid)) {
                return id;
            }
        }
        return null;
    }

    /**
     * Play attack effects for an NPC
     */
    private void playAttackEffects(NPC npc, Entity target) {
        if (!npc.isSpawned() || target == null) return;
        
        World world = npc.getEntity().getWorld();
        
        // Face the target for the attack
        npc.faceLocation(target.getLocation());
        
        // Make the NPC "swing" by temporarily disabling the lookclose trait
        LookClose lookTrait = npc.getTraitNullable(LookClose.class);
        if (lookTrait != null) {
            boolean wasLooking = lookTrait.toggle();
            
            // Re-enable after a short delay
            if (wasLooking) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (npc.isSpawned()) {
                        lookTrait.toggle();
                    }
                }, 10L);
            }
        }
        
        // CRITICAL FIX: Add the arm swing animation
        if (npc.getEntity() instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) npc.getEntity();
            living.swingMainHand();
            
            // Add attack particle effects
            world.spawnParticle(
                org.bukkit.Particle.SWEEP_ATTACK,
                living.getLocation().add(
                    living.getLocation().getDirection().multiply(1).add(new Vector(0, 1.0, 0))
                ),
                1, 0.0, 0.0, 0.0, 0.0
            );
        }
        
        // Play attack sound
        world.playSound(npc.getEntity().getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.0f);
    }
    
    /**
     * Apply damage to a player
     */
    private void applyDamageToPlayer(NPC npc, Player player, double damage, boolean isCritical) {
        // CRITICAL FIX: Skip players in creative mode
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Skipping damage application to player in creative/spectator mode: " + player.getName());
            }
            return;
        }
        
        // Get their profile to update health properly
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        // Check if player is valid before proceeding
        if (player == null || !player.isOnline()) return;
        
        // Check if NPC is still valid
        if (npc == null || !npc.isSpawned() || npc.getEntity() == null) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("Attempted to apply damage from a null or invalid NPC to player: " + player.getName());
            }
            return;
        }
        
        // Get current player health
        double currentHealth = player.getHealth();
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        
        // Calculate damage reduction from player's armor stat
        double finalDamage = damage;
        try {
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile != null) {
                int armor = profile.getStats().getArmor();
                // Calculate damage reduction (similar to how NPCs calculate it)
                double armorReduction = (armor * 100.0) / (armor + 100.0) / 100.0;
                finalDamage = damage * (1.0 - armorReduction);
                
                // Ensure minimum damage
                finalDamage = Math.max(1.0, finalDamage);
                
                // Apply critical hit multiplier if applicable
                if (isCritical) {
                    finalDamage *= 1.5;
                }
                
                // Debug log armor calculation
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Player armor reduction: " + armor + " armor = " + 
                        (armorReduction * 100) + "% reduction. Raw damage: " + damage + 
                        ", After armor: " + finalDamage);
                }
            }
        } catch (Exception e) {
            // If there's any issue with profile or stats, use the raw damage
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("Error calculating armor reduction: " + e.getMessage());
            }
        }
        
        // Safety check - if player died during the animation delay
        if (!player.isOnline() || player.isDead()) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Player " + player.getName() + " died before damage could be applied");
            }
            return;
        }
        
        try {
            // Apply damage directly without any health-based restrictions
            player.damage(0.1, npc.getEntity()); // Minimal direct damage for attribution and hit effects
            
            // Calculate new health directly
            double newHealth = Math.max(0.0, currentHealth - finalDamage);
            
            // Set health directly - this will kill the player if damage is sufficient
            try {
                player.setHealth(newHealth);
            } catch (IllegalArgumentException e) {
                // If health would be negative, kill the player
                player.setHealth(0.0);
            }
            
            // Apply knockback - with additional null checks
            if (npc.isSpawned() && npc.getEntity() != null && player.isValid()) {
                Vector knockback = player.getLocation().subtract(npc.getEntity().getLocation()).toVector().normalize();
                knockback.multiply(0.5); // Knockback strength
                knockback.setY(0.2); // Small vertical component
                player.setVelocity(player.getVelocity().add(knockback));
            }
            
            // Play hit effects
            player.playEffect(org.bukkit.EntityEffect.HURT);
            
            // Critical hit effect
            if (isCritical) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
                player.getWorld().spawnParticle(
                    org.bukkit.Particle.CRIT,
                    player.getLocation().add(0, 1, 0),
                    10, 0.5, 0.5, 0.5, 0.1
                );
                
                // Add brief slowness on critical hits
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0));
            }
                
            // Debug log
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("NPC " + npc.getName() + " -> Player " + player.getName() + 
                    " Damage: " + finalDamage + ", Critical: " + isCritical + 
                    ", Player Health Before: " + currentHealth + ", After: " + newHealth);
            }
        } catch (Exception e) {
            // Catch any other potential errors to prevent crashing
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("Error applying damage to player " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Set the current target for an NPC
     * 
     * @param npcId The UUID of the NPC
     * @param target The target entity
     */
    public void setCurrentTarget(UUID npcId, Entity target) {
        if (target != null && isValidCombatTarget(CitizensAPI.getNPCRegistry().getByUniqueId(npcId), target)) {
            currentTargets.put(npcId, target);
        }
    }
    
    /**
     * Apply damage to another NPC
     */
    public void applyDamageToNPC(NPC attacker, NPC target, double damage, boolean isCritical) {
        if (target == null || !target.isSpawned()) return;
        
        UUID targetId = target.getUniqueId();
        
        // Get target NPC's stats
        NPCStats targetStats = getNPCStats(targetId);
        
        // Debug logging
        plugin.getLogger().info("🔴 NPC DAMAGE: " + 
            (attacker != null ? attacker.getName() : "null") + " (ID:" + 
            (attacker != null ? attacker.getId() : "null") + ") -> " + 
            target.getName() + " (ID:" + target.getId() + "), Raw damage: " + damage);
        
        // CRITICAL: Apply actual vanilla damage for the red flash effect  
        if (target.getEntity() instanceof LivingEntity) {
            LivingEntity targetEntity = (LivingEntity) target.getEntity();
            
            // Apply a small amount of real damage (0.1) to trigger the red flash effect
            // This is small enough not to kill the NPC but will trigger the visual effect
            if (attacker != null && attacker.isSpawned()) {
                // Apply damage from the attacker entity
                targetEntity.damage(0.1, attacker.getEntity());
            } else {
                // Apply generic damage if no attacker
                targetEntity.damage(0.1);
            }
            
            // REMOVED: No need to play hurt effect manually - the vanilla damage system handles it
            // targetEntity.playEffect(org.bukkit.EntityEffect.HURT);
        }
        
        // Continue with our custom damage handling...
        // DEBUGGING: Check current health tracking
        double currentHealth = 0;
        if (npcHealth.containsKey(targetId)) {
            currentHealth = npcHealth.get(targetId);
            plugin.getLogger().info("🔴 HEALTH TRACKING: Found existing health for " + 
                target.getName() + " in npcHealth map: " + currentHealth);
        } else {
            currentHealth = targetStats.getMaxHealth();
            npcHealth.put(targetId, currentHealth);
            plugin.getLogger().info("🔴 HEALTH TRACKING: Initialized missing health for " + 
                target.getName() + " with max health: " + currentHealth);
        }
        
        // DEBUGGING: Check metadata health
        if (target.getEntity().hasMetadata("current_health")) {
            double metadataHealth = target.getEntity().getMetadata("current_health").get(0).asDouble();
            plugin.getLogger().info("🔴 HEALTH TRACKING: Found health in metadata for " + 
                target.getName() + ": " + metadataHealth);
            
            // CRITICAL FIX: Use metadata health if it exists, as it's more reliable
            currentHealth = metadataHealth;
        }
        
        // Calculate damage reduction from armor
        double armorReduction = targetStats.getArmor() > 0 ? targetStats.getArmor() / (targetStats.getArmor() + 100.0) : 0.0;
        double finalDamage = damage * (1.0 - armorReduction);
        
        if (isCritical) {
            finalDamage *= 1.5;
        }
        
        finalDamage = Math.max(1, finalDamage);
        double newHealth = Math.max(0, currentHealth - finalDamage);
        
        plugin.getLogger().info("🔴 DAMAGE CALCULATION: NPC " + target.getName() + 
            " Health: " + currentHealth + " -> " + newHealth + 
            " (Damage: " + finalDamage + ", Armor Reduction: " + 
            (armorReduction * 100) + "%, Raw: " + damage + ")");
        
        // CRITICAL FIX: Update the health value in BOTH storage mechanisms
        npcHealth.put(targetId, newHealth);
        target.getEntity().setMetadata("current_health", new FixedMetadataValue(plugin, newHealth));
        
        // Update nameplate - CRITICAL: Pass the correct current and max health values
        NPCManager.getInstance().updateNameplate(target, newHealth, targetStats.getMaxHealth());
        
        plugin.getLogger().info("🔴 HEALTH UPDATE: Set " + target.getName() + 
            " health to " + newHealth + "/" + targetStats.getMaxHealth() + 
            " (stored in map and metadata)");
        
        // SOUND EFFECT COOLDOWN FIX: Only play sound if no recent sound has been played
        boolean canPlaySound = true;
        if (target.getEntity().hasMetadata("last_hurt_sound")) {
            long lastSoundTime = target.getEntity().getMetadata("last_hurt_sound").get(0).asLong();
            if (System.currentTimeMillis() - lastSoundTime < 300) { // 300ms sound cooldown
                canPlaySound = false;
            }
        }
        
        // IMPROVED KNOCKBACK SYSTEM with anti-stack protection and safer vector calculations
        if (target.isSpawned() && attacker != null && attacker.isSpawned()) {
            // Check if entity already has recent knockback applied (within last 0.5 seconds)
            long currentTime = System.currentTimeMillis();
            long lastKnockbackTime = 0;
            
            if (target.getEntity().hasMetadata("last_knockback_time")) {
                lastKnockbackTime = target.getEntity().getMetadata("last_knockback_time").get(0).asLong();
            }
            
            // Only apply knockback if enough time has passed (500ms = 0.5 seconds)
            if (currentTime - lastKnockbackTime > 500) {
                try {
                    // Calculate knockback direction with safety checks
                    Location targetLoc = target.getEntity().getLocation();
                    Location attackerLoc = attacker.getEntity().getLocation();
                    
                    // CRITICAL FIX: Ensure locations are valid and in the same world
                    if (targetLoc != null && attackerLoc != null && 
                        targetLoc.getWorld() != null && 
                        targetLoc.getWorld().equals(attackerLoc.getWorld())) {
                        
                        // Calculate direction vector
                        Vector knockback = targetLoc.toVector().subtract(attackerLoc.toVector());
                        
                        // CRITICAL FIX: Check for zero vector (same location)
                        if (knockback.lengthSquared() < 0.0001) {
                            // Use a small random offset to prevent zero vector
                            knockback = new Vector(
                                Math.random() * 0.2 - 0.1,
                                0.2,
                                Math.random() * 0.2 - 0.1
                            );
                        } else {
                            knockback.normalize(); // Normalize the vector
                        }
                        
                        // Scale knockback based on damage but with reasonable limits
                        double knockbackStrength = Math.min(0.3, 0.1 + (finalDamage * 0.01));
                        
                        // Apply horizontal knockback with small vertical component
                        knockback.multiply(knockbackStrength);
                        knockback.setY(0.1); // Reduced vertical component to prevent excessive bouncing
                        
                        if (target.getEntity() instanceof LivingEntity) {
                            LivingEntity living = (LivingEntity) target.getEntity();
                            living.setVelocity(living.getVelocity().add(knockback));
                            
                            // Update last knockback time
                            target.getEntity().setMetadata("last_knockback_time", 
                                new FixedMetadataValue(plugin, currentTime));
                            
                            if (plugin.isDebugMode()) {
                                plugin.getLogger().info("🔴 APPLIED KNOCKBACK: " + knockback + 
                                    " with strength " + knockbackStrength);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Log any errors but don't crash
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().warning("Failed to apply knockback: " + e.getMessage());
                    }
                }
            } else if (plugin.isDebugMode()) {
                plugin.getLogger().info("🔴 KNOCKBACK IGNORED: Too soon since last knockback");
            }
        }
        
        // Show damage indicator
        DamageIndicatorManager damageManager = plugin.getDamageIndicatorManager();
        if (damageManager != null) {
            damageManager.spawnDamageIndicator(target.getEntity().getLocation().add(0, 1, 0), 
                (int)Math.round(finalDamage), isCritical);
        }
        
        // Check if the NPC died
        if (newHealth <= 0) {
            // Handle NPC death
            handleNPCDeath(target, attacker != null ? attacker.getEntity() : null);
        }
    }

    /**
     * Get the health of an NPC
     * 
     * @param npcId The UUID of the NPC
     * @return The current health
     */
    public double getHealth(UUID npcId) {
        return npcHealth.getOrDefault(npcId, 0.0);
    }

    /**
     * Set the health of an NPC
     * 
     * @param npcId The UUID of the NPC
     * @param health The health to set
     */
    public void setHealth(UUID npcId, double health) {
        npcHealth.put(npcId, health);
        
        // Also update the NPC's metadata if it exists
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.getUniqueId().equals(npcId) && npc.isSpawned()) {
                npc.getEntity().setMetadata("current_health", new FixedMetadataValue(plugin, health));
                // Update nameplate too
                NPCManager.getInstance().updateNameplate(npc, health, getNPCStats(npcId).getMaxHealth());
                break;
            }
        }
    }
    
    /**
     * Handle NPC death
     * 
     * @param npc The NPC that died
     * @param killer The entity that killed the NPC
     */
    public void handleNPCDeath(NPC npc, Entity killer) {
        // Store the location where the NPC died for respawn
        final Location deathLocation;
        if (npc != null && npc.isSpawned() && npc.getEntity() != null) {
            deathLocation = npc.getEntity().getLocation().clone();
        } else {
            // If NPC is already despawned, use a fallback location or return without respawning
            if (killer != null) {
                deathLocation = killer.getLocation().clone();
            } else {
                return; // Cannot respawn without a valid location
            }
        }
        
        // Store the NPC ID
        final UUID npcId = npc.getUniqueId();
        final String npcId_string = findNpcIdByUuid(npcId);
        
        // Get NPC stats for respawning with the same stats
        NPCStats stats = getNPCStats(npcId);
        if (stats == null) {
            stats = new NPCStats(); // Fallback to default stats if none found
        }
        
        // Store the final values for usage in the task
        final NPCStats finalStats = stats;
        final String finalNpcId = npcId_string;
        final String npcName = npc.getName();
        
        // Check if this NPC should respawn
        boolean shouldRespawn = true;
        
        // Determine NPC type
        boolean isHostile = npc.getEntity() != null && 
                        npc.getEntity().hasMetadata("hostile_npc") && 
                        npc.getEntity().getMetadata("hostile_npc").get(0).asBoolean();
        
        boolean isCombat = npc.getEntity() != null && 
                        npc.getEntity().hasMetadata("combat_npc") && 
                        npc.getEntity().getMetadata("combat_npc").get(0).asBoolean();
        
        // Basic respawn for all non-passive NPCs
        if (shouldRespawn) {
            // Store the respawn timer
            final int respawnTime = RESPAWN_TIME;
            final Location respawnLocation = deathLocation.clone();
            NPCManager manager = NPCManager.getInstance();
            
            // Schedule the respawn
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        // IMPORTANT: Pass the custom stats when respawning
                        double maxHealth = finalStats.getMaxHealth();
                        int physicalDamage = finalStats.getPhysicalDamage();
                        
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("Respawning NPC " + npcName + 
                                " with stats: Health=" + maxHealth + ", Damage=" + physicalDamage);
                        }
                        
                        // Create a new NPC to replace the old one
                        if (finalNpcId != null && !finalNpcId.isEmpty()) {
                            if (isHostile) {
                                plugin.getServer().dispatchCommand(
                                    plugin.getServer().getConsoleSender(), 
                                    "mmonpc create hostile " + finalNpcId + " " + npcName + " " + 
                                    "default " + maxHealth + " " + physicalDamage
                                );
                            } else if (isCombat) {
                                plugin.getServer().dispatchCommand(
                                    plugin.getServer().getConsoleSender(), 
                                    "mmonpc create combat " + finalNpcId + " " + npcName + " " + 
                                    "default " + maxHealth + " " + physicalDamage
                                );
                            }
                            
                            // Teleport the new NPC to the death location
                            if (respawnLocation != null) {
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        NPC newNpc = manager.getNPC(finalNpcId);
                                        if (newNpc != null) {
                                            // CRITICAL FIX: Check if entity is available before teleporting
                                            try {
                                                newNpc.teleport(respawnLocation, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                                            } catch (Exception e) {
                                                plugin.getLogger().warning("Error during NPC respawn teleport: " + e.getMessage());
                                            }
                                        }
                                    }
                                }.runTaskLater(plugin, 5L);
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error during NPC respawn: " + e.getMessage());
                    }
                }
            }.runTaskLater(plugin, respawnTime * 20L);
            
            // Remove the NPC immediately (nameplate and all)
            manager.removeNPC(finalNpcId != null ? finalNpcId : npc.getUniqueId().toString());
        }
    }
    
    /**
     * Respawn an NPC at the given location
     * 
     * @param npc The NPC to respawn
     * @param location The location to respawn at
     */
    private void respawnNPC(NPC npc, Location location) {
        if (location == null) {
            plugin.getLogger().warning("Cannot respawn NPC " + npc.getName() + " - no respawn location");
            return;
        }
        
        // Respawn the NPC
        npc.spawn(location);
        
        UUID npcId = npc.getUniqueId();
        NPCStats stats = getNPCStats(npcId);
        
        // Reset health
        double maxHealth = stats.getMaxHealth();
        npcHealth.put(npcId, maxHealth);
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("RESPAWN: Reset health for NPC " + npc.getName() + 
                " to max value of " + maxHealth);
        }
        
        // Update nameplate
        NPCManager.getInstance().createHologramNameplate(npc, npc.getName(), maxHealth, maxHealth);
        
        // Get targeting settings from metadata
        boolean targetsPlayers = true;
        boolean targetsNPCs = false;
        
        if (npc.getEntity().hasMetadata("targets_players")) {
            targetsPlayers = npc.getEntity().getMetadata("targets_players").get(0).asBoolean();
        }
        
        if (npc.getEntity().hasMetadata("targets_npcs")) {
            targetsNPCs = npc.getEntity().getMetadata("targets_npcs").get(0).asBoolean();
        }
        
        // Restart combat behavior if the NPC is hostile
        if (npc.getEntity().hasMetadata("hostile_npc") && 
                npc.getEntity().getMetadata("hostile_npc").get(0).asBoolean()) {
            findAndTargetNearbyEntities(npc, targetsPlayers, targetsNPCs);
        }
    }
    
    /**
     * Stop combat behavior for an NPC
     * 
     * @param npcId The UUID of the NPC
     */
    public void stopCombatBehavior(UUID npcId) {
        // Cancel combat task if it exists
        BukkitTask task = combatTasks.remove(npcId);
        if (task != null) {
            task.cancel();
        }
        
        // Remove from target tracking
        currentTargets.remove(npcId);
    }
    
    /**
     * Check if an NPC is in combat
     * 
     * @param npcId The UUID of the NPC
     * @return True if the NPC is in combat
     */
    public boolean isInCombat(UUID npcId) {
        return combatTasks.containsKey(npcId) && currentTargets.containsKey(npcId);
    }
    
    /**
     * Get an NPC's current target
     * 
     * @param npcId The UUID of the NPC
     * @return The target entity, or null if none
     */
    public Entity getCurrentTarget(UUID npcId) {
        return currentTargets.get(npcId);
    }
        
    /**
     * Set whether NPCs target players
     * 
     * @param targetsPlayers Whether to target players
     */
    public void setTargetsPlayers(boolean targetsPlayers) {
        this.targetsPlayers = targetsPlayers;
    }
    
    /**
     * Configure NPC to target other NPCs 
     */
    public void setTargetsNPCs(boolean targetsNPCs) {
        this.targetsNPCs = targetsNPCs;
    }
    
    /**
     * Set whether NPCs target hostile mobs
     * 
     * @param targetsHostileMobs Whether to target hostile mobs
     */
    public void setTargetsHostileMobs(boolean targetsHostileMobs) {
        this.targetsHostileMobs = targetsHostileMobs;
    }

    
}