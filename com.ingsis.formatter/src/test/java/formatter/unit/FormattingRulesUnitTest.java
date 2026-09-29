/*
 * My Project
 */

package formatter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import formatter.FormatContext;
import formatter.rule.BracePositionRule;
import formatter.rule.IndentationRule;
import formatter.rule.LineBreakAfterStatementRule;
import formatter.rule.LinesAfterPrintlnRule;
import formatter.rule.SingleSpaceSeparationRule;
import formatter.rule.SpaceAfterColonRule;
import formatter.rule.SpaceAfterCommaRule;
import formatter.rule.SpaceAroundEqualsRule;
import formatter.rule.SpaceAroundOperatorsRule;
import formatter.rule.SpaceBeforeColonRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import token.Token;
import token.TokenType;

class FormattingRulesUnitTest {
    private static final String LET = "let";

    @Test
    @DisplayName(
            "What: SpaceAroundOperatorsRule | When: checking operator separators | Then: formats"
                    + " spaces around operators")
    void spaceAroundOperatorsRule_whenCheckingSeparators_thenFormatsSpacesAroundOperators() {
        // What: SpaceAroundOperatorsRule and test tokens
        SpaceAroundOperatorsRule rule = new SpaceAroundOperatorsRule();
        Token plus = new Token(TokenType.PLUS, "+", new Position(1, 1));
        Token num = new Token(TokenType.NUMBER_LITERAL, "1", new Position(1, 3));
        FormatContext ctxEnabled =
                new FormatContext(0, 4, null, null, null, true, null, null, null, null, null);
        FormatContext ctxDisabled =
                new FormatContext(0, 4, null, null, null, false, null, null, null, null, null);
        FormatContext ctxNull =
                new FormatContext(0, 4, null, null, null, null, null, null, null, null, null);

        // When & Then: verify rule applicability
        assertTrue(rule.applies(plus, num, ctxEnabled));
        assertTrue(rule.applies(num, plus, ctxEnabled));
        assertFalse(rule.applies(num, num, ctxEnabled));
        assertFalse(rule.applies(plus, num, ctxNull));

        // When & Then: format separators
        assertEquals(" ", rule.formatSeparator(plus, num, "", ctxEnabled));
        assertEquals("", rule.formatSeparator(plus, num, "", ctxDisabled));
    }

    @Test
    @DisplayName(
            "What: SingleSpaceSeparationRule | When: checking adjacent tokens | Then: enforces"
                    + " single space or newline")
    void singleSpaceSeparationRule_whenCheckingSeparators_thenEnforcesSingleSpaceOrNewline() {
        // What: SingleSpaceSeparationRule and test tokens
        SingleSpaceSeparationRule rule = new SingleSpaceSeparationRule();
        Token let = new Token(TokenType.LET, LET, new Position(1, 1));
        Token id = new Token(TokenType.IDENTIFIER, "x", new Position(1, 5));
        Token semi = new Token(TokenType.SEMICOLON, ";", new Position(1, 6));
        FormatContext ctx =
                new FormatContext(0, 4, null, null, null, null, null, null, true, null, null);

        // When & Then: verify rule formatting
        assertTrue(rule.applies(let, id, ctx));
        assertEquals(" ", rule.formatSeparator(let, id, "   ", ctx));
        assertEquals("\n", rule.formatSeparator(let, id, "\n", ctx));
        assertEquals("", rule.formatSeparator(id, semi, " ", ctx));
    }

    @Test
    @DisplayName(
            "What: SpaceBeforeColonRule and SpaceAfterColonRule | When: formatting colons | Then:"
                    + " applies spaces before/after colon")
    void spaceBeforeAndAfterColonRule_whenCheckingSeparators_thenAppliesSpacesAccurately() {
        // What: rules and colon tokens
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

        // When & Then: verify before colon rule
        assertTrue(before.applies(id, colon, ctxBefore));
        assertFalse(before.applies(colon, type, ctxBefore));
        assertEquals(" ", before.formatSeparator(id, colon, "", ctxBefore));
        assertEquals("", before.formatSeparator(id, colon, "", ctxNoBefore));

        // When & Then: verify after colon rule
        assertTrue(after.applies(colon, type, ctxAfter));
        assertFalse(after.applies(id, colon, ctxAfter));
        assertEquals(" ", after.formatSeparator(colon, type, "", ctxAfter));
    }

    @Test
    @DisplayName(
            "What: SpaceAroundEqualsRule | When: checking equals tokens | Then: adds spaces around"
                    + " equals")
    void spaceAroundEqualsRule_whenCheckingSeparators_thenAddsSpacesAroundEquals() {
        // What: SpaceAroundEqualsRule and equal tokens
        SpaceAroundEqualsRule rule = new SpaceAroundEqualsRule();
        Token eq = new Token(TokenType.EQUAL, "=", new Position(1, 1));
        Token num = new Token(TokenType.NUMBER_LITERAL, "1", new Position(1, 3));
        FormatContext ctxOn =
                new FormatContext(0, 4, null, null, true, null, null, null, null, null, null);
        FormatContext ctxOff =
                new FormatContext(0, 4, null, null, false, null, null, null, null, null, null);

        // When & Then: verify formatting
        assertTrue(rule.applies(eq, num, ctxOn));
        assertTrue(rule.applies(num, eq, ctxOn));
        assertEquals(" ", rule.formatSeparator(eq, num, "", ctxOn));
        assertEquals("", rule.formatSeparator(eq, num, "", ctxOff));
    }

    @Test
    @DisplayName(
            "What: BracePositionRule | When: checking same line vs next line brace | Then: formats"
                    + " space or newline")
    void bracePositionRule_whenCheckingBracePosition_thenFormatsSpaceOrNewline() {
        // What: BracePositionRule and brace tokens
        BracePositionRule rule = new BracePositionRule();
        Token rparen = new Token(TokenType.RPAREN, ")", new Position(1, 1));
        Token lbrace = new Token(TokenType.LBRACE, "{", new Position(1, 3));
        Token elseTok = new Token(TokenType.ELSE, "else", new Position(1, 5));
        FormatContext ctxSame =
                new FormatContext(0, 4, null, null, null, null, null, null, null, true, false);
        FormatContext ctxNext =
                new FormatContext(0, 4, null, null, null, null, null, null, null, false, true);

        // When & Then: verify rule application
        assertTrue(rule.applies(rparen, lbrace, ctxSame));
        assertTrue(rule.applies(elseTok, lbrace, ctxSame));
        assertFalse(rule.applies(rparen, elseTok, ctxSame));

        assertEquals(" ", rule.formatSeparator(rparen, lbrace, "", ctxSame));
        assertEquals("\n", rule.formatSeparator(rparen, lbrace, "", ctxNext));
    }

    @Test
    @DisplayName(
            "What: LinesAfterPrintlnRule and LineBreakAfterStatementRule | When: formatting"
                    + " statements | Then: inserts line breaks")
    void lineBreakRules_whenFormattingStatements_thenInsertsExpectedLineBreaks() {
        // What: LinesAfterPrintlnRule and LineBreakAfterStatementRule
        LinesAfterPrintlnRule printlnRule = new LinesAfterPrintlnRule();
        LineBreakAfterStatementRule stmtRule = new LineBreakAfterStatementRule();
        Token semi = new Token(TokenType.SEMICOLON, ";", new Position(1, 1));
        Token let = new Token(TokenType.LET, LET, new Position(1, 2));

        FormatContext ctxPrintln =
                new FormatContext(0, 4, null, null, null, null, null, 2, null, null, null);
        FormatContext ctxStmt =
                new FormatContext(0, 4, null, null, null, null, true, null, null, null, null);

        // When & Then: println lines rule
        printlnRule.setAfterPrintln(true);
        assertTrue(printlnRule.isAfterPrintln());
        assertTrue(printlnRule.applies(semi, let, ctxPrintln));
        assertEquals("\n\n\n", printlnRule.formatSeparator(semi, let, "", ctxPrintln));
        assertFalse(printlnRule.isAfterPrintln());

        // When & Then: line break after statement rule
        assertTrue(stmtRule.applies(semi, let, ctxStmt));
        assertEquals("\n", stmtRule.formatSeparator(semi, let, "", ctxStmt));
    }

    @Test
    @DisplayName(
            "What: IndentationRule | When: tracking block depth | Then: indents with configured"
                    + " spaces")
    void indentationRule_whenTrackingBlockDepth_thenIndentsWithConfiguredSpaces() {
        // What: IndentationRule set to depth 2
        IndentationRule rule = new IndentationRule();
        rule.setDepth(2);
        assertEquals(2, rule.getDepth());

        FormatContext ctx =
                new FormatContext(0, 4, null, null, null, null, null, null, null, null, null);
        Token prev = new Token(TokenType.LBRACE, "{", new Position(1, 1));
        Token curr = new Token(TokenType.LET, LET, new Position(2, 1));
        Token rbrace = new Token(TokenType.RBRACE, "}", new Position(3, 1));

        // When & Then: verify indentation
        assertTrue(rule.applies(prev, curr, ctx));
        assertEquals(" ", rule.formatSeparator(prev, curr, " ", ctx));
        assertEquals("\n        ", rule.formatSeparator(prev, curr, "\n", ctx));
        assertEquals("\n    ", rule.formatSeparator(prev, rbrace, "\n", ctx));
    }

    @Test
    @DisplayName(
            "What: SpaceAfterCommaRule | When: formatting comma separators | Then: formats spaces"
                    + " after comma")
    void spaceAfterCommaRule_whenCheckingSeparators_thenFormatsSpaceAfterComma() {
        // What: SpaceAfterCommaRule and comma tokens
        SpaceAfterCommaRule rule = new SpaceAfterCommaRule();
        Token comma = new Token(TokenType.COMMA, ",", new Position(1, 1));
        Token id = new Token(TokenType.IDENTIFIER, "b", new Position(1, 2));
        FormatContext ctxOn =
                new FormatContext(0, 4, null, null, null, null, null, null, null, null, null, true);
        FormatContext ctxOff =
                new FormatContext(
                        0, 4, null, null, null, null, null, null, null, null, null, false);
        FormatContext ctxNull =
                new FormatContext(0, 4, null, null, null, null, null, null, null, null, null, null);

        // When & Then: verify rule applicability
        assertTrue(rule.applies(comma, id, ctxOn));
        assertFalse(rule.applies(id, comma, ctxOn));
        assertFalse(rule.applies(comma, id, ctxNull));

        // When & Then: format separators
        assertEquals(" ", rule.formatSeparator(comma, id, "", ctxOn));
        assertEquals("", rule.formatSeparator(comma, id, "", ctxOff));
    }
}
