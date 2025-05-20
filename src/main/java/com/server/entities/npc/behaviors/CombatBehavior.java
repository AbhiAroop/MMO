package com.server.entities.npc.behaviors;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.entities.npc.CombatHandler;
import com.server.entities.npc.NPCManager;

import net.citizensnpcs.api.npc.NPC;

/**
 * Combat behavior for NPCs that can engage in combat
 */
public class CombatBehavior implements NPCBehavior {

    private NPC npc;
    private Main plugin;
    private CombatHandler combatHandler;
    private boolean isInitialized = false;
    private boolean isHostile = false;
    
    // Combat behavior settings
    private boolean targetsPlayers = true;
    private boolean targetsNPCs = false;
    private boolean targetsHostileMobs = false;
    
    /**
     * Create a new combat behavior
     */
    public CombatBehavior() {
        this.plugin = Main.getInstance();
        this.combatHandler = NPCManager.getInstance().getCombatHandler();
    }
    
    /**
     * Create a combat behavior with specific targeting settings
     * 
     * @param targetsPlayers Whether to target players
     * @param targetsNPCs Whether to target NPCs
     * @param targetsHostileMobs Whether to target hostile mobs
     */
    public CombatBehavior(boolean targetsPlayers, boolean targetsNPCs, boolean targetsHostileMobs) {
        this();
        this.targetsPlayers = targetsPlayers;
        this.targetsNPCs = targetsNPCs;
        this.targetsHostileMobs = targetsHostileMobs;
    }

    @Override
    public void initialize(NPC npc) {
        this.npc = npc;
        this.isInitialized = true;
        
        // Load settings from metadata if available
        if (npc.isSpawned()) {
            if (npc.getEntity().hasMetadata("targets_players")) {
                this.targetsPlayers = npc.getEntity().getMetadata("targets_players").get(0).asBoolean();
            }
            
            if (npc.getEntity().hasMetadata("targets_npcs")) {
                this.targetsNPCs = npc.getEntity().getMetadata("targets_npcs").get(0).asBoolean();
            }
            
            if (npc.getEntity().hasMetadata("hostile")) {
                this.isHostile = npc.getEntity().getMetadata("hostile").get(0).asBoolean();
            }
        }
        
        if (plugin.isDebugEnabled(DebugSystem.NPC)) {
            plugin.debugLog(DebugSystem.NPC,"Initialized CombatBehavior for NPC " + npc.getName() + 
                                   " [targetsPlayers=" + targetsPlayers + 
                                   ", targetsNPCs=" + targetsNPCs + 
                                   ", isHostile=" + isHostile + "]");
        }
    }

    @Override
    public void update() {
        // Nothing to do in update - combat is handled by the combat handler tasks
    }

    @Override
    public boolean onDamage(Entity source, double amount) {
        if (!isInitialized || !npc.isSpawned()) return false;
        
        // Make the NPC hostile when damaged
        setIsHostile(true);
        
        // If source is a player, target them
        if (source instanceof Player) {
            startCombat((Player)source);
        }
        
        // Allow damage to be processed
        return false;
    }

    /**
     * Handle damage from a player
     * @param player The player that damaged this NPC
     * @param amount The damage amount
     */
    public void onDamage(Player player, double amount) {
        if (!isInitialized || !npc.isSpawned()) return;
        
        // Make the NPC hostile when damaged
        setIsHostile(true);
        
        // Apply damage through combat handler
        combatHandler.applyDamageToNPC(null, npc, amount, false);
        
        // Start combat with the player as target
        startCombat(player);
    }

    @Override
    public void onInteract(Player player, boolean isRightClick) {
        if (!isInitialized) return;
        
        // If left-click and hostile, initiate combat
        if (!isRightClick && isHostile) {
            startCombat(player);
        }
    }

    @Override
    public void cleanup() {
        if (!isInitialized) return;
        
        // Stop combat if active
        combatHandler.stopCombatBehavior(npc.getUniqueId());
        isInitialized = false;
    }

    /**
     * Start combat with a target
     * 
     * @param target The target entity
     */
    public void startCombat(Entity target) {
        if (!isInitialized || !npc.isSpawned()) return;
        
        // Use appropriate combat behavior based on whether NPC is hostile
        if (isHostile && target instanceof Player) {
            // Enhanced pathfinding for hostile NPCs
            configureBetterPathfinding();
            
            // Use hostile behavior for aggressive NPCs
            combatHandler.startHostileCombatBehavior(npc, (Player)target);
            
            if (plugin.isDebugEnabled(DebugSystem.NPC)) {
                plugin.debugLog(DebugSystem.NPC,"Started hostile combat behavior for NPC " + npc.getName() + 
                                    " targeting " + target.getName());
            }
        } else {
            // Configure standard pathfinding
            configureBetterPathfinding();
            
            // Use regular combat behavior for neutral NPCs
            if (target instanceof Player) {
                combatHandler.startCombatBehavior(npc, (Player)target);
            } else {
                // For non-player targets, just set the target directly
                combatHandler.setCurrentTarget(npc.getUniqueId(), target);
                // And start generic combat behavior
                combatHandler.startCombatBehavior(npc, null);
            }
            
            if (plugin.isDebugEnabled(DebugSystem.NPC)) {
                plugin.debugLog(DebugSystem.NPC,"Started regular combat behavior for NPC " + npc.getName() + 
                                    " targeting " + target.getName());
            }
        }
    }
    

    /**
     * Configure better pathfinding for NPCs
     */
    private void configureBetterPathfinding() {
        if (!npc.isSpawned()) return;
        
        // Get the navigator
        net.citizensnpcs.api.ai.Navigator navigator = npc.getNavigator();
        net.citizensnpcs.api.ai.NavigatorParameters params = navigator.getLocalParameters();
        
        // Improve pathfinding
        params.attackStrategy((attacker, target) -> true); // Simple attack strategy
        params.speedModifier(1.8f); // Reasonable movement speed
        params.range(40); // Good navigation range
        params.avoidWater(false); // Don't avoid water
        params.stationaryTicks(30); // More stationary ticks before considering stuck
        params.distanceMargin(1.5); // Better distance margin
        
        // Improve stuck handling
        params.stuckAction((stuckNpc, nav) -> {
            // Allow more attempts before giving up
            if (npc.isSpawned() && npc.getEntity() instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) npc.getEntity();
                
                // Try to jump over obstacles
                living.setVelocity(new Vector(
                    living.getVelocity().getX() * 1.1,
                    0.4, // Better jump height
                    living.getVelocity().getZ() * 1.1
                ));
                
                return false; // Don't give up, continue navigation
            }
            return false;
        });
    }
    
    /**
     * Stop combat
     */
    public void stopCombat() {
        if (!isInitialized) return;
        
        combatHandler.stopCombatBehavior(npc.getUniqueId());
        
        if (plugin.isDebugEnabled(DebugSystem.NPC)) {
            plugin.debugLog(DebugSystem.NPC,"Stopped combat for NPC " + npc.getName());
        }
    }
    
    /**
     * Check if this NPC is in combat
     * 
     * @return True if in combat
     */
    public boolean isInCombat() {
        return isInitialized && combatHandler.isInCombat(npc.getUniqueId());
    }
    
    /**
     * Get the current target
     * 
     * @return The current target entity or null if none
     */
    public Entity getCurrentTarget() {
        if (!isInitialized) return null;
        return combatHandler.getCurrentTarget(npc.getUniqueId());
    }
    
    /**
     * Set whether this NPC is hostile (attacks on sight)
     * 
     * @param hostile True if hostile
     */
    public void setIsHostile(boolean hostile) {
        this.isHostile = hostile;
        
        if (npc != null && npc.isSpawned()) {
            npc.getEntity().setMetadata("hostile", new FixedMetadataValue(plugin, hostile));
        }
    }
    
    /**
     * Set whether this NPC targets players
     * 
     * @param targetsPlayers True to target players
     */
    public void setTargetsPlayers(boolean targetsPlayers) {
        this.targetsPlayers = targetsPlayers;
        
        if (npc != null && npc.isSpawned()) {
            npc.getEntity().setMetadata("targets_players", new FixedMetadataValue(plugin, targetsPlayers));
        }
    }
    
    /**
     * Set whether this NPC targets other NPCs
     * 
     * @param targetsNPCs True to target NPCs
     */
    public void setTargetsNPCs(boolean targetsNPCs) {
        this.targetsNPCs = targetsNPCs;
        
        if (npc != null && npc.isSpawned()) {
            npc.getEntity().setMetadata("targets_npcs", new FixedMetadataValue(plugin, targetsNPCs));
        }
    }
    
    /**
     * Set whether this NPC targets hostile mobs
     * 
     * @param targetsHostileMobs True to target hostile mobs
     */
    public void setTargetsHostileMobs(boolean targetsHostileMobs) {
        this.targetsHostileMobs = targetsHostileMobs;
    }
    
    /**
     * Get whether this NPC is hostile
     * 
     * @return True if hostile
     */
    public boolean isHostile() {
        return isHostile;
    }
    
    /**
     * Get whether this NPC targets players
     * 
     * @return True if targeting players
     */
    public boolean targetsPlayers() {
        return targetsPlayers;
    }
    
    /**
     * Get whether this NPC targets other NPCs
     * 
     * @return True if targeting NPCs
     */
    public boolean targetsNPCs() {
        return targetsNPCs;
    }
    
    /**
     * Get whether this NPC targets hostile mobs
     * 
     * @return True if targeting hostile mobs
     */
    public boolean targetsHostileMobs() {
        return targetsHostileMobs;
    }

    @Override
    public int getPriority() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}