/*
 * My Project
 */

package builtin;

import builtin.provider.InputProvider;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import node.expression.literal.DataType;

public class ReadInputFunction implements BuiltInFunction {
    private final InputProvider inputProvider;

    public ReadInputFunction(InputProvider inputProvider) {
        this.inputProvider = inputProvider;
    }

    public ReadInputFunction() {
        this(prompt -> "");
    }

    @Override
    public String name() {
        return "readInput";
    }

    @Override
    public void execute(List<Object> arguments, Consumer<String> outputEmitter) {
        String prompt = arguments.isEmpty() ? "" : String.valueOf(arguments.get(0));
        if (outputEmitter != null && !prompt.isEmpty()) {
            outputEmitter.accept(prompt);
        }
        if (inputProvider != null) {
            inputProvider.readInput(prompt);
        }
    }

    public Object evaluate(
            List<Object> arguments, DataType targetType, Consumer<String> outputEmitter) {
        String prompt = arguments.isEmpty() ? "" : String.valueOf(arguments.get(0));
        if (outputEmitter != null && !prompt.isEmpty()) {
            outputEmitter.accept(prompt);
        }
        String input = (inputProvider != null) ? inputProvider.readInput(prompt) : "";
        return coerce(input, targetType);
    }

    private Object coerce(String raw, DataType targetType) {
        if (raw == null) {
            throw new RuntimeException("Runtime error: Input is null");
        }
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
            throw new RuntimeException("Runtime error: Cannot parse input '" + raw + "' as number");
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
        throw new RuntimeException("Runtime error: Cannot parse input '" + raw + "' as boolean");
    }
}
