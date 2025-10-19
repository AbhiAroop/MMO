# Effectiveness Messages Troubleshooting Guide

## Issue
Effectiveness feedback messages (Super Effective, Ineffective, etc.) are not appearing in PVP combat for custom items/armor.

## Important: PVP-Only Feature

**⚠️ CRITICAL**: Effectiveness messages and affinity-based damage modifiers **ONLY work in Player vs Player (PVP) combat**.

They do **NOT** apply to:
- Player vs NPC (Citizens NPCs, custom NPCs)
- Player vs Mob (vanilla or custom mobs)
- NPC vs Player

### Why?
The categorized affinity system is designed for **competitive PVP balance**. NPCs and mobs don't have player profiles, stats, or equipment-based affinity values.

### Testing Requirements
You **MUST** use **TWO REAL PLAYERS** to see effectiveness messages:
- Player A (attacker) with offensive enchantments
- Player B (defender) with defensive enchantments or no enchantments

If you test against NPCs, you will see:
```
[TRIGGER] ✓ Triggering Cinderwake (Quality: EPIC, Level: III)
[AFFINITY] PlayerA Offense(FIRE): 10.0 | NPC Defense(FIRE): 0.0
```
But **NO** `[AFFINITY] PVP:` or `[FEEDBACK]` logs, and **NO in-game messages**.

---

## Debug Logging Added

I've added comprehensive debug logging to help identify the issue. After restarting your server with the new build, check your server console/logs for these messages:

### 0. Enchantment Trigger Detection (NEW!)
```
[TRIGGER] ON_HIT: Scanning 5 equipment pieces for ImOnlyGod
[TRIGGER] Item: CARROT_ON_A_STICK | Enchants found: 1
[TRIGGER] Enchant ID: cinderwake | Registry: Cinderwake | Trigger: ON_HIT
[TRIGGER] ✓ Triggering Cinderwake (Quality: EPIC, Level: III)
```
**What to look for:**
- **Enchants found: 0** = EnchantmentData.getEnchantmentsFromItem() cannot read custom item format
- **Enchants found: 1+** but **Registry: NOT FOUND** = Enchantment ID doesn't match registry
- **Trigger: ON_DAMAGED** when expecting ON_HIT = Wrong trigger type for weapon

### 1. Affinity Values Check
```
[AFFINITY] PlayerA Offense(FIRE): 10.0 | PlayerB Defense(FIRE): 0.0
```
**What to look for:**
- Are the affinity values showing as 0 when they should have values?
- If both offensive and defensive affinities are 0, the modifier will be 1.0 (neutral)

### 2. Damage Modifier Calculation
```
[AFFINITY] PVP: PlayerA vs PlayerB | Element: FIRE | Modifier: 1.000 | Bonus Dmg: 5.00
```
**What to look for:**
- **Modifier: 1.000** = Neutral (no message will be sent)
- **Modifier: 1.200** = Advantage (should show "Super Effective")
- **Modifier: 0.800** = Counter-mechanic active (should show "Ineffective")

### 3. Feedback Method Execution
```
[FEEDBACK] Called: PlayerA vs PlayerB | Element: FIRE | Modifier: 1.000 | In Range: true
[FEEDBACK] Suppressed (neutral range)
```
**What to look for:**
- **In Range: true** = Modifier is between 0.95-1.05, message suppressed
- **In Range: false** = Modifier is outside neutral range, message should be sent

---

## Common Issues & Solutions

### Issue 1: Affinity Values are 0
**Symptoms:**
```
[AFFINITY] PlayerA Offense(FIRE): 0.0 | PlayerB Defense(FIRE): 0.0
[AFFINITY] PVP: PlayerA vs PlayerB | Element: FIRE | Modifier: 1.000 | Bonus Dmg: 5.00
[FEEDBACK] Suppressed (neutral range)
```

**Cause:** StatScanManager not detecting enchantments on custom items

**Solutions:**

1. **Check if enchantments are properly stored on items:**
   ```
   /enchant info  (while holding the item)
   ```
   Should show the enchantment data in lore

2. **Verify EnchantmentData is reading from NBT:**
   - Custom items must have enchantment data stored in NBT
   - Check `EnchantmentData.getEnchantmentsFromItem()` is finding your custom enchantments

3. **Check StatScanManager is scanning custom items:**
   - Open `/stats` GUI and check if affinity values update when you equip items
   - If values don't update, StatScanManager might not recognize your custom item format

### Issue 2: Modifier Always 1.0 (Neutral)
**Symptoms:**
```
[AFFINITY] PlayerA Offense(FIRE): 5.0 | PlayerB Defense(FIRE): 5.0
[AFFINITY] PVP: PlayerA vs PlayerB | Element: FIRE | Modifier: 1.000
```

**Cause:** Balanced affinity values cancel out

**Solution:**
- Attacker needs 10+ **offensive** affinity in an element
- Defender needs 0 defensive affinity in that element
- OR test counter-mechanic: both have 20+ in same element (attacker offense, defender defense)

### Issue 3: Enchantments Not Triggering
**Symptoms:** No `[AFFINITY]` or `[FEEDBACK]` logs appear at all

**Cause:** Enchantments aren't being triggered by EnchantmentTriggerListener

**Solutions:**

1. **Check custom items have enchantment NBT data:**
   ```java
   // In EnchantmentData.getEnchantmentsFromItem()
   // Should find enchantments via PDC or NBT API
   ```

2. **Verify item is in correct slot:**
   - ON_HIT enchantments: Must be in main hand OR armor slots
   - ON_DAMAGED enchantments: Must be in armor slots

3. **Check EnchantmentTriggerListener is registered:**
   ```
   /plugins  (verify MMO plugin is green/enabled)
   ```

### Issue 4: Only Works for Native Enchantments
**Symptoms:** Messages work with `/enchant add` command but not with custom items

**Cause:** Custom item system not compatible with EnchantmentData format

**Solution:** Need to check how custom items store enchantment data

**Required Format:**
```java
// Custom items must store enchantments in Persistent Data Container (PDC)
// OR in NBT format that EnchantmentData.getEnchantmentsFromItem() can read

// Expected structure:
itemStack.getItemMeta().getPersistentDataContainer()
    .set(new NamespacedKey(plugin, "enchantments"), PersistentDataType.STRING, encodedData)
```

---

## Testing Procedure

### Step 1: Verify Affinity Values Update
1. Give yourself a Fire offensive enchantment:
   ```
   /enchant add cinderwake epic 3
   ```

2. Open stats GUI:
   ```
   /stats
   ```

3. Check affinity section:
   - **Offensive Affinity** → Fire should be **10** (colored yellow/gold)
   - If Fire shows **0** (dark gray), StatScanManager isn't detecting the enchantment

### Step 2: Test PVP with Known Good Setup
1. **Player A** (Attacker):
   ```
   /enchant add cinderwake epic 3  (on sword)
   ```
   Expected: Fire Offense = 10

2. **Player B** (Defender):
   ```
   No enchantments
   ```
   Expected: All affinities = 0

3. **Attack Player B with the sword**

4. **Check console logs:**
   ```
   [AFFINITY] PlayerA Offense(FIRE): 10.0 | PlayerB Defense(FIRE): 0.0
   [AFFINITY] PVP: PlayerA vs PlayerB | Element: FIRE | Modifier: 1.190 | Bonus Dmg: X
   [FEEDBACK] Called: PlayerA vs PlayerB | Element: FIRE | Modifier: 1.190 | In Range: false
   ```

5. **Expected in-game messages:**
   - Player A sees: `§a⚡ Super Effective! §7(🔥 §cFire§7)`
   - Player B sees: `§c⚠ You're vulnerable to 🔥 §cFire!`

### Step 3: Test Counter-Mechanic
1. **Player A** (Attacker):
   ```
   /enchant add cinderwake epic 3  (Fire offense +10)
   ```

2. **Player B** (Defender):
   ```
   /enchant add FIRE_DEFENSIVE_ENCHANT epic 3  (Fire defense +10)
   /enchant add FIRE_DEFENSIVE_ENCHANT epic 3  (Fire defense +10 again = 20 total)
   ```
   Note: You need a Fire defensive enchantment. If none exists, use Earth/Water defense instead.

3. **Attack Player B**

4. **Expected console logs:**
   ```
   [AFFINITY] PlayerA Offense(FIRE): 10.0 | PlayerB Defense(FIRE): 20.0
   [AFFINITY] PVP: PlayerA vs PlayerB | Element: FIRE | Modifier: 0.800
   [FEEDBACK] Called: PlayerA vs PlayerB | Element: FIRE | Modifier: 0.800 | In Range: false
   ```

5. **Expected in-game messages:**
   - Player A sees: `§c✗ It's ineffective... §7(🔥 §cFire§7)`
   - Player B sees: `§a✓ Strong resistance to 🔥 §cFire!`

---

## Custom Item Integration Checklist

If messages work with `/enchant add` but NOT with your custom items:

### ✅ Requirements for Custom Items

1. **Enchantment data must be stored in NBT/PDC:**
   ```java
   // Check EnchantmentData.getEnchantmentsFromItem() can read your format
   List<EnchantmentData> enchants = EnchantmentData.getEnchantmentsFromItem(customItem);
   // Should return non-empty list
   ```

2. **Custom items must pass through EnchantmentTriggerListener:**
   ```java
   // In handleOnHitTriggers() and handleOnDamagedTriggers()
   // The listener scans equipment using:
   ItemStack[] equipment = getPlayerEquipment(player);
   // Your custom items must be in these slots
   ```

3. **Enchantment IDs must match registry:**
   ```java
   CustomEnchantment enchantment = EnchantmentRegistry.getInstance()
       .getEnchantment(data.getEnchantmentId());
   // Must return non-null
   ```

4. **Trigger types must be correct:**
   ```java
   // ON_HIT = Weapon enchantments (trigger when you attack)
   // ON_DAMAGED = Armor enchantments (trigger when you're hit)
   ```

### 🔧 Debug Your Custom Item System

Add logging to `EnchantmentTriggerListener.handleOnHitTriggers()`:

```java
private void handleOnHitTriggers(Player player, EntityDamageByEntityEvent event) {
    ItemStack[] equipment = getPlayerEquipment(player);
    
    for (ItemStack item : equipment) {
        if (item == null || item.getType() == Material.AIR) continue;
        
        List<EnchantmentData> enchantments = EnchantmentData.getEnchantmentsFromItem(item);
        
        // ADD THIS DEBUG LOG:
        Main.getInstance().getLogger().info(
            String.format("[TRIGGER] Item: %s | Enchants found: %d", 
                item.getType(), enchantments.size()));
        
        for (EnchantmentData data : enchantments) {
            // ... rest of code
        }
    }
}
```

**Expected output when attacking:**
```
[TRIGGER] Item: DIAMOND_SWORD | Enchants found: 1
[AFFINITY] PlayerA Offense(FIRE): 10.0 | PlayerB Defense(FIRE): 0.0
```

**If you see:**
```
[TRIGGER] Item: DIAMOND_SWORD | Enchants found: 0
```
Then `EnchantmentData.getEnchantmentsFromItem()` cannot read your custom item format.

---

## Modifier Calculation Formula

For reference, here's how modifiers are calculated:

```
Attacker Offense: 50
Defender Defense: 30
Relative Affinity: 50 - 30 = 20

Base Modifier = tanh(20 / 50) × 0.25 = 0.190
Counter Check = Defender Defense >= 20? YES
Counter Penalty = -0.20

Final Modifier = 1.0 + 0.190 - 0.20 = 0.99 (NEUTRAL - no message)
```

```
Attacker Offense: 50
Defender Defense: 0
Relative Affinity: 50 - 0 = 50

Base Modifier = tanh(50 / 50) × 0.25 = 0.190
Counter Check = Defender Defense >= 20? NO
Counter Penalty = 0.0

Final Modifier = 1.0 + 0.190 + 0.0 = 1.19 (SUPER EFFECTIVE!)
```

---

## Quick Diagnostic Commands

Run these in-game to check your setup:

```bash
# 1. Verify plugin loaded
/plugins

# 2. Check enchantment on held item
/enchant info

# 3. View your affinity values
/stats

# 4. Enable debug mode (if available)
/debug enchanting on
```

---

## Next Steps Based on Logs

### If logs show:
**`[AFFINITY] ... Modifier: 1.000`** (always neutral)
→ **Issue:** Affinity values are too low or balanced
→ **Fix:** Increase offensive affinity or decrease defensive affinity

### If logs show:
**`[FEEDBACK] Suppressed (neutral range)`** (always suppressed)
→ **Issue:** Modifier is between 0.95-1.05
→ **Fix:** Need bigger affinity difference (aim for 20+ offense vs 0 defense)

### If logs don't appear at all:
→ **Issue:** Enchantments not triggering
→ **Fix:** Check EnchantmentData format on custom items

### If logs only appear with `/enchant add` items:
→ **Issue:** Custom item system incompatible
→ **Fix:** Modify custom item creation to match EnchantmentData format

---

## Contact Information

After testing, provide these logs:
1. Console output showing `[AFFINITY]` logs
2. Console output showing `[FEEDBACK]` logs
3. Screenshot of `/stats` GUI showing affinity values
4. Which items work (command-created vs custom items)

This will help identify the exact issue!

---

**Build Version:** mmo-0.0.1 (with debug logging)
**Last Updated:** October 19, 2025
**Status:** Deployed and ready for testing
