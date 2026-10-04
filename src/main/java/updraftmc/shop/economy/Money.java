package updraftmc.shop.economy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Money rendering in the style Hypixel uses: thousands separators, and no trailing
 * {@code .00} on whole amounts, so a diamond reads {@code $500} and a fraction reads
 * {@code $12.50}.
 */
public final class Money {

    // DecimalFormat is not thread safe and economy lookups are not guaranteed to stay
    // on the main thread, so each thread gets its own pair.
    private static final ThreadLocal<DecimalFormat> WHOLE =
            ThreadLocal.withInitial(() -> new DecimalFormat("#,##0", symbols()));
    private static final ThreadLocal<DecimalFormat> CENTS =
            ThreadLocal.withInitial(() -> new DecimalFormat("#,##0.00", symbols()));

    private Money() {
    }

    public static String format(double amount, String symbol) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            return symbol + "0";
        }

        BigDecimal value = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);

        if (value.signum() < 0) {
            return "-" + symbol + format(value.abs());
        }

        return symbol + format(value);
    }

    private static String format(BigDecimal value) {
        // 12.00 prints as $12, anything with real cents prints as $12.50.
        return value.stripTrailingZeros().scale() <= 0
                ? WHOLE.get().format(value)
                : CENTS.get().format(value);
    }

    private static DecimalFormatSymbols symbols() {
        // Always "." and "," regardless of the server's locale, or prices would be
        // written differently on every operator's client.
        return DecimalFormatSymbols.getInstance(Locale.ROOT);
    }
}