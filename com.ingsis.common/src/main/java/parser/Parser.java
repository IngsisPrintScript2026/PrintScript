/*
 * My Project
 */

package parser;

import iterator.IterationStep;
import result.Result;
import tokenstream.TokenStream;

@FunctionalInterface
public interface Parser<T> {
    Result<IterationStep<T>> parse(TokenStream stream);
}
