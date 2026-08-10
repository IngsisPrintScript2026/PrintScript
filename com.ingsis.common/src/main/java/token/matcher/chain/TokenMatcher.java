package token.matcher.chain;

import result.Result;
import token.TokenType;
import token.matcher.TokenTypeMatcher;

public final class TokenMatcher {
    private static final TokenTypeMatcher CHAIN = ChainTokenTypeMatcher.defaultChain();
    public static Result<TokenType> match(String input) {
        if (input == null || input.isEmpty()) {
            return Result.failure("No se permite un input null o vacio");
        }
        return CHAIN.match(input);
    }
}
