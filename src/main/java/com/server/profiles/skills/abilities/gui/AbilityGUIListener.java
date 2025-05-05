package com.server.profiles.skills.abilities.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.Main;
import com.server.profiles.skills.abilities.AbilityRegistry;
import com.server.profiles.skills.abilities.SkillAbility;
import com.server.profiles.skills.abilities.active.ActiveAbility;
import com.server.profiles.skills.abilities.passive.PassiveAbility;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillRegistry;

/**
 * Listener for ability GUI interactions
 */
public class AbilityGUIListener implements Listener {
    
    private final Main plugin;
    
    public AbilityGUIListener(Main plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Check if this is one of our ability GUIs
        if (title.startsWith(AbilitiesGUI.GUI_TITLE_PREFIX)) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null)
                return;
            
            handleAbilitiesMenuClick(player, event.getCurrentItem(), event.getClick());
        }
        else if (title.startsWith("All Passive Abilities: ") || 
                title.startsWith("All Active Abilities: ") ||
                title.startsWith("Unlocked Passive Abilities: ") || 
                title.startsWith("Unlocked Active Abilities: ")) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null)
                return;
            
            handleAbilityListClick(player, event.getCurrentItem(), title, event.getClick());
        }
        else if (title.equals("VeinMiner Config")) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null)
                return;
            
            handleVeinMinerConfigClick(player, event.getCurrentItem());
        }
    }
    
    /**
     * Handle clicks in the VeinMiner config GUI
     */
    private void handleVeinMinerConfigClick(Player player, ItemStack clickedItem) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }
        
        String displayName = clickedItem.getItemMeta().getDisplayName();
        
        // Handle save button
        if ((clickedItem.getType() == Material.EMERALD || clickedItem.getType() == Material.EMERALD_BLOCK) && 
            displayName.equals(ChatColor.GREEN + "Save & Close")) {
            
            // Get the currently selected value from the inventory
            int selectedValue = getSelectedValueFromInventory(player.getOpenInventory().getTopInventory());
            
            // Save the setting only when clicking Save button
            if (selectedValue > 0) {
                AbilityRegistry registry = AbilityRegistry.getInstance();
                com.server.profiles.skills.abilities.passive.mining.VeinMinerAbility veinMiner = 
                    (com.server.profiles.skills.abilities.passive.mining.VeinMinerAbility) 
                    registry.getAbility("vein_miner");
                
                if (veinMiner != null) {
                    // Save the setting
                    veinMiner.setUserMaxBlockSetting(player, selectedValue);
                }
            }
            
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
            player.sendMessage(ChatColor.GREEN + "VeinMiner settings saved!");
            return;
        }
        
        // Handle cancel button
        if ((clickedItem.getType() == Material.BARRIER || clickedItem.getType() == Material.REDSTONE_BLOCK) && 
            displayName.equals(ChatColor.RED + "Cancel")) {
            
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            player.sendMessage(ChatColor.YELLOW + "Configuration cancelled. No changes were made.");
            return;
        }
        
        // Handle setting buttons - check for all concrete colors used in the GUI
        if (clickedItem.getType() == Material.LIME_CONCRETE || 
            clickedItem.getType() == Material.LIGHT_GRAY_CONCRETE ||
            clickedItem.getType() == Material.CYAN_CONCRETE || 
            clickedItem.getType() == Material.BLUE_CONCRETE) {
            
            // Extract block count from lore
            String blockCountStr = extractValueFromLore(clickedItem, "VEINMINER_BLOCKS:");
            if (blockCountStr != null) {
                try {
                    int blockCount = Integer.parseInt(blockCountStr);
                    
                    // Get the ability for information only
                    AbilityRegistry registry = AbilityRegistry.getInstance();
                    com.server.profiles.skills.abilities.passive.mining.VeinMinerAbility veinMiner = 
                        (com.server.profiles.skills.abilities.passive.mining.VeinMinerAbility) 
                        registry.getAbility("vein_miner");
                    
                    if (veinMiner != null) {
                        // DON'T save the setting yet, just update the GUI to show selection
                        updateSelectionInGUI(player, veinMiner, blockCount);
                        
                        // Play a sound
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    }
                } catch (NumberFormatException e) {
                    // Invalid number format
                    if (Main.getInstance().isDebugMode()) {
                        Main.getInstance().getLogger().warning("Error parsing VeinMiner block count from GUI: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Get the currently selected value from the VeinMiner config inventory
     */
    private int getSelectedValueFromInventory(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() == Material.LIME_CONCRETE && item.hasItemMeta()) {
                String blockCountStr = extractValueFromLore(item, "VEINMINER_BLOCKS:");
                if (blockCountStr != null) {
                    try {
                        return Integer.parseInt(blockCountStr);
                    } catch (NumberFormatException e) {
                        // Invalid format
                    }
                }
            }
        }
        return -1; // No selection found
    }

    /**
     * Update the selection visually in the GUI without saving changes
     */
    private void updateSelectionInGUI(Player player, 
                                    com.server.profiles.skills.abilities.passive.mining.VeinMinerAbility veinMiner, 
                                    int newSelection) {
        // Enforce minimum of 2
        if (newSelection < 2) {
            newSelection = 2;
        }
        
        // Get the current inventory
        Inventory inv = player.getOpenInventory().getTopInventory();
        
        // Get max possible size for buttons
        int maxPossibleSize = veinMiner.getSkillBasedMaxSize(player);
        
        // Update the info item
        ItemStack infoItem = inv.getItem(4);
        if (infoItem != null) {
            ItemMeta meta = infoItem.getItemMeta();
            List<String> lore = meta.getLore();
            
            // Update the "Current setting" line
            for (int i = 0; i < lore.size(); i++) {
                if (lore.get(i).startsWith(ChatColor.YELLOW + "Current setting:")) {
                    lore.set(i, ChatColor.YELLOW + "Current setting: " + ChatColor.GREEN + newSelection + " blocks");
                    break;
                }
            }
            
            meta.setLore(lore);
            infoItem.setItemMeta(meta);
            inv.setItem(4, infoItem);
        }
        
        // Update the mana item
        ItemStack manaItem = inv.getItem(13);
        if (manaItem != null) {
            ItemMeta meta = manaItem.getItemMeta();
            List<String> lore = meta.getLore();
            
            // Update the mana cost line
            for (int i = 0; i < lore.size(); i++) {
                if (i > 0 && lore.get(i-1).contains("With your current setting:")) {
                    lore.set(i, ChatColor.AQUA + "" + (newSelection * 10) + " mana " + ChatColor.GRAY + "per activation");
                    break;
                }
            }
            
            meta.setLore(lore);
            manaItem.setItemMeta(meta);
            inv.setItem(13, manaItem);
        }
        
        // FIXED: Use the same slot layout as in openVeinMinerConfigGUI
        int[][] slotGrid = {
            {10, 11, 12, 13, 14, 15, 16}, // Row 1: slots 10-16
            {19, 20, 21, 22, 23, 24, 25}, // Row 2: slots 19-25
            {28, 29, 30, 31, 32, 33, 34}  // Row 3: slots 28-34
        };
        
        // Convert 2D grid to 1D array of slots
        int[] slots = new int[slotGrid.length * slotGrid[0].length];
        int slotIndex = 0;
        for (int[] row : slotGrid) {
            for (int slot : row) {
                if (slotIndex < slots.length) {
                    slots[slotIndex++] = slot;
                }
            }
        }
        
        // Update all option buttons
        for (int i = 2; i <= maxPossibleSize; i++) {
            int buttonIndex = i - 2;
            
            // Check if we have a slot for this button
            if (buttonIndex < slots.length) {
                ItemStack button = createVeinMinerSettingButton(i, i == newSelection);
                inv.setItem(slots[buttonIndex], button);
            }
        }
    }

    /**
     * Handle clicks in the abilities menu
     */
    private void handleAbilitiesMenuClick(Player player, ItemStack clickedItem, ClickType clickType) {
        if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) {
            return;
        }
        
        String title = player.getOpenInventory().getTitle();
        String displayName = clickedItem.getItemMeta().getDisplayName();
        
        // Handle passive abilities button
        if (clickedItem.getType() == Material.REDSTONE_TORCH && 
            displayName.equals(ChatColor.GOLD + "Passive Abilities")) {
            
            // Extract skill ID from lore
            String skillId = extractValueFromLore(clickedItem, "SKILL:");
            if (skillId == null) {
                // Try to extract from title if lore is not available
                String skillName = "";
                if (title != null && title.startsWith("Abilities: ")) {
                    skillName = title.substring("Abilities: ".length());
                    Skill skill = findSkillByName(skillName);
                    if (skill != null) {
                        skillId = skill.getId();
                    }
                }
            }
            
            if (skillId != null) {
                player.closeInventory();
                
                // Open the passive abilities menu based on click type
                boolean showAll = clickType.isRightClick();
                final String skillIdString = skillId;
                // Short delay to prevent inventory conflicts
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    AbilityListGUI.openAbilityList(player, skillIdString, "PASSIVE", showAll);
                }, 1L);
            }
            
            return;
        }
        
        // Handle active abilities button
        else if (clickedItem.getType() == Material.BLAZE_POWDER && 
                displayName.equals(ChatColor.GOLD + "Active Abilities")) {
            
            // Extract skill ID from lore
            String skillId = extractValueFromLore(clickedItem, "SKILL:");
            if (skillId == null) {
                // Try to extract from title if lore is not available
                String skillName = "";
                if (title != null && title.startsWith("Abilities: ")) {
                    skillName = title.substring("Abilities: ".length());
                    Skill skill = findSkillByName(skillName);
                    if (skill != null) {
                        skillId = skill.getId();
                    }
                }
            }
            
            if (skillId != null) {
                player.closeInventory();
                
                // Open the active abilities menu based on click type
                boolean showAll = clickType.isRightClick();
                final String skillIdString = skillId;
                
                // Short delay to prevent inventory conflicts
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    AbilityListGUI.openAbilityList(player, skillIdString, "ACTIVE", showAll);
                }, 1L);
            }
            
            return;
        }
        
        // Handle back button
        else if (clickedItem.getType() == Material.ARROW && 
                displayName.equals(ChatColor.RED + "Back to Skill Details")) {
            player.closeInventory();
            
            // Get skill ID from button lore
            String skillId = extractValueFromLore(clickedItem, "SKILL:");
            
            // If no direct ID, try from title
            if (skillId == null && title != null && title.startsWith("Abilities: ")) {
                String skillName = title.substring("Abilities: ".length());
                Skill skill = findSkillByName(skillName);
                if (skill != null) {
                    skillId = skill.getId();
                }
            }
            
            if (skillId != null) {
                final Skill skill = SkillRegistry.getInstance().getSkill(skillId);
                if (skill != null) {
                    // Short delay to prevent inventory conflicts
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        com.server.profiles.skills.gui.SkillDetailsGUI.openSkillDetailsMenu(player, skill);
                    }, 1L);
                    return;
                }
            }
            
            // Fallback if we couldn't determine the skill
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                com.server.profiles.skills.gui.SkillsGUI.openSkillsMenu(player);
            }, 1L);
        }
    }

    /**
     * Find a skill by its display name
     */
    private Skill findSkillByName(String name) {
        for (Skill skill : SkillRegistry.getInstance().getAllSkills()) {
            if (skill.getDisplayName().equalsIgnoreCase(name)) {
                return skill;
            }
        }
        return null;
    }
    
    /**
     * Handle clicks in the ability list GUI
     */
    private void handleAbilityListClick(Player player, ItemStack clickedItem, String title, ClickType clickType) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }
        
        // Check if this is an ability item
        String abilityId = extractValueFromLore(clickedItem, "ABILITY:");
        if (abilityId == null) {
            // Handle other types of clicks
            String toggleShowAll = extractValueFromLore(clickedItem, "TOGGLE_SHOW_ALL:");
            if (toggleShowAll != null) {
                String skillId = extractValueFromLore(clickedItem, "SKILL:");
                String abilityType = extractValueFromLore(clickedItem, "ABILITY_TYPE:");
                
                if (skillId != null && abilityType != null) {
                    boolean showAll = Boolean.parseBoolean(toggleShowAll);
                    AbilityListGUI.openAbilityList(player, skillId, abilityType, showAll);
                }
                return;
            }
            
            // Handle back button
            if (clickedItem.getType() == Material.ARROW && 
                clickedItem.getItemMeta().getDisplayName().equals(ChatColor.RED + "Back to Abilities")) {
                player.closeInventory();
                
                // Extract skill ID from lore
                String skillId = extractValueFromLore(clickedItem, "SKILL:");
                
                if (skillId != null) {
                    Skill skill = SkillRegistry.getInstance().getSkill(skillId);
                    if (skill != null) {
                        // Use scheduler to prevent glitches
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            AbilitiesGUI.openAbilitiesMenu(player, skill);
                        }, 1L);
                    } else {
                        // Fallback to skills menu if skill not found
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            com.server.profiles.skills.gui.SkillsGUI.openSkillsMenu(player);
                        }, 1L);
                    }
                }
                return;
            }
            return;
        }
        
        // Get the ability
        SkillAbility ability = AbilityRegistry.getInstance().getAbility(abilityId);
        if (ability == null) {
            return;
        }
        
        // Add this to the handleAbilityListClick method, within the passive ability section
        if (ability instanceof PassiveAbility) {
            PassiveAbility passiveAbility = (PassiveAbility) ability;
            
            // Check if the ability is unlocked
            if (passiveAbility.isUnlocked(player)) {
                // LEFT click to toggle on/off
                if (clickType == ClickType.LEFT) {
                    boolean newState = passiveAbility.toggleEnabled(player);
                    
                    // Play sound based on the new state
                    player.playSound(player.getLocation(), 
                        newState ? Sound.BLOCK_NOTE_BLOCK_PLING : Sound.BLOCK_NOTE_BLOCK_BASS, 
                        0.5f, newState ? 1.5f : 0.8f);
                    
                    // Update the GUI to show the new state
                    String abilityType = extractValueFromLore(clickedItem, "ABILITY_TYPE:");
                    String skillId = extractValueFromLore(clickedItem, "SKILL:");
                    if (skillId != null && abilityType != null) {
                        // Reopen the ability list with the updated state
                        boolean showAll = title.startsWith(AbilityListGUI.GUI_TITLE_PREFIX_ALL);
                        AbilityListGUI.openAbilityList(player, skillId, abilityType, showAll);
                    }
                } 
                // RIGHT click for special configuration - only for certain abilities
                else if (clickType == ClickType.RIGHT) {
                    // Check if this is the VeinMiner ability
                    if (abilityId.equals("vein_miner")) {
                        // Open the VeinMiner configuration GUI
                        openVeinMinerConfigGUI(player, passiveAbility);
                        return;
                    } else {
                        // For other abilities, just display information
                        player.sendMessage(ChatColor.AQUA + "===== " + ChatColor.GOLD + passiveAbility.getDisplayName() + ChatColor.AQUA + " =====");
                        player.sendMessage(ChatColor.GREEN + "LEFT-CLICK to toggle this ability on/off");
                        player.sendMessage(ChatColor.YELLOW + "Current status: " + 
                            (passiveAbility.isEnabled(player) ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
                    }
                }
            } else {
                // Tell the player how to unlock this ability
                player.sendMessage(ChatColor.RED + "You haven't unlocked this ability yet!");
                player.sendMessage(ChatColor.YELLOW + "To unlock: " + ChatColor.WHITE + passiveAbility.getUnlockRequirement());
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            }
        } else if (ability instanceof ActiveAbility) {
            // Handle active abilities info display
            ActiveAbility activeAbility = (ActiveAbility) ability;
            player.sendMessage(ChatColor.AQUA + "===== " + ChatColor.GOLD + activeAbility.getDisplayName() + ChatColor.AQUA + " =====");
            player.sendMessage(ChatColor.GREEN + "This is an active ability!");
            player.sendMessage(ChatColor.YELLOW + "Activation: " + ChatColor.WHITE + activeAbility.getActivationMethod());
            player.sendMessage(ChatColor.YELLOW + "Cooldown: " + ChatColor.WHITE + activeAbility.getCooldownSeconds() + " seconds");
            
            // Show cooldown if on cooldown
            if (activeAbility.isOnCooldown(player)) {
                long remaining = activeAbility.getCooldownRemaining(player) / 1000; // Convert to seconds
                player.sendMessage(ChatColor.RED + "On cooldown: " + remaining + " seconds remaining");
            }
            
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
        }
    }
    
    /**
     * Extract a skill name from a GUI title
     */
    private String extractSkillNameFromTitle(String title) {
        if (title.startsWith(AbilityListGUI.GUI_TITLE_PREFIX_UNLOCKED)) {
            // Format: "Unlocked [Type] Abilities: [Skill]"
            int colonIndex = title.indexOf(": ");
            if (colonIndex >= 0 && colonIndex + 2 < title.length()) {
                return title.substring(colonIndex + 2);
            }
        }
        else if (title.startsWith(AbilityListGUI.GUI_TITLE_PREFIX_ALL)) {
            // Format: "All [Type] Abilities: [Skill]"
            int colonIndex = title.indexOf(": ");
            if (colonIndex >= 0 && colonIndex + 2 < title.length()) {
                return title.substring(colonIndex + 2);
            }
        }
        return null;
    }
    
    /**
     * Extract a value from an item's lore with a specific prefix
     */
    private String extractValueFromLore(ItemStack item, String prefix) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return null;
        }
        
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) return null;
        
        for (String line : lore) {
            if (line.contains(prefix)) {
                int startIndex = line.indexOf(prefix) + prefix.length();
                if (startIndex < line.length()) {
                    return line.substring(startIndex);
                }
            }
        }
        
        return null;
    }

    /**
     * Open a configuration GUI for the VeinMiner ability
     */
    private void openVeinMinerConfigGUI(Player player, PassiveAbility ability) {
        if (!(ability instanceof com.server.profiles.skills.abilities.passive.mining.VeinMinerAbility)) {
            return;
        }
        
        com.server.profiles.skills.abilities.passive.mining.VeinMinerAbility veinMiner = 
            (com.server.profiles.skills.abilities.passive.mining.VeinMinerAbility) ability;
        
        // Get the max potential vein size based on the player's skill level
        int maxPossibleSize = veinMiner.getSkillBasedMaxSize(player);
        
        // Get the current user setting (stored in metadata)
        int currentSetting = veinMiner.getUserMaxBlockSetting(player);
        if (currentSetting <= 0) {
            currentSetting = maxPossibleSize; // Default to max if not set
        } else if (currentSetting < 2) {
            currentSetting = 2; // Enforce minimum of 2
        }
        
        // Create the inventory - increased to 5 rows for more space
        String title = "VeinMiner Config";
        int rows = 5;
        Inventory inv = Bukkit.createInventory(null, rows * 9, title);
        
        // Fill background with glass panes for a cleaner look
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        
        // Fill all slots with the filler
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
        
        // Add decorative border
        ItemStack borderPane = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderPane.getItemMeta();
        borderMeta.setDisplayName(" ");
        borderPane.setItemMeta(borderMeta);
        
        // Top and bottom rows border
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, borderPane); // Top row
            inv.setItem(36 + i, borderPane); // Bottom row
        }
        
        // Side borders (excluding the corners which are already set)
        for (int row = 1; row < 4; row++) {
            inv.setItem(row * 9, borderPane); // Left side
            inv.setItem(row * 9 + 8, borderPane); // Right side
        }
        
        // Info item - now with a more detailed and visually appealing design
        ItemStack infoItem = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(ChatColor.GOLD + "⚒ VeinMiner Configuration ⚒");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add(ChatColor.GRAY + "Configure how many blocks the");
        infoLore.add(ChatColor.GRAY + "VeinMiner ability will mine at once.");
        infoLore.add("");
        infoLore.add(ChatColor.YELLOW + "Maximum allowed: " + ChatColor.WHITE + maxPossibleSize + " blocks");
        infoLore.add(ChatColor.YELLOW + "Current setting: " + ChatColor.GREEN + currentSetting + " blocks");
        infoLore.add("");
        infoLore.add(ChatColor.GRAY + "Select a number below to change the");
        infoLore.add(ChatColor.GRAY + "maximum blocks VeinMiner will break.");
        infoLore.add("");
        infoLore.add(ChatColor.RED + "Minimum setting: 2 blocks");
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        inv.setItem(4, infoItem);
        
        // Add mana cost explanation
        ItemStack manaItem = new ItemStack(Material.LAPIS_LAZULI);
        ItemMeta manaMeta = manaItem.getItemMeta();
        manaMeta.setDisplayName(ChatColor.AQUA + "Mana Usage");
        List<String> manaLore = new ArrayList<>();
        manaLore.add("");
        manaLore.add(ChatColor.GRAY + "VeinMiner consumes mana based on");
        manaLore.add(ChatColor.GRAY + "how many blocks you configure it to mine.");
        manaLore.add("");
        manaLore.add(ChatColor.AQUA + "Mana Cost Formula:");
        manaLore.add(ChatColor.YELLOW + "10 mana × number of blocks");
        manaLore.add("");
        manaLore.add(ChatColor.GRAY + "With your current setting:");
        manaLore.add(ChatColor.AQUA + "" + (currentSetting * 10) + " mana " + ChatColor.GRAY + "per activation");
        manaMeta.setLore(manaLore);
        manaItem.setItemMeta(manaMeta);
        inv.setItem(13, manaItem);
        
        // FIXED: Use a manually created list of slots that ensures all options display correctly
        // Each row has 7 usable slots (indices 1-7 of each row) since the borders take up slots 0 and 8
        int[][] slotGrid = {
            {10, 11, 12, 13, 14, 15, 16}, // Row 1: slots 10-16
            {19, 20, 21, 22, 23, 24, 25}, // Row 2: slots 19-25
            {28, 29, 30, 31, 32, 33, 34}  // Row 3: slots 28-34
        };
        
        // Convert 2D grid to 1D array of slots
        int[] slots = new int[slotGrid.length * slotGrid[0].length];
        int slotIndex = 0;
        for (int[] row : slotGrid) {
            for (int slot : row) {
                if (slotIndex < slots.length) {
                    slots[slotIndex++] = slot;
                }
            }
        }
        
        // Place buttons in the predefined slots
        for (int i = 2; i <= maxPossibleSize; i++) {
            int buttonIndex = i - 2;
            
            // Check if we have a slot for this button
            if (buttonIndex < slots.length) {
                ItemStack button = createVeinMinerSettingButton(i, i == currentSetting);
                inv.setItem(slots[buttonIndex], button);
            }
        }
        
        // Save button - bottom right corner
        ItemStack saveButton = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta saveMeta = saveButton.getItemMeta();
        saveMeta.setDisplayName(ChatColor.GREEN + "Save & Close");
        List<String> saveLore = new ArrayList<>();
        saveLore.add("");
        saveLore.add(ChatColor.GRAY + "Click to save your selection");
        saveLore.add(ChatColor.GRAY + "and close this menu.");
        saveMeta.setLore(saveLore);
        saveButton.setItemMeta(saveMeta);
        inv.setItem(44, saveButton);
        
        // Cancel button - bottom left corner
        ItemStack cancelButton = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancel");
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add("");
        cancelLore.add(ChatColor.GRAY + "Click to exit without saving");
        cancelLore.add(ChatColor.GRAY + "any changes.");
        cancelMeta.setLore(cancelLore);
        cancelButton.setItemMeta(cancelMeta);
        inv.setItem(36, cancelButton);
        
        // Add debug button that shows info about the menu
        if (Main.getInstance().isDebugMode()) {
            ItemStack debugItem = new ItemStack(Material.PAPER);
            ItemMeta debugMeta = debugItem.getItemMeta();
            debugMeta.setDisplayName(ChatColor.DARK_RED + "Debug Info");
            List<String> debugLore = new ArrayList<>();
            debugLore.add(ChatColor.GRAY + "Max Possible Size: " + maxPossibleSize);
            debugLore.add(ChatColor.GRAY + "Current Setting: " + currentSetting);
            debugLore.add(ChatColor.GRAY + "Total Slots: " + slots.length);
            debugMeta.setLore(debugLore);
            debugItem.setItemMeta(debugMeta);
            inv.setItem(8, debugItem);
        }
        
        // Open the inventory
        player.openInventory(inv);
    }

    /**
     * Create a button for selecting a VeinMiner setting
     */
    private ItemStack createVeinMinerSettingButton(int blockCount, boolean isSelected) {
        // Use different materials for selected vs unselected for better visual distinction
        Material material = isSelected ? Material.LIME_CONCRETE : Material.LIGHT_GRAY_CONCRETE;
        
        // For higher numbers, use a different color to indicate higher power/cost
        if (blockCount > 7 && !isSelected) {
            material = Material.CYAN_CONCRETE;
        } else if (blockCount > 12 && !isSelected) {
            material = Material.BLUE_CONCRETE;
        }
        
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        
        // Improve the display name with formatting
        meta.setDisplayName((isSelected ? ChatColor.GREEN + "» " : "") + 
                        (isSelected ? ChatColor.BOLD : "") + 
                        (isSelected ? ChatColor.GREEN : ChatColor.YELLOW) + 
                        blockCount + " Blocks" + 
                        (isSelected ? " «" : ""));
        
        List<String> lore = new ArrayList<>();
        
        // Add icon to make it more visually appealing
        lore.add(ChatColor.GRAY + "VeinMiner will mine up to");
        lore.add(ChatColor.WHITE + "" + blockCount + ChatColor.GRAY + " blocks at once");
        lore.add("");
        
        // Highlight mana cost differently based on the amount
        int manaCost = blockCount * 10;
        ChatColor manaColor = ChatColor.AQUA;
        if (manaCost > 70) manaColor = ChatColor.BLUE;
        if (manaCost > 120) manaColor = ChatColor.DARK_BLUE;
        
        lore.add(ChatColor.GRAY + "Mana Cost: " + manaColor + manaCost);
        
        // Add a clearer selected indicator
        if (isSelected) {
            lore.add("");
            lore.add(ChatColor.GREEN + "✓ CURRENTLY SELECTED");
        }
        
        // Add hidden data for event handling
        lore.add(ChatColor.BLACK + "VEINMINER_BLOCKS:" + blockCount);
        
        meta.setLore(lore);
        button.setItemMeta(meta);
        return button;
    }

}