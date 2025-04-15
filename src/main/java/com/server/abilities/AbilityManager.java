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
        int magicDamage = profile.getStats().getMagicDamage();
        double damagePerSecond = 3.0; // Base damage
        double finalDamagePerTick = (damagePerSecond / 20) * (1 + (magicDamage / 100.0)); // Convert to damage per tick with scaling
        
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
                
                // Check for entities in beam path
                for (Entity entity : world.getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity livingEntity = (LivingEntity) entity;
                        
                        // Apply fire
                        livingEntity.setFireTicks(60); // Set on fire for 3 seconds
                        
                        // Apply damage
                        livingEntity.damage(finalDamagePerTick * 5, player); // Initial hit damage
                        
                        // Apply DoT effect
                        applyBurnDamageOverTime(player, livingEntity, finalDamagePerTick, 5);
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
                
                // Apply damage every other tick (10 times per second)
                if (ticks % 2 == 0) {
                    target.damage(damagePerTick, source);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
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
        
        // Calculate damage
        int physicalDamage = profile.getStats().getPhysicalDamage();
        double baseDamage = 10.0;
        double percentDamage = physicalDamage * 0.1; // 10% of physical damage
        double totalDamage = baseDamage + percentDamage;
        
        // Store the current slot where the trident is
        final int tridentSlot = player.getInventory().getHeldItemSlot();
        final ItemStack originalItem = item.clone();
        
        // Hide the item from player's hand temporarily
        player.getInventory().setItem(tridentSlot, null);
        
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
                weaponVisual.getEquipment().setItemInMainHand(new ItemStack(Material.WOODEN_HOE)); // Changed from TRIDENT to WOODEN_HOE
                weaponVisual.setRightArmPose(new EulerAngle(Math.toRadians(90), 0, 0));

                // Schedule removal of the visual
                plugin.getServer().getScheduler().runTaskLater(plugin, weaponVisual::remove, 3L);
                
                // Check for entities in the trident's path
                for (Entity entity : world.getNearbyEntities(currentLoc, 1.0, 1.0, 1.0)) {
                    if (entity instanceof LivingEntity && entity != player && !hitEntities.contains(entity)) {
                        LivingEntity target = (LivingEntity) entity;
                        
                        // Add to hit list so we don't hit multiple times
                        hitEntities.add(target);
                        
                        // Apply damage
                        target.damage(totalDamage, player);
                        
                        // Visual and sound effects
                        world.spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.2);
                        world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
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
                        player.getInventory().setItem(tridentSlot, originalItem);
                        player.sendMessage("§aArcloom §7returns to your hand!");
                    }
                }, 40L); // 2 seconds (40 ticks)
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        return true;
    }
}