package engine;

@FunctionalInterface
public interface OutputEmitter {
    void emit(String message);
}
