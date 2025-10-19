# Custom Items Enchantment Triggering Issue - Analysis & Solution

## Problem Summary

**Issue**: Effectiveness messages (Super Effective, Ineffective, etc.) not appearing for custom items/armor in PVP combat.

**Root Cause**: Custom items do not have enchantments stored in the NBT format that `EnchantmentData.getEnchantmentsFromItem()` expects, so `EnchantmentTriggerListener` cannot detect them and never triggers their abilities.

---

## Evidence from Logs

### What's Working ✅
```
[DEBUG:ENCHANTING] Added affinity: FIRE: 70 (OFF: 10, DEF: 0, UTIL: 0)
[DEBUG:ENCHANTING] Scanned affinity for ImOnlyGod - Total: 70.0
```
- StatScanManager **IS** detecting affinities from custom items
- Custom items **DO** have enchantment information somewhere

### What's NOT Working ❌
```
No [TRIGGER] logs during combat
No [AFFINITY] logs from calculateDamageModifier
No [FEEDBACK] logs from sendEffectivenessFeedback
```
- EnchantmentTriggerListener is **NOT** finding enchantments during combat
- Enchantment abilities are **NEVER** being triggered
- Damage modifiers are **NEVER** being applied

---

## Why StatScanManager Works But TriggerListener Doesn't

### StatScanManager (Works)
Located in: `StatScanManager.java` line ~1571

```java
// StatScanManager has TWO methods to find enchantments:

// Method 1: Try standard NBT format
List<EnchantmentData> enchantmentsFromNBT = 
    EnchantmentData.getEnchantmentsFromItem(item);

// Method 2: FALLBACK - Parse from lore if NBT fails
if (enchantmentsFromNBT.isEmpty()) {
    // Custom parsing logic that reads enchantment info from item lore
    // This is why affinity scanning works!
}
```

### EnchantmentTriggerListener (Broken)
Located in: `EnchantmentTriggerListener.java` line ~76

```java
// TriggerListener ONLY uses standard NBT format:
List<EnchantmentData> enchantments = 
    EnchantmentData.getEnchantmentsFromItem(item);

// NO FALLBACK! If NBT is empty, list is empty, no triggers fire!
```

---

## The Solution

You have **TWO OPTIONS**:

### Option A: Add Fallback Parsing to EnchantmentTriggerListener (Recommended)

Copy the fallback logic from StatScanManager into EnchantmentTriggerListener so it can also read enchantments from lore.

**Pros:**
- Minimal changes to custom item system
- Maintains backward compatibility
- Works immediately

**Cons:**
- Code duplication
- Parsing lore is slower than NBT

---

### Option B: Store Enchantments in NBT Format (Best Long-Term)

Modify your custom item creation system to store enchantments in the NBT format that `EnchantmentData.getEnchantmentsFromItem()` expects.

**Required NBT Structure:**
```java
// When creating custom items with enchantments:
NBTItem nbtItem = new NBTItem(item);

// Set enchantment count
nbtItem.setInteger("CustomEnchantments_Count", 1);

// For each enchantment (index i):
String prefix = "CustomEnchantments_" + i + "_";
nbtItem.setString(prefix + "ID", "cinderwake");
nbtItem.setString(prefix + "Quality", "EPIC");
nbtItem.setInteger(prefix + "Level", 3);
nbtItem.setInteger(prefix + "StatCount", 2); // Number of scaled stats
nbtItem.setDouble(prefix + "Stat_0", 0.35); // Proc chance
nbtItem.setDouble(prefix + "Stat_1", 8.0);  // Bonus damage
nbtItem.setInteger(prefix + "Affinity", 10);
nbtItem.setString(prefix + "Type", "SINGLE");
nbtItem.setString(prefix + "Element", "FIRE");

// Apply NBT to item
item = nbtItem.getItem();
```

**Pros:**
- Clean, performant solution
- All systems work uniformly
- No code duplication

**Cons:**
- Requires modifying custom item creation code
- Need to update all existing custom items

---

## Immediate Workaround (Option A Implementation)

Here's the code to add to `EnchantmentTriggerListener.java`:

### Step 1: Add Helper Method

Add this method to `EnchantmentTriggerListener` class:

```java
/**
 * Get enchantments from item with fallback to lore parsing.
 * This handles custom items that don't store enchantments in NBT.
 */
private List<EnchantmentData> getEnchantmentsWithFallback(ItemStack item) {
    // Try NBT first
    List<EnchantmentData> enchantments = EnchantmentData.getEnchantmentsFromItem(item);
    
    if (!enchantments.isEmpty()) {
        return enchantments; // NBT format worked
    }
    
    // FALLBACK: Parse from lore (same logic as StatScanManager)
    if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) {
        return enchantments; // No lore to parse
    }
    
    List<String> lore = item.getItemMeta().getLore();
    
    // Look for enchantment section in lore
    boolean inEnchantSection = false;
    
    for (String line : lore) {
        String stripped = ChatColor.stripColor(line).trim();
        
        // Check for enchantment section header
        if (stripped.equals("Enchantments:")) {
            inEnchantSection = true;
            continue;
        }
        
        // Exit enchantment section
        if (inEnchantSection && stripped.isEmpty()) {
            break;
        }
        
        // Parse enchantment line
        if (inEnchantSection && stripped.startsWith("•")) {
            try {
                // Example format: "• [EPIC III] Cinderwake"
                String enchantInfo = stripped.substring(1).trim(); // Remove bullet
                
                // Extract quality and level from brackets
                int bracketEnd = enchantInfo.indexOf(']');
                if (bracketEnd == -1) continue;
                
                String bracketContent = enchantInfo.substring(1, bracketEnd); // Remove [
                String[] parts = bracketContent.split(" ");
                
                EnchantmentQuality quality = EnchantmentQuality.valueOf(parts[0]);
                EnchantmentLevel level = EnchantmentLevel.fromRoman(parts[1]);
                
                // Extract enchantment name
                String enchantName = enchantInfo.substring(bracketEnd + 1).trim();
                
                // Convert display name to ID (lowercase, no spaces)
                String enchantId = enchantName.toLowerCase().replace(" ", "");
                
                // Get enchantment from registry to get proper stats
                CustomEnchantment enchant = EnchantmentRegistry.getInstance().getEnchantment(enchantId);
                if (enchant == null) continue;
                
                // Get scaled stats
                double[] stats = enchant.getScaledStats(quality, level);
                
                // Get affinity contribution
                int affinity = 10; // Default contribution
                
                // Create EnchantmentData
                EnchantmentData data = new EnchantmentData(
                    enchantId, 
                    quality, 
                    level, 
                    stats, 
                    affinity,
                    enchant.getElement(),
                    null, // hybrid
                    false // isHybrid
                );
                
                enchantments.add(data);
                
            } catch (Exception e) {
                // Skip malformed enchantment lines
                Main.getInstance().getLogger().warning(
                    "[TRIGGER] Failed to parse enchantment from lore: " + line);
            }
        }
    }
    
    return enchantments;
}
```

### Step 2: Replace All Calls

Replace this:
```java
List<EnchantmentData> enchantments = EnchantmentData.getEnchantmentsFromItem(item);
```

With this:
```java
List<EnchantmentData> enchantments = getEnchantmentsWithFallback(item);
```

In these methods:
- `handleOnHitTriggers()`
- `handleOnDamagedTriggers()`
- `handleOnKillTriggers()`
- `handlePassiveTriggers()`

---

## Testing After Fix

### Step 1: Restart Server
Reload the plugin with the updated code.

### Step 2: Test PVP Combat
Player A (Attacker) with Fire offensive enchantment attacks Player B (Defender) with no defense.

### Step 3: Check Console
You should now see:
```
[TRIGGER] ON_HIT: Scanning 5 equipment pieces for ImOnlyGod
[TRIGGER] Item: CARROT_ON_A_STICK | Enchants found: 1
[TRIGGER] Enchant ID: cinderwake | Registry: Cinderwake | Trigger: ON_HIT
[TRIGGER] ✓ Triggering Cinderwake (Quality: EPIC, Level: III)
[AFFINITY] ImOnlyGod Offense(FIRE): 10.0 | lilneet Defense(FIRE): 0.0
[AFFINITY] PVP: ImOnlyGod vs lilneet | Element: FIRE | Modifier: 1.190 | Bonus Dmg: 9.52
[FEEDBACK] Called: ImOnlyGod vs lilneet | Element: FIRE | Modifier: 1.190 | In Range: false
```

### Step 4: Verify In-Game
- Attacker sees: `§a⚡ Super Effective! §7(🔥 §cFire§7)`
- Defender sees: `§c⚠ You're vulnerable to 🔥 §cFire!`

---

## Long-Term Recommendation

**Implement Option B** - Update your custom item creation system to store enchantments in NBT format.

### Where to Make Changes

Find where custom items are created in your codebase (likely in a CustomItemManager or similar), and add this code when applying enchantments:

```java
public static ItemStack addEnchantmentToCustomItem(ItemStack item, 
                                                    String enchantId,
                                                    EnchantmentQuality quality,
                                                    EnchantmentLevel level) {
    // Get enchantment from registry
    CustomEnchantment enchant = EnchantmentRegistry.getInstance().getEnchantment(enchantId);
    if (enchant == null) return item;
    
    // Get scaled stats
    double[] stats = enchant.getScaledStats(quality, level);
    
    // Create EnchantmentData
    EnchantmentData data = new EnchantmentData(
        enchantId,
        quality,
        level,
        stats,
        10, // affinity
        enchant.getElement(),
        null, // hybrid
        false // isHybrid
    );
    
    // Store in NBT (this makes it compatible with TriggerListener!)
    data.addToItem(item);
    
    // Also update lore for visual display
    updateItemLoreWithEnchantment(item, data);
    
    return item;
}
```

This way:
- ✅ StatScanManager works (reads from NBT)
- ✅ EnchantmentTriggerListener works (reads from NBT)
- ✅ Affinity system works
- ✅ Effectiveness messages work
- ✅ All enchantment abilities trigger correctly

---

## Summary

**Current State:**
- Custom items have enchantment info in **lore only**
- StatScanManager has **fallback parser** → affinity detection works
- EnchantmentTriggerListener has **NO fallback** → enchantments never trigger

**Quick Fix (Option A):**
- Add fallback parser to EnchantmentTriggerListener
- Copy logic from StatScanManager
- Works immediately but duplicates code

**Proper Fix (Option B):**
- Store enchantments in NBT when creating custom items
- Use `EnchantmentData.addToItem()` method
- All systems work uniformly

---

**Status**: Ready for implementation
**Recommended Approach**: Option A for immediate fix, then Option B for long-term
**Build Version**: mmo-0.0.1 (with trigger debugging)
**Date**: October 19, 2025
