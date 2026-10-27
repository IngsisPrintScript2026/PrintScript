/*
 * My Project
 */

package interpreter.fake;

import builtin.provider.EnvProvider;
import java.util.Map;

/** Environment-variable provider backed by an in-memory map. */
public final class FakeEnvProvider implements EnvProvider {
    private final Map<String, String> variables;

    public FakeEnvProvider(Map<String, String> variables) {
        this.variables = Map.copyOf(variables);
    }

    public FakeEnvProvider() {
        this(Map.of());
    }

    @Override
    public String getEnv(String name) {
        return variables.get(name);
    }
}
