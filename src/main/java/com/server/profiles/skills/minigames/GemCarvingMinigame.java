package com.server.profiles.skills.minigames;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import static org.bukkit.Material.EMERALD;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.core.SkillProgressionManager;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.core.SubskillType;
import com.server.profiles.skills.display.SkillActionBarManager;
import com.server.profiles.skills.skills.mining.subskills.GemCarvingSubskill;
import com.server.utils.NamespacedKeyUtils;

/**
 * Minigame for the GemCarving skill - Player must hit particles that appear
 * around a crystal to extract gems from it.
 */
public class GemCarvingMinigame {
    private final Main plugin;
    private final Map<UUID, GameSession> activeSessions = new HashMap<>();
    private final Random random = new Random();
    
    // Set to keep track of crystals on cooldown with their expiry time
    private final Map<UUID, Long> crystalCooldowns = new HashMap<>();
    
    // Constants for game mechanics
    private static final int REQUIRED_HITS = 5;
    private static final int BASE_GAME_TIMEOUT_TICKS = 300; // 15 seconds total
    
    // Default values for lowest tier crystal (Mooncrystal) - Baseline difficulty
    private static final double BASE_PARTICLE_RANGE = 1.0;
    private static final double BASE_PARTICLE_RADIUS = 0.4; // Base hitbox size
    private static final int BASE_PARTICLE_TIMEOUT_TICKS = 60; // 3 seconds at 20 ticks/sec
    private static final int BASE_CRYSTAL_COOLDOWN_SECONDS = 30; // 30 seconds cooldown
    
    // Crystal difficulty and reward configurations
    private static final Map<String, CrystalTier> CRYSTAL_TIERS = new HashMap<>();
    
    // Particle effects for different parts of the minigame
    private static final Particle.DustOptions COMPLETE_PARTICLE = new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.5f);
    private static final Particle.DustOptions COOLDOWN_PARTICLE = new Particle.DustOptions(Color.fromRGB(150, 150, 150), 0.7f);
    private static final Particle.DustOptions FAIL_PARTICLE = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f);
    
    // Initialize crystal tiers with their difficulty and rewards
    static {
        // Tier 1 - Mooncrystal (Base/reference crystal)
        CRYSTAL_TIERS.put("mooncrystal", new CrystalTier(
            1.0,   // Relative particle radius
            1.0,   // Relative timeout duration
            1.0,   // Relative cooldown
            0.0,   // Extra difficulty (for extraction success rate)
            100.0, // Base XP reward
            Color.fromRGB(220, 220, 255),  // Light blue-white color
            5      // 5 required hits (baseline)
        ));
        
        // Tier 2 - Azuralite - Slightly more difficult
        CRYSTAL_TIERS.put("azuralite", new CrystalTier(
            0.85,  // Smaller hitbox (reduced from 0.9)
            0.9,   // Slightly less time
            1.1,   // Slightly longer cooldown
            0.05,  // 5% more difficult for extraction
            125.0, // XP reward
            Color.fromRGB(100, 170, 255),  // Azure blue color
            5      // Same as tier 1
        ));
        
        // Tier 3 - Pyrethine - Moderately difficult
        CRYSTAL_TIERS.put("pyrethine", new CrystalTier(
            0.75,  // Even smaller hitbox
            0.8,   // Less time
            1.2,   // Longer cooldown
            0.1,   // 10% more difficult
            150.0, // XP reward
            Color.fromRGB(255, 150, 50),   // Fiery orange color
            6      // Increased to 6 hits
        ));
        
        // Tier 4 - Solvane Crystal - Challenging
        CRYSTAL_TIERS.put("solvanecystal", new CrystalTier(
            0.65,  // Much smaller hitbox
            0.75,  // Less time
            1.3,   // Longer cooldown
            0.15,  // 15% more difficult
            175.0, // XP reward
            Color.fromRGB(220, 220, 100),  // Yellow-gold color
            6      // Same as tier 3
        ));
        
        // Tier 5 - Nyxstone - Very challenging
        CRYSTAL_TIERS.put("nyxstone", new CrystalTier(
            0.55,  // Tiny hitbox
            0.7,   // Even less time
            1.5,   // Even longer cooldown
            0.2,   // 20% more difficult
            200.0, // XP reward
            Color.fromRGB(80, 10, 120),    // Deep purple color
            7      // Increased to 7 hits
        ));
        
        // Tier 6 - Lucenthar - Extremely challenging
        CRYSTAL_TIERS.put("lucenthar", new CrystalTier(
            0.45,  // Very tiny hitbox
            0.65,  // Very little time
            1.7,   // Much longer cooldown
            0.25,  // 25% more difficult
            250.0, // XP reward
            Color.fromRGB(200, 255, 200),  // Light green color
            7      // Same as tier 5
        ));
        
        // Tier 7 - Veyrith Crystal - Expert level
        CRYSTAL_TIERS.put("veyrithcrystal", new CrystalTier(
            0.35,  // Extremely small hitbox
            0.55,  // Extremely little time
            2.0,   // Very long cooldown
            0.3,   // 30% more difficult
            350.0, // XP reward
            Color.fromRGB(255, 100, 255),  // Magenta color
            8      // Increased to 8 hits
        ));
        
        // Tier 8 - Drakthyst (highest tier) - Master level
        CRYSTAL_TIERS.put("drakthyst", new CrystalTier(
            0.25,  // Nearly impossible hitbox
            0.45,  // Nearly impossibly fast
            2.5,   // Extremely long cooldown
            0.35,  // 35% more difficult
            500.0, // XP reward
            Color.fromRGB(255, 50, 50),    // Deep red color
            9      // Increased to 9 hits
        ));
    }
    
    public GemCarvingMinigame(Main plugin) {
        this.plugin = plugin;
        
        // Start a cleanup task for cooldowns
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredCooldowns();
            }
        }.runTaskTimer(plugin, 20 * 60, 20 * 60); // Run every minute
    }
    
    /**
     * Start a gem carving minigame if a player clicked near a crystal
     */
    public boolean tryStartGame(Player player, Location clickLocation) {
        // Check if player is already in a minigame
        if (isPlayerInGame(player)) {
            return false;
        }
        
        // Find nearby crystal armor stands
        ArmorStand crystal = findNearbyCrystal(clickLocation);
        if (crystal == null) {
            return false;
        }
        
        // Get crystal info
        String crystalType = getCrystalType(crystal);
        int quality = getCrystalQuality(crystal);
        
        // Get crystal tier info or use default if not found
        CrystalTier tier = CRYSTAL_TIERS.getOrDefault(crystalType.toLowerCase(), CRYSTAL_TIERS.get("mooncrystal"));
        
        // Check if crystal is on cooldown
        if (isCrystalOnCooldown(crystal)) {
            player.sendMessage("§8This crystal is cooling down after a recent extraction attempt.");
            // Show visual cooldown effect with tier-specific color
            showCooldownEffect(crystal, tier);
            return false;
        }
        
        // Check if the player has the required tool
        if (!hasGemCarvingTool(player)) {
            player.sendMessage("§cYou need a special tool to carve gems!");
            return false;
        }
        
        // Start a new game session
        GameSession session = new GameSession(player, crystal, crystalType, quality, tier);
        activeSessions.put(player.getUniqueId(), session);
        session.start();
        
        return true;
    }
    
    /**
     * Check if a crystal is currently on cooldown
     */
    private boolean isCrystalOnCooldown(ArmorStand crystal) {
        UUID crystalId = crystal.getUniqueId();
        if (!crystalCooldowns.containsKey(crystalId)) {
            return false;
        }
        
        long expiryTime = crystalCooldowns.get(crystalId);
        long currentTime = System.currentTimeMillis();
        
        return currentTime < expiryTime;
    }
    
    /**
     * Show a visual effect to indicate the crystal is on cooldown
     */
    private void showCooldownEffect(ArmorStand crystal, CrystalTier tier) {
        // Calculate remaining cooldown
        long remainingSecs = (crystalCooldowns.get(crystal.getUniqueId()) - System.currentTimeMillis()) / 1000;
        
        // Create a colored dust cloud around the crystal using tier-specific color
        Location effectLoc = crystal.getLocation().clone().add(0, 1.0, 0);
        
        // Custom cooldown particle for this tier
        Particle.DustOptions dustOptions = new Particle.DustOptions(
                tier.getParticleColor().mixColors(Color.GRAY), // Mix with gray for cooldown effect
                0.7f);
        
        crystal.getWorld().spawnParticle(Particle.DUST, 
                effectLoc, 10, 0.3, 0.3, 0.3, 0, dustOptions);
        
        // Add a subtle smoke effect
        crystal.getWorld().spawnParticle(Particle.SMOKE, 
                effectLoc, 5, 0.2, 0.2, 0.2, 0.01);
    }
    
    /**
     * Clean up expired cooldowns periodically
     */
    private void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();
        Set<UUID> toRemove = new HashSet<>();
        
        for (Map.Entry<UUID, Long> entry : crystalCooldowns.entrySet()) {
            if (entry.getValue() < currentTime) {
                toRemove.add(entry.getKey());
            }
        }
        
        for (UUID id : toRemove) {
            crystalCooldowns.remove(id);
        }
        
        if (!toRemove.isEmpty() && plugin.isDebugMode()) {
            plugin.getLogger().info("Cleaned up " + toRemove.size() + " expired crystal cooldowns");
        }
    }
    
    /**
     * Put a crystal on cooldown based on its tier
     */
    private void setCrystalCooldown(ArmorStand crystal, CrystalTier tier) {
        int cooldownSeconds = (int)(BASE_CRYSTAL_COOLDOWN_SECONDS * tier.getCooldownMultiplier());
        long expiryTime = System.currentTimeMillis() + (cooldownSeconds * 1000);
        crystalCooldowns.put(crystal.getUniqueId(), expiryTime);
    }
    
    /**
     * Check if a player is already in a gem carving game
     */
    public boolean isPlayerInGame(Player player) {
        return activeSessions.containsKey(player.getUniqueId()) && 
               activeSessions.get(player.getUniqueId()).isActive();
    }
    
    /**
     * Handle a player's click during the minigame
     */
    public void handleClick(Player player, Location clickLocation) {
        GameSession session = activeSessions.get(player.getUniqueId());
        if (session == null || !session.isActive()) {
            return;
        }
        
        // Use ray tracing to allow hitting through blocks
        session.handleRayTracedClick(player.getEyeLocation(), player.getEyeLocation().getDirection());
    }
    
    /**
     * End a player's minigame session
     */
    public void endSession(UUID playerId, boolean removeFromMap) {
        GameSession session = activeSessions.get(playerId);
        if (session != null) {
            session.endGame(false);
            if (removeFromMap) {
                activeSessions.remove(playerId);
            }
        }
    }
    
    /**
     * Check if player has a valid gem carving tool
     */
    private boolean hasGemCarvingTool(Player player) {
        // For now, accept pickaxes or shears as valid tools for gem carving
        Material mainHand = player.getInventory().getItemInMainHand().getType();
        return mainHand.name().contains("PICKAXE") || mainHand == Material.SHEARS;
    }
    
    /**
     * Find the nearest crystal armor stand to the specified location
     */
    private ArmorStand findNearbyCrystal(Location location) {
        double range = 2.0;
        
        for (Entity entity : location.getWorld().getNearbyEntities(location, range, range, range)) {
            if (entity instanceof ArmorStand) {
                ArmorStand stand = (ArmorStand) entity;
                PersistentDataContainer container = stand.getPersistentDataContainer();
                
                if (container.has(NamespacedKeyUtils.getCrystalKey(plugin), PersistentDataType.STRING)) {
                    // This is a crystal armor stand
                    return stand;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get the crystal type from an armor stand
     */
    private String getCrystalType(ArmorStand stand) {
        PersistentDataContainer container = stand.getPersistentDataContainer();
        return container.getOrDefault(NamespacedKeyUtils.getCrystalKey(plugin), PersistentDataType.STRING, "mooncrystal");
    }
    
    /**
     * Get the crystal quality from an armor stand
     */
    private int getCrystalQuality(ArmorStand stand) {
        PersistentDataContainer container = stand.getPersistentDataContainer();
        return container.getOrDefault(NamespacedKeyUtils.getCrystalQualityKey(plugin), PersistentDataType.INTEGER, 50);
    }
    
    /**
     * Cancel all active sessions (on plugin disable)
     */
    public void cancelAllSessions() {
        for (UUID playerId : activeSessions.keySet()) {
            endSession(playerId, false);
        }
        activeSessions.clear();
    }
    
     private class GameSession {
        // Constants for this session
        private static final int ACTIONBAR_UPDATE_TICKS = 10; // Update every half second
        
        private final Player player;
        private final ArmorStand crystal;
        private final String crystalType;
        private final int quality;
        private final CrystalTier tier;
        
        private Location currentTarget;
        private int hitsInSequence = 0;
        private boolean isActive = false;
        private final int requiredHits; // Store the required hits for this session
        
        private BukkitTask particleTask;
        private BukkitTask timeoutTask;
        private BukkitTask gameTimeoutTask;
        private BukkitTask actionBarTask;
        
        // Calculated values for this session based on crystal tier
        private final double particleRadius;
        private final int particleTimeoutTicks;
        private final Particle.DustTransition particleEffect;
        private final Particle.DustOptions successParticle;
        
        public GameSession(Player player, ArmorStand crystal, String crystalType, int quality, CrystalTier tier) {
            this.player = player;
            this.crystal = crystal;
            this.crystalType = crystalType;
            this.quality = quality;
            this.tier = tier;
            this.requiredHits = tier.getRequiredHits();
            
            // Calculate session-specific parameters based on tier
            this.particleRadius = BASE_PARTICLE_RADIUS * tier.getParticleRadiusMultiplier();
            this.particleTimeoutTicks = (int)(BASE_PARTICLE_TIMEOUT_TICKS * tier.getTimeoutMultiplier());
            
            // Create custom particle effects based on tier
            Color tierColor = tier.getParticleColor();
            // Calculate a lighter version by interpolating between the tier color and white
            int r = (int) (tierColor.getRed() + (255 - tierColor.getRed()) * 0.7);
            int g = (int) (tierColor.getGreen() + (255 - tierColor.getGreen()) * 0.7);
            int b = (int) (tierColor.getBlue() + (255 - tierColor.getBlue()) * 0.7);
            Color lightColor = Color.fromRGB(r, g, b); // Lighter version for transition outer
            
            this.particleEffect = new Particle.DustTransition(
                tierColor,      // Core color from tier
                lightColor,     // Lighter outer color
                2.5f           // Size
            );
            
            this.successParticle = new Particle.DustOptions(
                Color.fromRGB(
                    (int)(0 * 0.3 + tierColor.getRed() * 0.7),
                    (int)(255 * 0.3 + tierColor.getGreen() * 0.7),
                    (int)(0 * 0.3 + tierColor.getBlue() * 0.7)
                ), // Green with a hint of tier color
                1.0f
            );
        }
        
        /**
         * Start the game session
         */
        public void start() {
            isActive = true;
            
            // Send start message to player with tier-specific info
            player.sendMessage("§d✦ §bBegin " + getTierDisplayName() + " crystal carving! §d✦");
            player.sendMessage("§aHit the floating particles to extract the gem.");
            player.sendMessage("§7Tip: You can hit particles even through blocks! Just aim at them.");
            
            // Add information about the number of hits required for this crystal
            player.sendMessage("§eThis crystal requires §6" + requiredHits + " §esuccessful hits to complete.");
            
            // More descriptive warnings based on tier difficulty
            if (tier.getParticleRadiusMultiplier() <= 0.35) {
                player.sendMessage("§c⚠ §4This " + getDifficultyLabel() + " crystal requires exceptional precision and reflexes!");
                player.sendMessage("§c  Particles will be tiny and vanish quickly. Few can master this challenge.");
            } else if (tier.getParticleRadiusMultiplier() <= 0.6) {
                player.sendMessage("§c⚠ §cThis " + getDifficultyLabel() + " crystal demands great precision and quick reactions!");
                player.sendMessage("§c  Only skilled gem carvers should attempt this challenge.");
            } else if (tier.getParticleRadiusMultiplier() <= 0.8) {
                player.sendMessage("§e⚠ §6This " + getDifficultyLabel() + " crystal requires good aim and quick reflexes.");
            }
            
            // Play a starting sound - pitch increases with tier difficulty
            float pitch = 1.0f + ((1.0f - (float)tier.getParticleRadiusMultiplier()) * 0.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, pitch);
            
            // Create the first target
            createNewTarget();
            
            // Start game timeout - higher tiers have less time
            int gameTimeout = (int)(BASE_GAME_TIMEOUT_TICKS * tier.getTimeoutMultiplier());
            gameTimeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Game timed out
                endGame(false);
            }, gameTimeout);
            
            // Start the particle display task that will periodically show particles
            particleTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!isActive || currentTarget == null || !player.isOnline() || crystal.isDead()) {
                        cancel();
                        endGame(false);
                        return;
                    }
                    
                    // Create visual particles around the target location
                    showTargetParticles();
                }
            }.runTaskTimer(plugin, 0, 5); // Every 1/4 second
            
            // Register with the SkillActionBarManager to handle the custom display
            // and start displaying the progress information
            updateActionBar();
        }

        /**
         * Update the action bar display with current progress
         */
        private void updateActionBar() {
            if (!isActive || !player.isOnline()) return;
            
            // Create the progress bar text
            String progressBar = getProgressBar(hitsInSequence, requiredHits);
            String actionBarText = "§d" + getTierDisplayName() + " Carving: " + progressBar + 
                                " §7(" + hitsInSequence + "/" + requiredHits + ")";
            
            // Use the SkillActionBarManager to display the custom minigame status
            // This will register a custom action bar that won't conflict with skill XP displays
            SkillActionBarManager.getInstance().showCustomActionBar(player, actionBarText, ACTIONBAR_UPDATE_TICKS);
            
            // Schedule the next update if still active
            if (isActive) {
                actionBarTask = Bukkit.getScheduler().runTaskLater(plugin, this::updateActionBar, ACTIONBAR_UPDATE_TICKS);
            }
        }
        
        /**
         * Get a display-friendly name for the tier
         */
        private String getTierDisplayName() {
            // Format the crystal name more nicely
            String name = crystalType.replace("crystal", " Crystal");
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        
        /**
         * Get a label describing the difficulty based on the tier
         */
        private String getDifficultyLabel() {
            double radiusMult = tier.getParticleRadiusMultiplier();
            
            if (radiusMult <= 0.3) return "§4§lMASTER-CLASS";
            if (radiusMult <= 0.4) return "§4§lEXTREMELY DIFFICULT";
            if (radiusMult <= 0.5) return "§c§lVERY CHALLENGING";
            if (radiusMult <= 0.7) return "§c§lCHALLENGING";
            if (radiusMult <= 0.8) return "§6§lMODERATE";
            return "§a§lBASIC";
        }
        
        /**
         * Check if the session is active
         */
        public boolean isActive() {
            return isActive;
        }
        
        /**
         * Display target particles
         */
        private void showTargetParticles() {
            // Create dust particles at the target with tier-specific color
            currentTarget.getWorld().spawnParticle(
                    Particle.DUST_COLOR_TRANSITION, 
                    currentTarget, 
                    6,                               // Number of particles
                    particleRadius * 0.5,            // Spread based on tier
                    particleRadius * 0.5, 
                    particleRadius * 0.5, 
                    particleEffect);                 // Tier-specific particle effect
            
            // Add a more noticeable glow that can be seen through blocks
            // Use flame color matching the tier
            currentTarget.getWorld().spawnParticle(
                    CRYSTAL_TIERS.get("mooncrystal").getParticleRadiusMultiplier() < 0.7 ? 
                            Particle.SOUL_FIRE_FLAME : Particle.FLAME,
                    currentTarget,
                    3,                               // Number of particles
                    particleRadius * 0.2,            // Smaller spread for flame
                    particleRadius * 0.2,
                    particleRadius * 0.2,
                    0.01);
        }
        
        /**
         * Handle a click event during the game using ray tracing
         */
        public void handleRayTracedClick(Location eyeLocation, Vector direction) {
           if (!isActive || currentTarget == null) {
                return;
            }
            
            // Calculate the closest point on the ray to the target particle
            Vector eyeToTarget = currentTarget.toVector().subtract(eyeLocation.toVector());
            double projectionLength = eyeToTarget.dot(direction);
            
            // If the particle is behind the player, ignore this click
            if (projectionLength < 0) {
                return; // Don't fail for clicks behind player
            }
            
            // Find the closest point on the ray to the target
            Vector closestPoint = eyeLocation.toVector().add(direction.clone().multiply(projectionLength));
            
            // Calculate distance from the closest point to the target
            double distance = closestPoint.distance(currentTarget.toVector());
            
            // Check if the ray passes close enough to the target
            // Scale hitbox by tier difficulty with nearly no buffer for highest tiers
            boolean hit = distance <= (particleRadius * (tier.getParticleRadiusMultiplier() < 0.5 ? 1.05 : 1.15));
            
            // Check if the player's look direction is pointing towards the particle
            Vector lookToTarget = currentTarget.toVector().subtract(eyeLocation.toVector()).normalize();
            double lookAlignment = direction.dot(lookToTarget); // 1 = perfect alignment, 0 = 90 degrees off
            
            // Only count as a hit if player is somewhat looking towards the particle
            // For harder tiers, require much more precise aim
            double requiredAlignment = 0.6 + ((1.0 - tier.getParticleRadiusMultiplier()) * 0.35);
            
            if (hit && lookAlignment > requiredAlignment) {
                // Hit successful!
                onHitSuccess();
            } else if (lookAlignment > requiredAlignment + 0.1) {
                // Clear miss while aiming at particle - only fail if player was clearly aiming at it
                // Reduced buffer from 0.2 to 0.1 for higher difficulty
                onHitFail();
            }
            // Otherwise it's a miss but we don't reset progress (just for usability)
        }
        
        /**
         * Handle a successful hit
         */
        private void onHitSuccess() {
            // Cancel current timeout task if it exists
            if (timeoutTask != null) {
                timeoutTask.cancel();
            }
            
            // Play success sound and particles
            float pitch = 1.0f + (hitsInSequence * 0.1f); // Higher pitch with each successful hit
            player.playSound(currentTarget, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, pitch);
            
            // Show success particles with tier-specific color
            currentTarget.getWorld().spawnParticle(Particle.DUST, currentTarget, 
                    20, 0.3, 0.3, 0.3, 0, successParticle);
            
            // Increment hit counter
            hitsInSequence++;
            
            // Award a small amount of XP for each successful hit as immediate feedback
            GemCarvingSubskill gemSkill = (GemCarvingSubskill) SkillRegistry.getInstance().getSkill(SubskillType.GEM_CARVING.getId());
            if (gemSkill != null) {
                // Small XP reward for each hit - scaled by tier and quality
                double hitXp = 5.0 * (0.5 + (quality / 200.0)) * (tier.getBaseXp() / 100.0);
                
                // Add bonus XP from skill tree if applicable
                Map<String, Double> benefits = gemSkill.getSkillTreeBenefits(player);
                int bonusXp = (int)Math.round(benefits.getOrDefault("gem_carving_xp", 0.0));
                
                if (bonusXp > 0) {
                    // Apply a small portion of the bonus XP per hit (10%)
                    double hitBonusXp = bonusXp * 0.1 * (0.5 + (quality / 200.0));
                    hitXp += hitBonusXp;
                    
                    // Occasionally remind players that the bonus scales with quality
                    if (hitsInSequence == 1 || random.nextDouble() < 0.2) {
                        player.sendActionBar("§3Your Carver's Expertise skill increases XP based on gem quality!");
                    }
                }
                
                SkillProgressionManager.getInstance().addExperience(player, gemSkill, hitXp);
            }
            
            // Update the action bar with the new progress
            updateActionBar();
            
            // Check if player has reached the required number of hits for this tier
            if (hitsInSequence >= requiredHits) {
                // Player completed the challenge!
                completeGame();
                return;
            }
            
            // Create a new target for the next hit
            createNewTarget();
        }
        
        /**
         * Handle a failed hit
         */
        private void onHitFail() {
            // Play fail sound - higher pitched for higher tiers (more challenging)
            float pitch = 0.7f + ((1.0f - (float)tier.getParticleRadiusMultiplier()) * 0.3f);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, pitch);
            
            // Reset hit counter
            hitsInSequence = 0;
            
            // Show failure message
            SkillActionBarManager.getInstance().showCustomActionBar(player, "§cMissed! §7Progress reset.", 40);
            
            // Update the action bar with the reset progress after a short delay
            Bukkit.getScheduler().runTaskLater(plugin, this::updateActionBar, 40);
            
            // Create a new target
            createNewTarget();
        }
        
        /**
         * Create a new target location for the player to hit
         */
        private void createNewTarget() {
            if (!isActive) return;
            
            // Get armorstand head location as the base position
            Location headLoc = crystal.getEyeLocation();
            
            // Get the direction the armorstand's head is facing
            double headYaw = Math.toRadians(crystal.getHeadPose().getY());
            double headPitch = Math.toRadians(crystal.getHeadPose().getX());
            
            // For higher tiers, make particle placement much less predictable and more erratic
            double difficultyFactor = 2.5 - tier.getParticleRadiusMultiplier(); // 1.5 to 2.25 scale
            double randomnessFactor = 0.6 * difficultyFactor;
            
            // Higher tier crystals have a chance to create particles further away
            double distanceMultiplier = 1.0 + ((1.0 - tier.getParticleRadiusMultiplier()) * 0.5); // Up to 1.5x further
            
            // Calculate a position based on the head orientation
            if (Math.abs(headYaw) < 0.1 && Math.abs(headPitch) < 0.1) {
                // No specific rotation, spawn above the head
                double distance = BASE_PARTICLE_RANGE * (0.5 + (random.nextDouble() * 0.5)) * distanceMultiplier;
                double angle1 = random.nextDouble() * Math.PI * 2; // Random angle around Y axis
                
                // Calculate position in a dome above the armorstand
                double x = Math.sin(angle1) * distance;
                double y = random.nextDouble() * distance * 0.5; // Lower height range for easier targeting
                double z = Math.cos(angle1) * distance;
                
                // Create the new target location, offset from head position
                currentTarget = headLoc.clone().add(x, y, z);
            } else {
                // Use the head's orientation to determine spawn position
                // Convert head pose to directional vector
                double x = -Math.sin(headYaw) * Math.cos(headPitch);
                double y = Math.sin(headPitch);
                double z = Math.cos(headYaw) * Math.cos(headPitch);
                
                // Get a somewhat random position in that general direction
                double distance = BASE_PARTICLE_RANGE * (0.5 + (random.nextDouble() * 0.5)) * distanceMultiplier;
                
                // Higher tier crystals have more erratic particle placement
                x = x + (random.nextDouble() * randomnessFactor - randomnessFactor/2);
                y = y + (random.nextDouble() * randomnessFactor - randomnessFactor/2);
                z = z + (random.nextDouble() * randomnessFactor - randomnessFactor/2);
                
                // Normalize and scale the vector
                double length = Math.sqrt(x*x + y*y + z*z);
                if (length > 0) {
                    x = x / length * distance;
                    y = y / length * distance;
                    z = z / length * distance;
                }
                
                // Create the target at this position
                currentTarget = headLoc.clone().add(x, y, z);
            }
            
            // Set a timeout for this target with duration based on tier
            if (timeoutTask != null) {
                timeoutTask.cancel();
            }
            
            // For higher tiers, add a slight random variance to the timeout duration
            // This makes timing more unpredictable and keeps players on their toes
            double timeoutVariance = 1.0 + ((random.nextDouble() - 0.5) * 0.2 * (2.0 - tier.getParticleRadiusMultiplier()));
            int finalTimeoutTicks = (int)(particleTimeoutTicks * timeoutVariance);
            
            timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (isActive) {
                    // Player didn't hit the target in time
                    player.sendActionBar("§cToo slow! §7Progress reset.");
                    player.playSound(currentTarget, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
                    
                    // Show failure particles
                    currentTarget.getWorld().spawnParticle(Particle.DUST, currentTarget, 
                            15, 0.3, 0.3, 0.3, 0, FAIL_PARTICLE);
                    
                    // Reset hit counter
                    hitsInSequence = 0;
                    
                    // Create a new target
                    createNewTarget();
                }
            }, finalTimeoutTicks);
        }
        
        /**
         * Complete the game successfully
         */
        private void completeGame() {
            // Check if already completed/ended
            if (!isActive) return;
            
            // Set the crystal on cooldown regardless of outcome
            setCrystalCooldown(crystal, tier);
            
            // Get the GemCarving skill
            GemCarvingSubskill gemSkill = (GemCarvingSubskill) SkillRegistry.getInstance().getSkill(SubskillType.GEM_CARVING.getId());
            
            if (gemSkill != null) {
                // Calculate XP based on crystal quality and tier
                double baseXp = tier.getBaseXp();
                double qualityMultiplier = 0.5 + (quality / 100.0); // 0.5 to 1.5 based on quality
                
                // Apply skill level multiplier to extraction success
                int playerLevel = gemSkill.getSkillLevel(player).getLevel();
                
                // For higher tiers, extraction is more difficult
                double extractionSuccess = gemSkill.getExtractionSuccessChance(playerLevel) - tier.getExtraDifficulty();
                extractionSuccess = Math.max(0.05, extractionSuccess); // Minimum 5% chance
                
                // Determine if extraction was successful based on player skill and crystal difficulty
                boolean extractionSuccessful = random.nextDouble() < extractionSuccess;
                
                // Create a location for effects - used in both success and failure paths
                Location effectLoc = crystal.getLocation().clone().add(0, 1.0, 0);
                
                if (extractionSuccessful) {
                    // Get skill tree benefits
                    Map<String, Double> benefits = gemSkill.getSkillTreeBenefits(player);
                    int bonusXp = (int)Math.round(benefits.getOrDefault("gem_carving_xp", 0.0));
                    double miningFortune = benefits.getOrDefault("mining_fortune", 0.0);
                    
                    // Calculate XP with bonuses
                    double levelBonus = 1.0 + (playerLevel / 100.0); 
                    double finalBaseXp = baseXp * levelBonus * qualityMultiplier;
                    double totalXp = finalBaseXp;
                    double bonusXpWithQuality = 0;
                    
                    if (bonusXp > 0) {
                        double qualityBonusFactor = 0.5 + (quality / 200.0);
                        bonusXpWithQuality = bonusXp * qualityBonusFactor;
                        totalXp += bonusXpWithQuality;
                    }
                    
                    // Award XP
                    SkillProgressionManager.getInstance().addExperience(player, gemSkill, totalXp);
                    
                    // Quality descriptor for gems
                    String qualityText;
                    if (quality >= 90) qualityText = "§d§lexceptional §d";
                    else if (quality >= 75) qualityText = "§b§lhigh-quality §b";
                    else if (quality >= 50) qualityText = "§a§lquality §a";
                    else qualityText = "§e";
                    
                    // Calculate mining fortune drops
                    double totalMiningFortune = getMiningFortune(player);
                    double tierScalingFactor = calculateTierScalingFactor(tier);
                    int fortuneGems = calculateMiningFortuneDrops(player, tier) - 1; // -1 for base drop
                    
                    // Generate consolidated success message with formatting
                    StringBuilder message = new StringBuilder();
                    message.append("\n§d✦ §a§lSUCCESSFUL EXTRACTION! §d✦\n");
                    message.append("§fYou extracted a ").append(qualityText).append(quality).append("% §fgem from the ")
                        .append(getTierChatColor()).append(getTierDisplayName()).append("\n");
                    
                    // Prepare item details
                    double gemQualityMultiplier = gemSkill.getGemQualityMultiplier(playerLevel);
                    if (miningFortune > 0) {
                        gemQualityMultiplier += (miningFortune / 100.0);
                    }
                    
                    ItemStack reward = createGemReward(playerLevel, gemQualityMultiplier, tier);
                    
                    // Add fortune and XP details in one concise line
                    message.append("§e⭐ §7Received: §f").append(reward.getAmount()).append("× ")
                        .append(reward.getItemMeta().getDisplayName());
                    
                    if (fortuneGems > 0) {
                        message.append(" §7(§a+").append(fortuneGems).append(" §7from Fortune)");
                    }
                    
                    message.append("\n§e✨ §7XP: §f+").append(String.format("%.1f", totalXp));
                    
                    if (bonusXpWithQuality > 0) {
                        message.append(" §8[§7base: ").append(String.format("%.1f", finalBaseXp))
                            .append(" §7+ boost: ").append(String.format("%.1f", bonusXpWithQuality)).append("§8]");
                    }
                    
                    // Display the consolidated message
                    player.sendMessage(message.toString());
                    
                    // Award the item
                    player.getInventory().addItem(reward);
                    
                    // Create a success particle effect
                    for (int i = 0; i < 3; i++) {
                        final int index = i;
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            Color tierColor = tier.getParticleColor();
                            Color yellowColor = Color.YELLOW;
                            int r = (int)(tierColor.getRed() * 0.3 + yellowColor.getRed() * 0.7);
                            int g = (int)(tierColor.getGreen() * 0.3 + yellowColor.getGreen() * 0.7);
                            int b = (int)(tierColor.getBlue() * 0.3 + yellowColor.getBlue() * 0.7);
                            
                            Particle.DustOptions tierSuccessParticle = new Particle.DustOptions(
                                    Color.fromRGB(r, g, b), 1.5f);
                            
                            crystal.getWorld().spawnParticle(Particle.DUST, 
                                    effectLoc, 30, 0.5, 0.5, 0.5, 0, tierSuccessParticle);
                            crystal.getWorld().spawnParticle(Particle.WITCH, 
                                    effectLoc, 20, 0.5, 0.5, 0.5, 0.05);
                            
                            float pitch = 1.0f + (index * 0.2f);
                            player.playSound(effectLoc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, pitch);
                        }, i * 5L);
                    }
                    
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
                } else {
                    // Create consolidated failure message
                    StringBuilder failMessage = new StringBuilder();
                    failMessage.append("\n§c✖ §c§lEXTRACTION FAILED! §c✖");
                    failMessage.append("\n§7The gem cracked during the extraction process.");
                    
                    if (tier.getExtraDifficulty() > 0.1) {
                        failMessage.append(" This ").append(getTierChatColor()).append(getTierDisplayName())
                                .append(" §7is ").append(getDifficultyLabel().toLowerCase()).append(" §7to carve (")
                                .append(String.format("+%.0f", tier.getExtraDifficulty() * 100)).append("% difficulty)");
                    }
                    
                    // Calculate partial XP
                    double partialXpPercent = 0.3 + (tier.getExtraDifficulty() * 0.2);
                    double partialXp = baseXp * qualityMultiplier * partialXpPercent;
                    
                    // Award partial XP
                    SkillProgressionManager.getInstance().addExperience(player, gemSkill, partialXp);
                    
                    // Add XP info to the message
                    failMessage.append("\n§7XP: §f+").append(String.format("%.1f", partialXp))
                            .append(" §8(partial reward)");
                    
                    player.sendMessage(failMessage.toString());
                    
                    // Failure particles and effects
                    Color tierColor = tier.getParticleColor();
                    Color redColor = Color.RED;
                    
                    int r = (int)(tierColor.getRed() * 0.3 + redColor.getRed() * 0.7);
                    int g = (int)(tierColor.getGreen() * 0.3 + redColor.getGreen() * 0.7);
                    int b = (int)(tierColor.getBlue() * 0.3 + redColor.getBlue() * 0.7);
                    
                    Particle.DustOptions tierFailParticle = new Particle.DustOptions(
                            Color.fromRGB(r, g, b), 1.0f);
                    
                    crystal.getWorld().spawnParticle(Particle.DUST, 
                            effectLoc, 20, 0.5, 0.5, 0.5, 0, tierFailParticle);
                    
                    crystal.getWorld().spawnParticle(Particle.ITEM, 
                            effectLoc, 15, 0.3, 0.3, 0.3, 0.05, 
                            new ItemStack(Material.AMETHYST_SHARD));
                    
                    player.playSound(effectLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
                }
            }
            
            // End the game
            endGame(true);
        }
        
        /**
         * Create a gem reward item with quality based on player skill and crystal tier
         */
        private ItemStack createGemReward(int playerLevel, double qualityMultiplier, CrystalTier tier) {
            // Calculate chance for rare gems - increases with tier and player level
            double rarityBonus = ((tier.getBaseXp() / CRYSTAL_TIERS.get("mooncrystal").getBaseXp()) - 1.0) * 0.5;
            double rareChance = (playerLevel / 200.0) + rarityBonus; // Up to 50% chance + tier bonus
            
            // Determine gem material based on difficulty
            Material gemMaterial;
            
            if (random.nextDouble() < rareChance) {
                // Very rare gems for high tier crystals
                if (tier.getBaseXp() >= 250.0 && random.nextDouble() < 0.3) {
                    return new ItemStack(Material.NETHERITE_INGOT);
                }
                
                // Rare gems
                Material[] rareGems = {Material.DIAMOND, EMERALD};
                gemMaterial = rareGems[random.nextInt(rareGems.length)];
            } else {
                // Common gems
                Material[] commonGems = {Material.AMETHYST_SHARD, Material.QUARTZ, Material.LAPIS_LAZULI};
                gemMaterial = commonGems[random.nextInt(commonGems.length)];
            }
            
            // Create the gem item
            ItemStack gem = new ItemStack(gemMaterial);
            ItemMeta meta = gem.getItemMeta();
            
            // Calculate final gem quality based on both crystal quality and player skill
            int gemQuality = (int)(quality * qualityMultiplier);
            gemQuality = Math.min(100, gemQuality); // Cap at 100%
            
            // Set a custom name based on quality
            String qualityPrefix = "";
            if (gemQuality >= 90) qualityPrefix = "§d§lExceptional ";
            else if (gemQuality >= 75) qualityPrefix = "§b§lHigh-Quality ";
            else if (gemQuality >= 50) qualityPrefix = "§a§lQuality ";
            
            String gemName = gemMaterial.name().toLowerCase().replace("_", " ");
            gemName = gemName.substring(0, 1).toUpperCase() + gemName.substring(1);
            meta.setDisplayName(qualityPrefix + "§f" + gemName);
            
            // Add lore with quality information
            List<String> lore = new ArrayList<>();
            lore.add("§7Quality: " + getQualityColor(gemQuality) + gemQuality + "%");
            lore.add("§7Extracted from " + getTierChatColor() + getTierDisplayName());
            meta.setLore(lore);
            
            gem.setItemMeta(meta);
            
            // Calculate quantity based on mining fortune
            int quantity = calculateMiningFortuneDrops(player, tier);
            
            gem.setAmount(quantity);
            
            return gem;
        }

        /**
         * Calculate additional drops based on player's mining fortune and crystal tier
         * 
         * @param player The player extracting the gem
         * @param tier The crystal tier being extracted
         * @return The total amount of gems to drop (at least 1)
         */
        private int calculateMiningFortuneDrops(Player player, CrystalTier tier) {
            // Get the player's mining fortune stat
            double miningFortune = getMiningFortune(player);
            
            // Calculate tier-based scaling factor for mining fortune requirement
            double tierScalingFactor = calculateTierScalingFactor(tier);
            
            // Calculate guaranteed additional drops
            int guaranteedExtra = (int)(miningFortune / (100 * tierScalingFactor));
            
            // Calculate chance for one more drop beyond the guaranteed ones
            double remainingFortune = miningFortune % (100 * tierScalingFactor);
            double chanceForOneMore = remainingFortune / (100 * tierScalingFactor);
            
            // Base quantity is always 1
            int quantity = 1;
            
            // Add guaranteed extra drops from mining fortune
            quantity += guaranteedExtra;
            
            // Check for chance-based additional drop
            boolean gotLuckyDrop = false;
            if (random.nextDouble() < chanceForOneMore) {
                quantity++;
                gotLuckyDrop = true;
            }
            
            // Log for debugging
            if (Main.getInstance().isDebugMode() && (guaranteedExtra > 0 || chanceForOneMore > 0)) {
                Main.getInstance().getLogger().info("[GemCarvingMinigame] Player " + player.getName() + 
                    " with " + miningFortune + " mining fortune got " + quantity + " gems " +
                    "(guaranteed: " + guaranteedExtra + ", chance for one more: " + 
                    String.format("%.2f", chanceForOneMore * 100) + "%)");
            }
            
            return Math.max(1, quantity);
        }

        /**
         * Calculate the scaling factor for mining fortune requirements based on tier
         * 
         * @param tier The crystal tier
         * @return The scaling factor (1.0 for lowest tier, up to 10.0 for highest)
         */
        private double calculateTierScalingFactor(CrystalTier tier) {
            // Get the base XP value of the tier (higher tiers have higher base XP)
            double tierBaseXp = tier.getBaseXp();
            double baseXp = CRYSTAL_TIERS.get("mooncrystal").getBaseXp(); // The baseline
            
            // Calculate tier progression factor (1.0 for mooncrystal, increases for higher tiers)
            double tierFactor = tierBaseXp / baseXp;
            
            // Scale from 1.0 for lowest tier up to 10.0 for highest tier
            // Use log scale to make early tiers less steep
            double scalingFactor = 1.0 + 9.0 * Math.log10(tierFactor) / Math.log10(5.0);
            
            // Clamp to reasonable values (1.0 to 10.0)
            return Math.max(1.0, Math.min(10.0, scalingFactor));
        }

        /**
         * Get the player's mining fortune stat
         * 
         * @param player The player to check
         * @return The player's mining fortune value
         */
        private double getMiningFortune(Player player) {
            Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
            if (activeSlot == null) return 0;
            
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile == null) return 0;
            
            return profile.getStats().getMiningFortune();
        }

        /**
         * Get a color code based on quality percentage
         */
        private String getQualityColor(int quality) {
            if (quality >= 90) return "§d"; // Light purple for exceptional
            if (quality >= 75) return "§b"; // Aqua for high quality
            if (quality >= 50) return "§a"; // Green for good quality
            if (quality >= 25) return "§e"; // Yellow for medium quality
            return "§7";                    // Gray for low quality
        }
        
        /**
         * End the game session
         * 
         * @param completed Whether the game was completed successfully
         */
        public void endGame(boolean completed) {
            // Check if already ended
            if (!isActive) return;
            isActive = false;
            
            // Cancel all tasks
            if (particleTask != null) {
                particleTask.cancel();
                particleTask = null;
            }
            
            if (timeoutTask != null) {
                timeoutTask.cancel();
                timeoutTask = null;
            }
            
            if (gameTimeoutTask != null) {
                gameTimeoutTask.cancel();
                gameTimeoutTask = null;
            }
            
            if (actionBarTask != null) {
                actionBarTask.cancel();
                actionBarTask = null;
            }
            
            // Clear the custom action bar to restore normal functionality
            SkillActionBarManager.getInstance().clearCustomActionBar(player);
            
            // Remove from active sessions
            activeSessions.remove(player.getUniqueId());
            
            // Apply cooldown to the crystal (even if not completed successfully)
            setCrystalCooldown(crystal, tier);
            
            // If not completed successfully and player is still online
            if (!completed && player.isOnline()) {
                player.sendMessage("§7Gem carving attempt cancelled.");
                int cooldownSeconds = (int)(BASE_CRYSTAL_COOLDOWN_SECONDS * tier.getCooldownMultiplier());
                player.sendMessage("§8This crystal needs " + cooldownSeconds + " seconds to recover before another attempt.");
            }
        }
        
        /**
         * Create a visual progress bar
         */
        private String getProgressBar(int current, int max) {
            StringBuilder bar = new StringBuilder();
            
            // Convert tier color to chat color
            String barColor = getTierChatColor();
            bar.append(barColor);
            
            int filledSegments = (int) Math.ceil((double) current / max * 10);
            
            for (int i = 0; i < 10; i++) {
                if (i < filledSegments) {
                    bar.append("■");
                } else {
                    bar.append("§7■");
                }
            }
            
            return bar.toString();
        }
        
        /**
         * Convert tier color to approximate chat color
         */
        private String getTierChatColor() {
            Color color = tier.getParticleColor();
            
            // Simple color matching logic
            int r = color.getRed();
            int g = color.getGreen();
            int b = color.getBlue();
            
            if (r > 200 && g > 200 && b > 200) return "§f"; // WHITE
            if (r > 200 && g > 200 && b < 100) return "§e"; // YELLOW
            if (r < 100 && g > 200 && b < 100) return "§a"; // GREEN
            if (r < 100 && g > 200 && b > 200) return "§b"; // AQUA
            if (r < 100 && g < 100 && b > 200) return "§9"; // BLUE
            if (r > 200 && g < 100 && b > 200) return "§d"; // LIGHT PURPLE
            if (r > 200 && g < 100 && b < 100) return "§c"; // RED
            if (r < 100 && g < 100 && b < 100) return "§8"; // DARK GRAY
            
            return "§d"; // Default to light purple
        }
    }
    
        /**
     * Class to hold crystal tier information
     */
    private static class CrystalTier {
        private final double particleRadiusMultiplier;
        private final double timeoutMultiplier;
        private final double cooldownMultiplier;
        private final double extraDifficulty;
        private final double baseXp;
        private final Color particleColor;
        private final int requiredHits; // Added field for required hits
        
        public CrystalTier(double particleRadiusMultiplier, double timeoutMultiplier, 
                          double cooldownMultiplier, double extraDifficulty,
                          double baseXp, Color particleColor, int requiredHits) {
            this.particleRadiusMultiplier = particleRadiusMultiplier;
            this.timeoutMultiplier = timeoutMultiplier;
            this.cooldownMultiplier = cooldownMultiplier;
            this.extraDifficulty = extraDifficulty;
            this.baseXp = baseXp;
            this.particleColor = particleColor;
            this.requiredHits = requiredHits;
        }
        
        // Existing getters...
        
        public int getRequiredHits() {
            return requiredHits;
        }
        
        public double getParticleRadiusMultiplier() {
            return particleRadiusMultiplier;
        }
        
        public double getTimeoutMultiplier() {
            return timeoutMultiplier;
        }
        
        public double getCooldownMultiplier() {
            return cooldownMultiplier;
        }
        
        public double getExtraDifficulty() {
            return extraDifficulty;
        }
        
        public double getBaseXp() {
            return baseXp;
        }
        
        public Color getParticleColor() {
            return particleColor;
        }
    }
}