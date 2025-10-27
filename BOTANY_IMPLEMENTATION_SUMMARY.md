# Botany Subskill Implementation Summary

## ✅ Completed Implementation

### Core Systems Created

1. **CustomCrop.java** - Complete crop data model
   - Growth stages with custom model data
   - Rarity system (Common to Mythic)
   - Breeding recipes
   - XP rewards (plant, harvest, breed)
   - Seed and drop item creation
   - Full customization support

2. **CustomCropRegistry.java** - Central crop management
   - 6 default custom crops implemented
   - Lookup by ID or custom model data
   - Breeding recipe matching system
   - Progression tree: Golden Wheat → Crimson Carrot → Ender Berry → Crystal Melon → Celestial Potato → Void Pumpkin
   - Easy to extend with new crops

3. **PlantedCustomCrop.java** - World crop tracking
   - Tripwire block for collision
   - ItemDisplay entity for visuals
   - Growth stage management
   - Auto-growth system
   - Visual updates per stage
   - Serialization for persistence

4. **CropBreeder.java** - Multiblock breeding structure
   - 3x3 farmland + center composter design
   - Structure validation
   - Breeding process with timer
   - Visual feedback (floating parent crop displays)
   - Success chance system
   - Level requirements
   - Particle effects and sounds

5. **BOTANY_SYSTEM_GUIDE.md** - Complete documentation
   - Resource pack setup instructions
   - JSON model examples
   - Texture creation guide
   - Crop progression tree
   - Breeding mechanics explained
   - Custom model data reference (100001-100058 range)
   - Future feature ideas

---

## 🔨 What You Need to Do Next

### 1. Resource Pack Creation
Follow the guide in `BOTANY_SYSTEM_GUIDE.md` to create:
- Modify `wheat_seeds.json` with custom model data overrides
- Create individual crop model JSONs (30+ files)
- Create crop textures (30+ PNG files)
- Test in-game

**Quick Start Template** (for testing):
Use placeholder textures initially - copy wheat/carrot textures and tint them different colors:
- Golden Wheat = yellow tint
- Crimson Carrot = red tint
- Ender Berry = purple tint
- Crystal Melon = cyan tint
- Celestial Potato = white/glowing
- Void Pumpkin = dark purple/black

### 2. Complete BotanySubskill Integration

#### A. Update BotanySubskill.java
The stub exists, but you need to add:
```java
- Methods to check player Botany level
- XP gain integration
- Skill tree nodes for:
  * Unlock breeding recipes
  * Faster growth speed
  * Higher seed drop chances
  * Breeding success rate bonuses
- Rewards (farming fortune, growth speed boosts, etc.)
```

#### B. Create BotanyListener.java
Handle player interactions:
```java
- PlayerInteractEvent: Plant custom crops on farmland
- BlockBreakEvent: Harvest custom crops, give drops + XP
- PlayerInteractEvent: Right-click breeder with seeds to start breeding
```

#### C. Create BotanyManager.java
Background systems:
```java
- Growth tick task (runs every X seconds)
- Iterate all planted crops, call tryGrow()
- Save/load planted crop data to MongoDB
- Track breeder structures
- Update breeding progress
```

#### D. Register with SkillRegistry
Add Botany to the skill system so it shows in GUIs and gains XP properly.

### 3. Create Command Interface

Create `/botany` command for:
- `/botany give <player> <cropId>` - Give custom crop seeds
- `/botany info` - Show planted crops and breeders nearby
- `/botany reload` - Reload crop registry
- `/botany breed <crop1> <crop2>` - Manual breeding test

### 4. Skill Tree Integration

Add Botany nodes to farming skill tree:
- **Early Nodes** (Level 1-20):
  * Unlock Golden Wheat breeding
  * +5% growth speed
  * +2% seed drop chance
  
- **Mid Nodes** (Level 20-50):
  * Unlock Crimson Carrot, Ender Berry, Crystal Melon
  * +10% growth speed
  * +5% seed drop chance
  * +10% breeding success rate
  
- **Late Nodes** (Level 50-100):
  * Unlock Celestial Potato, Void Pumpkin
  * +20% growth speed
  * +10% seed drop chance
  * +25% breeding success rate
  * Unlock crop mutations

### 5. Testing Checklist

Once implemented:
- [ ] Plant Golden Wheat seed on farmland
- [ ] Wait for growth through all stages
- [ ] Harvest and receive drops + XP
- [ ] Build breeder structure
- [ ] Place two seeds in breeder
- [ ] Wait for breeding completion
- [ ] Receive new crop seed
- [ ] Verify XP awards correctly
- [ ] Test with Bedrock players (tripwire + display entities)
- [ ] Verify persistence (restart server, crops still there)

---

## 🎮 Player Experience Flow

1. **Discovery**: Player unlocks Botany at Farming level 20 (or via skill tree)
2. **First Crop**: Given starter Golden Wheat seed or finds via quest/reward
3. **Plant & Grow**: Places on farmland, watches ItemDisplay entity show growth
4. **Harvest**: Breaks crop, gets wheat + potential seed back + XP
5. **Build Breeder**: Creates 3x3 farmland + composter structure
6. **First Breed**: Combines two Golden Wheat seeds → 60% chance Crimson Carrot
7. **Progression**: Works up rarity ladder, each requiring higher Botany level
8. **Mastery**: Level 75+ players can breed Void Pumpkins (Mythic tier)

---

## 🔧 Technical Notes

### Block State Choice: Tripwire
- **Pros**: 
  * Invisible block with collision
  * Doesn't require block states (unlimited crops possible)
  * No lighting updates
- **Cons**:
  * Players can see in F3 debug
  * Can be triggered by entities walking over (disable in listener)

### Visual System: ItemDisplay
- **Pros**:
  * Full control over positioning, rotation, scale
  * Supports any item model
  * Smooth updates
- **Cons**:
  * Entity overhead (mitigated by chunk unloading)
  * Requires 1.19.4+ 

### Growth System
- Passive growth every X seconds (configurable)
- NOT tick-based (too expensive)
- Growth stored as timestamp + stage
- Check elapsed time vs required time per stage

### Breeding System
- Not instant (timed process)
- Visual feedback during breeding
- Success based on recipe chance + Botany level
- Can fail if level too low

---

## 📈 Balancing Considerations

### Growth Times
Current values are placeholders. Adjust based on playtesting:
- Common: 2 minutes (fast, starter crop)
- Uncommon: 3 minutes
- Rare: 4 minutes
- Epic: 6 minutes
- Legendary: 9 minutes
- Mythic: 13 minutes

### XP Values
Scale with rarity. Breeding gives most XP (reward for effort):
- Planting: 10-300 XP
- Harvesting: 15-500 XP  
- Breeding: 25-1000 XP

### Seed Drop Rates
Lower for rarer crops (makes breeding essential):
- Common: 15%
- Uncommon: 12%
- Rare: 10%
- Epic: 8%
- Legendary: 5%
- Mythic: 2%

### Breeding Success Rates
Adjust to control progression speed:
- Common → Uncommon: 60%
- Uncommon → Rare: 40%
- Rare → Epic: 30%
- Epic → Legendary: 20%
- Legendary → Mythic: 10%

Players can boost these with Botany skill tree nodes!

---

## 🚀 Future Expansion Ideas

### More Crops
- Elemental crops (fire, water, earth, air themed)
- Seasonal crops (only plantable in certain months)
- Mob drops as "seeds" (zombie flesh → undead wheat)

### Advanced Mechanics
- **Crop Quality**: Roll quality on harvest (affects drops/XP)
- **Fertilizers**: Craftable items to boost growth/quality
- **Diseases**: Random chance for crops to get sick (need cure)
- **Greenhouse**: Multiblock for faster growth indoors
- **Auto-Harvester**: Multiblock that auto-harvests when mature
- **Sprinkler**: Multiblock that waters crops in radius

### Integration
- **Alchemy**: Custom crops as potion ingredients
- **Cooking**: Custom crops for unique recipes
- **Quests**: "Breed 10 Ender Berries" objectives
- **Achievements**: "Master Botanist" for breeding all crops

---

## 📚 Code Structure

```
com.server.profiles.skills.skills.farming.botany/
├── CustomCrop.java            ✅ Complete
├── CustomCropRegistry.java    ✅ Complete
├── PlantedCustomCrop.java     ✅ Complete
├── CropBreeder.java           ✅ Complete
├── BotanyListener.java        ⏳ TODO
├── BotanyManager.java         ⏳ TODO
└── BotanyCommand.java         ⏳ TODO

BOTANY_SYSTEM_GUIDE.md         ✅ Complete (resource pack guide)
```

---

## ✨ Summary

**What's Done:**
- Complete data model for custom crops
- Registry system with 6 progression tiers
- In-world crop tracking with tripwire + ItemDisplay
- Full breeding mechanics with multiblock structure
- Comprehensive documentation for resource pack creation

**What's Next:**
1. Create resource pack textures
2. Implement event listeners for planting/harvesting
3. Add growth tick manager
4. Integrate with Botany skill XP system
5. Add skill tree nodes
6. Test and balance

**Estimated Time to Complete:**
- Resource Pack (basic): 2-4 hours
- Listeners + Manager: 2-3 hours  
- Testing + Balance: 1-2 hours
- **Total: ~7 hours for MVP**

The foundation is solid and follows the Custom-Crops approach perfectly. The system is modular, extensible, and ready for content expansion!
