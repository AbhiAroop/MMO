# Custom Anvil - Duplicate Lore Fix & Shift-Click Support

## Status: ✅ READY TO BUILD

Two improvements have been made to the custom anvil system:

---

## 🔧 Fix #1: Duplicate Enchantment Titles

### Problem
When combining two items in the anvil, the resulting item showed duplicate enchantment titles in the lore.

**Example of the Bug**:
```
Diamond Sword
• 🔥 Flame III [Epic]
  Burns enemies on hit
• 🔥 Flame III [Epic]    ← DUPLICATE!
  Burns enemies on hit
```

### Root Cause
When preserving the original item's metadata, the code was storing ALL lore (including enchantment lore), then adding new enchantment lore on top of it.

**What was happening**:
1. Clone item1 → includes original lore (base lore + enchantment lore)
2. Save `originalLore` → contains both base and enchantment sections
3. Clear enchantments (NBT only, doesn't remove lore)
4. Apply merged enchantments → adds NEW enchantment lore
5. Restore `originalLore` → adds OLD enchantment lore back
6. Result: Base lore + OLD enchantments + NEW enchantments ❌

### Solution
Extract only the **base lore** (non-enchantment lore) before applying enchantments.

**New Flow**:
1. Clone item1
2. Extract `baseLore` using `extractBaseLore()` → only custom item lore, no enchantments
3. Clear enchantments
4. Apply merged enchantments → adds enchantment lore
5. Restore `baseLore` → adds only base lore
6. Result: Base lore + NEW enchantments ✅

### Implementation

**File**: `AnvilCombiner.java`

**Added Method**: `extractBaseLore(ItemMeta)`
```java
private static List<String> extractBaseLore(ItemMeta meta) {
    // Find where enchantment section starts (looks for "⚔ Enchantments" header)
    // Return only lore BEFORE the enchantment section
    // This preserves custom item descriptions while removing enchantment duplication
}
```

**Modified Method**: `combineIdenticalItems()`
```java
// OLD:
List<String> originalLore = (originalMeta.hasLore()) ? new ArrayList<>(originalMeta.getLore()) : null;

// NEW:
List<String> baseLore = extractBaseLore(originalMeta); // Extract only non-enchantment lore
```

**Lore Restoration Logic**:
```java
if (baseLore != null && !baseLore.isEmpty()) {
    List<String> currentLore = finalMeta.getLore(); // Enchantment lore from addEnchantmentToItem()
    List<String> combinedLore = new ArrayList<>(baseLore); // Base lore first
    combinedLore.addAll(currentLore); // Then enchantment lore
    finalMeta.setLore(combinedLore);
}
```

### How extractBaseLore() Works

1. **Searches for enchantment header**: 
   - Pattern: `"§m          §r §6⚔ Enchantments §r§m          §r"`
   - Uses `ChatColor.stripColor()` to find "Enchantments" text
   - Checks for `§m` formatting (strikethrough decoration)

2. **Handles empty line before header**:
   - If empty line exists before header, includes it in the section
   - Ensures clean separation between base lore and enchantments

3. **Returns base lore only**:
   - Everything BEFORE the enchantment section
   - Empty list becomes `null` for cleaner handling

---

## 🔧 Fix #2: Shift-Click Support

### Problem
Players couldn't shift-click items from their inventory into the anvil GUI. They had to manually drag items, which is inconvenient.

### Solution
Implemented shift-click functionality matching the enchantment altar behavior.

**File**: `AnvilGUIListener.java`

**Added Method**: `handlePlayerInventoryClick()`
```java
private void handlePlayerInventoryClick(InventoryClickEvent event, Player player, AnvilGUI gui) {
    if (event.isShiftClick()) {
        // Try slot 1 first
        if (slot1 is empty && item can go in slot1) {
            Place in slot 1
        }
        // Then try slot 2
        else if (slot2 is empty && item can go in slot2) {
            Place in slot 2
        }
        // Otherwise show error
        else {
            "Cannot place that item" or "Both slots are full"
        }
    }
}
```

**Modified Method**: `onInventoryClick()`
```java
// OLD:
if (event.isShiftClick()) {
    event.setCancelled(true); // Blocked all shift-clicking
}

// NEW:
handlePlayerInventoryClick(event, player, gui); // Smart shift-click handling
```

### How Shift-Click Works

**Priority System**:
1. **Try Input Slot 1 first** (left side)
   - Check if slot is empty
   - Validate item with `gui.canPlaceInSlot1()`
   - Place if valid

2. **Try Input Slot 2 second** (right side)
   - Only if slot 1 is occupied
   - Check if slot is empty
   - Validate item with `gui.canPlaceInSlot2()`
   - Place if valid

3. **Show error if neither works**:
   - Item not valid: "Cannot place that item in the anvil."
   - Slots full: "Both input slots are full."

**Validation Rules**:
- **Slot 1**: No fragments allowed, custom items/tomes only
- **Slot 2**: Allows items, tomes, AND fragments

**After Placement**:
- Syncs GUI inventory
- Updates preview automatically
- Shows combined result in output slot

---

## 📋 Testing Guide

### Test 1: No Duplicate Enchantment Lore
1. Get two identical custom items with enchantments
2. Place both in anvil
3. Take result
4. **Expected**: Each enchantment appears ONCE in lore
5. **Expected**: Original item description (base lore) preserved

**Example Result**:
```
§6§lFire Sword
§7A blade forged in dragon flames
§7─────────────────────
§m          §r §6⚔ Enchantments §r§m          §r
§7• 🔥 Flame III [Epic]
§8  Burns enemies on hit
§7• ⚡ Voltbrand II [Rare]
§8  Chain lightning damage
```

### Test 2: Shift-Click from Player Inventory
1. Open anvil GUI
2. Shift-click a custom sword from inventory
3. **Expected**: Sword appears in slot 1
4. Shift-click an enchanted tome
5. **Expected**: Tome appears in slot 2
6. **Expected**: Preview updates automatically

### Test 3: Shift-Click Priority
1. Open anvil GUI
2. Place item manually in slot 1
3. Shift-click another item
4. **Expected**: Goes to slot 2 (slot 1 occupied)

### Test 4: Shift-Click Validation
1. Open anvil GUI
2. Shift-click a fragment
3. **Expected**: Fragment goes to slot 2 (slot 1 doesn't accept fragments)
4. Fill both slots
5. Shift-click another item
6. **Expected**: "Both input slots are full."

### Test 5: Invalid Item Shift-Click
1. Open anvil GUI
2. Shift-click a vanilla item (no custom model data)
3. **Expected**: "Cannot place that item in the anvil."

---

## 🔍 Technical Details

### Enchantment Lore Format
The enchantment system uses a specific lore format:
```
[Base Lore - custom item description]
[Empty Line]
§m          §r §6⚔ Enchantments §r§m          §r
§7• [Icon] [Name] [Level] [Quality]
§8  [Description]
...more enchantments...
```

**Detection Pattern**:
- Header contains: `"Enchantments"` text
- Header has: `§m` formatting (strikethrough)
- Empty line before header (optional but common)

### Base Lore Extraction
```java
// Scan lore line by line
for each line:
    if line contains "Enchantments" AND has §m:
        enchantSectionStart = current index
        break
        
// Everything before enchantSectionStart is base lore
baseLore = lore[0 to enchantSectionStart]
```

### Shift-Click Flow
```
Player Shift-Clicks Item
    ↓
Is item valid for slot 1?
    ├─ YES → Is slot 1 empty?
    │         ├─ YES → Place in slot 1 ✓
    │         └─ NO → Try slot 2
    └─ NO → Try slot 2
    
Try Slot 2:
Is item valid for slot 2?
    ├─ YES → Is slot 2 empty?
    │         ├─ YES → Place in slot 2 ✓
    │         └─ NO → Show "Both slots full"
    └─ NO → Show "Cannot place item"
```

---

## 📝 Files Modified

### AnvilCombiner.java
**Lines Changed**: ~60 lines

**Changes**:
1. Added `extractBaseLore(ItemMeta)` method (50 lines)
2. Modified `combineIdenticalItems()` to use `baseLore` instead of `originalLore`
3. Updated lore restoration logic to prevent duplication

**Key Logic**:
```java
// Extract base lore (without enchantments)
List<String> baseLore = extractBaseLore(originalMeta);

// Apply enchantments (adds enchantment lore)
for (EnchantmentData enchant : mergedEnchants.values()) {
    EnchantmentData.addEnchantmentToItem(result, ...);
}

// Restore base lore + new enchantment lore
if (baseLore != null) {
    List<String> currentLore = finalMeta.getLore(); // New enchant lore
    List<String> combinedLore = new ArrayList<>(baseLore);
    combinedLore.addAll(currentLore);
    finalMeta.setLore(combinedLore);
}
```

---

### AnvilGUIListener.java
**Lines Changed**: ~60 lines

**Changes**:
1. Added `handlePlayerInventoryClick()` method (55 lines)
2. Modified `onInventoryClick()` to call new handler instead of blocking shift-clicks
3. Implemented smart slot selection with priority system

**Key Logic**:
```java
// Try slot 1 first
if (slot1 is empty && canPlaceInSlot1(item)) {
    gui.getInventory().setItem(INPUT_SLOT_1, item);
    event.setCurrentItem(null);
    updatePreview();
}
// Then slot 2
else if (slot2 is empty && canPlaceInSlot2(item)) {
    gui.getInventory().setItem(INPUT_SLOT_2, item);
    event.setCurrentItem(null);
    updatePreview();
}
```

---

## 🎯 Summary of Improvements

### Before:
❌ Enchantment lore duplicated when combining items  
❌ No shift-click support  
❌ Manual dragging required  

### After:
✅ Clean lore with no duplicates  
✅ Shift-click from player inventory  
✅ Smart slot selection (slot 1 → slot 2)  
✅ Automatic preview updates  
✅ Validation messages  
✅ Matches enchantment altar UX  

---

## 🚀 Build Instructions

The code is ready to compile:

```powershell
cd C:\Users\Abhi\Desktop\Projects\MMOREPO\MMO
mvn clean package
```

Then copy to server:
```powershell
copy "C:\Users\Abhi\Desktop\Projects\MMOREPO\MMO\target\mmo-0.0.1.jar" "C:\Users\Abhi\Desktop\AI Paper Server\plugins\"
```

Then restart the server and test!

---

## 📊 Compatibility

These changes are **backward compatible**:
- Existing anvils continue to work
- No database changes needed
- No config changes needed
- Works with all existing items

---

**Status**: ✅ **BOTH FIXES COMPLETE - READY TO BUILD AND TEST**

**Priority**: Build → Deploy → Test lore preservation and shift-click functionality
