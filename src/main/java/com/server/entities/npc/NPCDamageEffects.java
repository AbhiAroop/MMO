package com.server.entities.npc;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry;

/**
 * Utilities for applying damage visual effects to NPCs using ProtocolLib
 */
public class NPCDamageEffects {
    
    private static ProtocolManager protocolManager;
    private static Plugin plugin;
    
    /**
     * Initialize the damage effects system
     * 
     * @param pluginInstance The main plugin instance
     */
    public static void initialize(Plugin pluginInstance) {
        plugin = pluginInstance;
        protocolManager = ProtocolLibrary.getProtocolManager();
    }
    
    /**
     * Apply the red damage effect to an entity
     * 
     * @param entity The entity to apply the effect to
     */
    public static void applyRedDamageEffect(Entity entity) {
        if (protocolManager == null || plugin == null) {
            plugin.getLogger().warning("Cannot apply damage effect - ProtocolLib manager not initialized");
            return;
        }
        
        if (!(entity instanceof LivingEntity)) {
            return;
        }

        // Get all players in range to send the packet to
        List<Player> players = getNearbyPlayers(entity, 50);
        if (players.isEmpty()) {
            return;
        }
        
        // Create the damage animation packet
        PacketContainer animationPacket = protocolManager.createPacket(PacketType.Play.Server.ANIMATION);
        animationPacket.getIntegers().write(0, entity.getEntityId());
        animationPacket.getIntegers().write(1, 1); // Damage animation

        // Create the entity metadata packet for red flash
        PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metadataPacket.getIntegers().write(0, entity.getEntityId());
        
        // Create the metadata value - using "hurt" time bytes
        WrappedDataWatcher watcher = new WrappedDataWatcher();
        
        // Entity hurt time (red flash)
        WrappedDataWatcher.WrappedDataWatcherObject hurtTimeObject = 
            new WrappedDataWatcher.WrappedDataWatcherObject(7, Registry.get(Integer.class));
        watcher.setObject(hurtTimeObject, 10); // Set hurt time to 10 ticks
            
        // Convert to the format needed for modern Minecraft versions
        List<WrappedDataValue> wrappedDataValueList = new ArrayList<>();
        
        // Create WrappedDataValue directly instead of using getWatchableObjects()
        wrappedDataValueList.add(new WrappedDataValue(
            7,  // Index for hurt time
            Registry.get(Integer.class),  // Serializer for integers
            10  // Hurt time value (10 ticks)
        ));
        
        metadataPacket.getDataValueCollectionModifier().write(0, wrappedDataValueList);

        // Send packets to all nearby players
        for (Player player : players) {
            protocolManager.sendServerPacket(player, animationPacket);
            protocolManager.sendServerPacket(player, metadataPacket);
        }

        // Reset the entity metadata after a delay
        new BukkitRunnable() {
            @Override
            public void run() {
                // Create reset packet
                PacketContainer resetPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
                resetPacket.getIntegers().write(0, entity.getEntityId());
                
                // Reset hurt time to 0 - use the same approach as when setting it
                List<WrappedDataValue> resetValues = new ArrayList<>();
                resetValues.add(new WrappedDataValue(
                    7,  // Index for hurt time
                    Registry.get(Integer.class),  // Serializer for integers
                    0   // Reset hurt time to 0
                ));
                
                resetPacket.getDataValueCollectionModifier().write(0, resetValues);
                
                // Send to all nearby players
                for (Player player : getNearbyPlayers(entity, 50)) {
                    protocolManager.sendServerPacket(player, resetPacket);
                }
            }
        }.runTaskLater(plugin, 10); // Reset after 10 ticks (0.5 seconds)
    }
    
    /**
     * Get all players near an entity
     * 
     * @param entity The center entity
     * @param range The range to check
     * @return List of nearby players
     */
    private static List<Player> getNearbyPlayers(Entity entity, double range) {
        List<Player> players = new ArrayList<>();
        for (Entity nearby : entity.getNearbyEntities(range, range, range)) {
            if (nearby.getType() == EntityType.PLAYER) {
                players.add((Player) nearby);
            }
        }
        return players;
    }
}