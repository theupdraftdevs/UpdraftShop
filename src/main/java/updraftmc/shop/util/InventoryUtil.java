package updraftmc.shop.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Inventory maths for trades.
 *
 * <p>The sell side deliberately only ever touches plain, undamaged, unenchanted
 * stacks. Without that check a player who names their lucky sword or drops in
 * enchanted gear would watch the shop eat the good item instead of the junk.
 */
public final class InventoryUtil {

    private InventoryUtil() {
    }

    /**
     * @return whether {@code stack} would fit in the player's storage, ignoring the
     *         hotbar and armour contents that are not part of storage
     */
    public static boolean hasRoom(Player player, ItemStack stack) {
        return freeSpace(player.getInventory(), stack) >= stack.getAmount();
    }

    /**
     * @return how many slots worth of items are free for {@code stack}
     */
    public static int freeSpace(PlayerInventory inventory, ItemStack stack) {
        int max = stack.getMaxStackSize();
        int capacity = 0;

        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                capacity += max;
            } else if (slot.isSimilar(stack)) {
                capacity += Math.max(0, Math.min(max - slot.getAmount(), max));
            }

            if (capacity >= stack.getAmount()) {
                return capacity;
            }
        }

        return capacity;
    }

    /**
     * Counts items of {@code material} that are safe to sell.
     */
    public static int countSellable(Player player, Material material) {
        int total = 0;

        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (isPlain(slot, material)) {
                total += slot.getAmount();
            }
        }

        return total;
    }

    /**
     * Removes up to {@code amount} sellable items, spread across stacks.
     *
     * @return how many were actually removed
     */
    public static int removeSellable(Player player, Material material, int amount) {
        PlayerInventory inventory = player.getInventory();
        int removed = 0;

        for (int slot = 0; slot < inventory.getStorageContents().length && removed < amount; slot++) {
            ItemStack stack = inventory.getItem(slot);

            if (!isPlain(stack, material)) {
                continue;
            }

            int take = Math.min(stack.getAmount(), amount - removed);
            removed += take;

            if (take >= stack.getAmount()) {
                inventory.setItem(slot, null);
            } else {
                stack.setAmount(stack.getAmount() - take);
                inventory.setItem(slot, stack);
            }
        }

        return removed;
    }

    /**
     * An item counts as sellable only if it is an untouched stack of the material.
     */
    private static boolean isPlain(ItemStack stack, Material material) {
        if (stack == null || stack.getType() != material || stack.getType() == Material.AIR) {
            return false;
        }

        if (stack.getAmount() <= 0) {
            return false;
        }

        ItemMeta meta = stack.getItemMeta();

        if (meta == null) {
            return true;
        }

        if (meta.hasDisplayName() || meta.hasLore() || !meta.getEnchants().isEmpty()) {
            return false;
        }

        if (meta instanceof Damageable damageable) {
            return damageable.getDamage() == 0;
        }

        return true;
    }
}