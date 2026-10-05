package updraftmc.shop.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import updraftmc.shop.ShopService;
import updraftmc.shop.UpdraftShop;

/**
 * {@code /updraftshop} is the plugin's own name in chat.
 *
 * <p>Opens the shop for a player, and from the console prints what is installed: the
 * version, how many entries loaded, and which economy is holding balances. That is
 * usually the first thing somebody needs when the shop is not behaving and they are
 * staring at a console.
 *
 * <p>It exists mainly for servers whose own {@code /shop} is already taken.
 */
public final class MainCommand extends CommandFm {

    private final UpdraftShop plugin;
    private final ShopService service;

    public MainCommand(UpdraftShop plugin, ShopService service) {
        super(plugin, "updraftshop");
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    protected boolean execute(CommandContext context) {
        Player player = context.player();

        if (player == null) {
            context.sender().sendMessage(Component.text("UpdraftShop ", NamedTextColor.GRAY)
                    .append(Component.text(plugin.getPluginMeta().getVersion(), NamedTextColor.WHITE))
                    .append(Component.text(" - " + service.registry().entryCount()
                            + " entries, economy: " + service.economy().providerName(),
                            NamedTextColor.DARK_GRAY)));

            return true;
        }

        service.openShop(player);
        return true;
    }
}