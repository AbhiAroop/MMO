package com.server.profiles.skills.abilities.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
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
 * GUI for listing passive or active abilities
 */
public class AbilityListGUI {
    
    // Keep title prefixes the same to maintain listener functionality
    public static final String GUI_TITLE_PREFIX_UNLOCKED = "Unlocked ";
    public static final String GUI_TITLE_PREFIX_ALL = "All ";
    
    /**
     * Open the list of abilities for a skill
     * 
     * @param player The player to show the GUI to
     * @param skillId The skill ID to show abilities for
     * @param abilityType The type of abilities to show ("PASSIVE" or "ACTIVE")
     * @param showAll Whether to show all abilities or just unlocked ones
     */
    public static void openAbilityList(Player player, String skillId, String abilityType, boolean showAll) {
        // Get the skill
        Skill skill = SkillRegistry.getInstance().getSkill(skillId);
        if (skill == null) {
            // Log error
            Main.getInstance().getLogger().warning("Attempt to open ability list for unknown skill ID: " + skillId);
            player.sendMessage(ChatColor.RED + "Error: Skill not found.");
            return;
        }
            
        // Debug logging
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("Opening ability list for " + player.getName() + 
                                            ", skill: " + skillId + 
                                            ", type: " + abilityType + 
                                            ", showAll: " + showAll);
            
            // Additional debug logging
            logAbilityDebugInfo(player, skill, skillId, abilityType);
        }
            
        // Create title based on type and mode - preserve for listener compatibility
        String title = (showAll ? GUI_TITLE_PREFIX_ALL : GUI_TITLE_PREFIX_UNLOCKED) + 
                    abilityType.charAt(0) + abilityType.substring(1).toLowerCase() + 
                    " Abilities: " + skill.getDisplayName();
            
        // Create inventory - use 54 slots (6 rows) for better layout
        Inventory gui = Bukkit.createInventory(null, 54, title);
            
        // Get the abilities
        AbilityRegistry registry = AbilityRegistry.getInstance();
        List<? extends SkillAbility> abilities = getAbilitiesList(registry, player, skillId, abilityType, showAll);
        
        // Debug log abilities found
        if (Main.getInstance().isDebugMode()) {
            Main.getInstance().getLogger().info("Found " + abilities.size() + " abilities for skill " + skillId + " of type " + abilityType);
            for (SkillAbility ability : abilities) {
                Main.getInstance().getLogger().info("  - " + ability.getDisplayName() + " (" + ability.getId() + ")");
            }
        }
        
        // Create decorative border
        createBorder(gui);
        
        // Add abilities to GUI
        if (abilities.isEmpty()) {
            // Create enhanced empty message
            ItemStack emptyItem = createEmptyAbilitiesItem(abilityType, showAll);
            gui.setItem(22, emptyItem);
        } else {
            // Add each ability item with enhanced presentation
            populateAbilityItems(gui, abilities, player);
        }
        
        // Add info item with enhanced design
        ItemStack infoItem = createInfoItem(skillId, abilityType, showAll, abilities.size(), player);
        gui.setItem(4, infoItem);
        
        // Add back button with improved design
        ItemStack backButton = createBackButton(skillId);
        gui.setItem(45, backButton);
        
        // Add toggle button with enhanced design
        ItemStack toggleButton = createToggleButton(skillId, abilityType, showAll);
        gui.setItem(49, toggleButton);
        
        // Add filter/sort buttons (if there are abilities to sort)
        if (!abilities.isEmpty()) {
            // Add filter unlocked button
            ItemStack filterButton = createFilterButton(skillId, abilityType, showAll);
            gui.setItem(47, filterButton);
            
            // Add help button
            ItemStack helpButton = createHelpButton(abilityType);
            gui.setItem(51, helpButton);
        }
        
        // Fill empty slots with glass panes
        fillEmptySlots(gui);
        
        // Open inventory
        player.openInventory(gui);
    }

    /**
     * Get the list of abilities based on type and filter settings
     */
    private static List<? extends SkillAbility> getAbilitiesList(AbilityRegistry registry, Player player, 
                                                              String skillId, String abilityType, boolean showAll) {
        if ("PASSIVE".equals(abilityType)) {
            return showAll ? registry.getPassiveAbilities(skillId) : 
                           registry.getUnlockedPassiveAbilities(player, skillId);
        } else {
            return showAll ? registry.getActiveAbilities(skillId) : 
                           registry.getUnlockedActiveAbilities(player, skillId);
        }
    }
    
    /**
     * Create a help button with usage information
     */
    private static ItemStack createHelpButton(String abilityType) {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "✦ " + ChatColor.YELLOW + "Help & Usage");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + "How to use " + abilityType.toLowerCase() + " abilities:");
        lore.add("");
        
        if ("PASSIVE".equals(abilityType)) {
            lore.add(ChatColor.AQUA + "» " + ChatColor.YELLOW + "Passive Abilities:");
            lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Toggle on/off with LEFT-CLICK");
            lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Some abilities have settings");
            lore.add(ChatColor.GRAY + "  that can be configured with RIGHT-CLICK");
            lore.add("");
            lore.add(ChatColor.GRAY + "When enabled, passive abilities");
            lore.add(ChatColor.GRAY + "will automatically activate while");
            lore.add(ChatColor.GRAY + "performing related activities.");
        } else {
            lore.add(ChatColor.AQUA + "» " + ChatColor.YELLOW + "Active Abilities:");
            lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Most active abilities are used with");
            lore.add(ChatColor.GRAY + "  specific keybinds or commands");
            lore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + "Each ability shows its activation");
            lore.add(ChatColor.GRAY + "  method in its description");
            lore.add("");
            lore.add(ChatColor.GRAY + "Active abilities consume mana or");
            lore.add(ChatColor.GRAY + "energy when triggered.");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create a filter button to quickly access common filters
     */
    private static ItemStack createFilterButton(String skillId, String abilityType, boolean showAll) {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "✦ " + ChatColor.AQUA + "Display Options");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.GRAY + "Currently showing: " + ChatColor.YELLOW + 
                (showAll ? "All abilities" : "Only unlocked abilities"));
        lore.add("");
        lore.add(ChatColor.GRAY + "Click to switch viewing mode");
        
        // Add hidden data for toggle functionality
        lore.add(ChatColor.BLACK + "SKILL:" + skillId);
        lore.add(ChatColor.BLACK + "ABILITY_TYPE:" + abilityType);
        lore.add(ChatColor.BLACK + "TOGGLE_SHOW_ALL:" + !showAll);
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Log detailed ability debug information
     */
    private static void logAbilityDebugInfo(Player player, Skill skill, String skillId, String abilityType) {
        Main.getInstance().getLogger().info("=== Available Abilities Debug Dump ===");
        Main.getInstance().getLogger().info("Checking abilities for skill: " + skillId);
        
        // Check parent skill ID if this is a subskill
        String parentSkillId = null;
        if (!skill.isMainSkill() && skill.getParentSkill() != null) {
            parentSkillId = skill.getParentSkill().getId();
            Main.getInstance().getLogger().info("This is a subskill. Parent skill: " + parentSkillId);
        }
        
        // Log all registered abilities
        AbilityRegistry registry = AbilityRegistry.getInstance();
        
        // Check passive abilities
        Main.getInstance().getLogger().info("Passive abilities registered for " + skillId + ":");
        List<PassiveAbility> passives = registry.getPassiveAbilities(skillId);
        for (PassiveAbility ability : passives) {
            Main.getInstance().getLogger().info("  - " + ability.getDisplayName() + " (" + ability.getId() + ")");
            Main.getInstance().getLogger().info("    Unlocked: " + ability.isUnlocked(player));
        }
        
        // Check active abilities
        Main.getInstance().getLogger().info("Active abilities registered for " + skillId + ":");
        List<ActiveAbility> actives = registry.getActiveAbilities(skillId);
        for (ActiveAbility ability : actives) {
            Main.getInstance().getLogger().info("  - " + ability.getDisplayName() + " (" + ability.getId() + ")");
            Main.getInstance().getLogger().info("    Unlocked: " + ability.isUnlocked(player));
        }
        
        // Check parent skills if this is a subskill
        if (parentSkillId != null) {
            Main.getInstance().getLogger().info("Checking parent skill abilities for " + parentSkillId + ":");
            
            // Check passive abilities for parent
            Main.getInstance().getLogger().info("Parent passive abilities:");
            List<PassiveAbility> parentPassives = registry.getPassiveAbilities(parentSkillId);
            for (PassiveAbility ability : parentPassives) {
                Main.getInstance().getLogger().info("  - " + ability.getDisplayName() + " (" + ability.getId() + ")");
                Main.getInstance().getLogger().info("    Unlocked: " + ability.isUnlocked(player));
            }
            
            // Check active abilities for parent
            Main.getInstance().getLogger().info("Parent active abilities:");
            List<ActiveAbility> parentActives = registry.getActiveAbilities(parentSkillId);
            for (ActiveAbility ability : parentActives) {
                Main.getInstance().getLogger().info("  - " + ability.getDisplayName() + " (" + ability.getId() + ")");
                Main.getInstance().getLogger().info("    Unlocked: " + ability.isUnlocked(player));
            }
        }
        
        Main.getInstance().getLogger().info("=== End of Abilities Debug Dump ===");
    }
    
    /**
     * Create empty abilities message with visual enhancements
     */
    private static ItemStack createEmptyAbilitiesItem(String abilityType, boolean showAll) {
        Material icon = "PASSIVE".equals(abilityType) ? Material.REDSTONE_TORCH : Material.BLAZE_POWDER;
        ItemStack emptyItem = new ItemStack(icon);
        ItemMeta emptyMeta = emptyItem.getItemMeta();
        emptyMeta.setDisplayName(ChatColor.RED + "No " + abilityType.toLowerCase() + " abilities found");
        
        List<String> emptyLore = new ArrayList<>();
        emptyLore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        if (showAll) {
            emptyLore.add(ChatColor.GRAY + "There are no " + abilityType.toLowerCase() + " abilities");
            emptyLore.add(ChatColor.GRAY + "available for this skill yet.");
            emptyLore.add("");
            emptyLore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "This skill might receive new");
            emptyLore.add(ChatColor.YELLOW + "abilities in future updates.");
        } else {
            emptyLore.add(ChatColor.GRAY + "You haven't unlocked any " + abilityType.toLowerCase());
            emptyLore.add(ChatColor.GRAY + "abilities for this skill yet.");
            emptyLore.add("");
            emptyLore.add(ChatColor.GOLD + "» " + ChatColor.YELLOW + "Right-click the display options");
            emptyLore.add(ChatColor.YELLOW + "button to view all available abilities");
            emptyLore.add(ChatColor.YELLOW + "and how to unlock them!");
        }
        
        emptyMeta.setLore(emptyLore);
        emptyItem.setItemMeta(emptyMeta);
        return emptyItem;
    }
    
    /**
     * Add ability items to the GUI with pagination if needed
     */
    private static void populateAbilityItems(Inventory gui, List<? extends SkillAbility> abilities, Player player) {
        // Define slots for ability items - 3 rows of 7 items each
        int[] slots = {
            10, 11, 12, 13, 14, 15, 16,  // Row 1
            19, 20, 21, 22, 23, 24, 25,  // Row 2
            28, 29, 30, 31, 32, 33, 34   // Row 3
        };
        
        // Add each ability to its slot
        for (int i = 0; i < abilities.size() && i < slots.length; i++) {
            SkillAbility ability = abilities.get(i);
            ItemStack abilityItem = createEnhancedAbilityItem(ability, player);
            gui.setItem(slots[i], abilityItem);
        }
    }
    
    /**
     * Create a visually enhanced ability item
     */
    private static ItemStack createEnhancedAbilityItem(SkillAbility ability, Player player) {
        ItemStack item = ability.createDisplayItem(player);
        ItemMeta meta = item.getItemMeta();
        
        // Add enchant glow for unlocked abilities
        if (ability.isUnlocked(player)) {
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        
        // Add a divider after the name for better readability
        if (!lore.isEmpty()) {
            if (!lore.get(0).contains("▬▬▬▬")) {
                lore.add(0, ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            }
        }
        
        // For passive abilities that are unlocked, add clear instructions
        if (ability instanceof PassiveAbility && ability.isUnlocked(player)) {
            // Find where to insert the instruction line - after Status
            int insertPos = -1;
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                if (line.contains("Status:")) {
                    insertPos = i + 1;
                    break;
                }
            }
            
            // Add a controls section with enhanced formatting
            if (insertPos != -1) {
                // Add empty line if there's not one already
                if (insertPos < lore.size() && !lore.get(insertPos).isEmpty()) {
                    lore.add(insertPos, "");
                    insertPos++;
                }
                
                // Add controls header
                lore.add(insertPos, ChatColor.GOLD + "» " + ChatColor.YELLOW + "Controls:");
                insertPos++;
                
                // Add toggle control
                lore.add(insertPos, ChatColor.LIGHT_PURPLE + "• " + ChatColor.GREEN + "LEFT-CLICK" + 
                    ChatColor.YELLOW + " to toggle on/off");
                insertPos++;
                
                // Add configuration control for abilities that support it
                if (ability.getId().equals("vein_miner") || ability.getId().equals("ore_conduit")) {
                    lore.add(insertPos, ChatColor.LIGHT_PURPLE + "• " + ChatColor.GREEN + "RIGHT-CLICK" + 
                        ChatColor.YELLOW + " to configure settings");
                }
            } else {
                // If we couldn't find the status line, add at the end before hidden data
                int lastIndex = Math.max(0, lore.size() - 3);
                
                lore.add(lastIndex, "");
                lore.add(lastIndex + 1, ChatColor.GOLD + "» " + ChatColor.YELLOW + "Controls:");
                lore.add(lastIndex + 2, ChatColor.LIGHT_PURPLE + "• " + ChatColor.GREEN + "LEFT-CLICK" + 
                    ChatColor.YELLOW + " to toggle on/off");
                
                if (ability.getId().equals("vein_miner") || ability.getId().equals("ore_conduit")) {
                    lore.add(lastIndex + 3, ChatColor.LIGHT_PURPLE + "• " + ChatColor.GREEN + "RIGHT-CLICK" + 
                        ChatColor.YELLOW + " to configure settings");
                }
            }
        }
        
        // For active abilities that are unlocked, add usage info
        if (ability instanceof ActiveAbility && ability.isUnlocked(player)) {
            // Add activation method details at bottom (before hidden data)
            ActiveAbility activeAbility = (ActiveAbility) ability;
            String activation = activeAbility.getActivationMethod();
            
            // Find existing activation info or add it
            boolean hasActivationInfo = false;
            for (int i = 0; i < lore.size(); i++) {
                if (lore.get(i).contains("Activation:")) {
                    hasActivationInfo = true;
                    break;
                }
            }
            
            if (!hasActivationInfo) {
                int lastIndex = Math.max(0, lore.size() - 3);
                lore.add(lastIndex, "");
                lore.add(lastIndex + 1, ChatColor.GOLD + "» " + ChatColor.YELLOW + "Activation:");
                lore.add(lastIndex + 2, ChatColor.LIGHT_PURPLE + "• " + ChatColor.GREEN + activation);
            }
        }
        
        // Add the ability ID for tracking (preserve these at the very end)
        boolean hasHiddenData = false;
        for (String line : lore) {
            if (line.contains("ABILITY:")) {
                hasHiddenData = true;
                break;
            }
        }
        
        if (!hasHiddenData) {
            lore.add(ChatColor.BLACK + "ABILITY:" + ability.getId());
            lore.add(ChatColor.BLACK + "ABILITY_TYPE:" + (ability instanceof PassiveAbility ? "PASSIVE" : "ACTIVE"));
            lore.add(ChatColor.BLACK + "SKILL:" + ability.getSkillId());
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create a toggle button for switching between showing all or unlocked abilities
     */
    private static ItemStack createToggleButton(String skillId, String abilityType, boolean showAll) {
        Material icon = showAll ? Material.CHEST : Material.ENDER_CHEST;
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(showAll ? 
            ChatColor.AQUA + "✦ Show Unlocked Abilities Only" : 
            ChatColor.AQUA + "✦ Show All Available Abilities");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        if (showAll) {
            lore.add(ChatColor.GRAY + "Currently showing: " + ChatColor.YELLOW + "All abilities");
            lore.add(ChatColor.GRAY + "Including those you haven't unlocked yet");
            lore.add("");
            lore.add(ChatColor.GREEN + "Click to show only unlocked abilities");
        } else {
            lore.add(ChatColor.GRAY + "Currently showing: " + ChatColor.YELLOW + "Only unlocked abilities");
            lore.add("");
            lore.add(ChatColor.GREEN + "Click to show all abilities");
            lore.add(ChatColor.GRAY + "(including locked ones and how to unlock them)");
        }
        
        // Add skill ID and ability type for identification in GUI handler (preserve for compatibility)
        lore.add(ChatColor.BLACK + "SKILL:" + skillId);
        lore.add(ChatColor.BLACK + "ABILITY_TYPE:" + abilityType);
        lore.add(ChatColor.BLACK + "TOGGLE_SHOW_ALL:" + !showAll);
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create an enhanced back button
     */
    private static ItemStack createBackButton(String skillId) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "« Back to Abilities");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Return to the ability selection menu");
        
        // Add skill ID for back navigation (preserve for compatibility)
        lore.add(ChatColor.BLACK + "SKILL:" + skillId);
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create enhanced info item for the GUI
     */
    private static ItemStack createInfoItem(String skillId, String abilityType, boolean showAll, int abilityCount, Player player) {
        Skill skill = SkillRegistry.getInstance().getSkill(skillId);
        if (skill == null) return new ItemStack(Material.BARRIER);
        
        // Choose appropriate icon based on skill and ability type
        Material icon = getMaterialForSkill(skill, abilityType);
        
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        // Add enchant glow for visual appeal
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        // Enhanced title with decorative elements
        meta.setDisplayName(ChatColor.GOLD + "✦ " + 
            ChatColor.YELLOW + skill.getDisplayName() + " " + 
            ("PASSIVE".equals(abilityType) ? ChatColor.AQUA : ChatColor.LIGHT_PURPLE) +
            abilityType.charAt(0) + abilityType.substring(1).toLowerCase() + " Abilities" +
            ChatColor.GOLD + " ✦");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        // Add skill description if available
        if (skill.getDescription() != null && !skill.getDescription().isEmpty()) {
            String[] descLines = skill.getDescription().split("\\.");
            for (String line : descLines) {
                if (!line.trim().isEmpty()) {
                    lore.add(ChatColor.GRAY + line.trim() + ".");
                }
            }
            lore.add("");
        }
        
        // Add ability count with enhanced formatting
        if ("PASSIVE".equals(abilityType)) {
            lore.add(ChatColor.AQUA + "» " + ChatColor.YELLOW + "Passive Abilities:");
        } else {
            lore.add(ChatColor.LIGHT_PURPLE + "» " + ChatColor.YELLOW + "Active Abilities:");
        }
        
        // Get total count of all abilities (unlocked and locked)
        AbilityRegistry registry = AbilityRegistry.getInstance();
        int totalCount = "PASSIVE".equals(abilityType) ? 
            registry.getPassiveAbilities(skillId).size() : 
            registry.getActiveAbilities(skillId).size();
        
        int unlockedCount = "PASSIVE".equals(abilityType) ?
            registry.getUnlockedPassiveAbilities(player, skillId).size() :
            registry.getUnlockedActiveAbilities(player, skillId).size();
        
        // Add visual progress bar for unlocked abilities
        lore.add(createProgressBar(unlockedCount, totalCount) + " " + 
                ChatColor.GREEN + unlockedCount + "/" + totalCount + " unlocked");
        
        lore.add("");
        lore.add(ChatColor.GRAY + "Currently viewing: " + ChatColor.YELLOW + 
                (showAll ? "All abilities" : "Unlocked abilities only") + 
                ChatColor.GRAY + " (" + abilityCount + ")");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Get appropriate material for the skill icon
     */
    private static Material getMaterialForSkill(Skill skill, String abilityType) {
        // First check skill ID
        switch (skill.getId()) {
            case "mining":
                return Material.DIAMOND_PICKAXE;
            case "excavating":
                return Material.DIAMOND_SHOVEL;
            case "fishing":
                return Material.FISHING_ROD;
            case "farming":
                return Material.DIAMOND_HOE;
            case "combat":
                return Material.DIAMOND_SWORD;
            case "ore_extraction":
                return Material.IRON_ORE;
            case "gem_carving":
                return Material.EMERALD;
        }
        
        // Fall back to skill name check
        if (skill.getDisplayName().equalsIgnoreCase("Mining")) {
            return Material.DIAMOND_PICKAXE;
        } else if (skill.getDisplayName().equalsIgnoreCase("Ore Extraction")) {
            return Material.IRON_ORE;
        } else if (skill.getDisplayName().equalsIgnoreCase("Gem Carving")) {
            return Material.EMERALD;
        }
        
        // Default based on ability type
        return "PASSIVE".equals(abilityType) ? Material.REDSTONE_TORCH : Material.BLAZE_POWDER;
    }
    
    /**
     * Create a fancy progress bar for visual representation
     */
    private static String createProgressBar(int value, int max) {
        StringBuilder bar = new StringBuilder();
        int barLength = 10;
        int filledBars = max > 0 ? (int) Math.round((double) value / max * barLength) : 0;
        
        // Start with bracket
        bar.append(ChatColor.GRAY + "[");
        
        // Add colored progress bars with gradient
        for (int i = 0; i < barLength; i++) {
            if (i < filledBars) {
                if ((double) value / max < 0.33) {
                    bar.append(ChatColor.YELLOW);
                } else if ((double) value / max < 0.67) {
                    bar.append(ChatColor.GREEN);
                } else {
                    bar.append(ChatColor.AQUA);
                }
                bar.append("■");
            } else {
                bar.append(ChatColor.DARK_GRAY).append("■");
            }
        }
        
        // Close bracket
        bar.append(ChatColor.GRAY + "]");
        
        return bar.toString();
    }
    
    /**
     * Create decorative border for GUI
     */
    private static void createBorder(Inventory gui) {
        ItemStack blue = createGlassPane(Material.BLUE_STAINED_GLASS_PANE);
        ItemStack purple = createGlassPane(Material.PURPLE_STAINED_GLASS_PANE);
        ItemStack corner = createGlassPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        
        // Set corners
        gui.setItem(0, corner);
        gui.setItem(8, corner);
        gui.setItem(45, corner); // Don't override back button
        gui.setItem(53, corner);
        
        // Top and bottom borders
        for (int i = 1; i < 8; i++) {
            gui.setItem(i, i % 2 == 0 ? blue : purple);
            gui.setItem(45 + i, i % 2 == 0 ? blue : purple);
        }
        
        // Side borders - leave space for ability items
        for (int i = 1; i <= 4; i++) {
            gui.setItem(i * 9, i % 2 == 0 ? purple : blue);
            gui.setItem(i * 9 + 8, i % 2 == 0 ? purple : blue);
        }
    }
    
    /**
     * Create glass pane with empty name
     */
    private static ItemStack createGlassPane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }
    
    /**
     * Fill empty slots with black glass panes
     */
    private static void fillEmptySlots(Inventory gui) {
        ItemStack filler = createGlassPane(Material.BLACK_STAINED_GLASS_PANE);
        
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }
}