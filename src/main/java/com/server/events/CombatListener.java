package com.server.events;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import static org.bukkit.persistence.PersistentDataType.INTEGER;
import org.bukkit.util.Vector;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.entities.CustomMobStats;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

public class CombatListener implements Listener {
    
    private final Main plugin;
    
    public CombatListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle damage dealt by players to entities
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;

        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;

        // Get base damage from stats - This already includes weapon damage from StatScanManager
        double damage = profile.getStats().getPhysicalDamage();
        
        // Check held weapon for special effects only (NOT for base damage)
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        boolean procBonusDamage = false;
        
        if (heldItem != null && heldItem.hasItemMeta()) {
            ItemMeta meta = heldItem.getItemMeta();
            
            // Only check for special effects like Precision Strike
            // DO NOT add the weapon's base damage again
            if (meta.hasCustomModelData() && meta.getCustomModelData() == 210001) {
                NamespacedKey key = new NamespacedKey(Main.getInstance(), "hit_counter");
                PersistentDataContainer container = meta.getPersistentDataContainer();
                
                if (container.has(key, PersistentDataType.INTEGER)) {
                    int hitCount = container.get(key, PersistentDataType.INTEGER);
                    hitCount++;
                    
                    // Every 5th hit
                    if (hitCount == 5) {
                        damage += 3.0; // Add bonus damage from passive
                        procBonusDamage = true;
                        hitCount = 0; // Reset counter
                        player.sendMessage("§6Precision Strike! §7Your attack deals §c+3 §7bonus damage!");
                    }
                    
                    container.set(key, INTEGER, hitCount);
                    heldItem.setItemMeta(meta);
                }
            }
            
        }
        
        // Get the attack charge progression
        float chargePercent = player.getAttackCooldown();
        
        // Determine if attack is fully charged (chargePercent is very close to 1.0)
        boolean isFullyCharged = chargePercent >= 0.9f;
        
        // Check for critical hit - only apply if attack is fully charged
        boolean isCritical = isFullyCharged && !player.isOnGround() && player.getFallDistance() > 0.0f;
        
        // UPDATED DAMAGE CALCULATION:
        // At 0% charge: Damage is 0.5
        // At 1% charge: Damage is max(0.5, 1% of full damage)
        // At X% charge: Damage is max(0.5, X% of full damage)
        // At 100% charge: Full damage
        double scaledDamage;
        if (chargePercent <= 0.01) {
            // At 0-1% charge, minimum damage is 0.5
            scaledDamage = 0.5;
        } else {
            // For charges above 1%, scale linearly but ensure minimum 0.5 damage
            scaledDamage = Math.max(0.5, damage * chargePercent);
        }
        
        // Apply critical hit multiplier if applicable
        if (isCritical) {
            scaledDamage *= profile.getStats().getCriticalDamage();
        }
        
        // Set the final damage
        event.setDamage(scaledDamage);
        
        // Debug information
        if (plugin.isDebugEnabled(DebugSystem.COMBAT)) {
            plugin.debugLog(DebugSystem.COMBAT,player.getName() + "'s attack: Charge=" + String.format("%.2f", chargePercent) + 
                                ", Base Damage=" + String.format("%.2f", damage) + 
                                ", Scaled Damage=" + String.format("%.2f", scaledDamage) +
                                ", Critical=" + isCritical);
        }
        
        // Apply lifesteal based on final damage dealt
        double lifeStealPercent = profile.getStats().getLifeSteal();
        if (lifeStealPercent > 0) {
            // Calculate amount to heal (lifeStealPercent% of final damage)
            double healAmount = scaledDamage * (lifeStealPercent / 100.0);
            
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
                
                // Show a visual effect for lifesteal if it's significant (at least 1 health point)
                if (healAmount >= 1.0) {
                    // Play a subtle healing sound
                    player.playSound(player.getLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, 0.5f, 1.2f);
                    
                    // Display healing message if significant
                    if (healAmount >= 3.0) {
                        player.sendMessage("§a⚕ §7Lifesteal healed you for §a" + String.format("%.1f", healAmount) + " §7health");
                    }
                    
                    // Debug info
                    if (plugin.isDebugEnabled(DebugSystem.COMBAT)) {
                        plugin.debugLog(DebugSystem.COMBAT,player.getName() + " healed for " + healAmount + 
                                            " from lifesteal (" + lifeStealPercent + "%)");
                    }
                }
            }
        }
        
        // Display damage indicator if procced bonus damage
        if (procBonusDamage) {
            // Optional: Add visual effect for Precision Strike
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
        }
        
        // Visual and sound effects based on charge level
        if (isFullyCharged) {
            // Full charge attack sound and effect
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
        } else if (chargePercent >= 0.5f) {
            // Medium charge attack sound
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK, 0.8f, 1.0f);
        } else {
            // Low charge attack sound
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.5f, 1.0f);
        }
    }

    /**
     * Handle special abilities for custom mobs
     */
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        LivingEntity entity = (LivingEntity) event.getEntity();
        
        // Check if it's a custom mob with abilities
        if (plugin.getCustomEntityManager().isCustomMob(entity)) {
            CustomMobStats stats = plugin.getCustomEntityManager().getMobStats(entity);
            if (stats == null || !stats.hasCustomAbilities()) return;
            
            // If the target is a player and the mob is not already attacking
            if (event.getTarget() instanceof Player && event.getReason() == TargetReason.CLOSEST_PLAYER) {
                // Random chance to use special ability when targeting a player
                if (Math.random() < 0.3) { // 30% chance
                    // Randomly choose between ability 1 and 2
                    int abilityIndex = (Math.random() < 0.5) ? 1 : 2;
                    
                    // Play the special ability animation
                    plugin.getCustomEntityManager().playSpecialAbility(entity, abilityIndex);
                    
                    // For the Runemark Colossus, implement actual ability effects
                    if (stats.getName().equals("Runemark Colossus")) {
                        Player player = (Player) event.getTarget();
                        
                        if (abilityIndex == 1) {
                            // Ground slam - area effect damage
                            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                                @Override
                                public void run() {
                                    // Create ground slam effect
                                    entity.getWorld().playSound(entity.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
                                    entity.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, entity.getLocation(), 1, 0, 0, 0, 0);
                                    
                                    // Damage nearby players
                                    for (Entity nearby : entity.getNearbyEntities(5, 2, 5)) {
                                        if (nearby instanceof Player) {
                                            Player nearbyPlayer = (Player) nearby;
                                            nearbyPlayer.damage(stats.getPhysicalDamage() * 0.8, entity);
                                            nearbyPlayer.setVelocity(nearbyPlayer.getLocation().subtract(entity.getLocation()).toVector().normalize().multiply(1.2).setY(0.5));
                                        }
                                    }
                                }
                            }, 15L); // Delay for animation to play before effect
                        } else if (abilityIndex == 2) {
                            // Energy beam
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                // Create beam effect
                                Location startLoc = entity.getEyeLocation();
                                Vector direction = player.getLocation().subtract(startLoc).toVector().normalize();
                                
                                // Particle beam effect
                                for (double d = 0; d < 10; d += 0.5) {
                                    Location particleLoc = startLoc.clone().add(direction.clone().multiply(d));
                                    org.bukkit.Particle.DustOptions dustOptions = new org.bukkit.Particle.DustOptions(
                                            org.bukkit.Color.fromRGB(0, 180, 255), 1.5f);
                                    entity.getWorld().spawnParticle(org.bukkit.Particle.DUST, particleLoc, 3, 0.1, 0.1, 0.1, 0, dustOptions);
                                }
                                
                                // Check if player is hit
                                if (player.getLocation().distance(entity.getLocation()) < 12) {
                                    player.damage(stats.getPhysicalDamage() * 1.5, entity);
                                    player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.0f);
                                }
                            }, 20L); // Delay for animation to play before effect
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Handle damage received by players and apply Armor/Magic Resist calculations
     * DamageTaken = IncomingPhysicalDamage × (100 / (100 + Armor))
     * DamageTaken = IncomingMagicDamage × (100 / (100 + MagicResist))
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamaged(EntityDamageEvent event) {
        // Only handle damage to players
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        
        // Get player profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;
        
        // Get armor and magic resist stats
        int armor = profile.getStats().getArmor();
        int magicResist = profile.getStats().getMagicResist();
        
        // Get original damage
        double originalDamage = event.getDamage();
        double reducedDamage = originalDamage;
        
        // Determine if damage is magical or physical
        boolean isMagical = isMagicalDamage(event);
        
        // Apply the appropriate damage reduction formula
        if (isMagical) {
            // Magic damage reduction
            reducedDamage = originalDamage * (100.0 / (100.0 + magicResist));
            
            if (plugin.isDebugEnabled(DebugSystem.COMBAT)) {
                plugin.debugLog(DebugSystem.COMBAT,"Magic damage to " + player.getName() + ": " +
                                     "Original: " + originalDamage + 
                                     ", Magic Resist: " + magicResist + 
                                     ", Reduced: " + reducedDamage);
            }
        } else {
            // Physical damage reduction
            reducedDamage = originalDamage * (100.0 / (100.0 + armor));
            
            if (plugin.isDebugEnabled(DebugSystem.COMBAT)) {
                plugin.debugLog(DebugSystem.COMBAT,"Physical damage to " + player.getName() + ": " +
                                     "Original: " + originalDamage + 
                                     ", Armor: " + armor + 
                                     ", Reduced: " + reducedDamage);
            }
        }
        
        // Set the reduced damage
        event.setDamage(reducedDamage);
        
        // Display the damage reduction if it's significant
        if ((originalDamage - reducedDamage) > 2.0 && 
            (event.getCause() != EntityDamageEvent.DamageCause.FALL)) {
            
            String defenseType = isMagical ? "§bMagic Resist" : "§aArmor";
            double percentReduction = ((originalDamage - reducedDamage) / originalDamage) * 100;
            
            // Only show message for significant reductions
            if (percentReduction >= 10) {
                player.sendMessage(defenseType + "§7 reduced damage by §f" + 
                                 String.format("%.1f", percentReduction) + "%");
            }
        }
    }
    
    /**
     * Determines if the damage is magical based on the damage cause
     */
    private boolean isMagicalDamage(EntityDamageEvent event) {
        switch (event.getCause()) {
            case MAGIC:
            case DRAGON_BREATH:
            case WITHER:
            case POISON:
            case LIGHTNING:
                return true;
            case ENTITY_ATTACK:
            case ENTITY_SWEEP_ATTACK:
            case PROJECTILE:
                // For entity attacks, we need to check if it's a custom mob that deals magic damage
                // For now, all vanilla entities deal physical damage
                return false;
            default:
                // Fall damage, fire, lava, etc. are all physical
                return false;
        }
    }
}