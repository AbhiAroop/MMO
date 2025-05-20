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
 * Nell Mossgleam - The tutorial NPC / Island Helper who guides new players
 */
public class NellMossgleam extends DialogueNPC {
    
    private final Main plugin;
    private BukkitRunnable particleTask;
    
    /**
     * Create a new instance of Nell Mossgleam
     */
    public NellMossgleam() {
        super("nell_mossgleam", "§aNell Mossgleam", "dialogue_nell");
        this.plugin = Main.getInstance();
        
        // Setup the dialogue tree
        setupDialogueTree();
    }
    
    @Override
    public NPC spawn(Location location, String skin) {
        // Use the base spawn method
        NPC npc = super.spawn(location, skin != null ? skin : "NellMossgleam");
        
        // Add special metadata
        if (npc.isSpawned()) {
            npc.getEntity().setMetadata("story_npc", new FixedMetadataValue(plugin, true));
            npc.getEntity().setMetadata("invulnerable", new FixedMetadataValue(plugin, true));
            npc.getEntity().setMetadata("tutorial_npc", new FixedMetadataValue(plugin, true));
            npc.getEntity().setMetadata("gnome", new FixedMetadataValue(plugin, true));
            
            // CRITICAL FIX: Explicitly set the entity invulnerable
            npc.getEntity().setInvulnerable(true);
            
            // Set custom equipment
            Equipment equipment = npc.getOrAddTrait(Equipment.class);
            
            // Multi-tool staff - custom model blaze rod
            ItemStack staff = new ItemStack(Material.BLAZE_ROD);
            if (staff.getItemMeta() != null) {
                staff.getItemMeta().setCustomModelData(500002);
                equipment.set(Equipment.EquipmentSlot.HAND, staff);
            }
            
            // Toolbelt - custom model leather leggings
            ItemStack toolbelt = new ItemStack(Material.LEATHER_LEGGINGS);
            if (toolbelt.getItemMeta() != null) {
                toolbelt.getItemMeta().setCustomModelData(300002);
                equipment.set(Equipment.EquipmentSlot.LEGGINGS, toolbelt);
            }
            
            // Oversized gloves - custom model leather helmet
            ItemStack gloves = new ItemStack(Material.LEATHER_HELMET);
            if (gloves.getItemMeta() != null) {
                gloves.getItemMeta().setCustomModelData(300003);
                equipment.set(Equipment.EquipmentSlot.HELMET, gloves);
            }
            
            // Start the mossy particle effect
            startMossyParticleEffect();
        }
        
        return npc;
    }
    
    /**
     * Setup dialogue tree for Nell
     */
    private void setupDialogueTree() {
        DialogueManager dialogueManager = DialogueManager.getInstance();
        
        // Main greeting dialogue
        DialogueNode mainGreeting = new DialogueNode("Oi! You blinkin'? Good! You Echo-types always look a bit foggy on the first few steps. Don't worry — I'll not bury you in scrolls. Just a little push to get you upright.");
        
        // Responses for the main greeting
        mainGreeting.addResponse(new DialogueResponse("What should I do first?", "nell_first_steps"));
        mainGreeting.addResponse(new DialogueResponse("Where am I exactly?", "nell_location_explanation"));
        mainGreeting.addResponse(new DialogueResponse("What's an Echo?", "nell_echo_explanation"));
        mainGreeting.addResponse(new DialogueResponse("How do I use skills?", "nell_skills_explanation"));
        mainGreeting.addResponse(new DialogueResponse("I'm lost.", "nell_lost_response"));
        mainGreeting.addResponse(new DialogueResponse("You smell like moss.", "nell_moss_comment"));
        mainGreeting.addResponse(new DialogueResponse("Did you build all this?", "nell_build_question"));
        mainGreeting.addResponse(new DialogueResponse("Can I have some starter gear?", "nell_starter_gear"));
        
        // Register the main greeting
        dialogueManager.registerDialogue("dialogue_nell", mainGreeting);
        
        // Register all the response dialogue nodes
        DialogueNode firstSteps = new DialogueNode("Pick something shiny or noisy — gathering, mining, whackin' things, doesn't matter. Just do. There's an open crate behind me, take what you like. Everything else'll follow.");
        firstSteps.addResponse(new DialogueResponse("Thanks, I'll get started.", "nell_farewell"));
        firstSteps.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_first_steps", firstSteps);
        
        DialogueNode locationExplanation = new DialogueNode("Verdant Docks. Edge of the Caeloran Isle. Used to be a trade outpost, now more of a… sleepy home for half-remembered folks like me.");
        locationExplanation.addResponse(new DialogueResponse("Tell me more about Caeloran Isle.", "nell_isle_explanation"));
        locationExplanation.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_location_explanation", locationExplanation);
        
        DialogueNode echoExplanation = new DialogueNode("Means you come back when you're smushed. Don't ask me how. Kaelen says it's ancient tethering. I say it's useful.");
        echoExplanation.addResponse(new DialogueResponse("Who is Kaelen?", "nell_kaelen_reference"));
        echoExplanation.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_echo_explanation", echoExplanation);
        
        DialogueNode skillsExplanation = new DialogueNode("Doing's learning. Chop wood — get better at choppin'. Mine a gem — maybe you won't break it next time. You'll notice. There's a skill journal if you press your… whatever-key-thing.");
        skillsExplanation.addResponse(new DialogueResponse("Can I see this skill journal?", "nell_skill_journal", player -> {
            // Play UI opening sound
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.0f);
            
            // Send info message to player about how to open skills menu
            player.sendMessage("§aSkill Journal: §7Press §f[J] §7or type §f/skills §7to open your skill journal.");
            
            // Command would normally be here to open the skill UI
            // plugin.getServer().dispatchCommand(player, "skills");
        }));
        skillsExplanation.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_skills_explanation", skillsExplanation);
        
        DialogueNode lostResponse = new DialogueNode("That's normal. Explore. Hit things. Break rocks. If it glows or hums weird, maybe don't lick it.");
        lostResponse.addResponse(new DialogueResponse("Any particular direction I should head?", "nell_direction_advice"));
        lostResponse.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_lost_response", lostResponse);
        
        DialogueNode mossComment = new DialogueNode("It's part of the charm, darling.");
        mossComment.addResponse(new DialogueResponse("It's... nice?", "nell_moss_compliment"));
        mossComment.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_moss_comment", mossComment);
        
        DialogueNode buildQuestion = new DialogueNode("Me, a goat, and a lot of angry mushrooms, yes.");
        buildQuestion.addResponse(new DialogueResponse("Angry... mushrooms?", "nell_mushrooms_story"));
        buildQuestion.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_build_question", buildQuestion);
        
        DialogueNode starterGear = new DialogueNode("'Course you can! Check the crate behind me. Take your pick - there's gathering tools, mining picks, or a sturdy knife if you fancy some combat.");
        starterGear.addResponse(new DialogueResponse("I'd like a pickaxe for mining.", "nell_give_pickaxe", player -> {
            // Give the player a basic pickaxe
            player.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));
            player.sendMessage("§aNell gives you a basic pickaxe!");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
        }));
        starterGear.addResponse(new DialogueResponse("I'd like a fishing rod for gathering.", "nell_give_fishing_rod", player -> {
            // Give the player a fishing rod
            player.getInventory().addItem(new ItemStack(Material.FISHING_ROD));
            player.sendMessage("§aNell gives you a basic fishing rod!");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
        }));
        starterGear.addResponse(new DialogueResponse("I'd like a knife for combat.", "nell_give_knife", player -> {
            // Give the player a basic knife (wooden sword)
            player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
            player.sendMessage("§aNell gives you a basic knife!");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
        }));
        starterGear.addResponse(new DialogueResponse("I'll check the crate myself.", "nell_check_crate"));
        starterGear.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_starter_gear", starterGear);
        
        // Additional dialogue nodes
        DialogueNode isleExplanation = new DialogueNode("Caelora... she's a strange one. Island sits on ancient magic. Some say she's alive. Has moods. The more you sync with her rhythms, the more she gives you.");
        isleExplanation.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_isle_explanation", isleExplanation);
        
        DialogueNode kaelenReference = new DialogueNode("Tall fella, one eye all glowy, wears fancy robes? He's up at the overlook. Bit dramatic for my taste, but knows his stuff about Echoes. Been here longer than anyone.");
        kaelenReference.addResponse(new DialogueResponse("I think I met him already.", "nell_kaelen_met"));
        kaelenReference.addResponse(new DialogueResponse("I'll look for him later.", "nell_kaelen_later"));
        kaelenReference.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_kaelen_reference", kaelenReference);
        
        DialogueNode kaelenMet = new DialogueNode("Did ya now? And did he do that thing where he talks all mystical-like about your 'journey' and 'purpose'? HA! Classic Kaelen.");
        kaelenMet.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_kaelen_met", kaelenMet);
        
        DialogueNode kaelenLater = new DialogueNode("Can't miss him. Just follow the floating crystals and cryptic statements.");
        kaelenLater.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_kaelen_later", kaelenLater);
        
        DialogueNode skillJournal = new DialogueNode("There ya go! It gets fancier as you level up skills. Each one's got its own perks and little tricks. Don't worry about picking wrong - experiment!");
        skillJournal.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_skill_journal", skillJournal);
        
        DialogueNode directionAdvice = new DialogueNode("Well, the settlement's just northeast, if you need supplies or folks. East leads to the mining caves. South takes you to the forest groves for gathering. West is the training grounds if you're the stabby type.");
        directionAdvice.addResponse(new DialogueResponse("Thanks for the directions.", "nell_farewell"));
        directionAdvice.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_direction_advice", directionAdvice);
        
        DialogueNode mossCompliment = new DialogueNode("Finally! Someone with taste! Been cultivating these varieties for decades. The teal ones behind my ear? Luminescent when excited. Want to see?");
        mossCompliment.addResponse(new DialogueResponse("Maybe another time...", "nell_moss_decline"));
        mossCompliment.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_moss_compliment", mossCompliment);
        
        DialogueNode mossDecline = new DialogueNode("Your loss! Folks around here don't appreciate a good bioluminescent culture these days.");
        mossDecline.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_moss_decline", mossDecline);
        
        DialogueNode mushroomsStory = new DialogueNode("Oh yes! Caeloran fungi are... opinionated. Got these Mi'conid colonies that don't like straight lines. Had to let them redesign the eastern wall three times!");
        mushroomsStory.addResponse(new DialogueResponse("That's... fascinating.", "nell_mushroom_interest"));
        mushroomsStory.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_mushrooms_story", mushroomsStory);
        
        DialogueNode mushroomInterest = new DialogueNode("If you're really interested, there's a whole glade of 'em northeast of here. Bring something sweet - they love tree sap. DON'T bring metal tools. They're... touchy about iron.");
        mushroomInterest.addResponse(new DialogueResponse("I'll keep that in mind.", "nell_farewell"));
        mushroomInterest.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_mushroom_interest", mushroomInterest);
        
        DialogueNode checkCrate = new DialogueNode("Help yourself! There's some food and a map snippet in there too. Oh, and a tattered journal to track your skills. Essential stuff for newcomers!");
        checkCrate.addResponse(new DialogueResponse("Thanks, Nell.", "nell_farewell"));
        checkCrate.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_check_crate", checkCrate);
        
        // Nell's farewell dialogue
        DialogueNode farewell = new DialogueNode("Off you go then! Come back if you need anything. And remember - when in doubt, just keep doing stuff. You'll figure it out!");
        farewell.addResponse(new DialogueResponse("Goodbye", ""));
        dialogueManager.registerDialogue("nell_farewell", farewell);
        
        // Gear-specific responses after receiving items
        DialogueNode givePickaxe = new DialogueNode("Good choice! The mines east of here are full of ore and gems. Start with the surface veins - they're easier to work with. And watch out for the deeper tunnels... strange critters down there.");
        givePickaxe.addResponse(new DialogueResponse("Thanks for the advice.", "nell_farewell"));
        givePickaxe.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_give_pickaxe", givePickaxe);
        
        DialogueNode giveFishingRod = new DialogueNode("Ah, the peaceful life! Try the docks first - easier catches. When you're ready, the northern cove has exotic fish, but watch the tides. They get... unusual around here.");
        giveFishingRod.addResponse(new DialogueResponse("Thanks for the advice.", "nell_farewell"));
        giveFishingRod.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_give_fishing_rod", giveFishingRod);
        
        DialogueNode giveKnife = new DialogueNode("Stabby it is! There's training dummies just west of here. Once you've got the hang of it, try the western woods - lots of critters to practice on. Nothing too deadly... well, mostly.");
        giveKnife.addResponse(new DialogueResponse("Thanks for the advice.", "nell_farewell"));
        giveKnife.addResponse(new DialogueResponse("Back to the beginning", "dialogue_nell"));
        dialogueManager.registerDialogue("nell_give_knife", giveKnife);
    }
    
    @Override
    public void onInteract(Player player, NPC npc, boolean rightClick) {
        // Log interaction to debug
        plugin.debugLog(DebugSystem.NPC,"Player " + player.getName() + " interacted with Nell Mossgleam (" + 
                            (rightClick ? "right" : "left") + " click)");
        
        // Play a nature sound when player interacts
        player.playSound(player.getLocation(), Sound.BLOCK_MOSS_PLACE, 0.6f, 1.2f);
        
        // Explicitly pass the dialogue through the DialogueManager
        DialogueNode dialogue = DialogueManager.getInstance().getDialogue("dialogue_nell");
        if (dialogue != null) {
            DialogueManager.getInstance().startDialogue(player, this, dialogue);
        } else {
            // Fallback if dialogue not found
            sendMessage(player, "Oi! You there! Looking a bit lost, aren't ya?");
            plugin.debugLog(DebugSystem.NPC,"Dialogue 'dialogue_nell' not found for Nell Mossgleam");
        }
    }

    /**
     * Start the mossy particle effect
     */
    private void startMossyParticleEffect() {
        if (particleTask != null) {
            particleTask.cancel();
        }
        
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isSpawned()) {
                    this.cancel();
                    return;
                }
                
                Location center = npc.getEntity().getLocation().add(0, 0.8, 0);
                
                // Create mossy particles around Nell
                for (int i = 0; i < 2; i++) {
                    double offsetX = (Math.random() - 0.5) * 0.8;
                    double offsetY = (Math.random()) * 1.2;
                    double offsetZ = (Math.random() - 0.5) * 0.8;
                    
                    Location particleLoc = center.clone().add(offsetX, offsetY, offsetZ);
                    
                    // Use spore blossom particle for a mossy effect
                    npc.getEntity().getWorld().spawnParticle(
                        Particle.SPORE_BLOSSOM_AIR, 
                        particleLoc,
                        1, 0.02, 0.02, 0.02, 0.0
                    );
                }
                
                // Occasionally make tool tinkering sound
                if (Math.random() < 0.05) { // 5% chance each tick
                    npc.getEntity().getWorld().playSound(
                        center,
                        Sound.BLOCK_AMETHYST_BLOCK_STEP,
                        0.2f,
                        1.2f + (float)Math.random() * 0.4f
                    );
                }
            }
        };
        
        // Run the effect every 10 ticks
        particleTask.runTaskTimer(plugin, 0, 10);
    }
    
    public void cleanup() {
        
        // Clean up the particle task
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
    }
}