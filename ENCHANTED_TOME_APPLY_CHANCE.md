# Enchanted Tome Apply Chance System

## 🎲 Overview

Each enchantment in an **Enchanted Tome** now has a **random apply chance** (0-100%) that determines the probability of successfully transferring the enchantment to equipment when using the custom anvil system.

---

## ✨ Key Features

### Random Apply Chance
- Every enchantment gets a **completely random** apply chance
- Range: **0% to 100%**
- Generated when tome is created (during enchanting)
- Stored in NBT data
- Displayed in tome lore

### Color-Coded Display
Apply chances are color-coded for quick readability:

| Chance Range | Color | Category |
|--------------|-------|----------|
| **80-100%** | 🟢 Green | High chance |
| **50-79%** | 🟡 Yellow | Medium chance |
| **25-49%** | 🟠 Gold | Low-medium chance |
| **0-24%** | 🔴 Red | Low chance |

---

## 📖 Lore Display

### Example: Enchanted Tome with 3 Enchantments

```
━━━━━━━━━━━━━━━━━━━━━━
✦ Enchanted Tome ✦

Contains 3 Enchantments:

▸ Cinderwake V [Legendary]
  Apply Chance: 92%  ← Green (high)
  Burns enemies with intense fire damage
  over time.

▸ Voltbrand IV [Epic]
  Apply Chance: 45%  ← Gold (low-medium)
  Shocks enemies with lightning damage.

▸ Dawnstrike III [Rare]
  Apply Chance: 12%  ← Red (low)
  Deal extra damage against undead.

Use in an anvil to apply
enchantments to equipment.

✦ Universal - Works on any gear ✦
━━━━━━━━━━━━━━━━━━━━━━
```

---

## 🔧 Technical Implementation

### NBT Storage

Each enchantment's apply chance is stored in NBT:

```
MMO_EnchantmentCount: 3
MMO_Enchantment_0_ID: "cinderwake"
MMO_Enchantment_0_Quality: "LEGENDARY"
MMO_Enchantment_0_Level: "V"
MMO_Enchantment_0_ApplyChance: 92  ← Apply chance stored here
...
MMO_Enchantment_1_ID: "voltbrand"
MMO_Enchantment_1_ApplyChance: 45
...
MMO_Enchantment_2_ID: "dawnstrike"
MMO_Enchantment_2_ApplyChance: 12
```

### Generation Logic

**Code in EnchantmentTome.java:**
```java
// Generate random apply chance (0-100%)
int applyChance = RANDOM.nextInt(101); // 0 to 100 inclusive

// Store in NBT
targetNBT.setInteger(prefix + "ApplyChance", applyChance);

// Color-code for display
ChatColor chanceColor;
if (applyChance >= 80) {
    chanceColor = ChatColor.GREEN;      // 80-100%
} else if (applyChance >= 50) {
    chanceColor = ChatColor.YELLOW;     // 50-79%
} else if (applyChance >= 25) {
    chanceColor = ChatColor.GOLD;       // 25-49%
} else {
    chanceColor = ChatColor.RED;        // 0-24%
}

// Display in lore
lore.add(ChatColor.GRAY + "  Apply Chance: " + chanceColor + applyChance + "%");
```

---

## 🎯 Future: Custom Anvil System

### How It Will Work

When a player uses an Enchanted Tome in the custom anvil:

1. **Place tome** + **equipment** in anvil
2. **System rolls** for each enchantment based on apply chance
3. **Successful rolls** transfer enchantment to equipment
4. **Failed rolls** don't transfer enchantment
5. **Tome consumed** regardless of success/failure

### Example Scenario

**Tome Contents:**
- Cinderwake V [92% apply chance] 🟢
- Voltbrand IV [45% apply chance] 🟠
- Dawnstrike III [12% apply chance] 🔴

**Possible Outcomes:**

**Lucky Roll** 🍀:
- Cinderwake: ✅ Success (rolled 87 < 92)
- Voltbrand: ✅ Success (rolled 32 < 45)
- Dawnstrike: ✅ Success (rolled 8 < 12) **Rare!**
- **Result**: All 3 enchantments applied!

**Average Roll**:
- Cinderwake: ✅ Success (rolled 45 < 92)
- Voltbrand: ❌ Failed (rolled 67 > 45)
- Dawnstrike: ❌ Failed (rolled 35 > 12)
- **Result**: Only 1 enchantment applied

**Unlucky Roll** 😢:
- Cinderwake: ❌ Failed (rolled 95 > 92) **Unlucky!**
- Voltbrand: ❌ Failed (rolled 88 > 45)
- Dawnstrike: ❌ Failed (rolled 90 > 12)
- **Result**: No enchantments applied, tome wasted

---

## 💡 Game Design Benefits

### 1. Risk vs Reward
- High-value enchantments might have low apply chances
- Players must weigh risk of losing tome
- Creates tension and excitement

### 2. Economy Impact
- Tomes with high apply chances are more valuable
- Creates market for "safe" vs "risky" tomes
- Encourages tome trading/selling

### 3. Player Stories
- Memorable moments: "I got a 5% apply chance to work!"
- Tragic losses: "My 95% failed..."
- Creates community discussion

### 4. Replayability
- Same tome, different results each time
- No guaranteed outcomes
- Encourages trying again

---

## 🎲 Probability Statistics

### Expected Success Rates

| Apply Chance | Expected Success (100 tries) |
|--------------|------------------------------|
| 100% | 100 successes |
| 90% | ~90 successes |
| 75% | ~75 successes |
| 50% | ~50 successes (coin flip) |
| 25% | ~25 successes |
| 10% | ~10 successes |
| 1% | ~1 success (very rare!) |

### Multiple Enchantments Probability

For a tome with 3 enchantments at 80%, 50%, and 20%:

| Outcome | Probability |
|---------|-------------|
| All 3 apply | 0.8 × 0.5 × 0.2 = **8%** |
| Exactly 2 apply | ~**35%** |
| Exactly 1 applies | ~**43%** |
| None apply | ~**14%** |

**Most likely**: Get 1 or 2 enchantments from the tome.

---

## 🧪 Testing Checklist

### Apply Chance Generation
- [ ] Each enchantment gets a random apply chance
- [ ] Values range from 0 to 100 (inclusive)
- [ ] Multiple enchantments have different chances
- [ ] Chances are stored in NBT correctly

### Lore Display
- [ ] Apply chance shows for each enchantment
- [ ] Color-coding works correctly:
  - [ ] Green for 80-100%
  - [ ] Yellow for 50-79%
  - [ ] Gold for 25-49%
  - [ ] Red for 0-24%
- [ ] Percentage symbol displays correctly

### NBT Storage
- [ ] Apply chance persists after server restart
- [ ] Apply chance matches between lore and NBT
- [ ] Multiple tomes have different chances (not same RNG)

### Edge Cases
- [ ] Tome with 1 enchantment shows apply chance
- [ ] Tome with 3 enchantments shows all apply chances
- [ ] 0% apply chance displays correctly
- [ ] 100% apply chance displays correctly

---

## 📊 Example Tomes

### "Lucky Tome" (All High Chances)
```
▸ Frostflow VIII [Godly]
  Apply Chance: 98% 🟢

▸ Stormfire VII [Legendary]
  Apply Chance: 87% 🟢

▸ Voltbrand VI [Epic]
  Apply Chance: 91% 🟢
```
**Value**: Extremely high (almost guaranteed success)

### "Gambler's Tome" (Mixed Chances)
```
▸ Cinderwake V [Legendary]
  Apply Chance: 15% 🔴

▸ Dawnstrike IV [Epic]
  Apply Chance: 73% 🟡

▸ Embershade III [Rare]
  Apply Chance: 44% 🟠
```
**Value**: Risky but potentially rewarding

### "Vendor Tome" (All Low Chances)
```
▸ Pure Reflection II [Common]
  Apply Chance: 8% 🔴

▸ Gale Step I [Common]
  Apply Chance: 3% 🔴
```
**Value**: Very low (likely to fail, cheap to sell)

---

## 🔮 Future Enhancements

### Potential Features
1. **Apply Chance Modifiers**:
   - Pristine anvils: +10% to all apply chances
   - Special materials: +5% to specific element types
   - Player perks/skills: Increase apply chances

2. **Tome Quality Influence**:
   - Higher quality enchantments = slightly better average apply chances
   - Legendary enchantments: minimum 20% apply chance
   - Common enchantments: can be as low as 0%

3. **Reroll System**:
   - Special items to reroll apply chances
   - Costs resources but gives new chances
   - Limited uses per tome

4. **Display Improvements**:
   - Show probability of getting N+ enchantments
   - "Overall Success Rate" calculation
   - Warning for very low chances

---

## ✅ Implementation Status

**COMPLETED**:
✅ Random apply chance generation (0-100%)
✅ NBT storage (MMO_Enchantment_X_ApplyChance)
✅ Color-coded lore display
✅ Synchronized lore and NBT values
✅ Documentation created

**PENDING**:
⏳ Custom anvil system to use apply chances
⏳ Roll mechanics for enchantment transfer
⏳ Success/failure feedback to players
⏳ Tome consumption logic

---

## 📝 Summary

Every Enchanted Tome now contains **randomized apply chances** (0-100%) for each enchantment, displayed in color-coded lore and stored in NBT data. This prepares for the custom anvil system where players will roll against these chances to transfer enchantments to equipment.

**Result**: 
- ✨ More exciting and unpredictable enchanting
- 🎲 Risk vs reward gameplay
- 💰 Dynamic tome economy
- 🎯 Memorable player experiences

**Ready for**: Custom anvil system implementation! 🔨
