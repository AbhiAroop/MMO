# Custom Anvil System - Complete Implementation

## Status: ✅ COMPILED SUCCESSFULLY - READY FOR TESTING

The custom anvil system has been successfully implemented and compiled!

## Overview

A sophisticated item combination system that allows players to:
- Merge two identical items and their enchantments
- Apply enchanted tomes to compatible items
- Combine enchanted tomes together
- Boost tome apply chances with elemental fragments

## Files Created/Modified

### New Files:
1. **CustomAnvil.java** - Armor stand placeholder item (Model: 500000)
2. **AnvilGUI.java** - 3-row GUI with 2 input slots + 1 output preview
3. **AnvilCombiner.java** - Core combination logic and calculations
4. **AnvilGUIListener.java** - GUI interaction and event handling

### Modified Files:
1. **EnchantCommand.java** - Added `/enchant give <player> anvil [amount]`
2. **Main.java** - Registered AnvilGUIListener

## How to Use

### Getting Started:
```
/enchant give <player> anvil
```

### Opening the GUI:
- Right-click the Custom Anvil armor stand
- GUI opens with 2 input slots (left/right) and output slot (center bottom)

## Combination Types

### 1. Two Identical Items (Merge Enchantments)

**Requirements:**
- Both items must have same Material
- Both items must have same Custom Model Data
- Both items can have enchantments

**Process:**
1. Place two identical items in input slots
2. All enchantments from both items are merged
3. Duplicate enchantments are upgraded:
   - **Same Level + Same Quality** → Level increases by 1 (if not maxed at VIII)
   - **Different Quality** → Higher quality is kept
   - **Different Level** → Higher level is kept

**Example:**
```
Input 1: Diamond Sword with Flame II [Pristine]
Input 2: Diamond Sword with Flame II [Pristine]
Output: Diamond Sword with Flame III [Pristine]
```

**Cost:**
- XP: 5 levels × number of enchantments
- Essence: 100 × number of enchantments

---

### 2. Item + Enchanted Tome (Apply with Chance)

**Requirements:**
- Input 1: Any custom item (weapon/armor)
- Input 2: Enchanted tome with apply chances

**Process:**
1. For each enchantment in the tome:
   - Check if compatible with item type
   - Roll random number (0-100)
   - If roll ≤ apply chance → Apply enchantment
   - If roll > apply chance → Failed (not applied)

**Compatibility:**
- Uses EquipmentTypeValidator
- Weapon enchants only on weapons
- Armor enchants only on armor
- etc.

**Example:**
```
Tome Contains:
- Flame II [Pristine] - Apply Chance: 85%
- Frost I [Common] - Apply Chance: 45%

Rolls:
- Flame: 72 ≤ 85 → SUCCESS ✓
- Frost: 68 > 45 → FAILED ✗

Result: Item only gets Flame II
```

**Cost:**
- XP: 5 levels × (successful + failed + incompatible)
- Essence: 100 × successful only

---

### 3. Two Enchanted Tomes (Combine Tomes)

**Requirements:**
- Both inputs must be enchanted tomes

**Process:**
1. Merge all enchantments (like identical items)
2. For duplicate enchantments:
   - Upgrade level if same level+quality
   - **Average apply chances**
3. Result is a new enchanted tome

**Apply Chance Averaging:**
```
Tome 1: Flame II [Pristine] - 80% apply chance
Tome 2: Flame II [Pristine] - 60% apply chance
Result: Flame III [Pristine] - 70% apply chance (average)
```

**Cost:**
- XP: 5 levels × number of enchantments
- Essence: 100 × number of enchantments

---

### 4. Tome + Fragments (Boost Apply Chance)

**Requirements:**
- Input 1: Enchanted tome
- Input 2: Elemental fragments (stackable)

**Process:**
1. Get fragment element type (Fire, Ice, Nature, etc.)
2. For each enchantment in tome:
   - Check if enchantment matches element
   - **Hybrid enchantments** match either element
3. Boost matching enchantments: **+5% per fragment** (max +25%)
4. Apply chance cannot exceed 100%

**Example:**
```
Tome:
- Flame II [Fire] - 50% apply chance
- Frost I [Ice] - 60% apply chance
- Tempest III [Lightning+Wind] - 40% apply chance

+ 3 Fire Fragments

Result:
- Flame II: 50% + 15% = 65% ✓
- Frost I: 60% (no change)
- Tempest III: 40% (no change, doesn't match fire)

+ 3 Lightning Fragments instead:

Result:
- Flame II: 50% (no change)
- Frost I: 60% (no change)
- Tempest III: 40% + 15% = 55% ✓ (hybrid matches)
```

**Cost:**
- XP: 5 levels × boosted enchantments
- Essence: 100 × fragment count

## GUI Layout

```
┌─────────────────────────────┐
│  ╔═══════════════════════╗  │ Row 1: Border
│  ║                       ║  │
├──╫───────────────────────╫──┤
│ ▓║  [1]    +    [2]     ║▓ │ Row 2: Input slots
│  ║                       ║  │        1 = Primary
├──╫───────────────────────╫──┤        2 = Secondary
│ ▓║       [OUTPUT]        ║▓ │ Row 3: Output preview
│  ╚═══════════════════════╝  │
└─────────────────────────────┘

Legend:
[1] = Input Slot 1 (Slot 11)
[2] = Input Slot 2 (Slot 15)
[OUTPUT] = Preview Slot (Slot 22)
+ = Info indicator (Slot 13)
▓ = Border/filler
```

## Preview System

The output slot shows a **live preview** before you commit:

**Preview Display:**
- Shows the result item with all enchantments
- Displays costs in lore:
  ```
  Cost:
    X XP Levels
    X Essence
  
  Click to combine!
  ```

**No Valid Combination:**
- Shows placeholder (lime glass pane)
- Message: "Result appears here"

## Cost System

### Base Costs:
- **XP:** 5 levels per enchantment
- **Essence:** 100 per enchantment

### Cost Calculation by Type:

**Identical Items / Tome Combining:**
- XP = 5 × total enchantments in result
- Essence = 100 × total enchantments

**Tome Application:**
- XP = 5 × (success + fail + incompatible count)
- Essence = 100 × successful applications only

**Fragment Boosting:**
- XP = 5 × boosted enchantments count
- Essence = 100 × fragment stack size

### Payment:
- XP is taken from player level
- Essence is taken from active profile currency
- **Both must be available** or combine fails
- Costs consumed **after** clicking output slot

## Input Validation

### Slot 1 (Primary):
✅ Custom weapons (swords, axes, etc.)
✅ Custom armor pieces
✅ Enchanted tomes
❌ Fragments
❌ Custom anvil itself

### Slot 2 (Secondary):
✅ Custom weapons
✅ Custom armor pieces
✅ Enchanted tomes
✅ **Elemental fragments**
❌ Custom anvil itself

### Output Slot:
❌ Cannot place items
✅ Can take result (if costs met)

## Commands

### Give Anvil:
```
/enchant give <player> anvil [amount]
```

**Tab Completion:**
- `/enchant give` → player names
- `/enchant give <player>` → "all", "tome", "anvil", element types
- `/enchant give <player> anvil` → amount (optional)

**Examples:**
```
/enchant give PlayerName anvil
/enchant give PlayerName anvil 5
```

## Testing Checklist

### Test 1: Identical Items
- [ ] Place 2 identical diamond swords
- [ ] Verify enchantments merge
- [ ] Test level upgrade (same level+quality)
- [ ] Test quality selection (different quality)
- [ ] Test level selection (different level)
- [ ] Verify costs calculated correctly
- [ ] Check XP and Essence consumed

### Test 2: Tome Application
- [ ] Create enchanted tome
- [ ] Apply to compatible item
- [ ] Verify apply chance rolls work
- [ ] Test incompatible enchantments rejected
- [ ] Try with various apply chance values
- [ ] Test costs (successful applications only)

### Test 3: Tome Combining
- [ ] Create 2 enchanted tomes
- [ ] Combine with overlapping enchantments
- [ ] Verify apply chances averaged correctly
- [ ] Test level upgrades work
- [ ] Check result is enchanted tome

### Test 4: Fragment Boosting
- [ ] Create tome with fire enchantments
- [ ] Add fire fragments
- [ ] Verify only matching elements boosted
- [ ] Test hybrid enchantment matching
- [ ] Verify 5% per fragment, max 25%
- [ ] Check cannot exceed 100%

### Test 5: Cost System
- [ ] Test with insufficient XP
- [ ] Test with insufficient Essence
- [ ] Verify both consumed on success
- [ ] Test different cost calculations

### Test 6: GUI Interactions
- [ ] Test invalid item placement
- [ ] Verify preview updates on changes
- [ ] Test closing GUI returns items
- [ ] Verify output slot protection

## Technical Details

### Custom Model Data:
- **Anvil:** 500000
- **Pattern:** 5X0YZZ (5 = anvil prefix)

### NBT Data:
- Apply chances stored per enchantment
- Format: `MMO_Enchantment_X_ApplyChance`
- Values: 0-100

### Profile System Integration:
- Uses ProfileManager.getInstance()
- Gets active profile slot
- Accesses profile array by slot
- Removes essence via profile.removeEssence()

### Enchantment Registry:
- Gets CustomEnchantment objects by ID
- Used for adding enchantments to items
- Uses EnchantmentData.addEnchantmentToItem()

## Known Limitations

1. **No Custom Preview Lore:**
   - Preview shows actual result item
   - Cost info added to lore temporarily
   - Actual result has clean lore

2. **Manual Level Tracking:**
   - System tracks which enchantments need upgrade
   - Applied at higher level when writing to item

3. **Apply Chance Precision:**
   - Tome combining uses integer average
   - 80% + 60% = 70% (not 70.5%)

## Future Enhancements

- [ ] Sound effects for different combinations
- [ ] Particles when combining
- [ ] Success/failure messages with details
- [ ] Configurable costs
- [ ] Cost reduction perks/skills
- [ ] Animation on combine
- [ ] Broadcast legendary combinations

## Error Messages

**No Active Profile:**
> "No active profile!"

**Insufficient XP:**
> "Not enough XP! Need X levels."

**Insufficient Essence:**
> "Not enough essence! Need X essence."

**Invalid Combination:**
> "No valid combination!"

**Cannot Place:**
> "Cannot place that item here!"
> "You cannot place items in the output slot!"

**Success:**
> "✓ Items combined successfully!"
> "Cost: X XP & X Essence"

## Architecture Notes

### Why No Direct EnchantmentData Creation?
The EnchantmentData class stores calculated stats and affinities that require the full enchantment definition from registry. Instead of creating new instances, we:
1. Use existing EnchantmentData from items
2. Get CustomEnchantment from registry
3. Apply via static EnchantmentData.addEnchantmentToItem()

### Upgrade Tracking:
Since we can't modify EnchantmentData instances, we:
1. Track which enchantments need upgrade in Map
2. Apply them at (level + 1) when writing to result
3. Preserve quality and element from source

### Apply Chance Management:
- Stored in NBT alongside enchantment data
- Read when previewing combinations
- Updated when boosting with fragments
- Averaged when combining tomes

## Performance Considerations

- Clones items to avoid modifying originals
- Uses HashMap for O(1) enchantment lookups
- Minimal NBT operations
- Preview calculated only on input changes

## Compatibility

- Works with all custom items (weapons/armor)
- Compatible with enchantment system
- Integrates with profile currency system
- Uses existing validator for compatibility checks

---

## Quick Reference

| Combination | Cost XP | Cost Essence | Special |
|-------------|---------|--------------|---------|
| Item + Item | 5 × total | 100 × total | Level up duplicates |
| Item + Tome | 5 × attempts | 100 × success | Apply chance rolls |
| Tome + Tome | 5 × total | 100 × total | Average apply chances |
| Tome + Frags | 5 × boosted | 100 × frags | +5% per fragment |

## Support

For issues or questions:
1. Check enchantment compatibility
2. Verify costs are available
3. Ensure items are custom items
4. Check profile is active
