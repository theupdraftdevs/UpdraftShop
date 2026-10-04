package updraftmc.shop;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import updraftmc.shop.config.Messages;
import updraftmc.shop.config.ShopRegistry;
import updraftmc.shop.economy.Economy;
import updraftmc.shop.gui.ShopGUI;
import updraftmc.shop.model.ShopItem;
import updraftmc.shop.util.InventoryUtil;

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

    private final ShopRegistry registry;
    private final Economy economy;
    private final Messages messages;

    public ShopService(ShopRegistry registry, Economy economy, Messages messages) {
        this.registry = registry;
        this.economy = economy;
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
     * Entry point for opening the shop, used by the command once it is implemented.
     */
    public void openShop(Player player) {
        ShopGUI.open(this, player);
    }

    public void shutdown() {
        economy.shutdown();
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
     * Charges the player and hands over the items.
     */
    public void buy(Player player, ShopItem item, int requested) {
        if (!item.buyable()) {
            messages.send(player, "not-for-sale");
            return;
        }

        int amount = clamp(requested, item.maxStack());
        double total = item.buyTotal(amount);

        if (!economy.has(player, total)) {
            messages.send(player, "not-enough-money", "{price}", economy.format(total));
            return;
        }

        ItemStack purchase = new ItemStack(item.material(), amount);

        if (!InventoryUtil.hasRoom(player, purchase)) {
            messages.send(player, "inventory-full");
            return;
        }

        if (!economy.withdraw(player, total)) {
            messages.send(player, "economy-error");
            return;
        }

        // Space was checked above, so leftovers should be impossible; drop them
        // rather than voiding them if something moved in the meantime.
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(purchase);
        leftover.values().forEach(extra ->
                player.getWorld().dropItemNaturally(player.getLocation(), extra));

        tradeSound(player);

        messages.send(player, "bought",
                "{item}", item.name(),
                "{amount}", String.valueOf(amount),
                "{price}", economy.format(total));
    }

    /**
     * Takes plain items off the player and pays them.
     *
     * <p>Sells what the player actually has rather than erroring out when they picked
     * an amount they cannot cover.
     */
    public void sell(Player player, ShopItem item, int requested) {
        if (!item.sellable()) {
            messages.send(player, "not-for-sale");
            return;
        }

        int available = InventoryUtil.countSellable(player, item.material());

        if (available <= 0) {
            messages.send(player, "nothing-to-sell", "{item}", item.name());
            return;
        }

        int wanted = clamp(requested, item.maxStack());
        int amount = Math.min(wanted, available);
        int removed = InventoryUtil.removeSellable(player, item.material(), amount);

        if (removed <= 0) {
            messages.send(player, "nothing-to-sell", "{item}", item.name());
            return;
        }

        double total = item.sellTotal(removed);

        if (!economy.deposit(player, total)) {
            // Give the items back rather than take them for nothing.
            player.getInventory().addItem(new ItemStack(item.material(), removed));
            messages.send(player, "economy-error");
            return;
        }

        tradeSound(player);

        if (removed < wanted) {
            messages.send(player, "sold-partial",
                    "{item}", item.name(),
                    "{available}", String.valueOf(removed),
                    "{price}", economy.format(total));
            return;
        }

        messages.send(player, "sold",
                "{item}", item.name(),
                "{amount}", String.valueOf(removed),
                "{price}", economy.format(total));
    }

    private static int clamp(int requested, int max) {
        return Math.max(1, Math.min(requested, max));
    }
}