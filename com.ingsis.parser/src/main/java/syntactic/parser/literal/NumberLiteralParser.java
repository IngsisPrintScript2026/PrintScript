/*
 * My Project
 */

package syntactic.parser.literal;

import iterator.IterationStep;
import java.math.BigDecimal;
import node.expression.literal.NumberLiteralNode;
import result.CorrectResult;
import result.Result;
import syntactic.Parser;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;

public class NumberLiteralParser implements Parser<NumberLiteralNode> {

    @Override
    public Result<IterationStep<NumberLiteralNode>> parse(TokenStream stream) {
        return switch (stream.consume(TokenType.NUMBER_LITERAL)) {
            case CorrectResult<IterationStep<Token>>(IterationStep<Token> step) -> {
                Token token = step.value();
                NumberLiteralNode node =
                        new NumberLiteralNode(
                                new BigDecimal(token.value()),
                                token.startPosition().line(),
                                token.startPosition().column());
                yield Result.success(new IterationStep<>(node, (TokenStream) step.next()));
            }
            default -> Result.failure("Expected number literal token");
        };
    }
}
