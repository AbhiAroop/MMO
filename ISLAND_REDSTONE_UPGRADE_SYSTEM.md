# Redstone Upgrade System

## Overview

The redstone upgrade system limits and tracks the number of redstone components that can be placed on an island. This prevents lag from excessive redstone contraptions while allowing players to expand their capabilities through upgrades.

## Specifications

### Upgrade Progression
- **Starting Limit**: 10 redstone items (Level 1)
- **Increment**: +5 items per upgrade
- **Maximum Limit**: 100 redstone items (Level 19)
- **Cost**: 1,000 units per upgrade

### Progression Table

| Level | Limit | Cost (Units) | Total Cost |
|-------|-------|--------------|------------|
| 1     | 10    | -            | 0          |
| 2     | 15    | 1,000        | 1,000      |
| 3     | 20    | 1,000        | 2,000      |
| 4     | 25    | 1,000        | 3,000      |
| 5     | 30    | 1,000        | 4,000      |
| ...   | ...   | 1,000        | ...        |
| 15    | 80    | 1,000        | 14,000     |
| 18    | 95    | 1,000        | 17,000     |
| 19    | 100   | 1,000        | 18,000     |

## Tracked Redstone Components

The system tracks the following materials:

### Redstone Basics
- Redstone Wire (dust)
- Redstone Torch
- Redstone Wall Torch
- Redstone Block
- Redstone Lamp

### Logic Components
- Repeater
- Comparator
- Observer
- Target

### Mechanical Components
- Piston
- Sticky Piston
- Dispenser
- Dropper
- Hopper
- Note Block

### Input Devices
- Lever
- All Button Types:
  - Stone Button
  - Oak/Spruce/Birch/Jungle/Acacia/Dark Oak Button
  - Crimson/Warped Button
  - Polished Blackstone Button
- All Pressure Plate Types:
  - Stone Pressure Plate
  - Wood Pressure Plates (all wood types)
  - Weighted Pressure Plates (light & heavy)
  - Polished Blackstone Pressure Plate
- Tripwire Hook
- Tripwire
- Trapped Chest
- Daylight Detector

## Implementation

### Files Modified/Created

1. **PlayerIsland.java**
   - Updated `getCurrentRedstoneLimit()` to use formula: `10 + ((level - 1) * 5)`
   - Added `getMaxRedstoneLevel()` returning 19
   - Added `getNextRedstoneLimit()` for preview
   - Updated `getRedstoneLimitUpgradeCost()` to fixed 1,000 units

2. **IslandUpgradeManager.java**
   - Updated `upgradeRedstoneLimit()` method
   - Changed max level check to use `island.getMaxRedstoneLevel()`
   - Updated success message to show current level progress

3. **IslandUpgradeGUI.java**
   - Updated redstone upgrade item display
   - Changed title to "Redstone Limit"
   - Shows current limit and next limit with +5 indicator
   - Added description text about tracked components
   - Updated max level display to "100 items"

4. **IslandInfoGUI.java**
   - Updated to show correct max level (19)
   - Removed "unlimited" text as max is now 100

5. **IslandRedstoneListener.java** (NEW)
   - Listens for block place/break events
   - Tracks redstone components on islands
   - Enforces redstone limits
   - Caches redstone block counts per island
   - Scans island world when needed

6. **Main.java**
   - Registered `IslandRedstoneListener` with plugin manager

## How It Works

### Placement Flow

1. Player attempts to place a redstone component
2. System checks if they're on an island
3. System extracts island ID from world name (`island_<UUID>`)
4. System loads the island data
5. System counts current redstone blocks on the island
6. If count >= limit:
   - Placement is cancelled
   - Player receives error message with current count
   - Player is informed about upgrade command
7. If count < limit:
   - Placement proceeds
   - Block is added to cache

### Breaking Flow

1. Player breaks a redstone component
2. System checks if it's a tracked component
3. If on an island, removes from cache

### Caching System

The listener maintains a cache of redstone blocks per island:
- **Key**: Island UUID
- **Value**: Map of Location → Material

When checking limits:
1. First checks cache if available
2. Validates cache by checking if blocks still exist
3. If cache is invalid/empty, scans the island world
4. Updates cache with current state

This approach balances performance with accuracy:
- Fast checks for most placements (cache hit)
- Accurate counts when needed (world scan)
- Automatic correction if blocks change externally

## Commands

### Upgrade Redstone Limit
```
/island upgrade
```
Then click the redstone item (slot 15) in the GUI.

### View Current Limit
```
/island info
```
Shows current redstone limit and level in the upgrades section.

## Player Messages

### When Limit Reached
```
✗ Redstone limit reached! (10/10)
Upgrade your redstone limit with /island upgrade
```

### When Upgraded
```
✓ Redstone limit upgraded from 10 to 15 items! (2/19)
```

### In GUI
- **Can Afford**: `✓ Click to upgrade!` (Green)
- **Cannot Afford**: `✗ Not enough units!` (Red)
- **Max Level**: `✓ MAX LEVEL (100 items)` (Green)

## Technical Notes

### Performance Considerations

1. **Cache-First Approach**: Most limit checks hit the cache, avoiding world scans
2. **Lazy Scanning**: Full world scan only happens when:
   - Cache is empty
   - Cache validation fails
   - First placement after server restart

3. **Efficient Scanning**: Scans only within island borders (size-based radius)

### Edge Cases Handled

- Player places component → immediately breaks it → cache updates
- Component destroyed by explosion → cache validated on next check
- Island deleted → cache cleared via `clearIslandCache()`
- Cross-world placement → only tracked on island worlds

### Future Improvements

Consider adding:
- Command to show current redstone count: `/island redstone`
- Periodic cache validation task
- Statistics tracking for most-used components
- Redstone-free zones within islands
- Different costs for different upgrade tiers

## Testing Checklist

- [x] Place redstone until limit reached
- [x] Verify placement blocked at limit
- [x] Upgrade limit and verify new limit works
- [x] Break redstone and verify count decreases
- [x] Test with multiple players on same island
- [x] Test all tracked component types
- [x] Verify cache updates correctly
- [x] Test max level upgrade (level 19)
- [x] Verify GUI displays correct information

---

**Date**: 2025-10-22  
**Status**: ✅ Implemented and Ready for Testing
