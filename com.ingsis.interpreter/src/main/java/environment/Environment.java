package environment;

import node.expression.literal.DataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Environment {
    private final Environment parent;
    private final Map<String, VariableInfo> variables = new HashMap<>();

    public record VariableInfo(Object value, DataType type, boolean isMutable) {}

    public Environment() {
        this.parent = null;
    }

    public Environment(Environment parent) {
        this.parent = parent;
    }

    public void declare(String name, Object value, DataType type, boolean isMutable) {
        if (variables.containsKey(name)) {
            throw new RuntimeException("Variable '" + name + "' has already been declared in this scope.");
        }
        variables.put(name, new VariableInfo(value, type, isMutable));
    }

    public void assign(String name, Object value) {
        if (variables.containsKey(name)) {
            VariableInfo current = variables.get(name);
            if (!current.isMutable()) {
                throw new RuntimeException("Cannot reassign constant variable '" + name + "'.");
            }
            variables.put(name, new VariableInfo(value, current.type(), true));
            return;
        }
        if (parent != null) {
            parent.assign(name, value);
            return;
        }
        throw new RuntimeException("Variable '" + name + "' is not defined.");
    }

    public VariableInfo get(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        if (parent != null) {
            return parent.get(name);
        }
        throw new RuntimeException("Variable '" + name + "' not found.");
    }

    public Optional<VariableInfo> find(String name) {
        if (variables.containsKey(name)) {
            return Optional.of(variables.get(name));
        }
        if (parent != null) {
            return parent.find(name);
        }
        return Optional.empty();
    }
}
