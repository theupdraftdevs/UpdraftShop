package updraftmc.shop.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the statistics store end to end on disk. No Bukkit types are involved, so
 * this runs like any other unit test.
 */
class TradeStatsStoreTest {

    private static final UUID STEVE = UUID.nameUUIDFromBytes("Steve".getBytes(StandardCharsets.UTF_8));
    private static final UUID ALEX = UUID.nameUUIDFromBytes("Alex".getBytes(StandardCharsets.UTF_8));

    private static final Logger LOGGER = Logger.getLogger(TradeStatsStoreTest.class.getName());

    @Test
    @DisplayName("a player who has never traded reads as zero, not null")
    void unknownPlayerIsZeroed() {
        TradeStats.Store store = new TradeStats.Store(file(), LOGGER);
        TradeStats.Entry entry = store.of(STEVE);

        assertEquals(0, entry.trades());
        assertEquals(0.0, entry.spent());
        assertEquals(0.0, entry.earned());
        assertEquals(0.0, entry.averageBuy());
        assertEquals(0.0, entry.averageSale());
    }

    @Test
    @DisplayName("of returns the same entry every time, so totals accumulate")
    void ofIsStable() {
        TradeStats.Store store = new TradeStats.Store(file(), LOGGER);

        store.recordBuy(STEVE, 10, 100);

        assertEquals(1, store.of(STEVE).buys());
        assertEquals(10, store.of(STEVE).itemsBought());
    }

    @Test
    @DisplayName("averages and net are worked out from the totals")
    void maths() {
        TradeStats.Store store = new TradeStats.Store(file(), LOGGER);

        store.recordBuy(STEVE, 10, 250);
        store.recordSell(STEVE, 4, 90);

        TradeStats.Entry entry = store.of(STEVE);

        assertEquals(25.0, entry.averageBuy());
        assertEquals(22.5, entry.averageSale());
        assertEquals(-160.0, entry.net());
        assertEquals(2, entry.trades());
    }

    @Test
    @DisplayName("a negative cost or payout is treated as zero rather than draining a total")
    void ignoresNegativeAmounts() {
        TradeStats.Store store = new TradeStats.Store(file(), LOGGER);

        store.recordSell(STEVE, 5, -100);

        assertEquals(0.0, store.of(STEVE).earned());
    }

    @Test
    @DisplayName("totals survive a save and reload")
    void survivesReload(@TempDir Path directory) {
        File target = directory.resolve("stats.yml").toFile();

        TradeStats.Store written = new TradeStats.Store(target, LOGGER);
        written.recordBuy(STEVE, 12, 300);
        written.recordSell(STEVE, 3, 45);

        assertTrue(written.save(), "save should succeed");

        TradeStats.Store reloaded = TradeStats.Store.load(target, LOGGER);
        TradeStats.Entry entry = reloaded.of(STEVE);

        assertEquals(1, entry.buys());
        assertEquals(12, entry.itemsBought());
        assertEquals(300.0, entry.spent());
        assertEquals(1, entry.sells());
        assertEquals(3, entry.itemsSold());
        assertEquals(45.0, entry.earned());
    }

    @Test
    @DisplayName("a player who only ever looked is not written to the file")
    void skipsIdlePlayers(@TempDir Path directory) throws Exception {
        File target = directory.resolve("stats.yml").toFile();

        TradeStats.Store store = new TradeStats.Store(target, LOGGER);
        store.of(STEVE);
        store.recordBuy(ALEX, 1, 10);

        assertTrue(store.save());

        String written = Files.readString(target.toPath(), StandardCharsets.UTF_8);

        assertFalse(written.contains(STEVE.toString()),
                "a player with no trades should not be persisted");
        assertTrue(written.contains(ALEX.toString()));
    }

    @Test
    @DisplayName("saving twice overwrites rather than accumulating duplicates")
    void overwrites(@TempDir Path directory) throws Exception {
        File target = directory.resolve("stats.yml").toFile();

        TradeStats.Store store = new TradeStats.Store(target, LOGGER);
        store.recordBuy(STEVE, 1, 10);

        assertTrue(store.save());
        assertTrue(store.save(), "a second save should also succeed");

        TradeStats.Entry entry = TradeStats.Store.load(target, LOGGER).of(STEVE);

        assertEquals(1, entry.buys(), "reloading must not double count");
        assertEquals(10.0, entry.spent());
    }

    @Test
    @DisplayName("a corrupt key is skipped rather than stopping the file loading")
    void toleratesCorruptKeys(@TempDir Path directory) throws Exception {
        File target = directory.resolve("stats.yml").toFile();

        Files.writeString(target.toPath(), """
                not-a-uuid:
                  buys: 4
                """ + ALEX + """
                :
                  buys: 2
                  spent: 50.0
                """, StandardCharsets.UTF_8);

        TradeStats.Store store = TradeStats.Store.load(target, LOGGER);

        assertEquals(2, store.of(ALEX).buys());
        assertEquals(50.0, store.of(ALEX).spent());
        assertEquals(1, store.tracked(), "only the valid entry is tracked");
    }

    @Test
    @DisplayName("a missing file loads as empty rather than throwing")
    void missingFileIsEmpty(@TempDir Path directory) {
        TradeStats.Store store = TradeStats.Store.load(
                directory.resolve("absent.yml").toFile(), LOGGER);

        assertEquals(0, store.tracked());
    }

    @Test
    @DisplayName("busiest ranks by trade count and drops idle players")
    void busiest(@TempDir Path directory) {
        TradeStats.Store store = new TradeStats.Store(file(), LOGGER);

        store.of(STEVE);
        store.recordBuy(STEVE, 1, 10);
        store.recordBuy(ALEX, 1, 10);
        store.recordBuy(ALEX, 1, 10);
        store.recordSell(ALEX, 1, 5);

        List<Map.Entry<UUID, TradeStats.Entry>> top = store.busiest(1);

        assertEquals(1, top.size());
        assertEquals(ALEX, top.getFirst().getKey());
        assertEquals(3, top.getFirst().getValue().trades());
        assertEquals(2, store.tracked());
    }

    @Test
    @DisplayName("busiest tolerates a silly limit")
    void busiestClamps() {
        TradeStats.Store store = new TradeStats.Store(file(), LOGGER);

        store.recordBuy(STEVE, 1, 10);

        assertEquals(0, store.busiest(0).size());
        assertEquals(0, store.busiest(-5).size());
        assertEquals(1, store.busiest(99).size());
    }

    @Test
    @DisplayName("logFailure does not throw when the write cannot happen")
    void logFailureSurvives(@TempDir Path directory) throws Exception {
        // A directory where the file should be makes the write fail for real.
        File target = directory.resolve("stats.yml").toFile();

        assertTrue(target.mkdir());

        TradeStats.Store store = new TradeStats.Store(target, LOGGER);
        store.logFailure();
    }

    private static File file() {
        return new File(System.getProperty("java.io.tmpdir"),
                "updraftshop-stats-" + UUID.randomUUID() + ".yml");
    }
}