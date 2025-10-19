# Multi-Enchantment System

## Overview
The enchanter can now grant **multiple enchantments** on a single item based on the total number of fragments used. This creates exciting possibilities for powerful multi-element builds and increases the value of collecting large amounts of fragments.

## How It Works

### Fragment Thresholds
- **64 fragments** = 1 enchantment (minimum)
- **128 fragments** = 2 enchantments
- **192 fragments** = 3 enchantments
- **256 fragments** = 4 enchantments (maximum)

### Enchantment Distribution
When multiple enchantments are granted, the fragments are **distributed** across them:
- 128 fragments → 2 enchantments @ ~64 fragments each (~Level III-IV each)
- 192 fragments → 3 enchantments @ ~64 fragments each (~Level III-IV each)
- 256 fragments → 4 enchantments @ ~64 fragments each (~Level III-IV each)

### Element Pool System
The enchantments selected come from a **mixed pool** based on fragment elements:

#### Single Element Fragments
- **All Fire**: Grants multiple Fire enchantments
- **All Water**: Grants multiple Water enchantments
- **All Lightning**: Grants multiple Lightning enchantments
- etc.

#### Mixed Element Fragments
- **Fire + Lightning**: Can grant:
  - Stormfire (hybrid) + Fire enchantments
  - Stormfire (hybrid) + Lightning enchantments
  - Multiple Fire + Lightning single-element enchantments
  - Mix of hybrid and single-element

- **Water + Air**: Can grant:
  - Mistborne Tempest (hybrid) + Water enchantments
  - Mistborne Tempest (hybrid) + Air enchantments
  - Multiple Water + Air single-element enchantments
  - Mix of hybrid and single-element

### Hybrid + Single Element Mix
**Key Feature**: You can receive BOTH hybrid and single-element enchantments in one application!

Example with 256 Fire + Lightning fragments:
- Stormfire VIII (hybrid - Fire/Lightning)
- Voltbrand V (Lightning)
- Cinderwake IV (Fire)
- Inferno Strike III (Fire)

## Examples

### Example 1: Pure Element (128 Fire Fragments)
```
⭐ MULTI-ENCHANTMENT! (2 enchantments)
  • Cinderwake IV [Epic]
  • Inferno Strike III [Rare]
```

### Example 2: Mixed Elements (192 Fire + Lightning)
```
⭐ MULTI-ENCHANTMENT! (3 enchantments) ⚡ 1 Hybrid
  • Stormfire VI [Legendary] ⚡HYBRID
  • Voltbrand IV [Epic]
  • Cinderwake III [Rare]
```

### Example 3: Maximum Power (256 Water + Light)
```
⭐ MULTI-ENCHANTMENT! (4 enchantments) ⚡ 2 Hybrids
  • Pure Reflection V [Godly] ⚡HYBRID
  • Radiant Grace IV [Epic]
  • Mistveil IV [Uncommon]
  • Frostflow III [Rare]
```

### Example 4: Triple Element Mix (256 Earth + Shadow + Air)
```
⭐ MULTI-ENCHANTMENT! (4 enchantments) ⚡ 1 Hybrid
  • Decayroot VI [Legendary] ⚡HYBRID (Earth/Shadow)
  • Terraheart IV [Epic] (Earth)
  • Veilborn IV [Rare] (Shadow)
  • Gale Step II [Uncommon] (Air)
```

## XP Cost

### Cost Calculation
- **Base Cost**: Sum of all enchantment rarity costs
- **Fragment Cost**: 2 XP per fragment slot used
- **Multi-Enchantment Discount**: 20% off total if 2+ enchantments

### Example Costs
- 1 enchantment (64 fragments): ~15-25 XP levels
- 2 enchantments (128 fragments): ~25-40 XP levels (with 20% discount)
- 3 enchantments (192 fragments): ~35-55 XP levels (with 20% discount)
- 4 enchantments (256 fragments): ~45-70 XP levels (with 20% discount)

## Strategy Guide

### Maximizing Enchantments
1. **Collect 256 Fragments**: Always aim for maximum to get 4 enchantments
2. **Mix Elements**: Use 2-3 different element types for hybrid + single-element variety
3. **Higher Tiers**: Better tiers = better quality rolls across ALL enchantments
4. **XP Preparation**: Save up XP before enchanting with large amounts

### Fragment Efficiency
- **64 fragments**: Best for single powerful enchantment (full Level V-VIII)
- **128 fragments**: Good balance - 2 moderate enchantments (Level III-IV each)
- **192 fragments**: Three enchantments spread across elements
- **256 fragments**: Maximum variety - 4 enchantments from your element pool

### Element Mixing Strategy

#### Two Elements (Best for Hybrids)
- Use equal amounts of two elements that form a hybrid
- Example: 128 Fire + 128 Lightning = Storm hybrid chance + Fire/Lightning enchants
- Result: High chance of 1-2 hybrid enchants + 2-3 single-element enchants

#### Three Elements (Maximum Variety)
- Use three different elements for diverse enchantment pool
- Example: 85 Earth + 85 Shadow + 86 Air
- Result: Possible Decay hybrid + enchants from all three elements

#### Single Element (Specialized Build)
- Use all of one element type for pure element focus
- Example: 256 Fire fragments
- Result: 4 Fire enchantments, no hybrids

## Balance Considerations

### Power vs Variety
- **Single High-Level Enchant**: 64 fragments → 1 enchant at Level V-VIII
- **Multiple Mid-Level Enchants**: 256 fragments → 4 enchants at Level III-IV each
- Trade-off: Depth vs Breadth

### Hybrid Chances
- Hybrid enchantments can still appear in multi-enchant rolls
- Each enchantment roll has independent hybrid chance
- Possible to get multiple hybrids if elements support it (rare but possible)

### Max Level Caps Still Apply
- Each enchantment respects its individual max level
- Example: Ember Veil (max 2) will cap at Level II even with 64 fragments allocated

## Visual Indicators

### Success Message Format
```
Successfully enchanted with:
  • [Enchantment] [Level] [Quality]
  • [Enchantment] [Level] [Quality] ⚡HYBRID
  • [Enchantment] [Level] [Quality]
⭐ MULTI-ENCHANTMENT! (3 enchantments) ⚡ 1 Hybrid
```

### Audio Feedback
- **Base Sound**: Enchantment table + level up (all enchantments)
- **Multi-Enchantment Bonus**: Evoker cast spell (2+ enchantments)
- **Volume**: Scales with number of enchantments

## Technical Details

### Distribution Algorithm
1. Calculate enchantment count: `min(max(1, totalFragments / 64), 4)`
2. Divide fragments evenly across enchantments
3. For each enchantment:
   - Roll element (hybrid or single based on fragment mix)
   - Select enchantment from element pool
   - Calculate level from allocated fragments
   - Roll quality from fragment tiers
   - Apply max level cap

### Element Selection Per Enchantment
- Each enchantment independently determines hybrid vs single-element
- Uses same hybrid chance calculation as before
- If hybrid fails, falls back to dominant element
- Multiple enchantments = multiple rolls = higher total chance for at least one hybrid

## Comparison: Old vs New

### Before (Single Enchantment)
```
256 fragments → 1 enchantment at Level VIII
```

### After (Multi-Enchantment)
```
256 fragments → 4 enchantments at Level III-IV each
```

### Which Is Better?
Depends on playstyle:
- **Single VIII**: Best for min-maxing one specific ability
- **Four III-IV**: Best for versatility and multiple effects
- **Player Choice**: Use fewer fragments for single high-level, or more for variety

## Future Enhancements
- Fragment allocation UI (let players choose distribution)
- Guaranteed hybrid option (consume more fragments for 100% hybrid chance)
- Element priority selection (prefer certain elements in mixed pools)
- Multi-enchant anvil upgrades (upgrade all enchants at once)
