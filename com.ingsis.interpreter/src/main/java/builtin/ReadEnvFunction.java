/*
 * My Project
 */

package builtin;

import builtin.provider.EnvProvider;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import node.expression.literal.DataType;

public class ReadEnvFunction implements BuiltInFunction {
    private final EnvProvider envProvider;

    public ReadEnvFunction(EnvProvider envProvider) {
        this.envProvider = envProvider;
    }

    public ReadEnvFunction() {
        this(System::getenv);
    }

    @Override
    public String name() {
        return "readEnv";
    }

    @Override
    public void execute(List<Object> arguments, Consumer<String> outputEmitter) {
        // Builtin function evaluation handles return values
    }

    public Object evaluate(List<Object> arguments, DataType targetType) {
        if (arguments.isEmpty()) {
            throw new RuntimeException("Runtime error: readEnv requires 1 argument (env var name)");
        }
        String varName = String.valueOf(arguments.get(0));
        String envValue =
                (envProvider != null) ? envProvider.getEnv(varName) : System.getenv(varName);

        if (envValue == null) {
            throw new RuntimeException(
                    "Runtime error: Environment variable '" + varName + "' is not set");
        }

        return coerce(envValue, targetType);
    }

    private Object coerce(String raw, DataType targetType) {
        DataType type = (targetType == null) ? DataType.STRING : targetType;
        return switch (type) {
            case STRING -> raw;
            case NUMBER -> parseNumber(raw);
            case BOOLEAN -> parseBoolean(raw);
        };
    }

    private BigDecimal parseNumber(String raw) {
        try {
            return new BigDecimal(raw.trim());
        } catch (Exception e) {
            throw new RuntimeException(
                    "Runtime error: Cannot parse env var value '" + raw + "' as number");
        }
    }

    private Boolean parseBoolean(String raw) {
        String clean = raw.trim().toLowerCase();
        if (clean.equals("true")) {
            return Boolean.TRUE;
        }
        if (clean.equals("false")) {
            return Boolean.FALSE;
        }
        throw new RuntimeException(
                "Runtime error: Cannot parse env var value '" + raw + "' as boolean");
    }
}
