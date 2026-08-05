package iterator;

public record IterationStep<T>(T value, SafeIterator<T> next) {}
