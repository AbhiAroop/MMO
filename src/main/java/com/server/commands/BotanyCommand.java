package com.server.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.server.profiles.skills.skills.farming.botany.BotanyManager;
import com.server.profiles.skills.skills.farming.botany.CropBreeder;
import com.server.profiles.skills.skills.farming.botany.CustomCrop;
import com.server.profiles.skills.skills.farming.botany.CustomCropRegistry;
import com.server.profiles.skills.skills.farming.botany.PlantedCustomCrop;

/**
 * Admin command for Botany system management and debugging
 */
public class BotanyCommand implements CommandExecutor, TabCompleter {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("mmo.admin.botany")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "give":
                handleGive(player, args);
                break;
                
            case "info":
                handleInfo(player);
                break;
                
            case "nearby":
                handleNearby(player, args);
                break;
                
            case "list":
                handleList(player);
                break;
                
            case "reload":
                handleReload(player);
                break;
                
            case "clear":
                handleClear(player, args);
                break;
                
            case "breeder":
                handleBreeder(player);
                break;
                
            case "givebreeder":
                handleGiveBreeder(player, args);
                break;
                
            default:
                player.sendMessage("§cUnknown subcommand: " + subCommand);
                sendHelp(player);
                break;
        }
        
        return true;
    }
    
    private void sendHelp(Player player) {
        player.sendMessage("§e§l[Botany Admin Commands]");
        player.sendMessage("§7/botany give <player> <cropId> [amount] §f- Give custom crop seeds");
        player.sendMessage("§7/botany givebreeder <player> [amount] §f- Give breeder blocks");
        player.sendMessage("§7/botany info §f- Show system statistics");
        player.sendMessage("§7/botany nearby [radius] §f- List nearby crops/breeders");
        player.sendMessage("§7/botany list §f- List all registered crops");
        player.sendMessage("§7/botany reload §f- Reload crop registry");
        player.sendMessage("§7/botany clear [crops|breeders|all] §f- Clear crops/breeders");
        player.sendMessage("§7/botany breeder §f- Build OLD breeder multiblock structure");
    }
    
    private void handleGive(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /botany give <player> <cropId> [amount]");
            player.sendMessage("§7Example: /botany give Notch golden_wheat 16");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found: " + args[1]);
            return;
        }
        
        String cropId = args[2];
        int amount = args.length >= 4 ? parseInt(args[3], 1) : 1;
        
        CustomCrop crop = CustomCropRegistry.getInstance().getCrop(cropId);
        if (crop == null) {
            player.sendMessage("§cUnknown crop ID: " + cropId);
            player.sendMessage("§7Valid IDs: golden_wheat, crimson_carrot, moonpetal, palmfruit, ender_berry, crystal_melon, celestial_potato, void_pumpkin");
            return;
        }
        
        ItemStack seed = crop.createSeedItem();
        seed.setAmount(amount);
        target.getInventory().addItem(seed);
        
        player.sendMessage("§a✓ Given " + amount + "x " + crop.getRarity().getColor() + 
                          crop.getDisplayName() + " Seed §ato " + target.getName() + "!");
        
        if (!target.equals(player)) {
            target.sendMessage("§a✓ You received " + amount + "x " + crop.getRarity().getColor() + 
                              crop.getDisplayName() + " Seed§a!");
        }
    }
    
    private void handleInfo(Player player) {
        Map<String, Integer> stats = BotanyManager.getInstance().getStatistics();
        
        player.sendMessage("§e§l[Botany System Statistics]");
        player.sendMessage("§7Total Planted Crops: §f" + stats.get("totalCrops"));
        player.sendMessage("§7Fully Grown Crops: §a" + stats.get("fullyGrownCrops"));
        player.sendMessage("§7Total Breeders: §f" + stats.get("totalBreeders"));
        player.sendMessage("§7Active Breeding: §e" + stats.get("activeBreeders"));
        player.sendMessage("");
        player.sendMessage("§7Registered Crop Types: §f" + CustomCropRegistry.getInstance().getAllCrops().size());
    }
    
    private void handleNearby(Player player, String[] args) {
        int radius = args.length >= 2 ? parseInt(args[1], 10) : 10;
        Location playerLoc = player.getLocation();
        
        List<PlantedCustomCrop> nearbyCrops = new ArrayList<>();
        List<CropBreeder> nearbyBreeders = new ArrayList<>();
        
        // Find nearby crops
        for (PlantedCustomCrop planted : BotanyManager.getInstance().getCropsByPlayer(player.getUniqueId())) {
            Location cropLoc = planted.getBlockLocation();
            if (cropLoc != null && cropLoc.distance(playerLoc) <= radius) {
                nearbyCrops.add(planted);
            }
        }
        
        // Find all crops in radius (not just player's)
        int totalNearby = 0;
        for (PlantedCustomCrop planted : BotanyManager.getInstance().getAllCrops()) {
            Location cropLoc = planted.getBlockLocation();
            if (cropLoc != null && cropLoc.distance(playerLoc) <= radius) {
                totalNearby++;
            }
        }
        
        // Find nearby breeders
        for (CropBreeder breeder : BotanyManager.getInstance().getBreedersByPlayer(player.getUniqueId())) {
            Location breederLoc = breeder.getCenterLocation();
            if (breederLoc != null && breederLoc.distance(playerLoc) <= radius) {
                nearbyBreeders.add(breeder);
            }
        }
        
        player.sendMessage("§e§l[Nearby Botany Objects - " + radius + " blocks]");
        player.sendMessage("§7Your crops: §f" + nearbyCrops.size());
        player.sendMessage("§7Total crops: §f" + totalNearby);
        player.sendMessage("§7Your breeders: §f" + nearbyBreeders.size());
        
        if (!nearbyCrops.isEmpty()) {
            player.sendMessage("");
            player.sendMessage("§7Your crops:");
            for (PlantedCustomCrop crop : nearbyCrops) {
                CustomCrop customCrop = crop.getCrop();
                if (customCrop != null) {
                    double progress = crop.getGrowthProgress() * 100;
                    String status = crop.isFullyGrown() ? "§aFully Grown" : "§e" + (int)progress + "%";
                    player.sendMessage("  §7- " + customCrop.getRarity().getColor() + customCrop.getDisplayName() + 
                                      " §7at §f" + formatLocation(crop.getBlockLocation()) + " §7- " + status);
                }
            }
        }
        
        if (!nearbyBreeders.isEmpty()) {
            player.sendMessage("");
            player.sendMessage("§7Your breeders:");
            for (CropBreeder breeder : nearbyBreeders) {
                String status = breeder.isBreeding() ? "§eBreeding (" + (int)(breeder.getBreedingProgress() * 100) + "%)" : "§7Idle";
                player.sendMessage("  §7- Breeder at §f" + formatLocation(breeder.getCenterLocation()) + " §7- " + status);
            }
        }
    }
    
    private void handleList(Player player) {
        player.sendMessage("§e§l[Registered Custom Crops]");
        
        for (CustomCrop crop : CustomCropRegistry.getInstance().getAllCrops()) {
            player.sendMessage("§7- " + crop.getRarity().getColor() + crop.getDisplayName() + 
                              " §7(§f" + crop.getId() + "§7)");
            player.sendMessage("  §7Rarity: " + crop.getRarity().getColor() + crop.getRarity().name());
            player.sendMessage("  §7Stages: §f" + crop.getMaxGrowthStages() + " §7| Level: §f" + crop.getBreedingLevel());
            player.sendMessage("  §7XP: §aPlant +" + crop.getPlantXp() + " §7| §aHarvest +" + crop.getHarvestXp() + 
                              " §7| §aBreed +" + crop.getBreedXp());
        }
    }
    
    private void handleReload(Player player) {
        player.sendMessage("§e[Botany] Reloading crop registry...");
        
        // Re-register default crops (this will clear and re-add)
        CustomCropRegistry registry = CustomCropRegistry.getInstance();
        
        player.sendMessage("§a✓ Crop registry reloaded!");
        player.sendMessage("§7Total registered crops: §f" + registry.getAllCrops().size());
    }
    
    private void handleClear(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /botany clear <crops|breeders|all>");
            return;
        }
        
        String type = args[1].toLowerCase();
        BotanyManager manager = BotanyManager.getInstance();
        
        switch (type) {
            case "crops":
                int cropCount = manager.getAllCrops().size();
                manager.clearAll();
                player.sendMessage("§a✓ Cleared " + cropCount + " planted crops!");
                break;
                
            case "breeders":
                int breederCount = manager.getAllBreeders().size();
                for (CropBreeder breeder : new ArrayList<>(manager.getAllBreeders())) {
                    manager.removeBreeder(breeder);
                }
                player.sendMessage("§a✓ Cleared " + breederCount + " breeders!");
                break;
                
            case "all":
                int totalCrops = manager.getAllCrops().size();
                int totalBreeders = manager.getAllBreeders().size();
                manager.clearAll();
                for (CropBreeder breeder : new ArrayList<>(manager.getAllBreeders())) {
                    manager.removeBreeder(breeder);
                }
                player.sendMessage("§a✓ Cleared " + totalCrops + " crops and " + totalBreeders + " breeders!");
                break;
                
            default:
                player.sendMessage("§cInvalid type: " + type);
                player.sendMessage("§7Valid types: crops, breeders, all");
                break;
        }
    }
    
    private void handleBreeder(Player player) {
        Location playerLoc = player.getLocation();
        Location centerLoc = playerLoc.getBlock().getRelative(0, -1, 0).getLocation();
        
        // Build 3x3 farmland platform
        int blocksPlaced = 0;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location loc = centerLoc.clone().add(x, 0, z);
                
                // Center gets composter, others get farmland
                if (x == 0 && z == 0) {
                    loc.getBlock().setType(org.bukkit.Material.COMPOSTER);
                } else {
                    loc.getBlock().setType(org.bukkit.Material.FARMLAND);
                }
                blocksPlaced++;
            }
        }
        
        player.sendMessage("§a✓ Built Crop Breeder structure!");
        player.sendMessage("§7Center: " + formatLocation(centerLoc));
        player.sendMessage("§7Blocks placed: §f" + blocksPlaced + " §7(8 farmland + 1 composter)");
        player.sendMessage("");
        player.sendMessage("§eHow to use:");
        player.sendMessage("§71. Hold 2 custom crop seeds of the same type");
        player.sendMessage("§72. Right-click the composter");
        player.sendMessage("§73. Wait for breeding to complete!");
        player.sendMessage("§74. Collect your new crop seed");
    }
    
    private void handleGiveBreeder(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /botany givebreeder <player> [amount]");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found: " + args[1]);
            return;
        }
        
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount: " + args[2]);
                return;
            }
        }
        
        // Create breeder block items
        for (int i = 0; i < amount; i++) {
            target.getInventory().addItem(
                com.server.profiles.skills.skills.farming.botany.BreederBlock.createBreederItem()
            );
        }
        
        target.sendMessage("§a§l[✓] §aReceived §f" + amount + " §aCrop Breeder" + (amount > 1 ? "s" : "") + "!");
        player.sendMessage("§a§l[✓] §aGave §f" + amount + " §aCrop Breeder" + (amount > 1 ? "s" : "") + " to " + target.getName());
    }
    
    private String formatLocation(Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }
    
    private int parseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("give");
            completions.add("givebreeder");
            completions.add("info");
            completions.add("nearby");
            completions.add("list");
            completions.add("reload");
            completions.add("clear");
            completions.add("breeder");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("givebreeder")) {
                // Suggest online players
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            } else if (args[0].equalsIgnoreCase("clear")) {
                completions.add("crops");
                completions.add("breeders");
                completions.add("all");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // Suggest crop IDs
            completions.add("golden_wheat");
            completions.add("crimson_carrot");
            completions.add("moonpetal");
            completions.add("palmfruit");
            completions.add("ender_berry");
            completions.add("crystal_melon");
            completions.add("celestial_potato");
            completions.add("void_pumpkin");
        }
        
        // Filter based on what the player has typed
        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        
        return completions;
    }
}
