package com.server.profiles.skills.skills.farming.botany;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.EulerAngle;

/**
 * Represents a custom breeder block using an armor stand with custom model data
 */
public class BreederBlock {
    
    private static final int BREEDER_CMD = 200001; // Custom model data for breeder block
    
    private final UUID armorStandId;
    private final Location location;
    private ArmorStand armorStand;
    private final TextDisplay textDisplay; // For multi-line nameplate
    
    public BreederBlock(Location location) {
        this.location = location.clone();
        this.armorStand = spawnArmorStand(location);
        this.armorStandId = armorStand.getUniqueId();
        this.textDisplay = spawnTextDisplay(location);
    }
    
    /**
     * Spawn the armor stand with custom model data helmet
     */
    private ArmorStand spawnArmorStand(Location loc) {
        Location spawnLoc = loc.clone().add(0.5, 0, 0.5);
        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        
        // Configure armor stand
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setPersistent(true);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setSmall(false);
        stand.setMarker(true); // Prevents collision
        stand.setCustomNameVisible(false);
        
        // Set head rotation to look like a block
        stand.setHeadPose(new EulerAngle(0, 0, 0));
        
        // Create custom model data helmet
        ItemStack helmet = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = helmet.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(BREEDER_CMD);
            helmet.setItemMeta(meta);
        }
        stand.getEquipment().setHelmet(helmet);
        
        return stand;
    }
    
    /**
     * Spawn a text display entity for multi-line nameplate
     */
    private TextDisplay spawnTextDisplay(Location loc) {
        Location displayLoc = loc.clone().add(0.5, 2.0, 0.5); // 2 blocks above breeder
        TextDisplay display = (TextDisplay) loc.getWorld().spawnEntity(displayLoc, EntityType.TEXT_DISPLAY);
        
        // Configure text display
        display.setBillboard(Display.Billboard.CENTER);
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setSeeThrough(false);
        display.setDefaultBackground(false);
        display.setText(""); // Start with no text
        display.setVisibleByDefault(true);
        
        return display;
    }
    
    /**
     * Update the nameplate above the breeder with detailed information
     */
    public void updateNameplate(String status, int remainingSeconds) {
        if (textDisplay != null && textDisplay.isValid()) {
            BreederData data = BotanyManager.getInstance().getBreederData(armorStandId);
            
            if (data != null && data.isBreeding()) {
                // Build multi-line display text
                StringBuilder text = new StringBuilder();
                text.append("§6§l⚗ CROP BREEDER ⚗§r\n");
                text.append("§7━━━━━━━━━━━━━━━\n");
                
                // Get recipe info
                BreederRecipe recipe = data.getActiveRecipe();
                if (recipe != null) {
                    // Get crop info from recipe output
                    ItemStack output = recipe.getOutput();
                    CustomCrop outputCrop = CustomCropRegistry.getInstance().getCropFromSeed(output);
                    
                    if (outputCrop != null) {
                        text.append("§eBreeding: ").append(outputCrop.getRarity().getColor())
                            .append(outputCrop.getDisplayName()).append("\n");
                    } else {
                        text.append("§eBreeding...§r\n");
                    }
                }
                
                text.append("§7━━━━━━━━━━━━━━━\n");
                
                // Status and time
                if (remainingSeconds > 0) {
                    int minutes = remainingSeconds / 60;
                    int seconds = remainingSeconds % 60;
                    text.append("§aStatus: §e").append(status).append("\n");
                    text.append("§aTime: §f").append(String.format("%d:%02d", minutes, seconds)).append("\n");
                } else {
                    text.append("§aStatus: §2§l").append(status).append("\n");
                }
                
                text.append("§7━━━━━━━━━━━━━━━");
                
                textDisplay.setText(text.toString());
            } else {
                // Idle state
                StringBuilder text = new StringBuilder();
                text.append("§6§l⚗ CROP BREEDER ⚗§r\n");
                text.append("§7━━━━━━━━━━━━━━━\n");
                text.append("§eRight-click to use\n");
                text.append("§7Breed custom crops!\n");
                text.append("§7━━━━━━━━━━━━━━━");
                
                textDisplay.setText(text.toString());
            }
        }
    }
    
    /**
     * Hide the nameplate
     */
    public void hideNameplate() {
        if (textDisplay != null && textDisplay.isValid()) {
            textDisplay.setText("");
        }
    }
    
    /**
     * Open the breeder GUI for a player
     */
    public void openGUI(Player player) {
        BreederData data = BotanyManager.getInstance().getBreederData(armorStandId);
        if (data == null) {
            data = new BreederData(armorStandId, location);
            BotanyManager.getInstance().registerBreederData(data);
        }
        
        BreederGUI gui = new BreederGUI(data);
        gui.open(player);
    }
    
    /**
     * Remove this breeder block
     */
    public void remove() {
        if (armorStand != null && armorStand.isValid()) {
            armorStand.remove();
        }
        if (textDisplay != null && textDisplay.isValid()) {
            textDisplay.remove();
        }
        BotanyManager.getInstance().removeBreederData(armorStandId);
    }
    
    /**
     * Check if the armor stand is still valid
     */
    public boolean isValid() {
        if (armorStand == null || !armorStand.isValid()) {
            // Try to find it again
            armorStand = (ArmorStand) location.getWorld().getNearbyEntities(location, 1, 1, 1)
                .stream()
                .filter(e -> e.getUniqueId().equals(armorStandId))
                .findFirst()
                .orElse(null);
        }
        return armorStand != null && armorStand.isValid();
    }
    
    // Getters
    public UUID getArmorStandId() {
        return armorStandId;
    }
    
    public Location getLocation() {
        return location.clone();
    }
    
    public ArmorStand getArmorStand() {
        return armorStand;
    }
    
    public static int getBreederCMD() {
        return BREEDER_CMD;
    }
    
    /**
     * Create a breeder block item for giving to players
     */
    public static ItemStack createBreederItem() {
        ItemStack item = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(BREEDER_CMD);
            meta.setDisplayName("§6§lCrop Breeder");
            meta.setLore(java.util.Arrays.asList(
                "§7Place this block to create a breeder",
                "§7Right-click to open the breeding GUI",
                "§7",
                "§eBreed custom crops together!"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
}
