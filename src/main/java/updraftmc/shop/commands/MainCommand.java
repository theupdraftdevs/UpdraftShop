package updraftmc.shop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import updraftmc.shop.UpdraftShop;

public class MainCommand extends CommandFm implements CommandExecutor {
    private UpdraftShop server;
    public MainCommand(UpdraftShop server) {
        this.server = server;
        super(server);
    }

    @Override
    public @NonNull String getName() {
        return "updraftshop";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        return true;
    }
}
