/*
 * My Project
 */

package parser.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iterator.IterationStep;
import iterator.SafeIterator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.Result;
import token.Token;
import token.TokenType;
import tokenstream.LazyTokenStream;
import tokenstream.TokenStream;

class LazyTokenStreamUnitTest {

    private static class FakeTokenIterator implements SafeIterator<Token> {
        private final List<Token> tokens;
        private final int index;
        private final String failureMessage;

        public FakeTokenIterator(List<Token> tokens, int index) {
            this(tokens, index, "EOF");
        }

        public FakeTokenIterator(List<Token> tokens, int index, String failureMessage) {
            this.tokens = tokens;
            this.index = index;
            this.failureMessage = failureMessage;
        }

        @Override
        public Result<IterationStep<Token>> next() {
            if (index >= tokens.size()) {
                return Result.failure(failureMessage);
            }
            Token t = tokens.get(index);
            return Result.success(
                    new IterationStep<>(
                            t, new FakeTokenIterator(tokens, index + 1, failureMessage)));
        }
    }

    @Test
    @DisplayName(
            "What: LazyTokenStream with FakeTokenIterator | When: peeking and consuming tokens |"
                    + " Then: navigates stream properly")
    void lazyTokenStream_whenPeekingAndConsuming_thenNavigatesStreamProperly() {
        // What: tokens and LazyTokenStream initialized using FakeTokenIterator
        Token tokenLet = new Token(TokenType.LET, "let", new Position(1, 1));
        Token tokenIdent = new Token(TokenType.IDENTIFIER, "x", new Position(1, 5));
        SafeIterator<Token> fakeLexer = new FakeTokenIterator(List.of(tokenLet, tokenIdent), 0);
        LazyTokenStream stream = new LazyTokenStream(fakeLexer);

        // When & Then: verifying pointer and peeks
        assertEquals(0, stream.pointer());
        assertFalse(stream.isEmpty());

        Result<Token> peek0 = stream.peek(0);
        assertTrue(peek0.isCorrect());
        assertEquals(TokenType.LET, ((CorrectResult<Token>) peek0).value().type());

        Result<Token> peek1 = stream.peek(1);
        assertTrue(peek1.isCorrect());
        assertEquals(TokenType.IDENTIFIER, ((CorrectResult<Token>) peek1).value().type());

        // When: consuming first token
        Result<IterationStep<Token>> consumeResult = stream.next();

        // Then: consumed token is LET and next stream is positioned at index 1
        assertTrue(consumeResult.isCorrect());
        IterationStep<Token> step = ((CorrectResult<IterationStep<Token>>) consumeResult).value();
        assertEquals(TokenType.LET, step.value().type());

        TokenStream nextStream = (TokenStream) step.next();
        assertEquals(1, nextStream.pointer());
        Result<Token> nextPeek = nextStream.peek(0);
        assertTrue(nextPeek.isCorrect());
        assertEquals(TokenType.IDENTIFIER, ((CorrectResult<Token>) nextPeek).value().type());

        // When & Then: consume by type and invalid peek
        assertTrue(nextStream.consume(TokenType.IDENTIFIER).isCorrect());
        assertFalse(nextStream.consume(TokenType.NUMBER).isCorrect());

        // When & Then: consume until EOF
        Result<IterationStep<Token>> nextCons = nextStream.consume();
        assertTrue(nextCons.isCorrect());
        TokenStream eofStream =
                (TokenStream) ((CorrectResult<IterationStep<Token>>) nextCons).value().next();
        assertTrue(eofStream.isEmpty());
        assertFalse(eofStream.consume().isCorrect());
        assertFalse(eofStream.peek(0).isCorrect());

        // Out of bounds peeks
        assertFalse(stream.peek(-1).isCorrect());
        assertFalse(stream.peek(10).isCorrect());
    }

    @Test
    @DisplayName(
            "What: LazyTokenStream with FakeTokenIterator | When: fake lexer emits error | Then:"
                    + " stream handles error gracefully")
    void lazyTokenStream_whenFakeLexerEmitsError_thenHandlesGracefully() {
        // What: FakeTokenIterator reporting error
        SafeIterator<Token> errorLexer =
                new FakeTokenIterator(List.of(), 0, "Lexical error: unexpected char '@'");
        LazyTokenStream stream = new LazyTokenStream(errorLexer);

        // When: querying empty/consume/peek
        boolean empty = stream.isEmpty();
        Result<IterationStep<Token>> consumeRes = stream.consume();
        Result<Token> peekRes = stream.peek(0);

        // Then: stream is empty and operations fail
        assertTrue(empty);
        assertFalse(consumeRes.isCorrect());
        assertFalse(peekRes.isCorrect());
    }
}
