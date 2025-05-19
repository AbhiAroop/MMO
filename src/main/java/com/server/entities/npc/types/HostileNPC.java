package com.server.entities.npc.types;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.entities.npc.NPCManager;
import com.server.entities.npc.NPCStats;
import com.server.entities.npc.NPCType;
import com.server.entities.npc.behaviors.CombatBehavior;

import net.citizensnpcs.api.npc.NPC;

/**
 * A hostile NPC that attacks on sight
 */
public class HostileNPC extends CombatNPC {
    
    private BukkitRunnable scanTask;
    private BukkitRunnable sprintTask;
    
    /**
     * Create a new hostile NPC
     * 
     * @param id The unique ID
     * @param name The display name
     * @param stats The NPC stats
     */
    public HostileNPC(String id, String name, NPCStats stats) {
        super(id, name, stats);
        
        // Hostile NPCs use ELITE type by default
        stats.setNpcType(NPCType.ELITE);
        
        // Set aggressive stats
        stats.setAttackSpeed(1.2); // Faster attacks
        stats.setAttackRange(3.0); // Shorter range forces getting closer
        
        // Configure to target both players and NPCs by default - but DON'T set hostile yet
        // as the NPC doesn't exist yet
        setTargetsPlayers(true);
        setTargetsNPCs(true);
        
        // Don't call setHostile(true) here as npc is still null
        // We'll set it as hostile in the spawn method instead
    }
    
   @Override
    public NPC spawn(Location location, String skin) {
        // Use base spawn method
        NPC npc = super.spawn(location, skin);
        
        // Additional hostile-specific metadata
        if (npc != null && npc.isSpawned()) {
            npc.getEntity().setMetadata("hostile", new FixedMetadataValue(plugin, true));
            npc.getEntity().setMetadata("hostile_npc", new FixedMetadataValue(plugin, true)); // Add this key metadata flag
            npc.getEntity().setMetadata("combat_npc", new FixedMetadataValue(plugin, true));
            npc.getEntity().setMetadata("targets_npcs", new FixedMetadataValue(plugin, true)); // Critical - explicit flag
            npc.getEntity().setMetadata("targets_players", new FixedMetadataValue(plugin, true));
            npc.getEntity().setMetadata("detection_range", new FixedMetadataValue(plugin, 15.0)); // 15 block range
            npc.getEntity().setMetadata("always_sprint", new FixedMetadataValue(plugin, true));   // Always sprint
            
            // NOW we can set it as hostile since the NPC exists
            setHostile(true);
            
            // Configure the combat behavior properly
            CombatBehavior combatBehavior = (CombatBehavior) behaviors.get("combat");
            if (combatBehavior != null) {
                combatBehavior.setIsHostile(true);
                combatBehavior.setTargetsNPCs(true);  // Force this to true
                combatBehavior.setTargetsPlayers(true);
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Initialized CombatBehavior for hostile NPC " + name + 
                        " [targetsPlayers=true, targetsNPCs=true, isHostile=true]");
                }
            }
            
            // Start the sprint task to ensure NPC is always sprinting
            startSprintTask();
        }
                
        return npc;
    }
    
    /**
     * Start a task to ensure NPC is always sprinting
     */
    private void startSprintTask() {
        // Cancel any existing sprint task
        if (sprintTask != null) {
            sprintTask.cancel();
        }
        
        // Create a new task that applies speed effect every 20 ticks to ensure sprinting
        sprintTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (npc == null || !npc.isSpawned()) {
                    this.cancel();
                    return;
                }
                
                // Apply speed effect to simulate sprinting
                if (npc.getEntity() instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity) npc.getEntity();
                    
                    // Apply speed 1 effect (20% faster movement)
                    living.addPotionEffect(new PotionEffect(
                        PotionEffectType.SPEED, 
                        40,  // Duration for 2 seconds (40 ticks)
                        1,   // Amplifier: Speed II
                        false, // Ambient
                        false  // Hide particles
                    ));
                    
                    // Set the run speed multiplier in Citizens
                    if (npc.getNavigator().isNavigating()) {
                        npc.getNavigator().getLocalParameters().speedModifier(2.0f);
                    }
                }
            }
        };
        
        // Run every second (20 ticks)
        sprintTask.runTaskTimer(plugin, 1L, 20L);
    }
    
    /**
     * Start a periodic task to scan for targets within a limited range
     */
    private void startTargetScanTask() {
        // Cancel any existing task
        if (scanTask != null) {
            scanTask.cancel();
        }
        
        scanTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (npc == null || !npc.isSpawned()) {
                    this.cancel();
                    return;
                }
                
                // Only scan if not already in combat
                CombatBehavior combatBehavior = (CombatBehavior) behaviors.get("combat");
                if (combatBehavior != null && !combatBehavior.isInCombat()) {
                    Player nearestPlayer = findNearestPlayerInRange(15.0); // Strict 15 block limit
                    if (nearestPlayer != null) {
                        // Target found within range - begin combat
                        combatBehavior.startCombat(nearestPlayer);
                        
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("HostileNPC " + name + " detected player " + 
                                                  nearestPlayer.getName() + " in range and engaging");
                        }
                    } else if (combatBehavior.targetsNPCs()) {
                        // Try to find NPC targets if no players in range
                        findNearestNPCInRange(15.0);
                    }
                }
            }
        };
        
        // Run scan every 1 second (20 ticks)
        scanTask.runTaskTimer(Main.getInstance(), 20L, 20L);
    }
    
    @Override
    public void onDamage(Player player, double damage) {
        super.onDamage(player, damage); // Make sure the parent CombatNPC's method is called
        
        // Get the combat behavior
        CombatBehavior behavior = (CombatBehavior) getBehavior("combat");
        if (behavior != null) {
            // Make hostile NPCs aggressive when attacked
            behavior.setTargetsPlayers(true);
            
            // Debug message
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Started aggressive behavior for hostile NPC: " + name + " with initial target: " + player.getName());
            }
        }
    }
    
    
    public void cleanup() {
        
        // Cancel our tasks when cleaning up
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
        
        if (sprintTask != null) {
            sprintTask.cancel();
            sprintTask = null;
        }
    }
    
    /**
     * Finds the nearest player within the specified range
     * 
     * @param range The maximum distance to search
     * @return The nearest player or null if none found
     */
    private Player findNearestPlayerInRange(double range) {
        if (npc == null || !npc.isSpawned()) {
            return null;
        }
        
        Location location = npc.getEntity().getLocation();
        Player closest = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (Entity entity : location.getWorld().getNearbyEntities(location, range, range, range)) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                double distance = location.distance(player.getLocation());
                
                if (distance <= range && distance < closestDistance) {
                    closest = player;
                    closestDistance = distance;
                }
            }
        }
        
        return closest;
    }
    
    /**
     * Finds the nearest NPC within the specified range and engages in combat if found
     * 
     * @param range The maximum distance to search
     * @return The nearest NPC or null if none found
     */
    private NPC findNearestNPCInRange(double range) {
        if (npc == null || !npc.isSpawned()) {
            return null;
        }
        
        Location location = npc.getEntity().getLocation();
        NPC closest = null;
        double closestDistance = Double.MAX_VALUE;
        
        // Use NPCRegistry instead of getAllNPCs which doesn't exist
        for (NPC otherNPC : NPCManager.getInstance().getNPCRegistry()) {
            if (otherNPC != npc && otherNPC.isSpawned()) {
                // Only target NPCs that would be valid targets (combat or hostile NPCs)
                if (!otherNPC.getEntity().hasMetadata("passive_npc") && 
                    (otherNPC.getEntity().hasMetadata("combat_npc") || 
                    otherNPC.getEntity().hasMetadata("hostile_npc"))) {
                    
                    double distance = location.distance(otherNPC.getEntity().getLocation());
                    
                    if (distance <= range && distance < closestDistance) {
                        closest = otherNPC;
                        closestDistance = distance;
                    }
                }
            }
        }
        
        // If we found an NPC target, start combat with it
        if (closest != null) {
            CombatBehavior combatBehavior = (CombatBehavior) behaviors.get("combat");
            if (combatBehavior != null) {
                combatBehavior.startCombat(closest.getEntity());
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Hostile NPC " + name + " targeting: " + 
                        closest.getName() + " at distance: " + closestDistance);
                }
            }
        }
        
        return closest;
    }
}