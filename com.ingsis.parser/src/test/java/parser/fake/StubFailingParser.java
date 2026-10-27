/*
 * My Project
 */

package parser.fake;

import iterator.IterationStep;
import node.Node;
import result.Result;
import syntactic.Parser;
import tokenstream.TokenStream;

/** Parser that always fails with a fixed message, whatever the stream holds. */
public final class StubFailingParser<T extends Node> implements Parser<T> {
    private final String error;

    public StubFailingParser(String error) {
        this.error = error;
    }

    @Override
    public Result<IterationStep<T>> parse(TokenStream stream) {
        return Result.failure(error);
    }
}
