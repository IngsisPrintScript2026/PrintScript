/*
 * My Project
 */

package formatter;

import static org.junit.jupiter.api.Assertions.*;

import formatter.rule.*;
import org.junit.jupiter.api.Test;
import position.Position;
import token.Token;
import token.TokenType;

class FormattingRulesTest {

    @Test
    void testSpaceAroundOperatorsRule() {
        SpaceAroundOperatorsRule rule = new SpaceAroundOperatorsRule();
        Token plus = new Token(TokenType.PLUS, "+", new Position(1, 1));
        Token num = new Token(TokenType.NUMBER_LITERAL, "1", new Position(1, 3));
        FormatContext ctxEnabled =
                new FormatContext(0, 4, null, null, null, true, null, null, null, null, null);
        FormatContext ctxDisabled =
                new FormatContext(0, 4, null, null, null, false, null, null, null, null, null);
        FormatContext ctxNull =
                new FormatContext(0, 4, null, null, null, null, null, null, null, null, null);

        assertTrue(rule.applies(plus, num, ctxEnabled));
        assertTrue(rule.applies(num, plus, ctxEnabled));
        assertFalse(rule.applies(num, num, ctxEnabled));
        assertFalse(rule.applies(plus, num, ctxNull));

        assertEquals(" ", rule.formatSeparator(plus, num, "", ctxEnabled));
        assertEquals("", rule.formatSeparator(plus, num, "", ctxDisabled));
    }

    @Test
    void testSingleSpaceSeparationRule() {
        SingleSpaceSeparationRule rule = new SingleSpaceSeparationRule();
        Token let = new Token(TokenType.LET, "let", new Position(1, 1));
        Token id = new Token(TokenType.IDENTIFIER, "x", new Position(1, 5));
        Token semi = new Token(TokenType.SEMICOLON, ";", new Position(1, 6));
        FormatContext ctx =
                new FormatContext(0, 4, null, null, null, null, null, null, true, null, null);

        assertTrue(rule.applies(let, id, ctx));
        assertEquals(" ", rule.formatSeparator(let, id, "   ", ctx));
        assertEquals("\n", rule.formatSeparator(let, id, "\n", ctx));
        assertEquals("", rule.formatSeparator(id, semi, " ", ctx));
    }

    @Test
    void testSpaceBeforeAndAfterColonRule() {
        SpaceBeforeColonRule before = new SpaceBeforeColonRule();
        SpaceAfterColonRule after = new SpaceAfterColonRule();
        Token id = new Token(TokenType.IDENTIFIER, "x", new Position(1, 1));
        Token colon = new Token(TokenType.COLON, ":", new Position(1, 2));
        Token type = new Token(TokenType.NUMBER, "number", new Position(1, 4));

        FormatContext ctxBefore =
                new FormatContext(0, 4, true, null, null, null, null, null, null, null, null);
        FormatContext ctxNoBefore =
                new FormatContext(0, 4, false, null, null, null, null, null, null, null, null);
        FormatContext ctxAfter =
                new FormatContext(0, 4, null, true, null, null, null, null, null, null, null);

        assertTrue(before.applies(id, colon, ctxBefore));
        assertFalse(before.applies(colon, type, ctxBefore));
        assertEquals(" ", before.formatSeparator(id, colon, "", ctxBefore));
        assertEquals("", before.formatSeparator(id, colon, "", ctxNoBefore));

        assertTrue(after.applies(colon, type, ctxAfter));
        assertFalse(after.applies(id, colon, ctxAfter));
        assertEquals(" ", after.formatSeparator(colon, type, "", ctxAfter));
    }

    @Test
    void testSpaceAroundEqualsRule() {
        SpaceAroundEqualsRule rule = new SpaceAroundEqualsRule();
        Token eq = new Token(TokenType.EQUAL, "=", new Position(1, 1));
        Token num = new Token(TokenType.NUMBER_LITERAL, "1", new Position(1, 3));
        FormatContext ctxOn =
                new FormatContext(0, 4, null, null, true, null, null, null, null, null, null);
        FormatContext ctxOff =
                new FormatContext(0, 4, null, null, false, null, null, null, null, null, null);

        assertTrue(rule.applies(eq, num, ctxOn));
        assertTrue(rule.applies(num, eq, ctxOn));
        assertEquals(" ", rule.formatSeparator(eq, num, "", ctxOn));
        assertEquals("", rule.formatSeparator(eq, num, "", ctxOff));
    }

    @Test
    void testBracePositionRule() {
        BracePositionRule rule = new BracePositionRule();
        Token rparen = new Token(TokenType.RPAREN, ")", new Position(1, 1));
        Token lbrace = new Token(TokenType.LBRACE, "{", new Position(1, 3));
        Token elseTok = new Token(TokenType.ELSE, "else", new Position(1, 5));
        FormatContext ctxSame =
                new FormatContext(0, 4, null, null, null, null, null, null, null, true, false);
        FormatContext ctxNext =
                new FormatContext(0, 4, null, null, null, null, null, null, null, false, true);

        assertTrue(rule.applies(rparen, lbrace, ctxSame));
        assertTrue(rule.applies(elseTok, lbrace, ctxSame));
        assertFalse(rule.applies(rparen, elseTok, ctxSame));

        assertEquals(" ", rule.formatSeparator(rparen, lbrace, "", ctxSame));
        assertEquals("\n", rule.formatSeparator(rparen, lbrace, "", ctxNext));
    }

    @Test
    void testLinesAfterPrintlnRule() {
        LinesAfterPrintlnRule rule = new LinesAfterPrintlnRule();
        Token semi = new Token(TokenType.SEMICOLON, ";", new Position(1, 1));
        Token let = new Token(TokenType.LET, "let", new Position(1, 2));
        FormatContext ctx =
                new FormatContext(0, 4, null, null, null, null, null, 2, null, null, null);

        rule.setAfterPrintln(true);
        assertTrue(rule.isAfterPrintln());
        assertTrue(rule.applies(semi, let, ctx));
        assertEquals("\n\n\n", rule.formatSeparator(semi, let, "", ctx));
        assertFalse(rule.isAfterPrintln());
    }

    @Test
    void testLineBreakAfterStatementRule() {
        LineBreakAfterStatementRule rule = new LineBreakAfterStatementRule();
        Token semi = new Token(TokenType.SEMICOLON, ";", new Position(1, 1));
        Token let = new Token(TokenType.LET, "let", new Position(1, 2));
        FormatContext ctx =
                new FormatContext(0, 4, null, null, null, null, true, null, null, null, null);

        assertTrue(rule.applies(semi, let, ctx));
        assertEquals("\n", rule.formatSeparator(semi, let, "", ctx));
    }

    @Test
    void testIndentationRule() {
        IndentationRule rule = new IndentationRule();
        rule.setDepth(2);
        assertEquals(2, rule.getDepth());

        FormatContext ctx =
                new FormatContext(0, 4, null, null, null, null, null, null, null, null, null);
        Token prev = new Token(TokenType.LBRACE, "{", new Position(1, 1));
        Token curr = new Token(TokenType.LET, "let", new Position(2, 1));
        Token rbrace = new Token(TokenType.RBRACE, "}", new Position(3, 1));

        assertTrue(rule.applies(prev, curr, ctx));
        assertEquals(" ", rule.formatSeparator(prev, curr, " ", ctx));
        assertEquals("\n        ", rule.formatSeparator(prev, curr, "\n", ctx));
        assertEquals("\n    ", rule.formatSeparator(prev, rbrace, "\n", ctx));
    }
}
