package updraftmc.shop.model;

import org.bukkit.Material;
import updraftmc.shop.economy.InternalEconomy;

import java.util.Objects;

/**
 * A single tradeable entry in the shop, as configured under a category in config.yml.
 *
 * <p>A price of {@code 0} disables that side of the trade, so {@code sell: 0} turns an
 * entry into buy-only.
 */
public record ShopItem(String categoryId, String id, Material material, String name,
                       double buyPrice, double sellPrice, int maxStack) {

    public static final int DEFAULT_MAX_STACK = 64;

    public ShopItem {
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(name, "name");

        if (buyPrice < 0 || sellPrice < 0) {
            throw new IllegalArgumentException("Prices cannot be negative: " + id);
        }

        if (buyPrice == 0 && sellPrice == 0) {
            throw new IllegalArgumentException("Item cannot have a price of zero both ways: " + id);
        }

        maxStack = clampStack(material, maxStack);
    }

    public boolean buyable() {
        return buyPrice > 0;
    }

    public boolean sellable() {
        return sellPrice > 0;
    }

    public double buyTotal(int amount) {
        return InternalEconomy.round(buyPrice * amount);
    }

    public double sellTotal(int amount) {
        return InternalEconomy.round(sellPrice * amount);
    }

    /**
     * Largest stack that can actually be moved in one go, honouring the material's
     * own stack limit so swords and armour trade one at a time.
     */
    public int maxStack() {
        return clampStack(material, maxStack);
    }

    private static int clampStack(Material material, int requested) {
        int limit = material.getMaxStackSize();
        return Math.max(1, Math.min(requested, limit));
    }
}