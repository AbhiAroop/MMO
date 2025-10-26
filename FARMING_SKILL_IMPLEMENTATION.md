# Farming Skill Implementation Summary

## Overview
Implemented the Farming skill system with 3 subskills following the same structure as the Mining skill.

## Structure

### Main Skill: FarmingSkill
- **Location**: `com.server.profiles.skills.skills.farming.FarmingSkill`
- **Max Level**: 50
- **XP Formula**: Base 200 XP, increasing by 20% each level
- **Milestone Levels**: 5, 10, 15, 20, 25, 30, 35, 40, 45, 50

#### Features
- Farming Fortune bonuses at milestone levels
- Item rewards (Iron Hoe at level 10)
- Currency rewards (coins)
- Skill tree node support:
  - `farming_fortune`: +0.5 farming fortune per level
  - `farming_speed`: +1% farming speed per level
  - `unlock_cultivating`: Unlocks Cultivating subskill

### Subskill 1: Harvesting
- **Location**: `com.server.profiles.skills.skills.farming.subskills.HarvestingSubskill`
- **Max Level**: 100
- **XP Formula**: 100.0 + (50.0 * level) + (10.0 * level^1.5)

#### Features
- **Default Unlocked Crop**: Wheat (always harvestable from level 1)
- **Progression System**: Players can only break crops they've unlocked via skill tree
- **XP Gain**: Breaking crops grants XP (only for unlocked crops)
- **Affected Crops**:
  - Wheat (default)
  - Carrots (requires unlock)
  - Potatoes (requires unlock)
  - Beetroots (requires unlock)
  - Sweet Berries (requires unlock)
  - Cocoa (requires unlock)
  - Nether Wart (requires unlock)
  - Melons (requires unlock)
  - Pumpkins (requires unlock)

#### Skill Tree Nodes
- `unlock_carrots`: Unlocks carrot harvesting
- `unlock_potatoes`: Unlocks potato harvesting
- `unlock_beetroots`: Unlocks beetroot harvesting
- `unlock_sweet_berries`: Unlocks sweet berry harvesting
- `unlock_cocoa`: Unlocks cocoa harvesting
- `unlock_nether_wart`: Unlocks nether wart harvesting
- `unlock_melons`: Unlocks melon harvesting
- `unlock_pumpkins`: Unlocks pumpkin harvesting
- `farming_fortune`: +0.5 farming fortune per level
- `harvest_speed`: +5% harvest speed per level
- `harvesting_xp`: +0.5% XP boost per level

#### Methods
- `canHarvestCrop(Player, Material)`: Check if player can harvest a crop
- `canGainXpFromCrop(Player, Material)`: Check if player can gain XP from a crop
- `affectsCrop(Material)`: Check if a material is a crop affected by this skill
- `getBonusDropChance(int)`: Calculate bonus drop chance (0.5% per level, max 50%)
- `getFarmingFortuneFromSkillTree(Player)`: Get farming fortune from skill tree
- `getSkillTreeBenefits(Player)`: Get all skill tree benefits
- `applyNodeUpgrade(Player, String, int, int)`: Handle node upgrades
- `handleSkillTreeReset(Player, Map)`: Handle skill tree resets

### Subskill 2: Cultivating
- **Location**: `com.server.profiles.skills.skills.farming.subskills.CultivatingSubskill`
- **Max Level**: 100
- **XP Formula**: 100.0 + (50.0 * level) + (10.0 * level^1.5)
- **Unlock Requirement**: Harvesting level 10

#### Features
- **Default Unlocked Crop**: Wheat Seeds (always plantable once Cultivating is unlocked)
- **Progression System**: Players can only plant crops they've unlocked via skill tree
- **XP Gain**: Planting crops and crop growth grants XP
- **Affected Plantable Items**:
  - Wheat Seeds (default)
  - Carrots (requires unlock)
  - Potatoes (requires unlock)
  - Beetroot Seeds (requires unlock)
  - Sweet Berries (requires unlock)
  - Cocoa Beans (requires unlock)
  - Nether Wart (requires unlock)
  - Melon Seeds (requires unlock)
  - Pumpkin Seeds (requires unlock)

#### Skill Tree Nodes
- `unlock_plant_carrots`: Unlocks carrot planting
- `unlock_plant_potatoes`: Unlocks potato planting
- `unlock_plant_beetroots`: Unlocks beetroot planting
- `unlock_plant_sweet_berries`: Unlocks sweet berry planting
- `unlock_plant_cocoa`: Unlocks cocoa planting
- `unlock_plant_nether_wart`: Unlocks nether wart planting
- `unlock_plant_melons`: Unlocks melon planting
- `unlock_plant_pumpkins`: Unlocks pumpkin planting
- `growth_speed`: +2% crop growth speed per level
- `cultivating_xp`: +0.5% XP boost per level
- `auto_replant`: +10% auto-replant chance per level

#### Methods
- `canPlantCrop(Player, Material)`: Check if player can plant a crop
- `canGainXpFromPlanting(Player, Material)`: Check if player can gain XP from planting
- `affectsPlantable(Material)`: Check if a material is plantable and affected by this skill
- `isCultivatingUnlocked(Player)`: Check if Cultivating is unlocked (requires Harvesting 10)
- `getCropGrowthSpeedMultiplier(int)`: Calculate crop growth speed (1% per level, max 100%)
- `getSkillTreeBenefits(Player)`: Get all skill tree benefits
- `applyNodeUpgrade(Player, String, int, int)`: Handle node upgrades
- `handleSkillTreeReset(Player, Map)`: Handle skill tree resets

### Subskill 3: Botany
- **Location**: `com.server.profiles.skills.skills.farming.subskills.BotanySubskill`
- **Max Level**: 100
- **XP Formula**: 150.0 + (75.0 * level) + (15.0 * level^1.5)
- **Status**: STUB - To be implemented later

#### Description
Advanced plant manipulation and custom crop mechanics. This is a more complex subskill that will be implemented after Harvesting and Cultivating are fully functional.

## Integration Requirements

### 1. PlayerStats Additions
Added to `PlayerStats.java`:
- `increaseDefaultFarmingFortune(double)`: Increase farming fortune stat
- `increaseDefaultFarmingSpeed(double)`: Placeholder for farming speed
- `getFarmingSpeed()`: Returns 1.0 as default for now

### 2. SkillRewardType Additions
Added to `SkillRewardType.java`:
- `CROP_GROWTH_SPEED`: Constant for crop growth speed reward type

### 3. SubskillType Enum Updates
Updated `SubskillType.java`:
- Replaced old farming subskills (CROP_GROWTH, ANIMAL_BREEDER, HARVESTER)
- Added new subskills: HARVESTING, CULTIVATING, BOTANY

## Next Steps

### Event Listeners Required
1. **BlockBreakEvent Listener** (Harvesting)
   - Check if broken block is a crop
   - Check if player has unlocked the crop via `HarvestingSubskill.canHarvestCrop()`
   - If not unlocked, cancel the event and send a message
   - If unlocked, grant XP via `HarvestingSubskill.canGainXpFromCrop()`
   - Apply bonus drop chances

2. **BlockPlaceEvent Listener** (Cultivating)
   - Check if placed block is a plantable crop
   - Check if Cultivating is unlocked via `CultivatingSubskill.isCultivatingUnlocked()`
   - Check if player has unlocked the crop via `CultivatingSubskill.canPlantCrop()`
   - If not unlocked, cancel the event and send a message
   - If unlocked, grant XP for planting

3. **BlockGrowEvent Listener** (Cultivating)
   - Check if growing block is a crop
   - Apply growth speed multipliers based on skill level
   - Grant XP when crops grow (smaller amount than planting)

### Skill Tree Definitions Required
Create JSON files for:
1. `farming_skill_tree.json` - Main farming skill tree
2. `harvesting_skill_tree.json` - Harvesting subskill tree with crop unlocks
3. `cultivating_skill_tree.json` - Cultivating subskill tree with planting unlocks
4. `botany_skill_tree.json` - Botany subskill tree (future)

### GUI Implementation
- Farming skill menu GUI
- Harvesting subskill tree GUI
- Cultivating subskill tree GUI
- Progress tracking and level display

## Design Decisions

1. **Wheat as Default Crop**: Wheat is unlocked by default for both Harvesting and Cultivating to provide an entry point for players.

2. **Level 10 Gate**: Cultivating requires Harvesting level 10 to ensure players learn harvesting mechanics first.

3. **Separate Unlock Trees**: Each crop must be unlocked separately in both Harvesting (to break) and Cultivating (to plant), allowing for diverse progression paths.

4. **Farming Fortune**: Similar to Mining Fortune, provides bonus drops from crops.

5. **Growth Speed**: Cultivating provides crop growth speed bonuses to reward active farmers.

## File Structure
```
farming/
├── FarmingSkill.java (Main skill)
└── subskills/
    ├── HarvestingSubskill.java (Crop breaking)
    ├── CultivatingSubskill.java (Crop planting)
    └── BotanySubskill.java (Advanced mechanics - stub)
```

## Testing Checklist
- [ ] Test wheat harvesting (should work by default)
- [ ] Test unlocking carrots via skill tree
- [ ] Test attempting to harvest locked crops (should be blocked)
- [ ] Test XP gain from harvesting unlocked crops
- [ ] Test Cultivating unlock at Harvesting level 10
- [ ] Test planting wheat seeds (should work once Cultivating unlocked)
- [ ] Test unlocking other crops for planting
- [ ] Test attempting to plant locked crops (should be blocked)
- [ ] Test XP gain from planting unlocked crops
- [ ] Test skill tree reset functionality
- [ ] Test farming fortune bonuses
- [ ] Test milestone level rewards

## Implementation Status
✅ FarmingSkill class complete
✅ HarvestingSubskill class complete
✅ CultivatingSubskill class complete
✅ BotanySubskill stub created
✅ SubskillType enum updated
✅ SkillRewardType constants added
✅ PlayerStats methods added
⏳ Event listeners (not implemented)
⏳ Skill tree JSON files (not created)
⏳ GUI implementation (not created)
⏳ Testing (not performed)
