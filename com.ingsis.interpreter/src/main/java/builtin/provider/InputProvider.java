package builtin.provider;

@FunctionalInterface
public interface InputProvider {
    String readInput(String prompt);
}
