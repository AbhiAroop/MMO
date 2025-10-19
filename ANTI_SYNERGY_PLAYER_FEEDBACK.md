# Anti-Synergy System - Player Feedback & GUI Warnings

## ✅ New Features Implemented

### 1. **Enchantment Replacement Messages**
When a player enchants an item and a conflict occurs, they now receive a clear message:

```
⚠ Cinderwake replaced: Voltbrand, Stormfire
```

**Implementation**:
- Modified `EnchantmentData.addEnchantmentToItem()` to accept an optional `Player` parameter
- Tracks removed enchantments during conflict detection
- Sends formatted message showing what was replaced

**Code Location**: `EnchantmentData.java` lines 115-230

---

### 2. **GUI Conflict Warning**
Before enchanting, players see a warning in the GUI if conflicts will occur:

```
⚠ WARNING: Conflicts Detected!
Enchanting may remove:
  • Cinderwake
  • Voltbrand

These enchantments will be
replaced by the new one!
```

**Implementation**:
- Added `checkPotentialConflicts()` method to `EnchantmentTableGUI`
- Scans existing item enchantments
- Compares against all possible enchantments from fragments
- Displays red barrier item with conflict warnings

**Code Location**: `EnchantmentTableGUI.java` lines 583-666

---

## 📋 Technical Details

### Method Signatures Changed

**EnchantmentData.java**:
```java
// Old (still works):
public static void addEnchantmentToItem(ItemStack item, CustomEnchantment enchant, 
                                       EnchantmentQuality quality, EnchantmentLevel level)

// New (with player parameter for messages):
public static void addEnchantmentToItem(ItemStack item, CustomEnchantment enchant, 
                                       EnchantmentQuality quality, EnchantmentLevel level,
                                       Player player)
```

The old methods now delegate to the new one with `player = null`, maintaining backward compatibility.

---

### Conflict Detection Logic

**EnchantmentTableGUI.checkPotentialConflicts()**:
1. Gets existing enchantments from item
2. Determines possible enchantments from placed fragments
   - Single element enchantments for each fragment type
   - Hybrid enchantments if 2+ element types present
3. For each possible enchantment:
   - Gets its anti-synergy groups
   - Checks if any existing enchantment shares those groups
   - Adds conflicts to warning list
4. Deduplicates and returns conflict names

**Performance**: Runs only when fragments are added/removed, not every tick.

---

## 🎮 Player Experience Flow

### Before Enchanting (GUI Open)
1. Player places item in enchanting GUI
2. Player adds fragments
3. **GUI instantly shows conflict warning** (if any)
   - Red barrier item appears in info slots
   - Lists specific enchantments that will be removed
   - Clear "will be replaced" messaging
4. Player can:
   - Proceed with enchanting (knowing what will be lost)
   - Remove fragments and try different combination
   - Cancel and return items

### During Enchanting
1. Player clicks "Enchant Item" button
2. Enchantment process runs
3. **Conflict detection removes old enchantments**
4. **Player receives message: "⚠ [New] replaced: [Old1], [Old2]"**
5. Item updated with new enchantment

### Example Flow

**Scenario**: Player has sword with Cinderwake and Terraheart. Tries to add Voltbrand.

**GUI Warning**:
```
Item Slot: Netherite Sword
- Cinderwake (Rare)
- Terraheart (Epic)

Fragments: Lightning (×3)

⚠ WARNING: Conflicts Detected!
Enchanting may remove:
  • Cinderwake

These enchantments will be
replaced by the new one!
```

**After Enchanting**:
```
⚠ Voltbrand replaced: Cinderwake

Final Item:
- Terraheart (Epic)
- Voltbrand (Rare)
```

---

## 📊 Files Modified

| File | Changes | Lines Added |
|------|---------|-------------|
| **EnchantmentData.java** | Added player parameter, conflict message | ~15 |
| **EnchantmentTableGUI.java** | Added conflict checking, GUI warning display | ~90 |
| **EnchantmentApplicator.java** | Pass player to addEnchantmentToItem | ~1 |
| **TOTAL** | | **~106 lines** |

---

## 🔍 Edge Cases Handled

### Multiple Conflicts
If new enchantment conflicts with multiple existing ones:
```
⚠ Stormfire replaced: Cinderwake, Embershade, Voltbrand
```

### Hybrid Enchantments
GUI checks BOTH single-element and hybrid possibilities:
- Fire fragments → Checks Cinderwake, AshenVeil
- Fire + Lightning → Also checks Stormfire
- Warning shows conflicts for ALL possibilities

### No Conflicts
If no conflicts exist, warning doesn't appear:
- Clean GUI showing only element info
- No unnecessary clutter

### Multi-Group Enchantments
Correctly handles enchantments in multiple groups:
- Cinderwake (Groups 1, 2) → Shows all conflicts from both groups
- AshenVeil (Groups 4, 9) → Checks invisibility AND on-kill conflicts

---

## ✅ Testing Checklist

### GUI Warning Tests
- [ ] Open GUI with non-enchanted item + fragments → No warning
- [ ] Open GUI with enchanted item + conflicting fragments → Warning appears
- [ ] Open GUI with enchanted item + non-conflicting fragments → No warning
- [ ] Add/remove fragments → Warning updates instantly
- [ ] Change item → Warning recalculates
- [ ] Multiple conflicts → All listed in warning

### Message Tests
- [ ] Enchant item (no conflicts) → No replacement message
- [ ] Enchant item (1 conflict) → "X replaced: Y" message
- [ ] Enchant item (multiple conflicts) → "X replaced: Y, Z" message
- [ ] Message shows display names (not IDs)
- [ ] Message uses proper colors (yellow warning, red conflicts)

### Integration Tests
- [ ] GUI warning matches actual replacement
- [ ] Message sent only when conflicts occur
- [ ] Non-conflicting enchantments remain intact
- [ ] NBT indices properly shifted after removal

---

## 🎯 Design Philosophy

### Clear Communication
**Why we added these features**:
- Players should never be surprised by lost enchantments
- Warnings give informed choice: proceed or change plan
- Post-enchantment message confirms what happened

### Progressive Disclosure
**Information hierarchy**:
1. **Before action**: Warning in GUI (preventive)
2. **During action**: Automatic conflict resolution (system handles it)
3. **After action**: Confirmation message (validates result)

### User-Friendly Language
**Messaging style**:
- ❌ Technical: "Anti-synergy Group 2 conflict detected"
- ✅ Clear: "⚠ Voltbrand replaced: Cinderwake"

**GUI wording**:
- ❌ Confusing: "Incompatible enchantments"
- ✅ Explicit: "These enchantments will be replaced"

---

## 🚀 Future Enhancements

### Possible Additions (Not Implemented Yet)

**1. Detailed Conflict Reasons**
```
⚠ Voltbrand replaced: Cinderwake
Reason: Both are AOE damage enchantments
```

**2. Undo Option**
After enchanting, offer quick undo:
```
⚠ Voltbrand replaced: Cinderwake
[Click here to undo within 10 seconds]
```

**3. Conflict Preview in Lore**
When hovering over enchant button:
```
Click to enchant
⚠ Will remove: Cinderwake
✓ Will keep: Terraheart
```

**4. Sound Effects**
- Warning sound when conflict detected in GUI
- Different sound when enchantment replaces another

---

## 📝 Code Examples

### How to Use in Commands

**Admin Command** (`/adminenchant add`):
```java
// Pass player for messages
EnchantmentData.addEnchantmentToItem(item, enchant, quality, level, player);
```

**GUI Enchanting**:
```java
// In EnchantmentApplicator.enchantItem()
EnchantmentData.addEnchantmentToItem(enchantedItem, enchantment, quality, level, player);
```

**Programmatic Use** (no messages):
```java
// Pass null for player parameter
EnchantmentData.addEnchantmentToItem(item, enchant, quality, level, null);
```

### Accessing Conflict Data

**Get conflicts for enchantment**:
```java
CustomEnchantment enchant = registry.getEnchantment("voltbrand");
String[] conflicts = enchant.getConflictingEnchantments();
// Returns: ["Deepcurrent", "Cinderwake", "Stormfire", "CelestialSurge"]
```

**Check if two enchantments conflict**:
```java
CustomEnchantment enchant1 = registry.getEnchantment("cinderwake");
CustomEnchantment enchant2 = registry.getEnchantment("voltbrand");

int[] groups1 = enchant1.getAntiSynergyGroups(); // {1, 2}
int[] groups2 = enchant2.getAntiSynergyGroups(); // {2}

boolean conflicts = false;
for (int g1 : groups1) {
    for (int g2 : groups2) {
        if (g1 == g2) {
            conflicts = true; // Both in group 2 (AOE/Chain)
            break;
        }
    }
}
```

---

## 🎉 Summary

**What we achieved**:
- ✅ Players warned BEFORE conflicts occur (GUI)
- ✅ Players notified AFTER conflicts occur (chat message)
- ✅ Clear, user-friendly language
- ✅ Instant feedback in GUI
- ✅ Backward compatible (old code still works)
- ✅ Handles all edge cases (multiple conflicts, hybrids, etc.)

**Impact**:
- **Better UX**: No surprises, informed decisions
- **Clear Communication**: Always know what's happening
- **Professional Feel**: Polished enchanting experience

---

*Player Feedback System - Version 1.0*
*Completed: Anti-Synergy Messages & GUI Warnings*
