package updraftmc.shop;

/**
 * Every permission node the plugin checks, in one place.
 *
 * <p>Nodes are declared in plugin.yml so they show up with a description in the server
 * manager and default sensibly, but the constants live here so a typo is a compile
 * error rather than a permission that silently never matches.
 */
public final class Permissions {

    /** Open the shop and buy items. */
    public static final String USE = "updraftshop.use";

    /** Sell items, whether through the trade screen or {@code /sell}. */
    public static final String SELL = "updraftshop.sell";

    /** See the balance head and the statistics screen. */
    public static final String STATS = "updraftshop.stats";

    /** Send money with {@code /pay}. */
    public static final String PAY = "updraftshop.pay";

    /** Reload the config and adjust other people's balances. */
    public static final String ADMIN = "updraftshop.admin";

    private Permissions() {
    }
}