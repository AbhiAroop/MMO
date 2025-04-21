package com.server.display;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

public class DamageIndicatorManager implements Listener {
    private final Main plugin;

    public DamageIndicatorManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Main damage handler that shows the indicators after all reductions
     * Use MONITOR priority to capture the final damage after all reductions
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        LivingEntity entity = (LivingEntity) event.getEntity();
        double damage = event.getFinalDamage();
        
        // Get damage type symbol and color
        String symbol;
        String color;
        
        // For now, most damage is physical
        boolean isMagical = false;
        switch (event.getCause()) {
            case MAGIC:
            case DRAGON_BREATH:
            case WITHER:
            case POISON:
            case LIGHTNING:
                symbol = "✦"; // Magic damage symbol
                color = "§b"; // Aqua color
                isMagical = true;
                break;
            default:
                symbol = "⚔"; // Physical damage symbol
                color = "§c"; // Red color
                break;
        }
        
        // Determine players involved in combat
        Player victimPlayer = entity instanceof Player ? (Player) entity : null;
        Player attackerPlayer = null;
        
        // If this is player vs player combat, get the attacker
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent edbeEvent = (EntityDamageByEntityEvent) event;
            if (edbeEvent.getDamager() instanceof Player) {
                attackerPlayer = (Player) edbeEvent.getDamager();
            }
        }
        
        // Create different indicators for different audiences
        
        // 1. Create indicator for the VICTIM (if it's a player)
        if (victimPlayer != null) {
            // Victim sees damage in red
            spawnPersonalizedDamageIndicator(
                entity.getLocation(), 
                damage, 
                symbol, 
                color, // Keep the damage type color for the symbol 
                "§c-%.1f", // Red for damage numbers
                victimPlayer // Only visible to victim
            );
        }
        
        // 2. Create indicator for the ATTACKER (if it's a player)
        if (attackerPlayer != null) {
            // Attacker sees damage in grey
            spawnPersonalizedDamageIndicator(
                entity.getLocation(), 
                damage, 
                symbol,
                color, // Keep the damage type color for the symbol
                "§7%.1f", // Grey for damage numbers
                attackerPlayer // Only visible to attacker
            );
        }
        
        // 3. Create indicator for SPECTATORS (everyone else nearby)
        // Find nearby players who aren't the attacker or victim
        for (Entity nearbyEntity : entity.getNearbyEntities(20, 20, 20)) {
            if (nearbyEntity instanceof Player) {
                Player spectator = (Player) nearbyEntity;
                if (spectator != victimPlayer && spectator != attackerPlayer) {
                    // Spectators see damage in grey
                    spawnPersonalizedDamageIndicator(
                        entity.getLocation(), 
                        damage, 
                        symbol,
                        color, // Keep the damage type color for the symbol
                        "§7%.1f", // Grey for damage numbers
                        spectator // Only visible to this spectator
                    );
                }
            }
        }
        
        // Special handling for players to show damage reduction
        if (victimPlayer != null) {
            Player player = victimPlayer;
            
            // Check if we have the original damage stored
            if (player.hasMetadata("originalDamage")) {
                double originalDamage = player.getMetadata("originalDamage").get(0).asDouble();
                double reducedAmount = originalDamage - damage;
                
                // Only show reduction indicator if significant damage was reduced
                if (reducedAmount > 2.0) {
                    // Get player profile for stat information
                    Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
                    if (activeSlot != null) {
                        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
                        if (profile != null) {
                            // Get appropriate stat for the damage type
                            int defenseValue = isMagical ? 
                                profile.getStats().getMagicResist() : 
                                profile.getStats().getArmor();
                            
                            // Calculate percentage reduction
                            double percentReduction = (reducedAmount / originalDamage) * 100;
                            
                            // Only show for significant reductions (10%+)
                            if (percentReduction >= 10) {
                                String defenseType = isMagical ? "Magic Resist" : "Armor";
                                String defenseColor = isMagical ? "§b" : "§a"; // Aqua for magic, green for physical
                                
                                // Only show reduction indicator to the player who took damage
                                spawnPersonalizedReductionIndicator(
                                    player.getLocation(),
                                    reducedAmount,
                                    percentReduction,
                                    defenseValue,
                                    defenseType,
                                    defenseColor,
                                    player // Only visible to the defender
                                );
                            }
                        }
                    }
                }
                
                // Clean up metadata to prevent memory leaks
                player.removeMetadata("originalDamage", plugin);
            }
        }
    }

   /**
     * Spawns a damage indicator that's only visible to a specific player
     */
    private void spawnPersonalizedDamageIndicator(Location loc, double damage, String symbol, String symbolColor, 
                                            String format, Player viewer) {
        // Check what kind of viewer we have to customize the display
        boolean isVictim = format.contains("-");
        boolean isAttacker = !isVictim && viewer == plugin.getServer().getPlayer(viewer.getUniqueId());
        
        // For all cases, position the indicator above the target entity's head
        // This ensures consistent visibility for all viewers including the victim
        if (loc.getWorld().getNearbyEntities(loc, 0.5, 1, 0.5).stream().anyMatch(e -> e instanceof Player)) {
            // For player targets (including victim viewing self)
            loc = loc.add(
                Math.random() * 0.8 - 0.4, // Wider x spread (-0.4 to 0.4)
                1.8 + Math.random() * 0.4, // Higher position above head
                Math.random() * 0.8 - 0.4  // Wider z spread (-0.4 to 0.4)
            );
        } else {
            // For entity targets, use standard positioning
            loc = loc.add(
                Math.random() * 0.6 - 0.3, // Standard x spread
                0.5 + Math.random() * 0.5, // Standard y height
                Math.random() * 0.6 - 0.3  // Standard z spread
            );
        }
        
        // Create TextDisplay instead of ArmorStand
        TextDisplay indicator = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        
        // Set TextDisplay properties
        indicator.setBillboard(TextDisplay.Billboard.CENTER); // Always face the player
        indicator.setSeeThrough(true); // Can be seen through blocks
        indicator.setShadowed(true); // Add text shadow for better visibility
        indicator.setAlignment(TextDisplay.TextAlignment.CENTER); // Center align the text
        
        // Define who can see this indicator
        indicator.setVisibleByDefault(false); // Hide from everyone by default
        viewer.showEntity(plugin, indicator); // Only visible to specified player
        
        // Format damage text with different styling based on viewer type
        String displayText;
        if (isVictim) {
            // For victims: larger text, bold symbol, bright red numbers
            displayText = symbolColor + "§l" + symbol + " §c§l" + String.format("%.1f", damage);
        } else if (isAttacker) {
            // For attackers: standard format
            displayText = symbolColor + symbol + " " + String.format(format, damage);
        } else {
            // For spectators: slightly enhanced visibility with white numbers
            displayText = symbolColor + symbol + " §f" + String.format("%.1f", damage);
        }
        
        indicator.setText(displayText);
        
        // Set default background opacity to 0 (completely transparent)
        indicator.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
        
        // Make text slightly larger for better visibility
        // Different scale factors based on viewer role
        float scaleFactor;
        if (isVictim) {
            scaleFactor = 1.5f;  // Largest for victims
        } else if (isAttacker) {
            scaleFactor = 1.25f; // Standard for attackers
        } else {
            scaleFactor = 1.4f;  // Larger for spectators
        }
        
        org.joml.Vector3f scale = new org.joml.Vector3f(scaleFactor, scaleFactor, scaleFactor);
        org.bukkit.util.Transformation transformation = indicator.getTransformation();
        transformation = new org.bukkit.util.Transformation(
            transformation.getTranslation(), 
            transformation.getLeftRotation(), 
            scale, 
            transformation.getRightRotation()
        );
        indicator.setTransformation(transformation);
        
        // Animation and removal
        new BukkitRunnable() {
            private int ticks = 0;
            private final Location startLoc = indicator.getLocation();
            
            @Override
            public void run() {
                if (ticks >= 20) { // Remove after 1 second
                    indicator.remove();
                    this.cancel();
                    return;
                }
                
                // Different movement patterns for different viewer types
                Location newLoc;
                if (isVictim) {
                    // Victim damage moves up slower for better visibility
                    double yOffset = ticks * 0.05;
                    // Add a slight bounce effect using sine function
                    if (ticks > 5) {
                        yOffset += Math.sin((ticks - 5) * 0.3) * 0.03;
                    }
                    newLoc = startLoc.clone().add(0, yOffset, 0);
                } else if (isAttacker) {
                    // Attacker sees standard upward movement
                    newLoc = startLoc.clone().add(0, ticks * 0.05, 0);
                } else {
                    // Spectators see enhanced movement for better visibility
                    newLoc = startLoc.clone().add(0, ticks * 0.06, 0);
                }
                indicator.teleport(newLoc);
                
                // Gradually decrease opacity, but keep victim text visible longer
                float opacity;
                if (isVictim) {
                    opacity = 1.0f - (ticks / 25.0f); // Slower fade for victims
                } else {
                    opacity = 1.0f - (ticks / 20.0f);
                }
                indicator.setTextOpacity((byte)(opacity * 255)); // Convert float 0-1 to byte 0-255
                
                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /**
     * Spawns a reduction indicator that's only visible to a specific player
     */
    private void spawnPersonalizedReductionIndicator(Location loc, double reducedAmount, double percentage, 
                                        int defenseValue, String defenseType, String color, Player viewer) {
        // Position the reduction indicator in the player's field of view
        // Get the player's look direction but only use horizontal component
        Vector lookDir = viewer.getLocation().getDirection().clone();
        lookDir.setY(0);  // Zero out the Y component for horizontal calculation
        lookDir.normalize().multiply(-1.0); // Reverse direction (in front of player)
        
        // Create a new location in front and above the player
        // Increased distance from 1.5 to 3.0 blocks for better visibility
        loc = viewer.getEyeLocation().clone().add(
            lookDir.getX() * 3.0 + (Math.random() * 0.5 - 0.25), // 3.0 blocks in front, slight randomness
            0.3 + Math.random() * 0.3,                           // Slightly above eye level
            lookDir.getZ() * 3.0 + (Math.random() * 0.5 - 0.25)  // 3.0 blocks in front, slight randomness
        );
        
        // Create TextDisplay instead of ArmorStand
        TextDisplay indicator = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        
        // Set TextDisplay properties
        indicator.setBillboard(TextDisplay.Billboard.CENTER); // Always face the player
        indicator.setSeeThrough(true); // Can be seen through blocks
        indicator.setShadowed(true); // Add text shadow for better visibility
        indicator.setAlignment(TextDisplay.TextAlignment.CENTER); // Center align the text
        
        // Make this indicator only visible to the specific player
        indicator.setVisibleByDefault(false);
        viewer.showEntity(plugin, indicator);
        
        // Format reduction text - enhanced for better visibility
        String displayText = String.format("%s§l-%.1f §f§l(%.0f%%)", color, reducedAmount, percentage);
        // Add a shield symbol for better visual impact
        displayText = "§f⛨ " + displayText;
        indicator.setText(displayText);
        
        // Set default background opacity to 0 (completely transparent)
        indicator.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
        
        // Make the text slightly larger for better visibility at greater distance
        org.joml.Vector3f scale = new org.joml.Vector3f(1.4f, 1.4f, 1.4f);
        org.bukkit.util.Transformation transformation = indicator.getTransformation();
        transformation = new org.bukkit.util.Transformation(
            transformation.getTranslation(), 
            transformation.getLeftRotation(), 
            scale, 
            transformation.getRightRotation()
        );
        indicator.setTransformation(transformation);
        
        // Animation and removal
        new BukkitRunnable() {
            private int ticks = 0;
            private final Location startLoc = indicator.getLocation();
            
            @Override
            public void run() {
                if (ticks >= 30) { // Display slightly longer (1.5 seconds)
                    indicator.remove();
                    this.cancel();
                    return;
                }
                
                // Make the reduction indicator stand out with a slight floating effect
                double yOffset = ticks * 0.03;
                // Add a gentle floating motion using sine
                yOffset += Math.sin(ticks * 0.2) * 0.02;
                Location newLoc = startLoc.clone().add(0, yOffset, 0);
                indicator.teleport(newLoc);
                
                // Slower fade out for reduction indicators
                float opacity = 1.0f - (ticks / 35.0f);
                indicator.setTextOpacity((byte)(opacity * 255)); // Convert float 0-1 to byte 0-255
                
                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
