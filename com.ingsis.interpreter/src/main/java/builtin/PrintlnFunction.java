package builtin;

import java.util.List;
import java.util.function.Consumer;

public class PrintlnFunction implements BuiltInFunction {
    @Override
    public String name() {
        return "println";
    }

    @Override
    public void execute(List<Object> arguments, Consumer<String> outputEmitter) {
        if (!arguments.isEmpty() && outputEmitter != null) {
            Object value = arguments.get(0);
            outputEmitter.accept(String.valueOf(value));
        }
    }
}
