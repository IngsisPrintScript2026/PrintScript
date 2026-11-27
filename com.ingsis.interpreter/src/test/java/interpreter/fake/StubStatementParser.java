/*
 * My Project
 */

package interpreter.fake;

import iterator.IterationStep;
import java.util.List;
import node.StatementNode;
import result.CorrectResult;
import result.Result;
import syntactic.Parser;
import tokenstream.TokenStream;

public final class StubStatementParser implements Parser<StatementNode> {
    private final List<StatementNode> statements;
    private final String error;

    private StubStatementParser(List<StatementNode> statements, String error) {
        this.statements = List.copyOf(statements);
        this.error = error;
    }

    public static StubStatementParser returning(StatementNode... statements) {
        return new StubStatementParser(List.of(statements), null);
    }

    public static StubStatementParser failingWith(String error) {
        return new StubStatementParser(List.of(), error);
    }

    @Override
    public Result<IterationStep<StatementNode>> parse(TokenStream stream) {
        if (error != null) {
            return Result.failure(error);
        }
        if (stream.pointer() >= statements.size()) {
            return Result.failure("EOF");
        }
        StatementNode statement = statements.get(stream.pointer());
        TokenStream next =
                (TokenStream)
                        ((CorrectResult<IterationStep<token.Token>>) stream.consume())
                                .value()
                                .next();
        return Result.success(new IterationStep<>(statement, next));
    }
}
