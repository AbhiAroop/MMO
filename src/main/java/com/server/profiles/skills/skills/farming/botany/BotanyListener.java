package com.server.profiles.skills.skills.farming.botany;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import com.server.Main;
import com.server.debug.DebugManager;
import com.server.debug.DebugManager.DebugSystem;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillProgressionManager;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.core.SubskillType;
import com.server.profiles.skills.data.SkillLevel;
import com.server.profiles.skills.skills.farming.subskills.CultivatingSubskill;
import com.server.profiles.skills.skills.farming.subskills.HarvestingSubskill;

/**
 * Handles all Botany-related events
 * - Planting custom crops
 * - Harvesting custom crops
 * - Interacting with crop breeders
 */
public class BotanyListener implements Listener {
    
    private final Random random;
    private final Map<UUID, BossBar> playerBossBars;
    private final Map<UUID, Location> lastLookedCrop;
    private final Main plugin;
    
    public BotanyListener(Main plugin) {
        this.random = new Random();
        this.playerBossBars = new HashMap<>();
        this.lastLookedCrop = new HashMap<>();
        this.plugin = plugin;
        
        // Start a repeating task to update boss bars every 10 ticks (0.5 seconds)
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllBossBars, 10L, 10L);
    }
    
    /**
     * Handle planting custom crops on farmland
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlantCrop(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.FARMLAND) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) {
            return;
        }
        
        // Check if this is a custom crop seed
        CustomCrop crop = CustomCropRegistry.getInstance().getCropFromSeed(item);
        if (crop == null) {
            return;
        }
        
        // Get the block above farmland
        Block aboveBlock = clickedBlock.getRelative(BlockFace.UP);
        if (aboveBlock.getType() != Material.AIR) {
            return; // Can't plant if there's already a block there
        }
        
        // Check if player has required Botany level
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (activeSlot == null) {
            return;
        }
        
        PlayerProfile[] profiles = ProfileManager.getInstance().getProfiles(player.getUniqueId());
        if (profiles == null || profiles[activeSlot] == null) {
            return;
        }
        
        PlayerProfile profile = profiles[activeSlot];
        Skill botanySkill = SkillRegistry.getInstance().getSubskill(SubskillType.BOTANY);
        
        if (botanySkill != null) {
            SkillLevel skillLevel = profile.getSkillData().getSkillLevel(SubskillType.BOTANY);
            int botanyLevel = skillLevel.getLevel();
            
            if (botanyLevel < crop.getBreedingLevel()) {
                player.sendMessage("§c✗ You need Botany level " + crop.getBreedingLevel() + 
                                  " to plant " + crop.getRarity().getColor() + crop.getDisplayName() + "§c!");
                player.sendMessage("§7Your Botany level: §f" + botanyLevel);
                event.setCancelled(true);
                return;
            }
        }
        
        // Check if there's already a crop at this location
        Location plantLocation = aboveBlock.getLocation();
        if (BotanyManager.getInstance().getCropAt(plantLocation) != null) {
            return; // Already a crop here
        }
        
        // Plant the crop
        PlantedCustomCrop plantedCrop = BotanyManager.getInstance().plantCrop(
            crop.getId(), plantLocation, player.getUniqueId()
        );
        
        if (plantedCrop != null) {
            // Cancel the event to prevent default seed placement
            event.setCancelled(true);
            
            // Remove seed from inventory
            if (player.getGameMode() != GameMode.CREATIVE) {
                item.setAmount(item.getAmount() - 1);
            }
            
            // Award planting XP to Cultivating subskill (not Botany)
            Skill cultivatingSkill = SkillRegistry.getInstance().getSubskill(SubskillType.CULTIVATING);
            if (cultivatingSkill instanceof CultivatingSubskill) {
                double xp = crop.getPlantXp() * crop.getRarity().getXpMultiplier();
                SkillProgressionManager.getInstance().addExperience(player, cultivatingSkill, xp);
            }
            
            // Feedback
            player.sendMessage("§a✓ Planted " + crop.getRarity().getColor() + crop.getDisplayName() + "§a!");
            Location playerLoc = player.getLocation();
            if (playerLoc != null) {
                player.playSound(playerLoc, Sound.ITEM_CROP_PLANT, 1.0f, 1.0f);
            }
            
            DebugManager.getInstance().debug(DebugSystem.SKILLS,
                "[Botany] " + player.getName() + " planted " + crop.getDisplayName());
        }
    }
    
    /**
     * Handle harvesting custom crops
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHarvestCrop(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        // Check if this is a tripwire (custom crop block)
        if (block.getType() != Material.TRIPWIRE) {
            return;
        }
        
        // Check if there's a planted crop at this location
        PlantedCustomCrop plantedCrop = BotanyManager.getInstance().getCropAt(block.getLocation());
        if (plantedCrop == null) {
            return; // Not a custom crop
        }
        
        Player player = event.getPlayer();
        CustomCrop crop = plantedCrop.getCrop();
        
        if (crop == null) {
            // Invalid crop data, remove it
            BotanyManager.getInstance().removeCrop(plantedCrop);
            return;
        }
        
        // Cancel event (we handle drops manually)
        event.setCancelled(true);
        event.setDropItems(false);
        
        Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
        
        // Check if crop is fully grown
        if (!plantedCrop.isFullyGrown()) {
            // Early harvest - only drop seed
            player.sendMessage("§e⚠ This " + crop.getRarity().getColor() + crop.getDisplayName() + 
                              "§e is not fully grown yet!");
            player.sendMessage("§7Growth: §f" + (int)(plantedCrop.getGrowthProgress() * 100) + "% §7- Seed returned");
            
            // Always drop the seed back
            ItemStack seed = crop.createSeedItem();
            block.getWorld().dropItemNaturally(dropLocation, seed);
            
            // Remove the crop
            BotanyManager.getInstance().removeCrop(plantedCrop);
            
            // Feedback
            Location playerLoc = player.getLocation();
            if (playerLoc != null) {
                player.playSound(playerLoc, Sound.BLOCK_CROP_BREAK, 1.0f, 0.8f);
            }
            block.getWorld().spawnParticle(Particle.BLOCK, dropLocation, 5, 0.2, 0.2, 0.2, 0.1, 
                Material.WHEAT.createBlockData());
            
            return;
        }
        
        // Get player profile for XP
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        PlayerProfile profile = null;
        
        if (activeSlot != null) {
            PlayerProfile[] profiles = ProfileManager.getInstance().getProfiles(player.getUniqueId());
            if (profiles != null && profiles[activeSlot] != null) {
                profile = profiles[activeSlot];
            }
        }
        
        // Calculate drops
        int dropAmount = crop.getMinDrops() + random.nextInt(crop.getMaxDrops() - crop.getMinDrops() + 1);
        
        // Apply farming fortune if available
        if (profile != null) {
            double fortune = profile.getStats().getFarmingFortune();
            if (fortune > 0) {
                // Similar to ore fortune calculation
                int guaranteedBonus = (int) (fortune / 100.0) * dropAmount;
                double remainder = (fortune % 100.0) / 100.0;
                int chanceBonus = random.nextDouble() < remainder ? dropAmount : 0;
                dropAmount += guaranteedBonus + chanceBonus;
            }
        }
        
        // Drop the crop items (reuse dropLocation from earlier)
        ItemStack drops = crop.createDropItem(dropAmount);
        block.getWorld().dropItemNaturally(dropLocation, drops);
        
        // Chance to drop seed
        if (random.nextDouble() < crop.getRareSeedChance()) {
            ItemStack seed = crop.createSeedItem();
            block.getWorld().dropItemNaturally(dropLocation, seed);
            
            player.sendMessage("§a✓ §7Found a " + crop.getRarity().getColor() + 
                              crop.getDisplayName() + " Seed§7!");
        }
        
        // Award harvest XP to Harvesting subskill (not Botany)
        if (profile != null) {
            Skill harvestingSkill = SkillRegistry.getInstance().getSubskill(SubskillType.HARVESTING);
            if (harvestingSkill instanceof HarvestingSubskill) {
                double xp = crop.getHarvestXp() * crop.getRarity().getXpMultiplier();
                SkillProgressionManager.getInstance().addExperience(player, harvestingSkill, xp);
            }
        }
        
        // Remove the planted crop
        BotanyManager.getInstance().removeCrop(plantedCrop);
        
        // Feedback
        player.sendMessage("§a✓ Harvested " + crop.getRarity().getColor() + crop.getDisplayName() + 
                          "§a x" + dropAmount + "!");
        Location playerLoc = player.getLocation();
        if (playerLoc != null) {
            player.playSound(playerLoc, Sound.BLOCK_CROP_BREAK, 1.0f, 1.0f);
        }
        block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, dropLocation, 10, 0.3, 0.3, 0.3);
        
        DebugManager.getInstance().debug(DebugSystem.SKILLS,
            "[Botany] " + player.getName() + " harvested " + crop.getDisplayName() + " x" + dropAmount);
    }
    
    /**
     * Handle interacting with crop breeder
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreederInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.COMPOSTER) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Check if player is holding two seeds
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) {
            return;
        }
        
        // Get the first seed
        CustomCrop crop1 = CustomCropRegistry.getInstance().getCropFromSeed(item);
        if (crop1 == null) {
            return; // Not a custom crop seed
        }
        
        // Check if this is a valid breeder location or create new one
        Location breederLoc = block.getLocation();
        CropBreeder breeder = BotanyManager.getInstance().getBreederAt(breederLoc);
        
        if (breeder == null) {
            // Try to create a new breeder
            breeder = new CropBreeder(breederLoc, player.getUniqueId());
            
            if (!breeder.isStructureValid()) {
                player.sendMessage("§c✗ Invalid Crop Breeder structure!");
                player.sendMessage("§7Build a 3x3 farmland platform with a composter in the center.");
                return;
            }
            
            BotanyManager.getInstance().registerBreeder(breeder);
            player.sendMessage("§a✓ Crop Breeder registered!");
        }
        
        // Check if already breeding
        if (breeder.isBreeding()) {
            double progress = breeder.getBreedingProgress() * 100;
            player.sendMessage("§e⚗ Breeding in progress... " + (int)progress + "%");
            event.setCancelled(true);
            return;
        }
        
        // Player needs to hold TWO different seeds
        // For now, we'll use a simple system: sneak + right-click to set parent 1, regular right-click to start breeding
        
        if (player.isSneaking()) {
            // Set parent crop 1 (stored temporarily in breeder)
            player.sendMessage("§a✓ Set parent crop: " + crop1.getRarity().getColor() + crop1.getDisplayName());
            player.sendMessage("§7Now right-click again (without sneaking) with the second parent crop.");
            // TODO: Store this in a temporary map player -> cropId
            event.setCancelled(true);
        } else {
            // For MVP, let's just use the same crop type (breed with itself)
            String crop1Id = crop1.getId();
            String crop2Id = crop1.getId(); // Same crop for now
            
            // Try to start breeding
            if (breeder.startBreeding(player, crop1Id, crop2Id)) {
                // Success! Remove seed from inventory
                if (player.getGameMode() != GameMode.CREATIVE) {
                    item.setAmount(item.getAmount() - 2); // Remove 2 seeds
                }
            }
            event.setCancelled(true);
        }
    }
    
    /**
     * Prevent farmland with custom crops from turning back to dirt
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFarmlandDecay(BlockFadeEvent event) {
        Block block = event.getBlock();
        
        // Only handle farmland turning to dirt
        if (block.getType() != Material.FARMLAND) {
            return;
        }
        
        // Check if there's a custom crop planted on this farmland
        Location cropLocation = block.getRelative(BlockFace.UP).getLocation();
        PlantedCustomCrop crop = BotanyManager.getInstance().getCropAt(cropLocation);
        
        if (crop != null) {
            // Cancel the decay - keep farmland hydrated while crop is planted
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle farmland trampling - break crop if farmland is destroyed
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFarmlandTrample(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) {
            return;
        }
        
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.FARMLAND) {
            return;
        }
        
        // Check if there's a crop on this farmland
        Location cropLocation = block.getRelative(BlockFace.UP).getLocation();
        PlantedCustomCrop crop = BotanyManager.getInstance().getCropAt(cropLocation);
        
        if (crop != null) {
            Player player = event.getPlayer();
            CustomCrop cropType = CustomCropRegistry.getInstance().getCrop(crop.getCropId());
            
            if (cropType != null) {
                Location dropLoc = cropLocation.clone().add(0.5, 0.5, 0.5);
                
                // Check if fully grown
                if (crop.isFullyGrown()) {
                    // Fully grown - drop normal harvest
                    int dropAmount = cropType.getMinDrops() + random.nextInt(cropType.getMaxDrops() - cropType.getMinDrops() + 1);
                    cropLocation.getWorld().dropItemNaturally(dropLoc, cropType.createDropItem(dropAmount));
                    
                    // Chance to drop seed
                    if (random.nextDouble() < cropType.getRareSeedChance()) {
                        cropLocation.getWorld().dropItemNaturally(dropLoc, cropType.createSeedItem());
                    }
                } else {
                    // Not fully grown - only drop seed
                    cropLocation.getWorld().dropItemNaturally(dropLoc, cropType.createSeedItem());
                }
                
                // Remove the crop
                BotanyManager.getInstance().removeCrop(crop);
                
                // Play break sound and particles
                cropLocation.getWorld().playSound(cropLocation, Sound.BLOCK_CROP_BREAK, 1.0f, 1.0f);
                cropLocation.getWorld().spawnParticle(Particle.BLOCK, cropLocation.clone().add(0.5, 0.5, 0.5), 
                    10, 0.3, 0.3, 0.3, 0.1, Material.WHEAT.createBlockData());
                
                // Send message to player
                String growthStatus = crop.isFullyGrown() ? "Fully Grown" : 
                    String.format("%.0f%% grown", crop.getGrowthProgress() * 100);
                player.sendMessage("§e§l[!] §cYou trampled a " + cropType.getRarity().getColor() + 
                    cropType.getDisplayName() + " §c(" + growthStatus + ")");
            }
            
            // Don't cancel the event - allow farmland to turn to dirt
        }
    }
    
    /**
     * Handle entity trampling (like mobs jumping on crops)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityTrample(EntityInteractEvent event) {
        Block block = event.getBlock();
        
        if (block.getType() != Material.FARMLAND) {
            return;
        }
        
        // Check if there's a crop on this farmland
        Location cropLocation = block.getRelative(BlockFace.UP).getLocation();
        PlantedCustomCrop crop = BotanyManager.getInstance().getCropAt(cropLocation);
        
        if (crop != null) {
            CustomCrop cropType = CustomCropRegistry.getInstance().getCrop(crop.getCropId());
            
            if (cropType != null) {
                Location dropLoc = cropLocation.clone().add(0.5, 0.5, 0.5);
                
                // Check if fully grown
                if (crop.isFullyGrown()) {
                    // Fully grown - drop harvest items
                    int dropAmount = cropType.getMinDrops() + random.nextInt(cropType.getMaxDrops() - cropType.getMinDrops() + 1);
                    cropLocation.getWorld().dropItemNaturally(dropLoc, cropType.createDropItem(dropAmount));
                    
                    // Chance to drop seed
                    if (random.nextDouble() < cropType.getRareSeedChance()) {
                        cropLocation.getWorld().dropItemNaturally(dropLoc, cropType.createSeedItem());
                    }
                } else {
                    // Not fully grown - only drop seed
                    cropLocation.getWorld().dropItemNaturally(dropLoc, cropType.createSeedItem());
                }
                
                // Remove the crop
                BotanyManager.getInstance().removeCrop(crop);
                
                // Play break sound
                cropLocation.getWorld().playSound(cropLocation, Sound.BLOCK_CROP_BREAK, 1.0f, 1.0f);
            }
        }
    }
    
    /**
     * Handle farmland being broken with a tool - destroy crop above it
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFarmlandBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        // Only handle farmland being broken
        if (block.getType() != Material.FARMLAND) {
            return;
        }
        
        // Check if there's a crop planted on this farmland
        Location cropLocation = block.getRelative(BlockFace.UP).getLocation();
        PlantedCustomCrop crop = BotanyManager.getInstance().getCropAt(cropLocation);
        
        if (crop != null) {
            Player player = event.getPlayer();
            CustomCrop cropType = CustomCropRegistry.getInstance().getCrop(crop.getCropId());
            
            if (cropType != null) {
                Location dropLoc = cropLocation.clone().add(0.5, 0.5, 0.5);
                
                // Check if fully grown
                if (crop.isFullyGrown()) {
                    // Fully grown - drop normal harvest
                    int dropAmount = cropType.getMinDrops() + random.nextInt(cropType.getMaxDrops() - cropType.getMinDrops() + 1);
                    cropLocation.getWorld().dropItemNaturally(dropLoc, cropType.createDropItem(dropAmount));
                    
                    // Chance to drop seed
                    if (random.nextDouble() < cropType.getRareSeedChance()) {
                        cropLocation.getWorld().dropItemNaturally(dropLoc, cropType.createSeedItem());
                    }
                } else {
                    // Not fully grown - only drop seed
                    cropLocation.getWorld().dropItemNaturally(dropLoc, cropType.createSeedItem());
                }
                
                // Remove the crop
                BotanyManager.getInstance().removeCrop(crop);
                
                // Play break sound and particles
                cropLocation.getWorld().playSound(cropLocation, Sound.BLOCK_CROP_BREAK, 1.0f, 1.0f);
                cropLocation.getWorld().spawnParticle(Particle.BLOCK, dropLoc, 
                    10, 0.3, 0.3, 0.3, 0.1, Material.WHEAT.createBlockData());
                
                // Send message to player
                player.sendMessage("§e§l[!] §7The " + cropType.getRarity().getColor() + cropType.getDisplayName() + 
                    " §7was destroyed by breaking the farmland!");
            }
        }
    }
    
    /**
     * Show boss bar when player looks at a custom crop
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Ray trace to see what block the player is looking at
        RayTraceResult result = player.rayTraceBlocks(5.0); // 5 block range
        
        if (result == null || result.getHitBlock() == null) {
            // Not looking at any block - hide boss bar
            hideBossBar(player);
            return;
        }
        
        Block targetBlock = result.getHitBlock();
        
        // Check if looking at a custom crop (tripwire)
        if (targetBlock == null || targetBlock.getType() != Material.TRIPWIRE) {
            hideBossBar(player);
            return;
        }
        
        // Check if there's a planted crop at this location
        PlantedCustomCrop plantedCrop = BotanyManager.getInstance().getCropAt(targetBlock.getLocation());
        if (plantedCrop == null) {
            hideBossBar(player);
            return;
        }
        
        CustomCrop crop = plantedCrop.getCrop();
        if (crop == null) {
            hideBossBar(player);
            return;
        }
        
        // Check if we're still looking at the same crop
        Location lastCrop = lastLookedCrop.get(player.getUniqueId());
        if (lastCrop != null && lastCrop.equals(targetBlock.getLocation())) {
            // Same crop - just update the boss bar
            updateBossBar(player, crop, plantedCrop);
        } else {
            // New crop - create/update boss bar
            lastLookedCrop.put(player.getUniqueId(), targetBlock.getLocation().clone());
            showBossBar(player, crop, plantedCrop);
        }
    }
    
    /**
     * Show or update the boss bar for a crop
     */
    private void showBossBar(Player player, CustomCrop crop, PlantedCustomCrop plantedCrop) {
        BossBar bossBar = playerBossBars.get(player.getUniqueId());
        
        if (bossBar == null) {
            // Create new boss bar
            bossBar = Bukkit.createBossBar("", BarColor.GREEN, BarStyle.SEGMENTED_10);
            bossBar.addPlayer(player);
            playerBossBars.put(player.getUniqueId(), bossBar);
        }
        
        updateBossBar(player, crop, plantedCrop);
        bossBar.setVisible(true);
    }
    
    /**
     * Update the boss bar text and progress
     */
    private void updateBossBar(Player player, CustomCrop crop, PlantedCustomCrop plantedCrop) {
        BossBar bossBar = playerBossBars.get(player.getUniqueId());
        if (bossBar == null) return;
        
        int currentStage = plantedCrop.getCurrentStage();
        int maxStages = crop.getMaxGrowthStages();
        double stageProgress = plantedCrop.getGrowthProgress();
        
        // Determine which stage we're in (for display)
        int displayStage = currentStage;
        
        // Check if fully grown
        String status;
        BarColor color;
        if (plantedCrop.isFullyGrown()) {
            status = "§a§lFULLY GROWN";
            color = BarColor.GREEN;
            stageProgress = 1.0;
        } else {
            status = "§e§lGROWING";
            color = BarColor.YELLOW;
        }
        
        // Build title
        String title = crop.getRarity().getColor() + crop.getDisplayName() + " §7- " + status + 
                      " §8[§f" + displayStage + "§7/§f" + maxStages + "§8] §7" + 
                      (int)(stageProgress * 100) + "%";
        
        bossBar.setTitle(title);
        bossBar.setProgress(Math.min(1.0, Math.max(0.0, stageProgress)));
        bossBar.setColor(color);
    }
    
    /**
     * Hide the boss bar for a player
     */
    private void hideBossBar(Player player) {
        BossBar bossBar = playerBossBars.get(player.getUniqueId());
        if (bossBar != null) {
            bossBar.setVisible(false);
        }
        lastLookedCrop.remove(player.getUniqueId());
    }
    
    /**
     * Clean up boss bars when player quits
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        BossBar bossBar = playerBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removeAll();
        }
        lastLookedCrop.remove(player.getUniqueId());
    }
    
    /**
     * Update all active boss bars for players looking at crops
     * This runs periodically to keep the growth progress updated in real-time
     */
    private void updateAllBossBars() {
        for (Map.Entry<UUID, Location> entry : lastLookedCrop.entrySet()) {
            UUID playerUUID = entry.getKey();
            Location cropLocation = entry.getValue();
            
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null || !player.isOnline()) {
                continue;
            }
            
            // Check if crop still exists at this location
            PlantedCustomCrop plantedCrop = BotanyManager.getInstance().getCropAt(cropLocation);
            if (plantedCrop == null) {
                hideBossBar(player);
                continue;
            }
            
            CustomCrop crop = plantedCrop.getCrop();
            if (crop == null) {
                hideBossBar(player);
                continue;
            }
            
            // Update the boss bar with current crop data
            updateBossBar(player, crop, plantedCrop);
        }
    }
}
