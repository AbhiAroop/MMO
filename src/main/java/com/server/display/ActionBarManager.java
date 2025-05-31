package com.server.display;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.display.SkillActionBarManager;
import com.server.profiles.stats.PlayerStats;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ActionBarManager implements Listener {
    private final Main plugin;
    private BukkitRunnable actionBarTask;
    private int tickCounter = 0;
    private static final int TICKS_PER_SECOND = 20;

    public ActionBarManager(Main plugin) {
        this.plugin = plugin;
        // Register this class as a listener for gamemode change events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Handle gamemode changes to immediately clear action bar for spectators
     */
    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        GameMode newGameMode = event.getNewGameMode();
        
        // Immediately clear action bar when entering spectator mode
        if (newGameMode == GameMode.SPECTATOR) {
            // Clear action bar immediately
            player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText("")
            );
            
            // Also clear any custom action bars from SkillActionBarManager
            SkillActionBarManager skillActionBarManager = SkillActionBarManager.getInstance();
            if (skillActionBarManager.hasCustomActionBar(player)) {
                skillActionBarManager.clearCustomActionBar(player);
            }
        }
        // Note: When leaving spectator mode, the action bar will automatically 
        // resume on the next tick cycle due to the existing logic
    }

    public void startActionBarUpdates() {
        actionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickCounter++;
                
                // Process all online players
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    // Skip action bar and mana regeneration for spectators
                    if (player.getGameMode() == GameMode.SPECTATOR) {
                        continue;
                    }
                    
                    // Update action bar every tick
                    updateActionBar(player);
                    
                    // Regenerate mana once per second
                    if (tickCounter >= TICKS_PER_SECOND) {
                        regeneratePlayerMana(player);
                    }
                }
                
                // Reset counter each second
                if (tickCounter >= TICKS_PER_SECOND) {
                    tickCounter = 0;
                }
            }
        };
        
        // Run every tick (20 ticks = 1 second)
        actionBarTask.runTaskTimer(plugin, 0L, 1L);
    }

    public void stopActionBarUpdates() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
    }

    private void regeneratePlayerMana(Player player) {
        // Additional check - don't regenerate mana for spectators
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;

        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;

        PlayerStats stats = profile.getStats();
        
        // IMPORTANT: Don't manually check held item for mana bonuses!
        // Let the mana regenerate naturally based on stats already set by StatScanManager
        
        // Get current mana values - already includes any held item bonuses
        int currentMana = stats.getMana();
        int totalMana = stats.getTotalMana();
        
        // Only regenerate if we haven't reached the cap
        if (currentMana < totalMana) {
            int newMana = Math.min(currentMana + stats.getManaRegen(), totalMana);
            stats.setMana(newMana);
        }
    }

    private void updateActionBar(Player player) {
        // Don't show action bar for spectators
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        
        // Check if there's a custom action bar from a minigame
        SkillActionBarManager skillActionBarManager = SkillActionBarManager.getInstance();
        if (skillActionBarManager.hasCustomActionBar(player)) {
            // We'll skip sending our default action bar as the minigame will handle displaying its own
            return;
        }

        // Continue with normal action bar display...
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;

        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;

        PlayerStats stats = profile.getStats();
        
        // Create action bar text with real-time values
        String actionBar = String.format(
            "§c❤ %.1f/%d §8| §e⚔ %d §8| §b✦ %d §8| §9✧ %d/%d §8| §a♦ %d/s",
            player.getHealth(),
            stats.getHealth(),
            stats.getPhysicalDamage(),
            stats.getMagicDamage(),
            stats.getMana(),
            stats.getTotalMana(),
            stats.getManaRegen()
        );

        player.spigot().sendMessage(
            ChatMessageType.ACTION_BAR,
            TextComponent.fromLegacyText(actionBar)
        );
    }
}