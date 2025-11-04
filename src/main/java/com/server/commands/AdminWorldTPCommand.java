package com.server.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.server.Main;

/**
 * Admin command to teleport to the admin build world
 * Creates a flat world with vanilla default terrain layers for testing and building
 */
public class AdminWorldTPCommand implements CommandExecutor {
    
    private final Main plugin;
    private static final String ADMIN_WORLD_NAME = "admin_build_world";
    
    public AdminWorldTPCommand(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("mmo.admin.worldtp")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        
        // Get or create the admin world
        World adminWorld = getOrCreateAdminWorld();
        
        if (adminWorld == null) {
            player.sendMessage("§cFailed to create or load the admin build world!");
            return true;
        }
        
        // Teleport player to spawn location
        Location spawnLocation = adminWorld.getSpawnLocation();
        player.teleport(spawnLocation);
        
        player.sendMessage("§a✓ Teleported to Admin Build World!");
        player.sendMessage("§7Use this world for testing and creating custom buildings.");
        
        return true;
    }
    
    /**
     * Gets the admin world if it exists, or creates it with vanilla flat terrain
     */
    private World getOrCreateAdminWorld() {
        // Check if world already exists
        World existingWorld = Bukkit.getWorld(ADMIN_WORLD_NAME);
        if (existingWorld != null) {
            return existingWorld;
        }
        
        // Create new flat world with vanilla default layers
        // Default superflat preset: 1 bedrock, 2 dirt, 1 grass
        WorldCreator creator = new WorldCreator(ADMIN_WORLD_NAME);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        // Generator settings for vanilla flat world with default layers
        // Format: minecraft:bedrock,2*minecraft:dirt,minecraft:grass_block;minecraft:plains
        creator.generatorSettings("{\"layers\": [{\"block\": \"minecraft:bedrock\", \"height\": 1}, {\"block\": \"minecraft:dirt\", \"height\": 2}, {\"block\": \"minecraft:grass_block\", \"height\": 1}], \"biome\":\"minecraft:plains\"}");
        
        plugin.getLogger().info("Creating admin build world: " + ADMIN_WORLD_NAME);
        
        try {
            World world = creator.createWorld();
            
            if (world != null) {
                // Set world rules for building
                world.setGameRuleValue("doDaylightCycle", "false");
                world.setGameRuleValue("doWeatherCycle", "false");
                world.setGameRuleValue("doMobSpawning", "false");
                world.setTime(6000); // Set to noon
                
                plugin.getLogger().info("Admin build world created successfully!");
                return world;
            } else {
                plugin.getLogger().severe("Failed to create admin build world!");
                return null;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating admin build world: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
