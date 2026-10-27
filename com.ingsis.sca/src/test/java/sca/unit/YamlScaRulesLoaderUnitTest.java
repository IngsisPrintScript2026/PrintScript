/*
 * My Project
 */

package sca.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sca.ScaContext;
import sca.config.YamlScaRulesLoader;

class YamlScaRulesLoaderUnitTest {

    private static InputStream yaml(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName(
            "Given: identifier_format set to camel case | When: loading the YAML | Then: the"
                    + " context enforces camel case")
    void loader_whenIdentifierFormatIsCamelCase_thenContextHasCamelCase() {
        // Given
        InputStream config = yaml("identifier_format: \"camel case\"\n");

        // When
        ScaContext context = YamlScaRulesLoader.loadFromYaml(config);

        // Then
        assertEquals("camel case", context.identifierFormat());
    }

    @Test
    @DisplayName(
            "Given: identifier_format set to snake case | When: loading the YAML | Then: the"
                    + " context enforces snake case")
    void loader_whenIdentifierFormatIsSnakeCase_thenContextHasSnakeCase() {
        // Given
        InputStream config = yaml("identifier_format: \"snake case\"\n");

        // When
        ScaContext context = YamlScaRulesLoader.loadFromYaml(config);

        // Then
        assertEquals("snake case", context.identifierFormat());
    }

    @Test
    @DisplayName(
            "Given: the kebab-case alias identifier-format | When: loading the YAML | Then: the"
                    + " naming convention is read")
    void loader_whenKebabCaseIdentifierFormatAlias_thenReadsConvention() {
        // Given
        InputStream config = yaml("identifier-format: \"snake case\"\n");

        // When
        ScaContext context = YamlScaRulesLoader.loadFromYaml(config);

        // Then
        assertEquals("snake case", context.identifierFormat());
    }

    @Test
    @DisplayName(
            "Given: the camelCase alias identifierFormat | When: loading the YAML | Then: the"
                    + " naming convention is read")
    void loader_whenCamelCaseIdentifierFormatAlias_thenReadsConvention() {
        // Given
        InputStream config = yaml("identifierFormat: \"camel case\"\n");

        // When
        ScaContext context = YamlScaRulesLoader.loadFromYaml(config);

        // Then
        assertEquals("camel case", context.identifierFormat());
    }

    @Test
    @DisplayName(
            "Given: mandatory-variable-or-literal-in-println: true | When: loading the YAML | Then:"
                    + " only the println rule is enabled")
    void loader_whenPrintlnRuleEnabled_thenOnlyPrintlnRuleIsOn() {
        // Given
        InputStream config = yaml("mandatory-variable-or-literal-in-println: true\n");

        // When
        ScaContext context = YamlScaRulesLoader.loadFromYaml(config);

        // Then
        assertTrue(context.mandatoryLiteralOrIdentifierInPrintln());
        assertFalse(context.mandatoryLiteralOrIdentifierInReadInput());
    }

    @Test
    @DisplayName(
            "Given: mandatory-variable-or-literal-in-readInput: true | When: loading the YAML |"
                    + " Then: only the readInput rule is enabled")
    void loader_whenReadInputRuleEnabled_thenOnlyReadInputRuleIsOn() {
        // Given
        InputStream config = yaml("mandatory-variable-or-literal-in-readInput: true\n");

        // When
        ScaContext context = YamlScaRulesLoader.loadFromYaml(config);

        // Then
        assertTrue(context.mandatoryLiteralOrIdentifierInReadInput());
        assertFalse(context.mandatoryLiteralOrIdentifierInPrintln());
    }

    @Test
    @DisplayName(
            "Given: the camelCase aliases of both argument rules | When: loading the YAML | Then:"
                    + " both rules are enabled")
    void loader_whenCamelCaseRuleAliases_thenBothRulesAreOn() {
        // Given
        InputStream config =
                yaml(
                        "mandatoryVariableOrLiteralInPrintln: true\n"
                                + "mandatoryVariableOrLiteralInReadInput: true\n");

        // When
        ScaContext context = YamlScaRulesLoader.loadFromYaml(config);

        // Then
        assertTrue(context.mandatoryLiteralOrIdentifierInPrintln());
        assertTrue(context.mandatoryLiteralOrIdentifierInReadInput());
    }

    @Test
    @DisplayName(
            "Given: the snake_case aliases of both argument rules | When: loading the YAML | Then:"
                    + " both rules are enabled")
    void loader_whenSnakeCaseRuleAliases_thenBothRulesAreOn() {
        // Given
        InputStream config =
                yaml(
                        "mandatory_variable_or_literal_in_println: true\n"
                                + "mandatory_variable_or_literal_in_read_input: true\n");

        // When
        ScaContext context = YamlScaRulesLoader.loadFromYaml(config);

        // Then
        assertTrue(context.mandatoryLiteralOrIdentifierInPrintln());
        assertTrue(context.mandatoryLiteralOrIdentifierInReadInput());
    }

    @Test
    @DisplayName(
            "Given: the lowercase alias mandatory-variable-or-literal-in-readinput | When: loading"
                    + " the YAML | Then: the readInput rule is enabled")
    void loader_whenLowercaseReadInputAlias_thenReadInputRuleIsOn() {
        // Given
        InputStream config = yaml("mandatory-variable-or-literal-in-readinput: true\n");

        // When
        ScaContext context = YamlScaRulesLoader.loadFromYaml(config);

        // Then
        assertTrue(context.mandatoryLiteralOrIdentifierInReadInput());
    }

    @Test
    @DisplayName(
            "Given: rule values written as quoted strings | When: loading the YAML | Then: \"true\""
                    + " enables and \"false\" disables")
    void loader_whenRuleValuesAreStrings_thenParsesThemAsBooleans() {
        // Given
        InputStream config =
                yaml(
                        "mandatory-variable-or-literal-in-println: \"true\"\n"
                                + "mandatory-variable-or-literal-in-readInput: \"false\"\n");

        // When
        ScaContext context = YamlScaRulesLoader.loadFromYaml(config);

        // Then
        assertTrue(context.mandatoryLiteralOrIdentifierInPrintln());
        assertFalse(context.mandatoryLiteralOrIdentifierInReadInput());
    }

    @Test
    @DisplayName(
            "Given: a rule value that is neither boolean nor string | When: loading the YAML |"
                    + " Then: the rule stays disabled")
    void loader_whenRuleValueIsNumber_thenRuleStaysDisabled() {
        // Given
        InputStream config = yaml("mandatory-variable-or-literal-in-println: 1\n");

        // When
        ScaContext context = YamlScaRulesLoader.loadFromYaml(config);

        // Then
        assertFalse(context.mandatoryLiteralOrIdentifierInPrintln());
    }

    @Test
    @DisplayName(
            "Given: a null stream | When: loading the YAML | Then: returns the default context")
    void loader_whenStreamIsNull_thenReturnsDefaults() {
        // Given
        InputStream config = null;

        // When
        ScaContext context = YamlScaRulesLoader.loadFromYaml(config);

        // Then
        assertEquals(new ScaContext(), context);
    }

    @Test
    @DisplayName(
            "Given: an empty YAML document | When: loading the YAML | Then: returns the default"
                    + " context")
    void loader_whenYamlIsEmpty_thenReturnsDefaults() {
        // Given
        InputStream config = yaml("");

        // When
        ScaContext context = YamlScaRulesLoader.loadFromYaml(config);

        // Then
        assertEquals(new ScaContext(), context);
    }

    @Test
    @DisplayName(
            "Given: malformed YAML | When: loading the YAML | Then: returns the default context"
                    + " instead of failing")
    void loader_whenYamlIsMalformed_thenReturnsDefaults() {
        // Given
        InputStream config = yaml("identifier_format: [unclosed\n");

        // When
        ScaContext context = YamlScaRulesLoader.loadFromYaml(config);

        // Then
        assertEquals(new ScaContext(), context);
    }

    @Test
    @DisplayName(
            "Given: YAML whose root is a list instead of a map | When: loading the YAML | Then:"
                    + " returns the default context")
    void loader_whenYamlRootIsNotAMap_thenReturnsDefaults() {
        // Given
        InputStream config = yaml("- identifier_format\n- snake case\n");

        // When
        ScaContext context = YamlScaRulesLoader.loadFromYaml(config);

        // Then
        assertEquals(new ScaContext(), context);
    }

    @Test
    @DisplayName(
            "Given: identifier_format present but null | When: loading the YAML | Then: no"
                    + " naming convention is enforced")
    void loader_whenIdentifierFormatIsNull_thenNoConvention() {
        // Given
        InputStream config = yaml("identifier_format:\n");

        // When
        ScaContext context = YamlScaRulesLoader.loadFromYaml(config);

        // Then
        assertNull(context.identifierFormat());
    }
}
