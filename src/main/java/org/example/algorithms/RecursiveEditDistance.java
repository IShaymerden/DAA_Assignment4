package org.example.algorithms;

import java.util.Objects;

public final class RecursiveEditDistance implements EditDistance {
    private final Metrics metrics = new Metrics();

    // State: D(i,j) is the distance between prefixes a[0..i) and b[0..j).
    // Transition: equal last characters -> D(i-1,j-1); else 1+min(delete,insert,replace).
    // Base: D(0,j)=j and D(i,0)=i. No memoization: repeated states are recomputed.
    @Override public int distance(String a, String b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        metrics.reset();
        return solve(a, b, a.length(), b.length());
    }

    private int solve(String a, String b, int i, int j) {
        metrics.recursiveCalls++;
        if (i == 0) return j;
        if (j == 0) return i;
        metrics.characterComparisons++;
        if (a.charAt(i - 1) == b.charAt(j - 1)) return solve(a, b, i - 1, j - 1);
        int deletion = solve(a, b, i - 1, j);
        int insertion = solve(a, b, i, j - 1);
        int replacement = solve(a, b, i - 1, j - 1);
        return 1 + Math.min(replacement, Math.min(deletion, insertion));
    }

    @Override public Metrics metrics() { return metrics; }
    @Override public String name() { return "recursive"; }
}
