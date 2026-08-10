package token.matcher;

import result.Result;
import token.TokenType;
import java.util.regex.Pattern;

public class NumberMatcher extends AbstractTokenTypeMatcher {
    private static final Pattern PATTERN = Pattern.compile("^\\d+(\\.\\d+)?$");

    @Override
    protected Result<TokenType> doMatch(String input) {
        return PATTERN.matcher(input).matches()
                ? Result.success(TokenType.NUMBER_LITERAL)
                : Result.failure("No es un número válido");
    }
}
