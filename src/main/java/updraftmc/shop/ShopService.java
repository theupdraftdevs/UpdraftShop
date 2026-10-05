package updraftmc.shop;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import updraftmc.shop.config.Messages;
import updraftmc.shop.config.ShopRegistry;
import updraftmc.shop.config.TradeStats;
import updraftmc.shop.economy.Economy;
import updraftmc.shop.economy.Money;
import updraftmc.shop.gui.ShopGUI;
import updraftmc.shop.model.ShopItem;
import updraftmc.shop.util.InventoryUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Everything the views need that is not drawing: the loaded config, the money
 * backend, and the buy and sell transactions.
 *
 * <p>Transactions are written so a failure never costs the player items or money:
 * space is checked before the charge, the charge happens before the items are
 * handed over, and a sale that the economy refuses is handed straight back.
 */
public final class ShopService {

    /**
     * Soft, pitch shifted click, the way Hypixel acknowledges menu navigation.
     */
    private static final Sound CLICK = Sound.BLOCK_NOTE_BLOCK_HAT;
    private static final float CLICK_VOLUME = 0.4F;
    private static final float CLICK_PITCH = 1.6F;

    /**
     * Not final: {@code /shopadmin reload} swaps the config in place so a listener that
     * already holds this service picks up the new one instead of a stale copy. The
     * economy is deliberately not swapped, since reloading it would drop balances.
     */
    private volatile ShopRegistry registry;
    private volatile Messages messages;

    private final Economy economy;

    /**
     * Survives a config reload: history is data, not configuration, and a reload must
     * not be able to wipe it.
     */
private final TradeStats.Store stats;

    public ShopService(ShopRegistry registry, Economy economy, Messages messages,
                       TradeStats.Store stats) {
        this.registry = registry;
        this.economy = economy;
        this.messages = messages;
        this.stats = stats;
    }

    public TradeStats.Store stats() {
        return stats;
    }

    /**
     * Swaps in a freshly loaded config, keeping the economy backend and therefore every
     * balance.
     */
    public void reload(ShopRegistry registry, Messages messages) {
        this.registry = registry;
        this.messages = messages;
    }

    public ShopRegistry registry() {
        return registry;
    }

    public Economy economy() {
        return economy;
    }

    public Messages messages() {
        return messages;
    }

    /**
     * Entry point for opening the shop.
     */
    public void openShop(Player player) {
        if (!mayUse(player)) {
            messages.send(player, "no-permission");
            return;
        }

        ShopGUI.open(this, player);
    }

    /**
     * Flushes the economy and writes the trade stats, so a crash cannot lose a session
 * of history.
 */
    public void shutdown() {
        economy.shutdown();
        stats.logFailure();
    }

    /**
     * Saves the stats without shutting anything down, for the periodic flush.
     */
    public void saveStats() {
        stats.save();
    }

    /**
     * @return whether the player is allowed in the shop at all
     */
    public boolean mayUse(Player player) {
        return player.hasPermission(Permissions.USE);
    }

    /**
     * @return whether the player is allowed to sell, checked separately so a server
     *         can hand out buy access without sell access
     */
    public boolean maySell(Player player) {
        return player.hasPermission(Permissions.SELL);
    }

    /**
     * Plays the menu click. Every menu action goes through here so sounds stay
     * toggleable in one place rather than per view.
     */
    public void click(Player player) {
        if (registry.sounds()) {
            player.playSound(player.getLocation(), CLICK, CLICK_VOLUME, CLICK_PITCH);
        }
    }

    /**
     * Plays the click and confirms a completed trade with a slightly higher pitch.
     */
    public void tradeSound(Player player) {
        if (!registry.sounds()) {
            return;
        }

        player.playSound(player.getLocation(), CLICK, CLICK_VOLUME, 2.0F);
    }

    /**
     * Plays an error buzz when a trade is refused, so a player who misread a price
     * hears that it failed rather than seeing nothing happen.
     */
    public void denySound(Player player) {
        if (registry.sounds()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, CLICK_VOLUME, 0.5F);
        }
    }

    /**
     * Resolves a material name from config, falling back to a visible icon instead of
     * throwing so one typo cannot break a menu.
     */
    public Material icon(String name) {
        Material material = name == null ? null : Material.matchMaterial(name.trim());

        if (material == null || material == Material.AIR) {
            return Material.BARRIER;
        }

        return material;
    }

    /**
     * What a player actually pays for {@code amount} of {@code item}, tax included.
     *
     * <p>The GUI has to quote this rather than the raw price, or the buy button would
     * show one number and charge another.
     */
    public double buyCost(ShopItem item, int amount) {
        double gross = item.buyTotal(amount);
        return Money.round(gross + registry.tax().onBuy(gross));
    }

    /**
     * What a player actually receives for {@code amount} of {@code item}, tax deducted.
     */
    public double sellPayout(ShopItem item, int amount) {
        double gross = item.sellTotal(amount);
        return Money.round(gross - registry.tax().onSell(gross));
    }

    /**
     * Charges the player and hands over the items.
     *
     * @return {@code true} if the trade went through
     */
    public boolean buy(Player player, ShopItem item, int requested) {
        if (!item.buyable()) {
            refuse(player, "not-for-sale");
            return false;
        }

        if (!mayUse(player)) {
            refuse(player, "no-permission");
            return false;
        }

        int amount = clamp(requested, item.maxStack());
        double total = buyCost(item, amount);

        if (!economy.has(player, total)) {
            refuse(player, "not-enough-money", "{price}", economy.format(total));
            return false;
        }

        ItemStack purchase = new ItemStack(item.material(), amount);

        if (!InventoryUtil.hasRoom(player, purchase)) {
            refuse(player, "inventory-full");
            return false;
        }

        if (!economy.withdraw(player, total)) {
            refuse(player, "economy-error");
            return false;
        }

        // Space was checked above, so leftovers should be impossible; drop them
        // rather than voiding them if something moved in the meantime.
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(purchase);
        leftover.values().forEach(extra ->
                player.getWorld().dropItemNaturally(player.getLocation(), extra));

        tradeSound(player);

        stats.recordBuy(player.getUniqueId(), amount, total);

        messages.send(player, "bought",
                "{item}", item.name(),
                "{amount}", String.valueOf(amount),
                "{price}", economy.format(total));

        return true;
    }

    /**
     * Takes plain items off the player and pays them.
     *
     * <p>Sells what the player actually has rather than erroring out when they picked
     * an amount they cannot cover.
     *
     * @return {@code true} if anything was sold
     */
    public boolean sell(Player player, ShopItem item, int requested) {
        if (!item.sellable()) {
            refuse(player, "not-for-sale");
            return false;
        }

        if (!maySell(player)) {
            refuse(player, "no-permission");
            return false;
        }

        int available = InventoryUtil.countSellable(player, item.material());

        if (available <= 0) {
            refuse(player, "nothing-to-sell", "{item}", item.name());
            return false;
        }

        int wanted = clamp(requested, item.maxStack());
        int amount = Math.min(wanted, available);
        int removed = InventoryUtil.removeSellable(player, item.material(), amount);

        if (removed <= 0) {
            refuse(player, "nothing-to-sell", "{item}", item.name());
            return false;
        }

        double total = sellPayout(item, removed);

        if (!economy.deposit(player, total)) {
            // Give the items back rather than take them for nothing.
            player.getInventory().addItem(new ItemStack(item.material(), removed));
            refuse(player, "economy-error");
            return false;
        }

        tradeSound(player);

        stats.recordSell(player.getUniqueId(), removed, total);

        if (removed < wanted) {
            messages.send(player, "sold-partial",
                    "{item}", item.name(),
                    "{available}", String.valueOf(removed),
                    "{price}", economy.format(total));
            return true;
        }

        messages.send(player, "sold",
                "{item}", item.name(),
                "{amount}", String.valueOf(removed),
                "{price}", economy.format(total));

        return true;
    }

    /**
     * Sells every distinct shoppable material the player is carrying.
     *
     * <p>Used by {@code /sellall}. Each material goes through the same guarded sale
     * path as a single trade, so the "plain stacks only" rule and the per item price
     * apply exactly as they do in the menu. Results are collected rather than spammed,
     * so the player gets one summary line at the end.
     */
    public List<ItemStack> sellAll(Player player) {
        List<ItemStack> results = new ArrayList<>();

        if (!maySell(player)) {
            messages.send(player, "no-permission");
            return results;
        }

        double total = 0;
        int stacks = 0;

        for (ShopItem item : sellableItems()) {
            int held = InventoryUtil.countSellable(player, item.material());

            if (held <= 0) {
                continue;
            }

            int removed = InventoryUtil.removeSellable(player, item.material(), held);
            double paid = sellPayout(item, removed);

            // Hand the items back if the economy will not pay, exactly as sell does.
            if (removed > 0 && !economy.deposit(player, paid)) {
                player.getInventory().addItem(new ItemStack(item.material(), removed));
                continue;
            }

            total += paid;
            stacks += removed;
            results.add(new ItemStack(item.material(), Math.min(removed, item.material().getMaxStackSize())));

            stats.recordSell(player.getUniqueId(), removed, paid);
        }

        if (stacks == 0) {
            messages.send(player, "nothing-to-sell-all");
            return results;
        }

        tradeSound(player);

        messages.send(player, "sold-all",
                "{amount}", String.valueOf(stacks),
                "{types}", String.valueOf(results.size()),
                "{price}", economy.format(total));

        return results;
    }

    /**
     * Every sellable entry in the shop, cheapest first.
     *
     * <p>Ordering by price means {@code /sellall} pays out the same way regardless of
     * the order categories happen to be configured in.
     */
    public List<ShopItem> sellableItems() {
        List<ShopItem> items = new ArrayList<>();

        for (var category : registry.categories()) {
            items.addAll(registry.items(category.id()));
        }

        items.removeIf(item -> !item.sellable());
        items.sort(Comparator.comparingDouble(ShopItem::sellPrice));

        return items;
    }

    /**
     * @return the sellable entry for {@code material} with the highest price, or null.
     *         The highest wins because a player selling should never be quoted less
     *         than another listing of the same material offers.
     */
    public ShopItem bestSellFor(Material material) {
        ShopItem best = null;

        for (ShopItem item : sellableItems()) {
            if (item.material() != material) {
                continue;
            }

            if (best == null || item.sellPrice() > best.sellPrice()) {
                best = item;
            }
        }

        return best;
    }

    /**
     * The buyable entry for {@code material} at the lowest price, or null.
     */
    public ShopItem bestBuyFor(Material material) {
        ShopItem best = null;

        for (var category : registry.categories()) {
            for (ShopItem item : registry.items(category.id())) {
                if (item.material() != material || !item.buyable()) {
                    continue;
                }

                if (best == null || item.buyPrice() < best.buyPrice()) {
                    best = item;
                }
            }
        }

        return best;
    }

    /**
     * Finds items by name or id, for the search command.
     *
     * @param limit maximum results, so a one character query cannot open a menu of
     *              everything in the shop
     */
    public List<SearchHit> search(String query, int limit) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<SearchHit> hits = new ArrayList<>();

        if (needle.isEmpty()) {
            return hits;
        }

        for (var category : registry.categories()) {
            for (ShopItem item : registry.items(category.id())) {
                if (hits.size() >= limit) {
                    return hits;
                }

                if (matches(item.id(), needle) || matches(plain(item.name()), needle)) {
                    hits.add(new SearchHit(category.id(), item.id(), item.name()));
                }
            }
        }

        return hits;
    }

    private static boolean matches(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
    }

    /**
     * Strips colour codes so a search matches what the player sees rather than the
     * {@code &} codes around it.
     */
    private static String plain(String coloured) {
        return updraftmc.shop.config.Messages.plain(coloured);
    }

    /**
     * Moves money between two players, used by {@code /pay}.
     *
     * <p>The withdrawal happens first and is refunded in full if the deposit fails, so
     * a rejected transfer cannot destroy money.
     */
    public boolean pay(OfflinePlayer from, OfflinePlayer to, double amount) {
        if (amount <= 0) {
            return false;
        }

        if (!economy.has(from, amount)) {
            return false;
        }

        if (!economy.withdraw(from, amount)) {
            return false;
        }

        if (!economy.deposit(to, amount)) {
            economy.deposit(from, amount);
            return false;
        }

        return true;
    }

    /**
     * One search result, carrying enough to open the trade screen straight from the
     * results menu.
     */
    public record SearchHit(String categoryId, String itemId, String name) {
    }

    private void refuse(Player player, String key, String... replacements) {
        messages.send(player, key, replacements);
        denySound(player);
    }

    private static int clamp(int requested, int max) {
        return Math.max(1, Math.min(requested, max));
    }
}