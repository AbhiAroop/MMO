# Enchantment Tome System

## 🎯 Overview

The **Enchantment Tome** system provides a universal enchanting medium that can hold enchantments for **any equipment type**. Unlike vanilla Minecraft books, these custom tomes work with the custom enchantment system and will be applied via a future custom anvil system.

---

## 📚 Tome Types

### 1. **Enchantment Tome** (Unenchanted)
- **Material**: `BOOK`
- **Custom Model Data**: `400000`
- **Description**: Blank tome ready to receive enchantments
- **Usage**: Place in Enchantment Table with fragments

**Visual**:
```
✦ Enchantment Tome ✦
━━━━━━━━━━━━━━━━━━━━━━
✦ Universal Enchanting Medium ✦

A mystical tome that can hold
enchantments for any equipment type.

▸ Place in Enchantment Table
▸ Add Elemental Fragments
▸ Receive Enchanted Tome

Apply enchantments via anvil
(Future Feature)
━━━━━━━━━━━━━━━━━━━━━━
```

### 2. **Enchanted Tome** (Holds Enchantments)
- **Material**: `ENCHANTED_BOOK`
- **Custom Model Data**: `410000`
- **Description**: Tome with stored enchantments
- **Usage**: Future custom anvil system

**Visual Example** (with 2 enchantments):
```
✦ Enchanted Tome ✦
━━━━━━━━━━━━━━━━━━━━━━
✦ Enchanted Tome ✦

Contains 2 Enchantments:

▸ Cinderwake V [Legendary]
▸ Voltbrand IV [Epic]

Use in an anvil to apply
enchantments to equipment.

✦ Universal - Works on any gear ✦
━━━━━━━━━━━━━━━━━━━━━━
```

---

## 🔄 How It Works

### Step 1: Obtain Unenchanted Tome
```
/enchant give <player> tome [amount]
```

### Step 2: Enchant the Tome
1. Open **Enchantment Table** GUI
2. Place **Enchantment Tome** in item slot
3. Add **Elemental Fragments** (1-3 stacks)
4. Click **Enchant** button
5. Receive **Enchanted Tome** with stored enchantments

### Step 3: Apply to Equipment (Future Feature)
- Use custom anvil system (not yet implemented)
- Combine Enchanted Tome with any equipment
- Enchantments transfer to equipment

---

## ✨ Key Features

### **Universal Compatibility**
- Tomes can receive **ANY enchantment type**
- Offensive, Defensive, and Utility enchantments all work
- No equipment type restrictions during enchanting

### **Multi-Enchantment Support**
- Can hold **1-4 enchantments** based on fragment count:
  - 64 fragments = 1 enchantment
  - 128 fragments = 2 enchantments
  - 192 fragments = 3 enchantments
  - 256 fragments = 4 enchantments

### **Quality & Level Preservation**
- All enchantment data preserved:
  - Enchantment ID
  - Quality (Poor → Godly)
  - Level (I → VIII)
  - Element type
  - Hybrid status
  - Affinity values

### **Enchantment Display**
- Enchanted Tomes show all stored enchantments in lore
- Color-coded by quality
- Includes level in Roman numerals
- Shows total enchantment count

---

## 📊 Custom Model Data Pattern

```
Format: 4X0YZZ
- 4: Tome prefix (400000 range)
- X: Type (0=Unenchanted, 1=Enchanted)
- 0: Separator
- Y: Reserved (0)
- ZZ: Variant (00)

Examples:
- Unenchanted Tome: 400000 (4 00 0 00)
- Enchanted Tome:   410000 (4 10 0 00)
```

### Model Data Ranges
| Item Type | Range | Example |
|-----------|-------|---------|
| Unenchanted Tome | 400000-404999 | 400000 |
| Enchanted Tome | 410000-414999 | 410000 |

---

## 🛠️ Technical Implementation

### Files Created
1. **EnchantmentTome.java**
   - `createUnenchantedTome()` - Create blank tome
   - `createEnchantedTome(ItemStack)` - Convert to enchanted
   - `isUnenchantedTome(ItemStack)` - Check if unenchanted
   - `isEnchantedTome(ItemStack)` - Check if enchanted
   - `canBeEnchanted(ItemStack)` - Validate enchantability

### Files Modified
1. **EquipmentTypeValidator.java**
   - Added `TOME` enum to `EquipmentType`
   - Added `TOME_MIN` and `TOME_MAX` ranges (400000-409999)
   - Updated `canEnchantmentApply()` to allow all enchantments on tomes

2. **EnchantmentApplicator.java**
   - Added tome conversion logic after enchanting
   - Automatically converts unenchanted → enchanted tome
   - Preserves all enchantment NBT data during conversion

3. **EnchantCommand.java**
   - Added `/enchant give <player> tome [amount]` command
   - Updated tab completion to include "tome"
   - Added help text for tome giving

---

## 💡 Usage Examples

### Give Tomes to Players
```
/enchant give PlayerName tome
/enchant give PlayerName tome 5
```

### Enchant a Tome
1. Get unenchanted tome: `/enchant give @s tome`
2. Open enchantment table
3. Place tome in slot
4. Add 256 Fire fragments (for max enchantments)
5. Click enchant → Receive enchanted tome with 4 fire enchantments

### View Stored Enchantments
- Hover over Enchanted Tome in inventory
- Lore shows all stored enchantments with quality/level

---

## 🎮 Player Experience

### Benefits
1. **Flexibility**: Enchant first, choose gear later
2. **Trading**: Enchanted tomes can be traded between players
3. **Storage**: Store powerful enchantments for future use
4. **Multi-Purpose**: Single tome works for any gear type

### Use Cases
- Enchant tomes with rare/expensive fragments
- Hold until you get the perfect weapon/armor
- Trade high-quality enchanted tomes
- Build a library of enchanted tomes

---

## 🔮 Future: Custom Anvil System

### Planned Features
- Combine Enchanted Tome + Equipment
- Transfer enchantments from tome to gear
- Costs XP levels based on enchantment quality
- Validates enchantment compatibility with equipment
- Supports multiple tomes on one item

### Example Future Workflow
```
Enchanted Tome (Cinderwake V [Legendary])
    +
Diamond Sword (Custom Model Data: 210001)
    =
Enchanted Diamond Sword (Cinderwake V)
```

---

## ⚙️ Configuration

### Custom Model Data
To customize tome models in a resource pack:
- Unenchanted: Override model `400000` for `minecraft:book`
- Enchanted: Override model `410000` for `minecraft:enchanted_book`

### NBT Data Structure
```
MMO_Tome_Type: "UNENCHANTED" or "ENCHANTED"
MMO_EnchantmentCount: <number>
MMO_Enchantment_0_ID: "<enchantment_id>"
MMO_Enchantment_0_Quality: "<quality>"
MMO_Enchantment_0_Level: "<level>"
MMO_Enchantment_0_Element: "<element>"
MMO_Enchantment_0_Hybrid: "<hybrid_element>" (if hybrid)
MMO_Enchantment_0_Affinity_Offensive: <value>
MMO_Enchantment_0_Affinity_Defensive: <value>
MMO_Enchantment_0_Affinity_Utility: <value>
... (repeat for each enchantment)
```

---

## 🧪 Testing Checklist

### Unenchanted Tome
- [ ] `/enchant give @s tome` creates correct item
- [ ] Custom model data = 400000
- [ ] Material = BOOK
- [ ] Lore displays correctly
- [ ] Can be placed in enchantment table

### Enchanting Process
- [ ] Tome accepts all enchantment types
- [ ] Enchantments apply correctly to tome
- [ ] Multi-enchantment works (1-4 enchants)
- [ ] NBT data stored properly
- [ ] Converts to enchanted tome after enchanting

### Enchanted Tome
- [ ] Custom model data = 410000
- [ ] Material = ENCHANTED_BOOK
- [ ] Lore shows all enchantments
- [ ] Quality colors display correctly
- [ ] Level Roman numerals correct
- [ ] Glow effect present
- [ ] NBT data preserved from unenchanted tome

### Edge Cases
- [ ] Tome with 1 enchantment
- [ ] Tome with 4 enchantments
- [ ] Hybrid enchantments on tome
- [ ] All quality levels (Poor → Godly)
- [ ] All levels (I → VIII)
- [ ] Mixed element enchantments

---

## 📈 Statistics

### Tome Advantages Over Direct Enchanting
| Feature | Direct Enchant | Tome Enchant |
|---------|---------------|--------------|
| Flexibility | Must have gear | Can enchant anytime |
| Trading | Can't trade enchanted gear easily | Tomes are tradeable |
| Storage | Limited by inventory | Compact storage |
| Mistakes | Wasted if wrong item | Can store for later |
| Universal | Equipment-specific | Works on any gear |

---

## ✅ Implementation Status

**COMPLETED**:
✅ EnchantmentTome item class created
✅ Unenchanted tome generation
✅ Enchanted tome conversion
✅ Custom model data pattern (400000-410000)
✅ EquipmentType.TOME enum added
✅ Tome validation in EquipmentTypeValidator
✅ Universal enchantment compatibility for tomes
✅ Tome enchanting in EnchantmentApplicator
✅ Automatic conversion after enchanting
✅ NBT data preservation
✅ Enchantment display in lore
✅ /enchant give tome command
✅ Tab completion for "tome"

**PENDING**:
⏳ Custom anvil system for applying tomes to gear
⏳ Resource pack models for custom tome appearance
⏳ Tome trading/market integration
⏳ Tome combination system (merge multiple tomes)

---

## 🎯 Summary

The **Enchantment Tome** system provides a flexible, universal enchanting medium that:
- Works with **all 26 custom enchantments**
- Supports **1-4 enchantments per tome** (based on fragments)
- Uses **custom model data** (400000 unenchanted, 410000 enchanted)
- Preserves **all enchantment data** (quality, level, element, hybrid)
- Enables **future anvil system** for applying to equipment
- **Tradeable** and **storable** for player convenience

Players can now enchant tomes in advance and apply them to gear later, providing maximum flexibility in the custom enchantment system!

---

**Status**: ✅ **IMPLEMENTED - READY FOR TESTING**

**Build Required**: Yes - recompile to apply changes

**Next Steps**: 
1. Build plugin with Maven
2. Test tome creation (`/enchant give @s tome`)
3. Test enchanting tomes in enchantment table
4. Verify conversion to enchanted tome
5. Plan custom anvil system implementation
