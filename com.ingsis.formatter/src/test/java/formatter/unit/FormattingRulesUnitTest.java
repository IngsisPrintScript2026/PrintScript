/*
 * My Project
 */

package formatter.unit;

import static formatter.fake.FakeTokens.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import formatter.FormatContext;
import formatter.rule.SingleSpaceSeparationRule;
import formatter.rule.SpaceAfterColonRule;
import formatter.rule.SpaceAfterCommaRule;
import formatter.rule.SpaceAroundEqualsRule;
import formatter.rule.SpaceAroundOperatorsRule;
import formatter.rule.SpaceBeforeColonRule;
import formatter.support.FormatContextBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import token.Token;
import token.TokenType;

/** Spacing rules: what separator each rule produces between two tokens on the same line. */
class FormattingRulesUnitTest {
    private static final Token LET = at(TokenType.LET, "let");
    private static final Token ID = at(TokenType.IDENTIFIER, "x");
    private static final Token NUM = at(TokenType.NUMBER_LITERAL, "1");
    private static final Token COLON = at(TokenType.COLON, ":");
    private static final Token TYPE = at(TokenType.NUMBER, "number");
    private static final Token EQUAL = at(TokenType.EQUAL, "=");
    private static final Token COMMA = at(TokenType.COMMA, ",");
    private static final Token SEMI = at(TokenType.SEMICOLON, ";");
    private static final FormatContext UNSET = FormatContextBuilder.unset().build();

    @Test
    @DisplayName(
            "Given: SpaceAroundOperatorsRule and null config | When: an operator follows a token |"
                    + " Then: the rule applies because it is mandatory")
    void spaceAroundOperatorsRule_whenConfigIsNullAndOperatorFollows_thenApplies() {
        // Given
        SpaceAroundOperatorsRule rule = new SpaceAroundOperatorsRule();

        // When
        boolean applies = rule.applies(NUM, at(TokenType.PLUS, "+"), UNSET);

        // Then
        assertTrue(applies);
    }

    @Test
    @DisplayName(
            "Given: SpaceAroundOperatorsRule | When: the previous token is each arithmetic operator"
                    + " | Then: the rule applies to all of + - * /")
    void spaceAroundOperatorsRule_whenPreviousIsAnyArithmeticOperator_thenApplies() {
        // Given
        SpaceAroundOperatorsRule rule = new SpaceAroundOperatorsRule();

        // When
        boolean plus = rule.applies(at(TokenType.PLUS, "+"), NUM, UNSET);
        boolean minus = rule.applies(at(TokenType.MINUS, "-"), NUM, UNSET);
        boolean star = rule.applies(at(TokenType.STAR, "*"), NUM, UNSET);
        boolean slash = rule.applies(at(TokenType.SLASH, "/"), NUM, UNSET);

        // Then
        assertTrue(plus && minus && star && slash);
    }

    @Test
    @DisplayName(
            "Given: SpaceAroundOperatorsRule | When: neither token is an operator | Then: the rule"
                    + " does not apply")
    void spaceAroundOperatorsRule_whenNoOperatorIsInvolved_thenDoesNotApply() {
        // Given
        SpaceAroundOperatorsRule rule = new SpaceAroundOperatorsRule();

        // When
        boolean applies = rule.applies(ID, NUM, UNSET);

        // Then
        assertFalse(applies);
    }

    @Test
    @DisplayName(
            "Given: SpaceAroundOperatorsRule and a config disabling operator spaces | When:"
                    + " formatting an empty separator | Then: always returns one space")
    void spaceAroundOperatorsRule_whenConfigDisablesIt_thenStillReturnsOneSpace() {
        // Given
        SpaceAroundOperatorsRule rule = new SpaceAroundOperatorsRule();
        FormatContext disabled =
                new FormatContext(0, 4, null, null, null, false, null, null, null, null, null);

        // When
        String separator = rule.formatSeparator(NUM, at(TokenType.STAR, "*"), "", disabled);

        // Then
        assertEquals(" ", separator);
    }

    @Test
    @DisplayName(
            "Given: SingleSpaceSeparationRule | When: the separator is a run of spaces | Then:"
                    + " collapses it to a single space")
    void singleSpaceSeparationRule_whenSeveralSpaces_thenCollapsesToOne() {
        // Given
        SingleSpaceSeparationRule rule = new SingleSpaceSeparationRule();

        // When
        String separator = rule.formatSeparator(LET, ID, "    ", UNSET);

        // Then
        assertEquals(" ", separator);
    }

    @Test
    @DisplayName(
            "Given: SingleSpaceSeparationRule | When: the current token is a semicolon | Then:"
                    + " removes the space before it")
    void singleSpaceSeparationRule_whenBeforeSemicolon_thenReturnsNoSpace() {
        // Given
        SingleSpaceSeparationRule rule = new SingleSpaceSeparationRule();

        // When
        String separator = rule.formatSeparator(NUM, SEMI, "  ", UNSET);

        // Then
        assertEquals("", separator);
    }

    @Test
    @DisplayName(
            "Given: SingleSpaceSeparationRule | When: the separator contains newlines | Then: keeps"
                    + " it untouched")
    void singleSpaceSeparationRule_whenSeparatorHasNewlines_thenKeepsIt() {
        // Given
        SingleSpaceSeparationRule rule = new SingleSpaceSeparationRule();

        // When
        String separator = rule.formatSeparator(SEMI, LET, "\n\n  ", UNSET);

        // Then
        assertEquals("\n\n  ", separator);
    }

    @Test
    @DisplayName(
            "Given: SingleSpaceSeparationRule | When: tokens are glued together | Then: keeps them"
                    + " glued and always applies")
    void singleSpaceSeparationRule_whenNoSeparator_thenKeepsTokensGlued() {
        // Given
        SingleSpaceSeparationRule rule = new SingleSpaceSeparationRule();

        // When
        String separator = rule.formatSeparator(ID, EQUAL, "", UNSET);

        // Then
        assertTrue(rule.applies(ID, EQUAL, UNSET));
        assertEquals("", separator);
    }

    @Test
    @DisplayName(
            "Given: SpaceBeforeColonRule enabled | When: a colon follows an identifier | Then:"
                    + " puts one space before the colon")
    void spaceBeforeColonRule_whenEnabled_thenAddsSpaceBeforeColon() {
        // Given
        SpaceBeforeColonRule rule = new SpaceBeforeColonRule();
        FormatContext ctx = FormatContextBuilder.unset().spaceBeforeColon(true).build();

        // When
        String separator = rule.formatSeparator(ID, COLON, "", ctx);

        // Then
        assertTrue(rule.applies(ID, COLON, ctx));
        assertEquals(" ", separator);
    }

    @Test
    @DisplayName(
            "Given: SpaceBeforeColonRule disabled | When: a colon follows a space | Then: removes"
                    + " the space")
    void spaceBeforeColonRule_whenDisabled_thenRemovesSpaceBeforeColon() {
        // Given
        SpaceBeforeColonRule rule = new SpaceBeforeColonRule();
        FormatContext ctx = FormatContextBuilder.unset().spaceBeforeColon(false).build();

        // When
        String separator = rule.formatSeparator(ID, COLON, " ", ctx);

        // Then
        assertEquals("", separator);
    }

    @Test
    @DisplayName(
            "Given: SpaceBeforeColonRule without config | When: a colon follows | Then: the rule"
                    + " does not apply (leaves the source as is)")
    void spaceBeforeColonRule_whenUnset_thenDoesNotApply() {
        // Given
        SpaceBeforeColonRule rule = new SpaceBeforeColonRule();

        // When
        boolean applies = rule.applies(ID, COLON, UNSET);

        // Then
        assertFalse(applies);
    }

    @Test
    @DisplayName(
            "Given: SpaceBeforeColonRule enabled | When: the current token is not a colon | Then:"
                    + " the rule does not apply")
    void spaceBeforeColonRule_whenCurrentIsNotColon_thenDoesNotApply() {
        // Given
        SpaceBeforeColonRule rule = new SpaceBeforeColonRule();
        FormatContext ctx = FormatContextBuilder.unset().spaceBeforeColon(true).build();

        // When
        boolean applies = rule.applies(COLON, TYPE, ctx);

        // Then
        assertFalse(applies || rule.applies(ID, null, ctx));
    }

    @Test
    @DisplayName(
            "Given: SpaceAfterColonRule enabled | When: a type follows a colon | Then: puts one"
                    + " space after the colon")
    void spaceAfterColonRule_whenEnabled_thenAddsSpaceAfterColon() {
        // Given
        SpaceAfterColonRule rule = new SpaceAfterColonRule();
        FormatContext ctx = FormatContextBuilder.unset().spaceAfterColon(true).build();

        // When
        String separator = rule.formatSeparator(COLON, TYPE, "", ctx);

        // Then
        assertTrue(rule.applies(COLON, TYPE, ctx));
        assertEquals(" ", separator);
    }

    @Test
    @DisplayName(
            "Given: SpaceAfterColonRule disabled | When: a type follows a colon with spaces | Then:"
                    + " removes the spaces")
    void spaceAfterColonRule_whenDisabled_thenRemovesSpaceAfterColon() {
        // Given
        SpaceAfterColonRule rule = new SpaceAfterColonRule();
        FormatContext ctx = FormatContextBuilder.unset().spaceAfterColon(false).build();

        // When
        String separator = rule.formatSeparator(COLON, TYPE, "   ", ctx);

        // Then
        assertEquals("", separator);
    }

    @Test
    @DisplayName(
            "Given: SpaceAfterColonRule | When: config is unset or previous is not a colon | Then:"
                    + " the rule does not apply")
    void spaceAfterColonRule_whenUnsetOrNotAfterColon_thenDoesNotApply() {
        // Given
        SpaceAfterColonRule rule = new SpaceAfterColonRule();
        FormatContext enabled = FormatContextBuilder.unset().spaceAfterColon(true).build();

        // When
        boolean unsetApplies = rule.applies(COLON, TYPE, UNSET);
        boolean notColonApplies = rule.applies(ID, COLON, enabled);

        // Then
        assertFalse(unsetApplies || notColonApplies);
    }

    @Test
    @DisplayName(
            "Given: SpaceAroundEqualsRule enabled | When: tokens are glued to '=' on both sides |"
                    + " Then: one space before and after")
    void spaceAroundEqualsRule_whenEnabled_thenSpacesBothSides() {
        // Given
        SpaceAroundEqualsRule rule = new SpaceAroundEqualsRule();
        FormatContext ctx = FormatContextBuilder.unset().spaceAroundEquals(true).build();

        // When
        String before = rule.formatSeparator(ID, EQUAL, "", ctx);
        String after = rule.formatSeparator(EQUAL, NUM, "", ctx);

        // Then
        assertEquals(" ", before);
        assertEquals(" ", after);
    }

    @Test
    @DisplayName(
            "Given: SpaceAroundEqualsRule disabled | When: '=' is surrounded by spaces | Then:"
                    + " removes them")
    void spaceAroundEqualsRule_whenDisabled_thenRemovesSpaces() {
        // Given
        SpaceAroundEqualsRule rule = new SpaceAroundEqualsRule();
        FormatContext ctx = FormatContextBuilder.unset().spaceAroundEquals(false).build();

        // When
        String separator = rule.formatSeparator(EQUAL, NUM, " ", ctx);

        // Then
        assertTrue(rule.applies(EQUAL, NUM, ctx));
        assertEquals("", separator);
    }

    @Test
    @DisplayName(
            "Given: SpaceAroundEqualsRule | When: config is unset or no '=' is involved | Then: the"
                    + " rule does not apply")
    void spaceAroundEqualsRule_whenUnsetOrNoEquals_thenDoesNotApply() {
        // Given
        SpaceAroundEqualsRule rule = new SpaceAroundEqualsRule();
        FormatContext enabled = FormatContextBuilder.unset().spaceAroundEquals(true).build();

        // When
        boolean unsetApplies = rule.applies(ID, EQUAL, UNSET);
        boolean noEqualsApplies = rule.applies(ID, NUM, enabled);
        boolean nullsApply = rule.applies(null, null, enabled);

        // Then
        assertFalse(unsetApplies || noEqualsApplies || nullsApply);
    }

    @Test
    @DisplayName(
            "Given: SpaceAfterCommaRule enabled | When: an argument follows a comma | Then: one"
                    + " space after the comma")
    void spaceAfterCommaRule_whenEnabled_thenAddsSpaceAfterComma() {
        // Given
        SpaceAfterCommaRule rule = new SpaceAfterCommaRule();
        FormatContext ctx = FormatContextBuilder.unset().spaceAfterComma(true).build();

        // When
        String separator = rule.formatSeparator(COMMA, NUM, "", ctx);

        // Then
        assertTrue(rule.applies(COMMA, NUM, ctx));
        assertEquals(" ", separator);
    }

    @Test
    @DisplayName(
            "Given: SpaceAfterCommaRule disabled | When: an argument follows a comma | Then: no"
                    + " space after the comma")
    void spaceAfterCommaRule_whenDisabled_thenRemovesSpaceAfterComma() {
        // Given
        SpaceAfterCommaRule rule = new SpaceAfterCommaRule();
        FormatContext ctx = FormatContextBuilder.unset().spaceAfterComma(false).build();

        // When
        String separator = rule.formatSeparator(COMMA, NUM, " ", ctx);

        // Then
        assertEquals("", separator);
    }

    @Test
    @DisplayName(
            "Given: SpaceAfterCommaRule | When: config is unset or previous is not a comma | Then:"
                    + " the rule does not apply")
    void spaceAfterCommaRule_whenUnsetOrNotAfterComma_thenDoesNotApply() {
        // Given
        SpaceAfterCommaRule rule = new SpaceAfterCommaRule();
        FormatContext enabled = FormatContextBuilder.unset().spaceAfterComma(true).build();

        // When
        boolean unsetApplies = rule.applies(COMMA, NUM, UNSET);
        boolean notCommaApplies = rule.applies(NUM, COMMA, enabled);

        // Then
        assertFalse(unsetApplies || notCommaApplies);
    }
}
