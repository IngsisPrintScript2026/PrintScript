/*
 * My Project
 */

package parser.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static parser.support.Sources.token;

import iterator.IterationStep;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;
import tokenstream.TokenStreamAdapter;

class TokenStreamAdapterUnitTest {

    private static TokenStream streamOf(Token... tokens) {
        return new TokenStreamAdapter(List.of(tokens), 0);
    }

    private static String errorOf(Result<?> result) {
        assertFalse(result.isCorrect());
        return ((IncorrectResult<?>) result).error();
    }

    @Test
    @DisplayName(
            "Given: a stream whose head is 'x' | When: consuming a ';' | Then: fails naming the"
                    + " token and its range")
    void tokenStreamAdapter_whenHeadDoesNotMatchType_thenReportsUnexpectedTokenWithRange() {
        // Given
        TokenStream stream = streamOf(token(TokenType.IDENTIFIER, "xyz", 5));

        // When
        Result<IterationStep<Token>> result = stream.consume(TokenType.SEMICOLON);

        // Then
        assertEquals(
                "Unexpected token 'xyz' from line 1, column 5 to line 1, column 7",
                errorOf(result));
    }

    @Test
    @DisplayName(
            "Given: an exhausted stream | When: consuming a ';' | Then: fails with 'Unexpected end"
                    + " of file'")
    void tokenStreamAdapter_whenConsumingTypeAtEnd_thenReportsUnexpectedEndOfFile() {
        // Given
        TokenStream stream = streamOf();

        // When
        Result<IterationStep<Token>> result = stream.consume(TokenType.SEMICOLON);

        // Then
        assertEquals("Unexpected end of file", errorOf(result));
    }

    @Test
    @DisplayName(
            "Given: a stream with a matching head | When: consuming with a predicate | Then:"
                    + " returns the token and advances the pointer")
    void tokenStreamAdapter_whenPredicateMatches_thenConsumesAndAdvances() {
        // Given
        TokenStream stream = streamOf(token(TokenType.LET, "let", 1));

        // When
        Result<IterationStep<Token>> result = stream.consume(t -> t.type() == TokenType.LET);

        // Then
        IterationStep<Token> step = ((CorrectResult<IterationStep<Token>>) result).value();
        assertEquals("let", step.value().value());
        assertEquals(1, ((TokenStream) step.next()).pointer());
        assertTrue(((TokenStream) step.next()).isEmpty());
    }

    @Test
    @DisplayName(
            "Given: an exhausted stream | When: consuming any token | Then: fails with an EOF"
                    + " message")
    void tokenStreamAdapter_whenConsumingAnyAtEnd_thenFailsWithEof() {
        // Given
        TokenStream stream = streamOf();

        // When
        Result<IterationStep<Token>> result = stream.next();

        // Then
        assertTrue(errorOf(result).startsWith("EOF"));
    }

    @Test
    @DisplayName(
            "Given: a one-token stream | When: peeking past the end and before the start | Then:"
                    + " both peeks fail")
    void tokenStreamAdapter_whenPeekingOutOfBounds_thenFails() {
        // Given
        TokenStream stream = streamOf(token(TokenType.SEMICOLON, ";", 1));

        // When
        Result<Token> beyond = stream.peek(1);
        Result<Token> before = stream.peek(-1);

        // Then
        assertFalse(beyond.isCorrect());
        assertFalse(before.isCorrect());
    }

    @Test
    @DisplayName(
            "Given: a stream created at a later pointer | When: peeking offset 0 | Then: returns"
                    + " the token at that pointer")
    void tokenStreamAdapter_whenCreatedWithPointer_thenPeeksFromPointer() {
        // Given
        List<Token> tokens =
                List.of(token(TokenType.IDENTIFIER, "a", 1), token(TokenType.SEMICOLON, ";", 2));
        TokenStream stream = new TokenStreamAdapter(tokens, 1);

        // When
        Result<Token> head = stream.peek(0);

        // Then
        assertEquals(";", ((CorrectResult<Token>) head).value().value());
        assertEquals(1, stream.pointer());
    }
}
