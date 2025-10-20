# Admin Commands Quick Reference

## Overview
The `/enchant` command has been restructured for better consistency and usability.

## Command Structure

### Fragment Management
```
/enchant give <player> <element> <tier> [amount]
```
Give enchantment fragments to a player.

**Elements**: fire, ice, lightning, nature, shadow, light, earth, wind, blood  
**Tiers**: 1-3  
**Amount**: Optional, defaults to 1  

**Examples**:
```
/enchant give Notch fire 1
/enchant give Notch fire 1 5
/enchant give Notch shadow 3 10
```

---

### Tome Management
```
/enchant tome <player> [amount] [enchanted]
```
Give enchanted tomes to a player.

**Parameters**:
- `player` - Target player name
- `amount` - Number of tomes (default: 1)
- `enchanted` - Whether to pre-enchant the tome with random enchants (default: false)

**Examples**:
```
/enchant tome Notch              # Give 1 blank tome
/enchant tome Notch 5            # Give 5 blank tomes
/enchant tome Notch 1 true       # Give 1 pre-enchanted tome
/enchant tome Notch 3 true       # Give 3 pre-enchanted tomes
```

---

### Anvil Management
```
/enchant anvil <player> [amount]
```
Give custom anvils to a player.

**Parameters**:
- `player` - Target player name
- `amount` - Number of anvils (default: 1)

**Examples**:
```
/enchant anvil Notch             # Give 1 anvil
/enchant anvil Notch 5           # Give 5 anvils
```

---

### GUI Access
```
/enchant open <altar|anvil>
```
Directly open enchantment GUIs without physical structures.

**GUI Types**:
- `altar` or `enchanter` - Opens the enchantment altar GUI
- `anvil` - Opens the custom anvil GUI

**Examples**:
```
/enchant open altar              # Open enchanter
/enchant open enchanter          # Same as above
/enchant open anvil              # Open anvil GUI
```

**Note**: Includes GUI-specific sounds for better user experience.

---

## Command Comparison

### Before (Old System)
```
/enchant give Notch tome                    # Mixed with fragments
/enchant give Notch anvil                   # Mixed with fragments
/enchant give Notch fire 1                  # Fragments
```

### After (New System)
```
/enchant tome Notch                         # Dedicated tome command
/enchant anvil Notch                        # Dedicated anvil command
/enchant give Notch fire 1                  # Fragments only
/enchant open altar                         # NEW: Direct GUI access
```

---

## Tab Completion

The command system includes intelligent tab completion:

1. **Main Command**: `/enchant <subcommand>`
   - Suggests: `give`, `tome`, `anvil`, `open`, `reload`, `debug`

2. **Give Command**: `/enchant give <player> <element> <tier> [amount]`
   - Auto-completes player names, elements, tiers

3. **Tome Command**: `/enchant tome <player> [amount] [enchanted]`
   - Auto-completes player names
   - Suggests "true" or "false" for enchanted parameter

4. **Anvil Command**: `/enchant anvil <player> [amount]`
   - Auto-completes player names

5. **Open Command**: `/enchant open <type>`
   - Suggests: `altar`, `anvil`, `enchanter`

---

## Permission Requirements

All commands require the same permission as before:
- **Permission**: `enchants.admin` (or OP status)

---

## Sound Effects

Each GUI interaction now includes contextual sound effects:

### Enchanter Sounds
- **Opening**: Enchantment table + mystical ambience
- **Item placement**: Item pickup sound
- **Fragment placement**: Amethyst crystal sounds
- **Enchanting success**: Enchantment table + level up
- **Enchanting failure**: Villager "no" sound
- **Item retrieval**: Level up + netherite equip

### Anvil Sounds
- **Opening**: Heavy anvil placement + iron door
- **Item placement**: Item pickup sound
- **Combining**: Anvil use + enchantment table
- **Success**: Anvil use + level up celebration
- **Failure**: Anvil break + item break
- **Preview update**: Subtle note block pling
- **Closing**: Chest close

---

## Implementation Details

### Why Separate Commands?

**Consistency**: Each item type has its own dedicated command
- Fragments: `/enchant give`
- Tomes: `/enchant tome`
- Anvils: `/enchant anvil`

**Clarity**: No more confusion about which format to use

**Flexibility**: Each command can have unique parameters
- Tomes can be blank or pre-enchanted
- Fragments can specify element and tier
- Anvils have simple amount parameter

**Extensibility**: Easy to add new item types in the future without cluttering `/enchant give`

### Direct GUI Access Benefits

**Convenience**: No need to place physical structures
- Save time during testing
- Access GUIs from anywhere
- Useful for debugging

**User Experience**: Includes appropriate sounds
- Anvil GUI: Heavy industrial sounds
- Enchanter GUI: Mystical magical sounds

**Safety**: Validates player eligibility
- Checks for active profile
- Ensures proper initialization

---

## Migration Guide

If you have existing scripts or aliases:

### Old Command → New Command

| Old Command | New Command |
|------------|-------------|
| `/enchant give @p tome` | `/enchant tome @p` |
| `/enchant give @p anvil` | `/enchant anvil @p` |
| `/enchant give @p fire 1` | `/enchant give @p fire 1` *(unchanged)* |

### Command Block Updates

If using command blocks, update them to use the new syntax:
```
# Old
/enchant give @p[distance=..5] tome

# New
/enchant tome @p[distance=..5]
```

---

## Related Systems

### Fragment Boosting
The fragment refund system works seamlessly with the new commands:
- Boost enchanted tomes to 100% apply chance
- Automatic refund of excess fragments
- See `FRAGMENT_BOOST_REFUND_SYSTEM.md` for details

### Custom Anvil System
The anvil GUI opened via `/enchant open anvil` has all features:
- Tome application with RNG
- Fragment boosting with refunds
- Preview system with uncertainty markers
- Sound effects for every interaction

---

## Troubleshooting

### "Unknown command" error
- Ensure you have the permission `enchants.admin` or OP status
- Check if the plugin is loaded: `/plugins`

### GUI doesn't open with `/enchant open`
- Verify you have an active profile
- Try `/profile list` to see your profiles
- Make sure you're using the correct GUI type: `altar`, `anvil`, or `enchanter`

### Tomes/anvils not appearing in inventory
- Check if inventory is full
- Items will drop on ground if inventory full
- Use `/clear` if needed to make space

### Sounds not playing
- Check client sound settings
- Verify you're not using a resource pack that removes sounds
- Some sounds may be very quiet - check volume levels

---

## Future Enhancements

Potential additions to the command system:
- `/enchant preview <item>` - View enchantment details on held item
- `/enchant clear <player>` - Remove all enchantments from item
- `/enchant copy` - Copy enchantments from one item to another
- `/enchant debug <system>` - Debug specific subsystems

---

## Support

For issues or questions:
1. Check `SOUND_EFFECTS_AND_COMMANDS_UPDATE.md` for implementation details
2. Review `ENCHANTMENT_GUI_FIXES_COMPLETE.md` for GUI system info
3. See `FRAGMENT_BOOST_REFUND_SYSTEM.md` for refund mechanics
