package updraftmc.shop;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import updraftmc.shop.commands.AdminCommand;
import updraftmc.shop.commands.BalanceCommand;
import updraftmc.shop.commands.HelpCommand;
import updraftmc.shop.commands.MainCommand;
import updraftmc.shop.commands.PayCommand;
import updraftmc.shop.commands.SellCommand;
import updraftmc.shop.commands.ShopCommand;
import updraftmc.shop.commands.StatsCommand;
import updraftmc.shop.config.Messages;
import updraftmc.shop.config.ShopRegistry;
import updraftmc.shop.config.TradeStats;
import updraftmc.shop.economy.Economy;
import updraftmc.shop.economy.InternalEconomy;
import updraftmc.shop.economy.VaultEconomy;
import updraftmc.shop.gui.GUIListener;

import java.io.File;
import java.util.logging.Level;

/**
 * Plugin entry point. Wires the config, the money backend and the menu listener
 * together, then hands off to {@link ShopService}.
 */
public final class UpdraftShop extends JavaPlugin {

    /**
     * How often trade stats are written out, in ticks. 20 ticks is one second.
     */
    private static final long SAVE_INTERVAL = 20L * 60L * 5L;

    private ShopService service;

    private BukkitTask saveTask;

    @Override
    public void onEnable() {
        try {
            reloadEverything();
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "Could not start the shop, disabling.", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (saveTask == null) {
            // Every five minutes: long enough to be off the hot path, short enough that a
            // hard crash loses very little history.
            saveTask = getServer().getScheduler()
                    .runTaskTimer(this, this::flushStats, SAVE_INTERVAL, SAVE_INTERVAL);
        }

        getServer().getPluginManager().registerEvents(new GUIListener(service), this);

        // Registered after construction so no command escapes `this` mid-init.
        new MainCommand(this, service).register();
        new ShopCommand(this, service).register();
        new SellCommand(this, service).register();
        new BalanceCommand(this, service).register();
        new PayCommand(this, service).register();
        new AdminCommand(this, service).register();
        new StatsCommand(this, service).register();
        new HelpCommand(this, service).register();

        getLogger().info("Loaded " + service.registry().entryCount()
                + " shop entries using " + service.economy().providerName() + ".");
    }

    /**
     * Re-reads config.yml and rebuilds the service.
     *
     * <p>Also the path {@code /shopadmin reload} takes. Balances are not touched: the
     * economy backend is created once and kept, because reloading it would either lose
     * the built-in balances or re-register with Vault for no reason.
     *
     * <p>Registered listeners keep working across a reload because they hold the
     * service, and {@link ShopService} is swapped in place below so they see the new
     * config rather than the old one.
     */
    public void reloadEverything() {
        saveDefaultConfig();
        reloadConfig();

        ShopRegistry registry = ShopRegistry.load(getConfig(), getLogger());
        Messages messages = Messages.load(getConfig());

        if (service == null) {
            service = new ShopService(registry, createEconomy(registry), messages,
                    TradeStats.Store.load(statsFile(), getLogger()));
            return;
        }

        service.reload(registry, messages);
    }

    /**
     * Flushes the trade stats on a timer.
     *
     * <p>Deliberately not on every trade: a disk write per click in the shop makes the
     * menu feel laggy, and a crash between flushes costs a few trades of history at
     * worst. Runs on the main thread, which is fine because it is one small file.
     */
    public void flushStats() {
        if (service != null) {
            service.saveStats();
        }
    }

    public ShopService service() {
        return service;
    }

    /**
     * Where trade statistics live. Kept out of the data folder root so it is obvious
     * which files the plugin owns.
     */
    public File statsFile() {
        return new File(getDataFolder(), "stats.yml");
    }

    /**
     * Prefers Vault so the shop respects whatever economy plugin the server already
     * runs, and falls back to balances in economy.yml when Vault is not installed.
     * The reflection based Vault lookup means this loads fine without Vault on the
     * server, so the fallback is not a hard dependency.
     */
    private Economy createEconomy(ShopRegistry registry) {
        Economy vault = VaultEconomy.createIfAvailable(getLogger());

        if (vault != null) {
            getLogger().info("Using Vault for balances.");
            return vault;
        }

        getLogger().warning("Vault not found, falling back to the built in balances in economy.yml.");

        return InternalEconomy.load(
                new File(getDataFolder(), "economy.yml"),
                registry.startingBalance(),
                registry.currencySymbol(),
                getLogger());
    }

    @Override
    public void onDisable() {
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }

        // Flushes economy.yml and the stats, so a server shutdown cannot lose either.
        if (service != null) {
            service.shutdown();
        }

        getLogger().info("UpdraftShop has been disabled.");
    }
}