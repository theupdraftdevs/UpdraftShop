package updraftmc.shop.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import updraftmc.shop.ShopService;
import updraftmc.shop.model.ShopCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * The main menu: one icon per configured category.
 */
public final class ShopGUI {

    private ShopGUI() {
    }

    public static void open(ShopService service, Player player) {
        int size = service.registry().gui().mainSize();
        ShopHolder holder = ShopHolder.main();

        Inventory inventory = Bukkit.createInventory(holder, size,
                Style.rootTitle(service.registry().gui().shopName()));
        holder.attach(inventory);

        Layout.fillBorder(inventory);

        List<ShopCategory> categories = service.registry().categories();
        List<Integer> slots = Layout.contentSlots(size);

        for (int index = 0; index < categories.size() && index < slots.size(); index++) {
            ShopCategory category = categories.get(index);

            inventory.setItem(slots.get(index), ItemBuilder.of(category.icon())
                    .hideAttributes()
                    .name(category.displayName())
                    .lore(lore(category, service.registry().items(category.id()).size()))
                    .build());
        }

        inventory.setItem(Layout.bottomSlot(size, 4),
                Style.close(service.icon(service.registry().gui().closeIcon()),
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

    public static void handleClick(ShopService service, Player player, int slot) {
        int size = service.registry().gui().mainSize();

        if (slot == Layout.bottomSlot(size, 4)) {
            player.closeInventory();
            return;
        }

        List<ShopCategory> categories = service.registry().categories();
        List<Integer> slots = Layout.contentSlots(size);
        int index = slots.indexOf(slot);

        if (index >= 0 && index < categories.size()) {
            CategoryGUI.open(service, player, categories.get(index).id());
        }
    }
}