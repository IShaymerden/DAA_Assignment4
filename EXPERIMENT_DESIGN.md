# Experiment Design Card - recorded before measurements

**Iskander Shaimerdinov | SE-2524 | Assignment 4**

| Field | Planned experiment |
|---|---|
| Questions | Edit distance: how do repeated recursive calls compare with DP work, and does two-row storage reduce memory without changing the answer? Search: how much does prefix reuse reduce work on repeated-prefix inputs? |
| Sizes | All edit versions: equal lengths n = 2, 4, 6, 8, 10, 12 (6x range). Table and two rows additionally: 128, 256, 512, 1024, 2048 (16x). Both searches: text n = 4096, 16384, 65536, 262144 (64x); pattern m = n/256. Small recursion sizes avoid exponential runaway; larger DP/search cases make elapsed times visible. |
| Edit inputs | `identical`: a=b, exposing the recursive equality shortcut; `random`: independent strings over abcd, showing mixed matches; `disjoint`: a^n versus b^n, forcing all three recursive branches at every non-base state. |
| Search inputs | `random`: lowercase random text, pattern copied from its middle to guarantee a match; `early_mismatch`: b^n versus a^m, rejecting immediately; `late_mismatch`: a^n versus a^(m-1)b, forcing almost a full comparison at every naive alignment. |
| Metrics | Time in ms via nanoTime; median of 5 measured invocations. Edit: recursive calls, interior DP cells updated, character comparisons, allocated int cells. Search: text/pattern comparisons plus LPS comparisons, counted separately and summed for plots. Counters are incremented inside the algorithms, not reconstructed from formulas. |
| Reproducibility | Java Random; base seed 25242026, mixed with size and input-type name. Inputs are generated outside timing. CSV includes a SHA-256 input fingerprint. Same inputs, answers and counters repeat; timing naturally varies. Global small-input warm-up and one warm-up invocation per case precede 5 measurements. |
| Predictions | Disjoint recursion grows exponentially; identical recursion is linear. Both DP versions update n*m cells even on identical inputs; full-table storage is quadratic and two-row storage linear. With m=n/256, late-mismatch Naive grows quadratically; KMP remains linear in n+m. Random/early-mismatch Naive may compete with KMP because it has less preprocessing. |
| Plots | (1) Small edit character comparisons on a log y-axis: repeated work. (2) Large random DP median time: scaling. (3) Search comparisons on log-log axes: adversarial growth. (4) Search median time on log-log axes: practical cost and input sensitivity. Every plot includes units, legend and a takeaway in the report. |

**Timing boundary:** includes method execution, per-call counter reset, DP/LPS allocation and result construction; excludes input generation, verification, CSV writing and plotting. No batching or post-hoc multiplication of counters. Storage counts integer array slots, not heap bytes or recursive stack frames. Run order rotates algorithm order between repetitions to reduce a fixed-order bias. No claim of a rigorous microbenchmark or statistical significance is intended.

**Stop rule:** do not run plain recursion above n=12; exponential work, not stack depth at these sizes, is the concern. This is a chosen experimental cap, not a measured hard machine limit.
