package updraftmc.shop.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import updraftmc.shop.ShopService;
import updraftmc.shop.model.ShopCategory;
import updraftmc.shop.util.Pagination;

import java.util.ArrayList;
import java.util.List;

/**
 * The main menu: one icon per configured category, plus the player's balance.
 */
public final class ShopGUI {

    private ShopGUI() {
    }

    public static void open(ShopService service, Player player) {
        open(service, player, 1);
    }

    public static void open(ShopService service, Player player, int requestedPage) {
        int size = service.registry().gui().mainSize();
        List<ShopCategory> categories = service.registry().categories();
        List<Integer> slots = Layout.contentSlots(size);

        int pages = Pagination.pages(categories.size(), slots.size());
        int page = Pagination.clamp(requestedPage, categories.size(), slots.size());

        ShopHolder holder = ShopHolder.main(page);

        Inventory inventory = Bukkit.createInventory(holder, size,
                Style.rootTitle(service.registry().gui().shopName())
                        .append(Style.pageSuffix(page, pages)));
        holder.attach(inventory);

        Layout.fillBorder(inventory);

        List<ShopCategory> visible = Pagination.slice(categories, page, slots.size());

        for (int index = 0; index < visible.size(); index++) {
            ShopCategory category = visible.get(index);

            inventory.setItem(slots.get(index), ItemBuilder.of(category.icon())
                    .hideAttributes()
                    .name(category.displayName())
                    .lore(lore(category, service.registry().items(category.id()).size()))
                    .build());
        }

        boolean hasPrevious = Pagination.hasPrevious(page);
        boolean hasNext = Pagination.hasNext(page, categories.size(), slots.size());

        Controls.drawPages(inventory, page, pages, hasPrevious, hasNext);

        inventory.setItem(Layout.bottomSlot(size, Layout.HEAD), Style.balance(
                service, player, pages > 1));

        inventory.setItem(Layout.bottomSlot(size, Layout.CLOSE), Style.close(
                service.icon(service.registry().gui().closeIcon()),
                service.registry().gui().closeItem()));

        player.openInventory(inventory);
    }

    private static String[] lore(ShopCategory category, int itemCount) {
        List<String> lore = new ArrayList<>(category.description());
        lore.add("");
        lore.add("&7Items: &f" + itemCount);
        lore.add("");
        lore.add(Style.CLICK_BROWSE);
        return lore.toArray(new String[0]);
    }

    public static void handleClick(ShopService service, Player player, ShopHolder holder, int slot) {
        int size = service.registry().gui().mainSize();
        Inventory inventory = holder.getInventory();
        int page = holder.page();

        List<ShopCategory> categories = service.registry().categories();
        List<Integer> slots = Layout.contentSlots(size);

        boolean hasPrevious = Pagination.hasPrevious(page);
        boolean hasNext = Pagination.hasNext(page, categories.size(), slots.size());

        if (Controls.isControl(inventory, slot)) {
            if (slot == Controls.previousSlot(inventory, hasPrevious)) {
                service.click(player);
                open(service, player, page - 1);
                return;
            }

            if (slot == Controls.nextSlot(inventory, hasNext)) {
                service.click(player);
                open(service, player, page + 1);
                return;
            }

            if (slot == Layout.bottomSlot(size, Layout.CLOSE)) {
                player.closeInventory();
                return;
            }

            // The balance head is information only.
            if (slot == Layout.bottomSlot(size, Layout.HEAD)) {
                service.click(player);
                open(service, player, page);
            }

            return;
        }

        int index = slots.indexOf(slot);

        if (index < 0) {
            return;
        }

        List<ShopCategory> visible = Pagination.slice(categories, page, slots.size());

        if (index < visible.size()) {
            service.click(player);
            CategoryGUI.open(service, player, visible.get(index).id());
        }
    }
}