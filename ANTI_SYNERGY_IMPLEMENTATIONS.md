# Anti-Synergy Implementation Code

## Add to each enchantment class:

### Cinderwake (Groups: 1, 2)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{1, 2}; // Fire Damage, AOE/Chain
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"Stormfire", "Embershade", "Voltbrand", "Deepcurrent", "CelestialSurge"};
}
```

### AshenVeil (Groups: 4, 9)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{4, 9}; // Invisibility, On-Kill
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"Veilborn", "Hollow Edge"};
}
```

### Deepcurrent (Group: 2)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{2}; // AOE/Chain
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"Voltbrand", "Cinderwake", "Stormfire", "CelestialSurge"};
}
```

### Mistveil (Group: 5)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{5}; // Defensive Response
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"Whispers", "Radiant Grace"};
}
```

### BurdenedStone (Group: 3)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{3}; // Crowd Control
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"Decayroot", "Dawnstrike"};
}
```

### Terraheart (Group: 8)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{8}; // Sustain/Barriers
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"PureReflection"};
}
```

### GaleStep (Group: 7)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{7}; // Movement
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"MistborneTempest"};
}
```

### Whispers (Group: 5)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{5}; // Defensive Response
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"Mistveil", "Radiant Grace"};
}
```

### Voltbrand (Group: 2)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{2}; // AOE/Chain
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"Deepcurrent", "Cinderwake", "Stormfire", "CelestialSurge"};
}
```

### ArcNexus (Group: 6)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{6}; // Attack Speed
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{}; // Currently alone in group
}
```

### HollowEdge (Group: 9)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{9}; // On-Kill
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"AshenVeil"};
}
```

### Veilborn (Group: 4)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{4}; // Invisibility
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"AshenVeil"};
}
```

### Dawnstrike (Group: 3)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{3}; // Crowd Control
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"BurdenedStone", "Decayroot"};
}
```

### RadiantGrace (Group: 5)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{5}; // Defensive Response
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"Mistveil", "Whispers"};
}
```

### Stormfire (Groups: 1, 2)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{1, 2}; // Fire Damage, AOE/Chain
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"Cinderwake", "Embershade", "Voltbrand", "Deepcurrent", "CelestialSurge"};
}
```

### MistborneTempest (Group: 7)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{7}; // Movement
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"GaleStep"};
}
```

### Decayroot (Group: 3)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{3}; // Crowd Control
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"BurdenedStone", "Dawnstrike"};
}
```

### CelestialSurge (Group: 2)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{2}; // AOE/Chain
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"Voltbrand", "Deepcurrent", "Cinderwake", "Stormfire"};
}
```

### Embershade (Group: 1)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{1}; // Fire Damage
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"Cinderwake", "Stormfire"};
}
```

### PureReflection (Group: 8)
```java
@Override
public int[] getAntiSynergyGroups() {
    return new int[]{8}; // Sustain/Barriers
}

@Override
public String[] getConflictingEnchantments() {
    return new String[]{"Terraheart"};
}
```
