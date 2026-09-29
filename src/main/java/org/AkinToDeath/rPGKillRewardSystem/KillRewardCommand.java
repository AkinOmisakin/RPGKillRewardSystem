package org.AkinToDeath.rPGKillRewardSystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Locale;

/**
 * Handles /killreward (alias /kr). Only players with the killreward.admin permission
 * can use it; that check is done by the server based on plugin.yml.
 *
 * /killreward reload  - re-read config.yml without restarting the server
 * /killreward list    - show every configured mob rule
 */
public final class KillRewardCommand implements CommandExecutor, TabCompleter {

    // The words suggested when a player presses Tab.
    private static final List<String> SUBCOMMANDS = List.of("reload", "list");

    private final RPGKillRewardSystem plugin;

    public KillRewardCommand(RPGKillRewardSystem plugin) {
        this.plugin = plugin;
    }

    /** Runs when someone types /killreward ... */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // args[0] is the first word after the command. No word = empty string = show usage.
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                plugin.reloadRewards();
                RewardConfig config = plugin.rewardConfig();
                sender.sendMessage(Component.text("Loaded " + config.ruleCount() + " mob rule(s) with "
                        + config.rewardCount() + " reward(s). Check console for warnings.", NamedTextColor.GREEN));
            }
            case "list" -> {
                List<MobRule> rules = plugin.rewardConfig().allRules();
                if (rules.isEmpty()) {
                    sender.sendMessage(Component.text("No mob rules configured.", NamedTextColor.RED));
                    return true;
                }
                // One line per rule, e.g. "- goblin_king: ZOMBIE named "goblin king" (2 rewards)"
                for (MobRule rule : rules) {
                    String filter = rule.nameFilter() == null ? "" : " named \"" + rule.nameFilter() + "\"";
                    sender.sendMessage(Component.text("- " + rule.id() + ": " + rule.type().name() + filter
                            + " (" + rule.rewards().size() + " rewards)", NamedTextColor.GRAY));
                }
            }
            // Anything else (including no argument) just prints how to use the command.
            default -> sender.sendMessage(
                    Component.text("Usage: /" + label + " <reload|list>", NamedTextColor.YELLOW));
        }
        return true; // true = "command handled", so the server doesn't print its own usage message
    }

    /** Provides Tab suggestions. Only the first word after the command gets suggestions. */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        // Only suggest subcommands that start with what the player has typed so far.
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return SUBCOMMANDS.stream().filter(s -> s.startsWith(prefix)).toList();
    }
}
