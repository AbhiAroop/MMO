package com.server.profiles.skills.minigames;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.Main;
import com.server.debug.DebugManager.DebugSystem;
import com.server.profiles.skills.events.GemCarvingListener;

/**
 * Manager class for the GemCarving minigame
 */
public class GemCarvingManager {
    private final Main plugin;
    private final GemCarvingMinigame minigame;
    private GemCarvingListener listener;
    
    public GemCarvingManager(Main plugin) {
        this.plugin = plugin;
        this.minigame = new GemCarvingMinigame(plugin);
    }
    
    /**
     * Initialize the manager
     */
    public void initialize() {
        // Register the listener
        listener = new GemCarvingListener(plugin, minigame);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        
        // Log initialization
        plugin.debugLog(DebugSystem.SKILLS,"GemCarving minigame initialized");
    }
    
    /**
     * Clean up when the plugin is disabled
     */
    public void shutdown() {
        // Cancel any ongoing minigames
        minigame.cancelAllSessions();
        
        // Unregister listeners
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
    }
    
    /**
     * Create a gem carving tool
     */
    public ItemStack createGemCarvingTool() {
        ItemStack tool = new ItemStack(Material.SHEARS);
        ItemMeta meta = tool.getItemMeta();
        
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Gem Carving Tool");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "A special tool used for",
            ChatColor.GRAY + "carefully extracting gems",
            ChatColor.GRAY + "from crystals.",
            "",
            ChatColor.YELLOW + "Left-click on a crystal to",
            ChatColor.YELLOW + "begin the extraction process!"
        ));
        
        // Add custom model data to make it look unique
        meta.setCustomModelData(8001);
        
        tool.setItemMeta(meta);
        return tool;
    }
    
    /**
     * Give a gem carving tool to a player
     */
    public void giveGemCarvingTool(Player player) {
        ItemStack tool = createGemCarvingTool();
        player.getInventory().addItem(tool);
        player.sendMessage(ChatColor.GREEN + "You received a " + ChatColor.LIGHT_PURPLE + "Gem Carving Tool" + ChatColor.GREEN + "!");
    }
    
    /**
     * Get the minigame instance
     */
    public GemCarvingMinigame getMinigame() {
        return minigame;
    }
}