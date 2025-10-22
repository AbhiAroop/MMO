package com.server.enchantments.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.server.Main;
import com.server.enchantments.effects.ElementalParticles;
import com.server.enchantments.elements.ElementType;
import com.server.enchantments.gui.AnvilCombiner;
import com.server.enchantments.gui.AnvilGUI;
import com.server.enchantments.items.EnchantmentTome;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

import de.tr7zw.changeme.nbtapi.NBTItem;

/**
 * Handles interactions with the custom anvil GUI.
 */
public class AnvilGUIListener implements Listener {
    
    private final Map<UUID, AnvilGUI> activeGUIs;
    private final Plugin plugin;
    
    public AnvilGUIListener(Plugin plugin) {
        this.activeGUIs = new HashMap<>();
        this.plugin = plugin;
    }
    
    /**
     * Registers a GUI for a player.
     */
    public void registerGUI(Player player, AnvilGUI gui) {
        activeGUIs.put(player.getUniqueId(), gui);
    }
    
    /**
     * Gets the active GUI for a player.
     */
    private AnvilGUI getGUI(Player player) {
        return activeGUIs.get(player.getUniqueId());
    }
    
    /**
     * Checks if a player has an active GUI.
     */
    public boolean hasActiveGUI(Player player) {
        return activeGUIs.containsKey(player.getUniqueId());
    }
    
    /**
     * Handles right-clicking the custom anvil armor stand to open GUI.
     */
    @EventHandler
    public void onAnvilClick(PlayerInteractEntityEvent event) {
        // Check if right-clicking an armor stand
        if (event.getRightClicked().getType() != EntityType.ARMOR_STAND) {
            return;
        }
        
        ArmorStand armorStand = (ArmorStand) event.getRightClicked();
        Player player = event.getPlayer();
        
        // Check if this is a custom anvil (has anvil helmet)
        ItemStack helmet = armorStand.getEquipment().getHelmet();
        if (helmet == null || helmet.getType() != Material.ANVIL) {
            return; // Not an anvil, ignore silently
        }
        
        plugin.getLogger().info("[Anvil] " + player.getName() + " clicked anvil armor stand");
        
        openAnvilGUI(player, armorStand);
    }
    
    /**
     * Handle right-click on blocks to detect nearby marker anvil armor stands
     * (Marker armor stands can't be clicked directly)
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check if player already has GUI open (prevent double-opening)
        if (hasActiveGUI(player)) {
            return;
        }
        
        // Find nearby anvil within click radius
        ArmorStand anvil = findNearbyAnvil(player.getLocation());
        if (anvil == null) return;
        
        plugin.getLogger().info("[Anvil] Found nearby anvil for " + player.getName());
        
        // Cancel event
        event.setCancelled(true);
        
        openAnvilGUI(player, anvil);
    }
    
    /**
     * Finds a nearby anvil armor stand within interaction range.
     */
    private ArmorStand findNearbyAnvil(Location location) {
        double searchRadius = 3.0; // Search within 3 blocks
        
        for (Entity entity : location.getWorld().getNearbyEntities(location, searchRadius, searchRadius, searchRadius)) {
            if (entity.getType() != EntityType.ARMOR_STAND) {
                continue;
            }
            
            ArmorStand stand = (ArmorStand) entity;
            ItemStack helmet = stand.getEquipment().getHelmet();
            
            if (helmet != null && helmet.getType() == Material.ANVIL) {
                // Check if player is looking at it (roughly)
                double distance = location.distance(stand.getLocation());
                if (distance <= searchRadius) {
                    return stand;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Opens the anvil GUI for a player.
     */
    private void openAnvilGUI(Player player, ArmorStand anvil) {
        // Check permissions
        if (!player.hasPermission("mmo.anvil.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use custom anvils!");
            return;
        }
        
        // Check if player already has GUI open (prevent double-opening)
        if (hasActiveGUI(player)) {
            plugin.getLogger().info("[Anvil] Player already has GUI open, ignoring");
            return;
        }
        
        // Open custom GUI
        plugin.getLogger().info("[Anvil] Opening anvil GUI for " + player.getName());
        AnvilGUI gui = new AnvilGUI(player);
        registerGUI(player, gui);
        gui.open();
        
        player.sendMessage(ChatColor.GREEN + "✓ Opened Custom Anvil!");
        // Play anvil opening sound
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_LAND, 0.5f, 1.0f);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_IRON_DOOR_OPEN, 0.3f, 0.8f);
    }
    
    /**
     * Handles inventory clicks in the anvil GUI.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        AnvilGUI gui = getGUI(player);
        
        if (gui == null) {
            return;
        }
        
        if (event.getClickedInventory() == null) {
            return;
        }
        
        // Check if clicking in GUI
        if (event.getClickedInventory().equals(gui.getInventory())) {
            handleGUIClick(event, player, gui);
        } else if (event.getClickedInventory().equals(player.getInventory())) {
            // Handle shift-clicking from player inventory
            handlePlayerInventoryClick(event, player, gui);
        }
    }
    
    /**
     * Handles clicks in player inventory while anvil GUI is open.
     */
    private void handlePlayerInventoryClick(InventoryClickEvent event, Player player, AnvilGUI gui) {
        // Handle shift-clicking items from player inventory into GUI
        if (event.isShiftClick()) {
            ItemStack clickedItem = event.getCurrentItem();
            
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return; // Nothing to shift-click
            }
            
            // Try to place in input slot 1 first
            ItemStack slot1 = gui.getInput1();
            if ((slot1 == null || slot1.getType() == Material.AIR) && gui.canPlaceInSlot1(clickedItem)) {
                event.setCancelled(true);
                gui.getInventory().setItem(AnvilGUI.INPUT_SLOT_1, clickedItem.clone());
                event.setCurrentItem(null);
                
                // Sync and update preview
                org.bukkit.Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    gui.syncWithInventory();
                    updatePreview(gui);
                }, 1L);
                return;
            }
            
            // Try to place in input slot 2
            ItemStack slot2 = gui.getInput2();
            if ((slot2 == null || slot2.getType() == Material.AIR) && gui.canPlaceInSlot2(clickedItem)) {
                event.setCancelled(true);
                gui.getInventory().setItem(AnvilGUI.INPUT_SLOT_2, clickedItem.clone());
                event.setCurrentItem(null);
                
                // Sync and update preview
                org.bukkit.Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    gui.syncWithInventory();
                    updatePreview(gui);
                }, 1L);
                return;
            }
            
            // Can't place anywhere - show message
            if (!gui.canPlaceInSlot1(clickedItem) && !gui.canPlaceInSlot2(clickedItem)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Cannot place that item in the anvil.");
            } else {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Both input slots are full.");
            }
        }
    }
    
    /**
     * Handles clicks within the GUI.
     */
    private void handleGUIClick(InventoryClickEvent event, Player player, AnvilGUI gui) {
        int slot = event.getSlot();
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        
        // Default: cancel all clicks
        event.setCancelled(true);
        
        // Handle input slot 1
        if (slot == AnvilGUI.INPUT_SLOT_1) {
            // Validate item
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (!gui.canPlaceInSlot1(cursor)) {
                    player.sendMessage(ChatColor.RED + "Cannot place that item here!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                    return;
                }
                // Valid item being placed - play placement sound
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.1f);
            } else if (current != null && current.getType() != Material.AIR) {
                // Item being removed - play pickup sound
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 0.9f);
            }
            
            // Allow placing/taking
            event.setCancelled(false);
            
            // Schedule sync after click
            org.bukkit.Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                gui.syncWithInventory();
                updatePreview(gui);
            }, 1L);
            return;
        }
        
        // Handle input slot 2
        if (slot == AnvilGUI.INPUT_SLOT_2) {
            // Validate item
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (!gui.canPlaceInSlot2(cursor)) {
                    player.sendMessage(ChatColor.RED + "Cannot place that item here!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                    return;
                }
                // Valid item being placed - play placement sound
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.1f);
            } else if (current != null && current.getType() != Material.AIR) {
                // Item being removed - play pickup sound
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 0.9f);
            }
            
            // Allow placing/taking
            event.setCancelled(false);
            
            // Schedule sync after click
            org.bukkit.Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                gui.syncWithInventory();
                updatePreview(gui);
            }, 1L);
            return;
        }
        
        // Handle output slot
        if (gui.isOutputSlot(slot)) {
            // Block placing items
            if (cursor != null && cursor.getType() != Material.AIR) {
                player.sendMessage(ChatColor.RED + "Cannot place items in output slot!");
                return;
            }
            
            // Check if placeholder (can be red or green glass pane)
            if (current == null || current.getType() == Material.AIR || 
                current.getType() == Material.LIME_STAINED_GLASS_PANE ||
                current.getType() == Material.RED_STAINED_GLASS_PANE) {
                return; // Can't take placeholder
            }
            
            // Try to perform combine
            handleCombine(player, gui);
            return;
        }
        
        // All other slots stay cancelled
    }
    
    /**
     * Updates the preview output.
     */
    private void updatePreview(AnvilGUI gui) {
        ItemStack input1 = gui.getInput1();
        ItemStack input2 = gui.getInput2();
        
        if (input1 == null || input2 == null) {
            gui.clearPreview();
            return;
        }
        
        // Use preview calculation (shows all enchants, marks uncertainty)
        AnvilCombiner.PreviewResult preview = AnvilCombiner.calculatePreview(input1, input2);
        
        if (preview == null) {
            gui.clearPreview();
            return;
        }
        
        // Set preview with uncertainty flag
        gui.setPreviewOutput(preview.getResult(), preview.getXpCost(), preview.getEssenceCost(), preview.hasUncertainty());
        
        // Play subtle preview update sound
        Player player = gui.getPlayer();
        if (player != null) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.3f, 2.0f);
        }
    }
    
    /**
     * Handles combining items when player takes output.
     */
    private void handleCombine(Player player, AnvilGUI gui) {
        ItemStack preview = gui.getPreviewOutput();
        
        if (preview == null) {
            player.sendMessage(ChatColor.RED + "No valid combination!");
            return;
        }
        
        int xpCost = gui.getXpCost();
        int essenceCost = gui.getEssenceCost();
        
        // Check if player has enough XP
        if (player.getLevel() < xpCost) {
            player.sendMessage(ChatColor.RED + "Not enough XP! Need " + xpCost + " levels.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return;
        }
        
        // Check if player has enough essence
        ProfileManager profileManager = ProfileManager.getInstance();
        Integer activeSlot = profileManager.getActiveProfile(player.getUniqueId());
        
        if (activeSlot == null) {
            player.sendMessage(ChatColor.RED + "No active profile!");
            return;
        }
        
        PlayerProfile[] profiles = profileManager.getProfiles(player.getUniqueId());
        PlayerProfile profile = profiles[activeSlot];
        
        if (profile == null) {
            player.sendMessage(ChatColor.RED + "Profile not found!");
            return;
        }
        
        if (profile.getEssence() < essenceCost) {
            player.sendMessage(ChatColor.RED + "Not enough essence! Need " + essenceCost + " essence.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return;
        }
        
        // Play anvil working sounds
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.7f, 1.2f);
        
        // Consume costs
        player.setLevel(player.getLevel() - xpCost);
        profile.removeEssence(essenceCost);
        
        // Determine which input is the tome and which is the item
        ItemStack input1 = gui.getInput1();
        ItemStack input2 = gui.getInput2();
        boolean input1IsTome = EnchantmentTome.isEnchantedTome(input1);
        boolean input2IsTome = EnchantmentTome.isEnchantedTome(input2);
        boolean isTomeApplication = input1IsTome || input2IsTome;
        
        // Calculate actual result with RNG
        AnvilCombiner.CombineResult actualResult = AnvilCombiner.calculateResult(input1, input2);
        
        // Check if all enchantments failed (for tome applications)
        if (actualResult == null && isTomeApplication) {
            // All enchantments failed - give back the weapon/armor, consume tome and costs
            ItemStack weaponToReturn = input1IsTome ? input2.clone() : input1.clone();
            weaponToReturn.setAmount(1);
            player.getInventory().addItem(weaponToReturn);
            
            // Consume ONLY the tome (not the weapon)
            ItemStack tomeSlotItem = gui.getInventory().getItem(input1IsTome ? AnvilGUI.INPUT_SLOT_1 : AnvilGUI.INPUT_SLOT_2);
            if (tomeSlotItem != null) {
                tomeSlotItem.setAmount(tomeSlotItem.getAmount() - 1);
                if (tomeSlotItem.getAmount() <= 0) {
                    gui.getInventory().setItem(input1IsTome ? AnvilGUI.INPUT_SLOT_1 : AnvilGUI.INPUT_SLOT_2, null);
                }
            }
            
            // Clear GUI state
            gui.clearInputs();
            
            // Sync after changes
            org.bukkit.Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                gui.syncWithInventory();
                updatePreview(gui);
            }, 1L);
            
            // Send formatted failure message
            player.sendMessage("");
            player.sendMessage(ChatColor.DARK_GRAY + "╔══════════════════════════════════════════╗");
            player.sendMessage(ChatColor.DARK_GRAY + "║ " + ChatColor.RED + "✗ ALL ENCHANTMENTS FAILED" + ChatColor.DARK_GRAY + "                 ║");
            player.sendMessage(ChatColor.DARK_GRAY + "╠══════════════════════════════════════════╣");
            player.sendMessage(ChatColor.DARK_GRAY + "║ " + ChatColor.GRAY + "Lost: " + ChatColor.YELLOW + xpCost + " XP, " + 
                              essenceCost + " Essence, Tome" + ChatColor.DARK_GRAY + "   ║");
            player.sendMessage(ChatColor.DARK_GRAY + "║ " + ChatColor.GREEN + "Returned: " + ChatColor.GRAY + "Your item (unchanged)" + ChatColor.DARK_GRAY + "       ║");
            player.sendMessage(ChatColor.DARK_GRAY + "╚══════════════════════════════════════════╝");
            player.sendMessage("");
            
            // Play failure sounds
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_BREAK, 0.5f, 0.8f);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 0.7f, 0.5f);
            return;
        } else if (actualResult == null) {
            // Invalid combination for non-tome operations
            player.sendMessage(ChatColor.RED + "✗ Invalid combination!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return;
        }
        
        // Give result to player
        ItemStack cleanResult = actualResult.getResult();
        player.getInventory().addItem(cleanResult);
        
        // Give refund to player if any (e.g., excess fragments)
        if (actualResult.hasRefund()) {
            player.getInventory().addItem(actualResult.getRefund());
            player.sendMessage(ChatColor.YELLOW + "⟳ Refunded " + actualResult.getRefund().getAmount() + 
                             " excess fragments (enchants already at 100%)");
        }
        
        // Consume input items
        ItemStack slot1Item = gui.getInventory().getItem(AnvilGUI.INPUT_SLOT_1);
        ItemStack slot2Item = gui.getInventory().getItem(AnvilGUI.INPUT_SLOT_2);
        
        if (slot1Item != null) {
            slot1Item.setAmount(slot1Item.getAmount() - 1);
            if (slot1Item.getAmount() <= 0) {
                gui.getInventory().setItem(AnvilGUI.INPUT_SLOT_1, null);
            }
        }
        
        if (slot2Item != null) {
            slot2Item.setAmount(slot2Item.getAmount() - 1);
            if (slot2Item.getAmount() <= 0) {
                gui.getInventory().setItem(AnvilGUI.INPUT_SLOT_2, null);
            }
        }
        
        // Clear GUI state
        gui.clearInputs();
        
        // Sync after changes
        org.bukkit.Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            gui.syncWithInventory();
            updatePreview(gui);
        }, 1L);
        
        // Send formatted success message
        player.sendMessage("");
        player.sendMessage(ChatColor.DARK_GRAY + "╔══════════════════════════════════════════╗");
        player.sendMessage(ChatColor.DARK_GRAY + "║ " + ChatColor.GREEN + "✓ ITEMS COMBINED SUCCESSFULLY" + ChatColor.DARK_GRAY + "             ║");
        player.sendMessage(ChatColor.DARK_GRAY + "╠══════════════════════════════════════════╣");
        player.sendMessage(ChatColor.DARK_GRAY + "║ " + ChatColor.GRAY + "Cost: " + ChatColor.YELLOW + xpCost + " XP " + 
                          ChatColor.GRAY + "& " + ChatColor.YELLOW + essenceCost + " Essence" + ChatColor.DARK_GRAY + "              ║");
        player.sendMessage(ChatColor.DARK_GRAY + "╚══════════════════════════════════════════╝");
        player.sendMessage("");
        
        // Play success sounds
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        
        // Spawn particle effects based on the result
        // Extract enchantment elements from the result item
        java.util.List<ElementType> resultElements = new java.util.ArrayList<>();
        NBTItem nbtResult = new NBTItem(cleanResult);
        if (nbtResult.hasTag("MMO_EnchantCount")) {
            int enchantCount = nbtResult.getInteger("MMO_EnchantCount");
            for (int i = 0; i < enchantCount; i++) {
                String prefix = "MMO_Enchant_" + i + "_";
                if (nbtResult.hasTag(prefix + "ID")) {
                    String enchantID = nbtResult.getString(prefix + "ID");
                    com.server.enchantments.data.CustomEnchantment enchant = 
                        com.server.enchantments.EnchantmentRegistry.getInstance().getEnchantment(enchantID);
                    if (enchant != null && enchant.getElement() != null) {
                        resultElements.add(enchant.getElement());
                    }
                }
            }
        }
        
        // Spawn particles for each unique element
        java.util.Set<ElementType> uniqueElements = new java.util.HashSet<>(resultElements);
        for (ElementType element : uniqueElements) {
            ElementalParticles.spawnElementalBurst(player.getLocation().add(0, 1, 0), element, 0.8);
        }
        
        // Create ring effect if we have enchantments
        if (!uniqueElements.isEmpty()) {
            ElementType primaryElement = resultElements.get(0);
            ElementalParticles.spawnElementalRing(player.getLocation().add(0, 0.1, 0), primaryElement, 1.2);
        }
    }
    
    /**
     * Handles closing the GUI.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        AnvilGUI gui = getGUI(player);
        
        if (gui == null) {
            return;
        }
        
        // Return items to player
        ItemStack input1 = gui.getInventory().getItem(AnvilGUI.INPUT_SLOT_1);
        ItemStack input2 = gui.getInventory().getItem(AnvilGUI.INPUT_SLOT_2);
        
        if (input1 != null && input1.getType() != Material.AIR) {
            player.getInventory().addItem(input1);
        }
        
        if (input2 != null && input2.getType() != Material.AIR) {
            player.getInventory().addItem(input2);
        }
        
        // Play closing sound
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_CLOSE, 0.7f, 1.0f);
        
        // Unregister GUI
        activeGUIs.remove(player.getUniqueId());
    }
}
