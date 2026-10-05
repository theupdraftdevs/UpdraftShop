package updraftmc.shop.commands;

import org.bukkit.entity.Player;
import updraftmc.shop.Permissions;
import updraftmc.shop.ShopService;
import updraftmc.shop.UpdraftShop;
import updraftmc.shop.config.TradeStats;

import java.util.List;

/**
 * {@code /shopstats} shows what a player has traded.
 *
 * <p>Gated on {@code updraftshop.stats} rather than being open to everyone: on a server
 * where the shop is a competitive part of the game, letting a player read anybody's
 * earnings is a way to find out who to rob.
 *
 * <p>Named {@code shopstats} rather than {@code stats} so it cannot collide with
 * whatever a server already has.
 */
public final class StatsCommand extends CommandFm {

    private final ShopService service;

    public StatsCommand(UpdraftShop plugin, ShopService service) {
        super(plugin, "shopstats");
        this.service = service;
    }

    @Override
    protected boolean execute(CommandContext context) {
        Player target;

        if (context.isEmpty()) {
            target = context.player();

            if (target == null) {
                service.messages().sendRaw(context.sender(), "players-only");
                return true;
            }
        } else {
            if (!context.sender().hasPermission(Permissions.ADMIN)) {
                service.messages().send(context.sender(), "no-permission");
                return true;
            }

            target = context.playerAt(0);

            if (target == null) {
                service.messages().send(context.sender(), "unknown-player",
                        "{name}", context.arg(0, ""));
                return true;
            }
        }

        if (!target.hasPermission(Permissions.STATS)) {
            service.messages().send(context.sender(), "no-permission");
            return true;
        }

        TradeStats.Entry entry = service.stats().of(target.getUniqueId());

        if (entry.trades() == 0) {
            service.messages().send(context.sender(), "no-stats", "{name}", target.getName());
            return true;
        }

        String name = target.getName();

        service.messages().send(context.sender(), "stats-header", "{name}", name);
        service.messages().send(context.sender(), "stats-trades",
                "{trades}", String.valueOf(entry.trades()));
        service.messages().send(context.sender(), "stats-bought",
                "{count}", String.valueOf(entry.itemsBought()),
                "{price}", service.economy().format(entry.spent()));
        service.messages().send(context.sender(), "stats-sold",
                "{count}", String.valueOf(entry.itemsSold()),
                "{price}", service.economy().format(entry.earned()));
        service.messages().send(context.sender(), "stats-net",
                "{price}", service.economy().format(entry.net()));

        return true;
    }

    @Override
    protected List<String> suggest(CommandContext context) {
        if (context.isEmpty() && context.sender().hasPermission(Permissions.ADMIN)) {
            return CommandContext.onlineNames();
        }

        return List.of();
    }
}