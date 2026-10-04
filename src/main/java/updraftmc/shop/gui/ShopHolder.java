package updraftmc.shop.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marks an inventory as part of the shop so clicks can be routed by type.
 *
 * <p>Clicks used to be matched on the inventory title, which breaks the moment two
 * views share a prefix and cannot be translated by clients. Keying off the holder is
 * both exact and locale independent.
 *
 * <p>The holder also carries the per-view state a screen needs to remember between
 * clicks, which for the trade view is the currently selected amount.
 */
public final class ShopHolder implements InventoryHolder {

    public enum View {
        MAIN,
        CATEGORY,
        TRADE
    }

    private final View view;
    private final String categoryId;
    private final String itemId;

    private Inventory inventory;
    private int amount = 1;

    private ShopHolder(View view, String categoryId, String itemId) {
        this.view = view;
        this.categoryId = categoryId;
        this.itemId = itemId;
    }

    public static ShopHolder main() {
        return new ShopHolder(View.MAIN, null, null);
    }

    public static ShopHolder category(String categoryId) {
        return new ShopHolder(View.CATEGORY, categoryId, null);
    }

    public static ShopHolder trade(String categoryId, String itemId) {
        return new ShopHolder(View.TRADE, categoryId, itemId);
    }

    /**
     * Called once the inventory has been created, since {@link #getInventory()} has to
     * return the very inventory this holder was used to build.
     */
    public void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public View view() {
        return view;
    }

    public String categoryId() {
        return categoryId;
    }

    public String itemId() {
        return itemId;
    }

    public int amount() {
        return amount;
    }

    public void amount(int amount) {
        this.amount = Math.max(1, amount);
    }
}