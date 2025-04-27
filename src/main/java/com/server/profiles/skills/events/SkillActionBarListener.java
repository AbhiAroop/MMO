package com.server.profiles.skills.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.display.SkillActionBarManager;

/**
 * Listener for skill events that should update the action bar
 */
public class SkillActionBarListener implements Listener {
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSkillExpGain(SkillExpGainEvent event) {
        Skill skill = event.getSkill();
        
        // Prioritize subskill progression display
        // Main skills progress more slowly and are generally displayed less frequently
        // We want to focus on showing the more common and faster progressing subskills
        
        if (!skill.isMainSkill()) {
            // This is a subskill, show it directly
            SkillActionBarManager.getInstance().handleSkillXpGain(event);
        } else {
            // This is a main skill - only show it if the amount is significant 
            // AND there's no active subskill display
            // For larger XP amounts, we still want to show main skill progression
            if (event.getAmount() >= 15.0) {
                // Check if the player is already seeing a subskill progression
                Player player = event.getPlayer();
                if (!SkillActionBarManager.getInstance().hasActiveSubskillDisplay(player)) {
                    SkillActionBarManager.getInstance().handleSkillXpGain(event);
                }
            }
        }
    }
}