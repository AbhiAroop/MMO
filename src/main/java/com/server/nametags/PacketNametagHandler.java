package com.server.nametags;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Handles NMS packet sending for custom nametags
 * Uses reflection to work across different Minecraft versions
 */
public class PacketNametagHandler {
    
    private static Class<?> packetClass;
    private static Class<?> packetParamsClass;
    private static Constructor<?> packetConstructor;
    private static Method craftPlayerGetHandle;
    private static Method sendPacket;
    private static Field playerConnectionField;
    
    // Packet fields
    private static Field teamNameField;
    private static Field modeField;
    private static Field paramsField;
    private static Field playersField;
    
    // Params fields (for 1.17+)
    private static Field displayNameField;
    private static Field prefixField;
    private static Field suffixField;
    private static Field nametagVisibilityField;
    private static Field collisionRuleField;
    private static Field colorField;
    private static Field optionsField;
    
    private static boolean initialized = false;
    private static String error = null;
    
    static {
        try {
            initializeReflection();
            initialized = true;
        } catch (Exception e) {
            error = "Failed to initialize packet handler: " + e.getMessage();
            e.printStackTrace();
        }
    }
    
    private static void initializeReflection() throws Exception {
        // Get the server version (craftbukkit package version)
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        String[] parts = packageName.split("\\.");
        String version = parts.length > 3 ? parts[3] : ""; // e.g., "v1_21_R1" or empty for modern Paper
        
        // For 1.17+ (Paper uses mapped names)
        try {
            // Try Paper's mapped names first (1.17+)
            packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket");
            packetParamsClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket$Parameters");
            
            // Get packet constructor
            packetConstructor = packetClass.getDeclaredConstructor(String.class, int.class, Optional.class, Collection.class);
            packetConstructor.setAccessible(true);
            
            // Get params constructor
            Constructor<?> paramsConstructor = packetParamsClass.getDeclaredConstructors()[0];
            paramsConstructor.setAccessible(true);
            
            // Packet fields
            teamNameField = packetClass.getDeclaredField("name");
            teamNameField.setAccessible(true);
            modeField = packetClass.getDeclaredField("method");
            modeField.setAccessible(true);
            paramsField = packetClass.getDeclaredField("parameters");
            paramsField.setAccessible(true);
            playersField = packetClass.getDeclaredField("players");
            playersField.setAccessible(true);
            
            // Params fields
            displayNameField = packetParamsClass.getDeclaredField("displayName");
            displayNameField.setAccessible(true);
            prefixField = packetParamsClass.getDeclaredField("playerPrefix");
            prefixField.setAccessible(true);
            suffixField = packetParamsClass.getDeclaredField("playerSuffix");
            suffixField.setAccessible(true);
            nametagVisibilityField = packetParamsClass.getDeclaredField("nametagVisibility");
            nametagVisibilityField.setAccessible(true);
            collisionRuleField = packetParamsClass.getDeclaredField("collisionRule");
            collisionRuleField.setAccessible(true);
            colorField = packetParamsClass.getDeclaredField("color");
            colorField.setAccessible(true);
            optionsField = packetParamsClass.getDeclaredField("options");
            optionsField.setAccessible(true);
            
            // Get player connection - try both versioned and non-versioned paths
            Class<?> craftPlayerClass;
            try {
                // Try versioned path first (older versions)
                craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            } catch (ClassNotFoundException e) {
                // Try non-versioned path (modern Paper)
                craftPlayerClass = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
            }
            craftPlayerGetHandle = craftPlayerClass.getMethod("getHandle");
            
            Class<?> entityPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
            playerConnectionField = entityPlayerClass.getField("connection");
            
            Class<?> playerConnectionClass = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl");
            sendPacket = playerConnectionClass.getMethod("send", Class.forName("net.minecraft.network.protocol.Packet"));
            
        } catch (ClassNotFoundException e) {
            throw new Exception("Could not find NMS classes: " + e.getMessage() + " (version: " + version + ")");
        }
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
    
    public static String getError() {
        return error;
    }
    
    /**
     * Create a team with displayName, prefix and suffix
     * Mode 0 = CREATE team with parameters
     */
    public static void createTeam(Player player, String teamName, String displayName, String prefix, String suffix) {
        if (!initialized) {
            Bukkit.getLogger().warning("Packet handler not initialized: " + error);
            return;
        }
        
        try {
            // Create parameters object
            Object params = createParameters(displayName, prefix, suffix);
            
            // Create packet: teamName, mode=0 (create), Optional.of(params), players list
            Object packet = packetConstructor.newInstance(
                teamName,
                0, // Mode 0 = CREATE
                Optional.of(params),
                Collections.singletonList(player.getName())
            );
            
            // Send to all online players
            for (Player online : Bukkit.getOnlinePlayers()) {
                sendPacket(online, packet);
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to create team packet: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Update team with displayName, prefix and suffix
     * Mode 2 = UPDATE team information
     */
    public static void updateTeam(Player player, String teamName, String displayName, String prefix, String suffix) {
        if (!initialized) {
            return;
        }
        
        try {
            // Create parameters object
            Object params = createParameters(displayName, prefix, suffix);
            
            // Create packet: teamName, mode=2 (update), Optional.of(params), empty players list
            Object packet = packetConstructor.newInstance(
                teamName,
                2, // Mode 2 = UPDATE
                Optional.of(params),
                Collections.emptyList()
            );
            
            // Send to all online players
            for (Player online : Bukkit.getOnlinePlayers()) {
                sendPacket(online, packet);
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to update team packet: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Remove team
     * Mode 1 = REMOVE team
     */
    public static void removeTeam(String teamName) {
        if (!initialized) {
            return;
        }
        
        try {
            // Create packet: teamName, mode=1 (remove), Optional.empty(), empty players list
            Object packet = packetConstructor.newInstance(
                teamName,
                1, // Mode 1 = REMOVE
                Optional.empty(),
                Collections.emptyList()
            );
            
            // Send to all online players
            for (Player online : Bukkit.getOnlinePlayers()) {
                sendPacket(online, packet);
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to remove team packet: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create a Parameters object with display name, prefix, suffix, etc.
     * Uses reflection to set fields directly to avoid constructor signature issues across versions
     */
    private static Object createParameters(String displayName, String prefix, String suffix) throws Exception {
        // Get Component class and create text components
        Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
        Method literalMethod = componentClass.getMethod("literal", String.class);
        
        Object displayNameComponent = literalMethod.invoke(null, displayName);
        Object prefixComponent = literalMethod.invoke(null, prefix);
        Object suffixComponent = literalMethod.invoke(null, suffix);
        
        // Get ChatFormatting enum (for color)
        Class<?> chatFormattingClass = Class.forName("net.minecraft.ChatFormatting");
        Object resetColor = Enum.valueOf((Class<Enum>) chatFormattingClass, "RESET");
        
        // Create Parameters object using Unsafe allocation (no constructor call)
        sun.misc.Unsafe unsafe = getUnsafe();
        Object params = unsafe.allocateInstance(packetParamsClass);
        
        // Set all fields directly
        displayNameField.set(params, displayNameComponent);
        prefixField.set(params, prefixComponent);
        suffixField.set(params, suffixComponent);
        
        // In 1.21.4+, nametagVisibility and collisionRule are Strings, not enums
        // Check field type and set appropriately
        if (nametagVisibilityField.getType() == String.class) {
            // New format: String values
            nametagVisibilityField.set(params, "always");
            collisionRuleField.set(params, "always");
        } else {
            // Old format: Enum values
            Class<?> visibilityClass = Class.forName("net.minecraft.world.scores.Team$Visibility");
            Object alwaysVisible = Enum.valueOf((Class<Enum>) visibilityClass, "ALWAYS");
            Class<?> collisionRuleClass = Class.forName("net.minecraft.world.scores.Team$CollisionRule");
            Object alwaysCollide = Enum.valueOf((Class<Enum>) collisionRuleClass, "ALWAYS");
            nametagVisibilityField.set(params, alwaysVisible);
            collisionRuleField.set(params, alwaysCollide);
        }
        
        colorField.set(params, resetColor);
        optionsField.set(params, 0);
        
        return params;
    }
    
    /**
     * Get Unsafe instance for direct memory allocation
     */
    private static sun.misc.Unsafe getUnsafe() throws Exception {
        java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (sun.misc.Unsafe) f.get(null);
    }
    
    /**
     * Send a packet to a player
     */
    private static void sendPacket(Player player, Object packet) throws Exception {
        Object handle = craftPlayerGetHandle.invoke(player);
        Object connection = playerConnectionField.get(handle);
        sendPacket.invoke(connection, packet);
    }
}
