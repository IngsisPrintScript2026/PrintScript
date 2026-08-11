package tokenstream.rules;

import token.Token;
import token.TokenType;

import java.util.Set;
import java.util.function.Predicate;

public final class TokenMatchers {

    private TokenMatchers() {}

    /* Coincidencia por tipo de token */
    public static Predicate<Token> isType(TokenType type) {
        return token -> token != null && token.type() == type;
    }

    /* Coincidencia si el token pertenece a un conjunto de tipos (ej. operadores +, -, *, /) */
    public static Predicate<Token> isOneOf(TokenType... types) {
        Set<TokenType> typeSet = Set.of(types);
        return token -> token != null && typeSet.contains(token.type());
    }

    /* Coincidencia por tipo y valor exacto de su lexema */
    public static Predicate<Token> isTypeAndValue(TokenType type, String expectedValue) {
        return token -> isType(type).test(token) && expectedValue.equals(token.value());
    }
}
