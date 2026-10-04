package updraftmc.shop.commands;

import org.bukkit.entity.Player;
import updraftmc.shop.ShopService;
import updraftmc.shop.UpdraftShop;

/**
 * {@code /updraftshop} is the plugin's main entry point and opens the shop, the same
 * as {@code /shop}. Kept as a separate command so the plugin can be named in chat
 * without colliding with a server's own {@code /shop}.
 */
public final class MainCommand extends CommandFm {

    private final ShopService service;

    public MainCommand(UpdraftShop plugin, ShopService service) {
        super(plugin, "updraftshop");
        this.service = service;
    }

    @Override
    protected boolean execute(CommandContext context) {
        Player player = context.player();

        if (player == null) {
            service.messages().sendRaw(context.sender(), "players-only");
            return true;
        }

        service.openShop(player);
        return true;
    }
}
