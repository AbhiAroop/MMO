package com.server;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.server.abilities.AbilityManager;
import com.server.commands.AdminFurnaceCommand;
import com.server.commands.AdminProfileCommand;
import com.server.commands.AdminSkillsCommand;
import com.server.commands.AdminStatsCommand;
import com.server.commands.AdminTokensCommand;
import com.server.commands.AnimationDebugCommand;
import com.server.commands.CosmeticCommand;
import com.server.commands.CraftingCommand;
import com.server.commands.CrystalCommand;
import com.server.commands.CurrencyCommand;
import com.server.commands.DebugCommand;
import com.server.commands.EnchantCommand;
import com.server.commands.FlyCommand;
import com.server.commands.GemCarvingToolCommand;
import com.server.commands.GiveHatCommand;
import com.server.commands.GiveItemCommand;
import com.server.commands.MenuCommand;
import com.server.commands.NPCCommand;
import com.server.commands.ProfileCommand;
import com.server.commands.SkillCommand;
import com.server.commands.SpawnCustomMobCommand;
import com.server.commands.StatsCommand;
import com.server.cosmetics.CosmeticManager;
import com.server.crafting.listeners.AdvancedCraftingListener;
import com.server.crafting.listeners.AutoCraftingListener;
import com.server.crafting.listeners.CustomCraftingListener;
import com.server.crafting.listeners.CustomFurnaceGUIListener;
import com.server.crafting.listeners.CustomFurnaceListener;
import com.server.crafting.listeners.VanillaCraftingReplacer;
import com.server.crafting.manager.CustomCraftingManager;
import com.server.crafting.manager.CustomFurnaceManager;
import com.server.debug.DebugManager;
import com.server.debug.DebugManager.DebugSystem;
import com.server.display.ActionBarManager;
import com.server.display.DamageIndicatorManager;
import com.server.display.MobDisplayManager;
import com.server.display.ScoreboardManager;
import com.server.enchantments.listeners.EnchantmentGUIListener;
import com.server.enchantments.listeners.EnchantmentTableListener;
import com.server.enchantments.listeners.EnchantmentTriggerListener;
import com.server.enchantments.structure.EnchantmentTableStructure;
import com.server.entities.CustomEntityManager;
import com.server.entities.npc.NPCManager;
import com.server.entities.npc.dialogue.DialogueManager;
import com.server.events.AbilityListener;
import com.server.events.AutoItemEnhancementListener;
import com.server.events.AutoRespawnListener;
import com.server.events.CombatListener;
import com.server.events.CustomMobListener;
import com.server.events.GUIListener;
import com.server.events.ItemListener;
import com.server.events.NPCDamageListener;
import com.server.events.PlayerListener;
import com.server.events.RangedCombatManager;
import com.server.events.RangedDamageListener;
import com.server.profiles.ProfileManager;
import com.server.profiles.gui.ProfileGUI;
import com.server.profiles.skills.abilities.AbilityRegistry;
import com.server.profiles.skills.abilities.gui.AbilityGUIListener;
import com.server.profiles.skills.core.Skill;
import com.server.profiles.skills.core.SkillLevelupListener;
import com.server.profiles.skills.core.SkillProgressionManager;
import com.server.profiles.skills.core.SkillRegistry;
import com.server.profiles.skills.display.SkillActionBarManager;
import com.server.profiles.skills.events.MiningListener;
import com.server.profiles.skills.events.SkillActionBarListener;
import com.server.profiles.skills.events.SkillEventListener;
import com.server.profiles.skills.gui.SkillGUIListener;
import com.server.profiles.skills.gui.SkillTreeGUIListener;
import com.server.profiles.skills.minigames.GemCarvingManager;
import com.server.profiles.skills.trees.SkillTreeRegistry;
import com.server.profiles.stats.StatScanManager;
import com.server.profiles.stats.health.HealthRegenerationListener;
import com.server.profiles.stats.health.HealthRegenerationManager;

public class Main extends JavaPlugin {
    private static Main instance;
    private static final Logger LOGGER = Logger.getLogger("mmo");
    private ActionBarManager actionBarManager;
    private MobDisplayManager mobDisplayManager;
    private DamageIndicatorManager damageIndicatorManager;
    private ScoreboardManager scoreboardManager;
    private HealthRegenerationManager healthRegenerationManager;
    private RangedCombatManager rangedCombatManager;
    private StatScanManager statScanManager;
    private CustomEntityManager customEntityManager;
    private AbilityRegistry abilityRegistry;
    private GemCarvingManager gemCarvingManager;
    private ScheduledExecutorService playtimeUpdateService;
    private EnchantmentTableStructure enchantmentTableStructure;
    private EnchantmentGUIListener enchantmentGUIListener;

    // Update the onEnable method
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        actionBarManager = new ActionBarManager(this);
        actionBarManager.startActionBarUpdates();
        
        mobDisplayManager = new MobDisplayManager();
        damageIndicatorManager = new DamageIndicatorManager(this);

        // Initialize scoreboard manager
        scoreboardManager = new ScoreboardManager(this);

        // Initialize health regeneration manager
        healthRegenerationManager = new HealthRegenerationManager(this);
        
        // Initialize the stat scan manager FIRST before RangedCombatManager
        statScanManager = new StatScanManager(this);

        // Initialize RangedCombatManager AFTER StatScanManager
        rangedCombatManager = new RangedCombatManager(this);

        // Initialize custom entity manager
        customEntityManager = new CustomEntityManager(this);

        // Initialize CosmeticManager
        CosmeticManager.initialize(this);
        AbilityManager.initialize(this);

        // Initialize skills system
        SkillRegistry.initialize(this);
        SkillProgressionManager.initialize(this);
        
        // Initialize skill action bar manager
        SkillActionBarManager.initialize(this);
        
        // Initialize SkillTreeRegistry AFTER SkillRegistry to ensure all skills are available
        SkillTreeRegistry.initialize(this);

        // Initialize ability registry
        AbilityRegistry.initialize(this);
        
        // Initialize the GemCarving minigame
        gemCarvingManager = new GemCarvingManager(this);
        gemCarvingManager.initialize();

        // Initialize NPC manager
        NPCManager.initialize(this);

        // Initialize DialogueManager after NPCManager
        DialogueManager.getInstance();

        // Initialize custom crafting system
        CustomCraftingManager.getInstance();

        initializeFurnaceSystem();
        
        // Initialize enchantment system
        initializeEnchantmentSystem();

        // Register commands and event listeners
        registerCommands();
        registerListeners();

        startPlaytimeUpdateService();
        
        // NEW: Verify all skills and subskills are properly registered
        if (isDebugMode()) {
            getLogger().info("====== Verifying Skill Registration ======");
            
            // Check for subskills
            getLogger().info("Checking for ore_extraction subskill: " + 
                        (SkillRegistry.getInstance().getSkill("ore_extraction") != null ? "FOUND" : "NOT FOUND"));
            getLogger().info("Checking for gem_carving subskill: " + 
                        (SkillRegistry.getInstance().getSkill("gem_carving") != null ? "FOUND" : "NOT FOUND"));
            
            // Log all registered skills
            getLogger().info("All registered skills:");
            for (Skill skill : SkillRegistry.getInstance().getAllSkills()) {
                getLogger().info("  - " + skill.getId() + ": " + skill.getDisplayName() + 
                            " (Main: " + skill.isMainSkill() + ")");
            }
            
            // Verify skill trees
            getLogger().info("Checking skill trees:");
            for (Skill skill : SkillRegistry.getInstance().getAllSkills()) {
                boolean hasTree = SkillTreeRegistry.getInstance().getSkillTree(skill) != null;
                getLogger().info("  - " + skill.getId() + " has tree: " + hasTree);
            }
            getLogger().info("=========================================");
        }
        
        getLogger().info("mmo enabled with ModelEngine integration");
    }

    @Override
    public void onDisable() {
        if (actionBarManager != null) {
            actionBarManager.stopActionBarUpdates();
        }

        if (scoreboardManager != null) {
            scoreboardManager.cleanup();
        }

        if (healthRegenerationManager != null) {
            healthRegenerationManager.cleanup();
        }
        
        // Clean up custom entities
        if (customEntityManager != null) {
            customEntityManager.cleanup();
        }

        // Clean up NPC resources
        NPCManager.getInstance().cleanup();
        
        // Cleanup cosmetics
        CosmeticManager.getInstance().cleanup();

        if (playtimeUpdateService != null) {
            playtimeUpdateService.shutdown();
        }

        if (CustomFurnaceManager.getInstance() != null) {
            CustomFurnaceManager.getInstance().shutdown();
        }
        
        LOGGER.info("mmo disabled");
    }

    private void registerListeners() {
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        this.getServer().getPluginManager().registerEvents(new AutoItemEnhancementListener(this), this);
        this.getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        this.getServer().getPluginManager().registerEvents(mobDisplayManager, this);
        this.getServer().getPluginManager().registerEvents(damageIndicatorManager, this);
        this.getServer().getPluginManager().registerEvents(new ItemListener(), this);
        this.getServer().getPluginManager().registerEvents(new AbilityListener(), this);
        this.getServer().getPluginManager().registerEvents(rangedCombatManager, this);
        this.getServer().getPluginManager().registerEvents(new HealthRegenerationListener(this), this);
        this.getServer().getPluginManager().registerEvents(new AutoRespawnListener(this), this);
        this.getServer().getPluginManager().registerEvents(new CustomMobListener(this), this);
        this.getServer().getPluginManager().registerEvents(new NPCDamageListener(this), this);
        this.getCommand("animdebug").setExecutor(new AnimationDebugCommand(this));
        
        // Register skill listeners
        this.getServer().getPluginManager().registerEvents(new SkillEventListener(this), this);
        this.getServer().getPluginManager().registerEvents(new SkillGUIListener(this), this);
        
        // Register skill action bar listener
        this.getServer().getPluginManager().registerEvents(new SkillActionBarListener(), this);
        this.getServer().getPluginManager().registerEvents(new SkillTreeGUIListener(), this);
        this.getServer().getPluginManager().registerEvents(new SkillLevelupListener(this), this);

        this.getServer().getPluginManager().registerEvents(new MiningListener(this), this);
        this.getServer().getPluginManager().registerEvents(new AbilityGUIListener(this), this);

        this.getServer().getPluginManager().registerEvents(new RangedDamageListener(this), this);

        this.getServer().getPluginManager().registerEvents(new CustomCraftingListener(this), this);
        this.getServer().getPluginManager().registerEvents(new AdvancedCraftingListener(this), this);
        this.getServer().getPluginManager().registerEvents(new AutoCraftingListener(this), this);
        this.getServer().getPluginManager().registerEvents(new VanillaCraftingReplacer(), this);

        this.getServer().getPluginManager().registerEvents(new CustomFurnaceListener(this), this);
        this.getServer().getPluginManager().registerEvents(new CustomFurnaceGUIListener(this), this);
        this.getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        
        // Register enchantment system listeners
        this.getServer().getPluginManager().registerEvents(new EnchantmentTableListener(this, enchantmentTableStructure, enchantmentGUIListener), this);
        this.getServer().getPluginManager().registerEvents(enchantmentGUIListener, this);
        this.getServer().getPluginManager().registerEvents(new EnchantmentTriggerListener(), this);


    }

    public static Main getInstance() {
        return instance;
    }

    /**
     * Check if global debug mode is enabled
     * 
     * @return true if global debug mode is enabled
     */
    public boolean isDebugMode() {
        return DebugManager.getInstance().isDebugEnabled(DebugSystem.ALL);
    }

    /**
     * Set global debug mode
     * 
     * @param enabled Whether debug mode should be enabled
     */
    public void setDebugMode(boolean enabled) {
        DebugManager.getInstance().setDebugEnabled(DebugSystem.ALL, enabled);
    }

    private void registerCommands() {
        org.bukkit.command.PluginCommand menuCommand = this.getCommand("menu");
        if (menuCommand != null) {
            menuCommand.setExecutor(new MenuCommand());
        } else {
            LOGGER.warning("Command 'menu' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand profileCommand = this.getCommand("profile");
        if (profileCommand != null) {
            profileCommand.setExecutor(new ProfileCommand());
        } else {
            LOGGER.warning("Command 'profile' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand statsCommand = this.getCommand("stats");
        if (statsCommand != null) {
            statsCommand.setExecutor(new StatsCommand());
        } else {
            LOGGER.warning("Command 'stats' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand cosmeticCommand = this.getCommand("cosmetics");
        if (cosmeticCommand != null) {
            cosmeticCommand.setExecutor(new CosmeticCommand());
        } else {
            LOGGER.warning("Command 'cosmetics' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand giveHatCommand = this.getCommand("givehat");
        if (giveHatCommand != null) {
            giveHatCommand.setExecutor(new GiveHatCommand());
        } else {
            LOGGER.warning("Command 'givehat' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand giveItemCommand = this.getCommand("giveitem");
        if (giveItemCommand != null) {
            GiveItemCommand giveItemHandler = new GiveItemCommand();
            giveItemCommand.setExecutor(giveItemHandler);
            giveItemCommand.setTabCompleter(giveItemHandler);
        } else {
            LOGGER.warning("Command 'giveitem' not registered in plugin.yml file!");
        }

        DebugCommand debugHandler = new DebugCommand(this);
        org.bukkit.command.PluginCommand debugCommand = this.getCommand("debugmode");
        if (debugCommand != null) {
            debugCommand.setExecutor(debugHandler);
            debugCommand.setTabCompleter(debugHandler);
        } else {
            LOGGER.warning("Command 'debugmode' not registered in plugin.yml file!");
        }

        // Register the new FlyCommand
        org.bukkit.command.PluginCommand flyCommand = this.getCommand("fly");
        if (flyCommand != null) {
            FlyCommand flyHandler = new FlyCommand(this);
            flyCommand.setExecutor(flyHandler);
            flyCommand.setTabCompleter(flyHandler);
        } else {
            LOGGER.warning("Command 'fly' not registered in plugin.yml file!");
        }

        CurrencyCommand currencyHandler = new CurrencyCommand(this);

        // Register all currency-related commands with the SAME instance
        org.bukkit.command.PluginCommand balanceCommand = this.getCommand("balance");
        if (balanceCommand != null) {
            balanceCommand.setExecutor(currencyHandler);
            balanceCommand.setTabCompleter(currencyHandler);
        } else {
            LOGGER.warning("Command 'balance' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand currencyCommand = this.getCommand("currency");
        if (currencyCommand != null) {
            currencyCommand.setExecutor(currencyHandler);
            currencyCommand.setTabCompleter(currencyHandler);
        } else {
            LOGGER.warning("Command 'currency' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand payCommand = this.getCommand("pay");
        if (payCommand != null) {
            payCommand.setExecutor(currencyHandler);
            payCommand.setTabCompleter(currencyHandler);
        } else {
            LOGGER.warning("Command 'pay' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand confirmCommand = this.getCommand("confirm");
        if (confirmCommand != null) {
            confirmCommand.setExecutor(currencyHandler);
            confirmCommand.setTabCompleter(currencyHandler);
        } else {
            LOGGER.warning("Command 'confirm' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand cancelCommand = this.getCommand("cancel");
        if (cancelCommand != null) {
            cancelCommand.setExecutor(currencyHandler);
            cancelCommand.setTabCompleter(currencyHandler);
        } else {
            LOGGER.warning("Command 'cancel' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand spawnMobCommand = this.getCommand("spawnmob");
        if (spawnMobCommand != null) {
            SpawnCustomMobCommand spawnMobHandler = new SpawnCustomMobCommand(this);
            spawnMobCommand.setExecutor(spawnMobHandler);
            spawnMobCommand.setTabCompleter(spawnMobHandler);
        } else {
            LOGGER.warning("Command 'spawnmob' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand skillsCommand = this.getCommand("skills");
        if (skillsCommand != null) {
            skillsCommand.setExecutor(new SkillCommand());
        } else {
            LOGGER.warning("Command 'skills' not registered in plugin.yml file!");
        }

        // Register AdminStatsCommand
         org.bukkit.command.PluginCommand adminStatsCommand = this.getCommand("adminstats");
        if (adminStatsCommand != null) {
            AdminStatsCommand adminStatsHandler = new AdminStatsCommand(this);
            adminStatsCommand.setExecutor(adminStatsHandler);
            adminStatsCommand.setTabCompleter(adminStatsHandler);
        } else {
            LOGGER.warning("Command 'adminstats' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand adminTokensCommand = this.getCommand("admintokens");
        if (adminTokensCommand != null) {
            AdminTokensCommand adminTokensHandler = new AdminTokensCommand(this);
            adminTokensCommand.setExecutor(adminTokensHandler);
            adminTokensCommand.setTabCompleter(adminTokensHandler);
        } else {
            LOGGER.warning("Command 'admintokens' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand adminSkillsCommand = this.getCommand("adminskills");
        if (adminSkillsCommand != null) {
            AdminSkillsCommand adminSkillsHandler = new AdminSkillsCommand(this);
            adminSkillsCommand.setExecutor(adminSkillsHandler);
            adminSkillsCommand.setTabCompleter(adminSkillsHandler);
        } else {
            LOGGER.warning("Command 'adminskills' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand crystalCommand = this.getCommand("crystal");
        if (crystalCommand != null) {
            CrystalCommand crystalHandler = new CrystalCommand(this);
            crystalCommand.setExecutor(crystalHandler);
            crystalCommand.setTabCompleter(crystalHandler);
        } else {
            LOGGER.warning("Command 'crystal' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand gemToolCommand = this.getCommand("gemtool");
        if (gemToolCommand != null) {
            GemCarvingToolCommand gemToolHandler = new GemCarvingToolCommand(this, gemCarvingManager);
            gemToolCommand.setExecutor(gemToolHandler);
            gemToolCommand.setTabCompleter(gemToolHandler);
        } else {
            LOGGER.warning("Command 'gemtool' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand npcCommand = this.getCommand("mmonpc");
        if (npcCommand != null) {
            NPCCommand npcHandler = new NPCCommand(this);
            npcCommand.setExecutor(npcHandler);
            npcCommand.setTabCompleter(npcHandler);
        } else {
            LOGGER.warning("Command 'mmonpc' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand adminProfileCommand = this.getCommand("adminprofile");
        if (adminProfileCommand != null) {
            AdminProfileCommand adminProfileHandler = new AdminProfileCommand(this);
            adminProfileCommand.setExecutor(adminProfileHandler);
            adminProfileCommand.setTabCompleter(adminProfileHandler);
        } else {
            LOGGER.warning("Command 'adminprofile' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand craftingCommand = this.getCommand("crafting");
        if (craftingCommand != null) {
            craftingCommand.setExecutor(new CraftingCommand());
        } else {
            LOGGER.warning("Command 'crafting' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand adminFurnaceCommand = this.getCommand("adminfurnace");
        if (adminFurnaceCommand != null) {
            adminFurnaceCommand.setExecutor(new AdminFurnaceCommand());
        } else {
            LOGGER.warning("Command 'adminfurnace' not registered in plugin.yml file!");
        }

        org.bukkit.command.PluginCommand enchantCommand = this.getCommand("enchant");
        if (enchantCommand != null) {
            EnchantCommand enchantHandler = new EnchantCommand(enchantmentTableStructure);
            enchantCommand.setExecutor(enchantHandler);
            enchantCommand.setTabCompleter(enchantHandler);
        } else {
            LOGGER.warning("Command 'enchant' not registered in plugin.yml file!");
        }
        
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public HealthRegenerationManager getHealthRegenerationManager() {
        return healthRegenerationManager;
    }

    /**
     * Get the ProfileManager instance
     * a
     * @return The ProfileManager instance
     */
    public ProfileManager getProfileManager() {
        return ProfileManager.getInstance();
    }

    /**
     * Get the RangedCombatManager instance
     * 
     * @return The RangedCombatManager instance
     */
    public RangedCombatManager getRangedCombatManager() {
        return rangedCombatManager;
    }

    public StatScanManager getStatScanManager() {
        return statScanManager;
    }

    public CustomEntityManager getCustomEntityManager() {
        return customEntityManager;
    }
    
    /**
     * Get the ActionBarManager instance
     * 
     * @return The ActionBarManager instance
     */
    public ActionBarManager getActionBarManager() {
        return actionBarManager;
    }

    public AbilityRegistry getAbilityRegistry() {
        return AbilityRegistry.getInstance();
    }

    /**
     * Check if debugging is enabled for a specific system
     * 
     * @param system The system to check
     * @return true if debugging is enabled for the system
     */
    public boolean isDebugEnabled(DebugSystem system) {
        return DebugManager.getInstance().isDebugEnabled(system);
    }

    /**
     * Log a debug message for a specific system
     * 
     * @param system The system logging the message
     * @param message The message to log
     */
    public void debugLog(DebugSystem system, String message) {
        DebugManager.getInstance().debug(system, message);
    }

    /**
     * Get the DamageIndicatorManager instance
     *
     * @return The DamageIndicatorManager instance
     */
    public DamageIndicatorManager getDamageIndicatorManager() {
        return damageIndicatorManager;
    }

    /**
     * Start the playtime update service for real-time GUI updates
     */
    private void startPlaytimeUpdateService() {
        playtimeUpdateService = newScheduledThreadPool(1);
        
        // Update playtime displays every 30 seconds
        playtimeUpdateService.scheduleAtFixedRate(() -> {
            // Update any open menu GUIs with current playtime
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getOpenInventory() != null) {
                    String title = player.getOpenInventory().getTitle();
                    
                    // Check if player has our menu GUIs open
                    if (title.equals(ProfileGUI.PLAYER_MENU_TITLE) || 
                        title.equals(ProfileGUI.PROFILE_SELECTION_TITLE)) {
                        
                        // Schedule GUI refresh on main thread
                        Bukkit.getScheduler().runTask(this, () -> {
                            if (title.equals(ProfileGUI.PLAYER_MENU_TITLE)) {
                                ProfileGUI.openMainMenu(player);
                            } else if (title.equals(ProfileGUI.PROFILE_SELECTION_TITLE)) {
                                ProfileGUI.openProfileSelector(player);
                            }
                        });
                    }
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * Enhanced furnace system initialization
     * Step 4: Complete system integration
     */
    private void initializeFurnaceSystem() {
        try {
            // Initialize fuel registry (loads all fuel types)
            com.server.crafting.fuel.FuelRegistry.getInstance();
            
            // Initialize recipe registry (loads all recipes)
            com.server.crafting.recipes.FurnaceRecipeRegistry.getInstance();
            
            // Initialize furnace manager (starts processing loop)
            com.server.crafting.manager.CustomFurnaceManager.getInstance();
            
            if (isDebugEnabled(DebugSystem.GUI)) {
                debugLog(DebugSystem.GUI, "[Main] Initialized complete furnace system with recipes and processing");
            }
            
        } catch (Exception e) {
            getLogger().severe("Failed to initialize furnace system: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializeEnchantmentSystem() {
        try {
            // Initialize enchantment structure manager
            enchantmentTableStructure = new EnchantmentTableStructure(this);
            
            // Initialize GUI listener (needs to be created first so table listener can use it)
            enchantmentGUIListener = new EnchantmentGUIListener();
            
            // Validate existing structures on startup
            int removed = enchantmentTableStructure.validateAllStructures();
            if (removed > 0) {
                getLogger().info("Removed " + removed + " invalid enchantment table structures");
            }
            
            getLogger().info("Initialized enchantment system with " + 
                enchantmentTableStructure.getRegisteredStructures().size() + " registered structures");
            
        } catch (Exception e) {
            getLogger().severe("Failed to initialize enchantment system: " + e.getMessage());
            e.printStackTrace();
        }
    }

}