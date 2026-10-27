/*
 * My Project
 */

package engine.fake;

import engine.InputSupplier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Answers readInput calls with predefined lines and remembers the prompts it was given. */
public final class FakeInputSupplier implements InputSupplier {
    private final Deque<String> lines;
    private final List<String> prompts = new ArrayList<>();

    public FakeInputSupplier(String... lines) {
        this.lines = new ArrayDeque<>(List.of(lines));
    }

    @Override
    public String readInput(String prompt) {
        prompts.add(prompt);
        return lines.pollFirst();
    }

    public List<String> prompts() {
        return List.copyOf(prompts);
    }
}
