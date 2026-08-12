package syntactic.parser.literal;

import iterator.IterationStep;
import node.expression.literal.StringLiteralNode;
import result.CorrectResult;
import result.Result;
import syntactic.Parser;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;

public class StringLiteralParser implements Parser<StringLiteralNode> {

    @Override
    public Result<IterationStep<StringLiteralNode>> parse(TokenStream stream) {
        return switch (stream.consume(TokenType.STRING_LITERAL)) {
            case CorrectResult<IterationStep<Token>>(IterationStep<Token> step) -> {
                Token token = step.value();
                String raw = token.value();
                String cleanVal = (raw.startsWith("\"") && raw.endsWith("\"")) || (raw.startsWith("'") && raw.endsWith("'"))
                        ? raw.substring(1, raw.length() - 1)
                        : raw;
                StringLiteralNode node = new StringLiteralNode(
                        cleanVal,
                        token.startPosition().line(),
                        token.startPosition().column()
                );
                yield Result.success(new IterationStep<>(node, (TokenStream) step.next()));
            }
            default -> Result.failure("Expected string literal token");
        };
    }
}