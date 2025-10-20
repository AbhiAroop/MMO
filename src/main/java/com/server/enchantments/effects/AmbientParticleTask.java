package com.server.enchantments.effects;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.server.enchantments.structure.EnchantmentTableStructure;

/**
 * Repeating task that spawns ambient particles around altars and anvils.
 */
public class AmbientParticleTask extends BukkitRunnable {
    
    private final Plugin plugin;
    private final EnchantmentTableStructure tableStructure;
    private int tickCounter = 0;
    
    public AmbientParticleTask(Plugin plugin, EnchantmentTableStructure tableStructure) {
        this.plugin = plugin;
        this.tableStructure = tableStructure;
    }
    
    @Override
    public void run() {
        tickCounter++;
        
        // Spawn particles around registered altars
        for (UUID altarUUID : tableStructure.getRegisteredAltars()) {
            // Try each loaded world
            for (World world : plugin.getServer().getWorlds()) {
                ArmorStand altar = tableStructure.getAltarByUUID(altarUUID, world);
                if (altar != null && !altar.isDead()) {
                    Location loc = altar.getLocation().clone().add(0, 0.5, 0);
                    // Altars get a mixed rainbow effect
                    ElementalParticles.spawnAmbientEffect(loc, null, tickCounter);
                }
            }
        }
        
        // Spawn particles around anvils (check all worlds)
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getType() == EntityType.ARMOR_STAND) {
                    ArmorStand stand = (ArmorStand) entity;
                    ItemStack helmet = stand.getEquipment().getHelmet();
                    
                    // Check if it's an anvil (has anvil helmet)
                    if (helmet != null && helmet.getType() == Material.ANVIL) {
                        Location loc = stand.getLocation().clone().add(0, 0.5, 0);
                        // Anvils get a mixed rainbow effect too
                        ElementalParticles.spawnAmbientEffect(loc, null, tickCounter);
                    }
                }
            }
        }
    }
}
