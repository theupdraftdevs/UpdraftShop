package updraftmc.shop.commands;

import org.bukkit.entity.Player;
import updraftmc.shop.Permissions;
import updraftmc.shop.ShopService;
import updraftmc.shop.UpdraftShop;
import updraftmc.shop.config.Messages;
import updraftmc.shop.util.Amounts;

import java.util.List;
import java.util.logging.Level;

/**
 * {@code /shopadmin} for staff: reload, balance adjustments and opening the menu for
 * somebody else.
 *
 * <p>The permission check sits above the subcommand switch rather than inside each
 * branch, so adding a subcommand later cannot accidentally make it reachable by
 * everyone.
 */
public final class AdminCommand extends CommandFm {

    private final UpdraftShop plugin;

    public AdminCommand(UpdraftShop plugin, ShopService service) {
        super(plugin, "shopadmin");
        this.plugin = plugin;
    }

    @Override
    protected boolean execute(CommandContext context) {
        if (!context.sender().hasPermission(Permissions.ADMIN)) {
            messages().send(context.sender(), "no-permission");
            return true;
        }

        if (context.isEmpty()) {
            messages().send(context.sender(), "admin-help");
            return true;
        }

        // Case insensitive, because tab completion sends back whatever the player typed
        // and staff do type "RELOAD".
        return switch (Amounts.lower(context.arg(0, ""))) {
            case "reload" -> reload(context);
            case "give", "take" -> adjust(context, Amounts.lower(context.arg(0, "")));
            case "set" -> set(context);
            case "open" -> openFor(context);
            default -> help(context);
        };
    }

    /**
     * Re-reads config.yml and messages, keeping balances.
     */
    private boolean reload(CommandContext context) {
        try {
            plugin.reloadEverything();
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Reload failed, keeping the previous config in place.", exception);
            messages().send(context.sender(), "reload-failed");
            return true;
        }

        messages().send(context.sender(), "reload-success",
                "{entries}", String.valueOf(plugin.service().registry().entryCount()));

        return true;
    }

    private boolean adjust(CommandContext context, String action) {
        Player target = target(context);

        if (target == null) {
            return true;
        }

        Double amount = amount(context, 2);

        if (amount == null || amount <= 0) {
            messages().send(context.sender(), "bad-amount",
                    "{amount}", context.arg(2, ""));
            return true;
        }

        boolean taking = action.equals("take");

        boolean applied = taking
                ? economy().withdraw(target, amount)
                : economy().deposit(target, amount);

        if (!applied) {
            messages().send(context.sender(), "economy-error");
            return true;
        }

        double balance = economy().getBalance(target);

        messages().send(context.sender(), taking ? "admin-took" : "admin-gave",
                "{name}", target.getName(),
                "{amount}", economy().format(amount),
                "{balance}", economy().format(balance));

        messages().send(target, taking ? "admin-charged" : "admin-received",
                "{amount}", economy().format(amount),
                "{balance}", economy().format(balance));

        return true;
    }

    private boolean set(CommandContext context) {
        Player target = target(context);

        if (target == null) {
            return true;
        }

        Double amount = amount(context, 2);

        if (amount == null || amount < 0) {
            messages().send(context.sender(), "bad-amount", "{amount}", context.arg(2, ""));
            return true;
        }

        double delta = amount - economy().getBalance(target);

        boolean applied = delta == 0
                || (delta > 0 ? economy().deposit(target, delta) : economy().withdraw(target, -delta));

        if (!applied) {
            messages().send(context.sender(), "economy-error");
            return true;
        }

        messages().send(context.sender(), "admin-set",
                "{name}", target.getName(),
                "{amount}", economy().format(amount));

        messages().send(target, "admin-balance-changed",
                "{balance}", economy().format(amount));

        return true;
    }

    private boolean openFor(CommandContext context) {
        Player target = target(context);

        if (target == null) {
            return true;
        }

        if (!target.isOnline()) {
            messages().send(context.sender(), "player-offline", "{name}", target.getName());
            return true;
        }

        plugin.service().openShop(target);

        messages().send(context.sender(), "admin-opened", "{name}", target.getName());
        return true;
    }

    private boolean help(CommandContext context) {
        messages().send(context.sender(), "admin-help");
        return true;
    }

    /**
     * Resolves the target player, reporting a miss once so every branch does not have
     * to repeat the two message calls.
     */
    private Player target(CommandContext context) {
        Player target = context.playerAt(1);

        if (target == null) {
            messages().send(context.sender(), "unknown-player", "{name}", context.arg(1, ""));
        }

        return target;
    }

    /**
     * Reads a number, accepting {@code 4000}, {@code 1k}, {@code 2.5m} and {@code 1b}.
     *
     * <p>Staff typing {@code /shopadmin give Steve 4000} is the exception, not the
     * rule, so the shorthand saves real time on large adjustments.
     *
     * @return the amount, or null if it is not a usable number
     */
    static Double amount(CommandContext context, int index) {
        return Amounts.parse(context.arg(index, ""));
    }

    private Messages messages() {
        return plugin.service().messages();
    }

    private updraftmc.shop.economy.Economy economy() {
        return plugin.service().economy();
    }

    @Override
    protected List<String> suggest(CommandContext context) {
        if (context.isEmpty()) {
            return List.of("reload", "give", "take", "set", "open");
        }

        if (!context.sender().hasPermission(Permissions.ADMIN)) {
            return List.of();
        }

        return switch (context.arg(0, "")) {
            case "give", "take", "set", "open" ->
                    CommandContext.matching(context.raw(1), CommandContext.onlineNames());
            default -> List.of();
        };
    }
}