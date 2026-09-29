package org.AkinToDeath.rPGKillRewardSystem;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Reads config.yml and converts it into the Java objects (MobRule, Reward, ItemTemplate)
 * that the rest of the plugin uses. Doing this once at load time means the listener never
 * has to parse text while a player is fighting.
 *
 * If something in the config is wrong (a typo in a material name, say), that entry is
 * skipped and a warning is printed to the console. The plugin never crashes over bad config.
 */
public final class RewardConfig {

    // Global settings from the "settings:" section.
    private final boolean giveDirectly;
    private final double lootingBonus;
    private final boolean sendMessages;

    // All mob rules, grouped by entity type so the listener can find "all ZOMBIE rules" instantly.
    private final Map<EntityType, List<MobRule>> rulesByType;

    // Just for the "Loaded X rules" messages.
    private final int ruleCount;
    private final int rewardCount;

    // Private: use RewardConfig.load(...) to create one.
    private RewardConfig(boolean giveDirectly, double lootingBonus, boolean sendMessages,
                         Map<EntityType, List<MobRule>> rulesByType, int ruleCount, int rewardCount) {
        this.giveDirectly = giveDirectly;
        this.lootingBonus = lootingBonus;
        this.sendMessages = sendMessages;
        this.rulesByType = rulesByType;
        this.ruleCount = ruleCount;
        this.rewardCount = rewardCount;
    }

    public boolean giveDirectly() { return giveDirectly; }
    public double lootingBonus() { return lootingBonus; }
    public boolean sendMessages() { return sendMessages; }
    public int ruleCount() { return ruleCount; }
    public int rewardCount() { return rewardCount; }

    /** All rules for one mob type, or an empty list if that mob has none. */
    public List<MobRule> rulesFor(EntityType type) {
        return rulesByType.getOrDefault(type, List.of());
    }

    /** Every rule across all mob types (used by /killreward list). */
    public List<MobRule> allRules() {
        return rulesByType.values().stream().flatMap(List::stream).toList();
    }

    /** Builds a RewardConfig from the raw YAML config. Entry point for parsing. */
    public static RewardConfig load(FileConfiguration config, Logger log) {
        // Second argument of each getter is the default, used if the setting is missing.
        boolean giveDirectly = config.getBoolean("settings.give-directly", true);
        double lootingBonus = Math.max(0, config.getDouble("settings.looting-bonus", 0.25));
        boolean sendMessages = config.getBoolean("settings.send-messages", true);

        Map<EntityType, List<MobRule>> rules = new EnumMap<>(EntityType.class);
        int ruleCount = 0;
        int rewardCount = 0;

        // Walk through every entry under "mobs:" (zombie, skeleton, goblin_king, ...).
        ConfigurationSection mobs = config.getConfigurationSection("mobs");
        if (mobs != null) {
            for (String id : mobs.getKeys(false)) {
                ConfigurationSection section = mobs.getConfigurationSection(id);
                if (section == null) {
                    continue; // not a proper section, ignore
                }
                MobRule rule = parseRule(id, section, log);
                if (rule == null) {
                    continue; // had an error, already warned about it
                }
                // Add the rule to the list for its entity type, creating the list if needed.
                rules.computeIfAbsent(rule.type(), k -> new ArrayList<>()).add(rule);
                ruleCount++;
                rewardCount += rule.rewards().size();
            }
        }
        return new RewardConfig(giveDirectly, lootingBonus, sendMessages, rules, ruleCount, rewardCount);
    }

    /** Parses one entry under "mobs:" into a MobRule, or returns null if the entity type is invalid. */
    private static MobRule parseRule(String id, ConfigurationSection section, Logger log) {
        // "type:" is optional; if missing we use the section's id (so "zombie:" means ZOMBIE).
        String typeName = section.getString("type", id).toUpperCase(Locale.ROOT).replace(' ', '_');
        EntityType type;
        try {
            type = EntityType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            log.warning("mobs." + id + ": unknown entity type '" + typeName + "', skipping.");
            return null;
        }

        // Optional custom-name filter, stored lower-case so matching ignores capitalisation.
        String name = section.getString("name");
        String nameFilter = (name == null || name.isBlank()) ? null : name.toLowerCase(Locale.ROOT);

        // Parse each reward listed under "rewards:".
        List<Reward> rewards = new ArrayList<>();
        ConfigurationSection rewardSection = section.getConfigurationSection("rewards");
        if (rewardSection != null) {
            for (String rewardId : rewardSection.getKeys(false)) {
                ConfigurationSection rs = rewardSection.getConfigurationSection(rewardId);
                if (rs == null) {
                    continue;
                }
                // The path (e.g. "mobs.zombie.rewards.cursed_blade") is only used in warnings,
                // so the server owner can see exactly where the mistake is.
                Reward reward = parseReward("mobs." + id + ".rewards." + rewardId, rewardId, rs, log);
                if (reward != null) {
                    rewards.add(reward);
                }
            }
        }
        return new MobRule(id, type, nameFilter, section.getBoolean("clear-vanilla-drops", false),
                List.copyOf(rewards));
    }

    /** Parses a single reward, or returns null (with a warning) if it's invalid or empty. */
    private static Reward parseReward(String path, String id, ConfigurationSection rs, Logger log) {
        // Clamp chance into 0-100 so a typo like 500 can't do anything strange.
        double chance = Math.max(0, Math.min(100, rs.getDouble("chance", 100.0)));

        // Optional item part.
        ItemTemplate item = null;
        ConfigurationSection itemSection = rs.getConfigurationSection("item");
        if (itemSection != null) {
            item = parseItem(path + ".item", itemSection, log);
            if (item == null) {
                return null; // item was invalid, drop the whole reward rather than half-give it
            }
        }

        // Optional commands part.
        List<String> commands = rs.getStringList("commands");

        // Optional XP part. IntRange.parse returns null when the text isn't a number/range.
        IntRange exp = IntRange.parse(rs.get("exp"), IntRange.ZERO);
        if (exp == null) {
            log.warning(path + ": invalid exp '" + rs.get("exp") + "', skipping reward.");
            return null;
        }

        // A reward that gives nothing at all is almost certainly a config mistake.
        if (item == null && commands.isEmpty() && exp.max() <= 0) {
            log.warning(path + ": reward has no item, commands or exp, skipping.");
            return null;
        }
        return new Reward(id, chance, item, List.copyOf(commands), exp, rs.getString("message"));
    }

    /** Parses the "item:" section of a reward into an ItemTemplate, or null if invalid. */
    private static ItemTemplate parseItem(String path, ConfigurationSection is, Logger log) {
        // matchMaterial accepts "IRON_SWORD", "iron_sword" and "minecraft:iron_sword".
        String materialName = is.getString("material", "");
        Material material = Material.matchMaterial(materialName);
        if (material == null || !material.isItem() || material.isAir()) {
            log.warning(path + ": unknown item material '" + materialName + "', skipping reward.");
            return null;
        }

        // Amount can be a number (3) or range ("1-3"). Defaults to exactly 1.
        IntRange amount = IntRange.parse(is.get("amount"), new IntRange(1, 1));
        if (amount == null) {
            log.warning(path + ": invalid amount '" + is.get("amount") + "', skipping reward.");
            return null;
        }

        // Enchantments: "sharpness: 3" style entries. Unknown names are ignored, not fatal.
        Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
        ConfigurationSection es = is.getConfigurationSection("enchantments");
        if (es != null) {
            for (String key : es.getKeys(false)) {
                Enchantment enchantment = findEnchantment(key);
                if (enchantment == null) {
                    log.warning(path + ": unknown enchantment '" + key + "', ignoring it.");
                    continue;
                }
                enchantments.put(enchantment, Math.max(1, es.getInt(key, 1)));
            }
        }
        return new ItemTemplate(material, amount, is.getString("name"), List.copyOf(is.getStringList("lore")),
                Map.copyOf(enchantments));
    }

    /**
     * Looks an enchantment up by its name (e.g. "sharpness" or "minecraft:sharpness").
     * Returns null if no such enchantment exists.
     */
    public static Enchantment findEnchantment(String key) {
        NamespacedKey namespaced = NamespacedKey.fromString(key.toLowerCase(Locale.ROOT));
        return namespaced == null ? null
                : RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(namespaced);
    }
}
