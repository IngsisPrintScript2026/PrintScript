package builtin;

import builtin.provider.InputProvider;
import node.expression.literal.DataType;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

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

    public Object evaluate(List<Object> arguments, DataType targetType, Consumer<String> outputEmitter) {
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
        if (targetType == null) targetType = DataType.STRING;

        return switch (targetType) {
            case STRING -> raw;
            case NUMBER -> {
                try {
                    yield new BigDecimal(raw.trim());
                } catch (Exception e) {
                    throw new RuntimeException("Runtime error: Cannot parse input '" + raw + "' as number");
                }
            }
            case BOOLEAN -> {
                String clean = raw.trim().toLowerCase();
                if (clean.equals("true")) yield Boolean.TRUE;
                if (clean.equals("false")) yield Boolean.FALSE;
                throw new RuntimeException("Runtime error: Cannot parse input '" + raw + "' as boolean");
            }
        };
    }
}
