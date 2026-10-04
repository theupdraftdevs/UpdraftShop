package updraftmc.shop.model;

import org.bukkit.Material;

import java.util.List;
import java.util.Objects;

/**
 * A top level group of shop items, rendered as one icon in the main menu.
 *
 * @param size inventory size of the category view, always a multiple of nine
 */
public record ShopCategory(String id, String displayName, List<String> description,
                           Material icon, int size) {

    public static final int DEFAULT_SIZE = 54;
    public static final int SMALL_SIZE = 27;

    public ShopCategory {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(icon, "icon");

        size = validSize(size);
    }

    /**
     * Coerces a configured size to something the layout code can actually draw.
     */
    public static int validSize(int configured) {
        if (configured == SMALL_SIZE) {
            return SMALL_SIZE;
        }

        return DEFAULT_SIZE;
    }
}