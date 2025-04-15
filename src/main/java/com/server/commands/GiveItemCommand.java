package com.server.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.server.items.CustomItems;

public class GiveItemCommand implements CommandExecutor {
    private final Map<String, Supplier<ItemStack>> itemCreators = new HashMap<>();
    
    public GiveItemCommand() {
        // Register all custom items here
        itemCreators.put("witchhat", CustomItems::createWitchHat);
        itemCreators.put("apprenticeedge", CustomItems::createApprenticeEdge);
        itemCreators.put("emberwood", CustomItems::createEmberwoodStaff);
        itemCreators.put("arcloom", CustomItems::createArcloom);
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
}