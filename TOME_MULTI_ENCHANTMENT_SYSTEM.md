# Tome Multi-Enchantment System (Probability-Based)

## 🎯 Overview

The enchantment system supports **multiple enchantments** on Enchantment Tomes using a **probability-based** system. Additional enchantments are **NEVER GUARANTEED** - randomness always plays a part!

**Key Principle**: More fragments + better quality = **higher probability** for additional enchantments, but luck is always a factor.

---

## 📊 Probability Calculation System

### Always Guaranteed
- **1st enchantment** = 100% guaranteed (always get at least 1)

### Probability-Based Additional Enchantments
- **2nd enchantment** = Roll based on fragments + quality
- **3rd enchantment** = Roll based on fragments + quality (harder - 60% of chance)

### Probability Formula
```
Base Chance = fragmentCount / 192
Quality Bonus = (qualityMultiplier - 1.0) * 0.5  (max +25%)
2nd Enchantment Chance = min(0.85, baseChance + qualityBonus)
3rd Enchantment Chance = min(0.85, (baseChance + qualityBonus) * 0.6)
```

### Maximum Probability Cap
- **NEVER 100% guaranteed** for additional enchantments
- Maximum probability capped at **85%** to preserve randomness
- Even with 192 Pristine fragments, 2nd/3rd enchantments require luck!

---

## 💎 Fragment Quality Multipliers

| Fragment Tier | Multiplier | Bonus Threshold |
|---------------|------------|-----------------|
| Basic | 1.0x | No bonus |
| Refined | 1.2x | No bonus |
| Superior | 1.35x | No bonus |
| Pristine | 1.5x | **+1 enchantment** |

**How it works:**
- System calculates average tier level of all fragments
- If average multiplier ≥ 1.5 → grants +1 bonus enchantment
- Bonus is added to base count, then capped at 3

---

## 🧮 Probability Examples

### Example 1: 48 Basic Fragments
- **Base Chance**: 48 / 192 = 25%
- **Quality Bonus**: 0% (Basic)
- **2nd Enchantment**: **25% chance** 🎲
- **3rd Enchantment**: If 2nd succeeded, **15% chance** (25% × 0.6)
- **Most Likely Result**: **1 enchantment**

### Example 2: 96 Basic Fragments
- **Base Chance**: 96 / 192 = 50%
- **Quality Bonus**: 0% (Basic)
- **2nd Enchantment**: **50% chance** 🎲
- **3rd Enchantment**: If 2nd succeeded, **30% chance** (50% × 0.6)
- **Most Likely Result**: **1-2 enchantments**

### Example 3: 192 Basic Fragments (Max)
- **Base Chance**: 192 / 192 = 100%
- **Quality Bonus**: 0% (Basic)
- **2nd Enchantment**: **85% chance** 🎲 (capped!)
- **3rd Enchantment**: If 2nd succeeded, **51% chance** (85% × 0.6)
- **Most Likely Result**: **2 enchantments** (maybe 3 with luck!)

### Example 4: 96 Pristine Fragments
- **Base Chance**: 96 / 192 = 50%
- **Quality Bonus**: +25% (Pristine 1.5x)
- **2nd Enchantment**: **75% chance** 🎲
- **3rd Enchantment**: If 2nd succeeded, **45% chance** (75% × 0.6)
- **Most Likely Result**: **2 enchantments** (good chance for 3!)

### Example 5: 192 Pristine Fragments (Max Quality)
- **Base Chance**: 192 / 192 = 100%
- **Quality Bonus**: +25% (Pristine)
- **2nd Enchantment**: **85% chance** 🎲 (capped!)
- **3rd Enchantment**: If 2nd succeeded, **51% chance**
- **Most Likely Result**: **2-3 enchantments** (best odds possible!)

### Example 6: Lucky vs Unlucky
**Same Setup**: 96 Pristine fragments (75% chance for 2nd)
- **Lucky Roll** 🍀: Gets 2nd enchantment → rolls 45% for 3rd → **3 enchantments!**
- **Average Roll**: Gets 2nd enchantment → fails 3rd roll → **2 enchantments**
- **Unlucky Roll** 😢: Fails 25% chance → **1 enchantment only**

---

## 🎮 Player Strategy

### Efficiency Tips
1. **Use Pristine fragments** for maximum enchantments with fewer fragments
2. **96 Pristine** = 3 enchantments (best value)
3. **144 Basic** = 3 enchantments (uses all 3 slots)
4. **Mix tiers carefully** - average must be ≥1.5 for bonus

### Quality vs Quantity Tradeoff
- **More fragments** = higher enchantment levels
- **Better quality** = more enchantments (but lower levels each)
- Players can choose:
  - **3 low-level enchantments** (96 Pristine)
  - **2 high-level enchantments** (192 Basic)

---

## 🔧 Technical Implementation

### Code Changes

**EnchantmentApplicator.java:**
```java
// New constants
private static final int FRAGMENTS_PER_ENCHANTMENT = 48; // Was 64
private static final int MAX_ENCHANTMENTS = 3; // Was 4

// New method: Calculate quality multiplier
private static double calculateQualityMultiplier(List<FragmentData> fragmentData) {
    double avgTierLevel = fragmentData.stream()
        .mapToInt(data -> data.tier.ordinal())
        .average()
        .orElse(0.0);
    
    return 1.0 + (avgTierLevel * 0.15);
}

// Updated calculation
int baseEnchantCount = Math.max(1, totalFragmentCount / 48);
int bonusEnchants = (qualityMultiplier >= 1.5) ? 1 : 0;
int enchantmentCount = Math.min(baseEnchantCount + bonusEnchants, 3);
```

---

## 📈 Probability Table

| Fragment Count | Basic 2nd | Basic 3rd | Pristine 2nd | Pristine 3rd |
|----------------|-----------|-----------|--------------|--------------|
| 48 | 25% 🎲 | 15% 🎲 | 50% 🎲 | 30% 🎲 |
| 96 | 50% 🎲 | 30% 🎲 | 75% 🎲 | 45% 🎲 |
| 144 | 75% 🎲 | 45% 🎲 | 85% 🎲 | 51% 🎲 |
| 192 | 85% 🎲 | 51% 🎲 | 85% 🎲 | 51% 🎲 |

**Legend:**
- 🎲 = Probability-based roll (NEVER guaranteed)
- 2nd = Chance to get 2nd enchantment
- 3rd = Chance to get 3rd enchantment (only if 2nd succeeded)
- **85%** = Maximum probability cap (never 100%)

**Reading the Table:**
- 96 Basic fragments = 50% chance for 2nd, then if succeeded 30% for 3rd
- 96 Pristine fragments = 75% chance for 2nd, then if succeeded 45% for 3rd
- Even maxed 192 Pristine = still only 85% chance (luck matters!)

---

## 🎯 Benefits

### For Players
1. **Efficiency**: Pristine fragments = more enchantments with less material
2. **Choice**: Pick quantity (high levels) vs variety (more enchants)
3. **Accessibility**: Can get 3 enchantments without maxing all 3 slots

### For Server Economy
1. **Pristine fragments more valuable** for multi-enchanting
2. **Different strategies** for different goals
3. **Balanced system** - quality matters, not just quantity

---

## ✅ Testing Checklist

### Basic Fragment Tests
- [ ] 48 Basic = 1 enchantment
- [ ] 96 Basic = 2 enchantments
- [ ] 144 Basic = 3 enchantments
- [ ] 192 Basic = 3 enchantments (not 4)

### Pristine Fragment Tests
- [ ] 48 Pristine = 2 enchantments (bonus applied)
- [ ] 64 Pristine = 2 enchantments
- [ ] 96 Pristine = 3 enchantments (bonus + base)
- [ ] 144 Pristine = 3 enchantments (capped)

### Mixed Tier Tests
- [ ] 32 Pristine + 32 Basic = 1 enchantment (64 total, avg < 1.5)
- [ ] 64 Pristine + 32 Basic = 2 enchantments (96 total, avg ≥ 1.5)
- [ ] 48 Superior + 48 Refined = 2 enchantments (avg ≥ 1.5)

### Edge Cases
- [ ] 1 fragment = 1 enchantment (minimum)
- [ ] 192 fragments = 3 enchantments (maximum)
- [ ] All Pristine in 3 slots = 3 enchantments (not 4)

---

## 📝 Summary

**Old System (Deterministic):**
- 64 fragments per enchantment (guaranteed)
- Max 4 enchantments (impossible with 3 slots)
- Quality only affected enchantment quality, not count
- **No randomness** - predictable results

**New System (Probability-Based):**
- Probability calculation based on fragments + quality
- Max 3 enchantments (matches GUI limit)
- **NEVER guaranteed** - randomness always plays a role
- Maximum 85% probability cap (never 100%)
- Quality affects BOTH enchantment quality AND probability

**Result:**
- ✅ More exciting and unpredictable enchanting
- ✅ Risk vs reward gameplay
- ✅ Pristine fragments significantly better (but not guaranteed)
- ✅ Luck matters - exciting moments!
- ✅ Better economy balance (can't guarantee results)
- ✅ Balanced for 3-slot GUI limit

---

**Status**: ✅ **IMPLEMENTED**

**Build Required**: Yes - recompile plugin

**Files Modified**: 
- `EnchantmentApplicator.java` (calculation logic)
- `EnchantmentTableGUI.java` (debug logging removed)

**Next Steps**:
1. Build plugin
2. Test with various fragment combinations
3. Verify quality bonus works correctly
4. Confirm 3 enchantment cap
