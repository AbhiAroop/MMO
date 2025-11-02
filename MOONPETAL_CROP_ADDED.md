# Moonpetal Custom Crop - Implementation Complete ✅

## 📋 Summary

Added **Moonpetal** as a new custom crop to the Botany system.

---

## 🌸 Moonpetal Details

| Property | Value |
|----------|-------|
| **ID** | `moonpetal` |
| **Display Name** | Moonpetal |
| **Rarity** | UNCOMMON (§a) |
| **Growth Stages** | 4 stages |
| **Growth Time** | 40 seconds per stage (160s / 2.67 min total) |
| **Required Level** | Botany Level 5 |
| **Base Drop** | Moonpetal (custom item) |
| **Drop Amount** | 3-5 per harvest |
| **Seed Drop Chance** | 12% |

---

## 📊 XP Rewards

| Action | XP Gained |
|--------|-----------|
| **Planting** | +20.0 XP |
| **Harvesting** | +30.0 XP |
| **Breeding** | +50.0 XP |

---

## 🧬 Breeding Recipe

**Parents:** Golden Wheat + Golden Wheat  
**Success Chance:** 60%  
**Required Level:** Botany Level 5

This gives players an alternative to breeding Crimson Carrot, providing more variety at the uncommon tier.

---

## 🎨 Custom Model Data Ranges

| Item/Stage | Custom Model Data | Texture File |
|------------|-------------------|--------------|
| **Seed** | 100060 | `moonpetal_seeds.png` |
| **Stage 1** | 100061 | `moonpetal_stage_1.png` |
| **Stage 2** | 100062 | `moonpetal_stage_2.png` |
| **Stage 3** | 100063 | `moonpetal_stage_3.png` |
| **Stage 4 (Fully Grown)** | 100064 | `moonpetal_stage_4.png` |
| **Harvested Item** | 100065 | `moonpetal.png` |

**CMD Range Reserved:** 100060-100065 (6 total)

---

## 📝 Changes Made

### 1. CustomCropRegistry.java
**Location:** `src/main/java/com/server/profiles/skills/skills/farming/botany/CustomCropRegistry.java`

**Added after Crimson Carrot (line ~160):**
```java
// Moonpetal - Mystical flower that blooms under moonlight
// Create custom moonpetal drop item
ItemStack moonpetalDrop = new ItemStack(Material.ALLIUM, 1);
org.bukkit.inventory.meta.ItemMeta moonpetalMeta = moonpetalDrop.getItemMeta();
if (moonpetalMeta != null) {
    moonpetalMeta.setDisplayName("§aMoonpetal");
    moonpetalMeta.setCustomModelData(100065); // moonpetal.png
    moonpetalDrop.setItemMeta(moonpetalMeta);
}

CustomCrop moonpetal = new CustomCrop(
    "moonpetal",
    "Moonpetal",
    CropRarity.UNCOMMON,
    4,
    800L, // 40 seconds per stage
    new int[]{100060, 100061, 100062, 100063, 100064}, // seed + 4 growth stages
    "crops/moonpetal",
    5, // Requires Botany level 5
    moonpetalDrop, // Custom drop item with CMD 100065
    3, 5,
    0.12,
    20.0,
    30.0,
    50.0
);
moonpetal.addBreedingRecipe(new BreedingRecipe(
    "golden_wheat", "golden_wheat", 0.60, 5
));
registerCrop(moonpetal);
```

### 2. BotanyCommand.java - Tab Completion
**Location:** `src/main/java/com/server/commands/BotanyCommand.java` (line ~383)

**Added `moonpetal` to tab completion:**
```java
completions.add("golden_wheat");
completions.add("crimson_carrot");
completions.add("moonpetal");  // NEW
completions.add("ender_berry");
completions.add("crystal_melon");
completions.add("celestial_potato");
completions.add("void_pumpkin");
```

### 3. BotanyCommand.java - Help Message
**Location:** `src/main/java/com/server/commands/BotanyCommand.java` (line ~122)

**Updated error message to include moonpetal:**
```java
player.sendMessage("§7Valid IDs: golden_wheat, crimson_carrot, moonpetal, ender_berry, crystal_melon, celestial_potato, void_pumpkin");
```

---

## 🎮 How to Use

### Getting Moonpetal Seeds

**Command:**
```
/botany give <player> moonpetal [amount]
```

**Examples:**
- `/botany give Notch moonpetal` - Give 1 moonpetal seed
- `/botany give Notch moonpetal 16` - Give 16 moonpetal seeds

### Breeding Moonpetal

1. Build a Crop Breeder (3x3 farmland + center composter)
2. Hold 2 Golden Wheat seeds
3. Shift-right-click the composter with first seed
4. Normal-right-click the composter with second seed
5. Wait for breeding progress (60% success chance)
6. Collect Moonpetal seed when complete!

### Planting & Harvesting

1. **Planting:**
   - Hold Moonpetal seed
   - Right-click on farmland
   - Requires Botany Level 5
   - Grants +20 XP

2. **Growing:**
   - 4 growth stages
   - 40 seconds per stage
   - 160 seconds (2.67 min) total
   - Faster near water (hydrated farmland)

3. **Harvesting:**
   - Break fully grown crop
   - Drops 3-5 Moonpetal items (custom item with CMD 100065)
   - 12% chance to drop seed
   - Grants +30 XP

---

## 📦 Resource Pack Integration

### Required Files

You need to add these files to your resource pack:

#### 1. Texture Files
**Location:** `assets/minecraft/textures/custom_crops/`

- `moonpetal_seeds.png` (seed item - CMD 100060)
- `moonpetal_stage_1.png` (growth stage 1 - CMD 100061)
- `moonpetal_stage_2.png` (growth stage 2 - CMD 100062)
- `moonpetal_stage_3.png` (growth stage 3 - CMD 100063)
- `moonpetal_stage_4.png` (growth stage 4/fully grown - CMD 100064)
- `moonpetal.png` (harvested item - CMD 100065)

**Note:** Based on your screenshot, you already have these texture files!

#### 2. Model Files
**Location:** `assets/minecraft/models/item/custom_crops/`

Create 6 model JSON files (seed + 4 growth stages + harvested item):

**Example: `moonpetal_seeds.json` (for the seed)**
```json
{
  "credit": "Botany System - Moonpetal",
  "textures": {
    "crop": "minecraft:custom_crops/moonpetal_seeds",
    "particle": "minecraft:custom_crops/moonpetal_seeds"
  },
  "elements": [
    {
      "from": [0, 0, 8],
      "to": [16, 16, 8],
      "faces": {
        "north": {"uv": [0, 0, 16, 16], "texture": "#crop"},
        "south": {"uv": [0, 0, 16, 16], "texture": "#crop"}
      }
    },
    {
      "from": [8, 0, 0],
      "to": [8, 16, 16],
      "faces": {
        "west": {"uv": [0, 0, 16, 16], "texture": "#crop"},
        "east": {"uv": [0, 0, 16, 16], "texture": "#crop"}
      }
    }
  ],
  "display": {
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.40, 0.40, 0.40]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.40, 0.40, 0.40]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "head": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]}
  }
}
```

**Example: `moonpetal_stage_1.json` (for growth stages)**
```json
{
  "credit": "Botany System - Moonpetal",
  "textures": {
    "crop": "minecraft:custom_crops/moonpetal_stage_1",
    "particle": "minecraft:custom_crops/moonpetal_stage_1"
  },
  "elements": [
    {
      "from": [0, 0, 8],
      "to": [16, 16, 8],
      "faces": {
        "north": {"uv": [0, 0, 16, 16], "texture": "#crop"},
        "south": {"uv": [0, 0, 16, 16], "texture": "#crop"}
      }
    },
    {
      "from": [8, 0, 0],
      "to": [8, 16, 16],
      "faces": {
        "west": {"uv": [0, 0, 16, 16], "texture": "#crop"},
        "east": {"uv": [0, 0, 16, 16], "texture": "#crop"}
      }
    }
  ],
  "display": {
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.40, 0.40, 0.40]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.40, 0.40, 0.40]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "head": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]}
  }
}
```

**Create similar files for:**
- `moonpetal_seeds.json` → CMD 100060 (texture: `moonpetal_seeds`)
- `moonpetal_stage_1.json` → CMD 100061 (texture: `moonpetal_stage_1`)
- `moonpetal_stage_2.json` → CMD 100062 (texture: `moonpetal_stage_2`)
- `moonpetal_stage_3.json` → CMD 100063 (texture: `moonpetal_stage_3`)
- `moonpetal_stage_4.json` → CMD 100064 (texture: `moonpetal_stage_4`)
- `moonpetal.json` → CMD 100065 (texture: `moonpetal`) - **For harvested item**

#### 3. Update wheat_seeds.json
**Location:** `assets/minecraft/models/item/wheat_seeds.json`

Add these lines to the `overrides` array:
```json
{ "predicate": { "custom_model_data": 100060 }, "model": "minecraft:item/custom_crops/moonpetal_seeds" },
{ "predicate": { "custom_model_data": 100061 }, "model": "minecraft:item/custom_crops/moonpetal_stage_1" },
{ "predicate": { "custom_model_data": 100062 }, "model": "minecraft:item/custom_crops/moonpetal_stage_2" },
{ "predicate": { "custom_model_data": 100063 }, "model": "minecraft:item/custom_crops/moonpetal_stage_3" },
{ "predicate": { "custom_model_data": 100064 }, "model": "minecraft:item/custom_crops/moonpetal_stage_4" },
```

#### 4. Update allium.json (for harvested item)
**Location:** `assets/minecraft/models/item/allium.json`

Add this line to the `overrides` array:
```json
{ "predicate": { "custom_model_data": 100065 }, "model": "minecraft:item/custom_crops/moonpetal" }
```

---

## 🧪 Testing Checklist

- [ ] Use `/botany give <player> moonpetal` command
- [ ] Verify seed has correct name "§aMoonpetal Seed"
- [ ] Verify seed shows UNCOMMON rarity in lore
- [ ] Plant seed on farmland (requires level 5)
- [ ] Watch crop grow through 4 stages
- [ ] Each stage should show different visual (custom model)
- [ ] Harvest fully grown crop
- [ ] Verify drops 3-5 Moonpetal items (custom item with green name)
- [ ] Verify moonpetal item has custom model (CMD 100065)
- [ ] Verify 12% seed drop chance
- [ ] Verify planting gives +20 XP
- [ ] Verify harvesting gives +30 XP
- [ ] Test breeding: 2x Golden Wheat → Moonpetal (60% success)
- [ ] Verify breeding gives +50 XP

---

## 🌳 Crop Progression Tree

```
Golden Wheat (Common, Lv1)
├── Crimson Carrot (Uncommon, Lv5) [60% success]
└── Moonpetal (Uncommon, Lv5) [60% success] ← NEW!

Crimson Carrot x Crimson Carrot
└── Ender Berry (Rare, Lv15) [40% success]

And so on...
```

Moonpetal provides an **alternative path** at the uncommon tier, giving players more variety and choices in their botany progression.

---

## 💡 Design Notes

### Why These Stats?

1. **UNCOMMON Rarity:** Same tier as Crimson Carrot to provide variety
2. **Same Breeding Recipe:** Makes both accessible from Golden Wheat
3. **Custom Drop Item:** Moonpetal with custom model (based on Allium material)
4. **4 Growth Stages:** Matches other uncommon crops for consistency
5. **CMD 100060-100065:** Complete range for seed, 4 stages, and harvested item (6 total)

### Balance Considerations

- **Growth Time (800L):** Same as Crimson Carrot for fair comparison
- **Drop Amount (3-5):** Standard for uncommon crops
- **Seed Chance (12%):** Balanced for sustainability
- **XP Rewards:** Matches Crimson Carrot exactly
- **Level Requirement (5):** Accessible early but not immediately

---

## 📚 Related Documentation

- `BOTANY_SYSTEM_GUIDE.md` - Full resource pack setup guide
- `BOTANY_COMPLETE.md` - Complete implementation documentation
- `BOTANY_IMPLEMENTATION_SUMMARY.md` - Technical implementation details

---

## ✅ Status

**Implementation:** ✅ COMPLETE  
**Code Changes:** ✅ COMPLETE  
**Resource Pack:** ⏳ PENDING (see section above)  
**Testing:** ⏳ PENDING (see checklist above)

---

## 🚀 Next Steps

1. **Build the project:**
   ```
   mvn clean package -DskipTests
   ```

2. **Copy plugin to server:**
   ```
   cp target/mmo-0.0.1.jar "AI Paper Server/plugins/"
   ```

3. **Create resource pack files** (see Resource Pack Integration section)

4. **Test in-game** (use testing checklist)

5. **Push to Git:**
   ```
   git add .
   git commit -m "Add moonpetal custom crop to botany system"
   git push
   ```

---

**Happy Growing! 🌸**
