package engine;

@FunctionalInterface
public interface InputSupplier {
    String readInput(String prompt);
}
