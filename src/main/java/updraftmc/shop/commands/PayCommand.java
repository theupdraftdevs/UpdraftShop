package updraftmc.shop.commands;

import org.bukkit.entity.Player;
import updraftmc.shop.Permissions;
import updraftmc.shop.ShopService;
import updraftmc.shop.UpdraftShop;
import updraftmc.shop.util.Amounts;

import java.util.List;

/**
 * {@code /pay} moves money from one player to another.
 *
 * <p>Goes through the same economy backend as the shop rather than keeping its own
 * ledger, so a balance stays correct whichever plugin is actually holding it.
 *
 * <p>The recipient has to be online. Paying somebody who is not would mean resolving a
 * name to a UUID, and Bukkit's own answer to that is a blocking web lookup against the
 * Mojang API, which is not something a chat command should trigger. Offline balances
 * keyed by name are not ours to guess at either.
 */
public final class PayCommand extends CommandFm {

    private final ShopService service;

    public PayCommand(UpdraftShop plugin, ShopService service) {
        super(plugin, "pay");
        this.service = service;
    }

    @Override
    protected boolean execute(CommandContext context) {
        if (context.size() < 2) {
            usage(context);
            return true;
        }

        if (!context.sender().hasPermission(Permissions.PAY)) {
            service.messages().send(context.sender(), "no-permission");
            return true;
        }

        Player from = context.player();

        if (from == null) {
            service.messages().sendRaw(context.sender(), "players-only");
            return true;
        }

        Player to = context.playerAt(0);

        if (to == null) {
            service.messages().send(context.sender(), "unknown-player", "{name}", context.arg(0, ""));
            return true;
        }

        if (to.equals(from)) {
            service.messages().send(from, "pay-self");
            return true;
        }

        Double amount = amount(context);

        if (amount == null || amount <= 0) {
            service.messages().send(from, "bad-amount", "{amount}", context.arg(1, ""));
            return true;
        }

        if (!service.economy().has(from, amount)) {
            service.messages().send(from, "not-enough-money",
                    "{price}", service.economy().format(amount));
            return true;
        }

        if (!service.pay(from, to, amount)) {
            service.messages().send(from, "economy-error");
            return true;
        }

        service.tradeSound(from);

        String formatted = service.economy().format(amount);

        service.messages().send(from, "paid",
                "{name}", to.getName(),
                "{amount}", formatted);

        service.messages().send(to, "received",
                "{name}", from.getName(),
                "{amount}", formatted);

        return true;
    }

    /**
     * @return the amount in {@code k}, {@code m} or {@code b} form, or null if the
     *         argument is not a usable number
     */
    static Double amount(CommandContext context) {
        return Amounts.parsePositive(context.arg(1, ""));
    }

    private void usage(CommandContext context) {
        service.messages().sendRaw(context.sender(), "pay-usage");
    }

    @Override
    protected List<String> suggest(CommandContext context) {
        return context.isEmpty() ? CommandContext.onlineNames() : List.of();
    }
}