# Island Upgrade GUI Fix

## Issues Fixed

### 1. GUI Not Opening When Using `/island upgrade` Command
**Problem**: The upgrade GUI wasn't opening most of the time when players used the `/island upgrade` command.

**Root Cause**: The `handleIslandUpgrade()` method in `IslandCommand.java` was attempting to open the GUI from within an async callback (`CompletableFuture.thenAccept()`). In Bukkit/Spigot, all GUI operations (like `player.openInventory()`) **must** be executed on the main server thread.

**Fix**: Wrapped the `IslandUpgradeGUI.open()` call in a `Bukkit.getScheduler().runTask()` to ensure it executes on the main thread.

```java
// BEFORE (broken):
islandManager.getIsland(player.getUniqueId()).thenAccept(island -> {
    if (island == null) {
        // error message
        return;
    }
    IslandUpgradeGUI.open(player, island, islandManager); // ❌ Running on async thread
});

// AFTER (fixed):
islandManager.getIsland(player.getUniqueId()).thenAccept(island -> {
    if (island == null) {
        // error message
        return;
    }
    // ✅ Schedule on main thread
    Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
        IslandUpgradeGUI.open(player, island, islandManager);
    });
});
```

### 2. Size Upgrade Button Not Working
**Problem**: Clicking the size upgrade button (or any upgrade button) didn't work.

**Root Cause #1**: The `handleUpgradeGUIClick()` method in `IslandGUIListener.java` was calling `islandManager.loadIsland(player.getUniqueId())`, but `loadIsland()` expects an **island ID**, not a player UUID. This was causing the island lookup to fail silently.

**Fix**: Changed to use `getIslandByOwner(player.getUniqueId())` which correctly looks up islands by owner UUID.

```java
// BEFORE (broken):
islandManager.loadIsland(player.getUniqueId()).thenAccept(island -> {
    // ❌ loadIsland() expects island ID, not player UUID
    ...
});

// AFTER (fixed):
islandManager.getIslandByOwner(player.getUniqueId()).thenAccept(island -> {
    // ✅ Correct method for looking up by owner
    ...
});
```

**Root Cause #2**: After a successful upgrade, the `handleUpgradeResult()` method was reopening the GUI from within an async callback, which would fail for the same threading reason as issue #1.

**Fix**: Wrapped the GUI reopen in a main thread scheduler call.

```java
// BEFORE (broken):
private static void handleUpgradeResult(...) {
    if (result.isSuccess()) {
        player.sendMessage(...);
        open(player, island, islandManager); // ❌ Running on async thread
    }
}

// AFTER (fixed):
private static void handleUpgradeResult(...) {
    if (result.isSuccess()) {
        player.sendMessage(...);
        // ✅ Schedule on main thread
        Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
            open(player, island, islandManager);
        });
    }
}
```

## Files Modified

1. **IslandCommand.java** (line ~172-187)
   - Fixed: GUI opening now scheduled on main thread

2. **IslandGUIListener.java** (line ~80-97)
   - Fixed: Using correct method `getIslandByOwner()` instead of `loadIsland()`

3. **IslandUpgradeGUI.java** (line ~348-359)
   - Fixed: GUI reopening after upgrade now scheduled on main thread

## Testing

Test the following scenarios:

1. **Command**: `/island upgrade`
   - ✅ GUI should open reliably every time
   
2. **Size Upgrade**: Click the Map icon (slot 11)
   - ✅ Should upgrade island size if you have enough units
   - ✅ GUI should reopen with updated values
   - ✅ World border should expand
   
3. **Other Upgrades**: Click any upgrade button (slots 11, 13, 15, 29, 31)
   - ✅ Should process the upgrade
   - ✅ GUI should reopen with updated values
   
4. **Insufficient Funds**:
   - ✅ Should show error message
   - ✅ GUI should NOT reopen (as expected)

## Technical Notes

### Why Thread Safety Matters

Bukkit/Spigot has a strict threading model:
- **Main Thread**: Handles all world modifications, entity operations, and GUI operations
- **Async Threads**: Used for I/O operations like database queries

Opening a GUI (`player.openInventory()`) modifies the player's state, which can only be done on the main thread. Attempting to do this from an async thread will:
- Sometimes work (race condition)
- Sometimes fail silently
- Sometimes cause concurrent modification exceptions

### CompletableFuture and Thread Context

When using `CompletableFuture.thenAccept()` or similar methods:
- The callback runs on whatever thread the future completes on
- For async operations (database queries, file I/O), this is typically an async thread pool
- You must explicitly schedule back to the main thread for Bukkit API calls

### Pattern to Follow

Always use this pattern when opening GUIs after async operations:

```java
asyncOperation().thenAccept(result -> {
    // Process result (safe on any thread)
    
    // Any Bukkit API calls must be scheduled:
    Bukkit.getScheduler().runTask(plugin, () -> {
        player.openInventory(gui);
        // or any other Bukkit API call
    });
});
```

## Related Systems

These fixes also apply to:
- `IslandCreateGUI` - Already correctly scheduled on main thread ✅
- `IslandInfoGUI` - Already correctly scheduled on main thread ✅
- Any future GUI implementations - Follow the same pattern

---

**Date**: 2025-10-22  
**Status**: ✅ Fixed and Ready for Testing
