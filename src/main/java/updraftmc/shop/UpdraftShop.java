package updraftmc.shop;

import org.bukkit.plugin.java.JavaPlugin;
import updraftmc.shop.config.Messages;
import updraftmc.shop.config.ShopRegistry;
import updraftmc.shop.economy.Economy;
import updraftmc.shop.economy.InternalEconomy;
import updraftmc.shop.economy.VaultEconomy;
import updraftmc.shop.gui.GUIListener;

import java.io.File;

/**
 * Plugin entry point. Wires the config, the money backend and the menu listener
 * together, then hands off to {@link ShopService}.
 */
public final class UpdraftShop extends JavaPlugin {

    private ShopService service;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ShopRegistry registry = ShopRegistry.load(getConfig(), getLogger());
        Messages messages = Messages.load(getConfig());

        service = new ShopService(registry, createEconomy(registry), messages);

        getServer().getPluginManager().registerEvents(new GUIListener(service), this);

        // TODO: commands. The /shop executor calls ShopService.openShop(player); the
        // /sell executor reads a held item and calls ShopService.sell(player, ...).
        // Left unwritten on purpose, so nothing references the command stubs yet.

        getLogger().info("UpdraftShop has been enabled.");
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
        // Flushes economy.yml, so a server shutdown cannot lose balances.
        if (service != null) {
            service.shutdown();
        }

        getLogger().info("UpdraftShop has been disabled.");
    }
}
