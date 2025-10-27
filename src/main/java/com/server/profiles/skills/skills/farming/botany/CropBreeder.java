package com.server.profiles.skills.skills.farming.botany;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.server.profiles.skills.skills.farming.botany.CustomCrop.BreedingRecipe;

/**
 * Represents a Crop Breeder multiblock structure
 * 
 * Structure (3x3x2):
 * Bottom Layer (Y+0):
 *   F F F    F = Farmland
 *   F C F    C = Composter (center)
 *   F F F
 * 
 * Top Layer (Y+1):
 *   Air with 2 item displays floating above center composter
 *   (displays show the two parent crops being bred)
 */
public class CropBreeder {
    
    private final UUID id;
    private final Location centerLocation; // The composter location
    private UUID ownerUuid;
    
    // Breeding state
    private boolean isBreeding;
    private String parentCrop1Id;
    private String parentCrop2Id;
    private long breedingStartTime;
    private long breedingDuration; // in milliseconds
    
    // Display entities for visual feedback
    private UUID displayEntity1Id;
    private UUID displayEntity2Id;
    
    private static final long BASE_BREEDING_TIME = 60000; // 1 minute base time
    
    public CropBreeder(Location centerLocation, UUID ownerUuid) {
        this.id = UUID.randomUUID();
        this.centerLocation = centerLocation.clone();
        this.ownerUuid = ownerUuid;
        this.isBreeding = false;
    }
    
    /**
     * Check if the multiblock structure is valid
     */
    public boolean isStructureValid() {
        Block center = centerLocation.getBlock();
        
        // Check center is composter
        if (center.getType() != Material.COMPOSTER) {
            return false;
        }
        
        // Check surrounding 8 blocks are farmland
        int[][] offsets = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1, 0},           {1, 0},
            {-1, 1},  {0, 1},  {1, 1}
        };
        
        for (int[] offset : offsets) {
            Block farmland = center.getRelative(offset[0], 0, offset[1]);
            if (farmland.getType() != Material.FARMLAND) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Start a breeding process
     * @return true if breeding started, false if invalid
     */
    public boolean startBreeding(Player player, String crop1Id, String crop2Id) {
        if (!isStructureValid()) {
            player.sendMessage("§c✗ Crop Breeder structure is invalid!");
            return false;
        }
        
        if (isBreeding) {
            player.sendMessage("§c✗ This breeder is already breeding crops!");
            return false;
        }
        
        // Get the crops
        CustomCrop crop1 = CustomCropRegistry.getInstance().getCrop(crop1Id);
        CustomCrop crop2 = CustomCropRegistry.getInstance().getCrop(crop2Id);
        
        if (crop1 == null || crop2 == null) {
            player.sendMessage("§c✗ Invalid crops!");
            return false;
        }
        
        // Check for possible breeding results
        List<CustomCrop> possibleResults = CustomCropRegistry.getInstance()
            .findBreedingResults(crop1Id, crop2Id);
        
        if (possibleResults.isEmpty()) {
            player.sendMessage("§c✗ These crops cannot be bred together!");
            player.sendMessage("§7Try different crop combinations.");
            return false;
        }
        
        // Calculate breeding time based on rarity
        long rarityMultiplier = Math.max(crop1.getRarity().ordinal(), crop2.getRarity().ordinal()) + 1;
        this.breedingDuration = BASE_BREEDING_TIME * rarityMultiplier;
        
        // Start breeding
        this.isBreeding = true;
        this.parentCrop1Id = crop1Id;
        this.parentCrop2Id = crop2Id;
        this.breedingStartTime = System.currentTimeMillis();
        
        // Create visual displays
        createDisplayEntities();
        
        // Feedback
        player.sendMessage("§a✓ Started breeding " + crop1.getRarity().getColor() + crop1.getDisplayName() + 
                          "§a and " + crop2.getRarity().getColor() + crop2.getDisplayName() + "§a!");
        player.sendMessage("§7Time required: §f" + (breedingDuration / 1000) + "s");
        player.sendMessage("§7Possible results: §f" + possibleResults.size());
        
        for (CustomCrop result : possibleResults) {
            BreedingRecipe recipe = null;
            for (BreedingRecipe r : result.getRecipes()) {
                if (r.matches(crop1Id, crop2Id)) {
                    recipe = r;
                    break;
                }
            }
            
            if (recipe != null) {
                player.sendMessage("  §8• " + result.getRarity().getColor() + result.getDisplayName() + 
                                  "§8 - §f" + (recipe.getSuccessChance() * 100) + "% §7(Requires Botany " + 
                                  recipe.getRequiredBotanyLevel() + ")");
            }
        }
        
        // Play sound
        player.playSound(centerLocation, Sound.BLOCK_COMPOSTER_READY, 1.0f, 1.0f);
        
        return true;
    }
    
    /**
     * Create visual display entities above the breeder
     */
    private void createDisplayEntities() {
        Location displayLoc1 = centerLocation.clone().add(0.3, 1.5, 0.0);
        Location displayLoc2 = centerLocation.clone().add(0.7, 1.5, 0.0);
        
        CustomCrop crop1 = CustomCropRegistry.getInstance().getCrop(parentCrop1Id);
        CustomCrop crop2 = CustomCropRegistry.getInstance().getCrop(parentCrop2Id);
        
        if (crop1 != null && crop2 != null) {
            // Create first display
            ItemDisplay display1 = (ItemDisplay) centerLocation.getWorld()
                .spawnEntity(displayLoc1, EntityType.ITEM_DISPLAY);
            display1.setItemStack(crop1.createSeedItem());
            display1.setInvulnerable(true);
            display1.setPersistent(false);
            display1.setBillboard(Display.Billboard.FIXED);
            displayEntity1Id = display1.getUniqueId();
            
            // Create second display
            ItemDisplay display2 = (ItemDisplay) centerLocation.getWorld()
                .spawnEntity(displayLoc2, EntityType.ITEM_DISPLAY);
            display2.setItemStack(crop2.createSeedItem());
            display2.setInvulnerable(true);
            display2.setPersistent(false);
            display2.setBillboard(Display.Billboard.FIXED);
            displayEntity2Id = display2.getUniqueId();
        }
    }
    
    /**
     * Remove display entities
     */
    private void removeDisplayEntities() {
        if (displayEntity1Id != null) {
            ItemDisplay display = (ItemDisplay) Bukkit.getEntity(displayEntity1Id);
            if (display != null) {
                display.remove();
            }
            displayEntity1Id = null;
        }
        
        if (displayEntity2Id != null) {
            ItemDisplay display = (ItemDisplay) Bukkit.getEntity(displayEntity2Id);
            if (display != null) {
                display.remove();
            }
            displayEntity2Id = null;
        }
    }
    
    /**
     * Update the breeding process
     * @return The result crop ID if breeding completed, null if still in progress
     */
    public String updateBreeding(Player player) {
        if (!isBreeding) {
            return null;
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - breedingStartTime;
        
        // Check if breeding is complete
        if (elapsed >= breedingDuration) {
            return completeBreeding(player);
        }
        
        // Show progress particles
        if (elapsed % 2000 < 100) { // Every 2 seconds
            centerLocation.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                centerLocation.clone().add(0.5, 1.2, 0.5),
                5, 0.3, 0.3, 0.3, 0.01
            );
        }
        
        return null;
    }
    
    /**
     * Complete the breeding process and determine result
     */
    private String completeBreeding(Player player) {
        List<CustomCrop> possibleResults = CustomCropRegistry.getInstance()
            .findBreedingResults(parentCrop1Id, parentCrop2Id);
        
        if (possibleResults.isEmpty()) {
            // Shouldn't happen, but handle gracefully
            cancelBreeding();
            return null;
        }
        
        // Check player's botany level for each possible result
        // TODO: Get player's botany level from profile
        int botanyLevel = 1; // Placeholder
        
        List<CustomCrop> availableResults = new ArrayList<>();
        List<BreedingRecipe> availableRecipes = new ArrayList<>();
        
        for (CustomCrop crop : possibleResults) {
            for (BreedingRecipe recipe : crop.getRecipes()) {
                if (recipe.matches(parentCrop1Id, parentCrop2Id) && 
                    recipe.getRequiredBotanyLevel() <= botanyLevel) {
                    availableResults.add(crop);
                    availableRecipes.add(recipe);
                }
            }
        }
        
        if (availableResults.isEmpty()) {
            player.sendMessage("§c✗ Your Botany level is too low for any results!");
            player.sendMessage("§7Level up Botany to unlock rarer breeding results.");
            cancelBreeding();
            return null;
        }
        
        // Roll for success based on recipe chances
        double totalWeight = availableRecipes.stream()
            .mapToDouble(BreedingRecipe::getSuccessChance)
            .sum();
        
        double roll = Math.random() * totalWeight;
        double cumulative = 0;
        
        CustomCrop result = null;
        BreedingRecipe usedRecipe = null;
        
        for (int i = 0; i < availableResults.size(); i++) {
            cumulative += availableRecipes.get(i).getSuccessChance();
            if (roll <= cumulative) {
                result = availableResults.get(i);
                usedRecipe = availableRecipes.get(i);
                break;
            }
        }
        
        // Shouldn't happen, but fallback to first result
        if (result == null) {
            result = availableResults.get(0);
            usedRecipe = availableRecipes.get(0);
        }
        
        // Complete breeding
        isBreeding = false;
        removeDisplayEntities();
        
        // Effects
        centerLocation.getWorld().spawnParticle(
            Particle.TOTEM_OF_UNDYING,
            centerLocation.clone().add(0.5, 1.5, 0.5),
            30, 0.5, 0.5, 0.5, 0.1
        );
        centerLocation.getWorld().playSound(
            centerLocation, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f
        );
        
        // Give player the result
        ItemStack resultSeed = result.createSeedItem();
        if (player.getInventory().addItem(resultSeed).isEmpty()) {
            // Successfully added to inventory
            player.sendMessage("§a✓ §lBreeding Complete!");
            player.sendMessage("§aYou created: " + result.getRarity().getColor() + "§l" + result.getDisplayName() + " Seed§a!");
            
            // Award XP
            // TODO: Award botany XP
            player.sendMessage("§7+" + result.getBreedXp() + " Botany XP");
        } else {
            // Inventory full, drop at location
            centerLocation.getWorld().dropItemNaturally(
                centerLocation.clone().add(0.5, 1.0, 0.5),
                resultSeed
            );
            player.sendMessage("§a✓ Breeding complete! Seed dropped (inventory full).");
        }
        
        return result.getId();
    }
    
    /**
     * Cancel ongoing breeding
     */
    public void cancelBreeding() {
        isBreeding = false;
        removeDisplayEntities();
        parentCrop1Id = null;
        parentCrop2Id = null;
    }
    
    /**
     * Get breeding progress percentage
     */
    public double getBreedingProgress() {
        if (!isBreeding) {
            return 0.0;
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - breedingStartTime;
        return Math.min(1.0, (double) elapsed / breedingDuration);
    }
    
    // Getters
    public UUID getId() { return id; }
    public Location getCenterLocation() { return centerLocation.clone(); }
    public UUID getOwnerUuid() { return ownerUuid; }
    public boolean isBreeding() { return isBreeding; }
    public String getParentCrop1Id() { return parentCrop1Id; }
    public String getParentCrop2Id() { return parentCrop2Id; }
}
