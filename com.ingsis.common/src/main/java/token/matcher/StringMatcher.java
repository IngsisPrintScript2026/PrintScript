/*
 * My Project
 */

package token.matcher;

import java.util.regex.Pattern;
import result.Result;
import token.TokenType;

public class StringMatcher extends AbstractTokenTypeMatcher {
    private static final Pattern PATTERN =
            Pattern.compile("^\"(?:\\\\.|[^\\\\\"])*\"|'(?:\\\\.|[^\\\\'])*'$");

    @Override
    protected Result<TokenType> doMatch(String input) {
        return PATTERN.matcher(input).matches()
                ? Result.success(TokenType.STRING_LITERAL)
                : Result.failure("No es un string literal");
    }
}
