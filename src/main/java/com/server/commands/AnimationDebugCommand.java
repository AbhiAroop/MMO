package com.server.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.entities.mobs.CustomMob;
import com.server.entities.mobs.colossus.RunemarkColossus;

public class AnimationDebugCommand implements CommandExecutor {

    private final Main plugin;
    private final Map<UUID, BukkitTask> autoAnimationTasks = new HashMap<>();
    
    public AnimationDebugCommand(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 1) {
            player.sendMessage("§cUsage: /animdebug <animation> [entityType]");
            player.sendMessage("§7Available animations: idle, attack, hurt, walk, debast, special2, death");
            player.sendMessage("§7Special commands: colossusauto, verdigranauto, duneetchedauto, colossusattack, fanghowl, diagnose");
            player.sendMessage("§7Entity types: nearest, target");
            return true;
        }
        
        String animationName = args[0].toLowerCase();
        String entityType = args.length > 1 ? args[1].toLowerCase() : "nearest";
        
        // Special handlers for colossus animations
        if ("colossusauto".equalsIgnoreCase(animationName)) {
            return toggleColossusAutoAttack(player, "runemark_colossus");
        }
        
        if ("verdigranauto".equalsIgnoreCase(animationName)) {
            return toggleColossusAutoAttack(player, "verdigran_colossus");
        }
        
        if ("duneetchedauto".equalsIgnoreCase(animationName)) {
            return toggleColossusAutoAttack(player, "duneetched_colossus");
        }
        
        if ("colossusattack".equalsIgnoreCase(animationName)) {
            // Get the type of colossus from args if provided
            String colossusType = args.length > 1 ? args[1].toLowerCase() : "runemark_colossus";
            
            // Map the command argument to the actual metadata key
            String metadataKey;
            switch (colossusType) {
                case "verdigran":
                    metadataKey = "verdigran_colossus";
                    break;
                case "duneetched":
                    metadataKey = "duneetched_colossus";
                    break;
                default:
                    metadataKey = "runemark_colossus";
            }
            
            LivingEntity colossusEntity = findEntityWithMetadata(player, 20, metadataKey);
            if (colossusEntity != null) {
                plugin.getCustomEntityManager().playAnimation(colossusEntity, "attack1");
                player.sendMessage("§aForced attack1 animation on " + getColossusDisplayName(metadataKey));
                return true;
            } else {
                player.sendMessage("§cNo " + getColossusDisplayName(metadataKey) + " found within 20 blocks!");
                return true;
            }
        }

        if ("colossusslam".equalsIgnoreCase(animationName) || "debast".equalsIgnoreCase(animationName)) {
            // Get the type of colossus from args if provided
            String colossusType = args.length > 1 ? args[1].toLowerCase() : "runemark_colossus";
            
            // Map the command argument to the actual metadata key
            String metadataKey;
            switch (colossusType) {
                case "verdigran":
                    metadataKey = "verdigran_colossus";
                    break;
                case "duneetched":
                    metadataKey = "duneetched_colossus";
                    break;
                default:
                    metadataKey = "runemark_colossus";
            }
            
            LivingEntity colossusEntity = findEntityWithMetadata(player, 20, metadataKey);
            if (colossusEntity != null) {
                // Get the RunemarkColossus from registry
                CustomMob mobType = plugin.getCustomEntityManager().getMobRegistry().getMobType(
                    metadataKey.equals("verdigran_colossus") ? "verdigrancolossus" :
                    metadataKey.equals("duneetched_colossus") ? "duneetchedcolossus" : 
                    "runemarkcolossus"
                );
                
                if (mobType instanceof RunemarkColossus) {
                    ((RunemarkColossus) mobType).playSpecialAbility(colossusEntity, 1);
                    player.sendMessage("§aForced debast slam ability on " + getColossusDisplayName(metadataKey));
                } else {
                    // Fallback to regular animation
                    plugin.getCustomEntityManager().playAnimation(colossusEntity, "debast");
                    player.sendMessage("§aForced debast animation on " + getColossusDisplayName(metadataKey));
                }
                return true;
            } else {
                player.sendMessage("§cNo " + getColossusDisplayName(metadataKey) + " found within 20 blocks!");
                return true;
            }
        }

        
        if ("specialability".equalsIgnoreCase(animationName)) {
            int abilityIndex = args.length > 1 ? Integer.parseInt(args[1]) : 1;
            LivingEntity colossusEntity = findEntityWithMetadata(player, 20, "runemark_colossus");
            if (colossusEntity != null) {
                // Get the RunemarkColossus from registry
                CustomMob mobType = plugin.getCustomEntityManager().getMobRegistry().getMobType("runemarkcolossus");
                
                if (mobType instanceof RunemarkColossus) {
                    ((RunemarkColossus) mobType).playSpecialAbility(colossusEntity, abilityIndex);
                    player.sendMessage("§aForced special ability " + abilityIndex + " on Runemark Colossus");
                } else {
                    // Fallback to regular animation
                    plugin.getCustomEntityManager().playAnimation(colossusEntity, "special" + abilityIndex);
                    player.sendMessage("§aForced special" + abilityIndex + " animation on Runemark Colossus");
                }
                return true;
            } else {
                player.sendMessage("§cNo Runemark Colossus found within 20 blocks!");
                return true;
            }
        }
        
        // Find the target entity
        LivingEntity targetEntity = null;
        
        if ("nearest".equals(entityType)) {
            double closestDistance = Double.MAX_VALUE;
            
            for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
                if (entity instanceof LivingEntity && plugin.getCustomEntityManager().isCustomMob(entity)) {
                    double distance = player.getLocation().distance(entity.getLocation());
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        targetEntity = (LivingEntity) entity;
                    }
                }
            }
        } else if ("target".equals(entityType)) {
            // Get the entity player is looking at
            targetEntity = getTargetEntity(player, 10);
        }
        
        if (targetEntity == null) {
            player.sendMessage("§cNo custom mob found nearby!");
            return true;
        }
        
        // Check if it's a custom mob
        if (!plugin.getCustomEntityManager().isCustomMob(targetEntity)) {
            player.sendMessage("§cTarget entity is not a custom mob!");
            return true;
        }
        
        // Handle diagnose command if specified
        if ("diagnose".equals(animationName)) {
            diagnoseEntityAnimations(targetEntity, player);
            return true;
        }
        
        // Play the animation
        plugin.getCustomEntityManager().playAnimation(targetEntity, animationName);
        player.sendMessage("§aPlaying animation §e" + animationName + "§a on entity §e" + targetEntity.getType() + "§a!");
        
        return true;
    }
    
    /**
     * Diagnose entity animations and output information
     */
    private void diagnoseEntityAnimations(LivingEntity entity, Player player) {
        plugin.debugLog(DebugSystem.ANIMATION,"=== Animation Diagnosis for Entity " + entity.getUniqueId() + " ===");
        plugin.debugLog(DebugSystem.ANIMATION,"Entity Type: " + entity.getType());
        
        // Check metadata to identify mob type
        StringBuilder metadataInfo = new StringBuilder("Metadata: ");
        for (String key : new String[] {"runemark_colossus", "verdigran_colossus", "duneetched_colossus", "duskhollow_fang"}) {
            if (entity.hasMetadata(key)) {
                metadataInfo.append(key).append("=true ");
            }
        }
        plugin.debugLog(DebugSystem.ANIMATION,metadataInfo.toString());
        
        // Attempt to play core animations and log results
        String[] coreAnimations = {"idle", "walk", "hurt", "attack", "attack1", "attack2", "debast", "special2", "death"};
        plugin.debugLog(DebugSystem.ANIMATION,"Testing core animations...");
        
        for (String anim : coreAnimations) {
            try {
                plugin.getCustomEntityManager().playAnimation(entity, anim);
                plugin.debugLog(DebugSystem.ANIMATION,"Animation '" + anim + "': Attempt made (check if visible in-game)");
            } catch (Exception e) {
                plugin.debugLog(DebugSystem.ANIMATION,"Animation '" + anim + "': Failed - " + e.getMessage());
            }
        }
        
        player.sendMessage("§aDiagnosing animations for entity. Check server console for results.");
    }
    
    /**
     * Toggle automatic attack1 animation for the nearest Runemark Colossus
     */
    private boolean toggleColossusAutoAttack(Player player) {
        LivingEntity colossusEntity = findEntityWithMetadata(player, 20, "runemark_colossus");
        if (colossusEntity == null) {
            player.sendMessage("§cNo Runemark Colossus found within 20 blocks!");
            return true;
        }
        
        UUID entityId = colossusEntity.getUniqueId();
        
        // Check if we already have an auto-animation task for this entity
        if (autoAnimationTasks.containsKey(entityId)) {
            // Stop the existing task
            autoAnimationTasks.get(entityId).cancel();
            autoAnimationTasks.remove(entityId);
            player.sendMessage("§cDisabled auto-attack animation for Runemark Colossus");
            
            // Remove debug metadata
            colossusEntity.removeMetadata("debug_auto_attack", plugin);
            return true;
        }
        
        // Start a new auto-animation task
        player.sendMessage("§aEnabled auto-attack animation for Runemark Colossus");
        colossusEntity.setMetadata("debug_auto_attack", new FixedMetadataValue(plugin, true));
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!colossusEntity.isValid() || colossusEntity.isDead()) {
                    cancel();
                    autoAnimationTasks.remove(entityId);
                    return;
                }
                
                // Play the attack1 animation directly
                plugin.getCustomEntityManager().playAnimation(colossusEntity, "attack1");
                
                // Log for debugging
                plugin.debugLog(DebugSystem.ANIMATION,"Auto-playing attack1 animation on Runemark Colossus: " + entityId);
            }
        }.runTaskTimer(plugin, 0L, 40L); // Every 2 seconds (40 ticks)
        
        autoAnimationTasks.put(entityId, task);
        return true;
    }
    
    /**
     * Find an entity with a specific metadata key
     */
    private LivingEntity findEntityWithMetadata(Player player, int radius, String metadataKey) {
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity && entity.hasMetadata(metadataKey)) {
                return (LivingEntity) entity;
            }
        }
        return null;
    }
    
    /**
     * Get the entity the player is looking at
     */
    private LivingEntity getTargetEntity(Player player, int maxDistance) {
        // Simple ray-casting to find target entity
        for (double d = 0; d <= maxDistance; d += 0.5) {
            org.bukkit.util.Vector direction = player.getLocation().getDirection().multiply(d);
            org.bukkit.Location checkLoc = player.getEyeLocation().add(direction);
            
            for (Entity entity : player.getWorld().getNearbyEntities(checkLoc, 1, 1, 1)) {
                if (entity instanceof LivingEntity && entity != player) {
                    return (LivingEntity) entity;
                }
            }
        }
        
        return null;
    }

    /**
     * Toggle automatic attack1 animation for a specific colossus type
     */
    private boolean toggleColossusAutoAttack(Player player, String metadataKey) {
        LivingEntity colossusEntity = findEntityWithMetadata(player, 20, metadataKey);
        if (colossusEntity == null) {
            player.sendMessage("§cNo " + getColossusDisplayName(metadataKey) + " found within 20 blocks!");
            return true;
        }
        
        UUID entityId = colossusEntity.getUniqueId();
        
        // Check if we already have an auto-animation task for this entity
        if (autoAnimationTasks.containsKey(entityId)) {
            // Stop the existing task
            autoAnimationTasks.get(entityId).cancel();
            autoAnimationTasks.remove(entityId);
            player.sendMessage("§cDisabled auto-attack animation for " + getColossusDisplayName(metadataKey));
            
            // Remove debug metadata
            colossusEntity.removeMetadata("debug_auto_attack", plugin);
            return true;
        }
        
        // Start a new auto-animation task
        player.sendMessage("§aEnabled auto-attack animation for " + getColossusDisplayName(metadataKey));
        colossusEntity.setMetadata("debug_auto_attack", new FixedMetadataValue(plugin, true));
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!colossusEntity.isValid() || colossusEntity.isDead()) {
                    cancel();
                    autoAnimationTasks.remove(entityId);
                    return;
                }
                
                // Play the attack1 animation directly
                plugin.getCustomEntityManager().playAnimation(colossusEntity, "attack1");
                
                // Log for debugging
                plugin.debugLog(DebugSystem.ANIMATION,"Auto-playing attack1 animation on " + getColossusDisplayName(metadataKey) + ": " + entityId);
            }
        }.runTaskTimer(plugin, 0L, 40L); // Every 2 seconds (40 ticks)
        
        autoAnimationTasks.put(entityId, task);
        return true;
    }

    /**
     * Get a display name for a colossus type based on its metadata key
     */
    private String getColossusDisplayName(String metadataKey) {
        switch (metadataKey) {
            case "verdigran_colossus":
                return "Verdigran Colossus";
            case "duneetched_colossus":
                return "Duneetched Colossus";
            default:
                return "Runemark Colossus";
        }
    }
    
    /**
     * Clean up any running tasks when plugin is disabled
     */
    public void cleanup() {
        for (BukkitTask task : autoAnimationTasks.values()) {
            task.cancel();
        }
        autoAnimationTasks.clear();
    }
}