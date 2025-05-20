package com.server.entities.npc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.entities.npc.types.CombatNPC;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.trait.SkinTrait;

/**
 * Manages NPCs using the Citizens API
 */
public class NPCManager {
    
    private static NPCManager instance;
    private final Main plugin;
    private final NPCRegistry npcRegistry;
    private final Map<String, NPC> npcById = new HashMap<>();
    private final Map<UUID, NPC> npcByUUID = new HashMap<>();
    private final Map<String, NPCInteractionHandler> interactionHandlers = new HashMap<>();
    private final Map<UUID, ArmorStand> nameplateStands = new HashMap<>();
    private final CombatHandler combatHandler = new CombatHandler();

    /**
     * Create a new NPCManager
     * 
     * @param plugin The main plugin instance
     */
    private NPCManager(Main plugin) {
        this.plugin = plugin;
        this.npcRegistry = CitizensAPI.getNPCRegistry();
    }
    
    /**
     * Initialize the NPC Manager
     * 
     * @param plugin The plugin instance
     */
    public static void initialize(Main plugin) {
        if (instance == null) {
            instance = new NPCManager(plugin);
            
            // Register the interaction listener
            plugin.getServer().getPluginManager().registerEvents(new NPCInteractionListener(instance), plugin);
            
            plugin.debugLog(DebugSystem.NPC,"NPCManager initialized with Citizens integration");
        }
    }
    
    /**
     * Get the NPCManager instance
     * 
     * @return The NPCManager instance
     */
    public static NPCManager getInstance() {
        return instance;
    }
    
    /**
     * Create a new NPC
     * 
     * @param id The unique ID for this NPC
     * @param name The display name of the NPC
     * @param location The location to spawn the NPC
     * @param skinName The name of the skin to use (player name)
     * @param lookAtPlayer Whether the NPC should look at nearby players
     * @return The created NPC
     */
    public NPC createNPC(String id, String name, Location location, String skinName, boolean lookAtPlayer) {
        // Check if an NPC with this ID already exists
        if (npcById.containsKey(id)) {
            plugin.debugLog(DebugSystem.NPC,"NPC with ID " + id + " already exists. Removing existing NPC.");
            removeNPC(id);
        }
        
        // Create a new NPC
        NPC npc = npcRegistry.createNPC(EntityType.PLAYER, name);
        
        // Store the NPC in our maps for later reference
        npcById.put(id, npc);
        npcByUUID.put(npc.getUniqueId(), npc);
        
        // Set the skin if provided
        if (skinName != null && !skinName.isEmpty()) {
            SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
            skinTrait.setSkinName(skinName);
        }
        
        // Add the look-close trait if requested
        if (lookAtPlayer) {
            LookClose lookTrait = npc.getOrAddTrait(LookClose.class);
            lookTrait.lookClose(true);
            lookTrait.setRange(8.0);
        }
        
        // Spawn the NPC at the specified location
        npc.spawn(location);
        
        plugin.debugLog(DebugSystem.NPC,"Created NPC " + name + " with ID " + id + " at " + location);
        return npc;
    }
    
    /**
     * Remove a nameplate for an NPC
     * 
     * @param npcId The UUID of the NPC
     */
    public void removeNameplate(UUID npcId) {
        ArmorStand stand = nameplateStands.remove(npcId);
        if (stand != null && stand.isValid() && !stand.isDead()) {
            // First remove it as a passenger if it has a vehicle
            if (stand.isInsideVehicle()) {
                stand.leaveVehicle();
            }
            
            // Then remove the stand itself
            stand.remove();
        }
        
        // Try to get the NPC and remove any other passengers it might have
        NPC npc = getNPCByUUID(npcId);
        if (npc != null && npc.isSpawned()) {
            // First try removing known passengers
            npc.getEntity().getPassengers().forEach(passenger -> npc.getEntity().removePassenger(passenger));
        }
    }
    
    /**
     * Remove an NPC by ID
     * 
     * @param id The ID of the NPC to remove
     */
    public void removeNPC(String id) {
        NPC npc = npcById.get(id);
        if (npc != null) {
            UUID uuid = npc.getUniqueId();
            
            // Remove nameplate first
            removeNameplate(uuid);
            
            // Then destroy NPC
            npc.destroy();
            npcById.remove(id);
            npcByUUID.remove(uuid);
            interactionHandlers.remove(id);
            
            plugin.debugLog(DebugSystem.NPC,"Removed NPC with ID: " + id);
        }
    }

    /**
     * Get all nameplates stands
     * 
     * @return Map of NPC UUIDs to ArmorStands
     */
    public Map<UUID, ArmorStand> getNameplateStands() {
        return nameplateStands;
    }
    
    /**
     * Register an interaction handler for an NPC
     * 
     * @param npcId The ID of the NPC
     * @param handler The interaction handler
     */
    public void registerInteractionHandler(String npcId, NPCInteractionHandler handler) {
        interactionHandlers.put(npcId, handler);
    }
    
    /**
     * Handle an interaction with an NPC
     * 
     * @param player The player who interacted
     * @param npc The NPC that was interacted with
     * @param rightClick Whether this was a right click interaction
     */
    public void handleInteraction(Player player, NPC npc, boolean rightClick) {
        // CRITICAL FIX: Add debug logging to track NPC interactions
        if (plugin.isDebugEnabled(DebugSystem.NPC)) {
            plugin.debugLog(DebugSystem.NPC,"NPC interaction: " + npc.getName() + " with " + player.getName() + 
                                " - right click: " + rightClick);
        }
        
        // Find the NPC ID by UUID
        String npcId = null;
        for (Map.Entry<String, NPC> entry : npcById.entrySet()) {
            if (entry.getValue().getUniqueId().equals(npc.getUniqueId())) {
                npcId = entry.getKey();
                break;
            }
        }
        
        if (npcId != null) {
            NPCInteractionHandler handler = interactionHandlers.get(npcId);
            if (handler != null) {
                // CRITICAL FIX: Wrap in try-catch to prevent interactions from breaking
                try {
                    handler.onInteract(player, npc, rightClick);
                } catch (Exception e) {
                    plugin.getLogger().severe("Error handling NPC interaction: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                // CRITICAL FIX: Log when handler is missing
                if (plugin.isDebugEnabled(DebugSystem.NPC)) {
                    plugin.debugLog(DebugSystem.NPC,"No interaction handler found for NPC: " + npcId);
                }
            }
        } else {
            // CRITICAL FIX: Log when NPC ID is not found
            if (plugin.isDebugEnabled(DebugSystem.NPC)) {
                plugin.debugLog(DebugSystem.NPC,"Could not find NPC ID for UUID: " + npc.getUniqueId());
            }
        }
    }
    
    /**
     * Get an NPC by ID
     * 
     * @param id The ID of the NPC
     * @return The NPC, or null if not found
     */
    public NPC getNPC(String id) {
        return npcById.get(id);
    }
    
    /**
     * Get an NPC by UUID
     * 
     * @param uuid The UUID of the NPC
     * @return The NPC, or null if not found
     */
    public NPC getNPCByUUID(UUID uuid) {
        return npcByUUID.get(uuid);
    }
    
    /**
     * Set equipment for an NPC
     * 
     * @param npcId The ID of the NPC
     * @param slot The equipment slot
     * @param item The item to set
     * @param updateStats Whether to update the NPC's stats (set to false if the caller will update them)
     */
    public void setEquipment(String npcId, EquipmentSlot slot, org.bukkit.inventory.ItemStack item, boolean updateStats) {
        NPC npc = npcById.get(npcId);
        if (npc == null) return;
        
        // Get the equipment trait
        net.citizensnpcs.api.trait.trait.Equipment equipment = npc.getOrAddTrait(net.citizensnpcs.api.trait.trait.Equipment.class);
        
        // Apply the equipment
        equipment.set(slot, item);
        
        // Debug log
        if (plugin.isDebugEnabled(DebugSystem.NPC)) {
            String itemInfo = item.getType().name();
            if (item.hasItemMeta()) {
                if (item.getItemMeta().hasDisplayName()) {
                    itemInfo = item.getItemMeta().getDisplayName();
                }
                if (item.getItemMeta().hasCustomModelData()) {
                    itemInfo += " (Model:" + item.getItemMeta().getCustomModelData() + ")";
                }
            }
            
            plugin.debugLog(DebugSystem.NPC,"NPCManager: Equipped " + itemInfo + " to " + npc.getName() + " in slot " + slot);
        }
        
        // Check if this NPC has an interaction handler that's a CombatNPC
        NPCInteractionHandler handler = interactionHandlers.get(npcId);
        
        // Only update stats if requested and handler is appropriate
        if (updateStats && handler instanceof CombatNPC) {
            plugin.debugLog(DebugSystem.NPC,"Found CombatNPC handler, updating stats from equipment");
            ((CombatNPC) handler).updateStatsFromEquipment();
        }
    }

    /**
     * Set equipment for an NPC (overload with default updateStats=true for backward compatibility)
     * 
     * @param npcId The ID of the NPC
     * @param slot The equipment slot
     * @param item The item to set
     */
    public void setEquipment(String npcId, EquipmentSlot slot, org.bukkit.inventory.ItemStack item) {
        setEquipment(npcId, slot, item, true);
    }
        
    /**
     * Clean up resources when the plugin is disabled
     */
    public void cleanup() {
        // Remove any remaining nameplate stands
        for (ArmorStand stand : nameplateStands.values()) {
            if (stand != null && stand.isValid() && !stand.isDead()) {
                stand.remove();
            }
        }
        nameplateStands.clear();
        
        // Despawn NPCs
        for (NPC npc : npcRegistry) {
            if (npc != null && npc.isSpawned()) {
                npc.despawn();
            }
        }
    }

    /**
     * Get all NPC IDs
     * 
     * @return List of NPC IDs
     */
    public List<String> getIds() {
        return new ArrayList<>(npcById.keySet());
    }

    /**
     * Get the interaction handler for an NPC
     * 
     * @param id The ID of the NPC
     * @return The interaction handler, or null if none
     */
    public NPCInteractionHandler getInteractionHandler(String id) {
        return interactionHandlers.get(id);
    }

    /**
     * Get the Citizens NPC registry
     * 
     * @return The NPC registry
     */
    public NPCRegistry getNPCRegistry() {
        return npcRegistry;
    }

    /**
     * Creates a hologram nameplate above an NPC
     * 
     * @param npc The NPC
     * @param name The name to display
     * @param health Current health
     * @param maxHealth Maximum health
     */
    public void createHologramNameplate(NPC npc, String name, double health, double maxHealth) {
        if (!npc.isSpawned()) return;
        
        // Remove any existing nameplate first
        removeNameplate(npc.getUniqueId());
        
        // Get level from metadata
        int level = 1;
        if (npc.getEntity().hasMetadata("level")) {
            level = npc.getEntity().getMetadata("level").get(0).asInt();
        }
        
        // Get NPC type from metadata or default to NORMAL
        NPCType npcType = NPCType.NORMAL;
        if (npc.getEntity().hasMetadata("npc_type")) {
            String typeName = npc.getEntity().getMetadata("npc_type").get(0).asString();
            try {
                npcType = NPCType.valueOf(typeName);
            } catch (IllegalArgumentException e) {
                // Ignore invalid type and use default
            }
        }
        
        // Format: [Lv.1] ❈ Combat NPC ❤ 100/100
        // Now use the NPC type's color and symbol
        String displayName = String.format("%s[Lv.%d] %s%s %s%s %s❤ %.1f/%.1f",
                ChatColor.GRAY,
                level,
                npcType.getColor(),
                npcType.getSymbol(), // Use symbol from NPC type
                ChatColor.WHITE,
                name,
                ChatColor.RED,
                health,
                maxHealth
        );
        
        // Create an invisible armor stand for the nameplate
        ArmorStand hologram = (ArmorStand) npc.getEntity().getWorld().spawnEntity(
                npc.getEntity().getLocation(),
                EntityType.ARMOR_STAND
        );
        
        // Configure the armor stand
        hologram.setVisible(false);
        hologram.setCustomName(displayName);
        hologram.setCustomNameVisible(true);
        hologram.setGravity(false);
        hologram.setInvulnerable(true);
        hologram.setSmall(true);
        hologram.setMarker(true);
        
        // Store original NPC name in metadata
        hologram.setMetadata("npc_uuid", new FixedMetadataValue(plugin, npc.getUniqueId().toString()));
        
        // Add the armor stand as a passenger to the NPC
        npc.getEntity().addPassenger(hologram);
        
        // Store the reference to the armor stand
        nameplateStands.put(npc.getUniqueId(), hologram);
    }

    /**
     * Update an existing nameplate with new health values
     * 
     * @param npc The NPC
     * @param health Current health
     * @param maxHealth Maximum health
     */
    public void updateNameplate(NPC npc, double health, double maxHealth) {
        if (!npc.isSpawned()) return;
        
        ArmorStand hologram = nameplateStands.get(npc.getUniqueId());
        if (hologram == null || !hologram.isValid() || hologram.isDead()) {
            // Recreate if missing
            String originalName = npc.getEntity().hasMetadata("original_name") ? 
                    npc.getEntity().getMetadata("original_name").get(0).asString() : npc.getName();
            createHologramNameplate(npc, originalName, health, maxHealth);
            return;
        }
        
        // Get the original name from metadata
        String originalName = npc.getEntity().hasMetadata("original_name") ? 
                npc.getEntity().getMetadata("original_name").get(0).asString() : npc.getName();
        
        // Get level from metadata
        int level = 1;
        if (npc.getEntity().hasMetadata("level")) {
            level = npc.getEntity().getMetadata("level").get(0).asInt();
        }
        
        // Get NPC type from metadata or default to NORMAL
        NPCType npcType = NPCType.NORMAL;
        if (npc.getEntity().hasMetadata("npc_type")) {
            String typeName = npc.getEntity().getMetadata("npc_type").get(0).asString();
            try {
                npcType = NPCType.valueOf(typeName);
            } catch (IllegalArgumentException e) {
                // Ignore invalid type and use default
            }
        }
        
        // Format: [Lv.1] ❈ NPC Name ❤ 100/100
        String displayName = String.format("%s[Lv.%d] %s%s %s%s %s❤ %.1f/%.1f",
                ChatColor.GRAY,
                level,
                npcType.getColor(),
                npcType.getSymbol(),
                ChatColor.WHITE,
                originalName,
                ChatColor.RED,
                health,
                maxHealth
        );
        
        // Update the name
        hologram.setCustomName(displayName);
        
        // Ensure the ArmorStand is still a passenger
        if (!hologram.isInsideVehicle() || hologram.getVehicle() != npc.getEntity()) {
            // If the hologram is no longer attached to the NPC, reattach it
            if (hologram.isInsideVehicle()) {
                hologram.leaveVehicle();
            }
            npc.getEntity().addPassenger(hologram);
        }
    }

    /**
     * Get the shared combat handler
     * 
     * @return The shared combat handler
     */
    public CombatHandler getCombatHandler() {
        return combatHandler;
    }
}