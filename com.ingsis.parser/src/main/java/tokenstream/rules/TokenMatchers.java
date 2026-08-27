/*
 * My Project
 */

package tokenstream.rules;

import java.util.Set;
import java.util.function.Predicate;
import token.Token;
import token.TokenType;

public final class TokenMatchers {
    private TokenMatchers() {}

    public static Predicate<Token> isType(TokenType type) {
        return token -> token != null && token.type() == type;
    }

    public static Predicate<Token> isOneOf(TokenType... types) {
        Set<TokenType> typeSet = Set.of(types);
        return token -> token != null && typeSet.contains(token.type());
    }

    public static Predicate<Token> isTypeAndValue(TokenType type, String expectedValue) {
        return token -> isType(type).test(token) && expectedValue.equals(token.value());
    }
}
