package updraftmc.shop.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Colour stripping, which derives a window title from a config value that carries its
 * own colours.
 */
class MessagesTest {

    @Test
    @DisplayName("strips style codes")
    void stripsStyleCodes() {
        assertEquals("Stone", Messages.plain("&fStone"));
        assertEquals("ORBS", Messages.plain("&a&lORBS"));
        assertEquals("Gold", Messages.plain("&6&rGold"));
        assertEquals("Dragon's Breath", Messages.plain("&dDragon's Breath"));
    }

    @Test
    @DisplayName("strips both hex forms, or a title leaks raw codes")
    void stripsHex() {
        // The &x&f&f&0&0&f&f form LegacyComponentSerializer is configured to read.
        assertEquals("Gold", Messages.plain("&6&x&f&f&0&0&f&f&rGold"));
        assertEquals("Gold", Messages.plain("&6&#ffaa00Gold"));
        assertEquals("Gold", Messages.plain("&x&A&a&0&a&0&0Gold"));
    }

    @Test
    @DisplayName("handles null and empty without throwing")
    void handlesNull() {
        assertEquals("", Messages.plain(null));
        assertEquals("", Messages.plain(""));
    }

    @Test
    @DisplayName("leaves a bare ampersand, which is not a colour code")
    void leavesBareAmpersand() {
        assertEquals("A & B", Messages.plain("A & B"));
    }
}
