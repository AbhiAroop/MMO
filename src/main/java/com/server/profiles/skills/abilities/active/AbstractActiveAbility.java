package com.server.profiles.skills.abilities.active;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.server.profiles.skills.abilities.AbstractSkillAbility;

/**
 * Abstract base class for active abilities
 */
public abstract class AbstractActiveAbility extends AbstractSkillAbility implements ActiveAbility {
    
    private static final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>> playerCooldowns = new ConcurrentHashMap<>();
    
    protected final int cooldownSeconds;
    protected final String activationMethod;
    
    public AbstractActiveAbility(String id, String displayName, String description, 
                              String skillId, Material icon, String unlockRequirement,
                              int cooldownSeconds, String activationMethod) {
        super(id, displayName, description, skillId, icon, unlockRequirement);
        this.cooldownSeconds = cooldownSeconds;
        this.activationMethod = activationMethod;
    }
    
    @Override
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }
    
    @Override
    public String getActivationMethod() {
        return activationMethod;
    }
    
    @Override
    public boolean isOnCooldown(Player player) {
        return getCooldownRemaining(player) > 0;
    }
    
    @Override
    public long getCooldownRemaining(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        if (!playerCooldowns.containsKey(playerUUID) || 
            !playerCooldowns.get(playerUUID).containsKey(id)) {
            return 0;
        }
        
        long cooldownUntil = playerCooldowns.get(playerUUID).get(id);
        long now = System.currentTimeMillis();
        
        return Math.max(0, cooldownUntil - now);
    }
    
    @Override
    public void setCooldown(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // Initialize nested maps if needed
        if (!playerCooldowns.containsKey(playerUUID)) {
            playerCooldowns.put(playerUUID, new ConcurrentHashMap<>());
        }
        
        // Calculate cooldown end time
        long cooldownUntil = System.currentTimeMillis() + (cooldownSeconds * 1000);
        
        // Set the cooldown
        playerCooldowns.get(playerUUID).put(id, cooldownUntil);
    }
    
    @Override
    protected void addAbilityInfoToLore(List<String> lore, Player player) {
        // Add cooldown information
        long cooldownRemaining = getCooldownRemaining(player);
        if (cooldownRemaining > 0) {
            long secondsRemaining = TimeUnit.MILLISECONDS.toSeconds(cooldownRemaining);
            lore.add(ChatColor.RED + "Cooldown: " + secondsRemaining + "s remaining");
        } else {
            lore.add(ChatColor.GREEN + "Ready to use!");
        }
        
        lore.add("");
        lore.add(ChatColor.YELLOW + "Cooldown: " + cooldownSeconds + "s");
        lore.add("");
        lore.add(ChatColor.YELLOW + "How to use:");
        lore.add(ChatColor.GRAY + activationMethod);
        
        // Add active-specific details
        addActiveDetailsToLore(lore);
    }
    
    @Override
    protected String getAbilityType() {
        return "ACTIVE";
    }
    
    /**
     * Add active ability-specific details to the lore
     */
    protected abstract void addActiveDetailsToLore(List<String> lore);
}