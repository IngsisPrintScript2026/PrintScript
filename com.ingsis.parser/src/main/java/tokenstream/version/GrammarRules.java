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
            case V_1_0 ->
                    new GrammarRules(
                            TokenMatchers.isOneOf(TokenType.LET),
                            TokenMatchers.isOneOf(TokenType.STRING, TokenType.NUMBER),
                            TokenMatchers.isOneOf(
                                    TokenType.PLUS,
                                    TokenType.MINUS,
                                    TokenType.STAR,
                                    TokenType.SLASH));
            case V_1_1 ->
                    new GrammarRules(
                            TokenMatchers.isOneOf(TokenType.LET, TokenType.CONST),
                            TokenMatchers.isOneOf(
                                    TokenType.STRING, TokenType.NUMBER, TokenType.BOOLEAN),
                            TokenMatchers.isOneOf(
                                    TokenType.PLUS,
                                    TokenType.MINUS,
                                    TokenType.STAR,
                                    TokenType.SLASH));
        };
    }
}
