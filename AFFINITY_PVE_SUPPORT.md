# Affinity System - PVE Support Enabled

## What Changed

The categorized affinity system now works for **BOTH PVP and PVE** combat!

### Previous Behavior (Incorrect)
- ❌ Affinity modifiers only applied in Player vs Player combat
- ❌ Effectiveness messages only shown in PVP
- ❌ NPCs and mobs had no interaction with affinity system

### New Behavior (Correct)
- ✅ Affinity modifiers apply to **ALL targets** (Players, NPCs, Mobs)
- ✅ Effectiveness messages shown when attacking **ANY entity**
- ✅ NPCs/Mobs treated as having 0 defensive affinity (pure offense scaling)

---

## How PVE Works

### Attacker's Offensive Affinity Scales Damage

When you have **offensive elemental affinity**, your damage is boosted against NPCs and mobs:

```
Offensive Affinity: 0   → Modifier: 1.00x (no bonus)
Offensive Affinity: 10  → Modifier: 1.04x (+4% damage)
Offensive Affinity: 20  → Modifier: 1.08x (+8% damage)
Offensive Affinity: 50  → Modifier: 1.19x (+19% damage)
Offensive Affinity: 100 → Modifier: 1.24x (+24% damage - near cap)
```

### NPCs Have 0 Defensive Affinity

- NPCs and mobs **do not have player profiles** or equipment
- They are treated as having **0 defensive affinity** in all elements
- **No counter-mechanic** applies (counter requires defender to have 20+ defense)

### Effectiveness Messages

Attackers will see messages based on their offensive affinity:

**High Offense (50+ affinity)**:
- Message: `§a⚡ Super Effective! §7(🔥 §cFire§7)`
- Modifier: ~1.19x damage

**Medium Offense (20-49 affinity)**:
- Message: `§a✓ It's effective! §7(🔥 §cFire§7)`
- Modifier: ~1.08x damage

**Low Offense (10-19 affinity)**:
- No message (modifier 1.04-1.07x is in neutral range 0.95-1.05)

**No Offense (0 affinity)**:
- No message (modifier 1.0x is neutral)

---

## Example: Fire Mage vs Zombie

### Player Setup
```
Weapon: Fire Staff
Enchantments: Cinderwake (Epic III) - Fire offense +10

Fire Offensive Affinity: 10
Fire Defensive Affinity: 0
```

### Combat Against Zombie
```
1. Player casts fire spell at zombie
2. Cinderwake triggers (ON_HIT)
3. System calculates affinity modifier:
   - Attacker Fire Offense: 10
   - Defender (Zombie) Fire Defense: 0
   - Relative Affinity: 10 - 0 = 10
   - Modifier: 1.04x
4. Bonus fire damage: 8.0 × 1.04 = 8.32
5. Message: (None - within neutral range)
```

---

## Example: Fire Specialist vs Zombie

### Player Setup
```
Weapon: Fire Sword with Cinderwake (Epic III) - Fire offense +10
Armor: Fire Chestplate with Embershade (Epic III) - Fire offense +6
Off-hand: Shield with Stormfire (Epic III) - Fire offense +6

Total Fire Offensive Affinity: 22
```

### Combat Against Zombie
```
1. Player attacks zombie with sword
2. Cinderwake + Embershade + Stormfire all contribute affinity
3. System calculates affinity modifier:
   - Attacker Fire Offense: 22
   - Defender (Zombie) Fire Defense: 0
   - Relative Affinity: 22 - 0 = 22
   - Modifier: 1.09x
4. Bonus fire damage: 8.0 × 1.09 = 8.72
5. Message: §a✓ It's effective! §7(🔥 §cFire§7)
```

---

## PVP vs PVE Comparison

### PVP (Player vs Player)

**Features**:
- ✅ Offensive vs Defensive affinity comparison
- ✅ Counter-mechanic (Fire offense vs Fire defense = -20%)
- ✅ Both players see messages (attacker + defender perspectives)
- ✅ Full affinity scaling (-25% to +25%)

**Example**:
```
Attacker: Fire Offense 50
Defender: Fire Defense 30
Modifier: 1.0 + 0.09 - 0.20 = 0.89x (counter-mechanic!)
Attacker sees: "✗ It's ineffective..."
Defender sees: "✓ Strong resistance to Fire!"
```

### PVE (Player vs NPC/Mob)

**Features**:
- ✅ Only offensive affinity matters (no defensive comparison)
- ❌ No counter-mechanic (NPCs have 0 defense)
- ✅ Only attacker sees messages
- ✅ Offense-only scaling (1.0x to +24%)

**Example**:
```
Attacker: Fire Offense 50
Defender (NPC): Fire Defense 0
Modifier: 1.0 + 0.19 = 1.19x (pure offense bonus)
Attacker sees: "⚡ Super Effective!"
NPC sees: (nothing - not a player)
```

---

## Console Debug Logs

### PVP Combat
```
[TRIGGER] ✓ Triggering Cinderwake (Quality: EPIC, Level: III)
[AFFINITY] PlayerA Offense(FIRE): 50.0 | PlayerB Defense(FIRE): 30.0
[AFFINITY] PVP: PlayerA vs PlayerB | Element: FIRE | Modifier: 0.890 | Bonus Dmg: 7.12
[FEEDBACK] Called: PlayerA vs PlayerB | Element: FIRE | Modifier: 0.890 | In Range: false
```

### PVE Combat
```
[TRIGGER] ✓ Triggering Cinderwake (Quality: EPIC, Level: III)
[AFFINITY] PlayerA Offense(FIRE): 50.0 | Zombie Defense(FIRE): 0.0 (NPC/Mob)
[AFFINITY] PVE: PlayerA vs Zombie | Element: FIRE | Modifier: 1.190 | Bonus Dmg: 9.52
[FEEDBACK] Called: PlayerA vs Zombie | Element: FIRE | Modifier: 1.190 | In Range: false
```

Notice the key differences:
- `(NPC/Mob)` marker in affinity log
- `PVE:` vs `PVP:` prefix
- Higher modifier in PVE (no counter-mechanic)

---

## Benefits of PVE Support

### 1. Build Specialization Matters in PVE
Players who invest in elemental affinity get rewarded with better PVE damage.

### 2. Clearer Damage Feedback
Players see when their build is effective, helping them understand their power.

### 3. Consistent System
Same affinity mechanics work everywhere - PVP and PVE use the same code.

### 4. Encourages Equipment Diversity
Fire builds do more fire damage, ice builds do more ice damage, etc.

---

## Technical Implementation

### AffinityModifier.java

Added overloaded method that accepts Entity:

```java
public static double calculateDamageModifier(Player attacker, Entity defender, ElementType element) {
    // If defender is a player, use full PVP logic
    if (defender instanceof Player) {
        return calculateDamageModifier(attacker, (Player) defender, element);
    }
    
    // For NPCs/Mobs: Use only attacker's offense
    double attackerOffense = attackerAffinity.getOffensive(element);
    double defenderDefense = 0.0; // NPCs have no defensive affinity
    
    // Calculate modifier without counter-mechanic
    return 1.0 + affinityModifier;
}
```

### EnchantmentDamageUtil.java

Updated to work with any Entity:

```java
// Apply affinity modifier for ALL targets (PVP and PVE)
double affinityModifier = AffinityModifier.calculateDamageModifier(
    damager, event.getEntity(), element);

// Send effectiveness feedback (works for PVP and PVE)
sendEffectivenessFeedback(damager, event.getEntity(), element, affinityModifier);
```

### Feedback Method

Updated to handle non-player targets:

```java
private static void sendEffectivenessFeedback(Player attacker, Entity defender, ...) {
    // Attacker always gets messages
    attacker.sendMessage("⚡ Super Effective!");
    
    // Defender only gets messages if they're a player
    if (defender instanceof Player) {
        ((Player) defender).sendMessage("⚠ You're vulnerable to Fire!");
    }
}
```

---

## Testing

### Test 1: Player vs NPC with Fire Offense
```
Player Setup:
- Cinderwake (Epic III) - Fire offense +10

Attack NPC:
Expected Console:
  [AFFINITY] PlayerA Offense(FIRE): 10.0 | NPC Defense(FIRE): 0.0 (NPC/Mob)
  [AFFINITY] PVE: PlayerA vs NPC | Element: FIRE | Modifier: 1.040

Expected Message:
  (None - modifier 1.04x is in neutral range 0.95-1.05)
```

### Test 2: Player vs NPC with High Fire Offense
```
Player Setup:
- Cinderwake + Embershade + Stormfire = Fire offense +22

Attack NPC:
Expected Console:
  [AFFINITY] PlayerA Offense(FIRE): 22.0 | NPC Defense(FIRE): 0.0 (NPC/Mob)
  [AFFINITY] PVE: PlayerA vs NPC | Element: FIRE | Modifier: 1.090

Expected Message:
  §a✓ It's effective! §7(🔥 §cFire§7)
```

### Test 3: Player vs Player with Counter-Mechanic
```
Player A Setup:
- Fire offense: 50

Player B Setup:
- Fire defense: 30

Attack Player B:
Expected Console:
  [AFFINITY] PlayerA Offense(FIRE): 50.0 | PlayerB Defense(FIRE): 30.0
  [AFFINITY] PVP: PlayerA vs PlayerB | Element: FIRE | Modifier: 0.890

Expected Messages:
  PlayerA: §c✗ It's ineffective... §7(🔥 §cFire§7)
  PlayerB: §a✓ Strong resistance to 🔥 §cFire!
```

---

## Summary

| Feature | PVP | PVE |
|---------|-----|-----|
| Affinity Modifiers | ✅ Yes | ✅ Yes |
| Counter-Mechanic | ✅ Yes | ❌ No |
| Attacker Messages | ✅ Yes | ✅ Yes |
| Defender Messages | ✅ Yes | ❌ No |
| Scaling Range | -25% to +25% | 0% to +24% |
| Defender Defense | Variable | Always 0 |

---

**Build Version**: mmo-0.0.1 (PVE support enabled)
**Date**: October 19, 2025
**Status**: ✅ Deployed and ready for testing

**Test Now**: Restart server and attack NPCs/mobs with enchanted weapons!
