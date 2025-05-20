package com.server.entities.npc.story;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.entities.npc.dialogue.DialogueManager;
import com.server.entities.npc.dialogue.DialogueNode;
import com.server.entities.npc.dialogue.DialogueResponse;
import com.server.entities.npc.types.DialogueNPC;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;

/**
 * Kaelen the Echobound - The mysterious mentor figure who introduces players to the Echo system.
 */
public class KaelenEchobound extends DialogueNPC {
    
    private final Main plugin;
    private BukkitRunnable particleTask;
    
    /**
     * Create a new instance of Kaelen the Echobound
     */
    public KaelenEchobound() {
        super("kaelen_echobound", "§3Kaelen the Echobound", "dialogue_kaelen");
        this.plugin = Main.getInstance();
        
        // Setup the dialogue tree
        setupDialogueTree();
    }
    
    @Override
    public NPC spawn(Location location, String skin) {
        // Use the base spawn method
        NPC npc = super.spawn(location, skin != null ? skin : "Kaelen");
        
        // Add special metadata
        if (npc.isSpawned()) {
            npc.getEntity().setMetadata("story_npc", new FixedMetadataValue(plugin, true));
            npc.getEntity().setMetadata("invulnerable", new FixedMetadataValue(plugin, true));
            npc.getEntity().setMetadata("echobound", new FixedMetadataValue(plugin, true));
            npc.getEntity().setMetadata("mentor", new FixedMetadataValue(plugin, true));
            
            // CRITICAL FIX: Explicitly set the entity invulnerable
            npc.getEntity().setInvulnerable(true);
            
            // Set custom equipment
            Equipment equipment = npc.getOrAddTrait(Equipment.class);
            
            // Lumivine Staff - custom model blaze rod
            ItemStack staff = new ItemStack(Material.BLAZE_ROD);
            if (staff.getItemMeta() != null) {
                staff.getItemMeta().setCustomModelData(500001);
                equipment.set(Equipment.EquipmentSlot.HAND, staff);
            }
            
            // Robes - custom model leather chestplate
            ItemStack robes = new ItemStack(Material.LEATHER_CHESTPLATE);
            if (robes.getItemMeta() != null) {
                robes.getItemMeta().setCustomModelData(300001);
                equipment.set(Equipment.EquipmentSlot.CHESTPLATE, robes);
            }
            
            // Start the floating crystal particle effect
            startCrystalParticleEffect();
        }
        
        return npc;
    }
    
    /**
     * Setup dialogue tree for Kaelen
     */
    private void setupDialogueTree() {
        DialogueManager dialogueManager = DialogueManager.getInstance();
        
        // Main greeting dialogue
        DialogueNode mainGreeting = new DialogueNode("Ah… another one returns. Or perhaps… begins. It's never quite clear, is it? You are an Echo, yes? Bound to something… older.");
        
        // Responses for the main greeting
        mainGreeting.addResponse(new DialogueResponse("Echo? What does that mean?", "kaelen_echo_explanation"));
        mainGreeting.addResponse(new DialogueResponse("Where am I?", "kaelen_location_explanation"));
        mainGreeting.addResponse(new DialogueResponse("Do you know who I am?", "kaelen_identity_response"));
        mainGreeting.addResponse(new DialogueResponse("Can you help me?", "kaelen_help_offer"));
        mainGreeting.addResponse(new DialogueResponse("I'm ready. What now?", "kaelen_ready_response"));
        mainGreeting.addResponse(new DialogueResponse("What are those crystals floating around you?", "kaelen_crystals_explanation"));
        mainGreeting.addResponse(new DialogueResponse("Are you human?", "kaelen_human_response"));
        
        // Register the main greeting
        dialogueManager.registerDialogue("dialogue_kaelen", mainGreeting);
        
        // Register all the response dialogue nodes
        DialogueNode echoExplanation = new DialogueNode("You are tethered. When your shell breaks, you reform — drawn back by something within the stone. You don't remember, but the Island does.");
        echoExplanation.addResponse(new DialogueResponse("Tell me more about this island.", "kaelen_location_explanation"));
        echoExplanation.addResponse(new DialogueResponse("Back to the beginning", "dialogue_kaelen"));
        dialogueManager.registerDialogue("kaelen_echo_explanation", echoExplanation);
        
        DialogueNode locationExplanation = new DialogueNode("Isla Caelora. Once a beacon of peace, now humming with tension. Old magic, awakened tech, and fractured truths. But you, Echo… you might make sense of it.");
        locationExplanation.addResponse(new DialogueResponse("What is an Echo exactly?", "kaelen_echo_explanation"));
        locationExplanation.addResponse(new DialogueResponse("Back to the beginning", "dialogue_kaelen"));
        dialogueManager.registerDialogue("kaelen_location_explanation", locationExplanation);
        
        DialogueNode identityResponse = new DialogueNode("Names hold less power here. What matters is what you shape — in stone, in self, and in choice.");
        identityResponse.addResponse(new DialogueResponse("What choices do I have?", "kaelen_choices_explanation"));
        identityResponse.addResponse(new DialogueResponse("Back to the beginning", "dialogue_kaelen"));
        dialogueManager.registerDialogue("kaelen_identity_response", identityResponse);
        
        DialogueNode helpOffer = new DialogueNode("I offer sparks. You must kindle the flame. There are others — scattered across this island. Some remember more than I do.");
        helpOffer.addResponse(new DialogueResponse("Who should I find first?", "kaelen_ready_response"));
        helpOffer.addResponse(new DialogueResponse("Back to the beginning", "dialogue_kaelen"));
        dialogueManager.registerDialogue("kaelen_help_offer", helpOffer);
        
        DialogueNode readyResponse = new DialogueNode("Then walk. Find the Crystarch at the Hollow Spire — he speaks to stone and shadow. Perhaps he will give your path form.");
        readyResponse.addResponse(new DialogueResponse("Where is the Hollow Spire?", "kaelen_spire_location"));
        readyResponse.addResponse(new DialogueResponse("Who is the Crystarch?", "kaelen_crystarch_info"));
        readyResponse.addResponse(new DialogueResponse("Back to the beginning", "dialogue_kaelen"));
        dialogueManager.registerDialogue("kaelen_ready_response", readyResponse);
        
        DialogueNode crystalsExplanation = new DialogueNode("They are Echo Fragments — memories of those who came before. Quiet, mostly. But sometimes… they scream.");
        crystalsExplanation.addResponse(new DialogueResponse("Can I collect these fragments?", "kaelen_fragments_collection"));
        crystalsExplanation.addResponse(new DialogueResponse("Back to the beginning", "dialogue_kaelen"));
        dialogueManager.registerDialogue("kaelen_crystals_explanation", crystalsExplanation);
        
        DialogueNode humanResponse = new DialogueNode("I was. Now, I am an Echo who remembered too much.");
        humanResponse.addResponse(new DialogueResponse("What happens when you remember too much?", "kaelen_memory_danger"));
        humanResponse.addResponse(new DialogueResponse("Back to the beginning", "dialogue_kaelen"));
        dialogueManager.registerDialogue("kaelen_human_response", humanResponse);
        
        // Additional dialogue nodes for deeper conversations
        DialogueNode choicesExplanation = new DialogueNode("Your path is yours to forge. Some seek to control the Echo, others to harness it. Some wish to escape it entirely. The Island, its factions, its mysteries - all await your decision.");
        choicesExplanation.addResponse(new DialogueResponse("Tell me about these factions.", "kaelen_faction_explanation"));
        choicesExplanation.addResponse(new DialogueResponse("Back to the beginning", "dialogue_kaelen"));
        dialogueManager.registerDialogue("kaelen_choices_explanation", choicesExplanation);
        
        DialogueNode spireLocation = new DialogueNode("Journey east from here, past the ancient grove. When the path splits beneath the twin waterfalls, follow the one that climbs. The Hollow Spire rises from the mist - you cannot miss its silent call.");
        spireLocation.addResponse(new DialogueResponse("I'll find it.", "kaelen_farewell", player -> {
            // Play a sound effect for immersion
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 0.8f);
            player.sendMessage("§3A waypoint has been added to your map.");
            // In a real implementation, this would add a waypoint
        }));
        spireLocation.addResponse(new DialogueResponse("Back to the beginning", "dialogue_kaelen"));
        dialogueManager.registerDialogue("kaelen_spire_location", spireLocation);
        
        DialogueNode crystarchInfo = new DialogueNode("The Crystarch... ancient guardian of crystalline knowledge. Once human, now more stone than flesh. He communes with the very foundations of this island and guides those who seek to understand the deeper patterns.");
        crystarchInfo.addResponse(new DialogueResponse("Is he dangerous?", "kaelen_crystarch_danger"));
        crystarchInfo.addResponse(new DialogueResponse("Back to the beginning", "dialogue_kaelen"));
        dialogueManager.registerDialogue("kaelen_crystarch_info", crystarchInfo);
        
        // Additional nodes for completeness
        DialogueNode fragmentsCollection = new DialogueNode("Yes, Echo Fragments can be gathered throughout the island. Each holds memories, power, sometimes knowledge. Seek them, but be cautious - not all memories are meant to be reclaimed.");
        fragmentsCollection.addResponse(new DialogueResponse("Back to the beginning", "dialogue_kaelen"));
        dialogueManager.registerDialogue("kaelen_fragments_collection", fragmentsCollection);
        
        DialogueNode memoryDanger = new DialogueNode("When an Echo remembers too much... the tether stretches thin. Identity fractures. Some become something new - neither Echo nor original. Others... fade entirely. The balance is delicate.");
        memoryDanger.addResponse(new DialogueResponse("Back to the beginning", "dialogue_kaelen"));
        dialogueManager.registerDialogue("kaelen_memory_danger", memoryDanger);
        
        DialogueNode factionExplanation = new DialogueNode("The Crystalline Covenant seeks harmony with the Echo. The Residuum Collective works to harness it through ancient tech. The Veilwalkers believe we should break free from the cycle entirely. Each offers a path... and a price.");
        factionExplanation.addResponse(new DialogueResponse("Back to the beginning", "dialogue_kaelen"));
        dialogueManager.registerDialogue("kaelen_faction_explanation", factionExplanation);
        
        DialogueNode crystarchDanger = new DialogueNode("Not to those who approach with respect. But his wisdom can be... overwhelming. The weight of ages has made him cryptic. Listen carefully, and remember that even stone can speak in riddles.");
        crystarchDanger.addResponse(new DialogueResponse("Back to the beginning", "dialogue_kaelen"));
        dialogueManager.registerDialogue("kaelen_crystarch_danger", crystarchDanger);
        
        DialogueNode farewell = new DialogueNode("May your Echo resonate true, traveler. We shall speak again when you've glimpsed more of what awaits.");
        farewell.addResponse(new DialogueResponse("Goodbye", ""));
        dialogueManager.registerDialogue("kaelen_farewell", farewell);
    }
    
    @Override
    public void onInteract(Player player, NPC npc, boolean rightClick) {
        // CRITICAL FIX: Log interaction to debug
        plugin.debugLog(DebugSystem.NPC,"Player " + player.getName() + " interacted with Kaelen the Echobound (" + 
                            (rightClick ? "right" : "left") + " click)");
        
        // Play a mystical sound when player interacts
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.5f);
        
        // CRITICAL FIX: Explicitly pass the dialogue through the DialogueManager
        DialogueNode dialogue = DialogueManager.getInstance().getDialogue("dialogue_kaelen");
        if (dialogue != null) {
            DialogueManager.getInstance().startDialogue(player, this, dialogue);
        } else {
            // Fallback if dialogue not found
            sendMessage(player, "Ah... another Echo returns. We shall speak more when the time is right.");
            plugin.debugLog(DebugSystem.NPC,"Dialogue 'dialogue_kaelen' not found for Kaelen the Echobound");
        }
    }

    /**
     * Start the floating crystal particle effect
     */
    private void startCrystalParticleEffect() {
        if (particleTask != null) {
            particleTask.cancel();
        }
        
        particleTask = new BukkitRunnable() {
            double angle = 0;
            
            @Override
            public void run() {
                if (!isSpawned()) {
                    this.cancel();
                    return;
                }
                
                Location center = npc.getEntity().getLocation().add(0, 1.8, 0);
                
                // Create 3 orbiting crystals
                for (int i = 0; i < 3; i++) {
                    double currentAngle = angle + ((Math.PI * 2) / 3) * i;
                    double x = Math.cos(currentAngle) * 0.7;
                    double z = Math.sin(currentAngle) * 0.7;
                    double y = Math.sin(angle * 2) * 0.2; // Slight up/down movement
                    
                    Location crystalLoc = center.clone().add(x, y, z);
                    
                    // Crystal particle (End Rod gives a nice crystal effect)
                    npc.getEntity().getWorld().spawnParticle(
                        Particle.END_ROD, 
                        crystalLoc,
                        1, 0.02, 0.02, 0.02, 0.0
                    );
                    
                    // Add small dust particles around the crystal
                    npc.getEntity().getWorld().spawnParticle(
                        Particle.DUST_COLOR_TRANSITION,
                        crystalLoc,
                        2, 0.05, 0.05, 0.05, 0.0,
                        new Particle.DustTransition(
                            org.bukkit.Color.fromRGB(80, 190, 230), 
                            org.bukkit.Color.fromRGB(150, 100, 220), 
                            0.8f
                        )
                    );
                }
                
                // Occasionally make "whispering" ambient sound
                if (Math.random() < 0.03) { // 3% chance each tick
                    npc.getEntity().getWorld().playSound(
                        center,
                        Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                        0.2f,
                        0.5f + (float)Math.random() * 0.5f
                    );
                }
                
                angle += 0.05;
                if (angle > Math.PI * 2) {
                    angle = 0;
                }
            }
        };
        
        // Run the effect every 5 ticks
        particleTask.runTaskTimer(plugin, 0, 5);
    }
    
    public void cleanup() {
        
        // Clean up the particle task
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
    }
}