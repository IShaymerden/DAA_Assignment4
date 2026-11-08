package org.example.algorithms;

import java.util.List;

public interface StringSearch {
    List<Integer> search(String text, String pattern);
    Metrics metrics();
    String name();
}
