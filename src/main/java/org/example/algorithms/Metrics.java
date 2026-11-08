package org.example.algorithms;

/** Mutable per-invocation instrumentation; algorithm instances are not thread-safe. */
public final class Metrics {
    public long recursiveCalls;
    public long cellsUpdated;
    public long characterComparisons;
    public long lpsComparisons;
    public long allocatedIntCells;

    public void reset() {
        recursiveCalls = cellsUpdated = characterComparisons = lpsComparisons = allocatedIntCells = 0;
    }
}
