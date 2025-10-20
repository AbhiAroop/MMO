package com.server.enchantments.effects;

import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import com.server.enchantments.elements.ElementType;
import com.server.enchantments.elements.HybridElement;

/**
 * Utility class for spawning element-based particle effects.
 * Provides different particle effects based on elemental types.
 */
public class ElementalParticles {
    
    private static final Random random = new Random();
    
    /**
     * Spawns a burst of elemental particles at a location.
     * 
     * @param location The center location for the particles
     * @param element The element type determining particle appearance
     * @param intensity Multiplier for particle count (1.0 = normal, 2.0 = double)
     */
    public static void spawnElementalBurst(Location location, ElementType element, double intensity) {
        if (location == null || element == null || location.getWorld() == null) {
            return;
        }
        
        int baseCount = (int) (20 * intensity);
        double spread = 0.5;
        
        switch (element) {
            case FIRE:
                location.getWorld().spawnParticle(
                    Particle.FLAME,
                    location,
                    baseCount,
                    spread, spread, spread,
                    0.05
                );
                location.getWorld().spawnParticle(
                    Particle.LAVA,
                    location,
                    (int) (baseCount * 0.3),
                    spread * 0.5, spread * 0.5, spread * 0.5,
                    0
                );
                break;
                
            case WATER:
                location.getWorld().spawnParticle(
                    Particle.SPLASH,
                    location,
                    baseCount,
                    spread, spread, spread,
                    0.1
                );
                location.getWorld().spawnParticle(
                    Particle.DRIPPING_WATER,
                    location.clone().add(0, 1, 0),
                    (int) (baseCount * 0.5),
                    spread, 0.3, spread,
                    0
                );
                break;
                
            case EARTH:
                Particle.DustOptions earthDust = new Particle.DustOptions(
                    Color.fromRGB(139, 90, 43),
                    1.5f
                );
                location.getWorld().spawnParticle(
                    Particle.DUST,
                    location,
                    baseCount,
                    spread, spread, spread,
                    0,
                    earthDust
                );
                location.getWorld().spawnParticle(
                    Particle.BLOCK,
                    location,
                    (int) (baseCount * 0.4),
                    spread, spread, spread,
                    0,
                    org.bukkit.Material.BROWN_TERRACOTTA.createBlockData()
                );
                break;
                
            case AIR:
                location.getWorld().spawnParticle(
                    Particle.CLOUD,
                    location,
                    baseCount,
                    spread, spread, spread,
                    0.08
                );
                location.getWorld().spawnParticle(
                    Particle.SWEEP_ATTACK,
                    location,
                    (int) (baseCount * 0.2),
                    spread * 0.7, spread * 0.7, spread * 0.7,
                    0
                );
                break;
                
            case LIGHTNING:
                location.getWorld().spawnParticle(
                    Particle.ELECTRIC_SPARK,
                    location,
                    baseCount,
                    spread, spread, spread,
                    0.15
                );
                location.getWorld().spawnParticle(
                    Particle.END_ROD,
                    location,
                    (int) (baseCount * 0.3),
                    spread * 0.5, spread * 0.5, spread * 0.5,
                    0.05
                );
                break;
                
            case SHADOW:
                location.getWorld().spawnParticle(
                    Particle.SQUID_INK,
                    location,
                    baseCount,
                    spread, spread, spread,
                    0.05
                );
                location.getWorld().spawnParticle(
                    Particle.SMOKE,
                    location,
                    (int) (baseCount * 0.5),
                    spread * 0.8, spread * 0.8, spread * 0.8,
                    0.02
                );
                break;
                
            case LIGHT:
                location.getWorld().spawnParticle(
                    Particle.GLOW,
                    location,
                    baseCount,
                    spread, spread, spread,
                    0.1
                );
                location.getWorld().spawnParticle(
                    Particle.END_ROD,
                    location,
                    (int) (baseCount * 0.4),
                    spread * 0.6, spread * 0.6, spread * 0.6,
                    0.08
                );
                break;
                
            case NATURE:
                Particle.DustOptions natureDust = new Particle.DustOptions(
                    Color.fromRGB(34, 139, 34),
                    1.5f
                );
                location.getWorld().spawnParticle(
                    Particle.DUST,
                    location,
                    baseCount,
                    spread, spread, spread,
                    0,
                    natureDust
                );
                location.getWorld().spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    location,
                    (int) (baseCount * 0.3),
                    spread, spread, spread,
                    0
                );
                break;
        }
    }
    
    /**
     * Spawns a burst for a hybrid element.
     * 
     * @param location The center location
     * @param hybrid The hybrid element type
     * @param intensity Particle intensity multiplier
     */
    public static void spawnHybridBurst(Location location, HybridElement hybrid, double intensity) {
        if (location == null || hybrid == null || location.getWorld() == null) {
            return;
        }
        
        // Spawn both component elements with reduced intensity
        spawnElementalBurst(location, hybrid.getElement1(), intensity * 0.5);
        spawnElementalBurst(location, hybrid.getElement2(), intensity * 0.5);
        
        // Add hybrid-specific accent particles
        int accentCount = (int) (10 * intensity);
        Particle.DustOptions hybridColor = new Particle.DustOptions(
            getHybridColor(hybrid),
            2.0f
        );
        
        location.getWorld().spawnParticle(
            Particle.DUST,
            location.clone().add(0, 0.5, 0),
            accentCount,
            0.3, 0.3, 0.3,
            0,
            hybridColor
        );
    }
    
    /**
     * Spawns ambient particles around a structure (altar/anvil).
     * Creates a slow spiral effect.
     * 
     * @param location The center of the structure
     * @param element The dominant element type (null for mixed)
     * @param tickOffset Offset for animation timing
     */
    public static void spawnAmbientEffect(Location location, ElementType element, int tickOffset) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        
        // Create spiral pattern
        double angle = (tickOffset * 0.1) % (2 * Math.PI);
        double radius = 1.2;
        double height = Math.sin(angle * 2) * 0.5;
        
        double x = Math.cos(angle) * radius;
        double z = Math.sin(angle) * radius;
        
        Location particleLoc = location.clone().add(x, height + 1.0, z);
        
        if (element == null) {
            // Mixed/rainbow effect for structures without specific element
            Particle.DustOptions rainbowDust = new Particle.DustOptions(
                Color.fromRGB(
                    (int) (Math.sin(angle) * 127 + 128),
                    (int) (Math.sin(angle + 2) * 127 + 128),
                    (int) (Math.sin(angle + 4) * 127 + 128)
                ),
                1.0f
            );
            location.getWorld().spawnParticle(
                Particle.DUST,
                particleLoc,
                2,
                0.1, 0.1, 0.1,
                0,
                rainbowDust
            );
        } else {
            // Element-specific ambient particle
            switch (element) {
                case FIRE:
                    location.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
                    break;
                case WATER:
                    location.getWorld().spawnParticle(Particle.SPLASH, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                    break;
                case EARTH:
                    Particle.DustOptions earthDust = new Particle.DustOptions(Color.fromRGB(139, 90, 43), 0.8f);
                    location.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0.05, 0.05, 0.05, 0, earthDust);
                    break;
                case AIR:
                    location.getWorld().spawnParticle(Particle.CLOUD, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
                    break;
                case LIGHTNING:
                    location.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0.05, 0.05, 0.05, 0.02);
                    break;
                case SHADOW:
                    location.getWorld().spawnParticle(Particle.SQUID_INK, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                    break;
                case LIGHT:
                    location.getWorld().spawnParticle(Particle.GLOW, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                    break;
                case NATURE:
                    Particle.DustOptions natureDust = new Particle.DustOptions(Color.fromRGB(34, 139, 34), 0.8f);
                    location.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0.05, 0.05, 0.05, 0, natureDust);
                    break;
            }
        }
    }
    
    /**
     * Spawns a helix of particles around the player.
     * 
     * @param location Player's location
     * @param element The element type
     * @param duration How many ticks the effect should last (affects spiral height)
     */
    public static void spawnPlayerHelix(Location location, ElementType element, int duration) {
        if (location == null || element == null || location.getWorld() == null) {
            return;
        }
        
        // Create double helix pattern
        int particles = 30;
        double radius = 0.8;
        double heightStep = 2.5 / particles;
        
        for (int i = 0; i < particles; i++) {
            double angle1 = (i / (double) particles) * Math.PI * 4;
            double angle2 = angle1 + Math.PI;
            double height = i * heightStep;
            
            // First helix strand
            Vector offset1 = new Vector(
                Math.cos(angle1) * radius,
                height,
                Math.sin(angle1) * radius
            );
            Location particleLoc1 = location.clone().add(offset1);
            
            // Second helix strand
            Vector offset2 = new Vector(
                Math.cos(angle2) * radius,
                height,
                Math.sin(angle2) * radius
            );
            Location particleLoc2 = location.clone().add(offset2);
            
            spawnSingleElementalParticle(particleLoc1, element);
            spawnSingleElementalParticle(particleLoc2, element);
        }
    }
    
    /**
     * Spawns a ring of particles around a location (used for successful enchanting).
     * 
     * @param location Center location
     * @param element Element type
     * @param radius Ring radius
     */
    public static void spawnElementalRing(Location location, ElementType element, double radius) {
        if (location == null || element == null || location.getWorld() == null) {
            return;
        }
        
        int particles = 24;
        for (int i = 0; i < particles; i++) {
            double angle = (i / (double) particles) * Math.PI * 2;
            Vector offset = new Vector(
                Math.cos(angle) * radius,
                0.1,
                Math.sin(angle) * radius
            );
            
            Location particleLoc = location.clone().add(offset);
            spawnSingleElementalParticle(particleLoc, element);
            
            // Add upward motion
            if (i % 3 == 0) {
                Location upwardLoc = particleLoc.clone().add(0, 0.5, 0);
                spawnSingleElementalParticle(upwardLoc, element);
            }
        }
    }
    
    /**
     * Spawns a single particle of the appropriate element type.
     */
    private static void spawnSingleElementalParticle(Location location, ElementType element) {
        if (location == null || element == null || location.getWorld() == null) {
            return;
        }
        
        switch (element) {
            case FIRE:
                location.getWorld().spawnParticle(Particle.FLAME, location, 1, 0, 0, 0, 0.01);
                break;
            case WATER:
                location.getWorld().spawnParticle(Particle.SPLASH, location, 1, 0, 0, 0, 0);
                break;
            case EARTH:
                Particle.DustOptions earthDust = new Particle.DustOptions(Color.fromRGB(139, 90, 43), 1.0f);
                location.getWorld().spawnParticle(Particle.DUST, location, 1, 0, 0, 0, 0, earthDust);
                break;
            case AIR:
                location.getWorld().spawnParticle(Particle.CLOUD, location, 1, 0.05, 0.05, 0.05, 0);
                break;
            case LIGHTNING:
                location.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, location, 1, 0, 0, 0, 0.01);
                break;
            case SHADOW:
                location.getWorld().spawnParticle(Particle.SQUID_INK, location, 1, 0, 0, 0, 0);
                break;
            case LIGHT:
                location.getWorld().spawnParticle(Particle.END_ROD, location, 1, 0, 0, 0, 0.01);
                break;
            case NATURE:
                Particle.DustOptions natureDust = new Particle.DustOptions(Color.fromRGB(34, 139, 34), 1.0f);
                location.getWorld().spawnParticle(Particle.DUST, location, 1, 0, 0, 0, 0, natureDust);
                break;
        }
    }
    
    /**
     * Gets the color associated with a hybrid element.
     */
    private static Color getHybridColor(HybridElement hybrid) {
        switch (hybrid) {
            case ICE:
                return Color.fromRGB(135, 206, 250); // Light sky blue
            case STORM:
                return Color.fromRGB(255, 215, 0); // Gold
            case MIST:
                return Color.fromRGB(176, 224, 230); // Powder blue
            case DECAY:
                return Color.fromRGB(105, 105, 105); // Dim gray
            case RADIANCE:
                return Color.fromRGB(255, 250, 205); // Lemon chiffon
            case ASH:
                return Color.fromRGB(128, 128, 128); // Gray
            case PURITY:
                return Color.fromRGB(248, 248, 255); // Ghost white
            default:
                return Color.WHITE;
        }
    }
}
