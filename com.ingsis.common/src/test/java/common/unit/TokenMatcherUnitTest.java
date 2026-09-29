/*
 * My Project
 */

package common.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import token.TokenType;
import token.matcher.chain.TokenMatcher;

class TokenMatcherUnitTest {

    @Test
    @DisplayName("What: TokenMatcher | When: matching keywords | Then: returns correct TokenType")
    void tokenMatcher_whenMatchingKeywords_thenReturnsExpectedTokenType() {
        // What: keyword strings
        // When & Then: matching keywords
        assertSuccess(TokenType.LET, TokenMatcher.match("let"));
        assertSuccess(TokenType.NUMBER, TokenMatcher.match("number"));
        assertSuccess(TokenType.PRINTLN, TokenMatcher.match("println"));
    }

    @Test
    @DisplayName(
            "What: TokenMatcher | When: matching symbols and operators | Then: returns correct"
                    + " TokenType")
    void tokenMatcher_whenMatchingSymbols_thenReturnsExpectedTokenType() {
        // What: symbol strings
        // When & Then: matching punctuation and operators
        assertSuccess(TokenType.PLUS, TokenMatcher.match("+"));
        assertSuccess(TokenType.COLON, TokenMatcher.match(":"));
        assertSuccess(TokenType.SEMICOLON, TokenMatcher.match(";"));
    }

    @Test
    @DisplayName(
            "What: TokenMatcher | When: matching literals | Then: returns correct literal"
                    + " TokenType")
    void tokenMatcher_whenMatchingLiterals_thenReturnsExpectedTokenType() {
        // What: literal representations
        // When & Then: matching numbers, strings, and booleans
        assertSuccess(TokenType.NUMBER_LITERAL, TokenMatcher.match("123"));
        assertSuccess(TokenType.NUMBER_LITERAL, TokenMatcher.match("3.14"));

        assertSuccess(TokenType.STRING_LITERAL, TokenMatcher.match("\"hello\""));
        assertSuccess(TokenType.STRING_LITERAL, TokenMatcher.match("'world'"));

        assertSuccess(TokenType.BOOLEAN_LITERAL, TokenMatcher.match("true"));
        assertSuccess(TokenType.BOOLEAN_LITERAL, TokenMatcher.match("false"));
    }

    @Test
    @DisplayName(
            "What: TokenMatcher | When: matching identifiers | Then: returns TokenType.IDENTIFIER")
    void tokenMatcher_whenMatchingIdentifiers_thenReturnsIdentifierTokenType() {
        // What: identifier string "myVariable"
        // When & Then: matching identifier
        assertSuccess(TokenType.IDENTIFIER, TokenMatcher.match("myVariable"));
    }

    @Test
    @DisplayName(
            "What: TokenMatcher | When: matching invalid or empty input | Then: returns failure"
                    + " result")
    void tokenMatcher_whenMatchingInvalidOrEmptyInput_thenReturnsFailureResult() {
        // What: invalid string and null/empty inputs
        // When & Then: assert failures with appropriate error descriptions
        assertFailure(TokenMatcher.match("123abcinvalid!!!"));
        assertNullOrEmptyFailure(TokenMatcher.match(null));
        assertNullOrEmptyFailure(TokenMatcher.match(""));
    }

    private void assertNullOrEmptyFailure(Result<TokenType> result) {
        switch (result) {
            case CorrectResult<TokenType> success ->
                    fail("Expected failure but got success: " + success.value());
            case IncorrectResult<TokenType> failure ->
                    assertTrue(failure.error().contains("No se permite un input null o vacio"));
        }
    }

    private void assertSuccess(TokenType expectedType, Result<TokenType> result) {
        switch (result) {
            case CorrectResult<TokenType>(var type) -> assertEquals(expectedType, type);
            case IncorrectResult<TokenType> failure ->
                    fail("Expected success but failed: " + failure.error());
        }
    }

    private void assertFailure(Result<TokenType> result) {
        switch (result) {
            case CorrectResult<TokenType> success ->
                    fail("Expected failure but got success: " + success.value());
            case IncorrectResult<TokenType> failure ->
                    assertTrue(failure.error().contains("Sin coincidencia"));
        }
    }
}
