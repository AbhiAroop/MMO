# Bedrock Mining: Bare Hands Only (Mining Speed Stat Based)

## Summary
Updated the Bedrock mining speed system so that **ALL blocks are broken as if using bare hands/fist**. Tools provide NO speed boost - ONLY the player's mining speed stat affects break times.

## Philosophy
- **Tools are cosmetic**: Holding a pickaxe, shovel, or axe does NOT make blocks break faster
- **Mining speed stat is everything**: The ONLY factor affecting break speed is the player's mining speed stat
- **Consistent with stat-based system**: This matches the custom stat system where player progression comes from stats, not vanilla tool mechanics

## Changes Made

### Before (Tool-Based System)
```java
// Stone bricks with pickaxe: 8 ticks base
// Stone bricks without pickaxe: 30 ticks base
case STONE_BRICKS:
    return isPickaxe(toolItem) ? 8 : 30;
```

### After (Stat-Based System)
```java
// Stone bricks always: 30 ticks base (bare hands)
// Mining speed stat determines actual break time
case STONE_BRICKS:
    return 30; // Bare hands only
```

## Block Break Times (Base - Bare Hands)

All values assume **1.0 mining speed** and **bare hands** (fist):

### Stone-Type Blocks
- **Base Time**: 30 ticks (1.5 seconds)
- **Blocks**: Stone, Cobblestone, Stone Bricks, Andesite, Diorite, Granite, Deepslate, Sandstone, Bricks, Nether Bricks, etc.
- **Example**: With 0.5 mining speed = 60 ticks (3 seconds)
- **Example**: With 1.0 mining speed = 30 ticks (1.5 seconds)
- **Example**: With 2.0 mining speed = 15 ticks (0.75 seconds)

### Ore Blocks
- **Base Time**: 60 ticks (3 seconds)
- **Blocks**: Coal, Copper, Iron, Gold, Lapis, Redstone, Diamond, Emerald (all variants)
- **Example**: With 0.5 mining speed = 120 ticks (6 seconds)
- **Example**: With 1.0 mining speed = 60 ticks (3 seconds)
- **Example**: With 2.0 mining speed = 30 ticks (1.5 seconds)

### Obsidian
- **Base Time**: 1000 ticks (50 seconds)
- **Blocks**: Obsidian, Crying Obsidian
- **Example**: With 0.5 mining speed = 2000 ticks (100 seconds)
- **Example**: With 1.0 mining speed = 1000 ticks (50 seconds)
- **Example**: With 2.0 mining speed = 500 ticks (25 seconds)

### Dirt/Sand/Gravel
- **Base Time**: 10 ticks (0.5 seconds)
- **Blocks**: Dirt, Grass, Sand, Gravel, Clay, Soul Sand, Soul Soil
- **Example**: With 0.5 mining speed = 20 ticks (1 second)
- **Example**: With 1.0 mining speed = 10 ticks (0.5 seconds)
- **Example**: With 2.0 mining speed = 5 ticks (0.25 seconds)

### Wood (Logs & Planks)
- **Base Time**: 40 ticks (2 seconds)
- **Blocks**: All log types, all plank types
- **Example**: With 0.5 mining speed = 80 ticks (4 seconds)
- **Example**: With 1.0 mining speed = 40 ticks (2 seconds)
- **Example**: With 2.0 mining speed = 20 ticks (1 second)

### Netherrack
- **Base Time**: 8 ticks (0.4 seconds)
- **Example**: With 0.5 mining speed = 16 ticks (0.8 seconds)
- **Example**: With 1.0 mining speed = 8 ticks (0.4 seconds)
- **Example**: With 2.0 mining speed = 4 ticks (0.2 seconds)

### End Stone
- **Base Time**: 60 ticks (3 seconds)
- **Example**: With 0.5 mining speed = 120 ticks (6 seconds)
- **Example**: With 1.0 mining speed = 60 ticks (3 seconds)
- **Example**: With 2.0 mining speed = 30 ticks (1.5 seconds)

### Glass
- **Base Time**: 2 ticks (0.1 seconds - instant)
- **Example**: With 0.5 mining speed = 4 ticks (0.2 seconds)
- **Example**: With 1.0 mining speed = 2 ticks (0.1 seconds)
- **Example**: With 2.0 mining speed = 1 tick (0.05 seconds)

### Leaves
- **Base Time**: 4 ticks (0.2 seconds)
- **Example**: With 0.5 mining speed = 8 ticks (0.4 seconds)
- **Example**: With 1.0 mining speed = 4 ticks (0.2 seconds)
- **Example**: With 2.0 mining speed = 2 ticks (0.1 seconds)

## Break Time Formula

```java
adjustedBreakTime = baseBreakTime / miningSpeed
```

### Example Calculations

#### Stone Bricks (30 base ticks)
| Mining Speed | Formula | Result | Time |
|--------------|---------|--------|------|
| 0.5 | 30 / 0.5 | 60 ticks | 3.0s |
| 0.8 | 30 / 0.8 | 37.5 ticks | 1.875s |
| 1.0 | 30 / 1.0 | 30 ticks | 1.5s |
| 1.5 | 30 / 1.5 | 20 ticks | 1.0s |
| 2.0 | 30 / 2.0 | 15 ticks | 0.75s |
| 3.0 | 30 / 3.0 | 10 ticks | 0.5s |

#### Obsidian (1000 base ticks)
| Mining Speed | Formula | Result | Time |
|--------------|---------|--------|------|
| 0.5 | 1000 / 0.5 | 2000 ticks | 100s |
| 1.0 | 1000 / 1.0 | 1000 ticks | 50s |
| 2.0 | 1000 / 2.0 | 500 ticks | 25s |
| 5.0 | 1000 / 5.0 | 200 ticks | 10s |
| 10.0 | 1000 / 10.0 | 100 ticks | 5s |

## Code Changes

### Method Signature Simplified
```java
// Before: Checked tool type
private int getBaseBreakTime(Material blockType, Player player) {
    ItemStack toolItem = player.getInventory().getItemInMainHand();
    // ... tool checking logic ...
}

// After: No tool checking needed
private int getBaseBreakTime(Material blockType, Player player) {
    // All blocks use bare hands timing
    switch (blockType) {
        case STONE_BRICKS:
            return 30; // Bare hands only
        // ...
    }
}
```

### Removed Methods
All tool detection methods removed (no longer needed):
- ❌ `isPickaxe(ItemStack)`
- ❌ `isShovel(ItemStack)`
- ❌ `isAxe(ItemStack)`
- ❌ `isShears(ItemStack)`
- ❌ `isSword(ItemStack)`
- ❌ `isDiamondOrBetterPickaxe(ItemStack)`

### Removed Imports
```java
// No longer needed
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
```

## Impact on Gameplay

### Custom Pickaxes
Your custom pickaxes (213001-213005) now provide benefits through **stats only**:

1. **Forged Copper Pickaxe** (213005)
   - Physical Damage: +7
   - **Mining Speed: +0.3** ✅ (This is what speeds up mining!)
   - Mining Fortune: +7
   
2. **Copperhead Pickaxe** (213004)
   - Physical Damage: +5
   - **Mining Speed: +0.3** ✅
   - Mining Fortune: +5

### Example Scenario

**Stone Bricks with Forged Copper Pickaxe:**
- Base time: 30 ticks (bare hands)
- Base mining speed: 0.5
- Pickaxe bonus: +0.3 mining speed
- Total mining speed: 0.5 + 0.3 = **0.8**
- Break time: 30 / 0.8 = **37.5 ticks** (1.875 seconds)

**Without pickaxe (bare hands):**
- Base time: 30 ticks (bare hands)
- Base mining speed: 0.5
- Break time: 30 / 0.5 = **60 ticks** (3 seconds)

**Difference**: The pickaxe's +0.3 mining speed stat makes it **1.6x faster**, not because it's a pickaxe, but because of the mining speed stat!

## Testing Instructions

### 1. Test with Base Stats (0.5 mining speed)
```
# No mining speed bonuses
# Mine stone bricks
Expected: ~60 ticks (3 seconds)
```

### 2. Test with Custom Pickaxe (+0.3 mining speed)
```
/give @s carrot_on_a_stick{CustomModelData:213005}
# Forged Copper Pickaxe gives +0.3 mining speed
# Total: 0.5 + 0.3 = 0.8 mining speed
# Mine stone bricks
Expected: ~37.5 ticks (1.875 seconds)
```

### 3. Test with Mining Speed Boost
```
# If you have a command to set mining speed directly
/stats set <player> mining_speed 2.0
# Mine stone bricks
Expected: ~15 ticks (0.75 seconds)
```

### 4. Debug Logs
Enable mining debug:
```
/debugmode mining
```

Expected logs:
```
[DEBUG][MINING] Bedrock player Notch - Block: STONE_BRICKS
[DEBUG][MINING] Bedrock player Notch - Base break time: 30 ticks (bare hands)
[DEBUG][MINING] Bedrock player Notch - Mining speed: 0.8
[DEBUG][MINING] Bedrock player Notch - Total break time: 37 ticks
```

Note: Debug logs should NOT mention tool detection anymore!

## Advantages of This System

### 1. Stat-Based Progression
- Players progress by increasing mining speed stat
- No reliance on vanilla tool mechanics
- Consistent with custom item system

### 2. Balanced Gameplay
- All blocks break at predictable rates based on stats
- No tool-tier confusion (diamond vs iron)
- Easy to balance: adjust mining speed stat values

### 3. Custom Item Design Freedom
- Custom "pickaxes" can be any material (carrot_on_a_stick)
- Visual appearance doesn't need to match function
- Stats on items are what matter, not the base material

### 4. Simplified Code
- No complex tool detection logic
- No CustomModelData checking needed
- Fewer edge cases and bugs

## Future Considerations

### Mining Fortune Stat
Your pickaxes have Mining Fortune stat (+1 to +7). This should be handled separately:
- **Mining Fortune**: Affects drop quantity/chance (separate system)
- **Mining Speed**: Affects break time (this system)

### Mining Speed Progression
With base 0.5 mining speed, consider progression path:
- Early game: 0.5-1.0 (slow but playable)
- Mid game: 1.0-2.0 (comfortable speed)
- Late game: 2.0-5.0 (fast mining)
- End game: 5.0+ (instant mining for some blocks)

### Block Drops
Remember to handle block drops correctly:
```java
block.breakNaturally(player.getInventory().getItemInMainHand());
```
This ensures proper drops based on the item (even though it doesn't affect speed).

---

## Files Modified
- `src/main/java/com/server/profiles/stats/BedrockMiningSpeedHandler.java`
  - Removed all tool detection methods
  - Removed ItemStack/ItemMeta imports
  - Updated all block cases to use bare hands timing only
  - Simplified getBaseBreakTime() method

## Related Systems
- **PlayerStats**: Mining speed stat value
- **CustomItems**: Pickaxes with mining speed bonuses
- **Mining Fortune**: Separate stat for drop bonuses (not implemented in this handler)

---
**Status**: ✅ COMPLETE - Bare Hands Only System
**Date**: 2025-01-XX
**Philosophy**: Stats-based progression, not tool-based
**Result**: Mining speed stat is the ONLY factor affecting break time
