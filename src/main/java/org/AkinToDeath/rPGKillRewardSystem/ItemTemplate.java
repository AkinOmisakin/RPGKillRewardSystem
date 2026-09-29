package org.AkinToDeath.rPGKillRewardSystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

/**
 * A blueprint for an item reward, read from the "item:" section of config.yml.
 * It is not an actual item; a brand new ItemStack is built from it every time
 * a reward drops, so each drop can have its own random amount.
 *
 * @param material     what the item is (e.g. IRON_SWORD)
 * @param amount       how many, as a range (e.g. 1-3)
 * @param name         nullable MiniMessage display name
 * @param lore         MiniMessage lines shown under the item name (may be empty)
 * @param enchantments enchantment -> level
 */
public record ItemTemplate(Material material, IntRange amount, String name, List<String> lore,
                           Map<Enchantment, Integer> enchantments) {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /** Builds a fresh item from this blueprint. */
    public ItemStack create() {
        // Roll the amount, but keep it between 1 and the item's max stack size (e.g. 64, or 1 for swords).
        int count = Math.max(1, Math.min(amount.roll(), material.getMaxStackSize()));
        ItemStack item = new ItemStack(material, count);

        // Name, lore and enchantments live in the item's "meta"; edit it, then put it back.
        ItemMeta meta = item.getItemMeta();
        if (name != null) {
            meta.displayName(noItalic(MINI.deserialize(name)));
        }
        if (!lore.isEmpty()) {
            meta.lore(lore.stream().map(line -> noItalic(MINI.deserialize(line))).toList());
        }
        // The "true" allows levels above vanilla's normal max (e.g. Sharpness 10).
        enchantments.forEach((enchantment, level) -> meta.addEnchant(enchantment, level, true));
        item.setItemMeta(meta);
        return item;
    }

    /** Custom names/lore are italic by default in-game; turn that off unless the config asks for it. */
    private static Component noItalic(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
