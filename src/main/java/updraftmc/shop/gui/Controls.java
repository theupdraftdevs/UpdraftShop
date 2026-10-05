package updraftmc.shop.gui;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * The page navigation controls, drawn the same way by every view.
 *
 * <p>Previous, next and the page counter are only drawn when there is more than one
 * page, so a shop with a handful of items does not grow three dead icons that a player
 * can click and get no response from.
 *
 * <p>This class only builds the icons. Deciding what a click means belongs to the view
 * that drew them, since only the view knows what to redraw afterwards.
 */
final class Controls {

    private Controls() {
    }

    /**
     * Draws navigation onto the bottom row.
     *
     * @param hasPrevious whether there is a page before this one
     * @param hasNext     whether there is a page after this one
     */
    static void drawPages(Inventory inventory, int page, int pages,
                          boolean hasPrevious, boolean hasNext) {

        int size = inventory.getSize();

        if (hasPrevious) {
            inventory.setItem(Layout.bottomSlot(size, Layout.PREVIOUS), arrow("&e&lPrevious Page", true));
        }

        if (hasNext) {
            inventory.setItem(Layout.bottomSlot(size, Layout.NEXT), arrow("&e&lNext Page", false));
        }

        if (pages > 1) {
            inventory.setItem(Layout.bottomSlot(size, Layout.PAGE_INFO), pageInfo(page, pages));
        }
    }

    /**
     * @return whether {@code slot} sits in the bottom row, which holds controls rather
     *         than shop content
     */
    static boolean isControl(Inventory inventory, int slot) {
        return slot >= Layout.bottomRow(inventory.getSize());
    }

    /**
     * The slot the previous page arrow occupies, or -1 when it is not drawn.
     */
    static int previousSlot(Inventory inventory, boolean hasPrevious) {
        return hasPrevious ? Layout.bottomSlot(inventory.getSize(), Layout.PREVIOUS) : -1;
    }

    /**
     * The slot the next page arrow occupies, or -1 when it is not drawn.
     */
    static int nextSlot(Inventory inventory, boolean hasNext) {
        return hasNext ? Layout.bottomSlot(inventory.getSize(), Layout.NEXT) : -1;
    }

    private static ItemStack arrow(String name, boolean previous) {
        return ItemBuilder.of(Material.ARROW)
                .name(name)
                .lore("&7Click to go", previous ? "&7back" : "&7forward")
                .build();
    }

    private static ItemStack pageInfo(int page, int pages) {
        return ItemBuilder.of(Material.PAPER)
                .name("&e&lPage " + page)
                .lore("&7of &f" + pages)
                .build();
    }
}