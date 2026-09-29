package org.AkinToDeath.rPGKillRewardSystem;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class of the plugin. The server creates one instance of it when the plugin loads.
 * Its job is just to wire everything together: load the config, register the kill
 * listener and register the /killreward command.
 */
public final class RPGKillRewardSystem extends JavaPlugin {

    // The parsed version of config.yml. "volatile" makes sure that when /killreward reload
    // swaps in a new one, the listener immediately sees it.
    private volatile RewardConfig rewardConfig;

    /** Called by the server when the plugin starts up. */
    @Override
    public void onEnable() {
        // Copies the default config.yml (from inside the jar) to plugins/<name>/config.yml,
        // but only if the server owner doesn't have one yet, so their edits are never overwritten.
        saveDefaultConfig();
        reloadRewards();

        // Tell the server to send us events. KillListener will now be called on every mob death.
        getServer().getPluginManager().registerEvents(new KillListener(this), this);

        // Connect the "killreward" command (declared in plugin.yml) to our handler class.
        PluginCommand command = getCommand("killreward");
        if (command != null) {
            KillRewardCommand handler = new KillRewardCommand(this);
            command.setExecutor(handler);      // runs the command
            command.setTabCompleter(handler);  // suggests "reload" / "list" when pressing Tab
        }

        getLogger().info("Loaded " + rewardConfig.ruleCount() + " mob rule(s) with "
                + rewardConfig.rewardCount() + " reward(s).");
    }

    /**
     * Re-reads config.yml from disk and rebuilds all the mob rules.
     * Used on startup and by /killreward reload.
     */
    public void reloadRewards() {
        reloadConfig(); // re-read the file from disk
        rewardConfig = RewardConfig.load(getConfig(), getLogger()); // turn the raw YAML into rules
    }

    /** The currently active rules and settings. */
    public RewardConfig rewardConfig() {
        return rewardConfig;
    }
}
