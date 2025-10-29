package com.server.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.server.items.CustomItems;
import com.server.items.ItemManager;

public class GiveItemCommand implements CommandExecutor, TabCompleter {
    private final Map<String, Supplier<ItemStack>> itemCreators = new HashMap<>();
    
    public GiveItemCommand() {
        // Register all custom items here
        itemCreators.put("witchhat", CustomItems::createWitchHat);
        itemCreators.put("apprenticeedge", CustomItems::createApprenticeEdge);
        itemCreators.put("emberwood", CustomItems::createEmberwoodStaff);
        itemCreators.put("arcloom", CustomItems::createArcloom);
        itemCreators.put("crownofmagnus", CustomItems::createCrownOfMagnus);
        itemCreators.put("siphonfang", CustomItems::createSiphonFang);
        itemCreators.put("fleshrake", CustomItems::createFleshrake);
        itemCreators.put("shatteredshell", CustomItems::createShatteredShellPickaxe);
         itemCreators.put("rustypickaxe", CustomItems::createRustyCrumbledPickaxe);
        itemCreators.put("rustycrumbled", CustomItems::createRustyCrumbledPickaxe);
        itemCreators.put("rootpickaxe", CustomItems::createRootCrackedPickaxe);
        itemCreators.put("rootcracked", CustomItems::createRootCrackedPickaxe);
        itemCreators.put("copperheadpickaxe", CustomItems::createCopperheadPickaxe);
        itemCreators.put("copperhead", CustomItems::createCopperheadPickaxe);
        itemCreators.put("forgedcopper", CustomItems::createForgedCopperPickaxe);
        itemCreators.put("forgedcopperpickaxe", CustomItems::createForgedCopperPickaxe);

        // Add Wanderer's Weave armor pieces individually
        itemCreators.put("wanderershood", CustomItems::createWanderersWeaveHood);
        itemCreators.put("wandererstunic", CustomItems::createWanderersWeaveTunic);
        itemCreators.put("wandererleggings", CustomItems::createWanderersWeaveLeggings);
        itemCreators.put("wandererboots", CustomItems::createWanderersWeaveBoots);        
        // Add full set option - special case handling in onCommand
        itemCreators.put("wandererset", () -> CustomItems.createWanderersWeaveHood());
        
        // Add fishing rods
        itemCreators.put("bamboorod", CustomItems::createBambooRod);
        itemCreators.put("bamboo", CustomItems::createBambooRod);
        
        // Add more items as they are created
        
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /giveitem <itemname> [player]");
            sender.sendMessage("§eAvailable items: §f" + String.join(", ", itemCreators.keySet()));
            return true;
        }
        
        String itemName = args[0].toLowerCase();
        
        // Special case for the Wanderer's Set
        boolean isWandererSet = itemName.equals("wandererset");
        
        if (!isWandererSet && !itemCreators.containsKey(itemName)) {
            sender.sendMessage("§cUnknown item: " + itemName);
            sender.sendMessage("§eAvailable items: §f" + String.join(", ", itemCreators.keySet()));
            return true;
        }
        
        Player targetPlayer;
        
        if (args.length > 1) {
            // Give to specified player
            targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage("§cPlayer not found: " + args[1]);
                return true;
            }
        } else if (sender instanceof Player) {
            // Give to command sender
            targetPlayer = (Player) sender;
        } else {
            sender.sendMessage("§cPlease specify a player when using this command from console");
            return true;
        }

        // ENHANCEMENT: Apply automatic enhancements to given items
        if (args.length >= 2 && args[1].equalsIgnoreCase("vanilla")) {
            // Special vanilla item giving with automatic enhancement
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /giveitem vanilla <material> [amount] [player]");
                return true;
            }
            
            String materialName = args[2].toUpperCase();
            int amount = 1;
            
            if (args.length >= 4) {
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid amount: " + args[3]);
                    return true;
                }
            }

            if (args.length >= 5) {
                targetPlayer = Bukkit.getPlayer(args[4]);
                if (targetPlayer == null) {
                    sender.sendMessage("§cPlayer not found: " + args[4]);
                    return true;
                }
            } else if (sender instanceof Player) {
                targetPlayer = (Player) sender;
            } else {
                sender.sendMessage("§cPlease specify a player when using this command from console");
                return true;
            }
            
            try {
                Material material = Material.valueOf(materialName);
                ItemStack vanillaItem = new ItemStack(material, amount);
                
                // CRITICAL: Apply automatic enhancements including fuel lore
                ItemStack enhanced = ItemManager.enhanceItemWithAllProperties(vanillaItem);
                
                targetPlayer.getInventory().addItem(enhanced);
                
                String materialDisplayName = material.name().toLowerCase().replace("_", " ");
                sender.sendMessage("§aGave " + amount + "x Enhanced " + materialDisplayName + " to " + targetPlayer.getName());
                
                if (sender != targetPlayer) {
                    targetPlayer.sendMessage("§aYou received: " + amount + "x Enhanced " + materialDisplayName);
                }
                
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cInvalid material: " + materialName);
            }
            
            return true;
        }
        
        // Handle special case for Wanderer's Set
        if (isWandererSet) {
            // Give all Wanderer's Weave armor pieces
            ItemStack hood = CustomItems.createWanderersWeaveHood();
            ItemStack tunic = CustomItems.createWanderersWeaveTunic();
            ItemStack leggings = CustomItems.createWanderersWeaveLeggings();
            ItemStack boots = CustomItems.createWanderersWeaveBoots();
            
            // Give each piece
            targetPlayer.getInventory().addItem(hood, tunic, leggings, boots);
            
            // Notify sender and receiver
            sender.sendMessage("§aGave §6§lWanderer's Weave Set §ato " + targetPlayer.getName());
            if (sender != targetPlayer) {
                targetPlayer.sendMessage("§aYou received: §6§lWanderer's Weave Set");
            }
            
            return true;
        }
        
        // Handle normal single item case
        Supplier<ItemStack> itemCreator = itemCreators.get(itemName);
        ItemStack item = itemCreator.get();
        targetPlayer.getInventory().addItem(item);
        
        String itemDisplayName = item.getItemMeta().getDisplayName();
        sender.sendMessage("§aGave " + itemDisplayName + " §ato " + targetPlayer.getName());
        if (sender != targetPlayer) {
            targetPlayer.sendMessage("§aYou received: " + itemDisplayName);
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument: item name
            String partialItem = args[0].toLowerCase();
            completions.addAll(itemCreators.keySet().stream()
                              .filter(item -> item.startsWith(partialItem))
                              .collect(Collectors.toList()));
        } else if (args.length == 2) {
            // Second argument: player name
            String partialName = args[1].toLowerCase();
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                             .map(Player::getName)
                             .filter(name -> name.toLowerCase().startsWith(partialName))
                             .collect(Collectors.toList()));
        }
        
        return completions;
    }
}