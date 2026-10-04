package updraftmc.shop.economy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Hypixel style money rendering: thousands separators, and no trailing {@code .00} on
 * whole amounts.
 */
class MoneyTest {

    @ParameterizedTest
    @DisplayName("drops the decimals on whole amounts and keeps them on fractions")
    @CsvSource({
            "500, '$500'",
            "1000, '$1,000'",
            "4000, '$4,000'",
            "1234.56, '$1,234.56'",
            "12.5, '$12.50'",
            "0.01, '$0.01'",
            "0, '$0'",
    })
    void formatsAmounts(double amount, String expected) {
        assertEquals(expected, Money.format(amount, "$"));
    }

    @ParameterizedTest
    @DisplayName("rounds half up, so a price never shows a third decimal")
    @CsvSource({
            "99.999, '$100'",
            "0.005, '$0.01'",
    })
    void roundsHalfUp(double amount, String expected) {
        assertEquals(expected, Money.format(amount, "$"));
    }

    @Test
    @DisplayName("floating point error does not leak into a price")
    void hidesFloatingPointError() {
        // 0.1 + 0.2 is 0.30000000000000004, which must not reach a player.
        assertEquals("$0.30", Money.format(0.1 + 0.2, "$"));
        assertEquals("$70", Money.format(10 * 7.0, "$"));
    }

    @Test
    @DisplayName("puts the sign before the symbol")
    void formatsNegatives() {
        assertEquals("-$5", Money.format(-5, "$"));
        assertEquals("-$12.50", Money.format(-12.5, "$"));
    }

    @Test
    @DisplayName("honours a configured symbol, including a suffix")
    void usesConfiguredSymbol() {
        assertEquals("coins5", Money.format(5, "coins"));
        assertEquals("10k1", Money.format(1, "10k"));
    }

    @Test
    @DisplayName("a nonsense balance shows zero instead of NaN in a price line")
    void survivesNonsense() {
        assertEquals("$0", Money.format(Double.NaN, "$"));
        assertEquals("$0", Money.format(Double.POSITIVE_INFINITY, "$"));
        assertEquals("$0", Money.format(Double.NEGATIVE_INFINITY, "$"));
    }
}
