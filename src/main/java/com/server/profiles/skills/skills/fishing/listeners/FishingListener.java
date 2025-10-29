package com.server.profiles.skills.skills.fishing.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillProgressionManager;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.skills.fishing.baits.BaitManager;
import com.server.profiles.skills.skills.fishing.baits.FishingBait;
import com.server.profiles.skills.skills.fishing.loot.Fish;
import com.server.profiles.skills.skills.fishing.loot.FishingMob;
import com.server.profiles.skills.skills.fishing.loot.FishingTreasure;
import com.server.profiles.skills.skills.fishing.minigame.FishingSession;
import com.server.profiles.skills.skills.fishing.minigame.FishingSessionManager;
import com.server.profiles.skills.skills.fishing.subskills.RodFishingSubskill;
import com.server.profiles.skills.skills.fishing.types.FishingType;
import com.server.profiles.skills.trees.PlayerSkillTreeData;

/**
 * Handles fishing events and integrates with the minigame system
 */
public class FishingListener implements Listener {
    
    private final FishingSessionManager sessionManager;
    
    public FishingListener() {
        this.sessionManager = FishingSessionManager.getInstance();
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        PlayerFishEvent.State state = event.getState();
        
        switch (state) {
            case FISHING:
                // Player casts fishing rod
                handleFishingStart(player, event);
                break;
                
            case BITE:
                // Fish bites - start minigame and cancel the vanilla bite
                handleBite(player, event);
                break;
                
            case CAUGHT_FISH:
            case CAUGHT_ENTITY:
                // Default catch - override with our minigame system
                handleCatch(player, event);
                break;
                
            case FAILED_ATTEMPT:
                // Reel in without catching
                handleFailedAttempt(player);
                break;
                
            case REEL_IN:
                // Player reels in manually
                handleReelIn(player);
                break;
        }
    }
    
    /**
     * Handle player starting to fish
     */
    @SuppressWarnings("unused")
    private void handleFishingStart(Player player, PlayerFishEvent event) {
        // Clean up any existing session
        if (sessionManager.hasActiveSession(player)) {
            sessionManager.endSession(player);
        }
        
        // Get player's lure potency stat and set custom wait time
        FishHook hook = event.getHook();
        if (hook != null) {
            Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
            if (activeSlot != null) {
                PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
                if (profile != null) {
                    int lurePotency = profile.getStats().getLurePotency();
                    int[] waitTime = profile.getStats().getFishingWaitTime();
                    
                    // Set min and max wait time on the hook
                    hook.setMinWaitTime(waitTime[0]); // Min wait in ticks (default 100 = 5s)
                    hook.setMaxWaitTime(waitTime[1]); // Max wait in ticks (scaled by lure potency)
                    
                    // Debug message (can be removed later)
                    if (lurePotency > 0) {
                        player.sendMessage(String.format(
                            "§7[Lure Potency: §e%d§7] Wait time: §e%.1f-%.1fs",
                            lurePotency, waitTime[0] / 20.0, waitTime[1] / 20.0
                        ));
                    }
                }
            }
        }
    }
    
    /**
     * Handle fish biting - start the minigame
     */
    
    /**
     * Handle fish biting - start the minigame
     * We cancel the vanilla bite and handle everything ourselves to avoid timeout issues
     */
    private void handleBite(Player player, PlayerFishEvent event) {
        // If player already has an active minigame session, ignore this bite
        // This prevents vanilla from restarting the minigame when another BOP occurs
        if (sessionManager.hasActiveSession(player)) {
            event.setCancelled(true);
            return;
        }
        
        // Check if player has bait
        FishingBait bait = BaitManager.findBaitInInventory(player);
        if (bait == null) {
            event.setCancelled(true);
            player.sendTitle("§c§lNo Bait!", "§7You need bait to catch fish!", 10, 40, 10);
            player.sendMessage("§c§l⚠ §cYou need bait to start fishing!");
            player.sendMessage("§7Purchase bait from the fishing shop or use §e/fishing bait§7.");
            return;
        }
        
        FishHook hook = event.getHook();
        
        // Determine fishing type based on hook location
        FishingType fishingType = determineFishingType(hook);
        
        // Check if player has unlocked this fishing type
        if (fishingType.requiresUnlock()) {
            Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
            if (activeSlot == null) {
                event.setCancelled(true);
                return;
            }
            
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile == null) {
                event.setCancelled(true);
                return;
            }
            
            PlayerSkillTreeData treeData = profile.getSkillTreeData();
            String unlockNodeId = fishingType.getUnlockNodeId();
            
            if (unlockNodeId != null && !treeData.isNodeUnlocked("fishing", unlockNodeId)) {
                player.sendMessage("§cYou need to unlock " + fishingType.getColoredName() + " §cfirst!");
                event.setCancelled(true);
                return;
            }
        }
        
        // Consume the bait
        FishingBait consumedBait = BaitManager.consumeBait(player);
        if (consumedBait == null) {
            // Shouldn't happen since we checked above, but just in case
            event.setCancelled(true);
            player.sendTitle("§c§lNo Bait!", "§7You need bait to catch fish!", 10, 40, 10);
            return;
        }
        
        // IMPORTANT: Don't cancel the event - let the bite happen
        // This keeps the hook in the water so we can continue the minigame
        // The hook will stay cast until we complete the minigame or the player cancels
        
        // Create and start fishing session with bait
        FishingSession session = sessionManager.createSession(player, fishingType);
        session.setBait(consumedBait); // Store the bait used for this session
        session.startMinigame();
        
        // Notify player
        player.sendMessage("§e§l⚡ Fish Hooked! §7Right-click when indicator is in the §a§lGREEN §7zone!");
        
        // Play sound if location is not null
        if (player.getLocation() != null) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
        }
    }
    
    /**
     * Handle catch attempt - integrate with minigame
     */
    private void handleCatch(Player player, PlayerFishEvent event) {
        FishingSession session = sessionManager.getSession(player);
        
        // If there's an active minigame session, this is the vanilla timeout trying to catch
        // Cancel it and let our minigame continue
        if (session != null && session.isActive()) {
            event.setCancelled(true);
            // Don't send message - this happens automatically from vanilla timeout
            return;
        }
        
        // If no active session, this is a normal vanilla catch (shouldn't happen with our system)
        // Cancel it anyway to prevent vanilla fish items
        event.setCancelled(true);
    }
    
    /**
     * Handle failed fishing attempt
     */
    private void handleFailedAttempt(Player player) {
        FishingSession session = sessionManager.getSession(player);
        
        // If there's an active minigame, don't let vanilla "failed attempt" end it
        if (session != null && session.isActive()) {
            // Ignore - the minigame is still running
            return;
        }
        
        // If no active session, clean up
        if (sessionManager.hasActiveSession(player)) {
            sessionManager.endSession(player);
        }
    }
    
    /**
     * Handle manual reel in
     */
    private void handleReelIn(Player player) {
        FishingSession session = sessionManager.getSession(player);
        
        // If there's an active minigame, the player is trying to reel in during the game
        // This should be handled by the minigame, not here
        if (session != null && session.isActive()) {
            // Don't end the session - let the minigame handle it
            return;
        }
        
        // If no active session or session already complete, clean up
        if (sessionManager.hasActiveSession(player)) {
            sessionManager.endSession(player);
        }
    }
    
    /**
     * Handle player right-click to attempt catch during minigame
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Check if player has active fishing session
        if (!sessionManager.hasActiveSession(player)) {
            return;
        }
        
        // Check if player is holding fishing rod
        if (player.getInventory().getItemInMainHand().getType() != Material.FISHING_ROD &&
            player.getInventory().getItemInOffHand().getType() != Material.FISHING_ROD) {
            return;
        }
        
        // Check if right-click action
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR ||
            event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            
            FishingSession session = sessionManager.getSession(player);
            if (session != null && session.isActive()) {
                // Attempt catch in minigame
                session.attemptCatch();
                event.setCancelled(true); // Prevent normal rod casting
            }
        }
    }
    
    /**
     * Clean up sessions when player quits
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessionManager.endSession(event.getPlayer());
    }
    
    /**
     * Determine fishing type based on hook location
     */
    private FishingType determineFishingType(FishHook hook) {
        Block hookBlock = hook.getLocation().getBlock();
        Material material = hookBlock.getType();
        
        // Check for void fishing (below Y=0)
        if (hook.getLocation().getY() < 0) {
            return FishingType.VOID;
        }
        
        // Check for lava fishing
        if (material == Material.LAVA) {
            return FishingType.LAVA;
        }
        
        // Check for ice fishing
        Block blockAbove = hookBlock.getRelative(0, 1, 0);
        if (blockAbove.getType() == Material.ICE || 
            blockAbove.getType() == Material.PACKED_ICE || 
            blockAbove.getType() == Material.BLUE_ICE) {
            return FishingType.ICE;
        }
        
        // Default to normal water fishing
        return FishingType.NORMAL_WATER;
    }
    
    /**
     * Grant fishing loot based on session performance
     * Called by FishingSession when completed successfully
     */
    public void grantFishingLoot(Player player, FishingSession session) {
        // Get player's luck stat for mob chance calculation
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        double luckBonus = 0.0;
        double treasureBonus = 0.0;
        
        if (activeSlot != null) {
            PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
            if (profile != null) {
                luckBonus = profile.getStats().getLuck() / 100.0; // Convert to 0.0-1.0+ range
                
                // Get treasure bonus from Rod Fishing subskill
                Skill rodFishingSkill = SkillRegistry.getInstance().getSkill("rod_fishing");
                if (rodFishingSkill instanceof RodFishingSubskill) {
                    // getTreasureChance returns percentage (0-100), convert to 0.0-1.0
                    treasureBonus = ((RodFishingSubskill) rodFishingSkill).getTreasureChance(player) / 100.0;
                }
            }
        }
        
        // Priority 1: Try to spawn a mob (8% base chance, reduced by luck)
        FishingMob mob = FishingMob.trySpawnMob(session.getFishingType(), luckBonus);
        if (mob != null && player.getFishHook() != null) {
            // Spawn the mob at the hook location, hooked to the fishing rod
            org.bukkit.Location spawnLoc = player.getFishHook().getLocation();
            org.bukkit.entity.Entity spawnedEntity = spawnLoc.getWorld().spawnEntity(spawnLoc, mob.getEntityType());
            
            // Hook the entity to the fishing rod
            player.getFishHook().setHookedEntity(spawnedEntity);
            
            // Notify player
            player.sendTitle("§c§l⚠ MOB HOOKED! ⚠", "§7You caught a " + mob.getDisplayName() + "§7!", 10, 60, 10);
            player.sendMessage("§c§l⚠ §cYou fished up a " + mob.getDisplayName() + "§c!");
            player.sendMessage("§7The creature is hooked to your fishing rod!");
            
            // Play dramatic sound
            if (player.getLocation() != null) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
            }
            
            // Grant reduced XP for mob catch
            double mobXp = 5.0 * session.getFishingType().getDifficultyMultiplier();
            Skill rodFishingSkill = SkillRegistry.getInstance().getSkill("rod_fishing");
            if (rodFishingSkill != null) {
                SkillProgressionManager.getInstance().addExperience(player, rodFishingSkill, mobXp);
            }
            
            player.sendMessage(String.format("§7XP: §e+%.1f §8(mob encounter)", mobXp));
            return;
        }
        
        // Priority 2: Try to get treasure/junk (based on treasure bonus)
        FishingTreasure treasure = FishingTreasure.tryGetTreasure(session.getFishingType(), treasureBonus);
        if (treasure != null) {
            // Give treasure item
            player.getInventory().addItem(treasure.toItemStack());
            
            // Notify player based on treasure type
            String treasureTypeMsg;
            switch (treasure.getType()) {
                case EPIC:
                    player.sendTitle("§6§l✦ EPIC TREASURE! ✦", "§eYou found " + treasure.getDisplayName() + "§e!", 10, 60, 10);
                    treasureTypeMsg = "§6§l✦ EPIC TREASURE! ";
                    if (player.getLocation() != null) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    }
                    break;
                case RARE:
                    player.sendTitle("§5§l◆ RARE TREASURE! ◆", "§dYou found " + treasure.getDisplayName() + "§d!", 10, 50, 10);
                    treasureTypeMsg = "§5§l◆ RARE TREASURE! ";
                    if (player.getLocation() != null) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                    }
                    break;
                case UNCOMMON:
                    treasureTypeMsg = "§9§l♦ Uncommon Treasure! ";
                    if (player.getLocation() != null) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.3f);
                    }
                    break;
                case COMMON:
                    treasureTypeMsg = "§f§l♦ Common Treasure! ";
                    if (player.getLocation() != null) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    }
                    break;
                case JUNK:
                    treasureTypeMsg = "§7Junk Item: ";
                    if (player.getLocation() != null) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 0.8f);
                    }
                    break;
                default:
                    treasureTypeMsg = "§7Found: ";
                    break;
            }
            
            player.sendMessage(treasureTypeMsg + treasure.getDisplayName());
            
            // Grant XP based on treasure type
            double treasureXp = 5.0;
            switch (treasure.getType()) {
                case EPIC:
                    treasureXp = 50.0;
                    break;
                case RARE:
                    treasureXp = 30.0;
                    break;
                case UNCOMMON:
                    treasureXp = 20.0;
                    break;
                case COMMON:
                    treasureXp = 10.0;
                    break;
                case JUNK:
                    treasureXp = 3.0;
                    break;
            }
            
            treasureXp *= session.getFishingType().getDifficultyMultiplier();
            
            Skill rodFishingSkill = SkillRegistry.getInstance().getSkill("rod_fishing");
            if (rodFishingSkill != null) {
                SkillProgressionManager.getInstance().addExperience(player, rodFishingSkill, treasureXp);
            }
            
            player.sendMessage(String.format("§7XP: §e+%.1f §8(treasure find)", treasureXp));
            return;
        }
        
        // Priority 3: Give normal fish (default behavior)
        // Calculate XP based on performance
        double baseXp = 10.0;
        double accuracy = session.getAccuracy();
        int perfectCatches = session.getPerfectCatches();
        
        // Bonus XP for accuracy and perfect catches
        double accuracyBonus = (accuracy / 100.0) * baseXp;
        double perfectBonus = perfectCatches * 5.0;
        
        double totalXp = baseXp + accuracyBonus + perfectBonus;
        
        // Apply fishing type multiplier
        totalXp *= session.getFishingType().getDifficultyMultiplier();
        
        // Grant XP to Rod Fishing subskill
        Skill rodFishingSkill = SkillRegistry.getInstance().getSkill("rod_fishing");
        
        if (rodFishingSkill != null) {
            SkillProgressionManager.getInstance().addExperience(player, rodFishingSkill, totalXp);
        }
        
        // Create fish based on performance and bait used (use treasureBonus calculated above)
        Fish caughtFish = Fish.createFish(
            session.getFishingType(), 
            accuracy, 
            treasureBonus, 
            perfectCatches,
            session.getBait() // Pass the bait used
        );
        
        // Give fish to player
        player.getInventory().addItem(caughtFish.toItemStack());
        
        // Send success message with fish details
        player.sendMessage(String.format(
            "§a§lFishing Complete! §7Caught a %s§7!",
            caughtFish.getFishType().getDisplayName()
        ));
        player.sendMessage(String.format(
            "§7Size: §f%.1fcm §7| Quality: %s §7| Rarity: %s",
            caughtFish.getSize(),
            caughtFish.getQuality().getColoredName(),
            caughtFish.getRarity().getColoredName()
        ));
        player.sendMessage(String.format(
            "§7XP: §e+%.1f §8(%.1f%% accuracy, %d perfect)",
            totalXp, accuracy, perfectCatches
        ));
        
        // Trophy announcement
        if (caughtFish.isTrophy()) {
            player.sendTitle("§6§l✦ TROPHY FISH! ✦", "§eAn legendary catch!", 10, 60, 10);
            // TODO: Broadcast to server when trophy fish is caught
        }
    }
    
    /**
     * Prevent bait items from being placed as blocks
     * Fixes bug where baits (wheat seeds) could be planted on farmland
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        
        // Check if the item being placed is a bait
        if (FishingBait.fromItem(event.getItemInHand()) != null) {
            event.setCancelled(true);
            player.sendMessage("§c§l⚠ §cBaits cannot be placed! They are meant for fishing only.");
        }
    }
}
