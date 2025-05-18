package com.server.entities.npc.behaviors;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.server.Main;
import com.server.entities.npc.NPCManager;
import com.server.entities.npc.NPCStats;

import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.npc.NPC;

/**
 * Behavior for NPCs that flee from threats
 */
public class FleeingBehavior implements NPCBehavior {

    private NPC npc;
    private NPCStats stats;
    private double currentHealth;
    private BukkitTask fleeingTask;
    private Entity fleeingFrom;
    private boolean isFleeing = false;
    private int fleeingCooldown = 0;
    private final int FLEE_DURATION = 100; // 5 seconds at 20 ticks per second
    
    @Override
    public void initialize(NPC npc) {
        this.npc = npc;
        
        // Load stats from metadata
        this.stats = loadStatsFromMetadata();
        
        // Initialize health if not already set
        if (npc.isSpawned() && npc.getEntity().hasMetadata("current_health")) {
            this.currentHealth = npc.getEntity().getMetadata("current_health").get(0).asDouble();
        } else {
            this.currentHealth = stats.getMaxHealth();
            if (npc.isSpawned()) {
                npc.getEntity().setMetadata("current_health", new FixedMetadataValue(Main.getInstance(), currentHealth));
            }
        }
        
        // Start fleeing task
        startFleeingTask();
    }

    @Override
    public void update() {
        // Most updates happen in the fleeing task
        
        // Check if nameplate health needs update
        if (npc.isSpawned()) {
            NPCManager.getInstance().updateNameplate(npc, currentHealth, stats.getMaxHealth());
        }
    }

    @Override
    public boolean onDamage(Entity source, double amount) {
        if (!npc.isSpawned()) return true;
        
        // Calculate damage reduction from armor
        double armorReduction = stats.getArmorDamageReduction() / 100.0;
        double finalDamage = amount * (1.0 - armorReduction);
        finalDamage = Math.max(1, Math.round(finalDamage));
        
        // Apply damage
        currentHealth -= finalDamage;
        currentHealth = Math.max(0, currentHealth);
        
        // Store current health in metadata for persistence
        npc.getEntity().setMetadata("current_health", new FixedMetadataValue(Main.getInstance(), currentHealth));
        
        // Update the nameplate with the new health value
        NPCManager.getInstance().updateNameplate(npc, currentHealth, stats.getMaxHealth());
        
        // Play hurt animation/sound
        npc.getEntity().getWorld().playSound(
            npc.getEntity().getLocation(),
            Sound.ENTITY_PLAYER_HURT,
            1.0f, 1.0f
        );
        
        // Show damage particles
        npc.getEntity().getWorld().spawnParticle(
            org.bukkit.Particle.DAMAGE_INDICATOR,
            npc.getEntity().getLocation().add(0, 1, 0),
            8, 0.2, 0.5, 0.2, 0.05
        );
        
        // Start fleeing from the damage source
        if (source != null && currentHealth > 0) {
            startFleeingFrom(source);
        }
        
        // Handle death if health <= 0
        if (currentHealth <= 0) {
            handleDeath(source);
            return true;
        }
        
        return true; // Always cancel original damage event, we handle it manually
    }

    @Override
    public void onInteract(Player player, boolean isRightClick) {
        // If left-clicked, react as if damaged
        if (!isRightClick) {
            onDamage(player, 0); // 0 damage but still trigger fleeing
        }
    }

    @Override
    public void cleanup() {
        if (fleeingTask != null) {
            fleeingTask.cancel();
            fleeingTask = null;
        }
    }

    @Override
    public int getPriority() {
        // Fleeing behaviors should have high priority
        return 8;
    }
    
    /**
     * Start fleeing from an entity
     * 
     * @param entity The entity to flee from
     */
    public void startFleeingFrom(Entity entity) {
        this.fleeingFrom = entity;
        this.isFleeing = true;
        this.fleeingCooldown = FLEE_DURATION;
        
        // Play a fleeing sound
        if (npc.isSpawned()) {
            npc.getEntity().getWorld().playSound(
                npc.getEntity().getLocation(),
                Sound.ENTITY_VILLAGER_HURT,
                1.0f, 1.2f
            );
        }
    }
    
    /**
     * Load NPC stats from metadata
     */
    private NPCStats loadStatsFromMetadata() {
        NPCStats stats = new NPCStats();
        
        if (npc.isSpawned()) {
            // Get level
            if (npc.getEntity().hasMetadata("level")) {
                stats.setLevel(npc.getEntity().getMetadata("level").get(0).asInt());
            }
            
            // Get max health
            if (npc.getEntity().hasMetadata("max_health")) {
                stats.setMaxHealth(npc.getEntity().getMetadata("max_health").get(0).asDouble());
            }
            
            // Get armor
            if (npc.getEntity().hasMetadata("armor")) {
                stats.setArmor(npc.getEntity().getMetadata("armor").get(0).asInt());
            }
        }
        
        return stats;
    }
    
    /**
     * Start the fleeing task
     */
    private void startFleeingTask() {
        // Cancel any existing task first
        if (fleeingTask != null) {
            fleeingTask.cancel();
        }
        
        fleeingTask = new BukkitRunnable() {
            private int tickCounter = 0;
            
            @Override
            public void run() {
                // Skip if NPC is not spawned
                if (!npc.isSpawned() || npc.getEntity().isDead()) {
                    this.cancel();
                    return;
                }
                
                // Decrease fleeing cooldown if active
                if (fleeingCooldown > 0) {
                    fleeingCooldown--;
                    
                    // Stop fleeing when cooldown expires
                    if (fleeingCooldown == 0) {
                        isFleeing = false;
                        fleeingFrom = null;
                        
                        // Stop navigating
                        if (npc.getNavigator().isNavigating()) {
                            npc.getNavigator().cancelNavigation();
                        }
                    }
                }
                
                // Check if we should be fleeing
                if (isFleeing && fleeingFrom != null && fleeingFrom.isValid() && !fleeingFrom.isDead() &&
                    fleeingFrom.getWorld() == npc.getEntity().getWorld()) {
                    
                    // Only update fleeing direction every second (20 ticks)
                    if (tickCounter % 20 == 0) {
                        updateFleeingDirection();
                    }
                }
                
                tickCounter++;
            }
        }.runTaskTimer(Main.getInstance(), 0, 1);
    }
    
    /**
     * Update the direction the NPC is fleeing towards
     */
    private void updateFleeingDirection() {
        if (!isFleeing || fleeingFrom == null || !npc.isSpawned()) return;
        
        // Get a vector pointing away from the threat
        Location npcLoc = npc.getEntity().getLocation();
        Vector fleeVector = npcLoc.clone().subtract(fleeingFrom.getLocation()).toVector().normalize();
        
        // Calculate a location to run to (8-12 blocks away in flee direction)
        double distance = 8 + Math.random() * 4; // 8-12 blocks
        Location targetLoc = npcLoc.clone().add(fleeVector.multiply(distance));
        
        // Find a safe location (not inside a block)
        targetLoc = findSafeLocation(targetLoc);
        
        // Set the fleeing parameters - fast movement and ignore obstacles
        Navigator navigator = npc.getNavigator();
        navigator.getLocalParameters()
            .speedModifier(1.5f) // Run faster when fleeing
            .range(20)
            .stuckAction(null)
            .distanceMargin(1.0);
            
        // Navigate to the fleeing location
        navigator.setTarget(targetLoc);
        
        // Look where we're going
        npc.faceLocation(targetLoc);
        
        // Play fleeing sound occasionally
        if (Math.random() < 0.3) {
            npc.getEntity().getWorld().playSound(
                npc.getEntity().getLocation(),
                Sound.ENTITY_VILLAGER_HURT,
                0.5f, 1.2f
            );
        }
    }
    
    /**
     * Find a safe location nearby that isn't inside blocks
     * 
     * @param origin The original target location
     * @return A safe location
     */
    private Location findSafeLocation(Location origin) {
        // Try to find a non-solid block at the height of the NPC
        for (int attempt = 0; attempt < 10; attempt++) {
            Location check = origin.clone();
            
            // Start at ground level and work up
            for (int y = 0; y < 10; y++) {
                check.setY(origin.getWorld().getHighestBlockYAt(check) + 1 + y);
                
                // Check if both this block and the one above are non-solid (room for NPC)
                if (!check.getBlock().getType().isSolid() && 
                    !check.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                    return check;
                }
            }
            
            // If we didn't find a spot, try a slightly different location
            origin.add((Math.random() - 0.5) * 3, 0, (Math.random() - 0.5) * 3);
        }
        
        // If all else fails, return the original location for the navigator to handle
        return origin;
    }
    
    /**
     * Handle NPC death
     * 
     * @param killer The entity that killed this NPC
     */
    private void handleDeath(Entity killer) {
        // Play death effects
        if (npc.isSpawned()) {
            Location location = npc.getEntity().getLocation().clone();
            
            // Play death sound
            location.getWorld().playSound(location, Sound.ENTITY_VILLAGER_DEATH, 1.0f, 1.0f);
            
            // Play particles
            location.getWorld().spawnParticle(
                org.bukkit.Particle.CLOUD,
                location.add(0, 1, 0),
                15, 0.5, 0.5, 0.5, 0.1
            );
        }
        
        // Remove the NPC (passive NPCs don't respawn)
        if (npc.isSpawned()) {
            npc.despawn();
        }
        
        // Schedule for permanent removal
        new BukkitRunnable() {
            @Override
            public void run() {
                NPCManager.getInstance().removeNameplate(npc.getUniqueId());
                npc.destroy();
            }
        }.runTaskLater(Main.getInstance(), 20L);
    }
}