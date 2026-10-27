/*
 * My Project
 */

package common.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import position.SourceRange;
import token.Token;
import token.TokenType;

class TokenRangeUnitTest {

    @Test
    @DisplayName(
            "Given: a token with start 1:8 and end 1:13 | When: asking its range | Then: the range"
                    + " spans from start to end")
    void token_whenEndPositionKnown_thenRangeSpansStartToEnd() {
        // Given
        Token token =
                new Token(TokenType.NUMBER, "number", new Position(1, 8), new Position(1, 13));

        // When
        SourceRange range = token.range();

        // Then
        assertEquals(new Position(1, 8), range.start());
        assertEquals(new Position(1, 13), range.end());
    }

    @Test
    @DisplayName(
            "Given: a token built without end position | When: asking its range | Then: the end"
                    + " falls back to the start position")
    void token_whenEndPositionMissing_thenRangeEndFallsBackToStart() {
        // Given
        Token token = new Token(TokenType.SEMICOLON, ";", new Position(3, 5));

        // When
        SourceRange range = token.range();

        // Then
        assertEquals(new Position(3, 5), range.end());
        assertEquals("from line 3, column 5 to line 3, column 5", range.toString());
    }

    @Test
    @DisplayName(
            "Given: a token without any position | When: asking line, column and range | Then:"
                    + " line and column are -1 and the range is unknown")
    void token_whenNoPositions_thenLineColumnAreMinusOneAndRangeUnknown() {
        // Given
        Token token = new Token(TokenType.IDENTIFIER, "x", null, null);

        // When
        String rangeText = token.range().toString();

        // Then
        assertEquals(-1, token.line());
        assertEquals(-1, token.column());
        assertEquals("at an unknown position", rangeText);
    }

    @Test
    @DisplayName(
            "Given: a regular token | When: asking whether it is the null token | Then: returns"
                    + " false")
    void token_whenRegularToken_thenIsNotNull() {
        // Given
        Token token = new Token(TokenType.IDENTIFIER, "x", new Position(1, 1));

        // When
        boolean isNull = token.isNull();

        // Then
        assertFalse(isNull);
    }
}
