# Phase 1B Testing Guide

## Build Status: ✅ SUCCESS

**Built**: October 18, 2025 12:40 PM  
**Files Compiled**: 187 source files  
**JAR Location**: `C:\Users\Abhi\Desktop\AI Paper Server\plugins\mmo-0.0.1.jar`  
**NBT-API**: Successfully shaded and included

---

## Quick Start Testing

### 1. Start Server
```
Start your Paper server to load the new plugin build
```

### 2. Verify Command Registration
```
/enchant
```
Should display help menu with all 8 subcommands.

### 3. Test Fragment Creation
```
/enchant give @s fire basic 1
/enchant give @s water pristine 5
/enchant give @s all
```

**Expected Results:**
- Basic Fire Fragment (gray color, quartz)
- Pristine Water Fragment (light purple color, glow effect, lapis lazuli)
- All 24 fragment types in inventory

**Verify:**
- Lore shows element icon, tier, hybrid chance (30%-50%), base affinity
- Refined/Pristine fragments glow (UNBREAKING enchant hidden)
- Fragment materials match element (QUARTZ=Fire, LAPIS_LAZULI=Water, etc.)

---

## Command Testing Checklist

### Fragment Commands

#### Give Single Fragment
```
/enchant give Notch fire basic 1
/enchant give Steve water refined 10
/enchant give Herobrine lightning pristine 64
```

#### Give All Fragments
```
/enchant give @s all
```
Should give all 24 types (8 elements × 3 tiers).

---

### Test Enchanted Items

#### Create Test Items
```
/enchant test @s ember_veil poor
/enchant test @s ember_veil godly
/enchant test @s inferno_strike legendary
/enchant test @s frostflow epic
```

**Expected Items:**
- **EmberVeil**: Diamond Chestplate (armor enchantment)
- **InfernoStrike**: Diamond Sword (weapon enchantment)
- **Frostflow**: Diamond Sword (weapon enchantment)

**Verify Lore:**
- Shows enchantment name with rarity color
- Shows quality with quality color
- Shows scaled stats in lore (optional - if lore implemented)

---

### Info Commands

#### Enchantment Details
```
/enchant info ember_veil
/enchant info inferno_strike
/enchant info frostflow
```

**Expected Output:**
- ID, Element, Rarity, Trigger, Description
- Base stats array
- Quality scaling for all 7 levels (Poor → Godly)

**Example Output:**
```
=== Ember Veil ===
ID: ember_veil
Element: Fire
Rarity: §fCommon
Trigger: ON_DAMAGED
Description: Ignites attackers when you take damage
Base Stats: [4.0, 1.0]
Quality Scaling:
  §8Poor: [2.0, 0.5]
  §fCommon: [3.0, 0.75]
  §aUncommon: [3.5, 0.875]
  §9Rare: [4.0, 1.0]
  §5Epic: [5.0, 1.25]
  §6Legendary: [6.0, 1.5]
  §dGodly: [8.0, 2.0]
```

---

### Inspect Commands

#### Inspect Held Item
```
/enchant inspect
/enchant inspect Notch
```

**Steps:**
1. Give yourself test item: `/enchant test @s inferno_strike godly`
2. Hold item in main hand
3. Run: `/enchant inspect`

**Expected Output:**
```
=== YourName's DIAMOND_SWORD ===
§e1. inferno_strike §dGodly
   §7Stats: [0.2, 8.0]
   §7Affinity: 100
```

**Verify:**
- Shows enchantment ID
- Shows quality with color
- Shows scaled stats (Godly = 2.0x multiplier)
- Shows affinity value (Godly = 100)

---

### List Commands

#### List All Enchantments
```
/enchant list
```

**Expected Output:**
```
=== All Enchantments ===
§fEmber Veil (ember_veil)
§9Inferno Strike (inferno_strike)
§aFrostflow (frostflow)
§eTotal: 3
```

#### List by Element
```
/enchant list fire
/enchant list water
```

**Fire Expected:**
- EmberVeil (Common)
- InfernoStrike (Rare)

**Water Expected:**
- Frostflow (Uncommon)

#### List by Rarity
```
/enchant list common
/enchant list uncommon
/enchant list rare
```

---

### Debug & Utility Commands

#### Debug Mode
```
/enchant debug on
/enchant debug off
/enchant debug
```

Toggles debug logging (currently doesn't output anything).

#### Clear Enchantments
```
/enchant clear @s
```

**Steps:**
1. Hold enchanted item
2. Run command
3. Verify enchantments removed (lore cleared, NBT data gone)

#### Reload Registry
```
/enchant reload
```

**Expected Output:**
```
§eReloading enchantment system...
§aEnchantment system reloaded!
§7Registered: 3 enchantments
```

---

## In-Game Functionality Testing

⚠️ **Note**: These require Phase 2 (GUI & Event Listeners) to be implemented.

### Once Phase 2 is complete, test:

#### EmberVeil (Reactive Fire)
1. Give: `/enchant test @s ember_veil godly`
2. Equip diamond chestplate
3. Let mob hit you
4. **Expected**: Mob catches fire for 8 seconds (Godly quality)

#### InfernoStrike (Proc Damage)
1. Give: `/enchant test @s inferno_strike godly`
2. Hold diamond sword
3. Attack mob repeatedly (20% proc chance at Godly)
4. **Expected**: Some hits deal +8 bonus damage and set mob on fire

#### Frostflow (Stacking Slow)
1. Give: `/enchant test @s frostflow godly`
2. Hold diamond sword
3. Attack same mob 5 times
4. **Expected**: 
   - Stack 1: Slow III for 10s
   - Stack 2: Slow VI for 10s
   - Stack 3-5: Slow IX for 10s (capped at 5 stacks)
   - Stacks decay after 10 seconds

---

## Known Issues & Limitations

### Current Phase (1B):
- ✅ Fragments can be created
- ✅ Test items can be created with enchantments
- ✅ NBT data is stored correctly
- ❌ **Enchantments do NOT trigger yet** (requires Phase 2 event listeners)
- ❌ **No GUI system yet** (requires Phase 2)
- ❌ **Cannot apply fragments to items** (requires Phase 2 applicator)
- ❌ **Item lore may not display enchantment details** (requires lore system)

### What Works:
- Command system (all 8 subcommands)
- Fragment generation (24 types)
- Enchantment registry (3 enchantments)
- NBT storage and retrieval
- Quality scaling calculations
- Admin tools (give, test, info, inspect, list, clear, reload)

### What Doesn't Work Yet:
- Enchanting via GUI
- Enchantment triggers (on hit, on damaged, etc.)
- Affinity system
- PvP modifiers
- Hybrid element formation
- Physical altar structures

---

## Troubleshooting

### "Command not found"
- Check `plugin.yml` has `enchant` command registered
- Verify permission: `mmo.admin.enchant` (default OP)
- Try alias: `/ench` or `/mmoe`

### "No custom enchantments on this item"
- Verify you're holding an enchanted item (created via `/enchant test`)
- Check NBT data exists (use `/data get entity @s SelectedItem`)

### "Enchantment not found"
- Check spelling: `ember_veil`, `inferno_strike`, `frostflow` (lowercase, underscore)
- Use `/enchant list` to see available IDs

### Fragment not glowing
- Only Refined and Pristine tiers glow
- Basic tier has no glow effect

### Stats seem wrong
- Verify quality multiplier:
  - Poor: 0.5x (half effectiveness)
  - Common: 0.75x
  - Uncommon: 0.875x
  - Rare: 1.0x (base stats)
  - Epic: 1.25x
  - Legendary: 1.5x
  - Godly: 2.0x (double effectiveness)

---

## Next Steps After Testing

If all commands work correctly, proceed to **Phase 2**:

### Phase 2A: Structure & GUI (4-6 hours)
1. EnchantmentTableStructure.java - Multi-block detection
2. EnchantmentTableGUI.java - 54-slot interface
3. EnchantmentApplicator.java - Fragment consumption & enchant logic

### Phase 2B: Event Listeners (2-3 hours)
1. EnchantmentTableListener.java - Right-click detection
2. EnchantmentTriggerListener.java - Ability triggers (ON_HIT, ON_DAMAGED)

### Phase 3: Full System Testing (1-2 hours)
- Build altar structures
- Test enchanting via GUI
- Verify ability triggers work
- Test hybrid formation (Ice from Air + Water)

---

## Success Criteria

Phase 1B is **COMPLETE** if:
- ✅ All 8 commands work without errors
- ✅ All 24 fragment types can be created
- ✅ Test items can be created for all 3 enchantments
- ✅ Info commands display correct data
- ✅ Inspect shows NBT data correctly
- ✅ No console errors on plugin load
- ✅ No console errors when running commands

**Current Status**: Ready for testing! 🚀
