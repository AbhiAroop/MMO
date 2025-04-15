package com.server.events;

import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import static org.bukkit.persistence.PersistentDataType.INTEGER;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

public class CombatListener implements Listener {

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
}