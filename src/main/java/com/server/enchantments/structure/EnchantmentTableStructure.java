package com.server.enchantments.structure;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Manages enchantment table structures - armor stands with enchanting table helmets.
 * 
 * Structure Requirements:
 * - 1 Armor Stand with an Enchanting Table as helmet
 * - Must be visible (not invisible)
 * - Must have arms disabled
 */
public class EnchantmentTableStructure {
    
    private static final double CLICK_RADIUS = 2.0;
    
    private final Plugin plugin;
    private final File structuresFile;
    private FileConfiguration structuresConfig;
    private final Map<UUID, Location> registeredStructures; // ArmorStand UUID -> Location
    
    public EnchantmentTableStructure(Plugin plugin) {
        this.plugin = plugin;
        this.structuresFile = new File(plugin.getDataFolder(), "enchantment_structures.yml");
        this.registeredStructures = new HashMap<>();
        loadStructures();
    }
    
    /**
     * Checks if the given armor stand is a valid enchantment altar.
     * 
     * @param armorStand The armor stand to check
     * @return true if it's a valid altar, false otherwise
     */
    public boolean isValidAltar(ArmorStand armorStand) {
        if (armorStand == null) {
            return false;
        }
        
        // Check if it has an enchanting table as helmet
        ItemStack helmet = armorStand.getEquipment().getHelmet();
        return helmet != null && helmet.getType() == Material.ENCHANTING_TABLE;
    }
    
    /**
     * Finds a valid enchantment altar near the given location.
     * Prioritizes registered altars for better performance.
     * 
     * @param location The location to search from
     * @return The armor stand altar if found, null otherwise
     */
    public ArmorStand findNearbyAltar(Location location) {
        World world = location.getWorld();
        if (world == null) return null;
        
        // First, check if any registered altars are nearby (faster)
        for (Map.Entry<UUID, Location> entry : registeredStructures.entrySet()) {
            Location altarLoc = entry.getValue();
            
            // Skip if in different world
            if (!altarLoc.getWorld().equals(world)) continue;
            
            // Check distance
            if (altarLoc.distance(location) <= CLICK_RADIUS) {
                // Try to get the entity
                for (Entity entity : world.getNearbyEntities(altarLoc, 0.5, 0.5, 0.5)) {
                    if (entity.getType() == EntityType.ARMOR_STAND) {
                        ArmorStand stand = (ArmorStand) entity;
                        if (stand.getUniqueId().equals(entry.getKey()) && isValidAltar(stand)) {
                            plugin.getLogger().info("[Altar] Found registered altar at " + altarLoc);
                            return stand;
                        }
                    }
                }
            }
        }
        
        // Fallback: search all nearby entities (for unregistered but valid altars)
        Collection<Entity> nearbyEntities = world.getNearbyEntities(
            location, 
            CLICK_RADIUS, 
            CLICK_RADIUS, 
            CLICK_RADIUS
        );
        
        for (Entity entity : nearbyEntities) {
            if (entity.getType() == EntityType.ARMOR_STAND) {
                ArmorStand stand = (ArmorStand) entity;
                // Only check if it has enchanting table helmet (skip player armor stands, etc.)
                ItemStack helmet = stand.getEquipment().getHelmet();
                if (helmet != null && helmet.getType() == Material.ENCHANTING_TABLE) {
                    plugin.getLogger().info("[Altar] Found valid unregistered altar");
                    return stand;
                }
            }
        }
        
        plugin.getLogger().info("[Altar] No altar found near " + location);
        return null;
    }
    
    /**
     * Registers a valid enchantment altar.
     * 
     * @param armorStand The armor stand to register as an altar
     * @return true if successfully registered, false if invalid or already registered
     */
    public boolean registerAltar(ArmorStand armorStand) {
        if (!isValidAltar(armorStand)) {
            return false;
        }
        
        UUID uuid = armorStand.getUniqueId();
        
        if (registeredStructures.containsKey(uuid)) {
            return false; // Already registered
        }
        
        registeredStructures.put(uuid, armorStand.getLocation());
        saveStructures();
        return true;
    }
    
    /**
     * Unregisters an enchantment altar.
     * 
     * @param armorStand The armor stand to unregister
     * @return true if successfully unregistered, false if not found
     */
    public boolean unregisterAltar(ArmorStand armorStand) {
        if (registeredStructures.remove(armorStand.getUniqueId()) != null) {
            saveStructures();
            return true;
        }
        return false;
    }
    
    /**
     * Checks if an altar is registered.
     * 
     * @param armorStand The armor stand to check
     * @return true if registered, false otherwise
     */
    public boolean isRegistered(ArmorStand armorStand) {
        return registeredStructures.containsKey(armorStand.getUniqueId());
    }
    
    /**
     * Gets the armor stand for a registered altar by UUID.
     * 
     * @param uuid The UUID of the armor stand
     * @param world The world to search in
     * @return The armor stand, or null if not found
     */
    public ArmorStand getAltarByUUID(UUID uuid, World world) {
        if (!registeredStructures.containsKey(uuid) || world == null) {
            return null;
        }
        
        Entity entity = world.getEntity(uuid);
        if (entity instanceof ArmorStand) {
            ArmorStand stand = (ArmorStand) entity;
            if (isValidAltar(stand)) {
                return stand;
            }
        }
        
        // Armor stand no longer valid, unregister
        registeredStructures.remove(uuid);
        saveStructures();
        return null;
    }
    
    /**
     * Gets all registered altar UUIDs.
     * 
     * @return Set of all registered altar UUIDs
     */
    public Set<UUID> getRegisteredAltars() {
        return new HashSet<>(registeredStructures.keySet());
    }
    
    /**
     * Gets all registered structure locations.
     * 
     * @return Collection of all registered locations
     */
    public Collection<Location> getRegisteredStructures() {
        return new HashSet<>(registeredStructures.values());
    }
    
    /**
     * Validates all registered structures and removes invalid ones.
     * Should be called periodically or on server start.
     * 
     * @return Number of invalid structures removed
     */
    public int validateAllStructures() {
        int removed = 0;
        Iterator<Map.Entry<UUID, Location>> iterator = registeredStructures.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<UUID, Location> entry = iterator.next();
            UUID uuid = entry.getKey();
            Location loc = entry.getValue();
            
            if (loc.getWorld() == null) {
                iterator.remove();
                removed++;
                continue;
            }
            
            // Check if armor stand still exists and is valid
            Entity entity = loc.getWorld().getEntity(uuid);
            if (!(entity instanceof ArmorStand) || !isValidAltar((ArmorStand) entity)) {
                iterator.remove();
                removed++;
            }
        }
        
        if (removed > 0) {
            saveStructures();
        }
        
        return removed;
    }
    
    /**
     * Loads registered structures from disk.
     */
    private void loadStructures() {
        if (!structuresFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                structuresFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create enchantment structures file");
                return;
            }
        }
        
        structuresConfig = YamlConfiguration.loadConfiguration(structuresFile);
        
        if (structuresConfig.contains("altars")) {
            ConfigurationSection section = structuresConfig.getConfigurationSection("altars");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    String path = "altars." + key;
                    
                    String uuidString = structuresConfig.getString(path + ".uuid");
                    String worldName = structuresConfig.getString(path + ".world");
                    double x = structuresConfig.getDouble(path + ".x");
                    double y = structuresConfig.getDouble(path + ".y");
                    double z = structuresConfig.getDouble(path + ".z");
                    
                    if (worldName == null || uuidString == null) continue;
                    
                    World world = plugin.getServer().getWorld(worldName);
                    if (world != null) {
                        Location loc = new Location(world, x, y, z);
                        UUID uuid = UUID.fromString(uuidString);
                        registeredStructures.put(uuid, loc);
                    }
                }
            }
        }
        
        plugin.getLogger().info(String.format("Loaded %d enchantment altars", registeredStructures.size()));
    }
    
    /**
     * Saves registered structures to disk.
     */
    private void saveStructures() {
        structuresConfig = new YamlConfiguration();
        
        int index = 0;
        for (Map.Entry<UUID, Location> entry : registeredStructures.entrySet()) {
            UUID uuid = entry.getKey();
            Location loc = entry.getValue();
            String path = "altars." + index;
            
            structuresConfig.set(path + ".uuid", uuid.toString());
            structuresConfig.set(path + ".world", loc.getWorld().getName());
            structuresConfig.set(path + ".x", loc.getX());
            structuresConfig.set(path + ".y", loc.getY());
            structuresConfig.set(path + ".z", loc.getZ());
            
            index++;
        }
        
        try {
            structuresConfig.save(structuresFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save enchantment structures");
        }
    }
    
    /**
     * Clears all registered structures. Use with caution!
     */
    public void clearAllStructures() {
        registeredStructures.clear();
        saveStructures();
    }
}
