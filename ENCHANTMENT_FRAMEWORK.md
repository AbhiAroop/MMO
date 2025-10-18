# Enchantment Implementation Framework

This framework provides templates and guidelines for implementing the new enchantments with affinity system integration.

## Template Structure

Each enchantment should follow this structure:

### 1. Package Organization
```
src/main/java/com/server/enchantments/abilities/
├── offensive/          # Damage-dealing enchantments
├── defensive/          # Defense and mitigation
├── utility/            # Movement, stealth, support
└── hybrid/             # Dual-element enchantments
```

### 2. Base Enchantment Template

```java
package com.server.enchantments.abilities.[category];

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;
import com.server.enchantments.utils.AffinityModifier;
import com.server.enchantments.utils.EnchantmentDamageUtil;

/**
 * [Enchantment Name]
 * Equipment: [List of valid equipment]
 * Effect: [Brief description]
 * Role: [Role in combat]
 * 
 * Base Stats: [stat1, stat2, stat3]
 * Quality Scaling: [multiplier increases per quality tier]
 */
public class [EnchantmentName] extends CustomEnchantment {
    
    public [EnchantmentName]() {
        super(
            "[enchantment_id]",           // Lowercase with underscores
            "[Display Name]",              // User-facing name
            "[Short description]",         // Tooltip description
            EnchantmentRarity.[RARITY],   // COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC
            ElementType.[ELEMENT]          // FIRE, WATER, EARTH, AIR, NATURE, LIGHTNING, SHADOW, LIGHT
        );
    }
    
    @Override
    public boolean canApplyTo(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        
        // List all valid materials
        return type == Material.DIAMOND_SWORD ||
               type == Material.NETHERITE_SWORD;
               // Add more as needed
    }
    
    @Override
    public double[] getBaseStats() {
        // Define base effectiveness values
        // Example: [proc_chance, damage, duration]
        return new double[]{0.15, 5.0, 40.0};
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        // Get scaled stats
        double[] stats = getScaledStats(quality);
        
        // Implement enchantment logic
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
            
            if (damageEvent.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) damageEvent.getEntity();
                
                // Your enchantment logic here
            }
        }
    }
}
```

## Enchantment Categories & Patterns

### Offensive Enchantments (Damage Dealing)

Use `EnchantmentDamageUtil.addBonusDamageToEvent()` for automatic affinity integration:

```java
@Override
public void trigger(Player player, EnchantmentQuality quality, Event event) {
    if (!(event instanceof EntityDamageByEntityEvent)) return;
    EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
    
    double[] stats = getScaledStats(quality);
    double procChance = stats[0];
    double bonusDamage = stats[1];
    
    // Check proc chance
    if (Math.random() > procChance) return;
    
    // Apply damage with automatic affinity modifier in PVP
    EnchantmentDamageUtil.addBonusDamageToEvent(
        damageEvent,
        bonusDamage,
        getElement()  // Automatically applies affinity in PVP
    );
    
    // Visual/audio feedback
    player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
    player.sendMessage("§c⚔ Bonus damage dealt!");
}
```

### DOT (Damage Over Time) Enchantments

Apply affinity to effect duration:

```java
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Override
public void trigger(Player player, EnchantmentQuality quality, Event event) {
    if (!(event instanceof EntityDamageByEntityEvent)) return;
    EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
    
    if (!(damageEvent.getEntity() instanceof LivingEntity)) return;
    LivingEntity target = (LivingEntity) damageEvent.getEntity();
    
    double[] stats = getScaledStats(quality);
    int baseDuration = (int) stats[0]; // in ticks
    int damagePerTick = (int) stats[1];
    
    // Apply affinity to duration in PVP
    int finalDuration = baseDuration;
    if (target instanceof Player) {
        finalDuration = AffinityModifier.applyEffectDurationModifier(
            baseDuration, player, (Player) target, getElement()
        );
    }
    
    // Apply the effect
    target.setFireTicks(finalDuration);
    // Or use custom damage-over-time logic
}
```

### Proc-Based Enchantments

Apply affinity to proc chance:

```java
@Override
public void trigger(Player player, EnchantmentQuality quality, Event event) {
    if (!(event instanceof EntityDamageByEntityEvent)) return;
    EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
    
    double[] stats = getScaledStats(quality);
    double baseProcChance = stats[0];
    
    // Apply affinity to proc chance in PVP
    double finalProcChance = baseProcChance;
    if (damageEvent.getEntity() instanceof Player) {
        finalProcChance = AffinityModifier.applyProcChanceModifier(
            baseProcChance, player, (Player) damageEvent.getEntity(), getElement()
        );
    }
    
    // Check if effect procs
    if (Math.random() < finalProcChance) {
        // Trigger special effect
    }
}
```

### Defensive Enchantments

Implement in CombatListener or use player metadata:

```java
import org.bukkit.metadata.FixedMetadataValue;
import com.server.Main;

@Override
public void trigger(Player player, EnchantmentQuality quality, Event event) {
    double[] stats = getScaledStats(quality);
    double damageReduction = stats[0]; // e.g., 0.10 for 10% reduction
    
    // Store metadata that CombatListener can check
    player.setMetadata("terraheart_reduction", 
        new FixedMetadataValue(Main.getInstance(), damageReduction));
    
    // Schedule removal after duration
    // Or check in CombatListener for stationary condition
}
```

### Cooldown Management

Use a static cooldown map:

```java
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class [EnchantmentName] extends CustomEnchantment {
    
    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 5000; // 5 seconds
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check cooldown
        if (cooldowns.containsKey(playerId)) {
            long lastUse = cooldowns.get(playerId);
            if (currentTime - lastUse < COOLDOWN_MS) {
                return; // Still on cooldown
            }
        }
        
        // Effect logic here
        
        // Update cooldown
        cooldowns.put(playerId, currentTime);
    }
}
```

### Charge/Stack Tracking

Track charges using player metadata:

```java
@Override
public void trigger(Player player, EnchantmentQuality quality, Event event) {
    // Get current charge count
    int charges = 0;
    if (player.hasMetadata("voltbrand_charges")) {
        charges = player.getMetadata("voltbrand_charges").get(0).asInt();
    }
    
    // Increment charge
    charges++;
    player.setMetadata("voltbrand_charges", 
        new FixedMetadataValue(Main.getInstance(), charges));
    
    // Check if charged
    if (charges >= 5) {
        // Trigger charged effect
        triggerChargedEffect(player, quality, event);
        
        // Reset charges
        player.removeMetadata("voltbrand_charges", Main.getInstance());
    }
}
```

### Area Effects (Trails, Zones, Pulses)

Use BukkitRunnable for persistent effects:

```java
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Particle;

private void createBurningZone(Player player, Location center, int duration, double damagePerTick) {
    new BukkitRunnable() {
        int ticksRemaining = duration;
        
        @Override
        public void run() {
            if (ticksRemaining <= 0) {
                cancel();
                return;
            }
            
            // Spawn particles
            center.getWorld().spawnParticle(
                Particle.FLAME,
                center,
                10,
                0.5, 0.5, 0.5,
                0.01
            );
            
            // Check for entities in range
            for (Entity entity : center.getWorld().getNearbyEntities(center, 2.0, 2.0, 2.0)) {
                if (entity instanceof LivingEntity && entity != player) {
                    LivingEntity target = (LivingEntity) entity;
                    
                    // Apply damage with affinity
                    if (target instanceof Player) {
                        double modifier = AffinityModifier.calculateDamageModifier(
                            player, (Player) target, getElement());
                        double modifiedDamage = damagePerTick * modifier;
                        target.damage(modifiedDamage, player);
                    } else {
                        target.damage(damagePerTick, player);
                    }
                }
            }
            
            ticksRemaining--;
        }
    }.runTaskTimer(Main.getInstance(), 0L, 20L); // Every 1 second
}
```

### Chain/Bounce Effects

Find and damage nearby enemies:

```java
private void chainLightning(Player player, LivingEntity initialTarget, 
                           double damage, int maxChains) {
    Set<UUID> hitEntities = new HashSet<>();
    hitEntities.add(initialTarget.getUniqueId());
    
    LivingEntity currentTarget = initialTarget;
    
    for (int i = 0; i < maxChains; i++) {
        // Find next target
        LivingEntity nextTarget = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (Entity entity : currentTarget.getNearbyEntities(5.0, 5.0, 5.0)) {
            if (entity instanceof LivingEntity && !hitEntities.contains(entity.getUniqueId())) {
                LivingEntity living = (LivingEntity) entity;
                double distance = living.getLocation().distance(currentTarget.getLocation());
                
                if (distance < closestDistance) {
                    closestDistance = distance;
                    nextTarget = living;
                }
            }
        }
        
        if (nextTarget == null) break; // No more targets
        
        // Visual effect between targets
        drawLightningBolt(currentTarget.getLocation(), nextTarget.getLocation());
        
        // Apply damage with affinity
        if (nextTarget instanceof Player) {
            double modifier = AffinityModifier.calculateDamageModifier(
                player, (Player) nextTarget, getElement());
            nextTarget.damage(damage * modifier * 0.7, player); // Reduced per bounce
        } else {
            nextTarget.damage(damage * 0.7, player);
        }
        
        hitEntities.add(nextTarget.getUniqueId());
        currentTarget = nextTarget;
    }
}
```

### Hybrid Enchantments

Use HybridElement constructor:

```java
import com.server.enchantments.elements.HybridElement;

public class Stormfire extends CustomEnchantment {
    
    public Stormfire() {
        super(
            "stormfire",
            "Stormfire",
            "Flame arcs leap between enemies with Burn and Shock",
            EnchantmentRarity.LEGENDARY,
            HybridElement.ICE  // Or create new hybrids
        );
    }
    
    @Override
    public void trigger(Player player, EnchantmentQuality quality, Event event) {
        // Hybrid enchantments can check both elements for affinity
        // Affinity is automatically split 60/40 between elements when scanned
        
        // Apply both element effects
        applyFireEffect(player, quality, event);
        applyLightningEffect(player, quality, event);
    }
}
```

## Registration

Add to EnchantmentRegistry:

```java
// In EnchantmentRegistry.java
public class EnchantmentRegistry {
    
    public EnchantmentRegistry() {
        // Fire
        register(new Cinderwake());
        register(new AshenVeil());
        
        // Water
        register(new Deepcurrent());
        register(new Mistveil());
        
        // ... etc
    }
}
```

## Testing Checklist

For each enchantment:

- [ ] Compiles without errors
- [ ] Registered in EnchantmentRegistry
- [ ] Can be applied to correct items
- [ ] Base stats scale properly with quality
- [ ] Triggers on correct events
- [ ] Visual/audio feedback works
- [ ] Affinity modifiers apply in PVP
- [ ] No affinity modifiers in PVE
- [ ] Cooldowns work correctly
- [ ] Stacks/charges track properly
- [ ] Particles spawn correctly
- [ ] No performance issues with runnable tasks

## Common Imports

```java
// Core
import com.server.Main;
import com.server.enchantments.data.CustomEnchantment;
import com.server.enchantments.data.EnchantmentQuality;
import com.server.enchantments.data.EnchantmentRarity;
import com.server.enchantments.elements.ElementType;
import com.server.enchantments.elements.HybridElement;

// Affinity System
import com.server.enchantments.utils.AffinityModifier;
import com.server.enchantments.utils.EnchantmentDamageUtil;

// Bukkit
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

// Java
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
```

## Quick Reference: Enchantment Stats

### Cinderwake (Fire - Offensive)
- Base: [duration_ticks(40), damage_per_tick(2.0), trail_length(3.0)]
- Equipment: Sword, Dagger, Spear
- Pattern: Area Effect with Runnable

### Ashen Veil (Fire - Utility)
- Base: [invisibility_duration(20), cooldown_reduction(0.0)]
- Equipment: Light Armor, Cloak
- Pattern: Event-triggered buff

### Deepcurrent (Water - Control)
- Base: [meter_threshold(5), knockback_power(2.0), radius(3.0)]
- Equipment: Greatsword, Hammer, Gauntlets
- Pattern: Charge tracking + Area knockback

### Mistveil (Water - Defensive)
- Base: [deflect_duration(20), deflect_chance(0.8)]
- Equipment: Armor, Cloak, Ring
- Pattern: Event-triggered shield

### Burdened Stone (Earth - Control)
- Base: [slow_strength(1), stacks_to_immobilize(3), immobilize_duration(10)]
- Equipment: Hammer, Mace, Shield
- Pattern: Stack tracking + Status effect

### Terraheart (Earth - Defensive)
- Base: [damage_reduction(0.10), velocity_threshold(0.1)]
- Equipment: Chestplate, Shield
- Pattern: Conditional reduction

### Gale Step (Air - Mobility)
- Base: [dash_bonus(1.5), gust_damage(3.0)]
- Equipment: Boots, Spear, Dagger
- Pattern: Movement enhancement

### Whispers (Air - Utility)
- Base: [speed_bonus(0.15), evasion_bonus(0.05)]
- Equipment: Helmet, Cloak
- Pattern: Passive buff

### Voltbrand (Lightning - Offensive)
- Base: [chain_damage(4.0), max_chains(3), chain_range(5.0)]
- Equipment: Sword, Spear, Crossbow
- Pattern: Charge tracking (every 5th) + Chain effect

### Arc Nexus (Lightning - Offensive)
- Base: [max_attack_speed_bonus(0.20), charge_buildup_rate(0.04)]
- Equipment: Gauntlets, Ring, Amulet
- Pattern: Progressive buff

### Hollow Edge (Shadow - Sustain)
- Base: [stamina_restore(2.0), mana_restore(5.0), crit_bonus_multiplier(1.5)]
- Equipment: Dagger, Scythe, Katana
- Pattern: Crit-triggered restore

### Veilborn (Shadow - Stealth)
- Base: [invisibility_duration(60), damage_free_time_required(200)]
- Equipment: Cloak, Armor, Ring
- Pattern: Time-based condition

### Radiant Grace (Light - Support)
- Base: [heal_amount(2.0), pulse_interval(200), radius(5.0)]
- Equipment: Staff, Armor, Amulet
- Pattern: Periodic area heal

### Dawnstrike (Light - Control)
- Base: [blind_duration(30), blind_strength(1), proc_chance(0.25)]
- Equipment: Sword, Bow
- Pattern: Proc-based debuff

---

Use this framework as your guide for implementing each enchantment. The affinity system is already integrated and will automatically apply in PVP scenarios!
