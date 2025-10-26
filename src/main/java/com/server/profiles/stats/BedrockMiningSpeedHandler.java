package com.server.profiles.stats;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.server.Main;
import com.server.debug.DebugManager;
import com.server.debug.DebugManager.DebugSystem;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.core.SubskillType;
import com.server.profiles.skills.skills.farming.subskills.HarvestingSubskill;
import com.server.profiles.skills.skills.mining.subskills.OreExtractionSubskill;
import com.server.util.BedrockPlayerUtil;

/**
 * Handles custom mining speed for Bedrock Edition players
 * 
 * Bedrock players don't support the PLAYER_BLOCK_BREAK_SPEED attribute,
 * so we need to implement a custom block breaking system similar to Hypixel Skyblock.
 * 
 * This works by:
 * 1. Detecting when a Bedrock player starts breaking a block
 * 2. Calculating the break time based on their mining speed stat
 * 3. Sending block damage packets to show proper breaking animation
 * 4. Breaking the block after the calculated time
 * 5. Matching the break time to what a Java player would experience with the same mining speed attribute
 */
public class BedrockMiningSpeedHandler implements Listener {
    
    private final Main plugin;
    
    // Track active mining operations for Bedrock players
    private final Map<UUID, MiningOperation> activeMining = new HashMap<>();
    
    // Track recently broken blocks to prevent immediate restart
    private final Map<UUID, Long> recentBreaks = new HashMap<>();
    private static final long BREAK_COOLDOWN_MS = 10; // 10ms cooldown (just 1 tick) to prevent double-processing
    
    // Track last arm animation packet for hold-to-break detection
    private final Map<UUID, Long> lastArmSwing = new HashMap<>();
    private static final long ARM_SWING_TIMEOUT_MS = 200; // 200ms = 4 ticks (if no swing, player stopped holding)
    
    public BedrockMiningSpeedHandler(Main plugin) {
        this.plugin = plugin;
        
        // Listen for arm animation packets to detect continuous mining
        ProtocolLibrary.getProtocolManager().addPacketListener(
            new com.comphenix.protocol.events.PacketAdapter(plugin, 
                com.comphenix.protocol.events.ListenerPriority.NORMAL, 
                PacketType.Play.Client.ARM_ANIMATION) {
                
                @Override
                public void onPacketReceiving(com.comphenix.protocol.events.PacketEvent event) {
                    Player player = event.getPlayer();
                    
                    // Only track for Bedrock players who are actively mining
                    if (BedrockPlayerUtil.isBedrockPlayer(player) && activeMining.containsKey(player.getUniqueId())) {
                        lastArmSwing.put(player.getUniqueId(), System.currentTimeMillis());
                        
                        DebugManager.getInstance().debug(DebugSystem.MINING, 
                            "Bedrock player " + player.getName() + " - ARM_ANIMATION packet received");
                    }
                }
            }
        );
    }
    
    /**
     * Send block crack animation using ProtocolLib for better control
     * This ensures consistent animation across Java and Bedrock clients through Geyser
     */
    private void sendBlockCrackPacket(Player player, Block block, int destroyStage, int entityId) {
        try {
            DebugManager.getInstance().debug(DebugSystem.MINING, 
                ">>> PACKET SEND START - Player: " + player.getName() + " | EntityID: " + entityId + 
                " | Block: " + block.getX() + "," + block.getY() + "," + block.getZ() + 
                " | DestroyStage: " + destroyStage);
            
            PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
            
            // Set entity ID (unique identifier for this break animation)
            packet.getIntegers().write(0, entityId);
            
            // Set block position
            BlockPosition blockPos = new BlockPosition(block.getX(), block.getY(), block.getZ());
            packet.getBlockPositionModifier().write(0, blockPos);
            
            // Set destroy stage (0-9, or 255 to remove animation)
            packet.getIntegers().write(1, destroyStage);
            
            // Send to the mining player
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
            
            // Also send to nearby players
            int nearbyCount = 0;
            for (org.bukkit.entity.Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), 64, 64, 64)) {
                if (entity instanceof Player) {
                    Player nearbyPlayer = (Player) entity;
                    if (!nearbyPlayer.equals(player)) {
                        try {
                            ProtocolLibrary.getProtocolManager().sendServerPacket(nearbyPlayer, packet);
                            nearbyCount++;
                        } catch (Exception ignored) {}
                    }
                }
            }
            
            DebugManager.getInstance().debug(DebugSystem.MINING, 
                "<<< PACKET SENT - EntityID: " + entityId + " | Block: " + 
                block.getX() + "," + block.getY() + "," + block.getZ() + 
                " | DestroyStage: " + destroyStage + " | Sent to " + (nearbyCount + 1) + " players");
            
        } catch (Exception e) {
            DebugManager.getInstance().debug(DebugSystem.MINING, 
                "!!! PACKET FAILED - EntityID: " + entityId + " | Error: " + e.getMessage());
        }
    }
    
    /**
     * Handle when a Bedrock player starts damaging a block
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        
        // Only handle Bedrock players
        if (!BedrockPlayerUtil.isBedrockPlayer(player)) {
            return;
        }
        
        // Skip if in creative mode
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        
        Block block = event.getBlock();
        UUID playerUuid = player.getUniqueId();
        Material blockType = block.getType();
        
        // CHECK PERMISSIONS: Prevent breaking locked ores and crops
        // Check if this is an ore that might be locked
        if (isOre(blockType)) {
            Skill oreExtractionSkill = SkillRegistry.getInstance().getSubskill(SubskillType.ORE_EXTRACTION);
            if (oreExtractionSkill instanceof OreExtractionSubskill) {
                OreExtractionSubskill oreExtraction = (OreExtractionSubskill) oreExtractionSkill;
                
                if (!oreExtraction.canMineOre(player, blockType)) {
                    // Don't allow mining this ore
                    player.sendMessage(org.bukkit.ChatColor.RED + "You need to unlock the ability to mine this ore first.");
                    DebugManager.getInstance().debug(DebugSystem.MINING, 
                        "Bedrock player " + player.getName() + " tried to mine locked ore: " + blockType);
                    return;
                }
            }
        }
        
        // Check if this is a crop that might be locked
        if (isCrop(blockType)) {
            Skill harvestingSkill = SkillRegistry.getInstance().getSubskill(SubskillType.HARVESTING);
            if (harvestingSkill instanceof HarvestingSubskill) {
                HarvestingSubskill harvesting = (HarvestingSubskill) harvestingSkill;
                
                if (!harvesting.canHarvestCrop(player, blockType)) {
                    // Don't allow harvesting this crop
                    player.sendMessage(org.bukkit.ChatColor.RED + "You need to unlock the ability to harvest " + 
                        getCropDisplayName(blockType) + org.bukkit.ChatColor.RED + " first!");
                    player.sendMessage(org.bukkit.ChatColor.YELLOW + "Check your Harvesting skill tree to unlock this crop.");
                    DebugManager.getInstance().debug(DebugSystem.MINING, 
                        "Bedrock player " + player.getName() + " tried to harvest locked crop: " + blockType);
                    return;
                }
            }
        }
        
        // Check if we're in cooldown period after a recent break
        Long lastBreak = recentBreaks.get(playerUuid);
        if (lastBreak != null) {
            long timeSinceBreak = System.currentTimeMillis() - lastBreak;
            if (timeSinceBreak < BREAK_COOLDOWN_MS) {
                DebugManager.getInstance().debug(DebugSystem.MINING, 
                    "Bedrock player " + player.getName() + " - In break cooldown, ignoring event (" + timeSinceBreak + "ms)");
                return;
            }
            // Cooldown expired, remove it
            recentBreaks.remove(playerUuid);
        }
        
        // Check if player is already mining this block
        MiningOperation existing = activeMining.get(playerUuid);
        if (existing != null && existing.isSameBlock(block)) {
            // Already mining this block, don't restart
            DebugManager.getInstance().debug(DebugSystem.MINING, 
                "Bedrock player " + player.getName() + " already mining this block - ignoring duplicate event");
            return;
        }
        
        // Get player's mining speed stat
        double miningSpeed = getMiningSpeed(player);
        
        // Calculate break time in ticks
        int breakTimeTicks = calculateBreakTime(block.getType(), player, miningSpeed);
        
        // Debug message
        DebugManager.getInstance().debug(DebugSystem.MINING, 
            "Bedrock player " + player.getName() + " started mining " + block.getType() + 
            " | Speed: " + String.format("%.2f", miningSpeed) + 
            " | Break Time: " + breakTimeTicks + " ticks");
        
        // Cancel any existing mining operation for this player (different block)
        cancelMining(playerUuid);
        
        // Record initial arm swing timestamp for hold-to-break detection
        lastArmSwing.put(playerUuid, System.currentTimeMillis());
        
        // Start a new mining operation with animation
        MiningOperation operation = new MiningOperation(player, block, breakTimeTicks);
        activeMining.put(playerUuid, operation);
        operation.start();
    }
    
    /**
     * ALWAYS cancel vanilla block breaking for Bedrock players in survival
     * We handle ALL breaking through our custom system
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        // Only handle Bedrock players
        if (!BedrockPlayerUtil.isBedrockPlayer(player)) {
            return;
        }
        
        // Skip if in creative mode
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        
        // ALWAYS cancel for Bedrock players in survival - we handle breaking internally
        event.setCancelled(true);
        
        DebugManager.getInstance().debug(DebugSystem.MINING, 
            "Bedrock player " + player.getName() + " - Cancelled vanilla break event (handled by custom system)");
    }
    
    /**
     * Handle when a Bedrock player stops mining a block
     * 
     * NOTE: We ignore this event because Bedrock clients send it constantly.
     * Instead, we check in the animation loop if they're still looking at the target block.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        // Intentionally empty - we handle abort detection in the animation loop
    }
    
    /**
     * Clean up when player quits
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        cancelMining(playerUuid);
        recentBreaks.remove(playerUuid);
        lastArmSwing.remove(playerUuid);
    }
    
    /**
     * Cancel an active mining operation
     */
    private void cancelMining(UUID playerUuid) {
        MiningOperation operation = activeMining.get(playerUuid);
        if (operation != null && !operation.isCancelled()) {
            operation.cancel();
            activeMining.remove(playerUuid);
        }
        // Clean up arm swing tracking
        lastArmSwing.remove(playerUuid);
    }
    
    /**
     * Get the player's mining speed stat
     */
    private double getMiningSpeed(Player player) {
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) return 0.5; // Default value
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
        if (profile == null) return 0.5;
        
        return profile.getStats().getMiningSpeed();
    }
    
    /**
     * Calculate break time in ticks based on mining speed
     * 
     * This matches the formula used by Minecraft's PLAYER_BLOCK_BREAK_SPEED attribute
     * Mining speed of 0.5 = 2x base break time (slower)
     * Mining speed of 1.0 = 1x base break time (normal)
     * Mining speed of 2.0 = 0.5x base break time (2x faster)
     * 
     * Formula: breakTime = baseBreakTime / miningSpeed
     */
    private int calculateBreakTime(Material blockType, Player player, double miningSpeed) {
        // Get base break time for the block
        int baseBreakTimeTicks = getBaseBreakTime(blockType, player);
        
        // Apply mining speed multiplier
        // Higher mining speed = faster breaking
        double adjustedBreakTime = baseBreakTimeTicks / miningSpeed;
        
        return (int) Math.max(1, adjustedBreakTime);
    }
    
    /**
     * Get the base break time for a block type in ticks
     * This is the time it takes to break the block with BARE HANDS at 1.0 mining speed
     * 
     * NOTE: All blocks are treated as being broken with fist/bare hands.
     * Tools do NOT speed up breaking - only the player's mining speed stat matters.
     * 
     * Times are based on Minecraft Java Edition's actual break times with bare hands.
     */
    private int getBaseBreakTime(Material blockType, Player player) {
        // Get block hardness and calculate base break time
        // These values are based on Minecraft Java Edition's actual break times (20 ticks = 1 second)
        // Formula: ticks = hardness * 1.5 * 20 (for blocks that can't be insta-mined)
        switch (blockType) {
            // Stone-type blocks (hardness 1.5) - 30 seconds = 600 ticks with bare hands
            case STONE:
            case COBBLESTONE:
            case MOSSY_COBBLESTONE:
            case STONE_BRICKS:
            case MOSSY_STONE_BRICKS:
            case CRACKED_STONE_BRICKS:
            case CHISELED_STONE_BRICKS:
            case INFESTED_STONE:
            case INFESTED_COBBLESTONE:
            case INFESTED_STONE_BRICKS:
            case INFESTED_MOSSY_STONE_BRICKS:
            case INFESTED_CRACKED_STONE_BRICKS:
            case INFESTED_CHISELED_STONE_BRICKS:
                return 600; // 30 seconds
                
            // Harder stone variants (hardness 1.5) - 30 seconds = 600 ticks
            case ANDESITE:
            case DIORITE:
            case GRANITE:
            case POLISHED_ANDESITE:
            case POLISHED_DIORITE:
            case POLISHED_GRANITE:
                return 600; // 30 seconds
                
            // Deepslate (hardness 3.0) - 60 seconds = 1200 ticks
            case DEEPSLATE:
            case COBBLED_DEEPSLATE:
            case POLISHED_DEEPSLATE:
            case DEEPSLATE_BRICKS:
            case DEEPSLATE_TILES:
            case CHISELED_DEEPSLATE:
            case CRACKED_DEEPSLATE_BRICKS:
            case CRACKED_DEEPSLATE_TILES:
                return 1200; // 60 seconds
                
            // Sandstone (hardness 0.8) - 12 seconds = 240 ticks
            case SANDSTONE:
            case RED_SANDSTONE:
            case SMOOTH_SANDSTONE:
            case SMOOTH_RED_SANDSTONE:
            case CHISELED_SANDSTONE:
            case CHISELED_RED_SANDSTONE:
            case CUT_SANDSTONE:
            case CUT_RED_SANDSTONE:
                return 240; // 12 seconds
                
            // Bricks (hardness 2.0) - 40 seconds = 800 ticks
            case BRICKS:
                return 800; // 40 seconds
                
            // Nether Bricks (hardness 2.0) - 40 seconds = 800 ticks
            case NETHER_BRICKS:
            case RED_NETHER_BRICKS:
            case CHISELED_NETHER_BRICKS:
            case CRACKED_NETHER_BRICKS:
                return 800; // 40 seconds
                
            // Blackstone (hardness 1.5) - 30 seconds = 600 ticks
            case BLACKSTONE:
            case POLISHED_BLACKSTONE:
            case POLISHED_BLACKSTONE_BRICKS:
            case CHISELED_POLISHED_BLACKSTONE:
            case CRACKED_POLISHED_BLACKSTONE_BRICKS:
            case GILDED_BLACKSTONE:
                return 600; // 30 seconds
                
            // Basalt (hardness 1.25) - 25 seconds = 500 ticks
            case BASALT:
            case SMOOTH_BASALT:
            case POLISHED_BASALT:
                return 500; // 25 seconds
                
            // Ore blocks (hardness 3.0) - 60 seconds = 1200 ticks with bare hands
            case COAL_ORE:
            case IRON_ORE:
            case COPPER_ORE:
            case GOLD_ORE:
            case REDSTONE_ORE:
            case EMERALD_ORE:
            case LAPIS_ORE:
            case DIAMOND_ORE:
            case NETHER_GOLD_ORE:
            case NETHER_QUARTZ_ORE:
                return 1200; // 60 seconds
                
            // Deepslate Ores (hardness 4.5) - 90 seconds = 1800 ticks
            case DEEPSLATE_COAL_ORE:
            case DEEPSLATE_IRON_ORE:
            case DEEPSLATE_COPPER_ORE:
            case DEEPSLATE_GOLD_ORE:
            case DEEPSLATE_REDSTONE_ORE:
            case DEEPSLATE_EMERALD_ORE:
            case DEEPSLATE_LAPIS_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                return 1800; // 90 seconds
                
            // Obsidian (hardness 50) - 250 seconds = 5000 ticks with bare hands
            case OBSIDIAN:
            case CRYING_OBSIDIAN:
                return 5000; // 250 seconds
                
            // Dirt/Sand/Gravel (hardness 0.5) - 0.75 seconds = 15 ticks with bare hands
            case DIRT:
            case COARSE_DIRT:
            case ROOTED_DIRT:
            case PODZOL:
            case MYCELIUM:
            case GRASS_BLOCK:
            case SAND:
            case RED_SAND:
            case GRAVEL:
            case SUSPICIOUS_SAND:
            case SUSPICIOUS_GRAVEL:
                return 15; // 0.75 seconds
                
            // Clay (hardness 0.6) - 0.9 seconds = 18 ticks
            case CLAY:
                return 18; // 0.9 seconds
                
            // Soul Sand/Soil (hardness 0.5) - 0.75 seconds = 15 ticks
            case SOUL_SAND:
            case SOUL_SOIL:
                return 15; // 0.75 seconds
                
            // Wood logs (hardness 2.0) - 3 seconds = 60 ticks with bare hands
            case OAK_LOG:
            case SPRUCE_LOG:
            case BIRCH_LOG:
            case JUNGLE_LOG:
            case ACACIA_LOG:
            case DARK_OAK_LOG:
            case MANGROVE_LOG:
            case CHERRY_LOG:
            case CRIMSON_STEM:
            case WARPED_STEM:
            case STRIPPED_OAK_LOG:
            case STRIPPED_SPRUCE_LOG:
            case STRIPPED_BIRCH_LOG:
            case STRIPPED_JUNGLE_LOG:
            case STRIPPED_ACACIA_LOG:
            case STRIPPED_DARK_OAK_LOG:
            case STRIPPED_MANGROVE_LOG:
            case STRIPPED_CHERRY_LOG:
            case STRIPPED_CRIMSON_STEM:
            case STRIPPED_WARPED_STEM:
            case OAK_WOOD:
            case SPRUCE_WOOD:
            case BIRCH_WOOD:
            case JUNGLE_WOOD:
            case ACACIA_WOOD:
            case DARK_OAK_WOOD:
            case MANGROVE_WOOD:
            case CHERRY_WOOD:
            case CRIMSON_HYPHAE:
            case WARPED_HYPHAE:
            case STRIPPED_OAK_WOOD:
            case STRIPPED_SPRUCE_WOOD:
            case STRIPPED_BIRCH_WOOD:
            case STRIPPED_JUNGLE_WOOD:
            case STRIPPED_ACACIA_WOOD:
            case STRIPPED_DARK_OAK_WOOD:
            case STRIPPED_MANGROVE_WOOD:
            case STRIPPED_CHERRY_WOOD:
            case STRIPPED_CRIMSON_HYPHAE:
            case STRIPPED_WARPED_HYPHAE:
                return 60; // 3 seconds
                
            // Wood planks (hardness 2.0) - 40 ticks with bare hands
            case OAK_PLANKS:
            case SPRUCE_PLANKS:
            case BIRCH_PLANKS:
            case JUNGLE_PLANKS:
            case ACACIA_PLANKS:
            case DARK_OAK_PLANKS:
            case MANGROVE_PLANKS:
            case CHERRY_PLANKS:
            case BAMBOO_PLANKS:
            case CRIMSON_PLANKS:
            case WARPED_PLANKS:
            case BAMBOO_MOSAIC:
                return 60; // 3 seconds
                
            // Netherrack (hardness 0.4) - 0.6 seconds = 12 ticks with bare hands
            case NETHERRACK:
                return 12; // 0.6 seconds
                
            // End Stone (hardness 3.0) - 45 seconds = 900 ticks with bare hands
            case END_STONE:
            case END_STONE_BRICKS:
                return 900; // 45 seconds
                
            // Glass (hardness 0.3) - 0.45 seconds = 9 ticks (instant break)
            case GLASS:
            case WHITE_STAINED_GLASS:
            case ORANGE_STAINED_GLASS:
            case MAGENTA_STAINED_GLASS:
            case LIGHT_BLUE_STAINED_GLASS:
            case YELLOW_STAINED_GLASS:
            case LIME_STAINED_GLASS:
            case PINK_STAINED_GLASS:
            case GRAY_STAINED_GLASS:
            case LIGHT_GRAY_STAINED_GLASS:
            case CYAN_STAINED_GLASS:
            case PURPLE_STAINED_GLASS:
            case BLUE_STAINED_GLASS:
            case BROWN_STAINED_GLASS:
            case GREEN_STAINED_GLASS:
            case RED_STAINED_GLASS:
            case BLACK_STAINED_GLASS:
            case TINTED_GLASS:
            case GLASS_PANE:
            case WHITE_STAINED_GLASS_PANE:
            case ORANGE_STAINED_GLASS_PANE:
            case MAGENTA_STAINED_GLASS_PANE:
            case LIGHT_BLUE_STAINED_GLASS_PANE:
            case YELLOW_STAINED_GLASS_PANE:
            case LIME_STAINED_GLASS_PANE:
            case PINK_STAINED_GLASS_PANE:
            case GRAY_STAINED_GLASS_PANE:
            case LIGHT_GRAY_STAINED_GLASS_PANE:
            case CYAN_STAINED_GLASS_PANE:
            case PURPLE_STAINED_GLASS_PANE:
            case BLUE_STAINED_GLASS_PANE:
            case BROWN_STAINED_GLASS_PANE:
            case GREEN_STAINED_GLASS_PANE:
            case RED_STAINED_GLASS_PANE:
            case BLACK_STAINED_GLASS_PANE:
                return 9; // 0.45 seconds
                
            // Leaves (hardness 0.2) - 0.3 seconds = 6 ticks with bare hands
            case OAK_LEAVES:
            case SPRUCE_LEAVES:
            case BIRCH_LEAVES:
            case JUNGLE_LEAVES:
            case ACACIA_LEAVES:
            case DARK_OAK_LEAVES:
            case MANGROVE_LEAVES:
            case CHERRY_LEAVES:
            case AZALEA_LEAVES:
            case FLOWERING_AZALEA_LEAVES:
                return 6; // 0.3 seconds
                
            // Default for other blocks - log warning
            default:
                DebugManager.getInstance().debug(DebugSystem.MINING, 
                    "Bedrock player " + player.getName() + " - Unknown block type: " + blockType + " - using default 20 ticks");
                return 20;
        }
    }
    
    /**
     * Represents an active mining operation for a Bedrock player
     * Handles the breaking animation and actual block breaking
     */
    private class MiningOperation {
        private final Player player;
        private final Block block;
        private final int totalBreakTimeTicks;
        private final int entityId;
        private BukkitRunnable animationTask;
        private BukkitRunnable breakTask;
        private BukkitRunnable monitorTask;
        private int currentDamage = 0;
        private boolean completed = false;
        private boolean cancelled = false;
        private static final int MAX_DAMAGE = 10; // Minecraft uses 10 stages of block damage
        
        public MiningOperation(Player player, Block block, int breakTimeTicks) {
            this.player = player;
            this.block = block;
            this.totalBreakTimeTicks = breakTimeTicks;
            // Use unique entity ID for this operation (player ID + block location hash)
            this.entityId = player.getEntityId() + block.getLocation().hashCode();
            
            DebugManager.getInstance().debug(DebugSystem.MINING, 
                "Created MiningOperation - Player EntityID: " + player.getEntityId() + 
                " | Animation EntityID: " + entityId);
        }
        
        public boolean isSameBlock(Block otherBlock) {
            return block.getLocation().equals(otherBlock.getLocation());
        }
        
        public boolean isCompleted() {
            return completed;
        }
        
        public boolean isCancelled() {
            return cancelled;
        }
        
        public void start() {
            DebugManager.getInstance().debug(DebugSystem.MINING, 
                "Bedrock player " + player.getName() + " - Mining START - Total: " + totalBreakTimeTicks + " ticks (" + 
                (totalBreakTimeTicks * 50) + "ms)");
            
            // Calculate ticks per damage stage for animation
            // Animation timing is independent of actual break time for testing
            // TESTING: Slow down animation 5x to diagnose if Geyser/client is the issue
            final int baseTicksPerStage = Math.max(1, totalBreakTimeTicks / MAX_DAMAGE);
            final int animationTicksPerStage = baseTicksPerStage * 5; // 5x slower animation
            
            // Animation completion (for visual effect only)
            final int animationCompletionTicks = animationTicksPerStage * MAX_DAMAGE;
            
            DebugManager.getInstance().debug(DebugSystem.MINING, 
                "Bedrock player " + player.getName() + 
                " | Block breaks at: " + totalBreakTimeTicks + " ticks (" + (totalBreakTimeTicks * 50) + "ms)" +
                " | Animation: " + animationTicksPerStage + " ticks (" + (animationTicksPerStage * 50) + "ms) per stage" +
                " | Animation completes: " + animationCompletionTicks + " ticks" +
                " | TESTING: Animation slowed 5x independently of break time");
            
            // Start a monitoring task that checks EVERY TICK if player is still breaking
            monitorTask = new BukkitRunnable() {
                @Override
                public void run() {
                    // Stop if already cancelled
                    if (cancelled) {
                        cancel();
                        return;
                    }
                    
                    // Check if player stopped mining
                    if (!player.isOnline()) {
                        DebugManager.getInstance().debug(DebugSystem.MINING, 
                            "Bedrock player " + player.getName() + " - Player offline - cancelling");
                        cancelMining(player.getUniqueId());
                        return;
                    }
                    
                    // Check if player is still holding the break button by checking arm swing packets
                    Long lastSwing = lastArmSwing.get(player.getUniqueId());
                    if (lastSwing == null) {
                        DebugManager.getInstance().debug(DebugSystem.MINING, 
                            "Bedrock player " + player.getName() + " - No arm swing tracked - cancelling");
                        cancelMining(player.getUniqueId());
                        return;
                    }
                    
                    long timeSinceLastSwing = System.currentTimeMillis() - lastSwing;
                    if (timeSinceLastSwing > ARM_SWING_TIMEOUT_MS) {
                        // Player hasn't swung in too long - they stopped holding the button
                        DebugManager.getInstance().debug(DebugSystem.MINING, 
                            "Bedrock player " + player.getName() + " - Stopped holding break button (" + 
                            timeSinceLastSwing + "ms since last arm swing) - cancelling");
                        cancelMining(player.getUniqueId());
                        return;
                    }
                    
                    // Check if player is still targeting the block (secondary check)
                    Block targetBlock = player.getTargetBlockExact(6);
                    if (targetBlock == null || !targetBlock.getLocation().equals(block.getLocation())) {
                        // Player looked away from the target block - cancel mining
                        DebugManager.getInstance().debug(DebugSystem.MINING, 
                            "Bedrock player " + player.getName() + " - No longer targeting block - cancelling");
                        cancelMining(player.getUniqueId());
                        return;
                    }
                    
                    // Check if block changed
                    if (block.getType() == Material.AIR) {
                        DebugManager.getInstance().debug(DebugSystem.MINING, 
                            "Bedrock player " + player.getName() + " - Block is air - cancelling");
                        cancelMining(player.getUniqueId());
                    }
                }
            };
            
            // Monitor every tick (1L delay, 1L period)
            monitorTask.runTaskTimer(plugin, 1L, 1L);
            
            // Start animation task that updates block crack animation at proper intervals
            animationTask = new BukkitRunnable() {
                private int ticksElapsed = 0;
                
                @Override
                public void run() {
                    // Stop if operation was cancelled
                    if (cancelled || completed) {
                        DebugManager.getInstance().debug(DebugSystem.MINING, 
                            "Bedrock player " + player.getName() + " - Animation task stopping (cancelled=" + cancelled + ", completed=" + completed + ")");
                        cancel();
                        return;
                    }
                    
                    ticksElapsed += animationTicksPerStage;
                    
                    // Increment damage stage (1 to 10)
                    currentDamage++;
                    
                    // Cap at MAX_DAMAGE
                    if (currentDamage > MAX_DAMAGE) {
                        DebugManager.getInstance().debug(DebugSystem.MINING, 
                            "Bedrock player " + player.getName() + " - Animation exceeded max, stopping");
                        cancel();
                        return;
                    }
                    
                    // Calculate destroy stage (0-9 for Minecraft protocol)
                    // currentDamage goes from 1-10, so we map it to 0-9
                    int destroyStage = currentDamage - 1;
                    
                    DebugManager.getInstance().debug(DebugSystem.MINING, 
                        "Bedrock player " + player.getName() + " - BEFORE SEND: Stage " + currentDamage + "/" + MAX_DAMAGE + 
                        " | Destroy Stage: " + destroyStage + " | EntityID: " + entityId);
                    
                    // Send block crack packet
                    sendBlockCrackPacket(player, block, destroyStage, entityId);
                    
                    // Calculate expected vs actual timing
                    int expectedTick = animationTicksPerStage * currentDamage;
                    float progress = ((float)currentDamage / MAX_DAMAGE) * 100f;
                    
                    DebugManager.getInstance().debug(DebugSystem.MINING, 
                        "Bedrock player " + player.getName() + " - AFTER SEND: Stage " + currentDamage + "/" + MAX_DAMAGE + " (" + 
                        String.format("%.0f", progress) + "%) | Destroy Stage: " + destroyStage + 
                        " | Expected Tick: " + expectedTick + " | Ticks Elapsed: " + ticksElapsed);
                    
                    // Stop after stage 10 (let the break task handle the actual breaking)
                    if (currentDamage >= MAX_DAMAGE) {
                        DebugManager.getInstance().debug(DebugSystem.MINING, 
                            "Bedrock player " + player.getName() + " - Animation COMPLETE at tick " + ticksElapsed);
                        cancel();
                    }
                }
            };
            
            // Start animation with the slowed down timing
            animationTask.runTaskTimer(plugin, animationTicksPerStage, animationTicksPerStage);
            
            // Schedule the actual block break
            breakTask = new BukkitRunnable() {
                @Override
                public void run() {
                    DebugManager.getInstance().debug(DebugSystem.MINING, 
                        "Bedrock player " + player.getName() + " - Break task executing after " + totalBreakTimeTicks + " ticks (mining speed based)");
                    
                    // Check if block is still the same
                    if (block.getType() == Material.AIR) {
                        DebugManager.getInstance().debug(DebugSystem.MINING, 
                            "Bedrock player " + player.getName() + " - Block already broken");
                        return;
                    }
                    
                    // Check if player is still online and in range
                    if (!player.isOnline()) {
                        DebugManager.getInstance().debug(DebugSystem.MINING, 
                            "Bedrock player " + player.getName() + " - Player offline during break");
                        return;
                    }
                    
                    // Check distance with null safety
                    org.bukkit.Location playerLoc = player.getLocation();
                    if (playerLoc != null && playerLoc.distance(block.getLocation()) > 6) {
                        DebugManager.getInstance().debug(DebugSystem.MINING, 
                            "Bedrock player " + player.getName() + " - Player too far during break");
                        return;
                    }
                    
                    // Mark as completed before breaking
                    completed = true;
                    
                    // Cancel the monitor task first
                    if (monitorTask != null) {
                        monitorTask.cancel();
                    }
                    
                    // Remove from active mining map
                    activeMining.remove(player.getUniqueId());
                    
                    // Remove block damage animation
                    sendBlockCrackPacket(player, block, 255, entityId);
                    
                    DebugManager.getInstance().debug(DebugSystem.MINING, 
                        "Bedrock player " + player.getName() + " - Breaking block now!");
                    
                    // Get player profile for fortune calculation
                    Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
                    if (activeSlot != null) {
                        PlayerProfile profile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[activeSlot];
                        if (profile != null) {
                            Material blockType = block.getType();
                            
                            // Handle ores with mining fortune
                            if (isOre(blockType)) {
                                handleOreBreak(player, block, profile);
                            }
                            // Handle crops with farming fortune
                            else if (isCrop(blockType)) {
                                handleCropBreak(player, block, profile);
                            }
                            // Handle regular blocks (just break normally)
                            else {
                                block.breakNaturally(player.getInventory().getItemInMainHand());
                            }
                        } else {
                            // Fallback if no profile
                            block.breakNaturally(player.getInventory().getItemInMainHand());
                        }
                    } else {
                        // Fallback if no profile
                        block.breakNaturally(player.getInventory().getItemInMainHand());
                    }
                    
                    // Set break cooldown to prevent immediate restart
                    recentBreaks.put(player.getUniqueId(), System.currentTimeMillis());
                }
            };
            
            // Break the block based on actual mining speed (NOT animation completion)
            // Animation is for visual feedback only, actual break time is determined by mining speed stat
            breakTask.runTaskLater(plugin, totalBreakTimeTicks);
        }
        
        public void cancel() {
            if (cancelled) {
                return; // Already cancelled
            }
            
            cancelled = true;
            
            DebugManager.getInstance().debug(DebugSystem.MINING, 
                "Bedrock player " + player.getName() + " - Mining operation cancelled");
            
            if (monitorTask != null) {
                monitorTask.cancel();
            }
            if (animationTask != null) {
                animationTask.cancel();
            }
            if (breakTask != null) {
                breakTask.cancel();
            }
            
            // Remove block damage animation
            if (player.isOnline()) {
                sendBlockCrackPacket(player, block, 255, entityId);
            }
        }
    }
    
    /**
     * Handle ore breaking with mining fortune
     */
    private void handleOreBreak(Player player, Block block, PlayerProfile profile) {
        Material blockType = block.getType();
        
        // Get mining fortune from player stats
        double miningFortune = profile.getStats().getMiningFortune();
        int fortuneMultiplier = calculateFortuneMultiplier(miningFortune);
        
        DebugManager.getInstance().debug(DebugSystem.MINING, 
            "Bedrock player " + player.getName() + " breaking ore " + blockType + 
            " with Mining Fortune multiplier: " + fortuneMultiplier);
        
        // Get the drops
        java.util.Collection<ItemStack> normalDrops = block.getDrops(player.getInventory().getItemInMainHand());
        
        // If no drops, add default ore drops
        if (normalDrops.isEmpty()) {
            Material dropType = getDefaultDropForOre(blockType);
            if (dropType != null) {
                normalDrops = java.util.Arrays.asList(new ItemStack(dropType));
            }
        }
        
        // Break the block first
        block.setType(Material.AIR);
        
        // Drop items with fortune multiplier
        if (!normalDrops.isEmpty()) {
            org.bukkit.Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
            for (ItemStack normalDrop : normalDrops) {
                ItemStack multipliedDrop = normalDrop.clone();
                multipliedDrop.setAmount(normalDrop.getAmount() * fortuneMultiplier);
                block.getWorld().dropItemNaturally(dropLocation, multipliedDrop);
            }
        }
        
        // Award vanilla XP if applicable
        int blockXP = getBlockXP(blockType);
        if (blockXP > 0) {
            player.giveExp(blockXP);
        }
    }
    
    /**
     * Handle crop breaking with farming fortune
     */
    private void handleCropBreak(Player player, Block block, PlayerProfile profile) {
        Material blockType = block.getType();
        
        // Get farming fortune from player stats
        double farmingFortune = profile.getStats().getFarmingFortune();
        int fortuneMultiplier = calculateFortuneMultiplier(farmingFortune);
        
        DebugManager.getInstance().debug(DebugSystem.MINING, 
            "Bedrock player " + player.getName() + " breaking crop " + blockType + 
            " with Farming Fortune multiplier: " + fortuneMultiplier);
        
        // Get the drops
        java.util.Collection<ItemStack> normalDrops = block.getDrops(player.getInventory().getItemInMainHand());
        
        // Break the block first
        block.setType(Material.AIR);
        
        // Drop items with fortune multiplier
        if (!normalDrops.isEmpty()) {
            org.bukkit.Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
            for (ItemStack normalDrop : normalDrops) {
                ItemStack multipliedDrop = normalDrop.clone();
                multipliedDrop.setAmount(normalDrop.getAmount() * fortuneMultiplier);
                block.getWorld().dropItemNaturally(dropLocation, multipliedDrop);
            }
        }
    }
    
    /**
     * Calculate fortune multiplier from fortune stat
     */
    private int calculateFortuneMultiplier(double fortune) {
        // Fortune is a percentage-based system:
        // 100 fortune = 2x drops (guaranteed)
        // 150 fortune = 2x drops + 50% chance for 3x drops
        
        // Calculate guaranteed multiplier (divide by 100 and add 1)
        int guaranteedMultiplier = (int) Math.floor(fortune / 100) + 1;
        
        // Calculate chance for an extra drop (remainder percentage)
        double chanceForExtraDrop = (fortune % 100);
        
        // Default multiplier is the guaranteed portion
        int fortuneMultiplier = guaranteedMultiplier;
        
        // Check for chance-based extra drop
        if (Math.random() * 100 < chanceForExtraDrop) {
            fortuneMultiplier++;
        }
        
        return fortuneMultiplier;
    }
    
    /**
     * Get the default drop for an ore type
     */
    private Material getDefaultDropForOre(Material oreType) {
        switch (oreType) {
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
                return Material.COAL;
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
                return Material.RAW_IRON;
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
                return Material.RAW_COPPER;
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
            case NETHER_GOLD_ORE:
                return Material.RAW_GOLD;
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
                return Material.REDSTONE;
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                return Material.LAPIS_LAZULI;
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                return Material.DIAMOND;
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                return Material.EMERALD;
            case NETHER_QUARTZ_ORE:
                return Material.QUARTZ;
            case ANCIENT_DEBRIS:
                return Material.ANCIENT_DEBRIS;
            default:
                return null;
        }
    }
    
    /**
     * Get vanilla XP for ore blocks
     */
    private int getBlockXP(Material material) {
        switch (material) {
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
                return 1;
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
            case NETHER_QUARTZ_ORE:
                return 2;
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                return 3;
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                return 5;
            case NETHER_GOLD_ORE:
                return 1;
            case ANCIENT_DEBRIS:
                return 3;
            default:
                return 0;
        }
    }
    
    /**
     * Check if a material is an ore
     */
    private boolean isOre(Material material) {
        switch (material) {
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
            case NETHER_GOLD_ORE:
            case NETHER_QUARTZ_ORE:
            case ANCIENT_DEBRIS:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Check if a material is a crop
     */
    private boolean isCrop(Material material) {
        switch (material) {
            case WHEAT:
            case CARROTS:
            case POTATOES:
            case BEETROOTS:
            case SWEET_BERRY_BUSH:
            case COCOA:
            case NETHER_WART:
            case MELON:
            case PUMPKIN:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Get display name for a crop material
     */
    private String getCropDisplayName(Material material) {
        switch (material) {
            case WHEAT:
                return "Wheat";
            case CARROTS:
                return "Carrots";
            case POTATOES:
                return "Potatoes";
            case BEETROOTS:
                return "Beetroots";
            case SWEET_BERRY_BUSH:
                return "Sweet Berries";
            case COCOA:
                return "Cocoa";
            case NETHER_WART:
                return "Nether Wart";
            case MELON:
                return "Melons";
            case PUMPKIN:
                return "Pumpkins";
            default:
                return material.name();
        }
    }
}
