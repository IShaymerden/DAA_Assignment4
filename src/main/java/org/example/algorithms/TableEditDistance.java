package org.example.algorithms;

import java.util.Objects;

public final class TableEditDistance implements EditDistance {
    private final Metrics metrics = new Metrics();

    // State: dp[i][j] is the edit distance between a[0..i) and b[0..j).
    // Transition: min(dp[i-1][j]+1,dp[i][j-1]+1,dp[i-1][j-1]+(a[i-1]!=b[j-1])).
    // Base: dp[i][0]=i and dp[0][j]=j; fill rows from top to bottom.
    @Override public int distance(String a, String b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        metrics.reset();
        int[][] dp = new int[a.length() + 1][];
        for (int i = 0; i <= a.length(); i++) {
            dp[i] = new int[b.length() + 1];
            metrics.allocatedIntCells += dp[i].length;
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                metrics.characterComparisons++;
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(dp[i - 1][j - 1] + cost,
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1));
                metrics.cellsUpdated++;
            }
        }
        return dp[a.length()][b.length()];
    }

    @Override public Metrics metrics() { return metrics; }
    @Override public String name() { return "table"; }
}
