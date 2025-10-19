# Effectiveness Message Cooldown System

## 🎯 Problem

Effectiveness messages were spamming players, especially with DoT (Damage over Time) enchantments:

**Before:**
```
Super Effective! 🔥 FIRE
Super Effective! 🔥 FIRE
Super Effective! 🔥 FIRE
Super Effective! 🔥 FIRE
Super Effective! 🔥 FIRE
Super Effective! 🔥 FIRE
Super Effective! 🔥 FIRE
Super Effective! 🔥 FIRE
```

**Issue**: Cinderwake's burn DoT triggers every **0.5 seconds**, sending a message **every tick** = 2 messages per second!

## ✅ Solution

Implemented a **10-second cooldown** per player-element combination.

### Cooldown System:

```java
// Track last message time per player-element
Map<String, Long> messageCooldowns
Key: "player-uuid:ELEMENT_NAME"
Value: timestamp of last message

// Cooldown duration
10,000 milliseconds (10 seconds)
```

### Logic:

1. **Check if message should be sent** (modifier outside neutral range)
2. **Check cooldown**: Has 10 seconds passed since last message for this element?
3. **If on cooldown**: Skip message, log suppression
4. **If cooldown expired**: Send message, update timestamp

## 📊 Before vs After

### Before (No Cooldown):

**Cinderwake Burn (5 second duration)**:
```
0.0s: Super Effective! 🔥 FIRE
0.5s: Super Effective! 🔥 FIRE
1.0s: Super Effective! 🔥 FIRE
1.5s: Super Effective! 🔥 FIRE
2.0s: Super Effective! 🔥 FIRE
2.5s: Super Effective! 🔥 FIRE
3.0s: Super Effective! 🔥 FIRE
3.5s: Super Effective! 🔥 FIRE
4.0s: Super Effective! 🔥 FIRE
4.5s: Super Effective! 🔥 FIRE
5.0s: Super Effective! 🔥 FIRE
```
**Total**: 11 messages in 5 seconds ❌

### After (10s Cooldown):

**Cinderwake Burn (5 second duration)**:
```
0.0s: Super Effective! 🔥 FIRE ✅ (first message)
0.5s: [suppressed - cooldown]
1.0s: [suppressed - cooldown]
1.5s: [suppressed - cooldown]
2.0s: [suppressed - cooldown]
2.5s: [suppressed - cooldown]
3.0s: [suppressed - cooldown]
3.5s: [suppressed - cooldown]
4.0s: [suppressed - cooldown]
4.5s: [suppressed - cooldown]
5.0s: [suppressed - cooldown]
10.0s: Super Effective! 🔥 FIRE ✅ (cooldown expired)
```
**Total**: 1 message per 10 seconds ✅

## 🔧 Implementation Details

### File Modified:
**EnchantmentDamageUtil.java**

### Changes:

1. **Added cooldown tracking map**:
```java
private static final Map<String, Long> messageCooldowns = new HashMap<>();
private static final long MESSAGE_COOLDOWN_MS = 10000; // 10 seconds
```

2. **Updated sendEffectivenessFeedback()**:
```java
// Check cooldown
String cooldownKey = attacker.getUniqueId() + ":" + element.name();
long currentTime = System.currentTimeMillis();

if (messageCooldowns.containsKey(cooldownKey)) {
    long lastMessageTime = messageCooldowns.get(cooldownKey);
    long timeSinceLastMessage = currentTime - lastMessageTime;
    
    if (timeSinceLastMessage < MESSAGE_COOLDOWN_MS) {
        // Still on cooldown, suppress message
        return;
    }
}

// Update cooldown timestamp
messageCooldowns.put(cooldownKey, currentTime);
```

## 🎮 Per-Element Cooldowns

The system tracks cooldowns **separately for each element**:

**Example**: Player attacking with both Fire and Lightning:
```
0s:  Fire attack     → "Super Effective! 🔥 FIRE" ✅
2s:  Lightning attack → "Super Effective! ⚡ LIGHTNING" ✅
4s:  Fire attack     → [suppressed - Fire on cooldown]
6s:  Lightning attack → [suppressed - Lightning on cooldown]
10s: Fire attack     → "Super Effective! 🔥 FIRE" ✅
12s: Lightning attack → "Super Effective! ⚡ LIGHTNING" ✅
```

Each element has its own independent 10-second cooldown.

## 🔍 Debug Logging

The system logs all cooldown decisions:

### Message Sent:
```
[FEEDBACK] Called: ImOnlyGod vs lilneet | Element: FIRE | Modifier: 1.193 | In Range: false
[FEEDBACK] Message sent for FIRE (cooldown set)
```

### Message Suppressed (Cooldown):
```
[FEEDBACK] Called: ImOnlyGod vs lilneet | Element: FIRE | Modifier: 1.193 | In Range: false
[FEEDBACK] Suppressed (cooldown: 9.5s remaining)
```

### Message Suppressed (Neutral):
```
[FEEDBACK] Called: ImOnlyGod vs lilneet | Element: FIRE | Modifier: 1.049 | In Range: true
[FEEDBACK] Suppressed (neutral range)
```

## 📊 Cooldown Duration Rationale

### Why 10 seconds?

1. **DoT Effects**: Most DoT enchantments last 3-10 seconds
   - Prevents spam during entire DoT duration
   - First tick shows effectiveness, rest are silent

2. **Combat Pacing**: 10 seconds is ~3-5 attack cycles
   - Not too frequent (spam)
   - Not too rare (loses feedback value)

3. **Multi-Element Builds**: Separate cooldowns per element
   - Fire, Lightning, Light can each send messages
   - But each respects its own 10s cooldown

### Alternative Durations:

| Duration | Effect | Use Case |
|----------|--------|----------|
| 5s | More frequent | Fast-paced PVP, rapid element switching |
| **10s** | **Balanced** | **DoT enchantments, normal combat** ✅ |
| 15s | Less frequent | Slower combat, minimize clutter |
| 20s | Rare | Only show on first hit of engagement |

**Current**: 10 seconds (can be adjusted by changing `MESSAGE_COOLDOWN_MS`)

## 🎯 Testing Results

### Expected Behavior:

**Test 1: Cinderwake Burn**
```
/enchant add cinderwake epic 3
Attack NPC
```
**Expected**:
- First hit: "Super Effective! 🔥 FIRE" ✅
- Burn ticks (0.5s interval): Silent for 10 seconds
- After 10s: Next message can appear

**Test 2: Rapid Attacks**
```
Attack, wait 2s, attack, wait 2s, attack, wait 2s
```
**Expected**:
- First attack: Message ✅
- Second attack (2s later): Suppressed
- Third attack (4s later): Suppressed
- Fourth attack (6s later): Suppressed
- Fifth attack (10s later): Message ✅

**Test 3: Multi-Element**
```
/enchant add cinderwake epic 3
/enchant add voltbrand epic 3
Attack (triggers both)
```
**Expected**:
- Fire message: "Super Effective! 🔥 FIRE" ✅
- Lightning message: "Super Effective! ⚡ LIGHTNING" ✅
- Both have separate 10s cooldowns

## ⚙️ Customization

To adjust cooldown duration, change this value:

```java
private static final long MESSAGE_COOLDOWN_MS = 10000; // 10 seconds

// Examples:
// 5 seconds:  MESSAGE_COOLDOWN_MS = 5000;
// 15 seconds: MESSAGE_COOLDOWN_MS = 15000;
// 30 seconds: MESSAGE_COOLDOWN_MS = 30000;
```

## 🧹 Cleanup

The cooldown map grows with `player-uuid:element` entries. Potential improvements:

1. **Auto-cleanup**: Remove entries after 1 minute of no activity
2. **Player logout**: Clear entries when player disconnects
3. **Max size**: Limit map to last 1000 entries (LRU cache)

**Current**: Simple map, no cleanup (acceptable for normal server sizes)

For large servers with 100+ players, consider implementing cleanup:

```java
// Example cleanup on player quit
@EventHandler
public void onPlayerQuit(PlayerQuitEvent event) {
    UUID playerId = event.getPlayer().getUniqueId();
    messageCooldowns.keySet().removeIf(key -> key.startsWith(playerId.toString()));
}
```

## ✅ Summary

| Feature | Before | After |
|---------|--------|-------|
| **Cinderwake (5s DoT)** | 11 messages | 1 message |
| **Voltbrand (3 chains)** | 3 messages | 1 message |
| **Rapid attacks** | Every hit | Once per 10s |
| **Multi-element** | Spam all | 1 per element per 10s |
| **User experience** | ❌ Overwhelming | ✅ Informative |

**Result**: Players get clear feedback without being spammed!

---

**Status**: ✅ **IMPLEMENTED - READY FOR TESTING**

**Build Required**: Yes - rebuild plugin to apply changes
