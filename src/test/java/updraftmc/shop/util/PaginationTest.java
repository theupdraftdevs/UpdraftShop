package updraftmc.shop.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginationTest {

    @Test
    @DisplayName("an empty list is one empty page, not zero pages")
    void emptyIsOnePage() {
        assertEquals(1, Pagination.pages(0, 21));
        assertTrue(Pagination.slice(List.of(), 1, 21).isEmpty());
    }

    @Test
    @DisplayName("pages round up so a partial last page still exists")
    void roundsUp() {
        assertEquals(1, Pagination.pages(21, 21));
        assertEquals(2, Pagination.pages(22, 21));
        assertEquals(3, Pagination.pages(43, 21));
    }

    @Test
    @DisplayName("a page holds at most perPage entries")
    void sliceIsBounded() {
        List<Integer> items = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        assertEquals(3, Pagination.slice(items, 1, 3).size());
        assertEquals(List.of(1, 2, 3), Pagination.slice(items, 1, 3));
        assertEquals(List.of(7, 8, 9), Pagination.slice(items, 3, 3));
        assertEquals(List.of(10), Pagination.slice(items, 4, 3));
    }

    @Test
    @DisplayName("pages past the end clamp to the last page instead of throwing")
    void clampsHigh() {
        List<Integer> items = List.of(1, 2, 3, 4, 5);

        assertEquals(List.of(4, 5), Pagination.slice(items, 99, 3));
        assertEquals(2, Pagination.pages(5, 3));
    }

    @Test
    @DisplayName("pages below one clamp to the first page")
    void clampsLow() {
        List<Integer> items = List.of(1, 2, 3, 4, 5);

        assertEquals(List.of(1, 2, 3), Pagination.slice(items, 0, 3));
        assertEquals(List.of(1, 2, 3), Pagination.slice(items, -7, 3));
    }

    @Test
    @DisplayName("offsetOf pairs a slice back up with the underlying entries")
    void offsets() {
        assertEquals(0, Pagination.offsetOf(1, 21));
        assertEquals(21, Pagination.offsetOf(2, 21));
        assertEquals(42, Pagination.offsetOf(3, 21));
        assertEquals(0, Pagination.offsetOf(0, 21));
    }

    @Test
    @DisplayName("a perPage of zero is rejected rather than dividing by zero")
    void rejectsZeroPerPage() {
        assertThrows(IllegalArgumentException.class, () -> Pagination.pages(10, 0));
        assertThrows(IllegalArgumentException.class, () -> Pagination.slice(List.of(1), 1, 0));
    }
}