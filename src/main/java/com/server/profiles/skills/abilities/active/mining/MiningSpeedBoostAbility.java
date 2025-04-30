package com.server.profiles.skills.abilities.active.mining;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.server.profiles.skills.abilities.active.AbstractActiveAbility;
import com.server.profiles.skills.core.SkillType;

/**
 * Mining Speed Boost ability - temporarily increases mining speed
 */
public class MiningSpeedBoostAbility extends AbstractActiveAbility {

    private static final int DURATION_SECONDS = 10;
    private static final int HASTE_LEVEL = 2; // Haste III (0-based indexing)
    
    public MiningSpeedBoostAbility() {
        super(
            "mining_speed_boost",
            "Mining Frenzy",
            "Temporarily boosts your mining speed",
            SkillType.MINING.getId(),
            Material.GOLDEN_PICKAXE,
            "Reach Mining level 20",
            60, // 60 second cooldown
            "Crouch + Right-click with a pickaxe"
        );
    }

    @Override
    public boolean activate(Player player) {
        if (isOnCooldown(player)) {
            player.sendMessage(ChatColor.RED + "Mining Frenzy is on cooldown! " + 
                              (getCooldownRemaining(player) / 1000) + "s remaining.");
            return false;
        }
        
        // Apply haste effect
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.HASTE, 
            DURATION_SECONDS * 20, // Convert to ticks
            HASTE_LEVEL,
            false, // No ambient particles
            true,  // Show particles
            true   // Show icon
        ));
        
        // Play activation sound
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.5f);
        
        // Show activation message
        player.sendMessage(ChatColor.GREEN + "Mining Frenzy activated! Mining speed boosted for " + 
                          DURATION_SECONDS + " seconds.");
        
        // Set cooldown
        setCooldown(player);
        
        return true;
    }

    @Override
    protected void addActiveDetailsToLore(List<String> lore) {
        lore.add("");
        lore.add(ChatColor.GRAY + "When activated, this ability gives");
        lore.add(ChatColor.GRAY + "you Haste " + (HASTE_LEVEL + 1) + " for " + DURATION_SECONDS + " seconds,");
        lore.add(ChatColor.GRAY + "significantly increasing mining speed.");
    }
}