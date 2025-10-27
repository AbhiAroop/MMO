package com.server.profiles.skills.skills.farming.botany;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Handles breeder block placement and interaction
 */
public class BreederBlockListener implements Listener {
    
    /**
     * Handle placing breeder block
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreederPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        
        // Check if it's a breeder block
        if (item.getType() != Material.CARVED_PUMPKIN) return;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return;
        if (meta.getCustomModelData() != BreederBlock.getBreederCMD()) return;
        
        // Cancel the block placement
        event.setCancelled(true);
        
        // Create breeder block at this location
        Location loc = event.getBlock().getLocation();
        BreederBlock breeder = new BreederBlock(loc);
        BotanyManager.getInstance().registerBreederBlock(breeder);
        
        // Consume item
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
        
        player.sendMessage("§a§l[✓] §aBreeder block placed at: §7" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        player.playSound(loc, org.bukkit.Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.2f);
    }
    
    /**
     * Handle right-clicking breeder block location
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBreederInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
        if (event.getClickedBlock() == null) return;
        
        Player player = event.getPlayer();
        Location blockLoc = event.getClickedBlock().getLocation();
        
        // Check if there's a breeder block at this location
        BreederBlock breeder = BotanyManager.getInstance().getBreederBlock(blockLoc);
        
        // Also check the block above (since armor stand might be floating above the clicked block)
        if (breeder == null) {
            Location aboveLoc = blockLoc.clone().add(0, 1, 0);
            breeder = BotanyManager.getInstance().getBreederBlock(aboveLoc);
        }
        
        if (breeder != null && breeder.isValid()) {
            event.setCancelled(true);
            breeder.openGUI(player);
        }
    }
    
    /**
     * Prevent players from manipulating breeder armor stands
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        ArmorStand stand = event.getRightClicked();
        
        ItemStack helmet = stand.getEquipment().getHelmet();
        if (helmet == null || helmet.getType() != Material.CARVED_PUMPKIN) return;
        
        ItemMeta meta = helmet.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return;
        if (meta.getCustomModelData() != BreederBlock.getBreederCMD()) return;
        
        // Cancel manipulation of breeder blocks
        event.setCancelled(true);
    }
    
    /**
     * Handle breaking breeder blocks (punch armor stand)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreederBreak(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ArmorStand)) return;
        if (!(event.getDamager() instanceof Player)) return;
        
        ArmorStand stand = (ArmorStand) event.getEntity();
        Player player = (Player) event.getDamager();
        
        // Check if it's a breeder block
        ItemStack helmet = stand.getEquipment().getHelmet();
        if (helmet == null || helmet.getType() != Material.CARVED_PUMPKIN) return;
        
        ItemMeta meta = helmet.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return;
        if (meta.getCustomModelData() != BreederBlock.getBreederCMD()) return;
        
        // Cancel damage
        event.setCancelled(true);
        
        // Remove breeder if player is sneaking
        if (player.isSneaking()) {
            Location loc = stand.getLocation().getBlock().getLocation();
            BreederBlock breeder = BotanyManager.getInstance().getBreederBlock(loc);
            
            if (breeder != null) {
                // Drop the breeder item
                loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), BreederBlock.createBreederItem());
                
                // Remove the breeder
                BotanyManager.getInstance().removeBreederBlock(loc);
                
                player.sendMessage("§e§l[!] §eBreeder block removed!");
                player.playSound(loc, org.bukkit.Sound.BLOCK_ANVIL_BREAK, 1.0f, 1.0f);
            }
        }
    }
}
