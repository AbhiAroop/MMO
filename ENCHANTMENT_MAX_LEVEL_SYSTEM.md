# Per-Enchantment Max Level System

## Overview
Each enchantment now has an individual maximum level (1-8) to provide variety in progression. Some enchantments cap at low levels (simple utilities), while others can reach the full VIII.

## Implementation Details

### 1. Framework
- **Base Class**: `CustomEnchantment.getMaxLevel()`
  - Default: 8 (maximum possible)
  - Each enchantment can override to set custom max (1-8)
  - Located in: `src/main/java/com/server/enchantments/data/CustomEnchantment.java`

### 2. Admin Command Bypass
- **Command**: `/enchant add <enchant> <level>`
- **Behavior**: 
  - Warns when level exceeds enchantment's max
  - Still applies the enchantment (admin override)
  - Example warning:
    ```
    ⚠ Warning: Cinderwake has a max level of 5 (I-V)
    ⚠ Applying level VIII as admin override.
    ```
- Located in: `src/main/java/com/server/commands/EnchantCommand.java` (handleAdd method)

### 3. Info Display
- **Command**: `/enchant info <enchant>`
- **Display**: Shows max level with Roman numeral
  - Example: `Max Level: 5 (I-V)`
- Located in: `src/main/java/com/server/commands/EnchantCommand.java` (handleInfo method)

### 4. GUI Enforcement
- **Enchanter GUI**: Automatically caps level at enchantment's max
- **Logic**: After calculating level from fragments, checks if it exceeds max and clamps down
- Located in: `src/main/java/com/server/enchantments/gui/EnchantmentApplicator.java`
- **Code**:
  ```java
  // Cap level at enchantment's maximum
  int maxLevel = enchantment.getMaxLevel();
  if (level.getNumericLevel() > maxLevel) {
      level = EnchantmentLevel.fromNumeric(maxLevel);
  }
  ```

## Setting Max Levels

### Override Example
To set a custom max level for an enchantment:

```java
@Override
public int getMaxLevel() {
    return 3; // This enchantment maxes at level III
}
```

### Recommended Variety
- **Simple Utilities** (1-3): Basic buffs, simple effects
  - Example: Speed boosts, minor damage reductions
  
- **Standard Combat** (4-6): Regular offensive/defensive enchants
  - Example: Most damage dealing enchants, standard protections
  
- **Powerful/Rare** (7-8): Endgame, powerful, or rare enchantments
  - Example: Legendary effects, complex mechanics

## Player Experience

### Normal Gameplay (Enchanter GUI)
1. Player collects fragments (1-256)
2. Uses enchanter GUI to apply enchantment
3. Level is calculated from fragment count
4. **If calculated level exceeds max**: Automatically capped at enchantment's max
5. Player receives enchantment at capped level

### Admin Testing (/enchant add)
1. Admin uses `/enchant add <enchant> <level>`
2. **If level exceeds max**: Warning displayed, but enchantment still applied
3. Allows admins to test high-level mechanics on low-cap enchantments

### Information Lookup (/enchant info)
1. Player uses `/enchant info <enchant>`
2. Sees max level clearly displayed
3. Helps players understand fragment requirements

## Fragment Planning
Players can use the max level info to plan fragment usage:
- **Low-cap enchants**: Don't waste 256 fragments on a max-3 enchant
- **High-cap enchants**: Save large fragment amounts for powerful enchants
- **Optimal usage**: Match fragment count to desired level within max

## Current Status
✅ Framework implemented (getMaxLevel method)
✅ Admin bypass with warning
✅ Info display updated
✅ GUI enforcement complete
⏳ Individual enchantment max levels need to be set (override getMaxLevel())

## Next Steps
1. Decide max levels for each enchantment
2. Add overrides to enchantment classes
3. Build and test in-game
4. Document which enchantments have which max levels
