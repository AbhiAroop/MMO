package com.server.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.entities.npc.NPCFactory;
import com.server.entities.npc.NPCManager;
import com.server.entities.npc.NPCStats;
import com.server.entities.npc.dialogue.DialogueManager;
import com.server.entities.npc.story.StoryNPCRegistry;
import com.server.entities.npc.types.CombatNPC;
import com.server.entities.npc.types.DialogueNPC;
import com.server.entities.npc.types.HostileNPC;
import com.server.entities.npc.types.PassiveNPC;
import com.server.items.CustomItems;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot;

/**
 * Command handler for NPC-related commands
 */
public class NPCCommand implements CommandExecutor, TabCompleter {
    
    private final Main plugin;
    
    /**
     * Create a new NPC command
     * 
     * @param plugin The plugin instance
     */
    public NPCCommand(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("mmo.command.npc")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        if (subCommand.equals("create")) {
            return handleCreate(player, args);
        } else if (subCommand.equals("remove")) {
            return handleRemove(player, args);
        } else if (subCommand.equals("removeall")) {
            return handleRemoveAll(player, args);
        } else if (subCommand.equals("dialogue")) {
            return handleDialogue(player, args);
        } else if (subCommand.equals("list")) {
            return handleList(player, args);
        } else if (subCommand.equals("equip")) {
            return handleEquip(player, args);
        } else if (subCommand.equals("story")) {
            return handleStory(player, args);
        } else {
            sendHelp(player);
            return true;
        }
    }
    
    /**
     * Handle the 'create' subcommand
     */
    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 4) {
            sendHelp(player);
            return true;
        }
        
        String type = args[1].toLowerCase();
        String id = args[2];
        
        // Combine the rest of the arguments as the name (if not provided separately)
        String name = args[3];
        String skin = args.length > 4 ? args[4] : player.getName();
        
        NPCFactory factory = NPCFactory.getInstance();
        NPCManager manager = NPCManager.getInstance();
        
        // Parse health and damage parameters if provided
        double health = 100.0; // Default health
        int damage = 10; // Default damage
        
        try {
            if (args.length > 5) {
                health = Double.parseDouble(args[5]);
            }
            if (args.length > 6) {
                damage = Integer.parseInt(args[6]);
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid health or damage value. Using defaults.");
        }
        
        // Debug log for custom stats
        if (health != 100.0 || damage != 10) {
            plugin.debugLog(DebugSystem.NPC,"Creating NPC with custom stats: Health=" + health + ", Damage=" + damage);
        }
        
        if (type.equals("talk")) {
            DialogueNPC npc = factory.createDialogueNPC(id, name, player.getLocation(), skin);
            player.sendMessage("§aCreated dialogue NPC: §e" + name);
        } else if (type.equals("passive")) {
            NPCStats stats = new NPCStats();
            stats.setMaxHealth(health);
            stats.setPhysicalDamage(damage);
            
            PassiveNPC npc = new PassiveNPC(id, name, stats);
            npc.spawn(player.getLocation(), skin);
            player.sendMessage("§aCreated passive NPC: §e" + name);
        } else if (type.equals("combat")) {
            NPCStats stats = new NPCStats();
            stats.setMaxHealth(health);
            stats.setPhysicalDamage(damage);
            
            // Create a combat NPC that only fights back when provoked
            CombatNPC npc = factory.createCustomCombatNPC(id, name, player.getLocation(), skin, stats);
            player.sendMessage("§aCreated combat NPC: §e" + name);
        } else if (type.equals("hostile")) {
            NPCStats stats = new NPCStats();
            stats.setMaxHealth(health);
            stats.setPhysicalDamage(damage);
            
            // Create a hostile NPC that attacks players on sight
            HostileNPC npc = factory.createCustomHostileNPC(id, name, player.getLocation(), skin, stats);
            player.sendMessage("§aCreated hostile NPC: §e" + name);
        } else {
            player.sendMessage("§cInvalid NPC type. Valid types: talk, passive, combat, hostile");
            return true;
        }
        
        return true;
    }
    
    /**
     * Handle the 'remove' subcommand
     */
    private boolean handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /mmonpc remove <id>");
            return true;
        }
        
        String id = args[1];
        NPCManager manager = NPCManager.getInstance();
        
        if (manager.getNPC(id) != null) {
            manager.removeNPC(id);
            player.sendMessage(ChatColor.GREEN + "Removed NPC with ID: " + id);
        } else {
            player.sendMessage(ChatColor.RED + "No NPC found with ID: " + id);
        }
        
        return true;
    }
    
    /**
     * Handle the 'dialogue' subcommand - used by dialogue click events
     */
    private boolean handleDialogue(Player player, String[] args) {
        // This should handle dialogue responses from players clicking dialogue options
        if (args.length < 3) {
            // Not enough arguments for dialogue command
            if (plugin.isDebugMode()) {
                plugin.debugLog(DebugSystem.NPC,"Invalid dialogue command format. Expected: /mmonpc dialogue <playerUUID> <responseIndex>");
            }
            return true;
        }
        
        try {
            // Parse player UUID and response index
            UUID playerUUID = UUID.fromString(args[1]);
            int responseIndex = Integer.parseInt(args[2]);
            
            // Verify this player is the one who issued the command
            if (!player.getUniqueId().equals(playerUUID)) {
                if (plugin.isDebugMode()) {
                    plugin.debugLog(DebugSystem.NPC,"Player " + player.getName() + " attempted to respond to dialogue for " + 
                                            playerUUID + " but UUIDs don't match");
                }
                return true;
            }
            
            // Forward the response to the DialogueManager
            DialogueManager.getInstance().handleResponse(player, responseIndex);
            
            // Debug logging
            if (plugin.isDebugMode()) {
                plugin.debugLog(DebugSystem.NPC,"Player " + player.getName() + " selected dialogue response " + responseIndex);
            }
            
            return true;
        } catch (IllegalArgumentException e) {
            // Invalid UUID or response index
            if (plugin.isDebugMode()) {
                plugin.debugLog(DebugSystem.NPC,"Error parsing dialogue command: " + e.getMessage());
            }
            return true;
        }
    }
    
    /**
     * Handle the 'list' subcommand
     */
    private boolean handleList(Player player, String[] args) {
        NPCManager manager = NPCManager.getInstance();
        List<String> ids = manager.getIds();
        
        if (ids.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No NPCs found.");
            return true;
        }
        
        player.sendMessage(ChatColor.GREEN + "NPCs (" + ids.size() + "):");
        for (String id : ids) {
            NPC npc = manager.getNPC(id);
            if (npc != null) {
                player.sendMessage(ChatColor.GREEN + " - " + id + ": " + npc.getName() + 
                                  (npc.isSpawned() ? ChatColor.GREEN + " (Spawned)" : ChatColor.RED + " (Despawned)"));
            }
        }
        
        return true;
    }
    
    /**
     * Send help message to a player
     */
    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GREEN + "==== NPC Commands ====");
        player.sendMessage(ChatColor.YELLOW + "/mmonpc create <type> <id> <name> [skin] [health] [damage]" + ChatColor.WHITE + " - Create an NPC");
        player.sendMessage(ChatColor.YELLOW + "/mmonpc remove <id>" + ChatColor.WHITE + " - Remove an NPC");
        player.sendMessage(ChatColor.YELLOW + "/mmonpc removeall [radius]" + ChatColor.WHITE + " - Remove all NPCs or those within a radius");
        player.sendMessage(ChatColor.YELLOW + "/mmonpc list" + ChatColor.WHITE + " - List all NPCs");
        player.sendMessage(ChatColor.YELLOW + "/mmonpc equip <id> <slot> <item>" + ChatColor.WHITE + " - Equip an NPC");
        player.sendMessage(ChatColor.YELLOW + "/mmonpc story <id> [skin]" + ChatColor.WHITE + " - Spawn a story NPC");
        player.sendMessage(ChatColor.GREEN + "===================");
        player.sendMessage(ChatColor.YELLOW + "Available NPC types: talk, passive, combat, hostile");
        player.sendMessage(ChatColor.YELLOW + "Available equipment slots: mainhand, offhand, helmet, chestplate, leggings, boots");
        player.sendMessage(ChatColor.YELLOW + "Available story NPCs: kaelen_echobound, nell_mossgleam");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Main commands
            List<String> commands = Arrays.asList("create", "remove", "removeall", "list", "equip","story");
            String input = args[0].toLowerCase();
            
            for (String cmd : commands) {
                if (cmd.startsWith(input)) {
                    completions.add(cmd);
                }
            }
        }
        else if (args.length == 2) {
        // Depends on first argument
            if (args[0].equalsIgnoreCase("create")) {
                // NPC types
                List<String> types = Arrays.asList("talk", "passive", "combat", "hostile");
                String input = args[1].toLowerCase();
                
                for (String type : types) {
                    if (type.startsWith(input)) {
                        completions.add(type);
                    }
                }
            }
            else if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("equip")) {
                // NPC IDs
                NPCManager manager = NPCManager.getInstance();
                String input = args[1].toLowerCase();
                
                for (String id : manager.getIds()) {
                    if (id.toLowerCase().startsWith(input)) {
                        completions.add(id);
                    }
                }
            }
            else if (args[0].equalsIgnoreCase("removeall")) {
                // For removeall, suggest some common radius values
                List<String> radii = Arrays.asList("5", "10", "20", "50");
                String input = args[1].toLowerCase();
                
                for (String radius : radii) {
                    if (radius.startsWith(input)) {
                        completions.add(radius);
                    }
                }
            }
            else if (args[0].equalsIgnoreCase("story")) {
                // Story NPC IDs
                List<String> storyNpcs = Arrays.asList("kaelen_echobound", "nell_mossgleam");
                String input = args[1].toLowerCase();
                
                for (String npc : storyNpcs) {
                    if (npc.startsWith(input)) {
                        completions.add(npc);
                    }
                }
            }
        }
        else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("equip")) {
                // Equipment slots
                List<String> slots = Arrays.asList("mainhand", "offhand", "helmet", "chestplate", "leggings", "boots");
                String input = args[2].toLowerCase();
                
                for (String slot : slots) {
                    if (slot.startsWith(input)) {
                        completions.add(slot);
                    }
                }
            }
        }
        else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("equip")) {
                // Item names
                List<String> items = Arrays.asList("witchhat", "apprenticeedge", "emberwood", "arcloom", 
                                                "crownofmagnus", "siphonfang", "fleshrake", "shatteredshell");
                String input = args[3].toLowerCase();
                
                for (String item : items) {
                    if (item.startsWith(input)) {
                        completions.add(item);
                    }
                }
            }
        }
        else if (args.length >= 4) {
            if (args[0].equalsIgnoreCase("create")) {
                // Suggest online players for skin names
                String input = args[args.length - 1].toLowerCase();
                return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
            }
        }
        
        return completions;
    }

    /**
     * Handle the 'removeall' subcommand - removes all NPCs or those within a radius
     */
    private boolean handleRemoveAll(Player player, String[] args) {
        NPCManager manager = NPCManager.getInstance();
        
        if (args.length > 1) {
            // Remove NPCs within a radius
            try {
                double radius = Double.parseDouble(args[1]);
                int removed = 0;
                
                for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                    if (entity.hasMetadata("NPC")) {
                        // Find the NPC ID by UUID
                        for (String id : new ArrayList<>(manager.getIds())) {
                            NPC npc = manager.getNPC(id);
                            if (npc != null && npc.getEntity() == entity) {
                                manager.removeNPC(id);
                                removed++;
                                break;
                            }
                        }
                    }
                }
                
                player.sendMessage(ChatColor.GREEN + "Removed " + removed + " NPCs within " + radius + " blocks.");
                
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid radius: " + args[1]);
                return true;
            }
        } else {
            // Remove all NPCs
            int count = manager.getIds().size();
            
            for (String id : new ArrayList<>(manager.getIds())) {
                manager.removeNPC(id);
            }
            
            player.sendMessage(ChatColor.GREEN + "Removed all NPCs (" + count + ").");
        }
        
        return true;
    }

    /**
     * Handle the 'story' subcommand - spawns a story NPC
     */
    private boolean handleStory(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /mmonpc story <id> [skin]");
            player.sendMessage(ChatColor.YELLOW + "Available story NPCs: kaelen_echobound");
            return true;
        }
        
        String id = args[1].toLowerCase();
        String skin = args.length > 2 ? args[2] : null;
        
        // Get the story NPC registry
        StoryNPCRegistry storyRegistry = StoryNPCRegistry.getInstance();
        
        // Attempt to spawn the story NPC
        boolean success = storyRegistry.spawnStoryNPC(id, player.getLocation(), skin);
        
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Spawned story NPC: " + id);
        } else {
            player.sendMessage(ChatColor.RED + "Failed to spawn story NPC: " + id);
            player.sendMessage(ChatColor.YELLOW + "Available story NPCs: kaelen_echobound");
        }
        
        return true;
    }

    /**
     * Handle the 'equip' subcommand - gives equipment to an NPC
     */
    private boolean handleEquip(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: /mmonpc equip <id> <slot> <item>");
            return true;
        }
        
        String id = args[1];
        String slotName = args[2].toLowerCase();
        String itemName = args[3].toLowerCase();
        
        NPCManager manager = NPCManager.getInstance();
        NPC npc = manager.getNPC(id);
        
        if (npc == null) {
            player.sendMessage(ChatColor.RED + "No NPC found with ID: " + id);
            return true;
        }
        
        // Convert slot name to EquipmentSlot
        EquipmentSlot slot;
        switch (slotName) {
            case "mainhand":
                slot = EquipmentSlot.HAND;
                break;
            case "offhand":
                slot = EquipmentSlot.OFF_HAND;
                break;
            case "helmet":
                slot = EquipmentSlot.HELMET;
                break;
            case "chestplate":
                slot = EquipmentSlot.CHESTPLATE;
                break;
            case "leggings":
                slot = EquipmentSlot.LEGGINGS;
                break;
            case "boots":
                slot = EquipmentSlot.BOOTS;
                break;
            default:
                player.sendMessage(ChatColor.RED + "Invalid slot. Use: mainhand, offhand, helmet, chestplate, leggings, boots");
                return true;
        }
        
        // Get the item
        ItemStack item = getCustomItemByName(itemName);
        if (item == null) {
            player.sendMessage(ChatColor.RED + "Unknown item: " + itemName);
            return true;
        }
        
        // Get the item debug info
        String itemInfo = item.getType().name();
        if (item.hasItemMeta()) {
            if (item.getItemMeta().hasDisplayName()) {
                itemInfo = item.getItemMeta().getDisplayName();
            }
            if (item.getItemMeta().hasCustomModelData()) {
                itemInfo += " (Model:" + item.getItemMeta().getCustomModelData() + ")";
            }
        }
        
        // Debug log the item that's being equipped
        plugin.debugLog(DebugSystem.NPC,"Equipping " + itemInfo + " to NPC " + id + " in slot " + slotName);
        
        // IMPORTANT FIX: Pass false to prevent NPCManager from updating stats - we'll do it ourselves
        manager.setEquipment(id, slot, item, false);
        player.sendMessage(ChatColor.GREEN + "Equipped " + itemName + " to " + slotName + " slot of NPC " + id);
        
        // Update the NPC's stats based on equipment
        Object handler = manager.getInteractionHandler(id);
        if (handler instanceof CombatNPC) {
            CombatNPC combatNPC = (CombatNPC) handler;
            
            // Get baseline stats before equipment is applied
            NPCStats baseStats = combatNPC.getStats().clone();
            
            // Update stats from equipment
            combatNPC.updateStatsFromEquipment();
            NPCStats updatedStats = combatNPC.getStats();
            
            // Show updated stats for confirmation and highlight changes
            player.sendMessage(ChatColor.YELLOW + "Updated NPC stats: ");
            
            if (baseStats.getMaxHealth() != updatedStats.getMaxHealth()) {
                player.sendMessage(ChatColor.RED + "Health: " + baseStats.getMaxHealth() + " → " + 
                                ChatColor.GREEN + updatedStats.getMaxHealth());
            } else {
                player.sendMessage(ChatColor.RED + "Health: " + updatedStats.getMaxHealth());
            }
            
            if (baseStats.getPhysicalDamage() != updatedStats.getPhysicalDamage()) {
                player.sendMessage(ChatColor.RED + "Physical Damage: " + baseStats.getPhysicalDamage() + " → " + 
                                ChatColor.GREEN + updatedStats.getPhysicalDamage());
            } else {
                player.sendMessage(ChatColor.RED + "Physical Damage: " + updatedStats.getPhysicalDamage());
            }
            
            if (baseStats.getArmor() != updatedStats.getArmor()) {
                player.sendMessage(ChatColor.BLUE + "Armor: " + baseStats.getArmor() + " → " + 
                                ChatColor.GREEN + updatedStats.getArmor());
            } else {
                player.sendMessage(ChatColor.BLUE + "Armor: " + updatedStats.getArmor());
            }
            
            if (baseStats.getAttackRange() != updatedStats.getAttackRange()) {
                player.sendMessage(ChatColor.YELLOW + "Attack Range: " + baseStats.getAttackRange() + " → " + 
                                ChatColor.GREEN + updatedStats.getAttackRange());
            } else {
                player.sendMessage(ChatColor.YELLOW + "Attack Range: " + updatedStats.getAttackRange());
            }
            
            if (baseStats.getAttackSpeed() != updatedStats.getAttackSpeed()) {
                player.sendMessage(ChatColor.YELLOW + "Attack Speed: " + baseStats.getAttackSpeed() + " → " + 
                                ChatColor.GREEN + updatedStats.getAttackSpeed());
            } else {
                player.sendMessage(ChatColor.YELLOW + "Attack Speed: " + updatedStats.getAttackSpeed());
            }
            
            // Log detailed info to console
            plugin.debugLog(DebugSystem.NPC,"NPC " + id + " stats updated for item " + itemName + " in slot " + slotName);
            plugin.debugLog(DebugSystem.NPC,"  Physical Damage: " + baseStats.getPhysicalDamage() + " → " + updatedStats.getPhysicalDamage());
            plugin.debugLog(DebugSystem.NPC,"  Health: " + baseStats.getMaxHealth() + " → " + updatedStats.getMaxHealth());
            plugin.debugLog(DebugSystem.NPC,"  Armor: " + baseStats.getArmor() + " → " + updatedStats.getArmor());
            plugin.debugLog(DebugSystem.NPC,"  Attack Range: " + baseStats.getAttackRange() + " → " + updatedStats.getAttackRange());
            plugin.debugLog(DebugSystem.NPC,"  Attack Speed: " + baseStats.getAttackSpeed() + " → " + updatedStats.getAttackSpeed());
        }
        
        return true;
    }

    /**
     * Get a custom item by its name - fixed to use the CustomItems class
     */
    private ItemStack getCustomItemByName(String name) {
        // Use CustomItems class to get items by name
        String lowercaseName = name.toLowerCase();
        
        // Try to get the item using reflection to avoid hardcoding item names
        try {
            java.lang.reflect.Method method = CustomItems.class.getMethod("create" + capitalizeFirstLetter(lowercaseName));
            return (ItemStack) method.invoke(null);
        } catch (Exception e) {
            // If reflection fails, try common items directly
            switch (lowercaseName) {
                case "witchhat":
                    return CustomItems.createWitchHat();
                case "apprenticeedge":
                    return CustomItems.createApprenticeEdge();
                case "emberwood":
                case "emberwoodstaff":
                    return CustomItems.createEmberwoodStaff();
                case "arcloom":
                    return CustomItems.createArcloom();
                case "crownofmagnus":
                    return CustomItems.createCrownOfMagnus();
                case "siphonfang":
                    return CustomItems.createSiphonFang();
                case "fleshrake":
                    return CustomItems.createFleshrake();
                case "shatteredshell":
                case "shatteredshellpickaxe":
                    return CustomItems.createShatteredShellPickaxe();
                default:
                    // Try to match as a material name
                    Material material = Material.matchMaterial(name);
                    if (material != null) {
                        return new ItemStack(material);
                    }
                    return null;
            }
        }
    }

    /**
     * Helper method to capitalize first letter
     * 
     * @param input The input string
     * @return The string with first letter capitalized
     */
    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}