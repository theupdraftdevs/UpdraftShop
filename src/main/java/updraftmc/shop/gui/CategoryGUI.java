package updraftmc.shop.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import updraftmc.shop.ShopService;
import updraftmc.shop.model.ShopCategory;
import updraftmc.shop.model.ShopItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists every tradeable item in one category.
 *
 * <p>Categories no longer come from a hardcoded switch statement: whatever is in
 * config.yml gets a screen here, which is what the old {@code custom} menu was
 * missing.
 */
public final class CategoryGUI {

    private CategoryGUI() {
    }

    public static void open(ShopService service, Player player, String categoryId) {
        ShopCategory category = service.registry().category(categoryId);

        if (category == null) {
            service.messages().send(player, "unknown-item");
            return;
        }

        ShopHolder holder = ShopHolder.category(category.id());

        Inventory inventory = Bukkit.createInventory(holder, category.size(),
                Style.nestedTitle(service.registry().gui().shopName(), category.displayName()));
        holder.attach(inventory);

        Layout.fillBorder(inventory);

        List<ShopItem> items = service.registry().items(category.id());
        List<Integer> slots = Layout.contentSlots(category.size());

        for (int index = 0; index < items.size() && index < slots.size(); index++) {
            inventory.setItem(slots.get(index), icon(service, items.get(index)));
        }

        inventory.setItem(Layout.bottomSlot(category.size(), 4),
                Style.back(service.icon(service.registry().gui().backIcon()),
                        service.registry().gui().backItem()));

        player.openInventory(inventory);
    }

    private static ItemStack icon(ShopService service, ShopItem item) {
        List<String> lore = new ArrayList<>();

        if (item.buyable()) {
            lore.add(Style.buyLine(service.economy().format(item.buyPrice())));
        }

        if (item.sellable()) {
            lore.add(Style.sellLine(service.economy().format(item.sellPrice())));
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

    public static void handleClick(ShopService service, Player player, String categoryId, int slot) {
        ShopCategory category = service.registry().category(categoryId);

        if (category == null) {
            player.closeInventory();
            return;
        }

        if (slot == Layout.bottomSlot(category.size(), 4)) {
            ShopGUI.open(service, player);
            return;
        }

        List<ShopItem> items = service.registry().items(categoryId);
        List<Integer> slots = Layout.contentSlots(category.size());
        int index = slots.indexOf(slot);

        if (index >= 0 && index < items.size()) {
            TradeGUI.open(service, player, categoryId, items.get(index).id());
        }
    }
}