package updraftmc.shop;

import org.bukkit.plugin.java.JavaPlugin;
import updraftmc.shop.commands.ShopCommand;
import updraftmc.shop.gui.GUIListener;

public final class UpdraftShop extends JavaPlugin {

    @Override
    public void onEnable() {
        getCommand("shop").setExecutor(new ShopCommand());

        getServer().getPluginManager().registerEvents(new GUIListener(), this);

        getLogger().info("UpdraftShop has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("UpdraftShop has been disabled.");
    }
}