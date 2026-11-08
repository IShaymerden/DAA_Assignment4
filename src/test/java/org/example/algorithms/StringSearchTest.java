package org.example.algorithms;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class StringSearchTest {
    private List<StringSearch> algorithms() { return List.of(new NaiveSearch(), new KmpSearch()); }

    @Test void referenceLpsArrays() {
        KmpSearch kmp = new KmpSearch();
        assertArrayEquals(new int[]{0,1,2,0,1,2,3,3}, kmp.lps("AAACAAAA"));
        assertArrayEquals(new int[]{0,0,1,2,0,1,2,3,4}, kmp.lps("ABABCABAB"));
        assertArrayEquals(new int[]{}, kmp.lps(""));
        assertArrayEquals(new int[]{0}, kmp.lps("A"));
        assertArrayEquals(new int[]{0,1,2,3}, kmp.lps("AAAA"));
    }

    @Test void referenceMatchesAndOverlaps() {
        for (StringSearch algorithm : algorithms()) {
            assertEquals(List.of(10), algorithm.search("ABABDABACDABABCABAB", "ABABCABAB"));
            assertEquals(List.of(0,1,2,3), algorithm.search("AAAAA", "AA"));
            assertEquals(List.of(0,2,4), algorithm.search("ABABABA", "ABA"));
            assertEquals(List.of(0), algorithm.search("exact", "exact"));
            assertEquals(List.of(0,2), algorithm.search("aba", "a"));
        }
    }

    @Test void emptyPatternAlwaysThrows() {
        for (StringSearch algorithm : algorithms()) {
            assertThrows(IllegalArgumentException.class, () -> algorithm.search("abc", ""));
            assertThrows(IllegalArgumentException.class, () -> algorithm.search("", ""));
        }
    }

    @Test void noMatchEmptyTextAndLongerPattern() {
        for (StringSearch algorithm : algorithms()) {
            assertEquals(List.of(), algorithm.search("", "a"));
            assertEquals(List.of(), algorithm.search("abc", "abcd"));
            assertEquals(List.of(), algorithm.search("abcdef", "xyz"));
        }
    }

    @Test void hundredRandomTextsAgreeWithIndependentIndexOfOracle() {
        Random random = new Random(20262524);
        for (int trial = 0; trial < 100; trial++) {
            String text = randomString(random, random.nextInt(81));
            String pattern = randomString(random, 1 + random.nextInt(10));
            if (!text.isEmpty() && trial % 2 == 0) {
                int start = random.nextInt(text.length());
                pattern = text.substring(start, Math.min(text.length(), start + 1 + random.nextInt(8)));
            }
            List<Integer> expected = new ArrayList<>();
            for (int pos = text.indexOf(pattern); pos >= 0; pos = text.indexOf(pattern, pos + 1)) expected.add(pos);
            for (StringSearch algorithm : algorithms()) {
                assertEquals(expected, algorithm.search(text, pattern), "trial=" + trial + ", " + algorithm.name());
            }
        }
    }

    @Test void countersIncludeLpsAndReset() {
        NaiveSearch naive = new NaiveSearch();
        KmpSearch kmp = new KmpSearch();
        assertEquals(naive.search("aaaaaaaa", "aaab"), kmp.search("aaaaaaaa", "aaab"));
        assertEquals(20, naive.metrics().characterComparisons);
        assertEquals(13, kmp.metrics().characterComparisons);
        assertEquals(5, kmp.metrics().lpsComparisons);
        kmp.search("a", "a");
        assertEquals(1, kmp.metrics().characterComparisons);
        assertEquals(0, kmp.metrics().lpsComparisons);
    }

    @Test void lpsMatchesDefinitionOnRandomPatterns() {
        Random random = new Random(42);
        KmpSearch kmp = new KmpSearch();
        for (int trial = 0; trial < 100; trial++) {
            String pattern = randomString(random, random.nextInt(30));
            int[] actual = kmp.lps(pattern);
            for (int i = 0; i < pattern.length(); i++) {
                int expected = 0;
                for (int length = 1; length <= i; length++) {
                    if (pattern.substring(0, length).equals(pattern.substring(i + 1 - length, i + 1))) expected = length;
                }
                assertEquals(expected, actual[i]);
            }
        }
    }

    private String randomString(Random random, int length) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < length; i++) s.append("abc".charAt(random.nextInt(3)));
        return s.toString();
    }
}
