/*
 * My Project
 */

package lexer.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import lexer.PrintScriptTokenizer;
import metaChar.MetaCharStringBuilder;
import metaChar.MetaCharacter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import result.Result;
import token.Token;
import token.TokenType;
import token.matcher.TokenTypeMatcher;
import token.tokenize.TokenizeResult;

class PrintScriptTokenizerRangeUnitTest {

    /** Matcher that always answers the same, to isolate the tokenizer from the real chain. */
    private static final class StubTokenTypeMatcher implements TokenTypeMatcher {
        private final Result<TokenType> answer;

        StubTokenTypeMatcher(Result<TokenType> answer) {
            this.answer = answer;
        }

        @Override
        public Result<TokenType> match(String input) {
            return answer;
        }
    }

    private static MetaCharStringBuilder lexemeAt(String text, int line, int column) {
        MetaCharStringBuilder builder = new MetaCharStringBuilder();
        for (int i = 0; i < text.length(); i++) {
            builder.append(new MetaCharacter(text.charAt(i), new Position(line, column + i)));
        }
        return builder;
    }

    private static Token completeToken(TokenizeResult result) {
        return assertInstanceOf(TokenizeResult.Complete.class, result).token();
    }

    @Test
    @DisplayName(
            "Given: lexeme 'number' starting at 1:8 | When: tokenizing | Then: the token ends at"
                    + " 1:13 (last char, inclusive)")
    void tokenizer_whenLexemeIsKeyword_thenTokenEndsAtLastChar() {
        // Given
        PrintScriptTokenizer tokenizer = new PrintScriptTokenizer();

        // When
        Token token = completeToken(tokenizer.tokenize(lexemeAt("number", 1, 8)));

        // Then
        assertEquals(TokenType.NUMBER, token.type());
        assertEquals(new Position(1, 8), token.startPosition());
        assertEquals(new Position(1, 13), token.endPosition());
    }

    @Test
    @DisplayName(
            "Given: single-char lexeme ';' at 2:4 | When: tokenizing | Then: start and end are the"
                    + " same position")
    void tokenizer_whenLexemeIsSingleChar_thenStartEqualsEnd() {
        // Given
        PrintScriptTokenizer tokenizer = new PrintScriptTokenizer();

        // When
        Token token = completeToken(tokenizer.tokenize(lexemeAt(";", 2, 4)));

        // Then
        assertEquals(token.startPosition(), token.endPosition());
    }

    @Test
    @DisplayName(
            "Given: an empty builder | When: tokenizing | Then: returns Invalid since there is"
                    + " nothing to match")
    void tokenizer_whenBuilderIsEmpty_thenReturnsInvalid() {
        // Given
        PrintScriptTokenizer tokenizer = new PrintScriptTokenizer();

        // When
        TokenizeResult result = tokenizer.tokenize(new MetaCharStringBuilder());

        // Then
        assertInstanceOf(TokenizeResult.Invalid.class, result);
    }

    @Test
    @DisplayName(
            "Given: an unfinished double-quoted string '\"hol' | When: tokenizing | Then: returns"
                    + " Prefix (keep reading)")
    void tokenizer_whenDoubleQuotedStringIsUnfinished_thenReturnsPrefix() {
        // Given
        PrintScriptTokenizer tokenizer = new PrintScriptTokenizer();

        // When
        TokenizeResult result = tokenizer.tokenize(lexemeAt("\"hol", 1, 1));

        // Then
        assertInstanceOf(TokenizeResult.Prefix.class, result);
    }

    @Test
    @DisplayName(
            "Given: a lone single quote | When: tokenizing | Then: returns Prefix (a string is"
                    + " starting)")
    void tokenizer_whenOnlyOpeningSingleQuote_thenReturnsPrefix() {
        // Given
        PrintScriptTokenizer tokenizer = new PrintScriptTokenizer();

        // When
        TokenizeResult result = tokenizer.tokenize(lexemeAt("'", 1, 1));

        // Then
        assertInstanceOf(TokenizeResult.Prefix.class, result);
    }

    @Test
    @DisplayName(
            "Given: a string whose last quote is escaped | When: tokenizing | Then: returns Prefix"
                    + " (the string is not closed yet)")
    void tokenizer_whenClosingQuoteIsEscaped_thenReturnsPrefix() {
        // Given
        PrintScriptTokenizer tokenizer = new PrintScriptTokenizer();

        // When
        TokenizeResult result = tokenizer.tokenize(lexemeAt("\"a\\\"", 1, 1));

        // Then
        assertInstanceOf(TokenizeResult.Prefix.class, result);
    }

    @Test
    @DisplayName(
            "Given: a single-quoted string 'hi' at 1:5 | When: tokenizing | Then: returns a string"
                    + " literal ending at the closing quote")
    void tokenizer_whenSingleQuotedStringIsClosed_thenReturnsStringLiteral() {
        // Given
        PrintScriptTokenizer tokenizer = new PrintScriptTokenizer();

        // When
        Token token = completeToken(tokenizer.tokenize(lexemeAt("'hi'", 1, 5)));

        // Then
        assertEquals(TokenType.STRING_LITERAL, token.type());
        assertEquals(new Position(1, 8), token.endPosition());
    }

    @Test
    @DisplayName(
            "Given: a number followed by a dot '12.' | When: tokenizing | Then: returns Prefix"
                    + " (decimal number in progress)")
    void tokenizer_whenNumberEndsWithDot_thenReturnsPrefix() {
        // Given
        PrintScriptTokenizer tokenizer = new PrintScriptTokenizer();

        // When
        TokenizeResult result = tokenizer.tokenize(lexemeAt("12.", 1, 1));

        // Then
        assertInstanceOf(TokenizeResult.Prefix.class, result);
    }

    @Test
    @DisplayName(
            "Given: a matcher that rejects with a reason | When: tokenizing a non-prefix lexeme |"
                    + " Then: returns Invalid carrying the matcher's reason")
    void tokenizer_whenMatcherRejects_thenInvalidCarriesReason() {
        // Given
        PrintScriptTokenizer tokenizer =
                new PrintScriptTokenizer(new StubTokenTypeMatcher(Result.failure("nope")));

        // When
        TokenizeResult result = tokenizer.tokenize(lexemeAt("#", 1, 1));

        // Then
        assertEquals("nope", assertInstanceOf(TokenizeResult.Invalid.class, result).reason());
    }

    @Test
    @DisplayName(
            "Given: a matcher that accepts as IDENTIFIER | When: tokenizing | Then: the token"
                    + " keeps the lexeme text and type from the matcher")
    void tokenizer_whenMatcherAccepts_thenTokenUsesMatcherType() {
        // Given
        PrintScriptTokenizer tokenizer =
                new PrintScriptTokenizer(
                        new StubTokenTypeMatcher(Result.success(TokenType.IDENTIFIER)));

        // When
        Token token = completeToken(tokenizer.tokenize(lexemeAt("abc", 3, 1)));

        // Then
        assertEquals(TokenType.IDENTIFIER, token.type());
        assertEquals("abc", token.value());
        assertEquals(new Position(3, 3), token.endPosition());
    }
}
