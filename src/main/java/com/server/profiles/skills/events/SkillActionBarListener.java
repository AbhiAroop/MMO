package com.server.profiles.skills.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.server.profiles.skills.display.SkillActionBarManager;

/**
 * Listener for skill events that should update the action bar
 */
public class SkillActionBarListener implements Listener {
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSkillExpGain(SkillExpGainEvent event) {
        // Show action bar for all skill XP gains
        // This ensures players see feedback for both main skills and subskills
        SkillActionBarManager.getInstance().handleSkillXpGain(event);
    }
}