package com.server.cosmetics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.items.ItemType;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;

public class CosmeticGUI implements Listener {
    private static final String GUI_TITLE = "Cosmetic Equipment";
    private static final Map<Integer, ItemType> SLOT_TYPES = new HashMap<>();
    
    static {
        SLOT_TYPES.put(10, ItemType.COSMETIC_HELMET);
        SLOT_TYPES.put(19, ItemType.COSMETIC_CHESTPLATE);
        SLOT_TYPES.put(28, ItemType.COSMETIC_LEGGINGS);
        SLOT_TYPES.put(37, ItemType.COSMETIC_BOOTS);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true); // Cancel all clicks by default
        
        Player player = (Player) event.getWhoClicked();
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;

        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;

        Inventory clickedInv = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();
        int clickedSlot = event.getSlot();

        // Player clicked in their own inventory
        if (clickedInv != null && clickedInv.equals(player.getInventory())) {
            if (clickedItem != null && isValidCosmeticItem(clickedItem)) {
                handleCosmeticEquip(player, profile, clickedItem, event.getView().getTopInventory());
            }
            return;
        }

        // Player clicked in cosmetic GUI
        if (clickedInv != null && clickedInv.equals(event.getView().getTopInventory())) {
            if (!isSlotIndicator(clickedItem)) {
                handleCosmeticUnequip(player, profile, clickedItem, clickedSlot, clickedInv);
            }
        }
    }

    private void handleCosmeticEquip(Player player, PlayerProfile profile, ItemStack cosmeticItem, Inventory cosmeticGui) {
        ItemType cosmeticType = ItemType.getTypeFromModelData(cosmeticItem.getItemMeta().getCustomModelData());
        int targetSlot = getSlotForType(cosmeticType);
        
        if (targetSlot == -1) return;

        ItemStack existingItem = cosmeticGui.getItem(targetSlot);
        if (existingItem != null && !isSlotIndicator(existingItem)) {
            player.sendMessage("§cPlease remove the existing cosmetic from that slot first!");
            return;
        }

        // Remove item from player's inventory
        cosmeticItem = cosmeticItem.clone();
        player.getInventory().removeItem(cosmeticItem);

        // Place in cosmetic GUI
        cosmeticGui.setItem(targetSlot, cosmeticItem);
        profile.setCosmetic(cosmeticType, cosmeticItem);
        
        // Update the cosmetic display
        CosmeticManager.getInstance().removeCosmetics(player); // Clear existing cosmetics
        CosmeticManager.getInstance().updateCosmeticDisplay(player); // Apply new cosmetics
    }

    private void handleCosmeticUnequip(Player player, PlayerProfile profile, ItemStack cosmeticItem, int slot, Inventory inventory) {
        ItemType slotType = SLOT_TYPES.get(slot);
        if (slotType == null) return;

        // Check if player has inventory space
        if (hasInventorySpace(player)) {
            // Give item to player
            player.getInventory().addItem(cosmeticItem.clone());
            
            // Remove from GUI and profile
            profile.removeCosmetic(slotType);
            inventory.setItem(slot, createSlotIndicator(slotType));
            
            // Update the cosmetic display
            CosmeticManager.getInstance().removeCosmetics(player); // Clear existing cosmetics
            CosmeticManager.getInstance().updateCosmeticDisplay(player); // Apply new cosmetics
        } else {
            player.sendMessage("§cYour inventory is full!");
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        
        Player player = (Player) event.getPlayer();
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return;

        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return;

        // Save cosmetics state
        Inventory inv = event.getInventory();
        for (Map.Entry<Integer, ItemType> entry : SLOT_TYPES.entrySet()) {
            ItemStack item = inv.getItem(entry.getKey());
            if (item != null && !isSlotIndicator(item)) {
                profile.setCosmetic(entry.getValue(), item.clone());
            } else {
                profile.removeCosmetic(entry.getValue());
            }
        }

        // Update the cosmetic display
        CosmeticManager.getInstance().removeCosmetics(player); // Clear existing cosmetics
        CosmeticManager.getInstance().updateCosmeticDisplay(player); // Apply new cosmetics
    }

    private boolean hasInventorySpace(Player player) {
        return player.getInventory().firstEmpty() != -1;
    }

    private boolean isValidCosmeticItem(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) return false;
        int modelData = item.getItemMeta().getCustomModelData();
        return ItemType.isCosmetic(modelData);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(GUI_TITLE)) {
            event.setCancelled(true);
        }
    }

    private int getSlotForType(ItemType type) {
        for (Map.Entry<Integer, ItemType> entry : SLOT_TYPES.entrySet()) {
            if (entry.getValue() == type) {
                return entry.getKey();
            }
        }
        return -1;
    }
    private static boolean isSlotIndicator(ItemStack item) {
        return item != null && 
               item.getType() == Material.LIGHT_GRAY_STAINED_GLASS_PANE && 
               item.hasItemMeta() && 
               item.getItemMeta().getDisplayName().contains("Slot");
    }

    private static ItemStack createSlotIndicator(ItemType type) {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        String name = type.toString().substring(9).toLowerCase();
        meta.setDisplayName("§6§l" + name.substring(0, 1).toUpperCase() + name.substring(1) + " Slot");
        meta.setLore(Arrays.asList(
            "§7Place cosmetic item here",
            "§e§lShift-Click §7to remove cosmetic"
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static void openCosmeticMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Get player's cosmetics from active profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot != null) {
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile != null) {
                Map<ItemType, ItemStack> cosmetics = profile.getCosmetics();
                
                // Add equipped cosmetics to respective slots
                for (Map.Entry<Integer, ItemType> entry : SLOT_TYPES.entrySet()) {
                    ItemStack cosmetic = cosmetics.get(entry.getValue());
                    if (cosmetic != null) {
                        gui.setItem(entry.getKey(), cosmetic);
                    } else {
                        createSlotIndicator(gui, entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        // Fill empty slots with black glass panes
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
    }

    private static void createSlotIndicator(Inventory gui, int slot, ItemType type) {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        String name = type.toString().substring(9).toLowerCase(); // Remove "COSMETIC_" prefix
        meta.setDisplayName("§6§l" + name.substring(0, 1).toUpperCase() + name.substring(1) + " Slot");
        meta.setLore(Arrays.asList("§7Place cosmetic item here"));
        item.setItemMeta(meta);
        gui.setItem(slot, item);
    }

    private static boolean isValidCosmeticForSlot(ItemStack item, ItemType slotType) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) return false;
        
        int modelData = item.getItemMeta().getCustomModelData();
        return ItemType.isCosmetic(modelData) && ItemType.getTypeFromModelData(modelData) == slotType;
    }
}