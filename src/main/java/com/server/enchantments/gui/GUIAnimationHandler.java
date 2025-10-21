package com.server.enchantments.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.server.Main;

/**
 * Handles animated transitions in enchantment and anvil GUIs.
 * Provides a 2-second animation sequence before showing the result.
 */
public class GUIAnimationHandler {
    
    private static final Map<UUID, AnimationState> activeAnimations = new HashMap<>();
    
    // Animation materials cycle
    private static final Material[] ANIMATION_MATERIALS = {
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
     * Starts an animation in the output slot before revealing the result.
     * 
     * @param player The player viewing the GUI
     * @param inventory The GUI inventory
     * @param outputSlot The slot where the result will appear
     * @param resultItem The final result item to show after animation
     * @param onComplete Callback to execute after animation completes
     */
    public static void startAnimation(Player player, Inventory inventory, int outputSlot, 
                                      ItemStack resultItem, Runnable onComplete) {
        // Cancel any existing animation for this player
        cancelAnimation(player);
        
        // Create animation state
        AnimationState state = new AnimationState(player, inventory, outputSlot, resultItem, onComplete);
        activeAnimations.put(player.getUniqueId(), state);
        
        // Start the animation
        state.start();
        
        // Play initial sound
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
    }
    
    /**
     * Cancels an active animation and immediately shows the result.
     * This happens when the player closes the GUI during animation.
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
     * Checks if a player has an active animation.
     */
    public static boolean hasActiveAnimation(Player player) {
        return activeAnimations.containsKey(player.getUniqueId());
    }
    
    /**
     * Skips the current animation and immediately shows the result.
     * Used when player clicks during animation.
     */
    public static void skipAnimation(Player player) {
        AnimationState state = activeAnimations.get(player.getUniqueId());
        if (state != null) {
            state.skip();
        }
    }
    
    /**
     * Internal class to manage animation state for a single player.
     */
    private static class AnimationState {
        private final Player player;
        private final Inventory inventory;
        private final int outputSlot;
        private final ItemStack resultItem;
        private final Runnable onComplete;
        private BukkitTask animationTask;
        private int frameCount = 0;
        private boolean cancelled = false;
        
        public AnimationState(Player player, Inventory inventory, int outputSlot, 
                            ItemStack resultItem, Runnable onComplete) {
            this.player = player;
            this.inventory = inventory;
            this.outputSlot = outputSlot;
            this.resultItem = resultItem;
            this.onComplete = onComplete;
        }
        
        public void start() {
            // Animation runs for 40 ticks (2 seconds), updating every 2 ticks
            animationTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (cancelled) {
                        this.cancel();
                        return;
                    }
                    
                    // Check if player still has inventory open
                    if (!inventory.equals(player.getOpenInventory().getTopInventory())) {
                        finish();
                        this.cancel();
                        return;
                    }
                    
                    // Update animation frame
                    updateFrame();
                    frameCount++;
                    
                    // Play sound every 5 frames
                    if (frameCount % 5 == 0) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f + (frameCount * 0.05f));
                    }
                    
                    // Spawn particles
                    if (frameCount % 3 == 0) {
                        player.spawnParticle(Particle.ENCHANT, 
                            player.getLocation().add(0, 1, 0), 
                            3, 0.3, 0.3, 0.3, 0.1);
                    }
                    
                    // Complete after 40 ticks (2 seconds)
                    if (frameCount >= 20) {
                        finish();
                        this.cancel();
                    }
                }
            }.runTaskTimer(Main.getInstance(), 0L, 2L);
        }
        
        private void updateFrame() {
            // Cycle through colorful glass panes
            Material material = ANIMATION_MATERIALS[frameCount % ANIMATION_MATERIALS.length];
            ItemStack frame = new ItemStack(material);
            ItemMeta meta = frame.getItemMeta();
            
            // Animated display name
            String[] symbols = {"◆", "◇", "❖", "✦", "✧", "✪", "✫", "✬", "✭", "✮"};
            String symbol = symbols[frameCount % symbols.length];
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + symbol + " Enchanting... " + symbol);
            
            // Progress bar
            int progress = (frameCount * 100) / 20;
            StringBuilder progressBar = new StringBuilder(ChatColor.GRAY + "[");
            int filled = progress / 5;
            for (int i = 0; i < 20; i++) {
                if (i < filled) {
                    progressBar.append(ChatColor.GREEN + "█");
                } else {
                    progressBar.append(ChatColor.DARK_GRAY + "█");
                }
            }
            progressBar.append(ChatColor.GRAY + "] " + ChatColor.YELLOW + progress + "%");
            
            List<String> lore = new ArrayList<>();
            lore.add(progressBar.toString());
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "✦ " + ChatColor.GRAY + "Infusing magical energies...");
            lore.add(ChatColor.DARK_GRAY + "✦ " + ChatColor.GRAY + "Close GUI to skip");
            meta.setLore(lore);
            
            frame.setItemMeta(meta);
            inventory.setItem(outputSlot, frame);
        }
        
        private void finish() {
            if (cancelled) return;
            
            // Show final result
            inventory.setItem(outputSlot, resultItem);
            
            // Play completion sound
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
            
            // Spawn completion particles
            player.spawnParticle(Particle.TOTEM_OF_UNDYING, 
                player.getLocation().add(0, 1, 0), 
                20, 0.5, 0.5, 0.5, 0.1);
            
            // Send success message
            player.sendMessage(ChatColor.GREEN + "✓ " + ChatColor.GOLD + "Enchantment complete!");
            
            // Execute completion callback
            if (onComplete != null) {
                onComplete.run();
            }
            
            // Remove from active animations
            activeAnimations.remove(player.getUniqueId());
        }
        
        public void skip() {
            if (cancelled) return;
            frameCount = 20; // Jump to end
            if (animationTask != null) {
                animationTask.cancel();
            }
            finish();
        }
        
        public void cancel() {
            cancelled = true;
            if (animationTask != null) {
                animationTask.cancel();
            }
            activeAnimations.remove(player.getUniqueId());
        }
    }
}
