/*
 * My Project
 */

package interpreter.fake;

import iterator.IterationStep;
import java.util.List;
import node.Node;
import result.CorrectResult;
import result.Result;
import syntactic.Parser;
import tokenstream.TokenStream;

/**
 * Parser that answers the i-th canned statement for the i-th token of the stream (one token per
 * statement), or a fixed error when one is configured.
 */
public final class StubStatementParser implements Parser<Node> {
    private final List<Node> statements;
    private final String error;

    private StubStatementParser(List<Node> statements, String error) {
        this.statements = List.copyOf(statements);
        this.error = error;
    }

    public static StubStatementParser returning(Node... statements) {
        return new StubStatementParser(List.of(statements), null);
    }

    public static StubStatementParser failingWith(String error) {
        return new StubStatementParser(List.of(), error);
    }

    @Override
    public Result<IterationStep<Node>> parse(TokenStream stream) {
        if (error != null) {
            return Result.failure(error);
        }
        if (stream.pointer() >= statements.size()) {
            return Result.failure("EOF");
        }
        Node statement = statements.get(stream.pointer());
        TokenStream next =
                (TokenStream)
                        ((CorrectResult<IterationStep<token.Token>>) stream.consume())
                                .value()
                                .next();
        return Result.success(new IterationStep<>(statement, next));
    }
}
