package iterator;

import result.Result;

import java.util.Iterator;

public interface SafeIterator<T> {
    Result<IterationStep<T>> next();
}