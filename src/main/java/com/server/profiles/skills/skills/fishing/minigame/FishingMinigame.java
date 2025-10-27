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
    private double catchZoneStart; // 0.0 to 100.0
    private double catchZoneEnd; // 0.0 to 100.0
    
    private int currentRound;
    private int requiredRounds;
    private int missedCatches;
    private static final int MAX_MISSED_CATCHES = 3;
    
    // Timing constants
    private static final long TICK_INTERVAL = 1L; // Run every tick (50ms)
    private static final double PERFECT_CATCH_THRESHOLD = 0.05; // 5% of catch zone
    
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
        this.missedCatches = 0;
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
        session.incrementTotalRounds();
        
        // Calculate catch zone size based on stats (with null check)
        double catchZoneSize;
        if (rodFishingSubskill != null) {
            catchZoneSize = rodFishingSubskill.getCatchZoneSize(player);
        } else {
            catchZoneSize = 10.0; // Default value if subskill not available
        }
        
        FishingType type = session.getFishingType();
        catchZoneSize = catchZoneSize / type.getDifficultyMultiplier();
        
        // Random catch zone position (ensure it fits within bounds)
        double maxStart = 100.0 - catchZoneSize;
        catchZoneStart = Math.random() * maxStart;
        catchZoneEnd = catchZoneStart + catchZoneSize;
        
        // Reset indicator to random starting position
        indicatorPosition = Math.random() * 100.0;
        indicatorDirection = Math.random() < 0.5 ? 1.0 : -1.0;
        
        // Update boss bar title
        bossBar.setTitle(session.getFishingType().getColoredName() + " §7- Round " + currentRound + "/" + requiredRounds);
    }
    
    /**
     * Update indicator position
     */
    private void updateIndicator() {
        // Get indicator speed from stats (with null check)
        double speed;
        if (rodFishingSubskill != null) {
            speed = rodFishingSubskill.getIndicatorSpeed(player);
        } else {
            speed = 2.0; // Default speed if subskill not available
        }
        
        FishingType type = session.getFishingType();
        speed = speed * type.getDifficultyMultiplier();
        
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
     * Update visual display (boss bar and action bar)
     */
    private void updateDisplay() {
        // Update boss bar color based on indicator position
        if (isIndicatorInCatchZone()) {
            bossBar.setColor(BarColor.GREEN);
        } else {
            bossBar.setColor(BarColor.RED);
        }
        
        // Create action bar display
        StringBuilder actionBar = new StringBuilder();
        int totalChars = 50;
        
        for (int i = 0; i < totalChars; i++) {
            double position = (i / (double) totalChars) * 100.0;
            
            // Check if this position is the indicator
            if (Math.abs(position - indicatorPosition) < 2.0) {
                actionBar.append("§e▼§r");
            }
            // Check if this position is in catch zone
            else if (position >= catchZoneStart && position <= catchZoneEnd) {
                actionBar.append("§a■§r");
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
        if (isIndicatorInCatchZone()) {
            // Successful catch
            session.incrementSuccessfulCatches();
            
            double accuracy = getCatchAccuracy();
            boolean isPerfect = accuracy >= (1.0 - PERFECT_CATCH_THRESHOLD);
            
            if (isPerfect) {
                session.incrementPerfectCatches();
                player.sendTitle("§6§l✦ PERFECT! ✦", "§eRound " + currentRound + "/" + requiredRounds, 5, 20, 5);
            } else {
                player.sendTitle("§a§lGOOD!", "§7Round " + currentRound + "/" + requiredRounds, 5, 15, 5);
            }
            
            // Check if all rounds completed
            if (currentRound >= requiredRounds) {
                session.complete();
            } else {
                // Start next round
                startNewRound();
            }
        } else {
            // Miss
            missedCatches++;
            player.sendTitle("§c§lMISS!", "§7" + (MAX_MISSED_CATCHES - missedCatches) + " attempts left", 5, 15, 5);
            
            if (missedCatches >= MAX_MISSED_CATCHES) {
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
    
    public int getMissedCatches() {
        return missedCatches;
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
