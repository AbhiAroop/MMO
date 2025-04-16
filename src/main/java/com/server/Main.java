package com.server;

import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import com.server.abilities.AbilityManager;
import com.server.commands.CosmeticCommand;
import com.server.commands.CurrencyCommand;
import com.server.commands.DebugCommand;
import com.server.commands.GiveHatCommand;
import com.server.commands.GiveItemCommand;
import com.server.commands.MenuCommand;
import com.server.commands.ProfileCommand;
import com.server.commands.StatsCommand;
import com.server.cosmetics.CosmeticGUI;
import com.server.cosmetics.CosmeticManager;
import com.server.display.ActionBarManager;
import com.server.display.DamageIndicatorManager;
import com.server.display.MobDisplayManager;
import com.server.display.ScoreboardManager;
import com.server.events.AbilityListener;
import com.server.events.CombatListener;
import com.server.events.GUIListener;
import com.server.events.ItemListener;
import com.server.events.PlayerListener;
import com.server.events.RangedCombatManager;

public class Main extends JavaPlugin {
    private static Main instance;
    private static final Logger LOGGER = Logger.getLogger("mmo");
    private ActionBarManager actionBarManager;
    private MobDisplayManager mobDisplayManager;
    private DamageIndicatorManager damageIndicatorManager;
    private ScoreboardManager scoreboardManager;

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

        // Initialize CosmeticManager
        CosmeticManager.initialize(this);
        AbilityManager.initialize(this);
        
        // Register commands and listeners
        registerCommands();
        registerListeners();
        LOGGER.info("mmo enabled");
    }
    
    @Override
    public void onDisable() {
        if (actionBarManager != null) {
            actionBarManager.stopActionBarUpdates();
        }

        if (scoreboardManager != null) {
            scoreboardManager.cleanup();
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
        this.getServer().getPluginManager().registerEvents(new CosmeticGUI(), this);
        this.getServer().getPluginManager().registerEvents(new AbilityListener(), this);
        this.getServer().getPluginManager().registerEvents(new RangedCombatManager(this), this);
    }

    public static Main getInstance() {
        return instance;
    }

    public boolean isDebugMode() {
        return getConfig().getBoolean("debug-mode", false);
    }

    public void setDebugMode(boolean enabled) {
        // Update the value in the config
        getConfig().set("debug-mode", enabled);
        // Save the config to persist the setting
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

    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }



}