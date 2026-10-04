package updraftmc.shop.economy;

import org.bukkit.OfflinePlayer;

/**
 * Money backend used by the shop.
 *
 * <p>Two implementations exist: {@link VaultEconomy}, which forwards to whichever
 * economy plugin Vault has registered, and {@link InternalEconomy}, a small
 * self-contained fallback used when Vault is not installed.
 */
public interface Economy {

    /**
     * @return the amount of money the player currently owns
     */
    double getBalance(OfflinePlayer player);

    /**
     * @return whether the player can afford {@code amount}
     */
    boolean has(OfflinePlayer player, double amount);

    /**
     * Takes {@code amount} from the player.
     *
     * @return {@code false} if the player cannot afford it or the backend rejected it,
     *         in which case no money was taken
     */
    boolean withdraw(OfflinePlayer player, double amount);

    /**
     * Gives {@code amount} to the player.
     *
     * @return {@code false} if the backend rejected it, in which case no money was given
     */
    boolean deposit(OfflinePlayer player, double amount);

    /**
     * @return {@code amount} rendered with the currency symbol, e.g. {@code $1,250.00}
     */
    String format(double amount);

    /**
     * @return a human readable name of the active backend, used in the startup log
     */
    String providerName();

    /**
     * Flushes any pending changes. Called when the plugin disables.
     */
    default void shutdown() {
    }
}