package updraftmc.shop;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the shipped resource files agree with the code that reads them.
 *
 * <p>A typo in a config key is invisible at compile time and shows up as a barrier icon
 * or a missing message in game, so it is worth catching in the build.
 */
class ResourcesTest {

    private static final List<String> COMMANDS = List.of(
            "updraftshop", "shop", "sell", "balance", "pay", "shopadmin",
            "shopstats", "shophelp");

    private static final List<String> PERMISSIONS = List.of(
            "updraftshop.use", "updraftshop.sell", "updraftshop.stats",
            "updraftshop.pay", "updraftshop.admin");

    private static final List<String> MESSAGES = List.of(
            "bought", "sold", "sold-partial", "not-enough-money", "inventory-full",
            "nothing-to-sell", "not-for-sale", "economy-error", "unknown-item",
            "players-only", "empty-hand", "not-in-shop", "bad-amount",
            "no-permission", "no-search-results",
            "nothing-to-sell-all", "sold-all",
            "balance", "paid", "received", "pay-self", "pay-usage",
            "no-stats", "stats-header", "stats-trades", "stats-bought", "stats-sold",
            "stats-net",
            "unknown-player", "player-offline", "reload-success", "reload-failed",
            "admin-help", "admin-gave", "admin-took", "admin-set",
            "admin-received", "admin-charged", "admin-balance-changed", "admin-opened",
            "help-header", "help-shop", "help-balance", "help-pay", "help-sell", "help-stats",
            "help-help");

    @Test
    @DisplayName("plugin.yml declares every command the code registers")
    void declaresEveryCommand() throws Exception {
        var yml = read("plugin.yml");

        assertEquals("updraftmc.shop.UpdraftShop", yml.getString("main"));
        assertEquals("UpdraftShop", yml.getString("name"));

        // The version is expanded from the build, so the raw file holds a token.
        assertTrue(yml.getString("version", "").contains("$"),
                "version should come from the build, not be hardcoded");

        var declared = yml.getConfigurationSection("commands");
        assertNotNull(declared, "plugin.yml declares no commands");

        for (String name : COMMANDS) {
            assertNotNull(declared.getConfigurationSection(name),
                    "command '" + name + "' is registered in code but missing from plugin.yml");
            assertNotNull(yml.getString("commands." + name + ".description"),
                    "command '" + name + "' has no description");
            assertNotNull(yml.getString("commands." + name + ".usage"),
                    "command '" + name + "' has no usage line");
        }
    }

    @Test
    @DisplayName("plugin.yml declares every permission the code checks")
    void declaresEveryPermission() throws Exception {
        var yml = read("plugin.yml");
        var declared = yml.getConfigurationSection("permissions");

        assertNotNull(declared, "plugin.yml declares no permissions");

        for (String node : PERMISSIONS) {
            assertNotNull(declared.getConfigurationSection(node),
                    "permission '" + node + "' is checked in code but missing from plugin.yml");
            assertNotNull(yml.getString("permissions." + node + ".description"),
                    "permission '" + node + "' has no description");
        }
    }

    @Test
    @DisplayName("plugin.yml soft-depends on Vault so it loads before an economy plugin")
    void softDependsOnVault() throws Exception {
        var softdepend = read("plugin.yml").getStringList("softdepend");

        assertTrue(softdepend.contains("Vault"), "Vault should be a softdepend, got " + softdepend);
    }

    @Test
    @DisplayName("config.yml defines every message key the plugin sends")
    void definesEveryMessage() throws Exception {
        var config = read("config.yml");

        for (String key : MESSAGES) {
            assertTrue(config.contains("messages." + key),
                    "messages." + key + " is used in code but missing from config.yml");
        }
    }

    @Test
    @DisplayName("config.yml defines no message key the plugin never sends")
    void definesNoUnusedMessage() throws Exception {
        var messages = read("config.yml").getConfigurationSection("messages");

        assertNotNull(messages);
        assertEquals(new ArrayList<>(MESSAGES).stream().sorted().toList(),
                messages.getKeys(false).stream().sorted().toList(),
                "config.yml and the code disagree about which messages exist");
    }

    @Test
    @DisplayName("config.yml has the gui keys the registry reads")
    void definesGuiKeys() throws Exception {
        var config = read("config.yml");

        assertTrue(config.contains("gui.shop-name"));
        assertTrue(config.contains("gui.main-size"));
        assertTrue(config.contains("gui.sounds"));
        assertTrue(config.contains("gui.back-item"));
        assertTrue(config.contains("gui.close-icon"));
        assertTrue(config.contains("gui.previous-icon"));
        assertTrue(config.contains("gui.next-icon"));
        assertTrue(config.contains("economy.currency-symbol"));
        assertTrue(config.contains("tax.buy"));
        assertTrue(config.contains("tax.sell"));

        int size = config.getInt("gui.main-size");
        assertTrue(size == 27 || size == 54, "gui.main-size must be 27 or 54, was " + size);
    }

    @Test
    @DisplayName("config.yml has categories and items")
    void definesCategories() throws Exception {
        var categories = read("config.yml").getConfigurationSection("categories");

        assertNotNull(categories, "config.yml has no categories");
        assertTrue(categories.getKeys(false).size() > 0, "no categories configured");

        for (String id : categories.getKeys(false)) {
            var category = categories.getConfigurationSection(id);
            assertNotNull(category, "category '" + id + "' is not a section");
            assertNotNull(category.getString("display-name"),
                    "category '" + id + "' has no display-name");

            var items = category.getConfigurationSection("items");
            assertNotNull(items, "category '" + id + "' has no items");
            assertTrue(items.getKeys(false).size() > 0, "category '" + id + "' has no items");

            for (String itemId : items.getKeys(false)) {
                var item = items.getConfigurationSection(itemId);

                assertNotNull(item, "item '" + id + "." + itemId + "' is not a section");
                assertTrue(item.getDouble("buy") > 0 || item.getDouble("sell") > 0,
                        "item '" + id + "." + itemId + "' is priced zero both ways");
            }
        }
    }

    /**
     * Parses a shipped resource.
     *
     * <p>Uses {@code loadFromString} rather than {@code loadConfiguration} on purpose:
     * the latter swallows a syntax error and reports it through {@code Bukkit.getLogger},
     * which is null off a server, so a malformed YAML file shows up as a bare
     * NullPointerException with no mention of which line was wrong. Here a broken file
     * fails loudly with the parser's own message.
     */
    private static org.bukkit.configuration.file.YamlConfiguration read(String name) throws Exception {
        Path file = Path.of("src/main/resources", name);

        String text = Files.readString(file, StandardCharsets.UTF_8);

        var yml = new org.bukkit.configuration.file.YamlConfiguration();

        try {
            yml.loadFromString(text);
        } catch (org.bukkit.configuration.InvalidConfigurationException exception) {
            throw new AssertionError(name + " is not valid YAML: " + exception.getMessage(), exception);
        }

        return yml;
    }
}
