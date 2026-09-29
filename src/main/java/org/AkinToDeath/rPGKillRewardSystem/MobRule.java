package org.AkinToDeath.rPGKillRewardSystem;

import org.bukkit.entity.EntityType;

import java.util.List;

/**
 * One entry under "mobs:" in config.yml: "when this kind of mob dies, roll these rewards".
 *
 * @param id                the name of the entry in the config (e.g. "goblin_king")
 * @param type              the kind of mob it applies to (e.g. ZOMBIE)
 * @param nameFilter        nullable, lower-cased text the mob's custom name must contain
 * @param clearVanillaDrops if true, the mob's normal Minecraft loot and XP are removed
 * @param rewards           the rewards to roll
 */
public record MobRule(String id, EntityType type, String nameFilter, boolean clearVanillaDrops,
                      List<Reward> rewards) {

    /**
     * Does a mob with this custom name count for this rule?
     *
     * @param plainCustomName the mob's name with formatting removed, or null if it has no name
     */
    public boolean matches(String plainCustomName) {
        if (nameFilter == null) {
            return true; // no filter, applies to every mob of this type
        }
        // Filter set: the mob must have a name, and it must contain the filter text (ignoring case).
        return plainCustomName != null && plainCustomName.toLowerCase().contains(nameFilter);
    }
}
