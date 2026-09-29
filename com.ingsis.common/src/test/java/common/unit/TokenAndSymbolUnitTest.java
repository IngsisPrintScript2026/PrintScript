/*
 * My Project
 */

package common.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import token.SymbolType;
import token.Token;
import token.TokenType;
import token.tokenize.TokenizeResult;

class TokenAndSymbolUnitTest {

    @Test
    @DisplayName(
            "What: Token | When: constructed with positions | Then: exposes positions, lines,"
                    + " columns, and properties")
    void token_whenConstructed_thenExposesPositionsAndProperties() {
        // What: Positions and Token instance
        Position p1 = new Position(1, 1);
        Position p2 = new Position(1, 4);
        Token tok = new Token(TokenType.LET, "let", p1, p2);

        // When: querying properties
        TokenType type = tok.type();
        String val = tok.value();
        int line = tok.line();
        int col = tok.column();

        // Then: all properties match constructor args
        assertEquals(TokenType.LET, type);
        assertEquals("let", val);
        assertEquals(p1, tok.startPosition());
        assertEquals(p2, tok.endPosition());
        assertEquals(1, line);
        assertEquals(1, col);
        assertFalse(tok.isNull());

        // When: created with null position
        Token tok2 = new Token(TokenType.IDENTIFIER, "foo", null);

        // Then: line and column default to -1
        assertEquals(-1, tok2.line());
        assertEquals(-1, tok2.column());
    }

    @Test
    @DisplayName(
            "What: SymbolType | When: checking token against symbols | Then: identifies symbols"
                    + " correctly")
    void symbolType_whenMatchingTokens_thenIdentifiesSymbolCorrectly() {
        // What: EQUAL and COLON tokens
        Token eqTok = new Token(TokenType.EQUAL, "=", new Position(1, 1));
        Token colonTok = new Token(TokenType.COLON, ":", new Position(1, 2));

        // When: verifying isSymbol with various tokens and types
        boolean isEq = SymbolType.isSymbol(eqTok, SymbolType.EQUAL);
        boolean isNotColon = SymbolType.isSymbol(eqTok, SymbolType.COLON);
        boolean nullTokenCheck = SymbolType.isSymbol(null, SymbolType.EQUAL);
        boolean nullSymbolCheck = SymbolType.isSymbol(eqTok, null);

        // Then: matches expected equality conditions
        assertTrue(isEq);
        assertFalse(isNotColon);
        assertFalse(nullTokenCheck);
        assertFalse(nullSymbolCheck);

        // When: converting from token and token type
        assertEquals(Optional.of(SymbolType.EQUAL), SymbolType.fromToken(eqTok));
        assertEquals(Optional.empty(), SymbolType.fromToken(null));
        assertEquals(
                Optional.empty(),
                SymbolType.fromToken(new Token(TokenType.IDENTIFIER, "abc", new Position(1, 1))));

        assertEquals(Optional.of(SymbolType.COLON), SymbolType.fromTokenType(TokenType.COLON));
        assertEquals(Optional.empty(), SymbolType.fromTokenType(null));
        assertEquals(Optional.empty(), SymbolType.fromTokenType(TokenType.LET));

        assertEquals(TokenType.EQUAL, SymbolType.EQUAL.tokenType());
        assertEquals("=", SymbolType.EQUAL.symbol());
    }

    @Test
    @DisplayName(
            "What: TokenizeResult | When: instantiating Complete, Prefix, and Invalid | Then: holds"
                    + " correct data")
    void tokenizeResult_whenInstantiated_thenContainsExpectedData() {
        // What: Token and TokenizeResult variants
        Token tok = new Token(TokenType.LET, "let", new Position(1, 1));

        // When: creating Complete, Prefix, and Invalid instances
        TokenizeResult complete = new TokenizeResult.Complete(tok);
        TokenizeResult prefix = new TokenizeResult.Prefix();
        TokenizeResult invalid = new TokenizeResult.Invalid("bad");

        // Then: types and values match expected variants
        assertInstanceOf(TokenizeResult.Complete.class, complete);
        assertInstanceOf(TokenizeResult.Prefix.class, prefix);
        assertInstanceOf(TokenizeResult.Invalid.class, invalid);
        assertEquals(tok, ((TokenizeResult.Complete) complete).token());
        assertEquals("bad", ((TokenizeResult.Invalid) invalid).reason());
    }
}
