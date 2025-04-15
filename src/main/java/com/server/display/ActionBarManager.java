package com.server.display;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.stats.PlayerStats;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ActionBarManager {
    private final Main plugin;
    private BukkitRunnable actionBarTask;
    private int tickCounter = 0;
    private static final int TICKS_PER_SECOND = 20;

    public ActionBarManager(Main plugin) {
        this.plugin = plugin;
    }

    public void startActionBarUpdates() {
        actionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickCounter++;
                
                // Process all online players
                for (Player player : plugin.getServer().getOnlinePlayers()) {
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
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;

        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;

        PlayerStats stats = profile.getStats();
        
        // Calculate temporary mana bonus from held item
        int bonusMana = 0;
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem != null && heldItem.hasItemMeta() && heldItem.getItemMeta().hasLore()) {
            for (String loreLine : heldItem.getItemMeta().getLore()) {
                if (loreLine.contains("Mana:")) {
                    try {
                        String manaStr = loreLine.split("\\+")[1].trim();
                        bonusMana = (int) Double.parseDouble(manaStr);
                    } catch (Exception e) {
                        // Ignore parsing errors
                    }
                }
            }
        }
        
        // Get current mana values
        int currentMana = stats.getMana();
        int effectiveTotalMana = stats.getTotalMana() + bonusMana;
        
        // Only regenerate if we haven't reached the effective cap
        if (currentMana < effectiveTotalMana) {
            int newMana = Math.min(currentMana + stats.getManaRegen(), effectiveTotalMana);
            
            // Use uncapped setMana to allow exceeding base totalMana
            stats.setMana(newMana);
        }
    }

    private void updateActionBar(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;

        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;

        PlayerStats stats = profile.getStats();
        
        // Check held item for weapon damage and mana bonuses
        int physicalDamage = 0;
        int magicDamage = 0;
        int bonusMana = 0;
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem != null && heldItem.hasItemMeta() && heldItem.getItemMeta().hasLore()) {
            for (String loreLine : heldItem.getItemMeta().getLore()) {
                // Check for physical damage specifically
                if (loreLine.contains("Physical Damage:")) {
                    try {
                        String damageStr = loreLine.split("\\+")[1].trim();
                        physicalDamage = (int) Double.parseDouble(damageStr);
                    } catch (Exception e) {
                        // Ignore parsing errors
                    }
                }
                // Check for magic damage specifically
                else if (loreLine.contains("Magic Damage:")) {
                    try {
                        String damageStr = loreLine.split("\\+")[1].trim();
                        magicDamage = (int) Double.parseDouble(damageStr);
                    } catch (Exception e) {
                        // Ignore parsing errors
                    }
                }
                // Check for mana specifically
                else if (loreLine.contains("Mana:")) {
                    try {
                        String manaStr = loreLine.split("\\+")[1].trim();
                        bonusMana = (int) Double.parseDouble(manaStr);
                    } catch (Exception e) {
                        // Ignore parsing errors
                    }
                }
            }
        }
        
        // Calculate effective total mana for display
        int effectiveTotalMana = stats.getTotalMana() + bonusMana;
        
        // Create action bar text with real-time values
        String actionBar = String.format(
            "§c❤ %.1f/%d §8| §e⚔ %d §8| §b✦ %d §8| §9✧ %d/%d §8| §a♦ %d/s",
            player.getHealth(),
            stats.getHealth(),
            stats.getPhysicalDamage() + physicalDamage,
            stats.getMagicDamage() + magicDamage,
            stats.getMana(),
            effectiveTotalMana,
            stats.getManaRegen()
        );

        player.spigot().sendMessage(
            ChatMessageType.ACTION_BAR,
            TextComponent.fromLegacyText(actionBar)
        );
    }
}