# Anti-Synergy Analysis - 20 Enchantments

## Complete Enchantment List

### Fire Element (2)
1. **Cinderwake** - ON_HIT area burn damage
2. **Ashen Veil** - ON_KILL invisibility

### Water Element (2)
3. **Deepcurrent** - ON_HIT chain pull enemies
4. **Mistveil** - ON_DAMAGED dodge/evasion

### Earth Element (2)
5. **Burdened Stone** - ON_HIT stacking slowness
6. **Terraheart** - PASSIVE low-health regeneration

### Air Element (2)
7. **GaleStep** - PASSIVE sneak speed boost
8. **Whispers** - ON_DAMAGED knockback

### Lightning Element (2)
9. **Voltbrand** - ON_HIT chain lightning
10. **Arc Nexus** - ON_HIT stacking attack speed

### Shadow Element (2)
11. **Hollow Edge** - ON_KILL vampiric life/mana steal
12. **Veilborn** - PASSIVE double-sneak invisibility

### Light Element (2)
13. **Dawnstrike** - ON_HIT blindness debuff
14. **Radiant Grace** - ON_DAMAGED AoE ally healing

### Hybrid Enchantments (6)
15. **Stormfire** (Fire/Lightning) - ON_HIT explosive flames
16. **MistborneTempest** (Water/Air) - Utility dash
17. **Decayroot** (Earth/Shadow) - ON_HIT rooting
18. **CelestialSurge** (Light/Lightning) - ON_HIT divine lightning
19. **Embershade** (Fire/Shadow) - ON_HIT dark flames
20. **PureReflection** (Water/Light) - Utility barrier

---

## Anti-Synergy Groups

### Group 1: ON_HIT Burn/Fire Damage (Redundant Fire Damage)
**Enchantments:**
- Cinderwake (Fire burn AOE)
- Stormfire (Fire/Lightning explosive flames)
- Embershade (Fire/Shadow dark flames)

**Reason:** All three apply fire/burn damage on hit. Having multiple fire damage sources is redundant and would stack burn effects excessively.

**Restriction:** Only ONE can be on an item at a time.

---

### Group 2: ON_HIT Chain/Area Effects (Overlapping AOE)
**Enchantments:**
- Voltbrand (Lightning chains to nearby enemies)
- Deepcurrent (Pulls nearby enemies in chain)
- Stormfire (Explosive AOE damage)
- CelestialSurge (Lightning strike AOE)

**Reason:** All have area-of-effect or chaining mechanics that hit multiple enemies. Combining them would be overwhelming and chaotic.

**Restriction:** Only ONE can be on an item at a time.

---

### Group 3: ON_HIT Crowd Control (CC Stacking)
**Enchantments:**
- Burdened Stone (Stacking slowness)
- Decayroot (Rooting/immobilize)
- Dawnstrike (Blindness)

**Reason:** All apply debilitating crowd control effects. Multiple CC effects would be too oppressive in PvP.

**Restriction:** Only ONE can be on an item at a time.

---

### Group 4: Invisibility/Stealth (Conflicting Stealth Mechanics)
**Enchantments:**
- Ashen Veil (ON_KILL invisibility)
- Veilborn (Double-sneak invisibility)

**Reason:** Both grant invisibility but with different triggers. Having both would create confusing interactions.

**Restriction:** Only ONE can be on an item at a time.

---

### Group 5: ON_DAMAGED Defensive Responses (Anti-Stacking Defense)
**Enchantments:**
- Mistveil (Dodge/evasion)
- Whispers (Knockback attacker)
- Radiant Grace (Heal allies)

**Reason:** All trigger when taking damage and provide defensive responses. Stacking would make the player too tanky.

**Restriction:** Only ONE can be on an item at a time.

---

### Group 6: Attack Speed Manipulation (Conflicting Speed Buffs)
**Enchantments:**
- Arc Nexus (Stacking attack speed on hit)

**Reason:** Currently only one, but should be flagged to prevent future attack speed enchantments from stacking.

**Note:** No conflicts yet, but prepared for future enchantments.

---

### Group 7: Passive Speed/Movement (Movement Speed Conflicts)
**Enchantments:**
- GaleStep (Sneak speed boost)
- MistborneTempest (Dash/velocity)

**Reason:** Both affect movement mechanics but in different ways. GaleStep is passive, MistborneTempest is active. Could conflict.

**Restriction:** Only ONE can be on an item at a time.

---

### Group 8: Utility Barriers/Shields (Defensive Overlap)
**Enchantments:**
- PureReflection (Damage absorption barrier)
- Terraheart (Low-health regeneration)

**Reason:** Both provide sustain/survival mechanics. PureReflection absorbs damage, Terraheart regens health. Too much sustain if combined.

**Restriction:** Only ONE can be on an item at a time.

---

### Group 9: ON_KILL Effects (Kill-Based Synergy Prevention)
**Enchantments:**
- Hollow Edge (Life/mana steal)
- Ashen Veil (Invisibility)

**Reason:** Both trigger on kill. While not directly conflicting, having both makes kills too rewarding.

**Restriction:** Only ONE can be on an item at a time.

---

## Summary of Anti-Synergy Groups

| Group | Name | Enchantments | Count |
|-------|------|--------------|-------|
| 1 | Fire Damage | Cinderwake, Stormfire, Embershade | 3 |
| 2 | AOE/Chain | Voltbrand, Deepcurrent, Stormfire, CelestialSurge | 4 |
| 3 | Crowd Control | Burdened Stone, Decayroot, Dawnstrike | 3 |
| 4 | Invisibility | Ashen Veil, Veilborn | 2 |
| 5 | Defensive Response | Mistveil, Whispers, Radiant Grace | 3 |
| 6 | Attack Speed | Arc Nexus | 1 |
| 7 | Movement | GaleStep, MistborneTempest | 2 |
| 8 | Sustain/Barriers | PureReflection, Terraheart | 2 |
| 9 | On-Kill | Hollow Edge, Ashen Veil | 2 |

**Note:** Some enchantments appear in multiple groups (e.g., Stormfire in both Fire Damage and AOE/Chain, Ashen Veil in both Invisibility and On-Kill).

---

## Implementation Strategy

1. **Add `getAntiSynergyGroups()` method to CustomEnchantment base class**
   - Returns array of group IDs this enchantment belongs to
   - Default returns empty array (no conflicts)

2. **Override in each enchantment that has conflicts**
   - Return appropriate group IDs

3. **Update EnchantmentData.addEnchantmentToItem()**
   - Check existing enchantments on item
   - If new enchantment shares anti-synergy group with existing one
   - Remove old enchantment, add new one (replacement)

4. **Update /enchant info command**
   - Show "Conflicts with: [list of enchantment names]"
   - Clear indication of what will be replaced

---

## Final Enchantment Groupings

**No Conflicts (Unique):**
- (None - all have at least one anti-synergy)

**Minimal Conflicts (1 group):**
- Arc Nexus (Group 6 only)
- Hollow Edge (Group 9 only)

**Moderate Conflicts (2 groups):**
- Ashen Veil (Groups 4, 9)
- Stormfire (Groups 1, 2)
- GaleStep (Group 7 only)
- MistborneTempest (Group 7 only)
- Terraheart (Group 8 only)
- PureReflection (Group 8 only)
- Veilborn (Group 4 only)

**High Conflicts (multiple groups):**
- Most offensive enchantments belong to 1-2 groups

This ensures strategic choices and prevents overpowered item combinations!
