package updraftmc.shop;

import org.bukkit.plugin.java.JavaPlugin;
import updraftmc.shop.config.Messages;
import updraftmc.shop.config.ShopRegistry;
import updraftmc.shop.economy.Economy;
import updraftmc.shop.economy.InternalEconomy;
import updraftmc.shop.economy.VaultEconomy;
import updraftmc.shop.gui.GUIListener;

import java.io.File;

public final class UpdraftShop extends JavaPlugin {

    private ShopService service;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ShopRegistry registry = ShopRegistry.load(getConfig(), getLogger());
        Messages messages = Messages.load(getConfig());

        Economy economy = VaultEconomy.createIfAvailable(getLogger());

        if (economy == null) {
            economy = InternalEconomy.load(
                    new File(getDataFolder(), "economy.yml"),
                    registry.startingBalance(),
                    registry.currencySymbol(),
                    getLogger());
        }

        service = new ShopService(registry, economy, messages);

        getServer().getPluginManager().registerEvents(new GUIListener(service), this);

        // TODO: commands -- register ShopCommand and SellCommand here once they are
        // implemented. The shop opens through ShopService#openShop(Player).

        getLogger().info("Loaded " + registry.entryCount() + " shop entries using " + economy.providerName() + ".");
        getLogger().info("UpdraftShop has been enabled.");
    }

    @Override
    public void onDisable() {
        if (service != null) {
            service.shutdown();
            service = null;
        }

        getLogger().info("UpdraftShop has been disabled.");
    }

    /**
     * @return the running shop service, or {@code null} before enable / after disable
     */
    public ShopService shop() {
        return service;
    }
}