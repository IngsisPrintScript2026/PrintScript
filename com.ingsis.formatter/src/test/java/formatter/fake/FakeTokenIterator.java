/*
 * My Project
 */

package formatter.fake;

import iterator.IterationStep;
import iterator.SafeIterator;
import java.util.List;
import result.Result;
import token.Token;

/** List-backed token iterator; once the tokens run out it fails with the configured error. */
public final class FakeTokenIterator implements SafeIterator<Token> {
    private final List<Token> tokens;
    private final int index;
    private final String terminalError;

    public FakeTokenIterator(List<Token> tokens, String terminalError) {
        this(List.copyOf(tokens), 0, terminalError);
    }

    public FakeTokenIterator(List<Token> tokens) {
        this(tokens, "EOF");
    }

    private FakeTokenIterator(List<Token> tokens, int index, String terminalError) {
        this.tokens = tokens;
        this.index = index;
        this.terminalError = terminalError;
    }

    @Override
    public Result<IterationStep<Token>> next() {
        if (index >= tokens.size()) {
            return Result.failure(terminalError);
        }
        FakeTokenIterator rest = new FakeTokenIterator(tokens, index + 1, terminalError);
        return Result.success(new IterationStep<>(tokens.get(index), rest));
    }
}
