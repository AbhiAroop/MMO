package com.server.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.server.Main;
import com.server.entities.npc.DialogueHandler;
import com.server.entities.npc.DialogueResponse;
import com.server.entities.npc.DialogueTree;
import com.server.entities.npc.NPCManager;

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
        } else if (subCommand.equals("dialogue")) {
            return handleDialogue(player, args);
        } else if (subCommand.equals("list")) {
            return handleList(player, args);
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
            // Create combat training NPC
            double health = args.length > 5 ? Double.parseDouble(args[5]) : 100.0;
            double damage = args.length > 6 ? Double.parseDouble(args[6]) : 10.0;
            
            manager.createCombatNPC(id, name, player.getLocation(), skin, health, damage);
            player.sendMessage(ChatColor.GREEN + "Created combat NPC " + name + 
                            " with ID " + id + 
                            ", health: " + health + 
                            ", damage: " + damage);
        } 
        else {
            player.sendMessage(ChatColor.RED + "Unknown NPC type: " + type);
            player.sendMessage(ChatColor.GRAY + "Valid types: talk, combat");
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
        player.sendMessage(ChatColor.YELLOW + "/mmonpc create combat <id> [name] [skin] [health] [damage]" + ChatColor.WHITE + " - Create a combat NPC");
        player.sendMessage(ChatColor.YELLOW + "/mmonpc remove <id>" + ChatColor.WHITE + " - Remove an NPC");
        player.sendMessage(ChatColor.YELLOW + "/mmonpc list" + ChatColor.WHITE + " - List all NPCs");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Main commands
            List<String> commands = Arrays.asList("create", "remove", "list");
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
                List<String> types = Arrays.asList("talk", "combat");
                for (String type : types) {
                    if (type.startsWith(args[1].toLowerCase())) {
                        completions.add(type);
                    }
                }
            }
            else if (args[0].equalsIgnoreCase("remove")) {
                // NPC IDs
                NPCManager manager = NPCManager.getInstance();
                String input = args[1].toLowerCase();
                
                for (String id : manager.getIds()) {
                    if (id.toLowerCase().startsWith(input)) {
                        completions.add(id);
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
}