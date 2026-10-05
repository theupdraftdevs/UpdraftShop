package updraftmc.shop.util;

import java.util.Locale;

/**
 * Reads the shorthand staff and players type for large amounts.
 *
 * <p>{@code 4000}, {@code 1k}, {@code 2.5m} and {@code 1b} all mean something
 * sensible, because typing a nine digit number is exactly the sort of thing that stops
 * somebody using the command at all. Bukkit free on purpose, so the parsing can be
 * tested directly rather than through a mock sender.
 *
 * <p>Always returns a finite number or null. NaN and Infinity are rejected because a
 * caller that multiplies a NaN amount into a balance turns the player's money into
 * NaN, and no economy backend recovers from that.
 */
public final class Amounts {

    private Amounts() {
    }

    /**
     * @return the amount {@code raw} names, or null if it is not a usable number
     */
    public static Double parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        char last = Character.toLowerCase(raw.charAt(raw.length() - 1));

        double multiplier = switch (last) {
            case 'k' -> 1_000;
            case 'm' -> 1_000_000;
            case 'b' -> 1_000_000_000d;
            default -> 1;
        };

        String digits = (multiplier == 1 ? raw : raw.substring(0, raw.length() - 1)).trim();

        Double parsed = number(digits);

        return parsed == null ? null : parsed * multiplier;
    }

    /**
     * @return the amount, or null if it is not a usable positive number
     */
    public static Double parsePositive(String raw) {
        Double parsed = parse(raw);

        return parsed == null || parsed <= 0 ? null : parsed;
    }

    private static Double number(String text) {
        if (text.isEmpty()) {
            return null;
        }

        try {
            double value = Double.parseDouble(text);

            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * @return {@code raw} in lower case, or an empty string, for case insensitive
     *         comparison of subcommands
     */
    public static String lower(String raw) {
        return raw == null ? "" : raw.toLowerCase(Locale.ROOT);
    }
}