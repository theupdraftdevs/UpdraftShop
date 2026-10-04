package updraftmc.shop.economy;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Small self-contained economy used when Vault is not available.
 *
 * <p>Balances are cached in memory and written back to {@code economy.yml} after
 * every change. Shop transactions are infrequent enough that the write is cheap,
 * and it removes any chance of losing balances if the server dies.
 */
public final class InternalEconomy implements Economy {

    private static final double EPSILON = 0.0001;
    private static final String SECTION = "balances";

    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();
    private final File file;
    private final double startingBalance;
    private final String currencySymbol;
    private final Logger logger;

    private YamlConfiguration data;

    private InternalEconomy(File file, double startingBalance, String currencySymbol, Logger logger) {
        this.file = file;
        this.startingBalance = round(startingBalance);
        this.currencySymbol = currencySymbol;
        this.logger = logger;
    }

    /**
     * Reads balances from {@code file}, creating it when missing.
     */
    public static InternalEconomy load(File file, double startingBalance, String currencySymbol, Logger logger) {
        InternalEconomy economy = new InternalEconomy(file, startingBalance, currencySymbol, logger);
        economy.read();
        return economy;
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return round(balances.getOrDefault(player.getUniqueId(), startingBalance));
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) + EPSILON >= amount;
    }

    @Override
    public synchronized boolean withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0) {
            return true;
        }

        if (!has(player, amount)) {
            return false;
        }

        balances.put(player.getUniqueId(), round(getBalance(player) - amount));
        write();
        return true;
    }

    @Override
    public synchronized boolean deposit(OfflinePlayer player, double amount) {
        if (amount <= 0) {
            return true;
        }

        balances.put(player.getUniqueId(), round(getBalance(player) + amount));
        write();
        return true;
    }

    @Override
    public String format(double amount) {
        return Money.format(amount, currencySymbol);
    }

    @Override
    public String providerName() {
        return "built-in economy (" + file.getName() + ")";
    }

    /**
     * Kills a fractional cent so repeated buys and sells cannot leave dust behind.
     */
    public static double round(double amount) {
        return BigDecimal.valueOf(amount)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private void read() {
        data = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = data.getConfigurationSection(SECTION);

        if (section == null) {
            write();
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                balances.put(UUID.fromString(key), round(section.getDouble(key)));
            } catch (IllegalArgumentException exception) {
                logger.warning("Skipping malformed balance entry '" + key + "' in " + file.getName() + ".");
            }
        }

        logger.info("Loaded " + balances.size() + " balances from " + file.getName() + ".");
    }

    private void write() {
        data.set("starting-balance", startingBalance);
        data.set(SECTION, null);

        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            data.set(SECTION + "." + entry.getKey(), entry.getValue());
        }

        try {
            data.save(file);
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Could not save balances to " + file.getName() + ".", exception);
        }
    }
}