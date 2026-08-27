/*
 * My Project
 */

package builtin;

import builtin.provider.EnvProvider;
import builtin.provider.InputProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultFunctionRegistry implements FunctionRegistry {
    private final Map<String, BuiltInFunction> functions = new HashMap<>();

    public DefaultFunctionRegistry(List<BuiltInFunction> functionList) {
        for (BuiltInFunction f : functionList) {
            functions.put(f.name().toLowerCase(), f);
        }
    }

    public DefaultFunctionRegistry(InputProvider inputProvider, EnvProvider envProvider) {
        this(
                List.of(
                        new PrintlnFunction(),
                        new ReadInputFunction(inputProvider),
                        new ReadEnvFunction(envProvider)));
    }

    public DefaultFunctionRegistry() {
        this(null, System::getenv);
    }

    @Override
    public BuiltInFunction get(String name) {
        if (name == null) return null;
        return functions.get(name.toLowerCase());
    }

    @Override
    public boolean contains(String name) {
        if (name == null) return false;
        return functions.containsKey(name.toLowerCase());
    }
}
