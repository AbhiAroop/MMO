# Anti-Synergy Quick Reference

## 9 Conflict Groups

| Group | Name | Enchantments | Count |
|-------|------|--------------|-------|
| **1** | Fire Damage | Cinderwake, Stormfire, Embershade | 3 |
| **2** | AOE/Chain | Cinderwake, Voltbrand, Deepcurrent, Stormfire, CelestialSurge | 5 |
| **3** | Crowd Control | BurdenedStone, Decayroot, Dawnstrike | 3 |
| **4** | Invisibility | AshenVeil, Veilborn | 2 |
| **5** | Defensive Response | Mistveil, Whispers, RadiantGrace | 3 |
| **6** | Attack Speed | ArcNexus | 1 |
| **7** | Movement | GaleStep, MistborneTempest | 2 |
| **8** | Sustain/Barriers | Terraheart, PureReflection | 2 |
| **9** | On-Kill Effects | AshenVeil, HollowEdge | 2 |

---

## Conflict Matrix (Alphabetical)

| Enchantment | Groups | Conflicts | Count |
|------------|--------|-----------|-------|
| **ArcNexus** | 6 | *None* | 0 |
| **AshenVeil** | 4, 9 | Veilborn, HollowEdge | 2 |
| **BurdenedStone** | 3 | Decayroot, Dawnstrike | 2 |
| **CelestialSurge** ⚡ | 2 | Voltbrand, Deepcurrent, Cinderwake, Stormfire | 4 |
| **Cinderwake** | 1, 2 | Stormfire, Embershade, Voltbrand, Deepcurrent, CelestialSurge | 5 |
| **Dawnstrike** | 3 | BurdenedStone, Decayroot | 2 |
| **Decayroot** ⚡ | 3 | BurdenedStone, Dawnstrike | 2 |
| **Deepcurrent** | 2 | Voltbrand, Cinderwake, Stormfire, CelestialSurge | 4 |
| **Embershade** ⚡ | 1 | Cinderwake, Stormfire | 2 |
| **GaleStep** | 7 | MistborneTempest | 1 |
| **HollowEdge** | 9 | AshenVeil | 1 |
| **MistborneTempest** ⚡ | 7 | GaleStep | 1 |
| **Mistveil** | 5 | Whispers, RadiantGrace | 2 |
| **PureReflection** ⚡ | 8 | Terraheart | 1 |
| **RadiantGrace** | 5 | Mistveil, Whispers | 2 |
| **Stormfire** ⚡ | 1, 2 | Cinderwake, Embershade, Voltbrand, Deepcurrent, CelestialSurge | 5 |
| **Terraheart** | 8 | PureReflection | 1 |
| **Veilborn** | 4 | AshenVeil | 1 |
| **Voltbrand** | 2 | Deepcurrent, Cinderwake, Stormfire, CelestialSurge | 4 |
| **Whispers** | 5 | Mistveil, RadiantGrace | 2 |

⚡ = Hybrid enchantment

---

## By Element

### Fire (2 + 2 hybrid)
- **Cinderwake** → Groups 1, 2 | Conflicts: 5
- **AshenVeil** → Groups 4, 9 | Conflicts: 2
- **Stormfire** (Lightning+Fire) → Groups 1, 2 | Conflicts: 5
- **Embershade** (Fire+Shadow) → Group 1 | Conflicts: 2

### Water (2 + 1 hybrid)
- **Deepcurrent** → Group 2 | Conflicts: 4
- **Mistveil** → Group 5 | Conflicts: 2
- **PureReflection** (Water+Light) → Group 8 | Conflicts: 1

### Earth (2 + 1 hybrid)
- **BurdenedStone** → Group 3 | Conflicts: 2
- **Terraheart** → Group 8 | Conflicts: 1
- **Decayroot** (Shadow+Earth) → Group 3 | Conflicts: 2

### Air (2 + 1 hybrid)
- **GaleStep** → Group 7 | Conflicts: 1
- **Whispers** → Group 5 | Conflicts: 2
- **MistborneTempest** (Air+Water) → Group 7 | Conflicts: 1

### Lightning (2 + 1 hybrid)
- **Voltbrand** → Group 2 | Conflicts: 4
- **ArcNexus** → Group 6 | Conflicts: 0
- **CelestialSurge** (Light+Lightning) → Group 2 | Conflicts: 4

### Shadow (2 + 1 hybrid)
- **HollowEdge** → Group 9 | Conflicts: 1
- **Veilborn** → Group 4 | Conflicts: 1
- **Embershade** (Fire+Shadow) → Group 1 | Conflicts: 2

### Light (2 + 1 hybrid)
- **Dawnstrike** → Group 3 | Conflicts: 2
- **RadiantGrace** → Group 5 | Conflicts: 2
- **CelestialSurge** (Light+Lightning) → Group 2 | Conflicts: 4

---

## Multi-Group Enchantments (3 total)

These enchantments belong to **multiple groups**:

1. **Cinderwake** → Groups 1 + 2
   - Fire Damage + AOE/Chain
   - Conflicts with: Stormfire, Embershade, Voltbrand, Deepcurrent, CelestialSurge

2. **Stormfire** → Groups 1 + 2
   - Fire Damage + AOE/Chain
   - Conflicts with: Cinderwake, Embershade, Voltbrand, Deepcurrent, CelestialSurge

3. **AshenVeil** → Groups 4 + 9
   - Invisibility + On-Kill
   - Conflicts with: Veilborn, HollowEdge

---

## Testing Commands

### View Conflicts
```
/enchant info cinderwake
/enchant info stormfire
/enchant info ashenveil
```

### Apply Enchantments (Admin)
```
/enchant give <player> <enchant_id> <quality>
```

### Test Scenarios

**Scenario 1: Simple Conflict**
1. Apply Cinderwake to sword
2. Apply Embershade to same sword
3. ✅ Expected: Cinderwake removed, Embershade remains

**Scenario 2: Multi-Group Conflict**
1. Apply Cinderwake to sword (Groups 1, 2)
2. Apply Voltbrand to same sword (Group 2)
3. ✅ Expected: Cinderwake removed (Group 2 conflict)

**Scenario 3: No Conflict**
1. Apply Cinderwake to sword (Groups 1, 2)
2. Apply Terraheart to armor (Group 8)
3. ✅ Expected: Both remain (different items)

**Scenario 4: Partial Conflict**
1. Apply Cinderwake, Terraheart, GaleStep to item
2. Apply Voltbrand (Group 2)
3. ✅ Expected: Cinderwake removed, Terraheart + GaleStep remain

---

## Code Reference

### Check Enchantment Groups
```java
CustomEnchantment enchant = EnchantmentRegistry.getInstance().getEnchantment("cinderwake");
int[] groups = enchant.getAntiSynergyGroups(); // Returns: {1, 2}
String[] conflicts = enchant.getConflictingEnchantments(); 
// Returns: {"Stormfire", "Embershade", "Voltbrand", "Deepcurrent", "CelestialSurge"}
```

### Add New Enchantment with Conflicts
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{3}; // Crowd Control
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"BurdenedStone", "Decayroot", "Dawnstrike"};
}
```

---

## Statistics

- **Total Enchantments**: 20 (14 elemental + 6 hybrid)
- **Total Groups**: 9
- **Enchantments with 0 conflicts**: 1 (ArcNexus)
- **Enchantments with 1 conflict**: 6 (GaleStep, HollowEdge, MistborneTempest, PureReflection, Terraheart, Veilborn)
- **Enchantments with 2 conflicts**: 8 (AshenVeil, BurdenedStone, Dawnstrike, Decayroot, Embershade, Mistveil, RadiantGrace, Whispers)
- **Enchantments with 4 conflicts**: 3 (CelestialSurge, Deepcurrent, Voltbrand)
- **Enchantments with 5 conflicts**: 2 (Cinderwake, Stormfire)
- **Average conflicts per enchantment**: 2.05
- **Most conflicted groups**: Group 2 (AOE/Chain) with 5 enchantments

---

*Quick Reference for Anti-Synergy System - Version 1.0*
