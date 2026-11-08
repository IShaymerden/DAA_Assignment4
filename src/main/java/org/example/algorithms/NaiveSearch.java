package org.example.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class NaiveSearch implements StringSearch {
    private final Metrics metrics = new Metrics();

    @Override public List<Integer> search(String text, String pattern) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.isEmpty()) throw new IllegalArgumentException("Pattern must not be empty");
        metrics.reset();
        List<Integer> matches = new ArrayList<>();
        for (int start = 0; start <= text.length() - pattern.length(); start++) {
            int j = 0;
            while (j < pattern.length()) {
                metrics.characterComparisons++;
                if (text.charAt(start + j) != pattern.charAt(j)) break;
                j++;
            }
            if (j == pattern.length()) matches.add(start);
        }
        return matches;
    }

    @Override public Metrics metrics() { return metrics; }
    @Override public String name() { return "naive"; }
}
