package com.server.entities.npc.dialogue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.entities.npc.types.BaseNPC;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Manages dialogues for NPCs
 */
public class DialogueManager {
    
    private static DialogueManager instance;
    private final Main plugin;
    private final Map<String, DialogueNode> dialogues = new HashMap<>();
    private final Map<UUID, DialogueState> playerDialogues = new HashMap<>();
    
    /**
     * Create a new DialogueManager
     * 
     * @param plugin The plugin instance
     */
    private DialogueManager(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Get the DialogueManager instance
     * 
     * @return The instance
     */
    public static DialogueManager getInstance() {
        if (instance == null) {
            instance = new DialogueManager(Main.getInstance());
        }
        return instance;
    }
    
    /**
     * Register a dialogue
     * 
     * @param id The unique ID
     * @param dialogue The dialogue node
     */
    public void registerDialogue(String id, DialogueNode dialogue) {
        dialogues.put(id, dialogue);
    }
    
    /**
     * Get a dialogue by ID
     * 
     * @param id The dialogue ID
     * @return The dialogue node, or null if not found
     */
    public DialogueNode getDialogue(String id) {
        return dialogues.get(id);
    }
    
    /**
     * Check if a dialogue exists
     * 
     * @param id The dialogue ID
     * @return True if the dialogue exists
     */
    public boolean hasDialogue(String id) {
        return dialogues.containsKey(id);
    }
    
    /**
     * Start a dialogue with a player
     * 
     * @param player The player
     * @param npc The NPC
     * @param dialogue The dialogue node
     */
    public void startDialogue(Player player, BaseNPC npc, DialogueNode dialogue) {
        UUID playerId = player.getUniqueId();
        
        // Clean up any existing dialogue
        endDialogue(playerId);
        
        // Create a new dialogue state
        DialogueState state = new DialogueState(npc, dialogue);
        playerDialogues.put(playerId, state);
        
        // Show the dialogue to the player
        showDialogue(player, state);
    }
    
    /**
     * Show a dialogue to a player
     * 
     * @param player The player
     * @param state The dialogue state
     */
    private void showDialogue(Player player, DialogueState state) {
        DialogueNode dialogue = state.getCurrentDialogue();
        BaseNPC npc = state.getNpc();
        
        // Display NPC name and dialogue text
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "--- " + ChatColor.YELLOW + npc.getName() + ChatColor.GOLD + " ---");
        player.sendMessage(ChatColor.WHITE + dialogue.getText());
        
        // If there are responses, display them as clickable options
        if (dialogue.hasResponses()) {
            player.sendMessage(ChatColor.GOLD + "--- " + ChatColor.YELLOW + "Responses" + ChatColor.GOLD + " ---");
            
            int index = 0;
            for (DialogueResponse response : dialogue.getResponses()) {
                TextComponent component = new TextComponent(ChatColor.GREEN + "[" + (index + 1) + "] " + ChatColor.WHITE + response.getText());
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    new ComponentBuilder(ChatColor.YELLOW + "Click to select this response").create()));
                component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                    "/mmonpc dialogue " + player.getUniqueId() + " " + index));
                
                player.spigot().sendMessage(component);
                index++;
            }
        } else {
            // If there are no responses, end the dialogue after a delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    endDialogue(player.getUniqueId());
                }
            }.runTaskLater(plugin, 60L); // 3 seconds
        }
    }
    
    /**
     * Handle a player selecting a response
     * 
     * @param player The player
     * @param responseIndex The index of the selected response
     */
    public void handleResponse(Player player, int responseIndex) {
        UUID playerId = player.getUniqueId();
        DialogueState state = playerDialogues.get(playerId);
        
        if (state == null) {
            // No active dialogue
            if (plugin.isDebugEnabled(DebugSystem.DIALOGUE)) {
                plugin.debugLog(DebugSystem.DIALOGUE,"Player " + player.getName() + 
                                        " tried to select dialogue response but has no active dialogue");
            }
            return;
        }
        
        DialogueNode dialogue = state.getCurrentDialogue();
        if (!dialogue.hasResponses() || responseIndex < 0 || responseIndex >= dialogue.getResponses().size()) {
            // Invalid response index
            if (plugin.isDebugEnabled(DebugSystem.DIALOGUE)) {
                plugin.debugLog(DebugSystem.DIALOGUE,"Invalid dialogue response index " + responseIndex + 
                                        " for player " + player.getName() + ". Available responses: " + 
                                        dialogue.getResponses().size());
            }
            return;
        }
        
        // Get the selected response
        DialogueResponse response = dialogue.getResponses().get(responseIndex);
        
        // Execute the response action if there is one
        if (response.hasAction()) {
            response.executeAction(player);
        }
        
        // Get the next dialogue node
        String nextNodeId = response.getNextNodeId();
        if (nextNodeId == null || nextNodeId.isEmpty() || !dialogues.containsKey(nextNodeId)) {
            // End the dialogue if there's no next node
            if (plugin.isDebugEnabled(DebugSystem.DIALOGUE)) {
                plugin.debugLog(DebugSystem.DIALOGUE,"Ending dialogue for player " + player.getName() + 
                                    " - no next dialogue node defined for response: " + response.getText());
            }
            endDialogue(playerId);
            return;
        }
        
        // Update the dialogue state
        state.setCurrentDialogue(dialogues.get(nextNodeId));
        
        // Show the next dialogue
        showDialogue(player, state);
    }
    
    /**
     * End a dialogue for a player
     * 
     * @param playerId The player's UUID
     */
    public void endDialogue(UUID playerId) {
        playerDialogues.remove(playerId);
    }
    
    /**
     * Class representing the state of a dialogue for a player
     */
    private static class DialogueState {
        private final BaseNPC npc;
        private DialogueNode currentDialogue;
        
        public DialogueState(BaseNPC npc, DialogueNode dialogue) {
            this.npc = npc;
            this.currentDialogue = dialogue;
        }
        
        public BaseNPC getNpc() {
            return npc;
        }
        
        public DialogueNode getCurrentDialogue() {
            return currentDialogue;
        }
        
        public void setCurrentDialogue(DialogueNode dialogue) {
            this.currentDialogue = dialogue;
        }
    }
}