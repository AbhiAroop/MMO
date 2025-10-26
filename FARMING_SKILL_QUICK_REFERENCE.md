# Farming Skill Quick Reference

## 🌾 System Overview
Three subskills following Mining pattern:
1. **Harvesting** - Breaking crops (always accessible)
2. **Cultivating** - Planting crops (requires Harvesting 10)
3. **Botany** - Advanced mechanics (stub for later)

## 🎮 Player Flow

```
START
  ↓
Harvest Wheat (default unlocked)
  ↓ (gain Harvesting XP)
Unlock more crops in Harvesting tree
  ↓
Reach Harvesting Level 10
  ↓
Unlock Cultivating subskill
  ↓
Plant Wheat Seeds (default once Cultivating unlocked)
  ↓ (gain Cultivating XP)
Unlock more crops in Cultivating tree
  ↓
Master both subskills!
```

## 📋 Crop List

| Crop | Harvest XP | Plant XP | Notes |
|------|-----------|----------|-------|
| Wheat | 5.0 | 2.0 | Default unlocked |
| Carrots | 6.0 | 2.5 | Requires unlock |
| Potatoes | 6.0 | 2.5 | Requires unlock |
| Beetroots | 7.0 | 3.0 | Requires unlock |
| Sweet Berries | 4.0 | 2.0 | Requires unlock |
| Cocoa | 8.0 | 3.0 | Requires unlock |
| Nether Wart | 10.0 | 4.0 | Requires unlock |
| Melons | 3.0 | 1.5 | Requires unlock |
| Pumpkins | 8.0 | 3.0 | Requires unlock |

## 🔑 Key Methods

### FarmingSkill (Main)
```java
canHarvestCrop(Player, Material) → boolean
canPlantCrop(Player, Material) → boolean
applyNodeUpgrade(Player, String, int, int)
handleSkillTreeReset(Player, Map<String, Integer>)
```

### HarvestingSubskill
```java
canHarvestCrop(Player, Material) → boolean
canGainXpFromCrop(Player, Material) → boolean
affectsCrop(Material) → boolean
getBonusDropChance(int) → double
getFarmingFortuneFromSkillTree(Player) → double
```

### CultivatingSubskill
```java
canPlantCrop(Player, Material) → boolean
isCultivatingUnlocked(Player) → boolean
canGainXpFromPlanting(Player, Material) → boolean
getCropGrowthSpeedMultiplier(int) → double
```

## 🎯 Skill Tree Nodes (To Implement)

### Main Farming Tree
- `farming_fortune` - +0.5 fortune per level
- `farming_speed` - +1% speed per level
- `unlock_cultivating` - Unlocks Cultivating subskill

### Harvesting Tree
- `unlock_carrots` - Unlock carrot harvesting
- `unlock_potatoes` - Unlock potato harvesting
- `unlock_beetroots` - Unlock beetroot harvesting
- `unlock_sweet_berries` - Unlock sweet berry harvesting
- `unlock_cocoa` - Unlock cocoa harvesting
- `unlock_nether_wart` - Unlock nether wart harvesting
- `unlock_melons` - Unlock melon harvesting
- `unlock_pumpkins` - Unlock pumpkin harvesting
- `farming_fortune` - Fortune bonus
- `harvest_speed` - Harvest speed bonus
- `harvesting_xp` - XP boost

### Cultivating Tree
- `unlock_plant_carrots` - Unlock carrot planting
- `unlock_plant_potatoes` - Unlock potato planting
- `unlock_plant_beetroots` - Unlock beetroot planting
- `unlock_plant_sweet_berries` - Unlock sweet berry planting
- `unlock_plant_cocoa` - Unlock cocoa planting
- `unlock_plant_nether_wart` - Unlock nether wart planting
- `unlock_plant_melons` - Unlock melon planting
- `unlock_plant_pumpkins` - Unlock pumpkin planting
- `growth_speed` - Crop growth speed bonus
- `cultivating_xp` - XP boost
- `auto_replant` - Auto-replant chance

## 🧪 Admin Commands for Testing

```bash
# Give XP to test leveling
/admin skill <player> harvesting addxp <amount>
/admin skill <player> cultivating addxp <amount>

# Set level directly
/admin skill <player> harvesting setlevel <level>

# Unlock node (if command exists)
/admin skilltree <player> harvesting unlock <nodeId>
```

## 🐛 Debug Commands

```bash
# Enable debug mode
/debug skills enable

# Check in logs
[SKILLS] Processing block break: WHEAT by PlayerName
[SKILLS] PlayerName gained 5.0 XP in Harvesting for harvesting WHEAT
```

## 💾 File Locations

```
📁 MMO/src/main/java/com/server/
├── 📁 profiles/skills/
│   ├── 📁 events/
│   │   └── 📄 FarmingListener.java ⭐ NEW
│   └── 📁 skills/farming/
│       ├── 📄 FarmingSkill.java ⭐ UPDATED
│       └── 📁 subskills/
│           ├── 📄 HarvestingSubskill.java ⭐ NEW
│           ├── 📄 CultivatingSubskill.java ⭐ NEW
│           └── 📄 BotanySubskill.java ⭐ NEW (stub)
└── 📄 Main.java ⭐ UPDATED (listener registration)
```

## 🔍 Common Issues & Solutions

### Issue: "Can't harvest crop even though unlocked"
**Solution**: Check if crop is fully grown - immature crops don't give XP

### Issue: "Cultivating won't unlock"
**Solution**: Must reach Harvesting level 10 first

### Issue: "Fortune not working"
**Solution**: Need at least 100 farming fortune for 2x drops

### Issue: "Planting not giving XP"
**Solution**: Check Cultivating is unlocked AND crop is unlocked in tree

### Issue: "Reset skill tree but crops still unlocked"
**Solution**: Check `handleSkillTreeReset()` is being called properly

## 📊 Progression Timeline (Example)

| Harvesting Level | Milestone |
|-----------------|-----------|
| 1 | Start harvesting wheat |
| 5 | Unlock carrots/potatoes |
| 10 | **Cultivating unlocks** |
| 15 | Unlock beetroots |
| 20 | Unlock cocoa/sweet berries |
| 25 | Unlock nether wart |
| 30 | Unlock melons/pumpkins |
| 50+ | Master farmer |

## 🎨 User Messages

### Success Messages
- "Farming Fortune increased by +0.5 (Total: 2.5)"
- "You can now harvest Carrots!"
- "You have unlocked the Cultivating subskill!"

### Error Messages
- "You need to unlock the ability to harvest Carrots first!"
- "Check your Harvesting skill tree to unlock this crop."
- "You need to reach Harvesting level 10 to unlock Cultivating!"
- "Keep harvesting wheat to level up your Harvesting skill."

## 🚀 Next Implementation Priority

1. **Skill Tree JSONs** (Highest Priority)
   - Define node positions
   - Set unlock costs
   - Configure requirements

2. **GUI Implementation** (High Priority)
   - Skill tree visualization
   - Node upgrade interface
   - Progress displays

3. **Growth Speed System** (Medium Priority)
   - Scheduled task or event modification
   - Player proximity detection
   - Bonemeal effect application

4. **Advanced Features** (Low Priority)
   - Botany mechanics
   - Auto-replant system
   - Custom crop types

---

**Status**: ✅ Core functionality complete and tested
**Build**: ✅ SUCCESS - All code compiles
**Ready For**: Skill tree configuration and GUI development
