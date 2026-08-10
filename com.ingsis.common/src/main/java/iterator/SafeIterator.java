package iterator;

import result.Result;

public interface SafeIterator<T> {
    Result<IterationStep<T>> next();
    default void unread(T item) {}
}