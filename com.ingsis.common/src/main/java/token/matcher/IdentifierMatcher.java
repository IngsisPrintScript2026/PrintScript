package token.matcher;

import result.Result;
import token.TokenType;
import java.util.regex.Pattern;

public class IdentifierMatcher extends AbstractTokenTypeMatcher {
    private static final Pattern PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    @Override
    protected Result<TokenType> doMatch(String input) {
        return PATTERN.matcher(input).matches()
                ? Result.success(TokenType.IDENTIFIER)
                : Result.failure("No es un identificador válido");
    }
}
