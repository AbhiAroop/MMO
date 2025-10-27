package com.server.profiles.skills.skills.farming.botany;

import java.util.Random;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

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
import com.server.profiles.skills.skills.farming.subskills.BotanySubskill;

/**
 * Handles all Botany-related events
 * - Planting custom crops
 * - Harvesting custom crops
 * - Interacting with crop breeders
 */
public class BotanyListener implements Listener {
    
    private final Random random;
    
    public BotanyListener(Main plugin) {
        this.random = new Random();
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
            
            // Award plant XP
            if (botanySkill instanceof BotanySubskill) {
                double xp = crop.getPlantXp() * crop.getRarity().getXpMultiplier();
                SkillProgressionManager.getInstance().addExperience(player, botanySkill, xp);
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
        
        // Check if crop is fully grown
        if (!plantedCrop.isFullyGrown()) {
            player.sendMessage("§c✗ This " + crop.getRarity().getColor() + crop.getDisplayName() + 
                              "§c is not fully grown yet!");
            player.sendMessage("§7Growth: §f" + (int)(plantedCrop.getGrowthProgress() * 100) + "%");
            event.setCancelled(true);
            return;
        }
        
        // Cancel event (we handle drops manually)
        event.setCancelled(true);
        event.setDropItems(false);
        
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
        
        // Drop the crop items
        Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
        ItemStack drops = crop.createDropItem(dropAmount);
        block.getWorld().dropItemNaturally(dropLocation, drops);
        
        // Chance to drop seed
        if (random.nextDouble() < crop.getRareSeedChance()) {
            ItemStack seed = crop.createSeedItem();
            block.getWorld().dropItemNaturally(dropLocation, seed);
            
            player.sendMessage("§a✓ §7Found a " + crop.getRarity().getColor() + 
                              crop.getDisplayName() + " Seed§7!");
        }
        
        // Award harvest XP
        if (profile != null) {
            Skill botanySkill = SkillRegistry.getInstance().getSubskill(SubskillType.BOTANY);
            if (botanySkill instanceof BotanySubskill) {
                double xp = crop.getHarvestXp() * crop.getRarity().getXpMultiplier();
                SkillProgressionManager.getInstance().addExperience(player, botanySkill, xp);
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
}
