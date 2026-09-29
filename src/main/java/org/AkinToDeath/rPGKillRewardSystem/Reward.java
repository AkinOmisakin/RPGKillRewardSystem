package org.AkinToDeath.rPGKillRewardSystem;

import java.util.List;

/**
 * One reward that can drop when a mob is killed, e.g. one entry under "rewards:" in config.yml.
 * A single reward can give an item, run commands, give XP and send a message all at once.
 *
 * @param id       the name of the entry in the config (only used for readable warnings)
 * @param chance   percent chance, 0-100
 * @param item     the item to give, or null for none
 * @param commands console commands to run (may be empty)
 * @param exp      XP points to give, {@link IntRange#ZERO} for none
 * @param message  MiniMessage text sent to the killer, or null for none
 */
public record Reward(String id, double chance, ItemTemplate item, List<String> commands, IntRange exp,
                     String message) {
}
