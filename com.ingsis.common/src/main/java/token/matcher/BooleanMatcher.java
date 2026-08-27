/*
 * My Project
 */

package token.matcher;

import result.Result;
import token.TokenType;

public class BooleanMatcher extends AbstractTokenTypeMatcher {
    @Override
    protected Result<TokenType> doMatch(String input) {
        if ("true".equalsIgnoreCase(input) || "false".equalsIgnoreCase(input)) {
            return Result.success(TokenType.BOOLEAN_LITERAL);
        }
        return Result.failure("No es un booleano");
    }
}
