package org.AkinToDeath.rPGKillRewardSystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The heart of the plugin. Minecraft fires an EntityDeathEvent every time a mob (or player)
 * dies; this class listens for it and decides what rewards the killer gets.
 */
public final class KillListener implements Listener {

    // Converts text like "<gold>Hello" into a coloured chat message.
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final RPGKillRewardSystem plugin;

    public KillListener(RPGKillRewardSystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Runs whenever any living thing dies.
     * HIGH priority = we run late, after most other plugins have had their say.
     * ignoreCancelled = skip the event if another plugin cancelled it.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity(); // the thing that died
        Player killer = mob.getKiller();      // the player who dealt the killing blow, or null
        if (killer == null) {
            return; // died to fall damage, lava, another mob, etc. -> no reward
        }

        // Look up the rules for this mob type (e.g. all ZOMBIE rules). Most mobs have none,
        // so we bail out early and cheaply for them.
        RewardConfig config = plugin.rewardConfig();
        List<MobRule> rules = config.rulesFor(mob.getType());
        if (rules.isEmpty()) {
            return;
        }

        // Get the mob's custom name (e.g. from a name tag or another RPG plugin) as plain text
        // with no colours, so rules with a "name:" filter can match against it.
        Component custom = mob.customName();
        String plainName = custom == null ? null : PlainTextComponentSerializer.plainText().serialize(custom);

        // Looting boosts drop chances: 1.0 with no Looting, more with each Looting level.
        double chanceMultiplier = 1.0 + lootingLevel(killer) * config.lootingBonus();

        for (MobRule rule : rules) {
            // Skip rules whose name filter doesn't match this mob (e.g. rule is for "Goblin King").
            if (!rule.matches(plainName)) {
                continue;
            }
            // Optionally wipe the normal Minecraft loot and XP so only our rewards are given.
            if (rule.clearVanillaDrops()) {
                event.getDrops().clear();
                event.setDroppedExp(0);
            }
            // Roll the dice for every reward in the rule.
            for (Reward reward : rule.rewards()) {
                double chance = reward.chance() * chanceMultiplier;
                // nextDouble() gives 0.0-1.0; times 100 gives 0-100. If it lands under the
                // chance, the reward drops. e.g. chance 25 -> succeeds on 0-25, so 25% of the time.
                if (ThreadLocalRandom.current().nextDouble() * 100.0 < chance) {
                    grant(config, reward, killer, mob);
                }
            }
        }
    }

    /** Hands out one successful reward: item, XP, commands and message, in that order. */
    private void grant(RewardConfig config, Reward reward, Player killer, LivingEntity mob) {
        Location where = mob.getLocation(); // where the mob died

        // --- Item ---
        if (reward.item() != null) {
            ItemStack stack = reward.item().create(); // build a fresh item (random amount, name, enchants)
            if (config.giveDirectly()) {
                // addItem returns whatever didn't fit. If the inventory is full, the leftovers
                // are dropped on the ground at the player's feet instead of being lost.
                killer.getInventory().addItem(stack)
                        .values()
                        .forEach(left -> killer.getWorld().dropItemNaturally(killer.getLocation(), left));
            } else {
                // Drop the item on the ground where the mob died.
                where.getWorld().dropItemNaturally(where, stack);
            }
        }

        // --- Experience ---
        int exp = reward.exp().roll(); // pick a random value in the configured range
        if (exp > 0) {
            killer.giveExp(exp);
        }

        // --- Commands ---
        for (String command : reward.commands()) {
            // Swap the placeholders for real values before running the command.
            String line = command
                    .replace("%player%", killer.getName())
                    .replace("%mob%", mob.getType().name())
                    .replace("%world%", where.getWorld().getName())
                    .replace("%x%", String.valueOf(where.getBlockX()))
                    .replace("%y%", String.valueOf(where.getBlockY()))
                    .replace("%z%", String.valueOf(where.getBlockZ()));
            // Run it as the server console (so it works even if the player lacks permission).
            // A leading "/" is stripped because the console doesn't use one.
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line.startsWith("/") ? line.substring(1) : line);
        }

        // --- Message ---
        if (config.sendMessages() && reward.message() != null && !reward.message().isBlank()) {
            killer.sendMessage(MINI.deserialize(reward.message()));
        }
    }

    /** Returns the Looting level on the item the player is holding (0 if none). */
    private static int lootingLevel(Player killer) {
        Enchantment looting = RewardConfig.findEnchantment("looting");
        return looting == null ? 0 : killer.getInventory().getItemInMainHand().getEnchantmentLevel(looting);
    }
}
