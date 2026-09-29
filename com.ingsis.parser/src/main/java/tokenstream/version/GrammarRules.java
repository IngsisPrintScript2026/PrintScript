/*
 * My Project
 */

package tokenstream.version;

import java.util.function.Predicate;
import token.Token;
import token.TokenType;
import tokenstream.rules.TokenMatchers;
import version.Version;

public record GrammarRules(
        Predicate<Token> declarationKeywords,
        Predicate<Token> supportedDataTypes,
        Predicate<Token> binaryOperators) {
    public static GrammarRules fromVersion(Version version) {
        return switch (version) {
            case V_1_0 -> forV10();
            case V_1_1 -> forV11();
        };
    }

    private static GrammarRules forV10() {
        return new GrammarRules(
                TokenMatchers.isOneOf(TokenType.LET),
                TokenMatchers.isOneOf(TokenType.STRING, TokenType.NUMBER),
                commonBinaryOps());
    }

    private static GrammarRules forV11() {
        return new GrammarRules(
                TokenMatchers.isOneOf(TokenType.LET, TokenType.CONST),
                TokenMatchers.isOneOf(TokenType.STRING, TokenType.NUMBER, TokenType.BOOLEAN),
                commonBinaryOps());
    }

    private static Predicate<Token> commonBinaryOps() {
        return TokenMatchers.isOneOf(
                TokenType.PLUS, TokenType.MINUS, TokenType.STAR, TokenType.SLASH);
    }
}
