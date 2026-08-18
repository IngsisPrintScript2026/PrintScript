package builtin;

import java.util.List;
import java.util.function.Consumer;

public interface BuiltInFunction {
    String name();
    void execute(List<Object> arguments, Consumer<String> outputEmitter);
}
