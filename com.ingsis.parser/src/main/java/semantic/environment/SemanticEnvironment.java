package semantic.environment;

import node.expression.literal.DataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SemanticEnvironment {
    private final SemanticEnvironment parent;
    private final Map<String, VariableSymbol> symbols = new HashMap<>();

    public record VariableSymbol(DataType type, boolean isMutable, boolean isInitialized) {}

    public SemanticEnvironment() {
        this.parent = null;
    }

    public SemanticEnvironment(SemanticEnvironment parent) {
        this.parent = parent;
    }

    public SemanticEnvironment define(String name, DataType type, boolean isMutable, boolean isInitialized) {
        SemanticEnvironment newEnv = new SemanticEnvironment(this.parent);
        newEnv.symbols.putAll(this.symbols);
        newEnv.symbols.put(name, new VariableSymbol(type, isMutable, isInitialized));
        return newEnv;
    }

    public Optional<VariableSymbol> lookup(String name) {
        if (symbols.containsKey(name)) {
            return Optional.of(symbols.get(name));
        }
        if (parent != null) {
            return parent.lookup(name);
        }
        return Optional.empty();
    }
}
