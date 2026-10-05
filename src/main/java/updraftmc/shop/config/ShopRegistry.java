package updraftmc.shop.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import updraftmc.shop.model.ShopCategory;
import updraftmc.shop.model.ShopItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Reads config.yml into {@link ShopCategory} and {@link ShopItem} objects.
 *
 * <p>Anything wrong in the config is logged and skipped rather than thrown, so a
 * single typo never takes the whole shop offline.
 */
public final class ShopRegistry {

    private final Map<String, ShopCategory> categories;
    private final Map<String, Map<String, ShopItem>> items;
    private final GuiSettings gui;
    private final String currencySymbol;
    private final double startingBalance;
    private final boolean sounds;
    private final TaxSettings tax;

    public ShopRegistry(Map<String, ShopCategory> categories,
                        Map<String, Map<String, ShopItem>> items,
                        GuiSettings gui,
                        String currencySymbol,
                        double startingBalance,
                        boolean sounds,
                        TaxSettings tax) {
        this.categories = categories;
        this.items = items;
        this.gui = gui;
        this.currencySymbol = currencySymbol;
        this.startingBalance = startingBalance;
        this.sounds = sounds;
        this.tax = tax;
    }

    /**
     * @param shopName title of every window
     * @param mainSize size of the category menu, 27 or 54
     * @param backItem label of the back button
     * @param backIcon material of the back button
     * @param closeItem label of the close button
     * @param closeIcon material of the close button
     * @param pageIcons materials for the page arrows, so a server can retint them
     */
    public record GuiSettings(String shopName, int mainSize,
                              String backItem, String backIcon,
                              String closeItem, String closeIcon,
                              String previousIcon, String nextIcon) {
    }

    public static ShopRegistry load(FileConfiguration config, Logger logger) {
        Map<String, ShopCategory> categories = new LinkedHashMap<>();
        Map<String, Map<String, ShopItem>> items = new LinkedHashMap<>();

        ConfigurationSection root = config.getConfigurationSection("categories");

        if (root == null) {
            logger.severe("No 'categories' section in config.yml, the shop menu will be empty.");
        } else {
            for (String rawId : root.getKeys(false)) {
                ConfigurationSection section = root.getConfigurationSection(rawId);

                if (section == null) {
                    logger.warning("Category '" + rawId + "' is not a section, skipping.");
                    continue;
                }

                String id = rawId.toLowerCase(Locale.ROOT);
                String path = "categories." + rawId;

                categories.put(id, new ShopCategory(
                        id,
                        section.getString("display-name", "&f" + id),
                        List.copyOf(section.getStringList("description")),
                        material(section.getString("icon"), path + ".icon", Material.CHEST, logger),
                        ShopCategory.validSize(section.getInt("size", ShopCategory.DEFAULT_SIZE))));

                Map<String, ShopItem> loaded = loadItems(section, id, logger);
                items.put(id, loaded);
            }
        }

        ConfigurationSection gui = config.getConfigurationSection("gui");
        ConfigurationSection economy = config.getConfigurationSection("economy");
        int mainSize = ShopCategory.validSize(number(gui, "main-size", ShopCategory.DEFAULT_SIZE));

        TaxSettings tax = TaxSettings.load(config.getConfigurationSection("tax"));

        if (tax.any()) {
            logger.info("Tax is enabled (" + tax + ").");
        }

        return new ShopRegistry(categories, items,
                new GuiSettings(
                        text(gui, "shop-name", "Updraft Shop"),
                        mainSize,
                        text(gui, "back-item", "&e&lBack"),
                        materialName(gui, "back-icon", "ARROW"),
                        text(gui, "close-item", "&c&lClose"),
                        materialName(gui, "close-icon", "BARRIER"),
                        materialName(gui, "previous-icon", "ARROW"),
                        materialName(gui, "next-icon", "ARROW")),
                text(economy, "currency-symbol", "$"),
                economy == null ? 0.0 : economy.getDouble("starting-balance", 0.0),
                gui == null || gui.getBoolean("sounds", true),
                tax);
    }

    public List<ShopCategory> categories() {
        return List.copyOf(categories.values());
    }

    /**
     * @return the category, or {@code null} when the id is unknown
     */
    public ShopCategory category(String categoryId) {
        return categories.get(categoryId);
    }

    public List<ShopItem> items(String categoryId) {
        Map<String, ShopItem> found = items.get(categoryId);
        return found == null ? List.of() : List.copyOf(found.values());
    }

    /**
     * @return the item, or {@code null} when the category or item id is unknown
     */
    public ShopItem item(String categoryId, String itemId) {
        Map<String, ShopItem> found = items.get(categoryId);
        return found == null ? null : found.get(itemId);
    }

    public GuiSettings gui() {
        return gui;
    }

    public String currencySymbol() {
        return currencySymbol;
    }

    public double startingBalance() {
        return startingBalance;
    }

    /**
     * @return whether menu clicks and trades play a sound
     */
    public boolean sounds() {
        return sounds;
    }

    /**
     * @return how much the shop takes from each trade, never null
     */
    public TaxSettings tax() {
        return tax;
    }

    /**
     * @return how many categories and items ended up loaded, for the startup log
     */
    public int entryCount() {
        return categories.size() + items.values().stream().mapToInt(Map::size).sum();
    }

    private static Map<String, ShopItem> loadItems(ConfigurationSection section, String categoryId, Logger logger) {
        Map<String, ShopItem> loaded = new LinkedHashMap<>();

        ConfigurationSection itemSection = section.getConfigurationSection("items");

        if (itemSection == null) {
            logger.warning("Category '" + categoryId + "' has no items.");
            return loaded;
        }

        for (String rawId : itemSection.getKeys(false)) {
            ConfigurationSection entry = itemSection.getConfigurationSection(rawId);

            if (entry == null) {
                logger.warning("Item '" + rawId + "' in '" + categoryId + "' is not a section, skipping.");
                continue;
            }

            String path = "categories." + categoryId + ".items." + rawId;
            Material material = material(entry.getString("material"), path + ".material", null, logger);

            if (material == null) {
                logger.warning("Item '" + rawId + "' in '" + categoryId
                        + "' has an unknown material, skipping.");
                continue;
            }

            String id = rawId.toLowerCase(Locale.ROOT);

            try {
                loaded.put(id, new ShopItem(
                        categoryId,
                        id,
                        material,
                        entry.getString("name", "&f" + pretty(material)),
                        entry.getDouble("buy", 0.0),
                        entry.getDouble("sell", 0.0),
                        entry.getInt("max-stack", ShopItem.DEFAULT_MAX_STACK)));
            } catch (IllegalArgumentException exception) {
                logger.warning("Skipping item '" + rawId + "' in '" + categoryId + "': " + exception.getMessage());
            }
        }

        return loaded;
    }

    private static Material material(String name, String path, Material fallback, Logger logger) {
        if (name == null || name.isBlank()) {
            return fallback;
        }

        Material material = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));

        if (material == null || material == Material.AIR) {
            logger.warning("Unknown material '" + name + "' for " + path + ", using "
                    + (fallback == null ? "nothing" : fallback.name()) + " instead.");
            return fallback;
        }

        return material;
    }

    private static String pretty(Material material) {
        String[] words = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
        }

        return builder.toString();
    }

    private static String materialName(ConfigurationSection section, String key, String fallback) {
        return section == null ? fallback : section.getString(key, fallback);
    }

    private static String text(ConfigurationSection section, String key, String fallback) {
        return section == null ? fallback : section.getString(key, fallback);
    }

    private static int number(ConfigurationSection section, String key, int fallback) {
        return section == null ? fallback : section.getInt(key, fallback);
    }
}