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
import parser.fake.FakeTokenIterator;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import token.Token;
import token.TokenType;
import tokenstream.LazyTokenStream;
import tokenstream.TokenStream;

class LazyTokenStreamErrorsUnitTest {

    private static final String LEXICAL =
            "Lexical error: unrecognized character '@' at line 1, column 3";

    private static String errorOf(Result<?> result) {
        assertFalse(result.isCorrect());
        return ((IncorrectResult<?>) result).error();
    }

    private static TokenStream afterFirst(TokenStream stream) {
        return (TokenStream)
                ((CorrectResult<IterationStep<Token>>) stream.consume()).value().next();
    }

    @Test
    @DisplayName(
            "Given: a lexer whose head token is 'x' | When: consuming a '=' | Then: fails naming"
                    + " 'x' and its range")
    void lazyTokenStream_whenHeadDoesNotMatch_thenReportsUnexpectedTokenWithRange() {
        // Given
        TokenStream stream =
                new LazyTokenStream(
                        new FakeTokenIterator(List.of(token(TokenType.IDENTIFIER, "x", 1)), "EOF"));

        // When
        Result<IterationStep<Token>> result = stream.consume(TokenType.EQUAL);

        // Then
        assertEquals(
                "Unexpected token 'x' from line 1, column 1 to line 1, column 1", errorOf(result));
    }

    @Test
    @DisplayName(
            "Given: a lexer that is already at EOF | When: consuming a ';' | Then: fails with"
                    + " 'Unexpected end of file'")
    void lazyTokenStream_whenConsumingTypeAtEof_thenReportsUnexpectedEndOfFile() {
        // Given
        TokenStream stream = new LazyTokenStream(new FakeTokenIterator(List.of(), "EOF"));

        // When
        Result<IterationStep<Token>> result = stream.consume(TokenType.SEMICOLON);

        // Then
        assertEquals("Unexpected end of file", errorOf(result));
    }

    @Test
    @DisplayName(
            "Given: a lexer that fails with an already-labelled lexical error | When: consuming a"
                    + " ';' | Then: the error passes through without a double prefix")
    void lazyTokenStream_whenLexerErrorIsLabelled_thenPassesThroughUnchanged() {
        // Given
        TokenStream stream =
                afterFirst(
                        new LazyTokenStream(
                                new FakeTokenIterator(
                                        List.of(token(TokenType.IDENTIFIER, "x", 1)), LEXICAL)));

        // When
        Result<IterationStep<Token>> result = stream.consume(TokenType.SEMICOLON);

        // Then
        assertEquals(LEXICAL, errorOf(result));
    }

    @Test
    @DisplayName(
            "Given: a lexer that fails with a raw message | When: consuming any token | Then: the"
                    + " message is prefixed with 'Lexical error: '")
    void lazyTokenStream_whenLexerErrorIsRaw_thenPrefixesLexicalError() {
        // Given
        TokenStream stream =
                afterFirst(
                        new LazyTokenStream(
                                new FakeTokenIterator(
                                        List.of(token(TokenType.IDENTIFIER, "x", 1)), "bad char")));

        // When
        Result<IterationStep<Token>> result = stream.consume();

        // Then
        assertEquals("Lexical error: bad char", errorOf(result));
    }

    @Test
    @DisplayName(
            "Given: a lexer that fails after one token | When: peeking past that token | Then: the"
                    + " peek reports the lexical error")
    void lazyTokenStream_whenPeekingIntoLexicalError_thenReportsIt() {
        // Given
        TokenStream stream =
                new LazyTokenStream(
                        new FakeTokenIterator(
                                List.of(token(TokenType.IDENTIFIER, "x", 1)), "bad char"));

        // When
        Result<Token> result = stream.peek(1);

        // Then
        assertTrue(errorOf(result).startsWith("Lexical error"));
    }

    @Test
    @DisplayName(
            "Given: a stream of two tokens | When: peeking a negative offset or beyond EOF | Then:"
                    + " both peeks fail")
    void lazyTokenStream_whenPeekingOutOfBounds_thenFails() {
        // Given
        TokenStream stream =
                new LazyTokenStream(
                        new FakeTokenIterator(
                                List.of(
                                        token(TokenType.IDENTIFIER, "x", 1),
                                        token(TokenType.SEMICOLON, ";", 2)),
                                "EOF"));

        // When
        Result<Token> negative = stream.peek(-1);
        Result<Token> beyond = stream.peek(5);

        // Then
        assertFalse(negative.isCorrect());
        assertFalse(beyond.isCorrect());
    }

    @Test
    @DisplayName(
            "Given: a stream of one token | When: consuming it | Then: the next stream is empty and"
                    + " its pointer moved by one")
    void lazyTokenStream_whenLastTokenConsumed_thenNextStreamIsEmpty() {
        // Given
        TokenStream stream =
                new LazyTokenStream(
                        new FakeTokenIterator(List.of(token(TokenType.SEMICOLON, ";", 1)), "EOF"));

        // When
        TokenStream next = afterFirst(stream);

        // Then
        assertTrue(next.isEmpty());
        assertEquals(1, next.pointer());
    }
}
