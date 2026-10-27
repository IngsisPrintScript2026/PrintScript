/*
 * My Project
 */

package formatter.unit;

import static formatter.fake.FakeTokens.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import formatter.FormatContext;
import formatter.rule.BracePositionRule;
import formatter.rule.IndentationRule;
import formatter.support.FormatContextBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import token.Token;
import token.TokenType;

/** Rules for if/else blocks: brace placement and indentation. */
class BlockRulesUnitTest {
    private static final Token RPAREN = at(TokenType.RPAREN, ")");
    private static final Token LBRACE = at(TokenType.LBRACE, "{");
    private static final Token RBRACE = at(TokenType.RBRACE, "}");
    private static final Token ELSE = at(TokenType.ELSE, "else");
    private static final Token LET = at(TokenType.LET, "let");
    private static final FormatContext UNSET = FormatContextBuilder.unset().build();

    @Test
    @DisplayName(
            "Given: BracePositionRule without config | When: '{' follows ')' on another line |"
                    + " Then: moves the brace to the same line")
    void bracePositionRule_whenUnset_thenPutsBraceOnSameLine() {
        // Given
        BracePositionRule rule = new BracePositionRule();

        // When
        String separator = rule.formatSeparator(RPAREN, LBRACE, "\n", UNSET);

        // Then
        assertTrue(rule.applies(RPAREN, LBRACE, UNSET));
        assertEquals(" ", separator);
    }

    @Test
    @DisplayName(
            "Given: BracePositionRule with if-brace-below-line | When: '{' follows ')' | Then:"
                    + " puts the brace on the next line")
    void bracePositionRule_whenBelowLineEnabled_thenPutsBraceOnNextLine() {
        // Given
        BracePositionRule rule = new BracePositionRule();
        FormatContext ctx = FormatContextBuilder.unset().ifBraceBelowLine(true).build();

        // When
        String separator = rule.formatSeparator(RPAREN, LBRACE, " ", ctx);

        // Then
        assertEquals("\n", separator);
    }

    @Test
    @DisplayName(
            "Given: BracePositionRule with both below-line and same-line enabled | When: '{'"
                    + " follows ')' | Then: same line wins")
    void bracePositionRule_whenBothFlagsEnabled_thenSameLineWins() {
        // Given
        BracePositionRule rule = new BracePositionRule();
        FormatContext ctx =
                FormatContextBuilder.unset().ifBraceBelowLine(true).ifBraceSameLine(true).build();

        // When
        String separator = rule.formatSeparator(RPAREN, LBRACE, "\n", ctx);

        // Then
        assertEquals(" ", separator);
    }

    @Test
    @DisplayName(
            "Given: BracePositionRule | When: 'else' follows '}' | Then: the rule applies so it"
                    + " becomes '} else'")
    void bracePositionRule_whenElseFollowsClosingBrace_thenApplies() {
        // Given
        BracePositionRule rule = new BracePositionRule();

        // When
        boolean applies = rule.applies(RBRACE, ELSE, UNSET);

        // Then
        assertTrue(applies);
        assertEquals(" ", rule.formatSeparator(RBRACE, ELSE, "\n", UNSET));
    }

    @Test
    @DisplayName(
            "Given: BracePositionRule | When: the current token is neither '{' nor an else after"
                    + " '}' | Then: the rule does not apply")
    void bracePositionRule_whenNoBraceInvolved_thenDoesNotApply() {
        // Given
        BracePositionRule rule = new BracePositionRule();

        // When
        boolean plainToken = rule.applies(RPAREN, LET, UNSET);
        boolean elseWithoutBrace = rule.applies(LET, ELSE, UNSET);
        boolean nulls = rule.applies(RBRACE, null, UNSET) || rule.applies(null, ELSE, UNSET);

        // Then
        assertFalse(plainToken || elseWithoutBrace || nulls);
    }

    @Test
    @DisplayName(
            "Given: IndentationRule at depth 2 with 4 spaces | When: a new line starts | Then:"
                    + " indents it with 8 spaces")
    void indentationRule_whenNewLineAtDepthTwo_thenIndentsEightSpaces() {
        // Given
        IndentationRule rule = new IndentationRule();
        rule.setDepth(2);
        FormatContext ctx = FormatContextBuilder.unset().indentSpaces(4).build();

        // When
        String separator = rule.formatSeparator(LBRACE, LET, "\n  ", ctx);

        // Then
        assertTrue(rule.applies(LBRACE, LET, ctx));
        assertEquals("\n        ", separator);
        assertEquals(2, rule.getDepth());
    }

    @Test
    @DisplayName(
            "Given: IndentationRule at depth 1 | When: the line starts with '}' | Then: dedents it"
                    + " one level")
    void indentationRule_whenLineStartsWithClosingBrace_thenDedents() {
        // Given
        IndentationRule rule = new IndentationRule();
        rule.setDepth(1);
        FormatContext ctx = FormatContextBuilder.unset().indentSpaces(2).build();

        // When
        String separator = rule.formatSeparator(LET, RBRACE, "\n      ", ctx);

        // Then
        assertEquals("\n", separator);
    }

    @Test
    @DisplayName(
            "Given: IndentationRule | When: the separator keeps empty lines | Then: keeps them and"
                    + " only indents the last line")
    void indentationRule_whenSeveralNewlines_thenKeepsThemAndIndentsLastLine() {
        // Given
        IndentationRule rule = new IndentationRule();
        rule.setDepth(1);

        // When
        String separator = rule.formatSeparator(LET, LET, "\n  \n", UNSET);

        // Then
        assertEquals("\n  \n    ", separator);
    }

    @Test
    @DisplayName(
            "Given: IndentationRule | When: the separator has no newline | Then: leaves it as is")
    void indentationRule_whenSameLine_thenLeavesSeparator() {
        // Given
        IndentationRule rule = new IndentationRule();
        rule.setDepth(3);

        // When
        String separator = rule.formatSeparator(LET, LET, " ", UNSET);

        // Then
        assertEquals(" ", separator);
    }
}
