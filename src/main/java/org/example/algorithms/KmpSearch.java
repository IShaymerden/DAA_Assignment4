package org.example.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class KmpSearch implements StringSearch {
    private final Metrics metrics = new Metrics();

    /** lps[i] = length of the longest proper prefix of p[0..i] also a suffix. */
    public int[] lps(String p) {
        Objects.requireNonNull(p, "pattern");
        metrics.reset();
        return buildLps(p);
    }

    private int[] buildLps(String p) {
        int[] lps = new int[p.length()];
        metrics.allocatedIntCells += lps.length;
        int i = 1, length = 0;
        while (i < p.length()) {
            metrics.lpsComparisons++;
            if (p.charAt(i) == p.charAt(length)) {
                lps[i++] = ++length;
            } else if (length > 0) {
                length = lps[length - 1];
            } else {
                lps[i++] = 0;
            }
        }
        return lps;
    }

    @Override public List<Integer> search(String text, String pattern) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.isEmpty()) throw new IllegalArgumentException("Pattern must not be empty");
        metrics.reset();
        List<Integer> matches = new ArrayList<>();
        if (pattern.length() > text.length()) return matches;
        int[] lps = buildLps(pattern);
        int i = 0, j = 0;
        while (i < text.length()) {
            metrics.characterComparisons++;
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                if (j == pattern.length()) {
                    matches.add(i - j);
                    j = lps[j - 1]; // Retain a matching suffix to allow overlaps.
                }
            } else if (j > 0) {
                j = lps[j - 1]; // i never decreases.
            } else {
                i++;
            }
        }
        return matches;
    }

    @Override public Metrics metrics() { return metrics; }
    @Override public String name() { return "kmp"; }
}
