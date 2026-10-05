package updraftmc.shop.commands;

import org.bukkit.entity.Player;
import updraftmc.shop.Permissions;
import updraftmc.shop.ShopService;
import updraftmc.shop.UpdraftShop;

import java.util.List;

/**
 * {@code /balance} shows what a player can spend.
 *
 * <p>Without an argument it reports the sender's own balance, so it works from the
 * console and from a player alike. With a name it falls back to looking the player up
 * by name, which needs the admin permission.
 */
public final class BalanceCommand extends CommandFm {

    private final ShopService service;

    public BalanceCommand(UpdraftShop plugin, ShopService service) {
        super(plugin, "balance");
        this.service = service;
    }

    @Override
    protected boolean execute(CommandContext context) {
        String asked = context.arg(0, "");

        if (asked.isEmpty()) {
            if (context.isPlayer()) {
                show(context.player());
                return true;
            }

            service.messages().sendRaw(context.sender(), "players-only");
            return true;
        }

        if (!context.sender().hasPermission(Permissions.ADMIN)) {
            service.messages().send(context.sender(), "no-permission");
            return true;
        }

        Player target = context.playerAt(0);

        if (target == null) {
            service.messages().send(context.sender(), "unknown-player", "{name}", asked);
            return true;
        }

        show(target);
        return true;
    }

    private void show(Player player) {
        double balance = service.economy().getBalance(player);

        service.messages().send(player, "balance",
                "{balance}", service.economy().format(balance));
    }

    @Override
    protected List<String> suggest(CommandContext context) {
        if (context.isEmpty() && context.sender().hasPermission(Permissions.ADMIN)) {
            return CommandContext.onlineNames();
        }

        return List.of();
    }
}