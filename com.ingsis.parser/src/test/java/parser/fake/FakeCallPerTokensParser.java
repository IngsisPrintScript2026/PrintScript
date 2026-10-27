/*
 * My Project
 */

package parser.fake;

import iterator.IterationStep;
import java.util.List;
import node.Node;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.Parser;
import token.Token;
import tokenstream.TokenStream;

/**
 * Simplified statement parser: each statement is a fixed number of tokens and becomes a call node
 * named after the first token, e.g. tokens {@code foo ;} become {@code foo()}.
 */
public final class FakeCallPerTokensParser implements Parser<Node> {
    private final int tokensPerStatement;

    public FakeCallPerTokensParser(int tokensPerStatement) {
        this.tokensPerStatement = tokensPerStatement;
    }

    @Override
    public Result<IterationStep<Node>> parse(TokenStream stream) {
        if (!(stream.peek(0) instanceof CorrectResult<Token>(Token first))) {
            return Result.failure("EOF");
        }
        TokenStream current = stream;
        for (int i = 0; i < tokensPerStatement; i++) {
            Result<IterationStep<Token>> consumed = current.consume();
            if (consumed instanceof IncorrectResult<IterationStep<Token>>(String err)) {
                return Result.failure(err);
            }
            current = (TokenStream) ((CorrectResult<IterationStep<Token>>) consumed).value().next();
        }
        return Result.success(new IterationStep<>(callNamed(first), current));
    }

    private static CallFunctionNode callNamed(Token token) {
        IdentifierNode id = new IdentifierNode(token.value(), token.line(), token.column());
        return new CallFunctionNode(id, List.of(), token.line(), token.column());
    }
}
