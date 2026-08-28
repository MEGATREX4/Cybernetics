package com.perigrine3.createcybernetics.common.breach;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BreachProtocolGenerator {
    private static final List<String> TOKENS = List.of(
            "1C",
            "55",
            "7A",
            "BD",
            "E9",
            "FF"
    );

    private static final int[] CODE_LENGTHS = {
            3,
            4,
            5,
            6
    };

    private BreachProtocolGenerator() {}

    public static BreachProtocolPuzzle generate(RandomSource random) {
        for (int puzzleAttempt = 0; puzzleAttempt < 100; puzzleAttempt++) {
            List<String> grid = generateGrid(random);
            List<List<String>> codes = new ArrayList<>();

            boolean valid = true;

            for (int codeLength : CODE_LENGTHS) {
                List<String> code = generateSolvableCode(grid, codeLength, random);

                if (code == null) {
                    valid = false;
                    break;
                }

                codes.add(code);
            }

            if (!valid) {
                continue;
            }

            if (hasPrefixConflict(codes)) {
                continue;
            }

            return new BreachProtocolPuzzle(grid, codes);
        }

        throw new IllegalStateException("Failed to generate Breach Protocol puzzle");
    }

    private static List<String> generateGrid(RandomSource random) {
        List<String> grid = new ArrayList<>(BreachProtocolPuzzle.GRID_SIZE * BreachProtocolPuzzle.GRID_SIZE);

        for (int i = 0; i < BreachProtocolPuzzle.GRID_SIZE * BreachProtocolPuzzle.GRID_SIZE; i++) {
            grid.add(TOKENS.get(random.nextInt(TOKENS.size())));
        }

        return grid;
    }

    private static List<String> generateSolvableCode(List<String> grid, int length, RandomSource random) {
        for (int attempt = 0; attempt < 100; attempt++) {
            Set<Integer> used = new HashSet<>();
            List<String> code = new ArrayList<>();

            boolean selectingRow = true;
            int requiredIndex = 0;

            for (int step = 0; step < length; step++) {
                List<Integer> candidates = new ArrayList<>();

                for (int i = 0; i < BreachProtocolPuzzle.GRID_SIZE; i++) {
                    int row = selectingRow ? requiredIndex : i;
                    int column = selectingRow ? i : requiredIndex;
                    int index = row * BreachProtocolPuzzle.GRID_SIZE + column;

                    if (!used.contains(index)) {
                        candidates.add(index);
                    }
                }

                if (candidates.isEmpty()) {
                    break;
                }

                int selectedIndex = candidates.get(random.nextInt(candidates.size()));

                used.add(selectedIndex);
                code.add(grid.get(selectedIndex));

                int row = selectedIndex / BreachProtocolPuzzle.GRID_SIZE;
                int column = selectedIndex % BreachProtocolPuzzle.GRID_SIZE;

                if (selectingRow) {
                    selectingRow = false;
                    requiredIndex = column;
                } else {
                    selectingRow = true;
                    requiredIndex = row;
                }
            }

            if (code.size() == length) {
                return code;
            }
        }

        return null;
    }

    private static boolean hasPrefixConflict(List<List<String>> codes) {
        for (int i = 0; i < codes.size(); i++) {
            for (int j = 0; j < codes.size(); j++) {
                if (i == j) continue;

                List<String> first = codes.get(i);
                List<String> second = codes.get(j);

                if (first.size() >= second.size()) continue;

                if (startsWith(second, first)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean startsWith(List<String> sequence, List<String> prefix) {
        if (prefix.size() > sequence.size()) {
            return false;
        }

        for (int i = 0; i < prefix.size(); i++) {
            if (!sequence.get(i).equals(prefix.get(i))) {
                return false;
            }
        }

        return true;
    }
}