/*
 * My Project
 */

package syntactic.parser.literal;

import iterator.IterationStep;
import node.expression.literal.BooleanLiteralNode;
import result.CorrectResult;
import result.Result;
import syntactic.Parser;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;

public class BooleanLiteralParser implements Parser<BooleanLiteralNode> {

    @Override
    public Result<IterationStep<BooleanLiteralNode>> parse(TokenStream stream) {
        return switch (stream.consume(TokenType.BOOLEAN_LITERAL)) {
            case CorrectResult<IterationStep<Token>>(IterationStep<Token> step) -> {
                Token token = step.value();
                BooleanLiteralNode node =
                        new BooleanLiteralNode(
                                Boolean.parseBoolean(token.value()),
                                token.startPosition().line(),
                                token.startPosition().column());
                yield Result.success(new IterationStep<>(node, (TokenStream) step.next()));
            }
            default -> Result.failure("Expected boolean literal token");
        };
    }
}
