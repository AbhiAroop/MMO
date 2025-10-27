package com.server.profiles.skills.skills.fishing;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import com.server.profiles.skills.core.AbstractSkill;
import com.server.profiles.skills.core.SkillType;
import com.server.profiles.skills.skills.fishing.subskills.RodFishingSubskill;

public class FishingSkill extends AbstractSkill {

    public FishingSkill() {
        super(SkillType.FISHING.getId(), 
              SkillType.FISHING.getDisplayName(), 
              SkillType.FISHING.getDescription(),
              50);
        
        this.subskills.add(new RodFishingSubskill(this));
    }

    public ItemStack getDisplayItem() {
        return new ItemStack(Material.FISHING_ROD);
    }
}
