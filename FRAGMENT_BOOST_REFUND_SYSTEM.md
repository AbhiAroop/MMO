# Fragment Boost Refund System

## 🔄 Overview

The fragment boosting system has been updated to intelligently consume only the minimum fragments needed to reach 100% apply chance and refund any excess fragments back to the player.

---

## ✨ New Behavior

### Smart Fragment Consumption

**Before:**
- Used a fixed amount per boost (5% per fragment, max 25% boost)
- Consumed all fragments even if enchantments would hit 100% before using them all
- No refunds for excess fragments

**After:**
- Calculates the minimum fragments needed to bring the **highest enchantment** to 100%
- Consumes only that amount
- **Refunds excess fragments** to the player
- Each fragment still provides 5% boost

---

## 📐 How It Works

### Calculation Logic

1. **Identify Matching Enchantments**
   - Find all enchantments in the tome that match the fragment element
   - Include hybrid enchantments (match either primary or secondary element)

2. **Calculate Fragments Needed**
   - For each matching enchantment, calculate: `fragmentsNeeded = ceil((100 - currentChance) / 5)`
   - Track the **maximum** fragments needed across all enchantments
   - This ensures all matching enchantments reach 100% together

3. **Smart Consumption**
   - Consume: `min(availableFragments, maxFragmentsNeeded)`
   - Apply boost: `consumedFragments × 5%` to all matching enchants (capped at 100%)
   - Refund: `availableFragments - consumedFragments`

---

## 💡 Examples

### Example 1: Single Enchantment

**Tome:**
- Flame II [Fire] - 75% apply chance

**Input:**
- 10 Fire Fragments

**Calculation:**
- Fragments needed: `(100 - 75) / 5 = 5 fragments`
- Fragments consumed: `5`
- New apply chance: `75% + (5 × 5%) = 100%`
- **Refund: 5 fragments**

**Result:**
- Flame II now at 100%
- Player gets 5 fragments back
- Cost: 5 XP, 500 Essence (for 5 fragments consumed)

---

### Example 2: Multiple Enchantments

**Tome:**
- Flame II [Fire] - 60% apply chance
- Ignite I [Fire] - 85% apply chance
- Frost I [Ice] - 70% apply chance

**Input:**
- 12 Fire Fragments

**Calculation:**
- Flame needs: `(100 - 60) / 5 = 8 fragments`
- Ignite needs: `(100 - 85) / 5 = 3 fragments`
- Frost: ignored (doesn't match fire)
- Maximum needed: `8 fragments` (limited by Flame)
- Fragments consumed: `8`

**Result:**
- Flame II: `60% + 40% = 100%` ✓
- Ignite I: `85% + 40% = 100%` ✓ (capped, only needed 15% but got 40%)
- Frost I: `70%` (unchanged)
- **Refund: 4 fragments**
- Cost: 5 XP per enchant × 2 = 10 XP, 800 Essence

---

### Example 3: Already at 100%

**Tome:**
- Tempest III [Lightning] - 100% apply chance

**Input:**
- 5 Lightning Fragments

**Result:**
- No boost possible (already at 100%)
- Combination invalid - returns `null`
- All fragments remain in slot (not consumed)

---

### Example 4: Insufficient Fragments

**Tome:**
- Flame II [Fire] - 50% apply chance

**Input:**
- 3 Fire Fragments

**Calculation:**
- Fragments needed: `(100 - 50) / 5 = 10 fragments`
- Fragments available: `3`
- Fragments consumed: `3` (use what's available)

**Result:**
- Flame II: `50% + 15% = 65%`
- **No refund** (all 3 fragments used)
- Cost: 5 XP, 300 Essence

---

## 🎮 Player Experience

### Visual Feedback

When fragments are refunded, the player receives:
```
✓ Items combined successfully!
Cost: 10 XP & 800 Essence
⟳ Refunded 4 excess fragments (enchants already at 100%)
```

### Inventory Management
- Refunded fragments automatically added to player inventory
- If inventory is full, items drop at player's feet (standard Minecraft behavior)

---

## 🔧 Technical Changes

### CombineResult Class Updates

**New Fields:**
```java
private final ItemStack refund; // Optional refund item
```

**New Methods:**
```java
public ItemStack getRefund()
public boolean hasRefund()
```

**New Constructor:**
```java
public CombineResult(ItemStack result, int xpCost, int essenceCost, ItemStack refund)
```

### boostTomeWithFragments() Method

**Algorithm:**
1. **First Pass:** Calculate max fragments needed
   - Loop through all enchantments
   - Find matching elements
   - Calculate fragments to reach 100% for each
   - Track maximum needed

2. **Second Pass:** Apply boost uniformly
   - Use calculated fragment amount
   - Apply same boost to all matching enchants
   - Cap at 100% for each

3. **Create Refund:**
   - Calculate excess: `availableFragments - consumedFragments`
   - Create ItemStack clone with excess amount
   - Return in CombineResult

### AnvilGUIListener Updates

**Refund Handling:**
```java
if (actualResult.hasRefund()) {
    player.getInventory().addItem(actualResult.getRefund());
    player.sendMessage(ChatColor.YELLOW + "⟳ Refunded " + 
                      actualResult.getRefund().getAmount() + 
                      " excess fragments (enchants already at 100%)");
}
```

---

## 🎯 Benefits

### For Players
✅ **No Waste:** Don't lose fragments when enchants hit 100%  
✅ **Predictable:** Know exactly how many fragments you need  
✅ **Efficient:** Use expensive fragments optimally  
✅ **Fair:** Get back what you don't need  

### For Game Balance
✅ **Resource Conservation:** Encourages strategic fragment use  
✅ **Clear Mechanics:** Transparent system (5% per fragment)  
✅ **No Exploits:** Can't boost beyond 100%  
✅ **Consistent Costs:** Essence cost scales with fragments consumed  

---

## 📊 Cost Breakdown

| Fragments Consumed | XP Cost (per enchant) | Essence Cost | Boost Applied |
|-------------------|-----------------------|--------------|---------------|
| 1 | 5 XP | 100 | +5% |
| 2 | 5 XP | 200 | +10% |
| 3 | 5 XP | 300 | +15% |
| 5 | 5 XP | 500 | +25% |
| 10 | 5 XP | 1,000 | +50% |
| 20 | 5 XP | 2,000 | +100%* |

*Capped at 100% apply chance

**Note:** XP cost is per enchantment boosted, not per fragment

---

## 🐛 Bug Fixes Included

### Fixed: NBT Prefix Error
- Corrected `"MMO_Enchantment_"` → `"MMO_Enchant_"`
- This was causing apply chances to be read as 0%
- Now correctly reads existing apply chance from tome

---

## 🚀 Future Enhancements (Potential)

- **Preview System:** Show how many fragments will be consumed before combining
- **Fragment Calculator:** UI showing exact fragments needed per enchant
- **Bulk Boosting:** Boost multiple tomes at once with smart distribution
- **Element Matching UI:** Highlight which enchants match the fragments

---

## ✅ Status

**IMPLEMENTED** ✓  
**TESTED** ✓  
**BUILD SUCCESS** ✓  
**DEPLOYED** ✓

---

*Last Updated: October 20, 2025*
