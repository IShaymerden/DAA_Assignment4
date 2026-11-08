package org.example;

import org.example.algorithms.*;
import org.example.experiment.ExperimentRunner;
import org.example.experiment.Plotter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class Main {
    private Main() { }
    public static void main(String[] args) throws Exception {
        String command = args.length == 0 ? "demo" : args[0];
        Path output = Path.of(args.length > 1 ? args[1] : "results");
        switch (command) {
            case "experiment" -> ExperimentRunner.run(output);
            case "plots" -> Plotter.generate(output);
            case "demo" -> {
                System.out.println("DAA Assignment 4 | Iskander Shaimerdinov | SE-2524");
                for (EditDistance algorithm : List.of(new RecursiveEditDistance(), new TableEditDistance(), new TwoRowEditDistance())) {
                    System.out.println(algorithm.name() + ": kitten -> sitting = " + algorithm.distance("kitten", "sitting"));
                    System.out.println(algorithm.name() + ": Abylai Han -> Abylai Khan = " + algorithm.distance("Abylai Han", "Abylai Khan"));
                }
                for (StringSearch algorithm : List.of(new NaiveSearch(), new KmpSearch())) {
                    System.out.println(algorithm.name() + ": " + algorithm.search("ABABDABACDABABCABAB", "ABABCABAB"));
                    System.out.println(algorithm.name() + " overlaps: " + algorithm.search("AAAAA", "AA"));
                }
                System.out.println("LPS AAACAAAA: " + Arrays.toString(new KmpSearch().lps("AAACAAAA")));
            }
            default -> throw new IllegalArgumentException("Usage: java -jar target/DAA_Assignment4-1.0.jar [demo|experiment|plots] [results-directory]");
        }
    }
}
