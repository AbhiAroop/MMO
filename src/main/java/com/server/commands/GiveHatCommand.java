package com.server.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.server.items.CustomItems;

public class GiveHatCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        ItemStack witchHat = CustomItems.createWitchHat();
        player.getInventory().addItem(witchHat);
        player.sendMessage("§aYou received a Witch's Hat!");
        return true;
    }
}