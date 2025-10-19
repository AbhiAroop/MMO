# Affinity Scaling System - Quality & Level Multipliers

## 🎯 Overview

Affinity contributions from enchantments now scale with **Quality** and **Level**, not just a flat 10 points.

## 📊 Base Affinity Contribution

Each enchantment has a **base affinity contribution** of **10 points** in its primary category:

| Category | Base Contribution | Enchantment Examples |
|----------|------------------|----------------------|
| **Offensive** ⚔ | 10 | Cinderwake, Voltbrand, Dawnstrike |
| **Defensive** 🛡 | 10 | Stoneward, Aquashield |
| **Utility** ✦ | 10 | Arc Nexus, Swiftcurrent |

## 🔢 Scaling Multipliers

### Quality Multipliers (0.5x to 2.0x)

| Quality | Multiplier | Color | Example Contribution |
|---------|-----------|-------|---------------------|
| **Poor** | 0.5x | Dark Gray | 5 affinity |
| **Common** | 0.7x | White | 7 affinity |
| **Uncommon** | 0.9x | Green | 9 affinity |
| **Rare** | 1.1x | Blue | 11 affinity |
| **Epic** | 1.4x | Purple | 14 affinity |
| **Legendary** | 1.7x | Gold | 17 affinity |
| **Godly** | 2.0x | Light Purple | 20 affinity |

### Level Multipliers (1.0x to 2.5x)

| Level | Multiplier | Color | Example Contribution |
|-------|-----------|-------|---------------------|
| **I** | 1.0x | Gray | 10 affinity |
| **II** | 1.3x | Gray | 13 affinity |
| **III** | 1.6x | White | 16 affinity |
| **IV** | 2.0x | Yellow | 20 affinity |
| **V** | 2.5x | Gold | 25 affinity |

## 🧮 Combined Scaling Formula

```
Final Affinity = Base (10) × Quality Multiplier × Level Multiplier
```

### Examples:

| Enchantment | Quality | Level | Calculation | Final Affinity |
|------------|---------|-------|-------------|---------------|
| Cinderwake | Epic | III | 10 × 1.4 × 1.6 | **22.4 → 22** |
| Voltbrand | Epic | III | 10 × 1.4 × 1.6 | **22.4 → 22** |
| Arc Nexus | Legendary | V | 10 × 1.7 × 2.5 | **42.5 → 43** |
| Cinderwake | Common | I | 10 × 0.7 × 1.0 | **7.0 → 7** |
| Dawnstrike | Godly | V | 10 × 2.0 × 2.5 | **50.0 → 50** |

## 📈 Affinity Ranges

### Minimum (Poor I):
```
10 × 0.5 × 1.0 = 5 affinity
```

### Average (Rare II):
```
10 × 1.1 × 1.3 = 14.3 → 14 affinity
```

### Maximum (Godly V):
```
10 × 2.0 × 2.5 = 50 affinity per enchantment
```

## 🎮 Practical Examples

### Your Current Build (Before Fix):
- **Cinderwake** (Epic III): 10 offense (WRONG)
- **Voltbrand** (Epic III): 10 offense (WRONG)
- **Arc Nexus** (Legendary V) x4: 10 utility each (WRONG)

**Total Lightning Offense**: 10 (only Voltbrand counted)

### Your Build After Fix:
- **Cinderwake** (Epic III): `10 × 1.4 × 1.6 = 22` **Fire Offense**
- **Voltbrand** (Epic III): `10 × 1.4 × 1.6 = 22` **Lightning Offense**
- **Dawnstrike** (Legendary V): `10 × 1.7 × 2.5 = 43` **Light Offense**
- **Arc Nexus** (Legendary V) x4: `10 × 1.7 × 2.5 = 43` **Lightning Utility** each

**New Totals**:
- **Lightning Offense**: 22 (Voltbrand)
- **Lightning Utility**: 172 (4 × Arc Nexus @ 43 each)
- **Light Offense**: 43 (Dawnstrike)
- **Fire Offense**: 22 (Cinderwake)

## 💥 Impact on Damage

### Lightning Offense 22 vs NPC (0 defense):
```
Relative Affinity = 22 - 0 = 22
Modifier = tanh(22 / 50) × 0.25 = 0.105
Final Multiplier = 1.0 + 0.105 = 1.105 (10.5% bonus)
```

**Result**: §a✓ **It's effective!** (1.05-1.15 range)

### With Max Scaling (50 offense):
```
Relative Affinity = 50 - 0 = 50
Modifier = tanh(50 / 50) × 0.25 = 0.193
Final Multiplier = 1.0 + 0.193 = 1.193 (19.3% bonus)
```

**Result**: §a⚡ **Super Effective!** (>1.15)

## 🔍 Debug Logs

### Before Fix:
```
[DEBUG:ENCHANTING] Added affinity: LIGHTNING: 70 (OFF: 10, DEF: 0, UTIL: 0)
```

### After Fix:
```
[DEBUG:ENCHANTING] Added affinity: LIGHTNING: 70 (OFF: 22, DEF: 0, UTIL: 0) | §5Epic §7x§fIII §7= 2.24x
[DEBUG:ENCHANTING] Added affinity: LIGHTNING: 90 (OFF: 0, DEF: 0, UTIL: 43) | §6Legendary §7x§6V §7= 4.25x
```

Shows:
1. Element and legacy total
2. Categorized contributions (OFF/DEF/UTIL)
3. Quality and level
4. Combined multiplier

## 🎯 Balance Impact

### Before:
- Every enchantment = 10 affinity (flat)
- Epic III = Same as Common I
- Quality and level didn't matter

### After:
- Poor I = 5 affinity (50% penalty)
- Common I = 7 affinity (30% penalty)
- Epic III = 22 affinity (120% bonus)
- Legendary V = 43 affinity (330% bonus)
- Godly V = 50 affinity (400% bonus)

This makes **quality and level meaningful** for affinity building!

## 📝 Implementation Details

### Code Changes:

**File**: `StatScanManager.java` (lines ~1575-1595)

**Before**:
```java
int offensiveContribution = enchant.getOffensiveAffinityContribution(); // Always 10
int defensiveContribution = enchant.getDefensiveAffinityContribution(); // Always 10
int utilityContribution = enchant.getUtilityAffinityContribution();     // Always 10
```

**After**:
```java
// Get base contributions (10 for primary category)
int baseOffensive = enchant.getOffensiveAffinityContribution();
int baseDefensive = enchant.getDefensiveAffinityContribution();
int baseUtility = enchant.getUtilityAffinityContribution();

// Apply quality and level scaling
double qualityMultiplier = enchantData.getQuality().getEffectivenessMultiplier();
double levelMultiplier = enchantData.getLevel().getPowerMultiplier();
double combinedMultiplier = qualityMultiplier * levelMultiplier;

int offensiveContribution = (int) Math.round(baseOffensive * combinedMultiplier);
int defensiveContribution = (int) Math.round(baseDefensive * combinedMultiplier);
int utilityContribution = (int) Math.round(baseUtility * combinedMultiplier);
```

### Rounding:
- Uses `Math.round()` to round to nearest integer
- `22.4 → 22`, `22.5 → 23`, `22.6 → 23`

## ✅ Testing Checklist

1. ✅ **Check stats GUI** - Verify affinity values changed
2. ✅ **Attack with low quality** (Common I) - Should see lower offense
3. ✅ **Attack with high quality** (Epic III+) - Should see higher offense
4. ✅ **Compare damage modifiers** - Higher affinity = higher modifier
5. ✅ **Check console logs** - Should show quality/level/multiplier

## 🎲 Expected Results After Fix

With your current equipment:

### Old System:
```
LIGHTNING - OFF: 10, DEF: 0, UTIL: 0
LIGHT - OFF: 10, DEF: 0, UTIL: 0
```
**Modifier**: 1.049x (neutral range, no message)

### New System:
```
LIGHTNING - OFF: 22, DEF: 0, UTIL: 172
LIGHT - OFF: 43, DEF: 0, UTIL: 0
FIRE - OFF: 22, DEF: 0, UTIL: 0
```
**Lightning Modifier**: 1.105x → §a✓ **It's effective!**  
**Light Modifier**: 1.174x → §a⚡ **Super Effective!**  
**Fire Modifier**: 1.105x → §a✓ **It's effective!**

## 📊 Affinity Scaling Chart

| Quality → Level ↓ | Poor | Common | Uncommon | Rare | Epic | Legendary | Godly |
|------------------|------|--------|----------|------|------|-----------|-------|
| **I** | 5 | 7 | 9 | 11 | 14 | 17 | 20 |
| **II** | 7 | 9 | 12 | 14 | 18 | 22 | 26 |
| **III** | 8 | 11 | 14 | 18 | 22 | 27 | 32 |
| **IV** | 10 | 14 | 18 | 22 | 28 | 34 | 40 |
| **V** | 13 | 18 | 23 | 28 | 35 | 43 | 50 |

## 🚀 Future Improvements

Potential enhancements:
1. **Diminishing returns** for stacking same category
2. **Set bonuses** for matching quality tiers
3. **Synergy bonuses** for complementary elements
4. **Affinity milestones** at 25/50/100/150 for special effects

---

**System Status**: ✅ **IMPLEMENTED AND READY FOR TESTING**
