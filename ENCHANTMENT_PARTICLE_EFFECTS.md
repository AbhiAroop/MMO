# Enchantment Particle Effects System

## Overview
Implemented a comprehensive particle effects system for the enchantment altar and anvil, featuring element-based particles that match the enchantments being applied.

## Features

### 1. Ambient Particles Around Structures
**Altars and Anvils** now display continuous ambient particle effects:
- **Rainbow spiral effect** around both altars and anvils
- Particles spawn in a circular pattern that rotates over time
- Updates every 5 ticks (4 times per second) for smooth animation
- Automatically tracks all registered altars and spawned anvils

### 2. Player Enchanting Effects
When a player successfully enchants an item at the **Enchantment Altar**:

**Burst Effects:**
- Spawns elemental particles based on each applied enchantment's element
- Different particle types for each element:
  - 🔥 **Fire**: FLAME + LAVA particles
  - 💧 **Water**: SPLASH + DRIPPING_WATER particles
  - ⛰ **Earth**: Brown dust + terracotta block particles
  - 💨 **Air**: CLOUD + SWEEP_ATTACK particles
  - ⚡ **Lightning**: ELECTRIC_SPARK + END_ROD particles
  - 🌑 **Shadow**: SQUID_INK + SMOKE particles
  - ✨ **Light**: GLOW + END_ROD particles
  - 🌿 **Nature**: Green dust + HAPPY_VILLAGER particles

**Helix Effect:**
- Double helix of particles spirals around the player
- Uses the primary enchantment's element colors
- Creates dramatic visual feedback for successful enchanting

**Ring Effect:**
- For multi-enchantment applications
- Creates expanding rings of elemental particles
- Secondary enchantment element determines ring color

### 3. Anvil Combining Effects
When items are successfully combined in the **Custom Anvil**:

**Element Detection:**
- Reads NBT data from the result item
- Identifies all enchantments and their elements
- Spawns appropriate particles for each unique element

**Visual Effects:**
- Burst effect for each unique element type on the result
- Ring effect using the primary enchantment element
- Smaller particle count than altar enchanting for subtlety

## Implementation Details

### New Files Created

#### `ElementalParticles.java`
Location: `src/main/java/com/server/enchantments/effects/ElementalParticles.java`

**Methods:**
- `spawnElementalBurst()` - Creates explosion of element-based particles
- `spawnHybridBurst()` - Combines two elements for hybrid enchantments
- `spawnAmbientEffect()` - Continuous ambient particles for structures
- `spawnPlayerHelix()` - Double helix effect around players
- `spawnElementalRing()` - Ring of particles on the ground
- `spawnSingleElementalParticle()` - Helper for precise particle placement
- `getHybridColor()` - Maps hybrid elements to specific colors

#### `AmbientParticleTask.java`
Location: `src/main/java/com/server/enchantments/effects/AmbientParticleTask.java`

**Purpose:** BukkitRunnable that continuously spawns ambient particles around altars and anvils
**Update Rate:** Every 5 ticks (250ms / 4 times per second)
**Detection:**
- Altars: Tracked via EnchantmentTableStructure registered UUIDs
- Anvils: Detected by scanning for armor stands with anvil helmets

### Modified Files

#### `EnchantmentApplicator.java`
**Changes:**
- Added import for `ElementalParticles`
- Added particle bursts when enchantments are successfully applied
- Added helix effect for primary enchantment
- Added ring effect for multi-enchantment applications
- Effects trigger alongside existing sound effects

#### `AnvilGUIListener.java`
**Changes:**
- Added imports for `ElementalParticles`, `ElementType`, and `NBTItem`
- Added particle effects when anvil combination succeeds
- Reads NBT data to detect enchantment elements
- Spawns bursts and rings based on result enchantments

#### `Main.java`
**Changes:**
- Added initialization of `AmbientParticleTask` in `initializeEnchantmentSystem()`
- Task starts 1 second after server start
- Repeats every 5 ticks for smooth animation

## Element-Specific Particle Mappings

| Element | Primary Particle | Secondary Particle | Color/Effect |
|---------|-----------------|-------------------|--------------|
| Fire | FLAME | LAVA | Red/Orange flames |
| Water | SPLASH | DRIPPING_WATER | Blue water splashes |
| Earth | DUST (brown) | BLOCK (terracotta) | Brown/tan earth |
| Air | CLOUD | SWEEP_ATTACK | White clouds/wind |
| Lightning | ELECTRIC_SPARK | END_ROD | Yellow sparks |
| Shadow | SQUID_INK | SMOKE | Dark gray/black |
| Light | GLOW | END_ROD | Bright white/purple |
| Nature | DUST (green) | HAPPY_VILLAGER | Green natural |

## Hybrid Elements

Hybrid enchantments combine particles from both component elements:
- Ice (Air + Water): Both cloud and water particles
- Storm (Fire + Lightning): Flames mixed with electric sparks
- Mist (Water + Air): Water and cloud effects
- Decay (Earth + Shadow): Earth and shadow particles
- Radiance (Light + Lightning): Bright light with sparks
- Ash (Fire + Shadow): Dark flames
- Purity (Water + Light): Bright water effects

Each hybrid also adds accent particles in a hybrid-specific color.

## Particle Intensity

**Altar Enchanting:**
- Burst: 1.0x intensity (full effect)
- Helix: 40 ticks duration, 30 particles per strand
- Ring: 1.5 block radius, 24 particles

**Anvil Combining:**
- Burst: 0.8x intensity (slightly reduced)
- Ring: 1.2 block radius, 24 particles
- No helix (simpler than altar)

**Ambient Effects:**
- 1-2 particles per frame
- Spiral pattern with 1.2 block radius
- Sine wave vertical motion

## Performance Considerations

- Ambient particles limited to 1-2 per structure per tick
- Particle counts balanced for visual appeal vs. performance
- Task runs on main thread but operations are lightweight
- Only spawns particles in loaded chunks
- Dead/invalid structures automatically skipped

## Testing

To test the particle effects:

1. **Altar Ambient Effects:**
   - `/enchant spawn altar` - Spawn an altar
   - Watch for rainbow spiral particles around it

2. **Anvil Ambient Effects:**
   - `/enchant spawn anvil` - Spawn an anvil
   - Watch for rainbow spiral particles around it

3. **Enchanting Effects:**
   - Use altar to enchant an item with elemental fragments
   - Observe element-specific burst, helix, and ring effects
   - Try multi-enchantments to see combined effects

4. **Anvil Effects:**
   - Combine items with enchantments in custom anvil
   - Observe element-specific bursts and rings based on result

## Future Enhancements

Potential additions:
- Sound effects synchronized with particle bursts
- Configurable particle density settings
- Different patterns for legendary vs common enchantments
- Trail effects when moving with enchanted items
- Altar activation animation when player approaches
- Element-specific ambient colors for altars based on last use

## Build Information

**Build Status:** ✅ Success  
**Compile Time:** 7.467s  
**Classes Added:** 2 new files (ElementalParticles, AmbientParticleTask)  
**Classes Modified:** 3 files (EnchantmentApplicator, AnvilGUIListener, Main)  
**Total Lines Added:** ~600 lines

## Notes

- All particle effects respect Minecraft's particle system
- Effects work in all dimensions (Overworld, Nether, End)
- Particles are client-side visual only (no gameplay impact)
- System automatically handles cleanup when structures are removed
- Compatible with resource packs (uses vanilla particles)
