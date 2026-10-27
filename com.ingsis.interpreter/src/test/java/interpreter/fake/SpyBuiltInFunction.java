/*
 * My Project
 */

package interpreter.fake;

import builtin.BuiltInFunction;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Built-in function that records the arguments of every call, optionally failing. */
public final class SpyBuiltInFunction implements BuiltInFunction {
    private final String name;
    private final String failure;
    private final List<List<Object>> calls = new ArrayList<>();

    public SpyBuiltInFunction(String name, String failure) {
        this.name = name;
        this.failure = failure;
    }

    public SpyBuiltInFunction(String name) {
        this(name, null);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void execute(List<Object> arguments, Consumer<String> outputEmitter) {
        calls.add(List.copyOf(arguments));
        if (failure != null) {
            throw new IllegalStateException(failure);
        }
    }

    public List<List<Object>> calls() {
        return List.copyOf(calls);
    }
}
