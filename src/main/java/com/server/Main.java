package com.server;

import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import com.server.abilities.AbilityManager;
import com.server.commands.AdminSkillsCommand;
import com.server.commands.AdminStatsCommand;
import com.server.commands.AdminTokensCommand;
import com.server.commands.AnimationDebugCommand;
import com.server.commands.CosmeticCommand;
import com.server.commands.CurrencyCommand;
import com.server.commands.DebugCommand;
import com.server.commands.FlyCommand;
import com.server.commands.GiveHatCommand;
import com.server.commands.GiveItemCommand;
import com.server.commands.MenuCommand;
import com.server.commands.ProfileCommand;
import com.server.commands.SkillCommand;
import com.server.commands.SpawnCustomMobCommand;
import com.server.commands.StatsCommand;
import com.server.cosmetics.CosmeticManager;
import com.server.display.ActionBarManager;
import com.server.display.DamageIndicatorManager;
import com.server.display.MobDisplayManager;
import com.server.display.ScoreboardManager;
import com.server.entities.CustomEntityManager;
import com.server.events.AbilityListener;
import com.server.events.AutoRespawnListener;
import com.server.events.CombatListener;
import com.server.events.CustomMobListener;
import com.server.events.GUIListener;
import com.server.events.ItemListener;
import com.server.events.PlayerListener;
import com.server.events.RangedCombatManager;
import com.server.profiles.ProfileManager;
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
        
        // Register commands and event listeners
        registerCommands();
        registerListeners();
        
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
        
        // Cleanup cosmetics
        CosmeticManager.getInstance().cleanup();
        
        LOGGER.info("mmo disabled");
    }

    private void registerListeners() {
        this.getServer().getPluginManager().registerEvents(new GUIListener(), this);
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        this.getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        this.getServer().getPluginManager().registerEvents(mobDisplayManager, this);
        this.getServer().getPluginManager().registerEvents(damageIndicatorManager, this);
        this.getServer().getPluginManager().registerEvents(new ItemListener(), this);
        this.getServer().getPluginManager().registerEvents(new AbilityListener(), this);
        this.getServer().getPluginManager().registerEvents(rangedCombatManager, this);
        this.getServer().getPluginManager().registerEvents(new HealthRegenerationListener(this), this);
        this.getServer().getPluginManager().registerEvents(new AutoRespawnListener(this), this);
        this.getServer().getPluginManager().registerEvents(new CustomMobListener(this), this);
        this.getCommand("animdebug").setExecutor(new AnimationDebugCommand(this));
        
        // Register skill listeners
        this.getServer().getPluginManager().registerEvents(new SkillEventListener(this), this);
        this.getServer().getPluginManager().registerEvents(new SkillGUIListener(), this);
        
        // Register skill action bar listener
        this.getServer().getPluginManager().registerEvents(new SkillActionBarListener(), this);
        this.getServer().getPluginManager().registerEvents(new SkillTreeGUIListener(), this);
        this.getServer().getPluginManager().registerEvents(new SkillLevelupListener(this), this);

        this.getServer().getPluginManager().registerEvents(new MiningListener(this), this);
        this.getServer().getPluginManager().registerEvents(new AbilityGUIListener(this), this);
    }

    public static Main getInstance() {
        return instance;
    }

    public boolean isDebugMode() {
        return getConfig().getBoolean("debug-mode", false);
    }

    public void setDebugMode(boolean enabled) {
        getConfig().set("debug-mode", enabled);
        saveConfig();
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

        org.bukkit.command.PluginCommand debugCommand = this.getCommand("debugmode");
        if (debugCommand != null) {
            debugCommand.setExecutor(new DebugCommand());
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
}