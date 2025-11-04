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
        
        // Teleport player to spawn location using vanilla teleport
        Location spawnLocation = world.getSpawnLocation().add(0.5, 0, 0.5); // Center of block
        
        // Use Bukkit's dispatchCommand for vanilla teleport behavior
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
            "tp " + player.getName() + " " + 
            spawnLocation.getX() + " " + 
            spawnLocation.getY() + " " + 
            spawnLocation.getZ() + " " +
            spawnLocation.getYaw() + " " +
            spawnLocation.getPitch() + " " +
            world.getName());
        
        player.sendMessage("§a✓ Teleported to admin world '§2" + args[1] + "§a'!");
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
            // Vanilla superflat: 1 bedrock, 62 dirt, 1 grass (total height 64)
            WorldCreator creator = new WorldCreator(worldName);
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            
            // Generator settings for vanilla flat world
            // Format matches vanilla superflat preset
            creator.generatorSettings("{\"layers\": [{\"block\": \"minecraft:bedrock\", \"height\": 1}, {\"block\": \"minecraft:dirt\", \"height\": 62}, {\"block\": \"minecraft:grass_block\", \"height\": 1}], \"biome\":\"minecraft:plains\"}");
            
            plugin.getLogger().info("Creating admin build world: " + worldName);
            
            World world = creator.createWorld();
            
            if (world != null) {
                // Set world rules for building
                world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
                world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
                world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
                world.setTime(6000); // Set to noon
                
                plugin.getLogger().info("Admin build world created successfully: " + worldName);
                return world;
            } else {
                plugin.getLogger().severe("Failed to create admin build world: " + worldName);
                return null;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating admin build world: " + e.getMessage());
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
            completions.add("list");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("teleport"))) {
            // Suggest existing admin worlds
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
