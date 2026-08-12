package iterator;

public record IterationStep<T>(T value, SafeIterator<?> next) {
    @SuppressWarnings("unchecked")
    public <S extends SafeIterator<?>> S nextStream() {
        return (S) next;
    }
}


