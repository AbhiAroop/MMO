package com.server.islands.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import com.server.islands.data.PlayerIsland;
import com.server.islands.managers.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Tracks redstone component placement and enforces redstone limits on islands.
 */
public class IslandRedstoneListener implements Listener {
    
    private final IslandManager islandManager;
    
    // Cache of redstone blocks per island: islandId -> Set<Location>
    private final Map<UUID, Map<Location, Material>> redstoneBlocks = new HashMap<>();
    
    // Redstone materials to track
    private static final Set<Material> REDSTONE_MATERIALS = Set.of(
        Material.REDSTONE_WIRE,
        Material.REDSTONE_TORCH,
        Material.REDSTONE_WALL_TORCH,
        Material.REPEATER,
        Material.COMPARATOR,
        Material.REDSTONE_BLOCK,
        Material.OBSERVER,
        Material.PISTON,
        Material.STICKY_PISTON,
        Material.DISPENSER,
        Material.DROPPER,
        Material.HOPPER,
        Material.LEVER,
        Material.STONE_BUTTON,
        Material.OAK_BUTTON,
        Material.SPRUCE_BUTTON,
        Material.BIRCH_BUTTON,
        Material.JUNGLE_BUTTON,
        Material.ACACIA_BUTTON,
        Material.DARK_OAK_BUTTON,
        Material.CRIMSON_BUTTON,
        Material.WARPED_BUTTON,
        Material.POLISHED_BLACKSTONE_BUTTON,
        Material.STONE_PRESSURE_PLATE,
        Material.OAK_PRESSURE_PLATE,
        Material.SPRUCE_PRESSURE_PLATE,
        Material.BIRCH_PRESSURE_PLATE,
        Material.JUNGLE_PRESSURE_PLATE,
        Material.ACACIA_PRESSURE_PLATE,
        Material.DARK_OAK_PRESSURE_PLATE,
        Material.CRIMSON_PRESSURE_PLATE,
        Material.WARPED_PRESSURE_PLATE,
        Material.POLISHED_BLACKSTONE_PRESSURE_PLATE,
        Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
        Material.HEAVY_WEIGHTED_PRESSURE_PLATE,
        Material.TRIPWIRE_HOOK,
        Material.TRIPWIRE,
        Material.TRAPPED_CHEST,
        Material.DAYLIGHT_DETECTOR,
        Material.REDSTONE_LAMP,
        Material.NOTE_BLOCK,
        Material.TARGET
    );
    
    public IslandRedstoneListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }
    
    /**
     * Handles redstone component placement.
     * Uses LOWEST priority to cancel before other plugins process the event.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Material material = block.getType();
        
        // Check if it's a redstone component
        if (!REDSTONE_MATERIALS.contains(material)) {
            return;
        }
        
        World world = block.getWorld();
        
        // Check if player is on an island
        if (!world.getName().startsWith("island_")) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Get the island from the world name
        String worldName = world.getName();
        String islandIdStr = worldName.substring(7); // Remove "island_" prefix
        UUID islandId;
        
        try {
            islandId = UUID.fromString(islandIdStr);
        } catch (IllegalArgumentException e) {
            return; // Not a valid island world
        }
        
        // Get the island from cache (must be synchronous for event cancellation to work)
        PlayerIsland island = islandManager.getCache().getIsland(islandId);
        if (island == null) {
            // Try to get from cache by checking if this is the owner's island
            // This handles the case where the island isn't cached yet
            event.setCancelled(true);
            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("Island data not loaded! Please try again.", NamedTextColor.RED)));
            return;
        }
        
        // Check current redstone count synchronously
        int currentCount = countRedstoneBlocks(island);
        int limit = island.getCurrentRedstoneLimit();
        
        // Debug logging
        islandManager.getPlugin().getLogger().info("[Island Redstone] Player " + player.getName() + 
            " trying to place " + material + " - Current: " + currentCount + "/" + limit);
        
        if (currentCount >= limit) {
            // Cancel the placement
            event.setCancelled(true);
            
            islandManager.getPlugin().getLogger().info("[Island Redstone] BLOCKED placement - limit reached");
            
            // Send message to player
            player.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("Redstone limit reached! ", NamedTextColor.RED))
                .append(Component.text("(" + currentCount + "/" + limit + ")", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("Upgrade your redstone limit with ", NamedTextColor.YELLOW)
                .append(Component.text("/island upgrade", NamedTextColor.GOLD)));
        } else {
            islandManager.getPlugin().getLogger().info("[Island Redstone] ALLOWED placement - adding to cache");
            
            // Add to cache
            redstoneBlocks
                .computeIfAbsent(islandId, k -> new HashMap<>())
                .put(block.getLocation(), material);
        }
    }
    
    /**
     * Handles redstone component breaking.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material material = block.getType();
        
        // Check if it's a redstone component
        if (!REDSTONE_MATERIALS.contains(material)) {
            return;
        }
        
        World world = block.getWorld();
        
        // Check if player is on an island
        if (!world.getName().startsWith("island_")) {
            return;
        }
        
        // Get the island from the world name
        String worldName = world.getName();
        String islandIdStr = worldName.substring(7); // Remove "island_" prefix
        UUID islandId;
        
        try {
            islandId = UUID.fromString(islandIdStr);
        } catch (IllegalArgumentException e) {
            return; // Not a valid island world
        }
        
        // Remove from cache
        Map<Location, Material> islandRedstone = redstoneBlocks.get(islandId);
        if (islandRedstone != null) {
            islandRedstone.remove(block.getLocation());
        }
    }
    
    /**
     * Counts redstone blocks on an island.
     * Uses cache first, only scans world if cache is empty/invalid.
     * For event checking, we use a fast count that assumes cache is valid.
     */
    private int countRedstoneBlocks(PlayerIsland island) {
        UUID islandId = island.getIslandId();
        
        // Check cache first
        Map<Location, Material> cached = redstoneBlocks.get(islandId);
        if (cached != null) {
            // For event checking, we trust the cache and do a quick validation
            // Only remove entries that are clearly invalid (block no longer exists)
            cached.entrySet().removeIf(entry -> {
                try {
                    Block block = entry.getKey().getBlock();
                    return block == null || block.getType() != entry.getValue();
                } catch (Exception e) {
                    return true; // Remove invalid entries
                }
            });
            return cached.size();
        }
        
        // If no cache exists, initialize empty cache and return 0
        // The first scan will happen async after server start
        redstoneBlocks.put(islandId, new HashMap<>());
        return 0;
    }
    
    /**
     * Performs a full world scan to rebuild the cache.
     * This should be called asynchronously, not during event handling.
     */
    public void scanIslandRedstone(PlayerIsland island) {
        UUID islandId = island.getIslandId();
        
        World world = Bukkit.getWorld(island.getWorldName());
        if (world == null) {
            return;
        }
        
        Map<Location, Material> newCache = new HashMap<>();
        
        // Scan within the island's border
        int radius = island.getCurrentSize() / 2;
        Location center = new Location(world, 0, 0, 0);
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    Location loc = center.clone().add(x, y, z);
                    
                    // Check if chunk is loaded before accessing block
                    if (!world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                        continue;
                    }
                    
                    Block block = world.getBlockAt(loc);
                    Material type = block.getType();
                    
                    if (REDSTONE_MATERIALS.contains(type)) {
                        newCache.put(loc, type);
                    }
                }
            }
        }
        
        // Update cache
        redstoneBlocks.put(islandId, newCache);
        
        islandManager.getPlugin().getLogger().info("[Island] Scanned " + island.getWorldName() + 
            " and found " + newCache.size() + " redstone components");
    }
    
    /**
     * Clears the cache for an island (called when island is deleted).
     */
    public void clearIslandCache(UUID islandId) {
        redstoneBlocks.remove(islandId);
    }
    
    /**
     * Gets the current redstone count for an island.
     */
    public int getRedstoneCount(PlayerIsland island) {
        return countRedstoneBlocks(island);
    }
}
