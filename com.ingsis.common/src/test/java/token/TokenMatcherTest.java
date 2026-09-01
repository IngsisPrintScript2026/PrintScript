/*
 * My Project
 */

package token;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import token.matcher.chain.TokenMatcher;

public class TokenMatcherTest {

    @Test
    void testChainTokenMatchingWithResultAndPatternMatching() {
        assertSuccess(TokenType.LET, TokenMatcher.match("let"));
        assertSuccess(TokenType.NUMBER, TokenMatcher.match("number"));
        assertSuccess(TokenType.PRINTLN, TokenMatcher.match("println"));

        assertSuccess(TokenType.PLUS, TokenMatcher.match("+"));
        assertSuccess(TokenType.COLON, TokenMatcher.match(":"));
        assertSuccess(TokenType.SEMICOLON, TokenMatcher.match(";"));

        assertSuccess(TokenType.NUMBER_LITERAL, TokenMatcher.match("123"));
        assertSuccess(TokenType.NUMBER_LITERAL, TokenMatcher.match("3.14"));

        assertSuccess(TokenType.STRING_LITERAL, TokenMatcher.match("\"hello\""));
        assertSuccess(TokenType.STRING_LITERAL, TokenMatcher.match("'world'"));

        assertSuccess(TokenType.BOOLEAN_LITERAL, TokenMatcher.match("true"));
        assertSuccess(TokenType.BOOLEAN_LITERAL, TokenMatcher.match("false"));

        assertSuccess(TokenType.IDENTIFIER, TokenMatcher.match("myVariable"));

        assertFailure(TokenMatcher.match("123abcinvalid!!!"));
        assertNullOrEmptyFailure(TokenMatcher.match(null));
        assertNullOrEmptyFailure(TokenMatcher.match(""));
    }

    private void assertNullOrEmptyFailure(Result<TokenType> result) {
        switch (result) {
            case CorrectResult<TokenType> success ->
                    fail("Se esperaba fallo pero fue exitoso: " + success.value());
            case IncorrectResult<TokenType> failure ->
                    assertTrue(failure.error().contains("No se permite un input null o vacio"));
        }
    }

    private void assertSuccess(TokenType expectedType, Result<TokenType> result) {
        switch (result) {
            case CorrectResult<TokenType>(var type) -> assertEquals(expectedType, type);
            case IncorrectResult<TokenType> failure ->
                    fail("Se esperaba éxito pero falló: " + failure.error());
        }
    }

    private void assertFailure(Result<TokenType> result) {
        switch (result) {
            case CorrectResult<TokenType> success ->
                    fail("Se esperaba fallo pero fue exitoso: " + success.value());
            case IncorrectResult<TokenType> failure ->
                    assertTrue(failure.error().contains("Sin coincidencia"));
        }
    }
}
