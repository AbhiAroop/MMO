package com.server.entities.npc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.stats.PlayerStats;

import net.citizensnpcs.api.event.NPCDamageByEntityEvent;
import net.citizensnpcs.api.npc.NPC;

public class CombatHandler implements NPCInteractionHandler, Listener {
    private final Map<UUID, Double> npcHealth = new HashMap<>();
    private final Map<UUID, BukkitTask> combatTasks = new HashMap<>();
    private final Map<UUID, NPCStats> npcStats = new HashMap<>();
    private final NPCStats defaultStats;
    
    public CombatHandler(double health, double damage) {
        // Create default stats for backward compatibility
        NPCStats stats = new NPCStats();
        stats.setMaxHealth(health);
        stats.setPhysicalDamage((int)damage);
        this.defaultStats = stats;
        
        Main.getInstance().getServer().getPluginManager().registerEvents(this, Main.getInstance());
    }
    
    /**
     * Get the NPC health map
     * This allows external access to health values
     */
    public Map<UUID, Double> getNpcHealth() {
        return npcHealth;
    }
    
    /**
     * Set custom stats for an NPC
     * 
     * @param npcId The UUID of the NPC
     * @param stats The stats to set
     */
    public void setNPCStats(UUID npcId, NPCStats stats) {
        npcStats.put(npcId, stats);
        
        // Reset health to max when setting new stats
        npcHealth.put(npcId, stats.getMaxHealth());
    }
    
    /**
     * Get stats for an NPC, falling back to defaults if none are set
     * 
     * @param npcId The UUID of the NPC
     * @return The NPC's stats
     */
    public NPCStats getNPCStats(UUID npcId) {
        return npcStats.getOrDefault(npcId, defaultStats);
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
        
        // Get this NPC's stats
        NPCStats stats = getNPCStats(npc.getUniqueId());
        
        // Initialize health if not already set
        if (!npcHealth.containsKey(npc.getUniqueId())) {
            npcHealth.put(npc.getUniqueId(), stats.getMaxHealth());
        }
        
        // Set level based on stats - this is now stored in the NPCStats object
        npc.getEntity().setMetadata("level", new FixedMetadataValue(Main.getInstance(), stats.getLevel()));
        
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
        
        // Store the NPC type as metadata
        npc.getEntity().setMetadata("npc_type", new FixedMetadataValue(Main.getInstance(), 
                stats.getNpcType().name()));
        
        // Update nameplate with health using the custom hologram system
        NPCManager.getInstance().updateNameplate(npc, npcHealth.get(npc.getUniqueId()), stats.getMaxHealth());
        
        // Mark that this NPC is in combat
        npc.getEntity().setMetadata("in_combat", new FixedMetadataValue(Main.getInstance(), true));
        
        // Override attackInterval to ensure attacks are twice per second (regardless of stats)
        final int attackInterval = 10; // 10 ticks = twice per second
        final double attackRange = stats.getAttackRange();
        
        // Create a combat AI task
        BukkitTask task = new BukkitRunnable() {
            private Player currentTarget = initialTarget;
            private int tickCounter = 0;
            private final int ATTACK_INTERVAL = attackInterval;
            private final double ATTACK_RANGE = attackRange;
            private final int ANIMATION_COOLDOWN = attackInterval - 1;
            private int animationCooldownTicks = 0;
            private float attackCharge = 0.0f;
            private boolean isCharging = true; // Track if we're in charging phase
            private int attackCooldown = 0;    // Track cooldown between attacks
            
            @Override
            public void run() {
                // Decrease animation cooldown if active
                if (animationCooldownTicks > 0) {
                    animationCooldownTicks--;
                }
                
                // Handle attack cooldown
                if (attackCooldown > 0) {
                    attackCooldown--;
                }
                
                // If we're charging up an attack, increase the charge
                if (isCharging && attackCooldown <= 0) {
                    attackCharge = Math.min(1.0f, attackCharge + 0.05f);
                }
                
                // If the NPC is no longer valid or is dead, cancel the task
                if (!npc.isSpawned() || npc.getEntity().isDead()) {
                    this.cancel();
                    stopCombatBehavior(npc.getUniqueId());
                    return;
                }
                
                // Check if the nameplate exists and is properly attached - once per second
                if (tickCounter % 20 == 0) {
                    ArmorStand nameplate = NPCManager.getInstance().getNameplateStands().get(npc.getUniqueId());
                    if (nameplate == null || !nameplate.isValid() || nameplate.isDead() || 
                        !nameplate.isInsideVehicle() || nameplate.getVehicle() != npc.getEntity()) {
                        
                        // Refresh nameplate if it's missing or detached
                        String originalName = npc.getEntity().hasMetadata("original_name") ?
                            npc.getEntity().getMetadata("original_name").get(0).asString() : npc.getName();
                        double health = npcHealth.getOrDefault(npc.getUniqueId(), stats.getMaxHealth());
                        
                        NPCManager.getInstance().removeNameplate(npc.getUniqueId());
                        NPCManager.getInstance().createHologramNameplate(npc, originalName, health, stats.getMaxHealth());
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
                
                // MOVEMENT LOGIC:
                // Only update movement every half second to reduce teleports/jitter
                if (tickCounter % 10 == 0) {
                    // Only move if not attacking and beyond attack range
                    if (distance > ATTACK_RANGE) {
                        // Set better movement parameters
                        npc.getNavigator().getLocalParameters()
                            .speedModifier(1.3f)
                            .distanceMargin(2.0)
                            .stuckAction(null)
                            .stationaryTicks(100);
                            
                        // Use Citizens' built-in pathfinding - this is most stable for ArmorStand passengers
                        npc.getNavigator().setTarget(currentTarget, false);
                    } else {
                        // Within attack range, stop moving
                        if (npc.getNavigator().isNavigating()) {
                            npc.getNavigator().cancelNavigation();
                        }
                    }
                }
                
                // Check if we can attack - must be in range, off cooldown, and fully charged
                if (distance <= ATTACK_RANGE && attackCooldown <= 0 && 
                    ((isCharging && attackCharge >= 0.9f) || (!isCharging && tickCounter % ATTACK_INTERVAL == 0))) {
                    
                    // Store the current charge value for damage calculation
                    float finalCharge = attackCharge;
                    
                    // Execute the attack
                    playAttackAnimation(npc);
                    
                    // Reset animation cooldown
                    animationCooldownTicks = ANIMATION_COOLDOWN;
                    
                    // Deal damage based on NPC stats and charge
                    dealDamageToPlayer(npc, currentTarget, finalCharge);
                    
                    // Reset charge and set cooldown
                    attackCharge = 0.0f;
                    attackCooldown = ATTACK_INTERVAL;
                    
                    // Play attack sound
                    Sound attackSound;
                    float volume, pitch;
                    
                    if (finalCharge >= 0.9f) {
                        // Full charge attack sound
                        attackSound = Sound.ENTITY_PLAYER_ATTACK_STRONG;
                        volume = 1.0f;
                        pitch = 1.0f;
                    } else if (finalCharge >= 0.5f) {
                        // Medium charge attack sound
                        attackSound = Sound.ENTITY_PLAYER_ATTACK_SWEEP;
                        volume = 0.8f;
                        pitch = 1.0f;
                    } else {
                        // Low charge attack sound
                        attackSound = Sound.ENTITY_PLAYER_ATTACK_WEAK;
                        volume = 0.5f;
                        pitch = 1.0f;
                    }
                    
                    currentTarget.getWorld().playSound(
                        npc.getEntity().getLocation(),
                        attackSound,
                        volume, pitch
                    );
                    
                    // Show particles
                    if (finalCharge >= 0.9f) {
                        // More particles for full charge
                        currentTarget.getWorld().spawnParticle(
                            org.bukkit.Particle.SWEEP_ATTACK,
                            currentTarget.getLocation().add(0, 1, 0),
                            3, 0.3, 0.5, 0.3, 0.1
                        );
                    } else {
                        // Less particles for weak charge
                        currentTarget.getWorld().spawnParticle(
                            org.bukkit.Particle.SWEEP_ATTACK,
                            currentTarget.getLocation().add(0, 1, 0),
                            1, 0, 0, 0, 0
                        );
                    }
                    
                    // Debug message
                    if (Main.getInstance().isDebugMode()) {
                        Main.getInstance().getLogger().info("NPC attack with charge: " + finalCharge);
                    }
                }
                
                tickCounter++;
            }
        }.runTaskTimer(Main.getInstance(), 0, 1); // Every tick
        
        // Store the task for later cancellation
        combatTasks.put(npc.getUniqueId(), task);
    }

    /**
     * Deal damage to a player based on the NPC's stats and attack charge
     * 
     * @param npc The NPC dealing damage
     * @param player The player target
     * @param chargePercent The attack charge percentage (0.0-1.0)
     */
    private void dealDamageToPlayer(NPC npc, Player player, float chargePercent) {
        NPCStats stats = getNPCStats(npc.getUniqueId());
        
        // Apply physical damage
        if (stats.getPhysicalDamage() > 0) {
            // Base damage from stats
            double baseDamage = stats.getPhysicalDamage();
            
            // Scale damage based on charge percentage, similar to player attacks
            double scaledDamage;
            if (chargePercent <= 0.01) {
                // At 0-1% charge, minimum damage is 0.5
                scaledDamage = 0.5;
            } else {
                // For charges above 1%, scale linearly but ensure minimum 0.5 damage
                scaledDamage = Math.max(0.5, baseDamage * chargePercent);
            }
            
            // Apply critical hit at full charge (10% chance)
            boolean isCritical = chargePercent >= 0.9f && Math.random() < 0.1;
            if (isCritical) {
                scaledDamage *= 1.5; // 50% more damage for critical hits
                
                // Show critical hit particles
                player.getWorld().spawnParticle(
                    org.bukkit.Particle.CRIT,
                    player.getLocation().add(0, 1, 0),
                    10, 0.3, 0.5, 0.3, 0.1
                );
            }
            
            try {
                // Get player stats if available
                ProfileManager profileManager = Main.getInstance().getProfileManager();
                if (profileManager != null) {
                    // Check if player has an active profile
                    Integer activeSlot = profileManager.getActiveProfile(player.getUniqueId());
                    if (activeSlot != null) {
                        PlayerProfile profile = profileManager.getProfiles(player.getUniqueId())[activeSlot];
                        if (profile != null) {
                            PlayerStats playerStats = profile.getStats();
                            
                            // Apply armor reduction
                            double armorReduction = playerStats.getPhysicalDamageReduction() / 100.0;
                            scaledDamage = scaledDamage * (1.0 - armorReduction);
                        }
                    }
                }
            } catch (Exception e) {
                // Fallback to direct damage if stats aren't available
                if (Main.getInstance().isDebugMode()) {
                    Main.getInstance().getLogger().warning("Failed to apply armor reduction: " + e.getMessage());
                }
            }
            
            // Apply the damage
            double finalDamage = Math.max(1, scaledDamage);
            player.damage(finalDamage, npc.getEntity());
            
            // Debug information if needed
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().info("NPC attack: Charge=" + String.format("%.2f", chargePercent) + 
                                            ", Base Damage=" + String.format("%.2f", baseDamage) + 
                                            ", Scaled Damage=" + String.format("%.2f", finalDamage) +
                                            ", Critical=" + isCritical);
            }
        }
        
        // Apply magic damage if any - apply full damage regardless of charge
        if (stats.getMagicDamage() > 0) {
            try {
                // Get player stats if available
                ProfileManager profileManager = Main.getInstance().getProfileManager();
                if (profileManager != null) {
                    // Check if player has an active profile
                    Integer activeSlot = profileManager.getActiveProfile(player.getUniqueId());
                    if (activeSlot != null) {
                        PlayerProfile profile = profileManager.getProfiles(player.getUniqueId())[activeSlot];
                        if (profile != null) {
                            PlayerStats playerStats = profile.getStats();
                            
                            // Calculate final damage by applying player's magic resist
                            double magicDamage = stats.getMagicDamage();
                            double magicResistReduction = playerStats.getMagicDamageReduction() / 100.0;
                            magicDamage = magicDamage * (1.0 - magicResistReduction);
                            
                            // Apply the damage directly to player's health
                            double newHealth = Math.max(1, player.getHealth() - magicDamage);
                            player.setHealth(newHealth);
                            
                            // Show magic damage particles
                            player.getWorld().spawnParticle(
                                org.bukkit.Particle.WITCH,
                                player.getLocation().add(0, 1, 0),
                                10, 0.3, 0.5, 0.3, 0
                            );
                        }
                    }
                }
            } catch (Exception e) {
                // If magic damage system fails, just log it
                if (Main.getInstance().isDebugMode()) {
                    Main.getInstance().getLogger().warning("Failed to apply magic damage: " + e.getMessage());
                }
            }
        }
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
            }.runTaskLater(Main.getInstance(), 10L);
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
    }
    
    /**
     * Handle NPC damage events
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onNPCDamage(NPCDamageByEntityEvent event) {
        NPC npc = event.getNPC();
        
        // Check if this is one of our combat NPCs
        if (npcHealth.containsKey(npc.getUniqueId())) {
            // Important: We let the vanilla event go through, but we handle the damage calculation ourselves
            // Cancel the event to prevent default damage, we'll apply it manually
            event.setCancelled(true);
            
            NPCStats stats = getNPCStats(npc.getUniqueId());
            
            // Get current health
            double currentHealth = npcHealth.getOrDefault(npc.getUniqueId(), stats.getMaxHealth());
            
            // Calculate base damage
            double damageAmount = 10.0;
            boolean isCritical = false;
            
            // If the attacker is a player, we can customize damage based on player stats
            if (event.getDamager() instanceof Player) {
                Player player = (Player) event.getDamager();
                
                // Skip players in creative mode - they shouldn't be able to damage NPCs
                if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                    return;
                }
                
                // Get the player's attack charge progression
                float chargePercent = player.getAttackCooldown();
                
                // Determine if attack is fully charged
                boolean isFullyCharged = chargePercent >= 0.9f;
                
                // Check for critical hit - only apply if attack is fully charged and player is falling
                isCritical = isFullyCharged && !player.isOnGround() && player.getFallDistance() > 0.0f;
                
                try {
                    // Get player stats if available
                    ProfileManager profileManager = Main.getInstance().getProfileManager();
                    if (profileManager != null) {
                        // Check if player has an active profile
                        Integer activeSlot = profileManager.getActiveProfile(player.getUniqueId());
                        if (activeSlot != null) {
                            PlayerProfile profile = profileManager.getProfiles(player.getUniqueId())[activeSlot];
                            if (profile != null) {
                                PlayerStats playerStats = profile.getStats();
                                
                                // Use player's physical damage as base damage
                                damageAmount = playerStats.getPhysicalDamage();

                                // Check if the player is holding a custom weapon
                                ItemStack heldItem = player.getInventory().getItemInMainHand();
                                if (isCustomWeapon(heldItem)) {
                                    // Apply any special weapon modifiers
                                    damageAmount = applyCustomWeaponModifiers(damageAmount, heldItem, player);
                                    
                                    // Apply reasonable scaling for custom weapons
                                    int customModelData = heldItem.getItemMeta().getCustomModelData();
                                    double damageScaling = getWeaponTierScaling(customModelData);
                                    
                                    // Apply scaling
                                    damageAmount *= damageScaling;
                                    
                                    if (Main.getInstance().isDebugMode()) {
                                        Main.getInstance().getLogger().info("Applied weapon damage scaling: " + 
                                            damageScaling + " for weapon tier: " + (customModelData / 10000));
                                    }
                                } else {
                                    // For vanilla weapons, apply scaling based on material
                                    Material material = heldItem.getType();
                                    double vanillaWeaponScaling = getVanillaWeaponScaling(material);
                                    
                                    // Apply vanilla weapon scaling
                                    damageAmount *= vanillaWeaponScaling;
                                    
                                    if (Main.getInstance().isDebugMode()) {
                                        Main.getInstance().getLogger().info("Applied vanilla weapon scaling: " + 
                                            vanillaWeaponScaling + " for material: " + material.name());
                                    }
                                }
                                
                                // CHARGE-BASED DAMAGE CALCULATION:
                                // Exactly like the player combat system
                                if (chargePercent <= 0.01) {
                                    // At 0-1% charge, minimum damage is 0.5
                                    damageAmount = 0.5;
                                } else {
                                    // For charges above 1%, scale linearly but ensure minimum 0.5 damage
                                    damageAmount = Math.max(0.5, damageAmount * chargePercent);
                                }
                                
                                // Apply critical hits - either from jump attack or random chance
                                if (isCritical || Math.random() < playerStats.getCriticalChance()) {
                                    damageAmount *= playerStats.getCriticalDamage();
                                    isCritical = true;
                                    
                                    // Show critical hit particles
                                    npc.getEntity().getWorld().spawnParticle(
                                        org.bukkit.Particle.CRIT,
                                        npc.getEntity().getLocation().add(0, 1, 0),
                                        10, 0.3, 0.5, 0.3, 0.1
                                    );
                                }
                                
                                // Check for burst damage (rare high damage)
                                if (Math.random() < playerStats.getBurstChance()) {
                                    damageAmount *= playerStats.getBurstDamage();
                                    
                                    // Show burst damage particles
                                    npc.getEntity().getWorld().spawnParticle(
                                        org.bukkit.Particle.EXPLOSION,
                                        npc.getEntity().getLocation().add(0, 1, 0),
                                        5, 0.2, 0.2, 0.2, 0.05
                                    );
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Fallback to default damage if stats aren't available
                    if (Main.getInstance().isDebugMode()) {
                        Main.getInstance().getLogger().warning("Failed to calculate player damage: " + e.getMessage());
                    }
                }
                
                // Calculate damage reduction from armor
                double armorReduction = stats.getArmorDamageReduction();
                
                // Apply armor damage reduction
                damageAmount = damageAmount * (1.0 - (armorReduction / 100.0));
                
                // Round to nearest whole number for cleaner display
                damageAmount = Math.max(1, Math.round(damageAmount));
                
                // Start combat if not already started
                startCombatBehavior(npc, player);
                
                // Play appropriate hit sound based on charge level
                playHitSound(player, npc.getEntity().getLocation(), chargePercent, isFullyCharged);
                
                // Apply knockback to the NPC entity - stronger if fully charged
                if (npc.isSpawned()) {
                    applyAttackKnockback(npc.getEntity(), player, isFullyCharged);
                    applyDamageEffect(npc.getEntity());
                }
                
                // Display a single damage indicator per hit
                Location damageLocation = npc.getEntity().getLocation().add(0, 1.5, 0);
                showDamageIndicator(damageLocation, damageAmount, player);
                
                // Debug information
                if (Main.getInstance().isDebugMode()) {
                    Main.getInstance().getLogger().info("Player attack to NPC: Charge=" + String.format("%.2f", chargePercent) + 
                                            ", Damage=" + String.format("%.2f", damageAmount) +
                                            ", Critical=" + isCritical);
                }
                
                // Get the player's held item
                ItemStack heldItem = player.getInventory().getItemInMainHand();
                
                // Apply life steal if player has it or is using a lifesteal weapon
                applyLifestealIfApplicable(player, damageAmount, heldItem);
            } else if (event.getDamager() instanceof org.bukkit.entity.Projectile) {
                // Handle damage from projectiles (arrows, etc.)
                org.bukkit.entity.Projectile projectile = (org.bukkit.entity.Projectile) event.getDamager();
                if (projectile.getShooter() instanceof Player) {
                    Player shooter = (Player) projectile.getShooter();
                    
                    // Skip players in creative mode
                    if (shooter.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                        return;
                    }
                    
                    handleProjectileDamage(npc, projectile, shooter, damageAmount);
                    return; // Already handled damage in the method
                }
            } else if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.CUSTOM) {
                // This may be damage from a custom ability - don't modify it further
                damageAmount = event.getDamage();
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
                // Update the nameplate with the new health value
                if (npc.isSpawned() && !npc.getEntity().isDead()) {
                    NPCManager.getInstance().updateNameplate(npc, currentHealth, stats.getMaxHealth());
                }
                
                // Also update with a small delay for reliability
                scheduleNameplateUpdate(npc, currentHealth, stats.getMaxHealth());
                
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
     * Determine scaling factor based on weapon tier
     */
    private double getWeaponTierScaling(int customModelData) {
        return 1.0;
    }

    /**
     * Determine scaling for vanilla weapons
     */
    private double getVanillaWeaponScaling(Material material) {
        return 1.0;
    }

    /**
     * Apply knockback to an entity from a player's attack
     */
    private void applyAttackKnockback(Entity entity, Player player, boolean isFullyCharged) {
        Vector knockbackDir = entity.getLocation().subtract(player.getLocation()).toVector().normalize();
        knockbackDir.setY(0.3); // Add some upward force
        
        // Apply stronger knockback for charged attacks
        double knockbackMultiplier = isFullyCharged ? 0.5 : 0.3;
        entity.setVelocity(knockbackDir.multiply(knockbackMultiplier));
    }

    /**
     * Play appropriate hit sound based on charge level
     */
    private void playHitSound(Player player, Location location, float chargePercent, boolean isFullyCharged) {
        Sound sound;
        float volume, pitch;
        
        if (isFullyCharged) {
            // Full charge attack sound
            sound = Sound.ENTITY_PLAYER_ATTACK_STRONG;
            volume = 1.0f;
            pitch = 1.0f;
        } else if (chargePercent >= 0.5f) {
            // Medium charge attack sound
            sound = Sound.ENTITY_PLAYER_ATTACK_SWEEP;
            volume = 0.8f;
            pitch = 1.0f;
        } else {
            // Low charge attack sound
            sound = Sound.ENTITY_PLAYER_ATTACK_WEAK;
            volume = 0.5f;
            pitch = 1.0f;
        }
        
        player.getWorld().playSound(location, sound, volume, pitch);
    }

    /**
     * Schedule a nameplate update with a delay
     */
    private void scheduleNameplateUpdate(NPC npc, double currentHealth, double maxHealth) {
        final NPC finalNpc = npc;
        final double finalCurrentHealth = currentHealth;
        final double finalMaxHealth = maxHealth;
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (finalNpc.isSpawned() && !finalNpc.getEntity().isDead()) {
                    NPCManager.getInstance().updateNameplate(finalNpc, finalCurrentHealth, finalMaxHealth);
                    
                    if (!NPCManager.getInstance().getNameplateStands().containsKey(finalNpc.getUniqueId())) {
                        String originalName = finalNpc.getEntity().hasMetadata("original_name") ?
                            finalNpc.getEntity().getMetadata("original_name").get(0).asString() : finalNpc.getName();
                        NPCManager.getInstance().createHologramNameplate(finalNpc, originalName, finalCurrentHealth, finalMaxHealth);
                    }
                }
            }
        }.runTaskLater(Main.getInstance(), 1L);
    }

    /**
     * Apply life steal effect to the player if they have the stat or are using a lifesteal weapon
     */
    private void applyLifestealIfApplicable(Player player, double damageAmount, ItemStack heldItem) {
        try {
            ProfileManager profileManager = Main.getInstance().getProfileManager();
            if (profileManager != null) {
                Integer activeSlot = profileManager.getActiveProfile(player.getUniqueId());
                if (activeSlot != null) {
                    PlayerProfile profile = profileManager.getProfiles(player.getUniqueId())[activeSlot];
                    if (profile != null) {
                        PlayerStats playerStats = profile.getStats();
                        
                        // Get base lifesteal from stats
                        double lifestealPercent = playerStats.getLifeSteal();
                        
                        // Add additional lifesteal from weapon if it's a Siphon Fang
                        if (heldItem != null && heldItem.hasItemMeta() && heldItem.getItemMeta().hasCustomModelData()) {
                            int customModelData = heldItem.getItemMeta().getCustomModelData();
                            
                            // Siphon Fang has custom model data 210002
                            if (customModelData == 210002) {
                                lifestealPercent += 3.0; // Additional 3% lifesteal from Siphon Fang
                            }
                        }
                        
                        // Apply lifesteal if there's any
                        if (lifestealPercent > 0) {
                            double healAmount = damageAmount * (lifestealPercent / 100.0);
                            double newHealth = Math.min(player.getMaxHealth(), player.getHealth() + healAmount);
                            player.setHealth(newHealth);
                            
                            // Show life steal particles
                            player.getWorld().spawnParticle(
                                org.bukkit.Particle.HEART,
                                player.getLocation().add(0, 1.2, 0),
                                3, 0.2, 0.2, 0.2, 0
                            );
                            
                            // Show healing message for significant healing
                            if (healAmount >= 1.0) {
                                player.sendMessage(ChatColor.GREEN + "⚕ " + ChatColor.GRAY + 
                                                "Lifesteal healed you for " + ChatColor.GREEN + 
                                                String.format("%.1f", healAmount) + ChatColor.GRAY + " health");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Just log if lifesteal fails
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().warning("Failed to apply life steal: " + e.getMessage());
            }
        }
    }

    /**
     * Check if the item is a custom weapon
     */
    private boolean isCustomWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        
        return meta.hasCustomModelData() && 
            (meta.getCustomModelData() >= 210000 && meta.getCustomModelData() < 260000);
    }

    /**
     * Apply damage modifiers based on the custom weapon being used
     */
    private double applyCustomWeaponModifiers(double baseDamage, ItemStack item, Player player) {
        if (item == null || !item.hasItemMeta()) return baseDamage;
        ItemMeta meta = item.getItemMeta();
        
        if (!meta.hasCustomModelData()) return baseDamage;
        
        int customModelData = meta.getCustomModelData();
                
        return baseDamage;
    }

    /**
     * Handle damage from projectiles 
     */
    private void handleProjectileDamage(NPC npc, org.bukkit.entity.Projectile projectile, Player shooter, double baseDamage) {
        // Apply damage based on projectile type and shooter's stats
        try {
            ProfileManager profileManager = Main.getInstance().getProfileManager();
            if (profileManager == null) return;
            
            Integer activeSlot = profileManager.getActiveProfile(shooter.getUniqueId());
            if (activeSlot == null) return;
            
            PlayerProfile profile = profileManager.getProfiles(shooter.getUniqueId())[activeSlot];
            if (profile == null) return;
            
            PlayerStats playerStats = profile.getStats();
            
            // Get base damage from player's ranged damage stat
            double damageAmount = playerStats.getPhysicalDamage();
            boolean isCritical = false;
            
            // Apply special projectile effects based on type
            if (projectile instanceof org.bukkit.entity.Arrow) {
                org.bukkit.entity.Arrow arrow = (org.bukkit.entity.Arrow) projectile;
                
                // Apply critical damage if it's a critical arrow
                if (arrow.isCritical()) {
                    damageAmount *= playerStats.getCriticalDamage();
                    isCritical = true;
                }
                
                // Check if this is a special arrow from an ability
                if (projectile.hasMetadata("ability_damage")) {
                    damageAmount = projectile.getMetadata("ability_damage").get(0).asDouble();
                    
                    // IMPORTANT: Scale down ability arrow damage to prevent one-shots
                    damageAmount *= 0.3; // 30% of original damage for ability arrows
                    
                    if (Main.getInstance().isDebugMode()) {
                        Main.getInstance().getLogger().info("Applied ability arrow damage scaling: 0.3, now: " + damageAmount);
                    }
                }
                
                // Check if this arrow was fired from a custom bow
                if (projectile.hasMetadata("bow_tier")) {
                    int bowTier = projectile.getMetadata("bow_tier").get(0).asInt();
                    double damageScaling = 1.0;
                    
                    
                    // Apply scaling
                    damageAmount *= damageScaling;
                    
                    if (Main.getInstance().isDebugMode()) {
                        Main.getInstance().getLogger().info("Applied bow damage scaling: " + 
                            damageScaling + " for bow tier: " + bowTier);
                    }
                }
            }
            
            // Set the damage amount after calculations
            double finalDamage = damageAmount;
            
            // Apply armor reduction from NPC
            NPCStats stats = getNPCStats(npc.getUniqueId());
            double armorReduction = stats.getArmorDamageReduction();
            finalDamage = finalDamage * (1.0 - (armorReduction / 100.0));
            
            // Round to nearest whole number
            finalDamage = Math.max(1, Math.round(finalDamage));
            
            // Get current health
            double currentHealth = npcHealth.getOrDefault(npc.getUniqueId(), stats.getMaxHealth());
            
            // Apply damage
            currentHealth -= finalDamage;
            currentHealth = Math.max(0, currentHealth);
            npcHealth.put(npc.getUniqueId(), currentHealth);
            
            // Show damage indicator
            Location damageLocation = npc.getEntity().getLocation().add(0, 1.5, 0);
            showDamageIndicator(damageLocation, finalDamage, shooter);
            
            // Start combat behavior
            startCombatBehavior(npc, shooter);
            
            // Show critical particles if needed
            if (isCritical) {
                npc.getEntity().getWorld().spawnParticle(
                    org.bukkit.Particle.CRIT,
                    npc.getEntity().getLocation().add(0, 1, 0),
                    10, 0.3, 0.5, 0.3, 0.1
                );
            }
            
            // Check if NPC is dead
            if (currentHealth <= 0) {
                // Handle NPC death
                handleNPCDeath(npc, shooter);
            } else {
                // Update the nameplate with the new health value
                if (npc.isSpawned() && !npc.getEntity().isDead()) {
                    NPCManager.getInstance().updateNameplate(npc, currentHealth, stats.getMaxHealth());
                }
                
                // Play hit sound
                npc.getEntity().getWorld().playSound(
                    npc.getEntity().getLocation(),
                    Sound.ENTITY_ARROW_HIT_PLAYER,
                    1.0f, 1.0f
                );
            }
        } catch (Exception e) {
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().warning("Failed to process projectile damage: " + e.getMessage());
            }
        }
    }

    /**
     * Handle direct ability damage from an ability identifier
     * Called externally from AbilityManager
     */
    public void handleAbilityDamage(NPC npc, Player caster, String abilityId, double damage) {
        if (!npcHealth.containsKey(npc.getUniqueId())) return;
        
        // Add a cooldown tracking system to prevent duplicate damage from rapid ticks
        UUID npcUUID = npc.getUniqueId();
        String uniqueAbilityHitId = abilityId + "-" + caster.getUniqueId().toString();
        
        // Track last hit time for this ability on this NPC
        Map<String, Long> abilityCooldowns = new HashMap<>();
        long currentTime = System.currentTimeMillis();
        long lastHitTime = 0;
        
        // Check if we have a record for this NPC
        if (abilityCooldownMap.containsKey(npcUUID)) {
            abilityCooldowns = abilityCooldownMap.get(npcUUID);
            
            // Check if this ability was used recently
            if (abilityCooldowns.containsKey(uniqueAbilityHitId)) {
                lastHitTime = abilityCooldowns.get(uniqueAbilityHitId);
                
                // If the ability was used too recently (within 250ms), skip to prevent multi-hit from same ability tick
                if (currentTime - lastHitTime < 250) {
                    if (Main.getInstance().isDebugMode()) {
                        Main.getInstance().getLogger().info("Prevented duplicate tick damage for " + abilityId + 
                            " (hit again within " + (currentTime - lastHitTime) + "ms)");
                    }
                    return;
                }
            }
        } else {
            abilityCooldownMap.put(npcUUID, abilityCooldowns);
        }
        
        // Record this hit time
        abilityCooldowns.put(uniqueAbilityHitId, currentTime);
        
        NPCStats stats = getNPCStats(npc.getUniqueId());
        double currentHealth = npcHealth.getOrDefault(npc.getUniqueId(), stats.getMaxHealth());
        
        // Apply damage reduction based on ability type - THIS IS THE ONLY REDUCTION THAT SHOULD OCCUR
        double damageReduction = 0;
        
        // Apply appropriate damage reductions based on ability type
        if (abilityId.equals("fire_beam") || abilityId.equals("blood_harvest")) {
            // Magic damage abilities - use magic resist
            damageReduction = stats.getMagicDamageReduction() / 100.0;
            
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().info("Applied Magic Resist reduction: " + 
                    (stats.getMagicDamageReduction()) + "% to " + abilityId);
            }
        } else if (abilityId.equals("lightning_throw") || 
                abilityId.equals("arcloom_ability") || 
                abilityId.equals("fleshrake_ability")) {
            // Physical damage ability - use armor
            damageReduction = stats.getArmorDamageReduction() / 100.0;
            
            if (Main.getInstance().isDebugMode()) {
                Main.getInstance().getLogger().info("Applied Armor reduction: " + 
                    (stats.getArmorDamageReduction()) + "% to " + abilityId);
            }
        }
        
        // Apply damage reduction - THIS IS THE ONLY DAMAGE ADJUSTMENT THAT SHOULD HAPPEN
        double finalDamage = damage * (1.0 - damageReduction);
        
        // No arbitrary damage caps - just ensure minimum damage of 1
        finalDamage = Math.max(1, finalDamage);
        
        // Apply the damage to our custom NPC health system
        currentHealth -= finalDamage;
        currentHealth = Math.max(0, currentHealth); // Ensure health doesn't go negative
        npcHealth.put(npc.getUniqueId(), currentHealth);
        
        // Debug logging
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("Ability damage: " + abilityId + 
                " dealt " + finalDamage + " to NPC " + npc.getName() + 
                " (Original damage: " + damage + ", Reduction: " + (damageReduction * 100) + "%)");
        }
        
        // Show damage indicator
        Location damageLocation = npc.getEntity().getLocation().add(0, 1.5, 0);
        showDamageIndicator(damageLocation, finalDamage, caster);
        
        // Start combat behavior
        startCombatBehavior(npc, caster);
        
        // Apply appropriate visual effects based on ability
        applyAbilityVisualEffects(npc, abilityId);
        
        // Check if NPC is dead
        if (currentHealth <= 0) {
            handleNPCDeath(npc, caster);
        } else {
            // Update the nameplate with the new health value
            NPCManager.getInstance().updateNameplate(npc, currentHealth, stats.getMaxHealth());
            
            // Play hurt animation/sound
            npc.getEntity().getWorld().playSound(
                npc.getEntity().getLocation(),
                Sound.ENTITY_PLAYER_HURT,
                1.0f,
                1.0f
            );
        }
    }

    // Add this field to the class
    private Map<UUID, Map<String, Long>> abilityCooldownMap = new HashMap<>();

    // Add this helper method for visual effects
    private void applyAbilityVisualEffects(NPC npc, String abilityId) {
        if (!npc.isSpawned()) return;
        
        switch (abilityId) {
            case "fire_beam":
                // Fire visual effects
                npc.getEntity().getWorld().spawnParticle(
                    org.bukkit.Particle.FLAME,
                    npc.getEntity().getLocation().add(0, 1, 0),
                    10, 0.3, 0.5, 0.3, 0.05
                );
                break;
                
            case "lightning_throw":
                // Lightning visual effects
                npc.getEntity().getWorld().spawnParticle(
                    org.bukkit.Particle.ELECTRIC_SPARK,
                    npc.getEntity().getLocation().add(0, 1, 0),
                    15, 0.5, 0.5, 0.5, 0.1
                );
                npc.getEntity().getWorld().playSound(
                    npc.getEntity().getLocation(),
                    Sound.ENTITY_LIGHTNING_BOLT_IMPACT,
                    0.6f,
                    1.2f
                );
                break;
                
            case "blood_harvest":
                // Blood harvest visual effects
                npc.getEntity().getWorld().spawnParticle(
                    org.bukkit.Particle.CRIMSON_SPORE,
                    npc.getEntity().getLocation().add(0, 1, 0),
                    20, 0.4, 0.6, 0.4, 0.05
                );
                npc.getEntity().getWorld().playSound(
                    npc.getEntity().getLocation(),
                    Sound.ENTITY_PHANTOM_BITE,
                    0.7f,
                    0.7f
                );
                break;
        }
    }
    
    /**
     * Handle NPC death
     */
    private void handleNPCDeath(NPC npc, Entity killer) {
        // Get NPC stats before stopping
        NPCStats stats = getNPCStats(npc.getUniqueId());
        
        // Stop combat behavior
        UUID npcId = npc.getUniqueId();
        stopCombatBehavior(npcId);
        
        // Store the location and original name before removing the NPC
        Location respawnLocation = npc.isSpawned() ? npc.getEntity().getLocation().clone() : 
                                npc.getStoredLocation();
        
        // Get the clean original name
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
        
        // Remove nameplate FIRST - this is important to do before anything else
        NPCManager.getInstance().removeNameplate(npcId);
        
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
            
            // Reward killer if it's a player
            if (killer instanceof Player) {
                Player player = (Player) killer;
                
                // Give XP based on NPC stats
                int expReward = (int)(stats.getExpReward() * stats.getNpcType().getRewardMultiplier());
                player.giveExp(expReward);
                player.sendMessage(ChatColor.GREEN + "You defeated " + ChatColor.YELLOW + originalName + 
                    ChatColor.GREEN + "! " + ChatColor.YELLOW + "+" + expReward + " XP");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
                
                // Gold drops based on NPC stats
                int minGold = stats.getMinGoldDrop();
                int maxGold = stats.getMaxGoldDrop();
                int goldAmount = minGold + (int)(Math.random() * (maxGold - minGold + 1));
                player.sendMessage(ChatColor.GOLD + "+" + goldAmount + " Gold");
                
                // Add gold to player if economy system is available
                try {
                    // You would have your economy system here
                    // Main.getInstance().getEconomyManager().addGold(player, goldAmount);
                } catch (Exception e) {
                    // Just log if economy system fails
                }
            }
            
            // CRITICAL: Check and remove ANY remaining ArmorStand passengers before despawning
            if (npc.getEntity().getPassengers() != null && !npc.getEntity().getPassengers().isEmpty()) {
                for (Entity passenger : new ArrayList<>(npc.getEntity().getPassengers())) {
                    passenger.remove();
                }
                npc.getEntity().eject(); // Clear all passengers
            }
            
            // Force remove any nameplate stand reference from NPCManager
            ArmorStand nameplate = NPCManager.getInstance().getNameplateStands().remove(npcId);
            if (nameplate != null && nameplate.isValid() && !nameplate.isDead()) {
                nameplate.remove();
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
                    npcHealth.put(npcId, stats.getMaxHealth());
                    
                    // Make the NPC vulnerable again
                    if (npc.isSpawned()) {
                        // IMPORTANT: Set to NOT invulnerable so we can receive damage events
                        npc.getEntity().setInvulnerable(false);
                        
                        // Hide default nameplate
                        npc.getEntity().setCustomNameVisible(false);
                        npc.getEntity().setCustomName(null);
                        npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, false);
                        
                        // Store the original name for consistency
                        npc.getEntity().setMetadata("original_name", new FixedMetadataValue(Main.getInstance(), finalOriginalName));
                        
                        // Store the level and type
                        npc.getEntity().setMetadata("level", new FixedMetadataValue(Main.getInstance(), stats.getLevel()));
                        npc.getEntity().setMetadata("npc_type", new FixedMetadataValue(Main.getInstance(), 
                                stats.getNpcType().name()));
                        
                        // Wait a tick to ensure NPC is fully spawned before adding nameplate
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (npc.isSpawned()) {
                                    // Create a new nameplate
                                    NPCManager.getInstance().createHologramNameplate(npc, finalOriginalName, stats.getMaxHealth(), stats.getMaxHealth());
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
        // Use CRIT particles without block data
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