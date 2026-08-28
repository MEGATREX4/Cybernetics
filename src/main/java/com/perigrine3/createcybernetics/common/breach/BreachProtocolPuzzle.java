package com.perigrine3.createcybernetics.common.breach;

import java.util.ArrayList;
import java.util.List;

public record BreachProtocolPuzzle(
        List<String> grid,
        List<List<String>> codes
) {

    public static final int GRID_SIZE = 5;

    public BreachProtocolPuzzle {
        grid = List.copyOf(grid);

        List<List<String>> copiedCodes = new ArrayList<>();

        for (List<String> code : codes) {
            copiedCodes.add(List.copyOf(code));
        }

        codes = List.copyOf(copiedCodes);
    }

    public String get(int row, int column) {
        return grid.get(row * GRID_SIZE + column);
    }
}