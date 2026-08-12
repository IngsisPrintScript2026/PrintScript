package syntactic.parser.literal;

import iterator.IterationStep;
import node.expression.Identifier.IdentifierNode;
import node.factory.NodeFactory;
import result.CorrectResult;
import result.Result;
import syntactic.Parser;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;

public class IdentifierParser implements Parser<IdentifierNode> {

    @Override
    public Result<IterationStep<IdentifierNode>> parse(TokenStream stream) {
        return switch (stream.consume(TokenType.IDENTIFIER)) {
            case CorrectResult<IterationStep<Token>>(IterationStep<Token> step) -> {
                IdentifierNode identifierNode = NodeFactory.createIdentifier(step.value());
                yield Result.success(new IterationStep<>(identifierNode, (TokenStream) step.next()));
            }
            default -> Result.failure("Expected identifier token");
        };
    }
}