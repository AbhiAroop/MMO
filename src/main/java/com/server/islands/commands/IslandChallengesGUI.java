package com.server.islands.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.islands.data.IslandChallenge.ChallengeCategory;
import com.server.islands.managers.ChallengeManager;
import com.server.islands.managers.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Main challenges menu showing all challenge categories.
 */
public class IslandChallengesGUI {
    
    private static final String GUI_TITLE = "🏆 Island Challenges";
    
    /**
     * Opens the main challenges menu for a player.
     */
    public static void open(Player player, IslandManager islandManager, ChallengeManager challengeManager) {
        // Get player's island first
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                player.sendMessage(Component.text("§c✗ §cYou need an island to view challenges!"));
                return;
            }
            
            // Load island to show token count
            islandManager.loadIsland(islandId).thenAccept(island -> {
                if (island == null) return;
                
                Bukkit.getScheduler().runTask(challengeManager.getPlugin(), () -> {
                    Inventory gui = Bukkit.createInventory(null, 54, Component.text(GUI_TITLE));
                    
                    // Island Tokens Display (Slot 4)
                    gui.setItem(4, createMenuItem(
                        Material.NETHER_STAR,
                        "§b§lIsland Tokens",
                        "§7Your island's currency for upgrades",
                        "",
                        "§fCurrent Balance: §6§l" + island.getIslandTokens() + " ⭐",
                        "",
                        "§7Complete challenges to earn tokens!",
                        "§7Use tokens to purchase island upgrades"
                    ));
                    
                    // Farming (Slot 10)
                    gui.setItem(10, createMenuItem(
                        Material.GOLDEN_HOE,
                        "§a§l" + ChallengeCategory.FARMING.getIcon() + " Farming",
                        "§7" + ChallengeCategory.FARMING.getDescription(),
                        "",
                        "§7Plant crops, breed animals",
                        "§7and grow your farm!",
                        "",
                        "§eClick to view challenges!"
                    ));
                    
                    // Mining (Slot 12)
                    gui.setItem(12, createMenuItem(
                        Material.DIAMOND_PICKAXE,
                        "§8§l" + ChallengeCategory.MINING.getIcon() + " Mining",
                        "§7" + ChallengeCategory.MINING.getDescription(),
                        "",
                        "§7Mine ores, stone, and gather",
                        "§7resources from the earth!",
                        "",
                        "§eClick to view challenges!"
                    ));
                    
                    // Combat (Slot 14)
                    gui.setItem(14, createMenuItem(
                        Material.DIAMOND_SWORD,
                        "§c§l" + ChallengeCategory.COMBAT.getIcon() + " Combat",
                        "§7" + ChallengeCategory.COMBAT.getDescription(),
                        "",
                        "§7Defeat mobs and prove your",
                        "§7strength in battle!",
                        "",
                        "§eClick to view challenges!"
                    ));
                    
                    // Building (Slot 16)
                    gui.setItem(16, createMenuItem(
                        Material.BRICKS,
                        "§6§l" + ChallengeCategory.BUILDING.getIcon() + " Building",
                        "§7" + ChallengeCategory.BUILDING.getDescription(),
                        "",
                        "§7Place blocks and create",
                        "§7amazing structures!",
                        "",
                        "§eClick to view challenges!"
                    ));
                    
                    // Crafting (Slot 19)
                    gui.setItem(19, createMenuItem(
                        Material.CRAFTING_TABLE,
                        "§e§l" + ChallengeCategory.CRAFTING.getIcon() + " Crafting",
                        "§7" + ChallengeCategory.CRAFTING.getDescription(),
                        "",
                        "§7Craft tools, items, and",
                        "§7useful equipment!",
                        "",
                        "§eClick to view challenges!"
                    ));
                    
                    // Exploration (Slot 21)
                    gui.setItem(21, createMenuItem(
                        Material.COMPASS,
                        "§3§l" + ChallengeCategory.EXPLORATION.getIcon() + " Exploration",
                        "§7" + ChallengeCategory.EXPLORATION.getDescription(),
                        "",
                        "§7Visit other islands and",
                        "§7explore the world!",
                        "",
                        "§eClick to view challenges!"
                    ));
                    
                    // Economy (Slot 23)
                    gui.setItem(23, createMenuItem(
                        Material.EMERALD,
                        "§2§l" + ChallengeCategory.ECONOMY.getIcon() + " Economy",
                        "§7" + ChallengeCategory.ECONOMY.getDescription(),
                        "",
                        "§7Trade with villagers and",
                        "§7build your wealth!",
                        "",
                        "§eClick to view challenges!"
                    ));
                    
                    // Social (Slot 25)
                    gui.setItem(25, createMenuItem(
                        Material.PLAYER_HEAD,
                        "§d§l" + ChallengeCategory.SOCIAL.getIcon() + " Social",
                        "§7" + ChallengeCategory.SOCIAL.getDescription(),
                        "",
                        "§7Invite members and grow",
                        "§7your island community!",
                        "",
                        "§eClick to view challenges!"
                    ));
                    
                    // Progression (Slot 28)
                    gui.setItem(28, createMenuItem(
                        Material.EXPERIENCE_BOTTLE,
                        "§b§l" + ChallengeCategory.PROGRESSION.getIcon() + " Progression",
                        "§7" + ChallengeCategory.PROGRESSION.getDescription(),
                        "",
                        "§7Level up your island and",
                        "§7reach new milestones!",
                        "",
                        "§eClick to view challenges!"
                    ));
                    
                    // Special (Slot 34)
                    gui.setItem(34, createMenuItem(
                        Material.DRAGON_EGG,
                        "§5§l" + ChallengeCategory.SPECIAL.getIcon() + " Special",
                        "§7" + ChallengeCategory.SPECIAL.getDescription(),
                        "",
                        "§7Unique and rare challenges",
                        "§7with special rewards!",
                        "",
                        "§eClick to view challenges!"
                    ));
                    
                    // Back to Island Menu (Slot 49)
                    gui.setItem(49, createBackButton());
                    
                    // Border decoration
                    ItemStack borderDecoration = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
                    ItemMeta borderMeta = borderDecoration.getItemMeta();
                    borderMeta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
                    borderDecoration.setItemMeta(borderMeta);
                    
                    ItemStack decoration = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                    ItemMeta decorationMeta = decoration.getItemMeta();
                    decorationMeta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
                    decoration.setItemMeta(decorationMeta);
                    
                    // Fill border (top and bottom rows)
                    for (int i = 0; i < 9; i++) {
                        if (gui.getItem(i) == null) gui.setItem(i, borderDecoration);
                        if (gui.getItem(45 + i) == null) gui.setItem(45 + i, borderDecoration);
                    }
                    
                    // Fill side borders (left and right columns)
                    for (int i = 1; i < 5; i++) {
                        if (gui.getItem(i * 9) == null) gui.setItem(i * 9, borderDecoration); // Left side
                        if (gui.getItem(i * 9 + 8) == null) gui.setItem(i * 9 + 8, borderDecoration); // Right side
                    }
                    
                    // Fill empty slots
                    for (int i = 9; i < 45; i++) {
                        if (gui.getItem(i) == null) {
                            gui.setItem(i, decoration);
                        }
                    }
                    
                    player.openInventory(gui);
                });
            });
        });
    }
    
    private static ItemStack createMenuItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        
        List<Component> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(Component.text(line).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(loreList);
        
        item.setItemMeta(meta);
        return item;
    }
    
    private static ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("❌ Back", net.kyori.adventure.text.format.NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Return to island menu", net.kyori.adventure.text.format.NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
