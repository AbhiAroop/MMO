# Elemental Fragment Custom Model Data Reference

## Pattern System

**Format**: `3X0YZZ`
- **3**: Fragment prefix (identifies as fragment)
- **X**: Element ID (0-7, based on enum order)
- **0**: Separator
- **Y**: Tier (0=Raw, 1=Refined, 2=Pristine)
- **ZZ**: Reserved for future use (00)

**Base**: 300000

---

## Element IDs

| Element | ID | Ordinal |
|---------|----|----|
| **Fire** | 0 | 0 |
| **Water** | 1 | 1 |
| **Earth** | 2 | 2 |
| **Air** | 3 | 3 |
| **Lightning** | 4 | 4 |
| **Shadow** | 5 | 5 |
| **Light** | 6 | 6 |
| **Void** | 7 | 7 |

## Tier IDs

| Tier | ID | Ordinal |
|------|----|---------|
| **Raw** | 0 | 0 |
| **Refined** | 1 | 1 |
| **Pristine** | 2 | 2 |

---

## Complete Fragment Model Data Table

### Fire Fragments (Element 0)
| Tier | Model Data | Calculation |
|------|------------|-------------|
| Raw | **300000** | 300000 + (0×10000) + (0×100) |
| Refined | **300100** | 300000 + (0×10000) + (1×100) |
| Pristine | **300200** | 300000 + (0×10000) + (2×100) |

### Water Fragments (Element 1)
| Tier | Model Data | Calculation |
|------|------------|-------------|
| Raw | **310000** | 300000 + (1×10000) + (0×100) |
| Refined | **310100** | 300000 + (1×10000) + (1×100) |
| Pristine | **310200** | 300000 + (1×10000) + (2×100) |

### Earth Fragments (Element 2)
| Tier | Model Data | Calculation |
|------|------------|-------------|
| Raw | **320000** | 300000 + (2×10000) + (0×100) |
| Refined | **320100** | 300000 + (2×10000) + (1×100) |
| Pristine | **320200** | 300000 + (2×10000) + (2×100) |

### Air Fragments (Element 3)
| Tier | Model Data | Calculation |
|------|------------|-------------|
| Raw | **330000** | 300000 + (3×10000) + (0×100) |
| Refined | **330100** | 300000 + (3×10000) + (1×100) |
| Pristine | **330200** | 300000 + (3×10000) + (2×100) |

### Lightning Fragments (Element 4)
| Tier | Model Data | Calculation |
|------|------------|-------------|
| Raw | **340000** | 300000 + (4×10000) + (0×100) |
| Refined | **340100** | 300000 + (4×10000) + (1×100) |
| Pristine | **340200** | 300000 + (4×10000) + (2×100) |

### Shadow Fragments (Element 5)
| Tier | Model Data | Calculation |
|------|------------|-------------|
| Raw | **350000** | 300000 + (5×10000) + (0×100) |
| Refined | **350100** | 300000 + (5×10000) + (1×100) |
| Pristine | **350200** | 300000 + (5×10000) + (2×100) |

### Light Fragments (Element 6)
| Tier | Model Data | Calculation |
|------|------------|-------------|
| Raw | **360000** | 300000 + (6×10000) + (0×100) |
| Refined | **360100** | 300000 + (6×10000) + (1×100) |
| Pristine | **360200** | 300000 + (6×10000) + (2×100) |

### Void Fragments (Element 7)
| Tier | Model Data | Calculation |
|------|------------|-------------|
| Raw | **370000** | 300000 + (7×10000) + (0×100) |
| Refined | **370100** | 300000 + (7×10000) + (1×100) |
| Pristine | **370200** | 300000 + (7×10000) + (2×100) |

---

## Quick Reference by Model Data Range

| Range | Element | Description |
|-------|---------|-------------|
| **300000-300299** | Fire | All fire fragments |
| **310000-310299** | Water | All water fragments |
| **320000-320299** | Earth | All earth fragments |
| **330000-330299** | Air | All air fragments |
| **340000-340299** | Lightning | All lightning fragments |
| **350000-350299** | Shadow | All shadow fragments |
| **360000-360299** | Light | All light fragments |
| **370000-370299** | Void | All void fragments |

### By Tier
| Range Pattern | Tier | Description |
|---------------|------|-------------|
| **XX0000** | Raw | All raw fragments (e.g., 300000, 310000) |
| **XX0100** | Refined | All refined fragments (e.g., 300100, 310100) |
| **XX0200** | Pristine | All pristine fragments (e.g., 300200, 310200) |

---

## Usage Examples

### Creating Fragments
```java
// Fire Raw Fragment (300000)
ItemStack fireRaw = ElementalFragment.createFragment(ElementType.FIRE, FragmentTier.RAW);

// Water Pristine Fragment (310200)
ItemStack waterPristine = ElementalFragment.createFragment(ElementType.WATER, FragmentTier.PRISTINE);

// Lightning Refined Fragment (340100)
ItemStack lightningRefined = ElementalFragment.createFragment(ElementType.LIGHTNING, FragmentTier.REFINED);
```

### Checking Fragment Type by Model Data
```java
ItemStack item = player.getInventory().getItemInMainHand();
if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
    int modelData = item.getItemMeta().getCustomModelData();
    
    // Check if it's a fragment (300000-379999)
    if (modelData >= 300000 && modelData < 380000) {
        int elementId = (modelData - 300000) / 10000; // Extract element
        int tierId = ((modelData - 300000) % 10000) / 100; // Extract tier
        
        System.out.println("Element: " + ElementType.values()[elementId]);
        System.out.println("Tier: " + FragmentTier.values()[tierId]);
    }
}
```

---

## Resource Pack Integration

### File Structure
```
assets/
  minecraft/
    models/
      item/
        prismarine_shard.json          # Fire fragments
        prismarine_crystals.json       # Water fragments
        amethyst_shard.json            # Earth fragments
        echo_shard.json                # Air fragments
        lightning_rod.json             # Lightning fragments
        coal.json                      # Shadow fragments
        glowstone_dust.json            # Light fragments
        phantom_membrane.json          # Void fragments
```

### Override Example (prismarine_shard.json for Fire)
```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "item/prismarine_shard"
  },
  "overrides": [
    {
      "predicate": {"custom_model_data": 300000},
      "model": "item/fragments/fire_raw"
    },
    {
      "predicate": {"custom_model_data": 300100},
      "model": "item/fragments/fire_refined"
    },
    {
      "predicate": {"custom_model_data": 300200},
      "model": "item/fragments/fire_pristine"
    }
  ]
}
```

### Custom Model Example (fire_raw.json)
```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "item/fragments/fire_raw"
  }
}
```

---

## Texture Naming Convention

**Recommended structure**:
```
textures/
  item/
    fragments/
      fire_raw.png
      fire_refined.png
      fire_pristine.png
      water_raw.png
      water_refined.png
      water_pristine.png
      earth_raw.png
      earth_refined.png
      earth_pristine.png
      air_raw.png
      air_refined.png
      air_pristine.png
      lightning_raw.png
      lightning_refined.png
      lightning_pristine.png
      shadow_raw.png
      shadow_refined.png
      shadow_pristine.png
      light_raw.png
      light_refined.png
      light_pristine.png
      void_raw.png
      void_refined.png
      void_pristine.png
```

**Total textures needed**: 24 (8 elements × 3 tiers)

---

## Design Guidelines

### Visual Hierarchy by Tier

**Raw (Tier 0)**:
- Base color of element
- Simple, unrefined appearance
- Rough edges
- Low glow/shine

**Refined (Tier 1)**:
- Brighter, more saturated colors
- Smoother appearance
- Slight glow effect
- Medium quality

**Pristine (Tier 2)**:
- Vibrant, intense colors
- Polished, crystalline appearance
- Strong glow/particle effects
- High quality, premium feel

### Element Color Schemes

| Element | Raw | Refined | Pristine |
|---------|-----|---------|----------|
| **Fire** | Dull red-orange | Bright orange | Brilliant red-yellow |
| **Water** | Light blue | Cyan | Deep ocean blue |
| **Earth** | Brown-gray | Rich brown | Emerald green |
| **Air** | Pale white | Silver-white | Bright sky blue |
| **Lightning** | Pale yellow | Bright yellow | Electric blue-white |
| **Shadow** | Dark gray | Deep purple | Void black-purple |
| **Light** | Cream | Bright yellow | Radiant gold-white |
| **Void** | Dull purple | Dark purple | Cosmic purple |

---

## Code Reference

### Fragment Creation with Model Data
```java
public static ItemStack createFragment(ElementType element, FragmentTier tier) {
    // ... existing code ...
    
    // Set custom model data for future custom textures
    int modelData = getFragmentModelData(element, tier);
    meta.setCustomModelData(modelData);
    
    // ... rest of code ...
}

private static int getFragmentModelData(ElementType element, FragmentTier tier) {
    int elementId = element.ordinal(); // 0-7
    int tierId = tier.ordinal(); // 0-2
    return 300000 + (elementId * 10000) + (tierId * 100);
}
```

---

## Reserved Space

**Current Range**: 300000-370299 (used)
**Reserved Range**: 370300-379999 (future fragments/variants)

**Future Use Cases**:
- Special event fragments
- Corrupted/enhanced variants
- Seasonal/limited fragments
- Experimental element types

---

## Migration Guide

### From No Custom Model Data
If you have existing fragments without custom model data:

1. **Automatic Update**: Fragments created after this update will automatically have model data
2. **Manual Update**: Use a command to update existing fragments:
   ```java
   /updatefragments
   ```
3. **Compatibility**: Old fragments will still work but won't have custom textures

### Resource Pack Deployment

1. **Create base pack** with vanilla overrides
2. **Add custom textures** for each fragment
3. **Test in-game** with `/give` commands
4. **Deploy to server** resource pack URL
5. **Players auto-download** on join

---

## Testing Commands

```
# Give specific fragments
/give @p prismarine_shard{CustomModelData:300000} 1  # Fire Raw
/give @p prismarine_shard{CustomModelData:300100} 1  # Fire Refined
/give @p prismarine_shard{CustomModelData:300200} 1  # Fire Pristine

/give @p prismarine_crystals{CustomModelData:310000} 1  # Water Raw
/give @p prismarine_crystals{CustomModelData:310200} 1  # Water Pristine

/give @p lightning_rod{CustomModelData:340200} 1  # Lightning Pristine
```

---

## Summary

- **Total Fragments**: 24 (8 elements × 3 tiers)
- **Model Data Range**: 300000-370299
- **Pattern**: Systematic and predictable
- **Extensible**: Room for future additions
- **Resource Pack Ready**: Easy texture integration

**File Modified**: `ElementalFragment.java`
**Method Added**: `getFragmentModelData(ElementType, FragmentTier)`
**Location**: Fragment creation in `createFragment()` method

---

*Fragment Custom Model Data System - Version 1.0*
*Ready for Resource Pack Integration*
