# Enchantment GUI Animation System

## Overview
Added animated transitions to both the **Enchantment Table GUI** and **Custom Anvil GUI** to create a more engaging and interactive experience when enchanting and combining items.

## Features

### 🎨 **2-Second Animation Sequence**
- When a player confirms an enchantment or combines items, an animated sequence plays for 2 seconds
- Colorful glass panes cycle through multiple colors (purple → magenta → pink → cyan → lime → yellow → orange → red → white)
- Animated display name with changing symbols (◆, ◇, ❖, ✦, ✧, ✪, ✫, ✬, ✭, ✮)
- Real-time progress bar showing completion percentage
- Sound effects play during the animation (chimes with increasing pitch)
- Particle effects spawn around the player (enchanting particles)

### ⏭️ **Skip Animation Options**
Players have **3 ways to skip the animation**:

1. **Click the Output Slot** - Clicking during animation immediately shows the result
2. **Close the GUI** - Closing the inventory cancels the animation and adds the result to player inventory
3. **Wait for Completion** - Animation completes after 2 seconds automatically

### 📦 **Inventory Management**
- If player inventory is full when skipping/closing:
  - Items are automatically dropped on the ground
  - Warning message displayed: "⚠ Inventory full! Item dropped on ground."
- Input items (fragments/materials) are consumed **immediately** before animation starts
- Result item appears in output slot after animation completes

## Implementation Details

### GUIAnimationHandler Class
**Location**: `com.server.enchantments.gui.GUIAnimationHandler`

**Key Methods**:
- `startAnimation(Player, Inventory, outputSlot, resultItem, onComplete)` - Initiates animation
- `cancelAnimation(Player)` - Stops animation and returns result item
- `hasActiveAnimation(Player)` - Checks if player has active animation
- `skipAnimation(Player)` - Immediately completes animation

**Animation Cycle**:
```
Frame 0-19 (40 ticks = 2 seconds, updates every 2 ticks)
├── Visual: Cycles through 10 different colored glass panes
├── Audio: Chime sound every 5 frames with increasing pitch
├── Particles: Enchanting particles spawn every 3 frames
└── Completion: Shows final result + level-up sound + totem particles
```

### Enchantment Table GUI Integration
**Modified Files**:
- `EnchantmentGUIListener.java`
  - `handleEnchantment()` - Starts animation instead of immediate result
  - `handleGUIClick()` - Detects animation skip on output slot click
  - `onInventoryClose()` - Handles animation cancellation and item return

**Behavior**:
1. Player clicks "Enchant" button
2. Input items consumed immediately
3. Animation starts in output slot
4. After 2 seconds → Result appears
5. Player takes result from output slot

### Custom Anvil GUI Integration
**Modified Files**:
- `AnvilGUIListener.java`
  - `handleCombine()` - Starts animation instead of immediate result
  - `handleGUIClick()` - Detects animation skip on output slot click
  - `onInventoryClose()` - Handles animation cancellation and item return

**Behavior**:
1. Player clicks preview/output slot to combine
2. XP and essence costs deducted
3. Input items consumed immediately
4. Animation starts in output slot
5. After 2 seconds → Result appears + particle effects
6. Player takes result from output slot

## Visual Effects

### Animation Frames
```
Purple Pane   → ◆ Enchanting... ◆  [████████░░░░░░░░░░░░] 40%
Magenta Pane  → ◇ Enchanting... ◇  [████████████░░░░░░░░] 60%
Pink Pane     → ❖ Enchanting... ❖  [████████████████░░░░] 80%
Cyan Pane     → ✦ Enchanting... ✦  [████████████████████] 100%
```

### Completion Effects
- **Sound**: Player level-up + enchantment table use
- **Particles**: Totem of Undying particles (20 particles)
- **Message**: "✓ Enchantment complete!" (green + gold)

### Anvil-Specific Effects
After animation completes, additional particle effects based on enchantments:
- **Elemental Bursts**: One for each unique element type on the result
- **Elemental Ring**: Ground ring effect using primary element

## User Experience

### Normal Flow
```
1. Player places item + fragments
2. Player clicks "Enchant" button
3. ✓ Inputs consumed immediately
4. 🎨 Animation plays (2 seconds)
5. ✨ Result appears with effects
6. Player takes result
```

### Quick Skip Flow
```
1. Player places item + fragments
2. Player clicks "Enchant" button
3. ✓ Inputs consumed immediately
4. 🎨 Animation starts
5. ⏭️ Player clicks output slot
6. ⚡ Animation skips instantly
7. ✨ Result appears immediately
8. Player takes result
```

### Close GUI Flow
```
1. Player places item + fragments
2. Player clicks "Enchant" button
3. ✓ Inputs consumed immediately
4. 🎨 Animation starts
5. 🚪 Player closes GUI
6. ⚡ Animation cancelled
7. 📦 Result added to inventory (or dropped)
8. ✅ "Animation skipped" message
```

## Technical Notes

### Thread Safety
- All animation updates run on the main server thread via `BukkitRunnable`
- Animation state tracked per player UUID in concurrent-safe HashMap
- Automatic cleanup when animation completes or is cancelled

### Performance
- Animation updates every 2 ticks (0.1 seconds)
- Maximum 20 frames per animation
- Particles limited to 3 per frame (enchanting) + burst at completion
- No performance impact when animations not active

### Edge Cases Handled
1. **Player disconnects during animation** - Animation cancelled, result lost
2. **Server reload during animation** - Animation state cleared on plugin disable
3. **Multiple animations same player** - Previous animation cancelled automatically
4. **Inventory full** - Items dropped on ground with warning message
5. **GUI closed during animation** - Result safely added to inventory

## Configuration
Currently no configuration options. Animation duration (2 seconds) and visual style are hardcoded.

Future enhancements could include:
- Configurable animation duration
- Custom animation styles per element type
- Disable animations option for performance
- Sound effect customization

## API Usage

### Starting an Animation
```java
GUIAnimationHandler.startAnimation(
    player,           // Player viewing GUI
    inventory,        // GUI inventory
    outputSlot,       // Slot index for animation
    resultItem,       // Final result to show
    () -> {           // Optional callback after completion
        // Custom logic here
    }
);
```

### Checking Animation Status
```java
if (GUIAnimationHandler.hasActiveAnimation(player)) {
    // Player has active animation
}
```

### Cancelling Animation
```java
ItemStack result = GUIAnimationHandler.cancelAnimation(player);
if (result != null) {
    // Animation was active, result item returned
    player.getInventory().addItem(result);
}
```

## Compatibility
- **Minecraft Version**: 1.20.6
- **Paper API**: Compatible
- **Resource Packs**: No conflicts, uses vanilla materials
- **Client-Side**: No client modifications required
- **Bedrock Players**: Fully compatible via Geyser

## Known Issues
None currently reported.

## Future Enhancements
- [ ] Configurable animation duration
- [ ] Per-element animation colors matching element theme
- [ ] Different animation styles (spiral, explosion, converge)
- [ ] Boss bar progress indicator option
- [ ] Title/subtitle animation messages
- [ ] Configurable particle effects
- [ ] Animation preview in settings GUI

---

**Version**: 1.0.0  
**Date**: October 22, 2025  
**Author**: AI Assistant
