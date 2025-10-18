# Elemental Affinity PVP Combat System

## Overview
The Affinity PVP Combat System modifies damage and effect durations based on the elemental affinity differences between attacking and defending players. This creates dynamic combat where your equipped enchantments directly influence your offensive and defensive capabilities against other players.

## How It Works

### Step 1: Player Affinity Calculation
Each player has an **Affinity Value** for each of the 8 elements, calculated from all equipped enchanted items:

```
Affinity(Element) = Σ (Enchantment Affinity Value)
```

- **Source**: Affinity comes from enchantments on equipped armor and held items
- **Dynamic**: Updates automatically when you equip/unequip items (scanned every 5 ticks)
- **Persistent**: Affinity is based on current equipment, not historical enchanting

**Example:**
- Wearing armor with 3 Fire enchantments (affinity values: 300, 450, 600) = 1,350 Fire Affinity
- Holding weapon with 2 Lightning enchantments (affinity values: 400, 500) = 900 Lightning Affinity

### Step 2: Relative Affinity in Combat
When Player A attacks Player B with an elemental attack:

```
Relative Affinity = Attacker's Affinity(Element) - Defender's Affinity(Element)
```

- **Positive Value**: Attacker has elemental advantage
- **Negative Value**: Defender has elemental resistance  
- **Zero**: Neutral matchup

**Example:**
- Attacker has 1,500 Fire Affinity
- Defender has 800 Fire Affinity
- Relative Affinity = 1,500 - 800 = +700 (Attacker Advantage)

### Step 3: Modifier Calculation
The relative affinity is converted to a damage/effect modifier using a **soft-cap function**:

```java
Modifier = Base Effect × tanh(Relative Affinity / Scaling Constant)
```

**Why tanh (hyperbolic tangent)?**
- Soft-caps extreme values between -1 and +1
- Prevents overpowered damage swings
- Provides smooth scaling

**Scaling Constant (K = 50):**
- Controls sensitivity to affinity differences
- ~50 affinity difference = ~65% of maximum modifier
- ~100 affinity difference = ~76% of maximum modifier
- ~200 affinity difference = ~96% of maximum modifier

### Step 4: Combat Effects

#### Damage Modifiers
```java
Final Damage = Base Damage × (1.0 + Modifier)
```

**Maximum Damage Modifier: ±25%**
- Strong Advantage (>15% bonus): §a⚡ Strong Elemental Advantage!
- Advantage (>5% bonus): §a✦ Elemental Advantage
- Neutral (±5%): §7⚖ Neutral
- Disadvantage (<-5%): §c✦ Elemental Disadvantage
- Strong Disadvantage (<-15%): §c⚡ Strong Elemental Disadvantage!

**Example Scenarios:**

| Relative Affinity | Damage Modifier | Effect |
|-------------------|-----------------|--------|
| +100 | ×1.19 (+19%) | Strong advantage |
| +50 | ×1.12 (+12%) | Advantage |
| 0 | ×1.00 (0%) | Neutral |
| -50 | ×0.88 (-12%) | Disadvantage |
| -100 | ×0.81 (-19%) | Strong disadvantage |

#### Effect Duration Modifiers
Applied to burn duration, poison ticks, stun duration, slow effects, etc.

```java
Modified Duration = Base Duration × (1.0 + Effect Modifier)
```

**Maximum Effect Modifier: ±30%**
- High affinity = Longer effect durations on enemies
- Low affinity = Shorter effect durations (more resistance)

#### Proc Chance Modifiers
Applied to enchantment trigger chances, critical bonuses, etc.

```java
Modified Chance = Base Chance × (1.0 + Proc Modifier)
```

**Capped at 100% (1.0)**

## Technical Implementation

### Classes

#### AffinityModifier.java
Core utility class for all affinity calculations:
- `calculateDamageModifier()` - Returns damage multiplier
- `calculateEffectModifier()` - Returns effect duration multiplier
- `calculateProcModifier()` - Returns proc chance multiplier
- `getRelativeAffinity()` - Returns raw affinity difference
- `getAffinityFeedback()` - Returns formatted combat feedback

#### EnchantmentDamageUtil.java
Integrates affinity into damage events:
- `addBonusDamageToEvent()` - Applies affinity modifier before adding bonus damage
- Automatically detects PVP vs PVE
- Sends combat feedback to players

#### StatScanManager.java
Manages affinity scanning from equipment:
- `scanAndUpdateAffinity()` - Scans all 6 equipment slots
- `scanAffinityFromItem()` - Reads enchantments from individual items
- Handles hybrid enchantments (60/40 split between elements)
- Updates every 5 ticks (0.25 seconds)

### Constants (Tunable)

```java
// AffinityModifier.java
private static final double SCALING_CONSTANT = 50.0;       // Sensitivity tuning
private static final double MAX_DAMAGE_MODIFIER = 0.25;    // ±25% damage
private static final double MAX_EFFECT_MODIFIER = 0.30;    // ±30% effects
```

## PVP vs PVE Behavior

### PVP (Player vs Player)
- ✅ Affinity modifiers apply
- ✅ Damage scaling based on affinity difference
- ✅ Effect duration scaling
- ✅ Proc chance scaling
- ✅ Combat feedback messages

### PVE (Player vs Entity)
- ❌ Affinity modifiers do NOT apply
- Base damage and effects unchanged
- No affinity advantage/disadvantage

## Balance Considerations

### Soft-Cap Benefits
1. **No Extreme Swings**: tanh prevents 2x or 0.5x damage multipliers
2. **Diminishing Returns**: Stacking one element has diminishing benefits
3. **Encourages Diversity**: Players benefit from balanced affinity across multiple elements
4. **Skill > Gear**: 25% max modifier means skill still matters more than affinity

### Strategic Depth
1. **Counter-Play**: Defenders can counter specific elements by equipping matching enchantments
2. **Build Variety**: Different affinity profiles create different playstyles
3. **Equipment Decisions**: Players must choose between high-affinity specialization vs balanced coverage
4. **Dynamic Meta**: PVP meta shifts based on popular element choices

## Example Combat Scenarios

### Scenario 1: Fire Specialist vs Balanced Player
**Attacker:**
- Fire Affinity: 2,000
- Other Elements: ~200 each

**Defender:**  
- All Elements: ~500 each

**Fire Attack:**
- Relative Affinity: 2,000 - 500 = +1,500
- Modifier: tanh(1,500/50) = tanh(30) ≈ 1.0 → ×1.25 (MAX)
- **Result**: §a⚡ Strong Elemental Advantage! +25% damage

### Scenario 2: Mirror Match
**Both Players:**
- Fire Affinity: 1,500

**Fire Attack:**
- Relative Affinity: 1,500 - 1,500 = 0
- Modifier: tanh(0/50) = 0 → ×1.00
- **Result**: §7⚖ Neutral (no modifier)

### Scenario 3: Defensive Counter
**Attacker:**
- Lightning Affinity: 800

**Defender:**
- Lightning Affinity: 1,800 (counter-built specifically for Lightning)

**Lightning Attack:**
- Relative Affinity: 800 - 1,800 = -1,000
- Modifier: tanh(-1,000/50) = tanh(-20) ≈ -1.0 → ×0.75 (near MAX reduction)
- **Result**: §c⚡ Strong Elemental Disadvantage! -25% damage

## Future Enhancements

### Potential Additions
1. **Element Interactions**: Rock-paper-scissors relationships (Fire > Nature, Water > Fire, etc.)
2. **Affinity Mastery Bonuses**: Unlock special abilities at high affinity tiers
3. **Hybrid Element Bonuses**: Special modifiers for Ice (Air+Water) in PVP
4. **Affinity-Based Skills**: Ultimate abilities that scale with dominant element
5. **Leaderboards**: Track highest affinities per element
6. **Affinity Titles**: Display elemental mastery ranks

## Testing & Tuning

### Key Metrics to Monitor
- Average damage modifier in PVP encounters
- Distribution of affinity advantages/disadvantages
- Player feedback on "feel" of advantages
- Combat duration changes

### Tuning Levers
```java
SCALING_CONSTANT     // Increase for less sensitivity, decrease for more
MAX_DAMAGE_MODIFIER  // Increase for larger swings, decrease for subtlety
MAX_EFFECT_MODIFIER  // Independent tuning for non-damage effects
```

## Integration Checklist

✅ **Core System**
- [x] AffinityModifier utility class created
- [x] Damage modifier calculation (tanh soft-cap)
- [x] Effect duration modifier calculation
- [x] Proc chance modifier calculation

✅ **Combat Integration**
- [x] EnchantmentDamageUtil applies modifiers
- [x] PVP detection (Player vs Player)
- [x] Combat feedback messages
- [x] Debug logging for testing

✅ **Affinity Tracking**
- [x] StatScanManager scans equipment
- [x] Real-time affinity updates
- [x] Hybrid enchantment support
- [x] ElementalAffinity data structure

✅ **Display**
- [x] StatsGUI shows affinity values
- [x] Affinity tiers (Novice → Legend)
- [x] Combat feedback during fights

🔄 **Future Enchantments**
- [ ] Update existing enchantments to use AffinityModifier helper methods
- [ ] Add effect duration scaling for DOT effects
- [ ] Add proc chance scaling for trigger-based enchantments
- [ ] Document per-enchantment affinity interactions

## Code Examples

### For Enchantment Developers

#### Apply Affinity to Damage
```java
// In your enchantment's trigger method
double baseDamage = 10.0;
EnchantmentDamageUtil.addBonusDamageToEvent(
    event, 
    baseDamage,  // Affinity modifier applied automatically in PVP
    ElementType.FIRE
);
```

#### Apply Affinity to Effect Duration
```java
// In your DOT enchantment
int baseDuration = 100; // ticks
if (target instanceof Player) {
    int modifiedDuration = AffinityModifier.applyEffectDurationModifier(
        baseDuration, attacker, (Player) target, ElementType.FIRE
    );
    applyBurnEffect(target, modifiedDuration);
}
```

#### Apply Affinity to Proc Chance
```java
// In your chance-based enchantment
double baseChance = 0.25; // 25%
if (target instanceof Player) {
    double modifiedChance = AffinityModifier.applyProcChanceModifier(
        baseChance, player, (Player) target, ElementType.LIGHTNING
    );
    if (Math.random() < modifiedChance) {
        triggerEffect();
    }
}
```

---

**System Status**: ✅ Fully Implemented & Ready for Testing
**Last Updated**: Phase 1C - Affinity PVP Combat System
