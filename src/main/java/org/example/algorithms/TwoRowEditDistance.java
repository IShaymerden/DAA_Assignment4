package org.example.algorithms;

import java.util.Objects;

public final class TwoRowEditDistance implements EditDistance {
    private final Metrics metrics = new Metrics();

    // State: previous[j]=D(i-1,j), current[j]=D(i,j) for prefix distances.
    // Transition: min(previous[j]+1,current[j-1]+1,previous[j-1]+character cost).
    // Base: previous[j]=j, current[0]=i; swap the rows after each completed i.
    @Override public int distance(String a, String b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        metrics.reset();
        // Symmetry lets the shorter string define the row width.
        if (a.length() < b.length()) { String temp = a; a = b; b = temp; }
        int[] previous = new int[b.length() + 1];
        metrics.allocatedIntCells += previous.length;
        int[] current = new int[b.length() + 1];
        metrics.allocatedIntCells += current.length;
        for (int j = 0; j <= b.length(); j++) previous[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                metrics.characterComparisons++;
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(previous[j - 1] + cost,
                        Math.min(previous[j] + 1, current[j - 1] + 1));
                metrics.cellsUpdated++;
            }
            int[] temp = previous; previous = current; current = temp;
        }
        return previous[b.length()];
    }

    @Override public Metrics metrics() { return metrics; }
    @Override public String name() { return "two_rows"; }
}
