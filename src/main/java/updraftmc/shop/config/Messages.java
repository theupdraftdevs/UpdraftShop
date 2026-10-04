package updraftmc.shop.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Chat messages, all pulled from the {@code messages} section of config.yml so staff
 * can reword them without a rebuild.
 */
public final class Messages {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final String prefix;
    private final Map<String, String> messages;

    private Messages(String prefix, Map<String, String> messages) {
        this.prefix = prefix;
        this.messages = messages;
    }

    public static Messages load(FileConfiguration config) {
        Map<String, String> messages = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("messages");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                messages.put(key, section.getString(key));
            }
        }

        return new Messages(
                config.getString("prefix", "&b&lUpdraft Shop &r&8» &r"),
                messages);
    }

    /**
     * Sends a message, appending the configured prefix.
     */
    public void send(Player player, String key, String... replacements) {
        player.sendMessage(text(prefix + get(key), replacements));
    }

    /**
     * Sends a message with no prefix, for cases where the prefix would be in the way.
     */
    public void sendRaw(Player player, String key, String... replacements) {
        player.sendMessage(text(get(key), replacements));
    }

    public Component component(String key, String... replacements) {
        return text(get(key), replacements);
    }

    /**
     * @return the raw message, or a visible placeholder if the key is missing so an
     *         incomplete config never sends a blank line
     */
    public String get(String key) {
        return messages.getOrDefault(key, "&c<missing message: " + key + ">");
    }

    public String prefix() {
        return prefix;
    }

    /**
     * Deserialises {@code text} and swaps in the given {@code key, value} pairs.
     */
    public static Component text(String text, String... replacements) {
        String result = text == null ? "" : text;

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace(replacements[i], replacements[i + 1]);
        }

        return LEGACY.deserialize(result);
    }

    public static Component color(String text) {
        return text(text, new String[0]);
    }

    /**
     * Strips every colour code, leaving the readable text.
     *
     * <p>Used to derive a window title from a config value that carries its own
     * colours, which would otherwise fight the title colour.
     */
    public static String plain(String text) {
        return text == null ? "" : COLOURS.matcher(text).replaceAll("");
    }

    /**
     * Matches all three forms this plugin accepts: the {@code &x&f&f&0&0&f&f} hex
     * sequence that {@link #LEGACY} is configured to read, {@code &#rrggbb} hex, and a
     * single {@code &a} style code.
     *
     * <p>The hex sequence has to be listed first, otherwise its {@code &x} gets eaten
     * as a style code and the remaining colour pairs are left behind as text.
     */
    private static final Pattern COLOURS = Pattern.compile(
            "(?i)&x(?:&[0-9a-f]){6}|&#([0-9a-f]{6})|&[0-9a-fk-or]");
}