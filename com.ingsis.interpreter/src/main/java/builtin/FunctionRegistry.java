package builtin;

public interface FunctionRegistry {
    BuiltInFunction get(String name);
    boolean contains(String name);
}
