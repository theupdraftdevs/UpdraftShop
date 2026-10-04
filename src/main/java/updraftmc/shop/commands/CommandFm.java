package updraftmc.shop.commands;

import org.bukkit.command.CommandExecutor;
import updraftmc.shop.UpdraftShop;

import java.util.Locale;
import java.util.Objects;

public class CommandFm {
    public CommandFm(UpdraftShop server) {
        Objects.requireNonNull(server.getCommand(getName())).setExecutor((CommandExecutor) this);
    }
    public String getName() {return getClass().getSimpleName().replaceFirst("Command$", "").toLowerCase(Locale.ROOT);}
}
