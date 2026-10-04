package updraftmc.shop;

import org.bukkit.plugin.java.JavaPlugin;
import updraftmc.shop.commands.HelpCommand;
import updraftmc.shop.commands.MainCommand;
import updraftmc.shop.commands.SellCommand;
import updraftmc.shop.commands.ShopCommand;

public class UpdraftShop extends JavaPlugin {



    @Override
    public void onEnable() {
        registerCommands();
    }

    private void registerCommands() {
        new SellCommand(this);
        new HelpCommand(this);
        new MainCommand(this);
        new ShopCommand(this);
    }
}