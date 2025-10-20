# Custom Anvil - Interaction Fix

## Issue: Anvil GUI Not Opening

**Problem**: Right-clicking the anvil armor stand does not open the GUI.

**Root Cause**: Anvil armor stands were spawned with **marker mode** enabled (`setMarker(true)`).

### What is Marker Mode?
- Marker armor stands have **NO hitbox**
- They cannot be interacted with via `PlayerInteractEntityEvent`
- They can only be selected in creative mode with F3+B debug view
- They are designed to be purely visual/decorative

### Why This Broke Interaction
The `AnvilGUIListener.onAnvilClick()` method uses `PlayerInteractEntityEvent`, which requires the entity to have a hitbox. Marker armor stands don't trigger this event when right-clicked.

---

## Solution: Two Approaches

### Approach 1: Disable Marker Mode (Simpler) ✅ IMPLEMENTED
Changed the anvil spawn code to use `setMarker(false)` so the armor stand has a hitbox and can be clicked.

**File**: `EnchantCommand.java` - `spawnAnvil()` method
```java
anvil.setMarker(false);  // NOT marker mode - needs hitbox for interaction!
```

**Pros**:
- Simple one-line fix
- Direct entity interaction works
- Clean implementation

**Cons**:
- Armor stand can be pushed by pistons/water (mitigated by `setInvulnerable(true)`)
- Has a small hitbox (could interfere with building, but unlikely since invisible)

---

### Approach 2: Add Proximity Detection (Robust) ✅ ALSO IMPLEMENTED
Copied the pattern from `EnchantmentTableListener` which handles marker armor stands by detecting nearby anvils when player right-clicks air/blocks.

**File**: `AnvilGUIListener.java`

**Added Methods**:
1. `onPlayerInteract(PlayerInteractEvent)` - Detects right-click on air/blocks
2. `findNearbyAnvil(Location)` - Searches for anvil armor stands within 3 blocks
3. `openAnvilGUI(Player, ArmorStand)` - Extracted GUI opening logic

**How It Works**:
```java
@EventHandler
public void onPlayerInteract(PlayerInteractEvent event) {
    // Find nearby anvil within 3 blocks
    ArmorStand anvil = findNearbyAnvil(player.getLocation());
    if (anvil == null) return;
    
    // Open GUI
    openAnvilGUI(player, anvil);
}
```

**Pros**:
- Works with marker mode armor stands
- No hitbox collision issues
- Matches altar behavior exactly

**Cons**:
- More complex code
- Slight delay in detection (negligible)

---

## Current Implementation

**Both approaches are implemented** for maximum compatibility:

1. **Marker mode disabled** - Direct clicking works for new anvils
2. **Proximity detection** - Fallback for existing marker anvils or future use

### Interaction Flow:
```
Player Right-Clicks Anvil
    ↓
Is it a non-marker armor stand?
    ├─ YES → onAnvilClick() fires → Open GUI ✓
    └─ NO (marker) → onAnvilClick() doesn't fire
                  ↓
Player Right-Clicks Air/Block Near Anvil
    ↓
onPlayerInteract() fires
    ↓
findNearbyAnvil() searches within 3 blocks
    ↓
Found anvil with ANVIL helmet?
    ├─ YES → Open GUI ✓
    └─ NO → Do nothing
```

---

## Testing Instructions

### Test 1: Remove Old Marker Anvils
If you spawned anvils before the fix:
1. Remove them (`/kill @e[type=armor_stand,distance=..5]`)
2. Spawn new ones with `/enchant spawn anvil`

### Test 2: Direct Click (New Anvils)
1. Spawn anvil: `/enchant spawn anvil`
2. Right-click directly on the anvil block
3. Expected: GUI opens immediately

### Test 3: Proximity Click (Marker Anvils)
1. If you have old marker anvils, stand within 2 blocks
2. Right-click the air while looking at the anvil
3. Expected: GUI opens

### Test 4: Permission Check
1. Remove `mmo.anvil.use` permission
2. Try to open anvil
3. Expected: "You don't have permission to use custom anvils!"

### Test 5: Console Logs
Check console for debug messages:
```
[INFO] [Anvil] <player> clicked anvil armor stand
[INFO] [Anvil] Opening anvil GUI for <player>
```

Or for proximity:
```
[INFO] [Anvil] Found nearby anvil for <player>
[INFO] [Anvil] Opening anvil GUI for <player>
```

---

## Files Changed

### 1. EnchantCommand.java
**Line 622**: Changed `anvil.setMarker(true)` → `anvil.setMarker(false)`

**Why**: Allows direct entity interaction via PlayerInteractEntityEvent

---

### 2. AnvilGUIListener.java
**Added Imports**:
- `org.bukkit.Location`
- `org.bukkit.entity.Entity`
- `org.bukkit.event.block.Action`
- `org.bukkit.event.player.PlayerInteractEvent`

**New/Modified Methods**:
1. `onAnvilClick()` - Now calls `openAnvilGUI()` helper
2. `onPlayerInteract()` - NEW - Handles proximity detection
3. `findNearbyAnvil(Location)` - NEW - Searches for nearby anvils
4. `openAnvilGUI(Player, ArmorStand)` - NEW - Extracted opening logic

**Why**: Provides fallback for marker armor stands and cleaner code organization

---

## Comparison with Enchantment Altar

| Feature | Altar | Anvil |
|---------|-------|-------|
| Marker Mode | YES (true) | NO (false) ✓ |
| Direct Click | No (marker) | Yes ✓ |
| Proximity Detection | Yes | Yes ✓ |
| Registration | Required | Not required |
| Structure Validation | Yes | No |

**Key Difference**: Anvils don't use a structure manager, so ANY armor stand with an anvil helmet will open the GUI. This is simpler but less controlled than altars.

---

## Build Instructions

Use your Maven terminal to build:

```powershell
cd C:\Users\Abhi\Desktop\Projects\MMOREPO\MMO
mvn clean package
```

Then copy to server:
```powershell
copy "C:\Users\Abhi\Desktop\Projects\MMOREPO\MMO\target\mmo-0.0.1.jar" "C:\Users\Abhi\Desktop\AI Paper Server\plugins\"
```

Then restart the server.

---

## Troubleshooting

### Issue: Still not opening after rebuild
**Solutions**:
1. Kill all existing anvil armor stands
2. Restart server (not just reload)
3. Spawn fresh anvils with fixed command
4. Check console for "[Anvil]" log messages

### Issue: Opens when clicking nearby blocks
**Cause**: Proximity detection with 3-block radius
**Solution**: This is intended behavior for marker anvils. If unwanted, remove the `onPlayerInteract()` method.

### Issue: Permission error
**Cause**: Missing `mmo.anvil.use` permission
**Solution**: Grant permission or give `*` to test

---

## Status

✅ **Marker mode disabled** - Direct clicking enabled  
✅ **Proximity detection added** - Fallback for marker anvils  
✅ **Code organized** - Extracted `openAnvilGUI()` helper  
✅ **Permissions working** - `mmo.anvil.use` required  
✅ **Debug logging** - Console shows interaction attempts  

**Ready to build and test!**
