# Custom Anvil System - Implementation In Progress

## Status: COMPILATION ERRORS - NEEDS FIXING

The custom anvil system has been partially implemented but requires fixes to compilation errors.

## What Was Created:

### 1. **CustomAnvil.java** ✅
- Armor stand placeholder item (Custom Model Data: 500000)
- Right-click to open GUI
- Similar to enchantment altar system

### 2. **AnvilGUI.java** ✅
- 3-row GUI (27 slots)
- Input slot 1 (left): Primary item
- Input slot 2 (right): Secondary item/fragments
- Output slot (center bottom): Preview result
- Shows costs (XP + Essence)

### 3. **AnvilCombiner.java** ⚠️ NEEDS FIXES
- Core combination logic
- Handles 4 scenarios:
  1. Two identical items → Merge enchantments
  2. Item + Enchanted Tome → Apply with apply chance
  3. Two Enchanted Tomes → Combine and average apply chances
  4. Tome + Fragments → Boost matching element apply chances

**COMPILATION ERRORS:**
- `applyToItem()` method doesn't exist (should use static `addEnchantmentToItem()`)
- `EnchantmentData` constructor signature mismatch
- `EquipmentTypeValidator.canEnchant()` method doesn't exist
- `ProfileManager.getProfile()` method name incorrect

### 4. **AnvilGUIListener.java** ⚠️ NEEDS FIXES
- Handles GUI interactions
- Validates input placement
- Calculates preview on changes
- Consumes XP and Essence on combine
- Returns items on close

**COMPILATION ERROR:**
- ProfileManager method name incorrect

### 5. **Commands** ✅
- Added `/enchant give <player> anvil [amount]`
- Tab completion includes "anvil"

### 6. **Listener Registration** ✅
- AnvilGUIListener registered in Main.java

## Features Designed (Not Yet Working):

### Combination Mechanics:

**1. Two Identical Items:**
- Checks same material + custom model data
- Merges all enchantments from both
- Upgrades matching enchantments:
  - Same level + quality → Level up (if not maxed)
  - Different → Pick higher quality or level

**2. Item + Enchanted Tome:**
- Rolls apply chance for each enchantment
- Checks compatibility with item type
- Only applies successful rolls
- Tracks success/fail/incompatible counts

**3. Two Enchanted Tomes:**
- Merges enchantments like items
- **AVERAGES** apply chances on duplicate enchants
- Creates new enchanted tome as result

**4. Tome + Fragments:**
- Boosts apply chance by 5% per fragment (max 25%)
- Only affects matching element enchantments
- Hybrid enchantments match either element
- Cannot exceed 100% apply chance

### Cost System:
- **Base XP Cost:** 5 levels per enchantment
- **Base Essence Cost:** 100 per enchantment
- Varies by operation type
- Preview shows costs before combine

## What Needs To Be Fixed:

### Critical Fixes Required:

1. **AnvilCombiner.java:**
   ```java
   // WRONG:
   result = enchant.applyToItem(result);
   
   // CORRECT: Need to call static method
   EnchantmentData.addEnchantmentToItem(result, enchantObject, quality, level);
   ```

2. **EnchantmentData Constructor:**
   - Need to pass all required parameters including scaledStats, affinity, etc.
   - Or find alternative way to create EnchantmentData instances

3. **Equipment Validation:**
   - Find correct method name in EquipmentTypeValidator
   - Likely `canEnchantmentApply()` instead of `canEnchant()`

4. **Profile Manager:**
   - Find correct method to get player profile
   - Likely `getPlayerProfile()` or similar

5. **NBT Operations:**
   - May need to work with item differently
   - Enchantments are added via static methods, not instance methods

### Architecture Issues:

The current implementation assumed EnchantmentData had instance methods for applying to items, but the actual system uses static methods with CustomEnchantment objects. The combiner needs to be refactored to:

1. Get CustomEnchantment objects from registry
2. Use static `addEnchantmentToItem()` methods
3. Manually manage NBT for apply chances
4. Handle tome creation through EnchantmentTome class

## Testing Plan (Once Fixed):

### Test 1: Two Identical Items
- Give 2 diamond swords with enchantments
- Combine in anvil
- Verify enchantments merge correctly
- Test level-up mechanic

### Test 2: Item + Tome
- Create enchanted tome
- Apply to compatible item
- Verify apply chance rolls
- Check incompatible enchantments rejected

### Test 3: Two Tomes
- Create 2 enchanted tomes with overlapping enchantments
- Combine
- Verify apply chances averaged

### Test 4: Tome + Fragments
- Create tome with fire/ice/etc enchantments
- Add matching fragments
- Verify only matching elements boosted
- Test hybrid element matching

### Test 5: Costs
- Verify XP consumed
- Verify Essence consumed
- Test insufficient funds blocked

## Commands for Testing:

```
/enchant give <player> anvil
/enchant give <player> tome
/enchant give <player> fire pristine 64
# etc
```

## Next Steps:

1. Fix EnchantmentData API usage
2. Fix ProfileManager method call
3. Fix EquipmentTypeValidator method call
4. Test compilation
5. In-game testing of all 4 combination types
6. Balance cost values
7. Add sound effects and particles
8. Create documentation

## Notes:

- Custom Model Data: 500000 (anvil)
- Pattern: 5X0YZZ (5 = anvil prefix)
- GUI is fully functional (layout-wise)
- Logic is designed but needs API corrections
- This is a complex system touching many parts of the codebase
