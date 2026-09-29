/*
 * My Project
 */

package lexer.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import charstream.CharStream;
import charstream.StreamCharReader;
import iterator.IterationStep;
import java.io.StringReader;
import lexer.Lexer;
import metaChar.MetaCharStringBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import token.Token;
import token.TokenType;
import token.tokenize.TokenizeResult;
import token.tokenizer.Tokenizer;

class LexerUnitTest {

    private static class FakeTokenizer implements Tokenizer {
        private final TokenizeResult fixedResult;

        FakeTokenizer(TokenizeResult fixedResult) {
            this.fixedResult = fixedResult;
        }

        @Override
        public TokenizeResult tokenize(MetaCharStringBuilder input) {
            return fixedResult;
        }
    }

    @Test
    @DisplayName(
            "What: Lexer | When: stream contains single identifier with whitespace | Then: skips"
                    + " whitespace and produces token")
    void lexer_whenStreamContainsWhitespaceAndToken_thenSkipsWhitespaceAndEmitsToken() {
        // What: CharStream with leading spaces "   let"
        StreamCharReader reader = new StreamCharReader(new StringReader("   let"));
        CharStream charStream = new CharStream(reader);
        Lexer lexer = new Lexer(charStream);

        // When: requesting next token
        Result<IterationStep<Token>> res = lexer.next();

        // Then: returns LET token
        assertTrue(res.isCorrect());
        IterationStep<Token> step = ((CorrectResult<IterationStep<Token>>) res).value();
        assertEquals(TokenType.LET, step.value().type());
        assertEquals("let", step.value().value());
    }

    @Test
    @DisplayName("What: Lexer | When: stream is empty | Then: returns failure result with EOF")
    void lexer_whenStreamIsEmpty_thenReturnsFailureWithEof() {
        // What: CharStream with empty string
        StreamCharReader reader = new StreamCharReader(new StringReader(""));
        CharStream charStream = new CharStream(reader);
        Lexer lexer = new Lexer(charStream);

        // When: requesting next token
        Result<IterationStep<Token>> res = lexer.next();

        // Then: returns failure with "EOF"
        assertFalse(res.isCorrect());
        assertEquals("EOF", ((IncorrectResult<IterationStep<Token>>) res).error());
    }

    @Test
    @DisplayName(
            "What: Lexer with FakeTokenizer | When: fake tokenizer returns Invalid | Then: returns"
                    + " lexical error failure")
    void lexer_whenFakeTokenizerReturnsInvalid_thenReturnsLexicalErrorFailure() {
        // What: Lexer using FakeTokenizer configured to return Invalid
        StreamCharReader reader = new StreamCharReader(new StringReader("x"));
        CharStream charStream = new CharStream(reader);
        FakeTokenizer fakeTokenizer =
                new FakeTokenizer(new TokenizeResult.Invalid("Unrecognized character"));
        Lexer lexer = new Lexer(charStream, fakeTokenizer);

        // When: requesting next token
        Result<IterationStep<Token>> res = lexer.next();

        // Then: returns failure with lexical error
        assertFalse(res.isCorrect());
        assertTrue(((IncorrectResult<IterationStep<Token>>) res).error().contains("Error léxico"));
    }
}
