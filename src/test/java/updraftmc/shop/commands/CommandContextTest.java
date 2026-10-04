package updraftmc.shop.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Argument handling for the commands.
 *
 * <p>Nothing here constructs a {@code ShopItem} or opens an inventory: those need a
 * running server. What is covered is the part a player can break by typing, which is
 * exactly where the bugs have been.
 */
class CommandContextTest {

    @Test
    @DisplayName("reads arguments and falls back past the end")
    void readsArguments() {
        var context = new CommandContext(null, "sell", List.of("16"));

        assertEquals("16", context.arg(0, "fallback"));
        assertEquals("fallback", context.arg(9, "fallback"));
        assertEquals("", new CommandContext(null, "sell", List.of()).arg(0, ""));
        assertEquals(1, context.size());
        assertFalse(context.isEmpty());
    }

    @ParameterizedTest
    @DisplayName("strips a trailing dot, which Bukkit autocompletion can append")
    @CsvSource({"diamond.,diamond", "DIAMOND.,DIAMOND", "Diamond.,Diamond", "diamond,diamond"})
    void stripsTrailingDot(String typed, String expected) {
        assertEquals(expected, new CommandContext(null, "sell", List.of(typed)).raw(0));
    }

    @Test
    @DisplayName("leaves an argument without a dot alone")
    void leavesPlainArgument() {
        assertEquals("diamond", new CommandContext(null, "sell", List.of("diamond")).raw(0));
        assertEquals("", new CommandContext(null, "sell", List.of()).raw(0));
    }

    @ParameterizedTest
    @DisplayName("lowercases the amount keyword so ALL works")
    @ValueSource(strings = {"all", "ALL", "All", "aLl"})
    void lowercasesKeyword(String typed) {
        assertEquals("all", new CommandContext(null, "sell", List.of(typed)).arg(0, ""));
    }

    @Test
    @DisplayName("joins arguments for echoing usage back")
    void joinsArguments() {
        assertEquals("16 stone",
                new CommandContext(null, "sell", List.of("16", "stone")).join());
        assertEquals("", new CommandContext(null, "sell", List.of()).join());
    }

    @Test
    @DisplayName("copies the argument list so a command cannot mutate the caller's")
    void copiesArguments() {
        var context = new CommandContext(null, "sell", List.of("a"));

        assertThrows(UnsupportedOperationException.class, () -> context.args().add("b"));
    }

    @Test
    @DisplayName("reports a console sender as not a player")
    void consoleIsNotPlayer() {
        var context = new CommandContext(null, "sell", List.of());

        assertFalse(context.isPlayer());
        assertEquals(null, context.player());
    }
}
