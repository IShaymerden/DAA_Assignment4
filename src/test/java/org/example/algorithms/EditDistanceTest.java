package org.example.algorithms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class EditDistanceTest {
    private List<EditDistance> algorithms() {
        return List.of(new RecursiveEditDistance(), new TableEditDistance(), new TwoRowEditDistance());
    }

    @ParameterizedTest
    @CsvSource({"kitten,sitting,3", "flaw,lawn,2", "intention,execution,5", "abc,abc,0",
            "Abylai Han,Abylai Khan,2", "abylai han,abylai khan,1", "ab,ba,2"})
    void referenceValues(String a, String b, int expected) {
        for (EditDistance algorithm : algorithms()) assertEquals(expected, algorithm.distance(a, b), algorithm.name());
    }

    @Test void emptyInputs() {
        for (EditDistance algorithm : algorithms()) {
            assertEquals(0, algorithm.distance("", ""));
            assertEquals(3, algorithm.distance("", "abc"));
            assertEquals(3, algorithm.distance("abc", ""));
        }
    }

    @Test void hundredRandomPairsAgreeAndRespectBounds() {
        Random random = new Random(2524);
        List<EditDistance> algorithms = algorithms();
        for (int trial = 0; trial < 100; trial++) {
            String a = randomString(random, random.nextInt(7));
            String b = randomString(random, random.nextInt(7));
            int expected = algorithms.get(0).distance(a, b);
            for (EditDistance algorithm : algorithms) {
                assertEquals(expected, algorithm.distance(a, b), "trial=" + trial + ", " + algorithm.name());
                assertEquals(expected, algorithm.distance(b, a));
            }
            assertTrue(expected >= Math.abs(a.length() - b.length()));
            assertTrue(expected <= Math.max(a.length(), b.length()));
        }
    }

    @Test void countersMeasureWorkAndResetPerCall() {
        RecursiveEditDistance recursive = new RecursiveEditDistance();
        recursive.distance("aa", "bb");
        assertEquals(19, recursive.metrics().recursiveCalls);
        assertEquals(6, recursive.metrics().characterComparisons);
        recursive.distance("", "");
        assertEquals(1, recursive.metrics().recursiveCalls);
        assertEquals(0, recursive.metrics().characterComparisons);
        TableEditDistance table = new TableEditDistance();
        TwoRowEditDistance rows = new TwoRowEditDistance();
        for (EditDistance algorithm : List.of(table, rows)) {
            algorithm.distance("abc", "defgh");
            assertEquals(15, algorithm.metrics().cellsUpdated);
            assertEquals(15, algorithm.metrics().characterComparisons);
        }
        assertEquals(24, table.metrics().allocatedIntCells);
        assertEquals(8, rows.metrics().allocatedIntCells);
        rows.distance("a", "b");
        assertEquals(1, rows.metrics().cellsUpdated);
    }

    private String randomString(Random random, int length) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < length; i++) s.append("abc".charAt(random.nextInt(3)));
        return s.toString();
    }
}
