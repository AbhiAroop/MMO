package com.server.display;

import org.bukkit.ChatColor;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;

public class MobDisplayManager implements Listener {
    private static final String MOB_TAG_IDENTIFIER = "MMO_MOB";

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        // Check for both hostile and passive mobs
        if (entity instanceof Monster || entity instanceof Animals) {
            LivingEntity livingEntity = (LivingEntity) entity;
            if (!hasMMOTag(livingEntity)) return;

            // Update display after one tick to ensure damage is applied
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!entity.isDead()) {
                        updateMobDisplay(livingEntity);
                    }
                }
            }.runTask(Main.getInstance());
        }
    }

    @EventHandler
    public void onEntitySpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        // Check for both hostile and passive mobs
        if (entity instanceof Monster || entity instanceof Animals) {
            setupMobDisplay(entity);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            // Check for both hostile and passive mobs
            if (entity instanceof Monster || entity instanceof Animals) {
                LivingEntity livingEntity = (LivingEntity) entity;
                if (!livingEntity.isCustomNameVisible() || !hasMMOTag(livingEntity)) {
                    entity.remove(); // Despawn mobs without proper tag
                }
            }
        }
    }

    private void setupMobDisplay(LivingEntity entity) {
        // Set format name
        String mobType = (entity instanceof Monster) ? "§c❈" : "§a❀"; // Hostile vs Passive indicator
        
        updateMobDisplay(entity, mobType);
        entity.setCustomNameVisible(true);
        
        // Add identifier to metadata
        entity.setMetadata(MOB_TAG_IDENTIFIER, new FixedMetadataValue(Main.getInstance(), true));
    }

    public void updateMobDisplay(LivingEntity entity) {
        String mobType = (entity instanceof Monster) ? "§c❈" : "§a❀";
        updateMobDisplay(entity, mobType);
    }

    private void updateMobDisplay(LivingEntity entity, String mobType) {
        String mobName = formatMobName(entity.getType().toString());
        int level = 1;
        double maxHealth = entity.getMaxHealth();
        double currentHealth = Math.max(0, entity.getHealth()); // Ensure health doesn't show negative
        
        // Format: [Lv.1] ❈ Zombie ❤ 20/20  (❈ for hostile, ❀ for passive)
        String displayName = String.format("%s[Lv.%d] %s %s%s %s❤ %.1f/%.1f",
            ChatColor.GRAY,
            level,
            mobType,
            ChatColor.WHITE,
            mobName,
            ChatColor.RED,
            currentHealth,
            maxHealth
        );

        entity.setCustomName(displayName);
    }

    private String formatMobName(String entityType) {
        // Convert CAVE_SPIDER to Cave Spider
        String[] words = entityType.toLowerCase().split("_");
        StringBuilder formattedName = new StringBuilder();
        
        for (String word : words) {
            if (formattedName.length() > 0) formattedName.append(" ");
            formattedName.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        
        return formattedName.toString();
    }

    private boolean hasMMOTag(LivingEntity entity) {
        return entity.hasMetadata(MOB_TAG_IDENTIFIER);
    }
}