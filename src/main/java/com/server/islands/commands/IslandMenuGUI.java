package com.server.islands.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.server.islands.data.PlayerIsland;
import com.server.islands.managers.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Main island menu GUI - hub for all island commands
 */
public class IslandMenuGUI {
    
    private static final String GUI_TITLE = "🏝 Island Menu";
    
    public static void open(Player player, IslandManager islandManager) {
        Inventory gui = Bukkit.createInventory(null, 54, Component.text(GUI_TITLE));
        
        // Get player's island ID (works for both owners and members)
        islandManager.getPlayerIslandId(player.getUniqueId()).thenAccept(islandId -> {
            if (islandId == null) {
                return;
            }
            
            // Load the island by ID
            islandManager.loadIsland(islandId).thenAccept(island -> {
                if (island == null) {
                    return;
                }
                
                // Get player's role on this island
                islandManager.getMemberRole(islandId, player.getUniqueId()).thenAccept(role -> {
                    Bukkit.getScheduler().runTask(islandManager.getPlugin(), () -> {
                        populateGUI(gui, player, island, role, islandManager);
                        player.openInventory(gui);
                    });
                });
            });
        });
    }
    
    private static void populateGUI(Inventory gui, Player player, PlayerIsland island, 
                                    com.server.islands.data.IslandMember.IslandRole role, IslandManager islandManager) {
        // Island Home (Slot 10)
        gui.setItem(10, createMenuItem(
            Material.OAK_DOOR,
            "§b§lIsland Home",
            "§7Click to teleport to",
            "§7your island",
            "",
            "§eClick to teleport!"
        ));
        
        // Island Info (Slot 12)
        gui.setItem(12, createMenuItem(
            Material.PAPER,
            "§a§lIsland Information",
            "§7View detailed information",
            "§7about your island",
            "",
            "§7Type: §e" + island.getIslandType().getDisplayName(),
            "§7Size: §e" + island.getCurrentSize() + "x" + island.getCurrentSize(),
            "",
            "§eClick to view details!"
        ));
        
        // Island Upgrade (Slot 14)
        gui.setItem(14, createMenuItem(
            Material.EXPERIENCE_BOTTLE,
            "§6§lIsland Upgrades",
            "§7Upgrade your island's",
            "§7size, redstone, and more!",
            "",
            "§eClick to open upgrades!"
        ));
        
        // Island Members (Slot 16)
        gui.setItem(16, createMenuItem(
            Material.PLAYER_HEAD,
            "§d§lIsland Members",
            "§7Manage your island team",
            "§7View roles and permissions",
            "",
            "§eClick to manage members!"
        ));
        
        // Invite Player (Slot 19) - MOD+
        if (role != null && role.hasPermission(com.server.islands.data.IslandMember.IslandRole.MOD)) {
            gui.setItem(19, createMenuItem(
                Material.WRITABLE_BOOK,
                "§2§lInvite Player",
                "§7Invite someone to join",
                "§7your island",
                "",
                "§7Required: §aModerator",
                "",
                "§eClick to invite!"
            ));
        } else {
            gui.setItem(19, createMenuItem(
                Material.GRAY_DYE,
                "§8§lInvite Player",
                "§7Invite someone to join",
                "§7your island",
                "",
                "§cRequired: Moderator+",
                "",
                "§c✗ Insufficient permissions"
            ));
        }
        
        // Island Settings (Slot 21) - ADMIN+
        if (role != null && role.hasPermission(com.server.islands.data.IslandMember.IslandRole.ADMIN)) {
            gui.setItem(21, createMenuItem(
                Material.COMPARATOR,
                "§e§lIsland Settings",
                "§7Configure island options",
                "§7and permissions",
                "",
                "§7Required: §6Admin",
                "",
                "§eClick to configure!"
            ));
        } else {
            gui.setItem(21, createMenuItem(
                Material.GRAY_DYE,
                "§8§lIsland Settings",
                "§7Configure island options",
                "§7and permissions",
                "",
                "§cRequired: Admin+",
                "",
                "§c✗ Insufficient permissions"
            ));
        }
        
        // Island Shop (Slot 22)
        gui.setItem(22, createMenuItem(
            Material.EMERALD,
            "§b§lIsland Shop",
            "§7Purchase special items",
            "§7using Island Tokens!",
            "",
            "§aBalance: §e" + island.getIslandTokens() + " Tokens",
            "",
            "§eClick to browse!"
        ));
        
        // Visit Island (Slot 23)
        gui.setItem(23, createMenuItem(
            Material.ENDER_PEARL,
            "§5§lVisit Island",
            "§7Visit another player's",
            "§7island",
            "",
            "§eClick to visit!"
        ));
        
        // Leave Island (Slot 25) - Non-owners only
        if (role != com.server.islands.data.IslandMember.IslandRole.OWNER) {
            gui.setItem(25, createMenuItem(
                Material.BARRIER,
                "§c§lLeave Island",
                "§7Leave your current island",
                "",
                "§cThis cannot be undone!",
                "",
                "§eClick to leave!"
            ));
        }
        
        // Manage Members (Slot 28) - ADMIN+
        if (role != null && role.hasPermission(com.server.islands.data.IslandMember.IslandRole.ADMIN)) {
            gui.setItem(28, createMenuItem(
                Material.NAME_TAG,
                "§3§lManage Roles",
                "§7Promote, demote, or kick",
                "§7island members",
                "",
                "§7Required: §6Admin",
                "",
                "§eClick to manage!"
            ));
        } else {
            gui.setItem(28, createMenuItem(
                Material.GRAY_DYE,
                "§8§lManage Roles",
                "§7Promote, demote, or kick",
                "§7island members",
                "",
                "§cRequired: Admin+",
                "",
                "§c✗ Insufficient permissions"
            ));
        }
        
        // Transfer Ownership (Slot 30) - OWNER only
        if (role == com.server.islands.data.IslandMember.IslandRole.OWNER) {
            gui.setItem(30, createMenuItem(
                Material.GOLDEN_APPLE,
                "§6§lTransfer Ownership",
                "§7Transfer island ownership",
                "§7to another member",
                "",
                "§7You will become Co-Owner",
                "",
                "§eClick to transfer!"
            ));
        }
        
        // Delete Island (Slot 32) - OWNER only
        if (role == com.server.islands.data.IslandMember.IslandRole.OWNER) {
            gui.setItem(32, createMenuItem(
                Material.TNT,
                "§4§lDelete Island",
                "§7Permanently delete your island",
                "",
                "§c⚠ THIS CANNOT BE UNDONE!",
                "§cAll members will be removed!",
                "",
                "§eClick to delete!"
            ));
        }
        
        // Island Challenges (Slot 34)
        gui.setItem(34, createMenuItem(
            Material.WRITABLE_BOOK,
            "§6§lIsland Challenges",
            "§7Complete challenges to earn",
            "§bIsland Tokens §7for upgrades!",
            "",
            "§710 challenge categories",
            "§7100s of challenges to complete",
            "",
            "§eClick to view challenges!"
        ));
        
        // Help/Info (Slot 49)
        gui.setItem(49, createMenuItem(
            Material.BOOK,
            "§f§lIsland Help",
            "§7View all island commands",
            "§7and their usage",
            "",
            "§eClick for help!"
        ));
        
        // Your Role (Slot 4)
        if (role != null) {
            gui.setItem(4, createMenuItem(
                Material.DIAMOND,
                "§b§lYour Role",
                "§7Current Role: " + getRoleColor(role) + role.getDisplayName(),
                "",
                "§7Permissions:",
                getRolePermissions(role)
            ));
        }
        
        // Decoration
        ItemStack decoration = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta decorMeta = decoration.getItemMeta();
        decorMeta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        decoration.setItemMeta(decorMeta);
        
        ItemStack borderDecoration = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderDecoration.getItemMeta();
        borderMeta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        borderDecoration.setItemMeta(borderMeta);
        
        // Fill border with cyan glass panes (top and bottom rows)
        for (int i = 0; i < 9; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, borderDecoration);
            if (gui.getItem(45 + i) == null) gui.setItem(45 + i, borderDecoration);
        }
        
        // Fill side borders (left and right columns)
        for (int i = 1; i < 5; i++) {
            if (gui.getItem(i * 9) == null) gui.setItem(i * 9, borderDecoration); // Left side
            if (gui.getItem(i * 9 + 8) == null) gui.setItem(i * 9 + 8, borderDecoration); // Right side
        }
        
        // Fill empty slots with black decoration
        for (int i = 9; i < 45; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, decoration);
            }
        }
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
    
    private static String getRoleColor(com.server.islands.data.IslandMember.IslandRole role) {
        switch (role) {
            case OWNER: return "§6";
            case CO_OWNER: return "§e";
            case ADMIN: return "§c";
            case MOD: return "§9";
            case MEMBER: return "§a";
            default: return "§7";
        }
    }
    
    private static String getRolePermissions(com.server.islands.data.IslandMember.IslandRole role) {
        switch (role) {
            case OWNER:
                return "§a✓ §7Full access to everything";
            case CO_OWNER:
                return "§a✓ §7Manage members, settings\n§a✓ §7Invite players\n§a✓ §7Can leave island";
            case ADMIN:
                return "§a✓ §7Manage members\n§a✓ §7Invite players\n§a✓ §7Kick lower ranks";
            case MOD:
                return "§a✓ §7Invite players\n§a✓ §7Build on island";
            case MEMBER:
                return "§a✓ §7Build on island\n§a✓ §7Use facilities";
            default:
                return "§7No permissions";
        }
    }
}
