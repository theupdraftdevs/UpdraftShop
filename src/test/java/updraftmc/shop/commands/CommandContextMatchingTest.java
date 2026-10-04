package updraftmc.shop.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandContextMatchingTest {

    private static final List<String> OPTIONS =
            List.of("stone", "sand", "diamond_sword", "all");

    @Test
    @DisplayName("matches by prefix, case insensitively")
    void matchesPrefix() {
        assertEquals(List.of("stone"), CommandContext.matching("st", OPTIONS));
        assertEquals(List.of("all"), CommandContext.matching("a", OPTIONS));
        assertEquals(List.of("all"), CommandContext.matching("A", OPTIONS));
        assertEquals(List.of("stone", "sand"), CommandContext.matching("s", OPTIONS));
    }

    @Test
    @DisplayName("an empty prefix offers everything, for completion before typing")
    void emptyPrefixOffersAll() {
        assertEquals(OPTIONS, CommandContext.matching("", OPTIONS));
    }

    @Test
    @DisplayName("no match returns empty rather than null")
    void noMatchIsEmpty() {
        assertEquals(List.of(), CommandContext.matching("zzz", OPTIONS));
    }
}
