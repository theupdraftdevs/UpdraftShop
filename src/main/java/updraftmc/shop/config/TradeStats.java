package updraftmc.shop.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import updraftmc.shop.economy.Money;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * What each player has traded, kept so {@code /shopstats} has something to say.
 *
 * <p>Written to its own file rather than folded into the shop config, because it is data
 * that changes on every trade and has to survive a config reload. Reloading
 * config.yml must never be able to wipe somebody's history.
 *
 * <p>Kept in memory and saved lazily, on a timer or at shutdown. Saving per trade
 * would put a disk write on the hot path of every click in the shop, which is exactly
 * the sort of thing that makes a menu feel laggy.
 */
public final class TradeStats {

    /** One player's totals. Mutated through {@link Store#record}, never directly. */
    public static final class Entry {

        private long buys;
        private long itemsBought;
        private long sells;
        private long itemsSold;
        private double spent;
        private double earned;

        /**
         * @return money earned per item sold, or 0 when nothing was sold
         */
        public double averageSale() {
            return itemsSold == 0 ? 0 : Money.round(earned / itemsSold);
        }

        /**
         * @return money spent per item bought, or 0 when nothing was bought
         */
        public double averageBuy() {
            return itemsBought == 0 ? 0 : Money.round(spent / itemsBought);
        }

        /**
         * @return net money made in the shop, negative if they are a net spender
         */
        public double net() {
            return Money.round(earned - spent);
        }

        public long trades() {
            return buys + sells;
        }

        public long buys() {
            return buys;
        }

        public long sells() {
            return sells;
        }

        public long itemsBought() {
            return itemsBought;
        }

        public long itemsSold() {
            return itemsSold;
        }

        public double spent() {
            return spent;
        }

        public double earned() {
            return earned;
        }
    }

    /**
     * The store itself: the per player entries, plus reading and writing the file.
     */
    public static final class Store {

        private final File file;
        private final Logger logger;
        private final Map<UUID, Entry> entries = new LinkedHashMap<>();

        /**
         * An empty store that never touches the disk until something is written to it.
         *
         * <p>Public because tests need a store they can scribble on without a file, and
         * because an in-memory store is genuinely useful for anything that only wants to
         * aggregate.
         */
        public Store(File file, Logger logger) {
            this.file = file;
            this.logger = logger;
        }

        /**
         * Reads stats from disk, or starts empty if the file is missing or unreadable.
         *
         * <p>A broken stats file is a nuisance, not an outage: losing trade history is
         * much better than refusing to start the shop.
         */
        public static Store load(File file, Logger logger) {
            Store store = new Store(file, logger);

            if (!file.exists()) {
                return store;
            }

            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);

            for (String key : yml.getKeys(false)) {
                ConfigurationSection section = yml.getConfigurationSection(key);

                if (section == null) {
                    continue;
                }

                UUID id;

                try {
                    id = UUID.fromString(key);
                } catch (IllegalArgumentException exception) {
                    // A hand edited or truncated file should not stop the shop loading.
                    continue;
                }

                Entry entry = new Entry();
                entry.buys = section.getLong("buys", 0);
                entry.itemsBought = section.getLong("items-bought", 0);
                entry.sells = section.getLong("sells", 0);
                entry.itemsSold = section.getLong("items-sold", 0);
                entry.spent = Math.max(0, section.getDouble("spent", 0));
                entry.earned = Math.max(0, section.getDouble("earned", 0));

                store.entries.put(id, entry);
            }

            return store;
        }

        /**
         * @return the entry for {@code id}, never null. A player who has never traded
         *         gets a zeroed entry rather than null, so callers need no null check.
         */
        public Entry of(UUID id) {
            return entries.computeIfAbsent(id, key -> new Entry());
        }

        public void recordBuy(UUID id, int amount, double cost) {
            Entry entry = of(id);

            entry.buys++;
            entry.itemsBought += amount;
            entry.spent = Money.round(entry.spent + Math.max(0, cost));
        }

        public void recordSell(UUID id, int amount, double paid) {
            Entry entry = of(id);

            entry.sells++;
            entry.itemsSold += amount;
            entry.earned = Money.round(entry.earned + Math.max(0, paid));
        }

        /**
         * The busiest players by trade count, biggest first.
         *
         * <p>Used by {@code /shopadmin} to show who is actually using the shop.
         */
        public List<Map.Entry<UUID, Entry>> busiest(int limit) {
            List<Map.Entry<UUID, Entry>> sorted = new ArrayList<>(entries.entrySet());

            sorted.removeIf(pair -> pair.getValue().trades() == 0);

            // Then by UUID so the order is stable between runs; a leaderboard that
            // reshuffles equal scores on every save looks broken.
            sorted.sort(Comparator
                    .comparingLong((Map.Entry<UUID, Entry> pair) -> pair.getValue().trades())
                    .reversed()
                    .thenComparing(Map.Entry.comparingByKey()));

            return sorted.subList(0, Math.max(0, Math.min(limit, sorted.size())));
        }

        /**
         * @return how many players have any recorded trade
         */
        public int tracked() {
            return (int) entries.values().stream().filter(entry -> entry.trades() > 0).count();
        }

        /**
         * Writes the file.
         *
         * <p>Saves to a temporary file and moves it into place, so a crash mid write
         * cannot leave a half written file that fails to parse on the next start.
         *
         * @return whether the write succeeded
         */
        public boolean save() {
            YamlConfiguration yml = new YamlConfiguration();

            for (Map.Entry<UUID, Entry> pair : entries.entrySet()) {
                Entry entry = pair.getValue();

                if (entry.trades() == 0) {
                    // Do not litter the file with players who only ever looked.
                    continue;
                }

                String path = pair.getKey().toString();

                yml.set(path + ".buys", entry.buys);
                yml.set(path + ".items-bought", entry.itemsBought);
                yml.set(path + ".sells", entry.sells);
                yml.set(path + ".items-sold", entry.itemsSold);
                yml.set(path + ".spent", entry.spent);
                yml.set(path + ".earned", entry.earned);
            }

            try {
                File parent = file.getParentFile();

                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    logger.warning("Could not create " + parent + " to save stats.");
                    return false;
                }

                File temporary = new File(file.getParentFile(), file.getName() + ".tmp");

                yml.save(temporary);

                if (file.exists() && !file.delete()) {
                    logger.warning("Could not replace " + file + " while saving stats.");
                    return false;
                }

                if (!temporary.renameTo(file)) {
                    logger.warning("Could not move the stats file into place at " + file);
                    return false;
                }

                return true;
            } catch (IOException exception) {
                logger.log(Level.WARNING, "Could not save stats to " + file, exception);
                return false;
            }
        }

        public void logFailure() {
            if (!save()) {
                logger.warning("Trade stats were not saved and will be lost.");
            }
        }
    }

    private TradeStats() {
    }
}