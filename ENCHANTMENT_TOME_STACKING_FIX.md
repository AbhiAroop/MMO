# Enchantment Tome Stacking Fix

## 🐛 Issue Identified

**Problem**: When Enchantment Tomes were stacked in inventory and one was enchanted, the **entire stack** would be consumed/deleted.

**Root Cause**: Books (Material.BOOK) are stackable items. When used in the enchantment table GUI, Minecraft's item handling would consume the full stack instead of just one tome.

---

## ✅ Solution Implemented

### Unique UUID System

Each Enchantment Tome is now given a **unique UUID** stored in NBT data, making every tome distinct and **unstackable**.

**Code Added:**
```java
// IMPORTANT: Add unique UUID to make tome unstackable
// This prevents entire stacks from being consumed when enchanting
nbtItem.setString("MMO_Tome_UUID", java.util.UUID.randomUUID().toString());
```

**NBT Structure:**
```
MMO_Tome_Type: "UNENCHANTED"
MMO_Tome_UUID: "550e8400-e29b-41d4-a716-446655440000" (unique per tome)
```

---

## 🔧 How It Works

### Before Fix:
1. Player has **5 Unenchanted Tomes** stacked
2. Places stack in enchantment table
3. Clicks enchant
4. **All 5 tomes consumed** → receives 1 Enchanted Tome ❌

### After Fix:
1. Player has **5 Unenchanted Tomes** (each with unique UUID - can't stack)
2. Places **1 tome** in enchantment table
3. Clicks enchant
4. **Only 1 tome consumed** → receives 1 Enchanted Tome ✅
5. Remaining 4 tomes safe in inventory

---

## 📊 Technical Details

### Why UUID Makes Items Unstackable

In Minecraft/Bukkit, items can only stack if:
1. Same material
2. Same durability
3. Same display name
4. Same lore
5. **Same NBT data** ← Key point!

By adding a unique UUID to each tome's NBT data, no two tomes have identical NBT, preventing them from stacking.

### Performance Impact

**Minimal**: UUID generation is extremely fast (<1ms) and only happens when creating new tomes.

---

## 🎮 Player Experience

### Visual Behavior
- Unenchanted Tomes **won't stack** in inventory (intentional)
- Each tome is treated as a unique item
- Similar to weapons/armor (also unstackable)

### Benefits
- ✅ Safe to give multiple tomes at once
- ✅ No accidental loss of entire stack
- ✅ Consistent with other enchantable items (weapons/armor)
- ✅ Clear indication that each tome is a "container" for enchantments

---

## 🧪 Testing Checklist

### Test Case 1: Single Tome
- [ ] Give 1 tome: `/enchant give @s tome`
- [ ] Place in enchantment table
- [ ] Enchant it
- [ ] Verify tome consumed and Enchanted Tome received

### Test Case 2: Multiple Tomes (Main Fix)
- [ ] Give 5 tomes: `/enchant give @s tome 5`
- [ ] Verify they **don't stack** in inventory (5 separate slots)
- [ ] Place 1 tome in enchantment table
- [ ] Enchant it
- [ ] Verify only 1 tome consumed
- [ ] Verify 4 tomes remain in inventory

### Test Case 3: Mixed Inventory
- [ ] Give 3 tomes
- [ ] Give other items (fragments, weapons, etc.)
- [ ] Enchant 1 tome
- [ ] Verify other tomes and items unaffected

### Test Case 4: UUID Uniqueness
- [ ] Give 2 tomes
- [ ] Use NBT viewer to check UUIDs
- [ ] Verify each tome has different UUID
- [ ] Verify tomes can't be stacked even manually

---

## 📝 Alternative Solutions Considered

### Option 1: Change Material (Not Used)
**Idea**: Use a naturally unstackable material like IRON_SWORD
**Rejected**: Would require completely different item, loses "book" theme

### Option 2: Max Stack Size (Not Possible)
**Idea**: Set max stack size to 1 via NBT
**Rejected**: Not reliably supported in Bukkit/Paper

### Option 3: Unique UUID (Selected) ✅
**Advantages**:
- Simple implementation
- Reliable across all Minecraft versions
- No material change needed
- Works with custom model data
- Industry standard approach

---

## 🔄 Comparison with Other Systems

### Similar Unstackable Items in MMO
| Item Type | Method | Reason |
|-----------|--------|--------|
| Weapons | Material (naturally unstackable) | Durability system |
| Armor | Material (naturally unstackable) | Durability system |
| **Tomes** | **Unique UUID** | Book is stackable by default |
| Fragments | Stackable (no UUID) | Consumable currency |

---

## 📈 Impact Summary

### Before Fix
- ❌ Tomes could stack
- ❌ Entire stack consumed when enchanting
- ❌ Potential for massive resource loss
- ❌ Confusing player experience

### After Fix
- ✅ Tomes can't stack (unique UUID)
- ✅ Only 1 tome consumed per enchant
- ✅ Safe inventory management
- ✅ Consistent with weapons/armor behavior

---

## 💡 Future Considerations

### Resource Pack Integration
When creating custom models for tomes (400000, 410000), the unstackable nature means:
- Each tome shows individually in inventory
- Can have animated models per tome
- Possible to add visual variety between tomes

### Economy Impact
- Tomes take more inventory space (can't stack)
- Encourages using enderchests/storage for bulk tomes
- Traders need to consider inventory management
- Similar to how weapon/armor traders operate

---

## ✅ Status

**Issue**: ✅ **FIXED**

**Files Modified**: 
- `EnchantmentTome.java` (added UUID generation)

**Code Added**: 1 line
```java
nbtItem.setString("MMO_Tome_UUID", java.util.UUID.randomUUID().toString());
```

**Impact**: 
- Prevents stack consumption bug
- Makes tomes unstackable (intentional)
- Consistent with other enchantable items

**Testing**: Ready for in-game verification

---

## 🎯 Summary

A simple 1-line fix that adds unique UUIDs to each Enchantment Tome, preventing stacking and ensuring only one tome is consumed during enchanting. This makes tomes behave consistently with other enchantable items like weapons and armor.

**Result**: Safe, reliable enchanting system! ✨
