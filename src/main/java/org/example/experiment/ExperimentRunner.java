package org.example.experiment;

import org.example.algorithms.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

public final class ExperimentRunner {
    private static final int RUNS = 5;
    private static volatile int sink;
    private ExperimentRunner() { }

    public static void run(Path output) throws IOException {
        Files.createDirectories(output);
        Files.writeString(output.resolve("environment.txt"),
                "timestamp_utc=" + Instant.now() + "\njava=" + System.getProperty("java.version")
                + "\nvm=" + System.getProperty("java.vm.name") + "\nos=" + System.getProperty("os.name")
                + " " + System.getProperty("os.arch") + "\nprocessors=" + Runtime.getRuntime().availableProcessors()
                + "\nseed=" + InputGenerator.SEED + "\nruns_per_case=" + RUNS + "\n");
        List<EditDistance> allEdit = List.of(new RecursiveEditDistance(), new TableEditDistance(), new TwoRowEditDistance());
        List<StringSearch> searches = List.of(new NaiveSearch(), new KmpSearch());
        // Warm the same method bodies before collecting any measurements.
        for (int i = 0; i < 2000; i++) {
            for (EditDistance algorithm : allEdit) sink = algorithm.distance("kitten", "sitting");
            for (StringSearch algorithm : searches) sink = algorithm.search("ABABABA", "ABA").size();
        }
        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(output.resolve("results.csv"), StandardCharsets.UTF_8))) {
            csv.println("algorithm,input,n,m,run,time_ms,recursive_calls,cells_updated,char_comparisons,lps_comparisons,allocated_int_cells,result,seed,input_sha256");
            for (int n : new int[]{2,4,6,8,10,12}) {
                for (String type : List.of("identical", "random", "disjoint")) editCase(csv, allEdit, type, n);
            }
            for (int n : new int[]{128,256,512,1024,2048}) {
                for (String type : List.of("identical", "random", "disjoint")) editCase(csv, allEdit.subList(1,3), type, n);
            }
            for (int n : new int[]{4096,16384,65536,262144}) {
                for (String type : List.of("random", "early_mismatch", "late_mismatch")) searchCase(csv, searches, type, n);
            }
            if (csv.checkError()) throw new IOException("Failed to write results.csv");
        }
        Plotter.generate(output);
        System.out.println("Finished: " + output.toAbsolutePath() + " (540 measured runs, 4 PNG plots)");
    }

    private static void editCase(PrintWriter csv, List<EditDistance> algorithms, String type, int n) {
        InputGenerator.Pair pair = InputGenerator.edit(type, n);
        String hash = fingerprint(pair);
        int expected = new TableEditDistance().distance(pair.first(), pair.second());
        for (EditDistance algorithm : algorithms) {
            if (algorithm.distance(pair.first(), pair.second()) != expected) throw new IllegalStateException("Edit disagreement");
        }
        for (int run = 1; run <= RUNS; run++) {
            for (int index = 0; index < algorithms.size(); index++) {
                EditDistance algorithm = algorithms.get((index + run - 1) % algorithms.size());
                long start = System.nanoTime();
                int result = algorithm.distance(pair.first(), pair.second());
                long elapsed = System.nanoTime() - start;
                sink = result;
                if (result != expected) throw new IllegalStateException("Wrong edit result");
                write(csv, algorithm.name(), type, n, pair.second().length(), run, elapsed, algorithm.metrics(), result, hash);
            }
        }
        System.out.println("edit " + type + " n=" + n);
    }

    private static void searchCase(PrintWriter csv, List<StringSearch> algorithms, String type, int n) {
        InputGenerator.Pair pair = InputGenerator.search(type, n);
        String hash = fingerprint(pair);
        List<Integer> expected = new ArrayList<>();
        for (int i = pair.first().indexOf(pair.second()); i >= 0; i = pair.first().indexOf(pair.second(), i + 1)) expected.add(i);
        for (StringSearch algorithm : algorithms) {
            if (!algorithm.search(pair.first(), pair.second()).equals(expected)) throw new IllegalStateException("Search disagreement");
        }
        for (int run = 1; run <= RUNS; run++) {
            for (int index = 0; index < algorithms.size(); index++) {
                StringSearch algorithm = algorithms.get((index + run - 1) % algorithms.size());
                long start = System.nanoTime();
                List<Integer> result = algorithm.search(pair.first(), pair.second());
                long elapsed = System.nanoTime() - start;
                sink = result.size();
                if (!result.equals(expected)) throw new IllegalStateException("Wrong search result");
                write(csv, algorithm.name(), type, n, pair.second().length(), run, elapsed, algorithm.metrics(), result.size(), hash);
            }
        }
        System.out.println("search " + type + " n=" + n + " m=" + pair.second().length());
    }

    private static void write(PrintWriter csv, String algorithm, String input, int n, int m, int run,
                              long elapsed, Metrics c, int result, String hash) {
        csv.printf(Locale.ROOT, "%s,%s,%d,%d,%d,%.9f,%d,%d,%d,%d,%d,%d,%d,%s%n",
                algorithm, input, n, m, run, elapsed / 1_000_000.0, c.recursiveCalls, c.cellsUpdated,
                c.characterComparisons, c.lpsComparisons, c.allocatedIntCells, result, InputGenerator.SEED, hash);
    }

    private static String fingerprint(InputGenerator.Pair pair) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(pair.first().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            return HexFormat.of().formatHex(digest.digest(pair.second().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
