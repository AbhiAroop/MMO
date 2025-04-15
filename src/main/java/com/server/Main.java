package com.server;

import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import com.server.abilities.AbilityManager;
import com.server.commands.CosmeticCommand;
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

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        actionBarManager = new ActionBarManager(this);
        actionBarManager.startActionBarUpdates();
        
        mobDisplayManager = new MobDisplayManager();
        damageIndicatorManager = new DamageIndicatorManager(this);

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
        
        // Cleanup cosmetics
        CosmeticManager.getInstance().cleanup();
        
        LOGGER.info("mmo disabled");
    }

    private void registerListeners() {
        this.getServer().getPluginManager().registerEvents(new GUIListener(), this);
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        this.getServer().getPluginManager().registerEvents(new CombatListener(), this);
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
            giveItemCommand.setExecutor(new GiveItemCommand());
        } else {
            LOGGER.warning("Command 'giveitem' not registered in plugin.yml file!");
        }
    }



}