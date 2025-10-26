# Bedrock Custom Tool Detection Implementation

## Summary
Fixed the Bedrock mining speed system to properly detect **custom pickaxes** (carrot_on_a_stick with CustomModelData) instead of only recognizing vanilla pickaxes.

## Problem
- Stone bricks were taking **60 ticks (3 seconds)** to break instead of the proper **8-16 ticks**
- Root cause: Custom items (carrot_on_a_stick with CustomModelData 213XXX) were not recognized as pickaxes
- System was using "bare hands" timing (30 base ticks) instead of "pickaxe" timing (8 base ticks)
- With 0.5 mining speed: 30 / 0.5 = 60 ticks = 3 seconds (wrong)
- Expected: 8 / 0.5 = 16 ticks = 0.8 seconds (correct)

## Solution Implemented

### 1. Updated Tool Detection Methods
Modified all tool checking methods in `BedrockMiningSpeedHandler.java` to:
- Accept `ItemStack` instead of `Material`
- Check vanilla tools first (Material type check)
- Check custom items via CustomModelData ranges

### 2. Custom Model Data Ranges
Based on your `EquipmentTypeValidator.java` and `CustomItems.java`:
- **Pickaxes**: 213000-213999 (e.g., 213001-213005)
- **Swords**: 210000-210999 (e.g., 210001-210002)
- **Weapons**: 210000-219999 (general range)
- **Shields**: 220000-229999
- **Armor**: 230000-239999
- **Ranged**: 250000-259999
- **Staves**: 260000-269999

### 3. Tool Detection Methods Updated

#### `isPickaxe(ItemStack toolItem)`
```java
// Check vanilla pickaxes
if (tool.name().endsWith("_PICKAXE")) {
    return true;
}

// Check custom pickaxes (carrot_on_a_stick with CustomModelData 213XXX)
if (tool == Material.CARROT_ON_A_STICK && toolItem.hasItemMeta()) {
    ItemMeta meta = toolItem.getItemMeta();
    if (meta.hasCustomModelData()) {
        int modelData = meta.getCustomModelData();
        // Pickaxes: 213000-213999
        return modelData >= 213000 && modelData <= 213999;
    }
}
```

#### `isDiamondOrBetterPickaxe(ItemStack toolItem)`
```java
// Check vanilla diamond/netherite pickaxes
if (tool == Material.DIAMOND_PICKAXE || tool == Material.NETHERITE_PICKAXE) {
    return true;
}

// Check custom pickaxes (all custom pickaxes count as "diamond tier")
if (tool == Material.CARROT_ON_A_STICK && toolItem.hasItemMeta()) {
    ItemMeta meta = toolItem.getItemMeta();
    if (meta.hasCustomModelData()) {
        int modelData = meta.getCustomModelData();
        return modelData >= 213000 && modelData <= 213999;
    }
}
```

#### `isSword(ItemStack toolItem)`
```java
// Check vanilla swords
if (tool.name().endsWith("_SWORD")) {
    return true;
}

// Check custom swords (carrot_on_a_stick with CustomModelData 210XXX)
if (tool == Material.CARROT_ON_A_STICK && toolItem.hasItemMeta()) {
    ItemMeta meta = toolItem.getItemMeta();
    if (meta.hasCustomModelData()) {
        int modelData = meta.getCustomModelData();
        // Swords: 210000-210999 (first 1000 in weapon range)
        return modelData >= 210000 && modelData <= 210999;
    }
}
```

#### Other Methods
- `isShovel(ItemStack toolItem)` - Vanilla only (add custom detection when needed)
- `isAxe(ItemStack toolItem)` - Vanilla only (add custom detection when needed)
- `isShears(ItemStack toolItem)` - Vanilla only (add custom detection when needed)

### 4. Method Signature Changes
Updated all tool check methods from:
```java
private boolean isPickaxe(Material tool)
```
To:
```java
private boolean isPickaxe(ItemStack toolItem)
```

### 5. Call Site Updates
Updated `getBaseBreakTime()` to:
1. Get `ItemStack toolItem` from player's main hand
2. Pass `toolItem` to all tool checking methods
3. Removed unused `Material tool` variable

### 6. Imports Added
```java
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
```

## Custom Pickaxes Detected
Based on `CustomItems.java`, these pickaxes will now be detected:
1. **Shattered Shell Pickaxe** (213001) - Mining Speed +0.1
2. **Rusty-Crumbled Pickaxe** (213002) - Mining Speed +0.1, Fortune +1
3. **Root-Cracked Pickaxe** (213003) - Mining Speed +0.2, Fortune +2
4. **Copperhead Pickaxe** (213004) - Phys Dmg +5, Mining Speed +0.3, Fortune +5
5. **Forged Copper Pickaxe** (213005) - Phys Dmg +7, Mining Speed +0.3, Fortune +7

## Expected Results After Fix

### Stone Bricks with Custom Pickaxe
- **Before**: 30 base ticks (bare hands) / 0.5 mining speed = 60 ticks = **3 seconds**
- **After**: 8 base ticks (pickaxe) / 0.5 mining speed = 16 ticks = **0.8 seconds**

### With Higher Mining Speed (e.g., +0.3 from Forged Copper)
- Base: 8 ticks (pickaxe detected)
- Mining Speed: 0.5 + 0.3 = 0.8
- Break Time: 8 / 0.8 = 10 ticks = **0.5 seconds**

### Obsidian with Custom Pickaxe
- **Before**: 1000 base ticks (not diamond pickaxe) / 0.5 = 2000 ticks = **100 seconds**
- **After**: 250 base ticks (diamond tier) / 0.5 = 500 ticks = **25 seconds**

## Testing Instructions

### 1. Debug Mode
Enable mining debug:
```
/debugmode mining
```

### 2. Give Custom Pickaxe
Get a custom pickaxe (e.g., Forged Copper):
```
/give @s carrot_on_a_stick{CustomModelData:213005}
```

### 3. Test Stone Bricks
1. Place stone bricks
2. Hold the custom pickaxe
3. Start mining (hold mouse button)
4. Check debug logs:
   - Should show "8 ticks with pickaxe" (NOT "30 ticks bare hands")
   - Should show calculated break time ~16 ticks with 0.5 speed
   - Animation should play ~1.6 stages per second
   - Block should break in ~0.8 seconds

### 4. Test Obsidian
1. Place obsidian
2. Hold custom pickaxe
3. Start mining
4. Check debug logs:
   - Should show "250 ticks with diamond/netherite pickaxe"
   - Should break in ~25 seconds with 0.5 speed

### 5. Test Without Pickaxe (Bare Hands)
1. Hold nothing or non-tool item
2. Mine stone bricks
3. Check debug logs:
   - Should show "30 ticks bare hands"
   - Should break in ~3 seconds with 0.5 speed

## Debug Log Examples

### With Custom Pickaxe (CORRECT)
```
[DEBUG][MINING] Bedrock player Notch - Block: STONE_BRICKS
[DEBUG][MINING] Bedrock player Notch - Tool: CARROT_ON_A_STICK (CustomModelData: 213005)
[DEBUG][MINING] Bedrock player Notch - Detected as: PICKAXE
[DEBUG][MINING] Bedrock player Notch - Base break time: 8 ticks (with pickaxe)
[DEBUG][MINING] Bedrock player Notch - Mining speed: 0.5
[DEBUG][MINING] Bedrock player Notch - Total break time: 16 ticks
```

### Without Pickaxe (WRONG TOOL)
```
[DEBUG][MINING] Bedrock player Notch - Block: STONE_BRICKS
[DEBUG][MINING] Bedrock player Notch - Tool: AIR
[DEBUG][MINING] Bedrock player Notch - Detected as: BARE HANDS
[DEBUG][MINING] Bedrock player Notch - Base break time: 30 ticks (bare hands)
[DEBUG][MINING] Bedrock player Notch - Mining speed: 0.5
[DEBUG][MINING] Bedrock player Notch - Total break time: 60 ticks
```

## Future Enhancements

### 1. Custom Shovels
Add detection for CustomModelData 214XXX range if created

### 2. Custom Axes
Add detection for CustomModelData 215XXX range if created

### 3. NMS Packet Animation
User requested "NMS packets or ProtocolLib for better animation":
- Current: `player.sendBlockDamage(location, progress, entityId)`
- Potential: Use `ClientboundBlockDestructionPacket` directly
- Benefit: Smoother animation for Bedrock clients
- Note: Requires NMS or ProtocolLib dependency

### 4. Configuration File
Allow server owners to define custom tool ranges:
```yaml
custom_tools:
  pickaxes:
    - range: 213000-213999
  shovels:
    - range: 214000-214999
  axes:
    - range: 215000-215999
```

## Files Modified
- `src/main/java/com/server/profiles/stats/BedrockMiningSpeedHandler.java`
  - Added imports: ItemStack, ItemMeta
  - Changed method signatures: All tool methods now accept ItemStack
  - Added CustomModelData checking for custom pickaxes and swords
  - Updated all call sites to pass ItemStack instead of Material

## Related Documentation
- `ENCHANTMENT_SYSTEM_PROGRESS.md` - Equipment type validation system
- `CustomItems.java` - Custom item definitions with model data
- `EquipmentTypeValidator.java` - Model data ranges for all equipment types
- `FRAGMENT_CUSTOM_MODEL_DATA.md` - Fragment model data reference

## Build Instructions
```bash
cd "C:\Users\Abhi\Desktop\Projects\MMOREPO\MMO"
mvn clean package -DskipTests
```

Copy `target/mmo-0.0.1.jar` to your server's `plugins/` folder and restart.

---
**Status**: ✅ IMPLEMENTATION COMPLETE - Ready for Testing
**Date**: 2025-01-XX
**Issue**: Stone bricks breaking too slow with custom pickaxes
**Resolution**: Added CustomModelData detection for custom tools
