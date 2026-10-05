package updraftmc.shop.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a list across fixed size pages and reports which page a slot belongs to.
 *
 * <p>Kept free of any Bukkit type so the page maths can be tested directly. That
 * matters because an off by one here silently hides the last few shop items, which is
 * exactly the kind of bug that only shows up once a server has added more than one
 * page of stock.
 *
 * <p>Pages are 1-based, because that is how they are shown to a player. An empty list
 * still has one page, so a menu never reports "page 1 of 0".
 */
public final class Pagination {

    private Pagination() {
    }

    /**
     * @return how many pages {@code total} entries need, never below one
     */
    public static int pages(int total, int perPage) {
        if (perPage < 1) {
            throw new IllegalArgumentException("perPage must be at least 1, was " + perPage);
        }

        if (total <= 0) {
            return 1;
        }

        return (total + perPage - 1) / perPage;
    }

    /**
     * @return {@code page} clamped to a page that actually exists, never below one
     */
    public static int clamp(int page, int total, int perPage) {
        return Math.max(1, Math.min(page, pages(total, perPage)));
    }

    /**
     * @return {@code true} when there is a page before this one
     */
    public static boolean hasPrevious(int page) {
        return page > 1;
    }

    /**
     * @return {@code true} when there is a page after this one
     */
    public static boolean hasNext(int page, int total, int perPage) {
        return page < pages(total, perPage);
    }

    /**
     * The slice of {@code items} shown on {@code page}.
     *
     * <p>The page is clamped first, so a stale page number from a menu that was drawn
     * before a reload cannot throw or return a range outside the list.
     */
    public static <T> List<T> slice(List<T> items, int page, int perPage) {
        if (items.isEmpty()) {
            return List.of();
        }

        int safePage = clamp(page, items.size(), perPage);
        int from = (safePage - 1) * perPage;
        int to = Math.min(from + perPage, items.size());

        if (from >= to) {
            return List.of();
        }

        return new ArrayList<>(items.subList(from, to));
    }

    /**
     * Where {@code page} starts in the underlying list, for callers that need to pair a
     * slice back up with the entries behind it rather than copy the slice.
 */
    public static int offsetOf(int page, int perPage) {
        return (Math.max(1, page) - 1) * perPage;
    }
}