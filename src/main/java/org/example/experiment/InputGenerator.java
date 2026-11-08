package org.example.experiment;

import java.util.Random;

/** Own deterministic generator: each case is independent of iteration order. */
public final class InputGenerator {
    public static final long SEED = 25242026L;
    public record Pair(String first, String second) { }
    private InputGenerator() { }

    public static Pair edit(String type, int n) {
        Random random = new Random(SEED + 31L * n + type.hashCode());
        return switch (type) {
            case "identical" -> { String a = randomString(random, n, "abcd"); yield new Pair(a, a); }
            case "random" -> new Pair(randomString(random, n, "abcd"), randomString(random, n, "abcd"));
            case "disjoint" -> new Pair("a".repeat(n), "b".repeat(n));
            default -> throw new IllegalArgumentException("Unknown edit input: " + type);
        };
    }

    public static Pair search(String type, int n) {
        int m = n / 256;
        Random random = new Random(SEED + 31L * n + type.hashCode());
        return switch (type) {
            case "random" -> {
                String text = randomString(random, n, "abcdefghijklmnopqrstuvwxyz");
                yield new Pair(text, text.substring(n / 2, n / 2 + m));
            }
            case "early_mismatch" -> new Pair("b".repeat(n), "a".repeat(m));
            case "late_mismatch" -> new Pair("a".repeat(n), "a".repeat(m - 1) + "b");
            default -> throw new IllegalArgumentException("Unknown search input: " + type);
        };
    }

    private static String randomString(Random random, int n, String alphabet) {
        StringBuilder s = new StringBuilder(n);
        for (int i = 0; i < n; i++) s.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return s.toString();
    }
}
