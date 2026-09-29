/*
 * My Project
 */

package common.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.Result;
import token.TokenType;
import token.matcher.chain.TokenMatcher;

class TokenMatcherIntegrationTest {

    @Test
    @DisplayName(
            "What: TokenMatcher chain | When: matching full suite of language lexemes | Then: maps"
                    + " every lexeme to correct TokenType")
    void tokenMatcherChain_whenMatchingFullSuiteOfLexemes_thenMapsEveryLexemeAccurately() {
        // What: map of expected lexemes to TokenTypes
        Map<String, TokenType> expectedMatches =
                Map.ofEntries(
                        Map.entry("let", TokenType.LET),
                        Map.entry("number", TokenType.NUMBER),
                        Map.entry("println", TokenType.PRINTLN),
                        Map.entry("+", TokenType.PLUS),
                        Map.entry(":", TokenType.COLON),
                        Map.entry(";", TokenType.SEMICOLON),
                        Map.entry("123", TokenType.NUMBER_LITERAL),
                        Map.entry("3.14", TokenType.NUMBER_LITERAL),
                        Map.entry("\"hello\"", TokenType.STRING_LITERAL),
                        Map.entry("'world'", TokenType.STRING_LITERAL),
                        Map.entry("true", TokenType.BOOLEAN_LITERAL),
                        Map.entry("false", TokenType.BOOLEAN_LITERAL),
                        Map.entry("myVariable", TokenType.IDENTIFIER));

        // When & Then: iterating through each lexeme through TokenMatcher chain
        for (Map.Entry<String, TokenType> entry : expectedMatches.entrySet()) {
            Result<TokenType> result = TokenMatcher.match(entry.getKey());
            assertTrue(result.isCorrect(), "Should match: " + entry.getKey());
            assertEquals(entry.getValue(), ((CorrectResult<TokenType>) result).value());
        }
    }
}
