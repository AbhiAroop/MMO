package com.server.entities.npc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.server.Main;

import net.citizensnpcs.api.npc.NPC;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Handles dialogue interactions with NPCs
 */
public class DialogueHandler implements NPCInteractionHandler {
    
    private final DialogueTree dialogueTree;
    private final Map<UUID, String> playerCurrentNodes;
    
    /**
     * Create a new dialogue handler
     * 
     * @param dialogueTree The dialogue tree for this handler
     */
    public DialogueHandler(DialogueTree dialogueTree) {
        this.dialogueTree = dialogueTree;
        this.playerCurrentNodes = new HashMap<>();
    }
    
    @Override
    public void onInteract(Player player, NPC npc, boolean rightClick) {
        if (rightClick) {
            // Start or continue dialogue
            String currentNodeId = playerCurrentNodes.getOrDefault(player.getUniqueId(), null);
            
            if (currentNodeId == null) {
                // Start new dialogue with root node
                showDialogueNode(player, npc, dialogueTree.getRootNode());
            } else {
                // Continue existing dialogue
                DialogueNode currentNode = dialogueTree.getNode(currentNodeId);
                if (currentNode != null) {
                    showDialogueNode(player, npc, currentNode);
                } else {
                    // Invalid node, restart dialogue
                    playerCurrentNodes.remove(player.getUniqueId());
                    showDialogueNode(player, npc, dialogueTree.getRootNode());
                }
            }
        }
    }
    
    /**
     * Display a dialogue node to a player
     * 
     * @param player The player to show dialogue to
     * @param npc The NPC involved in the dialogue
     * @param node The dialogue node to show
     */
    private void showDialogueNode(Player player, NPC npc, DialogueNode node) {
        if (node == null) {
            endDialogue(player);
            return;
        }
        
        // Store the current node for this player
        playerCurrentNodes.put(player.getUniqueId(), node.getId());
        
        // Send the NPC dialogue text
        player.sendMessage(ChatColor.YELLOW + "[" + npc.getName() + "] " + ChatColor.WHITE + node.getText());
        
        // If there are responses, send them as clickable options
        if (node.hasResponses()) {
            player.sendMessage(ChatColor.GRAY + "Choose a response:");
            
            for (int i = 0; i < node.getResponses().size(); i++) {
                DialogueResponse response = node.getResponses().get(i);
                
                // Create a clickable response option
                TextComponent message = new TextComponent("  → " + response.getText());
                message.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                        "/npc dialogue " + npc.getId() + " " + i));
                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        new ComponentBuilder("Click to select this response").create()));
                
                player.spigot().sendMessage(message);
            }
        } else {
            // No responses, end dialogue
            endDialogue(player);
        }
    }
    
    /**
     * Handle a player selecting a dialogue response
     * 
     * @param player The player who selected the response
     * @param npc The NPC involved in the dialogue
     * @param responseIndex The index of the response selected
     */
    public void handleResponse(Player player, NPC npc, int responseIndex) {
        String currentNodeId = playerCurrentNodes.get(player.getUniqueId());
        if (currentNodeId == null) return;
        
        DialogueNode currentNode = dialogueTree.getNode(currentNodeId);
        if (currentNode == null || !currentNode.hasResponses() || 
                responseIndex < 0 || responseIndex >= currentNode.getResponses().size()) {
            return;
        }
        
        // Get the selected response
        DialogueResponse response = currentNode.getResponses().get(responseIndex);
        
        // Show the player's response
        player.sendMessage(ChatColor.GREEN + "[You] " + ChatColor.WHITE + response.getText());
        
        // Execute any action associated with this response
        if (response.hasAction()) {
            response.executeAction(player);
        }
        
        // Move to the next node if there is one
        String nextNodeId = response.getNextNodeId();
        if (nextNodeId != null) {
            showDialogueNode(player, npc, dialogueTree.getNode(nextNodeId));
        } else {
            // No next node, end dialogue
            endDialogue(player);
        }
    }
    
    /**
     * End dialogue with a player
     * 
     * @param player The player to end dialogue with
     */
    private void endDialogue(Player player) {
        playerCurrentNodes.remove(player.getUniqueId());
    }
}