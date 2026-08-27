/*
 * My Project
 */

package tokenstream;

import iterator.IterationStep;
import java.util.List;
import java.util.function.Predicate;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import token.Token;
import token.TokenType;

public final class TokenStreamAdapter implements TokenStream {

    private final List<Token> tokens;
    private final int pointer;

    public TokenStreamAdapter(List<Token> tokens, int pointer) {
        this.tokens = List.copyOf(tokens);
        this.pointer = pointer;
    }

    @Override
    public Result<IterationStep<Token>> next() {
        return consume();
    }

    @Override
    public Result<IterationStep<Token>> consume() {
        if (isEmpty()) {
            return Result.failure("EOF: Se alcanzó el fin del flujo de tokens.");
        }
        Token currentToken = tokens.get(pointer);
        TokenStream nextStream = new TokenStreamAdapter(tokens, pointer + 1);
        return Result.success(new IterationStep<>(currentToken, nextStream));
    }

    @Override
    public Result<IterationStep<Token>> consume(TokenType expectedType) {
        return consume(token -> token != null && token.type() == expectedType);
    }

    @Override
    public Result<IterationStep<Token>> consume(Predicate<Token> matcher) {
        return switch (peek(0)) {
            case IncorrectResult<Token>(String err) -> Result.failure(err);
            case CorrectResult<Token>(Token token) ->
                    matcher.test(token) ? consume() : Result.failure("Token inesperado: " + token);
        };
    }

    @Override
    public Result<Token> peek(int offset) {
        int targetIndex = pointer + offset;
        if (targetIndex < 0 || targetIndex >= tokens.size()) {
            return Result.failure("Índice fuera de los límites del stream: " + targetIndex);
        }
        return Result.success(tokens.get(targetIndex));
    }

    @Override
    public boolean isEmpty() {
        return pointer >= tokens.size();
    }

    @Override
    public int pointer() {
        return pointer;
    }
}
