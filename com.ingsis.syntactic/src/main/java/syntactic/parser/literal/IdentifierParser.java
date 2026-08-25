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

import java.util.Set;

public class IdentifierParser implements Parser<IdentifierNode> {
    private final Set<TokenType> validTypes;

    public IdentifierParser(Set<TokenType> validTypes) {
        this.validTypes = validTypes;
    }

    public IdentifierParser() {
        this(Set.of(TokenType.IDENTIFIER, TokenType.PRINTLN));
    }

    @Override
    public Result<IterationStep<IdentifierNode>> parse(TokenStream stream) {
        return switch (stream.consume(token -> token != null && validTypes.contains(token.type()))) {
            case CorrectResult<IterationStep<Token>>(IterationStep<Token> step) -> {
                IdentifierNode identifierNode = NodeFactory.createIdentifier(step.value());
                yield Result.success(new IterationStep<>(identifierNode, (TokenStream) step.next()));
            }
            default -> Result.failure("Expected identifier token");
        };
    }
}