/*
 * My Project
 */

package sca.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import sca.ScaContext;
import sca.TokenStreamSca;
import sca.config.YamlScaRulesLoader;

class TokenStreamScaIntegrationTest {

    private static ScaContext config(String yaml) {
        return YamlScaRulesLoader.loadFromYaml(LintRunner.stream(yaml));
    }

    private static List<String> violations(String source, String yaml) {
        Result<List<String>> result =
                new TokenStreamSca(config(yaml)).analyze(LintRunner.stream(source));
        return ((CorrectResult<List<String>>) result).value();
    }

    @Test
    @DisplayName(
            "Given: camel case config | When: analyzing camel case source | Then: reports no"
                    + " violation")
    void tokenStreamSca_whenCodeAdheresToCamelCase_thenPasses() {
        // Given
        String source = "let myVariable: number = 10;\nconst otherOne: string = 'a';";

        // When
        List<String> violations = violations(source, "identifier_format: \"camel case\"\n");

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: camel case config | When: line 2 declares 'my_variable' | Then: reports the"
                    + " name and its line and column")
    void tokenStreamSca_whenCodeViolatesCamelCase_thenReportsViolationWithPosition() {
        // Given
        String source = "let ok: number = 1;\nlet my_variable: number = 10;";

        // When
        List<String> violations = violations(source, "identifier_format: \"camel case\"\n");

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("'my_variable'"));
        assertTrue(violations.get(0).contains("line 2, column 5"));
    }

    @Test
    @DisplayName(
            "Given: snake case config | When: a const is declared in camel case | Then: reports"
                    + " the violation")
    void tokenStreamSca_whenConstViolatesSnakeCase_thenReportsViolation() {
        // Given
        String source = "const maxValue: number = 10;";

        // When
        List<String> violations = violations(source, "identifier_format: \"snake case\"\n");

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("snake case"));
    }

    @Test
    @DisplayName(
            "Given: println rule enabled | When: println(1 + 2) | Then: reports the operator"
                    + " position")
    void tokenStreamSca_whenPrintlnHasExpression_thenReportsViolation() {
        // Given
        String source = "println(1 + 2);";

        // When
        List<String> violations =
                violations(source, "mandatory-variable-or-literal-in-println: true\n");

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("'println'"));
        assertTrue(violations.get(0).contains("line 1, column 11"));
    }

    @Test
    @DisplayName(
            "Given: println rule enabled | When: println receives a literal | Then: reports no"
                    + " violation")
    void tokenStreamSca_whenPrintlnHasLiteral_thenPasses() {
        // Given
        String source = "println(\"hello\");";

        // When
        List<String> violations =
                violations(source, "mandatory-variable-or-literal-in-println: true\n");

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName(
            "Given: readInput rule enabled | When: readInput(\"a\" + \"b\") | Then: reports a"
                    + " readInput violation")
    void tokenStreamSca_whenReadInputHasExpression_thenReportsViolation() {
        // Given
        String source = "let name: string = readInput(\"a\" + \"b\");";

        // When
        List<String> violations =
                violations(source, "mandatory-variable-or-literal-in-readInput: true\n");

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("'readInput'"));
    }

    @Test
    @DisplayName(
            "Given: snake case config | When: a declaration sits inside an if block | Then: it is"
                    + " also checked")
    void tokenStreamSca_whenDeclarationInsideIf_thenIsChecked() {
        // Given
        String source = "if (true) {\n    let innerName: number = 1;\n}";

        // When
        List<String> violations = violations(source, "identifier_format: \"snake case\"\n");

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("line 2"));
    }

    @Test
    @DisplayName(
            "Given: TokenStreamSca with default rules | When: analyzing with an explicit context |"
                    + " Then: the explicit context is applied")
    void tokenStreamSca_whenExplicitContextGiven_thenAppliesIt() {
        // Given
        InputStream source = LintRunner.stream("let badName: number = 1;");

        // When
        Result<List<String>> result =
                new TokenStreamSca().analyze(source, new ScaContext("snake case", false, false));

        // Then
        assertEquals(1, ((CorrectResult<List<String>>) result).value().size());
    }

    @Test
    @DisplayName(
            "Given: TokenStreamSca | When: analyzing a null input stream | Then: fails with an"
                    + " analysis error")
    void tokenStreamSca_whenInputStreamIsNull_thenFails() {
        // Given
        InputStream source = null;

        // When
        Result<List<String>> result = new TokenStreamSca().analyze(source);

        // Then
        assertFalse(result.isCorrect());
        assertTrue(((IncorrectResult<List<String>>) result).error().contains("Analysis error"));
    }
}
