# Affinity System Testing - Player Setup Examples

## Overview

This guide provides specific enchantment setups for testing the categorized affinity system, counter-mechanics, and effectiveness feedback. Each setup is designed to test different aspects of the system.

---

## Testing Format

**⚠️ CRITICAL REQUIREMENT**: You **MUST** use **TWO REAL PLAYERS** for all tests.

### Why Two Players Required?

The categorized affinity system and effectiveness messages are **PVP-ONLY features**. They do NOT work against:
- ❌ NPCs (Citizens NPCs, custom NPCs)
- ❌ Mobs (vanilla zombies, skeletons, custom entities)
- ❌ Training dummies or combat dummies

### Setup Requirements

For each test, you need **TWO PLAYERS**:
- **Attacker** (Player A) - Real player with offensive enchantments
- **Defender** (Player B) - Real player with defensive enchantments

Use `/enchant add <enchantment> <quality> <level>` to apply enchantments.

### What You'll See Against NPCs (Wrong)
```
[TRIGGER] ✓ Triggering Cinderwake
[AFFINITY] PlayerA Offense(FIRE): 10.0 | NPC Defense(FIRE): 0.0
NO [AFFINITY] PVP: logs
NO [FEEDBACK] logs
NO in-game messages
```

### What You'll See in Real PVP (Correct)
```
[TRIGGER] ✓ Triggering Cinderwake
[AFFINITY] PlayerA Offense(FIRE): 10.0 | PlayerB Defense(FIRE): 0.0
[AFFINITY] PVP: PlayerA vs PlayerB | Element: FIRE | Modifier: 1.190
[FEEDBACK] Called: PlayerA vs PlayerB | Element: FIRE | Modifier: 1.190
In-game messages appear for both players
```

---

## Test 1: Pure Fire Offensive vs Pure Fire Defensive (Counter-Mechanic)

### Purpose
Test that the counter-mechanic reduces damage when offensive element matches defensive element.

### Player A Setup (Fire Attacker)
```
Weapon: Diamond Sword
/enchant add cinderwake epic 3

Expected Affinity:
- Fire Offense: 10
- Fire Defense: 0
- Fire Utility: 0
```

### Player B Setup (Fire Tank)
```
Armor: Diamond Chestplate + Leggings
/enchant add terraheart rare 2    (on chestplate - for regen, gives Earth defense)
Note: Need to create a Fire defensive enchantment or use hybrid

Alternative: Use Earth defense for now:
/enchant add terraheart epic 3

Expected Affinity:
- Fire Offense: 0
- Fire Defense: 0 (or 10 if Fire defensive enchantment exists)
- Earth Defense: 10
```

### Expected Results
- **If Fire Defense**: 
  - Attacker sees: `§c✗ It's ineffective... §7(🔥 §cFire§7)`
  - Defender sees: `§a✓ Strong resistance to 🔥 §cFire!`
  - Damage modifier: ~0.80x (counter-mechanic -20%)
  
- **Without matching defense**:
  - Attacker sees: `§a⚡ Super Effective! §7(🔥 §cFire§7)`
  - Defender sees: `§c⚠ You're vulnerable to 🔥 §cFire!`
  - Damage modifier: ~1.19x

---

## Test 2: High Offense vs No Defense (Maximum Advantage)

### Purpose
Test super effective damage when defender has no defensive affinity.

### Player A Setup (Multi-Element Attacker)
```
Weapon: Diamond Sword
/enchant add cinderwake epic 3      (Fire offense +10)
/enchant add voltbrand epic 3       (Lightning offense +10)
/enchant add dawnstrike rare 2      (Light offense +10)

Expected Affinity:
- Fire Offense: 10
- Lightning Offense: 10
- Light Offense: 10
- All Defense: 0
```

### Player B Setup (No Enchantments)
```
Armor: Plain diamond armor (no enchantments)

Expected Affinity:
- All elements: 0 (offensive, defensive, utility)
```

### Expected Results
- Attacker sees: `§a⚡ Super Effective!` for ALL elements
- Defender sees: `§c⚠ You're vulnerable to [Element]!` for each attack
- Damage modifiers: ~1.19x (maximum advantage)
- StatsGUI shows: Attacker has 30 total offensive affinity spread across 3 elements

---

## Test 3: Balanced Build vs Balanced Build (Neutral Combat)

### Purpose
Test neutral damage when both players have moderate offensive/defensive affinity.

### Player A Setup (Balanced Fighter)
```
Weapon: Diamond Sword
/enchant add deepcurrent rare 2     (Water offense +10)

Armor: Diamond Chestplate
/enchant add mistveil rare 2        (Water defense +10)

Expected Affinity:
- Water Offense: 10
- Water Defense: 10
```

### Player B Setup (Balanced Fighter)
```
Weapon: Diamond Sword
/enchant add burdenedstone rare 2   (Earth offense +10)

Armor: Diamond Chestplate
/enchant add terraheart rare 2      (Earth defense +10)

Expected Affinity:
- Earth Offense: 10
- Earth Defense: 10
```

### Expected Results
- NO MESSAGES (different elements, neutral matchup)
- Player A's Water attacks vs Player B's Earth defense = no advantage
- Player B's Earth attacks vs Player A's Water defense = no advantage
- Damage modifiers: ~1.0x (neutral)

---

## Test 4: Hybrid Enchantment Testing

### Purpose
Test that hybrid enchantments split affinity correctly between elements.

### Player A Setup (Hybrid Attacker)
```
Weapon: Diamond Sword
/enchant add stormfire epic 3       (Fire/Lightning hybrid offense)

Expected Affinity:
- Fire Offense: 6 (60% of 10)
- Lightning Offense: 4 (40% of 10)
- All Defense: 0
```

### Player B Setup (Single Element Defense)
```
Armor: Diamond Chestplate + Leggings
/enchant add radiantgrace epic 3    (Light defense +10)

Expected Affinity:
- Light Defense: 10
- All Offense: 0
```

### Expected Results
- Stormfire triggers Fire AND Lightning damage
- Both elements show as "Super Effective" (no matching defense)
- Attacker sees: `§a⚡ Super Effective! §7(🔥 §cFire§7)`
- Attacker sees: `§a⚡ Super Effective! §7(⚡ §eLightning§7)`
- StatsGUI shows: Attacker has Fire (6) and Lightning (4) offensive affinity

---

## Test 5: Utility Affinity (No Combat Impact)

### Purpose
Verify that utility affinity does NOT affect damage calculations.

### Player A Setup (Utility Build)
```
Armor: Diamond Boots + Helmet
/enchant add galestep epic 3        (Air utility +10)
/enchant add ashenveil rare 2       (Fire utility +10)

Weapon: Diamond Sword (no enchantments)

Expected Affinity:
- Air Utility: 10
- Fire Utility: 10
- All Offense: 0
- All Defense: 0
```

### Player B Setup (Offensive Build)
```
Weapon: Diamond Sword
/enchant add cinderwake epic 3      (Fire offense +10)

Expected Affinity:
- Fire Offense: 10
```

### Expected Results
- Player B attacks Player A with Fire damage
- Player A's Fire UTILITY does NOT counter the attack
- Player B sees: `§a⚡ Super Effective! §7(🔥 §cFire§7)` (no defense to counter)
- Utility affinity appears in StatsGUI but doesn't affect combat
- Damage modifier: ~1.19x (Player A has no defensive affinity)

---

## Test 6: Multiple Offensive Elements vs Single Defense

### Purpose
Test counter-mechanic selectivity - only matching element triggers counter.

### Player A Setup (Tri-Element Attacker)
```
Weapon: Diamond Sword + Axe (2 weapons)
Sword: /enchant add cinderwake epic 3      (Fire offense)
Axe: /enchant add voltbrand epic 3         (Lightning offense)

Off-hand: Shield
Shield: /enchant add dawnstrike rare 2     (Light offense)

Expected Affinity:
- Fire Offense: 10
- Lightning Offense: 10
- Light Offense: 10
```

### Player B Setup (Single Element Tank)
```
Armor: Full diamond armor (4 pieces)
Chestplate: /enchant add radiantgrace epic 3    (Light defense +10)

Expected Affinity:
- Light Defense: 10
- All other Defense: 0
```

### Expected Results
When Player A attacks with:
- **Fire (Cinderwake)**: `§a⚡ Super Effective!` - No counter
- **Lightning (Voltbrand)**: `§a⚡ Super Effective!` - No counter
- **Light (Dawnstrike)**: `§c✗ It's ineffective...` - COUNTER-MECHANIC!

Player B sees:
- Vulnerable to Fire and Lightning
- Strong resistance to Light

---

## Test 7: Threshold Testing (Counter-Mechanic Activation)

### Purpose
Test that counter-mechanic only activates at defense >= 20 threshold.

### Round 1: Below Threshold
**Player A**: Fire Offense = 50 (5x Epic Cinderwake on different items)
**Player B**: Fire Defense = 10 (1x Rare Fire defensive enchantment)

Expected: NO counter-mechanic (defense below 20 threshold)
- Messages: Super Effective / Vulnerable
- Modifier: ~1.20x

### Round 2: At Threshold
**Player A**: Fire Offense = 50
**Player B**: Fire Defense = 20 (2x Epic Fire defensive enchantments)

Expected: Counter-mechanic ACTIVATES
- Messages: Ineffective / Strong resistance
- Modifier: ~0.80x

### Round 3: Above Threshold
**Player A**: Fire Offense = 50
**Player B**: Fire Defense = 40 (4x Epic Fire defensive enchantments)

Expected: Strong counter-mechanic
- Messages: Ineffective / Strong resistance
- Modifier: ~0.75x

---

## Test 8: Real-World PVP Builds

### The Fire Specialist
```
Weapon: Netherite Sword
- Cinderwake (Epic III) - Fire offense

Off-hand: Shield
- Stormfire (Epic III) - Fire/Lightning hybrid offense

Chestplate:
- Embershade (Rare II) - Fire/Shadow hybrid offense

Expected Affinity:
- Fire Offense: 26 (10 + 6 + 6)
- Lightning Offense: 4
- Shadow Offense: 4
```

### The Water Tank
```
Helmet: Diamond
- Mistveil (Epic III) - Water defense

Chestplate: Diamond
- Pure Reflection (Epic III) - Water/Light hybrid defense

Leggings: Diamond
- Deepcurrent (Rare II) - Water offense (for counter-attacking)

Expected Affinity:
- Water Defense: 16 (10 + 6)
- Light Defense: 4
- Water Offense: 10
```

### Expected Result
- Fire Specialist attacks Water Tank: No counter (different elements)
- Water Tank has high Water defense but Fire attacks bypass it
- Fire Specialist should see: `§a⚡ Super Effective!`

---

## Test 9: StatsGUI Verification

### Purpose
Verify that the StatsGUI displays all three affinity categories correctly.

### Player Setup (Mixed Build)
```
Weapon: Diamond Sword
- Cinderwake (Epic III) - Fire OFFENSE

Chestplate: Diamond
- Mistveil (Epic III) - Water DEFENSE

Boots: Diamond
- GaleStep (Epic III) - Air UTILITY

Expected StatsGUI Display:

§c⚔ §eOffensive Affinity:
 • 🔥 §cFire: §e10
 • 💧 §bWater: §80
 • ... (all other elements at 0)

§9🛡 §eDefensive Affinity:
 • 🔥 §cFire: §80
 • 💧 §bWater: §e10
 • ... (all other elements at 0)

§a✦ §eUtility Affinity:
 • 💨 §fAir: §e10
 • ... (all other elements at 0)
```

### Verification Steps
1. Open StatsGUI (`/stats` or equivalent)
2. Scroll to affinity section
3. Verify THREE separate sections with headers
4. Verify Fire shows ONLY in Offensive (10)
5. Verify Water shows ONLY in Defensive (10)
6. Verify Air shows ONLY in Utility (10)
7. Verify color coding: 0=dark gray, 10=gray, 20+=white/yellow/gold

---

## Test 10: Dynamic Equipment Updates

### Purpose
Test that affinity values update in real-time when equipment changes.

### Steps
1. **Start with no enchantments**
   - Open StatsGUI
   - Verify all affinities = 0

2. **Equip Fire offensive enchantment**
   - `/enchant add cinderwake epic 3` on sword
   - Wait 0.25 seconds (5 ticks = scan interval)
   - Check StatsGUI
   - Verify Fire Offense increased to 10

3. **Add second Fire enchantment**
   - `/enchant add stormfire epic 3` on axe
   - Wait 0.25 seconds
   - Check StatsGUI
   - Verify Fire Offense increased to 16 (10 + 6)

4. **Switch to defensive enchantment**
   - Remove sword (unequip)
   - Wait 0.25 seconds
   - Check StatsGUI
   - Verify Fire Offense decreased to 6
   - Equip chestplate with Fire defensive enchantment
   - Verify Fire Defense increased to 10

5. **Remove all enchantments**
   - Unequip all items
   - Wait 0.25 seconds
   - Verify all affinities return to 0

---

## Quick Command Reference

### Apply Enchantments
```
/enchant add <enchantment> <quality> <level>

Qualities: common, uncommon, rare, epic, legendary
Levels: 1, 2, 3
```

### Offensive Enchantments
```
/enchant add cinderwake epic 3        # Fire offense
/enchant add deepcurrent epic 3       # Water offense
/enchant add burdenedstone epic 3     # Earth offense
/enchant add voltbrand epic 3         # Lightning offense
/enchant add hollowedge epic 3        # Shadow offense
/enchant add dawnstrike epic 3        # Light offense
```

### Defensive Enchantments
```
/enchant add mistveil epic 3          # Water defense
/enchant add terraheart epic 3        # Earth defense
/enchant add whispers epic 3          # Air defense
/enchant add radiantgrace epic 3      # Light defense
```

### Utility Enchantments
```
/enchant add ashenveil epic 3         # Fire utility
/enchant add galestep epic 3          # Air utility
/enchant add veilborn epic 3          # Shadow utility
```

### Hybrid Enchantments
```
/enchant add stormfire epic 3         # Fire/Lightning offense
/enchant add mistbornetempest epic 3  # Water/Air utility
/enchant add decayroot epic 3         # Earth/Shadow offense
/enchant add celestialsurge epic 3    # Light/Lightning offense
/enchant add embershade epic 3        # Fire/Shadow offense
/enchant add purereflection epic 3    # Water/Light defense
```

### View Stats
```
/stats                    # Open StatsGUI
/enchant info            # Check enchantment details
```

---

## Expected Debug Output

With debug mode enabled (`/debug enchanting on`), you should see console messages like:

```
[ENCHANTING] Scanned affinity for Player1 - Total: 30
[ENCHANTING] Fire - OFF: 16, DEF: 0, UTIL: 0
[ENCHANTING] Lightning - OFF: 4, DEF: 0, UTIL: 0
[ENCHANTING] Shadow - OFF: 4, DEF: 0, UTIL: 0

[ENCHANTING] PVP Affinity Modifier: Player1 vs Player2, Element: FIRE, Modifier: 0.89x, Feedback: §c✗ It's ineffective...
```

---

## Success Criteria Checklist

### Basic Functionality
- [ ] Affinities display in three separate sections in StatsGUI
- [ ] Offensive enchantments increase offensive affinity only
- [ ] Defensive enchantments increase defensive affinity only
- [ ] Utility enchantments increase utility affinity only
- [ ] Hybrid enchantments split affinity 60/40 between elements

### Counter-Mechanic
- [ ] Matching offense vs defense shows "Ineffective" message
- [ ] Different offense vs defense shows "Effective" message
- [ ] Counter-mechanic only triggers when defense >= 20
- [ ] Damage modifier correctly reduces damage (~20%)

### Effectiveness Messages
- [ ] Super Effective (>1.15) shows green message to attacker
- [ ] Ineffective (<0.85) shows red message to attacker
- [ ] Neutral (0.95-1.05) shows NO message
- [ ] Defender receives opposite perspective messages
- [ ] Element icon and name display correctly

### Real-Time Updates
- [ ] Affinity updates within 0.25 seconds of equipment change
- [ ] Equipping item increases affinity
- [ ] Unequipping item decreases affinity
- [ ] Swapping items updates correctly

### Edge Cases
- [ ] Multiple same-element enchantments stack correctly
- [ ] Utility affinity doesn't affect damage calculations
- [ ] Hybrid enchantments contribute to both elements
- [ ] Empty equipment slots don't cause errors
- [ ] Player without profile doesn't crash

---

## Troubleshooting

### Issue: Affinity Not Updating
**Solution**: Wait 5 ticks (0.25 seconds) for next scan cycle

### Issue: No Effectiveness Messages
**Check**: Ensure modifier is outside 0.95-1.05 range (neutral)

### Issue: Wrong Affinity Category
**Check**: Enchantment's `getPrimaryAffinityCategory()` method

### Issue: Counter-Mechanic Not Triggering
**Check**: Defender's defensive affinity must be >= 20 in matching element

### Issue: Hybrid Not Splitting
**Check**: Enchantment is registered as hybrid with HybridElement

---

## Recommended Testing Order

1. **Test 9** - Verify StatsGUI displays correctly
2. **Test 10** - Verify real-time updates work
3. **Test 2** - Test basic effective damage
4. **Test 1** - Test counter-mechanic
5. **Test 4** - Test hybrid enchantments
6. **Test 5** - Verify utility doesn't affect combat
7. **Test 6** - Test selective counter-mechanic
8. **Test 7** - Test threshold mechanics
9. **Test 8** - Test realistic PVP scenarios
10. **Test 3** - Test balanced builds

---

**Testing Guide Version**: 1.0
**Last Updated**: October 19, 2025
**Status**: Ready for In-Game Testing

