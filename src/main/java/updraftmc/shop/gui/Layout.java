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
 *
 * <p>The bottom row offsets are fixed here so every view puts its controls in the same
 * place. A player learns one layout instead of relearning it per screen.
 */
final class Layout {

    /**
     * Bottom row slots, counted from the left edge of that row.
     *
     * <p>The corners are used because the middle is already busy, and because it keeps
     * the eye-catching controls off the centre where a misclick is most likely.
     */
    static final int HEAD = 0;
    static final int PREVIOUS = 1;
    static final int BACK = 3;
    static final int CLOSE = 5;
    static final int NEXT = 7;
    static final int PAGE_INFO = 8;

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
     *
     * @return the same list every time, so callers can index it without copying
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
     * How many icons fit in a menu of {@code size}, matching the layout drawn above.
     */
    static int capacity(int size) {
        return Math.max(0, (size / 9 - 2) * 7);
    }

    /**
     * The smallest menu size that fits {@code items} content icons.
     *
     * <p>Used by views that build their size from their content, such as search
     * results, so a three hit search does not open an empty 54 slot menu.
     */
    static int validSize(int items) {
        return capacity(SMALL_SIZE) >= items ? SMALL_SIZE : FULL_SIZE;
    }

    private static final int SMALL_SIZE = 27;
    private static final int FULL_SIZE = 54;

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