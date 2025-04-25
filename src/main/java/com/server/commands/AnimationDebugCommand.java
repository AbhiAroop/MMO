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
            player.sendMessage("§7Available animations: idle, attack, hurt, walk, special1, special2, death");
            player.sendMessage("§7Special commands: colossusauto, colossusattack, diagnose");
            player.sendMessage("§7Entity types: nearest, target");
            return true;
        }
        
        String animationName = args[0].toLowerCase();
        String entityType = args.length > 1 ? args[1].toLowerCase() : "nearest";
        
        // Special handler for colossus auto attack animation
        if ("colossusauto".equalsIgnoreCase(animationName)) {
            return toggleColossusAutoAttack(player);
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

        if ("colossusattack".equalsIgnoreCase(animationName)) {
            plugin.getLogger().info("Debug command: Forcing Runemark Colossus attack1 animation");
            LivingEntity colossusEntity = findRunemarkColossus(player, 20);
            if (colossusEntity != null) {
                plugin.getCustomEntityManager().playAnimation(colossusEntity, "attack1");
                player.sendMessage("§aForced attack1 animation on Runemark Colossus");
                return true;
            } else {
                player.sendMessage("§cNo Runemark Colossus found within 20 blocks!");
                return true;
            }
        }
        
        // Diagnose animations
        if ("diagnose".equals(animationName)) {
            plugin.getCustomEntityManager().diagnoseAnimations(targetEntity);
            player.sendMessage("§aDiagnosing animations for entity " + targetEntity.getUniqueId() + ". Check console for results.");
            return true;
        }
        
        // Play the animation
        plugin.getCustomEntityManager().playAnimation(targetEntity, animationName);
        player.sendMessage("§aPlaying animation §e" + animationName + "§a on entity §e" + targetEntity.getType() + "§a!");
        
        return true;
    }
    
    /**
     * Toggle automatic attack1 animation for the nearest Runemark Colossus
     * This helps debug and fix animation issues by repeatedly playing the animation
     */
    private boolean toggleColossusAutoAttack(Player player) {
        LivingEntity colossusEntity = findRunemarkColossus(player, 20);
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
                plugin.getLogger().info("Auto-playing attack1 animation on Runemark Colossus: " + entityId);
            }
        }.runTaskTimer(plugin, 0L, 40L); // Every 2 seconds (40 ticks)
        
        autoAnimationTasks.put(entityId, task);
        return true;
    }
    
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

    private LivingEntity findRunemarkColossus(Player player, int radius) {
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity && entity.hasMetadata("runemark_colossus")) {
                return (LivingEntity) entity;
            }
        }
        return null;
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