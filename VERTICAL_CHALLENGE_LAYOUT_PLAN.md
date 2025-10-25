# Vertical Challenge Tree Layout Plan
## Heart of the Mountain Style - Bottom to Top Progression

### Grid System
- **Width**: 5 columns (X: -2, -1, 0, 1, 2)
- **Height**: Unlimited vertical (Y: -2 to +10 or higher)
- **Starting View**: Centered at (0, 0), showing rows -2 to +2
- **Navigation**: Up/Down arrows only (vertical progression)

---

## 1. FARMING CHALLENGES (6 total)
**Theme**: Agricultural progression from basic crops to master farming

```
Row +3:  farming_master_farmer (0, 3) [HARD] - 10 tokens
            |
Row +2:  farming_carrot_potato (0, 2) [MEDIUM] - 8 tokens
            |
Row +1:  farming_wheat_harvest (0, 1) [EASY] - 5 tokens
            |
Row  0:  farming_animal_breeding (0, 0) [EASY] - 3 tokens
            |
Row -1:  farming_crop_variety (-1, -1) + farming_first_harvest (1, -1) [STARTER] - 2 tokens each
            |                    |
Row -2:  --------- farming_starter (0, -2) [STARTER] - 1 token ---------
```

**Challenges**:
1. `farming_starter` (0, -2) - Plant first crop - 1 WHEAT - 1 token
2. `farming_first_harvest` (1, -1) - Harvest 10 wheat - Prereq: farming_starter - 2 tokens
3. `farming_crop_variety` (-1, -1) - Plant carrots/potatoes - Prereq: farming_starter - 2 tokens
4. `farming_animal_breeding` (0, 0) - Breed 5 animals - Prereq: farming_first_harvest, farming_crop_variety - 3 tokens
5. `farming_wheat_harvest` (0, 1) - Harvest 64 wheat - Prereq: farming_animal_breeding - 5 tokens
6. `farming_master_farmer` (0, 3) - Harvest 32 carrots + 128 potatoes - Prereq: farming_wheat_harvest - 10 tokens

---

## 2. MINING CHALLENGES (6 total)
**Theme**: Ore tier progression

```
Row +3:  mining_netherite (0, 3) [VERY_HARD] - 15 tokens
            |
Row +2:  mining_diamond (0, 2) [HARD] - 10 tokens
            |
Row +1:  mining_gold (0, 1) [MEDIUM] - 6 tokens
            |
Row  0:  mining_iron (0, 0) [EASY] - 4 tokens
            |
Row -1:  mining_coal (0, -1) [EASY] - 2 tokens
            |
Row -2:  mining_starter (0, -2) [STARTER] - 1 token
```

**Challenges**:
1. `mining_starter` (0, -2) - Mine first cobblestone - 1 token
2. `mining_coal` (0, -1) - Mine 16 coal - Prereq: mining_starter - 2 tokens
3. `mining_iron` (0, 0) - Mine 32 iron - Prereq: mining_coal - 4 tokens
4. `mining_gold` (0, 1) - Mine 16 gold - Prereq: mining_iron - 6 tokens
5. `mining_diamond` (0, 2) - Mine 8 diamonds - Prereq: mining_gold - 10 tokens
6. `mining_netherite` (0, 3) - Mine 3 ancient debris - Prereq: mining_diamond - 15 tokens

---

## 3. COMBAT CHALLENGES (6 total)
**Theme**: Mob difficulty progression

```
Row +3:  combat_endermen (0, 3) [VERY_HARD] - 12 tokens
            |
Row +2:  combat_spiders (0, 2) [HARD] - 8 tokens
            |
Row +1:  combat_creepers (0, 1) [MEDIUM] - 6 tokens
            |
Row  0:  -------- combat_skeletons (0, 0) [EASY] - 4 tokens --------
            |                                      |
Row -1:  combat_zombies (-1, -1) [STARTER] - 2 tokens     combat_passive (1, -1) - 2 tokens
            |                                      |
Row -2:  -------------- combat_starter (0, -2) [STARTER] - 1 token --------------
```

**Challenges**:
1. `combat_starter` (0, -2) - Defeat first mob - 1 token
2. `combat_zombies` (-1, -1) - Defeat 10 zombies - Prereq: combat_starter - 2 tokens
3. `combat_passive` (1, -1) - Defeat 5 passive mobs - Prereq: combat_starter - 2 tokens
4. `combat_skeletons` (0, 0) - Defeat 15 skeletons - Prereq: combat_zombies, combat_passive - 4 tokens
5. `combat_creepers` (0, 1) - Defeat 10 creepers - Prereq: combat_skeletons - 6 tokens
6. `combat_spiders` (0, 2) - Defeat 20 spiders - Prereq: combat_creepers - 8 tokens
7. `combat_endermen` (0, 3) - Defeat 5 endermen - Prereq: combat_spiders - 12 tokens

---

## 4. BUILDING CHALLENGES (4 total)
**Theme**: Construction progression

```
Row +1:  building_master (0, 1) [HARD] - 10 tokens
            |
Row  0:  building_farm (0, 0) [MEDIUM] - 6 tokens
            |
Row -1:  building_house (0, -1) [EASY] - 3 tokens
            |
Row -2:  building_starter (0, -2) [STARTER] - 1 token
```

**Challenges**:
1. `building_starter` (0, -2) - Place 100 blocks - 1 token
2. `building_house` (0, -1) - Place 500 blocks - Prereq: building_starter - 3 tokens
3. `building_farm` (0, 0) - Place 1000 blocks - Prereq: building_house - 6 tokens
4. `building_master` (0, 1) - Place 5000 blocks - Prereq: building_farm - 10 tokens

---

## 5. CRAFTING CHALLENGES (4 total)
**Theme**: Crafting progression

```
Row +1:  crafting_master (0, 1) [HARD] - 8 tokens
            |
Row  0:  crafting_tools (-1, 0) + crafting_armor (1, 0) [MEDIUM] - 4 tokens each
            |                      |
Row -1:  ------------ crafting_starter (0, -1) [STARTER] - 2 tokens ------------
```

**Challenges**:
1. `crafting_starter` (0, -1) - Craft 5 items - 2 tokens
2. `crafting_tools` (-1, 0) - Craft 20 tools - Prereq: crafting_starter - 4 tokens
3. `crafting_armor` (1, 0) - Craft full armor set - Prereq: crafting_starter - 4 tokens
4. `crafting_master` (0, 1) - Craft 100 items - Prereq: crafting_tools, crafting_armor - 8 tokens

---

## 6. EXPLORATION CHALLENGES (2 total)
**Theme**: Simple exploration

```
Row  0:  exploration_distance (0, 0) [MEDIUM] - 5 tokens
            |
Row -1:  exploration_starter (0, -1) [STARTER] - 2 tokens
```

**Challenges**:
1. `exploration_starter` (0, -1) - Travel 1000 blocks - 2 tokens
2. `exploration_distance` (0, 0) - Travel 10000 blocks - Prereq: exploration_starter - 5 tokens

---

## 7. ECONOMY CHALLENGES (2 total)
**Theme**: Wealth accumulation

```
Row  0:  economy_wealthy (0, 0) [HARD] - 8 tokens
            |
Row -1:  economy_starter (0, -1) [EASY] - 3 tokens
```

**Challenges**:
1. `economy_starter` (0, -1) - Earn 1000 coins - 3 tokens
2. `economy_wealthy` (0, 0) - Earn 10000 coins - Prereq: economy_starter - 8 tokens

---

## 8. SOCIAL CHALLENGES (2 total)
**Theme**: Community interaction

```
Row  0:  social_cooperative (0, 0) [MEDIUM] - 6 tokens
            |
Row -1:  social_starter (0, -1) [STARTER] - 2 tokens
```

**Challenges**:
1. `social_starter` (0, -1) - Add island member - 2 tokens
2. `social_cooperative` (0, 0) - Complete 10 shared tasks - Prereq: social_starter - 6 tokens

---

## 9. PROGRESSION CHALLENGES (3 total)
**Theme**: Overall advancement

```
Row +1:  progression_veteran (0, 1) [VERY_HARD] - 12 tokens
            |
Row  0:  progression_intermediate (0, 0) [MEDIUM] - 5 tokens
            |
Row -1:  progression_novice (0, -1) [EASY] - 2 tokens
```

**Challenges**:
1. `progression_novice` (0, -1) - Complete 5 challenges - 2 tokens
2. `progression_intermediate` (0, 0) - Complete 15 challenges - Prereq: progression_novice - 5 tokens
3. `progression_veteran` (0, 1) - Complete 30 challenges - Prereq: progression_intermediate - 12 tokens

---

## 10. SPECIAL CHALLENGES (2 total)
**Theme**: Unique tasks

```
Row  0:  special_rare (-1, 0) + special_event (1, 0) [SPECIAL] - 10 tokens each
```

**Challenges**:
1. `special_rare` (-1, 0) - Find rare item - 10 tokens (no prereq)
2. `special_event` (1, 0) - Participate in event - 10 tokens (no prereq)

---

## Summary
- **Total Challenges**: 38
- **Y Range**: -2 (starting position) to +3 (highest challenges)
- **X Range**: -1 to +1 (mostly center column, some branch at -1/+1)
- **View Window**: Shows 5 rows at a time (Y-2 to Y+2 relative to center position)
- **Starting View**: Center at (0, 0) shows rows -2 to +2 (all starter challenges visible)
- **Progression**: Clear upward paths with prerequisites creating natural difficulty curves
