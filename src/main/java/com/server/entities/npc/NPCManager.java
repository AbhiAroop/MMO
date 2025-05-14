package com.server.entities.npc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import com.server.Main;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.Equipment;
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
            
            plugin.getLogger().info("NPCManager initialized with Citizens integration");
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
     * Create a new talking NPC
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
            plugin.getLogger().warning("NPC with ID " + id + " already exists. Removing existing NPC.");
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
        
        plugin.getLogger().info("Created NPC " + name + " with ID " + id + " at " + location);
        return npc;
    }
    
    /**
     * Create a conversational NPC with dialogue
     * 
     * @param id The unique ID for this NPC
     * @param name The display name of the NPC
     * @param location The location to spawn the NPC
     * @param skinName The name of the skin to use (player name)
     * @param dialogueTree The dialogue tree for this NPC
     * @return The created NPC
     */
    public NPC createTalkingNPC(String id, String name, Location location, String skinName, DialogueTree dialogueTree) {
        NPC npc = createNPC(id, name, location, skinName, true);
        
        // Register a dialogue handler for this NPC
        DialogueHandler handler = new DialogueHandler(dialogueTree);
        registerInteractionHandler(id, handler);
        
        return npc;
    }
    
    /**
     * Create a combat NPC for training
     * 
     * @param id The unique ID for this NPC
     * @param name The display name of the NPC
     * @param location The location to spawn the NPC
     * @param skinName The name of the skin to use (player name)
     * @param health The health of the NPC
     * @param damage The damage the NPC deals
     * @return The created NPC
     * @deprecated Use createCombatNPC with NPCStats instead
     */
    @Deprecated
    private NPC createCombatNPCLegacy(String id, String name, Location location, String skinName, double health, double damage) {
        // Create a basic NPC without AI
        NPC npc = createNPC(id, name, location, skinName, false);
        
        // Make sure the NPC is vulnerable
        npc.data().set(NPC.Metadata.DEFAULT_PROTECTED, false);
        
        // Set entity type to PLAYER for combat NPCs
        if (npc.hasTrait(net.citizensnpcs.api.trait.trait.MobType.class)) {
            npc.getTrait(net.citizensnpcs.api.trait.trait.MobType.class).setType(EntityType.PLAYER);
        }
        
        // Give it equipment if it supports equipment
        if (npc.hasTrait(net.citizensnpcs.api.trait.trait.Equipment.class)) {
            net.citizensnpcs.api.trait.trait.Equipment equipment = npc.getTrait(net.citizensnpcs.api.trait.trait.Equipment.class);
            equipment.set(net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot.HAND, new org.bukkit.inventory.ItemStack(Material.IRON_SWORD));
        }

        // IMPORTANT: Completely hide the default nameplate
        npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, false);
        
        // Make the entity vulnerable (important!)
        if (npc.isSpawned() && npc.getEntity() instanceof LivingEntity) {
            npc.getEntity().setInvulnerable(false);
            
            // Completely hide the entity name
            npc.getEntity().setCustomNameVisible(false);
            npc.getEntity().setCustomName(null);
            
            // Store the original clean name in metadata
            npc.getEntity().setMetadata("original_name", new FixedMetadataValue(plugin, name));
        }
        
        // Create and register combat handler
        CombatHandler handler = new CombatHandler(health, damage);
        registerInteractionHandler(id, handler);
        
        // Initialize the NPC with the combat handler - the handler will manage the nameplate
        if (npc.isSpawned()) {
            handler.startCombatBehavior(npc, null);
            
            // Create custom nameplate after a small delay to ensure NPC is fully spawned
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (npc.isSpawned()) {
                    // Create a custom nameplate as a passenger armor stand
                    createHologramNameplate(npc, name, health, health);
                }
            }, 5L);
        }
        
        return npc;
    }

    /**
     * Create a combat NPC with custom stats
     */
    public NPC createCombatNPC(String id, String name, Location location, String skinName, NPCStats stats) {
        // Create a basic NPC without AI
        NPC npc = createNPC(id, name, location, skinName, false);
        
        // Make sure the NPC is NOT protected from Citizens' perspective to allow our custom damage handling
        npc.data().set(NPC.Metadata.DEFAULT_PROTECTED, false);
        
        // Set entity type to PLAYER for combat NPCs
        if (npc.hasTrait(net.citizensnpcs.api.trait.trait.MobType.class)) {
            npc.getTrait(net.citizensnpcs.api.trait.trait.MobType.class).setType(EntityType.PLAYER);
        }
        
        // Give it equipment if it supports equipment
        if (npc.hasTrait(net.citizensnpcs.api.trait.trait.Equipment.class)) {
            net.citizensnpcs.api.trait.trait.Equipment equipment = npc.getTrait(net.citizensnpcs.api.trait.trait.Equipment.class);
            equipment.set(net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot.HAND, new org.bukkit.inventory.ItemStack(Material.IRON_SWORD));
        }

        // IMPORTANT: Completely hide the default nameplate
        npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, false);
        
        // Make the entity vulnerable to allow our custom damage handling
        if (npc.isSpawned()) {
            // For entity interaction, set to NOT invulnerable so damage events fire
            npc.getEntity().setInvulnerable(false);
            
            // Completely hide the entity name
            npc.getEntity().setCustomNameVisible(false);
            npc.getEntity().setCustomName(null);
            
            // Store metadata for our custom damage handling
            npc.getEntity().setMetadata("custom_health", new FixedMetadataValue(plugin, stats.getMaxHealth()));
            npc.getEntity().setMetadata("max_health", new FixedMetadataValue(plugin, stats.getMaxHealth()));
            
            // Store the original clean name in metadata
            npc.getEntity().setMetadata("original_name", new FixedMetadataValue(plugin, name));
        }
        
        // Create and register combat handler
        CombatHandler handler = new CombatHandler(stats.getMaxHealth(), stats.getPhysicalDamage());
        handler.setNPCStats(npc.getUniqueId(), stats);
        registerInteractionHandler(id, handler);
        
        // Initialize the NPC with the combat handler - the handler will manage the nameplate
        if (npc.isSpawned()) {
            handler.startCombatBehavior(npc, null);
            
            // Create custom nameplate after a small delay to ensure NPC is fully spawned
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (npc.isSpawned()) {
                    // Create a custom nameplate as a passenger armor stand
                    createHologramNameplate(npc, name, stats.getMaxHealth(), stats.getMaxHealth());
                }
            }, 5L);
        }
        
        return npc;
    }

    // For backward compatibility
    public NPC createCombatNPC(String id, String name, Location location, String skinName, double health, double damage) {
        NPCStats stats = new NPCStats();
        stats.setMaxHealth(health);
        stats.setPhysicalDamage((int)damage);
        
        return createCombatNPC(id, name, location, skinName, stats);
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
        
        // Format: [Lv.1] ❈ Combat NPC ❤ 100/100
        String displayName = String.format("%s[Lv.%d] %s %s%s %s❤ %.1f/%.1f",
                org.bukkit.ChatColor.GRAY,
                level,
                org.bukkit.ChatColor.GOLD + "❈", // Gold elite marker for combat NPCs
                org.bukkit.ChatColor.WHITE,
                originalName,
                org.bukkit.ChatColor.RED,
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
            // Remove all passengers using Bukkit API
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
            
            plugin.getLogger().info("Removed NPC with ID: " + id);
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
                handler.onInteract(player, npc, rightClick);
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
    public NPC getNPC(UUID uuid) {
        return npcByUUID.get(uuid);
    }
    
    /**
     * Set equipment for an NPC
     * 
     * @param npcId The ID of the NPC
     * @param slot The equipment slot
     * @param item The item to set
     */
    public void setEquipment(String npcId, EquipmentSlot slot, org.bukkit.inventory.ItemStack item) {
        NPC npc = npcById.get(npcId);
        if (npc != null) {
            Equipment equipment = npc.getOrAddTrait(Equipment.class);
            equipment.set(slot, item);
        }
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
     * Get an NPC by UUID
     * 
     * @param uuid The UUID of the NPC
     * @return The NPC, or null if not found
     */
    public NPC getNPCByUUID(UUID uuid) {
        return npcByUUID.get(uuid);
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
}