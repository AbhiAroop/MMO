package com.server.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.Main;
import com.server.crafting.furnace.FurnaceData;
import com.server.crafting.furnace.FurnaceType;
import com.server.crafting.manager.CustomFurnaceManager;
import com.server.debug.DebugManager.DebugSystem;

/**
 * Admin command for managing custom furnaces
 * Step 2: Administrative tools
 */
public class AdminFurnaceCommand implements CommandExecutor, TabCompleter {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mmo.admin.furnace")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                return handleCreateCommand(sender, args);
            case "remove":
                return handleRemoveCommand(sender, args);
            case "list":
                return handleListCommand(sender, args);
            case "info":
                return handleInfoCommand(sender, args);
            case "give":
                return handleGiveCommand(sender, args);
            case "settemp":
                return handleSetTempCommand(sender, args);
            case "addfuel":
                return handleAddFuelCommand(sender, args);
            case "recipes":
                return handleRecipesCommand(sender, args);
            case "debug":
                return handleDebugCommand(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + subCommand);
                sendHelpMessage(sender);
                return true;
        }
    }
    
    /**
     * Create a furnace at player's location
     */
    private boolean handleCreateCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /adminfurnace create <type>");
            sender.sendMessage(ChatColor.GRAY + "Available types: " + Arrays.stream(FurnaceType.values())
                .map(FurnaceType::name)
                .collect(Collectors.joining(", ")));
            return true;
        }
        
        String typeName = args[1].toUpperCase();
        FurnaceType furnaceType;
        
        try {
            furnaceType = FurnaceType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid furnace type: " + args[1]);
            sender.sendMessage(ChatColor.GRAY + "Available types: " + Arrays.stream(FurnaceType.values())
                .map(FurnaceType::name)
                .collect(Collectors.joining(", ")));
            return true;
        }
        
        Location location = player.getTargetBlock(null, 5).getLocation();
        
        // Place furnace block first
        location.getBlock().setType(Material.FURNACE);
        
        // Create custom furnace
        boolean success = CustomFurnaceManager.getInstance()
            .createCustomFurnace(location, furnaceType, player);
        
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "✓ Created " + furnaceType.getColoredName() + 
                              ChatColor.GREEN + " at your target location!");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to create furnace!");
        }
        
        return true;
    }
    
    /**
     * Remove furnace at player's location
     */
    private boolean handleRemoveCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        Location location = player.getTargetBlock(null, 5).getLocation();
        
        boolean success = CustomFurnaceManager.getInstance()
            .removeCustomFurnace(location, player);
        
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "✓ Removed custom furnace!");
        } else {
            sender.sendMessage(ChatColor.RED + "No custom furnace found at target location!");
        }
        
        return true;
    }
    
    /**
     * List all active furnaces
     */
    private boolean handleListCommand(CommandSender sender, String[] args) {
        Map<String, FurnaceData> furnaces = CustomFurnaceManager.getInstance().getAllFurnaces();
        
        if (furnaces.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No active custom furnaces found.");
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== Active Custom Furnaces ===");
        
        for (Map.Entry<String, FurnaceData> entry : furnaces.entrySet()) {
            FurnaceData data = entry.getValue();
            FurnaceType type = data.getFurnaceType();
            
            sender.sendMessage(ChatColor.GRAY + "• " + type.getColoredName() + 
                              ChatColor.GRAY + " at " + ChatColor.WHITE + entry.getKey());
            sender.sendMessage(ChatColor.GRAY + "  Temperature: " + data.getFormattedTemperature() + 
                              ", Fuel: " + data.getFuelTime() + " ticks");
        }
        
        return true;
    }
    
    /**
     * Show detailed info about furnace at target location
     */
    private boolean handleInfoCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        Location location = player.getTargetBlock(null, 5).getLocation();
        
        FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(location);
        if (furnaceData == null) {
            sender.sendMessage(ChatColor.RED + "No custom furnace found at target location!");
            return true;
        }
        
        FurnaceType type = furnaceData.getFurnaceType();
        
        sender.sendMessage(ChatColor.GOLD + "=== " + type.getColoredName() + ChatColor.GOLD + " Info ===");
        sender.sendMessage(ChatColor.GRAY + "Location: " + ChatColor.WHITE + 
            location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
        sender.sendMessage(type.getTemperatureRange());
        sender.sendMessage(ChatColor.GRAY + "Current Temperature: " + furnaceData.getFormattedTemperature());
        sender.sendMessage(ChatColor.GRAY + "Target Temperature: " + 
            com.server.crafting.temperature.TemperatureSystem.formatTemperature(furnaceData.getTargetTemperature()));
        sender.sendMessage(ChatColor.GRAY + "Fuel Time: " + ChatColor.YELLOW + furnaceData.getFuelTime() + " ticks");
        sender.sendMessage(ChatColor.GRAY + "Status: " + 
            (furnaceData.isActive() ? ChatColor.GREEN + "Active" : ChatColor.RED + "Inactive"));
        
        if (furnaceData.isHeating()) {
            sender.sendMessage(ChatColor.YELLOW + "🔥 Heating up");
        } else if (furnaceData.isCooling()) {
            sender.sendMessage(ChatColor.AQUA + "❄ Cooling down");
        }
        
        if (furnaceData.isOverheating()) {
            sender.sendMessage(ChatColor.RED + "⚠ OVERHEATING! Time: " + furnaceData.getOverheatingTime() + " ticks");
        }
        
        if (furnaceData.isEmergencyShutdown()) {
            sender.sendMessage(ChatColor.DARK_RED + "🛑 EMERGENCY SHUTDOWN");
        }
        
        return true;
    }
    
    /**
     * Give custom furnace item to player
     */
    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /adminfurnace give <player> <type>");
            return true;
        }
        
        Player target = sender.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }
        
        String typeName = args[2].toUpperCase();
        FurnaceType furnaceType;
        
        try {
            furnaceType = FurnaceType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid furnace type: " + args[2]);
            return true;
        }
        
        // Create custom furnace item
        ItemStack furnaceItem = createCustomFurnaceItem(furnaceType);
        target.getInventory().addItem(furnaceItem);
        
        sender.sendMessage(ChatColor.GREEN + "✓ Gave " + furnaceType.getColoredName() + 
                          ChatColor.GREEN + " to " + target.getName());
        target.sendMessage(ChatColor.GREEN + "You received a " + furnaceType.getColoredName() + 
                          ChatColor.GREEN + "!");
        
        return true;
    }
    
    /**
     * Set furnace temperature (debug command)
     */
    private boolean handleSetTempCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /adminfurnace settemp <temperature>");
            return true;
        }
        
        Player player = (Player) sender;
        Location location = player.getTargetBlock(null, 5).getLocation();
        
        FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(location);
        if (furnaceData == null) {
            sender.sendMessage(ChatColor.RED + "No custom furnace found at target location!");
            return true;
        }
        
        try {
            int temperature = Integer.parseInt(args[1]);
            furnaceData.setCurrentTemperature(temperature);
            furnaceData.setTargetTemperature(temperature);
            
            sender.sendMessage(ChatColor.GREEN + "✓ Set furnace temperature to " + 
                com.server.crafting.temperature.TemperatureSystem.formatTemperature(temperature));
            
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid temperature: " + args[1]);
        }
        
        return true;
    }
    
    /**
     * Add fuel to furnace (debug command) - ENHANCED with fuel selection
     */
    private boolean handleAddFuelCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        Location location = player.getTargetBlock(null, 5).getLocation();
        
        FurnaceData furnaceData = CustomFurnaceManager.getInstance().getFurnaceData(location);
        if (furnaceData == null) {
            sender.sendMessage(ChatColor.RED + "No custom furnace found at target location!");
            return true;
        }
        
        // Determine fuel type
        String fuelType = "coal"; // default
        int amount = 64; // default amount
        
        if (args.length >= 2) {
            fuelType = args[1].toLowerCase();
        }
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                amount = Math.max(1, Math.min(64, amount)); // Clamp between 1 and 64
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[2]);
                return true;
            }
        }
        
        ItemStack fuelItem = createFuelItem(fuelType, amount);
        if (fuelItem == null) {
            sender.sendMessage(ChatColor.RED + "Unknown fuel type: " + fuelType);
            sender.sendMessage(ChatColor.YELLOW + "Available fuels: coal, charcoal, coal_block, blaze_rod, lava_bucket, stick, planks");
            return true;
        }
        
        // Add fuel to first available fuel slot
        boolean added = false;
        for (int i = 0; i < furnaceData.getFurnaceType().getFuelSlots(); i++) {
            ItemStack currentFuel = furnaceData.getFuelSlot(i);
            if (currentFuel == null || currentFuel.getType() == Material.AIR) {
                furnaceData.setFuelSlot(i, fuelItem);
                added = true;
                break;
            } else if (currentFuel.isSimilar(fuelItem)) {
                // Try to stack with existing fuel
                int maxStack = currentFuel.getMaxStackSize();
                int currentAmount = currentFuel.getAmount();
                int spaceAvailable = maxStack - currentAmount;
                
                if (spaceAvailable > 0) {
                    int toAdd = Math.min(spaceAvailable, amount);
                    currentFuel.setAmount(currentAmount + toAdd);
                    amount -= toAdd;
                    
                    if (amount <= 0) {
                        added = true;
                        break;
                    }
                }
            }
        }
        
        if (added) {
            com.server.crafting.fuel.FuelData fuelData = 
                com.server.crafting.fuel.FuelRegistry.getInstance().getFuelData(fuelItem);
            
            sender.sendMessage(ChatColor.GREEN + "Added " + fuelItem.getAmount() + "x " + 
                            fuelItem.getType().name().replace("_", " ") + " to furnace!");
            
            if (fuelData != null) {
                sender.sendMessage(ChatColor.GRAY + "Fuel Temperature: " + fuelData.getTemperature());
                sender.sendMessage(ChatColor.GRAY + "Burn Time: " + fuelData.getFormattedBurnTime());
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Furnace fuel slots are full!");
        }
        
        return true;
    }
    
    /**
     * Toggle debug mode for furnace system
     */
    private boolean handleDebugCommand(CommandSender sender, String[] args) {
        boolean currentState = Main.getInstance().isDebugEnabled(DebugSystem.GUI);
        boolean newState = !currentState;
        
        // This would need to be implemented in DebugManager
        // For now, just show current state
        sender.sendMessage(ChatColor.YELLOW + "Furnace debug mode is currently: " + 
                          (currentState ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
        
        return true;
    }

    /**
     * Create a fuel item by type name
     */
    private ItemStack createFuelItem(String fuelType, int amount) {
        Material material;
        
        switch (fuelType.toLowerCase()) {
            case "coal":
                material = Material.COAL;
                break;
            case "charcoal":
                material = Material.CHARCOAL;
                break;
            case "coal_block":
            case "coalblock":
                material = Material.COAL_BLOCK;
                break;
            case "blaze_rod":
            case "blazerod":
                material = Material.BLAZE_ROD;
                break;
            case "lava_bucket":
            case "lavabucket":
            case "lava":
                material = Material.LAVA_BUCKET;
                break;
            case "stick":
                material = Material.STICK;
                break;
            case "planks":
            case "wood":
                material = Material.OAK_PLANKS;
                break;
            case "magma_block":
            case "magmablock":
            case "magma":
                material = Material.MAGMA_BLOCK;
                break;
            case "netherite_scrap":
            case "netheritescrap":
                material = Material.NETHERITE_SCRAP;
                break;
            default:
                return null;
        }
        
        return new ItemStack(material, amount);
    }
    
    /**
     * Create a custom furnace item with NBT data
     */
    private ItemStack createCustomFurnaceItem(FurnaceType furnaceType) {
        ItemStack item = new ItemStack(Material.FURNACE);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(furnaceType.getColoredName());
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + furnaceType.getDescription(),
            "",
            furnaceType.getTemperatureRange(),
            ChatColor.GRAY + "Slots: " + ChatColor.WHITE + 
                furnaceType.getInputSlots() + "I/" + 
                furnaceType.getFuelSlots() + "F/" + 
                furnaceType.getOutputSlots() + "O",
            "",
            ChatColor.YELLOW + "Place to create a " + furnaceType.getDisplayName()
        ));
        
        // Add NBT data to identify furnace type
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(Main.getInstance(), "furnace_type"),
            org.bukkit.persistence.PersistentDataType.STRING,
            furnaceType.name()
        );
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Send help message
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Admin Furnace Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/adminfurnace create <type>" + ChatColor.GRAY + " - Create furnace at target");
        sender.sendMessage(ChatColor.YELLOW + "/adminfurnace remove" + ChatColor.GRAY + " - Remove furnace at target");
        sender.sendMessage(ChatColor.YELLOW + "/adminfurnace list" + ChatColor.GRAY + " - List all active furnaces");
        sender.sendMessage(ChatColor.YELLOW + "/adminfurnace info" + ChatColor.GRAY + " - Show furnace info");
        sender.sendMessage(ChatColor.YELLOW + "/adminfurnace give <player> <type>" + ChatColor.GRAY + " - Give furnace item");
        sender.sendMessage(ChatColor.YELLOW + "/adminfurnace settemp <temp>" + ChatColor.GRAY + " - Set furnace temperature");
        sender.sendMessage(ChatColor.YELLOW + "/adminfurnace addfuel [type] [amount]" + ChatColor.GRAY + " - Add fuel to furnace");
        sender.sendMessage(ChatColor.GRAY + "  Fuel types: coal, charcoal, coal_block, blaze_rod, lava_bucket, stick, planks");
        sender.sendMessage(ChatColor.YELLOW + "/adminfurnace recipes <list|info|temp|all>" + ChatColor.GRAY + " - Recipe management");
        sender.sendMessage(ChatColor.GRAY + "  • " + ChatColor.WHITE + "list" + ChatColor.GRAY + " - Show recipe summary by type");
        sender.sendMessage(ChatColor.GRAY + "  • " + ChatColor.WHITE + "info <recipe_id>" + ChatColor.GRAY + " - Show specific recipe details");
        sender.sendMessage(ChatColor.GRAY + "  • " + ChatColor.WHITE + "temp <temperature>" + ChatColor.GRAY + " - Show recipes for temperature");
        sender.sendMessage(ChatColor.GRAY + "  • " + ChatColor.WHITE + "all" + ChatColor.GRAY + " - Show complete recipe database");
        sender.sendMessage(ChatColor.YELLOW + "/adminfurnace debug" + ChatColor.GRAY + " - Toggle debug mode");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Available furnace types: " + 
            Arrays.stream(FurnaceType.values())
                .map(type -> type.getNameColor() + type.name())
                .collect(Collectors.joining(ChatColor.GRAY + ", ")));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "remove", "list", "info", "give", "settemp", "addfuel", "recipes", "debug"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "create":
                case "give":
                    for (FurnaceType type : FurnaceType.values()) {
                        completions.add(type.name().toLowerCase());
                    }
                    break;
                case "addfuel":
                    completions.addAll(Arrays.asList("coal", "charcoal", "coal_block", "blaze_rod", "lava_bucket", "stick", "planks", "magma_block", "netherite_scrap"));
                    break;
                case "recipes":
                    completions.addAll(Arrays.asList("list", "info", "temp", "all"));
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if ("give".equals(subCommand)) {
                // Add player names
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            } else if ("addfuel".equals(subCommand)) {
                // Add amount suggestions
                completions.addAll(Arrays.asList("1", "8", "16", "32", "64"));
            } else if ("recipes".equals(subCommand) && "info".equals(args[1].toLowerCase())) {
                // Add recipe IDs for info command
                Collection<com.server.crafting.recipes.FurnaceRecipe> recipes = 
                    com.server.crafting.recipes.FurnaceRecipeRegistry.getInstance().getAllRecipes();
                for (com.server.crafting.recipes.FurnaceRecipe recipe : recipes) {
                    completions.add(recipe.getRecipeId());
                }
            } else if ("recipes".equals(subCommand) && "temp".equals(args[1].toLowerCase())) {
                // Add common temperature suggestions
                completions.addAll(Arrays.asList("100", "200", "400", "600", "800", "1000", "1200", "1500", "1800"));
            }
        }
        
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Show recipe information for debugging
     */
    private boolean handleRecipesCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /adminfurnace recipes <list|info <recipe_id>|temp <temperature>|all>");
            return true;
        }
        
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "list":
                listAllRecipes(sender);
                break;
            case "info":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /adminfurnace recipes info <recipe_id>");
                    return true;
                }
                showRecipeInfo(sender, args[2]);
                break;
            case "temp":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /adminfurnace recipes temp <temperature>");
                    return true;
                }
                try {
                    int temperature = Integer.parseInt(args[2]);
                    listRecipesForTemperature(sender, temperature);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid temperature: " + args[2]);
                }
                break;
            case "all":
                showAllRecipeDetails(sender);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown recipes subcommand: " + subCommand);
                sender.sendMessage(ChatColor.YELLOW + "Available subcommands: list, info, temp, all");
                break;
        }
        
        return true;
    }

    /**
     * List all available recipes - ENHANCED: Better organization and statistics
     */
    private void listAllRecipes(CommandSender sender) {
        Collection<com.server.crafting.recipes.FurnaceRecipe> recipes = 
            com.server.crafting.recipes.FurnaceRecipeRegistry.getInstance().getAllRecipes();
        
        sender.sendMessage(ChatColor.GOLD + "=== All Furnace Recipes (" + recipes.size() + " total) ===");
        
        // Group recipes by type and count
        Map<com.server.crafting.recipes.FurnaceRecipe.RecipeType, List<com.server.crafting.recipes.FurnaceRecipe>> 
            recipesByType = recipes.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    com.server.crafting.recipes.FurnaceRecipe::getRecipeType));
        
        // Show summary first
        sender.sendMessage("");
        sender.sendMessage(ChatColor.AQUA + "Recipe Type Summary:");
        for (com.server.crafting.recipes.FurnaceRecipe.RecipeType type : 
            com.server.crafting.recipes.FurnaceRecipe.RecipeType.values()) {
            
            List<com.server.crafting.recipes.FurnaceRecipe> typeRecipes = recipesByType.get(type);
            int count = typeRecipes != null ? typeRecipes.size() : 0;
            
            if (count > 0) {
                // Get temperature range for this type
                int minTemp = typeRecipes.stream()
                    .mapToInt(com.server.crafting.recipes.FurnaceRecipe::getRequiredTemperature)
                    .min().orElse(0);
                int maxTemp = typeRecipes.stream()
                    .mapToInt(com.server.crafting.recipes.FurnaceRecipe::getRequiredTemperature)
                    .max().orElse(0);
                
                sender.sendMessage(ChatColor.GRAY + "  • " + ChatColor.WHITE + type.getDisplayName() + 
                                ChatColor.GRAY + ": " + ChatColor.YELLOW + count + " recipes " +
                                ChatColor.GRAY + "(" + minTemp + "°T - " + maxTemp + "°T)");
            }
        }
        
        // Show detailed recipes by type
        for (com.server.crafting.recipes.FurnaceRecipe.RecipeType type : 
            com.server.crafting.recipes.FurnaceRecipe.RecipeType.values()) {
            
            List<com.server.crafting.recipes.FurnaceRecipe> typeRecipes = recipesByType.get(type);
            if (typeRecipes != null && !typeRecipes.isEmpty()) {
                sender.sendMessage("");
                sender.sendMessage(ChatColor.AQUA + type.getDisplayName() + " (" + typeRecipes.size() + "):");
                sender.sendMessage(ChatColor.GRAY + type.getDescription());
                
                for (com.server.crafting.recipes.FurnaceRecipe recipe : typeRecipes) {
                    String difficultyColor = getDifficultyColor(recipe.getRequiredTemperature());
                    sender.sendMessage(ChatColor.GRAY + "  • " + ChatColor.WHITE + recipe.getDisplayName() + 
                                    ChatColor.GRAY + " - " + difficultyColor + recipe.getFormattedTemperature() + 
                                    ChatColor.GRAY + ", " + ChatColor.YELLOW + recipe.getFormattedCookTime() +
                                    ChatColor.GRAY + " (ID: " + ChatColor.DARK_GRAY + recipe.getRecipeId() + ChatColor.GRAY + ")");
                }
            }
        }
        
        // Show usage instructions
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Use '/adminfurnace recipes info <recipe_id>' for detailed recipe information");
        sender.sendMessage(ChatColor.YELLOW + "Use '/adminfurnace recipes temp <temperature>' to filter by temperature");
        sender.sendMessage(ChatColor.YELLOW + "Use '/adminfurnace recipes all' to see full recipe details");
    }

    /**
     * Show all recipes with complete details - NEW METHOD
     */
    private void showAllRecipeDetails(CommandSender sender) {
        Collection<com.server.crafting.recipes.FurnaceRecipe> recipes = 
            com.server.crafting.recipes.FurnaceRecipeRegistry.getInstance().getAllRecipes();
        
        sender.sendMessage(ChatColor.GOLD + "=== Complete Recipe Database (" + recipes.size() + " recipes) ===");
        
        // Sort recipes by temperature for better organization
        List<com.server.crafting.recipes.FurnaceRecipe> sortedRecipes = recipes.stream()
            .sorted((r1, r2) -> Integer.compare(r1.getRequiredTemperature(), r2.getRequiredTemperature()))
            .collect(java.util.stream.Collectors.toList());
        
        int currentPage = 1;
        int recipesPerPage = 5;
        int totalPages = (int) Math.ceil((double) recipes.size() / recipesPerPage);
        
        for (int i = 0; i < sortedRecipes.size(); i++) {
            com.server.crafting.recipes.FurnaceRecipe recipe = sortedRecipes.get(i);
            
            // Page header
            if (i % recipesPerPage == 0) {
                if (i > 0) {
                    sender.sendMessage("");
                    sender.sendMessage(ChatColor.GRAY + "--- Page " + currentPage + " of " + totalPages + " ---");
                    sender.sendMessage("");
                }
                currentPage++;
            }
            
            // Recipe header
            String difficultyColor = getDifficultyColor(recipe.getRequiredTemperature());
            sender.sendMessage(ChatColor.GOLD + "▼ " + recipe.getDisplayName() + 
                            ChatColor.GRAY + " (ID: " + recipe.getRecipeId() + ")");
            
            // Recipe details
            sender.sendMessage(ChatColor.GRAY + "   Type: " + ChatColor.WHITE + recipe.getRecipeType().getDisplayName());
            sender.sendMessage(ChatColor.GRAY + "   Temperature: " + difficultyColor + recipe.getFormattedTemperature());
            sender.sendMessage(ChatColor.GRAY + "   Cook Time: " + ChatColor.YELLOW + recipe.getFormattedCookTime());
            
            if (!recipe.getDescription().isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "   Description: " + ChatColor.ITALIC + recipe.getDescription());
            }
            
            // Show inputs
            sender.sendMessage(ChatColor.AQUA + "   Inputs:");
            for (ItemStack input : recipe.getInputs()) {
                String inputName = formatItemName(input);
                sender.sendMessage(ChatColor.WHITE + "     • " + input.getAmount() + "x " + inputName);
            }
            
            // Show outputs
            sender.sendMessage(ChatColor.GREEN + "   Outputs:");
            for (ItemStack output : recipe.getOutputs()) {
                String outputName = formatItemName(output);
                sender.sendMessage(ChatColor.WHITE + "     • " + output.getAmount() + "x " + outputName);
            }
            
            // Efficiency information
            double baseEfficiency = com.server.crafting.temperature.TemperatureSystem
                .getTemperatureEfficiency(recipe.getRequiredTemperature(), recipe.getRequiredTemperature());
            double highTempEfficiency = com.server.crafting.temperature.TemperatureSystem
                .getTemperatureEfficiency(recipe.getRequiredTemperature() + 200, recipe.getRequiredTemperature());
            
            if (highTempEfficiency > baseEfficiency) {
                int timeSaved = (int) (recipe.getCookTime() * (1.0 - (baseEfficiency / highTempEfficiency)));
                sender.sendMessage(ChatColor.YELLOW + "   Efficiency: " + ChatColor.GREEN + 
                                String.format("%.0f%% faster", (highTempEfficiency - 1.0) * 100) + 
                                ChatColor.GRAY + " at " + (recipe.getRequiredTemperature() + 200) + "°T " +
                                ChatColor.GRAY + "(" + formatTime(timeSaved) + " saved)");
            }
            
            sender.sendMessage(""); // Spacing between recipes
        }
        
        // Show statistics
        sender.sendMessage(ChatColor.GOLD + "=== Recipe Statistics ===");
        
        // Temperature distribution
        Map<String, Integer> tempRanges = new HashMap<>();
        tempRanges.put("Low (0-400°T)", 0);
        tempRanges.put("Medium (400-800°T)", 0);
        tempRanges.put("High (800-1200°T)", 0);
        tempRanges.put("Very High (1200-1600°T)", 0);
        tempRanges.put("Extreme (1600°T+)", 0);
        
        for (com.server.crafting.recipes.FurnaceRecipe recipe : recipes) {
            int temp = recipe.getRequiredTemperature();
            if (temp < 400) {
                tempRanges.put("Low (0-400°T)", tempRanges.get("Low (0-400°T)") + 1);
            } else if (temp < 800) {
                tempRanges.put("Medium (400-800°T)", tempRanges.get("Medium (400-800°T)") + 1);
            } else if (temp < 1200) {
                tempRanges.put("High (800-1200°T)", tempRanges.get("High (800-1200°T)") + 1);
            } else if (temp < 1600) {
                tempRanges.put("Very High (1200-1600°T)", tempRanges.get("Very High (1200-1600°T)") + 1);
            } else {
                tempRanges.put("Extreme (1600°T+)", tempRanges.get("Extreme (1600°T+)") + 1);
            }
        }
        
        sender.sendMessage(ChatColor.AQUA + "Temperature Distribution:");
        for (Map.Entry<String, Integer> entry : tempRanges.entrySet()) {
            if (entry.getValue() > 0) {
                sender.sendMessage(ChatColor.GRAY + "  • " + ChatColor.WHITE + entry.getKey() + 
                                ChatColor.GRAY + ": " + ChatColor.YELLOW + entry.getValue() + " recipes");
            }
        }
        
        // Average cook time
        double avgCookTime = recipes.stream()
            .mapToInt(com.server.crafting.recipes.FurnaceRecipe::getCookTime)
            .average().orElse(0.0);
        sender.sendMessage(ChatColor.AQUA + "Average Cook Time: " + ChatColor.YELLOW + formatTime((int) avgCookTime));
        
        // Recipe complexity (input count)
        double avgInputs = recipes.stream()
            .mapToInt(r -> r.getInputs().size())
            .average().orElse(0.0);
        sender.sendMessage(ChatColor.AQUA + "Average Inputs per Recipe: " + ChatColor.YELLOW + String.format("%.1f", avgInputs));
    }

    /**
     * Get difficulty color based on temperature
     */
    private String getDifficultyColor(int temperature) {
        if (temperature >= 1600) {
            return ChatColor.DARK_RED.toString();
        } else if (temperature >= 1200) {
            return ChatColor.RED.toString();
        } else if (temperature >= 800) {
            return ChatColor.GOLD.toString();
        } else if (temperature >= 400) {
            return ChatColor.YELLOW.toString();
        } else {
            return ChatColor.GREEN.toString();
        }
    }

    /**
     * Format item name for display
     */
    private String formatItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return ChatColor.stripColor(item.getItemMeta().getDisplayName());
        }
        
        // Convert MATERIAL_NAME to Material Name
        String[] words = item.getType().name().toLowerCase().split("_");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        return formatted.toString();
    }

    /**
     * Format time from ticks to readable format
     */
    private String formatTime(int ticks) {
        int seconds = ticks / 20;
        if (seconds < 60) {
            return seconds + "s";
        } else {
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            return minutes + "m " + remainingSeconds + "s";
        }
    }

    /**
     * Show detailed recipe information
     */
    private void showRecipeInfo(CommandSender sender, String recipeId) {
        com.server.crafting.recipes.FurnaceRecipe recipe = 
            com.server.crafting.recipes.FurnaceRecipeRegistry.getInstance().getRecipe(recipeId);
        
        if (recipe == null) {
            sender.sendMessage(ChatColor.RED + "Recipe not found: " + recipeId);
            return;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== Recipe: " + recipe.getDisplayName() + " ===");
        sender.sendMessage(ChatColor.GRAY + "ID: " + ChatColor.WHITE + recipe.getRecipeId());
        sender.sendMessage(ChatColor.GRAY + "Type: " + ChatColor.WHITE + recipe.getRecipeType().getDisplayName());
        sender.sendMessage(ChatColor.GRAY + "Description: " + ChatColor.WHITE + recipe.getDescription());
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Required Temperature: " + recipe.getFormattedTemperature());
        sender.sendMessage(ChatColor.GRAY + "Cook Time: " + ChatColor.WHITE + recipe.getFormattedCookTime());
        sender.sendMessage("");
        
        sender.sendMessage(ChatColor.AQUA + "Inputs:");
        for (ItemStack input : recipe.getInputs()) {
            sender.sendMessage(ChatColor.WHITE + "  • " + input.getAmount() + "x " + 
                            input.getType().name().replace("_", " "));
        }
        
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GREEN + "Outputs:");
        for (ItemStack output : recipe.getOutputs()) {
            sender.sendMessage(ChatColor.WHITE + "  • " + output.getAmount() + "x " + 
                            output.getType().name().replace("_", " "));
        }
    }

    /**
     * List recipes that can be processed at a given temperature
     */
    private void listRecipesForTemperature(CommandSender sender, int temperature) {
        List<com.server.crafting.recipes.FurnaceRecipe> recipes = 
            com.server.crafting.recipes.FurnaceRecipeRegistry.getInstance().getRecipesForTemperature(temperature);
        
        sender.sendMessage(ChatColor.GOLD + "=== Recipes for " + temperature + "°T (" + recipes.size() + ") ===");
        
        if (recipes.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No recipes can be processed at this temperature.");
            return;
        }
        
        for (com.server.crafting.recipes.FurnaceRecipe recipe : recipes) {
            sender.sendMessage(ChatColor.GRAY + "• " + ChatColor.WHITE + recipe.getDisplayName() + 
                            ChatColor.GRAY + " (" + recipe.getFormattedTemperature() + ")");
        }
    }
}