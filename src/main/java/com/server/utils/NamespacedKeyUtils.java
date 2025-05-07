package com.server.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Utility class for managing NamespacedKey instances used across the plugin
 */
public class NamespacedKeyUtils {
    
    // Crystal-related keys
    public static NamespacedKey getCrystalKey(Plugin plugin) {
        return new NamespacedKey(plugin, "crystal_type");
    }
    
    public static NamespacedKey getCrystalQualityKey(Plugin plugin) {
        return new NamespacedKey(plugin, "crystal_quality");
    }
    
    public static NamespacedKey getCrystalSizeKey(Plugin plugin) {
        return new NamespacedKey(plugin, "crystal_size");
    }
    
    public static NamespacedKey getCrystalRotationKey(Plugin plugin) {
        return new NamespacedKey(plugin, "crystal_rotation");
    }

    public static NamespacedKey getCrystalTiltXKey(Plugin plugin) {
        return new NamespacedKey(plugin, "crystal_tiltx");
    }

    public static NamespacedKey getCrystalTiltZKey(Plugin plugin) {
        return new NamespacedKey(plugin, "crystal_tiltz");
    }
}