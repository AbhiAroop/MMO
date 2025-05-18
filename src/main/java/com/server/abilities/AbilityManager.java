package com.server.abilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

public class AbilityManager {
    private static AbilityManager instance;
    private final Main plugin;
    private final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();
    private final Map<UUID, Map<String, Long>> entityHitCooldowns = new HashMap<>();

    private AbilityManager(Main plugin) {
        this.plugin = plugin;
    }

    public static void initialize(Main plugin) {
        if (instance == null) {
            instance = new AbilityManager(plugin);
        }
    }

    public static AbilityManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AbilityManager has not been initialized!");
        }
        return instance;
    }
    
    public boolean activateAbility(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey keyAbilityId = new NamespacedKey(Main.getInstance(), "ability_id");
        
        if (!meta.getPersistentDataContainer().has(keyAbilityId, PersistentDataType.STRING)) return false;
        
        String abilityId = meta.getPersistentDataContainer().get(keyAbilityId, PersistentDataType.STRING);
        
        // Check which ability to use
        if (abilityId.equals("fire_beam")) {
            return castFireBeam(player, item);
        } else if (abilityId.equals("lightning_throw")) {
            return castLightningThrow(player, item);
        } else if (abilityId.equals("blood_harvest")) {
        return castBloodHarvest(player, item);
    }
        
        
        return false;
    }
    
    private boolean castFireBeam(Player player, ItemStack item) {
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return false;

        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return false;
        
        // Check for cooldown
        if (isOnCooldown(player, "fire_beam")) {
            long remainingCooldown = getCooldownTimeRemaining(player, "fire_beam") / 1000;
            player.sendMessage("§cAbility on cooldown for " + remainingCooldown + " more seconds!");
            return false;
        }
        
        // Check for mana cost
        if (!profile.getStats().canUseMana(30)) {
            player.sendMessage("§cNot enough mana! Required: §b30");
            return false;
        }
        
        // Use mana
        profile.getStats().useMana(30);
        
        // Apply cooldown
        double baseCooldown = 20.0; // 20 seconds
        double cooldownReduction = 1.0 - (profile.getStats().getCooldownReduction() / 100.0);
        int finalCooldown = Math.max(1, (int)(baseCooldown * cooldownReduction));
        setCooldown(player, "fire_beam", finalCooldown * 1000); // Convert to milliseconds
        
        // Cast the beam
        World world = player.getWorld();
        Location startLoc = player.getEyeLocation();
        Vector direction = player.getEyeLocation().getDirection();
        
        // Fire sound effect
        world.playSound(startLoc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
        // Calculate beam damage based on player's magic damage

        double damagePerSecond = 3.0; // Base damage
        double finalDamagePerTick = (damagePerSecond / 20); // Convert to damage per tick with scaling
        
        // Create the particle beam
        new BukkitRunnable() {
            double distance = 0;
            final double maxDistance = 10.0;
            final double particleDistance = 0.5;
            
            @Override
            public void run() {
                // Cancel if max distance reached
                if (distance > maxDistance) {
                    this.cancel();
                    return;
                }
                
                // Move forward
                Location currentLoc = startLoc.clone().add(direction.clone().multiply(distance));
                
                // Check for blocks
                if (!currentLoc.getBlock().isPassable()) {
                    // Hit a block, create impact particles
                    DustOptions dustOptions = new DustOptions(Color.fromRGB(255, 150, 50), 1.0f);
                    world.spawnParticle(Particle.DUST, currentLoc, 15, 0.1, 0.1, 0.1, 0.1, dustOptions);
                    world.playSound(currentLoc, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f);
                    this.cancel();
                    return;
                }
                
                // Display particle beam
                DustOptions dustOptions = new DustOptions(Color.fromRGB(255, 80, 30), 1.0f);
                world.spawnParticle(Particle.DUST, currentLoc, 5, 0.05, 0.05, 0.05, 0.01, dustOptions);
                world.spawnParticle(Particle.FLAME, currentLoc, 1, 0.05, 0.05, 0.05, 0.01);
                
                // Fire beam - update the entity damage section
                for (Entity entity : world.getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)) {
                    if (entity instanceof LivingEntity && 
                        entity != player && 
                        isValidAbilityTarget(entity)) {  // Add this filter
                        
                        LivingEntity livingEntity = (LivingEntity) entity;
                        
                        // Apply fire
                        livingEntity.setFireTicks(60); // Set on fire for 3 seconds
                        
                        // Calculate damage
                        double initialDamage = finalDamagePerTick * 3;
                        
                        // Use our safe damage method
                        applyAbilityDamageToTarget(livingEntity, player, "fire_beam", initialDamage);

                        // Apply omnivamp from initial hit
                        applyOmnivampHealing(player, initialDamage);
                        
                        // Apply DoT effect but don't hit the entity repeatedly
                        applyBurnDamageOverTime(player, livingEntity, finalDamagePerTick, 3);
                    }
                }
                
                // Increase distance for next tick
                distance += particleDistance;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        // Update action bar with mana
        player.sendMessage("§bMana: " + profile.getStats().getMana() + "/" + profile.getStats().getTotalMana());
        
        return true;
    }
    
    // Apply a burn damage over time effect
    private void applyBurnDamageOverTime(Player source, LivingEntity target, double damagePerTick, int seconds) {
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = seconds * 20; // Convert seconds to ticks
            
            @Override
            public void run() {
                if (ticks >= maxTicks || target.isDead()) {
                    this.cancel();
                    return;
                }
                
                // Show burning particle effect
                target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.01);
                
                // Apply damage (1 time per second)
                if (ticks % 20 == 0) {
                    target.damage(damagePerTick, source);

                    // Apply omnivamp healing for DoT damage
                    applyOmnivampHealing(source, damagePerTick);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Safely apply ability damage to a target (NPC or other entity)
     * This method includes protection against multiple hits
     */
    private void applyAbilityDamageToTarget(LivingEntity target, Player player, String abilityId, double damage) {
        
        // First validate that this is a real target, not a nameplate
        if (!isValidAbilityTarget(target)) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Skipping invalid ability target: " + target.getType());
            }
            return;
        }

        // Check if it's a Citizens NPC
        net.citizensnpcs.api.npc.NPC npc = null;
        try {
            npc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(target);
        } catch (Exception e) {
            // Not a Citizens NPC or Citizens API not available
        }
        
        if (npc != null) {
            // Use the CombatHandler's built-in ability damage system
            
        }
        
        // For regular entities (non-NPCs), use cooldown tracking and apply damage
        // (Rest of the method remains unchanged for non-NPC entities)
        UUID entityId = target.getUniqueId();
        String uniqueAbilityId = abilityId + "-" + player.getUniqueId().toString();
        
        // Create a cooldown map for entities to track recent hits
        if (!entityHitCooldowns.containsKey(entityId)) {
            entityHitCooldowns.put(entityId, new HashMap<>());
        }
        
        // Check if this entity was hit recently by this ability from this player
        Map<String, Long> cooldowns = entityHitCooldowns.get(entityId);
        if (cooldowns.containsKey(uniqueAbilityId)) {
            long cooldownEndTime = cooldowns.get(uniqueAbilityId);
            if (System.currentTimeMillis() < cooldownEndTime) {
                // Entity was hit too recently, don't apply damage again
                return;
            }
        }
        
        // Set cooldown (2 seconds) to prevent multiple hits
        cooldowns.put(uniqueAbilityId, System.currentTimeMillis() + 2000);
        
        // Apply regular damage with caps to prevent one-shots
        double currentHealth = target.getHealth();
        double maxHealth = target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        double maxAllowedDamage = Math.min(damage, currentHealth * 0.4); // Cap at 40% of current health
        
        // Apply damage directly for better control
        target.damage(0.1, player); // Apply tiny amount for attribution
        target.setHealth(Math.max(1.0, currentHealth - maxAllowedDamage)); // Ensure we don't kill with 1 hit
    }
    
    public boolean isOnCooldown(Player player, String abilityId) {
        Map<String, Long> cooldowns = playerCooldowns.getOrDefault(player.getUniqueId(), new HashMap<>());
        long currentTime = System.currentTimeMillis();
        
        if (cooldowns.containsKey(abilityId)) {
            return cooldowns.get(abilityId) > currentTime;
        }
        
        return false;
    }
    
    public long getCooldownTimeRemaining(Player player, String abilityId) {
        Map<String, Long> cooldowns = playerCooldowns.getOrDefault(player.getUniqueId(), new HashMap<>());
        long currentTime = System.currentTimeMillis();
        
        if (cooldowns.containsKey(abilityId)) {
            long remaining = cooldowns.get(abilityId) - currentTime;
            return remaining > 0 ? remaining : 0;
        }
        
        return 0;
    }
    
    public void setCooldown(Player player, String abilityId, long cooldownMs) {
        Map<String, Long> cooldowns = playerCooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        cooldowns.put(abilityId, System.currentTimeMillis() + cooldownMs);
    }

    private boolean castLightningThrow(Player player, ItemStack item) {
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return false;

        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return false;
        
        // Check for cooldown
        if (isOnCooldown(player, "lightning_throw")) {
            long remainingCooldown = getCooldownTimeRemaining(player, "lightning_throw") / 1000;
            player.sendMessage("§cAbility on cooldown for " + remainingCooldown + " more seconds!");
            return false;
        }
        
        // Check for mana cost
        if (!profile.getStats().canUseMana(20)) {
            player.sendMessage("§cNot enough mana! Required: §b20");
            return false;
        }
        
        // Use mana
        profile.getStats().useMana(20);
        
        // Apply cooldown with reduction from player stats
        double baseCooldown = 10.0; // 10 seconds
        double cooldownReduction = 1.0 - (profile.getStats().getCooldownReduction() / 100.0);
        int finalCooldown = Math.max(1, (int)(baseCooldown * cooldownReduction));
        setCooldown(player, "lightning_throw", finalCooldown * 1000); // Convert to milliseconds
        
        // Get player's physical damage - already includes weapon damage
        int playerPhysicalDamage = profile.getStats().getPhysicalDamage();
        
        // New damage calculation formula - use only the player stats which already include weapon damage
        // int totalPhysicalDamage = playerPhysicalDamage + itemPhysicalDamage; // OLD: double-counting
        int totalPhysicalDamage = playerPhysicalDamage; // NEW: use only player stats which already include weapon damage
        double baseDamage = 10.0;
        double bonusDamage = totalPhysicalDamage;
        double percentBonus = totalPhysicalDamage * 0.1; // 10% of total physical damage
        double finalDamage = baseDamage + bonusDamage + percentBonus;
        
        // Rest of the method remains unchanged...
        // Store the current slot where the weapon is
        final int weaponSlot = player.getInventory().getHeldItemSlot();
        final ItemStack originalItem = item.clone();
        
        // Hide the item from player's hand temporarily
        player.getInventory().setItem(weaponSlot, null);
        
        // Play throw sound
        World world = player.getWorld();
        Location startLoc = player.getEyeLocation();
        world.playSound(startLoc, Sound.ITEM_TRIDENT_THROW, 1.0f, 0.8f);
        
        // Launch the trident
        Vector direction = player.getEyeLocation().getDirection();
        
        new BukkitRunnable() {
            double distance = 0;
            final double maxDistance = 30.0; // Maximum throw distance
            final double particleDistance = 0.5;
            final List<Entity> hitEntities = new ArrayList<>(); // Track entities hit
            Location currentLoc;
            
            @Override
            public void run() {
                // Cancel if max distance reached
                if (distance > maxDistance) {
                    returnTrident();
                    this.cancel();
                    return;
                }
                
                // Move forward
                currentLoc = startLoc.clone().add(direction.clone().multiply(distance));
                
                // Check for blocks
                if (!currentLoc.getBlock().isPassable()) {
                    world.playSound(currentLoc, Sound.BLOCK_STONE_HIT, 1.0f, 1.0f);
                    world.spawnParticle(Particle.CRIT, currentLoc, 15, 0.2, 0.2, 0.2, 0.1);
                    returnTrident();
                    this.cancel();
                    return;
                }
                
                // Display trident particles
                DustOptions blueTrail = new DustOptions(Color.fromRGB(30, 144, 255), 1.0f);
                world.spawnParticle(Particle.DUST, currentLoc, 3, 0.1, 0.1, 0.1, 0, blueTrail);
                world.spawnParticle(Particle.ELECTRIC_SPARK, currentLoc, 1, 0.1, 0.1, 0.1, 0);
                
                ArmorStand weaponVisual = (ArmorStand) world.spawnEntity(currentLoc, EntityType.ARMOR_STAND);
                weaponVisual.setVisible(false);
                weaponVisual.setGravity(false);
                weaponVisual.setSmall(true);
                weaponVisual.setMarker(true);
                weaponVisual.getEquipment().setItemInMainHand(new ItemStack(Material.WOODEN_HOE)); 
                weaponVisual.setRightArmPose(new EulerAngle(Math.toRadians(90), 0, 0));

                // Schedule removal of the visual
                plugin.getServer().getScheduler().runTaskLater(plugin, weaponVisual::remove, 3L);
                
                // Check for entities in the trident's path
                for (Entity entity : world.getNearbyEntities(currentLoc, 1.0, 1.0, 1.0)) {
                    if (entity instanceof LivingEntity && 
                        entity != player && 
                        !hitEntities.contains(entity) && 
                        isValidAbilityTarget(entity)) {  // Add this filter
                        
                        LivingEntity target = (LivingEntity) entity;
                        
                        // Add to hit list so we don't hit multiple times
                        hitEntities.add(target);
                        
                        // Use safe damage method
                        applyAbilityDamageToTarget(target, player, "lightning_throw", finalDamage);
                        
                        // Apply the lightning visual effects
                        target.getWorld().strikeLightningEffect(target.getLocation());
                        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, 
                            target.getLocation().add(0, 1, 0),
                            20, 0.5, 0.5, 0.5, 0.2);
                        
                        // Play hit sound
                        world.playSound(target.getLocation(), 
                            Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.2f);
                    }
                }
                
                // Increase distance for next tick
                distance += particleDistance;
            }
            
            private void returnTrident() {
                // Schedule return of trident after 2 seconds
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    // Play return sound
                    world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.2f);
                    
                    // Visual effect of return
                    if (currentLoc != null) {
                        Location returnStart = currentLoc.clone();
                        Vector returnVector = player.getLocation().add(0, 1, 0).toVector().subtract(returnStart.toVector());
                        double returnDistance = returnStart.distance(player.getLocation());
                        Vector returnDir = returnVector.normalize().multiply(0.5);
                        
                        new BukkitRunnable() {
                            double traveled = 0;
                            @Override
                            public void run() {
                                if (traveled >= returnDistance) {
                                    this.cancel();
                                    return;
                                }
                                
                                returnStart.add(returnDir);
                                traveled += 0.5;
                                
                                world.spawnParticle(Particle.ELECTRIC_SPARK, returnStart, 2, 0.1, 0.1, 0.1, 0);
                            }
                        }.runTaskTimer(plugin, 0L, 1L);
                    }
                    
                    // Return item to player's inventory
                    if (player.isOnline()) {
                        player.getInventory().setItem(weaponSlot, originalItem);
                        player.sendMessage("§aArcloom §7returns to your hand!");
                    }
                }, 40L); // 2 seconds (40 ticks)
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        return true;
    }

    /**
     * Applies omnivamp healing based on magic damage dealt
     * @param player The player who dealt the damage
     * @param damage The amount of magic damage dealt
     */
    private void applyOmnivampHealing(Player player, double damage) {
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;

        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Apply omnivamp healing
        double omnivampPercent = profile.getStats().getOmnivamp();
        if (omnivampPercent > 0) {
            // Calculate amount to heal (omnivampPercent% of damage)
            double healAmount = damage * (omnivampPercent / 100.0);
            
            // Get player's current and max health
            double currentHealth = player.getHealth();
            double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            
            // Only heal if player isn't at full health
            if (currentHealth < maxHealth && healAmount > 0) {
                // Calculate new health value (capped at max health)
                double newHealth = Math.min(currentHealth + healAmount, maxHealth);
                
                // Apply the healing
                player.setHealth(newHealth);
                
                // Store the updated health in player stats
                profile.getStats().setCurrentHealth(newHealth);
                
                // Show a visual effect for omnivamp if it's significant (at least 1 health point)
                if (healAmount >= 1.0) {
                    // Play a subtle healing sound
                    player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.4f);
                    
                    // Display healing message if significant
                    if (healAmount >= 3.0) {
                        player.sendMessage("§d✦ §7Omnivamp healed you for §d" + String.format("%.1f", healAmount) + " §7health");
                    }
                    
                    // Debug info
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info(player.getName() + " healed for " + healAmount + 
                                                " from omnivamp (" + omnivampPercent + "%)");
                    }
                }
            }
        }
    }

    private boolean castBloodHarvest(Player player, ItemStack item) {
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return false;

        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return false;
        
        // Check for cooldown
        if (isOnCooldown(player, "blood_harvest")) {
            long remainingCooldown = getCooldownTimeRemaining(player, "blood_harvest") / 1000;
            player.sendMessage("§cAbility on cooldown for " + remainingCooldown + " more seconds!");
            return false;
        }
        
        // Check for mana cost
        if (!profile.getStats().canUseMana(40)) {
            player.sendMessage("§cNot enough mana! Required: §b40");
            return false;
        }
        
        // Use mana
        profile.getStats().useMana(40);
        
        // Apply cooldown with reduction from player stats
        double baseCooldown = 10.0; // 10 seconds
        double cooldownReduction = 1.0 - (profile.getStats().getCooldownReduction() / 100.0);
        int finalCooldown = Math.max(1, (int)(baseCooldown * cooldownReduction));
        setCooldown(player, "blood_harvest", finalCooldown * 1000); // Convert to milliseconds
        
        // Base damage values - FIXED values that won't be modified by scaling
        double baseDamage = 50.0;
        double enhancedDamage = 80.0;
        
        // Get player's location and direction
        Location playerLoc = player.getLocation();
        Vector playerDir = player.getLocation().getDirection().setY(0).normalize();
        World world = player.getWorld();
        
        // Play initial cast sound
        world.playSound(playerLoc, Sound.ENTITY_WITHER_SHOOT, 0.7f, 1.5f);
        world.playSound(playerLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, 0.5f, 0.8f);
        
        // Create a list to track hit entities
        List<LivingEntity> hitEntities = new ArrayList<>();
        
        // Create cone-shaped attack
        double coneAngle = Math.PI / 3; // 60-degree cone
        double coneRange = 5.0; // 5 blocks range
        
        // Animation and damage application
        new BukkitRunnable() {
            double animTime = 0;
            final double animDuration = 0.5; // Animation lasts 0.5 seconds
            final double step = 0.05;
            
            @Override
            public void run() {
                // Inside the castBloodHarvest method, update this section:
                if (animTime >= animDuration) {
                    // Animation complete, apply final effects
                    
                    // Determine if enhanced damage should be applied (3+ targets hit)
                    boolean applyEnhancedDamage = hitEntities.size() >= 3;
                    double finalDamage = applyEnhancedDamage ? enhancedDamage : baseDamage;

                    // Apply damage to all hit entities
                    for (LivingEntity target : hitEntities) {
                        // Apply damage using our safe method
                        applyAbilityDamageToTarget(target, player, "blood_harvest", finalDamage);
                        
                        // Play effects at the target
                        Location targetLoc = target.getLocation().add(0, 1, 0);
                        world.spawnParticle(Particle.SOUL, targetLoc, 10, 0.3, 0.3, 0.3, 0.05);
                        
                        // Healing from blood harvest - only apply if the target isn't an NPC
                        if (!(net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(target))) {
                            double healthSteal = finalDamage * 0.3; // 30% lifesteal
                            double currentPlayerHealth = player.getHealth();
                            double maxPlayerHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                            double newPlayerHealth = Math.min(maxPlayerHealth, currentPlayerHealth + healthSteal);
                            player.setHealth(newPlayerHealth);
                            
                            // Show healing effect
                            player.getWorld().spawnParticle(
                                Particle.HEART,
                                player.getLocation().add(0, 1.2, 0),
                                3, 0.3, 0.3, 0.3, 0.1
                            );
                        } else {
                            // For NPCs, still apply some lifesteal but based on the NPC's custom health
                           
                        }
                    }

                    // Success message with damage value included
                    if (applyEnhancedDamage) {
                        player.sendMessage("§4Blood Harvest §cstrikes " + hitEntities.size() + 
                                        " targets with §4enhanced damage §c(" + (int)finalDamage + ")!");
                    } else if (hitEntities.size() > 0) {
                        player.sendMessage("§4Blood Harvest §cstrikes " + hitEntities.size() + " target" + 
                                        (hitEntities.size() > 1 ? "s" : "") + " §c(" + (int)finalDamage + ")!");
                    } else {
                        player.sendMessage("§4Blood Harvest §cfound no targets!");
                    }
                    
                    this.cancel();
                    return;
                }
                
                // Calculate progress (0 to 1)
                double progress = animTime / animDuration;
                
                // Create the cone visual with two sets of particles for a more dramatic effect
                for (double r = 0; r < coneRange; r += 0.5) {
                    double arcWidth = Math.tan(coneAngle) * r;
                    
                    for (double a = -arcWidth; a <= arcWidth; a += 0.3) {
                        // Calculate position in the arc
                        Vector right = playerDir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                        Vector arcPos = playerDir.clone().multiply(r).add(right.multiply(a));
                        
                        // Add some vertical variation for a 3D effect
                        double yOffset = Math.sin(r + progress * Math.PI * 2) * 0.2;
                        
                        // Calculate position in world space
                        Location particleLoc = playerLoc.clone().add(arcPos).add(0, 0.5 + yOffset, 0);
                        
                        // Blood particle effect (red dust)
                        DustOptions bloodDust = new DustOptions(Color.fromRGB(128, 0, 0), 1.2f);
                        world.spawnParticle(Particle.DUST, particleLoc, 1, 0.05, 0.05, 0.05, 0, bloodDust);
                        
                        // Trail effects that follow the arc shape
                        if (Math.random() < 0.3) {
                            DustOptions darkDust = new DustOptions(Color.fromRGB(50, 0, 0), 1.0f);
                            Location trailLoc = particleLoc.clone().add(0, Math.sin(progress * Math.PI * 4) * 0.3, 0);
                            world.spawnParticle(Particle.DUST, trailLoc, 1, 0.05, 0.05, 0.05, 0, darkDust);
                        }
                        
                        // Soul fire effect for a supernatural touch
                        if (Math.random() < 0.1) {
                            world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0.05, 0.05, 0.05, 0);
                        }
                        
                        // Find entities in this part of the cone (in the sweep)
                        for (Entity entity : world.getNearbyEntities(particleLoc, 0.8, 1.0, 0.8)) {
                            if (entity instanceof LivingEntity && 
                                entity != player && 
                                !hitEntities.contains(entity) && 
                                isValidAbilityTarget(entity)) {  // Add this filter
                                
                                // Calculate angle between player direction and entity direction
                                Vector toEntity = entity.getLocation().toVector().subtract(playerLoc.toVector());
                                double angle = playerDir.angle(toEntity);
                                
                                // Check if entity is within cone angle and range
                                if (angle <= coneAngle && toEntity.length() <= coneRange) {
                                    hitEntities.add((LivingEntity) entity);
                                    
                                    // Target acquired indicator
                                    DustOptions targetDust = new DustOptions(Color.fromRGB(255, 0, 0), 1.5f);
                                    world.spawnParticle(Particle.DUST, entity.getLocation().add(0, 1, 0), 
                                                8, 0.3, 0.3, 0.3, 0, targetDust);
                                }
                            }
                        }
                    }
                }
                
                // Update animation time
                animTime += step;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        return true;
    }

    /**
     * Check if an entity should be counted for ability hit tracking
     * @param entity The entity to check
     * @return true if the entity is a valid target, false if it should be ignored
     */
    private boolean isValidAbilityTarget(Entity entity) {
        // Ignore null entities
        if (entity == null) return false;
        
        // Ignore dead entities
        if (entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) return false;
        
        // Special handling for ArmorStands - ignore nameplates
        if (entity instanceof ArmorStand) {
            ArmorStand stand = (ArmorStand) entity;
            
            // Check for NPC nameplate metadata
            if (stand.hasMetadata("npc_uuid")) {
                return false;
            }
            
            // Check for characteristics of nameplate stands
            if (stand.isSmall() && stand.isMarker() && stand.isCustomNameVisible() && !stand.isVisible()) {
                return false;
            }
            
            // Skip armor stands riding other entities (these are likely nameplates)
            if (stand.isInsideVehicle()) {
                return false;
            }
        }
        
        return true;
    }    
}