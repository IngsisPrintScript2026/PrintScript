/*
 * My Project
 */

package tokenstream;

import static org.junit.jupiter.api.Assertions.*;

import iterator.IterationStep;
import iterator.SafeIterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.Result;
import token.Token;
import token.TokenType;

public class LazyTokenStreamTest {

    private static class MockTokenIterator implements SafeIterator<Token> {
        private final List<Token> tokens;
        private final int index;
        private final String failureMessage;

        public MockTokenIterator(List<Token> tokens, int index) {
            this(tokens, index, "EOF");
        }

        public MockTokenIterator(List<Token> tokens, int index, String failureMessage) {
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
                            t, new MockTokenIterator(tokens, index + 1, failureMessage)));
        }
    }

    @Test
    void testLazyTokenStreamConsumeAndPeek() {
        Token tokenLet = new Token(TokenType.LET, "let", new Position(1, 1));
        Token tokenIdent = new Token(TokenType.IDENTIFIER, "x", new Position(1, 5));

        SafeIterator<Token> mockLexer = new MockTokenIterator(List.of(tokenLet, tokenIdent), 0);
        LazyTokenStream stream = new LazyTokenStream(mockLexer);

        assertEquals(0, stream.pointer());
        assertFalse(stream.isEmpty());
        Result<Token> peek0 = stream.peek(0);
        assertTrue(peek0.isCorrect());
        assertEquals(TokenType.LET, ((CorrectResult<Token>) peek0).value().type());

        Result<Token> peek1 = stream.peek(1);
        assertTrue(peek1.isCorrect());
        assertEquals(TokenType.IDENTIFIER, ((CorrectResult<Token>) peek1).value().type());

        // Test next() and consume()
        Result<IterationStep<Token>> consumeResult = stream.next();
        assertTrue(consumeResult.isCorrect());
        IterationStep<Token> step = ((CorrectResult<IterationStep<Token>>) consumeResult).value();
        assertEquals(TokenType.LET, step.value().type());

        TokenStream nextStream = (TokenStream) step.next();
        assertEquals(1, nextStream.pointer());
        Result<Token> nextPeek = nextStream.peek(0);
        assertTrue(nextPeek.isCorrect());
        assertEquals(TokenType.IDENTIFIER, ((CorrectResult<Token>) nextPeek).value().type());

        // consume by type and predicate
        assertTrue(nextStream.consume(TokenType.IDENTIFIER).isCorrect());
        assertFalse(nextStream.consume(TokenType.NUMBER).isCorrect());

        // Consume until EOF
        Result<IterationStep<Token>> nextCons = nextStream.consume();
        assertTrue(nextCons.isCorrect());
        TokenStream eofStream =
                (TokenStream) ((CorrectResult<IterationStep<Token>>) nextCons).value().next();
        assertTrue(eofStream.isEmpty());
        assertFalse(eofStream.consume().isCorrect());
        assertFalse(eofStream.peek(0).isCorrect());

        // Invalid peek
        assertFalse(stream.peek(-1).isCorrect());
        assertFalse(stream.peek(10).isCorrect());
    }

    @Test
    void testLazyTokenStreamLexerError() {
        SafeIterator<Token> errorLexer =
                new MockTokenIterator(List.of(), 0, "Lexical error: unexpected char '@'");
        LazyTokenStream stream = new LazyTokenStream(errorLexer);

        assertTrue(stream.isEmpty());
        assertFalse(stream.consume().isCorrect());
        assertFalse(stream.peek(0).isCorrect());
    }
}
