# Enchantment GUI Animations - Bug Fixes

## Overview
This document details the critical bug fixes applied to the GUI animation system to resolve item duplication glitches and incorrect behavior.

---

## Bug #1: Enchantment GUI Glass Pane Duplication

### Issue
When a player closed the enchantment GUI during an animation, they received the animation glass pane item in their inventory.

### Root Cause
The output slot was not cleared before calling `gui.returnItems()`, so the animation glass pane was included in the returned items.

### Fix
**File**: `EnchantmentGUIListener.java`
**Location**: `onInventoryClose()` method

Added output slot clearing **before** returning items:
```java
if (animationResult != null) {
    // Animation was active - clear the output slot first to prevent returning the glass pane
    gui.getInventory().setItem(EnchantmentTableGUI.OUTPUT_SLOT, null);
    
    // Give result item to player
    // ...
}
```

---

## Bug #2: Anvil GUI Preview Item Given to Player

### Issue
When clicking the preview (green/red glass pane) in the anvil GUI, the player instantly received the preview item instead of triggering the combination and animation.

### Root Cause
The preview item was not removed from the output slot before `handleCombine()` was called.

### Fix
**File**: `AnvilGUIListener.java`
**Location**: Output slot click handler

Clear the preview from output slot **before** calling `handleCombine()`:
```java
if (current.getType() == Material.LIME_STAINED_GLASS_PANE || 
    current.getType() == Material.RED_STAINED_GLASS_PANE) {
    // This is a preview - clicking it triggers combine
    // Clear the preview from output slot FIRST (don't give preview to player)
    gui.getInventory().setItem(AnvilGUI.OUTPUT_SLOT, null);
    handleCombine(player, gui);
    return;
}
```

---

## Bug #3: Anvil GUI Item Duplication Exploit

### Issue
Input items were not consumed when combining items in the anvil GUI, allowing infinite duplication.

### Root Cause
The preview system conflicted with the animation system. Items were supposed to be consumed in `handleCombine()`, but the timing was incorrect.

### Fix
**File**: `AnvilGUIListener.java`
**Location**: `handleCombine()` method (lines 525-540)

Items are consumed **immediately** after costs are deducted and **before** the animation starts:
```java
// Consume input items IMMEDIATELY
ItemStack slot1Item = gui.getInventory().getItem(AnvilGUI.INPUT_SLOT_1);
ItemStack slot2Item = gui.getInventory().getItem(AnvilGUI.INPUT_SLOT_2);

if (slot1Item != null) {
    slot1Item.setAmount(slot1Item.getAmount() - 1);
    if (slot1Item.getAmount() <= 0) {
        gui.getInventory().setItem(AnvilGUI.INPUT_SLOT_1, null);
    }
}

if (slot2Item != null) {
    slot2Item.setAmount(slot2Item.getAmount() - 1);
    if (slot2Item.getAmount() <= 0) {
        gui.getInventory().setItem(AnvilGUI.INPUT_SLOT_2, null);
    }
}

// THEN start animation
GUIAnimationHandler.startAnimation(...);
```

---

## Bug #4: GUI Listener Conflicts

### Issue
The `EnchantmentGUIListener` was processing clicks for the Anvil GUI, causing the default Bukkit inventory behavior to occur (giving preview items to players).

### Debug Evidence
```
[10:50:09 INFO]: [mmo] [DEBUG:GUI] [Enchant] Click detected. Title: §8⚒ §7Custom Anvil §8⚒, GUI found: false
```
The `[Enchant]` prefix shows the wrong listener was processing Anvil GUI clicks.

### Root Cause
Both listeners checked if they had a registered GUI for the player, but if they didn't find one, they simply returned **without checking the inventory title**. This meant that when a player had an Anvil GUI open:
1. EnchantmentGUIListener checked for GUI → Not found → Returned (but didn't cancel event)
2. Default Bukkit behavior → Gave preview item to player

### Fix
**Files**: `EnchantmentGUIListener.java` and `AnvilGUIListener.java`
**Locations**: `onInventoryClick()` and `onInventoryClose()` methods

Added title checks to ensure each listener only processes its own GUI:

**EnchantmentGUIListener**:
```java
// Double-check this is actually our GUI by title (prevents conflicts with other GUIs)
String title = event.getView().getTitle();
if (!title.contains("Enchantment Altar")) {
    return;
}
```

**AnvilGUIListener**:
```java
// Double-check this is actually our GUI by title (prevents conflicts with other GUIs)
String title = event.getView().getTitle();
if (!title.contains("Custom Anvil")) {
    return;
}
```

### Why This Works
- **Enchantment GUI Title**: `§5⚔ §dEnchantment Altar §5⚔`
- **Anvil GUI Title**: `§8⚒ §7Custom Anvil §8⚒`

By checking the title, each listener now **only processes its own events**, preventing conflicts and unwanted default behavior.

---

## Bug #5: Anvil GUI Close Handler Glass Pane

### Issue
Similar to Bug #1, but for the Anvil GUI. When closing during animation, the glass pane could be given to the player.

### Fix
**File**: `AnvilGUIListener.java`
**Location**: `onInventoryClose()` method

Same pattern as Bug #1 fix - clear output slot before returning inputs:
```java
if (animationResult != null) {
    // Animation was active - clear the output slot first to prevent returning the glass pane
    gui.getInventory().setItem(AnvilGUI.OUTPUT_SLOT, null);
    
    // Give result item to player
    // ...
}
```

---

## Testing Checklist

### Enchantment GUI
- [x] Close during animation → No glass pane given
- [x] Click output during animation → Skips animation, gives result
- [x] Wait for animation → Result appears after 2 seconds
- [x] Inventory full → Items drop on ground

### Anvil GUI
- [x] Click preview (green pane) → Triggers combination (no preview item given)
- [x] Animation plays after clicking preview
- [x] Input items consumed immediately
- [x] Close during animation → No glass pane given
- [x] Result appears after animation completes
- [x] No duplication exploit possible

### GUI Conflicts
- [x] EnchantmentGUIListener only processes Enchantment Altar
- [x] AnvilGUIListener only processes Custom Anvil
- [x] No cross-contamination between GUIs

---

## Technical Summary

### Flow for Anvil Combination (After Fixes)
1. Player places two items in input slots
2. Preview calculated and displayed (green/red glass pane)
3. Player clicks preview
4. **Preview cleared from output slot immediately**
5. **Check costs (XP, essence)**
6. **Consume costs**
7. **Consume input items**
8. **Start 2-second animation**
9. Animation plays with colored glass panes
10. After 2 seconds (or skip), result item appears
11. Player takes result item

### Key Principles
1. **Clear slots BEFORE operations** that might return items
2. **Consume inputs IMMEDIATELY** after cost checks pass
3. **Title checks** prevent GUI listener conflicts
4. **Animation after consumption** prevents duplication
5. **Separate preview from animation** (green/red panes vs colored panes)

---

## Performance Notes

- Title checks are cheap string operations (negligible overhead)
- Title checks happen AFTER null checks and player checks
- All fixes maintain O(1) complexity
- No additional loops or heavy operations added

---

## Commit Information

**Branch**: master
**Files Modified**:
- `src/main/java/com/server/enchantments/listeners/EnchantmentGUIListener.java`
- `src/main/java/com/server/enchantments/listeners/AnvilGUIListener.java`

**Changes Summary**:
- Added output slot clearing before item returns (both GUIs)
- Added preview clearing before combine operation
- Added title checks to prevent GUI conflicts
- Verified item consumption timing

---

## Related Documentation

- See `ENCHANTMENT_GUI_ANIMATIONS.md` for original feature documentation
- See `CUSTOM_ANVIL_SYSTEM_COMPLETE.md` for anvil system overview
- See `GUI_FIXES_AND_TOME_ENCHANT_COMMAND.md` for previous GUI fixes
