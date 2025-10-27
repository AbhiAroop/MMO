# Botany System - Quick Integration Guide

## 📋 What You Need to Do

The core Botany system is implemented, but it needs to be registered with your Main plugin. Follow these steps to complete the integration.

---

## 1. Register BotanyManager in Main.java

### In `onEnable()` method:
```java
// After ProfileManager and SkillRegistry are initialized

// Initialize Botany Manager
getLogger().info("[Botany] Initializing Botany Manager...");
com.server.profiles.skills.skills.farming.botany.BotanyManager.initialize(this);
getLogger().info("[Botany] Botany Manager initialized successfully!");
```

### In `onDisable()` method:
```java
// Before other managers shut down

// Shutdown Botany Manager
if (com.server.profiles.skills.skills.farming.botany.BotanyManager.getInstance() != null) {
    getLogger().info("[Botany] Shutting down Botany Manager...");
    com.server.profiles.skills.skills.farming.botany.BotanyManager.getInstance().shutdown();
    getLogger().info("[Botany] Botany Manager shut down successfully!");
}
```

---

## 2. Register BotanyListener in Main.java

### In `onEnable()` method (after BotanyManager):
```java
// Register Botany Listener
getLogger().info("[Botany] Registering Botany event listener...");
getServer().getPluginManager().registerEvents(
    new com.server.profiles.skills.skills.farming.botany.BotanyListener(this), 
    this
);
getLogger().info("[Botany] Botany listener registered successfully!");
```

---

## 3. (Optional) Add Botany Admin Command

If you want debug/admin commands for Botany, you can add them now or later.

### Create BotanyCommand.java:
```java
package com.server.profiles.skills.skills.farming.botany;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BotanyCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            player.sendMessage("§e§l[Botany Commands]");
            player.sendMessage("§7/botany give <cropId> [amount] §f- Get custom crop seeds");
            player.sendMessage("§7/botany info §f- Show system statistics");
            player.sendMessage("§7/botany nearby [radius] §f- List nearby crops/breeders");
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "give":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /botany give <cropId> [amount]");
                    return true;
                }
                
                String cropId = args[1];
                int amount = args.length >= 3 ? Integer.parseInt(args[2]) : 1;
                
                CustomCrop crop = CustomCropRegistry.getInstance().getCrop(cropId);
                if (crop == null) {
                    player.sendMessage("§cUnknown crop ID: " + cropId);
                    player.sendMessage("§7Valid IDs: golden_wheat, crimson_carrot, ender_berry, crystal_melon, celestial_potato, void_pumpkin");
                    return true;
                }
                
                ItemStack seed = crop.createSeedItem();
                seed.setAmount(amount);
                player.getInventory().addItem(seed);
                player.sendMessage("§a✓ Given " + amount + "x " + crop.getRarity().getColor() + crop.getDisplayName() + " Seed§a!");
                break;
                
            case "info":
                java.util.Map<String, Integer> stats = BotanyManager.getInstance().getStatistics();
                player.sendMessage("§e§l[Botany Statistics]");
                player.sendMessage("§7Total Crops: §f" + stats.get("totalCrops"));
                player.sendMessage("§7Fully Grown: §a" + stats.get("fullyGrownCrops"));
                player.sendMessage("§7Total Breeders: §f" + stats.get("totalBreeders"));
                player.sendMessage("§7Active Breeding: §e" + stats.get("activeBreeders"));
                break;
                
            case "nearby":
                int radius = args.length >= 2 ? Integer.parseInt(args[1]) : 10;
                org.bukkit.Location playerLoc = player.getLocation();
                
                int nearbyCount = 0;
                for (PlantedCustomCrop planted : BotanyManager.getInstance().getCropsByPlayer(player.getUniqueId())) {
                    if (planted.getBlockLocation().distance(playerLoc) <= radius) {
                        nearbyCount++;
                    }
                }
                
                player.sendMessage("§e§l[Nearby Botany Objects]");
                player.sendMessage("§7Crops within " + radius + " blocks: §f" + nearbyCount);
                // Add breeder count if needed
                break;
                
            default:
                player.sendMessage("§cUnknown subcommand: " + subCommand);
                break;
        }
        
        return true;
    }
}
```

### Register in plugin.yml:
```yaml
commands:
  botany:
    description: Botany system commands
    usage: /<command> [give|info|nearby]
    permission: mmo.admin.botany
```

### Register in Main.java onEnable():
```java
getCommand("botany").setExecutor(new com.server.profiles.skills.skills.farming.botany.BotanyCommand());
```

---

## 4. Test Basic Functionality

### Step 1: Start Server
- Check console for initialization messages
- Verify no errors during BotanyManager.initialize()

### Step 2: Get Seeds
```
/botany give golden_wheat 10
```

### Step 3: Plant Crop
- Find farmland or create some
- Right-click farmland with seed
- Should see "§a✓ Planted Golden Wheat!"

### Step 4: Wait for Growth
- Wait ~2 minutes (or check with debug timer)
- Crop should visually update through stages

### Step 5: Harvest
- Break the fully grown crop
- Should get 2-4 Golden Wheat items
- Should see "§a✓ Harvested Golden Wheat x<amount>!"

### Step 6: Build Breeder (Easy Way!)
Use the command to build it automatically:
```
/botany breeder
```
This will build a 3x3 farmland platform with a composter in the center at your feet!

**Or build manually:**
- Place 3x3 farmland
- Place composter in center
- Right-click with 2 Golden Wheat seeds
- Should see breeding start with floating displays

---

## 5. Add Data Persistence (Future)

When you implement MongoDB persistence:

### Save on Server Shutdown:
```java
// In Main.onDisable(), before BotanyManager.shutdown()
BotanyManager manager = BotanyManager.getInstance();
// TODO: Save manager.getAllCrops() to MongoDB
// TODO: Save manager.getAllBreeders() to MongoDB
```

### Load on Server Startup:
```java
// In Main.onEnable(), after BotanyManager.initialize(this)
BotanyManager manager = BotanyManager.getInstance();
// TODO: Load crops from MongoDB and call manager.plantCrop() for each
// TODO: Load breeders from MongoDB and call manager.registerBreeder() for each
```

---

## 6. Skill Tree Integration (Future)

Add these nodes to FarmingSkill's skill tree:

### Unlock Nodes:
- `unlock_golden_wheat` (Level 1) - Already unlocked by default
- `unlock_crimson_carrot` (Level 5)
- `unlock_ender_berry` (Level 15)
- `unlock_crystal_melon` (Level 30)
- `unlock_celestial_potato` (Level 50)
- `unlock_void_pumpkin` (Level 75)

### Bonus Nodes:
- `growth_speed_1/2/3` - Increase crop growth speed by 10%/20%/30%
- `seed_drop_1/2/3` - Increase seed drop chance by 5%/10%/15%
- `breed_success_1/2/3` - Increase breeding success by 10%/20%/30%

---

## 🎮 Quick Test Commands

```bash
# Give yourself Golden Wheat seeds
/botany give YourName golden_wheat 16

# Build a breeder structure instantly
/botany breeder

# Check system statistics
/botany info

# Find nearby crops
/botany nearby 20

# List all crop types
/botany list
```

---

## 🐛 Troubleshooting

### "BotanyManager not initialized" Error
- Ensure `BotanyManager.initialize(this)` is called in `Main.onEnable()`
- Check that it's called AFTER ProfileManager and SkillRegistry

### Crops not appearing after planting
- Verify resource pack is loaded on client
- Check if ItemDisplay entity is spawning (`/execute as @e[type=item_display]`)
- Ensure custom model data is set correctly

### Growth task not working
- Check if BukkitRunnable tasks are starting
- Look for errors in console during task creation
- Verify `manager.getStatistics()` shows crops

### Breeding not starting
- Verify 3x3 farmland + center composter structure
- Check if player has required Botany level
- Ensure breeding recipe exists for the crop combination

---

## ✅ Integration Checklist

- [ ] Added `BotanyManager.initialize(this)` to Main.onEnable()
- [ ] Added `BotanyManager.getInstance().shutdown()` to Main.onDisable()
- [ ] Registered BotanyListener with PluginManager
- [ ] (Optional) Created and registered BotanyCommand
- [ ] Tested planting Golden Wheat seed
- [ ] Tested crop growth (waited 2 minutes)
- [ ] Tested harvesting fully grown crop
- [ ] Built and tested breeder multiblock
- [ ] Verified XP is awarded correctly
- [ ] Resource pack distributed to players (if ready)

---

**Once you complete these steps, the Botany system will be fully operational!**

If you encounter any issues, check the console logs and the debug output (DebugSystem.SKILLS).
