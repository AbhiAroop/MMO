package com.server.islands.listeners;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.server.islands.data.IslandMember;
import com.server.islands.data.PlayerIsland;
import com.server.islands.managers.IslandManager;
import com.server.util.BedrockPlayerUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Protects islands from unauthorized access.
 */
public class IslandProtectionListener implements Listener {
    
    private final IslandManager islandManager;
    
    public IslandProtectionListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }
    
    /**
     * Send an error message with action bar support for Bedrock players
     */
    private void sendErrorMessage(Player player, String message) {
        Component errorComponent = Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
            .append(Component.text(message, NamedTextColor.RED));
        player.sendMessage(errorComponent);
        
        // Send simplified version to action bar for Bedrock players
        String actionBarMessage = "§c✗ " + message;
        BedrockPlayerUtil.sendActionBar(player, actionBarMessage);
    }
    
    /**
     * Check if a player has permission to build on an island.
     */
    private boolean canBuild(Player player, Location location) {
        // Check if this location is on an island world
        String worldName = location.getWorld().getName();
        if (!worldName.startsWith("island_")) {
            return true; // Not an island world, allow
        }
        
        // Find which island this location belongs to
        PlayerIsland island = null;
        for (PlayerIsland checkIsland : islandManager.getCache().getAllIslands()) {
            if (checkIsland.getWorldName().equals(worldName)) {
                island = checkIsland;
                break;
            }
        }
        
        if (island == null) {
            return true; // Island not loaded, allow for now
        }
        
        UUID playerUuid = player.getUniqueId();
        
        // Check if player is the owner
        if (island.getOwnerUuid().equals(playerUuid)) {
            return true;
        }
        
        // Check if player is a member with build permissions
        IslandMember.IslandRole role = islandManager.getMemberRole(island.getIslandId(), playerUuid).join();
        
        if (role != null && role.hasPermission(IslandMember.IslandRole.MEMBER)) {
            return true; // MEMBER or higher can build
        }
        
        return false; // Not authorized
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        
        if (!canBuild(player, location)) {
            event.setCancelled(true);
            sendErrorMessage(player, "You don't have permission to break blocks on this island!");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        
        if (!canBuild(player, location)) {
            event.setCancelled(true);
            sendErrorMessage(player, "You don't have permission to place blocks on this island!");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getClickedBlock() == null) {
            return;
        }
        
        Player player = event.getPlayer();
        Location location = event.getClickedBlock().getLocation();
        
        // Only protect certain interactions (doors, buttons, levers, chests, etc.)
        switch (event.getClickedBlock().getType()) {
            case CHEST:
            case BARREL:
            case FURNACE:
            case BLAST_FURNACE:
            case SMOKER:
            case HOPPER:
            case DROPPER:
            case DISPENSER:
            case CRAFTING_TABLE:
            case ENCHANTING_TABLE:
            case ANVIL:
            case CHIPPED_ANVIL:
            case DAMAGED_ANVIL:
            case ENDER_CHEST:
            case SHULKER_BOX:
            case LECTERN:
            case BREWING_STAND:
                if (!canBuild(player, location)) {
                    event.setCancelled(true);
                    sendErrorMessage(player, "You don't have permission to use this on this island!");
                }
                break;
            default:
                break;
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Prevent players from attacking entities on other players' islands
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player damager = (Player) event.getDamager();
        Location location = event.getEntity().getLocation();
        
        String worldName = location.getWorld().getName();
        if (!worldName.startsWith("island_")) {
            return; // Not an island world
        }
        
        // Find which island this location belongs to
        PlayerIsland island = null;
        for (PlayerIsland checkIsland : islandManager.getCache().getAllIslands()) {
            if (checkIsland.getWorldName().equals(worldName)) {
                island = checkIsland;
                break;
            }
        }
        
        if (island == null) {
            return; // Island not loaded
        }
        
        UUID damagerUuid = damager.getUniqueId();
        
        // Check if target is a player (PVP check)
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            UUID victimUuid = victim.getUniqueId();
            
            // Check if both are members of this island
            IslandMember.IslandRole damagerRole = islandManager.getMemberRole(island.getIslandId(), damagerUuid).join();
            IslandMember.IslandRole victimRole = islandManager.getMemberRole(island.getIslandId(), victimUuid).join();
            
            boolean damagerIsMember = island.getOwnerUuid().equals(damagerUuid) || (damagerRole != null && damagerRole.hasPermission(IslandMember.IslandRole.MEMBER));
            boolean victimIsMember = island.getOwnerUuid().equals(victimUuid) || (victimRole != null && victimRole.hasPermission(IslandMember.IslandRole.MEMBER));
            
            // If both are members and PVP is disabled, block the attack
            if (damagerIsMember && victimIsMember && !island.isPvpEnabled()) {
                event.setCancelled(true);
                damager.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("PVP is disabled on this island!", NamedTextColor.RED)));
                return;
            }
        }
        
        // Check if player is the owner
        if (island.getOwnerUuid().equals(damagerUuid)) {
            return; // Owner can do anything
        }
        
        // Check if player is a member
        IslandMember.IslandRole role = islandManager.getMemberRole(island.getIslandId(), damagerUuid).join();
        
        if (role != null && role.hasPermission(IslandMember.IslandRole.MEMBER)) {
            return; // Members can attack entities
        }
        
        // Not authorized
        event.setCancelled(true);
        damager.sendMessage(Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
            .append(Component.text("You don't have permission to do that on this island!", NamedTextColor.RED)));
    }
}
