package com.server.enchantments.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;

/**
 * Handles animated GUI transitions for the Enchantment Table GUI only.
 * Provides a 2-second animation sequence with colored glass panes before showing the result.
 */
public class GUIAnimationHandler {
    
    private static final Map<UUID, AnimationState> activeAnimations = new HashMap<>();
    
    // Animation configuration
    private static final int ANIMATION_DURATION = 40; // 40 ticks = 2 seconds
    private static final int FRAME_INTERVAL = 2; // Update every 2 ticks
    
    // Color sequence for animation frames
    private static final Material[] FRAME_COLORS = {
        Material.PURPLE_STAINED_GLASS_PANE,
        Material.MAGENTA_STAINED_GLASS_PANE,
        Material.PINK_STAINED_GLASS_PANE,
        Material.LIGHT_BLUE_STAINED_GLASS_PANE,
        Material.CYAN_STAINED_GLASS_PANE,
        Material.LIME_STAINED_GLASS_PANE,
        Material.YELLOW_STAINED_GLASS_PANE,
        Material.ORANGE_STAINED_GLASS_PANE,
        Material.RED_STAINED_GLASS_PANE,
        Material.WHITE_STAINED_GLASS_PANE
    };
    
    /**
     * Starts an animation for a player's GUI.
     * 
     * @param player The player viewing the GUI
     * @param inventory The inventory to animate
     * @param slot The slot to animate in
     * @param resultItem The final item to show after animation
     * @param onComplete Callback to run after animation completes
     */
    public static void startAnimation(Player player, Inventory inventory, int slot, 
                                     ItemStack resultItem, Runnable onComplete) {
        startAnimation(player, inventory, slot, resultItem, null, onComplete);
    }
    
    /**
     * Starts an animation for a player's GUI with a success message.
     * 
     * @param player The player viewing the GUI
     * @param inventory The inventory to animate
     * @param slot The slot to animate in
     * @param resultItem The final item to show after animation
     * @param successMessage Optional message to show when animation is cancelled/skipped
     * @param onComplete Callback to run after animation completes
     */
    public static void startAnimation(Player player, Inventory inventory, int slot, 
                                     ItemStack resultItem, String successMessage, Runnable onComplete) {
        // Cancel any existing animation for this player
        cancelAnimation(player);
        
        // Create new animation state
        AnimationState state = new AnimationState(player, inventory, slot, resultItem, successMessage, onComplete);
        activeAnimations.put(player.getUniqueId(), state);
        
        // Start the animation
        state.start();
    }
    
    /**
     * Cancels an active animation and returns the result item.
     * 
     * @param player The player whose animation to cancel
     * @return The result item if animation was active, null otherwise
     */
    public static ItemStack cancelAnimation(Player player) {
        AnimationState state = activeAnimations.remove(player.getUniqueId());
        if (state != null) {
            state.cancel();
            return state.resultItem;
        }
        return null;
    }
    
    /**
     * Gets the stored success message for a player's animation.
     * 
     * @param player The player whose message to get
     * @return The success message if available, null otherwise
     */
    public static String getSuccessMessage(Player player) {
        AnimationState state = activeAnimations.get(player.getUniqueId());
        if (state != null) {
            return state.successMessage;
        }
        return null;
    }
    
    /**
     * Checks if a player has an active animation.
     * 
     * @param player The player to check
     * @return true if animation is active, false otherwise
     */
    public static boolean hasActiveAnimation(Player player) {
        return activeAnimations.containsKey(player.getUniqueId());
    }
    
    /**
     * Skips the animation and immediately shows the result.
     * 
     * @param player The player whose animation to skip
     */
    public static void skipAnimation(Player player) {
        AnimationState state = activeAnimations.remove(player.getUniqueId());
        if (state != null) {
            state.complete();
        }
    }
    
    /**
     * Internal class to manage animation state for a single player.
     */
    private static class AnimationState extends BukkitRunnable {
        private final Player player;
        private final Inventory inventory;
        private final int slot;
        private final ItemStack resultItem;
        private final String successMessage;
        private final Runnable onComplete;
        private int ticksElapsed = 0;
        private boolean completed = false;
        
        public AnimationState(Player player, Inventory inventory, int slot, 
                            ItemStack resultItem, String successMessage, Runnable onComplete) {
            this.player = player;
            this.inventory = inventory;
            this.slot = slot;
            this.resultItem = resultItem;
            this.successMessage = successMessage;
            this.onComplete = onComplete;
        }
        
        public void start() {
            runTaskTimer(Main.getInstance(), 0L, FRAME_INTERVAL);
        }
        
        @Override
        public void run() {
            // Check if player is still online and has the GUI open
            if (!player.isOnline() || player.getOpenInventory().getTopInventory() != inventory) {
                complete();
                return;
            }
            
            ticksElapsed += FRAME_INTERVAL;
            
            // Check if animation is complete
            if (ticksElapsed >= ANIMATION_DURATION) {
                complete();
                return;
            }
            
            // Update animation frame
            updateFrame();
        }
        
        private void updateFrame() {
            // Calculate progress (0.0 to 1.0)
            float progress = (float) ticksElapsed / ANIMATION_DURATION;
            
            // Select color based on progress
            int colorIndex = (int) (progress * FRAME_COLORS.length);
            if (colorIndex >= FRAME_COLORS.length) {
                colorIndex = FRAME_COLORS.length - 1;
            }
            
            Material frameColor = FRAME_COLORS[colorIndex];
            
            // Create animated frame item
            ItemStack frame = new ItemStack(frameColor);
            ItemMeta meta = frame.getItemMeta();
            if (meta != null) {
                // Add progress bar to name
                int progressBars = (int) (progress * 20);
                StringBuilder progressBar = new StringBuilder(ChatColor.GOLD + "");
                for (int i = 0; i < 20; i++) {
                    if (i < progressBars) {
                        progressBar.append("█");
                    } else {
                        progressBar.append(ChatColor.GRAY).append("█");
                    }
                }
                
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "✦ Enchanting... " + 
                                   ChatColor.YELLOW + (int)(progress * 100) + "% " + ChatColor.LIGHT_PURPLE + "✦");
                meta.setLore(java.util.Arrays.asList(
                    progressBar.toString(),
                    "",
                    ChatColor.GRAY + "⌛ " + ChatColor.DARK_GRAY + "Magic is taking form...",
                    ChatColor.GRAY + "Click or close GUI to skip"
                ));
                frame.setItemMeta(meta);
            }
            
            // Set the frame in the slot
            inventory.setItem(slot, frame);
            
            // Play sound effects
            if (ticksElapsed % 8 == 0) { // Every 8 ticks (0.4 seconds)
                float pitch = 0.8f + (progress * 0.8f); // Pitch increases with progress
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.3f, pitch);
            }
            
            // Spawn particles
            if (ticksElapsed % 4 == 0) { // Every 4 ticks (0.2 seconds)
                player.spawnParticle(org.bukkit.Particle.ENCHANT, 
                    player.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.1);
            }
        }
        
        private void complete() {
            if (completed) {
                return;
            }
            completed = true;
            
            // Cancel the task
            cancel();
            
            // Remove from active animations
            activeAnimations.remove(player.getUniqueId());
            
            // Set the final result item
            inventory.setItem(slot, resultItem);
            
            // Play completion sound
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
            
            // Spawn completion particles
            player.spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, 
                player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            
            // Run completion callback
            if (onComplete != null) {
                Bukkit.getScheduler().runTask(Main.getInstance(), onComplete);
            }
        }
    }
}
