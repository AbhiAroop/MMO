package com.server.profiles.skills.skills.farming.botany;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.debug.DebugManager;

/**
 * GUI for the crop breeder
 */
public class BreederGUI {
    
    private static final Map<UUID, BreederData> openGUIs = new HashMap<>(); // Player UUID -> BreederData
    private static final Map<UUID, UUID> breederOwners = new HashMap<>(); // Breeder UUID -> Player UUID (who has it open)
    private static final BreederGUIListener listener = new BreederGUIListener();
    
    private final BreederData data;
    private final Inventory inventory;
    
    public BreederGUI(BreederData data) {
        this.data = data;
        // Clean any corrupted inventory data before setting up GUI
        data.cleanInventory();
        this.inventory = Bukkit.createInventory(null, 27, "§6§lCrop Breeder");
        setupGUI();
    }
    
    /**
     * Get the static listener for registration
     */
    public static Listener getListener() {
        return listener;
    }
    
    /**
     * Setup the GUI layout
     */
    private void setupGUI() {
        // Fill with glass panes
        ItemStack grayPane = createGlassPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, grayPane);
        }
        
        // Clear input slots
        inventory.setItem(BreederData.SEED_SLOT_1, null);
        inventory.setItem(BreederData.SEED_SLOT_2, null);
        inventory.setItem(BreederData.CATALYST_SLOT, null);
        inventory.setItem(BreederData.FLUID_SLOT, null);
        inventory.setItem(BreederData.OUTPUT_SLOT, null);
        inventory.setItem(BreederData.TIMER_DISPLAY_SLOT, null);
        
        // Add labels
        inventory.setItem(1, createGlassPane(Material.LIME_STAINED_GLASS_PANE, "§a§lSeed 1"));
        inventory.setItem(3, createGlassPane(Material.LIME_STAINED_GLASS_PANE, "§a§lSeed 2"));
        inventory.setItem(5, createGlassPane(Material.ORANGE_STAINED_GLASS_PANE, "§6§lCatalyst"));
        inventory.setItem(7, createGlassPane(Material.CYAN_STAINED_GLASS_PANE, "§b§lFluid"));
        inventory.setItem(15, createGlassPane(Material.YELLOW_STAINED_GLASS_PANE, "§e§lOutput"));
        
        // Add start button and timer display
        updateStartButton();
        updateTimerDisplay();
        
        // Load saved items ONLY from valid slots (don't overwrite decorative panes)
        ItemStack seed1 = data.getItem(BreederData.SEED_SLOT_1);
        ItemStack seed2 = data.getItem(BreederData.SEED_SLOT_2);
        ItemStack catalyst = data.getItem(BreederData.CATALYST_SLOT);
        ItemStack fluid = data.getItem(BreederData.FLUID_SLOT);
        ItemStack output = data.getItem(BreederData.OUTPUT_SLOT);
        
        if (seed1 != null) inventory.setItem(BreederData.SEED_SLOT_1, seed1);
        if (seed2 != null) inventory.setItem(BreederData.SEED_SLOT_2, seed2);
        if (catalyst != null) inventory.setItem(BreederData.CATALYST_SLOT, catalyst);
        if (fluid != null) inventory.setItem(BreederData.FLUID_SLOT, fluid);
        if (output != null) inventory.setItem(BreederData.OUTPUT_SLOT, output);
    }
    
    /**
     * Update the start button based on breeding state
     */
    private void updateStartButton() {
        ItemStack button;
        
        if (data.isBreeding()) {
            int remaining = data.getRemainingTime();
            button = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
            ItemMeta meta = button.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6§lBreeding...");
                meta.setLore(Arrays.asList(
                    "§7Time Remaining: §e" + remaining + "s",
                    "§7",
                    "§cBreeding in progress!"
                ));
                button.setItemMeta(meta);
            }
        } else {
            button = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
            ItemMeta meta = button.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§a§lStart Breeding");
                meta.setLore(Arrays.asList(
                    "§7Click to start breeding",
                    "§7Requires valid recipe"
                ));
                button.setItemMeta(meta);
            }
        }
        
        inventory.setItem(BreederData.START_BUTTON_SLOT, button);
    }
    
    /**
     * Update the timer display in the center of the GUI
     */
    private void updateTimerDisplay() {
        ItemStack timerItem;
        
        if (data.isBreeding()) {
            BreederRecipe recipe = data.getActiveRecipe();
            if (recipe != null) {
                int remaining = data.getRemainingTime();
                int duration = data.getBreedingDuration();
                double progress = 1.0 - ((double) remaining / duration);
                
                timerItem = new ItemStack(Material.CLOCK);
                ItemMeta meta = timerItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§6§lBreeding Progress");
                    
                    // Create progress bar
                    String progressBar = createProgressBar(progress, 15);
                    
                    meta.setLore(Arrays.asList(
                        "§7Progress: §e" + String.format("%.1f%%", progress * 100),
                        "§7Time Left: §e" + remaining + "s",
                        "§7Total Time: §7" + duration + "s",
                        "",
                        "§6" + progressBar,
                        "",
                        "§7Output: §f" + recipe.getOutput().getType().name()
                    ));
                    timerItem.setItemMeta(meta);
                }
            } else {
                // Shouldn't happen, but safety
                timerItem = new ItemStack(Material.BARRIER);
                ItemMeta meta = timerItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§c§lError");
                    timerItem.setItemMeta(meta);
                }
            }
        } else {
            // Not breeding - show info
            timerItem = new ItemStack(Material.BOOK);
            ItemMeta meta = timerItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§7§lRecipe Info");
                meta.setLore(Arrays.asList(
                    "§7Place items in the slots",
                    "§7and click Start Breeding",
                    "",
                    "§7Requires:",
                    "§a• 2 Seeds",
                    "§6• 1 Catalyst",
                    "§b• 1 Fluid"
                ));
                timerItem.setItemMeta(meta);
            }
        }
        
        inventory.setItem(BreederData.TIMER_DISPLAY_SLOT, timerItem);
    }
    
    /**
     * Create a progress bar
     */
    private String createProgressBar(double progress, int length) {
        int filled = (int) (progress * length);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        return bar.toString();
    }
    
    /**
     * Refresh the GUI display (called from tick system)
     */
    public void refresh() {
        updateStartButton();
        updateTimerDisplay();
    }
    
    /**
     * Update all players viewing this breeder's GUI
     */
    public static void updateAllViewingGUIs(BreederData data) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            BreederData playerData = openGUIs.get(player.getUniqueId());
            if (playerData != null && playerData.getArmorStandId().equals(data.getArmorStandId())) {
                // Refresh the GUI
                Inventory inv = player.getOpenInventory().getTopInventory();
                if (inv != null && inv.getSize() == 27) {
                    // Update dynamic slots (button, timer)
                    BreederGUI gui = new BreederGUI(data);
                    inv.setItem(BreederData.START_BUTTON_SLOT, gui.inventory.getItem(BreederData.START_BUTTON_SLOT));
                    inv.setItem(BreederData.TIMER_DISPLAY_SLOT, gui.inventory.getItem(BreederData.TIMER_DISPLAY_SLOT));
                    
                    // Update input/output slots to reflect consumed items and outputs
                    inv.setItem(BreederData.SEED_SLOT_1, data.getItem(BreederData.SEED_SLOT_1));
                    inv.setItem(BreederData.SEED_SLOT_2, data.getItem(BreederData.SEED_SLOT_2));
                    inv.setItem(BreederData.CATALYST_SLOT, data.getItem(BreederData.CATALYST_SLOT));
                    inv.setItem(BreederData.FLUID_SLOT, data.getItem(BreederData.FLUID_SLOT));
                    inv.setItem(BreederData.OUTPUT_SLOT, data.getItem(BreederData.OUTPUT_SLOT));
                }
            }
        }
    }
    
    /**
     * Create a glass pane with a name
     */
    private ItemStack createGlassPane(Material material, String name) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            pane.setItemMeta(meta);
        }
        return pane;
    }
    
    /**
     * Open the GUI for a player
     */
    public void open(Player player) {
        UUID breederUUID = data.getArmorStandId();
        
        // Check if another player already has this breeder open
        UUID currentOwner = breederOwners.get(breederUUID);
        if (currentOwner != null && !currentOwner.equals(player.getUniqueId())) {
            // Another player has this breeder open
            Player owner = Bukkit.getPlayer(currentOwner);
            if (owner != null && owner.isOnline()) {
                player.sendMessage("§c§l[!] §cThis breeder is currently being used by §e" + owner.getName() + "§c!");
                DebugManager.getInstance().debug(DebugManager.DebugSystem.BREEDING,
                    "[BreederGUI] Blocked " + player.getName() + " - breeder in use by " + owner.getName());
                return;
            } else {
                // Owner is offline, release the lock
                DebugManager.getInstance().debug(DebugManager.DebugSystem.BREEDING,
                    "[BreederGUI] Previous owner offline, releasing lock");
                breederOwners.remove(breederUUID);
            }
        }
        
        // Check if player already has this GUI open (duplicate open prevention)
        BreederData existingData = openGUIs.get(player.getUniqueId());
        if (existingData != null && existingData.getArmorStandId().equals(breederUUID)) {
            DebugManager.getInstance().debug(DebugManager.DebugSystem.BREEDING,
                "[BreederGUI] GUI already open for " + player.getName() + " - ignoring duplicate open");
            return;
        }
        
        DebugManager.getInstance().debug(DebugManager.DebugSystem.BREEDING,
            "[BreederGUI] Opening GUI for " + player.getName() + ", adding to openGUIs map. Current size: " + openGUIs.size());
        
        // Claim ownership of this breeder
        breederOwners.put(breederUUID, player.getUniqueId());
        openGUIs.put(player.getUniqueId(), data);
        
        DebugManager.getInstance().debug(DebugManager.DebugSystem.BREEDING,
            "[BreederGUI] Added to openGUIs. New size: " + openGUIs.size() + ", claimed breeder ownership");
        
        player.openInventory(inventory);
        
        DebugManager.getInstance().debug(DebugManager.DebugSystem.BREEDING,
            "[BreederGUI] Inventory opened for " + player.getName());
    }
    
    /**
     * Get the open GUI for a player
     */
    public static BreederData getOpenGUI(Player player) {
        return openGUIs.get(player.getUniqueId());
    }
    
    /**
     * Internal listener class
     */
    private static class BreederGUIListener implements Listener {
        
        /**
         * Handle inventory clicks
         * Using MONITOR priority to run last and override any cancellations
         */
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
        public void onInventoryClick(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            
            // Check if this is the breeder inventory FIRST
            if (!event.getView().getTitle().equals("§6§lCrop Breeder")) return;
            
            // DEBUG: Confirm we reached this point
            DebugManager.getInstance().debug(DebugManager.DebugSystem.BREEDING,
                "[BreederGUI] Handler reached! Player: " + player.getName());
            
            // Find the GUI instance for this player
            BreederData playerData = openGUIs.get(player.getUniqueId());
            
            // DEBUG: Check if we found player data
            DebugManager.getInstance().debug(DebugManager.DebugSystem.BREEDING,
                "[BreederGUI] Player data found: " + (playerData != null) + ", openGUIs size: " + openGUIs.size());
            
            if (playerData == null) {
                event.setCancelled(true);
                DebugManager.getInstance().debug(DebugManager.DebugSystem.BREEDING,
                    "[BreederGUI] No player data found - cancelling event");
                return;
            }
            
            int slot = event.getRawSlot();
            boolean isBreeding = playerData.isBreeding();
            
            // DEBUG: Log what we're doing
            DebugManager.getInstance().debug(DebugManager.DebugSystem.BREEDING,
                "[BreederGUI] Click at slot " + slot + ", cancelled: " + event.isCancelled() +
                ", cursor: " + (event.getCursor() != null ? event.getCursor().getType() : "null") +
                ", current: " + (event.getCurrentItem() != null ? event.getCurrentItem().getType() : "null"));
            
            // If clicking in the breeder GUI (top inventory)
            if (slot >= 0 && slot < 27) {
                // Allow interactions with input/output slots (but not timer display)
                if ((slot == BreederData.SEED_SLOT_1 || slot == BreederData.SEED_SLOT_2 ||
                    slot == BreederData.CATALYST_SLOT || slot == BreederData.FLUID_SLOT ||
                    slot == BreederData.OUTPUT_SLOT) && slot != BreederData.TIMER_DISPLAY_SLOT) {
                    
                    // EXPLICITLY ALLOW THIS CLICK
                    event.setCancelled(false);
                    
                    // DEBUG: Confirm we un-cancelled it
                    DebugManager.getInstance().debug(DebugManager.DebugSystem.BREEDING,
                        "[BreederGUI] Slot " + slot + " is input/output, un-cancelled. Now: " + event.isCancelled());
                    
                    // For output slot: allow taking out but prevent placing in
                    if (slot == BreederData.OUTPUT_SLOT) {
                        ItemStack cursorItem = event.getCursor();
                        ItemStack currentItem = event.getCurrentItem();
                        
                        // If cursor has an item and slot is empty, trying to place - cancel
                        if (cursorItem != null && cursorItem.getType() != Material.AIR &&
                            (currentItem == null || currentItem.getType() == Material.AIR)) {
                            event.setCancelled(true);
                            return;
                        }
                        
                        // Otherwise allow (taking out or swapping with existing)
                        Bukkit.getScheduler().runTaskLater(BotanyManager.getPlugin(), () -> {
                            playerData.setItem(slot, event.getInventory().getItem(slot));
                        }, 1L);
                        return;
                    }
                    
                    // For input slots: allow all interactions and save after
                    Bukkit.getScheduler().runTaskLater(BotanyManager.getPlugin(), () -> {
                        playerData.setItem(slot, event.getInventory().getItem(slot));
                        
                        // If breeding, check if recipe is still valid
                        if (playerData.isBreeding()) {
                            ItemStack seed1 = playerData.getItem(BreederData.SEED_SLOT_1);
                            ItemStack seed2 = playerData.getItem(BreederData.SEED_SLOT_2);
                            ItemStack fluid = playerData.getItem(BreederData.FLUID_SLOT);
                            
                            // Check if recipe still matches (catalyst was already consumed, so skip that check)
                            BreederRecipe activeRecipe = playerData.getActiveRecipe();
                            if (activeRecipe != null) {
                                boolean seed1Valid = seed1 != null && BreederRecipe.matchesItem(activeRecipe.getSeed1(), seed1);
                                boolean seed2Valid = seed2 != null && BreederRecipe.matchesItem(activeRecipe.getSeed2(), seed2);
                                boolean fluidValid = fluid != null && fluid.getType() == activeRecipe.getFluidType();
                                
                                if (!seed1Valid || !seed2Valid || !fluidValid) {
                                    // Recipe no longer valid, cancel breeding
                                    playerData.cancelBreeding();
                                    player.sendMessage("§e§l[!] §eBreeding cancelled - missing required items!");
                                    if (player.getLocation() != null) {
                                        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
                                    }
                                    
                                    // Update all viewing GUIs
                                    BreederGUI.updateAllViewingGUIs(playerData);
                                }
                            }
                        }
                    }, 1L);
                    // DO NOT CANCEL - return without cancelling
                    return;
                }
                
                // Handle start button
                if (slot == BreederData.START_BUTTON_SLOT) {
                    event.setCancelled(true);
                    handleStartButton(player, playerData, event.getInventory());
                    return;
                }
                
                // Cancel all other clicks in the GUI (glass panes and decorative items)
                event.setCancelled(true);
                return;
            }
            
            // Clicking in player's inventory (slots 27+)
            // EXPLICITLY ALLOW these clicks
            event.setCancelled(false);
            
            // If shift-clicking, save input slots after the item moves
            if (event.isShiftClick() && slot >= 27) {
                // Delay saving to allow shift-click to complete
                Bukkit.getScheduler().runTaskLater(BotanyManager.getPlugin(), () -> {
                    playerData.setItem(BreederData.SEED_SLOT_1, event.getInventory().getItem(BreederData.SEED_SLOT_1));
                    playerData.setItem(BreederData.SEED_SLOT_2, event.getInventory().getItem(BreederData.SEED_SLOT_2));
                    playerData.setItem(BreederData.CATALYST_SLOT, event.getInventory().getItem(BreederData.CATALYST_SLOT));
                    playerData.setItem(BreederData.FLUID_SLOT, event.getInventory().getItem(BreederData.FLUID_SLOT));
                }, 1L);
            }
        }
        
        /**
         * Handle start button click
         */
        private void handleStartButton(Player player, BreederData data, Inventory inventory) {
            if (data.isBreeding()) {
                player.sendMessage("§c§l[!] §cBreeding is already in progress!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            
            // Get input items
            ItemStack seed1 = data.getItem(BreederData.SEED_SLOT_1);
            ItemStack seed2 = data.getItem(BreederData.SEED_SLOT_2);
            ItemStack catalyst = data.getItem(BreederData.CATALYST_SLOT);
            ItemStack fluid = data.getItem(BreederData.FLUID_SLOT);
            
            // Validate inputs
            if (seed1 == null || seed2 == null || catalyst == null || fluid == null) {
                player.sendMessage("§c§l[!] §cMissing required items!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            
            // Find matching recipe
            BreederRecipe recipe = BotanyManager.getInstance().findBreederRecipe(seed1, seed2, catalyst, fluid);
            if (recipe == null) {
                player.sendMessage("§c§l[!] §cNo valid breeding recipe found!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            
            // Start breeding
            if (data.startBreeding(recipe)) {
                player.sendMessage("§a§l[✓] §aBreeding started! §7(" + recipe.getDuration() + " seconds)");
                player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 1.0f);
                
                // Update inventory to reflect consumed catalyst
                inventory.setItem(BreederData.CATALYST_SLOT, data.getItem(BreederData.CATALYST_SLOT));
            }
        }
        
        /**
         * Handle inventory close
         */
        @EventHandler
        public void onInventoryClose(InventoryCloseEvent event) {
            if (!(event.getPlayer() instanceof Player)) return;
            Player player = (Player) event.getPlayer();
            
            if (!event.getView().getTitle().equals("§6§lCrop Breeder")) return;
            
            DebugManager.getInstance().debug(DebugManager.DebugSystem.BREEDING,
                "[BreederGUI] Closing inventory for " + player.getName() + ", openGUIs size before: " + openGUIs.size());
            
            BreederData data = openGUIs.get(player.getUniqueId());
            if (data == null) {
                DebugManager.getInstance().debug(DebugManager.DebugSystem.BREEDING,
                    "[BreederGUI] No data found for player on close!");
                return;
            }
            
            // Save inventory state
            data.setItem(BreederData.SEED_SLOT_1, event.getInventory().getItem(BreederData.SEED_SLOT_1));
            data.setItem(BreederData.SEED_SLOT_2, event.getInventory().getItem(BreederData.SEED_SLOT_2));
            data.setItem(BreederData.CATALYST_SLOT, event.getInventory().getItem(BreederData.CATALYST_SLOT));
            data.setItem(BreederData.FLUID_SLOT, event.getInventory().getItem(BreederData.FLUID_SLOT));
            data.setItem(BreederData.OUTPUT_SLOT, event.getInventory().getItem(BreederData.OUTPUT_SLOT));
            
            // Release breeder ownership
            UUID breederUUID = data.getArmorStandId();
            UUID currentOwner = breederOwners.get(breederUUID);
            if (currentOwner != null && currentOwner.equals(player.getUniqueId())) {
                breederOwners.remove(breederUUID);
                DebugManager.getInstance().debug(DebugManager.DebugSystem.BREEDING,
                    "[BreederGUI] Released breeder ownership for " + player.getName());
            }
            
            openGUIs.remove(player.getUniqueId());
            
            DebugManager.getInstance().debug(DebugManager.DebugSystem.BREEDING,
                "[BreederGUI] Removed player from openGUIs. New size: " + openGUIs.size());
        }
    }
}
