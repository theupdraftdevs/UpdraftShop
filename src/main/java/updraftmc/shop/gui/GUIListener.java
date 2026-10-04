package updraftmc.shop.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import updraftmc.shop.ShopService;

/**
 * Routes clicks on shop inventories.
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

        switch (holder.view()) {
            case MAIN -> ShopGUI.handleClick(service, player, slot);
            case CATEGORY -> CategoryGUI.handleClick(service, player, holder.categoryId(), slot);
            case TRADE -> TradeGUI.handleClick(service, player, holder, slot);
        }
    }

    /**
     * Dragging across the shop would otherwise drop items into it.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        ShopHolder holder = holderOf(event);

        if (holder == null) {
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
        return event.getView().getTopInventory().getHolder() instanceof ShopHolder holder ? holder : null;
    }

    private static ShopHolder holderOf(InventoryDragEvent event) {
        return event.getView().getTopInventory().getHolder() instanceof ShopHolder holder ? holder : null;
    }
}