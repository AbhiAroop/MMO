package com.server.entities.npc.story;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;

import com.server.Main;
import com.server.entities.npc.NPCInteractionHandler;
import com.server.entities.npc.NPCManager;

import net.citizensnpcs.api.npc.NPC;

/**
 * Registry for all story-related NPCs
 */
public class StoryNPCRegistry {
    
    private static StoryNPCRegistry instance;
    private final Main plugin;
    private final Map<String, NPCInteractionHandler> storyNPCs = new HashMap<>();
    
    private StoryNPCRegistry() {
        this.plugin = Main.getInstance();
        registerStoryNPCs();
    }
    
    /**
     * Get the instance of the StoryNPCRegistry
     * 
     * @return The single instance
     */
    public static StoryNPCRegistry getInstance() {
        if (instance == null) {
            instance = new StoryNPCRegistry();
        }
        return instance;
    }
    
    /**
     * Register all story NPCs
     */
    private void registerStoryNPCs() {
        // Register Kaelen the Echobound
        KaelenEchobound kaelen = new KaelenEchobound();
        storyNPCs.put("kaelen_echobound", kaelen);
        
        // Register additional story NPCs here as they are created
    }
    
    /**
     * Spawn a story NPC at the specified location
     * 
     * @param id The ID of the NPC
     * @param location The location to spawn at
     * @param skin The skin to use (or null for default)
     * @return True if the NPC was spawned
     */
    public boolean spawnStoryNPC(String id, Location location, String skin) {
        NPCInteractionHandler handler = storyNPCs.get(id);
        if (handler == null) {
            plugin.getLogger().warning("Attempted to spawn unknown story NPC: " + id);
            return false;
        }
        
        if (handler instanceof KaelenEchobound) {
            KaelenEchobound kaelen = (KaelenEchobound) handler;
            NPC npc = kaelen.spawn(location, skin);
            
            // CRITICAL FIX: Explicitly register the handler with the NPC manager to ensure interactions work
            NPCManager.getInstance().registerInteractionHandler(id, kaelen);
            
            // Log success
            plugin.getLogger().info("Successfully spawned story NPC: " + id + " with handler " + 
                                kaelen.getClass().getSimpleName());
            return true;
        }
        
        // Add additional NPC type checks as they are created
        
        return false;
    }
    
    /**
     * Get a story NPC by ID
     * 
     * @param id The ID of the NPC
     * @return The NPC handler, or null if not found
     */
    public NPCInteractionHandler getStoryNPC(String id) {
        return storyNPCs.get(id);
    }
}