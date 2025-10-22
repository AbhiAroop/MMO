package com.server.islands.managers;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.server.islands.data.IslandType;
import com.server.islands.data.PlayerIsland;

/**
 * Manages island world generation, loading, and unloading using SlimeWorldManager.
 * Uses SlimeWorld format for optimized storage and performance.
 */
public class IslandWorldManager {
    
    private final JavaPlugin plugin;
    private final AdvancedSlimePaperAPI slimeAPI;
    private final SlimeLoader loader;
    
    public IslandWorldManager(JavaPlugin plugin) {
        this.plugin = plugin;
        // Get AdvancedSlimePaper API instance
        this.slimeAPI = AdvancedSlimePaperAPI.instance();
        // Create file-based SlimeLoader
        File slimeWorldsFolder = new File(plugin.getDataFolder(), "slimeworlds");
        if (!slimeWorldsFolder.exists()) {
            slimeWorldsFolder.mkdirs();
        }
        this.loader = new com.infernalsuite.asp.api.loaders.SlimeLoader() {
            @Override
            public byte[] readWorld(String worldName) throws com.infernalsuite.asp.api.exceptions.UnknownWorldException, IOException {
                File worldFile = new File(slimeWorldsFolder, worldName + ".slime");
                if (!worldFile.exists()) {
                    throw new com.infernalsuite.asp.api.exceptions.UnknownWorldException(worldName);
                }
                return java.nio.file.Files.readAllBytes(worldFile.toPath());
            }
            
            @Override
            public boolean worldExists(String worldName) throws IOException {
                return new File(slimeWorldsFolder, worldName + ".slime").exists();
            }
            
            @Override
            public java.util.List<String> listWorlds() throws IOException {
                File[] files = slimeWorldsFolder.listFiles((dir, name) -> name.endsWith(".slime"));
                if (files == null) return java.util.Collections.emptyList();
                return java.util.Arrays.stream(files)
                    .map(f -> f.getName().replace(".slime", ""))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            @Override
            public void saveWorld(String worldName, byte[] data) throws IOException {
                File worldFile = new File(slimeWorldsFolder, worldName + ".slime");
                java.nio.file.Files.write(worldFile.toPath(), data);
            }
            
            @Override
            public void deleteWorld(String worldName) throws com.infernalsuite.asp.api.exceptions.UnknownWorldException, IOException {
                File worldFile = new File(slimeWorldsFolder, worldName + ".slime");
                if (!worldFile.exists()) {
                    throw new com.infernalsuite.asp.api.exceptions.UnknownWorldException(worldName);
                }
                if (!worldFile.delete()) {
                    throw new IOException("Failed to delete world file: " + worldName);
                }
            }
        };
        
        plugin.getLogger().info("IslandWorldManager initialized with AdvancedSlimePaper");
    }
    
    /**
     * Generates a new island world using AdvancedSlimePaper.
     */
    public CompletableFuture<World> generateIslandWorld(PlayerIsland island) {
        return CompletableFuture.supplyAsync(() -> {
            String worldName = island.getWorldName();
            
            // Check if world already loaded
            World existingWorld = Bukkit.getWorld(worldName);
            if (existingWorld != null) {
                plugin.getLogger().warning("Island world already loaded: " + worldName);
                return existingWorld;
            }
            
            try {
                // Get or create the template for this island type
                String templateName = "island_template_" + island.getIslandType().name().toLowerCase();
                SlimeWorld templateWorld = getOrCreateTemplate(island.getIslandType(), templateName);
                
                if (templateWorld == null) {
                    plugin.getLogger().severe("Failed to get/create template for " + island.getIslandType());
                    return null;
                }
                
                // Clone the template to create the new island world
                SlimeWorld slimeWorld = templateWorld.clone(worldName, loader);
                
                // Generate and load world on main thread
                CompletableFuture<World> worldFuture = new CompletableFuture<>();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        // Load the SlimeWorld
                        SlimeWorldInstance instance = slimeAPI.loadWorld(slimeWorld, true);
                        World world = instance.getBukkitWorld();
                        
                        if (world != null) {
                            // Set world border
                            setWorldBorder(world, island.getCurrentSize());
                            
                            plugin.getLogger().info("Generated island world with SlimeWorld: " + worldName);
                            worldFuture.complete(world);
                        } else {
                            worldFuture.completeExceptionally(new IllegalStateException("Failed to get Bukkit world"));
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("Failed to load SlimeWorld: " + e.getMessage());
                        e.printStackTrace();
                        worldFuture.completeExceptionally(e);
                    }
                });
                
                return worldFuture.join();
                
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create SlimeWorld: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }
    
    /**
     * Gets or creates a template SlimeWorld for the given island type.
     * Templates are created once and cloned for each new island.
     */
    private SlimeWorld getOrCreateTemplate(com.server.islands.data.IslandType type, String templateName) {
        try {
            // Check if template already exists
            if (loader.worldExists(templateName)) {
                plugin.getLogger().info("[Island] Loading existing template: " + templateName);
                return slimeAPI.readWorld(loader, templateName, false, new SlimePropertyMap());
            }
            
            plugin.getLogger().info("[Island] Creating new template: " + templateName);
            
            // Create empty template world
            SlimePropertyMap properties = new SlimePropertyMap();
            SlimeWorld templateWorld = slimeAPI.createEmptyWorld(templateName, false, properties, loader);
            
            // Load it temporarily to add structures
            CompletableFuture<Boolean> generationFuture = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    SlimeWorldInstance instance = slimeAPI.loadWorld(templateWorld, false);
                    World world = instance.getBukkitWorld();
                    
                    if (world != null) {
                        plugin.getLogger().info("[Island] Generating template structures for: " + type);
                        
                        // Generate structures
                        generateIslandStructure(world, type);
                        
                        // Set spawn location
                        org.bukkit.Location spawnLoc = new org.bukkit.Location(world, 0.5, 101, 0.5);
                        world.setSpawnLocation(spawnLoc);
                        
                        // Save and unload
                        world.save();
                        Bukkit.unloadWorld(world, true);
                        
                        plugin.getLogger().info("[Island] Template created and saved: " + templateName);
                        generationFuture.complete(true);
                    } else {
                        generationFuture.complete(false);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to generate template: " + e.getMessage());
                    e.printStackTrace();
                    generationFuture.complete(false);
                }
            });
            
            // Wait for generation to complete
            if (generationFuture.join()) {
                // Reload the saved template
                return slimeAPI.readWorld(loader, templateName, false, new SlimePropertyMap());
            }
            
            return null;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to get/create template: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Loads an existing island world using AdvancedSlimePaper.
     */
    public CompletableFuture<World> loadWorld(PlayerIsland island) {
        return CompletableFuture.supplyAsync(() -> {
            String worldName = island.getWorldName();
            
            // Check if already loaded
            World existingWorld = Bukkit.getWorld(worldName);
            if (existingWorld != null) {
                return existingWorld;
            }
            
            try {
                // Check if SlimeWorld exists
                if (!loader.worldExists(worldName)) {
                    plugin.getLogger().warning("SlimeWorld not found: " + worldName + " - Generating new world");
                    return generateIslandWorld(island).join();
                }
                
                // Read the SlimeWorld data (throws exceptions that need to be handled)
                SlimeWorld slimeWorld;
                try {
                    slimeWorld = slimeAPI.readWorld(loader, worldName, false, new SlimePropertyMap());
                } catch (com.infernalsuite.asp.api.exceptions.UnknownWorldException | 
                         com.infernalsuite.asp.api.exceptions.CorruptedWorldException |
                         com.infernalsuite.asp.api.exceptions.NewerFormatException e) {
                    plugin.getLogger().severe("Failed to read SlimeWorld: " + e.getMessage());
                    return null;
                }
                
                // Load world on main thread
                CompletableFuture<World> worldFuture = new CompletableFuture<>();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        SlimeWorldInstance instance = slimeAPI.loadWorld(slimeWorld, true);
                        World world = instance.getBukkitWorld();
                        
                        if (world != null) {
                            // Ensure world border is correct
                            setWorldBorder(world, island.getCurrentSize());
                            plugin.getLogger().info("Loaded island world with SlimeWorld: " + worldName);
                            worldFuture.complete(world);
                        } else {
                            worldFuture.completeExceptionally(new IllegalStateException("Failed to get Bukkit world"));
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("Failed to load SlimeWorld: " + e.getMessage());
                        e.printStackTrace();
                        worldFuture.completeExceptionally(e);
                    }
                });
                
                return worldFuture.join();
                
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to load SlimeWorld: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }
    
    /**
     * Unloads an island world and saves it to SlimeWorld format.
     */
    public CompletableFuture<Boolean> unloadWorld(String worldName) {
        return CompletableFuture.supplyAsync(() -> {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return true; // Already unloaded
            }
            
            // Unload on main thread
            CompletableFuture<Boolean> unloadFuture = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    // Unload the world (SlimeWorld auto-saves)
                    boolean success = Bukkit.unloadWorld(world, true);
                    plugin.getLogger().info("Unloaded island world: " + worldName);
                    unloadFuture.complete(success);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to unload island world: " + e.getMessage());
                    e.printStackTrace();
                    unloadFuture.complete(false);
                }
            });
            
            return unloadFuture.join();
        });
    }
    
    /**
     * Deletes an island world using AdvancedSlimePaper.
     */
    public void deleteWorld(String worldName) {
        try {
            plugin.getLogger().info("[Island] Deleting world: " + worldName);
            
            // First check if world is loaded
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                // Kick all players from the world first (safety check) - must run on main thread
                for (Player player : world.getPlayers()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        World mainWorld = Bukkit.getWorld("world");
                        if (mainWorld == null) {
                            mainWorld = Bukkit.getWorlds().get(0);
                        }
                        Location spawn = mainWorld.getSpawnLocation();
                        
                        // Get the world key properly - for overworld it's "minecraft:overworld"
                        String worldKey = mainWorld.key().asString();
                        String command = String.format("execute in %s run tp %s %.2f %.2f %.2f",
                            worldKey,
                            player.getName(),
                            spawn.getX(),
                            spawn.getY(),
                            spawn.getZ()
                        );
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    });
                }
                
                // Wait a tick for teleports to complete, then unload
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    unloadWorld(worldName);
                }, 2L);
                
                // Wait for unload to complete before deleting file
                Thread.sleep(100);
            }
            
            // Delete SlimeWorld file
            if (loader.worldExists(worldName)) {
                loader.deleteWorld(worldName);
                plugin.getLogger().info("[Island] Successfully deleted SlimeWorld file: " + worldName);
            } else {
                plugin.getLogger().warning("[Island] SlimeWorld file not found for deletion: " + worldName);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[Island] Failed to delete island world " + worldName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sets the world border for an island.
     */
    public void setWorldBorder(World world, int size) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            WorldBorder border = world.getWorldBorder();
            border.setCenter(0, 0);
            border.setSize(size);
            border.setWarningDistance(5);
        });
    }
    
    /**
     * Updates the world border size (used when upgrading).
     */
    public void updateWorldBorder(PlayerIsland island, int newSize) {
        World world = Bukkit.getWorld(island.getWorldName());
        if (world != null) {
            setWorldBorder(world, newSize);
        }
    }
    
    /**
     * Generates the initial island structure based on type.
     */
    private void generateIslandStructure(World world, IslandType type) {
        Location center = new Location(world, 0, 100, 0);
        plugin.getLogger().info("[Island] Generating " + type + " structure at " + center);
        
        switch (type) {
            case SKY:
                generateSkyIsland(world, center);
                break;
            case OCEAN:
                generateOceanIsland(world, center);
                break;
            case FOREST:
                generateForestIsland(world, center);
                break;
        }
        
        plugin.getLogger().info("[Island] Structure generation complete for " + type);
    }
    
    /**
     * Generates a floating sky island.
     */
    private void generateSkyIsland(World world, Location center) {
        plugin.getLogger().info("[Island] Starting SKY island generation at " + center);
        
        // Force load and generate the chunk first
        int chunkX = center.getBlockX() >> 4;
        int chunkZ = center.getBlockZ() >> 4;
        plugin.getLogger().info("[Island] Loading chunk " + chunkX + ", " + chunkZ);
        world.loadChunk(chunkX, chunkZ, true);
        
        int blocksPlaced = 0;
        
        // Generate a small grass platform
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                Location loc = center.clone().add(x, -1, z);
                world.getBlockAt(loc).setType(org.bukkit.Material.GRASS_BLOCK, true);
                blocksPlaced++;
                
                // Add dirt layers
                for (int y = -2; y >= -4; y--) {
                    world.getBlockAt(loc.clone().add(0, y, 0)).setType(org.bukkit.Material.DIRT, true);
                    blocksPlaced++;
                }
            }
        }
        
        plugin.getLogger().info("[Island] Placed " + blocksPlaced + " platform blocks");
        
        // Add a tree (offset from spawn so player doesn't spawn inside it)
        Location treeBase = center.clone().add(-3, 0, -3);
        world.getBlockAt(treeBase).setType(org.bukkit.Material.OAK_LOG, true);
        world.getBlockAt(treeBase.clone().add(0, 1, 0)).setType(org.bukkit.Material.OAK_LOG, true);
        world.getBlockAt(treeBase.clone().add(0, 2, 0)).setType(org.bukkit.Material.OAK_LOG, true);
        
        // Add chest with starter items (also away from spawn)
        Location chestLoc = center.clone().add(3, 0, 0);
        world.getBlockAt(chestLoc).setType(org.bukkit.Material.CHEST, true);
        
        plugin.getLogger().info("[Island] SKY island generation complete, total blocks: " + (blocksPlaced + 4));
        
        // Verify blocks were placed
        org.bukkit.block.Block testBlock = world.getBlockAt(center.clone().add(0, -1, 0));
        plugin.getLogger().info("[Island] Verification: Block at spawn platform is " + testBlock.getType());
    }
    
    /**
     * Generates an ocean island.
     */
    private void generateOceanIsland(World world, Location center) {
        // Generate a sand island
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance <= 7) {
                    Location loc = center.clone().add(x, -1, z);
                    world.getBlockAt(loc).setType(org.bukkit.Material.SAND);
                    
                    // Add sandstone layers
                    for (int y = -2; y >= -4; y--) {
                        world.getBlockAt(loc.clone().add(0, y, 0)).setType(org.bukkit.Material.SANDSTONE);
                    }
                }
            }
        }
        
        // Fill surrounding with water
        for (int x = -12; x <= 12; x++) {
            for (int z = -12; z <= 12; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance > 7 && distance <= 12) {
                    for (int y = 0; y >= -3; y--) {
                        Location loc = center.clone().add(x, y, z);
                        if (world.getBlockAt(loc).isEmpty()) {
                            world.getBlockAt(loc).setType(org.bukkit.Material.WATER);
                        }
                    }
                }
            }
        }
        
        // Add chest (away from spawn point)
        Location chestLoc = center.clone().add(5, 0, 0);
        world.getBlockAt(chestLoc).setType(org.bukkit.Material.CHEST);
    }
    
    /**
     * Generates a forest island.
     */
    private void generateForestIsland(World world, Location center) {
        // Generate a grass platform with trees
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                Location loc = center.clone().add(x, -1, z);
                world.getBlockAt(loc).setType(org.bukkit.Material.GRASS_BLOCK);
                
                // Add dirt layers
                for (int y = -2; y >= -4; y--) {
                    world.getBlockAt(loc.clone().add(0, y, 0)).setType(org.bukkit.Material.DIRT);
                }
                
                // Add random trees
                if (Math.random() < 0.1 && Math.abs(x) > 2 && Math.abs(z) > 2) {
                    Location treeLoc = loc.clone().add(0, 1, 0);
                    world.getBlockAt(treeLoc).setType(org.bukkit.Material.OAK_LOG);
                    world.getBlockAt(treeLoc.clone().add(0, 1, 0)).setType(org.bukkit.Material.OAK_LOG);
                }
            }
        }
        
        // Add chest (away from spawn point)
        Location chestLoc = center.clone().add(5, 0, 0);
        world.getBlockAt(chestLoc).setType(org.bukkit.Material.CHEST);
    }
}
