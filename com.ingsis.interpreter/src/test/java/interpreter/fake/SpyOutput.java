/*
 * My Project
 */

package interpreter.fake;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Output emitter that collects every emitted line. */
public final class SpyOutput implements Consumer<String> {
    private final List<String> lines = new ArrayList<>();

    @Override
    public void accept(String line) {
        lines.add(line);
    }

    public List<String> lines() {
        return List.copyOf(lines);
    }
}
