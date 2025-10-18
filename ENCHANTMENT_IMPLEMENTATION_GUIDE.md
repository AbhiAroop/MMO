# 🔮 Elemental Enchantment System - Implementation Status & Next Steps

**Date:** October 18, 2025  
**Status:** Phase 1 - Foundation (50% Complete)

---

## ✅ COMPLETED - Phase 1A: Core Data Structures

### 1. Element System ✅
**Files Created:**
- `ElementType.java` - 8 base elements with counter cycle
- `HybridElement.java` - Hybrid combinations (Ice implemented)
- `FragmentTier.java` - 3 quality tiers with weighted rolling

**Features:**
- Counter cycle: Fire > Nature > Earth > Lightning > Water > Air > Shadow > Light > Fire
- Damage multipliers: 0.6x (weak), 1.0x (neutral), 1.4x (strong)
- Hybrid chance: 30% (Basic), 40% (Refined), 50% (Pristine)
- Affinity values: 10/30/50 per tier

### 2. Quality & Rarity System ✅
**Files Created:**
- `EnchantmentQuality.java` - 7 quality tiers (Poor → Godly)
- `EnchantmentRarity.java` - 5 rarity levels (Common → Legendary)

**Features:**
- Effectiveness multipliers: 0.5x to 2.0x
- Affinity scaling: 10 to 100 points
- XP cost scaling: 5-75 levels
- Weighted quality rolling based on fragment tier

### 3. Enchantment Base Classes ✅
**Files Created:**
- `CustomEnchantment.java` - Abstract base class for all enchantments
- `EnchantmentData.java` - NBT storage and retrieval system

**Features:**
- Trigger types: ON_HIT, ON_DAMAGED, ON_KILL, PASSIVE, etc.
- Quality-based stat scaling
- NBT persistence with prefix system
- Multiple enchantments per item support

### 4. Build Configuration ✅
**Modified:**
- `pom.xml` - Added NBT-API dependency and repository

---

## 🚧 IN PROGRESS - Phase 1B: Items & Enchantments

### Next Immediate Tasks:

#### 1. Create Elemental Fragment Items
**File to create:** `ElementalFragment.java`
```java
// Location: com.server.enchantments.items.ElementalFragment
// Purpose: Generate fragment items (8 elements × 3 tiers = 24 types)
// Features:
- Custom ItemStack creation with element colors
- Tier-based glow/enchantment effect
- Lore showing element, tier, hybrid chance
- Stack size: 64
- Material from ElementType enum
```

#### 2. Implement 3 Initial Enchantments

**A. Ember Veil (Fire, Common) - Reactive Ignite**
```java
// Location: com.server.enchantments.abilities.EmberVeil
// Stats: [burn_duration, damage_per_second]
// Trigger: ON_DAMAGED
// Effect: When hit, ignite attacker
// Scaling:
  - Poor: 2s, 0.5 hearts/s
  - Godly: 8s, 2.0 hearts/s
```

**B. Inferno Strike (Fire, Rare) - Proc Fire Damage**
```java
// Location: com.server.enchantments.abilities.InfernoStrike
// Stats: [proc_chance, bonus_damage]
// Trigger: ON_HIT
// Effect: Chance to deal bonus fire damage
// Scaling:
  - Poor: 5%, +2 damage
  - Godly: 20%, +8 damage
```

**C. Frostflow (Water, Uncommon) - Stacking Slow**
```java
// Location: com.server.enchantments.abilities.Frostflow
// Stats: [slow_duration, slow_amplifier, max_stacks]
// Trigger: ON_HIT
// Effect: Apply stacking slowness debuff
// Scaling:
  - Poor: 3s, Slow I, 2 stacks
  - Godly: 10s, Slow III, 5 stacks
```

#### 3. Create Registry System
**File to create:** `EnchantmentRegistry.java`
```java
// Location: com.server.enchantments.EnchantmentRegistry
// Purpose: Register and retrieve all enchantments
// Features:
- Singleton pattern
- Register all enchantments on init
- Get by ID
- Get by element/rarity
- Weighted random selection
```

**File to create:** `FragmentRegistry.java`
```java
// Location: com.server.enchantments.FragmentRegistry  
// Purpose: Fragment item creation and identification
// Features:
- Generate fragment items
- Identify fragment from ItemStack
- Get element and tier from item
```

---

## 📋 Phase 2: GUI & Core Mechanics (Next)

### Files to Create:

#### 1. Enchantment Table Structure
**File:** `EnchantmentTableStructure.java`
```java
// Location: com.server.enchantments.table.EnchantmentTableStructure
// Purpose: Detect armor stand + enchantment table setup
// Features:
- Multi-block structure validation
- Armor stand as interaction point
- Enchantment table as crafting surface
- Optional decorative blocks
```

#### 2. Enchantment GUI
**File:** `EnchantmentTableGUI.java`
```java
// Location: com.server.enchantments.gui.EnchantmentTableGUI
// Purpose: Custom 54-slot GUI for enchanting
// Layout:
  Slot 13: Item input
  Slots 20-23: Fragment inputs (2-4 fragments, orange glass when empty)
  Slot 31: XP cost display
  Slot 40: Enchant button (green = ready, red = invalid)
  Slot 53: Cancel button
```

#### 3. Enchantment Application Logic
**File:** `EnchantmentApplicator.java`
```java
// Location: com.server.enchantments.EnchantmentApplicator
// Purpose: Apply enchantments to items
// Features:
- Validate item compatibility
- Calculate hybrid chance
- Roll quality based on fragment tier
- Select enchantment from weighted pool
- Apply to item NBT
- Update item lore/display
- Consume fragments and XP
```

#### 4. Event Listeners
**Files:**
- `EnchantmentTableListener.java` - Right-click armor stand
- `EnchantmentTriggerListener.java` - Trigger enchantment effects

---

## 📋 Phase 3: Abilities & Effects (After Phase 2)

### Files to Create:

#### 1. Ability Trigger System
**File:** `AbilityTriggerManager.java`
```java
// Purpose: Route events to enchantment triggers
// Features:
- Scan player equipment for enchantments
- Match trigger types to events
- Execute enchantment effects
- Handle cooldowns
```

#### 2. Effect Handlers
**Files:**
- `BurnEffect.java` - Fire damage over time
- `SlowEffect.java` - Slowness stacking
- `FreezeEffect.java` - Immobilization (Ice hybrid)

---

## 📋 Phase 4: Affinity System (After Phase 3)

### Integration with PlayerStats

**File to modify:** `PlayerStats.java`
```java
// Add fields:
private Map<ElementType, Integer> elementalAffinity = new HashMap<>();
private ElementType dominantElement = null;

// Add methods:
public void recalculateAffinity(Player player)
public ElementType getDominantElement()
public int getAffinityValue(ElementType element)
public double getPvPModifier(ElementType attackerElement)
```

**File to create:** `AffinityTracker.java`
```java
// Purpose: Track and calculate player affinity
// Features:
- Scan all equipped items
- Sum affinity by element
- Determine dominant element
- Calculate PvP modifiers
```

**File to modify:** `StatScanManager.java`
```java
// Re-enable enchantment scanning (currently disabled)
// Integrate with affinity calculation
```

---

## 🛠️ Admin Commands Structure

**File to create:** `EnchantCommand.java`
```java
// Location: com.server.enchantments.commands.EnchantCommand
// Command: /enchant <subcommand> [args]
// Subcommands:
  /enchant give <player> <element> <tier> [amount]
    - Give elemental fragments
  
  /enchant test <player> <enchantment> <quality>
    - Give test item with specific enchantment
  
  /enchant info <enchantment_id>
    - Show enchantment details
  
  /enchant inspect [player]
    - View item enchantments or player affinity
  
  /enchant list [element|rarity]
    - List all enchantments (optionally filtered)
  
  /enchant debug [on|off]
    - Toggle debug mode
  
  /enchant clear <player> [slot]
    - Remove enchantments from item
  
  /enchant table create|remove
    - Place/remove enchantment table structure
```

---

## 🎯 Testing Checklist (After Implementation)

### Fragment System
- [ ] All 24 fragment types generate correctly
- [ ] Fragment identification from ItemStack works
- [ ] Fragment colors and lore display properly

### Enchantment Application
- [ ] GUI opens on armor stand right-click
- [ ] Fragments can be placed in slots
- [ ] XP cost calculates correctly
- [ ] Quality rolling matches tier weights
- [ ] Hybrid chance works (30-50%)
- [ ] Failed hybrids select random element
- [ ] Enchantment data persists in NBT
- [ ] Item lore updates with enchantment info

### Ability Triggers
- [ ] Ember Veil ignites attackers
- [ ] Inferno Strike procs fire damage
- [ ] Frostflow stacks slowness
- [ ] Quality scaling affects all stats
- [ ] Cooldowns prevent spam

### Affinity System
- [ ] Affinity calculates from equipped items
- [ ] Dominant element updates correctly
- [ ] PvP modifiers apply properly
- [ ] StatScanManager integration works

---

## 📝 Code Examples

### Creating a Fragment
```java
ElementalFragment fireBasic = FragmentRegistry.createFragment(ElementType.FIRE, FragmentTier.BASIC);
player.getInventory().addItem(fireBasic);
```

### Applying an Enchantment
```java
ItemStack sword = player.getInventory().getItemInMainHand();
CustomEnchantment emberVeil = EnchantmentRegistry.getEnchantment("ember_veil");
EnchantmentQuality quality = EnchantmentQuality.RARE;
EnchantmentData.addEnchantmentToItem(sword, emberVeil, quality);
```

### Checking Item Enchantments
```java
List<EnchantmentData> enchants = EnchantmentData.getEnchantmentsFromItem(sword);
for (EnchantmentData data : enchants) {
    player.sendMessage(data.getEnchantmentId() + " - " + data.getQuality().name());
}
```

### Calculating Player Affinity
```java
AffinityTracker.recalculateAffinity(player);
ElementType dominant = AffinityTracker.getDominantElement(player);
int fireAffinity = AffinityTracker.getAffinityValue(player, ElementType.FIRE);
```

---

## 🐛 Known Limitations

1. **NBT-API Dependency** - Requires Maven reload/rebuild
2. **Hybrid Enchantments** - Only Ice implemented, others commented out
3. **PvP System** - Affinity modifiers not yet integrated
4. **Particle Effects** - Not implemented yet
5. **Sound Effects** - Not implemented yet

---

## 💡 Future Enhancements

### Additional Hybrid Elements
- Storm (Lightning + Air) - Chain lightning effects
- Magma (Fire + Earth) - Area denial, burn ground
- Poison (Nature + Shadow) - Damage over time, weakening
- Crystal (Earth + Light) - Shields, damage reduction

### Advanced Features
- Enchantment upgrade system (consume fragments to improve quality)
- Enchantment extraction (remove from item, get fragments back)
- Set bonuses (multiple enchantments of same element)
- Legendary unique enchantments (one-of-a-kind effects)
- Enchantment conflicts (certain enchantments can't coexist)
- Element resonance (bonus for matching player's dominant element)

### Visual Polish
- Particle effects per element type
- Custom sounds for each enchantment trigger
- Glowing item effects based on quality
- Animated GUI elements
- Element-themed armor stand decorations

---

## 🚀 Development Priority

**Current Phase:** Phase 1B (50% complete)

**Next Steps (In Order):**
1. Create ElementalFragment.java (30 min)
2. Implement 3 initial enchantments (1-2 hours)
3. Create EnchantmentRegistry + FragmentRegistry (1 hour)
4. Test fragment generation and identification (30 min)
5. Move to Phase 2: GUI system (2-3 hours)

**Estimated Time to Functional System:** 8-12 hours of development

**Estimated Time to Complete System:** 20-30 hours total
