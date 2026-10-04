package updraftmc.shop;

import org.bukkit.plugin.java.JavaPlugin;
import updraftmc.shop.commands.HelpCommand;
import updraftmc.shop.commands.MainCommand;
import updraftmc.shop.commands.SellCommand;
import updraftmc.shop.commands.ShopCommand;
import updraftmc.shop.gui.GUIListener;

public class UpdraftShop extends JavaPlugin {



    @Override
    public void onEnable() {
        registerCommands();
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getLogger().info("UpdraftShop has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("UpdraftShop has been disabled.");
    }

    private void registerCommands() {
        new SellCommand(this);
        new HelpCommand(this);
        new MainCommand(this);
        new ShopCommand(this);
    }
}