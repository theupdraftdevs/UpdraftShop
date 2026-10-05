package updraftmc.shop.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import updraftmc.shop.ShopService;

/**
 * Routes clicks on shop inventories.
 *
 * <p>Four kinds of window exist and each is recognised by the holder the view attached
 * to the inventory, never by the window title: the category menu, a category, a trade
 * screen, and a search result list. Keying off the holder means a click can never be
 * routed to the wrong screen by a server that renames the shop or a client that
 * translates its title.
 *
 * <p>Every event is cancelled first: a shop view is decoration, so nothing in it may
 * ever be picked up, swapped or shift-clicked. Clicks in the player's own inventory
 * are left alone so they can still rearrange their backpack while browsing.
 */
public final class GUIListener implements Listener {

    private final ShopService service;

    public GUIListener(ShopService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        ShopHolder holder = holderOf(event);

        if (holder == null) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!isSafeAction(event)) {
            return;
        }

        int slot = event.getRawSlot();

        // Clicks below the top inventory are the player's own items; leave them be.
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }

        // The search window has its own holder type rather than a fourth ShopHolder
        // view, because it carries a result list the other three have no use for.
        if (event.getView().getTopInventory().getHolder() instanceof SearchHolder search) {
            SearchGUI.handleClick(service, player, search, slot);
            return;
        }

        switch (holder.view()) {
            case MAIN -> ShopGUI.handleClick(service, player, holder, slot);
            case CATEGORY -> CategoryGUI.handleClick(service, player, holder, slot);
            case TRADE -> TradeGUI.handleClick(service, player, holder, slot);
        }
    }

    /**
     * Dropping an item into a shop window is the other way to lose something: a
     * player dragging from their inventory onto the menu would otherwise deposit it
     * in a menu that belongs to the plugin.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!isShopWindow(event.getView().getTopInventory())) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();

        for (int slot : event.getRawSlots()) {
            if (slot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Defends against a client that asks for a number-key swap with a different action
     * than {@link #isSafeAction} expects.
     *
     * <p>{@link #onInventoryClick} already cancels before it inspects the action, so
     * this exists as an explicit guard rather than relying on that ordering staying
     * correct: swapping into a shop slot is the one move that could duplicate an item.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onSwapAttempt(InventoryClickEvent event) {
        if (!isShopWindow(event.getView().getTopInventory())) {
            return;
        }

        if (event.getClick() == ClickType.NUMBER_KEY || event.getClick() == ClickType.SWAP_OFFHAND) {
            event.setCancelled(true);
        }
    }

    /**
     * Only plain clicks are forwarded to a view. Everything else (shift-click, number
     * key swap, collect to cursor) stays cancelled so nothing can ever be moved
     * out of a shop slot or dropped into one.
     */
    private static boolean isSafeAction(InventoryClickEvent event) {
        return switch (event.getAction()) {
            case PICKUP_ALL, PICKUP_HALF, PICKUP_SOME, PICKUP_ONE, NOTHING -> true;
            default -> false;
        };
    }

    private static ShopHolder holderOf(InventoryClickEvent event) {
        return holderOf(event.getView().getTopInventory());
    }

    private static ShopHolder holderOf(Inventory inventory) {
        return inventory.getHolder() instanceof ShopHolder holder ? holder : null;
    }

    /**
     * @return whether {@code inventory} belongs to the shop, whether or not it is one of
     *         the three fixed views
     */
    private static boolean isShopWindow(Inventory inventory) {
        return inventory.getHolder() instanceof ShopHolder || inventory.getHolder() instanceof SearchHolder;
    }
}