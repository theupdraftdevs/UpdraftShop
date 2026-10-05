package updraftmc.shop.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import updraftmc.shop.economy.Money;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxSettingsTest {

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.05, 0.5, 1.0})
    @DisplayName("a fraction is stored as the fraction actually charged")
    void fractionsAreTakenLiterally(double rate) {
        assertEquals(rate, new TaxSettings(rate, rate).buyRate());
    }

    @Test
    @DisplayName("five per cent of ten dollars is fifty cents")
    void appliesToGross() {
        assertEquals(0.5, new TaxSettings(0.05, 0).onBuy(10.0));
        assertEquals(5.0, new TaxSettings(0.05, 0).onBuy(100.0));
    }

    @Test
    @DisplayName("tax is rounded to the cent, so repeated trades leave no dust")
    void roundsToCents() {
        // 0.3333 of 10 is 3.333, which has to become a real amount of money.
        assertEquals(3.33, new TaxSettings(1.0 / 3, 0).onBuy(10.0));
    }

    @Test
    @DisplayName("onSell reports the tax taken, and net is what is actually paid")
    void saleDeducts() {
        TaxSettings tax = new TaxSettings(0, 0.1);

        assertEquals(10.0, tax.onSell(100.0));
        assertEquals(90.0, TaxSettings.net(100.0, 0.1));
        assertEquals(90.0, Money.round(100.0 - tax.onSell(100.0)));
    }

    @Test
    @DisplayName("a buy tax is added on top, not taken out of the price")
    void buyAdds() {
        TaxSettings tax = new TaxSettings(0.1, 0);

        assertEquals(10.0, tax.onBuy(100.0));
        assertEquals(110.0, Money.round(100.0 + tax.onBuy(100.0)));
    }

    @Test
    @DisplayName("a 100% sale tax pays nothing rather than going negative")
    void cannotPayNegative() {
        assertEquals(0.0, TaxSettings.net(100.0, 1.0));
        assertEquals(0.0, TaxSettings.apply(0.0, 0.5));
    }

    @Test
    @DisplayName("an amount of zero or less is never taxed")
    void ignoresNonPositiveGross() {
        assertEquals(0.0, TaxSettings.apply(-5.0, 0.5));
        assertEquals(0.0, TaxSettings.apply(10.0, 0.0));
    }

    @ParameterizedTest
    @CsvSource({"5, 0.05", "2.5, 0.025", "100, 1.0", "0.5, 0.5"})
    @DisplayName("apply/net agree on the round trip")
    void roundTrip(double gross, double rate) {
        double taxed = TaxSettings.apply(gross, rate);

        assertEquals(TaxSettings.net(gross, rate), Math.max(0, gross - taxed));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, Double.NaN, Double.POSITIVE_INFINITY})
    @DisplayName("a nonsensical rate is rejected at construction")
    void rejectsNonsense(double rate) {
        assertThrows(IllegalArgumentException.class, () -> new TaxSettings(rate, 0));
        assertThrows(IllegalArgumentException.class, () -> new TaxSettings(0, rate));
    }

    @Test
    @DisplayName("any() reports whether there is anything to apply")
    void anyReportsWork() {
        assertFalse(TaxSettings.NONE.any());
        assertFalse(new TaxSettings(0, 0).any());
        assertTrue(new TaxSettings(0.05, 0).any());
        assertTrue(new TaxSettings(0, 0.05).any());
    }

    @Test
    @DisplayName("toString reads as a percentage for the startup log")
    void readable() {
        assertEquals("buy 5.0%, sell 2.5%", new TaxSettings(0.05, 0.025).toString());
    }
}