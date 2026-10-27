/*
 * My Project
 */

package interpreter.fake;

import builtin.BuiltInFunction;
import builtin.FunctionRegistry;
import java.util.HashMap;
import java.util.Map;

/** Function registry backed by a map keyed by the exact function name. */
public final class FakeFunctionRegistry implements FunctionRegistry {
    private final Map<String, BuiltInFunction> functions = new HashMap<>();

    public FakeFunctionRegistry(BuiltInFunction... builtIns) {
        for (BuiltInFunction function : builtIns) {
            functions.put(function.name(), function);
        }
    }

    @Override
    public BuiltInFunction get(String name) {
        return functions.get(name);
    }

    @Override
    public boolean contains(String name) {
        return functions.containsKey(name);
    }
}
