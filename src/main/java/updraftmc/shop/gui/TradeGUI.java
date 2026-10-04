package updraftmc.shop.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import updraftmc.shop.ShopService;
import updraftmc.shop.model.ShopItem;
import updraftmc.shop.util.InventoryUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Buy and sell screen for a single item.
 *
 * <p>A confirmation step exists on purpose: a single misclick should not cost a
 * player 4000 dollars, so nothing is charged until a trade button is pressed.
 */
public final class TradeGUI {

    private static final int SIZE = 54;
    private static final int PREVIEW_SLOT = 4;
    private static final int SUMMARY_SLOT = 31;
    private static final int BUY_SLOT = 39;
    private static final int SELL_SLOT = 41;
    private static final int MAX_SLOT = 24;

    private static final int[] AMOUNT_SLOTS = {19, 20, 21, 22, 23};
    private static final int[] AMOUNTS = {1, 8, 16, 32, 64};

    private TradeGUI() {
    }

    public static void open(ShopService service, Player player, String categoryId, String itemId) {
        ShopItem item = service.registry().item(categoryId, itemId);

        if (item == null) {
            service.messages().send(player, "unknown-item");
            return;
        }

        ShopHolder holder = ShopHolder.trade(categoryId, itemId);
        holder.amount(1);

        Inventory inventory = Bukkit.createInventory(holder, SIZE,
                Style.nestedTitle(service.registry().gui().shopName(), item.name()));
        holder.attach(inventory);

        render(service, player, holder);

        player.openInventory(inventory);
    }

    /**
     * Redraws the screen in place. Runs on every click so the summary always reflects
     * the live balance and the selected amount.
     */
    private static void render(ShopService service, Player player, ShopHolder holder) {
        Inventory inventory = holder.getInventory();

        if (inventory == null) {
            return;
        }

        ShopItem item = service.registry().item(holder.categoryId(), holder.itemId());

        if (item == null) {
            player.closeInventory();
            return;
        }

        Layout.fillBorder(inventory);

        int limit = limit(service, player, item);
        int amount = Math.min(holder.amount(), limit);
        holder.amount(amount);

        inventory.setItem(PREVIEW_SLOT, preview(service, item, amount));
        inventory.setItem(SUMMARY_SLOT, summary(service, player, item, amount));

        for (int index = 0; index < AMOUNT_SLOTS.length; index++) {
            inventory.setItem(AMOUNT_SLOTS[index], amountButton(AMOUNTS[index], amount, AMOUNTS[index] > limit));
        }

        inventory.setItem(MAX_SLOT, amountButton(limit, amount, false));
        inventory.setItem(BUY_SLOT, buyButton(service, player, item, amount));
        inventory.setItem(SELL_SLOT, sellButton(service, player, item, amount));

        inventory.setItem(Layout.bottomSlot(SIZE, 3), Style.back(
                service.icon(service.registry().gui().backIcon()),
                service.registry().gui().backItem()));

        inventory.setItem(Layout.bottomSlot(SIZE, 5), Style.close(
                service.icon(service.registry().gui().closeIcon()),
                service.registry().gui().closeItem()));
    }

    /**
     * Largest amount the player can actually trade right now: whatever they can
     * afford for a buy, or whatever they are holding for a sell.
     */
    private static int limit(ShopService service, Player player, ShopItem item) {
        int limit = item.maxStack();

        if (item.buyable()) {
            double affordable = service.economy().getBalance(player) / item.buyPrice();
            limit = Math.min(limit, (int) Math.max(0, Math.min(Math.floor(affordable), Integer.MAX_VALUE)));
        }

        if (item.sellable()) {
            int held = InventoryUtil.countSellable(player, item.material());
            limit = Math.max(limit, Math.min(item.maxStack(), held));
        }

        return Math.max(1, limit);
    }

    private static ItemStack preview(ShopService service, ShopItem item, int amount) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Amount: &f" + amount);

        if (item.buyable()) {
            lore.add(Style.buyLine(service.economy().format(item.buyTotal(amount))));
        }

        if (item.sellable()) {
            lore.add(Style.sellLine(service.economy().format(item.sellTotal(amount))));
        }

        return ItemBuilder.of(item.material())
                .hideAttributes()
                .amount(Math.min(amount, item.material().getMaxStackSize()))
                .name("&e&l" + Style.upper(item.name()))
                .lore(lore.toArray(new String[0]))
                .build();
    }

    private static ItemStack summary(ShopService service, Player player, ShopItem item, int amount) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Amount: &f" + amount);

        if (item.buyable()) {
            lore.add("&7Total cost: &a" + service.economy().format(item.buyTotal(amount)));
        }

        if (item.sellable()) {
            lore.add("&7You receive: &a" + service.economy().format(item.sellTotal(amount)));
        }

        lore.add("");
        lore.add("&7Balance: &e" + service.economy().format(service.economy().getBalance(player)));

        return ItemBuilder.of(Material.PAPER)
                .name("&e&lOrder Summary")
                .lore(lore.toArray(new String[0]))
                .build();
    }

    private static ItemStack amountButton(int value, int selected, boolean outOfReach) {
        boolean active = value == selected;
        String amount = value == 1 ? "1" : String.valueOf(value);

        return ItemBuilder.of(active ? Material.LIME_DYE : Material.GRAY_DYE)
                .name((active ? "&a&l" : "&7") + "x" + amount)
                .lore(outOfReach ? "&cYou cannot trade this many" : "&7Click to select")
                .build();
    }

    private static ItemStack buyButton(ShopService service, Player player, ShopItem item, int amount) {
        if (!item.buyable()) {
            return ItemBuilder.of(Material.BARRIER)
                    .name("&c&lSell Only")
                    .lore("&7This item cannot be bought.")
                    .build();
        }

        return ItemBuilder.of(Material.LIME_CONCRETE)
                .name("&a&lBUY &7- &a&l" + service.economy().format(item.buyTotal(amount)))
                .lore("&7Amount: &f" + amount,
                        "&7Balance: &e" + service.economy().format(service.economy().getBalance(player)),
                        "",
                        Style.CLICK_PURCHASE)
                .build();
    }

    private static ItemStack sellButton(ShopService service, Player player, ShopItem item, int amount) {
        if (!item.sellable()) {
            return ItemBuilder.of(Material.BARRIER)
                    .name("&c&lBuy Only")
                    .lore("&7This item cannot be sold.")
                    .build();
        }

        int held = InventoryUtil.countSellable(player, item.material());
        int sellable = Math.min(amount, held);

        List<String> lore = new ArrayList<>();
        lore.add("&7Amount: &f" + sellable);
        lore.add("&7You hold: &f" + held);
        lore.add("");

        if (sellable < amount) {
            lore.add("&cOnly " + sellable + " will be sold");
            lore.add("");
        }

        lore.add("&7Click to &aSell");

        return ItemBuilder.of(sellable > 0 ? Material.RED_CONCRETE : Material.BARRIER)
                .name("&c&lSELL &7- &a&l" + service.economy().format(item.sellTotal(sellable)))
                .lore(lore.toArray(new String[0]))
                .build();
    }

    public static void handleClick(ShopService service, Player player, ShopHolder holder, int slot) {
        ShopItem item = service.registry().item(holder.categoryId(), holder.itemId());

        if (item == null) {
            player.closeInventory();
            return;
        }

        if (slot == Layout.bottomSlot(SIZE, 3)) {
            CategoryGUI.open(service, player, holder.categoryId());
            return;
        }

        if (slot == Layout.bottomSlot(SIZE, 5)) {
            player.closeInventory();
            return;
        }

        for (int index = 0; index < AMOUNT_SLOTS.length; index++) {
            if (AMOUNT_SLOTS[index] == slot) {
                holder.amount(AMOUNTS[index]);
                service.click(player);
                render(service, player, holder);
                return;
            }
        }

        if (slot == MAX_SLOT) {
            holder.amount(limit(service, player, item));
            service.click(player);
            render(service, player, holder);
            return;
        }

        if (slot == BUY_SLOT) {
            service.buy(player, item, holder.amount());
            render(service, player, holder);
            return;
        }

        if (slot == SELL_SLOT) {
            service.sell(player, item, holder.amount());
            render(service, player, holder);
        }
    }
}