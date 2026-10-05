package updraftmc.shop.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import updraftmc.shop.ShopService;
import updraftmc.shop.model.ShopCategory;
import updraftmc.shop.model.ShopItem;
import updraftmc.shop.util.Pagination;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists every tradeable item in one category, a page at a time.
 *
 * <p>Categories no longer come from a hardcoded switch statement: whatever is in
 * config.yml gets a screen here, which is what the old {@code custom} menu was
 * missing. A category with more items than fit now pages instead of quietly hiding
 * the overflow.
 */
public final class CategoryGUI {

    private CategoryGUI() {
    }

    public static void open(ShopService service, Player player, String categoryId) {
        open(service, player, categoryId, 1);
    }

    public static void open(ShopService service, Player player, String categoryId, int requestedPage) {
        ShopCategory category = service.registry().category(categoryId);

        if (category == null) {
            service.messages().send(player, "unknown-item");
            return;
        }

        List<ShopItem> items = service.registry().items(category.id());
        List<Integer> slots = Layout.contentSlots(category.size());

        int pages = Pagination.pages(items.size(), slots.size());
        int page = Pagination.clamp(requestedPage, items.size(), slots.size());

        ShopHolder holder = ShopHolder.category(category.id(), page);

        Inventory inventory = Bukkit.createInventory(holder, category.size(),
                Style.nestedTitle(service.registry().gui().shopName(), category.displayName())
                        .append(Style.pageSuffix(page, pages)));
        holder.attach(inventory);

        Layout.fillBorder(inventory);

        List<ShopItem> visible = Pagination.slice(items, page, slots.size());

        for (int index = 0; index < visible.size(); index++) {
            inventory.setItem(slots.get(index), icon(service, visible.get(index)));
        }

        Controls.drawPages(inventory, page, pages,
                Pagination.hasPrevious(page),
                Pagination.hasNext(page, items.size(), slots.size()));

        inventory.setItem(Layout.bottomSlot(category.size(), Layout.BACK), Style.back(
                service.icon(service.registry().gui().backIcon()),
                service.registry().gui().backItem()));

        inventory.setItem(Layout.bottomSlot(category.size(), Layout.CLOSE), Style.close(
                service.icon(service.registry().gui().closeIcon()),
                service.registry().gui().closeItem()));

        player.openInventory(inventory);
    }

    private static ItemStack icon(ShopService service, ShopItem item) {
        List<String> lore = new ArrayList<>();

        if (item.buyable()) {
            lore.add(Style.buyLine(service.economy().format(service.buyCost(item, 1))));
        }

        if (item.sellable()) {
            lore.add(Style.sellLine(service.economy().format(service.sellPayout(item, 1))));
        }

        lore.add("");
        lore.add("&7Max per trade: &f" + item.maxStack());
        lore.add("");
        lore.add(Style.CLICK_PURCHASE);

        return ItemBuilder.of(item.material())
                .hideAttributes()
                .name(item.name())
                .lore(lore.toArray(new String[0]))
                .build();
    }

    public static void handleClick(ShopService service, Player player, ShopHolder holder, int slot) {
        ShopCategory category = service.registry().category(holder.categoryId());

        if (category == null) {
            player.closeInventory();
            return;
        }

        Inventory inventory = holder.getInventory();

        if (inventory == null) {
            player.closeInventory();
            return;
        }

        int page = holder.page();
        List<ShopItem> items = service.registry().items(category.id());
        List<Integer> slots = Layout.contentSlots(category.size());

        if (Controls.isControl(inventory, slot)) {
            if (slot == Controls.previousSlot(inventory, Pagination.hasPrevious(page))) {
                service.click(player);
                open(service, player, category.id(), page - 1);
                return;
            }

            if (slot == Controls.nextSlot(inventory,
                    Pagination.hasNext(page, items.size(), slots.size()))) {
                service.click(player);
                open(service, player, category.id(), page + 1);
                return;
            }

            if (slot == Layout.bottomSlot(category.size(), Layout.BACK)) {
                service.click(player);
                ShopGUI.open(service, player);
                return;
            }

            if (slot == Layout.bottomSlot(category.size(), Layout.CLOSE)) {
                player.closeInventory();
            }

            return;
        }

        int index = slots.indexOf(slot);

        if (index < 0) {
            return;
        }

        List<ShopItem> visible = Pagination.slice(items, page, slots.size());

        if (index < visible.size()) {
            service.click(player);
            TradeGUI.open(service, player, category.id(), visible.get(index).id());
        }
    }
}