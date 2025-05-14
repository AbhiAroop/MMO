package com.server.entities.npc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.server.Main;

import net.citizensnpcs.api.event.NPCDamageByEntityEvent;
import net.citizensnpcs.api.npc.NPC;

public class CombatHandler implements NPCInteractionHandler, Listener {
    private final double maxHealth;
    private final double damage;
    private final Map<UUID, Double> npcHealth = new HashMap<>();
    private final Map<UUID, BukkitTask> combatTasks = new HashMap<>();
    
    public CombatHandler(double health, double damage) {
        this.maxHealth = health;
        this.damage = damage;
        Main.getInstance().getServer().getPluginManager().registerEvents(this, Main.getInstance());
    }
    
    /**
     * Get the NPC health map
     * This allows external access to health values
     */
    public Map<UUID, Double> getNpcHealth() {
        return npcHealth;
    }
    
    @Override
    public void onInteract(Player player, NPC npc, boolean rightClick) {
        if (rightClick) {
            // On right-click, if not already in combat, initiate combat
            startCombatBehavior(npc, player);
        }
    }
    
    /**
     * Start the combat behavior for this NPC
     * 
     * @param npc The NPC
     * @param initialTarget The initial target player
     */
    public void startCombatBehavior(NPC npc, Player initialTarget) {
        if (!npc.isSpawned() || combatTasks.containsKey(npc.getUniqueId())) {
            return;
        }
        
        // Initialize health if not already set
        if (!npcHealth.containsKey(npc.getUniqueId())) {
            npcHealth.put(npc.getUniqueId(), maxHealth);
        }
        
        // Set level based on health and damage - higher stats = higher level
        int npcLevel = (int) Math.max(1, Math.min(50, (maxHealth / 50) + (damage / 5)));
        npc.getEntity().setMetadata("level", new FixedMetadataValue(Main.getInstance(), npcLevel));
        
        // Store original name BEFORE updating nameplate
        if (!npc.getEntity().hasMetadata("original_name")) {
            String originalName = npc.getName()
                .replaceAll("§[0-9a-fA-Fk-oK-OrR]", "") // Strip color codes
                .replaceAll("\\[Lv\\.[0-9]+\\]\\s*[❈✦❀✵☠]\\s*", "") // Strip level and symbol prefixes
                .replaceAll("❤\\s*[0-9]+\\.[0-9]+/[0-9]+\\.[0-9]+", "") // Strip health indicator
                .trim(); // Clean up any extra spaces
                
            npc.getEntity().setMetadata("original_name", new FixedMetadataValue(Main.getInstance(), originalName));
        }
        
        // IMPORTANT: Hide the default name
        npc.getEntity().setCustomNameVisible(false);
        npc.getEntity().setCustomName(null);
        npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, false);
        
        // Update nameplate with health using the custom hologram system
        NPCManager.getInstance().updateNameplate(npc, npcHealth.get(npc.getUniqueId()), maxHealth);
        
        // Mark that this NPC is in combat
        npc.getEntity().setMetadata("in_combat", new FixedMetadataValue(Main.getInstance(), true));
        
        // Create a combat AI task
        BukkitTask task = new BukkitRunnable() {
            private Player currentTarget = initialTarget;
            private int tickCounter = 0;
            private final double ATTACK_RANGE = 4.0; // Increased from 2.5 to 4.0
            private final int ATTACK_INTERVAL = 10; // Attack every 10 ticks (twice per second)
            private final int ANIMATION_COOLDOWN = ATTACK_INTERVAL - 1; // Animation cooldown to prevent spam
            private int animationCooldownTicks = 0;
            
            @Override
            public void run() {
                // Decrease animation cooldown if active
                if (animationCooldownTicks > 0) {
                    animationCooldownTicks--;
                }
                
                // If the NPC is no longer valid or is dead, cancel the task
                if (!npc.isSpawned() || npc.getEntity().isDead()) {
                    this.cancel();
                    stopCombatBehavior(npc.getUniqueId());
                    return;
                }
                
                // Check if the nameplate exists and is properly attached - keep this only for emergencies
                if (tickCounter % 20 == 0) { // Check only every second to reduce overhead
                    ArmorStand nameplate = NPCManager.getInstance().getNameplateStands().get(npc.getUniqueId());
                    if (nameplate == null || !nameplate.isValid() || nameplate.isDead() || 
                        !nameplate.isInsideVehicle() || nameplate.getVehicle() != npc.getEntity()) {
                        
                        // Refresh nameplate if it's missing or detached
                        String originalName = npc.getEntity().hasMetadata("original_name") ?
                            npc.getEntity().getMetadata("original_name").get(0).asString() : npc.getName();
                        double health = npcHealth.getOrDefault(npc.getUniqueId(), maxHealth);
                        
                        // Remove any existing nameplate first to avoid duplicates
                        NPCManager.getInstance().removeNameplate(npc.getUniqueId());
                        
                        // Create a new nameplate
                        NPCManager.getInstance().createHologramNameplate(npc, originalName, health, maxHealth);
                    }
                }
                
                // Find a target if we don't have one
                if (currentTarget == null || !currentTarget.isOnline() || 
                    currentTarget.getWorld() != npc.getEntity().getWorld() || 
                    currentTarget.getLocation().distance(npc.getEntity().getLocation()) > 15) {
                    
                    // Find closest player within 10 blocks
                    double closestDist = 10.0;
                    currentTarget = null;
                    
                    for (Entity entity : npc.getEntity().getNearbyEntities(10, 10, 10)) {
                        if (entity instanceof Player) {
                            Player player = (Player) entity;
                            
                            // Skip players in creative mode
                            if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                                continue;
                            }
                            
                            double dist = player.getLocation().distance(npc.getEntity().getLocation());
                            if (dist < closestDist) {
                                closestDist = dist;
                                currentTarget = player;
                            }
                        }
                    }
                    
                    // If we still don't have a target, just return
                    if (currentTarget == null) {
                        return;
                    }
                }
                
                // Calculate distance to target
                double distance = npc.getEntity().getLocation().distance(currentTarget.getLocation());
                
                // Face the target - this doesn't cause jitter
                Location npcLoc = npc.getEntity().getLocation();
                Vector direction = currentTarget.getLocation().subtract(npcLoc).toVector();
                npcLoc.setDirection(direction);
                
                // Only update head rotation (this is safe and won't cause jitter)
                npc.faceLocation(currentTarget.getLocation());
                
                // MOVEMENT LOGIC - CRITICAL CHANGE:
                // Only update movement every half second to reduce teleports/jitter
                if (tickCounter % 10 == 0) {
                    // Only move if not attacking and beyond attack range
                    if (distance > ATTACK_RANGE) {
                        // Set better movement parameters
                        npc.getNavigator().getLocalParameters()
                            .speedModifier(1.3f)         // Move slightly faster than normal
                            .distanceMargin(2.0)         // Get within 2 blocks
                            .stuckAction(null)           // Disable stuck action to prevent teleports
                            .stationaryTicks(100);       // More tolerant of being stationary
                            
                        // Use Citizens' built-in pathfinding - this is most stable for ArmorStand passengers
                        npc.getNavigator().setTarget(currentTarget, false);
                    } else {
                        // Within attack range, stop moving
                        if (npc.getNavigator().isNavigating()) {
                            npc.getNavigator().cancelNavigation();
                        }
                    }
                }
                
                // Check if we're close enough to attack (every ATTACK_INTERVAL ticks = twice per second)
                if (tickCounter % ATTACK_INTERVAL == 0) {
                    if (distance <= ATTACK_RANGE) { // Attack range (increased)
                        // Always play attack animation - animation cooldown is now matched to attack interval
                        playAttackAnimation(npc);
                        
                        // Reset animation cooldown
                        animationCooldownTicks = ANIMATION_COOLDOWN;
                        
                        // Deal damage to the player
                        currentTarget.damage(damage, npc.getEntity());
                        
                        // Play attack sound
                        currentTarget.getWorld().playSound(
                            npc.getEntity().getLocation(),
                            Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                            1.0f, 1.0f
                        );
                        
                        // Show particles
                        currentTarget.getWorld().spawnParticle(
                            org.bukkit.Particle.SWEEP_ATTACK,
                            currentTarget.getLocation().add(0, 1, 0),
                            1, 0, 0, 0, 0
                        );
                    }
                }
                
                tickCounter++;
            }
        }.runTaskTimer(Main.getInstance(), 0, 1); // Every tick
        
        // Store the task for later cancellation
        combatTasks.put(npc.getUniqueId(), task);
    }

    /**
     * Play attack animation for an NPC
     * 
     * @param npc The NPC to animate
     */
    private void playAttackAnimation(NPC npc) {
        if (!npc.isSpawned()) return;
        
        try {
            // Get the entity associated with this NPC
            Entity entity = npc.getEntity();
            
            // Set metadata to indicate we're playing an animation
            entity.setMetadata("playing_attack_anim", new FixedMetadataValue(Main.getInstance(), true));
            
            // Make the entity swing its arm if it's a HumanEntity
            if (entity instanceof org.bukkit.entity.HumanEntity) {
                ((org.bukkit.entity.HumanEntity) entity).swingMainHand();
            }
            
            // Play attack sound for better feedback even if animation fails
            entity.getWorld().playSound(
                entity.getLocation(),
                Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                0.6f,
                1.0f
            );
            
            // Remove the metadata after a short delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (entity.isValid() && !entity.isDead()) {
                        entity.removeMetadata("playing_attack_anim", Main.getInstance());
                    }
                }
            }.runTaskLater(Main.getInstance(), 10L); // 0.5 second animation
        } catch (Exception e) {
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().warning("Failed to play NPC attack animation: " + e.getMessage());
            }
        }
    }
        
    /**
     * Stop combat behavior for an NPC
     * 
     * @param npcId The NPC's UUID
     */
    public void stopCombatBehavior(UUID npcId) {
        BukkitTask task = combatTasks.remove(npcId);
        if (task != null) {
            task.cancel();
        }
        
        // Don't remove the NPC from the health map here - we need to keep track of health even when not in combat
    }
    
    /**
     * Handle NPC damage events
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onNPCDamage(NPCDamageByEntityEvent event) {
        NPC npc = event.getNPC();
        
        // Check if this is one of our combat NPCs
        if (npcHealth.containsKey(npc.getUniqueId())) {
            // Get current health
            double currentHealth = npcHealth.getOrDefault(npc.getUniqueId(), maxHealth);
            
            // Calculate damage (use a standard value for now)
            double damageAmount = 10.0;
            
            // If the attacker is a player, we can customize damage
            if (event.getDamager() instanceof Player) {
                Player player = (Player) event.getDamager();
                
                // You could calculate custom damage based on player stats here
                // For example, get the player's attack strength stat
                
                // Start combat if not already started
                startCombatBehavior(npc, player);
                
                // Play hit sound and effects
                player.getWorld().playSound(
                    npc.getEntity().getLocation(),
                    Sound.ENTITY_PLAYER_ATTACK_STRONG,
                    1.0f, 1.0f
                );
                
                // Apply knockback to the NPC entity
                if (npc.isSpawned()) {
                    Entity entity = npc.getEntity();
                    Vector knockbackDir = entity.getLocation().subtract(player.getLocation()).toVector().normalize();
                    knockbackDir.setY(0.3); // Add some upward force
                    
                    // Apply the knockback force
                    entity.setVelocity(knockbackDir.multiply(0.4));
                    
                    // Make the entity turn red (damage effect)
                    applyDamageEffect(entity);
                }
                
                // Display a single damage indicator per hit
                Location damageLocation = npc.getEntity().getLocation().add(0, 1.5, 0);
                showDamageIndicator(damageLocation, damageAmount, player);
            }
            
            // Apply damage to our NPC health
            currentHealth -= damageAmount;
            currentHealth = Math.max(0, currentHealth); // Ensure health doesn't go negative
            npcHealth.put(npc.getUniqueId(), currentHealth);
            
            // Check if NPC is dead
            if (currentHealth <= 0) {
                // Handle NPC death
                handleNPCDeath(npc, event.getDamager());
            } else {
                // Immediately update the nameplate with the new health value
                // We do it both immediately and with a delay to ensure it updates properly
                if (npc.isSpawned() && !npc.getEntity().isDead()) {
                    NPCManager.getInstance().updateNameplate(npc, currentHealth, maxHealth);
                }
                
                // Also update with a small delay for reliability
                final NPC finalNpc = npc;
                final double finalCurrentHealth = currentHealth;
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (finalNpc.isSpawned() && !finalNpc.getEntity().isDead()) {
                            // Update the nameplate display directly through NPCManager
                            NPCManager.getInstance().updateNameplate(finalNpc, finalCurrentHealth, maxHealth);
                            
                            // If the ArmorStand is missing for some reason, recreate it
                            if (!NPCManager.getInstance().getNameplateStands().containsKey(finalNpc.getUniqueId())) {
                                String originalName = finalNpc.getEntity().hasMetadata("original_name") ?
                                    finalNpc.getEntity().getMetadata("original_name").get(0).asString() : finalNpc.getName();
                                NPCManager.getInstance().createHologramNameplate(finalNpc, originalName, finalCurrentHealth, maxHealth);
                            }
                        }
                    }
                }.runTaskLater(Main.getInstance(), 1L);
                
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
            }
        }
    }
        
    /**
     * Update the NPC's nameplate to show health
     * This method should be the ONLY place that updates combat NPC names
     */
    public void updateNPCNameplate(NPC npc, double health) {
        if (!npc.isSpawned()) return;
        
        // Get the original name without formatting
        String originalName;
        if (npc.getEntity().hasMetadata("original_name")) {
            originalName = npc.getEntity().getMetadata("original_name").get(0).asString();
        } else {
            originalName = npc.getName()
                .replaceAll("§[0-9a-fA-Fk-oK-OrR]", "") // Strip color codes
                .replaceAll("\\[Lv\\.[0-9]+\\]\\s*[❈✦❀✵☠]\\s*", "") // Strip level and symbol prefixes
                .replaceAll("❤\\s*[0-9]+\\.[0-9]+/[0-9]+\\.[0-9]+", "") // Strip health indicator
                .trim(); // Clean up any extra spaces
                
            // Store it for future use
            npc.getEntity().setMetadata("original_name", new FixedMetadataValue(Main.getInstance(), originalName));
        }
        
        // CRITICAL - Ensure health is properly clamped between 0 and maxHealth
        health = Math.max(0, Math.min(maxHealth, health));
        
        // Use the NPCManager to update our custom hologram nameplate
        NPCManager.getInstance().updateNameplate(npc, health, maxHealth);
    }

    /**
     * Create a visual health bar for tooltip/chat display
     */
    private String createHealthBar(double health, double maxHealth) {
        // Calculate health percentage
        double percentage = Math.max(0, Math.min(1, health / maxHealth));
        
        // Create health bar segments
        int bars = 10;
        int filledBars = (int) Math.ceil(percentage * bars);
        
        StringBuilder healthBar = new StringBuilder();
        
        // Add filled bars
        healthBar.append(ChatColor.RED);
        for (int i = 0; i < filledBars; i++) {
            healthBar.append("■");
        }
        
        // Add empty bars
        healthBar.append(ChatColor.GRAY);
        for (int i = 0; i < bars - filledBars; i++) {
            healthBar.append("■");
        }
        
        return healthBar.toString();
    }
    
    /**
     * Handle NPC death
     */
    private void handleNPCDeath(NPC npc, Entity killer) {
        // Stop combat behavior
        UUID npcId = npc.getUniqueId();
        stopCombatBehavior(npcId);
        
        // Store the location and original name before removing the NPC
        Location respawnLocation = npc.isSpawned() ? npc.getEntity().getLocation().clone() : 
                                npc.getStoredLocation();
        
        // Get the clean original name (very important for consistency)
        String originalName;
        if (npc.isSpawned() && npc.getEntity().hasMetadata("original_name")) {
            originalName = npc.getEntity().getMetadata("original_name").get(0).asString();
        } else {
            originalName = npc.getName()
                .replaceAll("§[0-9a-fA-Fk-oK-OrR]", "") 
                .replaceAll("\\[Lv\\.[0-9]+\\]\\s*[❈✦❀✵☠]\\s*", "") 
                .replaceAll("❤\\s*[0-9]+\\.[0-9]+/[0-9]+\\.[0-9]+", "")
                .trim();
        }
        
        // Remove health tracking - we'll reset it on respawn
        npcHealth.remove(npcId);
        
        // Play death effects
        if (npc.isSpawned()) {
            Location location = npc.getEntity().getLocation().clone();
            
            // Play death sound
            location.getWorld().playSound(location, Sound.ENTITY_PLAYER_DEATH, 1.0f, 1.0f);
            
            // Play particles
            location.getWorld().spawnParticle(
                org.bukkit.Particle.CLOUD,
                location.add(0, 1, 0),
                15, 0.5, 0.5, 0.5, 0.1
            );
            
            // Add death particles for better visuals
            location.getWorld().spawnParticle(
                org.bukkit.Particle.SMOKE,
                location,
                20, 0.3, 0.5, 0.3, 0.05
            );
            
            // CRITICAL: Remove the nameplate BEFORE despawning the NPC
            // This ensures the ArmorStand doesn't get orphaned
            NPCManager.getInstance().removeNameplate(npcId);
            
            // Reward killer if it's a player
            if (killer instanceof Player) {
                Player player = (Player) killer;
                
                // Give XP or other rewards
                player.sendMessage(ChatColor.GREEN + "You defeated " + ChatColor.YELLOW + originalName + ChatColor.GREEN + "!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
                
                // Gold drops similar to custom mobs
                int minGold = 5;
                int maxGold = 20;
                int goldAmount = minGold + (int)(Math.random() * (maxGold - minGold));
                player.sendMessage(ChatColor.GOLD + "+" + goldAmount + " Gold");
            }
            
            // CRITICAL: Check and remove ANY remaining ArmorStand passengers before despawning
            if (npc.getEntity().getPassengers() != null && !npc.getEntity().getPassengers().isEmpty()) {
                for (Entity passenger : new ArrayList<>(npc.getEntity().getPassengers())) {
                    passenger.remove();
                }
                npc.getEntity().eject(); // Clear all passengers
            }
            
            // First despawn the NPC
            if (npc.isSpawned()) {
                npc.despawn();
            }
            
            // Save the final originalName for clean respawn
            final String finalOriginalName = originalName;
            
            // Respawn the NPC after a delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Respawn at the stored location
                    if (!npc.isSpawned()) {
                        npc.spawn(respawnLocation);
                    }
                    
                    // Reset health
                    npcHealth.put(npcId, maxHealth);
                    
                    // Make the NPC vulnerable again
                    if (npc.isSpawned()) {
                        npc.getEntity().setInvulnerable(false);
                        
                        // Hide default nameplate
                        npc.getEntity().setCustomNameVisible(false);
                        npc.getEntity().setCustomName(null);
                        npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, false);
                        
                        // Store the original name for consistency
                        npc.getEntity().setMetadata("original_name", new FixedMetadataValue(Main.getInstance(), finalOriginalName));
                        
                        // Set level based on health and damage - higher stats = higher level
                        int npcLevel = (int) Math.max(1, Math.min(50, (maxHealth / 50) + (damage / 5)));
                        npc.getEntity().setMetadata("level", new FixedMetadataValue(Main.getInstance(), npcLevel));
                        
                        // Wait a tick to ensure NPC is fully spawned before adding nameplate
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (npc.isSpawned()) {
                                    // Create a new nameplate
                                    NPCManager.getInstance().createHologramNameplate(npc, finalOriginalName, maxHealth, maxHealth);
                                }
                            }
                        }.runTaskLater(Main.getInstance(), 2L);
                    }
                }
            }.runTaskLater(Main.getInstance(), 100L); // 5-second respawn
        }
    }
    
    /**
     * Apply a damage visual effect to an entity
     */
    private void applyDamageEffect(Entity entity) {
        // Fix: Use CRIT particles without block data
        entity.getWorld().spawnParticle(
            org.bukkit.Particle.CRIT,
            entity.getLocation().add(0, 1, 0),
            20, 0.3, 0.5, 0.3, 0.1
        );
        
    }
    
    /**
     * Show a floating damage indicator
     */
    private void showDamageIndicator(Location location, double damage, Player viewer) {
        // Add random offset to prevent stacking multiple indicators in the same spot
        Location adjustedLoc = location.clone().add(
            Math.random() * 0.6 - 0.3,
            Math.random() * 0.4,
            Math.random() * 0.6 - 0.3
        );
        
        // Create a single indicator with animation instead of many small ones
        ArmorStand stand = location.getWorld().spawn(adjustedLoc, ArmorStand.class);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setMarker(true);
        stand.setCustomNameVisible(true);
        stand.setCustomName(ChatColor.RED + "-" + (int)damage + " ❤");
        
        // Animate the single indicator
        new BukkitRunnable() {
            private int ticks = 0;
            private final int maxTicks = 20; // 1 second
            
            @Override
            public void run() {
                if (ticks >= maxTicks || stand.isDead() || !stand.isValid()) {
                    stand.remove();
                    this.cancel();
                    return;
                }
                
                // Move upward slowly
                stand.teleport(stand.getLocation().add(0, 0.05, 0));
                
                // Fade out toward the end
                if (ticks > maxTicks * 0.7) {
                    // Make invisible for last few ticks
                    if (ticks > maxTicks * 0.9) {
                        stand.setCustomNameVisible(false);
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(Main.getInstance(), 0, 1);
    }
}