/*
 * My Project
 */

package formatter.unit;

import static formatter.fake.FakeTokens.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import formatter.FormatContext;
import formatter.rule.LineBreakAfterStatementRule;
import formatter.rule.LinesAfterPrintlnRule;
import formatter.rule.LinesBeforePrintlnRule;
import formatter.support.FormatContextBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import token.Token;
import token.TokenType;

/** Rules that decide where line breaks go between statements. */
class LineBreakRulesUnitTest {
    private static final Token LET = at(TokenType.LET, "let");
    private static final Token ID = at(TokenType.IDENTIFIER, "x");
    private static final Token SEMI = at(TokenType.SEMICOLON, ";");
    private static final Token LBRACE = at(TokenType.LBRACE, "{");
    private static final Token RBRACE = at(TokenType.RBRACE, "}");
    private static final Token ELSE = at(TokenType.ELSE, "else");
    private static final Token PRINTLN = at(TokenType.PRINTLN, "println");
    private static final FormatContext UNSET = FormatContextBuilder.unset().build();

    @Test
    @DisplayName(
            "Given: LineBreakAfterStatementRule | When: a statement follows ';' on the same line |"
                    + " Then: breaks the line")
    void lineBreakAfterStatementRule_whenAfterSemicolon_thenBreaksLine() {
        // Given
        LineBreakAfterStatementRule rule = new LineBreakAfterStatementRule();

        // When
        String separator = rule.formatSeparator(SEMI, LET, " ", UNSET);

        // Then
        assertTrue(rule.applies(SEMI, LET, UNSET));
        assertEquals("\n", separator);
    }

    @Test
    @DisplayName(
            "Given: LineBreakAfterStatementRule | When: the source already has empty lines | Then:"
                    + " keeps the original line breaks")
    void lineBreakAfterStatementRule_whenSeparatorHasNewlines_thenKeepsThem() {
        // Given
        LineBreakAfterStatementRule rule = new LineBreakAfterStatementRule();

        // When
        String separator = rule.formatSeparator(SEMI, LET, "\n\n", UNSET);

        // Then
        assertEquals("\n\n", separator);
    }

    @Test
    @DisplayName(
            "Given: LineBreakAfterStatementRule | When: a non-empty block opens and closes | Then:"
                    + " applies after '{' and before '}'")
    void lineBreakAfterStatementRule_whenInsideNonEmptyBlock_thenApplies() {
        // Given
        LineBreakAfterStatementRule rule = new LineBreakAfterStatementRule();

        // When
        boolean afterOpen = rule.applies(LBRACE, PRINTLN, UNSET);
        boolean beforeClose = rule.applies(SEMI, RBRACE, UNSET);
        boolean afterClose = rule.applies(RBRACE, LET, UNSET);

        // Then
        assertTrue(afterOpen && beforeClose && afterClose);
    }

    @Test
    @DisplayName(
            "Given: LineBreakAfterStatementRule | When: the block is empty or '}' is followed by"
                    + " else | Then: the rule does not apply")
    void lineBreakAfterStatementRule_whenEmptyBlockOrElse_thenDoesNotApply() {
        // Given
        LineBreakAfterStatementRule rule = new LineBreakAfterStatementRule();

        // When
        boolean emptyBlock = rule.applies(LBRACE, RBRACE, UNSET);
        boolean beforeElse = rule.applies(RBRACE, ELSE, UNSET);
        boolean midStatement = rule.applies(LET, ID, UNSET);

        // Then
        assertFalse(emptyBlock || beforeElse || midStatement);
    }

    @Test
    @DisplayName(
            "Given: LineBreakAfterStatementRule | When: a token is missing | Then: the rule does"
                    + " not apply")
    void lineBreakAfterStatementRule_whenTokenMissing_thenDoesNotApply() {
        // Given
        LineBreakAfterStatementRule rule = new LineBreakAfterStatementRule();

        // When
        boolean noPrev = rule.applies(null, LET, UNSET);
        boolean noCurrent = rule.applies(SEMI, null, UNSET);

        // Then
        assertFalse(noPrev || noCurrent);
    }

    @Test
    @DisplayName(
            "Given: LinesBeforePrintlnRule with 2 | When: println starts a statement after ';' |"
                    + " Then: leaves two empty lines before it")
    void linesBeforePrintlnRule_whenTwoConfigured_thenLeavesTwoEmptyLines() {
        // Given
        LinesBeforePrintlnRule rule = new LinesBeforePrintlnRule();
        FormatContext ctx = FormatContextBuilder.unset().lineBreaksBeforePrintln(2).build();

        // When
        String separator = rule.formatSeparator(SEMI, PRINTLN, "\n", ctx);

        // Then
        assertTrue(rule.applies(SEMI, PRINTLN, ctx));
        assertEquals("\n\n\n", separator);
    }

    @Test
    @DisplayName(
            "Given: LinesBeforePrintlnRule with 0 | When: println follows statements separated by"
                    + " empty lines | Then: removes the empty lines")
    void linesBeforePrintlnRule_whenZeroConfigured_thenRemovesEmptyLines() {
        // Given
        LinesBeforePrintlnRule rule = new LinesBeforePrintlnRule();
        FormatContext ctx = FormatContextBuilder.unset().lineBreaksBeforePrintln(0).build();

        // When
        String separator = rule.formatSeparator(SEMI, PRINTLN, "\n\n\n", ctx);

        // Then
        assertEquals("\n", separator);
    }

    @Test
    @DisplayName(
            "Given: LinesBeforePrintlnRule configured | When: println follows '{' or '}' | Then:"
                    + " the rule applies")
    void linesBeforePrintlnRule_whenAfterBrace_thenApplies() {
        // Given
        LinesBeforePrintlnRule rule = new LinesBeforePrintlnRule();
        FormatContext ctx = FormatContextBuilder.unset().lineBreaksBeforePrintln(1).build();

        // When
        boolean afterOpen = rule.applies(LBRACE, PRINTLN, ctx);
        boolean afterClose = rule.applies(RBRACE, PRINTLN, ctx);

        // Then
        assertTrue(afterOpen && afterClose);
    }

    @Test
    @DisplayName(
            "Given: LinesBeforePrintlnRule | When: unset, not a println, or println mid-statement |"
                    + " Then: the rule does not apply")
    void linesBeforePrintlnRule_whenNotAtStatementStart_thenDoesNotApply() {
        // Given
        LinesBeforePrintlnRule rule = new LinesBeforePrintlnRule();
        FormatContext ctx = FormatContextBuilder.unset().lineBreaksBeforePrintln(1).build();

        // When
        boolean unset = rule.applies(SEMI, PRINTLN, UNSET);
        boolean notPrintln = rule.applies(SEMI, LET, ctx);
        boolean midStatement = rule.applies(ID, PRINTLN, ctx);
        boolean nullTokens = rule.applies(null, PRINTLN, ctx) || rule.applies(SEMI, null, ctx);

        // Then
        assertFalse(unset || notPrintln || midStatement || nullTokens);
    }

    @Test
    @DisplayName(
            "Given: LinesAfterPrintlnRule after a println with 1 configured | When: formatting the"
                    + " separator after ';' | Then: leaves one empty line and resets its state")
    void linesAfterPrintlnRule_whenAfterPrintln_thenAddsLinesAndResets() {
        // Given
        LinesAfterPrintlnRule rule = new LinesAfterPrintlnRule();
        FormatContext ctx = FormatContextBuilder.unset().lineBreaksAfterPrintln(1).build();
        rule.setAfterPrintln(true);

        // When
        boolean applies = rule.applies(SEMI, LET, ctx);
        String separator = rule.formatSeparator(SEMI, LET, " ", ctx);

        // Then
        assertTrue(applies);
        assertEquals("\n\n", separator);
        assertFalse(rule.isAfterPrintln());
    }

    @Test
    @DisplayName(
            "Given: LinesAfterPrintlnRule | When: no println was seen or config is unset | Then:"
                    + " the rule does not apply")
    void linesAfterPrintlnRule_whenNotAfterPrintlnOrUnset_thenDoesNotApply() {
        // Given
        LinesAfterPrintlnRule rule = new LinesAfterPrintlnRule();
        FormatContext ctx = FormatContextBuilder.unset().lineBreaksAfterPrintln(1).build();

        // When
        boolean noPrintln = rule.applies(SEMI, LET, ctx);
        rule.setAfterPrintln(true);
        boolean unset = rule.applies(SEMI, LET, UNSET);
        boolean notSemicolon = rule.applies(ID, LET, ctx) || rule.applies(null, LET, ctx);

        // Then
        assertFalse(noPrintln || unset || notSemicolon);
    }
}
