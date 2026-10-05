package updraftmc.shop.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AmountsTest {

    @ParameterizedTest
    @CsvSource({
            "0, 0",
            "4000, 4000",
            "10.5, 10.5",
            "-25, -25",
            "1k, 1000",
            "1K, 1000",
            "2.5m, 2500000",
            "1b, 1000000000",
            "1.5k, 1500"
    })
    @DisplayName("reads plain numbers and the k/m/b shorthand")
    void parses(String raw, double expected) {
        assertEquals(expected, Amounts.parse(raw));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "k", "m", "b", "1kk", "1.2.3", "--5", " ", "NaN", "Infinity"})
    @DisplayName("rejects anything that is not a finite number")
    void rejectsGarbage(String raw) {
        assertNull(Amounts.parse(raw));
    }

    @Test
    @DisplayName("rejects a null argument instead of throwing")
    void rejectsNull() {
        assertNull(Amounts.parse(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-0.01"})
    @DisplayName("parsePositive drops zero and negatives")
    void positiveOnly(String raw) {
        assertNull(Amounts.parsePositive(raw));
    }

    @Test
    @DisplayName("parsePositive keeps a usable amount")
    void positiveKeeps() {
        assertEquals(2500.0, Amounts.parsePositive("2.5k"));
    }

    @Test
    @DisplayName("lower is locale independent and null safe")
    void lower() {
        assertEquals("reload", Amounts.lower("RELOAD"));
        assertEquals("", Amounts.lower(null));
    }
}