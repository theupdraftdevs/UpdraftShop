package updraftmc.shop;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
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

    private static final List<String> COMMANDS = List.of("updraftshop", "shop", "sell", "shophelp");

    private static final List<String> MESSAGES = List.of(
            "bought", "sold", "sold-partial", "not-enough-money", "inventory-full",
            "nothing-to-sell", "not-for-sale", "economy-error", "unknown-item",
            "players-only", "empty-hand", "not-in-shop", "bad-amount",
            "help-header", "help-shop", "help-sell", "help-help");

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
        assertTrue(config.contains("economy.currency-symbol"));

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

    private static org.bukkit.configuration.file.YamlConfiguration read(String name) throws Exception {
        Path file = Path.of("src/main/resources", name);

        try (InputStream in = Files.newInputStream(file)) {
            return org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }
}
