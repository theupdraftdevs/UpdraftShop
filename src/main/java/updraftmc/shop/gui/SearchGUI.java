package updraftmc.shop.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import updraftmc.shop.ShopService;
import updraftmc.shop.util.Pagination;

import java.util.ArrayList;
import java.util.List;

/**
 * Results of {@code /shop <query>}.
 *
 * <p>A shop with a hundred items cannot be navigated by memory, so a search that finds
 * more than one match opens a menu of them rather than complaining that the query was
 * ambiguous.
 */
public final class SearchGUI {

    private SearchGUI() {
    }

    /**
     * Draws the results menu and opens it.
     *
     * @return the inventory that was opened
     */
    public static Inventory open(ShopService service, Player player, String query,
                                 List<ShopService.SearchHit> hits) {

        SearchHolder holder = SearchHolder.create(query, hits);

        Inventory inventory = Bukkit.createInventory(holder, sizeFor(hits.size()),
                Style.rootTitle(service.registry().gui().shopName())
                        .append(Style.searchSuffix(query, hits.size())));
        holder.attach(inventory);

        draw(service, player, holder);

        player.openInventory(inventory);
        return inventory;
    }

    /**
     * Redraws {@code holder} in place, on whatever page it is currently showing.
     *
     * <p>Redrawing rather than building a new inventory keeps the player's cursor,
     * scroll position and open window, so paging does not feel like the menu is being
     * thrown away and rebuilt.
     */
    private static void draw(ShopService service, Player player, SearchHolder holder) {
        Inventory inventory = holder.getInventory();

        if (inventory == null) {
            return;
        }

        inventory.clear();
        Layout.fillBorder(inventory);

        int size = inventory.getSize();
        List<Integer> slots = Layout.contentSlots(size);
        List<ShopService.SearchHit> visible = Pagination.slice(holder.hits(), holder.page(), slots.size());

        for (int index = 0; index < visible.size(); index++) {
            inventory.setItem(slots.get(index), result(service, visible.get(index)));
        }

        int pages = Pagination.pages(holder.hits().size(), slots.size());

        Controls.drawPages(inventory, holder.page(), pages,
                Pagination.hasPrevious(holder.page()),
                Pagination.hasNext(holder.page(), holder.hits().size(), slots.size()));

        inventory.setItem(Layout.bottomSlot(size, Layout.CLOSE), Style.close(
                service.icon(service.registry().gui().closeIcon()),
                service.registry().gui().closeItem()));

        }

    /**
     * @return the smallest menu that can hold every hit on one page, so a search with
     *         three results does not open a mostly empty 54 slot window. Once there are
     *         more hits than that the menu stays at 54 and pages instead.
     */
    private static int sizeFor(int hits) {
        return Layout.validSize(hits);
    }

    private static ItemStack result(ShopService service, ShopService.SearchHit hit) {
        var item = service.registry().item(hit.categoryId(), hit.itemId());

        if (item == null) {
            // The shop was reloaded and this entry no longer exists.
            return ItemBuilder.of(Material.BARRIER)
                    .name("&c" + hit.name())
                    .lore("&7No longer sold")
                    .build();
        }

        List<String> lore = new ArrayList<>();

        if (item.buyable()) {
            lore.add(Style.buyLine(service.economy().format(service.buyCost(item, 1))));
        }

        if (item.sellable()) {
            lore.add(Style.sellLine(service.economy().format(service.sellPayout(item, 1))));
        }

        lore.add("");
        lore.add(Style.CLICK_BROWSE);

        return ItemBuilder.of(item.material())
                .hideAttributes()
                .name(item.name())
                .lore(lore.toArray(new String[0]))
                .build();
    }

    /**
     * Opens the trade screen for whichever result was clicked.
     */
    public static void handleClick(ShopService service, Player player, SearchHolder holder, int slot) {
        Inventory inventory = holder.getInventory();

        if (inventory == null) {
            player.closeInventory();
            return;
        }

        int size = inventory.getSize();

        if (slot == Layout.bottomSlot(size, Layout.CLOSE)) {
            player.closeInventory();
            return;
        }

        List<Integer> slots = Layout.contentSlots(size);
        int pages = Pagination.pages(holder.hits().size(), slots.size());

        if (slot == Controls.previousSlot(inventory, Pagination.hasPrevious(holder.page()))) {
            service.click(player);
            holder.page(holder.page() - 1, pages);
            draw(service, player, holder);
            return;
        }

        if (slot == Controls.nextSlot(inventory,
                Pagination.hasNext(holder.page(), holder.hits().size(), slots.size()))) {
            service.click(player);
            holder.page(holder.page() + 1, pages);
            draw(service, player, holder);
            return;
        }

        int index = slots.indexOf(slot);

        if (index < 0) {
            return;
        }

        // Index against the visible page, not the whole list, or a click on the second
        // page would open whatever happens to sit at that offset on the first.
        ShopService.SearchHit hit = Pagination.slice(holder.hits(), holder.page(), slots.size())
                .get(index);

        if (hit == null) {
            return;
        }

        service.click(player);
        TradeGUI.open(service, player, hit.categoryId(), hit.itemId());
    }
}