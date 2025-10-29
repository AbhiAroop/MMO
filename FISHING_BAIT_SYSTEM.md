# Fishing Bait System Implementation

## Overview
Created a comprehensive fishing bait system that requires players to have bait in their inventory before the fishing minigame will start. The system includes 5 different bait types with custom model data, specific fish attraction, quality boost mechanics, and admin commands.

## Bait Types

### 1. Worm Bait (COMMON)
- **Display Name**: §aWorm Bait
- **Custom Model Data**: 100001
- **Base Item**: Wheat Seeds
- **Attracts**: Cod, Tropical Fish
- **Quality Boost**: +5%
- **Description**: A common earthworm that attracts basic freshwater fish

### 2. Cricket Bait (UNCOMMON)
- **Display Name**: §2Cricket Bait
- **Custom Model Data**: 100002
- **Base Item**: Wheat Seeds
- **Attracts**: Salmon, Pufferfish
- **Quality Boost**: +8%
- **Description**: A lively cricket that attracts surface-dwelling fish

### 3. Minnow Bait (RARE)
- **Display Name**: §bMinnow Bait
- **Custom Model Data**: 100003
- **Base Item**: Wheat Seeds
- **Attracts**: Frozen Cod, Frozen Salmon, Ice Pike, Frost Trout
- **Quality Boost**: +12%
- **Description**: A small live fish that attracts larger predatory ice fish

### 4. Leech Bait (EPIC)
- **Display Name**: §5Leech Bait
- **Custom Model Data**: 100004
- **Base Item**: Wheat Seeds
- **Attracts**: Magma Fish, Fire Bass, Molten Grouper
- **Quality Boost**: +15% + Mob Chance
- **Description**: A blood-sucking leech that attracts exotic and dangerous lava fish

### 5. Magic Lure (LEGENDARY)
- **Display Name**: §6§lMagic Lure
- **Custom Model Data**: 100005
- **Base Item**: Wheat Seeds
- **Attracts**: Blazefin, Void Angler, Eldritch Eel, Abyss Dweller, Dimension Leviathan
- **Quality Boost**: +20% + Rare Mob Chance
- **Description**: An enchanted lure that attracts the rarest and most powerful void fish

## Technical Implementation

### Files Created:
1. **FishingBait.java** - Enum defining all bait types with properties
   - Display names with color codes
   - Custom model data assignments
   - Lore descriptions
   - **Specific fish types attracted** (not rarities)
   - **Quality boost percentage** (5-20%)
   - Item creation and identification methods
   - Methods: `canAttractFish()`, `getRandomAttractedFish()`, `getQualityBoost()`

2. **BaitManager.java** - Utility class for bait management
   - `findBaitInInventory()` - Check if player has any bait
   - `consumeBait()` - Remove one bait from inventory
   - `hasBait()` - Boolean check for bait presence
   - `giveBait()` - Admin method to give baits to players

3. **FishingCommand.java** - Admin command handler
   - `/fishing bait <type> [amount] [player]` - Give baits
   - `/fishing listbaits` - Show all available bait types with fish and quality boost
   - Tab completion for bait types and player names
   - Permission: `mmo.admin.fishing`

### Files Modified:

1. **FishingListener.java**
   - Added bait check in `handleBite()` method
   - Consumes one bait before starting minigame
   - Sends failure messages if no bait available:
     - Title: "§c§lNo Bait!"
     - Subtitle: "§7You need bait to catch fish!"
     - Chat: Purchase/command instructions
   - Passes bait to `Fish.createFish()` method

2. **FishingSession.java**
   - Added `FishingBait bait` field to store consumed bait
   - Added `getBait()` and `setBait()` methods
   - Bait is stored per session for fish generation and mob spawning

3. **Fish.java**
   - Updated `createFish()` to accept `FishingBait` parameter
   - **Uses bait to determine which fish to catch** (not environment)
   - If bait present: Catches fish from bait's attracted list
   - If no bait: Falls back to environment-based selection
   - Passes bait quality boost to quality calculation
   - Legacy method for backwards compatibility

4. **FishQuality.java**
   - Added overloaded `fromAccuracy()` method with parameters:
     - `accuracy` - Base accuracy percentage
     - `perfectCatches` - Number of perfect catches (each adds 2% to quality roll)
     - `qualityBoost` - Bait quality boost (added to accuracy)
   - **Perfect/Good catches now directly improve quality chances**
   - Quality boost from bait stacks with perfect catch bonuses
   - Original method calls new one with 0 values for compatibility

5. **Main.java**
   - Registered FishingCommand executor and tab completer
   - Added import for FishingCommand

6. **plugin.yml**
   - Added `fishing` command definition with aliases
   - Added `mmo.admin.fishing` permission

## How It Works

### Quality Calculation System:
The quality of caught fish is determined by three factors:

1. **Base Accuracy** (0-100%)
   - From minigame performance (successful catches / total attempts)
   
2. **Perfect Catches Bonus**
   - Each perfect catch (hitting green zone) adds **+2% to quality roll**
   - Example: 3 perfect catches = -6% from quality roll (better odds)
   
3. **Bait Quality Boost**
   - Added directly to accuracy percentage
   - Worm: +5%, Cricket: +8%, Minnow: +12%, Leech: +15%, Magic Lure: +20%
   
**Example Calculation:**
- Base accuracy: 85%
- Perfect catches: 4 (4 × 2% = 8% bonus)
- Bait: Minnow (+12%)
- Adjusted accuracy: 85% + 12% = 97%
- Quality roll: Random(0-100) - 8% = better odds for higher quality

### Fish Selection System:
1. **With Bait**: Random fish from bait's attracted list
   - Worm → Cod or Tropical Fish
   - Cricket → Salmon or Pufferfish
   - Minnow → Frozen fish varieties
   - Leech → Lava fish (Magma Fish, Fire Bass, Molten Grouper)
   - Magic Lure → Void fish (Void Angler, Eldritch Eel, etc.)

2. **Without Bait**: Random fish from current environment (legacy behavior)

### Quality Distribution Example (with 100% accuracy + Minnow bait):
- Adjusted accuracy: 100% + 12% = 100% (capped)
- With 3 perfect catches: Quality roll reduced by 6%
- **Result chances:**
  - PERFECT: 5% → ~11% (with perfect bonus)
  - EXCELLENT: 25% → ~31%
  - GOOD: 40% → ~34%
  - NORMAL: 25% → ~19%
  - POOR: 5% → ~5%

## Usage Flow

### Player Experience:
1. Player obtains bait through admin commands or shops (future)
2. Player casts fishing rod into water
3. When fish bites (BOP event):
   - System checks for bait in inventory
   - If no bait: Shows error title/messages and cancels event
   - If bait found: Consumes 1 bait and starts minigame
4. Minigame proceeds with bait stored in session
5. On completion:
   - Fish selected from bait's attracted list
   - Quality calculated with bait boost + perfect catch bonuses
   - Player receives fish with improved quality chances

### Admin Commands:
```
/fishing bait worm 64           - Give yourself 64 worm baits
/fishing bait magic_lure 1 PlayerName  - Give specific player 1 magic lure
/fishing listbaits              - Show all bait types, attracted fish, and quality boosts
```

## Strategic Bait Usage

### Bait Progression:
1. **Early Game (Normal Water)**
   - **Worm**: Basic fish, small quality boost
   - **Cricket**: Better fish (Salmon), moderate boost

2. **Mid Game (Ice Fishing)**
   - **Minnow**: Ice fish required, +12% quality boost
   - Essential for Frozen Cod, Ice Pike, Frost Trout

3. **Late Game (Lava Fishing)**
   - **Leech**: Exclusive lava fish access
   - +15% quality, mob spawn chance

4. **End Game (Void Fishing)**
   - **Magic Lure**: Only way to catch void fish
   - +20% quality, rare mob spawns
   - Dimension Leviathan exclusive to this bait

### Quality Optimization Tips:
- **Perfect Catches**: Each one adds 2% to quality roll - aim for green zone!
- **Better Baits**: Higher tier baits give better quality boosts
- **Accuracy**: Combined with bait boost, can push quality into higher tiers
- **Trophy Fish**: Require 3+ perfect catches + PERFECT quality + large size

## Custom Model Data Allocation

Baits use the 100000-100999 range:
- 100001-100005: Basic baits (current)
- 100006-100099: Reserved for future bait types
- 100100-100199: Reserved for special event baits

## Testing Checklist

- [x] Build successful (305 source files compiled)
- [x] Command registered in plugin.yml
- [x] Permission system configured
- [x] Bait consumption implemented
- [x] Error messages for no bait
- [x] Bait-specific fish attraction implemented
- [x] Quality boost system integrated
- [x] Perfect catches impact quality calculation
- [ ] In-game test: Give bait via command
- [ ] In-game test: Fish without bait (should fail)
- [ ] In-game test: Fish with worm bait (should catch Cod/Tropical Fish)
- [ ] In-game test: Fish with cricket bait (should catch Salmon/Pufferfish)
- [ ] In-game test: Compare quality with/without good bait
- [ ] In-game test: Verify perfect catches improve quality
- [ ] In-game test: Tab completion works
- [ ] In-game test: Multiple bait types

## Future Integration Points

### Mob Spawning
Higher tier baits (Leech, Magic Lure) are designed to attract hostile mobs:
- Check bait type during loot generation
- Spawn mobs with probability based on bait tier
- Leech: 10% chance for lava mobs
- Magic Lure: 25% chance including void creatures

### Shop Integration
Baits can be sold in fishing shops:
- Worm: 10 coins
- Cricket: 25 coins
- Minnow: 50 coins
- Leech: 100 coins
- Magic Lure: 250 coins

### Bait Crafting (Future)
Players could craft baits from materials:
- Worm: String + Dirt
- Cricket: String + Grass
- Minnow: Raw Cod + String
- Leech: Fermented Spider Eye + String
- Magic Lure: Enchanted materials

## Notes

- **Base Item Choice**: Wheat Seeds chosen because:
  - Stackable to 64
  - Not commonly used in vanilla gameplay
  - Small sprite works well for bait visual
  - No interference with other game mechanics

- **Bait Storage**: Baits stack normally and consume one at a time, allowing players to carry multiple fishing sessions worth of bait

- **Rarity Overlap**: Some baits attract overlapping rarities (e.g., Cricket attracts Uncommon & Rare) to ensure players have options at different progression stages

- **Permission Required**: Admin-only commands prevent players from generating unlimited baits. Future shop/crafting integration will provide legitimate acquisition methods.
