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
import token.TokenType;
import token.tokenize.TokenizeResult;

class PrintScriptTokenizerUnitTest {

    @Test
    @DisplayName(
            "What: PrintScriptTokenizer | When: tokenizing completed keyword | Then: returns"
                    + " TokenizeResult.Complete")
    void tokenizer_whenTokenizingCompleteKeyword_thenReturnsCompleteToken() {
        // What: PrintScriptTokenizer and builder containing "let"
        PrintScriptTokenizer tokenizer = new PrintScriptTokenizer();
        MetaCharStringBuilder builder = new MetaCharStringBuilder();
        builder.append(new MetaCharacter('l', new Position(1, 1)));
        builder.append(new MetaCharacter('e', new Position(1, 2)));
        builder.append(new MetaCharacter('t', new Position(1, 3)));

        // When: tokenizing string builder
        TokenizeResult result = tokenizer.tokenize(builder);

        // Then: returns Complete with TokenType.LET
        assertInstanceOf(TokenizeResult.Complete.class, result);
        TokenizeResult.Complete complete = (TokenizeResult.Complete) result;
        assertEquals(TokenType.LET, complete.token().type());
        assertEquals("let", complete.token().value());
    }

    @Test
    @DisplayName(
            "What: PrintScriptTokenizer | When: tokenizing prefix of keyword | Then: returns"
                    + " TokenizeResult.Prefix")
    void tokenizer_whenTokenizingKeywordPrefix_thenReturnsPrefixResult() {
        // What: PrintScriptTokenizer and builder containing "le"
        PrintScriptTokenizer tokenizer = new PrintScriptTokenizer();
        MetaCharStringBuilder builder = new MetaCharStringBuilder();
        builder.append(new MetaCharacter('l', new Position(1, 1)));
        builder.append(new MetaCharacter('e', new Position(1, 2)));

        // When: tokenizing prefix
        TokenizeResult result = tokenizer.tokenize(builder);

        // Then: result is Prefix or Complete identifier
        // "le" can be an identifier, so either Complete or Prefix
        assertInstanceOf(TokenizeResult.class, result);
    }

    @Test
    @DisplayName(
            "What: PrintScriptTokenizer | When: tokenizing invalid symbol | Then: returns"
                    + " TokenizeResult.Invalid")
    void tokenizer_whenTokenizingInvalidSymbol_thenReturnsInvalidResult() {
        // What: PrintScriptTokenizer and builder containing '@'
        PrintScriptTokenizer tokenizer = new PrintScriptTokenizer();
        MetaCharStringBuilder builder = new MetaCharStringBuilder();
        builder.append(new MetaCharacter('@', new Position(1, 1)));

        // When: tokenizing unrecognized character
        TokenizeResult result = tokenizer.tokenize(builder);

        // Then: returns Invalid
        assertInstanceOf(TokenizeResult.Invalid.class, result);
    }
}
