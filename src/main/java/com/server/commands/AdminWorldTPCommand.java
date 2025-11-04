package com.server.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.server.Main;

/**
 * Admin command to create and teleport to admin build worlds
 * Creates flat worlds with vanilla default terrain layers (62 dirt + 1 bedrock + 1 grass)
 */
public class AdminWorldTPCommand implements CommandExecutor, TabCompleter {
    
    private final Main plugin;
    
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
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                handleCreate(player, args);
                break;
                
            case "tp":
            case "teleport":
                handleTeleport(player, args);
                break;
                
            case "delete":
            case "remove":
                handleDelete(player, args);
                break;
                
            case "list":
                handleList(player);
                break;
                
            default:
                player.sendMessage("§cUnknown subcommand: " + subCommand);
                sendHelp(player);
                break;
        }
        
        return true;
    }
    
    private void sendHelp(Player player) {
        player.sendMessage("§e§l[Admin World Commands]");
        player.sendMessage("§7/adminworld create <name> §f- Create a new admin build world");
        player.sendMessage("§7/adminworld tp <name> §f- Teleport to an admin build world");
        player.sendMessage("§7/adminworld delete <name> §f- Delete an admin build world");
        player.sendMessage("§7/adminworld list §f- List all admin build worlds");
    }
    
    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /adminworld create <name>");
            player.sendMessage("§7Example: /adminworld create test_builds");
            return;
        }
        
        String worldName = "admin_" + args[1];
        
        // Check if world already exists
        if (Bukkit.getWorld(worldName) != null) {
            player.sendMessage("§cAdmin world '§e" + args[1] + "§c' already exists!");
            player.sendMessage("§7Use §e/adminworld tp " + args[1] + "§7 to teleport to it.");
            return;
        }
        
        player.sendMessage("§eCreating admin world '§6" + args[1] + "§e'...");
        
        // Create the world
        World world = createAdminWorld(worldName);
        
        if (world == null) {
            player.sendMessage("§cFailed to create admin world!");
            return;
        }
        
        player.sendMessage("§a✓ Admin world '§2" + args[1] + "§a' created successfully!");
        player.sendMessage("§7Use §e/adminworld tp " + args[1] + "§7 to teleport to it.");
    }
    
    private void handleTeleport(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /adminworld tp <name>");
            player.sendMessage("§7Example: /adminworld tp test_builds");
            return;
        }
        
        String worldName = "admin_" + args[1];
        
        // Get the world
        World world = Bukkit.getWorld(worldName);
        
        if (world == null) {
            player.sendMessage("§cAdmin world '§e" + args[1] + "§c' does not exist!");
            player.sendMessage("§7Use §e/adminworld create " + args[1] + "§7 to create it first.");
            return;
        }
        
        // Get spawn location and ensure it's safe
        Location spawnLocation = world.getSpawnLocation();
        
        // Make sure we're on a solid block (Y should be at least 64 for superflat)
        if (spawnLocation.getY() < 64) {
            spawnLocation.setY(64); // Default spawn height for superflat (on top of grass)
        }
        
        // Center the player on the block
        spawnLocation.setX(spawnLocation.getBlockX() + 0.5);
        spawnLocation.setZ(spawnLocation.getBlockZ() + 0.5);
        
        // Load chunk first
        int chunkX = spawnLocation.getBlockX() >> 4;
        int chunkZ = spawnLocation.getBlockZ() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            world.loadChunk(chunkX, chunkZ);
        }
        
        // Use vanilla execute command for cross-world teleportation (same as islands)
        // Format: /execute in <world> run tp <player> <x> <y> <z> <yaw> <pitch>
        // World name needs minecraft: prefix for execute command
        java.text.DecimalFormat df = new java.text.DecimalFormat("0.##");
        String worldKey = "minecraft:" + world.getName().toLowerCase();
        String command = String.format("execute in %s run tp %s %s %s %s %s %s",
            worldKey,
            player.getName(),
            df.format(spawnLocation.getX()),
            df.format(spawnLocation.getY()),
            df.format(spawnLocation.getZ()),
            df.format(spawnLocation.getYaw()),
            df.format(spawnLocation.getPitch())
        );
        
        plugin.getLogger().info("[AdminWorld] Executing teleport command: /" + command);
        boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        
        if (success) {
            player.sendMessage("§a✓ Teleported to admin world '§2" + args[1] + "§a'!");
        } else {
            plugin.getLogger().warning("[AdminWorld] Teleport failed for player " + player.getName());
            player.sendMessage("§cFailed to teleport to admin world!");
        }
    }
    
    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /adminworld delete <name>");
            player.sendMessage("§7Example: /adminworld delete test_builds");
            return;
        }
        
        String worldName = "admin_" + args[1];
        
        // Get the world
        World world = Bukkit.getWorld(worldName);
        
        if (world == null) {
            player.sendMessage("§cAdmin world '§e" + args[1] + "§c' does not exist!");
            return;
        }
        
        player.sendMessage("§eDeleting admin world '§6" + args[1] + "§e'...");
        
        // Get all players in the world
        List<Player> playersInWorld = new ArrayList<>(world.getPlayers());
        
        // Teleport all players out of the world first (to main world spawn)
        World mainWorld = Bukkit.getWorlds().get(0); // Get the first world (usually "world")
        Location mainSpawn = mainWorld.getSpawnLocation();
        
        if (!playersInWorld.isEmpty()) {
            player.sendMessage("§7Teleporting " + playersInWorld.size() + " player(s) out of the world...");
            for (Player p : playersInWorld) {
                p.teleport(mainSpawn);
                if (!p.equals(player)) {
                    p.sendMessage("§eThe admin world you were in has been deleted.");
                }
            }
        }
        
        // Unload the world
        plugin.getLogger().info("[AdminWorld] Unloading world: " + worldName);
        boolean unloaded = Bukkit.unloadWorld(world, false); // false = don't save
        
        if (!unloaded) {
            player.sendMessage("§cFailed to unload the world! Cannot delete.");
            return;
        }
        
        // Delete the world folder
        try {
            java.io.File worldFolder = world.getWorldFolder();
            deleteWorldFolder(worldFolder);
            
            plugin.getLogger().info("[AdminWorld] Deleted world: " + worldName);
            player.sendMessage("§a✓ Admin world '§2" + args[1] + "§a' has been deleted!");
            
        } catch (Exception e) {
            plugin.getLogger().severe("[AdminWorld] Error deleting world folder: " + e.getMessage());
            player.sendMessage("§cFailed to delete world folder! Check console for errors.");
            e.printStackTrace();
        }
    }
    
    /**
     * Recursively deletes a world folder
     */
    private void deleteWorldFolder(java.io.File folder) {
        if (folder.exists()) {
            java.io.File[] files = folder.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    if (file.isDirectory()) {
                        deleteWorldFolder(file);
                    } else {
                        file.delete();
                    }
                }
            }
            folder.delete();
        }
    }
    
    private void handleList(Player player) {
        List<String> adminWorlds = new ArrayList<>();
        
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().startsWith("admin_")) {
                adminWorlds.add(world.getName().substring(6)); // Remove "admin_" prefix
            }
        }
        
        if (adminWorlds.isEmpty()) {
            player.sendMessage("§eNo admin worlds exist yet.");
            player.sendMessage("§7Use §e/adminworld create <name>§7 to create one.");
            return;
        }
        
        player.sendMessage("§e§l[Admin Build Worlds]");
        for (String worldName : adminWorlds) {
            player.sendMessage("§7- §f" + worldName);
        }
        player.sendMessage("§7Use §e/adminworld tp <name>§7 to teleport.");
    }
    
    /**
     * Creates an admin world with vanilla flat terrain (62 dirt layers)
     */
    private World createAdminWorld(String worldName) {
        try {
            // Create new flat world with vanilla default layers
            // WorldType.FLAT automatically creates: 1 bedrock, 2 dirt, 1 grass (default superflat)
            // For proper 62 dirt layers, we need to use JSON format for generator settings
            WorldCreator creator = new WorldCreator(worldName);
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            
            // Use proper JSON format for flat world generation settings
            // This creates the vanilla superflat preset with correct layers
            String flatSettings = "{" +
                "\"layers\": [" +
                    "{\"block\":\"minecraft:bedrock\",\"height\":1}," +
                    "{\"block\":\"minecraft:dirt\",\"height\":62}," +
                    "{\"block\":\"minecraft:grass_block\",\"height\":1}" +
                "]," +
                "\"biome\":\"minecraft:plains\"" +
            "}";
            
            creator.generatorSettings(flatSettings);
            
            plugin.getLogger().info("[AdminWorld] Creating admin build world: " + worldName);
            
            World world = creator.createWorld();
            
            if (world != null) {
                plugin.getLogger().info("[AdminWorld] World created, setting game rules...");
                
                // Set world rules for building
                world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
                world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
                world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
                world.setTime(6000); // Set to noon
                
                plugin.getLogger().info("[AdminWorld] Admin build world created successfully: " + worldName);
                plugin.getLogger().info("[AdminWorld] World type: " + world.getWorldType());
                
                return world;
            } else {
                plugin.getLogger().severe("[AdminWorld] Failed to create admin build world: " + worldName);
                return null;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[AdminWorld] Error creating admin build world: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("create");
            completions.add("tp");
            completions.add("delete");
            completions.add("list");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("teleport") || 
                                        args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("remove"))) {
            // Suggest existing admin worlds for tp and delete commands
            for (World world : Bukkit.getWorlds()) {
                if (world.getName().startsWith("admin_")) {
                    completions.add(world.getName().substring(6)); // Remove "admin_" prefix
                }
            }
        }
        
        // Filter based on what the player has typed
        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        
        return completions;
    }
}
