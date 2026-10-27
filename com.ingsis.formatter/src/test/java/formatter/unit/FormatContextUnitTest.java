/*
 * My Project
 */

package formatter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import formatter.FormatContext;
import formatter.support.FormatContextBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FormatContextUnitTest {

    @Test
    @DisplayName(
            "Given: the default FormatContext | When: reading its options | Then: uses the"
                    + " documented defaults")
    void formatContext_whenDefault_thenUsesDocumentedDefaults() {
        // Given
        FormatContext ctx = new FormatContext();

        // When
        int indent = ctx.getIndentSpaces();

        // Then
        assertEquals(4, indent);
        assertFalse(ctx.isSpaceBeforeColon());
        assertTrue(ctx.isSpaceAfterColon() && ctx.isSpaceAroundEquals());
        assertTrue(ctx.isIfBraceSameLine() && ctx.isSpaceAfterComma());
        assertEquals(1, ctx.getLineBreaksAfterPrintln());
        assertNull(ctx.lineBreaksBeforePrintln());
    }

    @Test
    @DisplayName(
            "Given: a FormatContext with every option unset | When: reading the convenience getters"
                    + " | Then: falls back to the defaults")
    void formatContext_whenOptionsUnset_thenGettersFallBackToDefaults() {
        // Given
        FormatContext ctx = FormatContextBuilder.unset().build();

        // When
        int indent = ctx.getIndentSpaces();

        // Then
        assertEquals(4, indent);
        assertFalse(ctx.isSpaceBeforeColon());
        assertTrue(ctx.isSpaceAfterColon() && ctx.isSpaceAroundEquals());
        assertTrue(ctx.isSpaceAroundOperators() && ctx.isIfBraceSameLine());
        assertTrue(ctx.isSpaceAfterComma());
        assertEquals(1, ctx.getLineBreaksAfterPrintln());
    }

    @Test
    @DisplayName(
            "Given: a FormatContext with explicit options | When: reading the convenience getters |"
                    + " Then: returns the configured values")
    void formatContext_whenOptionsSet_thenGettersReturnThem() {
        // Given
        FormatContext ctx =
                new FormatContext(
                        0, 2, true, false, false, false, true, 3, true, false, true, false, 2);

        // When
        int indent = ctx.getIndentSpaces();

        // Then
        assertEquals(2, indent);
        assertTrue(ctx.isSpaceBeforeColon());
        assertFalse(ctx.isSpaceAfterColon() || ctx.isSpaceAroundEquals());
        assertFalse(ctx.isSpaceAroundOperators() || ctx.isIfBraceSameLine());
        assertFalse(ctx.isSpaceAfterComma());
        assertEquals(3, ctx.getLineBreaksAfterPrintln());
        assertEquals(2, ctx.lineBreaksBeforePrintln());
    }

    @Test
    @DisplayName(
            "Given: the 12-argument constructor | When: building a context | Then: lines before"
                    + " println stays unset")
    void formatContext_whenTwelveArgConstructor_thenLinesBeforePrintlnIsNull() {
        // Given
        Boolean spaceAfterComma = false;

        // When
        FormatContext ctx =
                new FormatContext(
                        0,
                        4,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        spaceAfterComma);

        // Then
        assertNull(ctx.lineBreaksBeforePrintln());
        assertEquals(false, ctx.spaceAfterComma());
    }

    @Test
    @DisplayName(
            "Given: the 11-argument constructor | When: building a context | Then: space after"
                    + " comma defaults to true and lines before println stays unset")
    void formatContext_whenElevenArgConstructor_thenSpaceAfterCommaIsTrue() {
        // Given
        Integer indentSpaces = 3;

        // When
        FormatContext ctx =
                new FormatContext(
                        0, indentSpaces, null, null, null, null, null, null, null, null, null);

        // Then
        assertEquals(true, ctx.spaceAfterComma());
        assertNull(ctx.lineBreaksBeforePrintln());
        assertEquals(3, ctx.getIndentSpaces());
    }

    @Test
    @DisplayName(
            "Given: a context at indent level 0 with 2 spaces | When: incrementing the indent twice"
                    + " | Then: the indent is 4 spaces and the other options are kept")
    void formatContext_whenIncrementingIndent_thenGrowsIndentAndKeepsOptions() {
        // Given
        FormatContext ctx =
                FormatContextBuilder.unset().indentSpaces(2).lineBreaksBeforePrintln(1).build();

        // When
        FormatContext nested = ctx.incrementIndent().incrementIndent();

        // Then
        assertEquals("", ctx.getIndent());
        assertEquals("    ", nested.getIndent());
        assertEquals(2, nested.indentLevel());
        assertEquals(1, nested.lineBreaksBeforePrintln());
    }
}
