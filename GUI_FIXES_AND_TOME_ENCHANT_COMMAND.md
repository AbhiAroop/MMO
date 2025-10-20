# GUI Fixes and Tome Enchant Command Update

## Summary
Fixed GUI functionality issues with `/enchant open` command, improved player messages for anvil operations, and added a new admin command for adding enchantments to tomes with custom properties.

## Changes Overview

### 1. Fixed GUI Registration Issue

**Problem**: When using `/enchant open <altar|anvil>`, players could move glass panes around and the GUIs were not functional.

**Root Cause**: The GUIs were not being registered with their respective listeners, so click events were not being handled properly.

**Solution**:
- Added `anvilGUIListener` field to `Main.java` (line 104)
- Stored AnvilGUIListener instance instead of creating it inline (line 281)
- Added getter methods in `Main.java`:
  - `getEnchantmentGUIListener()` (lines 601-607)
  - `getAnvilGUIListener()` (lines 609-615)
- Updated `EnchantCommand.handleOpen()` to register GUIs before opening (lines 372 and 387)

### 2. Improved Player Messages

**Before**: Simple one-line messages for anvil operations
```
✓ Items combined successfully!
Cost: 25 XP & 15 Essence
```

**After**: Formatted message boxes for better visibility

#### Success Message:
```
╔══════════════════════════════════════════╗
║ ✓ ITEMS COMBINED SUCCESSFULLY             ║
╠══════════════════════════════════════════╣
║ Cost: 25 XP & 15 Essence              ║
╚══════════════════════════════════════════╝
```

#### Failure Message:
```
╔══════════════════════════════════════════╗
║ ✗ ALL ENCHANTMENTS FAILED                 ║
╠══════════════════════════════════════════╣
║ Lost: 25 XP, 15 Essence, Tome   ║
║ Returned: Your item (unchanged)       ║
╚══════════════════════════════════════════╝
```

### 3. New Admin Command: `/enchant addtomeenchant`

**Purpose**: Add enchantments to enchantment tomes with full control over properties.

**Usage**:
```
/enchant addtomeenchant <enchantmentId> [quality] [level] [applyChance]
```

**Parameters**:
- `enchantmentId` - The ID of the enchantment to add
- `quality` (optional) - POOR, COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC, GODLY (default: COMMON)
- `level` (optional) - 1-8 (I-VIII) (default: 1)
- `applyChance` (optional) - 0-100% (default: 100)

**Examples**:
```
/enchant addtomeenchant emberveil
/enchant addtomeenchant emberveil LEGENDARY
/enchant addtomeenchant emberveil LEGENDARY 5
/enchant addtomeenchant emberveil LEGENDARY 5 75
```

**Requirements**:
- Player must be holding an enchantment tome (blank or already enchanted)
- Enchantment must exist in the registry

**Features**:
- **Works on blank tomes**: Converts blank tomes (BOOK) to enchanted tomes (ENCHANTED_BOOK)
- **Works on enchanted tomes**: Can add additional enchantments to already-enchanted tomes
- Adds enchantment directly to tome NBT data
- Stores custom apply chance (useful for testing)
- Allows admin override of max levels (with warning)
- **Automatic lore updates**: Regenerates complete lore showing ALL enchantments
- **Apply chance color coding**: Green (80-100%), Yellow (50-79%), Gold (25-49%), Red (0-24%)
- Tab completion for all parameters
- Stores element data automatically
- Updates enchantment count on tome
- **Automatically converts blank tomes to enchanted tomes** with proper material (ENCHANTED_BOOK)
- **Generates and applies proper lore** showing all enchantments with apply chances
- **Updates lore when adding to existing enchanted tomes**

**Implementation Details**:
The command manually writes to NBT using the following structure:
```
MMO_Enchant_<index>_ID: enchantmentId
MMO_Enchant_<index>_Quality: quality name
MMO_Enchant_<index>_Level: numeric level (1-8)
MMO_Enchant_<index>_ApplyChance: percentage (0-100)
MMO_Enchant_<index>_Element: element name (if applicable)
MMO_EnchantCount: total enchantments on tome
```

### 4. Updated Help and Tab Completion

- Added `addtomeenchant` to command list
- Added tab completion for:
  - Command name (arg 1)
  - Enchantment ID (arg 2)
  - Quality (arg 3)
  - Level (arg 4)
  - Apply Chance suggestions (arg 5): 0, 25, 50, 75, 100

## Code Changes

### Main.java
**Added Fields**:
```java
private com.server.enchantments.listeners.AnvilGUIListener anvilGUIListener; // Line 104
```

**Updated Listener Registration**:
```java
anvilGUIListener = new com.server.enchantments.listeners.AnvilGUIListener(this);
this.getServer().getPluginManager().registerEvents(anvilGUIListener, this); // Line 281
```

**Added Getters**:
```java
public EnchantmentGUIListener getEnchantmentGUIListener() { return enchantmentGUIListener; }
public com.server.enchantments.listeners.AnvilGUIListener getAnvilGUIListener() { return anvilGUIListener; }
```

### EnchantCommand.java
**Added Import**:
```java
import com.server.Main;
import de.tr7zw.changeme.nbtapi.NBTItem;
```

**Added to Switch Statement** (Line 64):
```java
case "addtomeenchant":
    return handleAddTomeEnchant(sender, args);
```

**New Method** (Lines 204-330):
```java
private boolean handleAddTomeEnchant(CommandSender sender, String[] args) {
    // Full implementation with NBT manipulation
    // Converts blank tomes to enchanted tomes
    // Updates lore for all enchantments
}
```

**New Helper Method** (Lines 395-485):
```java
private ItemStack updateTomeLore(ItemStack tome) {
    // Reads all enchantments from NBT
    // Generates formatted lore with apply chances
    // Color-codes apply chances (green/yellow/gold/red)
    // Includes enchantment descriptions
}
```

**Updated handleOpen()** (Lines 372 and 387):
```java
// Register the GUI with the listener
Main.getInstance().getEnchantmentGUIListener().registerGUI(player, gui);
// and
Main.getInstance().getAnvilGUIListener().registerGUI(player, anvilGui);
```

**Updated Help Text** (Line 918):
```java
sender.sendMessage(ChatColor.YELLOW + "/enchant addtomeenchant <enchantmentId> [quality] [level] [applyChance]" + 
                 ChatColor.GRAY + " - Add enchantment to tome");
```

**Updated Tab Completion** (Lines 948, 960, 987):
- Added "addtomeenchant" to subcommand list
- Added enchantment ID suggestions for arg 2
- Added quality suggestions for arg 3
- Added level suggestions for arg 4
- Added apply chance suggestions for arg 5

### AnvilGUIListener.java
**Updated Success Message** (Lines 512-523):
```java
// Send formatted success message
player.sendMessage("");
player.sendMessage(ChatColor.DARK_GRAY + "╔══════════════════════════════════════════╗");
player.sendMessage(ChatColor.DARK_GRAY + "║ " + ChatColor.GREEN + "✓ ITEMS COMBINED SUCCESSFULLY" + ChatColor.DARK_GRAY + "             ║");
// ... (full box formatting)
player.sendMessage("");
```

**Updated Failure Message** (Lines 459-469):
```java
// Send formatted failure message
player.sendMessage("");
player.sendMessage(ChatColor.DARK_GRAY + "╔══════════════════════════════════════════╗");
player.sendMessage(ChatColor.DARK_GRAY + "║ " + ChatColor.RED + "✗ ALL ENCHANTMENTS FAILED" + ChatColor.DARK_GRAY + "                 ║");
// ... (full box formatting)
player.sendMessage("");
```

## Testing Checklist

### GUI Registration
- [ ] `/enchant open altar` - Opens enchanter GUI, can't move glass panes
- [ ] `/enchant open anvil` - Opens anvil GUI, can't move glass panes
- [ ] Enchanter GUI - All slots function correctly (item, fragments, output)
- [ ] Anvil GUI - All slots function correctly (input 1, input 2, output)
- [ ] GUI closing returns items properly

### Tome Enchant Command
- [ ] `/enchant addtomeenchant emberveil` - Adds COMMON I with 100% apply chance
- [ ] Blank tome converts to enchanted book with proper lore
- [ ] `/enchant addtomeenchant emberveil LEGENDARY` - Adds LEGENDARY I with 100%
- [ ] `/enchant addtomeenchant emberveil LEGENDARY 5` - Adds LEGENDARY V with 100%
- [ ] `/enchant addtomeenchant emberveil LEGENDARY 5 75` - Adds LEGENDARY V with 75%
- [ ] Add second enchantment to existing enchanted tome - lore updates properly
- [ ] Lore shows all enchantments with correct formatting
- [ ] Apply chance colors: Green (80-100%), Yellow (50-79%), Gold (25-49%), Red (0-24%)
- [ ] Enchantment descriptions appear in lore
- [ ] Command only works when holding a tome
- [ ] Command shows error for invalid enchantment IDs
- [ ] Command shows error for invalid quality
- [ ] Command shows error for invalid level (out of range 1-8)
- [ ] Command shows error for invalid apply chance (out of range 0-100)
- [ ] Tab completion works for all arguments

### Message Formatting
- [ ] Successful anvil combine shows formatted box message
- [ ] Failed anvil combine (all enchants fail) shows formatted box message
- [ ] Messages are properly aligned and readable
- [ ] Blank lines before/after box improve visibility

## Build Information

**Build Status**: ✅ SUCCESS

**Build Command**:
```bash
mvn clean package -DskipTests
```

**Output**: `target/mmo-0.0.1.jar`

**Auto-copied to**: `C:\Users\Abhi\Desktop\AI Paper Server\plugins`

## Notes

### Tome Conversion Process
**When adding an enchantment to a BLANK tome**:
1. NBT data is added to the blank tome (BOOK material)
2. Item is converted to ENCHANTED_BOOK material
3. Display name set to "Enchanted Tome" with purple bold formatting
4. Custom model data set to 1002 (ENCHANTED_TOME_MODEL)
5. Glow effect added (hidden UNBREAKING enchantment)
6. All NBT data is copied to the new enchanted book
7. Lore is generated showing all enchantments with apply chances

**When adding an enchantment to an ALREADY-ENCHANTED tome**:
1. NBT data for the new enchantment is added
2. Enchantment count (MMO_EnchantCount) is incremented
3. Meta is verified/updated (display name, custom model, glow)
4. Lore is regenerated showing ALL enchantments (old + new)
5. Item in hand is updated with new data

Both scenarios use the `updateTomeLore()` method to regenerate the complete lore.

### Lore Generation
The `updateTomeLore()` helper method:
- Reads all enchantments from NBT (MMO_EnchantCount determines how many)
- For each enchantment, displays:
  - Quality-colored name with level (e.g., `▸ Emberveil V [Legendary]`)
  - Apply chance with color coding:
    - 🟢 Green: 80-100% (very likely)
    - 🟡 Yellow: 50-79% (likely)
    - 🟠 Gold: 25-49% (moderate)
    - 🔴 Red: 0-24% (unlikely)
  - Enchantment description (word-wrapped at ~40 characters)
- Adds usage instructions and decoration
- Updates both blank → enchanted and enchanted → enchanted scenarios

### NBT Structure Compatibility
The `addtomeenchant` command uses the same NBT structure as the existing tome system:
- Prefix: `MMO_Enchant_<index>_`
- Counter: `MMO_EnchantCount`
- Compatible with anvil system and tome application

### Admin Override
The command allows setting levels beyond the enchantment's max level with a warning message. This is intentional for admin testing purposes.

### Apply Chance Flexibility
Unlike normal tome creation (which uses random apply chance), this command allows specifying exact apply chance values. This is useful for:
- Testing anvil mechanics
- Creating guaranteed-success tomes
- Creating failure-prone tomes for testing
- Creating tomes with specific success rates

## Related Documentation
- `SOUND_EFFECTS_AND_COMMANDS_UPDATE.md` - Previous sound effects update
- `FRAGMENT_BOOST_REFUND_SYSTEM.md` - Fragment refund system
- `CUSTOM_ANVIL_SYSTEM_COMPLETE.md` - Anvil system documentation

## Git Commit Message
```
Fix GUI registration, improve anvil messages, add tome enchant command

- Fixed /enchant open GUIs not being functional (added GUI registration)
- Added getter methods for GUI listeners in Main class
- Improved anvil success/failure messages with formatted boxes
- Added /enchant addtomeenchant command for custom tome enchanting
- Added tab completion for new command with all parameters
- Updated help text with new command
```
