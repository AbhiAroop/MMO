package com.server.entities.npc.types;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import com.server.entities.npc.CombatHandler;
import com.server.entities.npc.NPCManager;
import com.server.entities.npc.NPCStats;
import com.server.entities.npc.NPCType;
import com.server.entities.npc.behaviors.CombatBehavior;
import com.server.entities.npc.behaviors.EquipmentBehavior;
import com.server.entities.npc.behaviors.NPCBehavior;

import net.citizensnpcs.api.npc.NPC;

/**
 * A neutral NPC that will fight back when attacked
 */
public class CombatNPC extends BaseNPC {
    
    private final CombatHandler combatHandler;
    private String dialogueId;
    private boolean isHostile = false;
    protected final Map<String, NPCBehavior> behaviors = new HashMap<>();
    
    /**
     * Create a new combat NPC
     * 
     * @param id The unique ID
     * @param name The display name
     * @param stats The NPC stats
     */
    public CombatNPC(String id, String name, NPCStats stats) {
        super(id, name, stats);
        
        // Combat NPCs use NORMAL type by default
        stats.setNpcType(NPCType.NORMAL);
        
        // Create combat handler
        this.combatHandler = NPCManager.getInstance().getCombatHandler();
        this.dialogueId = "combat_" + id;
        
        // Setup default dialogue
        setupDefaultDialogue(dialogueId, "Hello traveler. I'm " + name + ". Need something?");
    }
    
    @Override
    public NPC spawn(Location location, String skin) {
        // Create the NPC using NPCManager instead of directly creating it
        this.npc = NPCManager.getInstance().createNPC(id, name, location, skin, true);
        
        // Apply metadata
        applyBaseMetadata();
        
        // Additional combat-specific metadata
        if (npc.isSpawned()) {
            npc.getEntity().setInvulnerable(false);
            npc.getEntity().setMetadata("combat_npc", new FixedMetadataValue(plugin, true));
            npc.getEntity().setMetadata("targets_players", new FixedMetadataValue(plugin, true));
            npc.getEntity().setMetadata("targets_npcs", new FixedMetadataValue(plugin, false));
        }
        
        // Register this instance as the interaction handler for this NPC
        NPCManager.getInstance().registerInteractionHandler(id, this);
        
        // Setup combat handler with this NPC's stats
        combatHandler.setNPCStats(npc.getUniqueId(), stats);
        
        // Initialize behaviors (only call this once!)
        initializeBehaviors();
        
        // Create custom nameplate
        NPCManager.getInstance().createHologramNameplate(npc, name, stats.getMaxHealth(), stats.getMaxHealth());
        
        return npc;
    }
    
    /**
     * Initialize behavior classes for this NPC
     */
    protected void initializeBehaviors() {
        // Create and add combat behavior
        CombatBehavior combatBehavior = new CombatBehavior();
        combatBehavior.initialize(npc);
        behaviors.put("combat", combatBehavior);
        
        // Create and add equipment behavior
        EquipmentBehavior equipmentBehavior = new EquipmentBehavior();
        equipmentBehavior.initialize(npc);
        behaviors.put("equipment", equipmentBehavior);
        
        // Update stats from equipment immediately upon initialization
        updateStatsFromEquipment();
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Initialized behaviors for NPC " + name + ": " + 
                                behaviors.keySet().toString());
        }
    }

    /**
     * Update NPC stats from equipped items
     */
    public void updateStatsFromEquipment() {
        EquipmentBehavior equipBehavior = (EquipmentBehavior) behaviors.get("equipment");
        if (equipBehavior == null) return;
        
        // IMPORTANT FIX: Store the original base values before resetting stats
        NPCStats baseStats = new NPCStats();
        // Copy only the essential base values from our current stats that we want to preserve
        baseStats.setMaxHealth(stats.getMaxHealth());  // Preserve custom max health
        baseStats.setPhysicalDamage(stats.getPhysicalDamage()); // Preserve custom physical damage
        baseStats.setAttackSpeed(stats.getAttackSpeed());   // Preserve attack speed
        baseStats.setAttackRange(stats.getAttackRange());   // Preserve attack range
        baseStats.setNpcType(stats.getNpcType()); // Preserve NPC type
        baseStats.setLevel(stats.getLevel());     // Preserve level
        baseStats.setMagicDamage(stats.getMagicDamage());  // Preserve magic damage
        baseStats.setArmor(stats.getArmor());     // Preserve armor
        baseStats.setMagicResist(stats.getMagicResist()); // Preserve magic resist
        
        // Process each equipped item and apply stats
        Map<net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot, org.bukkit.inventory.ItemStack> equippedItems = 
            equipBehavior.getEquippedItems();
        
        // Log the number of equipped items found for debugging
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Found " + equippedItems.size() + " equipped items for NPC " + name);
        }
        
        // Reset stats to base values - CRITICAL FIX: Use the preserved values instead of defaults
        stats.setMaxHealth(baseStats.getMaxHealth());
        stats.setPhysicalDamage(baseStats.getPhysicalDamage());
        stats.setAttackSpeed(baseStats.getAttackSpeed());
        stats.setAttackRange(baseStats.getAttackRange());
        stats.setNpcType(baseStats.getNpcType());
        stats.setLevel(baseStats.getLevel());
        stats.setMagicDamage(baseStats.getMagicDamage());
        stats.setArmor(baseStats.getArmor());
        stats.setMagicResist(baseStats.getMagicResist());
        
        // Now continue with the existing method to apply equipment bonuses
        if (equippedItems.isEmpty()) {
            // Early exit if there are no equipped items - but make sure we update combat handler
            if (npc != null && npc.isSpawned()) {
                // Update metadata to reflect base stats
                npc.getEntity().setMetadata("max_health", new FixedMetadataValue(plugin, stats.getMaxHealth()));
                npc.getEntity().setMetadata("physical_damage", new FixedMetadataValue(plugin, stats.getPhysicalDamage()));
                npc.getEntity().setMetadata("magic_damage", new FixedMetadataValue(plugin, stats.getMagicDamage()));
                npc.getEntity().setMetadata("armor", new FixedMetadataValue(plugin, stats.getArmor()));
                npc.getEntity().setMetadata("magic_resist", new FixedMetadataValue(plugin, stats.getMagicResist()));
                
                // Update the combat handler with preserved stats
                combatHandler.setNPCStats(npc.getUniqueId(), stats);
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("No equipped items, using base stats for NPC " + name + 
                        ": Health=" + stats.getMaxHealth() + 
                        ", PhysDmg=" + stats.getPhysicalDamage());
                }
            }
            return;
        }
        
        // Process each equipped item
        for (Map.Entry<net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot, org.bukkit.inventory.ItemStack> entry : equippedItems.entrySet()) {
            org.bukkit.inventory.ItemStack item = entry.getValue();
            net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot slot = entry.getKey();
            
            if (item != null) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Processing item in slot " + slot.name() + ": " + 
                        (item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? 
                        item.getItemMeta().getDisplayName() : item.getType().name()));
                }
                
                // Check for custom model data first (hardcoded stats for specific items)
                if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
                    int modelData = item.getItemMeta().getCustomModelData();
                    
                    // Log model data for debugging
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Processing item with model data: " + modelData);
                    }
                }
                
                // Also process lore if available (for items without specific model data handling)
                if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                    List<String> lore = item.getItemMeta().getLore();
                    
                    // Process lore
                    for (String line : lore) {
                        // Strip color codes
                        String cleanLine = line.replaceAll("§[0-9a-fA-Fk-oK-OrR]", "").trim();
                        
                        // Log for debugging
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("  Processing lore line: " + cleanLine);
                        }
                        
                        // Extract stats from lore
                        try {
                            if (cleanLine.startsWith("Physical Damage: +")) {
                                String valueStr = cleanLine.substring("Physical Damage: +".length()).trim();
                                // Extract just the numeric part using regex
                                String numericValue = valueStr.replaceAll("[^0-9]", "");
                                int amount = Integer.parseInt(numericValue);
                                stats.setPhysicalDamage(stats.getPhysicalDamage() + amount);
                                if (plugin.isDebugMode()) {
                                    plugin.getLogger().info("    Added Physical Damage: +" + amount);
                                }
                            }
                            else if (cleanLine.startsWith("Health: +")) {
                                String valueStr = cleanLine.substring("Health: +".length()).trim();
                                // Extract just the numeric part using regex
                                String numericValue = valueStr.replaceAll("[^0-9]", "");
                                int amount = Integer.parseInt(numericValue);
                                stats.setMaxHealth(stats.getMaxHealth() + amount);
                                if (plugin.isDebugMode()) {
                                    plugin.getLogger().info("    Added Health: +" + amount);
                                }
                            }
                            else if (cleanLine.startsWith("Armor: +")) {
                                int amount = Integer.parseInt(cleanLine.substring("Armor: +".length()).trim());
                                stats.setArmor(stats.getArmor() + amount);
                                if (plugin.isDebugMode()) {
                                    plugin.getLogger().info("    Added Armor: +" + amount);
                                }
                            }
                            else if (cleanLine.startsWith("Magic Damage: +")) {
                                int amount = Integer.parseInt(cleanLine.substring("Magic Damage: +".length()).trim());
                                stats.setMagicDamage(stats.getMagicDamage() + amount);
                                if (plugin.isDebugMode()) {
                                    plugin.getLogger().info("    Added Magic Damage: +" + amount);
                                }
                            }
                            else if (cleanLine.startsWith("Magic Resist: +")) {
                                int amount = Integer.parseInt(cleanLine.substring("Magic Resist: +".length()).trim());
                                stats.setMagicResist(stats.getMagicResist() + amount);
                                if (plugin.isDebugMode()) {
                                    plugin.getLogger().info("    Added Magic Resist: +" + amount);
                                }
                            }
                            else if (cleanLine.startsWith("Attack Speed: +")) {
                                double amount = Double.parseDouble(cleanLine.substring("Attack Speed: +".length()).trim());
                                stats.setAttackSpeed(stats.getAttackSpeed() + amount);
                                if (plugin.isDebugMode()) {
                                    plugin.getLogger().info("    Added Attack Speed: +" + amount);
                                }
                            }
                            else if (cleanLine.startsWith("Attack Range: +")) {
                                double amount = Double.parseDouble(cleanLine.substring("Attack Range: +".length()).trim());
                                stats.setAttackRange(stats.getAttackRange() + amount);
                                if (plugin.isDebugMode()) {
                                    plugin.getLogger().info("    Added Attack Range: +" + amount);
                                }
                            }
                        } catch (NumberFormatException e) {
                            if (plugin.isDebugMode()) {
                                plugin.getLogger().warning("    Error parsing stat value from lore: " + cleanLine);
                            }
                        }
                    }
                }
            }
        }
        
        // Update the combat handler with the new stats
        if (npc != null && npc.isSpawned()) {
            // Debug log comparing old vs new stats
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Stats after equipment: " + 
                    "Health=" + stats.getMaxHealth() + 
                    ", PhysDmg=" + stats.getPhysicalDamage() + 
                    ", MagicDmg=" + stats.getMagicDamage() + 
                    ", Armor=" + stats.getArmor() +
                    ", MR=" + stats.getMagicResist() + 
                    ", AtkSpd=" + stats.getAttackSpeed() +
                    ", AtkRange=" + stats.getAttackRange());
            }
            
            // Update metadata to reflect new stats
            npc.getEntity().setMetadata("max_health", new FixedMetadataValue(plugin, stats.getMaxHealth()));
            npc.getEntity().setMetadata("physical_damage", new FixedMetadataValue(plugin, stats.getPhysicalDamage()));
            npc.getEntity().setMetadata("magic_damage", new FixedMetadataValue(plugin, stats.getMagicDamage()));
            npc.getEntity().setMetadata("armor", new FixedMetadataValue(plugin, stats.getArmor()));
            npc.getEntity().setMetadata("magic_resist", new FixedMetadataValue(plugin, stats.getMagicResist()));
            npc.getEntity().setMetadata("attack_speed", new FixedMetadataValue(plugin, stats.getAttackSpeed()));
            npc.getEntity().setMetadata("attack_range", new FixedMetadataValue(plugin, stats.getAttackRange()));
            
            // Update the combat handler
            combatHandler.setNPCStats(npc.getUniqueId(), stats);
            
            // IMPORTANT: Get current health from metadata before updating it
            double currentHealth = npc.getEntity().hasMetadata("current_health") ?
                npc.getEntity().getMetadata("current_health").get(0).asDouble() : baseStats.getMaxHealth();
            
            // CRITICAL FIX: Get the old max health from metadata instead of using a field
            double oldMaxHealth = npc.getEntity().hasMetadata("old_max_health") ? 
                npc.getEntity().getMetadata("old_max_health").get(0).asDouble() : baseStats.getMaxHealth();
            
            // When max health increases, also increase current health by the difference
            if (stats.getMaxHealth() > oldMaxHealth) {
                // If max health increased, increase current health by the difference
                double healthIncrease = stats.getMaxHealth() - oldMaxHealth;
                currentHealth += healthIncrease;
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("Max health increased by " + healthIncrease + 
                        ", so current health also increased from " + (currentHealth - healthIncrease) + 
                        " to " + currentHealth);
                }
            }
            
            // Store the new max health for future comparisons
            npc.getEntity().setMetadata("old_max_health", new FixedMetadataValue(plugin, stats.getMaxHealth()));
            
            // Update current health metadata
            npc.getEntity().setMetadata("current_health", new FixedMetadataValue(plugin, currentHealth));
            
            // CRITICAL FIX: Make sure to update the combat handler's health tracking too
            combatHandler.setHealth(npc.getUniqueId(), currentHealth);

            // Update the nameplate
            NPCManager.getInstance().updateNameplate(npc, currentHealth, stats.getMaxHealth());
            
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Updated CombatHandler stats for NPC " + name + 
                    " - Physical damage: " + stats.getPhysicalDamage() +
                    ", Health: " + stats.getMaxHealth() + 
                    ", Armor: " + stats.getArmor() +
                    ", Attack Range: " + stats.getAttackRange() +
                    ", Attack Speed: " + stats.getAttackSpeed());
            }
        }

        // CRITICAL FIX: Remove this recursive call that was causing the double application
        // equipBehavior.applyItemStatsToNPC(stats, item, slot);
    }
    
    @Override
    public void onInteract(Player player, NPC npc, boolean rightClick) {
        // Check if we're in combat first
        CombatBehavior combatBehavior = (CombatBehavior) behaviors.get("combat");
        boolean inCombat = combatBehavior != null && combatBehavior.isInCombat();
        
        if (rightClick && !inCombat) {
            // Start dialogue if not in combat
            startDialogue(player, dialogueId);
        } else if (!rightClick && isHostile) {
            // Left click by player while NPC is hostile - initiate combat
            if (combatBehavior != null) {
                combatBehavior.onInteract(player, false);
            }
        }
    }
    
    /**
     * Set this NPC as hostile (will attack on sight)
     * 
     * @param isHostile True to make hostile
     */
    public void setHostile(boolean isHostile) {
        this.isHostile = isHostile;
        
        if (npc != null && npc.isSpawned()) {
            npc.getEntity().setMetadata("hostile", new FixedMetadataValue(plugin, isHostile));
            
            CombatBehavior combatBehavior = (CombatBehavior) behaviors.get("combat");
            if (combatBehavior != null) {
                // Update combat behavior settings
                combatBehavior.setTargetsPlayers(isHostile);
                
                // If turning hostile and not in combat, look for targets
                if (isHostile && !combatBehavior.isInCombat()) {
                    // Find nearest player as potential target
                    Player nearestPlayer = findNearestPlayer();
                    
                    if (nearestPlayer != null) {
                        combatBehavior.startCombat(nearestPlayer);
                        sendMessage(nearestPlayer, "You look like trouble. Prepare yourself!");
                    }
                }
            } else {
                // Fallback to direct combat handler if behavior doesn't exist
                combatHandler.setTargetsPlayers(isHostile);
                
                if (isHostile && !combatHandler.isInCombat(npc.getUniqueId())) {
                    Player nearestPlayer = findNearestPlayer();
                    if (nearestPlayer != null) {
                        combatHandler.startCombatBehavior(npc, nearestPlayer);
                        sendMessage(nearestPlayer, "You look like trouble. Prepare yourself!");
                    }
                }
            }
        }
    }

    /**
     * Find the nearest player to this NPC
     * 
     * @return The nearest player, or null if none is in range
     */
    private Player findNearestPlayer() {
        if (npc == null || !npc.isSpawned()) return null;
        
        Player nearestPlayer = null;
        double nearestDistance = 16.0; // Max targeting range
        
        for (Player player : npc.getEntity().getWorld().getPlayers()) {
            // Skip players in creative/spectator or vanished
            if (player.getGameMode() == org.bukkit.GameMode.CREATIVE || 
                player.getGameMode() == org.bukkit.GameMode.SPECTATOR ||
                player.hasMetadata("vanished")) {
                continue;
            }
            
            double distance = player.getLocation().distance(npc.getEntity().getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestPlayer = player;
            }
        }
        
        return nearestPlayer;
    }

    /**
     * Called when this NPC is damaged
     * 
     * @param player The attacking player
     * @param damage The damage amount
     */
    public void onDamage(Player player, double damage) {
        // Skip if NPC is not spawned
        if (!npc.isSpawned()) return;
        
        // Get the combat behavior if available
        CombatBehavior behavior = (CombatBehavior) getBehavior("combat");
        if (behavior != null) {
            behavior.onDamage(player, damage);
            return;
        }
        
        // If no behavior or this is called directly, handle damage here
        // Get current health value from CombatHandler
        CombatHandler combatHandler = (CombatHandler) NPCManager.getInstance().getInteractionHandler(id);
        if (combatHandler == null) return;
        
        // Get current and max health
        double currentHealth = 0;
        double maxHealth = 0;
        
        if (npc != null && npc.isSpawned()) {
            // Get current health directly from the combat handler
            currentHealth = combatHandler.getHealth(npc.getUniqueId());
            
            // Get max health from stats
            NPCStats stats = combatHandler.getNPCStats(npc.getUniqueId());
            maxHealth = stats.getMaxHealth();
            
            // CRITICAL: Update nameplate with current health
            NPCManager.getInstance().updateNameplate(npc, currentHealth, maxHealth);
            
            // Debug health values
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("CombatNPC damaged: " + name + " health = " + currentHealth + "/" + maxHealth);
            }
        }
    }
    
    /**
     * Set whether this NPC targets players
     * 
     * @param targetsPlayers True to target players
     */
    public void setTargetsPlayers(boolean targetsPlayers) {
        CombatBehavior behavior = (CombatBehavior) behaviors.get("combat");
        if (behavior != null) {
            behavior.setTargetsPlayers(targetsPlayers);
        } else {
            combatHandler.setTargetsPlayers(targetsPlayers);
        }
        
        if (npc != null && npc.isSpawned()) {
            npc.getEntity().setMetadata("targets_players", new FixedMetadataValue(plugin, targetsPlayers));
        }
    }
    
    /**
     * Set whether this NPC targets other NPCs
     * 
     * @param targetsNPCs True to target other NPCs
     */
    public void setTargetsNPCs(boolean targetsNPCs) {
        CombatBehavior behavior = (CombatBehavior) behaviors.get("combat");
        if (behavior != null) {
            behavior.setTargetsNPCs(targetsNPCs);
        } else {
            combatHandler.setTargetsNPCs(targetsNPCs);
        }
        
        if (npc != null && npc.isSpawned()) {
            npc.getEntity().setMetadata("targets_npcs", new FixedMetadataValue(plugin, targetsNPCs));
        }
    }
    
    /**
     * Set the dialogue for this NPC
     * 
     * @param dialogueId The dialogue ID
     */
    public void setDialogue(String dialogueId) {
        this.dialogueId = dialogueId;
        setupDefaultDialogue(dialogueId, "Hello traveler. I'm " + name + ". Need something?");
    }
    
    /**
     * Get the combat handler for this NPC
     * 
     * @return The combat handler
     */
    public CombatHandler getCombatHandler() {
        return combatHandler;
    }
        
    /**
     * Check if this NPC is hostile
     * 
     * @return True if hostile
     */
    public boolean isHostile() {
        return isHostile;
    }
    
    /**
     * Get a behavior by name
     * 
     * @param name The behavior name
     * @return The behavior, or null if not found
     */
    public NPCBehavior getBehavior(String name) {
        return behaviors.get(name);
    }

    /**
     * Called when this NPC attacks another NPC
     * 
     * @param targetNPC The target NPC
     */
    public void onAttackNPC(NPC targetNPC) {
        if (npc == null || !npc.isSpawned() || targetNPC == null) return;
        
        // Make sure our stats reflect any equipment bonuses
        updateStatsFromEquipment();
        
        // Calculate critical hit chance based on NPC type
        double critChance = 0.1; // Default 10% chance
        if (stats.getNpcType() == NPCType.ELITE) critChance = 0.15;
        else if (stats.getNpcType() == NPCType.MINIBOSS) critChance = 0.2;
        else if (stats.getNpcType() == NPCType.BOSS) critChance = 0.25;
        
        boolean isCritical = Math.random() < critChance;
        
        // Apply damage using combat handler - using current stats that include equipment bonuses
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("NPC " + name + " attacking NPC " + targetNPC.getName() + 
                " with damage: " + stats.getPhysicalDamage() + ", Critical: " + isCritical);
        }
        
        // First ensure we have the targeting settings enabled
        setTargetsNPCs(true);
        
        // Make sure our NPC has the correct metadata for targeting NPCs
        if (npc != null && npc.isSpawned()) {
            npc.getEntity().setMetadata("targets_npcs", new FixedMetadataValue(plugin, true));
        }
        
        // Apply damage directly to target NPC
        combatHandler.applyDamageToNPC(npc, targetNPC, stats.getPhysicalDamage(), isCritical);
        
        // Update hostility status
        setHostile(true);
    }

    /**
     * Called when another NPC damages this NPC
     * 
     * @param attackerNPC The attacking NPC
     * @param damage The damage amount
     * @param isCritical Whether the hit is a critical hit
     */
    public void onNPCDamage(NPC attackerNPC, double damage, boolean isCritical) {
        if (npc == null || !npc.isSpawned()) return;
        
        // Get current health from metadata or default to max health
        double currentHealth;
        if (npc.getEntity().hasMetadata("current_health")) {
            currentHealth = npc.getEntity().getMetadata("current_health").get(0).asDouble();
            plugin.getLogger().info("🟢 HEALTH CHECK: " + name + " current health from metadata: " + currentHealth);
        } else {
            currentHealth = stats.getMaxHealth();
            plugin.getLogger().info("🟢 HEALTH CHECK: " + name + " using default max health: " + currentHealth);
        }
        
        // Calculate damage reduction from armor
        double armorReduction = stats.getArmor() > 0 ? stats.getArmor() / (stats.getArmor() + 100.0) : 0.0;
        double finalDamage = damage * (1.0 - armorReduction);
        
        // Apply critical damage multiplier if applicable
        if (isCritical) {
            finalDamage *= 1.5; // 50% more damage for critical hits
        }
        
        // Apply minimum damage
        finalDamage = Math.max(1.0, finalDamage);
        
        // Calculate new health after damage
        double newHealth = Math.max(0, currentHealth - finalDamage);
        
        // Debug info with safe attacker name
        String attackerName = attackerNPC != null ? attackerNPC.getName() : "Player/Environment";
        plugin.getLogger().info("🟢 DAMAGE PROCESSING: " + attackerName + " damaged NPC " + name + 
            " for " + finalDamage + " damage (raw damage: " + damage + 
            ", armor reduction: " + (armorReduction * 100) + "%)");
        plugin.getLogger().info("🟢 HEALTH UPDATE: NPC " + name + " health: " + currentHealth + " → " + newHealth);
        
        // CRITICAL FIX: Update health values in both systems
        npc.getEntity().setMetadata("current_health", new FixedMetadataValue(plugin, newHealth));
        combatHandler.setHealth(npc.getUniqueId(), newHealth);
        
        // Update nameplate to show new health value
        NPCManager.getInstance().updateNameplate(npc, newHealth, stats.getMaxHealth());
        
        // Check if the entity's nameplate reflects the health change
        plugin.getLogger().info("🟢 NAMEPLATE UPDATE: " + name + " nameplate updated to show " + 
            newHealth + "/" + stats.getMaxHealth() + " health");
        
        // IMPROVED: Chance to immediately counter-attack while being attacked
        if (Math.random() < 0.6) { // 60% chance to counter
            if (attackerNPC != null) {
                plugin.getLogger().info("🟢 COUNTER-ATTACK: " + name + " attempting counter-attack against " + attackerNPC.getName());
                combatHandler.triggerCounterAttack(npc, attackerNPC.getEntity());
            }
        }
        
        // Only set hostile and target the attacking NPC if there is one
        if (attackerNPC != null) {
            // Set hostile and target the attacking NPC
            setHostile(true);
            
            // Use combat behavior if available to retaliate
            CombatBehavior combatBehavior = (CombatBehavior) behaviors.get("combat");
            if (combatBehavior != null) {
                plugin.getLogger().info("🟢 RETALIATION: " + name + " retaliating against " + attackerNPC.getName());
                combatBehavior.startCombat(attackerNPC.getEntity());
            }
        }
        
        // Show damage indicators
        if (plugin.getDamageIndicatorManager() != null) {
            plugin.getDamageIndicatorManager().spawnDamageIndicator(
                npc.getEntity().getLocation().add(0, 1, 0),
                (int)Math.round(finalDamage), 
                isCritical);
        }
        
        // Check if NPC died
        if (newHealth <= 0) {
            // Handle NPC death
            plugin.getLogger().info("🟢 NPC DEATH: " + name + " was killed by " + attackerName);
            
            // Use the standard death handling from combat handler
            Entity killerEntity = attackerNPC != null ? attackerNPC.getEntity() : null;
            combatHandler.handleNPCDeath(npc, killerEntity);
        }
    }

    /**
     * Called when this NPC is damaged by magic
     * 
     * @param player The attacking player
     * @param damage The damage amount
     */
    public void onMagicDamage(Player player, double damage) {
        if (npc == null || !npc.isSpawned()) return;
        
        // CRITICAL FIX: Make sure the combat handler has the correct max health value before damage is applied
        double currentHealth = npc.getEntity().hasMetadata("current_health") ?
            npc.getEntity().getMetadata("current_health").get(0).asDouble() : stats.getMaxHealth();
        
        // Ensure the combat handler has the correct current health value
        combatHandler.setHealth(npc.getUniqueId(), currentHealth);
        
        // Calculate damage reduction from magic resist
        double magicResistReduction = stats.getMagicResist() > 0 ? 
            stats.getMagicResist() / (stats.getMagicResist() + 100.0) : 0.0;
        double finalDamage = damage * (1.0 - magicResistReduction);
        
        // Apply minimum damage
        finalDamage = Math.max(1.0, finalDamage);
        
        // Debug logging
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("💫 MAGIC DAMAGE: " + player.getName() + " -> " + name + 
                ", Raw: " + damage + ", After MR(" + stats.getMagicResist() + 
                "): " + finalDamage + ", Reduction: " + (magicResistReduction * 100) + "%");
        }
        
        // Apply visual damage indicators with magic color
        if (plugin.getDamageIndicatorManager() != null) {
            plugin.getDamageIndicatorManager().spawnDamageIndicator(
                npc.getEntity().getLocation().add(0, 1, 0),
                (int) finalDamage,
                true); // Use true to indicate magic damage
        }
        
        // Magic damage visual effects
        npc.getEntity().getWorld().spawnParticle(
            org.bukkit.Particle.WITCH,
            npc.getEntity().getLocation().add(0, 1, 0),
            15, 0.3, 0.5, 0.3, 0.05
        );
        
        // Play magic damage sound
        npc.getEntity().getWorld().playSound(
            npc.getEntity().getLocation(),
            Sound.ENTITY_PLAYER_HURT_ON_FIRE,
            0.8f, 1.2f
        );
        
        // IMPROVED: Chance to immediately counter-attack while being attacked by magic
        if (Math.random() < 0.4) { // 40% chance to counter magical attacks
            plugin.getLogger().info("🟢 MAGIC COUNTER-ATTACK: " + name + " attempting counter-attack against " + player.getName());
            combatHandler.triggerCounterAttack(npc, player);
        }
        
        // Update the NPC's health directly
        double newHealth = Math.max(0, currentHealth - finalDamage);
        npc.getEntity().setMetadata("current_health", new FixedMetadataValue(plugin, newHealth));
        combatHandler.setHealth(npc.getUniqueId(), newHealth);
        
        // Update nameplate with new health value
        NPCManager.getInstance().updateNameplate(npc, newHealth, stats.getMaxHealth());
        
        // Use combat behavior if available
        CombatBehavior combatBehavior = (CombatBehavior) behaviors.get("combat");
        if (combatBehavior != null) {
            // Start combat with the player who attacked
            combatBehavior.startCombat(player);
            
            if (combatBehavior.isInCombat()) {
                // Send threatening message occasionally
                if (Math.random() < 0.2) {
                    String[] magicCombatMessages = {
                        "Your magic is weak against me!",
                        "Magic? Is that all you've got?",
                        "Your spells won't save you!",
                        "I've faced stronger mages than you!",
                        "Magic tricks won't stop me!"
                    };
                    sendMessage(player, magicCombatMessages[(int)(Math.random() * magicCombatMessages.length)]);
                }
            } else {
                // Not in combat yet, start combat and send initial message
                sendMessage(player, "Your magic has awakened my wrath!");
            }
        }
        
        // Check if NPC died
        if (newHealth <= 0) {
            // Let the combat handler handle this NPC's death
            combatHandler.handleNPCDeath(npc, player);
        }
    }
    
}