# Player Island System Implementation Plan

## Overview
A comprehensive island system using Advanced SlimeWorldManager where players can purchase, customize, and upgrade personal islands using the in-game "units" currency.

---

## System Architecture

### Core Components

#### 1. **Island Data Model** (`PlayerIsland.java`)
- Island UUID
- Owner UUID
- Island name (customizable)
- Island type (SKY, OCEAN, FOREST)
- Island level
- Island value
- World border size (25x25 to 500x500)
- Creation timestamp
- Last accessed timestamp
- Upgrade levels:
  - Player limit
  - Redstone limit
  - Crop growth multiplier
  - Weather control
  - Biome type
  - Size

#### 2. **Island Manager** (`IslandManager.java`)
- Create new islands
- Load/unload islands
- Track active islands
- Auto-unload inactive islands (configurable timeout)
- Handle teleportation
- Manage upgrades
- Island value calculation

#### 3. **Island World Manager** (`IslandWorldManager.java`)
- Integration with Advanced SlimeWorldManager
- World generation per island type
- World border management
- Spawn point management
- World unloading/loading optimization

#### 4. **Island Data Storage** (`IslandDataManager.java`)
- SQLite/MySQL database storage
- Island ownership tracking
- Upgrade levels storage
- Statistics tracking

#### 5. **Island GUI** (`IslandGUI.java`)
- Island management interface
- Upgrade purchase menu
- Settings configuration
- Visitor management
- Statistics display

#### 6. **Island Commands** (`IslandCommand.java`)
- `/island` - Teleport to own island
- `/island create <type>` - Create island
- `/island upgrade` - Open upgrade GUI
- `/island visit <player>` - Visit another island
- `/island sethome` - Set island spawn
- `/island settings` - Open settings GUI
- `/island delete` - Delete island (confirmation)
- `/island info` - Show island information
- `/island invite <player>` - Invite player
- `/island kick <player>` - Kick player
- `/island setname <name>` - Set island name

---

## Island Types

### 1. **Sky Island**
- **Cost**: 10,000 units
- **Theme**: Floating island in the sky
- **Starting biome**: Plains
- **Special feature**: Void below, no fall damage on island

### 2. **Ocean Island**
- **Cost**: 15,000 units
- **Theme**: Island surrounded by ocean
- **Starting biome**: Beach
- **Special feature**: Rich fishing area, dolphins spawn

### 3. **Forest Island**
- **Cost**: 12,000 units
- **Theme**: Dense forest environment
- **Starting biome**: Forest
- **Special feature**: Enhanced tree growth, wildlife

---

## Upgrade System

### Size Upgrades
| Level | Size | Cost (Units) |
|-------|------|--------------|
| 1     | 25x25 | Included |
| 2     | 50x50 | 5,000 |
| 3     | 100x100 | 15,000 |
| 4     | 200x200 | 50,000 |
| 5     | 300x300 | 150,000 |
| 6     | 400x400 | 300,000 |
| 7     | 500x500 | 500,000 |

### Player Limit Upgrades
| Level | Max Players | Cost (Units) |
|-------|-------------|--------------|
| 1     | 2 | Included |
| 2     | 5 | 2,000 |
| 3     | 10 | 10,000 |
| 4     | 20 | 30,000 |
| 5     | 50 | 100,000 |

### Redstone Limit Upgrades
| Level | Max Redstone | Cost (Units) |
|-------|--------------|--------------|
| 1     | 50 | Included |
| 2     | 100 | 5,000 |
| 3     | 200 | 15,000 |
| 4     | 500 | 50,000 |
| 5     | Unlimited | 150,000 |

### Crop Growth Upgrades
| Level | Growth Speed | Cost (Units) |
|-------|--------------|--------------|
| 1     | 1.0x | Included |
| 2     | 1.5x | 10,000 |
| 3     | 2.0x | 30,000 |
| 4     | 3.0x | 100,000 |

### Weather Control
- **Cost**: 20,000 units
- **Feature**: Toggle weather on/off
- **Feature**: Set specific weather types

### Biome Selection
- **Cost per biome**: 15,000 units
- **Available biomes**: All Minecraft biomes
- **Feature**: Change island biome

---

## Database Schema

### `player_islands` Table
```sql
CREATE TABLE IF NOT EXISTS player_islands (
    island_id VARCHAR(36) PRIMARY KEY,
    owner_uuid VARCHAR(36) NOT NULL,
    island_name VARCHAR(50) NOT NULL,
    island_type VARCHAR(20) NOT NULL,
    world_name VARCHAR(50) NOT NULL,
    created_at BIGINT NOT NULL,
    last_accessed BIGINT NOT NULL,
    island_level INT DEFAULT 1,
    island_value BIGINT DEFAULT 0,
    size_level INT DEFAULT 1,
    player_limit_level INT DEFAULT 1,
    redstone_limit_level INT DEFAULT 1,
    crop_growth_level INT DEFAULT 1,
    weather_control BOOLEAN DEFAULT FALSE,
    current_biome VARCHAR(30),
    spawn_x DOUBLE DEFAULT 0,
    spawn_y DOUBLE DEFAULT 100,
    spawn_z DOUBLE DEFAULT 0,
    spawn_yaw FLOAT DEFAULT 0,
    spawn_pitch FLOAT DEFAULT 0,
    UNIQUE(owner_uuid)
);
```

### `island_members` Table
```sql
CREATE TABLE IF NOT EXISTS island_members (
    island_id VARCHAR(36) NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    permission_level INT DEFAULT 0,
    added_at BIGINT NOT NULL,
    PRIMARY KEY (island_id, player_uuid),
    FOREIGN KEY (island_id) REFERENCES player_islands(island_id) ON DELETE CASCADE
);
```

### `island_statistics` Table
```sql
CREATE TABLE IF NOT EXISTS island_statistics (
    island_id VARCHAR(36) PRIMARY KEY,
    total_visits INT DEFAULT 0,
    unique_visitors INT DEFAULT 0,
    blocks_placed BIGINT DEFAULT 0,
    blocks_broken BIGINT DEFAULT 0,
    mobs_killed INT DEFAULT 0,
    FOREIGN KEY (island_id) REFERENCES player_islands(island_id) ON DELETE CASCADE
);
```

---

## Performance Optimizations

### 1. **World Unloading**
- Auto-unload islands after 5 minutes of inactivity
- Configurable timeout in config.yml
- Save world state before unloading
- Queue-based loading system

### 2. **Caching**
- Cache active island data in memory
- Cache player island ownership
- Cache upgrade levels for quick access
- Clear cache on logout

### 3. **Async Operations**
- Async world loading
- Async database queries
- Async world saving
- Async teleportation

### 4. **SlimeWorld Benefits**
- Worlds stored as compressed files
- Faster loading times
- Less disk I/O
- Efficient memory usage

---

## Edge Case Handling

### 1. **Player Offline During Load**
- Queue teleport for when player logs in
- Send message when world is ready
- Timeout after 30 seconds

### 2. **Corrupted Worlds**
- Backup system (keep last 3 backups)
- Automatic backup before modifications
- Restore from backup if corruption detected
- Alert admin if restoration fails

### 3. **Database Connection Loss**
- Cache write operations
- Retry mechanism
- Emergency save to flatfile

### 4. **Server Crash**
- Auto-save every 5 minutes
- Save on player logout
- Save before unload

### 5. **Multiple Islands**
- Prevent duplicate island creation
- Check ownership before operations
- Handle legacy data migration

---

## Configuration (config.yml)

```yaml
islands:
  enabled: true
  auto-unload-timeout: 300  # seconds (5 minutes)
  max-islands-loaded: 100
  world-save-interval: 300  # seconds
  
  # Island costs
  costs:
    sky-island: 10000
    ocean-island: 15000
    forest-island: 12000
  
  # Upgrade costs
  upgrades:
    size:
      level-2: 5000
      level-3: 15000
      level-4: 50000
      level-5: 150000
      level-6: 300000
      level-7: 500000
    player-limit:
      level-2: 2000
      level-3: 10000
      level-4: 30000
      level-5: 100000
    redstone-limit:
      level-2: 5000
      level-3: 15000
      level-4: 50000
      level-5: 150000
    crop-growth:
      level-2: 10000
      level-3: 30000
      level-4: 100000
    weather-control: 20000
    biome-change: 15000
  
  # Performance settings
  performance:
    async-loading: true
    async-saving: true
    backup-count: 3
```

---

## Implementation Phases

### Phase 1: Core System (Priority)
- [ ] Add SlimeWorldManager dependency
- [ ] Create PlayerIsland data model
- [ ] Create IslandManager
- [ ] Create IslandWorldManager
- [ ] Database setup and migrations
- [ ] Basic /island command structure

### Phase 2: World Management
- [ ] SlimeWorld integration
- [ ] World generation for each type
- [ ] World border management
- [ ] Auto-unload system
- [ ] Backup system

### Phase 3: Commands & GUI
- [ ] Island creation command
- [ ] Teleportation system
- [ ] Island upgrade GUI
- [ ] Settings GUI
- [ ] Visitor management

### Phase 4: Upgrades & Features
- [ ] Size upgrades
- [ ] Player limit enforcement
- [ ] Redstone limit system
- [ ] Crop growth multiplier
- [ ] Weather control
- [ ] Biome changing

### Phase 5: Polish & Optimization
- [ ] Statistics tracking
- [ ] Island value calculation
- [ ] Performance optimization
- [ ] Edge case handling
- [ ] Testing at scale

---

## File Structure

```
com/server/islands/
├── IslandPlugin.java              # Main plugin integration
├── data/
│   ├── PlayerIsland.java          # Island data model
│   ├── IslandType.java            # Enum for island types
│   ├── IslandUpgrade.java         # Upgrade data model
│   └── IslandStatistics.java      # Statistics data model
├── manager/
│   ├── IslandManager.java         # Core island management
│   ├── IslandWorldManager.java    # World loading/unloading
│   ├── IslandDataManager.java     # Database operations
│   ├── IslandUpgradeManager.java  # Upgrade logic
│   └── IslandCacheManager.java    # Caching system
├── command/
│   └── IslandCommand.java         # All island commands
├── gui/
│   ├── IslandMainGUI.java         # Main island menu
│   ├── IslandUpgradeGUI.java      # Upgrade menu
│   ├── IslandSettingsGUI.java     # Settings menu
│   └── IslandVisitorGUI.java      # Visitor management
├── listener/
│   ├── IslandPlayerListener.java  # Player join/quit events
│   ├── IslandBlockListener.java   # Block place/break tracking
│   └── IslandWorldListener.java   # World-specific events
└── util/
    ├── IslandUtil.java             # Utility methods
    └── WorldBorderUtil.java        # World border helper
```

---

## Next Steps

1. Implement Phase 1 (Core System)
2. Test with SlimeWorldManager integration
3. Create database schema
4. Implement basic commands
5. Add GUI system
6. Implement upgrades
7. Performance testing with multiple islands
8. Edge case testing

This is a comprehensive system that will scale well and provide players with a highly customizable island experience!
