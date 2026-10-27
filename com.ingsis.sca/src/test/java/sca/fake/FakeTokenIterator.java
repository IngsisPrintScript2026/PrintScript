/*
 * My Project
 */

package sca.fake;

import iterator.IterationStep;
import iterator.SafeIterator;
import java.util.List;
import result.Result;
import token.Token;

/** List-backed token iterator; once exhausted it fails with the configured terminal error. */
public final class FakeTokenIterator implements SafeIterator<Token> {
    private final List<Token> tokens;
    private final int position;
    private final String terminalError;

    private FakeTokenIterator(List<Token> tokens, int position, String terminalError) {
        this.tokens = List.copyOf(tokens);
        this.position = position;
        this.terminalError = terminalError;
    }

    public static FakeTokenIterator of(List<Token> tokens) {
        return new FakeTokenIterator(tokens, 0, "EOF");
    }

    public static FakeTokenIterator endingWith(List<Token> tokens, String terminalError) {
        return new FakeTokenIterator(tokens, 0, terminalError);
    }

    @Override
    public Result<IterationStep<Token>> next() {
        if (position >= tokens.size()) {
            return Result.failure(terminalError);
        }
        FakeTokenIterator rest = new FakeTokenIterator(tokens, position + 1, terminalError);
        return Result.success(new IterationStep<>(tokens.get(position), rest));
    }
}
