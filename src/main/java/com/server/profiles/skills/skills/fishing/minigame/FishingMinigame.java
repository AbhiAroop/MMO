package com.server.profiles.skills.skills.fishing.minigame;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.skills.fishing.subskills.RodFishingSubskill;
import com.server.profiles.skills.skills.fishing.types.FishingType;
import com.server.profiles.stats.PlayerStats;

/**
 * Handles the timing-based fishing minigame mechanics
 */
public class FishingMinigame {
    private final FishingSession session;
    private final Player player;
    private final RodFishingSubskill rodFishingSubskill;
    
    // Minigame state
    private BossBar bossBar;
    private double indicatorPosition; // 0.0 to 100.0
    private double indicatorDirection; // 1.0 or -1.0
    private double indicatorSpeed; // Speed multiplier for current round
    private double indicatorSize; // Size of the indicator (spike) for current round
    private double catchZoneStart; // 0.0 to 100.0
    private double catchZoneEnd; // 0.0 to 100.0
    
    private int currentRound;
    private int requiredRounds;
    private int missedCatchesThisRound;
    private int maxMissesPerRound; // Can be increased by enchantments
    
    // Inactivity tracking
    private long roundStartTime;
    private static final long INACTIVITY_TIMEOUT = 10000L; // 10 seconds in milliseconds
    
    // Timing constants
    private static final long TICK_INTERVAL = 1L; // Run every tick (50ms)
    private static final double MIN_SPEED_MULTIPLIER = 0.7; // 70% of base speed
    private static final double MAX_SPEED_MULTIPLIER = 1.5; // 150% of base speed
    private static final double MIN_ZONE_MULTIPLIER = 0.4; // 40% of base catch zone (harder)
    private static final double MAX_ZONE_MULTIPLIER = 2.5; // 250% of base catch zone (easier, up to 25% of bar)
    private static final double MIN_INDICATOR_SIZE = 1.5; // Minimum indicator size (small spike)
    private static final double MAX_INDICATOR_SIZE = 3.5; // Maximum indicator size (large spike)
    
    public FishingMinigame(FishingSession session) {
        this.session = session;
        this.player = session.getPlayer();
        
        // Get Rod Fishing subskill for stat bonuses
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot != null) {
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile != null) {
                // Use SkillRegistry to get the skill directly
                RodFishingSubskill rodFishing = (RodFishingSubskill) SkillRegistry.getInstance().getSkill("rod_fishing");
                this.rodFishingSubskill = rodFishing;
            } else {
                this.rodFishingSubskill = null;
            }
        } else {
            this.rodFishingSubskill = null;
        }
        
        this.currentRound = 0;
        this.missedCatchesThisRound = 0;
        
        // Calculate max misses based on Fishing Resilience stat
        // Base: 2 misses per round
        // Resilience works like fortune: 100% = +1 guaranteed miss, remainder = chance for additional miss
        int baseMisses = 2;
        int extraMisses = 0;
        
        if (activeSlot != null) {
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile != null) {
                PlayerStats stats = profile.getStats();
                double resilience = stats.getFishingResilience();
                
                // Calculate guaranteed extra misses (floor of resilience / 100)
                int guaranteedExtra = (int) Math.floor(resilience / 100.0);
                extraMisses += guaranteedExtra;
                
                // Calculate chance for one more miss (remainder percentage)
                double chanceForNext = resilience % 100.0;
                if (chanceForNext > 0 && Math.random() * 100.0 < chanceForNext) {
                    extraMisses++;
                }
            }
        }
        
        this.maxMissesPerRound = baseMisses + extraMisses;
        this.roundStartTime = System.currentTimeMillis();
    }
    
    /**
     * Start the minigame
     */
    public void start() {
        // Calculate difficulty based on fishing type and player stats
        FishingType type = session.getFishingType();
        double difficultyMultiplier = type.getDifficultyMultiplier();
        
        // Get stat bonuses from subskill (with null check)
        double catchZoneSize;
        if (rodFishingSubskill != null) {
            catchZoneSize = rodFishingSubskill.getCatchZoneSize(player);
            requiredRounds = rodFishingSubskill.getRequiredRounds(player);
        } else {
            catchZoneSize = 10.0; // Default values
            requiredRounds = 5;
        }
        
        // Adjust catch zone based on difficulty (result stored for display purposes)
        @SuppressWarnings("unused")
        double adjustedCatchZone = catchZoneSize / difficultyMultiplier;
        
        // Initialize boss bar
        bossBar = org.bukkit.Bukkit.createBossBar(
            type.getColoredName() + " Fishing",
            BarColor.BLUE,
            BarStyle.SOLID
        );
        bossBar.addPlayer(player);
        bossBar.setProgress(1.0);
        
        // Start first round
        startNewRound();
        
        // Start indicator movement task
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!session.isActive()) {
                    cancel();
                    return;
                }
                
                // Check for inactivity timeout
                long currentTime = System.currentTimeMillis();
                long timeSinceRoundStart = currentTime - roundStartTime;
                
                if (timeSinceRoundStart > INACTIVITY_TIMEOUT) {
                    // Player has been inactive for too long
                    player.sendMessage("§c§lTIMEOUT! §7You took too long to react. The fish got away!");
                    session.fail();
                    cancel();
                    return;
                }
                
                // Update boss bar color based on time remaining
                updateBossBarColor(timeSinceRoundStart);
                
                updateIndicator();
                updateDisplay();
            }
        };
        
        session.setMinigameTask(task.runTaskTimer(Main.getInstance(), 0L, TICK_INTERVAL));
    }
    
    /**
     * Start a new round with random catch zone
     */
    private void startNewRound() {
        currentRound++;
        
        // Reset round timer for inactivity detection
        roundStartTime = System.currentTimeMillis();
        
        // Reset miss counter for new round
        missedCatchesThisRound = 0;
        
        // Calculate base catch zone size from stats (with null check)
        double baseCatchZoneSize;
        if (rodFishingSubskill != null) {
            baseCatchZoneSize = rodFishingSubskill.getCatchZoneSize(player);
        } else {
            baseCatchZoneSize = 10.0; // Default value if subskill not available
        }
        
        FishingType type = session.getFishingType();
        baseCatchZoneSize = baseCatchZoneSize / type.getDifficultyMultiplier();
        
        // Randomize catch zone size for this round (between 40% and 250% of base)
        // Using weighted random to make extreme values rare
        double zoneMultiplier;
        double random = Math.random();
        
        // Use a bell curve distribution (Box-Muller transform approximation)
        // Most values will be near 1.0 (100%), extreme values are rare
        double mean = 1.3; // Slightly favor larger zones (130% of base)
        double stdDev = 0.5; // Standard deviation controls spread
        
        // Generate normal distribution value
        double normalRandom = Math.cos(2 * Math.PI * random) * Math.sqrt(-2 * Math.log(Math.random()));
        zoneMultiplier = mean + (normalRandom * stdDev);
        
        // Clamp to our min/max range
        zoneMultiplier = Math.max(MIN_ZONE_MULTIPLIER, Math.min(MAX_ZONE_MULTIPLIER, zoneMultiplier));
        
        double catchZoneSize = baseCatchZoneSize * zoneMultiplier;
        
        // Ensure catch zone doesn't exceed 25% of the total bar
        catchZoneSize = Math.min(catchZoneSize, 25.0);
        
        // Random catch zone position (ensure it fits within bounds)
        double maxStart = 100.0 - catchZoneSize;
        catchZoneStart = Math.random() * maxStart;
        catchZoneEnd = catchZoneStart + catchZoneSize;
        
        // Reset indicator to random starting position
        indicatorPosition = Math.random() * 100.0;
        indicatorDirection = Math.random() < 0.5 ? 1.0 : -1.0;
        
        // Randomize indicator speed for this round (between 70% and 150% of base speed)
        indicatorSpeed = MIN_SPEED_MULTIPLIER + (Math.random() * (MAX_SPEED_MULTIPLIER - MIN_SPEED_MULTIPLIER));
        
        // Randomize indicator size (spike width) for this round
        // Ensure spike cannot be larger than the catch zone
        double maxIndicatorSize = Math.min(MAX_INDICATOR_SIZE, catchZoneSize / 2.0);
        indicatorSize = MIN_INDICATOR_SIZE + (Math.random() * (maxIndicatorSize - MIN_INDICATOR_SIZE));
        
        // Only show title for the first round
        if (currentRound == 1) {
            player.sendTitle("§b§lRound " + currentRound + "/" + requiredRounds, "§7Click when the spike hits the green zone!", 5, 30, 5);
        }
        
        // Update boss bar title with current round number
        bossBar.setTitle(session.getFishingType().getColoredName() + " §7- Round " + currentRound + "/" + requiredRounds);
    }
    
    /**
     * Update indicator position
     */
    private void updateIndicator() {
        // Get indicator speed from stats (with null check)
        double baseSpeed;
        if (rodFishingSubskill != null) {
            baseSpeed = rodFishingSubskill.getIndicatorSpeed(player);
        } else {
            baseSpeed = 2.0; // Default speed if subskill not available
        }
        
        FishingType type = session.getFishingType();
        baseSpeed = baseSpeed * type.getDifficultyMultiplier();
        
        // Apply randomized speed multiplier for this round
        double speed = baseSpeed * indicatorSpeed;
        
        // Move indicator
        indicatorPosition += indicatorDirection * speed;
        
        // Bounce off edges
        if (indicatorPosition <= 0.0) {
            indicatorPosition = 0.0;
            indicatorDirection = 1.0;
        } else if (indicatorPosition >= 100.0) {
            indicatorPosition = 100.0;
            indicatorDirection = -1.0;
        }
    }
    
    /**
     * Update boss bar color based on time remaining in round
     * Green (0-5s) -> Yellow (5-7.5s) -> Red (7.5-10s)
     */
    private void updateBossBarColor(long timeSinceRoundStart) {
        double timeRemainingMs = INACTIVITY_TIMEOUT - timeSinceRoundStart;
        double percentageRemaining = timeRemainingMs / INACTIVITY_TIMEOUT;
        
        if (percentageRemaining > 0.5) {
            // More than 50% time remaining (0-5 seconds elapsed) - GREEN
            bossBar.setColor(BarColor.GREEN);
        } else if (percentageRemaining > 0.25) {
            // 25-50% time remaining (5-7.5 seconds elapsed) - YELLOW
            bossBar.setColor(BarColor.YELLOW);
        } else {
            // Less than 25% time remaining (7.5-10 seconds elapsed) - RED
            bossBar.setColor(BarColor.RED);
        }
    }
    
    /**
     * Update visual display (boss bar and action bar)
     */
    private void updateDisplay() {
        // Boss bar color is updated in updateBossBarColor based on time remaining
        // Don't override it here based on indicator position
        
        // Calculate zones to ensure all three are always visible
        double catchZoneCenter = (catchZoneStart + catchZoneEnd) / 2.0;
        double catchZoneSize = catchZoneEnd - catchZoneStart;
        
        // Ensure minimum zone size for visibility (at least 6 units for all 3 zones to show)
        double minZoneSize = 6.0;
        if (catchZoneSize < minZoneSize) {
            // Expand zone to minimum size
            double expansion = (minZoneSize - catchZoneSize) / 2.0;
            catchZoneStart = Math.max(0, catchZoneStart - expansion);
            catchZoneEnd = Math.min(100.0, catchZoneEnd + expansion);
            catchZoneSize = catchZoneEnd - catchZoneStart;
        }
        
        // Calculate perfect zone (center 30% of catch zone - always visible)
        double perfectZoneSize = catchZoneSize * 0.30;
        
        // Ensure perfect zone is at least as large as the spike (minimum 2x spike size for visibility)
        double minPerfectZoneSize = indicatorSize * 2.5;
        if (perfectZoneSize < minPerfectZoneSize) {
            perfectZoneSize = minPerfectZoneSize;
            // If perfect zone needs to be larger, expand catch zone accordingly
            if (perfectZoneSize > catchZoneSize * 0.30) {
                double requiredCatchZoneSize = perfectZoneSize / 0.30;
                if (requiredCatchZoneSize > catchZoneSize) {
                    double expansion = (requiredCatchZoneSize - catchZoneSize) / 2.0;
                    catchZoneStart = Math.max(0, catchZoneStart - expansion);
                    catchZoneEnd = Math.min(100.0, catchZoneEnd + expansion);
                    catchZoneSize = catchZoneEnd - catchZoneStart;
                    catchZoneCenter = (catchZoneStart + catchZoneEnd) / 2.0;
                }
            }
        }
        
        double perfectZoneStart = catchZoneCenter - (perfectZoneSize / 2.0);
        double perfectZoneEnd = catchZoneCenter + (perfectZoneSize / 2.0);
        
        // Calculate good zone (center 65% of catch zone - surrounds perfect)
        double goodZoneSize = catchZoneSize * 0.65;
        double goodZoneStart = catchZoneCenter - (goodZoneSize / 2.0);
        double goodZoneEnd = catchZoneCenter + (goodZoneSize / 2.0);
        
        // Yellow zone is the full catch zone (outer layer)
        
        // Create action bar display
        StringBuilder actionBar = new StringBuilder();
        int totalChars = 50;
        
        for (int i = 0; i < totalChars; i++) {
            double position = (i / (double) totalChars) * 100.0;
            
            // Check if this position is the indicator (using dynamic indicator size)
            if (Math.abs(position - indicatorPosition) < indicatorSize) {
                // Color indicator based on catch zone proximity
                if (position >= perfectZoneStart && position <= perfectZoneEnd) {
                    actionBar.append("§a▼§r"); // Green for perfect zone
                } else if (position >= goodZoneStart && position <= goodZoneEnd) {
                    actionBar.append("§6▼§r"); // Gold/orange for good zone
                } else if (position >= catchZoneStart && position <= catchZoneEnd) {
                    actionBar.append("§e▼§r"); // Yellow for okay zone
                } else {
                    actionBar.append("§e▼§r"); // Yellow when outside zone
                }
            }
            // Check if this position is in perfect zone (green center)
            else if (position >= perfectZoneStart && position <= perfectZoneEnd) {
                actionBar.append("§a■§r");
            }
            // Check if this position is in good zone (orange/gold surrounding)
            else if (position >= goodZoneStart && position <= goodZoneEnd) {
                actionBar.append("§6■§r");
            }
            // Check if this position is in catch zone (yellow outer)
            else if (position >= catchZoneStart && position <= catchZoneEnd) {
                actionBar.append("§e■§r");
            }
            // Regular position
            else {
                actionBar.append("§7■§r");
            }
        }
        
        player.sendActionBar(actionBar.toString());
    }
    
    /**
     * Check if indicator is in catch zone
     */
    private boolean isIndicatorInCatchZone() {
        return indicatorPosition >= catchZoneStart && indicatorPosition <= catchZoneEnd;
    }
    
    /**
     * Calculate how close to perfect the catch was
     */
    private double getCatchAccuracy() {
        if (!isIndicatorInCatchZone()) {
            return 0.0;
        }
        
        // Calculate distance from center of catch zone
        double catchZoneCenter = (catchZoneStart + catchZoneEnd) / 2.0;
        double distanceFromCenter = Math.abs(indicatorPosition - catchZoneCenter);
        double catchZoneRadius = (catchZoneEnd - catchZoneStart) / 2.0;
        
        // Return accuracy as percentage (1.0 = perfect center, 0.0 = edge)
        return 1.0 - (distanceFromCenter / catchZoneRadius);
    }
    
    /**
     * Player attempts to catch
     */
    public void attemptCatch() {
        // Reset the round timer since player took action
        roundStartTime = System.currentTimeMillis();
        
        // Count every attempt (success or miss) for accuracy calculation
        session.incrementTotalRounds();
        
        if (isIndicatorInCatchZone()) {
            // Successful catch
            session.incrementSuccessfulCatches();
            
            // Determine which zone the indicator is in
            double catchZoneCenter = (catchZoneStart + catchZoneEnd) / 2.0;
            double catchZoneSize = catchZoneEnd - catchZoneStart;
            double distanceFromCenter = Math.abs(indicatorPosition - catchZoneCenter);
            double relativePosition = distanceFromCenter / (catchZoneSize / 2.0); // 0.0 at center, 1.0 at edge
            
            boolean isPerfect = relativePosition <= 0.30; // Within 30% of center (green zone)
            boolean isGood = relativePosition <= 0.65; // Within 65% of center (orange zone)
            
            // Check if all rounds completed
            if (currentRound >= requiredRounds) {
                // Show final completion message with quality
                if (isPerfect) {
                    session.incrementPerfectCatches();
                    player.sendTitle("§a§l✦ PERFECT ✦", "§e§lRound " + currentRound + "/" + requiredRounds + " Complete!", 5, 30, 10);
                } else if (isGood) {
                    player.sendTitle("§6§l✦ GOOD ✦", "§e§lRound " + currentRound + "/" + requiredRounds + " Complete!", 5, 30, 10);
                } else {
                    player.sendTitle("§e§l✦ NICE ✦", "§e§lRound " + currentRound + "/" + requiredRounds + " Complete!", 5, 30, 10);
                }
                session.complete();
            } else {
                // Show completion message for this round with quality
                if (isPerfect) {
                    session.incrementPerfectCatches();
                    player.sendTitle("§a§l✦ PERFECT ✦", "§e§lRound " + currentRound + "/" + requiredRounds + " Complete!", 5, 30, 5);
                } else if (isGood) {
                    player.sendTitle("§6§l✦ GOOD ✦", "§e§lRound " + currentRound + "/" + requiredRounds + " Complete!", 5, 30, 5);
                } else {
                    player.sendTitle("§e§l✦ NICE ✦", "§e§lRound " + currentRound + "/" + requiredRounds + " Complete!", 5, 30, 5);
                }
                
                // Start next round immediately (action bar updates right away)
                // Title message stays visible for 2 seconds as configured above
                startNewRound();
            }
        } else {
            // Miss
            missedCatchesThisRound++;
            int remainingMisses = maxMissesPerRound - missedCatchesThisRound;
            player.sendTitle("§c§lMISS!", "§7" + remainingMisses + " " + (remainingMisses == 1 ? "miss" : "misses") + " left this round", 5, 15, 5);
            
            if (missedCatchesThisRound >= maxMissesPerRound) {
                // Too many misses this round - fish escapes
                player.sendMessage("§c§lFISH ESCAPED! §7You missed too many times this round.");
                session.fail();
            }
        }
    }
    
    /**
     * Called when session completes successfully
     */
    public void onComplete() {
        // Calculate performance bonus
        double accuracy = session.getAccuracy();
        int perfectCount = session.getPerfectCatches();
        
        player.sendTitle(
            "§a§lFISHING SUCCESS!",
            String.format("§7Accuracy: §e%.1f%% §7| Perfect: §6%d", accuracy, perfectCount),
            10, 40, 10
        );
        
        // Grant loot and XP through the listener
        com.server.profiles.skills.skills.fishing.listeners.FishingListener listener = 
            new com.server.profiles.skills.skills.fishing.listeners.FishingListener();
        listener.grantFishingLoot(player, session);
        
        // Remove the fishing hook to reel in the line
        if (player.getFishHook() != null) {
            player.getFishHook().remove();
        }
        
        cleanup();
    }
    
    /**
     * Called when session is cancelled
     */
    public void onCancel() {
        player.sendTitle("§7Fishing Cancelled", "", 5, 20, 5);
        
        // Remove the fishing hook to reel in the line
        if (player.getFishHook() != null) {
            player.getFishHook().remove();
        }
        
        cleanup();
    }
    
    /**
     * Called when session fails
     */
    public void onFail() {
        player.sendTitle("§c§lFISHED AWAY!", "§7The fish got away...", 10, 40, 10);
        
        // Remove the fishing hook to reel in the line
        if (player.getFishHook() != null) {
            player.getFishHook().remove();
        }
        
        cleanup();
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        player.sendActionBar("");
    }
    
    // Getters
    
    public int getCurrentRound() {
        return currentRound;
    }
    
    public int getRequiredRounds() {
        return requiredRounds;
    }
    
    public int getMissedCatchesThisRound() {
        return missedCatchesThisRound;
    }
    
    public int getMaxMissesPerRound() {
        return maxMissesPerRound;
    }
    
    public void setMaxMissesPerRound(int maxMisses) {
        this.maxMissesPerRound = Math.max(1, maxMisses); // Minimum 1 miss allowed
    }
    
    public double getCatchZoneStart() {
        return catchZoneStart;
    }
    
    public double getCatchZoneEnd() {
        return catchZoneEnd;
    }
    
    public double getIndicatorPosition() {
        return indicatorPosition;
    }
}
