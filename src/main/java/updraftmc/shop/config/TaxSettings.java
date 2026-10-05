package updraftmc.shop.config;

import org.bukkit.configuration.ConfigurationSection;
import updraftmc.shop.economy.Money;

import java.util.Locale;

/**
 * How much the shop takes from a trade, as a percentage.
 *
 * <p>A tax is stored as the fraction actually charged, so {@code 0.05} means five per
 * cent, and applying it is a single multiplication. Kept as its own record rather than
 * a bare double so the intent travels with the number and {@link #apply} cannot be
 * called on the wrong thing.
 *
 * <p>Every percentage in the plugin is validated to be non-negative and finite, because
 * a negative tax on a sale would pay the player for selling, and a non-finite one would
 * quietly turn a balance into NaN.
 */
public record TaxSettings(double buyRate, double sellRate) {

    /** No tax at all. */
    public static final TaxSettings NONE = new TaxSettings(0.0, 0.0);

    public TaxSettings {
        buyRate = valid(buyRate, "buy-rate");
        sellRate = valid(sellRate, "sell-rate");
    }

    /**
     * @return the amount actually charged for {@code gross} at {@code rate}
     */
    public static double apply(double gross, double rate) {
        if (gross <= 0 || rate <= 0) {
            return 0;
        }

        return Money.round(gross * rate);
    }

    /**
     * @return the amount paid out for {@code gross} at {@code rate}
     */
    public static double net(double gross, double rate) {
        return Money.round(gross - apply(gross, rate));
    }

    /**
     * Reads {@code tax.buy} and {@code tax.sell} from config.
     *
     * <p>Accepts either a fraction ({@code 0.05}) or a percentage ({@code 5}), because
     * both readings are natural and guessing wrong by a factor of 100 is a very
     * expensive mistake. A value above 1 is treated as a percentage, since no sane
     * server wants a 500 per cent tax and nobody means one.
     */
    public static TaxSettings load(ConfigurationSection section) {
        if (section == null) {
            return NONE;
        }

        return new TaxSettings(rate(section, "buy"), rate(section, "sell"));
    }

    private static double rate(ConfigurationSection section, String key) {
        double configured = section.getDouble(key, 0.0);

        if (!Double.isFinite(configured) || configured <= 0) {
            return 0;
        }

        return configured > 1 ? configured / 100.0 : configured;
    }

    private static double valid(double rate, String name) {
        if (!Double.isFinite(rate) || rate < 0) {
            throw new IllegalArgumentException("Tax " + name + " must be a finite, non-negative number, was " + rate);
        }

        return rate;
    }

    /**
     * @return whether any tax applies at all, so callers can skip the extra arithmetic
     */
    public boolean any() {
        return buyRate > 0 || sellRate > 0;
    }

    /**
     * @return the tax taken on a purchase of {@code gross}
     */
    public double onBuy(double gross) {
        return apply(gross, buyRate);
    }

    /**
     * @return the tax taken on a sale of {@code gross}
     */
    public double onSell(double gross) {
        return apply(gross, sellRate);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "buy %.1f%%, sell %.1f%%", buyRate * 100, sellRate * 100);
    }
}