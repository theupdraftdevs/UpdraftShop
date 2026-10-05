package updraftmc.shop.commands;

import updraftmc.shop.ShopService;
import updraftmc.shop.UpdraftShop;
import updraftmc.shop.config.Messages;

/**
 * {@code /shophelp} lists the commands and what they do.
 *
 * <p>Deliberately not {@code /help}: a server is very likely to own {@code /help} or a
 * help plugin already, and claiming it would be rude.
 */
public final class HelpCommand extends CommandFm {

    private final ShopService service;

    public HelpCommand(UpdraftShop plugin, ShopService service) {
        super(plugin, "shophelp");
        this.service = service;
    }

    @Override
    protected boolean execute(CommandContext context) {
        Messages messages = service.messages();

        messages.sendRaw(context.sender(), "help-header");
        messages.sendRaw(context.sender(), "help-shop");
        messages.sendRaw(context.sender(), "help-balance");
        messages.sendRaw(context.sender(), "help-pay");
        messages.sendRaw(context.sender(), "help-sell");
        messages.sendRaw(context.sender(), "help-stats");
        messages.sendRaw(context.sender(), "help-help");

        return true;
    }
}