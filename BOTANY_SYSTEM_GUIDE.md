# Botany Subskill - Custom Crops System

## Overview
The Botany subskill introduces custom crops that players can discover through breeding. The system uses **tripwire block states** for collision detection and **ItemDisplay entities** with custom model data for visual representation, inspired by the Custom-Crops plugin.

---

## 📦 Resource Pack Setup - Complete Tutorial

### Overview
The Botany system uses **Custom Model Data** on wheat seeds to display different crop models. You'll need to create:
- 58 crop model files (JSON)
- 58 crop texture files (PNG)
- 1 modified wheat_seeds.json file

**Estimated Time:** 2-3 hours for all textures (or use AI image generation tools!)

---

### 🗂️ Required Files Structure
```
YourResourcePack/
├── pack.mcmeta
├── pack.png (optional icon)
└── assets/
    └── minecraft/
        ├── models/
        │   └── item/
        │       ├── wheat_seeds.json (MODIFIED - adds overrides)
        │       └── custom_crops/
        │           ├── golden_wheat_0.json      (4 stages)
        │           ├── golden_wheat_1.json
        │           ├── golden_wheat_2.json
        │           ├── golden_wheat_3.json
        │           ├── crimson_carrot_0.json    (4 stages)
        │           ├── crimson_carrot_1.json
        │           ├── crimson_carrot_2.json
        │           ├── crimson_carrot_3.json
        │           ├── ender_berry_0.json       (5 stages)
        │           ├── ender_berry_1.json
        │           ├── ender_berry_2.json
        │           ├── ender_berry_3.json
        │           ├── ender_berry_4.json
        │           ├── crystal_melon_0.json     (6 stages)
        │           ├── crystal_melon_1.json
        │           ├── crystal_melon_2.json
        │           ├── crystal_melon_3.json
        │           ├── crystal_melon_4.json
        │           ├── crystal_melon_5.json
        │           ├── celestial_potato_0.json  (7 stages)
        │           ├── celestial_potato_1.json
        │           ├── celestial_potato_2.json
        │           ├── celestial_potato_3.json
        │           ├── celestial_potato_4.json
        │           ├── celestial_potato_5.json
        │           ├── celestial_potato_6.json
        │           ├── void_pumpkin_0.json      (8 stages)
        │           ├── void_pumpkin_1.json
        │           ├── void_pumpkin_2.json
        │           ├── void_pumpkin_3.json
        │           ├── void_pumpkin_4.json
        │           ├── void_pumpkin_5.json
        │           ├── void_pumpkin_6.json
        │           └── void_pumpkin_7.json
        └── textures/
            └── custom_crops/
                ├── golden_wheat_0.png      (16x16)
                ├── golden_wheat_1.png
                ├── golden_wheat_2.png
                ├── golden_wheat_3.png
                ├── crimson_carrot_0.png
                ├── crimson_carrot_1.png
                ├── crimson_carrot_2.png
                ├── crimson_carrot_3.png
                ├── ender_berry_0.png
                ├── ender_berry_1.png
                ├── ender_berry_2.png
                ├── ender_berry_3.png
                ├── ender_berry_4.png
                ├── crystal_melon_0.png
                ├── crystal_melon_1.png
                ├── crystal_melon_2.png
                ├── crystal_melon_3.png
                ├── crystal_melon_4.png
                ├── crystal_melon_5.png
                ├── celestial_potato_0.png
                ├── celestial_potato_1.png
                ├── celestial_potato_2.png
                ├── celestial_potato_3.png
                ├── celestial_potato_4.png
                ├── celestial_potato_5.png
                ├── celestial_potato_6.png
                ├── void_pumpkin_0.png
                ├── void_pumpkin_1.png
                ├── void_pumpkin_2.png
                ├── void_pumpkin_3.png
                ├── void_pumpkin_4.png
                ├── void_pumpkin_5.png
                ├── void_pumpkin_6.png
                └── void_pumpkin_7.png

Total: 58 model files + 58 texture files + 1 modified file = 117 files
```

---

### 📝 Step-by-Step Guide

#### Step 0: Create pack.mcmeta
Create this file in the root of your resource pack folder:

```json
{
  "pack": {
    "pack_format": 34,
    "description": "§6MMO Botany §7- Custom Crops"
  }
}
```

**Note:** Pack format 34 is for Minecraft 1.21.x. Adjust if needed.

### Step 1: Update wheat_seeds.json
Create/modify `assets/minecraft/models/item/wheat_seeds.json`:

This file tells Minecraft to use different models based on custom model data values.

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "minecraft:item/wheat_seeds"
  },
  "overrides": [
    { "predicate": { "custom_model_data": 100001 }, "model": "minecraft:item/custom_crops/golden_wheat_0" },
    { "predicate": { "custom_model_data": 100002 }, "model": "minecraft:item/custom_crops/golden_wheat_1" },
    { "predicate": { "custom_model_data": 100003 }, "model": "minecraft:item/custom_crops/golden_wheat_2" },
    { "predicate": { "custom_model_data": 100004 }, "model": "minecraft:item/custom_crops/golden_wheat_3" },
    
    { "predicate": { "custom_model_data": 100011 }, "model": "minecraft:item/custom_crops/crimson_carrot_0" },
    { "predicate": { "custom_model_data": 100012 }, "model": "minecraft:item/custom_crops/crimson_carrot_1" },
    { "predicate": { "custom_model_data": 100013 }, "model": "minecraft:item/custom_crops/crimson_carrot_2" },
    { "predicate": { "custom_model_data": 100014 }, "model": "minecraft:item/custom_crops/crimson_carrot_3" },
    
    { "predicate": { "custom_model_data": 100021 }, "model": "minecraft:item/custom_crops/ender_berry_0" },
    { "predicate": { "custom_model_data": 100022 }, "model": "minecraft:item/custom_crops/ender_berry_1" },
    { "predicate": { "custom_model_data": 100023 }, "model": "minecraft:item/custom_crops/ender_berry_2" },
    { "predicate": { "custom_model_data": 100024 }, "model": "minecraft:item/custom_crops/ender_berry_3" },
    { "predicate": { "custom_model_data": 100025 }, "model": "minecraft:item/custom_crops/ender_berry_4" },
    
    { "predicate": { "custom_model_data": 100031 }, "model": "minecraft:item/custom_crops/crystal_melon_0" },
    { "predicate": { "custom_model_data": 100032 }, "model": "minecraft:item/custom_crops/crystal_melon_1" },
    { "predicate": { "custom_model_data": 100033 }, "model": "minecraft:item/custom_crops/crystal_melon_2" },
    { "predicate": { "custom_model_data": 100034 }, "model": "minecraft:item/custom_crops/crystal_melon_3" },
    { "predicate": { "custom_model_data": 100035 }, "model": "minecraft:item/custom_crops/crystal_melon_4" },
    { "predicate": { "custom_model_data": 100036 }, "model": "minecraft:item/custom_crops/crystal_melon_5" },
    
    { "predicate": { "custom_model_data": 100041 }, "model": "minecraft:item/custom_crops/celestial_potato_0" },
    { "predicate": { "custom_model_data": 100042 }, "model": "minecraft:item/custom_crops/celestial_potato_1" },
    { "predicate": { "custom_model_data": 100043 }, "model": "minecraft:item/custom_crops/celestial_potato_2" },
    { "predicate": { "custom_model_data": 100044 }, "model": "minecraft:item/custom_crops/celestial_potato_3" },
    { "predicate": { "custom_model_data": 100045 }, "model": "minecraft:item/custom_crops/celestial_potato_4" },
    { "predicate": { "custom_model_data": 100046 }, "model": "minecraft:item/custom_crops/celestial_potato_5" },
    { "predicate": { "custom_model_data": 100047 }, "model": "minecraft:item/custom_crops/celestial_potato_6" },
    
    { "predicate": { "custom_model_data": 100051 }, "model": "minecraft:item/custom_crops/void_pumpkin_0" },
    { "predicate": { "custom_model_data": 100052 }, "model": "minecraft:item/custom_crops/void_pumpkin_1" },
    { "predicate": { "custom_model_data": 100053 }, "model": "minecraft:item/custom_crops/void_pumpkin_2" },
    { "predicate": { "custom_model_data": 100054 }, "model": "minecraft:item/custom_crops/void_pumpkin_3" },
    { "predicate": { "custom_model_data": 100055 }, "model": "minecraft:item/custom_crops/void_pumpkin_4" },
    { "predicate": { "custom_model_data": 100056 }, "model": "minecraft:item/custom_crops/void_pumpkin_5" },
    { "predicate": { "custom_model_data": 100057 }, "model": "minecraft:item/custom_crops/void_pumpkin_6" },
    { "predicate": { "custom_model_data": 100058 }, "model": "minecraft:item/custom_crops/void_pumpkin_7" }
  ]
}
```

---

### Step 2: Create Individual Crop Models
Each crop stage needs a model file. You have two options:

#### Option A: Simple 2D Model (Easiest)
**Example:** `assets/minecraft/models/item/custom_crops/golden_wheat_0.json`

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "minecraft:custom_crops/golden_wheat_0"
  }
}
```

This creates a flat 2D sprite (like vanilla seeds). Simple but less immersive.

#### Option B: 3D Crop Model (Recommended, Custom-Crops Style)
**Example:** `assets/minecraft/models/item/custom_crops/golden_wheat_0.json`

```json
{
  "credit": "Botany System",
  "textures": {
    "crop": "minecraft:custom_crops/golden_wheat_0",
    "particle": "minecraft:custom_crops/golden_wheat_0"
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
        "east": {"uv": [0, 0, 16, 16], "texture": "#crop"},
        "west": {"uv": [0, 0, 16, 16], "texture": "#crop"}
      }
    }
  ],
  "display": {
    "thirdperson_righthand": {
      "rotation": [75, 45, 0],
      "translation": [0, 2.5, 0],
      "scale": [0.375, 0.375, 0.375]
    },
    "thirdperson_lefthand": {
      "rotation": [75, 45, 0],
      "translation": [0, 2.5, 0],
      "scale": [0.375, 0.375, 0.375]
    },
    "firstperson_righthand": {
      "rotation": [0, 45, 0],
      "translation": [0, 0, 0],
      "scale": [0.4, 0.4, 0.4]
    },
    "firstperson_lefthand": {
      "rotation": [0, 225, 0],
      "translation": [0, 0, 0],
      "scale": [0.4, 0.4, 0.4]
    },
    "ground": {
      "rotation": [0, 0, 0],
      "translation": [0, 3, 0],
      "scale": [0.25, 0.25, 0.25]
    },
    "gui": {
      "rotation": [30, 225, 0],
      "translation": [0, 0, 0],
      "scale": [0.625, 0.625, 0.625]
    },
    "fixed": {
      "rotation": [0, 0, 0],
      "translation": [0, 0, 0],
      "scale": [0.5, 0.5, 0.5]
    },
    "head": {
      "rotation": [0, 0, 0],
      "translation": [0, 0, 0],
      "scale": [1, 1, 1]
    }
  }
}
```

This creates two intersecting planes forming an "X" shape (like vanilla crops in the world). Much more immersive!

**What changed for 1.21.1?**
- Removed `gui_light` (deprecated in 1.21+)
- Added all display contexts (required for proper rendering in hand/GUI/ground)
- Simplified structure (removed unused faces with 0-width UVs)

**Quick tip:** Create one file with Option B, then copy and modify the texture path for each crop!

**All 58 model files follow this exact pattern:**
- `golden_wheat_0.json` → texture: `minecraft:custom_crops/golden_wheat_0`
- `golden_wheat_1.json` → texture: `minecraft:custom_crops/golden_wheat_1`
- `crimson_carrot_0.json` → texture: `minecraft:custom_crops/crimson_carrot_0`
- etc.

**Batch Creation Script (PowerShell) - 3D Models:**
```powershell
# Create directory first
New-Item -ItemType Directory -Force -Path "assets/minecraft/models/item/custom_crops"

$crops = @(
    @{name="golden_wheat"; stages=4},
    @{name="crimson_carrot"; stages=4},
    @{name="ender_berry"; stages=5},
    @{name="crystal_melon"; stages=6},
    @{name="celestial_potato"; stages=7},
    @{name="void_pumpkin"; stages=8}
)

foreach ($crop in $crops) {
    for ($i = 0; $i -lt $crop.stages; $i++) {
        $content = @"
{
  "credit": "Botany System",
  "textures": {
    "crop": "minecraft:custom_crops/$($crop.name)_$i",
    "particle": "minecraft:custom_crops/$($crop.name)_$i"
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
        "east": {"uv": [0, 0, 16, 16], "texture": "#crop"},
        "west": {"uv": [0, 0, 16, 16], "texture": "#crop"}
      }
    }
  ],
  "display": {
    "thirdperson_righthand": {
      "rotation": [75, 45, 0],
      "translation": [0, 2.5, 0],
      "scale": [0.375, 0.375, 0.375]
    },
    "thirdperson_lefthand": {
      "rotation": [75, 45, 0],
      "translation": [0, 2.5, 0],
      "scale": [0.375, 0.375, 0.375]
    },
    "firstperson_righthand": {
      "rotation": [0, 45, 0],
      "translation": [0, 0, 0],
      "scale": [0.4, 0.4, 0.4]
    },
    "firstperson_lefthand": {
      "rotation": [0, 225, 0],
      "translation": [0, 0, 0],
      "scale": [0.4, 0.4, 0.4]
    },
    "ground": {
      "rotation": [0, 0, 0],
      "translation": [0, 3, 0],
      "scale": [0.25, 0.25, 0.25]
    },
    "gui": {
      "rotation": [30, 225, 0],
      "translation": [0, 0, 0],
      "scale": [0.625, 0.625, 0.625]
    },
    "fixed": {
      "rotation": [0, 0, 0],
      "translation": [0, 0, 0],
      "scale": [0.5, 0.5, 0.5]
    },
    "head": {
      "rotation": [0, 0, 0],
      "translation": [0, 0, 0],
      "scale": [1, 1, 1]
    }
  }
}
"@
        $content | Out-File "assets/minecraft/models/item/custom_crops/$($crop.name)_$i.json" -Encoding utf8
    }
}

Write-Host "Created 34 3D crop model files!" -ForegroundColor Green
```

**For Simple 2D Models (Alternative):**
```powershell
# Much simpler version if you want flat sprites
foreach ($crop in $crops) {
    for ($i = 0; $i -lt $crop.stages; $i++) {
        $content = @"
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "minecraft:custom_crops/$($crop.name)_$i"
  }
}
"@
        $content | Out-File "assets/minecraft/models/item/custom_crops/$($crop.name)_$i.json" -Encoding utf8
    }
}
```

---

### Step 3: Create Textures (The Creative Part!)
Create **16x16 pixel** PNG images for each growth stage.

**IMPORTANT: Make crops grow TALLER as they progress!**
- Stage 0: Use bottom 4-6 pixels (small sprout)
- Stage 1: Use bottom 8-10 pixels (growing)
- Stage 2: Use bottom 12-14 pixels (almost full)
- Stage 3+: Use all 16 pixels (fully grown and tall)

This creates the visual effect of crops **growing upward** as they mature!

**Naming Convention:**
- `assets/minecraft/textures/custom_crops/[crop_name]_[stage].png`

**Texture Guidelines:**

#### Golden Wheat (Common - Yellow/Gold)
- **Stage 0:** Tiny green sprout at bottom (2-4 pixels tall)
- **Stage 1:** Taller green stem with small golden tip (6-8 pixels tall)
- **Stage 2:** Medium wheat plant with golden heads forming (10-12 pixels tall)
- **Stage 3:** Full-height golden wheat with bright heads (16 pixels tall)

**Color Palette:** `#F4E542` (gold), `#98D982` (green), `#8B7355` (brown stems)

#### Crimson Carrot (Uncommon - Red/Orange)
- **Stage 0:** Tiny red-tinted sprout (2-4 pixels)
- **Stage 1:** Growing red-orange leaves (6-8 pixels)
- **Stage 2:** Taller with crimson highlights (10-12 pixels)
- **Stage 3:** Full crimson carrot tops reaching top (16 pixels)

**Color Palette:** `#DC143C` (crimson), `#FF6347` (tomato), `#228B22` (forest green)

#### Ender Berry (Rare - Purple/Magenta)
- **Stage 0:** Small purple sprout (2-3 pixels)
- **Stage 1:** Growing purple vines (5-7 pixels)
- **Stage 2-3:** Taller ender-touched vines (8-12 pixels)
- **Stage 4:** Full berry bush with glowing berries at top (16 pixels)

**Color Palette:** `#8B008B` (dark magenta), `#DA70D6` (orchid), `#4B0082` (indigo)

#### Crystal Melon (Epic - Cyan/Aqua)
- **Stage 0-1:** Tiny crystalline sprouts (2-4 pixels)
- **Stage 2-3:** Growing crystalline structure (6-10 pixels)
- **Stage 4-5:** Tall crystal formations (12-14 pixels)
- **Stage 6:** Full melon with glowing crystal facets reaching top (16 pixels)

**Color Palette:** `#00CED1` (dark turquoise), `#40E0D0` (turquoise), `#87CEEB` (sky blue), add sparkle effects at top

#### Celestial Potato (Legendary - White/Gold)
- **Stage 0-2:** Ethereal white sprouts (2-5 pixels)
- **Stage 3-4:** Growing celestial vines with golden highlights (7-11 pixels)
- **Stage 5-6:** Tall vines with star particles (13-15 pixels)
- **Stage 7:** Full plant with radiant white-gold glow filling entire height (16 pixels)

**Color Palette:** `#FFFACD` (lemon chiffon), `#FFD700` (gold), `#F0F8FF` (alice blue), add star particles at top

#### Void Pumpkin (Mythic - Dark Purple/Black)
- **Stage 0-3:** Dark purple/black sprouts growing (2-8 pixels)
- **Stage 4-5:** Growing void-touched vines (9-12 pixels)
- **Stage 6-7:** Very tall dark vines (13-15 pixels)
- **Stage 8:** Massive dark pumpkin with swirling void effects at top (16 pixels full height)

**Color Palette:** `#1C1C1C` (jet black), `#4B0082` (indigo), `#9400D3` (dark violet), add void particle effects at top

**PRO TIP:** Leave transparent pixels at the top for early stages - this creates empty space that makes the crop appear shorter!

---

### Step 4: Texture Creation Tools

**Recommended Tools:**
1. **Aseprite** ($20) - Best for pixel art, has animation support
2. **GIMP** (Free) - Free alternative with pencil tool
3. **Piskel** (Free, Browser) - https://www.piskelapp.com/
4. **Pixilart** (Free, Browser) - https://www.pixilart.com/

**AI Generation Tools (Fast but less control):**
1. **Midjourney** - Use prompts like "16x16 pixel art golden wheat minecraft style"
2. **DALL-E** - Similar prompts
3. **Stable Diffusion** - Local generation with ControlNet for pixel art

**Quick Start Template:**
Use vanilla Minecraft wheat textures as a base and recolor them for your first attempt!

---

### Step 5: Testing Your Resource Pack

1. **Zip the folder:** Select all contents (not the folder itself) and create a .zip file
2. **Rename to .zip:** Ensure it ends with `.zip` extension
3. **Place in resourcepacks folder:** `%appdata%/.minecraft/resourcepacks/`
4. **Enable in-game:** Options → Resource Packs → Select your pack
5. **Test:** `/botany give YourName golden_wheat 1` and check if it looks correct

**Troubleshooting:**
- Seeds look like normal wheat seeds? → Check custom model data is correct
- Missing texture (purple/black)? → Verify texture file path and name
- Pack doesn't load? → Check pack.mcmeta format (use JSON validator)

---

### Step 6: Distribution

**For Server:**
1. Upload pack to a file host (Google Drive, Dropbox, etc.)
2. Add to `server.properties`:
   ```
   resource-pack=https://yourlink.com/BotanyPack.zip
   resource-pack-sha1=[SHA1 hash of your zip]
   require-resource-pack=true
   ```
3. Players auto-download on join!

**Generate SHA1 Hash:**
```powershell
Get-FileHash -Path "BotanyPack.zip" -Algorithm SHA1
```

---

### 📐 Visual Guide: Height Progression

Here's how to make crops grow taller using transparent pixels:

```
Stage 0 (Tiny Sprout):          Stage 3 (Full Height):
┌────────────────┐             ┌────────────────┐
│ [Transparent]  │ ← 12px      │ ###  Wheat  ###│ 
│ [Transparent]  │             │ ###  Heads  ###│
│ [Transparent]  │             │ ### ###### ###│
│ [Transparent]  │             │  Green Stem   │
│ [Transparent]  │             │  Green Stem   │
│ [Transparent]  │             │  Green Stem   │
│   Green Leaf   │ ← 2px       │  Green Stem   │
│   Brown Stem   │ ← 2px       │  Brown Stem   │
└────────────────┘             └────────────────┘
 4 pixels used                  16 pixels used
 Appears short                  Appears tall!
```

**Key Points:**
- ✅ Use transparent pixels (alpha = 0) for the top portion of early stages
- ✅ Gradually fill in more pixels as the crop grows
- ✅ Final stage uses all 16 pixels for maximum height
- ✅ The crossed-plane model displays the full 16px height, but transparent areas are invisible
- ✅ This creates a smooth "growing upward" animation

---

## 🎨 Example Texture Descriptions

To help with creation/AI generation, here are detailed descriptions:

### Golden Wheat - Stage 0 (Early Sprout)
```
A 16x16 pixel art representation of a tiny wheat sprout.
- Only use bottom 4 pixels of the texture (top 12 pixels are TRANSPARENT)
- Bottom 2 pixels: Small brown stem base (color: #8B7355)
- Next 2 pixels: Two tiny light green leaves (color: #98D982)
- Minecraft pixel art style, simple and clean
- IMPORTANT: Leave top 12 pixels completely transparent so it appears short
```

### Golden Wheat - Stage 3 (Fully Grown)
```
A 16x16 pixel art representation of magical golden wheat at full height.
- USE ALL 16 PIXELS for maximum height
- Bottom 4 pixels: Brown stem (color: #8B7355)
- Middle 6 pixels: Light green leaves and stem (color: #98D982) 
- Top 6 pixels: Bright golden wheat heads (color: #F4E542)
- Add subtle highlights on wheat heads (color: #FFF4A3)
- Minecraft pixel art style, reaches full height
```

### Void Pumpkin - Stage 0 (Tiny Sprout)
```
A 16x16 pixel art representation of a dark void pumpkin sprout.
- Only use bottom 3 pixels (top 13 pixels TRANSPARENT)
- Bottom 1 pixel: Tiny dark purple root (color: #4B0082)
- Next 2 pixels: Small dark leaves with slight void energy (color: #1C1C1C with #9400D3 highlights)
- Minecraft pixel art style, very small and mysterious
```

### Void Pumpkin - Stage 7 (Fully Grown)
```
A 16x16 pixel art representation of a mystical void pumpkin at maximum height.
- USE ALL 16 PIXELS - this is the tallest stage!
- Bottom 5 pixels: Twisted dark purple vines (color: #4B0082)
- Middle 11 pixels: Large dark pumpkin shape (color: #1C1C1C)
- Add swirling purple energy effect around edges (color: #9400D3)
- Add 2-3 bright cyan dots as "eyes" near top (color: #00FFFF)
- Glowing purple particles at top corners (color: #8B00FF)
- Minecraft pixel art style, dark, ominous, and TALL
```

---

## 🌱 Custom Crops Reference

### Crop Progression Tree

```
Golden Wheat (Common, Level 1)
  ↓ (breed with self)
Crimson Carrot (Uncommon, Level 5)
  ↓ (breed with self)
Ender Berry (Rare, Level 15)
  ↓ (breed with Crimson Carrot)
Crystal Melon (Epic, Level 30)
  ↓ (breed with Ender Berry)
Celestial Potato (Legendary, Level 50)
  ↓ (breed with Crystal Melon)
Void Pumpkin (Mythic, Level 75)
```

### Crop Details

| Crop | Rarity | Stages | Growth Time | Level Req | XP (Plant/Harvest/Breed) | Seed Drop % |
|------|--------|--------|-------------|-----------|-------------------------|-------------|
| Golden Wheat | Common | 4 | 2 min | 1 | 10/15/25 | 15% |
| Crimson Carrot | Uncommon | 4 | 2.7 min | 5 | 20/30/50 | 12% |
| Ender Berry | Rare | 5 | 4.2 min | 15 | 40/60/100 | 10% |
| Crystal Melon | Epic | 6 | 6 min | 30 | 80/120/200 | 8% |
| Celestial Potato | Legendary | 7 | 9.3 min | 50 | 150/250/500 | 5% |
| Void Pumpkin | Mythic | 8 | 13.3 min | 75 | 300/500/1000 | 2% |

---

## 🏭 Crop Breeder Multiblock

### Structure
Build a 3x3 platform of farmland with a composter in the center:

```
Bottom Layer (Y+0):
  F F F    F = Farmland
  F C F    C = Composter (center)
  F F F

Top Layer (Y+1):
  Air (display entities spawn here during breeding)
```

### Usage
1. Build the structure
2. Right-click the composter with two different custom crop seeds
3. Wait for the breeding timer to complete
4. Collect your new crop seed!

### Breeding Mechanics
- **Success Chance**: Each breeding recipe has a success rate
- **Level Requirement**: Must have sufficient Botany level
- **Time**: Breeding time increases with crop rarity
- **Visual Feedback**: Parent crop displays float above breeder during process
- **Particles**: Happy villager particles show progress, totem particles on completion

---

## 🎮 Gameplay Mechanics

### Planting Custom Crops
1. Obtain custom crop seeds (via breeding or rewards)
2. Place seed on farmland (like vanilla seeds)
3. System places invisible tripwire + ItemDisplay entity
4. Crop grows through stages automatically

### Harvesting Custom Crops
1. Break the crop when fully grown
2. Receive crop drops (based on rarity)
3. Chance to get seed back for replanting
4. Gain Botany XP

### Growth System
- **Auto-Growth**: Crops grow automatically over time
- **Stage-Based**: Each crop has multiple growth stages
- **Visual Updates**: ItemDisplay entity updates with new texture each stage
- **No Bonemeal**: Custom crops cannot be bonemealed (planned feature)

---

## 📊 Custom Model Data Ranges

| Crop | CMD Range | Description |
|------|-----------|-------------|
| Golden Wheat | 100001-100004 | 4 stages |
| Crimson Carrot | 100011-100014 | 4 stages |
| Ender Berry | 100021-100025 | 5 stages |
| Crystal Melon | 100031-100036 | 6 stages |
| Celestial Potato | 100041-100047 | 7 stages |
| Void Pumpkin | 100051-100058 | 8 stages |

Reserve 100059-110000 for future custom crops.

---

## 🔧 Technical Implementation

### ItemDisplay Transformation (Updated for Tall Crops)
The `PlantedCustomCrop.java` class uses these transformation values:
- **Scale**: `1.5f` (makes crops 1.5x larger - much more visible!)
- **Translation Y**: `0.75f` (raises crops up to sit ON TOP of farmland block)
- **Translation X/Z**: `0f` (centered horizontally on block)
- **Billboard**: `FIXED` (doesn't rotate with camera view)

This ensures crops appear **above ground** like real plants, not sunken into the farmland!

### Core Classes
- **CustomCrop**: Data model for crop definitions
- **CustomCropRegistry**: Central registry for all crops
- **PlantedCustomCrop**: Tracks individual planted crops in world
- **CropBreeder**: Multiblock structure for breeding
- **BotanySubskill**: Skill progression and XP

### Data Storage
- Planted crops are saved to player profile data
- Breeder structures tracked in world data
- Uses Bukkit serialization for persistence

### Performance Considerations
- ItemDisplay entities are persistent but culled when chunks unload
- Growth checks run on a timed task (every few seconds)
- Tripwire blocks provide collision without rendering overhead

---

## 📝 TODO / Future Features

### Planned Features
- [ ] Bonemeal support for custom crops
- [ ] Watering can item to speed growth
- [ ] Crop mutations (random rare variants)
- [ ] Greenhouse structure (boosts growth speed)
- [ ] Crop quality system (Perfect/Great/Good/Poor)
- [ ] Crop diseases/pests
- [ ] Fertilizer system
- [ ] Seasonal crop bonuses
- [ ] Cross-breeding with vanilla crops
- [ ] Crop storage crate multiblock
- [ ] Auto-harvester multiblock
- [ ] Crop almanac GUI (recipe book)

### Integration Points
- **BotanyListener**: Handle planting, breaking, breeding
- **BotanyManager**: Manage growth ticks, save/load data
- **Commands**: `/botany` command for debugging and admin
- **Skill Tree**: Unlock breeding recipes, boost growth speed, increase seed drops

---

## 🎨 Resource Pack Tips

### Texture Creation Tools
- **Blockbench**: 3D modeling for complex crop models
- **Paint.NET**: Free tool for texture creation
- **GIMP**: Advanced texture editing
- **Aseprite**: Pixel art animation

### Animation Ideas
- Add slight swaying animation to crops (using display entity rotation)
- Glowing effects for rare crops (emissive textures)
- Particle effects attached to mythic crops
- Color shifting for celestial/void crops

### Testing
1. Place resource pack in `.minecraft/resourcepacks/`
2. Select in Options > Resource Packs
3. Use `/reload` after pack changes
4. Test each custom model data value in-game

---

## 📞 Support

For issues or questions about the Botany system:
- Check the code documentation in `com.server.profiles.skills.skills.farming.botany`
- Review breeding recipes in `CustomCropRegistry.java`
- Adjust growth times in crop definitions
- Modify XP values for balance

**Note**: This system is designed to be fully modular. Add new crops by:
1. Defining crop in `CustomCropRegistry`
2. Adding breeding recipes
3. Creating resource pack textures
4. Testing in-game

Enjoy creating unique farming experiences! 🌾✨
