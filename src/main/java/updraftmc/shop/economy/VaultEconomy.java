package updraftmc.shop.economy;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bridges to Vault so the shop uses the economy plugin the server already runs.
 *
 * <p>Vault is reached reflectively on purpose: the API is only ever touched if the
 * plugin is actually installed, which keeps UpdraftShop free of a hard dependency
 * and loadable on servers without Vault.
 */
public final class VaultEconomy implements Economy {

    private static final String VAULT_ECONOMY_CLASS = "net.milkbowl.vault.economy.Economy";

    private final Object provider;
    private final String providerName;
    private final Method getBalance;
    private final Method has;
    private final Method withdrawPlayer;
    private final Method depositPlayer;
    private final Method format;

    private VaultEconomy(String providerName, Class<?> vaultEconomy, Object provider)
            throws NoSuchMethodException {
        this.provider = provider;
        this.providerName = providerName;
        this.getBalance = vaultEconomy.getMethod("getBalance", OfflinePlayer.class);
        this.has = vaultEconomy.getMethod("has", OfflinePlayer.class, double.class);
        this.withdrawPlayer = vaultEconomy.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
        this.depositPlayer = vaultEconomy.getMethod("depositPlayer", OfflinePlayer.class, double.class);
        this.format = vaultEconomy.getMethod("format", double.class);
    }

    /**
     * @return a Vault backed economy, or {@code null} when Vault (or its economy
     *         service) is unavailable so the caller can fall back
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Economy createIfAvailable(Logger logger) {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return null;
        }

        try {
            Class<?> vaultEconomy = Class.forName(VAULT_ECONOMY_CLASS);

            RegisteredServiceProvider<?> registration =
                    (RegisteredServiceProvider<?>) Bukkit.getServicesManager().getRegistration((Class) vaultEconomy);

            if (registration == null) {
                logger.warning("Vault is installed but no economy plugin is registered with it.");
                return null;
            }

            Object provider = registration.getProvider();

            if (provider == null) {
                logger.warning("Vault returned an empty economy provider.");
                return null;
            }

            return new VaultEconomy(registration.getPlugin().getName(), vaultEconomy, provider);
        } catch (ReflectiveOperationException | LinkageError exception) {
            logger.log(Level.WARNING,
                    "Could not hook into Vault, using the built-in economy instead.", exception);
            return null;
        }
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        try {
            return (Double) getBalance.invoke(provider, player);
        } catch (ReflectiveOperationException exception) {
            return 0.0;
        }
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        try {
            return Boolean.TRUE.equals(has.invoke(provider, player, amount));
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        try {
            return succeeded(withdrawPlayer.invoke(provider, player, amount));
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        try {
            return succeeded(depositPlayer.invoke(provider, player, amount));
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    /**
     * Vault 1.7+ returns an {@link Component} error message on failure and
     * {@code null} on success. Older versions return a plain boolean.
     */
    private static boolean succeeded(Object result) {
        if (result instanceof Component response) {
            return response == null;
        }

        return Boolean.TRUE.equals(result);
    }

    @Override
    public String format(double amount) {
        try {
            Object result = format.invoke(provider, amount);
            return result == null ? String.valueOf(amount) : String.valueOf(result);
        } catch (ReflectiveOperationException exception) {
            return String.valueOf(amount);
        }
    }

    @Override
    public String providerName() {
        return providerName;
    }
}