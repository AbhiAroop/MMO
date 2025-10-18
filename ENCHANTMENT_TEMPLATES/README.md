# Enchantment Implementation Templates

This folder contains ready-to-use templates for implementing new enchantments with the affinity PVP system integrated.

## Available Templates

### 1. **OffensiveSimpleTemplate.java**
**Use for:** Basic damage enchantments with proc chance
- Simple bonus damage on hit
- Optional cooldown system
- Automatic affinity integration via `EnchantmentDamageUtil`
- **Examples:** InfernoStrike, bonus damage procs

### 2. **OffensiveAreaTemplate.java**
**Use for:** Area-of-effect damage enchantments
- Creates persistent damage zones
- BukkitRunnable for duration-based effects
- Particle effects and entity checking
- Affinity applied per target
- **Examples:** Cinderwake (burning trails), damage zones

### 3. **OffensiveChainTemplate.java**
**Use for:** Chain/bounce damage effects
- Charge-based activation (every Nth hit)
- Finds and damages nearby enemies
- Visual chain effects between targets
- Damage reduction per bounce
- **Examples:** Voltbrand (chain lightning), bouncing projectiles

### 4. **UtilityBuffTemplate.java**
**Use for:** Buff/debuff enchantments
- Potion effects on trigger
- Kill-based activation example
- Stealth/invisibility mechanics
- **Examples:** Ashen Veil (invisibility on kill), movement speed buffs

## Quick Start Guide

### Step 1: Choose Your Template
Pick the template that best matches your enchantment's behavior:
- **Damage on hit?** → OffensiveSimpleTemplate
- **Area damage over time?** → OffensiveAreaTemplate  
- **Chain between enemies?** → OffensiveChainTemplate
- **Buff/debuff effect?** → UtilityBuffTemplate

### Step 2: Copy and Rename
```bash
cp OffensiveSimpleTemplate.java ../src/main/java/com/server/enchantments/abilities/offensive/MyEnchantment.java
```

### Step 3: Customize

#### Change Basic Info
```java
public MyEnchantment() {
    super(
        "my_enchant_id",           // Unique ID (lowercase_with_underscores)
        "My Enchantment",           // Display name
        "What it does",             // Description
        EnchantmentRarity.RARE,     // COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC
        ElementType.FIRE            // Element type
    );
}
```

#### Define Stats
```java
@Override
public double[] getBaseStats() {
    // Return array of stats your enchantment uses
    // These will be scaled by quality automatically
    return new double[]{
        0.25,  // proc_chance (25%)
        5.0,   // bonus_damage
        40.0   // duration_ticks (2 seconds)
    };
}
```

#### Specify Valid Items
```java
@Override
public boolean canApplyTo(ItemStack item) {
    if (item == null) return false;
    Material type = item.getType();
    
    return type == Material.DIAMOND_SWORD ||
           type == Material.NETHERITE_SWORD ||
           type.name().endsWith("_AXE");
}
```

### Step 4: Register
Add to `EnchantmentRegistry.java`:
```java
public EnchantmentRegistry() {
    // ... existing enchantments
    register(new MyEnchantment());
}
```

## Affinity Integration

### Automatic (Offensive Enchantments)
Use `EnchantmentDamageUtil.addBonusDamageToEvent()` and affinity is applied automatically:
```java
EnchantmentDamageUtil.addBonusDamageToEvent(
    damageEvent,
    bonusDamage,
    getElement()  // Affinity modifier applied in PVP automatically
);
```

### Manual (DOT/Area Effects)
For damage-over-time or custom damage application:
```java
if (target instanceof Player) {
    // PVP - apply affinity modifier
    double modifier = AffinityModifier.calculateDamageModifier(
        player, (Player) target, getElement());
    double modifiedDamage = baseDamage * modifier;
    target.damage(modifiedDamage, player);
} else {
    // PVE - no affinity
    target.damage(baseDamage, player);
}
```

### Effect Duration Modifiers
For buffs/debuffs in PVP:
```java
int baseDuration = 100; // ticks
if (target instanceof Player) {
    int finalDuration = AffinityModifier.applyEffectDurationModifier(
        baseDuration, player, (Player) target, getElement()
    );
    applyEffect(target, finalDuration);
}
```

### Proc Chance Modifiers
For chance-based effects in PVP:
```java
double baseChance = 0.25;
if (target instanceof Player) {
    double finalChance = AffinityModifier.applyProcChanceModifier(
        baseChance, player, (Player) target, getElement()
    );
    if (Math.random() < finalChance) {
        triggerEffect();
    }
}
```

## Common Patterns

### Cooldown System
```java
private static final Map<UUID, Long> cooldowns = new HashMap<>();
private static final long COOLDOWN_MS = 5000;

// In trigger method:
UUID playerId = player.getUniqueId();
long currentTime = System.currentTimeMillis();

if (cooldowns.containsKey(playerId)) {
    long lastUse = cooldowns.get(playerId);
    if (currentTime - lastUse < COOLDOWN_MS) {
        return; // Still on cooldown
    }
}

// ... enchantment effect ...

cooldowns.put(playerId, currentTime);
```

### Charge/Stack Tracking
```java
private static final Map<UUID, Integer> charges = new HashMap<>();
private static final int MAX_CHARGES = 5;

// In trigger method:
UUID playerId = player.getUniqueId();
int currentCharges = charges.getOrDefault(playerId, 0) + 1;

if (currentCharges >= MAX_CHARGES) {
    // Trigger charged effect
    triggerChargedEffect();
    charges.put(playerId, 0);
} else {
    charges.put(playerId, currentCharges);
}
```

### Area Effect with BukkitRunnable
```java
new BukkitRunnable() {
    int ticksRemaining = duration;
    
    @Override
    public void run() {
        if (ticksRemaining <= 0) {
            cancel();
            return;
        }
        
        // Spawn particles
        location.getWorld().spawnParticle(...);
        
        // Check for entities
        for (Entity entity : location.getNearbyEntities(radius, radius, radius)) {
            // Apply effect
        }
        
        ticksRemaining--;
    }
}.runTaskTimer(Main.getInstance(), 0L, 20L); // Every 20 ticks (1 second)
```

## Implementation Checklist

For each new enchantment:

- [ ] Copy appropriate template
- [ ] Rename class and file
- [ ] Update constructor (ID, name, description, rarity, element)
- [ ] Define base stats array
- [ ] Implement canApplyTo() with valid materials
- [ ] Implement trigger() logic
- [ ] Add affinity integration (automatic or manual)
- [ ] Add visual/audio feedback
- [ ] Test compilation
- [ ] Register in EnchantmentRegistry
- [ ] Test in-game
- [ ] Test PVP affinity modifiers
- [ ] Verify no affinity in PVE

## Troubleshooting

### "Cannot find symbol: getTriggerType()"
The templates don't implement this method - it's automatically handled by the event system. If you see this error, you may need to add an empty implementation or the CustomEnchantment base class has changed.

### Affinity Not Applying
- Check you're using `EnchantmentDamageUtil.addBonusDamageToEvent()` for automatic integration
- For manual damage, ensure you're checking `instanceof Player` and calling `AffinityModifier.calculateDamageModifier()`
- Verify both players have active profiles with affinity data

### Particles Not Showing
- Ensure world is not null
- Check particle type is valid for your Minecraft version
- Adjust particle count and spread for visibility

### Effect Not Triggering
- Verify enchantment is registered in EnchantmentRegistry
- Check event type matches (EntityDamageByEntityEvent, EntityDeathEvent, etc.)
- Add debug messages to confirm trigger() is being called
- Verify canApplyTo() returns true for your test item

## Element Types

```java
ElementType.FIRE       // Physical damage
ElementType.WATER      // Physical damage
ElementType.EARTH      // Physical damage  
ElementType.AIR        // Physical damage
ElementType.NATURE     // Physical damage
ElementType.LIGHTNING  // Magical damage
ElementType.SHADOW     // Magical damage
ElementType.LIGHT      // Magical damage
```

## Rarity Levels

```java
EnchantmentRarity.COMMON      // 1.0x effectiveness
EnchantmentRarity.UNCOMMON    // 1.2x effectiveness
EnchantmentRarity.RARE        // 1.5x effectiveness
EnchantmentRarity.EPIC        // 2.0x effectiveness
EnchantmentRarity.LEGENDARY   // 2.5x effectiveness
EnchantmentRarity.MYTHIC      // 3.0x effectiveness
```

## Quality Scaling

Quality automatically scales your base stats:
- **Poor**: 0.5x
- **Common**: 0.7x
- **Uncommon**: 0.9x
- **Rare**: 1.1x
- **Epic**: 1.4x
- **Legendary**: 1.7x
- **Godly**: 2.0x

Access scaled stats with:
```java
double[] stats = getScaledStats(quality);
double procChance = stats[0];  // Already scaled by quality
double damage = stats[1];       // Already scaled by quality
```

## Need Help?

Refer to:
- **ENCHANTMENT_FRAMEWORK.md** - Detailed implementation guide
- **AFFINITY_PVP_SYSTEM.md** - Complete affinity system documentation
- **InfernoStrike.java** - Real working example
- Existing enchantments in `/abilities/` folders

---

**Remember:** The affinity system is already integrated! Just use the templates and focus on your enchantment's unique mechanics.
