package com.server.events;

import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import static org.bukkit.persistence.PersistentDataType.INTEGER;

import com.server.Main;
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

        // Get base damage from stats
        double damage = profile.getStats().getPhysicalDamage();
        
        // Check held weapon for bonus damage and special effects
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        boolean procBonusDamage = false;
        
        if (heldItem != null && heldItem.hasItemMeta()) {
            ItemMeta meta = heldItem.getItemMeta();
            
            // Check for Apprentice's Edge passive - Precision Strike
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
            
            // Add weapon damage from lore
            if (meta.hasLore()) {
                for (String loreLine : meta.getLore()) {
                    if (loreLine.contains("Physical Damage:")) {
                        try {
                            String damageStr = loreLine.split("\\+")[1].trim();
                            damage += Double.parseDouble(damageStr);
                            break;
                        } catch (Exception e) {
                            continue;
                        }
                    }
                }
            }
        }

        // Apply critical hit multiplier if applicable
        boolean isCritical = !player.isOnGround() && player.getFallDistance() > 0.0f;
        if (isCritical) {
            damage *= profile.getStats().getCriticalDamage();
        }
        
        // Set the final damage
        event.setDamage(damage);
        
        // Display damage indicator if procced bonus damage
        if (procBonusDamage) {
            // Optional: Add visual effect for Precision Strike
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
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
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Magic damage to " + player.getName() + ": " +
                                     "Original: " + originalDamage + 
                                     ", Magic Resist: " + magicResist + 
                                     ", Reduced: " + reducedDamage);
            }
        } else {
            // Physical damage reduction
            reducedDamage = originalDamage * (100.0 / (100.0 + armor));
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Physical damage to " + player.getName() + ": " +
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