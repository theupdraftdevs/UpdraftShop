package updraftmc.shop.commands;

import org.bukkit.entity.Player;
import updraftmc.shop.ShopService;
import updraftmc.shop.UpdraftShop;
import updraftmc.shop.gui.SearchGUI;
import updraftmc.shop.model.ShopItem;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /shop} opens the category menu, with a search shortcut for large shops.
 *
 * <p>{@code /shop <query>} opens a results menu rather than a category, because on a
 * server with a hundred items nobody remembers which category an item sits in.
 */
public final class ShopCommand extends CommandFm {

    /** Enough results to be useful without building an unreadable menu. */
    private static final int SEARCH_LIMIT = 45;

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

        if (!service.mayUse(player)) {
            service.messages().send(player, "no-permission");
            return true;
        }

        if (context.isEmpty()) {
            service.openShop(player);
            return true;
        }

        search(player, context.arg(0, ""));
        return true;
    }

    /**
     * Opens the results for {@code query}, or falls back to the plain menu when
     * nothing matched, so a typo does not leave the player with nothing open.
     */
    private void search(Player player, String query) {
        List<ShopService.SearchHit> hits = service.search(query, SEARCH_LIMIT);

        if (hits.isEmpty()) {
            service.messages().send(player, "no-search-results", "{query}", query);
            return;
        }

        if (hits.size() == 1) {
            // An unambiguous match is what they meant, so skip the extra click.
            ShopService.SearchHit only = hits.getFirst();
            SearchGUI.open(service, player, query, List.of(only));
            return;
        }

        SearchGUI.open(service, player, query, hits);
    }

    @Override
    protected List<String> suggest(CommandContext context) {
        if (!context.isEmpty()) {
            return List.of();
        }

        List<String> ids = new ArrayList<>();

        service.registry().categories().forEach(category -> {
            for (ShopItem item : service.registry().items(category.id())) {
                if (ids.size() < SEARCH_LIMIT) {
                    ids.add(item.id());
                }
            }
        });

        return ids;
    }
}