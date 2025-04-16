package com.server.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.Main;
import com.server.profiles.PlayerProfile;
import com.server.profiles.ProfileManager;
import com.server.utils.CurrencyFormatter;

/**
 * Command handler for currency-related commands
 */
public class CurrencyCommand implements CommandExecutor, TabCompleter {
    
    private final Main plugin;

    // Track pending transfers for confirmation
    private static class TransferRequest {
        final Player sender;
        final Player recipient;
        final String currencyType;
        final int amount;
        final long timestamp;
        
        public TransferRequest(Player sender, Player recipient, String currencyType, int amount) {
            this.sender = sender;
            this.recipient = recipient;
            this.currencyType = currencyType;
            this.amount = amount;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Check if request has expired (2 minutes)
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 120000;
        }
        
        @Override
        public String toString() {
            return "TransferRequest{sender=" + sender.getName() + 
                ", recipient=" + recipient.getName() + 
                ", currencyType=" + currencyType + 
                ", amount=" + amount + 
                ", age=" + (System.currentTimeMillis() - timestamp)/1000 + "s}";
        }
    }
        
    // Map to store pending transfers: player UUID -> transfer request
    private final Map<UUID, TransferRequest> pendingTransfers = new HashMap<>();
    
    public CurrencyCommand(Main plugin) {
        this.plugin = plugin;
        
        // Schedule task to clean up expired transfer requests
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredRequests();
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // Run every minute (20 ticks/sec * 60 sec)
    }
    
    /**
     * Clean up expired transfer requests
     */
    private void cleanupExpiredRequests() {
        // Create a copy of the entry set to avoid ConcurrentModificationException
        pendingTransfers.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired();
            if (expired && plugin.isDebugMode()) {
                plugin.getLogger().info("Removed expired transfer request from " + 
                                     entry.getValue().sender.getName());
            }
            return expired;
        });
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("CurrencyCommand handling: " + command.getName() + " (label: " + label + ") from " + 
                                sender.getName() + " with " + args.length + " args");
        }
        
        // Use command.getName() instead of label for more reliability
        String commandName = command.getName().toLowerCase();
        
        if (commandName.equals("balance") || commandName.equals("bal") || commandName.equals("money")) {
            return handleBalanceCommand(sender, args);
        } else if (commandName.equals("currency")) {
            return handleCurrencyAdminCommand(sender, args);
        } else if (commandName.equals("pay")) {
            return handlePayCommand(sender, args);
        } else if (commandName.equals("confirm")) {
            return handleConfirmCommand(sender);
        } else if (commandName.equals("cancel")) {
            return handleCancelCommand(sender);
        }
        
        return false;
    }
    
    /**
     * Handle the /pay command for players to transfer currency to each other
     */
    private boolean handlePayCommand(CommandSender sender, String[] args) {
        // Only players can use the pay command
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check for pending transfers
        if (pendingTransfers.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You already have a pending transfer. Use /confirm to confirm or /cancel to cancel.");
            return true;
        }
        
        // Check args
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /pay <player> <amount> <units|premium>");
            return true;
        }
        
        // Parse target player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found or not online");
            return true;
        }
        
        // Can't pay yourself
        if (target.equals(player)) {
            player.sendMessage(ChatColor.RED + "You cannot pay yourself!");
            return true;
        }
        
        // Parse amount
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
            if (amount <= 0) {
                player.sendMessage(ChatColor.RED + "Amount must be positive");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
            return true;
        }
        
        // Parse currency type
        String currencyType = args[2].toLowerCase();
        if (!currencyType.equals("units") && !currencyType.equals("premium")) {
            player.sendMessage(ChatColor.RED + "You can only transfer 'units' or 'premium' currency");
            return true;
        }
        
        // Get sender's profile
        Integer senderSlot = ProfileManager.getInstance().getActiveProfile(player.getUniqueId());
        if (senderSlot == null) {
            player.sendMessage(ChatColor.RED + "You don't have an active profile");
            return true;
        }
        PlayerProfile senderProfile = ProfileManager.getInstance().getProfiles(player.getUniqueId())[senderSlot];
        
        // Get recipient's profile
        Integer recipientSlot = ProfileManager.getInstance().getActiveProfile(target.getUniqueId());
        if (recipientSlot == null) {
            player.sendMessage(ChatColor.RED + "The recipient doesn't have an active profile");
            return true;
        }
        PlayerProfile recipientProfile = ProfileManager.getInstance().getProfiles(target.getUniqueId())[recipientSlot];
        
        // Check if sender has enough currency
        boolean hasSufficientFunds = false;
        if (currencyType.equals("units")) {
            hasSufficientFunds = senderProfile.getUnits() >= amount;
        } else if (currencyType.equals("premium")) {
            hasSufficientFunds = senderProfile.getPremiumUnits() >= amount;
        }
        
        if (!hasSufficientFunds) {
            player.sendMessage(ChatColor.RED + "You don't have enough " + 
                             (currencyType.equals("units") ? "Units" : "Premium Units") + " to complete this transfer");
            return true;
        }
        
        // Determine if confirmation is needed
        boolean requireConfirmation = false;
        if (currencyType.equals("units") && amount >= 10000) {
            requireConfirmation = true;
        } else if (currencyType.equals("premium") && amount >= 100) {
            requireConfirmation = true;
        }
        
        if (requireConfirmation) {
            // Store pending transfer
            TransferRequest request = new TransferRequest(player, target, currencyType, amount);
            pendingTransfers.put(player.getUniqueId(), request);
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Created pending transfer request for " + player.getName() + 
                                     " to " + target.getName() + ": " + amount + " " + currencyType);
            }
            
            // Send confirmation message
            String formattedAmount = currencyType.equals("units") 
                ? CurrencyFormatter.formatUnits(amount)
                : CurrencyFormatter.formatPremiumUnits(amount);
                
            player.sendMessage(ChatColor.GOLD + "Are you sure you want to send " + formattedAmount + 
                             ChatColor.GOLD + " to " + target.getName() + "?");
            player.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.GREEN + "/confirm" + 
                             ChatColor.YELLOW + " to confirm or " + ChatColor.RED + "/cancel" + 
                             ChatColor.YELLOW + " to cancel. This request will expire in 2 minutes.");
            return true;
        } else {
            // Process the transfer directly for small amounts
            return processTransfer(player, target, currencyType, amount);
        }
    }
    
    /**
     * Handle the /confirm command to confirm a pending transfer
     */
    private boolean handleConfirmCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players");
            return true;
        }
        
        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();
        
        // Debug all pending transfers to see what's happening
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Confirm command from " + player.getName() + " (UUID: " + playerUUID + ")");
            plugin.getLogger().info("All pending transfers: " + pendingTransfers.size());
            for (Map.Entry<UUID, TransferRequest> entry : pendingTransfers.entrySet()) {
                plugin.getLogger().info("  - Transfer from: " + entry.getValue().sender.getName() + 
                                    " (UUID: " + entry.getKey() + ")");
            }
        }
        
        // Check if there's a pending transfer for this player
        TransferRequest request = pendingTransfers.get(playerUUID);
        
        if (request == null) {
            sender.sendMessage(ChatColor.RED + "You don't have any pending transfers to confirm");
            return true;
        }
        
        // Check if request has expired
        if (request.isExpired()) {
            pendingTransfers.remove(playerUUID);
            player.sendMessage(ChatColor.RED + "Your transfer request has expired. Please try again.");
            return true;
        }
        
        // Check if recipient is still online
        if (!request.recipient.isOnline()) {
            pendingTransfers.remove(playerUUID);
            player.sendMessage(ChatColor.RED + "The recipient is no longer online");
            return true;
        }
        
        // Remove the request first to prevent double-processing
        pendingTransfers.remove(playerUUID);
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Processing confirmed transfer from " + player.getName() + 
                                " to " + request.recipient.getName() + ": " + 
                                request.amount + " " + request.currencyType);
        }
        
        // Process the transfer
        return processTransfer(request.sender, request.recipient, request.currencyType, request.amount);
    }
    
    /**
     * Handle the /cancel command to cancel a pending transfer
     */
    private boolean handleCancelCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players");
            return true;
        }
        
        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();
        
        // Debug all pending transfers
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Cancel command from " + player.getName() + " (UUID: " + playerUUID + ")");
            plugin.getLogger().info("All pending transfers: " + pendingTransfers.size());
            for (Map.Entry<UUID, TransferRequest> entry : pendingTransfers.entrySet()) {
                plugin.getLogger().info("  - Transfer from: " + entry.getValue().sender.getName() + 
                                    " (UUID: " + entry.getKey() + ")");
            }
        }
        
        TransferRequest request = pendingTransfers.remove(playerUUID);
        if (request != null) {
            player.sendMessage(ChatColor.YELLOW + "Transfer cancelled");
        } else {
            player.sendMessage(ChatColor.RED + "You don't have any pending transfers to cancel");
        }
        
        return true;
    }
    
    /**
     * Process a currency transfer between players
     */
    private boolean processTransfer(Player sender, Player recipient, String currencyType, int amount) {
        // Get profiles
        Integer senderSlot = ProfileManager.getInstance().getActiveProfile(sender.getUniqueId());
        if (senderSlot == null) {
            sender.sendMessage(ChatColor.RED + "Error: You no longer have an active profile");
            return true;
        }
        
        PlayerProfile senderProfile = ProfileManager.getInstance().getProfiles(sender.getUniqueId())[senderSlot];
        if (senderProfile == null) {
            sender.sendMessage(ChatColor.RED + "Error: Cannot access your profile");
            return true;
        }
        
        Integer recipientSlot = ProfileManager.getInstance().getActiveProfile(recipient.getUniqueId());
        if (recipientSlot == null) {
            sender.sendMessage(ChatColor.RED + "Error: Recipient no longer has an active profile");
            return true;
        }
        
        PlayerProfile recipientProfile = ProfileManager.getInstance().getProfiles(recipient.getUniqueId())[recipientSlot];
        if (recipientProfile == null) {
            sender.sendMessage(ChatColor.RED + "Error: Cannot access recipient's profile");
            return true;
        }
        
        // Process transfer based on currency type
        boolean success = false;
        String formattedAmount = "";
        String currencyName = "";
        
        if (currencyType.equals("units")) {
            // Check again to prevent potential exploits
            if (senderProfile.getUnits() < amount) {
                sender.sendMessage(ChatColor.RED + "You don't have enough Units to complete this transfer");
                return true;
            }
            
            // Transfer units
            senderProfile.removeUnits(amount);
            recipientProfile.addUnits(amount);
            success = true;
            formattedAmount = CurrencyFormatter.formatUnits(amount);
            currencyName = "Units";
        } else if (currencyType.equals("premium")) {
            // Check again to prevent potential exploits
            if (senderProfile.getPremiumUnits() < amount) {
                sender.sendMessage(ChatColor.RED + "You don't have enough Premium Units to complete this transfer");
                return true;
            }
            
            // Transfer premium units
            senderProfile.removePremiumUnits(amount);
            recipientProfile.addPremiumUnits(amount);
            success = true;
            formattedAmount = CurrencyFormatter.formatPremiumUnits(amount);
            currencyName = "Premium Units";
        }
        
        if (success) {
            // Send messages
            sender.sendMessage(ChatColor.GREEN + "You sent " + formattedAmount + ChatColor.GREEN + " to " + recipient.getName());
            recipient.sendMessage(ChatColor.GREEN + "You received " + formattedAmount + ChatColor.GREEN + " from " + sender.getName());
            
            // Log the transaction
            plugin.getLogger().info("[Currency] " + sender.getName() + " sent " + amount + " " + currencyName + " to " + recipient.getName());
        }
        
        return true;
    }
    
    // [Original handleBalanceCommand method continues below]
    private boolean handleBalanceCommand(CommandSender sender, String[] args) {
        Player target;
        
        // Check if viewing own balance or another player's (with permission)
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console cannot check balance. Use /balance <player>");
                return true;
            }
            target = (Player) sender;
        } else {
            // Checking another player's balance requires permission
            if (!sender.hasPermission("mmo.admin.balance")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to check other players' balances");
                return true;
            }
            
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found or not online");
                return true;
            }
        }
        
        // Get player's active profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(target.getUniqueId());
        if (activeSlot == null) {
            sender.sendMessage(ChatColor.RED + "Player doesn't have an active profile");
            return true;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(target.getUniqueId())[activeSlot];
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "Error accessing player profile");
            return true;
        }
        
        // Display balance information
        sender.sendMessage(ChatColor.GOLD + "=== " + (target == sender ? "Your" : target.getName() + "'s") + " Balance ===");
        sender.sendMessage(ChatColor.YELLOW + "Units: " + CurrencyFormatter.formatUnits(profile.getUnits()));
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Premium Units: " + CurrencyFormatter.formatPremiumUnits(profile.getPremiumUnits()));
        sender.sendMessage(ChatColor.AQUA + "Essence: " + CurrencyFormatter.formatEssence(profile.getEssence()));
        
        // Only show Bits to the player themselves or to admins
        if (target == sender || sender.hasPermission("mmo.admin.balance")) {
            sender.sendMessage(ChatColor.GREEN + "Bits: " + CurrencyFormatter.formatBits(profile.getBits()));
        }
        
        return true;
    }
    
    /**
     * Handle the /currency command for admins to modify balances
     */
    private boolean handleCurrencyAdminCommand(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("mmo.admin.currency")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command");
            return true;
        }
        
        // Validate args
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /currency <give|take|set> <player> <currency> <amount>");
            sender.sendMessage(ChatColor.GRAY + "Currencies: units, premium, essence, bits");
            return true;
        }
        
        String action = args[0].toLowerCase();
        String playerName = args[1];
        String currencyType = args[2].toLowerCase();
        int amount;
        
        try {
            amount = Integer.parseInt(args[3]);
            if (amount < 0) {
                sender.sendMessage(ChatColor.RED + "Amount must be positive");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[3]);
            return true;
        }
        
        // Get target player
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or not online");
            return true;
        }
        
        // Get player's active profile
        Integer activeSlot = ProfileManager.getInstance().getActiveProfile(target.getUniqueId());
        if (activeSlot == null) {
            sender.sendMessage(ChatColor.RED + "Player doesn't have an active profile");
            return true;
        }
        
        PlayerProfile profile = ProfileManager.getInstance().getProfiles(target.getUniqueId())[activeSlot];
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "Error accessing player profile");
            return true;
        }
        
        // Process the command based on currency type
        String currencyName;
        int newBalance;
        String formattedAmount;
        
        switch (currencyType) {
            case "units":
            case "unit":
                currencyName = "Units";
                formattedAmount = CurrencyFormatter.formatUnits(amount);
                
                if ("give".equals(action)) {
                    newBalance = profile.addUnits(amount);
                    sender.sendMessage(ChatColor.GREEN + "Gave " + formattedAmount + ChatColor.GREEN + " to " + target.getName());
                    target.sendMessage(ChatColor.GREEN + "You received " + formattedAmount);
                } else if ("take".equals(action)) {
                    if (!profile.removeUnits(amount)) {
                        sender.sendMessage(ChatColor.RED + "Player doesn't have enough Units");
                        return true;
                    }
                    newBalance = profile.getUnits();
                    sender.sendMessage(ChatColor.GREEN + "Took " + formattedAmount + ChatColor.GREEN + " from " + target.getName());
                    target.sendMessage(ChatColor.RED + "You lost " + formattedAmount);
                } else if ("set".equals(action)) {
                    profile.setUnits(amount);
                    newBalance = amount;
                    sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s Units to " + formattedAmount);
                    target.sendMessage(ChatColor.YELLOW + "Your Units balance was set to " + formattedAmount);
                } else {
                    sender.sendMessage(ChatColor.RED + "Unknown action: " + action);
                    return true;
                }
                break;
                
            case "premium":
            case "premiumunits":
                currencyName = "Premium Units";
                formattedAmount = CurrencyFormatter.formatPremiumUnits(amount);
                
                if ("give".equals(action)) {
                    newBalance = profile.addPremiumUnits(amount);
                    sender.sendMessage(ChatColor.GREEN + "Gave " + formattedAmount + ChatColor.GREEN + " to " + target.getName());
                    target.sendMessage(ChatColor.GREEN + "You received " + formattedAmount);
                } else if ("take".equals(action)) {
                    if (!profile.removePremiumUnits(amount)) {
                        sender.sendMessage(ChatColor.RED + "Player doesn't have enough Premium Units");
                        return true;
                    }
                    newBalance = profile.getPremiumUnits();
                    sender.sendMessage(ChatColor.GREEN + "Took " + formattedAmount + ChatColor.GREEN + " from " + target.getName());
                    target.sendMessage(ChatColor.RED + "You lost " + formattedAmount);
                } else if ("set".equals(action)) {
                    profile.setPremiumUnits(amount);
                    newBalance = amount;
                    sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s Premium Units to " + formattedAmount);
                    target.sendMessage(ChatColor.YELLOW + "Your Premium Units balance was set to " + formattedAmount);
                } else {
                    sender.sendMessage(ChatColor.RED + "Unknown action: " + action);
                    return true;
                }
                break;
                
            case "essence":
                currencyName = "Essence";
                formattedAmount = CurrencyFormatter.formatEssence(amount);
                
                if ("give".equals(action)) {
                    newBalance = profile.addEssence(amount);
                    sender.sendMessage(ChatColor.GREEN + "Gave " + formattedAmount + ChatColor.GREEN + " to " + target.getName());
                    target.sendMessage(ChatColor.GREEN + "You received " + formattedAmount);
                } else if ("take".equals(action)) {
                    if (!profile.removeEssence(amount)) {
                        sender.sendMessage(ChatColor.RED + "Player doesn't have enough Essence");
                        return true;
                    }
                    newBalance = profile.getEssence();
                    sender.sendMessage(ChatColor.GREEN + "Took " + formattedAmount + ChatColor.GREEN + " from " + target.getName());
                    target.sendMessage(ChatColor.RED + "You lost " + formattedAmount);
                } else if ("set".equals(action)) {
                    profile.setEssence(amount);
                    newBalance = amount;
                    sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s Essence to " + formattedAmount);
                    target.sendMessage(ChatColor.YELLOW + "Your Essence balance was set to " + formattedAmount);
                } else {
                    sender.sendMessage(ChatColor.RED + "Unknown action: " + action);
                    return true;
                }
                break;
                
            case "bits":
            case "bit":
                currencyName = "Bits";
                formattedAmount = CurrencyFormatter.formatBits(amount);
                
                if ("give".equals(action)) {
                    newBalance = profile.addBits(amount);
                    sender.sendMessage(ChatColor.GREEN + "Gave " + formattedAmount + ChatColor.GREEN + " to " + target.getName());
                    target.sendMessage(ChatColor.GREEN + "You received " + formattedAmount);
                } else if ("take".equals(action)) {
                    if (!profile.removeBits(amount)) {
                        sender.sendMessage(ChatColor.RED + "Player doesn't have enough Bits");
                        return true;
                    }
                    newBalance = profile.getBits();
                    sender.sendMessage(ChatColor.GREEN + "Took " + formattedAmount + ChatColor.GREEN + " from " + target.getName());
                    target.sendMessage(ChatColor.RED + "You lost " + formattedAmount);
                } else if ("set".equals(action)) {
                    profile.setBits(amount);
                    newBalance = amount;
                    sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s Bits to " + formattedAmount);
                    target.sendMessage(ChatColor.YELLOW + "Your Bits balance was set to " + formattedAmount);
                } else {
                    sender.sendMessage(ChatColor.RED + "Unknown action: " + action);
                    return true;
                }
                break;
                
            default:
                sender.sendMessage(ChatColor.RED + "Unknown currency: " + currencyType);
                sender.sendMessage(ChatColor.GRAY + "Available currencies: units, premium, essence, bits");
                return true;
        }
        
        // Log the currency operation
        plugin.getLogger().info("[Currency] " + sender.getName() + " " + action + " " + amount + 
                              " " + currencyName + " " + (action.equals("set") ? "to" : "from/to") + 
                              " " + target.getName() + ". New balance: " + newBalance);
        
        return true;
    }

        @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // Different tab completion logic for different commands
        if (command.getName().equalsIgnoreCase("balance") || command.getName().equalsIgnoreCase("bal") || command.getName().equalsIgnoreCase("money")) {
            if (args.length == 1) {
                // Player name for balance command
                String partialName = args[0].toLowerCase();
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                                 .map(Player::getName)
                                 .filter(name -> name.toLowerCase().startsWith(partialName))
                                 .collect(Collectors.toList()));
            }
        } else if (command.getName().equalsIgnoreCase("currency")) {
            if (args.length == 1) {
                // First argument: action
                List<String> actions = Arrays.asList("give", "take", "set");
                String partialAction = args[0].toLowerCase();
                completions.addAll(actions.stream()
                                 .filter(action -> action.startsWith(partialAction))
                                 .collect(Collectors.toList()));
            } else if (args.length == 2) {
                // Second argument: player name
                String partialName = args[1].toLowerCase();
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                                 .map(Player::getName)
                                 .filter(name -> name.toLowerCase().startsWith(partialName))
                                 .collect(Collectors.toList()));
            } else if (args.length == 3) {
                // Third argument: currency type
                List<String> currencyTypes = Arrays.asList("units", "premium", "essence", "bits");
                String partialType = args[2].toLowerCase();
                completions.addAll(currencyTypes.stream()
                                 .filter(type -> type.startsWith(partialType))
                                 .collect(Collectors.toList()));
            } else if (args.length == 4) {
                // Fourth argument: suggest some amounts
                List<String> amounts = Arrays.asList("100", "1000", "10000", "100000");
                String partialAmount = args[3].toLowerCase();
                completions.addAll(amounts.stream()
                                 .filter(amount -> amount.startsWith(partialAmount))
                                 .collect(Collectors.toList()));
            }
        } else if (command.getName().equalsIgnoreCase("pay")) {
            if (args.length == 1) {
                // First argument: player name
                String partialName = args[0].toLowerCase();
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                                 .map(Player::getName)
                                 .filter(name -> !name.equals(sender.getName())) // Can't pay yourself
                                 .filter(name -> name.toLowerCase().startsWith(partialName))
                                 .collect(Collectors.toList()));
            } else if (args.length == 2) {
                // Second argument: suggest some amounts
                List<String> amounts = Arrays.asList("100", "1000", "10000", "100000");
                String partialAmount = args[1].toLowerCase();
                completions.addAll(amounts.stream()
                                 .filter(amount -> amount.startsWith(partialAmount))
                                 .collect(Collectors.toList()));
            } else if (args.length == 3) {
                // Third argument: currency type (only units and premium for pay)
                List<String> currencyTypes = Arrays.asList("units", "premium");
                String partialType = args[2].toLowerCase();
                completions.addAll(currencyTypes.stream()
                                 .filter(type -> type.startsWith(partialType))
                                 .collect(Collectors.toList()));
            }
        }
        
        return completions;
    }

}