/*
 * My Project
 */

package parser.fake;

import iterator.IterationStep;
import iterator.SafeIterator;
import java.util.List;
import result.Result;
import token.Token;

/** Lexer double: yields the given tokens and then fails with {@code endMessage}. */
public final class FakeTokenIterator implements SafeIterator<Token> {
    private final List<Token> tokens;
    private final int index;
    private final String endMessage;

    public FakeTokenIterator(List<Token> tokens, String endMessage) {
        this(List.copyOf(tokens), 0, endMessage);
    }

    private FakeTokenIterator(List<Token> tokens, int index, String endMessage) {
        this.tokens = tokens;
        this.index = index;
        this.endMessage = endMessage;
    }

    @Override
    public Result<IterationStep<Token>> next() {
        if (index >= tokens.size()) {
            return Result.failure(endMessage);
        }
        return Result.success(
                new IterationStep<>(
                        tokens.get(index), new FakeTokenIterator(tokens, index + 1, endMessage)));
    }
}
