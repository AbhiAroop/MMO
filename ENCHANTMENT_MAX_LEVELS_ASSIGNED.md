# Enchantment Max Levels - Complete Assignment

## Overview
Each enchantment now has a custom max level (1-8) based on power, complexity, and utility. This creates natural progression variety where simpler enchantments cap early, while epic/hybrid enchantments can reach maximum power.

## Max Level Distribution

### Level 2 (Very Simple/Reactive)
- **Ember Veil** - Simple reactive burn, very basic

### Level 3 (Simple Utilities)
- **Gale Step** - Basic dash utility
- **Ashen Veil** - Kill invisibility utility

### Level 4 (Moderate Combat/Debuff)
- **Burdened Stone** - Stacking slow debuff
- **Hollow Edge** - Simple sustain mechanic
- **Inferno Strike** - Proc damage
- **Mistveil** - Projectile deflection

### Level 5 (Balanced Combat/CC)
- **Cinderwake** - AoE fire trails
- **Whispers** - Evasion mechanic
- **Arc Nexus** - Attack speed stacking
- **Dawnstrike** - Blind CC
- **Frostflow** - Stacking CC

### Level 6 (Strong Combat/Defense)
- **Deepcurrent** - AoE knockback wave
- **Terraheart** - Damage reduction while stationary
- **Veilborn** - Invisibility activation
- **Radiant Grace** - AoE healing support

### Level 7 (Powerful/Complex)
- **Voltbrand** - Chain lightning mechanic
- **Mistborne Tempest** - Epic hybrid dash (Water/Air)
- **Embershade** - Epic hybrid debuff (Fire/Shadow)

### Level 8 (Maximum Power - Epic/Legendary)
- **Stormfire** - Epic hybrid AoE (Fire/Lightning)
- **Decayroot** - Epic hybrid root + DoT (Earth/Shadow)
- **Celestial Surge** - Epic hybrid divine lightning (Light/Lightning)
- **Pure Reflection** - Epic hybrid support barrier (Water/Light)

## Design Philosophy

### Simple Enchantments (1-3)
- Basic effects with minimal mechanics
- Common/Uncommon rarity
- Single-purpose utilities
- Examples: Ember Veil, Gale Step, Ashen Veil

### Standard Enchantments (4-6)
- Moderate complexity
- Rare rarity typically
- Combat-focused or defensive
- Examples: Cinderwake, Deepcurrent, Terraheart, Veilborn

### Powerful Enchantments (7-8)
- Complex mechanics (chains, hybrids, AoE)
- Epic/Legendary rarity
- Multi-element hybrids
- Game-changing effects
- Examples: Voltbrand, Stormfire, Decayroot, Celestial Surge, Pure Reflection

## Element Distribution

### Pure Elements
- **Fire**: Ember Veil (2), Ashen Veil (3), Inferno Strike (4), Cinderwake (5)
- **Water**: Mistveil (4), Frostflow (5), Deepcurrent (6)
- **Earth**: Burdened Stone (4), Terraheart (6)
- **Air**: Gale Step (3), Whispers (5)
- **Lightning**: Arc Nexus (5), Voltbrand (7)
- **Shadow**: Hollow Edge (4), Veilborn (6)
- **Light**: Dawnstrike (5), Radiant Grace (6)

### Hybrid Elements
- **Storm (Fire/Lightning)**: Stormfire (8)
- **Mist (Water/Air)**: Mistborne Tempest (7)
- **Ash (Fire/Shadow)**: Embershade (7)
- **Decay (Earth/Shadow)**: Decayroot (8)
- **Radiance (Light/Lightning)**: Celestial Surge (8)
- **Purity (Water/Light)**: Pure Reflection (8)

## Fragment Planning Guide

Players can now optimize fragment usage based on max levels:

- **2-3 fragments**: Good for simple enchants (levels 2-3)
- **32-64 fragments**: Suitable for moderate enchants (levels 4-5)
- **96-128 fragments**: Recommended for strong enchants (levels 6)
- **176-216 fragments**: Ideal for powerful enchants (level 7)
- **217-256 fragments**: Maximum for epic enchants (level 8)

## Balance Notes

### Why Different Max Levels?
1. **Progression Variety**: Not all enchantments should feel the same
2. **Fragment Efficiency**: Don't waste 256 fragments on a level 2 enchant
3. **Power Budget**: Simple effects shouldn't scale infinitely
4. **Rarity Alignment**: Epic hybrids deserve higher caps than common reactives
5. **Build Diversity**: Forces players to choose between many weak enchants vs few powerful ones

### PvP/PvE Balance
- **Low-cap enchants** (2-4): Great for early game, PvE farming
- **Mid-cap enchants** (5-6): Standard PvP/PvE balance
- **High-cap enchants** (7-8): Endgame investments, rare hybrids

## Testing Recommendations

1. **Verify GUI Capping**: Use enchanter with 256 fragments on each enchant
   - Ember Veil should cap at level II
   - Gale Step should cap at level III
   - Stormfire should reach level VIII

2. **Admin Override Testing**: Use `/enchant add` with excessive levels
   - Should show warning but still apply
   - Example: `/enchant add ember_veil 8` warns but applies VIII

3. **Info Display**: Check `/enchant info` for each enchant
   - Should show correct max level
   - Should show Roman numeral range (I-II, I-VIII, etc.)

## Future Considerations

- New enchantments should be assigned max levels based on:
  - Mechanical complexity
  - Rarity tier
  - Element type (hybrid vs pure)
  - Impact on gameplay
  - PvP/PvE balance

- Consider seasonal adjustments if certain enchants dominate
- May introduce enchantment "evolution" systems later (upgrade max level via quest)
