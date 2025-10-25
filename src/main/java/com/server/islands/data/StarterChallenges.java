package com.server.islands.data;

import com.server.islands.data.IslandChallenge.ChallengeCategory;
import com.server.islands.data.IslandChallenge.ChallengeDifficulty;
import com.server.islands.data.IslandChallenge.ChallengeType;
import com.server.islands.managers.ChallengeManager;

/**
 * Registers starter challenges for the island system.
 * All challenges are arranged in vertical progression trees (Heart of the Mountain style).
 * Starting nodes are at Y=-2 (bottom), progressing upward to higher Y values.
 */
public class StarterChallenges {
    
    /**
     * Registers all starter challenges with the challenge manager.
     */
    public static void registerAll(ChallengeManager manager) {
        registerFarmingChallenges(manager);
        registerMiningChallenges(manager);
        registerCombatChallenges(manager);
        registerBuildingChallenges(manager);
        registerCraftingChallenges(manager);
        registerExplorationChallenges(manager);
        registerEconomyChallenges(manager);
        registerSocialChallenges(manager);
        registerProgressionChallenges(manager);
        registerSpecialChallenges(manager);
    }
    
    private static void registerFarmingChallenges(ChallengeManager manager) {
        /*
         * FARMING TREE LAYOUT (Vertical Progression):
         * 
         * Row +3:  farming_master_farmer (0, 3)
         *              |
         * Row +2:  farming_carrot_potato (0, 2)
         *              |
         * Row +1:  farming_wheat_harvest (0, 1)
         *              |
         * Row  0:  farming_animal_breeding (0, 0)
         *              |
         * Row -1:  farming_crop_variety (-1, -1) + farming_first_harvest (1, -1)
         *              |                               |
         * Row -2:  ----------- farming_starter (0, -2) -----------
         */
        
        // STARTER (Y=-2) - Plant first crop
        manager.registerChallenge(new IslandChallenge.Builder("farming_starter", "First Seed")
            .description("Place your first crop on farmland to begin farming")
            .category(ChallengeCategory.FARMING)
            .difficulty(ChallengeDifficulty.STARTER)
            .tokenReward(1)
            .type(ChallengeType.HARVEST)
            .targetKey("WHEAT")
            .targetAmount(1)
            .isIslandWide(true)
            .gridPosition(0, -2)
            .build());
        
        // TIER 1 (Y=-1) - Branch into two paths
        manager.registerChallenge(new IslandChallenge.Builder("farming_first_harvest", "First Harvest")
            .description("Harvest 10 wheat from your farm")
            .category(ChallengeCategory.FARMING)
            .difficulty(ChallengeDifficulty.STARTER)
            .tokenReward(2)
            .type(ChallengeType.HARVEST)
            .targetKey("WHEAT")
            .targetAmount(10)
            .prerequisite("farming_starter")
            .isIslandWide(true)
            .gridPosition(1, -1)
            .build());
        
        manager.registerChallenge(new IslandChallenge.Builder("farming_crop_variety", "Crop Diversity")
            .description("Plant both carrots and potatoes")
            .category(ChallengeCategory.FARMING)
            .difficulty(ChallengeDifficulty.STARTER)
            .tokenReward(2)
            .type(ChallengeType.HARVEST)
            .targetKey("CARROT")
            .targetAmount(1)
            .prerequisite("farming_starter")
            .isIslandWide(true)
            .gridPosition(-1, -1)
            .build());
        
        // TIER 2 (Y=0) - Animal breeding (requires both branches)
        manager.registerChallenge(new IslandChallenge.Builder("farming_animal_breeding", "Animal Breeder")
            .description("Breed 5 animals on your island")
            .category(ChallengeCategory.FARMING)
            .difficulty(ChallengeDifficulty.EASY)
            .tokenReward(3)
            .type(ChallengeType.BREED_ANIMALS)
            .targetKey("ANY")
            .targetAmount(5)
            .prerequisite("farming_first_harvest")
            .isIslandWide(true)
            .gridPosition(0, 0)
            .build());
        
        // TIER 3 (Y=1) - Wheat mastery
        manager.registerChallenge(new IslandChallenge.Builder("farming_wheat_harvest", "Wheat Expert")
            .description("Harvest 64 wheat to prove your farming skills")
            .category(ChallengeCategory.FARMING)
            .difficulty(ChallengeDifficulty.EASY)
            .tokenReward(5)
            .type(ChallengeType.HARVEST)
            .targetKey("WHEAT")
            .targetAmount(64)
            .prerequisite("farming_animal_breeding")
            .isIslandWide(true)
            .gridPosition(0, 1)
            .build());
        
        // TIER 4 (Y=2) - Carrot and potato mastery
        manager.registerChallenge(new IslandChallenge.Builder("farming_carrot_potato", "Root Vegetable Master")
            .description("Harvest 32 carrots AND 128 potatoes")
            .category(ChallengeCategory.FARMING)
            .difficulty(ChallengeDifficulty.MEDIUM)
            .tokenReward(8)
            .type(ChallengeType.HARVEST)
            .targetKey("CARROT")
            .targetAmount(32)
            .prerequisite("farming_wheat_harvest")
            .isIslandWide(true)
            .gridPosition(0, 2)
            .build());
        
        // TIER 5 (Y=3) - Master farmer
        manager.registerChallenge(new IslandChallenge.Builder("farming_master_farmer", "Master Farmer")
            .description("The pinnacle of agricultural achievement - complete all farming tasks")
            .category(ChallengeCategory.FARMING)
            .difficulty(ChallengeDifficulty.HARD)
            .tokenReward(10)
            .type(ChallengeType.HARVEST)
            .targetKey("WHEAT")
            .targetAmount(128)
            .prerequisite("farming_carrot_potato")
            .isIslandWide(true)
            .gridPosition(0, 3)
            .build());
    }
    
    private static void registerMiningChallenges(ChallengeManager manager) {
        /*
         * MINING TREE LAYOUT (Vertical Ore Progression):
         * 
         * Row +3:  mining_netherite (0, 3)
         *              |
         * Row +2:  mining_diamond (0, 2)
         *              |
         * Row +1:  mining_gold (0, 1)
         *              |
         * Row  0:  mining_iron (0, 0)
         *              |
         * Row -1:  mining_coal (0, -1)
         *              |
         * Row -2:  mining_starter (0, -2)
         */
        
        // STARTER (Y=-2) - First cobblestone
        manager.registerChallenge(new IslandChallenge.Builder("mining_starter", "First Strike")
            .description("Mine your first cobblestone block")
            .category(ChallengeCategory.MINING)
            .difficulty(ChallengeDifficulty.STARTER)
            .tokenReward(1)
            .type(ChallengeType.MINING)
            .targetKey("COBBLESTONE")
            .targetAmount(1)
            .isIslandWide(false)
            .gridPosition(0, -2)
            .build());
        
        // TIER 1 (Y=-1) - Coal
        manager.registerChallenge(new IslandChallenge.Builder("mining_coal", "Coal Miner")
            .description("Mine 16 coal ore")
            .category(ChallengeCategory.MINING)
            .difficulty(ChallengeDifficulty.EASY)
            .tokenReward(2)
            .type(ChallengeType.MINING)
            .targetKey("COAL_ORE")
            .targetAmount(16)
            .prerequisite("mining_starter")
            .isIslandWide(false)
            .gridPosition(0, -1)
            .build());
        
        // TIER 2 (Y=0) - Iron
        manager.registerChallenge(new IslandChallenge.Builder("mining_iron", "Iron Miner")
            .description("Mine 32 iron ore")
            .category(ChallengeCategory.MINING)
            .difficulty(ChallengeDifficulty.EASY)
            .tokenReward(4)
            .type(ChallengeType.MINING)
            .targetKey("IRON_ORE")
            .targetAmount(32)
            .prerequisite("mining_coal")
            .isIslandWide(false)
            .gridPosition(0, 0)
            .build());
        
        // TIER 3 (Y=1) - Gold
        manager.registerChallenge(new IslandChallenge.Builder("mining_gold", "Gold Miner")
            .description("Mine 16 gold ore")
            .category(ChallengeCategory.MINING)
            .difficulty(ChallengeDifficulty.MEDIUM)
            .tokenReward(6)
            .type(ChallengeType.MINING)
            .targetKey("GOLD_ORE")
            .targetAmount(16)
            .prerequisite("mining_iron")
            .isIslandWide(false)
            .gridPosition(0, 1)
            .build());
        
        // TIER 4 (Y=2) - Diamond
        manager.registerChallenge(new IslandChallenge.Builder("mining_diamond", "Diamond Miner")
            .description("Mine 8 diamond ore - the most precious resource")
            .category(ChallengeCategory.MINING)
            .difficulty(ChallengeDifficulty.HARD)
            .tokenReward(10)
            .type(ChallengeType.MINING)
            .targetKey("DIAMOND_ORE")
            .targetAmount(8)
            .prerequisite("mining_gold")
            .isIslandWide(false)
            .gridPosition(0, 2)
            .build());
        
        // TIER 5 (Y=3) - Netherite
        manager.registerChallenge(new IslandChallenge.Builder("mining_netherite", "Ancient Debris Hunter")
            .description("Mine 3 ancient debris - the ultimate mining achievement")
            .category(ChallengeCategory.MINING)
            .difficulty(ChallengeDifficulty.EXPERT)
            .tokenReward(15)
            .type(ChallengeType.MINING)
            .targetKey("ANCIENT_DEBRIS")
            .targetAmount(3)
            .prerequisite("mining_diamond")
            .isIslandWide(false)
            .gridPosition(0, 3)
            .build());
    }
    
    private static void registerCombatChallenges(ChallengeManager manager) {
        /*
         * COMBAT TREE LAYOUT (Vertical Mob Difficulty):
         * 
         * Row +3:  combat_endermen (0, 3)
         *              |
         * Row +2:  combat_spiders (0, 2)
         *              |
         * Row +1:  combat_creepers (0, 1)
         *              |
         * Row  0:  combat_skeletons (0, 0)
         *              |
         * Row -1:  combat_zombies (-1, -1) + combat_passive (1, -1)
         *              |                          |
         * Row -2:  ---------- combat_starter (0, -2) ----------
         */
        
        // STARTER (Y=-2) - First mob
        manager.registerChallenge(new IslandChallenge.Builder("combat_starter", "First Blood")
            .description("Defeat your first hostile mob")
            .category(ChallengeCategory.COMBAT)
            .difficulty(ChallengeDifficulty.STARTER)
            .tokenReward(1)
            .type(ChallengeType.KILL_MOBS)
            .targetKey("ANY")
            .targetAmount(1)
            .isIslandWide(false)
            .gridPosition(0, -2)
            .build());
        
        // TIER 1 (Y=-1) - Branch into zombie and passive paths
        manager.registerChallenge(new IslandChallenge.Builder("combat_zombies", "Zombie Slayer")
            .description("Defeat 10 zombies")
            .category(ChallengeCategory.COMBAT)
            .difficulty(ChallengeDifficulty.STARTER)
            .tokenReward(2)
            .type(ChallengeType.KILL_MOBS)
            .targetKey("ZOMBIE")
            .targetAmount(10)
            .prerequisite("combat_starter")
            .isIslandWide(false)
            .gridPosition(-1, -1)
            .build());
        
        manager.registerChallenge(new IslandChallenge.Builder("combat_passive", "Hunter")
            .description("Defeat 5 passive mobs")
            .category(ChallengeCategory.COMBAT)
            .difficulty(ChallengeDifficulty.STARTER)
            .tokenReward(2)
            .type(ChallengeType.KILL_MOBS)
            .targetKey("PIG")
            .targetAmount(5)
            .prerequisite("combat_starter")
            .isIslandWide(false)
            .gridPosition(1, -1)
            .build());
        
        // TIER 2 (Y=0) - Skeletons
        manager.registerChallenge(new IslandChallenge.Builder("combat_skeletons", "Skeleton Slayer")
            .description("Defeat 15 skeletons")
            .category(ChallengeCategory.COMBAT)
            .difficulty(ChallengeDifficulty.EASY)
            .tokenReward(4)
            .type(ChallengeType.KILL_MOBS)
            .targetKey("SKELETON")
            .targetAmount(15)
            .prerequisite("combat_zombies")
            .isIslandWide(false)
            .gridPosition(0, 0)
            .build());
        
        // TIER 3 (Y=1) - Creepers
        manager.registerChallenge(new IslandChallenge.Builder("combat_creepers", "Creeper Hunter")
            .description("Defeat 10 creepers without getting blown up")
            .category(ChallengeCategory.COMBAT)
            .difficulty(ChallengeDifficulty.MEDIUM)
            .tokenReward(6)
            .type(ChallengeType.KILL_MOBS)
            .targetKey("CREEPER")
            .targetAmount(10)
            .prerequisite("combat_skeletons")
            .isIslandWide(false)
            .gridPosition(0, 1)
            .build());
        
        // TIER 4 (Y=2) - Spiders
        manager.registerChallenge(new IslandChallenge.Builder("combat_spiders", "Spider Slayer")
            .description("Defeat 20 spiders")
            .category(ChallengeCategory.COMBAT)
            .difficulty(ChallengeDifficulty.HARD)
            .tokenReward(8)
            .type(ChallengeType.KILL_MOBS)
            .targetKey("SPIDER")
            .targetAmount(20)
            .prerequisite("combat_creepers")
            .isIslandWide(false)
            .gridPosition(0, 2)
            .build());
        
        // TIER 5 (Y=3) - Endermen
        manager.registerChallenge(new IslandChallenge.Builder("combat_endermen", "Enderman Slayer")
            .description("Defeat 5 endermen - the ultimate combat challenge")
            .category(ChallengeCategory.COMBAT)
            .difficulty(ChallengeDifficulty.EXPERT)
            .tokenReward(12)
            .type(ChallengeType.KILL_MOBS)
            .targetKey("ENDERMAN")
            .targetAmount(5)
            .prerequisite("combat_spiders")
            .isIslandWide(false)
            .gridPosition(0, 3)
            .build());
    }
    
    private static void registerBuildingChallenges(ChallengeManager manager) {
        /*
         * BUILDING TREE LAYOUT (Vertical Construction):
         * 
         * Row +1:  building_master (0, 1)
         *              |
         * Row  0:  building_farm (0, 0)
         *              |
         * Row -1:  building_house (0, -1)
         *              |
         * Row -2:  building_starter (0, -2)
         */
        
        // STARTER (Y=-2) - Place first blocks
        manager.registerChallenge(new IslandChallenge.Builder("building_starter", "First Builder")
            .description("Place 100 blocks on your island")
            .category(ChallengeCategory.BUILDING)
            .difficulty(ChallengeDifficulty.STARTER)
            .tokenReward(1)
            .type(ChallengeType.BUILD)
            .targetKey("ANY")
            .targetAmount(100)
            .isIslandWide(true)
            .gridPosition(0, -2)
            .build());
        
        // TIER 1 (Y=-1) - Build a house
        manager.registerChallenge(new IslandChallenge.Builder("building_house", "House Builder")
            .description("Place 500 blocks - enough for a nice house")
            .category(ChallengeCategory.BUILDING)
            .difficulty(ChallengeDifficulty.EASY)
            .tokenReward(3)
            .type(ChallengeType.BUILD)
            .targetKey("ANY")
            .targetAmount(500)
            .prerequisite("building_starter")
            .isIslandWide(true)
            .gridPosition(0, -1)
            .build());
        
        // TIER 2 (Y=0) - Build a farm
        manager.registerChallenge(new IslandChallenge.Builder("building_farm", "Farm Builder")
            .description("Place 1000 blocks - expand your island")
            .category(ChallengeCategory.BUILDING)
            .difficulty(ChallengeDifficulty.MEDIUM)
            .tokenReward(6)
            .type(ChallengeType.BUILD)
            .targetKey("ANY")
            .targetAmount(1000)
            .prerequisite("building_house")
            .isIslandWide(true)
            .gridPosition(0, 0)
            .build());
        
        // TIER 3 (Y=1) - Master builder
        manager.registerChallenge(new IslandChallenge.Builder("building_master", "Master Builder")
            .description("Place 5000 blocks - a true architect")
            .category(ChallengeCategory.BUILDING)
            .difficulty(ChallengeDifficulty.HARD)
            .tokenReward(10)
            .type(ChallengeType.BUILD)
            .targetKey("ANY")
            .targetAmount(5000)
            .prerequisite("building_farm")
            .isIslandWide(true)
            .gridPosition(0, 1)
            .build());
    }
    
    private static void registerCraftingChallenges(ChallengeManager manager) {
        /*
         * CRAFTING TREE LAYOUT (Vertical Crafting):
         * 
         * Row +1:  crafting_master (0, 1)
         *              |
         * Row  0:  crafting_tools (-1, 0) + crafting_armor (1, 0)
         *              |                         |
         * Row -1:  ---------- crafting_starter (0, -1) ----------
         */
        
        // STARTER (Y=-1) - First crafts
        manager.registerChallenge(new IslandChallenge.Builder("crafting_starter", "First Crafter")
            .description("Craft 5 items")
            .category(ChallengeCategory.CRAFTING)
            .difficulty(ChallengeDifficulty.STARTER)
            .tokenReward(2)
            .type(ChallengeType.CRAFT_ITEMS)
            .targetKey("ANY")
            .targetAmount(5)
            .isIslandWide(false)
            .gridPosition(0, -1)
            .build());
        
        // TIER 1 (Y=0) - Branch into tools and armor
        manager.registerChallenge(new IslandChallenge.Builder("crafting_tools", "Tool Maker")
            .description("Craft 20 tools")
            .category(ChallengeCategory.CRAFTING)
            .difficulty(ChallengeDifficulty.MEDIUM)
            .tokenReward(4)
            .type(ChallengeType.CRAFT_ITEMS)
            .targetKey("WOODEN_PICKAXE")
            .targetAmount(20)
            .prerequisite("crafting_starter")
            .isIslandWide(false)
            .gridPosition(-1, 0)
            .build());
        
        manager.registerChallenge(new IslandChallenge.Builder("crafting_armor", "Armor Smith")
            .description("Craft a full set of armor")
            .category(ChallengeCategory.CRAFTING)
            .difficulty(ChallengeDifficulty.MEDIUM)
            .tokenReward(4)
            .type(ChallengeType.CRAFT_ITEMS)
            .targetKey("IRON_CHESTPLATE")
            .targetAmount(1)
            .prerequisite("crafting_starter")
            .isIslandWide(false)
            .gridPosition(1, 0)
            .build());
        
        // TIER 2 (Y=1) - Master crafter
        manager.registerChallenge(new IslandChallenge.Builder("crafting_master", "Master Crafter")
            .description("Craft 100 items total")
            .category(ChallengeCategory.CRAFTING)
            .difficulty(ChallengeDifficulty.HARD)
            .tokenReward(8)
            .type(ChallengeType.CRAFT_ITEMS)
            .targetKey("ANY")
            .targetAmount(100)
            .prerequisite("crafting_tools")
            .isIslandWide(false)
            .gridPosition(0, 1)
            .build());
    }
    
    private static void registerExplorationChallenges(ChallengeManager manager) {
        /*
         * EXPLORATION TREE LAYOUT (Vertical Distance):
         * 
         * Row  0:  exploration_distance (0, 0)
         *              |
         * Row -1:  exploration_starter (0, -1)
         */
        
        // STARTER (Y=-1) - First steps
        manager.registerChallenge(new IslandChallenge.Builder("exploration_starter", "First Steps")
            .description("Travel 1000 blocks")
            .category(ChallengeCategory.EXPLORATION)
            .difficulty(ChallengeDifficulty.STARTER)
            .tokenReward(2)
            .type(ChallengeType.EXPLORATION)
            .targetKey("ANY")
            .targetAmount(1000)
            .isIslandWide(false)
            .gridPosition(0, -1)
            .build());
        
        // TIER 1 (Y=0) - Long distance
        manager.registerChallenge(new IslandChallenge.Builder("exploration_distance", "World Traveler")
            .description("Travel 10000 blocks - explore the world")
            .category(ChallengeCategory.EXPLORATION)
            .difficulty(ChallengeDifficulty.MEDIUM)
            .tokenReward(5)
            .type(ChallengeType.EXPLORATION)
            .targetKey("ANY")
            .targetAmount(10000)
            .prerequisite("exploration_starter")
            .isIslandWide(false)
            .gridPosition(0, 0)
            .build());
    }
    
    private static void registerEconomyChallenges(ChallengeManager manager) {
        /*
         * ECONOMY TREE LAYOUT (Vertical Wealth):
         * 
         * Row  0:  economy_wealthy (0, 0)
         *              |
         * Row -1:  economy_starter (0, -1)
         */
        
        // STARTER (Y=-1) - First earnings
        manager.registerChallenge(new IslandChallenge.Builder("economy_starter", "First Earnings")
            .description("Earn 1000 coins")
            .category(ChallengeCategory.ECONOMY)
            .difficulty(ChallengeDifficulty.EASY)
            .tokenReward(3)
            .type(ChallengeType.ECONOMY)
            .targetKey("COINS")
            .targetAmount(1000)
            .isIslandWide(true)
            .gridPosition(0, -1)
            .build());
        
        // TIER 1 (Y=0) - Wealthy
        manager.registerChallenge(new IslandChallenge.Builder("economy_wealthy", "Wealthy Trader")
            .description("Earn 10000 coins - become wealthy")
            .category(ChallengeCategory.ECONOMY)
            .difficulty(ChallengeDifficulty.HARD)
            .tokenReward(8)
            .type(ChallengeType.ECONOMY)
            .targetKey("COINS")
            .targetAmount(10000)
            .prerequisite("economy_starter")
            .isIslandWide(true)
            .gridPosition(0, 0)
            .build());
    }
    
    private static void registerSocialChallenges(ChallengeManager manager) {
        /*
         * SOCIAL TREE LAYOUT (Vertical Community):
         * 
         * Row  0:  social_cooperative (0, 0)
         *              |
         * Row -1:  social_starter (0, -1)
         */
        
        // STARTER (Y=-1) - First member
        manager.registerChallenge(new IslandChallenge.Builder("social_starter", "Team Player")
            .description("Add a member to your island")
            .category(ChallengeCategory.SOCIAL)
            .difficulty(ChallengeDifficulty.STARTER)
            .tokenReward(2)
            .type(ChallengeType.SOCIAL)
            .targetKey("ADD_MEMBER")
            .targetAmount(1)
            .isIslandWide(true)
            .gridPosition(0, -1)
            .build());
        
        // TIER 1 (Y=0) - Cooperative
        manager.registerChallenge(new IslandChallenge.Builder("social_cooperative", "Community Builder")
            .description("Complete 10 shared tasks with island members")
            .category(ChallengeCategory.SOCIAL)
            .difficulty(ChallengeDifficulty.MEDIUM)
            .tokenReward(6)
            .type(ChallengeType.SOCIAL)
            .targetKey("SHARED_TASKS")
            .targetAmount(10)
            .prerequisite("social_starter")
            .isIslandWide(true)
            .gridPosition(0, 0)
            .build());
    }
    
    private static void registerProgressionChallenges(ChallengeManager manager) {
        /*
         * PROGRESSION TREE LAYOUT (Vertical Achievement):
         * 
         * Row +1:  progression_veteran (0, 1)
         *              |
         * Row  0:  progression_intermediate (0, 0)
         *              |
         * Row -1:  progression_novice (0, -1)
         */
        
        // STARTER (Y=-1) - Complete first challenges
        manager.registerChallenge(new IslandChallenge.Builder("progression_novice", "Novice Achiever")
            .description("Complete 5 challenges")
            .category(ChallengeCategory.PROGRESSION)
            .difficulty(ChallengeDifficulty.EASY)
            .tokenReward(2)
            .type(ChallengeType.CUSTOM)
            .targetKey("ANY")
            .targetAmount(5)
            .isIslandWide(true)
            .gridPosition(0, -1)
            .build());
        
        // TIER 1 (Y=0) - Intermediate
        manager.registerChallenge(new IslandChallenge.Builder("progression_intermediate", "Intermediate Achiever")
            .description("Complete 15 challenges")
            .category(ChallengeCategory.PROGRESSION)
            .difficulty(ChallengeDifficulty.MEDIUM)
            .tokenReward(5)
            .type(ChallengeType.CUSTOM)
            .targetKey("ANY")
            .targetAmount(15)
            .prerequisite("progression_novice")
            .isIslandWide(true)
            .gridPosition(0, 0)
            .build());
        
        // TIER 2 (Y=1) - Veteran
        manager.registerChallenge(new IslandChallenge.Builder("progression_veteran", "Veteran Achiever")
            .description("Complete 30 challenges - a true veteran")
            .category(ChallengeCategory.PROGRESSION)
            .difficulty(ChallengeDifficulty.EXPERT)
            .tokenReward(12)
            .type(ChallengeType.CUSTOM)
            .targetKey("ANY")
            .targetAmount(30)
            .prerequisite("progression_intermediate")
            .isIslandWide(true)
            .gridPosition(0, 1)
            .build());
    }
    
    private static void registerSpecialChallenges(ChallengeManager manager) {
        /*
         * SPECIAL TREE LAYOUT (Independent Challenges):
         * 
         * Row  0:  special_rare (-1, 0) + special_event (1, 0)
         */
        
        // TIER 1 (Y=0) - Two independent special challenges
        manager.registerChallenge(new IslandChallenge.Builder("special_rare", "Rare Collector")
            .description("Find a rare item")
            .category(ChallengeCategory.SPECIAL)
            .difficulty(ChallengeDifficulty.EXPERT)
            .tokenReward(10)
            .type(ChallengeType.CUSTOM)
            .targetKey("RARE_ITEM")
            .targetAmount(1)
            .isIslandWide(false)
            .gridPosition(-1, 0)
            .build());
        
        manager.registerChallenge(new IslandChallenge.Builder("special_event", "Event Participant")
            .description("Participate in a server event")
            .category(ChallengeCategory.SPECIAL)
            .difficulty(ChallengeDifficulty.EXPERT)
            .tokenReward(10)
            .type(ChallengeType.CUSTOM)
            .targetKey("EVENT_PARTICIPATION")
            .targetAmount(1)
            .isIslandWide(false)
            .gridPosition(1, 0)
            .build());
    }
}


