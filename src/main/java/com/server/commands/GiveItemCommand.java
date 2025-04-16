package com.server.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.server.items.CustomItems;

public class GiveItemCommand implements CommandExecutor, TabCompleter {
    private final Map<String, Supplier<ItemStack>> itemCreators = new HashMap<>();
    
    public GiveItemCommand() {
        // Register all custom items here
        itemCreators.put("witchhat", CustomItems::createWitchHat);
        itemCreators.put("apprenticeedge", CustomItems::createApprenticeEdge);
        itemCreators.put("emberwood", CustomItems::createEmberwoodStaff);
        itemCreators.put("arcloom", CustomItems::createArcloom);
        itemCreators.put("crownofmagnus", CustomItems::createCrownOfMagnus);
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
        Supplier<ItemStack> itemCreator = itemCreators.get(itemName);
        
        if (itemCreator == null) {
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
        
        // Create the item and give it to the player
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