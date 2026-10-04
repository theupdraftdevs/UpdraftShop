package updraftmc.shop.commands;

import org.bukkit.entity.Player;
import updraftmc.shop.ShopService;
import updraftmc.shop.UpdraftShop;

/**
 * {@code /shop} opens the category menu.
 */
public final class ShopCommand extends CommandFm {

    private final ShopService service;

    public ShopCommand(UpdraftShop plugin, ShopService service) {
        super(plugin, "shop");
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
