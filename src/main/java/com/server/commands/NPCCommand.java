package com.server.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import com.server.Main;
import com.server.entities.npc.CombatHandler;
import com.server.entities.npc.DialogueHandler;
import com.server.entities.npc.DialogueResponse;
import com.server.entities.npc.DialogueTree;
import com.server.entities.npc.NPCInteractionHandler;
import com.server.entities.npc.NPCManager;
import com.server.entities.npc.NPCStats;

import net.citizensnpcs.api.npc.NPC;

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
            sender.sendMessage(ChatColor.RED + "This command can only be used by players");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("mmo.admin.npc")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command");
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
        } else {
            sendHelp(player);
            return true;
        }
    }
    
    /**
     * Handle the 'create' subcommand
     */
    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /mmonpc create <type> <id> [name] [skinName]");
            player.sendMessage(ChatColor.GRAY + "Types: talk, combat");
            return true;
        }
        
        String type = args[1].toLowerCase();
        String id = args[2];
        String name = args.length > 3 ? args[3] : id;
        String skin = args.length > 4 ? args[4] : name;
        
        NPCManager manager = NPCManager.getInstance();
        
        if (type.equals("talk")) {
            // Create simple talking NPC with basic dialogue
            DialogueTree.Builder builder = new DialogueTree.Builder("root");
            
            List<DialogueResponse> responses = new ArrayList<>();
            responses.add(new DialogueResponse("Hello there!", "greeting"));
            responses.add(new DialogueResponse("What do you do here?", "role"));
            responses.add(new DialogueResponse("Goodbye.", "goodbye"));
            
            builder.addNode("root", "Hello! How can I help you today?", responses);
            
            responses = new ArrayList<>();
            responses.add(new DialogueResponse("Nice to meet you!", "nice"));
            responses.add(new DialogueResponse("I'll be going now.", "goodbye"));
            builder.addNode("greeting", "It's a pleasure to meet you as well! My name is " + name + ".", responses);
            
            builder.addNode("role", "I'm here to provide information and assistance to adventurers like yourself.", 
                Arrays.asList(new DialogueResponse("That's great.", "root")));
            
            builder.addNode("nice", "The pleasure is all mine! Is there anything else I can help you with?",
                Arrays.asList(new DialogueResponse("Return to topics", "root")));
            
            builder.addNode("goodbye", "Farewell! Come back if you need anything else.", new ArrayList<>());
            
            DialogueTree dialogueTree = builder.build();
            
            manager.createTalkingNPC(id, name, player.getLocation(), skin, dialogueTree);
            player.sendMessage(ChatColor.GREEN + "Created talking NPC " + name + " with ID " + id);
        } 
        else if (type.equals("combat")) {
            // Create combat training NPC that only targets players
            double health = args.length > 5 ? Double.parseDouble(args[5]) : 100.0;
            double damage = args.length > 6 ? Double.parseDouble(args[6]) : 10.0;
            
            // Create customized stats object
            NPCStats customStats = new NPCStats();
            customStats.setMaxHealth(health);
            customStats.setPhysicalDamage((int)damage);
            
            // Create NPC with custom stats
            NPC npc = manager.createCombatNPC(id, name, player.getLocation(), skin, customStats);

            // IMPORTANT: Store ALL critical stats in metadata for persistence
            npc.getEntity().setMetadata("max_health", new FixedMetadataValue(Main.getInstance(), health));
            npc.getEntity().setMetadata("physical_damage", new FixedMetadataValue(Main.getInstance(), damage));
            npc.getEntity().setMetadata("targets_players", new FixedMetadataValue(Main.getInstance(), true));
            npc.getEntity().setMetadata("targets_npcs", new FixedMetadataValue(Main.getInstance(), false));
            npc.getEntity().setMetadata("npc_type", new FixedMetadataValue(Main.getInstance(), "NORMAL"));
            
            // Configure it to target only players
            CombatHandler handler = (CombatHandler) manager.getInteractionHandler(id);
            handler.setTargetsPlayers(true);
            handler.setTargetsNPCs(false);
            
            player.sendMessage(ChatColor.GREEN + "Created combat NPC " + name + 
                            " with ID " + id + 
                            ", health: " + health + 
                            ", damage: " + damage + 
                            " (targets only players)");
        }
        // In the NPCCommand class's handleCreate method
        else if (type.equals("hostile")) {
            // Create hostile NPC that can target both players and other NPCs
            double health = args.length > 5 ? Double.parseDouble(args[5]) : 100.0;
            double damage = args.length > 6 ? Double.parseDouble(args[6]) : 10.0;
            
            // Create NPC with standard stats
            NPCStats customStats = new NPCStats();
            customStats.setMaxHealth(health);
            customStats.setPhysicalDamage((int)damage);
            
            // Apply any other custom stat settings as needed
            // For example: customStats.setArmor(10);
            
            NPC npc = manager.createCombatNPC(id, name, player.getLocation(), skin, customStats);
            
            // IMPORTANT: Store ALL critical stats in metadata for persistence across respawns
            npc.getEntity().setMetadata("max_health", new FixedMetadataValue(Main.getInstance(), health));
            npc.getEntity().setMetadata("physical_damage", new FixedMetadataValue(Main.getInstance(), damage));
            
            // Set NPC type and targeting settings
            npc.getEntity().setMetadata("targets_players", new FixedMetadataValue(Main.getInstance(), true));
            npc.getEntity().setMetadata("targets_npcs", new FixedMetadataValue(Main.getInstance(), true));
            npc.getEntity().setMetadata("npc_type", new FixedMetadataValue(Main.getInstance(), "HOSTILE"));
            
            // Configure it to target both players and NPCs
            CombatHandler handler = (CombatHandler) manager.getInteractionHandler(id);
            handler.setTargetsPlayers(true);
            handler.setTargetsNPCs(true);
            
            player.sendMessage(ChatColor.GREEN + "Created hostile NPC " + name + 
                            " with ID " + id + 
                            ", health: " + health + 
                            ", damage: " + damage +
                            " (targets players AND other NPCs)");
        }
        else {
            player.sendMessage(ChatColor.RED + "Unknown NPC type: " + type);
            player.sendMessage(ChatColor.GRAY + "Valid types: talk, combat, hostile");
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
            player.sendMessage(ChatColor.GREEN + "Removed NPC with ID " + id);
        } else {
            player.sendMessage(ChatColor.RED + "No NPC found with ID " + id);
        }
        
        return true;
    }
    
    /**
     * Handle the 'dialogue' subcommand - used by dialogue click events
     */
    private boolean handleDialogue(Player player, String[] args) {
        if (args.length < 3) {
            return true;
        }
        
        try {
            int npcId = Integer.parseInt(args[1]);
            int responseIndex = Integer.parseInt(args[2]);
            
            NPCManager manager = NPCManager.getInstance();
            NPC npc = null;
            
            // Find the NPC by its Citizens ID
            for (NPC n : manager.getNPCRegistry()) {
                if (n.getId() == npcId) {
                    npc = n;
                    break;
                }
            }
            
            if (npc == null) {
                return true;
            }
            
            // Get the handler for this NPC
            for (String id : manager.getIds()) {
                if (manager.getNPC(id) == npc) {
                    if (manager.getInteractionHandler(id) instanceof DialogueHandler) {
                        DialogueHandler handler = (DialogueHandler) manager.getInteractionHandler(id);
                        handler.handleResponse(player, npc, responseIndex);
                        return true;
                    }
                }
            }
        } catch (NumberFormatException e) {
            // Silently ignore invalid numbers
        }
        
        return true;
    }
    
    /**
     * Handle the 'list' subcommand
     */
    private boolean handleList(Player player, String[] args) {
        NPCManager manager = NPCManager.getInstance();
        List<String> npcIds = manager.getIds();
        
        if (npcIds.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No NPCs have been created yet.");
            return true;
        }
        
        player.sendMessage(ChatColor.YELLOW + "NPCs (" + npcIds.size() + "):");
        for (String id : npcIds) {
            NPC npc = manager.getNPC(id);
            if (npc != null) {
                String status = npc.isSpawned() ? ChatColor.GREEN + "Spawned" : ChatColor.RED + "Despawned";
                player.sendMessage(ChatColor.GOLD + " - " + id + ": " + ChatColor.WHITE + npc.getName() + 
                        " (" + status + ChatColor.WHITE + ")");
            }
        }
        
        return true;
    }
    
    /**
     * Send help message to a player
     */
    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== MMO NPC Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/mmonpc create talk <id> [name] [skin]" + ChatColor.WHITE + " - Create a talking NPC");
        player.sendMessage(ChatColor.YELLOW + "/mmonpc create combat <id> [name] [skin] [health] [damage]" + ChatColor.WHITE + " - Create a combat NPC (targets players)");
        player.sendMessage(ChatColor.YELLOW + "/mmonpc create hostile <id> [name] [skin] [health] [damage]" + ChatColor.WHITE + " - Create a hostile NPC (targets players AND NPCs)");
        player.sendMessage(ChatColor.YELLOW + "/mmonpc remove <id>" + ChatColor.WHITE + " - Remove an NPC");
        player.sendMessage(ChatColor.YELLOW + "/mmonpc removeall [radius]" + ChatColor.WHITE + " - Remove all NPCs or those within radius");
        player.sendMessage(ChatColor.YELLOW + "/mmonpc equip <id> <slot> <item>" + ChatColor.WHITE + " - Equip an item on an NPC");
        player.sendMessage(ChatColor.YELLOW + "/mmonpc list" + ChatColor.WHITE + " - List all NPCs");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Main commands
            List<String> commands = Arrays.asList("create", "remove", "removeall", "list", "equip");
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
                // NPC types - add "hostile"
                List<String> types = Arrays.asList("talk", "combat", "hostile");
                for (String type : types) {
                    if (type.startsWith(args[1].toLowerCase())) {
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
        List<String> npcIds = manager.getIds();
        
        if (npcIds.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No NPCs found to remove.");
            return true;
        }
        
        // Check if a radius was specified
        double radius = -1; // Default: no radius limit (remove all)
        if (args.length > 1) {
            try {
                radius = Double.parseDouble(args[1]);
                if (radius <= 0) {
                    player.sendMessage(ChatColor.RED + "Radius must be a positive number.");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid radius. Usage: /mmonpc removeall [radius]");
                return true;
            }
        }
        
        int removedCount = 0;
        List<String> toRemove = new ArrayList<>();
        
        // Gather NPCs to remove
        for (String id : npcIds) {
            NPC npc = manager.getNPC(id);
            if (npc == null) continue;
            
            // If radius specified, check distance
            if (radius > 0) {
                if (!npc.isSpawned()) continue; // Skip despawned NPCs
                
                Location npcLoc = npc.getEntity().getLocation();
                double distance = player.getLocation().distance(npcLoc);
                
                if (distance <= radius) {
                    toRemove.add(id);
                }
            } else {
                // No radius, remove all
                toRemove.add(id);
            }
        }
        
        // Now remove the NPCs
        for (String id : toRemove) {
            manager.removeNPC(id);
            removedCount++;
        }
        
        // Send feedback message
        if (radius > 0) {
            player.sendMessage(ChatColor.GREEN + "Removed " + removedCount + " NPCs within " + 
                            String.format("%.1f", radius) + " blocks.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Removed all " + removedCount + " NPCs.");
        }
        
        return true;
    }

    /**
     * Handle the 'equip' subcommand - gives equipment to an NPC
     */
    private boolean handleEquip(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: /mmonpc equip <id> <slot> <item>");
            player.sendMessage(ChatColor.GRAY + "Slots: mainhand, offhand, helmet, chestplate, leggings, boots");
            player.sendMessage(ChatColor.GRAY + "Items: Use item names from /giveitem command");
            return true;
        }
        
        String id = args[1];
        String slotName = args[2].toLowerCase();
        String itemName = args[3].toLowerCase();
        
        NPCManager manager = NPCManager.getInstance();
        NPC npc = manager.getNPC(id);
        
        if (npc == null) {
            player.sendMessage(ChatColor.RED + "No NPC found with ID " + id);
            return true;
        }
        
        // Convert slot name to equipment slot
        net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot slot;
        switch (slotName) {
            case "mainhand":
                slot = net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot.HAND;
                break;
            case "offhand":
                slot = net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot.OFF_HAND;
                break;
            case "helmet":
                slot = net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot.HELMET;
                break;
            case "chestplate":
                slot = net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot.CHESTPLATE;
                break;
            case "leggings":
                slot = net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot.LEGGINGS;
                break;
            case "boots":
                slot = net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot.BOOTS;
                break;
            default:
                player.sendMessage(ChatColor.RED + "Invalid slot: " + slotName);
                player.sendMessage(ChatColor.GRAY + "Valid slots: mainhand, offhand, helmet, chestplate, leggings, boots");
                return true;
        }
        
        // Get the item from the custom items registry
        ItemStack item = getCustomItemByName(itemName);
        
        if (item == null) {
            player.sendMessage(ChatColor.RED + "Unknown item: " + itemName);
            player.sendMessage(ChatColor.GRAY + "Use /giveitem to see available items");
            return true;
        }
        
        // Equip the item on the NPC
        manager.setEquipment(id, slot, item);
        
        // Update NPC stats if it's a combat NPC
        NPCInteractionHandler handler = manager.getInteractionHandler(id);
        if (handler instanceof CombatHandler) {
            CombatHandler combatHandler = (CombatHandler) handler;
            UUID npcUuid = npc.getUniqueId();
            NPCStats stats = combatHandler.getNPCStats(npcUuid);
            
            // Apply item stats to NPC
            applyItemStatsToNPC(stats, item, slot);
            
            // Update the NPC with the new stats
            combatHandler.setNPCStats(npcUuid, stats);
            
            // Update nameplate to reflect new stats if NPC is spawned
            if (npc.isSpawned()) {
                manager.updateNameplate(npc, combatHandler.getNpcHealth().getOrDefault(npcUuid, stats.getMaxHealth()), 
                        stats.getMaxHealth());
            }
        }
        
        player.sendMessage(ChatColor.GREEN + "Equipped " + ChatColor.YELLOW + item.getItemMeta().getDisplayName() + 
                        ChatColor.GREEN + " on " + ChatColor.YELLOW + npc.getName() + 
                        ChatColor.GREEN + " (" + slot.name().toLowerCase().replace("_", " ") + ")");
        
        return true;
    }

    /**
     * Get a custom item by its name
     */
    private ItemStack getCustomItemByName(String name) {
        switch (name.toLowerCase()) {
            case "witchhat":
                return com.server.items.CustomItems.createWitchHat();
            case "apprenticeedge":
                return com.server.items.CustomItems.createApprenticeEdge();
            case "emberwood":
                return com.server.items.CustomItems.createEmberwoodStaff();
            case "arcloom":
                return com.server.items.CustomItems.createArcloom();
            case "crownofmagnus":
                return com.server.items.CustomItems.createCrownOfMagnus();
            case "siphonfang":
                return com.server.items.CustomItems.createSiphonFang();
            case "fleshrake":
                return com.server.items.CustomItems.createFleshrake();
            case "shatteredshell":
                return com.server.items.CustomItems.createShatteredShellPickaxe();
            default:
                return null;
        }
    }

    /**
     * Apply item stats to NPC based on the item's lore
     */
    private void applyItemStatsToNPC(NPCStats stats, ItemStack item, net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot slot) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return;
        
        for (String loreLine : item.getItemMeta().getLore()) {
            String cleanLine = ChatColor.stripColor(loreLine).trim();
            
            // Physical damage (weapon)
            if (cleanLine.startsWith("Physical Damage: +")) {
                try {
                    int damage = Integer.parseInt(cleanLine.substring("Physical Damage: +".length()));
                    stats.setPhysicalDamage(stats.getPhysicalDamage() + damage);
                } catch (NumberFormatException e) {
                    // Ignore parse errors
                }
            }
            
            // Health (armor)
            else if (cleanLine.startsWith("Health: +")) {
                try {
                    int health = Integer.parseInt(cleanLine.substring("Health: +".length()));
                    stats.setMaxHealth(stats.getMaxHealth() + health);
                } catch (NumberFormatException e) {
                    // Ignore parse errors
                }
            }
            
            // Armor (armor)
            else if (cleanLine.startsWith("Armor: +")) {
                try {
                    int armor = Integer.parseInt(cleanLine.substring("Armor: +".length()));
                    stats.setArmor(stats.getArmor() + armor);
                } catch (NumberFormatException e) {
                    // Ignore parse errors
                }
            }
            
            // Magic resist (armor)
            else if (cleanLine.startsWith("Magic Resist: +")) {
                try {
                    int resist = Integer.parseInt(cleanLine.substring("Magic Resist: +".length()));
                    stats.setMagicResist(stats.getMagicResist() + resist);
                } catch (NumberFormatException e) {
                    // Ignore parse errors
                }
            }
            
            // Magic damage (weapon)
            else if (cleanLine.startsWith("Magic Damage: +")) {
                try {
                    int magicDamage = Integer.parseInt(cleanLine.substring("Magic Damage: +".length()));
                    stats.setMagicDamage(stats.getMagicDamage() + magicDamage);
                } catch (NumberFormatException e) {
                    // Ignore parse errors
                }
            }
        }
    }
}