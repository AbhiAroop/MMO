package com.server.profiles.skills.abilities.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.abilities.AbilityRegistry;
import com.server.profiles.skills.abilities.SkillAbility;
import com.server.profiles.skills.abilities.active.ActiveAbility;
import com.server.profiles.skills.abilities.passive.PassiveAbility;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.trees.PlayerSkillTreeData;

/**
 * Listener for ability GUI interactions
 */
public class AbilityGUIListener implements Listener {
    
    private final Main plugin;
    private final Map<UUID, Double> tempOreConduitPercentage = new HashMap<>();
    
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
        else if (title.equals("OreConduit Config")) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null)
                return;
            
            handleOreConduitConfigClick(player, event.getCurrentItem());
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
                    }  // Check if this is the OreConduit ability
                    else if (abilityId.equals("ore_conduit")) {
                        // Open the OreConduit configuration GUI
                        openOreConduitConfigGUI(player, passiveAbility);
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

    /**
     * Open a configuration GUI for the OreConduit ability
     */
    private void openOreConduitConfigGUI(Player player, PassiveAbility ability) {
        if (!(ability instanceof com.server.profiles.skills.abilities.passive.mining.OreConduitAbility)) {
            return;
        }
        
        com.server.profiles.skills.abilities.passive.mining.OreConduitAbility oreConduit = 
            (com.server.profiles.skills.abilities.passive.mining.OreConduitAbility) ability;
        
        // Get the player's skill node level
        int nodeLevel = getOreConduitNodeLevel(player);
        
        // Get the current user setting (if custom) and maximum allowed percentage based on node level
        double currentPercentage = oreConduit.getUserSplitPercentage(player);
        double defaultPercentage = nodeLevel * 0.5; // 0.5% per level
        double maxPercentage = Math.min(50.0, defaultPercentage); // Cap at 50%
        
        if (currentPercentage < 0) {
            currentPercentage = defaultPercentage; // If no custom setting, use default
        } else {
            // Ensure percentage doesn't exceed max allowed
            currentPercentage = Math.min(currentPercentage, maxPercentage);
        }
        
        // Format percentages for display
        String currentPercentageStr = String.format("%.1f", currentPercentage);
        String maxPercentageStr = String.format("%.1f", maxPercentage);
        String defaultPercentageStr = String.format("%.1f", defaultPercentage);
        
        // Create the inventory with a cleaner 3x9 layout (27 slots)
        String title = "OreConduit Config";
        int rows = 3;
        Inventory inv = Bukkit.createInventory(null, rows * 9, title);
        
        // Fill background with glass panes
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
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
        
        // Top and bottom row borders
        for (int i = 0; i < 9; i++) {
            if (i != 4) { // Skip center of top row for main display
                inv.setItem(i, borderPane);
            }
            if (i != 0 && i != 8) { // Skip bottom corners for save/cancel buttons
                inv.setItem(18 + i, borderPane);
            }
        }
        
        // Info item - This is the central display now
        ItemStack infoItem = new ItemStack(Material.CONDUIT);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(ChatColor.AQUA + "✧ OreConduit Setting: " + ChatColor.GREEN + currentPercentageStr + "%" + ChatColor.AQUA + " ✧");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add(ChatColor.YELLOW + "Node Level: " + ChatColor.WHITE + nodeLevel);
        infoLore.add(ChatColor.YELLOW + "Default: " + ChatColor.WHITE + defaultPercentageStr + "%");
        infoLore.add(ChatColor.YELLOW + "Maximum: " + ChatColor.WHITE + maxPercentageStr + "%");
        infoLore.add("");
        
        if (currentPercentage == 0.0) {
            infoLore.add(ChatColor.GRAY + "No XP will be split to Mining");
            infoLore.add(ChatColor.GRAY + "All XP stays with OreExtraction");
        } else {
            infoLore.add(ChatColor.GRAY + "Split " + ChatColor.AQUA + currentPercentageStr + "%" + 
                    ChatColor.GRAY + " of OreExtraction XP");
            infoLore.add(ChatColor.GRAY + "to your Mining skill");
        }
        
        infoLore.add("");
        infoLore.add(ChatColor.GRAY + "Use the buttons to adjust the percentage");
        infoLore.add(ChatColor.BLACK + "CONDUIT_PERCENT:" + currentPercentage);
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        inv.setItem(4, infoItem);

        // Increment buttons - cleaner layout on left side
        addControlButton(inv, 12, Material.LIME_CONCRETE, "+1.0%", 1.0, currentPercentage, maxPercentage);
        addControlButton(inv, 3, Material.LIME_TERRACOTTA, "+0.1%", 0.1, currentPercentage, maxPercentage);
        
        // Decrement buttons - cleaner layout on right side
        addControlButton(inv, 14, Material.RED_CONCRETE, "-1.0%", -1.0, currentPercentage, maxPercentage);
        addControlButton(inv, 5, Material.RED_TERRACOTTA, "-0.1%", -0.1, currentPercentage, maxPercentage);
        
        // Common presets in a clean row
        addPresetButton(inv, 10, 0.0, "None", currentPercentage, maxPercentage);
        addPresetButton(inv, 11, 10.0, "Low", currentPercentage, maxPercentage);
        addPresetButton(inv, 15, 25.0, "Medium", currentPercentage, maxPercentage);
        addPresetButton(inv, 16, 40.0, "High", currentPercentage, maxPercentage);
        
        // Default value button - centered below main display
        ItemStack defaultButton = new ItemStack(Material.GOLD_INGOT);
        ItemMeta defaultMeta = defaultButton.getItemMeta();
        defaultMeta.setDisplayName(ChatColor.GOLD + "Use Default Value (" + defaultPercentageStr + "%)");
        
        List<String> defaultLore = new ArrayList<>();
        defaultLore.add("");
        defaultLore.add(ChatColor.GRAY + "Reset to the default percentage");
        defaultLore.add(ChatColor.GRAY + "for your current node level");
        defaultLore.add("");
        defaultLore.add(ChatColor.BLACK + "CONDUIT_PERCENT:" + defaultPercentage);
        defaultMeta.setLore(defaultLore);
        defaultButton.setItemMeta(defaultMeta);
        inv.setItem(13, defaultButton);
        
        // Save button - bottom left corner
        ItemStack saveButton = new ItemStack(Material.EMERALD);
        ItemMeta saveMeta = saveButton.getItemMeta();
        saveMeta.setDisplayName(ChatColor.GREEN + "Save & Close");
        List<String> saveLore = new ArrayList<>();
        saveLore.add("");
        saveLore.add(ChatColor.GRAY + "Click to save your setting");
        saveMeta.setLore(saveLore);
        saveButton.setItemMeta(saveMeta);
        inv.setItem(18, saveButton);
        
        // Cancel button - bottom right corner
        ItemStack cancelButton = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancel");
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add("");
        cancelLore.add(ChatColor.GRAY + "Exit without saving changes");
        cancelMeta.setLore(cancelLore);
        cancelButton.setItemMeta(cancelMeta);
        inv.setItem(26, cancelButton);
        
        // Open the inventory
        player.openInventory(inv);
    }

    /**
     * Create a unified control button (increment/decrement) for the OreConduit GUI
     */
    private void addControlButton(Inventory inv, int slot, Material material, String label, double adjustment, 
                                double currentPercentage, double maxPercentage) {
        boolean isPositive = adjustment > 0;
        ChatColor valueColor = isPositive ? ChatColor.GREEN : ChatColor.RED;
        double newValue = currentPercentage + adjustment;
        boolean isDisabled = (isPositive && newValue > maxPercentage) || (!isPositive && newValue < 0);
        
        if (isDisabled) {
            material = Material.BARRIER;
        }
        
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(valueColor + label);
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        
        if (isDisabled) {
            lore.add(ChatColor.RED + (isPositive ? 
                    "Cannot exceed " + String.format("%.1f", maxPercentage) + "%" : 
                    "Cannot go below 0.0%"));
        } else {
            String action = isPositive ? "Add" : "Subtract";
            String amountStr = String.format("%.1f", Math.abs(adjustment));
            
            lore.add(ChatColor.GRAY + action + " " + valueColor + amountStr + "%" + 
                    ChatColor.GRAY + " to the current value");
            lore.add("");
            lore.add(ChatColor.GRAY + "New value: " + ChatColor.YELLOW + 
                    String.format("%.1f", Math.max(0, Math.min(maxPercentage, newValue))) + "%");
        }
        
        // Add hidden data for event handling
        lore.add(ChatColor.BLACK + "CONDUIT_ADJUST:" + adjustment);
        
        meta.setLore(lore);
        button.setItemMeta(meta);
        inv.setItem(slot, button);
    }

    /**
     * Helper method to add increment buttons to the OreConduit GUI
     */
    private void addIncrementButton(Inventory inv, int slot, double currentPercentage, double maxPercentage, double amount) {
        Material material;
        String amountStr;
        String size;
        
        if (amount >= 10.0) {
            material = Material.LIME_WOOL;
            size = "Large";
        } else if (amount >= 1.0) {
            material = Material.LIME_CONCRETE;
            size = "Medium";
        } else if (amount >= 0.5) {
            material = Material.LIME_TERRACOTTA;
            size = "Small";
        } else {
            material = Material.GREEN_STAINED_GLASS;
            size = "Tiny";
        }
        
        amountStr = String.format("%.1f", amount);
        
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "+" + amountStr + "%");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + size + " Increase");
        lore.add("");
        
        // Indicate if adding this amount would exceed the maximum
        boolean wouldExceedMax = (currentPercentage + amount) > maxPercentage;
        if (wouldExceedMax) {
            lore.add(ChatColor.RED + "Cannot exceed maximum of " + String.format("%.1f", maxPercentage) + "%");
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        } else {
            lore.add(ChatColor.GRAY + "Click to add " + ChatColor.GREEN + amountStr + "%" + 
                    ChatColor.GRAY + " to the current setting");
            lore.add(ChatColor.GRAY + "New value would be: " + 
                    ChatColor.GREEN + String.format("%.1f", Math.min(maxPercentage, currentPercentage + amount)) + "%");
        }
        
        // Add hidden data for event handling
        lore.add("");
        lore.add(ChatColor.BLACK + "CONDUIT_ADJUST:" + amount);
        
        meta.setLore(lore);
        button.setItemMeta(meta);
        inv.setItem(slot, button);
    }

    /**
     * Helper method to add decrement buttons to the OreConduit GUI
     */
    private void addDecrementButton(Inventory inv, int slot, double currentPercentage, double amount) {
        Material material;
        String amountStr;
        String size;
        
        if (amount <= -10.0) {
            material = Material.RED_WOOL;
            size = "Large";
        } else if (amount <= -1.0) {
            material = Material.RED_CONCRETE;
            size = "Medium";
        } else if (amount <= -0.5) {
            material = Material.RED_TERRACOTTA;
            size = "Small";
        } else {
            material = Material.RED_STAINED_GLASS;
            size = "Tiny";
        }
        
        amountStr = String.format("%.1f", Math.abs(amount)); // Remove negative sign for display
        
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "-" + amountStr + "%");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + size + " Decrease");
        lore.add("");
        
        // Indicate if removing this amount would go below 0
        boolean wouldGoBelowZero = (currentPercentage + amount) < 0;
        if (wouldGoBelowZero) {
            lore.add(ChatColor.RED + "Cannot go below 0.0%");
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        } else {
            lore.add(ChatColor.GRAY + "Click to subtract " + ChatColor.RED + amountStr + "%" + 
                    ChatColor.GRAY + " from the current setting");
            lore.add(ChatColor.GRAY + "New value would be: " + 
                    ChatColor.GREEN + String.format("%.1f", Math.max(0, currentPercentage + amount)) + "%");
        }
        
        // Add hidden data for event handling
        lore.add("");
        lore.add(ChatColor.BLACK + "CONDUIT_ADJUST:" + amount);
        
        meta.setLore(lore);
        button.setItemMeta(meta);
        inv.setItem(slot, button);
    }

    /**
     * Get the node level for OreConduit
     */
    private int getOreConduitNodeLevel(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return 0;
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return 0;
        
        PlayerSkillTreeData treeData = profile.getSkillTreeData();
        return treeData.getNodeLevel("ore_extraction", "ore_conduit");
    }

    /**
     * Handle clicks in the OreConduit config GUI
     */
    private void handleOreConduitConfigClick(Player player, ItemStack clickedItem) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }
        
        String displayName = clickedItem.getItemMeta().getDisplayName();
        
        // Handle save button - now using EMERALD
        if (clickedItem.getType() == Material.EMERALD && 
            displayName.contains("Save & Close")) {
            
            // Get the currently selected percentage from the inventory
            double selectedPercentage = getSelectedPercentageFromInventory(player.getOpenInventory().getTopInventory());
            
            // Save the setting only when clicking Save button
            if (selectedPercentage >= 0) {
                AbilityRegistry registry = AbilityRegistry.getInstance();
                com.server.profiles.skills.abilities.passive.mining.OreConduitAbility oreConduit = 
                    (com.server.profiles.skills.abilities.passive.mining.OreConduitAbility) 
                    registry.getAbility("ore_conduit");
                
                if (oreConduit != null) {
                    // Save the setting
                    oreConduit.setUserSplitPercentage(player, selectedPercentage);
                }
            }
            
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
            player.sendMessage(ChatColor.GREEN + "OreConduit settings saved!");
            return;
        }
        
        // Handle cancel button - now using BARRIER
        if (clickedItem.getType() == Material.BARRIER && 
            displayName.equals(ChatColor.RED + "Cancel")) {
            
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            player.sendMessage(ChatColor.YELLOW + "Configuration cancelled. No changes were made.");
            return;
        }
        
        // Handle preset buttons
        String percentStr = extractValueFromLore(clickedItem, "CONDUIT_PERCENT:");
        if (percentStr != null) {
            try {
                double percentage = Double.parseDouble(percentStr);
                
                // Get max percentage allowed for this player
                double maxPercentage = getOreConduitMaxPercentage(player);
                
                // Validate percentage
                if (percentage < 0 || percentage > maxPercentage) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
                    return;
                }
                
                // Update the GUI to show the selection
                updateOreConduitSelection(player, percentage);
                
                // Play sound
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            } catch (NumberFormatException e) {
                // Invalid percentage format
                if (Main.getInstance().isDebugMode()) {
                    Main.getInstance().getLogger().warning("Error parsing OreConduit percentage: " + e.getMessage());
                }
            }
            return;
        }
        
        // Handle increment/decrement buttons
        String adjustStr = extractValueFromLore(clickedItem, "CONDUIT_ADJUST:");
        if (adjustStr != null) {
            try {
                double adjustment = Double.parseDouble(adjustStr);
                double currentPercentage = getSelectedPercentageFromInventory(player.getOpenInventory().getTopInventory());
                double maxPercentage = getOreConduitMaxPercentage(player);
                
                if (currentPercentage < 0) {
                    // If no current setting, start from default
                    int nodeLevel = getOreConduitNodeLevel(player);
                    double defaultPercentage = nodeLevel * 0.5; // 0.5% per level
                    currentPercentage = Math.min(maxPercentage, defaultPercentage);
                }
                
                // Calculate new percentage
                double newPercentage = currentPercentage + adjustment;
                
                // Ensure it's within valid range
                if (newPercentage < 0) {
                    newPercentage = 0;
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
                } else if (newPercentage > maxPercentage) {
                    newPercentage = maxPercentage;
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
                } else {
                    // Round to one decimal place
                    newPercentage = Math.round(newPercentage * 10) / 10.0;
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                }
                
                // Update the GUI to show the selection
                updateOreConduitSelection(player, newPercentage);
            } catch (NumberFormatException e) {
                // Invalid adjustment format
                if (Main.getInstance().isDebugMode()) {
                    Main.getInstance().getLogger().warning("Error parsing OreConduit adjustment: " + e.getMessage());
                }
            }
            return;
        }
    }

    /**
     * Get the max allowed percentage for OreConduit based on node level
     */
    private double getOreConduitMaxPercentage(Player player) {
        int nodeLevel = getOreConduitNodeLevel(player);
        return Math.min(50.0, nodeLevel * 0.5); // 0.5% per level, max 50%
    }

    /**
     * Get the currently selected percentage from the OreConduit config inventory
     */
    private double getSelectedPercentageFromInventory(Inventory inventory) {
        // Get it from the main center display item
        ItemStack item = inventory.getItem(4);  // New slot for the main display
        if (item != null && item.hasItemMeta()) {
            String percentStr = extractValueFromLore(item, "CONDUIT_PERCENT:");
            if (percentStr != null) {
                try {
                    return Double.parseDouble(percentStr);
                } catch (NumberFormatException e) {
                    // Invalid format
                }
            }
        }
        return -1; // No selection found
    }

    /**
     * Update the selection in the OreConduit GUI
     */
    private void updateOreConduitSelection(Player player, double newPercentage) {
        // Get the current inventory
        Inventory inv = player.getOpenInventory().getTopInventory();
        
        // Get max percentage allowed
        double maxPercentage = getOreConduitMaxPercentage(player);
        
        // Format for display
        String newPercentageStr = String.format("%.1f", newPercentage);
        
        // Update the info item (central display)
        ItemStack infoItem = inv.getItem(4);
        if (infoItem != null) {
            ItemMeta meta = infoItem.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + "✧ OreConduit Setting: " + ChatColor.GREEN + newPercentageStr + "%" + ChatColor.AQUA + " ✧");
            
            List<String> lore = meta.getLore();
            
            // Update the display content
            for (int i = 0; i < lore.size(); i++) {
                // Find the XP split lines and update them
                if (i >= 4 && i <= 6) {
                    if (newPercentage == 0.0) {
                        lore.set(4, ChatColor.GRAY + "No XP will be split to Mining");
                        lore.set(5, ChatColor.GRAY + "All XP stays with OreExtraction");
                        // Remove any third line from previous state if it exists
                        if (lore.size() > 6 && !lore.get(6).isEmpty() && 
                            !lore.get(6).contains("Use the buttons")) {
                            lore.remove(6);
                        }
                    } else {
                        lore.set(4, ChatColor.GRAY + "Split " + ChatColor.AQUA + newPercentageStr + "%" + 
                                ChatColor.GRAY + " of OreExtraction XP");
                        lore.set(5, ChatColor.GRAY + "to your Mining skill");
                    }
                    break;
                }
            }
            
            // Update the hidden value
            for (int i = 0; i < lore.size(); i++) {
                if (lore.get(i).startsWith(ChatColor.BLACK + "CONDUIT_PERCENT:")) {
                    lore.set(i, ChatColor.BLACK + "CONDUIT_PERCENT:" + newPercentage);
                    break;
                }
            }
            
            meta.setLore(lore);
            infoItem.setItemMeta(meta);
            inv.setItem(4, infoItem);
        }
        
        // Update the control buttons
        updateControlButtons(inv, newPercentage, maxPercentage);
        
        // Update preset buttons
        updatePresetButton(inv, 10, 0.0, "None", newPercentage, maxPercentage);
        updatePresetButton(inv, 11, 10.0, "Low", newPercentage, maxPercentage);
        updatePresetButton(inv, 15, 25.0, "Medium", newPercentage, maxPercentage);
        updatePresetButton(inv, 16, 40.0, "High", newPercentage, maxPercentage);
    }

    /**
     * Update all control buttons based on the new percentage
     */
    private void updateControlButtons(Inventory inv, double currentPercentage, double maxPercentage) {
        // Update the increment/decrement buttons
        updateControlButton(inv, 12, 1.0, currentPercentage, maxPercentage);
        updateControlButton(inv, 3, 0.1, currentPercentage, maxPercentage);
        updateControlButton(inv, 14, -1.0, currentPercentage, maxPercentage);
        updateControlButton(inv, 5, -0.1, currentPercentage, maxPercentage);
        
        // Update the default button highlight state
        ItemStack defaultButton = inv.getItem(13);
        if (defaultButton != null) {
            ItemMeta meta = defaultButton.getItemMeta();
            List<String> lore = meta.getLore();
            
            // Get the default percentage from the button
            String percentStr = extractValueFromLore(defaultButton, "CONDUIT_PERCENT:");
            if (percentStr != null) {
                try {
                    double defaultPercentage = Double.parseDouble(percentStr);
                    boolean isSelected = Math.abs(defaultPercentage - currentPercentage) < 0.01;
                    
                    if (isSelected) {
                        // Highlight if selected
                        defaultButton.setType(Material.ENCHANTED_BOOK);
                        meta.setDisplayName(ChatColor.GREEN + "» Default Value (" + 
                                        String.format("%.1f", defaultPercentage) + "%) «");
                        
                        // Add selection indicator if not present
                        boolean hasIndicator = false;
                        for (int i = 0; i < lore.size(); i++) {
                            if (lore.get(i).contains("CURRENTLY SELECTED")) {
                                hasIndicator = true;
                                break;
                            }
                        }
                        
                        if (!hasIndicator) {
                            // Add indicator before the hidden data
                            for (int i = lore.size() - 1; i >= 0; i--) {
                                if (lore.get(i).contains("CONDUIT_PERCENT:")) {
                                    lore.add(i, ChatColor.GREEN + "✓ CURRENTLY SELECTED");
                                    lore.add(i, "");
                                    break;
                                }
                            }
                        }
                    } else {
                        // Normal appearance if not selected
                        defaultButton.setType(Material.GOLD_INGOT);
                        meta.setDisplayName(ChatColor.GOLD + "Use Default Value (" + 
                                        String.format("%.1f", defaultPercentage) + "%)");
                        
                        // Remove selection indicator if present
                        for (int i = 0; i < lore.size(); i++) {
                            if (lore.get(i).contains("CURRENTLY SELECTED")) {
                                lore.remove(i);
                                if (i > 0 && i < lore.size() && lore.get(i-1).isEmpty()) {
                                    lore.remove(i-1);
                                }
                                break;
                            }
                        }
                    }
                    
                    meta.setLore(lore);
                    defaultButton.setItemMeta(meta);
                } catch (NumberFormatException ignored) {}
            }
        }
    }
    
    /**
     * Update a preset button based on the new percentage
     */
    private void updatePresetButton(Inventory inv, int slot, double value, String label, double currentPercentage, double maxPercentage) {
        ItemStack button = inv.getItem(slot);
        if (button == null) return;
        
        boolean isSelected = Math.abs(value - currentPercentage) < 0.01;
        boolean isAvailable = value <= maxPercentage;
        String valueStr = String.format("%.1f", value);
        
        // Update material based on state
        Material material;
        if (isSelected) {
            material = Material.LIME_CONCRETE;
        } else if (!isAvailable) {
            material = Material.BARRIER;
        } else {
            material = Material.LIGHT_GRAY_CONCRETE;
        }
        button.setType(material);
        
        ItemMeta meta = button.getItemMeta();
        
        // Update display name
        meta.setDisplayName((isSelected ? ChatColor.GREEN + "» " : "") + 
                        ChatColor.YELLOW + valueStr + "%" + 
                        (label != null ? " - " + label : "") + 
                        (isSelected ? ChatColor.GREEN + " «" : ""));
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        
        if (!isAvailable) {
            lore.add(ChatColor.RED + "Requires higher node level");
            lore.add(ChatColor.RED + "Current max: " + String.format("%.1f", maxPercentage) + "%");
        } else {
            if (value == 0.0) {
                lore.add(ChatColor.GRAY + "Keep all XP in OreExtraction");
            } else {
                lore.add(ChatColor.GRAY + "Send " + ChatColor.AQUA + valueStr + "%" + 
                        ChatColor.GRAY + " to Mining skill");
            }
            
            if (isSelected) {
                lore.add("");
                lore.add(ChatColor.GREEN + "✓ CURRENTLY SELECTED");
            }
        }
        
        // Keep the hidden data
        lore.add(ChatColor.BLACK + "CONDUIT_PERCENT:" + value);
        
        meta.setLore(lore);
        button.setItemMeta(meta);
    }

    /**
     * Update a control button based on the new percentage
     */
    private void updateControlButton(Inventory inv, int slot, double adjustment, double currentPercentage, double maxPercentage) {
        ItemStack button = inv.getItem(slot);
        if (button == null) return;
        
        boolean isPositive = adjustment > 0;
        ChatColor valueColor = isPositive ? ChatColor.GREEN : ChatColor.RED;
        double newValue = currentPercentage + adjustment;
        boolean isDisabled = (isPositive && newValue > maxPercentage) || (!isPositive && newValue < 0);
        
        // Update material based on state
        Material material;
        if (isDisabled) {
            material = Material.BARRIER;
        } else if (isPositive) {
            material = adjustment >= 1.0 ? Material.LIME_CONCRETE : Material.LIME_TERRACOTTA;
        } else {
            material = Math.abs(adjustment) >= 1.0 ? Material.RED_CONCRETE : Material.RED_TERRACOTTA;
        }
        button.setType(material);
        
        ItemMeta meta = button.getItemMeta();
        
        // Keep the same display name, only update the lore
        List<String> lore = new ArrayList<>();
        lore.add("");
        
        if (isDisabled) {
            lore.add(ChatColor.RED + (isPositive ? 
                    "Cannot exceed " + String.format("%.1f", maxPercentage) + "%" : 
                    "Cannot go below 0.0%"));
        } else {
            String action = isPositive ? "Add" : "Subtract";
            String amountStr = String.format("%.1f", Math.abs(adjustment));
            
            lore.add(ChatColor.GRAY + action + " " + valueColor + amountStr + "%" + 
                    ChatColor.GRAY + " to the current value");
            lore.add("");
            lore.add(ChatColor.GRAY + "New value: " + ChatColor.YELLOW + 
                    String.format("%.1f", Math.max(0, Math.min(maxPercentage, newValue))) + "%");
        }
        
        // Keep the hidden data
        lore.add(ChatColor.BLACK + "CONDUIT_ADJUST:" + adjustment);
        
        meta.setLore(lore);
        button.setItemMeta(meta);
    }

    /**
     * Helper method to update a decrement button's state based on the current value
     */
    private void updateDecrementButton(Inventory inv, int slot, double currentPercentage, double amount) {
        ItemStack button = inv.getItem(slot);
        if (button == null) return;
        
        ItemMeta meta = button.getItemMeta();
        List<String> lore = meta.getLore();
        
        // Clear the old lore from the non-static portion
        while (lore.size() > 3) {
            lore.remove(3);
        }
        
        // Indicate if removing this amount would go below 0
        boolean wouldGoBelowZero = (currentPercentage + amount) < 0;
        if (wouldGoBelowZero) {
            lore.add(ChatColor.RED + "Cannot go below 0.0%");
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        } else {
            lore.add(ChatColor.GRAY + "Click to subtract " + ChatColor.RED + String.format("%.1f", Math.abs(amount)) + "%" + 
                    ChatColor.GRAY + " from the current setting");
            lore.add(ChatColor.GRAY + "New value would be: " + 
                    ChatColor.GREEN + String.format("%.1f", Math.max(0, currentPercentage + amount)) + "%");
        }
        
        // Add hidden data for event handling
        lore.add("");
        lore.add(ChatColor.BLACK + "CONDUIT_ADJUST:" + amount);
        
        meta.setLore(lore);
        button.setItemMeta(meta);
    }

    /**
     * Helper method to update a preset button's state
     */
    private void updatePresetButton(Inventory inv, int slot, double buttonPercentage, double selectedPercentage) {
        ItemStack button = inv.getItem(slot);
        if (button == null) return;
        
        boolean isSelected = Math.abs(buttonPercentage - selectedPercentage) < 0.01;
        
        // Update material and look based on selection state
        Material material = isSelected ? Material.LIME_CONCRETE : Material.LIGHT_GRAY_CONCRETE;
        button.setType(material);
        
        ItemMeta meta = button.getItemMeta();
        
        // Update display name based on selection state
        meta.setDisplayName((isSelected ? ChatColor.GREEN + "» " : "") +
                        (isSelected ? ChatColor.BOLD : "") + 
                        (isSelected ? ChatColor.GREEN : ChatColor.YELLOW) + 
                        String.format("%.1f", buttonPercentage) + "% Split" + 
                        (isSelected ? " «" : ""));
        
        List<String> lore = meta.getLore();
        
        // Add or remove selection indicator
        boolean hasSelectionIndicator = false;
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).contains("CURRENTLY SELECTED")) {
                hasSelectionIndicator = true;
                if (!isSelected) {
                    lore.remove(i);
                    if (i > 0 && lore.get(i-1).isEmpty()) {
                        lore.remove(i-1);
                    }
                }
                break;
            }
        }
        
        // Add indicator if it should be selected but doesn't have indicator
        if (isSelected && !hasSelectionIndicator) {
            for (int i = 0; i < lore.size(); i++) {
                if (lore.get(i).contains("CONDUIT_PERCENT:")) {
                    lore.add(i, ChatColor.GREEN + "✓ CURRENTLY SELECTED");
                    lore.add(i, "");
                    break;
                }
            }
        }
        
        meta.setLore(lore);
        button.setItemMeta(meta);
    }

    /**
     * Create a preset button for the OreConduit GUI
     */
    private void addPresetButton(Inventory inv, int slot, double value, String label, double currentPercentage, double maxPercentage) {
        boolean isSelected = Math.abs(value - currentPercentage) < 0.01;
        boolean isAvailable = value <= maxPercentage;
        String valueStr = String.format("%.1f", value);
        
        Material material;
        if (isSelected) {
            material = Material.LIME_CONCRETE;
        } else if (!isAvailable) {
            material = Material.BARRIER;
        } else {
            material = Material.LIGHT_GRAY_CONCRETE;
        }
        
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        
        // Create a cleaner display name
        meta.setDisplayName((isSelected ? ChatColor.GREEN + "» " : "") + 
                        ChatColor.YELLOW + valueStr + "%" + 
                        (label != null ? " - " + label : "") + 
                        (isSelected ? ChatColor.GREEN + " «" : ""));
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        
        if (!isAvailable) {
            lore.add(ChatColor.RED + "Requires higher node level");
            lore.add(ChatColor.RED + "Current max: " + String.format("%.1f", maxPercentage) + "%");
        } else {
            if (value == 0.0) {
                lore.add(ChatColor.GRAY + "Keep all XP in OreExtraction");
            } else {
                lore.add(ChatColor.GRAY + "Send " + ChatColor.AQUA + valueStr + "%" + 
                        ChatColor.GRAY + " to Mining skill");
            }
            
            if (isSelected) {
                lore.add("");
                lore.add(ChatColor.GREEN + "✓ CURRENTLY SELECTED");
            }
        }
        
        // Add hidden data for event handling
        lore.add(ChatColor.BLACK + "CONDUIT_PERCENT:" + value);
        
        meta.setLore(lore);
        button.setItemMeta(meta);
        inv.setItem(slot, button);
    }

}