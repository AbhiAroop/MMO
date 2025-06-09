package com.server.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            case "recipes": // NEW
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
     * Add fuel to furnace (debug command)
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
        
        // Add coal to first fuel slot
        ItemStack coal = new ItemStack(Material.COAL, 64);
        furnaceData.setFuelSlot(0, coal);
        
        sender.sendMessage(ChatColor.GREEN + "✓ Added 64 coal to furnace fuel slot!");
        
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
        sender.sendMessage(ChatColor.YELLOW + "/adminfurnace addfuel" + ChatColor.GRAY + " - Add fuel to furnace");
        sender.sendMessage(ChatColor.YELLOW + "/adminfurnace recipes <list|info|temp>" + ChatColor.GRAY + " - Recipe management");
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
            completions.addAll(Arrays.asList("create", "remove", "list", "info", "give", "settemp", "addfuel", "debug"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if ("create".equals(subCommand) || "give".equals(subCommand)) {
                completions.addAll(Arrays.stream(FurnaceType.values())
                    .map(Enum::name)
                    .collect(Collectors.toList()));
            } else if ("give".equals(subCommand)) {
                completions.addAll(sender.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()));
            }
        } else if (args.length == 3 && "give".equals(args[0].toLowerCase())) {
            completions.addAll(Arrays.stream(FurnaceType.values())
                .map(Enum::name)
                .collect(Collectors.toList()));
        }
        
        return completions.stream()
            .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }

    /**
     * Show recipe information for debugging
     */
    private boolean handleRecipesCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /adminfurnace recipes <list|info <recipe_id>|temp <temperature>>");
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
            default:
                sender.sendMessage(ChatColor.RED + "Unknown recipes subcommand: " + subCommand);
                break;
        }
        
        return true;
    }

    /**
     * List all available recipes
     */
    private void listAllRecipes(CommandSender sender) {
        Collection<com.server.crafting.recipes.FurnaceRecipe> recipes = 
            com.server.crafting.recipes.FurnaceRecipeRegistry.getInstance().getAllRecipes();
        
        sender.sendMessage(ChatColor.GOLD + "=== All Furnace Recipes (" + recipes.size() + ") ===");
        
        Map<com.server.crafting.recipes.FurnaceRecipe.RecipeType, List<com.server.crafting.recipes.FurnaceRecipe>> 
            recipesByType = recipes.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    com.server.crafting.recipes.FurnaceRecipe::getRecipeType));
        
        for (com.server.crafting.recipes.FurnaceRecipe.RecipeType type : 
            com.server.crafting.recipes.FurnaceRecipe.RecipeType.values()) {
            
            List<com.server.crafting.recipes.FurnaceRecipe> typeRecipes = recipesByType.get(type);
            if (typeRecipes != null && !typeRecipes.isEmpty()) {
                sender.sendMessage("");
                sender.sendMessage(ChatColor.AQUA + type.getDisplayName() + " (" + typeRecipes.size() + "):");
                
                for (com.server.crafting.recipes.FurnaceRecipe recipe : typeRecipes) {
                    sender.sendMessage(ChatColor.GRAY + "  • " + ChatColor.WHITE + recipe.getDisplayName() + 
                                    ChatColor.GRAY + " - " + recipe.getFormattedTemperature() + 
                                    ", " + recipe.getFormattedCookTime());
                }
            }
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