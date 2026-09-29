package org.AkinToDeath.rPGKillRewardSystem;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A whole-number range where both ends are included. In config.yml you can write
 * either a single number ("5" -> always 5) or a range ("1-3" -> 1, 2 or 3).
 */
public record IntRange(int min, int max) {

    /** Used as the default for "no XP". */
    public static final IntRange ZERO = new IntRange(0, 0);

    /**
     * Turns config text like "5" or "1-3" into an IntRange.
     *
     * @return {@code fallback} if the setting is missing, or null if the text isn't valid
     */
    public static IntRange parse(Object raw, IntRange fallback) {
        if (raw == null) {
            return fallback; // setting not present in the config
        }
        String text = String.valueOf(raw).trim();
        try {
            // Look for a dash from index 1 onwards (index 0 would be a minus sign, not a range).
            int dash = text.indexOf('-', 1);
            if (dash > 0) {
                int a = Integer.parseInt(text.substring(0, dash).trim());
                int b = Integer.parseInt(text.substring(dash + 1).trim());
                // Accept "3-1" as well as "1-3".
                return new IntRange(Math.min(a, b), Math.max(a, b));
            }
            int value = Integer.parseInt(text);
            return new IntRange(value, value);
        } catch (NumberFormatException e) {
            return null; // not a number, caller will log a warning
        }
    }

    /** Picks a random number from min to max (both included). */
    public int roll() {
        return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
