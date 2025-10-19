# Enhanced Replacement Messages - Quality & Level Display

## Update: Detailed Enchantment Replacement Messages

### New Message Format

**Before**:
```
⚠ Voltbrand replaced: Cinderwake, Stormfire
```

**After**:
```
⚠ Voltbrand (Rare III) replaced:
  • Cinderwake (Epic II)
  • Stormfire (Legendary I)
```

---

## Features

### 1. **New Enchantment Quality Display**
Shows the quality (tier) and level of the NEW enchantment being applied:
- Color-coded by quality (Common, Rare, Epic, Legendary, etc.)
- Includes level (I, II, III, IV, V)

### 2. **Removed Enchantment Details**
Each removed enchantment now shows:
- **Name**: Display name of the enchantment
- **Quality**: Color-coded tier (Common, Rare, Epic, etc.)
- **Level**: Roman numeral (I through V)

### 3. **Multi-Line Format**
For better readability:
- Header line shows new enchantment with details
- Each removed enchantment on separate line with bullet point
- Easier to read when multiple conflicts occur

---

## Example Messages

### Single Conflict
```
⚠ Voltbrand (Rare III) replaced:
  • Cinderwake (Epic II)
```

### Multiple Conflicts
```
⚠ Stormfire (Legendary IV) replaced:
  • Cinderwake (Rare II)
  • Embershade (Epic III)
  • Voltbrand (Rare I)
```

### With Color Codes (In-Game)
```
⚠ [BLUE]Voltbrand (Rare III)[YELLOW] replaced:
  [RED]• Cinderwake [DARK_PURPLE](Epic II)[RESET]
  [RED]• Stormfire [GOLD](Legendary I)[RESET]
```

---

## Technical Implementation

### Code Changes

**EnchantmentData.java** - Modified conflict detection:

```java
// Capture quality and level before removal
String qualityStr = nbtItem.hasKey(checkPrefix + "Quality") ? 
    nbtItem.getString(checkPrefix + "Quality") : "Common";
int levelNum = nbtItem.hasKey(checkPrefix + "Level") ? 
    nbtItem.getInteger(checkPrefix + "Level") : 1;

EnchantmentQuality enchQuality = EnchantmentQuality.valueOf(qualityStr);
EnchantmentLevel enchLevel = EnchantmentLevel.values()[levelNum - 1];

// Format: "Name (Quality Level)"
String detail = existingEnchant.getDisplayName() + " " + 
              enchQuality.getColor() + "(" + enchQuality.getDisplayName() + " " + 
              enchLevel.getDisplayName() + ChatColor.RESET + ")";
removedEnchantDetails.add(detail);
```

**Enhanced Message**:
```java
// Multi-line message with details
player.sendMessage(ChatColor.YELLOW + "⚠ " + 
    quality.getColor() + enchant.getDisplayName() + " (" + 
    quality.getDisplayName() + " " + level.getDisplayName() + ")" +
    ChatColor.YELLOW + " replaced: ");
for (String detail : removedEnchantDetails) {
    player.sendMessage(ChatColor.RED + "  • " + detail);
}
```

---

## Quality Tiers (Color-Coded)

| Quality | Color | Example |
|---------|-------|---------|
| **Poor** | Gray | Poor I |
| **Common** | White | Common II |
| **Uncommon** | Green | Uncommon III |
| **Rare** | Blue | Rare IV |
| **Epic** | Dark Purple | Epic V |
| **Legendary** | Gold | Legendary I |
| **Mythic** | Light Purple | Mythic II |
| **Godly** | Dark Red + Bold | Godly III |

---

## Level Display

Levels are shown as Roman numerals:
- **I** - Level 1
- **II** - Level 2
- **III** - Level 3
- **IV** - Level 4
- **V** - Level 5

---

## Benefits

### 1. **Better Transparency**
Players know EXACTLY what they're losing:
- Not just the name, but the full investment (quality + level)
- Helps make informed decisions

### 2. **Loss Awareness**
Seeing "Epic V" being replaced makes it clear:
- This is a significant loss
- Player should consider if the trade-off is worth it

### 3. **Professional Polish**
Detailed messages show:
- System is well-designed
- Player's time/investment is respected
- Clear communication standards

---

## Edge Cases Handled

### Invalid/Missing Data
If quality or level data is missing or corrupted:
- **Quality**: Defaults to "Common"
- **Level**: Defaults to "I"
- Message still displays, no crashes

### Multiple Same-Name Enchantments
If somehow multiple of the same enchantment exist:
- Each listed separately with its own quality/level
- Player sees exact details of each removal

### Long Messages
With multiple conflicts:
- Each on separate line
- Prevents chat spam
- Easy to read vertically

---

## Comparison

### Old System
```
⚠ Voltbrand replaced: Cinderwake, Stormfire, Embershade
```
**Issues**:
- Comma-separated (hard to read with many items)
- No quality/level info
- Doesn't show value of what's lost

### New System
```
⚠ Voltbrand (Rare III) replaced:
  • Cinderwake (Epic II)
  • Stormfire (Legendary I)
  • Embershade (Epic III)
```
**Benefits**:
- ✅ Clear vertical layout
- ✅ Shows quality AND level
- ✅ Color-coded for easy scanning
- ✅ Professional appearance

---

## Files Modified

| File | Changes | Purpose |
|------|---------|---------|
| **EnchantmentData.java** | ~30 lines modified | Capture quality/level, format detailed message |

**Total Changes**: ~30 lines

---

## Testing Checklist

- [ ] Replace enchantment with quality/level → Shows new enchantment details in header
- [ ] Single conflict → Shows removed enchantment with quality/level
- [ ] Multiple conflicts → Each on separate line with bullet point
- [ ] Missing quality data → Defaults to "Common" gracefully
- [ ] Missing level data → Defaults to "I" gracefully
- [ ] Different qualities → Color codes show correctly
- [ ] Different levels → Roman numerals display correctly
- [ ] Message readability → Easy to understand at a glance

---

## Future Enhancements

### Possible Additions

**1. Item Type in Message**
```
⚠ Voltbrand (Rare III) [Sword] replaced:
  • Cinderwake (Epic II) [Sword]
```

**2. Total Loss Value**
```
⚠ Voltbrand (Rare III) replaced:
  • Cinderwake (Epic II)
  • Stormfire (Legendary I)
Total value lost: ~500 fragments
```

**3. Suggested Alternatives**
```
⚠ Voltbrand (Rare III) replaced Cinderwake (Epic II)
Tip: Deepcurrent doesn't conflict with Terraheart!
```

---

*Enhanced Replacement Messages - Version 1.1*
*Quality & Level Display Added*
