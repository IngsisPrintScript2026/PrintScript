/*
 * My Project
 */

package token.matcher;

import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import token.TokenType;

public abstract class AbstractTokenTypeMatcher implements TokenTypeMatcher {
    private TokenTypeMatcher next;

    public AbstractTokenTypeMatcher linkWith(AbstractTokenTypeMatcher next) {
        this.next = next;
        return next;
    }

    @Override
    public Result<TokenType> match(String input) {
        if (input == null || input.isEmpty()) {
            return passToNext(input, "Entrada vacía");
        }
        Result<TokenType> result = doMatch(input);
        return switch (result) {
            case CorrectResult<TokenType> success -> success;
            case IncorrectResult<TokenType> failure -> passToNext(input, failure.error());
        };
    }

    private Result<TokenType> passToNext(String input, String reason) {
        if (next != null) {
            return next.match(input);
        }
        return Result.failure("Sin coincidencia para: '" + input + "', razón: " + reason);
    }

    protected abstract Result<TokenType> doMatch(String input);
}
