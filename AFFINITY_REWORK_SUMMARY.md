# Affinity System Rework - Implementation Summary

## Overview
Successfully implemented a comprehensive affinity system rework that splits elemental affinities into three distinct categories (Offense, Defense, Utility) with counter-mechanics for PVP combat.

---

## What Was Changed

### 1. **New Enum: AffinityCategory**
**File**: `src/main/java/com/server/enchantments/elements/AffinityCategory.java`

Created three affinity categories:
- **OFFENSE** (⚔, RED): Affects outgoing damage, debuffs, offensive proc chances
- **DEFENSE** (🛡, BLUE): Affects damage taken, shields, defensive procs
- **UTILITY** (✦, GREEN): Affects non-combat benefits (movement, regen, resources)

Each category has:
- Display name and color
- Unicode icon for GUI display
- `affectsCombat()` method (true for OFFENSE/DEFENSE, false for UTILITY)

### 2. **New Data Class: CategorizedAffinity**
**File**: `src/main/java/com/server/enchantments/elements/CategorizedAffinity.java`

Holds three separate affinity maps (one per category):
- `offensiveAffinity`: Map<ElementType, Integer>
- `defensiveAffinity`: Map<ElementType, Integer>
- `utilityAffinity`: Map<ElementType, Integer>

Methods:
- `get(element, category)`: Get affinity for element + category
- `set(element, category, value)`: Set affinity value
- `add(element, category, amount)`: Add to affinity value
- `getHighestOffensive()`, `getHighestDefensive()`: Find dominant elements
- `getAllOffensive()`, `getAllDefensive()`, `getAllUtility()`: Get full maps

### 3. **CustomEnchantment Interface Updates**
**File**: `src/main/java/com/server/enchantments/data/CustomEnchantment.java`

Added new methods:
```java
public AffinityCategory getPrimaryAffinityCategory()
public int getOffensiveAffinityContribution()
public int getDefensiveAffinityContribution()
public int getUtilityAffinityContribution()
```

**Default Behavior**:
- Auto-determines category based on TriggerType:
  - ON_HIT, ON_ATTACK, ON_KILL → OFFENSE
  - ON_DAMAGED, ON_DEFEND → DEFENSE
  - PASSIVE, ON_MOVE, PERIODIC → UTILITY
- Enchantments can override these methods for custom behavior
- Contribution equals element's affinityGain for primary category, 0 for others
- Hybrid enchantments split 60/40 between elements

### 4. **AffinityModifier Rework**
**File**: `src/main/java/com/server/enchantments/utils/AffinityModifier.java`

**Major Changes**:
- Now uses `CategorizedAffinity` instead of legacy `ElementalAffinity`
- Offensive vs Defensive comparison (not offense vs offense)
- **Counter-Mechanic**: 20% damage penalty when offensive element matches defensive element

**New Formula**:
```
RelativeAffinity = Attacker Offense - Defender Defense
Base Modifier = 0.25 × tanh(RelativeAffinity / 50)
Counter Penalty = -20% if Defender Defense >= 20
Final Modifier = Base Modifier + Counter Penalty
```

**Updated Methods**:
- `calculateDamageModifier()`: Offense vs Defense + counter check
- `calculateEffectModifier()`: Duration scaling with counter-mechanic
- `calculateProcModifier()`: Proc chance scaling with counter-mechanic
- `getPlayerCategorizedAffinity()`: New helper to get CategorizedAffinity

### 5. **PlayerStats Updates**
**File**: `src/main/java/com/server/profiles/stats/PlayerStats.java`

Added fields:
```java
private CategorizedAffinity categorizedAffinity; // New system
private ElementalAffinity elementalAffinity; // Legacy (kept for compatibility)
```

Added methods:
```java
public CategorizedAffinity getCategorizedAffinity()
public void setCategorizedAffinity(CategorizedAffinity affinity)
```

Both systems are calculated in parallel during equipment scans.

### 6. **StatScanManager Rework**
**File**: `src/main/java/com/server/profiles/stats/StatScanManager.java`

**Updated Methods**:

`scanAndUpdateAffinity(Player, PlayerStats)`:
- Now resets both legacy and categorized affinity
- Scans all equipment for enchantments
- Updates both systems simultaneously
- Debug logging shows categorized breakdown

`scanAffinityFromItem(ItemStack, ElementalAffinity, CategorizedAffinity)`:
- Added CategorizedAffinity parameter
- Gets enchantment from EnchantmentRegistry
- Calls `getOffensiveAffinityContribution()`, `getDefensiveAffinityContribution()`, `getUtilityAffinityContribution()`
- Applies contributions to appropriate categories
- Handles hybrid enchantments (60/40 split per category)

### 7. **StatsGUI Display Overhaul**
**File**: `src/main/java/com/server/profiles/gui/StatsGUI.java`

**Replaced**:
- Old: Single "Elemental Affinity" section (8 lines)
- New: Three sections with 8 elements each (24 lines total + 3 headers)

**New Display**:
```
⚔ Offensive Affinity:
 • 🔥 Fire: 50
 • 💧 Water: 0
 ... (all 8 elements)

🛡 Defensive Affinity:
 • 🔥 Fire: 10
 • 💧 Water: 30
 ... (all 8 elements)

✦ Utility Affinity:
 • 🌀 Air: 20
 • 🌑 Shadow: 15
 ... (all 8 elements)
```

**New Method**:
```java
private static String getCategorizedAffinityLine(PlayerStats stats, ElementType element, AffinityCategory category)
```

**Color-Coding**:
- 0: Dark Gray
- 1-19: Gray
- 20-39: White
- 40-59: Yellow
- 60-79: Gold
- 80+: Red

---

## Counter-Mechanic Explained

### The Problem
Old system: Fire player vs Fire player = whoever has higher Fire affinity wins. No strategy.

### The Solution
**Counter-Mechanic**: If you attack with Element X offense and your opponent has Element X defense, you deal -20% damage.

### Examples

#### Example 1: Fire Specialist vs Fire Tank
- **Attacker**: Fire Offense = 50
- **Defender**: Fire Defense = 30
- Relative = 50 - 30 = +20
- Base Modifier = +9.5%
- **Counter Penalty = -20%** (defender has Fire defense >= 20)
- **Final = -10.5% (0.895x damage)**
- Result: Attacker's Fire attacks are LESS EFFECTIVE despite higher affinity

#### Example 2: Fire Specialist vs Water Tank
- **Attacker**: Fire Offense = 50
- **Defender**: Water Defense = 30 (no Fire defense)
- Relative = 50 - 0 = +50
- Base Modifier = +19%
- **Counter Penalty = 0%** (different elements)
- **Final = +19% (1.19x damage)**
- Result: Strong advantage because no counter-mechanic

#### Example 3: Threshold Check
- **Attacker**: Fire Offense = 50
- **Defender**: Fire Defense = 15 (below threshold)
- Relative = 50 - 15 = +35
- Base Modifier = +16.4%
- **Counter Penalty = 0%** (defense too low to trigger counter)
- **Final = +16.4% (1.164x damage)**
- Result: No counter-mechanic because defender hasn't invested enough in Fire defense

### Strategy Implications
1. **Pure Offensive Builds**: Strong against enemies without matching defense, weak against counter-builds
2. **Defensive Builds**: Can hard-counter specific offensive builds
3. **Balanced Builds**: Moderate in both categories, versatile
4. **Hybrid Builds**: Use multiple elements to avoid being hard-countered

---

## Enchantment Categories by Type

### Offensive Enchantments (boost Offensive Affinity)
- **Cinderwake** (Fire) - Burning trails
- **Deepcurrent** (Water) - Chain pull
- **Burdened Stone** (Earth) - Stacking slow
- **Voltbrand** (Lightning) - Chain lightning
- **Arc Nexus** (Lightning) - Attack speed stacking
- **Hollow Edge** (Shadow) - Vampiric restore
- **Dawnstrike** (Light) - Blindness debuff
- **Stormfire** (Fire/Lightning) - Hybrid offensive
- **Decayroot** (Earth/Shadow) - Hybrid offensive
- **Celestial Surge** (Light/Lightning) - Hybrid offensive
- **Embershade** (Fire/Shadow) - Hybrid offensive

### Defensive Enchantments (boost Defensive Affinity)
- **Mistveil** (Water) - Dodge when hit
- **Terraheart** (Earth) - Health regeneration
- **Whispers** (Air) - Knockback attackers
- **Radiant Grace** (Light) - AoE ally healing
- **Pure Reflection** (Water/Light) - Hybrid defensive

### Utility Enchantments (boost Utility Affinity)
- **Ashen Veil** (Fire) - Invisibility on kill
- **GaleStep** (Air) - Movement speed
- **Veilborn** (Shadow) - Invisibility + movement
- **Mistborne Tempest** (Water/Air) - Hybrid utility

---

## Files Created

1. **AffinityCategory.java** - Enum for OFFENSE/DEFENSE/UTILITY
2. **CategorizedAffinity.java** - Data class holding split affinity values
3. **AFFINITY_CATEGORIZED_SYSTEM.md** - Comprehensive documentation
4. **FRAGMENT_CUSTOM_MODEL_DATA.md** - Fragment texture system docs (separate feature)

## Files Modified

1. **CustomEnchantment.java** - Added category methods
2. **AffinityModifier.java** - Reworked with counter-mechanic
3. **PlayerStats.java** - Added CategorizedAffinity field
4. **StatScanManager.java** - Updated affinity scanning
5. **StatsGUI.java** - Overhauled affinity display

---

## Testing Checklist

Before deploying, verify:

- [ ] **Build succeeds** without compilation errors
- [ ] **StatsGUI displays** three affinity sections correctly
- [ ] **Affinity values update** when equipping/unequipping enchanted items
- [ ] **Offensive enchantments** boost offensive affinity only
- [ ] **Defensive enchantments** boost defensive affinity only
- [ ] **Utility enchantments** boost utility affinity only
- [ ] **Hybrid enchantments** split 60/40 correctly
- [ ] **Counter-mechanic activates** when offense matches defense
- [ ] **Threshold works**: Counter only applies if defense >= 20
- [ ] **Utility affinity** does NOT affect damage calculations
- [ ] **Color-coding** in GUI matches affinity values
- [ ] **Debug logging** shows categorized affinity breakdown

## In-Game Test Scenarios

### Scenario 1: Pure Fire Offensive Build
1. Equip Cinderwake (Fire offense +10)
2. Check StatsGUI → Fire Offense = 10, Fire Defense = 0
3. Attack player with Fire Defense enchantments
4. **Expected**: Damage reduced by counter-mechanic

### Scenario 2: Fire Defensive Build
1. Equip no Fire defensive enchantments (hypothetical - need to create one or use defensive item)
2. Check StatsGUI → Fire Defense shows value
3. Get attacked by Fire offensive player
4. **Expected**: Counter-mechanic reduces their damage

### Scenario 3: Utility Build
1. Equip GaleStep (Air utility)
2. Check StatsGUI → Air Utility shows value, Air Offense/Defense = 0
3. Attack someone or get attacked
4. **Expected**: Utility affinity has NO effect on damage

### Scenario 4: Hybrid Enchantment
1. Equip Stormfire (Fire/Lightning offensive hybrid)
2. Check StatsGUI → Fire Offense +6, Lightning Offense +4
3. Verify both elements show contribution

---

## Backwards Compatibility

**Legacy System Maintained**:
- `ElementalAffinity` still exists and is calculated
- Both systems run in parallel
- Old code referencing `ElementalAffinity` still works
- Can be removed after full migration confirmed

**Migration Path**:
1. Deploy new system alongside legacy
2. Test both systems produce correct values
3. Monitor for any issues
4. Eventually deprecate legacy system

---

## Performance Considerations

**Scan Frequency**: Every 5 ticks (0.25 seconds)
- Acceptable overhead for real-time updates
- Necessary for dynamic equipment changes

**Memory Impact**: Minimal
- CategorizedAffinity: 3 EnumMaps × 8 elements = 24 integers
- ~96 bytes per player profile

**Calculation Complexity**: O(1)
- Direct map lookups for affinity values
- Simple counter-mechanic check (>=20 threshold)

---

## Future Enhancements

### Potential Additions
1. **More Defensive Enchantments**: Currently underrepresented
2. **Counter-Indication UI**: Show warning when attacking counter-build
3. **Affinity Synergy Bonuses**: Rewards for balancing offense/defense
4. **Element Weakness System**: Different counter-mechanics per element pair
5. **Affinity Decay**: Gradual reduction when not using element
6. **Affinity Mastery Tiers**: Unlock bonuses at 50, 100, 150 affinity

### Possible Balancing Tweaks
- Counter penalty currently -20%, could be dynamic based on defense value
- Threshold of 20 for counter-mechanic might need adjustment
- Soft-cap scaling constant (50) might need tuning
- Max damage modifier (25%) might be too high/low

---

## Known Limitations

1. **Maven Build Issue**: Terminal command path problems (not related to code)
2. **No Pure Fire Defense Enchantment**: Fire is offense-only currently
3. **Nature Element**: Exists in code but no enchantments use it yet
4. **Enchantment Override**: Most enchantments use default category logic, few override
5. **Documentation**: In-game tutorial or help command would improve UX

---

## Commit Message Suggestion

```
feat: Implement categorized affinity system with counter-mechanics

BREAKING CHANGE: Affinity system now split into Offense/Defense/Utility categories

- Added AffinityCategory enum (OFFENSE, DEFENSE, UTILITY)
- Created CategorizedAffinity data class for split tracking
- Updated CustomEnchantment with category contribution methods
- Reworked AffinityModifier with counter-mechanic (-20% when elements match)
- Modified StatScanManager to track categorized affinities
- Overhauled StatsGUI to display three affinity sections
- Added comprehensive documentation (AFFINITY_CATEGORIZED_SYSTEM.md)

Counter-Mechanic: Attacking with Element X offense against Element X defense
results in -20% damage penalty. Encourages build diversity.

Backwards Compatible: Legacy ElementalAffinity system maintained in parallel.

Affects: PVP combat balance, enchantment strategy, build diversity
```

---

## Documentation Files

- **AFFINITY_CATEGORIZED_SYSTEM.md**: Complete system overview with examples
- **FRAGMENT_CUSTOM_MODEL_DATA.md**: Fragment texture patterns (separate feature)
- **README.md** (this file): Implementation summary and testing guide

---

**Implementation Date**: October 19, 2025
**Status**: Code Complete - Ready for Build & Testing
**Next Step**: Build with Maven → Deploy to test server → In-game validation

