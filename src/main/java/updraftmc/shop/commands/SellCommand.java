package updraftmc.shop.commands;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import updraftmc.shop.ShopService;
import updraftmc.shop.UpdraftShop;
import updraftmc.shop.model.ShopItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code /sell} sells the item the player is holding, without opening a menu.
 *
 * <p>Looks the held material up across every category, so an item works no matter which
 * category lists it. Ties are resolved towards the better price, so a player is never
 * quoted less than another listing of the same material offers.
 */
public final class SellCommand extends CommandFm {

    private static final String ALL = "all";
    private static final String HAND = "hand";

    private final ShopService service;

    public SellCommand(UpdraftShop plugin, ShopService service) {
        super(plugin, "sell");
        this.service = service;
    }

    @Override
    protected boolean execute(CommandContext context) {
        Player player = context.player();

        if (player == null) {
            service.messages().sendRaw(context.sender(), "players-only");
            return true;
        }

        if (!service.maySell(player)) {
            service.messages().send(player, "no-permission");
            return true;
        }

        if (ALL.equals(context.arg(0, ""))) {
            service.sellAll(player);
            return true;
        }

        ItemStack held = player.getInventory().getItemInMainHand();

        if (held.getType() == Material.AIR || held.getAmount() <= 0) {
            service.messages().send(player, "empty-hand");
            return true;
        }

        ShopItem item = find(held.getType());

        if (item == null) {
            service.messages().send(player, "not-in-shop", "{item}", describe(held));
            return true;
        }

        int amount = amount(context, held.getAmount(), item.maxStack());

        if (amount < 0) {
            service.messages().send(player, "bad-amount", "{amount}", context.raw(0));
            return true;
        }

        service.sell(player, item, amount);
        return true;
    }

    /**
     * Resolves how many to sell.
     *
     * <p>Defaults to the whole held stack, clamped to what the item allows in one
     * trade, so {@code /sell} on a stack of 200 cobble sells the largest legal batch
     * rather than erroring.
     *
     * @return the amount, or -1 when the argument was not a number
     */
    private static int amount(CommandContext context, int held, int maxStack) {
        String raw = context.arg(0, "");

        if (raw.isEmpty() || HAND.equals(raw)) {
            return Math.min(held, maxStack);
        }

        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    /**
     * The entry for {@code material} that pays the most, falling back to the cheapest
     * buyable listing so a buy-only entry still produces the clearer "cannot be sold"
     * reply instead of "not in the shop".
     */
    private ShopItem find(Material material) {
        ShopItem best = service.bestSellFor(material);

        return best != null ? best : service.bestBuyFor(material);
    }

    @Override
    protected List<String> suggest(CommandContext context) {
        if (!context.isEmpty()) {
            return List.of();
        }

        List<String> options = new ArrayList<>();
        options.add(ALL);
        options.add(HAND);

        for (ShopItem item : service.sellableItems()) {
            options.add(item.id());
        }

        return options;
    }

    /**
     * A readable name for an item that is not in the shop at all.
     */
    private static String describe(ItemStack stack) {
        return stack.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}