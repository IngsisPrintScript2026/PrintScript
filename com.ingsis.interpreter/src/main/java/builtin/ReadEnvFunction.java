package builtin;

import builtin.provider.EnvProvider;
import node.expression.literal.DataType;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

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
        String envValue = (envProvider != null) ? envProvider.getEnv(varName) : System.getenv(varName);

        if (envValue == null) {
            throw new RuntimeException("Runtime error: Environment variable '" + varName + "' is not set");
        }

        return coerce(envValue, targetType);
    }

    private Object coerce(String raw, DataType targetType) {
        if (targetType == null) targetType = DataType.STRING;

        return switch (targetType) {
            case STRING -> raw;
            case NUMBER -> {
                try {
                    yield new BigDecimal(raw.trim());
                } catch (Exception e) {
                    throw new RuntimeException("Runtime error: Cannot parse env var value '" + raw + "' as number");
                }
            }
            case BOOLEAN -> {
                String clean = raw.trim().toLowerCase();
                if (clean.equals("true")) yield Boolean.TRUE;
                if (clean.equals("false")) yield Boolean.FALSE;
                throw new RuntimeException("Runtime error: Cannot parse env var value '" + raw + "' as boolean");
            }
        };
    }
}
