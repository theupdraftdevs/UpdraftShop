package updraftmc.shop.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
     * Slot maths shared by every shop view.
 *
 * <p>Views are drawn as: a full glass border around the outside, clickable content
 * in the rows between the top and bottom border, and controls on the bottom row.
 */
final class Layout {

    private Layout() {
    }

    /**
     * Fills the outermost ring with panes.
     */
    static void fillBorder(Inventory inventory) {
        ItemStack border = Style.pane();

        int size = inventory.getSize();
        int rows = size / 9;

        for (int column = 0; column < 9; column++) {
            inventory.setItem(column, border);
            inventory.setItem((rows - 1) * 9 + column, border);
        }

        for (int row = 1; row < rows - 1; row++) {
            inventory.setItem(row * 9, border);
            inventory.setItem(row * 9 + 8, border);
        }
    }

    /**
     * Slots available for content, in reading order. Row zero and the bottom row are
     * left out because they hold the border and the controls.
     */
    static List<Integer> contentSlots(int size) {
        List<Integer> slots = new ArrayList<>();
        int rows = size / 9;

        for (int row = 1; row < rows - 1; row++) {
            for (int column = 1; column <= 7; column++) {
                slots.add(row * 9 + column);
            }
        }

        return slots;
    }

    /**
     * First slot of the bottom row, where controls live.
     */
    static int bottomRow(int size) {
        return size - 9;
    }

    /**
     * Slot n places into the bottom row, counted from its left edge.
     */
    static int bottomSlot(int size, int offset) {
        return bottomRow(size) + offset;
    }
}