/*
 * My Project
 */

package formatter.unit;

import static formatter.support.FormatterFixtures.context;
import static formatter.support.FormatterFixtures.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import formatter.FormatContext;
import formatter.config.YamlFormatRulesLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class YamlFormatRulesLoaderUnitTest {

    @Test
    @DisplayName(
            "Given: a null YAML stream | When: loading the config | Then: returns the default"
                    + " context")
    void loader_whenStreamIsNull_thenReturnsDefaults() {
        // Given
        FormatContext defaults = new FormatContext();

        // When
        FormatContext ctx = YamlFormatRulesLoader.loadFromYaml(null);

        // Then
        assertEquals(defaults, ctx);
    }

    @Test
    @DisplayName(
            "Given: an empty YAML document | When: loading the config | Then: returns the default"
                    + " context")
    void loader_whenDocumentIsEmpty_thenReturnsDefaults() {
        // Given
        String yaml = "";

        // When
        FormatContext ctx = context(yaml);

        // Then
        assertEquals(new FormatContext(), ctx);
    }

    @Test
    @DisplayName(
            "Given: malformed YAML | When: loading the config | Then: falls back to the default"
                    + " context")
    void loader_whenYamlIsMalformed_thenReturnsDefaults() {
        // Given
        String yaml = "space-before-colon: [unclosed";

        // When
        FormatContext ctx = YamlFormatRulesLoader.loadFromYaml(stream(yaml));

        // Then
        assertEquals(new FormatContext(), ctx);
    }

    @Test
    @DisplayName(
            "Given: a YAML map without known keys | When: loading the config | Then: every option"
                    + " stays unset (leave as is)")
    void loader_whenNoKnownKeys_thenOptionsAreUnset() {
        // Given
        String yaml = "unknown: 1";

        // When
        FormatContext ctx = context(yaml);

        // Then
        assertNull(ctx.spaceBeforeColon());
        assertNull(ctx.spaceAroundEquals());
        assertNull(ctx.spaceAfterComma());
        assertNull(ctx.lineBreaksBeforePrintln());
        assertNull(ctx.indentSpaces());
    }

    @Test
    @DisplayName(
            "Given: the assignment's colon keys | When: loading the config | Then: maps them to"
                    + " space before and after colon")
    void loader_whenColonKeys_thenMapsColonSpacing() {
        // Given
        String yaml =
                "enforce-spacing-before-colon-in-declaration: true\n"
                        + "enforce-spacing-after-colon-in-declaration: false";

        // When
        FormatContext ctx = context(yaml);

        // Then
        assertEquals(true, ctx.spaceBeforeColon());
        assertEquals(false, ctx.spaceAfterColon());
    }

    @Test
    @DisplayName("Given: short colon keys | When: loading the config | Then: maps them as well")
    void loader_whenShortColonKeys_thenMapsColonSpacing() {
        // Given
        String yaml = "space-before-colon: false\nspaceAfterColon: true";

        // When
        FormatContext ctx = context(yaml);

        // Then
        assertEquals(false, ctx.spaceBeforeColon());
        assertEquals(true, ctx.spaceAfterColon());
    }

    @Test
    @DisplayName(
            "Given: enforce-spacing-around-equals false | When: loading the config | Then: space"
                    + " around equals is disabled")
    void loader_whenSpacingAroundEqualsFalse_thenDisablesIt() {
        // Given
        String yaml = "enforce-spacing-around-equals: false";

        // When
        FormatContext ctx = context(yaml);

        // Then
        assertEquals(false, ctx.spaceAroundEquals());
    }

    @Test
    @DisplayName(
            "Given: enforce-no-spacing-around-equals true | When: loading the config | Then: space"
                    + " around equals is disabled (the flag is inverted)")
    void loader_whenNoSpacingAroundEqualsTrue_thenDisablesIt() {
        // Given
        String yaml = "enforce-no-spacing-around-equals: true";

        // When
        FormatContext ctx = context(yaml);

        // Then
        assertEquals(false, ctx.spaceAroundEquals());
    }

    @Test
    @DisplayName(
            "Given: enforce-no-spacing-around-equals false | When: loading the config | Then: space"
                    + " around equals is enabled")
    void loader_whenNoSpacingAroundEqualsFalse_thenEnablesIt() {
        // Given
        String yaml = "enforce-no-spacing-around-equals: false";

        // When
        FormatContext ctx = context(yaml);

        // Then
        assertEquals(true, ctx.spaceAroundEquals());
    }

    @Test
    @DisplayName(
            "Given: line-breaks-before-println and line-breaks-after-println | When: loading the"
                    + " config | Then: each maps to its own option")
    void loader_whenPrintlnBreakKeys_thenMapsBeforeAndAfterSeparately() {
        // Given
        String yaml = "line-breaks-before-println: 2\nline-breaks-after-println: 0";

        // When
        FormatContext ctx = context(yaml);

        // Then
        assertEquals(2, ctx.lineBreaksBeforePrintln());
        assertEquals(0, ctx.lineBreaksAfterPrintln());
    }

    @Test
    @DisplayName(
            "Given: the camelCase lineBreaksBeforePrintln key as a string | When: loading the"
                    + " config | Then: parses it as a number")
    void loader_whenCamelCaseBeforePrintlnAsString_thenParsesIt() {
        // Given
        String yaml = "lineBreaksBeforePrintln: '1'";

        // When
        FormatContext ctx = context(yaml);

        // Then
        assertEquals(1, ctx.lineBreaksBeforePrintln());
        assertNull(ctx.lineBreaksAfterPrintln());
    }

    @Test
    @DisplayName("Given: a non-numeric line break value | When: loading the config | Then: uses 0")
    void loader_whenLineBreakValueIsNotNumeric_thenUsesZero() {
        // Given
        String yaml = "line-breaks-before-println: many";

        // When
        FormatContext ctx = context(yaml);

        // Then
        assertEquals(0, ctx.lineBreaksBeforePrintln());
    }

    @Test
    @DisplayName(
            "Given: indent-inside-if and if-brace-below-line | When: loading the config | Then:"
                    + " maps indentation and brace placement")
    void loader_whenIndentAndBraceKeys_thenMapsBlockOptions() {
        // Given
        String yaml = "indent-inside-if: 2\nif-brace-below-line: true\nif-brace-same-line: false";

        // When
        FormatContext ctx = context(yaml);

        // Then
        assertEquals(2, ctx.indentSpaces());
        assertEquals(true, ctx.ifBraceBelowLine());
        assertEquals(false, ctx.ifBraceSameLine());
    }

    @Test
    @DisplayName(
            "Given: boolean values written as strings | When: loading the config | Then: parses"
                    + " them as booleans")
    void loader_whenBooleansAreStrings_thenParsesThem() {
        // Given
        String yaml = "space-after-comma: 'false'\nmandatory-line-break-after-statement: 'true'";

        // When
        FormatContext ctx = context(yaml);

        // Then
        assertEquals(false, ctx.spaceAfterComma());
        assertEquals(true, ctx.lineBreakAfterStatement());
    }

    @Test
    @DisplayName(
            "Given: a boolean key with a numeric value | When: loading the config | Then: treats"
                    + " it as false")
    void loader_whenBooleanKeyHasNumber_thenTreatsAsFalse() {
        // Given
        String yaml = "mandatory-space-surrounding-operations: 1\nsingle-space-separation: true";

        // When
        FormatContext ctx = context(yaml);

        // Then
        assertEquals(false, ctx.spaceAroundOperators());
        assertEquals(true, ctx.singleSpaceSeparation());
    }
}
