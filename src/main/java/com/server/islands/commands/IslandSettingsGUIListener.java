package com.server.islands.commands;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.server.islands.managers.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Handles clicks in the Island Settings GUI
 */
public class IslandSettingsGUIListener implements Listener {
    
    private final IslandManager islandManager;
    
    public IslandSettingsGUIListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        String title = org.bukkit.Bukkit.getServer().getClass().getPackage().getName();
        Component viewTitle = event.getView().title();
        String titleString = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(viewTitle);
        
        if (!titleString.equals("⚙ Island Settings")) return;
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        int slot = event.getSlot();
        
        // PVP Toggle (slot 20)
        if (slot == 20) {
            handlePvPToggle(player);
        }
        // Visitor Access Toggle (slot 24)
        else if (slot == 24) {
            handleVisitorToggle(player);
        }
        // Back button (slot 49)
        else if (slot == 49) {
            player.closeInventory();
            IslandMenuGUI.open(player, islandManager);
        }
    }
    
    private void handlePvPToggle(Player player) {
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) return;
            
            islandManager.loadIsland(islandId).thenAccept(island -> {
                if (island == null) return;
                
                boolean newState = !island.isPvpEnabled();
                island.setPvpEnabled(newState);
                
                // Save to database using the data manager
                islandManager.getDataManager().saveIsland(island).thenRun(() -> {
                    org.bukkit.Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                        if (newState) {
                            player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .append(Component.text("Island PVP has been ", NamedTextColor.GREEN))
                                .append(Component.text("ENABLED", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                                .append(Component.text("!", NamedTextColor.GREEN)));
                        } else {
                            player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .append(Component.text("Island PVP has been ", NamedTextColor.GREEN))
                                .append(Component.text("DISABLED", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                                .append(Component.text("!", NamedTextColor.GREEN)));
                        }
                        
                        // Reopen GUI to show updated state
                        IslandSettingsGUI.open(player, islandManager);
                    });
                });
            });
        });
    }
    
    private void handleVisitorToggle(Player player) {
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) return;
            
            islandManager.loadIsland(islandId).thenAccept(island -> {
                if (island == null) return;
                
                boolean oldState = island.isVisitorsEnabled();
                boolean newState = !oldState;
                
                islandManager.getPlugin().getLogger().info("[Island] [Settings] Visitor toggle: old=" + oldState + ", new=" + newState);
                
                island.setVisitorsEnabled(newState);
                
                islandManager.getPlugin().getLogger().info("[Island] [Settings] After set: isVisitorsEnabled=" + island.isVisitorsEnabled());
                
                // Save to database using the data manager
                islandManager.getDataManager().saveIsland(island).thenRun(() -> {
                    islandManager.getPlugin().getLogger().info("[Island] [Settings] Island saved to database with visitorsEnabled=" + island.isVisitorsEnabled());
                    
                    org.bukkit.Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                        if (newState) {
                            player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .append(Component.text("Visitors are now ", NamedTextColor.GREEN))
                                .append(Component.text("ALLOWED", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                                .append(Component.text(" on your island!", NamedTextColor.GREEN)));
                        } else {
                            player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .append(Component.text("Visitors are now ", NamedTextColor.GREEN))
                                .append(Component.text("BLOCKED", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                                .append(Component.text(" from your island!", NamedTextColor.GREEN)));
                        }
                        
                        // Reopen GUI to show updated state
                        IslandSettingsGUI.open(player, islandManager);
                    });
                });
            });
        });
    }
}
