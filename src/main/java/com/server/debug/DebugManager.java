package com.server.debug;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;

import com.server.Main;

/**
 * Manages debug flags for different systems in the plugin
 */
public class DebugManager {
    
    private static DebugManager instance;
    private final Main plugin;
    private final Logger logger;
    private final Map<DebugSystem, Boolean> debugFlags = new HashMap<>();
    
    /**
     * Available debug systems that can be toggled independently
     */
    public enum DebugSystem {
        ALL("all", "Toggle all debugging"),
        STATS("stats", "Player stats system"),
        NPC("npc", "NPC system"),
        COMBAT("combat", "Combat system"),
        SKILLS("skills", "Skills system"),
        ABILITIES("abilities", "Abilities system"),
        GUI("gui", "GUI system"),
        ITEMS("items", "Items system"),
        MINING("mining", "Mining system"),
        FARMING("farming", "Farming system"),
        FISHING("fishing", "Fishing system"),
        ANIMATION("animation", "Animation system"),
        DIALOGUE("dialogue", "NPC dialogue system"),
        ENTITY("entity", "Custom entity system"),
        PROFILE("profile", "Profile system"),
        ENCHANTING("enchanting", "Enchanting system and stat processing"),
        ALTAR("altar", "Altar system"),
        BREEDING("breeding", "Botany breeding system");
        
        private final String id;
        private final String description;
        
        DebugSystem(String id, String description) {
            this.id = id;
            this.description = description;
        }
        
        public String getId() {
            return id;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static DebugSystem fromId(String id) {
            for (DebugSystem system : values()) {
                if (system.getId().equalsIgnoreCase(id)) {
                    return system;
                }
            }
            return null;
        }
    }
    
    private DebugManager(Main plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        loadDebugSettings();
    }
    
    /**
     * Get the instance of the DebugManager
     * 
     * @return The DebugManager instance
     */
    public static DebugManager getInstance() {
        if (instance == null) {
            instance = new DebugManager(Main.getInstance());
        }
        return instance;
    }
    
    /**
     * Load debug settings from config
     */
    private void loadDebugSettings() {
        FileConfiguration config = plugin.getConfig();
        boolean globalDebugMode = config.getBoolean("debug-mode", false);
        
        // Set default values for all systems based on global setting
        for (DebugSystem system : DebugSystem.values()) {
            if (system == DebugSystem.ALL) {
                debugFlags.put(system, globalDebugMode);
            } else {
                String configPath = "debug-systems." + system.getId();
                debugFlags.put(system, config.getBoolean(configPath, globalDebugMode));
            }
        }
        
        // Ensure values are saved to config
        saveDebugSettings();
    }
    
    /**
     * Save debug settings to config
     */
    private void saveDebugSettings() {
        FileConfiguration config = plugin.getConfig();
        
        // Save global debug mode
        config.set("debug-mode", debugFlags.getOrDefault(DebugSystem.ALL, false));
        
        // Save each system's debug mode
        for (DebugSystem system : DebugSystem.values()) {
            if (system != DebugSystem.ALL) {
                config.set("debug-systems." + system.getId(), isDebugEnabled(system));
            }
        }
        
        plugin.saveConfig();
    }
    
    /**
     * Check if debugging is enabled for a specific system
     * 
     * @param system The system to check
     * @return true if debugging is enabled
     */
    public boolean isDebugEnabled(DebugSystem system) {
        // For the ALL system, just return its flag directly
        if (system == DebugSystem.ALL) {
            return debugFlags.getOrDefault(DebugSystem.ALL, false);
        }
        
        // For individual systems, check if ALL is enabled OR the specific system is enabled
        boolean allEnabled = debugFlags.getOrDefault(DebugSystem.ALL, false);
        boolean systemEnabled = debugFlags.getOrDefault(system, false);
        
        return allEnabled || systemEnabled;
    }
    
    /**
     * Set debugging state for a specific system
     * 
     * @param system The system to set
     * @param enabled Whether debugging should be enabled
     */
    public void setDebugEnabled(DebugSystem system, boolean enabled) {
        debugFlags.put(system, enabled);
        
        // Special handling for ALL
        if (system == DebugSystem.ALL) {
            if (enabled) {
                // When ALL is enabled, enable all systems
                for (DebugSystem sys : DebugSystem.values()) {
                    if (sys != DebugSystem.ALL) {
                        debugFlags.put(sys, true);
                    }
                }
            } else {
                // When ALL is disabled, disable all systems
                for (DebugSystem sys : DebugSystem.values()) {
                    if (sys != DebugSystem.ALL) {
                        debugFlags.put(sys, false);
                    }
                }
            }
        }
        
        // Save changes to config
        saveDebugSettings();
    }
    
    /**
     * Toggle debugging for a specific system
     * 
     * @param system The system to toggle
     * @return The new state
     */
    public boolean toggleDebug(DebugSystem system) {
        boolean currentState = isDebugEnabled(system);
        boolean newState = !currentState;
        setDebugEnabled(system, newState);
        return newState;
    }
    
    /**
     * Get all systems that have debugging enabled
     * 
     * @return Set of enabled debug systems
     */
    public Set<DebugSystem> getEnabledSystems() {
        Set<DebugSystem> enabledSystems = new java.util.HashSet<>();
        
        for (DebugSystem system : DebugSystem.values()) {
            if (isDebugEnabled(system)) {
                enabledSystems.add(system);
            }
        }
        
        return enabledSystems;
    }
    
    /**
     * Log a debug message for a specific system
     * 
     * @param system The system logging the message
     * @param message The message to log
     */
    public void debug(DebugSystem system, String message) {
        if (isDebugEnabled(system)) {
            logger.info("[DEBUG:" + system.getId().toUpperCase() + "] " + message);
        }
    }
}