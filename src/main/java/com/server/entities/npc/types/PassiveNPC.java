package com.server.entities.npc.types;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.server.entities.npc.NPCManager;
import com.server.entities.npc.NPCStats;

import net.citizensnpcs.api.npc.NPC;
// Remove incorrect import
// import net.citizensnpcs.trait.Pushable;

/**
 * A passive NPC that will run away when attacked
 */
public class PassiveNPC extends BaseNPC {
    
    private String dialogueId;
    private boolean isRunning = false;
    
    /**
     * Create a new passive NPC
     * 
     * @param id The unique ID
     * @param name The display name
     * @param stats The NPC stats
     */
    public PassiveNPC(String id, String name, NPCStats stats) {
        super(id, name, stats);
        this.dialogueId = "passive_" + id;
        
        // Setup a default dialogue
        setupDefaultDialogue(dialogueId, "Hello there! Please don't hurt me.");
    }
    
    @Override
    public NPC spawn(Location location, String skin) {
        this.npc = NPCManager.getInstance().createNPC(id, name, location, skin, true);
        
        // Make it vulnerable to attack
        if (npc.isSpawned()) {
            npc.getEntity().setInvulnerable(false);
        }
        
        // Instead of using Pushable trait which doesn't exist,
        // we'll make the NPC move when collided with using our own logic
        // This will be implemented in a separate collision handler
        
        // Set as passive NPC
        if (npc.isSpawned()) {
            npc.getEntity().setMetadata("passive_npc", new FixedMetadataValue(plugin, true));
            
            // Set other metadata to control behavior
            npc.getEntity().setMetadata("can_be_pushed", new FixedMetadataValue(plugin, true));
        }
        
        applyBaseMetadata();
        
        // Create custom nameplate
        NPCManager.getInstance().createHologramNameplate(npc, name, stats.getMaxHealth(), stats.getMaxHealth());
        
        return npc;
    }
    
    @Override
    public void onInteract(Player player, NPC npc, boolean rightClick) {
        if (rightClick) {
            // Start dialogue on right-click if not running
            if (!isRunning) {
                startDialogue(player, dialogueId);
            } else {
                sendMessage(player, "Leave me alone!");
            }
        } else if (!isRunning) {
            // Just a greeting on left-click
            sendMessage(player, "Hello there! Right-click to talk to me.");
        }
    }
    
    /**
     * Called when this NPC is damaged
     * 
     * @param player The attacking player
     * @param damage The damage amount
     */
    public void onDamage(Player player, double damage) {
        // FIXED: Remove check that prevented damage while running
        // Allow damage even if already running
        if (!npc.isSpawned()) return;
        
        // CRITICAL FIX: Make sure we have the correct health value first
        double currentHealth = npc.getEntity().hasMetadata("current_health") ?
            npc.getEntity().getMetadata("current_health").get(0).asDouble() : stats.getMaxHealth();
        
        // Calculate damage reduction from armor
        double armorReduction = stats.getArmor() > 0 ? stats.getArmor() / (stats.getArmor() + 100.0) : 0.0;
        double finalDamage = damage * (1.0 - armorReduction);
        finalDamage = Math.max(1, finalDamage);
        
        // CRITICAL FIX: Update health value
        double newHealth = Math.max(0, currentHealth - finalDamage);
        
        // Store updated health in metadata
        npc.getEntity().setMetadata("current_health", new FixedMetadataValue(plugin, newHealth));
        
        // Update the nameplate
        NPCManager.getInstance().updateNameplate(npc, newHealth, stats.getMaxHealth());
        
        // Display damage indicators
        if (plugin.getDamageIndicatorManager() != null) {
            plugin.getDamageIndicatorManager().spawnDamageIndicator(
                npc.getEntity().getLocation().add(0, 1, 0),
                (int) finalDamage,
                false);
        }
        
        // Play hurt sound and animation
        npc.getEntity().getWorld().playSound(
            npc.getEntity().getLocation(),
            org.bukkit.Sound.ENTITY_PLAYER_HURT,
            0.8f, 1.0f
        );
        
        // Debug log the damage
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("PassiveNPC " + name + " damaged by " + player.getName() + 
                ": health " + currentHealth + " → " + newHealth + 
                " (damage: " + finalDamage + ")");
        }
        
        // Check if NPC is now dead
        if (newHealth <= 0) {
            // Handle death
            plugin.getLogger().info("PassiveNPC " + name + " was killed by " + player.getName());
            handleDeath(player);
            return;
        }
        
        // If not already fleeing, start fleeing
        if (!isRunning) {
            isRunning = true;
            
            // Run away from the player
            sendMessage(player, "Aaaah! Help!");
            
            // Calculate direction away from player
            Vector direction = npc.getEntity().getLocation().subtract(player.getLocation()).toVector().normalize();
            
            // Run away for 5 seconds
            new BukkitRunnable() {
                int ticks = 0;
                final int maxTicks = 5 * 20; // 5 seconds
                
                @Override
                public void run() {
                    if (!npc.isSpawned() || ticks++ > maxTicks) {
                        isRunning = false;
                        this.cancel();
                        return;
                    }
                    
                    // Only update movement every few ticks to reduce jitter
                    if (ticks % 10 == 0) {
                        // Calculate new run away position
                        Location npcLoc = npc.getEntity().getLocation();
                        Location runTo = npcLoc.clone().add(direction.clone().multiply(10));
                        
                        // Use Citizens pathfinding
                        npc.getNavigator().getLocalParameters().speedModifier(1.5f);
                        npc.getNavigator().setTarget(runTo);
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        }
    }

    /**
     * Called when this NPC is damaged by magic
     * 
     * @param player The attacking player
     * @param damage The damage amount
     */
    public void onMagicDamage(Player player, double damage) {
        if (!npc.isSpawned()) return;
        
        // CRITICAL FIX: Make sure we have the correct health value first
        double currentHealth = npc.getEntity().hasMetadata("current_health") ?
            npc.getEntity().getMetadata("current_health").get(0).asDouble() : stats.getMaxHealth();
        
        // Calculate damage reduction from magic resist
        double magicResistReduction = stats.getMagicResist() > 0 ? 
            stats.getMagicResist() / (stats.getMagicResist() + 100.0) : 0.0;
        double finalDamage = damage * (1.0 - magicResistReduction);
        
        // Apply minimum damage
        finalDamage = Math.max(1.0, finalDamage);
        
        // Debug logging
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("💫 PASSIVE NPC MAGIC DAMAGE: " + player.getName() + " -> " + name + 
                ", Raw: " + damage + ", After MR(" + stats.getMagicResist() + 
                "): " + finalDamage + ", Reduction: " + (magicResistReduction * 100) + "%");
        }
        
        // CRITICAL FIX: Update health value
        double newHealth = Math.max(0, currentHealth - finalDamage);
        
        // Store updated health in metadata
        npc.getEntity().setMetadata("current_health", new FixedMetadataValue(plugin, newHealth));
        
        // Update the nameplate
        NPCManager.getInstance().updateNameplate(npc, newHealth, stats.getMaxHealth());
        
        // Display damage indicators with magic appearance
        if (plugin.getDamageIndicatorManager() != null) {
            plugin.getDamageIndicatorManager().spawnDamageIndicator(
                npc.getEntity().getLocation().add(0, 1, 0),
                (int) finalDamage,
                true); // true for magic damage
        }
        
        // Play magic damage sound
        npc.getEntity().getWorld().playSound(
            npc.getEntity().getLocation(),
            Sound.ENTITY_PLAYER_HURT_ON_FIRE,
            0.8f, 1.2f
        );
        
        // Magic damage visual effects
        npc.getEntity().getWorld().spawnParticle(
            org.bukkit.Particle.WITCH,
            npc.getEntity().getLocation().add(0, 1, 0),
            15, 0.3, 0.5, 0.3, 0.05
        );
        
        // Check if NPC is now dead
        if (newHealth <= 0) {
            // Handle death
            plugin.getLogger().info("PassiveNPC " + name + " was killed by magic from " + player.getName());
            handleDeath(player);
            return;
        }
        
        // If not already fleeing, start fleeing
        if (!isRunning) {
            isRunning = true;
            
            // Run away from the player with magical fright message
            sendMessage(player, "Your magic frightens me! Stay away!");
            
            // Calculate direction away from player
            Vector direction = npc.getEntity().getLocation().subtract(player.getLocation()).toVector().normalize();
            
            // Run away for 5 seconds
            new BukkitRunnable() {
                int ticks = 0;
                final int maxTicks = 5 * 20; // 5 seconds
                
                @Override
                public void run() {
                    if (!npc.isSpawned() || ticks++ > maxTicks) {
                        isRunning = false;
                        this.cancel();
                        return;
                    }
                    
                    // Only update movement every few ticks to reduce jitter
                    if (ticks % 10 == 0) {
                        // Calculate new run away position
                        Location npcLoc = npc.getEntity().getLocation();
                        Location runTo = npcLoc.clone().add(direction.clone().multiply(10));
                        
                        // Use Citizens pathfinding with slightly faster speed due to magical fright
                        npc.getNavigator().getLocalParameters().speedModifier(1.6f); // Slightly faster than normal flee
                        npc.getNavigator().setTarget(runTo);
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        }
    }

    /**
     * Handle the death of this NPC
     * 
     * @param killer The entity that killed this NPC
     */
    private void handleDeath(Entity killer) {
        if (npc == null) return;
        
        // Play death effects
        if (npc.isSpawned()) {
            Location location = npc.getEntity().getLocation();
            
            // Play death sound
            location.getWorld().playSound(
                location,
                Sound.ENTITY_PLAYER_DEATH,
                1.0f, 1.0f
            );
            
            // Show death particles
            location.getWorld().spawnParticle(
                org.bukkit.Particle.CLOUD,
                location.add(0, 1, 0),
                20, 0.3, 0.5, 0.3, 0.05
            );
        }
        
        // If the killer is a player, give them rewards
        if (killer instanceof Player) {
            Player player = (Player) killer;
            
            // Give experience based on NPC level
            int expReward = stats.getExpReward();
            player.giveExp(expReward);
            
            // Show message
            player.sendMessage(ChatColor.RED + "You killed " + name + "!");
            player.sendMessage(ChatColor.GREEN + "+" + expReward + " exp");
            
            // Random gold drop between min and max values
            int goldAmount = stats.getMinGoldDrop();
            if (stats.getMaxGoldDrop() > stats.getMinGoldDrop()) {
                goldAmount += new Random().nextInt(stats.getMaxGoldDrop() - stats.getMinGoldDrop() + 1);
            }
            
            // Use the plugin's currency system if gold amount > 0
            if (goldAmount > 0) {
                // This would usually call your currency system
                player.sendMessage(ChatColor.GOLD + "+" + goldAmount + " gold");
            }
        }
        
        // Schedule respawn if needed (in a production plugin we would probably want to respawn the NPC)
        // For this example, we'll just remove it permanently
        remove();
    }
    
    /**
     * Called when a player gets close to this NPC
     * This simulates the "pushable" behavior without using the trait
     * 
     * @param player The player who is close
     */
    public void onPlayerNearby(Player player) {
        if (isRunning || !npc.isSpawned()) return;
        
        // Calculate direction away from player
        Vector direction = npc.getEntity().getLocation().subtract(player.getLocation()).toVector().normalize();
        
        // Step slightly aside
        Location npcLoc = npc.getEntity().getLocation();
        Location stepAside = npcLoc.clone().add(direction.clone().multiply(1.5));
        
        // Make the NPC step aside briefly
        npc.getNavigator().getLocalParameters().speedModifier(1.0f);
        npc.getNavigator().setTarget(stepAside);
    }
    
    /**
     * Set the dialogue for this NPC
     * 
     * @param dialogueId The dialogue ID
     */
    public void setDialogue(String dialogueId) {
        this.dialogueId = dialogueId;
        setupDefaultDialogue(dialogueId, "Hello there! Please don't hurt me.");
    }
}