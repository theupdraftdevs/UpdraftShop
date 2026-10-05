package updraftmc.shop.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import updraftmc.shop.ShopService;

import java.util.List;

/**
 * Holds the results of a search so a click can be routed to the right item.
 *
 * <p>Search results are not one of the three fixed views, so this has its own holder
 * type rather than forcing {@link ShopHolder} to grow a fourth enum constant and a
 * pair of fields that mean nothing in every other view.
 */
public final class SearchHolder implements InventoryHolder {

    private final String query;
    private final List<ShopService.SearchHit> hits;

    private Inventory inventory;

    /**
     * Which page is currently drawn.
     *
     * <p>Mutable on purpose: the inventory a player is looking at is the same object
     * every time they page through it, so redrawing in place is what keeps the arrow
     * clicking working. A new inventory per page would need a new holder and a way to
     * find it again, which is more state than the problem deserves.
     */
    private int page = 1;

    private SearchHolder(String query, List<ShopService.SearchHit> hits) {
        this.query = query;
        this.hits = List.copyOf(hits);
    }

    public static SearchHolder create(String query, List<ShopService.SearchHit> hits) {
        return new SearchHolder(query, hits);
    }

    public int page() {
        return page;
    }

    /**
     * Moves to {@code requested}, clamped to a page that exists.
     *
     * <p>The clamping is what makes a stale arrow click harmless: paging forwards from
     * the last page lands back on the last page rather than on an empty one.
     */
    public void page(int requested, int pages) {
        this.page = Math.max(1, Math.min(requested, pages));
    }

    public void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public String query() {
        return query;
    }

    public List<ShopService.SearchHit> hits() {
        return hits;
    }
}