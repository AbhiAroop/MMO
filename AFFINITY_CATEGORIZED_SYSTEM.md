# Affinity System Rework - Categorized Affinities

## Overview

The affinity system has been reworked to split elemental affinities into three distinct categories:
- **OFFENSE** (⚔): Affects outgoing damage, debuffs, and offensive proc chances
- **DEFENSE** (🛡): Affects damage taken, shields, and defensive procs
- **UTILITY** (✦): Affects non-combat benefits (movement, regeneration, resource management)

## Counter-Mechanic

**Key Rule**: When an attacker's offensive element matches a defender's defensive element, attacks are **LESS EFFECTIVE** (-20% penalty).

**Example**: If you build Fire offense and attack someone with Fire defense, your Fire attacks deal -20% damage.

**Reasoning**: Fighting fire with fire shouldn't be effective. This encourages build diversity and counter-play strategies.

---

## Enchantment Categories

### Offensive Enchantments (⚔)
These enchantments boost your **offensive affinity** for their element.

#### Fire Offensive
- **Cinderwake** (ON_HIT) - Leaves burning trails that ignite enemies

#### Water Offensive
- **Deepcurrent** (ON_HIT) - Chain pull effect, drags multiple enemies

#### Earth Offensive
- **Burdened Stone** (ON_HIT) - Stacking slow debuff

#### Air Offensive
- **None** - Air has no pure offensive enchantments (has utility and defensive)

#### Lightning Offensive
- **Voltbrand** (ON_HIT) - Chain lightning damage
- **Arc Nexus** (ON_HIT) - Stacking attack speed buff (offensive utility)

#### Shadow Offensive
- **Hollow Edge** (ON_KILL) - Vampiric health/mana restore

#### Light Offensive
- **Dawnstrike** (ON_HIT) - Blindness debuff with radiant burst

### Defensive Enchantments (🛡)
These enchantments boost your **defensive affinity** for their element.

#### Fire Defensive
- **None** - Fire is primarily offensive

#### Water Defensive
- **Mistveil** (ON_DAMAGED) - Dodge/evasion when hit

#### Earth Defensive
- **Terraheart** (PASSIVE) - Health regeneration at low health

#### Air Defensive
- **Whispers** (ON_DAMAGED) - Knockback attackers

#### Lightning Defensive
- **None** - Lightning is primarily offensive

#### Shadow Defensive
- **None** - Shadow is primarily offensive

#### Light Defensive
- **Radiant Grace** (ON_DAMAGED) - AoE ally healing when damaged

### Utility Enchantments (✦)
These enchantments boost your **utility affinity** for their element.
**Note**: Utility affinity does NOT affect combat damage calculations.

#### Fire Utility
- **Ashen Veil** (ON_KILL) - Invisibility when killing enemies

#### Water Utility
- **None** - Water focuses on offense/defense

#### Earth Utility
- **None** - Earth focuses on offense/defense

#### Air Utility
- **GaleStep** (PASSIVE) - Movement speed boost while sneaking

#### Lightning Utility
- **Arc Nexus** (ON_HIT) - Could be classified as offensive utility

#### Shadow Utility
- **Veilborn** (PASSIVE) - Double-sneak invisibility + movement speed

#### Light Utility
- **None** - Light focuses on offense/defense

---

## Hybrid Enchantments

Hybrid enchantments split their affinity contribution 60/40 between their two elements.

### Offensive Hybrid
- **Stormfire** (Fire 60% / Lightning 40%) - Offensive
- **Celestial Surge** (Light 60% / Lightning 40%) - Offensive

### Defensive Hybrid
- **None currently** - Most hybrids are offensive or utility

### Utility Hybrid
- **Mistborne Tempest** (Water 60% / Air 40%) - Utility (movement)
- **Decayroot** (Earth 60% / Shadow 40%) - Offensive (slow/decay)
- **Embershade** (Fire 60% / Shadow 40%) - Offensive
- **Pure Reflection** (Water 60% / Light 40%) - Defensive (shields)

---

## Category Breakdown by Enchantment

| Enchantment | Element | Category | Trigger Type | Contribution |
|-------------|---------|----------|--------------|--------------|
| **Cinderwake** | Fire | OFFENSE | ON_HIT | Fire Offense +10 |
| **Ashen Veil** | Fire | UTILITY | ON_KILL | Fire Utility +10 |
| **Deepcurrent** | Water | OFFENSE | ON_HIT | Water Offense +10 |
| **Mistveil** | Water | DEFENSE | ON_DAMAGED | Water Defense +10 |
| **Burdened Stone** | Earth | OFFENSE | ON_HIT | Earth Offense +10 |
| **Terraheart** | Earth | DEFENSE | PASSIVE | Earth Defense +10 |
| **GaleStep** | Air | UTILITY | PASSIVE | Air Utility +10 |
| **Whispers** | Air | DEFENSE | ON_DAMAGED | Air Defense +10 |
| **Voltbrand** | Lightning | OFFENSE | ON_HIT | Lightning Offense +10 |
| **Arc Nexus** | Lightning | OFFENSE | ON_HIT | Lightning Offense +10 |
| **Hollow Edge** | Shadow | OFFENSE | ON_KILL | Shadow Offense +10 |
| **Veilborn** | Shadow | UTILITY | PASSIVE | Shadow Utility +10 |
| **Dawnstrike** | Light | OFFENSE | ON_HIT | Light Offense +10 |
| **Radiant Grace** | Light | DEFENSE | ON_DAMAGED | Light Defense +10 |
| **Stormfire** | Fire/Lightning | OFFENSE | ON_HIT | Fire Off +6, Lightning Off +4 |
| **Mistborne Tempest** | Water/Air | UTILITY | PASSIVE | Water Util +6, Air Util +4 |
| **Decayroot** | Earth/Shadow | OFFENSE | ON_HIT | Earth Off +6, Shadow Off +4 |
| **Celestial Surge** | Light/Lightning | OFFENSE | ON_HIT | Light Off +6, Lightning Off +4 |
| **Embershade** | Fire/Shadow | OFFENSE | ON_HIT | Fire Off +6, Shadow Off +4 |
| **Pure Reflection** | Water/Light | DEFENSE | ON_DAMAGED | Water Def +6, Light Def +4 |

---

## Combat Calculations

### Damage Modifier Formula

```
Base Modifier = MAX_DAMAGE_MODIFIER × tanh(RelativeAffinity / 50)
RelativeAffinity = Attacker Offense - Defender Defense

Counter Penalty = -20% if Defender Defense >= 20
Final Modifier = Base Modifier + Counter Penalty
```

### Examples

#### Example 1: Fire vs Fire (Counter-Mechanic)
- Attacker: Fire Offense = 50
- Defender: Fire Defense = 30
- Relative Affinity = 50 - 30 = +20
- Base Modifier = 0.25 × tanh(20/50) = 0.25 × 0.379 = +9.5%
- Counter Penalty = -20% (defender has Fire defense >= 20)
- **Final Modifier = +9.5% - 20% = -10.5% (0.895x damage)**
- Result: Despite having higher affinity, the attacker deals LESS damage due to counter-mechanic

#### Example 2: Fire vs Water (No Counter)
- Attacker: Fire Offense = 50
- Defender: Water Defense = 30
- Relative Affinity = 50 (attacker Fire) vs 0 (defender has no Fire defense)
- Base Modifier = 0.25 × tanh(50/50) = 0.25 × 0.761 = +19%
- Counter Penalty = 0% (different elements)
- **Final Modifier = +19% (1.19x damage)**
- Result: Strong advantage because defender has no Fire defense

#### Example 3: Fire vs Fire (Low Defense)
- Attacker: Fire Offense = 50
- Defender: Fire Defense = 10 (below threshold)
- Relative Affinity = 50 - 10 = +40
- Base Modifier = 0.25 × tanh(40/50) = 0.25 × 0.655 = +16.4%
- Counter Penalty = 0% (defense below 20 threshold)
- **Final Modifier = +16.4% (1.164x damage)**
- Result: No counter-mechanic because defender's Fire defense is too low

---

## Build Strategy Implications

### Offensive Builds
- **Focus**: Stack ONE offensive element (e.g., all Fire offense)
- **Strength**: Maximum damage against enemies without that element's defense
- **Weakness**: Weak against counter-builds (enemies with matching defensive element)
- **Counter-Play**: Carry hybrid enchantments for backup damage types

### Defensive Builds
- **Focus**: Stack defensive affinity in common offensive elements
- **Strength**: Reduce damage from popular attack types via counter-mechanic
- **Weakness**: Lower offensive pressure
- **Counter-Play**: Mix defensive enchantments across multiple elements

### Balanced Builds
- **Focus**: Split between offense and defense in same element
- **Strength**: Can both deal and resist damage in chosen element
- **Weakness**: Lower peak values in either category
- **Counter-Play**: Most versatile in varied matchups

### Hybrid Builds
- **Focus**: Use hybrid enchantments for multi-element coverage
- **Strength**: Hard to counter, flexible damage types
- **Weakness**: Lower individual element affinity values
- **Counter-Play**: Good against specialized builds

---

## Display in Stats GUI

The StatsGUI now shows three sections:

```
=== ⚔ OFFENSIVE AFFINITY ===
🔥 Fire: 50
⚡ Lightning: 20
🌊 Water: 0
... (all 8 elements)

=== 🛡 DEFENSIVE AFFINITY ===
🔥 Fire: 10
💧 Water: 30
... (all 8 elements)

=== ✦ UTILITY AFFINITY ===
🌀 Air: 20
🌑 Shadow: 15
... (all 8 elements)
```

---

## Technical Implementation

### Files Modified
1. **AffinityCategory.java** - New enum (OFFENSE, DEFENSE, UTILITY)
2. **CategorizedAffinity.java** - Data class holding split affinity values
3. **CustomEnchantment.java** - Added category methods:
   - `getPrimaryAffinityCategory()`
   - `getOffensiveAffinityContribution()`
   - `getDefensiveAffinityContribution()`
   - `getUtilityAffinityContribution()`
4. **AffinityModifier.java** - Updated to use categorized affinity + counter-mechanic
5. **PlayerStats.java** - Added `CategorizedAffinity categorizedAffinity` field
6. **StatScanManager.java** - Updated to scan and track categorized affinity
7. **StatsGUI.java** - Display split into three sections

### Backwards Compatibility
- Legacy `ElementalAffinity` system still exists alongside new system
- Both are calculated simultaneously during equipment scans
- Can be removed after full migration

---

## Testing Checklist

- [ ] Verify categorized affinity calculation in StatScanManager
- [ ] Check StatsGUI displays all three categories
- [ ] Test counter-mechanic: Fire offense vs Fire defense
- [ ] Test normal advantage: Fire offense vs Water defense
- [ ] Test hybrid enchantments split affinity correctly (60/40)
- [ ] Verify utility affinity doesn't affect damage calculations
- [ ] Test threshold: counter-mechanic only applies if defense >= 20
- [ ] Verify multiple enchantments stack correctly
- [ ] Test affinity updates in real-time when equipment changes

---

*Affinity System 2.0 - Categorized Combat with Counter-Mechanics*
*Version 1.0 - October 2025*
