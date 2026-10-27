/*
 * My Project
 */

package interpreter.fake;

import builtin.provider.InputProvider;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Input provider backed by a queue of lines; records every prompt it receives. */
public final class FakeInputProvider implements InputProvider {
    private final Deque<String> lines;
    private final List<String> prompts = new ArrayList<>();

    public FakeInputProvider(String... lines) {
        this.lines = new ArrayDeque<>(List.of(lines));
    }

    @Override
    public String readInput(String prompt) {
        prompts.add(prompt);
        return lines.poll();
    }

    public List<String> prompts() {
        return List.copyOf(prompts);
    }
}
