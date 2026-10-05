package updraftmc.shop.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import updraftmc.shop.ShopService;
import updraftmc.shop.config.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Hypixel look, in one place.
 *
 * <p>Hypixel's menus are instantly recognisable: dark grey window titles joined by a
 * grey {@code »}, bold coloured item names, and prices written as
 * {@code §a§lBUY §7- §a§l$500}. Every string a menu renders goes through this class,
 * so retinting the whole shop means editing one file rather than hunting through the
 * views.
 */
final class Style {

    static final String TITLE_ROOT = "&8";
    static final String TITLE_SECTION = "&8";
    static final String SEPARATOR = " &7» ";

    static final String CLICK_BROWSE = "&7Click to &aBrowse";
    static final String CLICK_PURCHASE = "&7Click to &aPurchase";

    static final Material BORDER = Material.GRAY_STAINED_GLASS_PANE;

    private Style() {
    }

    /**
     * Root title for the main menu, e.g. {@code §8Updraft Shop}.
     */
    static Component rootTitle(String shopName) {
        return Messages.color(TITLE_ROOT + shopName);
    }

    /**
     * Nested title, e.g. {@code §8Updraft Shop §7» §8ORES}. Any colour codes already
     * in {@code section} are stripped so they cannot fight the title colour.
     */
    static Component nestedTitle(String shopName, String section) {
        return Messages.color(TITLE_ROOT + shopName + SEPARATOR + TITLE_SECTION + upper(section));
    }

    /**
     * Page marker appended to a window title.
     *
     * <p>Empty on a single page menu, so a shop that fits everything does not get a
     * redundant "Page 1" on every window.
     */
    static Component pageSuffix(int page, int pages) {
        if (pages <= 1) {
            return Component.empty();
        }

        return Messages.color(" &7» &8Page " + page);
    }

    /**
     * Title marker for a search results window.
     *
     * @param count how many hits were found, so the result set is self describing
     */
    static Component searchSuffix(String query, int count) {
        return Messages.color(" &7» &8" + upper(query) + " (" + count + ")");
    }

    /**
     * The border filler. Hypixel keeps it unnamed so it reads as a frame.
     */
    static ItemStack pane() {
        return ItemBuilder.of(BORDER).name(" ").build();
    }

    /**
     * The player's own head, showing their balance. Hypixel does the same thing on its
     * main menus, and it means a player never has to run a separate command to find
     * out what they can afford.
     *
     * <p>The lore doubles as the shortcut list, so the corner of the menu is useful
     * rather than decorative.
     */
    static ItemStack balance(ShopService service, Player player, boolean showCommands) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Balance: &e" + service.economy().format(service.economy().getBalance(player)));

        if (showCommands) {
            lore.add("");
            lore.add("&7Sell what you hold: &e/sell");
            lore.add("&7Check your balance: &e/balance");
        } else {
            lore.add("");
            lore.add(Style.CLICK_BROWSE);
        }

        return ItemBuilder.of(Material.PLAYER_HEAD)
                .name("&e&lYour Balance")
                .skullOwner(player)
                .hideAttributes()
                .lore(lore.toArray(new String[0]))
                .build();
    }

    /**
     * Back arrow, in Hypixel's wording. The icon stays configurable so a server can
     * change the look without touching code.
     */
    static ItemStack back(Material icon, String name) {
        return ItemBuilder.of(icon)
                .name(name)
                .lore("&7Go back to previous page")
                .build();
    }

    /**
     * Close button.
     */
    static ItemStack close(Material icon, String name) {
        return ItemBuilder.of(icon)
                .name(name)
                .lore("&7Close this menu")
                .build();
    }

    /**
     * A buy price line, {@code §a§lBUY §7- §a§l$500}.
     */
    static String buyLine(String price) {
        return "&a&lBUY &7- &a&l" + price;
    }

    /**
     * A sell price line, {@code §c§lSELL §7- §c§l$250}.
     */
    static String sellLine(String price) {
        return "&c&lSELL &7- &c&l" + price;
    }

    /**
     * Strips colour codes and shouts the rest, matching Hypixel's all caps headers.
     */
    static String upper(String text) {
        return Messages.plain(text).toUpperCase(Locale.ROOT);
    }
}