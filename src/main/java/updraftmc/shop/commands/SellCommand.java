package updraftmc.shop.commands;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import updraftmc.shop.ShopService;
import updraftmc.shop.UpdraftShop;
import updraftmc.shop.model.ShopCategory;
import updraftmc.shop.model.ShopItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code /sell} sells the item the player is holding, without opening a menu.
 *
 * <p>Looks the held material up across every category, so an item works no matter which
 * category lists it. Ties are resolved in config order, which keeps the price stable
 * when the same material is deliberately listed twice at different prices.
 */
public final class SellCommand extends CommandFm {

    private static final String ALL = "all";

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

        if (!item.sellable()) {
            service.messages().send(player, "not-for-sale");
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
     */
    private static int amount(CommandContext context, int held, int maxStack) {
        if (context.isEmpty()) {
            return Math.min(held, maxStack);
        }

        String raw = context.arg(0, "");

        if (ALL.equals(raw)) {
            return maxStack;
        }

        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            // Not a number: fall through and let the caller report it.
            return -1;
        }
    }

    /**
     * First sellable entry for {@code material} across all categories.
     */
    private ShopItem find(Material material) {
        ShopItem fallback = null;

        for (ShopCategory category : service.registry().categories()) {
            for (ShopItem item : service.registry().items(category.id())) {
                if (item.material() != material) {
                    continue;
                }

                // Prefer something actually sellable, but remember the first match so a
                // buy-only listing still produces the clearer "cannot be sold" reply
                // instead of "not in the shop".
                if (item.sellable()) {
                    return item;
                }

                if (fallback == null) {
                    fallback = item;
                }
            }
        }

        return fallback;
    }

    @Override
    protected List<String> suggest(CommandContext context) {
        if (!context.isEmpty()) {
            return List.of();
        }

        List<String> options = new ArrayList<>();
        options.add(ALL);

        for (ShopCategory category : service.registry().categories()) {
            for (ShopItem item : service.registry().items(category.id())) {
                if (item.sellable()) {
                    options.add(item.id());
                }
            }
        }

        return options;
    }

    /**
     * A readable name for an item that is not in the shop at all.
     */
    private static String describe(@NotNull ItemStack stack) {
        return stack.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
